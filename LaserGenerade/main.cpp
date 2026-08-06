#include <windows.h>
#include <windowsx.h>
#include <gdiplus.h>
#include <commdlg.h>
#include <ole2.h>
#include <dwmapi.h>
#include <string>
#include <vector>
#include <cmath>

using namespace Gdiplus;

static GdiplusStartupInput g_gdiplusStartupInput;
static ULONG_PTR g_gdiplusToken;

static Bitmap* g_original = nullptr;
static Bitmap* g_gray = nullptr;
static std::wstring g_originalPath;
static std::wstring g_resultPath;
static std::wstring g_status;
static bool g_statusOk = false;

static bool g_dragging = false;
static bool g_tracking = false;

static const wchar_t* kWindowClass = L"BwPhotoApp";
static HWND g_hwnd = nullptr;

// ---- Buttons (fully custom-drawn) ----
enum { kBtnOpen = 1, kBtnSaveAs = 2, kBtnClear = 3 };

struct Btn
{
    RECT r;
    int id;
    bool enabled;
    bool hover;
    bool pressed;
    const wchar_t* label;
};

static Btn g_btns[3] = {
    { {}, kBtnOpen,   true,  false, false, L"Открыть файл" },
    { {}, kBtnSaveAs, false, false, false, L"Сохранить как…" },
    { {}, kBtnClear,  false, false, false, L"Сбросить" },
};

static Btn* GetBtn(int id)
{
    for (int i = 0; i < 3; i++)
        if (g_btns[i].id == id) return &g_btns[i];
    return nullptr;
}

static int HitTestBtn(int x, int y)
{
    for (int i = 0; i < 3; i++)
    {
        RECT r = g_btns[i].r;
        r.right++; r.bottom++;
        if (x >= r.left && x < r.right && y >= r.top && y < r.bottom)
            return g_btns[i].id;
    }
    return 0;
}

// ---- Fonts ----
static Font* g_fontTitle = nullptr;
static Font* g_fontSub = nullptr;
static Font* g_fontBtn = nullptr;
static Font* g_fontPanelLabel = nullptr;
static Font* g_fontHint = nullptr;
static Font* g_fontStatus = nullptr;
static Font* g_fontOverlay = nullptr;

// ---- Colors ----
static const Color kBgTop(255, 30, 30, 33);
static const Color kBgBottom(255, 23, 23, 26);
static const Color kPanelFill(255, 44, 44, 49);
static const Color kPanelBorder(255, 63, 63, 70);
static const Color kAccent(255, 59, 130, 246);
static const Color kAccentHover(255, 82, 148, 250);
static const Color kAccentPressed(255, 37, 99, 235);
static const Color kTextMain(255, 240, 240, 245);
static const Color kTextDim(255, 158, 158, 168);
static const Color kBtnSecondaryFill(255, 45, 45, 50);
static const Color kBtnSecondaryHover(255, 56, 56, 62);
static const Color kBtnSecondaryPressed(255, 36, 36, 41);
static const Color kBtnBorder(255, 72, 72, 80);
static const Color kDisabledText(255, 115, 115, 125);
static const Color kDisabledFill(255, 36, 36, 40);
static const Color kDragFill(140, 59, 130, 246);
static const Color kDragBorder(255, 96, 160, 255);
static const Color kSuccess(255, 52, 211, 153);
static const Color kError(255, 248, 113, 113);

// ---- Laser engraving background ----
static const wchar_t kEngravedText[] = L"ДРЕВЕСИНА ЛАЗЕР";
static const int kEngraveFontSize = 44;
static const float kEngraveTop = 152.0f;

static unsigned int g_rngState = 0xC0FFEEu;

static float Rand01()
{
    g_rngState = g_rngState * 1664525u + 1013904223u;
    return (float)(g_rngState >> 8) * (1.0f / 16777216.0f);
}

static Bitmap* g_wood = nullptr;
static int g_woodW = 0, g_woodH = 0;

static Bitmap* g_textChar = nullptr;
static Bitmap* g_textScorch = nullptr;
static float g_textW = 0.0f, g_textH = 0.0f;
static float g_animTime = 0.0f;

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

static void EnsureWood(int W, int H)
{
    if (g_wood && g_woodW == W && g_woodH == H) return;
    delete g_wood;
    g_wood = GenerateWood(W, H);
    g_woodW = W;
    g_woodH = H;
}

static void CreateTextBitmaps()
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

