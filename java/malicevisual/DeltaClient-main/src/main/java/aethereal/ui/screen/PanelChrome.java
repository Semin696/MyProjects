package aethereal.ui.screen;

import aethereal.render.ColorUtil;
import aethereal.render.Draw2DProcessor;
import aethereal.render.Fonts;
import aethereal.render.ScissorUtil;
import aethereal.util.MathUtil;
import net.minecraft.client.util.math.MatrixStack;

final class PanelChrome {
    private PanelChrome() {
    }

    static void clipBegin(MatrixStack matrices, float x, float y, float width, float height) {
        ScissorUtil.a(matrices, x, y, Math.max(0.0f, width), Math.max(0.0f, height));
    }

    static void clipEnd(MatrixStack matrices) {
        ScissorUtil.a(matrices);
    }

    static void scrollbar(Draw2DProcessor draw, MatrixStack matrices, float listX, float listW, float listTop, float listH,
                          float contentH, float scroll, float maxScroll, float alpha, int primary) {
        if (maxScroll <= 0.5f || listH <= 0.0f) {
            return;
        }
        float barW = 2.5f;
        float barX = listX + listW - barW;
        float barH = Math.max(16.0f, (listH / contentH) * listH);
        float ratio = maxScroll <= 0.0f ? 0.0f : scroll / maxScroll;
        float barY = listTop + ratio * (listH - barH);
        draw.a(matrices, barX, listTop, barW, listH, 1.2f, ColorUtil.convertToARGB(12, 14, 20, (int) (140 * alpha)));
        draw.a(matrices, barX, barY, barW, barH, 1.2f, ColorUtil.applyAlphaToColor(primary, 0.5f * alpha));
    }

    static Hit icon(Draw2DProcessor draw, MatrixStack matrices, float x, float y, float size, String icon, int color,
                    double mouseX, double mouseY, float alpha) {
        boolean hover = MathUtil.a(mouseX, mouseY, x, y, size, size);
        draw.a(matrices, x, y, size, size, 5.5f, ColorUtil.convertToARGB(16, 18, 26, (int) ((hover ? 230 : 165) * alpha)));
        draw.a(matrices, x, y, size, size, 5.5f, 0.6f, ColorUtil.applyAlphaToColor(color, (hover ? 0.85f : 0.38f) * alpha));
        float iconSize = 8.6f;
        float iconW = Fonts.a.a(icon, iconSize);
        float iconY = Fonts.a.a(icon, iconSize, y + size * 0.5f);
        Fonts.a.a(matrices, icon, x + (size - iconW) * 0.5f, iconY, iconSize, ColorUtil.applyAlphaToColor(color, alpha));
        Hit hit = new Hit();
        hit.x = x;
        hit.y = y;
        hit.w = size;
        hit.h = size;
        return hit;
    }

    static boolean inside(double mouseX, double mouseY, Hit hit) {
        return hit != null && MathUtil.a(mouseX, mouseY, hit.x, hit.y, hit.w, hit.h);
    }

    static final class Hit {
        float x;
        float y;
        float w;
        float h;
    }
}
