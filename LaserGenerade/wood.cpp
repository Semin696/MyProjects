#include "app.h"

// ---- Laser engraving background ----
static const wchar_t kEngravedText[] = L"ДРЕВЕСИНА ЛАЗЕР";
static const int kEngraveFontSize = 44;
static const float kEngraveTop = 152.0f;

static unsigned int g_rngState = 0xC0FFEEu;

float g_animTime = 0.0f;

float Rand01()
{
    g_rngState = g_rngState * 1664525u + 1013904223u;
    return (float)(g_rngState >> 8) * (1.0f / 16777216.0f);
}

static Bitmap* g_wood = nullptr;
static int g_woodW = 0, g_woodH = 0;

static Bitmap* g_textChar = nullptr;
static Bitmap* g_textScorch = nullptr;
static float g_textW = 0.0f, g_textH = 0.0f;

static Bitmap* GenerateWood(int W, int H)
{
    Bitmap* bmp = new Bitmap(W, H, PixelFormat32bppARGB);
    Graphics g(bmp);
    g.SetSmoothingMode(SmoothingModeAntiAlias);
    g.Clear(Color(255, 66, 44, 26));

    LinearGradientBrush base(Rect(0, 0, W, H),
        Color(255, 108, 74, 44), Color(255, 56, 37, 21), LinearGradientModeVertical);
    g.FillRectangle(&base, 0, 0, W, H);

    // horizontal wavy grain
    int grain = (int)(H * 0.9f);
    for (int i = 0; i < grain; i++)
    {
        float y0 = Rand01() * H;
        float amp = 0.5f + Rand01() * 3.5f;
        float freq = 0.008f + Rand01() * 0.03f;
        float phase = Rand01() * 6.2831f;
        bool light = Rand01() < 0.35f;
        BYTE a = (BYTE)(16 + Rand01() * 30);
        Color c;
        if (light)
            c = Color(a, (BYTE)(120 + Rand01() * 40), (BYTE)(86 + Rand01() * 30), (BYTE)(52 + Rand01() * 20));
        else
            c = Color(a, (BYTE)(50 + Rand01() * 40), (BYTE)(34 + Rand01() * 26), (BYTE)(18 + Rand01() * 14));
        Pen p(c, 1.0f + Rand01() * 2.0f);
        const int seg = 48;
        PointF pts[seg + 1];
        for (int k = 0; k <= seg; k++)
        {
            float x = W * (float)k / (float)seg;
            float y = y0 + sinf(x * freq + phase) * amp + (Rand01() - 0.5f) * 1.4f;
            pts[k] = PointF(x, y);
        }
        g.DrawLines(&p, pts, seg + 1);
    }

    // knots
    int knots = 3 + (int)(Rand01() * 3);
    for (int i = 0; i < knots; i++)
    {
        float kx = 50 + Rand01() * (W - 100);
        float ky = 60 + Rand01() * (H - 120);
        float kr = 4 + Rand01() * 9;
        for (int ring = 0; ring < 5; ring++)
        {
            float rr = kr + ring * (2.0f + Rand01() * 3.0f);
            Pen pk(Color((BYTE)(28 + Rand01() * 22), 55, 37, 20), 1.2f);
            g.DrawEllipse(&pk, kx - rr, ky - rr, rr * 2, rr * 2);
        }
        SolidBrush kb(Color(220, 38, 25, 14));
        g.FillEllipse(&kb, RectF(kx - 2.5f, ky - 2.5f, 5, 5));
    }

    // plank seams
    int seams = 1 + (int)(W / 550);
    for (int i = 0; i < seams; i++)
    {
        float sx = (i + 1) * W / (seams + 1) + (Rand01() - 0.5f) * 70;
        Pen ps(Color(90, 0, 0, 0), 1.5f);
        g.DrawLine(&ps, sx, 0.0f, sx, (float)H);
        Pen ph(Color(45, 150, 112, 72), 1.0f);
        g.DrawLine(&ph, sx + 2.0f, 0.0f, sx + 2.0f, (float)H);
    }

    // subtle vignette
    for (int i = 0; i < H; i += 3)
    {
        float v = (float)i / (float)H;
        BYTE a = (BYTE)(v * 60);
        SolidBrush vb(Color(a, 0, 0, 0));
        g.FillRectangle(&vb, 0, i, W, 3);
    }
    return bmp;
}

void EnsureWood(int W, int H)
{
    if (g_wood && g_woodW == W && g_woodH == H) return;
    delete g_wood;
    g_wood = GenerateWood(W, H);
    g_woodW = W;
    g_woodH = H;
}

