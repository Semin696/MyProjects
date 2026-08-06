#include "app.h"

// ===================== Intro / boot animation (Windows 11 style) =====================
// Timeline (seconds since launch):
//   0.0 - 2.2  INTRO_BOOT    - dark screen + Win11 spinner dots
//   2.2 - 3.6  INTRO_LOAD    - loading bar slides in and fills
//   3.6 - 4.0  INTRO_RIP     - crowbar pries, bar is ripped out and falls
//   4.0 - 6.0  INTRO_ASSEMBLE- title springs in, parts fly in and get nailed
//   6.0+       INTRO_DONE    - only window-move wobble remains

static ULONGLONG g_startTick = 0;
IntroStage g_introStage = INTRO_BOOT;
static float g_introT = 0.0f;

static const float LOAD_W = 320.0f, LOAD_H = 16.0f;
static float g_loadX, g_loadY, g_loadVX, g_loadVY, g_loadRot, g_loadVRot;
static float g_loadProgress = 0.0f;
static bool g_loadRipped = false;
static bool g_loadGone = false;
static float g_crowPhase = 0.0f;
static float g_crowAng = 0.0f;

// title spring
static float g_titleX = 40.0f, g_titleY = 900.0f, g_titleVY = 0.0f;
static bool g_titleStarted = false;

// hammer (nailing animation)
static bool g_hammerActive = false;
static float g_hammerX, g_hammerY, g_hammerT;
static float g_hammerAng = 0.0f;

static WobblePart g_parts[PART_COUNT];

static POINT g_winPos = { 0, 0 };

static float Clamp01(float v) { return v < 0 ? 0 : (v > 1 ? 1 : v); }

static float EaseOutBack(float u)
{
    const float c1 = 1.70158f;
    const float c3 = c1 + 1.0f;
    return 1.0f + c3 * powf(u - 1, 3) + c1 * powf(u - 1, 2);
}

bool IsIntroDone() { return g_introStage == INTRO_DONE; }

void ComputePartTargets()
{
    RECT rc;
    GetClientRect(g_hwnd, &rc);
    int W = rc.right - rc.left, H = rc.bottom - rc.top;
    const int margin = 26;

    g_parts[PART_ACCENT].tx = (float)margin; g_parts[PART_ACCENT].ty = 24;
    g_parts[PART_ACCENT].w = 4; g_parts[PART_ACCENT].h = 26;

    g_parts[PART_SUB].tx = (float)(margin + 14); g_parts[PART_SUB].ty = 52;
    g_parts[PART_SUB].w = 400; g_parts[PART_SUB].h = 20;

    int x = 26, y = 90, wBtn = 150, hBtn = 38, gap = 12;
    g_parts[PART_BTN0].tx = (float)x; g_parts[PART_BTN0].ty = (float)y;
    g_parts[PART_BTN0].w = (float)wBtn; g_parts[PART_BTN0].h = (float)hBtn;
    g_parts[PART_BTN1].tx = (float)(x + wBtn + gap); g_parts[PART_BTN1].ty = (float)y;
    g_parts[PART_BTN1].w = 165; g_parts[PART_BTN1].h = (float)hBtn;
    g_parts[PART_BTN2].tx = (float)(x + wBtn + gap + 165 + gap); g_parts[PART_BTN2].ty = (float)y;
    g_parts[PART_BTN2].w = 115; g_parts[PART_BTN2].h = (float)hBtn;

    int contentTop = 244;
    int contentBottom = H - 44;
    int contentH = contentBottom - contentTop;
    int panelW = (W - margin * 2 - 16) / 2;
    if (panelW < 10) panelW = 10;
    if (contentH < 10) contentH = 10;
    g_parts[PART_PANEL0].tx = (float)margin; g_parts[PART_PANEL0].ty = (float)contentTop;
    g_parts[PART_PANEL0].w = (float)panelW; g_parts[PART_PANEL0].h = (float)contentH;
    g_parts[PART_PANEL1].tx = (float)(margin + panelW + 16); g_parts[PART_PANEL1].ty = (float)contentTop;
    g_parts[PART_PANEL1].w = (float)panelW; g_parts[PART_PANEL1].h = (float)contentH;
}

