#include "app.h"

GraphicsPath* CreateRoundRect(float x, float y, float w, float h, float r)
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

void DrawImageInPanel(Graphics& g, Bitmap* img, const RectF& box)
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

void DrawButton(Graphics& g, const Btn& b)
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

void DrawPanel(Graphics& g, const RectF& box, int idx)
{
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
    g.DrawString(idx == 0 ? L"Оригинал" : L"Чёрно-белое", -1, g_fontPanelLabel, labelBox, &lf, &labelCol);

    RectF imgBox(box.X + 12, box.Y + 46, box.Width - 24, box.Height - 58);
    Bitmap* img = (idx == 0) ? g_original : g_gray;
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
        g.DrawString(idx == 0 ? L"Здесь появится\nизображение"
                              : L"Здесь появится\nчёрно-белый результат",
                     -1, g_fontHint, hintBox, &hf, &hint);
    }
}