static void DeleteTextBitmaps()
{
    delete g_textChar;  g_textChar = nullptr;
    delete g_textScorch; g_textScorch = nullptr;
    delete g_wood;      g_wood = nullptr;
    g_woodW = g_woodH = 0;
}

static void RenderBackdrop(Graphics& g, int W, int H)
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


static GraphicsPath* CreateRoundRect(float x, float y, float w, float h, float r)
{
    GraphicsPath* p = new GraphicsPath();
    float d = 2.0f * r;
    if (d > w) d = w;
    if (d > h) d = h;
    r = d / 2.0f;
    p->AddArc(x, y, d, d, 180, 90);
    p->AddArc(x + w - d, y, d, d, 270, 90);
    p->AddArc(x + w - d, y + h - d, d, d, 0, 90);
    p->AddArc(x, y + h - d, d, d, 90, 90);
    p->CloseFigure();
    return p;
}

static int GetEncoderClsid(const WCHAR* mime, CLSID* pClsid)
{
    UINT num = 0, size = 0;
    GetImageEncodersSize(&num, &size);
    if (size == 0) return -1;
    ImageCodecInfo* info = (ImageCodecInfo*)malloc(size);
    if (!info) return -1;
    GetImageEncoders(num, size, info);
    for (UINT i = 0; i < num; i++)
    {
        if (wcscmp(info[i].MimeType, mime) == 0)
        {
            *pClsid = info[i].Clsid;
            free(info);
            return (int)i;
        }
    }
    free(info);
    return -1;
}

static Bitmap* MakeGrayscale(Bitmap* src)
{
    if (!src) return nullptr;
    UINT w = src->GetWidth();
    UINT h = src->GetHeight();
    Bitmap* dst = new Bitmap(w, h, PixelFormat32bppARGB);
    Graphics g(dst);
    ColorMatrix cm;
    memset(&cm, 0, sizeof(cm));
    cm.m[0][0] = 0.299f; cm.m[0][1] = 0.299f; cm.m[0][2] = 0.299f;
    cm.m[1][0] = 0.587f; cm.m[1][1] = 0.587f; cm.m[1][2] = 0.587f;
    cm.m[2][0] = 0.114f; cm.m[2][1] = 0.114f; cm.m[2][2] = 0.114f;
    cm.m[3][3] = 1.0f;
    cm.m[4][4] = 1.0f;
    ImageAttributes attrs;
    attrs.SetColorMatrix(&cm);
    g.DrawImage(src, Rect(0, 0, (int)w, (int)h), 0, 0, (int)w, (int)h, UnitPixel, &attrs);
    return dst;
}

static std::wstring MakeOutputPath(const std::wstring& srcPath)
{
    size_t slash = srcPath.find_last_of(L"\\/");
    size_t dot = srcPath.find_last_of(L'.');
    std::wstring dir = (slash == std::wstring::npos) ? L"" : srcPath.substr(0, slash + 1);
    std::wstring name;
    std::wstring ext = L".png";
    if (dot != std::wstring::npos && dot > slash)
    {
        name = srcPath.substr(slash + 1, dot - slash - 1);
        ext = srcPath.substr(dot);
    }
    else
    {
        name = srcPath.substr(slash + 1);
    }
    std::wstring lower = ext;
    for (auto& c : lower) c = towlower((wint_t)c);
    std::wstring outExt = L".png";
    if (lower == L".jpg" || lower == L".jpeg" || lower == L".bmp" ||
        lower == L".gif" || lower == L".tif" || lower == L".tiff" || lower == L".png")
        outExt = lower;
    return dir + name + L"_bw" + outExt;
}

static const WCHAR* MimeForPath(const std::wstring& path)
{
    std::wstring lower = path;
    for (auto& c : lower) c = towlower((wint_t)c);
    if (lower.find(L".jpg") != std::wstring::npos || lower.find(L".jpeg") != std::wstring::npos)
        return L"image/jpeg";
    if (lower.find(L".bmp") != std::wstring::npos) return L"image/bmp";
    if (lower.find(L".gif") != std::wstring::npos) return L"image/gif";
    if (lower.find(L".tif") != std::wstring::npos || lower.find(L".tiff") != std::wstring::npos)
        return L"image/tiff";
    return L"image/png";
}

static bool SaveBitmap(Bitmap* bmp, const std::wstring& path)
{
    CLSID clsid;
    if (GetEncoderClsid(MimeForPath(path), &clsid) == -1) return false;
    return bmp->Save(path.c_str(), &clsid, NULL) == Ok;
}