static void InitPart(int id, float flyT, float sx, float sy, float k, float c, bool nails)
{
    WobblePart& p = g_parts[id];
    p.sx = sx; p.sy = sy;
    p.k = k; p.c = c;
    p.hasNails = nails;
    p.visible = false;
    p.landed = false;
    p.ox = sx; p.oy = sy;
    p.rot = 0; p.vrot = 0; p.ovx = 0; p.ovy = 0;
    p.nailScale = 0;
}

void InitIntro()
{
    g_startTick = GetTickCount64();
    g_introStage = INTRO_BOOT;
    g_introT = 0.0f;

    g_loadX = 0; g_loadY = 0; g_loadVX = 0; g_loadVY = 0;
    g_loadRot = 0; g_loadVRot = 0; g_loadProgress = 0;
    g_loadRipped = false; g_loadGone = false;
    g_crowPhase = 0; g_crowAng = 0;

    g_titleStarted = false;
    g_titleY = 900.0f; g_titleVY = 0;
    g_hammerActive = false;

    RECT wr;
    GetWindowRect(g_hwnd, &wr);
    g_winPos = { wr.left, wr.top };

    // fly-in schedules: start off-screen, spring k/c for wobble after nailing
    InitPart(PART_ACCENT, 4.15f, -60, 30, 40, 8, false);
    InitPart(PART_SUB,    4.30f, -260, 60, 40, 8, false);
    InitPart(PART_BTN0,   4.45f, -260, 90, 42, 8, true);
    InitPart(PART_BTN1,   4.58f, -300, 110, 42, 8, true);
    InitPart(PART_BTN2,   4.70f, -220, 80, 42, 8, true);
    InitPart(PART_PANEL0, 4.90f, 0, 900, 30, 6, true);
    InitPart(PART_PANEL1, 5.05f, 60, 960, 30, 6, true);
}

