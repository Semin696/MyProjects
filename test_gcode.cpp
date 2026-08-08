#include "../LaserGenerade/app.h"

// test harness for ProcessFile/GenerateGCode without the full GUI

const wchar_t* kWindowClass = L"BwPhotoApp";
HWND g_hwnd = nullptr;
bool g_dragging = false;
bool g_tracking = false;
float g_animTime = 0.0f;
Font* g_fontTitle = nullptr;
Font* g_fontSub = nullptr;
Font* g_fontBtn = nullptr;
Font* g_fontPanelLabel = nullptr;
Font* g_fontHint = nullptr;
Font* g_fontStatus = nullptr;
Font* g_fontOverlay = nullptr;

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
int HitTestBtn(int, int) { return 0; }

#include "../LaserGenerade/image.cpp"
#include "../LaserGenerade/wood.cpp"

int wmain(int argc, wchar_t** argv)
{
    GdiplusStartupInput si;
    ULONG_PTR tok;
    GdiplusStartup(&tok, &si, NULL);

    bool ok = ProcessFile(argv[1]);
    printf("ProcessFile: %s\n", ok ? "OK" : "FAIL");
    printf("status: %ls\n", g_status.c_str());
    if (ok)
    {
        FILE* f = _wfopen(argv[2], L"r");
        if (f)
        {
            char line[512];
            int count = 0;
            while (fgets(line, sizeof(line), f)) count++;
            fclose(f);
            printf("gcode lines: %d\n", count);
        }
        else printf("gcode file missing\n");
    }

    GdiplusShutdown(tok);
    return 0;
}