static void UpdateButtonStates()
{
    bool has = (g_gray != nullptr);
    GetBtn(kBtnSaveAs)->enabled = has;
    GetBtn(kBtnClear)->enabled = has;
}

static bool ProcessFile(const std::wstring& path)
{
    Bitmap* src = Bitmap::FromFile(path.c_str());
    if (!src || src->GetLastStatus() != Ok)
    {
        if (src) delete src;
        g_status = L"Не удалось открыть: " + path;
        g_statusOk = false;
        InvalidateRect(g_hwnd, NULL, TRUE);
        return false;
    }
    Bitmap* gray = MakeGrayscale(src);

    delete g_original;
    delete g_gray;
    g_original = src;
    g_gray = gray;
    g_originalPath = path;
    g_resultPath = MakeOutputPath(path);

    if (SaveBitmap(gray, g_resultPath))
    {
        g_status = L"Готово — " + g_resultPath;
        g_statusOk = true;
    }
    else
    {
        g_status = L"Ошибка сохранения: " + g_resultPath;
        g_statusOk = false;
    }

    UpdateButtonStates();
    InvalidateRect(g_hwnd, NULL, TRUE);
    return true;
}

static void DrawImageInPanel(Graphics& g, Bitmap* img, const RectF& box)
{
    if (!img) return;
    float iw = (float)img->GetWidth();
    float ih = (float)img->GetHeight();
    if (iw <= 0 || ih <= 0) return;

    GraphicsPath* clip = CreateRoundRect(box.X, box.Y, box.Width, box.Height, 8);
    g.SetClip(clip);
    delete clip;

    float scale = std::min(box.Width / iw, box.Height / ih);
    float w = iw * scale;
    float h = ih * scale;
    float x = box.X + (box.Width - w) / 2.0f;
    float y = box.Y + (box.Height - h) / 2.0f;
    g.SetInterpolationMode(InterpolationModeHighQualityBicubic);
    g.SetPixelOffsetMode(PixelOffsetModeHalf);
    g.DrawImage(img, RectF(x, y, w, h), 0, 0, iw, ih, UnitPixel);
    g.ResetClip();
}

static void DrawButton(Graphics& g, const Btn& b)
{
    float x = (float)b.r.left, y = (float)b.r.top;
    float w = (float)(b.r.right - b.r.left), h = (float)(b.r.bottom - b.r.top);

    GraphicsPath* path = CreateRoundRect(x, y, w, h, 10);

    if (b.id == kBtnOpen)
    {
        Color c = !b.enabled ? Color(255, 60, 60, 66)
                 : b.pressed ? kAccentPressed
                 : b.hover ? kAccentHover : kAccent;
        SolidBrush fill(c);
        g.FillPath(&fill, path);
    }
    else
    {
        Color fill = !b.enabled ? kDisabledFill
                    : b.pressed ? kBtnSecondaryPressed
                    : b.hover ? kBtnSecondaryHover : kBtnSecondaryFill;
        SolidBrush f(fill);
        g.FillPath(&f, path);
        if (b.enabled)
        {
            Pen pen(b.hover ? Color(255, 96, 96, 106) : kBtnBorder, 1.0f);
            g.DrawPath(&pen, path);
        }
    }
    delete path;

    Color textCol = b.enabled ? kTextMain : kDisabledText;
    SolidBrush tb(textCol);
    StringFormat sf;
    sf.SetAlignment(StringAlignmentCenter);
    sf.SetLineAlignment(StringAlignmentCenter);
    RectF tr(x, y, w, h);
    g.DrawString(b.label, -1, g_fontBtn, tr, &sf, &tb);
}

