#pragma once

#include <windows.h>
#include <windowsx.h>
#include <gdiplus.h>
#include <commdlg.h>
#include <ole2.h>
#include <dwmapi.h>
#include <string>
#include <vector>
#include <cmath>
#include <algorithm>

using namespace Gdiplus;

// ---- Window ----
extern const wchar_t* kWindowClass;
extern HWND g_hwnd;

// ---- State ----
extern Bitmap* g_original;
extern Bitmap* g_gray;
extern std::wstring g_originalPath;
extern std::wstring g_resultPath;
extern std::wstring g_status;
extern bool g_statusOk;
extern bool g_dragging;
extern bool g_tracking;
extern float g_animTime;

// ---- Fonts ----
extern Font* g_fontTitle;
extern Font* g_fontSub;
extern Font* g_fontBtn;
extern Font* g_fontPanelLabel;
extern Font* g_fontHint;
extern Font* g_fontStatus;
extern Font* g_fontOverlay;

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

// ---- Buttons ----
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

extern Btn g_btns[3];
Btn* GetBtn(int id);
int HitTestBtn(int x, int y);

// ---- Intro (one-time title appearance) ----
enum IntroStage { INTRO_BOOT, INTRO_DONE };
extern IntroStage g_introStage;
bool IsIntroDone();

void InitIntro();
void UpdateIntro(float dt);
void DrawIntroOverlay(Graphics& g, int W, int H);
void DrawAssembledUI(Graphics& g, int W, int H);
void DrawHammer(Graphics& g);

// ---- Wood / engraving backdrop ----
float Rand01();
void EnsureWood(int W, int H);
void CreateTextBitmaps();
void DeleteTextBitmaps();
void RenderBackdrop(Graphics& g, int W, int H);

// ---- Controls ----
GraphicsPath* CreateRoundRect(float x, float y, float w, float h, float r);
void DrawButton(Graphics& g, const Btn& b);
void DrawPanel(Graphics& g, const RectF& box, int idx);
void DrawImageInPanel(Graphics& g, Bitmap* img, const RectF& box);

// ---- Image processing ----
void UpdateButtonStates();
bool ProcessFile(const std::wstring& path);
void OpenFileDialog();
void SaveAsDialog();
void ClearAll();
void DoAction(int id);
bool SaveBitmap(Bitmap* bmp, const std::wstring& path);
Bitmap* MakeGrayscale(Bitmap* src);