void UpdateIntro(float dt)
{
    g_introT = (float)(GetTickCount64() - g_startTick) / 1000.0f;
    float t = g_introT;

    if (t < 2.2f) g_introStage = INTRO_BOOT;
    else if (t < 3.6f) g_introStage = INTRO_LOAD;
    else if (t < 4.0f) g_introStage = INTRO_RIP;
    else if (t < 6.0f) g_introStage = INTRO_ASSEMBLE;
    else g_introStage = INTRO_DONE;

    RECT rc;
    GetClientRect(g_hwnd, &rc);
    int W = rc.right - rc.left, H = rc.bottom - rc.top;

    // ---- window movement for loose wobble ----
    RECT wr;
    GetWindowRect(g_hwnd, &wr);
    float wdx = (float)(wr.left - g_winPos.x);
    float wdy = (float)(wr.top - g_winPos.y);
    g_winPos.x = wr.left;
    g_winPos.y = wr.top;

    // ---- loading bar ----
    if (g_introStage == INTRO_LOAD)
    {
        float appear = Clamp01((t - 2.2f) / 0.4f);
        g_loadX = (W - LOAD_W) * 0.5f;
        g_loadY = H * 0.5f + 60.0f + (1.0f - appear) * 40.0f;
        g_loadRot = 0;
        g_loadProgress = Clamp01((t - 2.4f) / 1.1f);
    }
    else if (g_introStage == INTRO_RIP && !g_loadRipped)
    {
        g_crowPhase = Clamp01((t - 3.6f) / 0.4f);
        g_crowAng = -25.0f + g_crowPhase * 40.0f;   // prying lever motion
        g_loadRot = -0.15f * g_crowPhase;           // bar lifts at the end
        g_loadX += 0;
        g_loadY -= 1.5f * g_crowPhase;
        if (g_crowPhase >= 1.0f)
        {
            g_loadRipped = true;
            g_loadVX = -460.0f;
            g_loadVY = -640.0f;
            g_loadVRot = -9.0f;
        }
    }
    if (g_loadRipped && !g_loadGone)
    {
        g_loadVY += 1900.0f * dt;
        g_loadX += g_loadVX * dt;
        g_loadY += g_loadVY * dt;
        g_loadRot += g_loadVRot * dt;
        if (g_loadY > H + 140) g_loadGone = true;
    }

    // ---- title spring (overshoot: a bit above, a bit below, settle) ----
    if (!g_titleStarted && t >= 4.0f)
    {
        g_titleStarted = true;
        g_titleY = 900.0f;
        g_titleVY = 0;
    }
    if (g_titleStarted)
    {
        const float targetY = 22.0f;
        const float k = 26.0f, c = 3.6f;
        g_titleVY += ((targetY - g_titleY) * k - g_titleVY * c) * dt;
        g_titleY += g_titleVY * dt;
    }

    // ---- parts fly in and get nailed ----
    for (int i = 0; i < PART_COUNT; i++)
    {
        WobblePart& p = g_parts[i];
        if (t < 4.15f) { p.visible = false; continue; }

        // stagger each part by its index-dependent start
        float flyT = 4.15f + i * 0.15f;
        p.visible = true;

        if (!p.landed)
        {
            float u = (t - flyT) / 0.45f;
            if (u >= 1.0f)
            {
                p.landed = true;
                p.ox = 0; p.oy = 0;
                p.ovx = (Rand01() - 0.5f) * 40.0f;
                p.ovy = (Rand01() - 0.5f) * 40.0f;
                p.vrot = (Rand01() - 0.5f) * 0.6f;
                p.nailScale = 0;
                if (p.hasNails)
                {
                    g_hammerActive = true;
                    g_hammerX = p.tx + p.w * 0.5f;
                    g_hammerY = p.ty + 4.0f;
                    g_hammerT = 0;
                }
            }
            else
            {
                float e = EaseOutBack(u);
                p.ox = (p.sx - p.tx) * (1.0f - e);
                p.oy = (p.sy - p.ty) * (1.0f - e);
                p.rot = -0.25f * (1.0f - e);
                p.nailScale = 0;
            }
        }

        if (p.landed)
        {
            // loose-nail wobble physics
            p.ovx += wdx * 1.3f;
            p.ovy += wdy * 1.3f;
            p.vrot += wdx * 0.004f;
            p.ovx += (-p.k * p.ox - p.c * p.ovx) * dt;
            p.ovy += (-p.k * p.oy - p.c * p.ovy) * dt;
            p.ox += p.ovx * dt;
            p.oy += p.ovy * dt;
            p.vrot += (-p.k * 0.4f * p.rot - p.c * p.vrot) * dt;
            p.rot += p.vrot * dt;
            p.nailScale = std::min(1.0f, p.nailScale + dt * 8.0f);
        }
    }

    // ---- hammer swing ----
    if (g_hammerActive)
    {
        g_hammerT += dt;
        if (g_hammerT < 0.22f)
            g_hammerAng = -1.2f + (g_hammerT / 0.22f) * 1.2f;   // swing down
        else if (g_hammerT < 0.4f)
            g_hammerAng = 0.0f;                                 // impact hold
        else
            g_hammerAng = -1.0f * Clamp01((g_hammerT - 0.4f) / 0.2f); // up
        if (g_hammerT > 0.6f) g_hammerActive = false;
    }
}

// ===================== Drawing =====================

static void DrawSpinner(Graphics& g, float cx, float cy, float alpha, float t)
{
    const int n = 5;
    const float R = 18.0f;
    float base = t * 2.2f;
    for (int i = 0; i < n; i++)
    {
        float a = base + i * (6.28318f / n);
        float x = cx + cosf(a) * R;
        float y = cy + sinf(a) * R;
        float b = 0.5f + 0.5f * cosf(a - base);
        SolidBrush dot(Color((BYTE)(alpha * 255), (BYTE)(40 + 60 * b), (BYTE)(110 + 110 * b), 235));
        float s = 3.5f + 2.5f * b;
        g.FillEllipse(&dot, x - s, y - s, s * 2, s * 2);
    }
}

static void DrawCrowbar(Graphics& g, float cx, float cy, float ang, float alpha)
{
    GraphicsState st = g.Save();
    g.TranslateTransform(cx, cy);
    g.RotateTransform(ang);
    Pen body(Color((BYTE)(alpha * 255), 150, 152, 162), 9.0f);
    Pen dark(Color((BYTE)(alpha * 255), 95, 97, 108), 2.0f);
    // claw curve hooking under the bar end
    GraphicsPath claw;
    claw.AddArc(-20, -34, 40, 40, 200, -120);
    g.DrawPath(&body, &claw);
    g.DrawPath(&dark, &claw);
    // handle
    g.DrawLine(&body, -4, 2, -4, 64);
    g.DrawLine(&dark, -4, 2, -4, 64);
    // handle end bend
    g.DrawLine(&body, -4, 62, 12, 78);
    g.Restore(st);
}