static void Paint(Graphics& g, int W, int H)
{
    const int margin = 26;

    RenderBackdrop(g, W, H);

    // ---- Header ----
    SolidBrush accent(kAccent);
    GraphicsPath* accentBar = CreateRoundRect((REAL)margin, 24, 4, 26, 2);
    g.FillPath(&accent, accentBar);
    delete accentBar;

    SolidBrush title(kTextMain);
    g.DrawString(L"Фото в чёрно-белое", -1, g_fontTitle, PointF((REAL)(margin + 14), 22), &title);

    SolidBrush sub(kTextDim);
    g.DrawString(L"Перетащите изображение или выберите файл", -1, g_fontSub,
                 PointF((REAL)(margin + 14), 52), &sub);

    // ---- Buttons ----
    for (int i = 0; i < 3; i++)
        DrawButton(g, g_btns[i]);

    // ---- Content layout ----
    int contentTop = 244;
    int contentBottom = H - 44;
    int contentH = contentBottom - contentTop;
    int panelW = (W - margin * 2 - 16) / 2;
    int panelH = contentH;

    if (panelW > 0 && panelH > 0)
    {
        RectF boxes[2] = {
            RectF((REAL)margin, (REAL)contentTop, (REAL)panelW, (REAL)panelH),
            RectF((REAL)(margin + panelW + 16), (REAL)contentTop, (REAL)panelW, (REAL)panelH)
        };
        const wchar_t* labels[2] = { L"Оригинал", L"Чёрно-белое" };

        for (int i = 0; i < 2; i++)
        {
            RectF box = boxes[i];

            GraphicsPath* path = CreateRoundRect(box.X, box.Y, box.Width, box.Height, 12);
            SolidBrush panelFill(kPanelFill);
            g.FillPath(&panelFill, path);
            Pen panelPen(kPanelBorder, 1.0f);
            g.DrawPath(&panelPen, path);
            delete path;

            SolidBrush labelCol(kTextMain);
            StringFormat lf;
            lf.SetAlignment(StringAlignmentCenter);
            RectF labelBox(box.X, box.Y + 12, box.Width, 24);
            g.DrawString(labels[i], -1, g_fontPanelLabel, labelBox, &lf, &labelCol);

            RectF imgBox(box.X + 12, box.Y + 46, box.Width - 24, box.Height - 58);
            Bitmap* img = (i == 0) ? g_original : g_gray;
            if (img)
            {
                DrawImageInPanel(g, img, imgBox);
            }
            else
            {
                SolidBrush hint(kTextDim);
                StringFormat hf;
                hf.SetAlignment(StringAlignmentCenter);
                hf.SetLineAlignment(StringAlignmentCenter);
                RectF hintBox(box.X + 16, box.Y + 46, box.Width - 32, box.Height - 58);
                g.DrawString(i == 0 ? L"Здесь появится\nизображение"
                                    : L"Здесь появится\nчёрно-белый результат",
                             -1, g_fontHint, hintBox, &hf, &hint);
            }
        }
    }

    // ---- Status ----
    if (!g_status.empty())
    {
        SolidBrush statusCol(g_statusOk ? kSuccess : kError);
        StringFormat sf;
        sf.SetTrimming(StringTrimmingEllipsisCharacter);
        RectF statusBox((REAL)margin, (REAL)(H - 34), (REAL)(W - margin * 2), 24);
        g.DrawString(g_status.c_str(), -1, g_fontStatus, statusBox, &sf, &statusCol);
    }

    // ---- Drag overlay ----
    if (g_dragging)
    {
        RectF over((REAL)(margin - 2), (REAL)(contentTop - 2),
                   (REAL)(W - margin * 2 + 4), (REAL)(contentH + 4));
        GraphicsPath* path = CreateRoundRect(over.X, over.Y, over.Width, over.Height, 14);
        SolidBrush fill(kDragFill);
        g.FillPath(&fill, path);
        Pen pen(kDragBorder, 2.0f);
        g.DrawPath(&pen, path);
        delete path;

        SolidBrush overlayCol(kTextMain);
        StringFormat of;
        of.SetAlignment(StringAlignmentCenter);
        of.SetLineAlignment(StringAlignmentCenter);
        RectF textBox(over.X, over.Y, over.Width, over.Height);
        g.DrawString(L"Отпустите, чтобы обработать", -1, g_fontOverlay, textBox, &of, &overlayCol);
    }
}

static void OpenFileDialog()
{
    wchar_t file[MAX_PATH] = L"";
    OPENFILENAMEW ofn = {};
    ofn.lStructSize = sizeof(ofn);
    ofn.hwndOwner = g_hwnd;
    ofn.lpstrFilter = L"Изображения\0*.jpg;*.jpeg;*.png;*.bmp;*.gif;*.tif;*.tiff\0Все файлы\0*.*\0";
    ofn.lpstrFile = file;
    ofn.nMaxFile = MAX_PATH;
    ofn.Flags = OFN_FILEMUSTEXIST | OFN_PATHMUSTEXIST;
    if (GetOpenFileNameW(&ofn))
        ProcessFile(ofn.lpstrFile);
}

