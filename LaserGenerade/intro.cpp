#include "app.h"

// Simple one-time title appearance. No boot overlay, no repeated animation.
// Timeline: title "Фото в чёрно-белое" slides up once over ~0.7s, then done.

static ULONGLONG g_startTick = 0;
IntroStage g_introStage = INTRO_DONE;
static float g_introT = 0.0f;
static float g_titleY = 60.0f;
static bool g_titleDone = false;

static float EaseOutCubic(float u) { return 1.0f - powf(1.0f - u, 3); }

bool IsIntroDone() { return g_titleDone; }

void InitIntro()
{
    g_startTick = GetTickCount64();
    g_introT = 0.0f;
    g_titleY = 60.0f;
    g_titleDone = false;
    g_introStage = INTRO_BOOT;
}

void UpdateIntro(float dt)
{
    g_introT = (float)(GetTickCount64() - g_startTick) / 1000.0f;
    const float targetY = 22.0f;
    const float dur = 0.7f;
    if (g_introT < dur)
        g_titleY = targetY + (1.0f - EaseOutCubic(g_introT / dur)) * 44.0f;
    else
    {
        g_titleY = targetY;
        g_titleDone = true;
        g_introStage = INTRO_DONE;
    }
}

void DrawIntroOverlay(Graphics& g, int W, int H) {}

void DrawHammer(Graphics& g) {}

void DrawAssembledUI(Graphics& g, int W, int H)
{
    const int margin = 26;

    // accent bar
    SolidBrush accent(kAccent);
    GraphicsPath* bar = CreateRoundRect((REAL)margin, 24, 4, 26, 2);
    g.FillPath(&accent, bar);
    delete bar;

    // title (appears once)
    SolidBrush title(kTextMain);
    g.DrawString(L"Фото в чёрно-белое", -1, g_fontTitle, PointF(40.0f, g_titleY), &title);

    // subtitle
    SolidBrush sub(kTextDim);
    g.DrawString(L"Перетащите изображение или выберите файл", -1, g_fontSub,
                 PointF((REAL)(margin + 14), 52.0f), &sub);

    // buttons
    for (int i = 0; i < 3; i++)
        DrawButton(g, g_btns[i]);

    // panels
    int contentTop = 244;
    int contentBottom = H - 44;
    int contentH = contentBottom - contentTop;
    int panelW = (W - margin * 2 - 16) / 2;
    if (panelW < 10) panelW = 10;
    if (contentH < 10) contentH = 10;
    DrawPanel(g, RectF((REAL)margin, (REAL)contentTop, (REAL)panelW, (REAL)contentH), 0);
    DrawPanel(g, RectF((REAL)(margin + panelW + 16), (REAL)contentTop, (REAL)panelW, (REAL)contentH), 1);
}