void CreateTextBitmaps()
{
    Font f(L"Segoe UI", (REAL)kEngraveFontSize, FontStyleBold, UnitPixel);

    Bitmap measure(1, 1, PixelFormat32bppARGB);
    Graphics mg(&measure);
    RectF m;
    mg.MeasureString(kEngravedText, -1, &f, PointF(0, 0), &m);
    g_textW = m.Width + 10.0f;
    g_textH = m.Height + 6.0f;
    int tw = (int)g_textW + 4;
    int th = (int)g_textH + 4;

    // charred letters (dark core + scorch halo)
    g_textChar = new Bitmap(tw, th, PixelFormat32bppARGB);
    {
        Graphics g(g_textChar);
        g.SetTextRenderingHint(TextRenderingHintAntiAlias);
        SolidBrush scorch(Color(150, 158, 96, 48));
        g.DrawString(kEngravedText, -1, &f, PointF(3, 3), &scorch);
        SolidBrush halo(Color(130, 100, 60, 30));
        g.DrawString(kEngravedText, -1, &f, PointF(2, 2), &halo);
        SolidBrush core(Color(255, 32, 18, 9));
        g.DrawString(kEngravedText, -1, &f, PointF(2, 2), &core);
    }

    // scorched (orange, for the zone right behind the beam)
    g_textScorch = new Bitmap(tw, th, PixelFormat32bppARGB);
    {
        Graphics g(g_textScorch);
        g.SetTextRenderingHint(TextRenderingHintAntiAlias);
        SolidBrush s(Color(220, 176, 96, 44));
        g.DrawString(kEngravedText, -1, &f, PointF(3, 3), &s);
        SolidBrush s2(Color(255, 120, 62, 26));
        g.DrawString(kEngravedText, -1, &f, PointF(2, 2), &s2);
    }
}

void DeleteTextBitmaps()
{
    delete g_textChar;  g_textChar = nullptr;
    delete g_textScorch; g_textScorch = nullptr;
    delete g_wood;      g_wood = nullptr;
    g_woodW = g_woodH = 0;
}

void RenderBackdrop(Graphics& g, int W, int H)
{
    if (W <= 0 || H <= 0) return;
    EnsureWood(W, H);
    g.DrawImage(g_wood, 0, 0, W, H);

    float t = g_animTime;
    float textLeft = ((float)W - g_textW) * 0.5f;
    float endX = textLeft + g_textW;

    const float scanDur = 3.8f;
    const float holdDur = 1.2f;
    float cycle = scanDur + holdDur;
    float tt = fmodf(t, cycle);
    float scan = tt < scanDur ? tt / scanDur : 1.0f;
    bool beamOn = tt < scanDur;
    float beamX = textLeft + scan * (endX - textLeft) + sinf(t * 3.0f) * 1.5f;

    // engrave letters left of the beam
    if (beamX > textLeft)
    {
        GraphicsState st = g.Save();

        RectF charClip(textLeft, kEngraveTop - 2, beamX - textLeft, g_textH + 4);
        g.SetClip(charClip);
        g.DrawImage(g_textChar, textLeft, kEngraveTop - 2, g_textW, g_textH + 4);

        RectF scorchClip(beamX - 36, kEngraveTop - 2, 36, g_textH + 4);
        g.SetClip(scorchClip);
        g.DrawImage(g_textScorch, textLeft, kEngraveTop - 2, g_textW, g_textH + 4);

        g.Restore(st);
    }

    // laser beam
    if (beamOn)
    {
        float flick = 0.72f + 0.28f * sinf(t * 40.0f) + 0.12f * sinf(t * 63.0f);
        if (flick < 0.35f) flick = 0.35f;

        // soft glow column
        SolidBrush glow(Color((BYTE)(70 * flick), 255, 120, 40));
        g.FillRectangle(&glow, RectF(beamX - 11, kEngraveTop - 26, 22, g_textH + 52));

        // beam core
        Pen corePen(Color((BYTE)(235 * flick), 255, 255, 230), 2.0f);
        g.DrawLine(&corePen, beamX, kEngraveTop - 22, beamX, kEngraveTop + g_textH + 26);

        // bright head
        float hy = kEngraveTop + g_textH * 0.5f;
        SolidBrush head(Color((BYTE)(200 * flick), 255, 240, 200));
        g.FillEllipse(&head, RectF(beamX - 5, hy - 5, 10, 10));
        SolidBrush hot(Color(255, 255, 255, 255));
        g.FillEllipse(&hot, RectF(beamX - 1.5f, hy - 1.5f, 3, 3));
    }
}