static void SaveAsDialog()
{
    if (!g_gray || g_resultPath.empty()) return;
    wchar_t file[MAX_PATH] = L"";
    wcscpy_s(file, g_resultPath.c_str());
    OPENFILENAMEW ofn = {};
    ofn.lStructSize = sizeof(ofn);
    ofn.hwndOwner = g_hwnd;
    ofn.lpstrFilter = L"PNG (*.png)\0*.png\0JPEG (*.jpg)\0*.jpg\0BMP (*.bmp)\0*.bmp\0GIF (*.gif)\0*.gif\0TIFF (*.tiff)\0*.tiff\0";
    ofn.lpstrFile = file;
    ofn.nMaxFile = MAX_PATH;
    ofn.lpstrDefExt = L"png";
    ofn.Flags = OFN_OVERWRITEPROMPT;
    if (GetSaveFileNameW(&ofn))
    {
        if (SaveBitmap(g_gray, ofn.lpstrFile))
        {
            g_status = L"Сохранено — " + std::wstring(ofn.lpstrFile);
            g_statusOk = true;
        }
        else
        {
            g_status = L"Ошибка сохранения";
            g_statusOk = false;
        }
        InvalidateRect(g_hwnd, NULL, TRUE);
    }
}

static void ClearAll()
{
    delete g_original; g_original = nullptr;
    delete g_gray; g_gray = nullptr;
    g_originalPath.clear();
    g_resultPath.clear();
    g_status.clear();
    UpdateButtonStates();
    InvalidateRect(g_hwnd, NULL, TRUE);
}

static void DoAction(int id)
{
    switch (id)
    {
    case kBtnOpen: OpenFileDialog(); break;
    case kBtnSaveAs: SaveAsDialog(); break;
    case kBtnClear: ClearAll(); break;
    }
}

static void ResizeButtons()
{
    RECT rc;
    GetClientRect(g_hwnd, &rc);
    int x = 26, y = 90;
    int wBtn = 150, hBtn = 38, gap = 12;
    g_btns[0].r = { x, y, x + wBtn, y + hBtn };
    g_btns[1].r = { x + wBtn + gap, y, x + wBtn + gap + 165, y + hBtn };
    g_btns[2].r = { x + wBtn + gap + 165 + gap, y, x + wBtn + gap + 165 + gap + 115, y + hBtn };
}

static void EnableDarkTitleBar(HWND hwnd)
{
    BOOL dark = TRUE;
    if (DwmSetWindowAttribute(hwnd, 20, &dark, sizeof(dark)) != S_OK)
        DwmSetWindowAttribute(hwnd, 19, &dark, sizeof(dark));
}

static void InvalidateBtn(Btn* b)
{
    RECT r = b->r;
    r.right++; r.bottom++;
    InvalidateRect(g_hwnd, &r, FALSE);
}

static void UpdateHover(int x, int y)
{
    int id = HitTestBtn(x, y);
    for (int i = 0; i < 3; i++)
    {
        bool hov = (g_btns[i].id == id);
        if (g_btns[i].hover != hov)
        {
            g_btns[i].hover = hov;
            InvalidateBtn(&g_btns[i]);
        }
    }
}

// ---- IDropTarget ----
class FileDropTarget : public IDropTarget
{
public:
    FileDropTarget(HWND hwnd) : m_hwnd(hwnd), m_ref(1) {}

    STDMETHODIMP QueryInterface(REFIID riid, void** ppv) override
    {
        if (riid == IID_IUnknown || riid == IID_IDropTarget)
        {
            *ppv = this;
            AddRef();
            return S_OK;
        }
        *ppv = nullptr;
        return E_NOINTERFACE;
    }

    STDMETHODIMP_(ULONG) AddRef() override { return ++m_ref; }

    STDMETHODIMP_(ULONG) Release() override
    {
        ULONG r = --m_ref;
        if (r == 0) delete this;
        return r;
    }

    STDMETHODIMP DragEnter(IDataObject* pDataObj, DWORD, POINTL, DWORD* pdwEffect) override
    {
        FORMATETC fm = { CF_HDROP, nullptr, DVASPECT_CONTENT, -1, TYMED_HGLOBAL };
        if (pDataObj && pDataObj->QueryGetData(&fm) == S_OK)
        {
            g_dragging = true;
            InvalidateRect(m_hwnd, nullptr, TRUE);
            *pdwEffect = DROPEFFECT_COPY;
        }
        else
        {
            *pdwEffect = DROPEFFECT_NONE;
        }
        return S_OK;
    }

