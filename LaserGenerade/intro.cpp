#include "app.h"

// Simple one-time intro: the title is revealed letter by letter from the first
// character (typewriter style), then everything stays put.

static ULONGLONG g_startTick = 0;
IntroStage g_introStage = INTRO_DONE;
static float g_introT = 0.0f;
static bool g_titleDone = false;

static const wchar_t* kTitle = L"Фото в чёрно-белое";

bool IsIntroDone() { return g_titleDone; }

void InitIntro()
{
    g_startTick = GetTickCount64();
    g_introT = 0.0f;
    g_titleDone = false;
    g_introStage = INTRO_BOOT;
}

void UpdateIntro(float dt)
{
    g_introT = (float)(GetTickCount64() - g_startTick) / 1000.0f;
    const float dur = 0.9f;          // ~20 chars * 45 ms
    if (g_introT >= dur)
    {
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

    // title (typewriter reveal from the first letter)
    int total = (int)wcslen(kTitle);
    int n = (int)(total * (g_introT / 0.9f));
    if (n > total) n = total;
    if (n < 0) n = 0;

    SolidBrush title(kTextMain);
    float tx = 40.0f, ty = 22.0f;
    if (n > 0)
    {
        std::wstring shown(kTitle, n);
        g.DrawString(shown.c_str(), -1, g_fontTitle, PointF(tx, ty), &title);

        // blinking caret while typing
        if (!g_titleDone && ((int)(g_animTime * 2) % 2 == 0))
        {
            RectF m;
            g.MeasureString(shown.c_str(), -1, g_fontTitle, PointF(0, 0), &m);
            Pen caret(Color(255, 240, 240, 245), 2.0f);
            g.DrawLine(&caret, tx + m.Width + 2, ty + 2, tx + m.Width + 2, ty + m.Height - 2);
        }
    }

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
