#include "app.h"

Bitmap* g_original = nullptr;
Bitmap* g_gray = nullptr;
std::wstring g_originalPath;
std::wstring g_resultPath;
std::wstring g_status;
bool g_statusOk = false;

float g_mmW = 100.0f;
float g_mmH = 100.0f;
float g_dpi = 100.0f;

// Fit the grayscale image to the fixed engraving canvas (mm x DPI -> pixels).
// Top-left aligned so GRBL always burns from the same origin.
Bitmap* FitToLaser(Bitmap* src)
{
    if (!src) return nullptr;
    float iw = (float)src->GetWidth();
    float ih = (float)src->GetHeight();
    if (iw <= 0 || ih <= 0) return nullptr;

    int canvasW = (int)(g_mmW * g_dpi / 25.4f + 0.5f);
    int canvasH = (int)(g_mmH * g_dpi / 25.4f + 0.5f);
    if (canvasW < 1) canvasW = 1;
    if (canvasH < 1) canvasH = 1;

    float scale = std::min((float)canvasW / iw, (float)canvasH / ih);
    int w = (int)(iw * scale + 0.5f);
    int h = (int)(ih * scale + 0.5f);
    if (w < 1) w = 1;
    if (h < 1) h = 1;

    Bitmap* out = new Bitmap(canvasW, canvasH, PixelFormat32bppARGB);
    Graphics g(out);
    g.Clear(Color::White);
    g.SetInterpolationMode(InterpolationModeHighQualityBicubic);
    g.DrawImage(src, 0, 0, w, h);
    return out;
}

// Re-apply current engraving settings to the existing result.
void ApplyLaserSettings()
{
    if (!g_gray) return;
    Bitmap* fitted = FitToLaser(g_gray);
    if (!fitted) return;
    delete g_gray;
    g_gray = fitted;

    if (SaveBitmap(g_gray, g_resultPath))
    {
        g_status = L"Готово — " + g_resultPath + L" (" +
                   std::to_wstring(fitted->GetWidth()) + L"×" +
                   std::to_wstring(fitted->GetHeight()) + L" px)";
        g_statusOk = true;
    }
    else
    {
        g_status = L"Ошибка сохранения: " + g_resultPath;
        g_statusOk = false;
    }
    UpdateButtonStates();
    InvalidateRect(g_hwnd, NULL, TRUE);
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

Bitmap* MakeGrayscale(Bitmap* src)
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

bool SaveBitmap(Bitmap* bmp, const std::wstring& path)
{
    CLSID clsid;
    if (GetEncoderClsid(MimeForPath(path), &clsid) == -1) return false;
    return bmp->Save(path.c_str(), &clsid, NULL) == Ok;
}

void UpdateButtonStates()
{
    bool has = (g_gray != nullptr);
    GetBtn(kBtnSaveAs)->enabled = has;
    GetBtn(kBtnClear)->enabled = has;
}

bool ProcessFile(const std::wstring& path)
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
    Bitmap* fitted = FitToLaser(gray);
    delete gray;
    gray = fitted;

    delete g_original;
    delete g_gray;
    g_original = src;
    g_gray = gray;
    g_originalPath = path;
    g_resultPath = MakeOutputPath(path);

    if (SaveBitmap(gray, g_resultPath))
    {
        g_status = L"Готово — " + g_resultPath + L" (" +
                   std::to_wstring(gray->GetWidth()) + L"×" +
                   std::to_wstring(gray->GetHeight()) + L" px)";
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

void OpenFileDialog()
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

void SaveAsDialog()
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

void ClearAll()
{
    delete g_original; g_original = nullptr;
    delete g_gray; g_gray = nullptr;
    g_originalPath.clear();
    g_resultPath.clear();
    g_status.clear();
    UpdateButtonStates();
    InvalidateRect(g_hwnd, NULL, TRUE);
}

void DoAction(int id)
{
    switch (id)
    {
    case kBtnOpen: OpenFileDialog(); break;
    case kBtnSaveAs: SaveAsDialog(); break;
    case kBtnClear: ClearAll(); break;
    }
}