    STDMETHODIMP DragOver(DWORD, POINTL, DWORD* pdwEffect) override
    {
        *pdwEffect = g_dragging ? DROPEFFECT_COPY : DROPEFFECT_NONE;
        return S_OK;
    }

    STDMETHODIMP DragLeave() override
    {
        g_dragging = false;
        InvalidateRect(m_hwnd, nullptr, TRUE);
        return S_OK;
    }

    STDMETHODIMP Drop(IDataObject* pDataObj, DWORD, POINTL, DWORD* pdwEffect) override
    {
        g_dragging = false;
        InvalidateRect(m_hwnd, nullptr, TRUE);
        FORMATETC fm = { CF_HDROP, nullptr, DVASPECT_CONTENT, -1, TYMED_HGLOBAL };
        STGMEDIUM med = {};
        if (pDataObj && pDataObj->GetData(&fm, &med) == S_OK)
        {
            HDROP drop = (HDROP)GlobalLock(med.hGlobal);
            if (drop)
            {
                UINT count = DragQueryFileW(drop, 0xFFFFFFFF, nullptr, 0);
                for (UINT i = 0; i < count; i++)
                {
                    UINT len = DragQueryFileW(drop, i, nullptr, 0);
                    std::wstring path(len, L'\0');
                    DragQueryFileW(drop, i, &path[0], len + 1);
                    ProcessFile(path);
                }
                GlobalUnlock(med.hGlobal);
            }
            ReleaseStgMedium(&med);
        }
        *pdwEffect = DROPEFFECT_COPY;
        return S_OK;
    }

private:
    HWND m_hwnd;
    ULONG m_ref;
};

static LRESULT CALLBACK WndProc(HWND hwnd, UINT msg, WPARAM wParam, LPARAM lParam)
{
    switch (msg)
    {
    case WM_CREATE:
        EnableDarkTitleBar(hwnd);
        ResizeButtons();
        SetTimer(hwnd, 1, 16, NULL);
        return 0;

    case WM_TIMER:
        if (wParam == 1)
        {
            g_animTime = (float)(GetTickCount64() % 1000000) / 1000.0f;
            InvalidateRect(hwnd, NULL, FALSE);
        }
        return 0;

    case WM_SIZE:
        ResizeButtons();
        InvalidateRect(hwnd, NULL, TRUE);
        return 0;

    case WM_GETMINMAXINFO:
    {
        MINMAXINFO* mmi = (MINMAXINFO*)lParam;
        mmi->ptMinTrackSize.x = 620;
        mmi->ptMinTrackSize.y = 460;
        return 0;
    }

    case WM_MOUSEMOVE:
    {
        UpdateHover(GET_X_LPARAM(lParam), GET_Y_LPARAM(lParam));
        if (!g_tracking)
        {
            TRACKMOUSEEVENT tme = {};
            tme.cbSize = sizeof(tme);
            tme.dwFlags = TME_LEAVE;
            tme.hwndTrack = hwnd;
            TrackMouseEvent(&tme);
            g_tracking = true;
        }
        return 0;
    }

    case WM_MOUSELEAVE:
    {
        g_tracking = false;
        for (int i = 0; i < 3; i++)
        {
            if (g_btns[i].hover)
            {
                g_btns[i].hover = false;
                InvalidateBtn(&g_btns[i]);
            }
        }
        return 0;
    }

    case WM_LBUTTONDOWN:
    {
        SetCapture(hwnd);
        int id = HitTestBtn(GET_X_LPARAM(lParam), GET_Y_LPARAM(lParam));
        Btn* b = GetBtn(id);
        if (b && b->enabled)
        {
            b->pressed = true;
            InvalidateBtn(b);
        }
        return 0;
    }

    case WM_LBUTTONUP:
    {
        int id = HitTestBtn(GET_X_LPARAM(lParam), GET_Y_LPARAM(lParam));
        Btn* b = GetBtn(id);
        for (int i = 0; i < 3; i++)
            if (g_btns[i].pressed)
            {
                g_btns[i].pressed = false;
                InvalidateBtn(&g_btns[i]);
            }
        if (b && b->enabled)
            DoAction(b->id);
        ReleaseCapture();
        return 0;
    }

    case WM_CAPTURECHANGED:
    {
        for (int i = 0; i < 3; i++)
            if (g_btns[i].pressed)
            {
                g_btns[i].pressed = false;
                InvalidateBtn(&g_btns[i]);
            }
        return 0;
    }

    case WM_PAINT:
    {
        PAINTSTRUCT ps;
        HDC hdc = BeginPaint(hwnd, &ps);
        RECT rc;
        GetClientRect(hwnd, &rc);
        int W = rc.right - rc.left;
        int H = rc.bottom - rc.top;

        HDC memdc = CreateCompatibleDC(hdc);
        HBITMAP mbmp = CreateCompatibleBitmap(hdc, W, H);
        HBITMAP oldBmp = (HBITMAP)SelectObject(memdc, mbmp);
        {
            Graphics g(memdc);
            g.SetSmoothingMode(SmoothingModeAntiAlias);
            g.SetTextRenderingHint(TextRenderingHintClearTypeGridFit);
            Paint(g, W, H);
        }
        BitBlt(hdc, 0, 0, W, H, memdc, 0, 0, SRCCOPY);
        SelectObject(memdc, oldBmp);
        DeleteObject(mbmp);
        DeleteDC(memdc);
        EndPaint(hwnd, &ps);
        return 0;
    }

    case WM_ERASEBKGND:
        return 1;

    case WM_DESTROY:
        KillTimer(hwnd, 1);
        delete g_original;
        delete g_gray;
        PostQuitMessage(0);
        return 0;
    }
    return DefWindowProcW(hwnd, msg, wParam, lParam);
}