static void DrawLoadingBar(Graphics& g, float alpha)
{
    if (g_loadGone) return;
    GraphicsState st = g.Save();
    g.TranslateTransform(g_loadX + LOAD_W * 0.5f, g_loadY + LOAD_H * 0.5f);
    g.RotateTransform(g_loadRot * 57.3f);
    g.TranslateTransform(-(g_loadX + LOAD_W * 0.5f), -(g_loadY + LOAD_H * 0.5f));

    // label flies with the bar
    SolidBrush lc(Color((BYTE)(alpha * 255), 220, 220, 230));
    StringFormat sf;
    sf.SetAlignment(StringAlignmentCenter);
    RectF lr(g_loadX - 40, g_loadY - 34, LOAD_W + 80, 22);
    g.DrawString(L"Загрузка…", -1, g_fontSub, lr, &sf, &lc);

    // track
    GraphicsPath* tr = CreateRoundRect(g_loadX, g_loadY, LOAD_W, LOAD_H, LOAD_H / 2);
    SolidBrush tb(Color((BYTE)(alpha * 255), 45, 45, 52));
    g.FillPath(&tb, tr);
    delete tr;

    // fill
    float fw = (LOAD_W - 8) * g_loadProgress;
    if (fw > 0)
    {
        GraphicsPath* fl = CreateRoundRect(g_loadX + 4, g_loadY + 4, fw, LOAD_H - 8, (LOAD_H - 8) / 2);
        SolidBrush fb(Color((BYTE)(alpha * 255), 59, 130, 246));
        g.FillPath(&fb, fl);
        delete fl;
    }
    g.Restore(st);
}

void DrawHammer(Graphics& g)
{
    if (!g_hammerActive) return;
    GraphicsState st = g.Save();
    g.TranslateTransform(g_hammerX, g_hammerY);
    g.RotateTransform(g_hammerAng * 57.3f);
    // handle
    Pen hp(Color(255, 140, 105, 60), 7.0f);
    g.DrawLine(&hp, 0, 6, 0, 40);
    // head
    SolidBrush hb(Color(255, 168, 170, 180));
    g.FillRectangle(&hb, -16, 34, 32, 14);
    SolidBrush hhi(Color(255, 205, 207, 215));
    g.FillRectangle(&hhi, -16, 34, 32, 4);
    g.Restore(st);
}

static void DrawNail(Graphics& g, float x, float y, float s)
{
    if (s <= 0.01f) return;
    float w = 8.0f * s, h = 6.0f * s;
    SolidBrush nb(Color(255, 168, 170, 178));
    g.FillRectangle(&nb, x, y, w, h);
    SolidBrush nh(Color(230, 220, 222, 230));
    g.FillRectangle(&nh, x + 1, y + 1, w - 2, 1.5f);
}

