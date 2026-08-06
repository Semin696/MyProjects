#include "app.h"

static GdiplusStartupInput g_gdiplusStartupInput;
static ULONG_PTR g_gdiplusToken;

const wchar_t* kWindowClass = L"BwPhotoApp";
HWND g_hwnd = nullptr;

bool g_dragging = false;
bool g_tracking = false;

// ---- Fonts ----
Font* g_fontTitle = nullptr;
Font* g_fontSub = nullptr;
Font* g_fontBtn = nullptr;
Font* g_fontPanelLabel = nullptr;
Font* g_fontHint = nullptr;
Font* g_fontStatus = nullptr;
Font* g_fontOverlay = nullptr;

// ---- Buttons ----
Btn g_btns[3] = {
    { {}, kBtnOpen,   true,  false, false, L"Открыть файл" },
    { {}, kBtnSaveAs, false, false, false, L"Сохранить как…" },
    { {}, kBtnClear,  false, false, false, L"Сбросить" },
};

Btn* GetBtn(int id)
{
    for (int i = 0; i < 3; i++)
        if (g_btns[i].id == id) return &g_btns[i];
    return nullptr;
}

int HitTestBtn(int x, int y)
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

static void Paint(Graphics& g, int W, int H)
{
    const int margin = 26;

    RenderBackdrop(g, W, H);

    // assembled UI (buttons/panels at wobble positions)
    DrawAssembledUI(g, W, H);

    // hammer swing over the last nailed part
    DrawHammer(g);

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
        int contentTop = 244;
        int contentBottom = H - 44;
        int contentH = contentBottom - contentTop;
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

    // ---- Boot / loading overlay (Windows 11 style) ----
    DrawIntroOverlay(g, W, H);
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
            UpdateIntro(0.016f);
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
        if (b && b->enabled && IsIntroDone())
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

    InitIntro();

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