static void CreateFonts()
{
    g_fontTitle = new Font(L"Segoe UI", 19.0f, FontStyleBold, UnitPixel);
    g_fontSub = new Font(L"Segoe UI", 11.0f, FontStyleRegular, UnitPixel);
    g_fontBtn = new Font(L"Segoe UI", 12.0f, FontStyleRegular, UnitPixel);
    g_fontPanelLabel = new Font(L"Segoe UI", 12.0f, FontStyleBold, UnitPixel);
    g_fontHint = new Font(L"Segoe UI", 11.5f, FontStyleRegular, UnitPixel);
    g_fontStatus = new Font(L"Segoe UI", 11.0f, FontStyleRegular, UnitPixel);
    g_fontOverlay = new Font(L"Segoe UI", 16.0f, FontStyleBold, UnitPixel);
}

static void DeleteFonts()
{
    delete g_fontTitle; delete g_fontSub; delete g_fontBtn;
    delete g_fontPanelLabel; delete g_fontHint;
    delete g_fontStatus; delete g_fontOverlay;
}

int WINAPI wWinMain(HINSTANCE hInstance, HINSTANCE, PWSTR, int nCmdShow)
{
    OleInitialize(NULL);
    GdiplusStartup(&g_gdiplusToken, &g_gdiplusStartupInput, NULL);
    CreateFonts();
    CreateTextBitmaps();

    WNDCLASSEXW wc = {};
    wc.cbSize = sizeof(wc);
    wc.style = CS_HREDRAW | CS_VREDRAW;
    wc.lpfnWndProc = WndProc;
    wc.hInstance = hInstance;
    wc.hIcon = LoadIconW(hInstance, MAKEINTRESOURCEW(100));
    wc.hCursor = LoadCursor(NULL, IDC_ARROW);
    wc.hbrBackground = (HBRUSH)GetStockObject(NULL_BRUSH);
    wc.lpszClassName = kWindowClass;
    RegisterClassExW(&wc);

    g_hwnd = CreateWindowExW(0, kWindowClass, L"Фото в чёрно-белое",
        WS_OVERLAPPEDWINDOW, CW_USEDEFAULT, CW_USEDEFAULT, 1000, 700,
        NULL, NULL, hInstance, NULL);

    FileDropTarget* dropTarget = new FileDropTarget(g_hwnd);
    RegisterDragDrop(g_hwnd, dropTarget);

    ShowWindow(g_hwnd, nCmdShow);
    UpdateWindow(g_hwnd);

    MSG msg;
    while (GetMessageW(&msg, NULL, 0, 0))
    {
        TranslateMessage(&msg);
        DispatchMessageW(&msg);
    }

    RevokeDragDrop(g_hwnd);
    DeleteFonts();
    DeleteTextBitmaps();
    GdiplusShutdown(g_gdiplusToken);
    OleUninitialize();
    return (int)msg.wParam;
}
