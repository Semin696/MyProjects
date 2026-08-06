#include <windows.h>
#include <gdiplus.h>
#include <commdlg.h>
#include <shellapi.h>
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

static const wchar_t* kWindowClass = L"BwPhotoApp";

static HWND g_hwnd = nullptr;
static HWND g_btnOpen = nullptr;
static HWND g_btnSaveAs = nullptr;
static HWND g_btnClear = nullptr;
static HFONT g_font = nullptr;

static const COLORREF kBgColor = RGB(37, 37, 38);
static const COLORREF kPanelColor = RGB(51, 51, 54);
static const COLORREF kBorderColor = RGB(92, 92, 94);
static const COLORREF kTextColor = RGB(235, 235, 235);
static const COLORREF kHintColor = RGB(150, 150, 150);

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

static std::wstring GetFileBaseName(const std::wstring& path)
{
    size_t slash = path.find_last_of(L"\\/");
    return (slash == std::wstring::npos) ? path : path.substr(slash + 1);
}

static bool ProcessFile(const std::wstring& path)
{
    Bitmap* src = Bitmap::FromFile(path.c_str());
    if (!src || src->GetLastStatus() != Ok)
    {
        if (src) delete src;
        g_status = L"Не удалось открыть: " + path;
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
        g_status = L"Сохранено: " + g_resultPath;
    else
        g_status = L"Ошибка сохранения: " + g_resultPath;

    InvalidateRect(g_hwnd, NULL, TRUE);
    return true;
}

static void DrawImageScaled(Graphics& g, Bitmap* img, const RectF& box)
{
    if (!img) return;
    float iw = (float)img->GetWidth();
    float ih = (float)img->GetHeight();
    if (iw <= 0 || ih <= 0) return;
    float scale = std::min(box.Width / iw, box.Height / ih);
    float w = iw * scale;
    float h = ih * scale;
    float x = box.X + (box.Width - w) / 2.0f;
    float y = box.Y + (box.Height - h) / 2.0f;
    g.SetInterpolationMode(InterpolationModeHighQualityBicubic);
    g.DrawImage(img, RectF(x, y, w, h), 0, 0, iw, ih, UnitPixel);
}

static void Paint(HWND hwnd, HDC hdc)
{
    RECT rc;
    GetClientRect(hwnd, &rc);
    int w = rc.right - rc.left;
    int h = rc.bottom - rc.top;

    Graphics g(hdc);
    g.SetSmoothingMode(SmoothingModeAntiAlias);
    g.SetTextRenderingHint(TextRenderingHintClearTypeGridFit);
    SolidBrush bg(kBgColor);
    g.FillRectangle(&bg, RectF(0, 0, (REAL)w, (REAL)h));

    const int top = 64;
    const int pad = 12;

    if (!g_original || !g_gray)
    {
        Font hintFont(L"Segoe UI", 18.0f, FontStyleRegular, UnitPixel);
        SolidBrush hint(kHintColor);
        StringFormat sf;
        sf.SetAlignment(StringAlignmentCenter);
        sf.SetLineAlignment(StringAlignmentCenter);
        RectF box(0, (REAL)top, (REAL)w, (REAL)(h - top));
        g.DrawString(L"Перетащите фото сюда\nили нажмите «Открыть файл»",
                     -1, &hintFont, box, &sf, &hint);
    }
    else
    {
        int imgAreaH = h - top - pad * 3;
        int panelW = (w - pad * 3) / 2;
        if (panelW < 100) panelW = 100;
        int panelH = imgAreaH - 24;

        Font labelFont(L"Segoe UI", 13.0f, FontStyleBold, UnitPixel);

        // left panel
        SolidBrush panel(kPanelColor);
        Pen border(kBorderColor, 1.0f);
        RectF leftBox((REAL)pad, (REAL)top + 2, (REAL)panelW, (REAL)panelH);
        g.FillRectangle(&panel, leftBox);
        g.DrawRectangle(&border, leftBox);
        DrawImageScaled(g, g_original, RectF(leftBox.X + 4, leftBox.Y + 4,
                                             leftBox.Width - 8, leftBox.Height - 30));
        SolidBrush txt(kTextColor);
        StringFormat cf;
        cf.SetAlignment(StringAlignmentCenter);
        RectF leftLabel(leftBox.X, leftBox.Y + leftBox.Height - 26, leftBox.Width, 24);
        g.DrawString(L"Оригинал", -1, &labelFont, leftLabel, &cf, &txt);

        // right panel
        RectF rightBox((REAL)(pad * 2 + panelW), (REAL)top + 2, (REAL)panelW, (REAL)panelH);
        g.FillRectangle(&panel, rightBox);
        g.DrawRectangle(&border, rightBox);
        DrawImageScaled(g, g_gray, RectF(rightBox.X + 4, rightBox.Y + 4,
                                         rightBox.Width - 8, rightBox.Height - 30));
        RectF rightLabel(rightBox.X, rightBox.Y + rightBox.Height - 26, rightBox.Width, 24);
        g.DrawString(L"Чёрно-белое", -1, &labelFont, rightLabel, &cf, &txt);

        // status line at bottom
        Font statusFont(L"Segoe UI", 11.0f, FontStyleRegular, UnitPixel);
        SolidBrush statusCol(kHintColor);
        std::wstring text = g_status;
        RectF statusBox((REAL)pad, (REAL)(h - 26), (REAL)(w - pad * 2), 22);
        g.DrawString(text.c_str(), -1, &statusFont, statusBox, &cf, &statusCol);
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
            g_status = L"Сохранено: " + std::wstring(ofn.lpstrFile);
        else
            g_status = L"Ошибка сохранения";
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
    InvalidateRect(g_hwnd, NULL, TRUE);
}

static void CreateButtons(HWND hwnd)
{
    g_btnOpen = CreateWindowW(L"BUTTON", L"Открыть файл",
        WS_CHILD | WS_VISIBLE | BS_PUSHBUTTON, 12, 12, 120, 34, hwnd,
        (HMENU)1, GetModuleHandleW(NULL), NULL);
    g_btnSaveAs = CreateWindowW(L"BUTTON", L"Сохранить как…",
        WS_CHILD | WS_VISIBLE | BS_PUSHBUTTON, 140, 12, 140, 34, hwnd,
        (HMENU)2, GetModuleHandleW(NULL), NULL);
    g_btnClear = CreateWindowW(L"BUTTON", L"Сбросить",
        WS_CHILD | WS_VISIBLE | BS_PUSHBUTTON, 288, 12, 100, 34, hwnd,
        (HMENU)3, GetModuleHandleW(NULL), NULL);
    for (HWND b : { g_btnOpen, g_btnSaveAs, g_btnClear })
        SendMessageW(b, WM_SETFONT, (WPARAM)g_font, TRUE);
}

static LRESULT CALLBACK WndProc(HWND hwnd, UINT msg, WPARAM wParam, LPARAM lParam)
{
    switch (msg)
    {
    case WM_CREATE:
        g_font = (HFONT)GetStockObject(DEFAULT_GUI_FONT);
        CreateButtons(hwnd);
        DragAcceptFiles(hwnd, TRUE);
        return 0;

    case WM_DROPFILES:
    {
        HDROP drop = (HDROP)wParam;
        UINT count = DragQueryFileW(drop, 0xFFFFFFFF, NULL, 0);
        for (UINT i = 0; i < count; i++)
        {
            UINT len = DragQueryFileW(drop, i, NULL, 0);
            std::wstring path(len, L'\0');
            DragQueryFileW(drop, i, &path[0], len + 1);
            ProcessFile(path);
        }
        DragFinish(drop);
        return 0;
    }

    case WM_COMMAND:
        switch (LOWORD(wParam))
        {
        case 1: OpenFileDialog(); break;
        case 2: SaveAsDialog(); break;
        case 3: ClearAll(); break;
        }
        return 0;

    case WM_PAINT:
    {
        PAINTSTRUCT ps;
        HDC hdc = BeginPaint(hwnd, &ps);
        Paint(hwnd, hdc);
        EndPaint(hwnd, &ps);
        return 0;
    }

    case WM_ERASEBKGND:
        return 1;

    case WM_DESTROY:
        delete g_original;
        delete g_gray;
        DragAcceptFiles(hwnd, FALSE);
        PostQuitMessage(0);
        return 0;
    }
    return DefWindowProcW(hwnd, msg, wParam, lParam);
}

int WINAPI wWinMain(HINSTANCE hInstance, HINSTANCE, PWSTR, int nCmdShow)
{
    GdiplusStartup(&g_gdiplusToken, &g_gdiplusStartupInput, NULL);

    WNDCLASSEXW wc = {};
    wc.cbSize = sizeof(wc);
    wc.style = CS_HREDRAW | CS_VREDRAW;
    wc.lpfnWndProc = WndProc;
    wc.hInstance = hInstance;
    wc.hCursor = LoadCursor(NULL, IDC_ARROW);
    wc.hbrBackground = (HBRUSH)GetStockObject(NULL_BRUSH);
    wc.lpszClassName = kWindowClass;
    RegisterClassExW(&wc);

    g_hwnd = CreateWindowExW(0, kWindowClass, L"Фото в чёрно-белое",
        WS_OVERLAPPEDWINDOW, CW_USEDEFAULT, CW_USEDEFAULT, 920, 620,
        NULL, NULL, hInstance, NULL);

    ShowWindow(g_hwnd, nCmdShow);
    UpdateWindow(g_hwnd);

    MSG msg;
    while (GetMessageW(&msg, NULL, 0, 0))
    {
        TranslateMessage(&msg);
        DispatchMessageW(&msg);
    }

    GdiplusShutdown(g_gdiplusToken);
    return (int)msg.wParam;
}