void DrawAssembledUI(Graphics& g, int W, int H)
{
    const int margin = 26;

    // accent bar
    WobblePart& ac = g_parts[PART_ACCENT];
    if (ac.visible)
    {
        float x = ac.tx + ac.ox, y = ac.ty + ac.oy;
        GraphicsState st = g.Save();
        g.TranslateTransform(x + ac.w * 0.5f, y + ac.h * 0.5f);
        g.RotateTransform(ac.rot * 57.3f);
        g.TranslateTransform(-(x + ac.w * 0.5f), -(y + ac.h * 0.5f));
        SolidBrush accent(kAccent);
        GraphicsPath* bar = CreateRoundRect(x, y, ac.w, ac.h, 2);
        g.FillPath(&accent, bar);
        delete bar;
        g.Restore(st);
    }

    // title (springs in with overshoot)
    SolidBrush title(kTextMain);
    g.DrawString(L"Фото в чёрно-белое", -1, g_fontTitle, PointF(g_titleX, g_titleY), &title);

    // subtitle
    WobblePart& sb = g_parts[PART_SUB];
    if (sb.visible)
    {
        float x = sb.tx + sb.ox, y = sb.ty + sb.oy;
        GraphicsState st = g.Save();
        g.TranslateTransform(x + sb.w * 0.5f, y + sb.h * 0.5f);
        g.RotateTransform(sb.rot * 57.3f);
        g.TranslateTransform(-(x + sb.w * 0.5f), -(y + sb.h * 0.5f));
        SolidBrush sub(kTextDim);
        g.DrawString(L"Перетащите изображение или выберите файл", -1, g_fontSub, PointF(x, y), &sub);
        g.Restore(st);
    }

    // buttons (keep hit-test rects in sync with wobble)
    for (int i = 0; i < 3; i++)
    {
        WobblePart& p = g_parts[PART_BTN0 + i];
        if (!p.visible) continue;
        float x = p.tx + p.ox, y = p.ty + p.oy;
        g_btns[i].r = { (LONG)x, (LONG)y, (LONG)(x + p.w), (LONG)(y + p.h) };
        GraphicsState st = g.Save();
        g.TranslateTransform(x + p.w * 0.5f, y + p.h * 0.5f);
        g.RotateTransform(p.rot * 57.3f);
        g.TranslateTransform(-(x + p.w * 0.5f), -(y + p.h * 0.5f));
        DrawButton(g, g_btns[i]);
        if (p.hasNails && p.landed)
        {
            DrawNail(g, x + 3, y + 3, p.nailScale);
            DrawNail(g, x + p.w - 11, y + 3, p.nailScale);
        }
        g.Restore(st);
    }

    // panels
    for (int i = 0; i < 2; i++)
    {
        WobblePart& p = g_parts[PART_PANEL0 + i];
        if (!p.visible) continue;
        float x = p.tx + p.ox, y = p.ty + p.oy;
        GraphicsState st = g.Save();
        g.TranslateTransform(x + p.w * 0.5f, y + p.h * 0.5f);
        g.RotateTransform(p.rot * 57.3f);
        g.TranslateTransform(-(x + p.w * 0.5f), -(y + p.h * 0.5f));
        DrawPanel(g, RectF(x, y, p.w, p.h), i);
        if (p.hasNails && p.landed)
        {
            float n = 6.0f;
            DrawNail(g, x + 4, y + 4, p.nailScale);
            DrawNail(g, x + p.w - 12, y + 4, p.nailScale);
            DrawNail(g, x + 4, y + p.h - n, p.nailScale);
            DrawNail(g, x + p.w - 12, y + p.h - n, p.nailScale);
        }
        g.Restore(st);
    }
}

void DrawIntroOverlay(Graphics& g, int W, int H)
{
    if (g_introStage == INTRO_DONE) return;
    float t = g_introT;

    // overlay fades out as parts assemble
    float alpha = 1.0f;
    if (t >= 4.0f) alpha = 1.0f - (t - 4.0f) / 0.7f;
    if (alpha < 0) alpha = 0;
    if (alpha <= 0.01f && g_introStage != INTRO_DONE) return;

    // dark backdrop
    SolidBrush bg(Color((BYTE)(alpha * 250), 22, 22, 28));
    g.FillRectangle(&bg, 0, 0, W, H);

    float cx = W * 0.5f, cy = H * 0.5f;

    if (g_introStage == INTRO_BOOT)
    {
        SolidBrush nm(Color((BYTE)(alpha * 230), 240, 240, 245));
        StringFormat sf;
        sf.SetAlignment(StringAlignmentCenter);
        RectF tr(cx - 180, cy - 90, 360, 26);
        g.DrawString(L"Фото в чёрно-белое", -1, g_fontTitle, tr, &sf, &nm);
        DrawSpinner(g, cx, cy + 20, alpha, t);
    }
    else if (g_introStage == INTRO_LOAD)
    {
        DrawSpinner(g, cx, cy - 40, alpha, t);
        DrawLoadingBar(g, alpha);
    }
    else if (g_introStage == INTRO_RIP)
    {
        DrawLoadingBar(g, alpha);
        float cxp = g_loadX + LOAD_W, cyp = g_loadY;
        DrawCrowbar(g, cxp, cyp, g_crowAng, alpha);
    }
    else // INTRO_ASSEMBLE - bar still flying, overlay fades
    {
        DrawLoadingBar(g, alpha);
    }
}
