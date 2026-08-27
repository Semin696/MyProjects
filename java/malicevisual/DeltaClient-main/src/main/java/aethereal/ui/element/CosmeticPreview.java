package aethereal.ui.element;

import aethereal.render.ColorUtil;
import aethereal.render.Draw2DProcessor;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

/**
 * Solid-looking 3D cosmetic previews for the click-GUI (rotates while hovered).
 */
public final class CosmeticPreview {
    private CosmeticPreview() {
    }

    public static boolean isCosmeticStyle(String settingName) {
        if (settingName == null) {
            return false;
        }
        return settingName.startsWith("Стиль шапки")
                || settingName.startsWith("Стиль рюкзака")
                || settingName.startsWith("Стиль питомца")
                || settingName.startsWith("Стиль нимба")
                || settingName.startsWith("Стиль крыльев");
    }

    public static void draw(Draw2DProcessor draw, MatrixStack matrices, String settingName, String mode,
                            float x, float y, float size, float rotationDeg, int accent, float alpha) {
        if (draw == null || settingName == null || mode == null || alpha <= 0.01f) {
            return;
        }
        float panel = size + 18.0f;
        float px = x - panel * 0.5f;
        float py = y - panel - 8.0f;
        int bg = ColorUtil.convertToARGB(10, 12, 18, (int) (220 * alpha));
        int outline = ColorUtil.applyAlphaToColor(accent, 0.55f * alpha);
        draw.a(matrices, px, py, panel, panel, 10.0f, bg);
        draw.a(matrices, px, py, panel, panel, 10.0f, 0.7f, outline);
        draw.a(matrices, px + 8.0f, py + 8.0f, panel - 16.0f, panel - 16.0f, 8.0f,
                ColorUtil.convertToARGB(16, 18, 28, (int) (180 * alpha)));

        float cx = px + panel * 0.5f;
        float cy = py + panel * 0.55f;
        matrices.push();
        matrices.translate(cx, cy, 200.0f);
        matrices.scale(size * 0.42f, -size * 0.42f, size * 0.42f);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(18.0f));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotationDeg));
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        beginSolid();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        int[] rgb = ColorUtil.b(accent);
        renderModel(buffer, matrix, settingName, mode, rgb[0], rgb[1], rgb[2], alpha);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        endSolid();
        matrices.pop();
    }

    private static void renderModel(BufferBuilder buffer, Matrix4f matrix, String setting, String mode, int r, int g, int b, float alpha) {
        int a = Math.round(255 * alpha);
        if (setting.startsWith("Стиль шапки")) {
            hat(buffer, matrix, mode, r, g, b, a);
        } else if (setting.startsWith("Стиль рюкзака")) {
            backpack(buffer, matrix, mode, r, g, b, a);
        } else if (setting.startsWith("Стиль питомца")) {
            pet(buffer, matrix, mode, r, g, b, a);
        } else if (setting.startsWith("Стиль нимба")) {
            halo(buffer, matrix, mode, r, g, b, a);
        } else if (setting.startsWith("Стиль крыльев")) {
            wings(buffer, matrix, mode, r, g, b, a);
        }
    }

    private static void hat(BufferBuilder buffer, Matrix4f m, String mode, int r, int g, int b, int a) {
        if ("Сундук".equals(mode)) {
            float t = (System.currentTimeMillis() % 100000L) / 1000.0f;
            float open = 0.5f + 0.5f * (float) Math.sin(t * 1.35d);
            box(buffer, m, 0, -0.05f, 0, 0.95f, 0.38f, 0.95f, new int[]{140, 92, 42}, a);
            box(buffer, m, 0, 0.28f + open * 0.22f, -0.05f - open * 0.08f, 0.95f, 0.28f, 0.95f, new int[]{150, 100, 48}, a);
            box(buffer, m, 0, 0.05f, 0.48f, 0.14f, 0.14f, 0.08f, new int[]{210, 190, 120}, a);
        } else if ("Шалкер".equals(mode)) {
            float t = (System.currentTimeMillis() % 100000L) / 1000.0f;
            float lid = (0.5f + 0.5f * (float) Math.sin(t * 2.1d)) * 0.28f;
            int[] shell = new int[]{clamp(150 + r / 8), clamp(90 + g / 10), clamp(180 + b / 12)};
            box(buffer, m, 0, -0.10f, 0, 0.95f, 0.42f, 0.95f, shell, a);
            box(buffer, m, 0, 0.35f + lid, 0, 0.95f, 0.48f, 0.95f, shell, a);
            box(buffer, m, 0, 0.18f + lid * 0.35f, 0, 0.40f, 0.22f, 0.40f, new int[]{255, 220, 255}, a);
        } else if ("Волшебник".equals(mode)) {
            box(buffer, m, 0, -0.05f, 0, 0.95f, 0.12f, 0.95f, shade(40, 30, 55), a);
            cone(buffer, m, 0, 0.0f, 0, 0.42f, 1.05f, 16, r, g, b, a);
            box(buffer, m, 0.05f, 1.05f, -0.05f, 0.12f, 0.12f, 0.12f, new int[]{255, 220, 120}, a);
        } else if ("Рога".equals(mode)) {
            box(buffer, m, 0, 0.0f, 0, 0.55f, 0.22f, 0.55f, new int[]{30, 20, 28}, a);
            horn(buffer, m, 1, r, g, b, a);
            horn(buffer, m, -1, r, g, b, a);
        } else if ("Ушки".equals(mode)) {
            box(buffer, m, 0, 0.0f, 0, 0.55f, 0.22f, 0.55f, tint(r, g, b, 0.35f), a);
            ear(buffer, m, 1, r, g, b, a);
            ear(buffer, m, -1, r, g, b, a);
        } else if ("Конус".equals(mode)) {
            cone(buffer, m, 0, -0.05f, 0, 0.85f, 0.55f, 20, r, g, b, a);
            torus(buffer, m, 0, -0.02f, 0, 0.88f, 0.04f, 20, 8, 255, 230, 140, a);
        } else if ("Кепка".equals(mode)) {
            box(buffer, m, 0, 0.08f, 0, 0.70f, 0.28f, 0.70f, tint(r, g, b, 0.15f), a);
            box(buffer, m, 0, 0.0f, 0.42f, 0.72f, 0.06f, 0.34f, tint(r, g, b, 0.05f), a);
        } else {
            // crown
            torus(buffer, m, 0, 0.0f, 0, 0.55f, 0.08f, 20, 8, 255, 214, 110, a);
            for (int i = 0; i < 7; i++) {
                double ang = i * (Math.PI * 2.0 / 7.0) - Math.PI * 0.5;
                float bx = (float) Math.cos(ang) * 0.52f;
                float bz = (float) Math.sin(ang) * 0.52f;
                float h = (i % 2 == 0) ? 0.42f : 0.26f;
                spike(buffer, m, bx * 0.75f, 0.05f, bz * 0.75f, bx, 0.05f + h, bz, 0.06f, 255, 214, 110, a);
                if (i % 2 == 0) {
                    sphere(buffer, m, bx, 0.08f + h, bz, 0.07f, 10, clamp(r + 30), clamp(g / 2 + 40), clamp(b + 40), a);
                }
            }
        }
    }

    private static void backpack(BufferBuilder buffer, Matrix4f m, String mode, int r, int g, int b, int a) {
        if ("Сумка".equals(mode)) {
            box(buffer, m, 0, 0.0f, -0.15f, 0.70f, 0.85f, 0.35f, new int[]{110, 70, 45}, a);
            box(buffer, m, 0, 0.28f, -0.15f, 0.55f, 0.16f, 0.12f, new int[]{40, 28, 20}, a);
        } else if ("Джетпак".equals(mode)) {
            box(buffer, m, -0.22f, 0.0f, -0.12f, 0.28f, 0.80f, 0.28f, new int[]{140, 148, 168}, a);
            box(buffer, m, 0.22f, 0.0f, -0.12f, 0.28f, 0.80f, 0.28f, new int[]{140, 148, 168}, a);
            box(buffer, m, 0, 0.05f, -0.02f, 0.22f, 0.35f, 0.18f, new int[]{r, g, b}, a);
            spike(buffer, m, -0.22f, -0.42f, -0.12f, -0.22f, -0.72f, -0.12f, 0.08f, 255, 120, 30, a);
            spike(buffer, m, 0.22f, -0.42f, -0.12f, 0.22f, -0.72f, -0.12f, 0.08f, 255, 120, 30, a);
        } else if ("Школьный".equals(mode)) {
            box(buffer, m, 0, 0.0f, -0.12f, 0.75f, 0.90f, 0.32f, tint(r, g, b, 0.2f), a);
            box(buffer, m, 0, 0.05f, -0.28f, 0.45f, 0.40f, 0.10f, tint(r, g, b, 0.0f), a);
            sphere(buffer, m, 0, -0.05f, -0.30f, 0.07f, 10, 255, 220, 90, a);
        } else if ("Крылатый".equals(mode)) {
            box(buffer, m, 0, 0.0f, -0.10f, 0.45f, 0.70f, 0.28f, new int[]{28, 18, 36}, a);
            crystal(buffer, m, 0, 0.1f, -0.32f, 0.22f, 0.55f, r, g, b, a);
            wingMini(buffer, m, 1, r, g, b, a);
            wingMini(buffer, m, -1, r, g, b, a);
        } else {
            box(buffer, m, 0, 0.0f, -0.10f, 0.45f, 0.70f, 0.28f, new int[]{28, 18, 36}, a);
            crystal(buffer, m, 0, 0.1f, -0.32f, 0.24f, 0.58f, r, g, b, a);
            sphere(buffer, m, 0, 0.1f, -0.32f, 0.10f, 12, 255, 255, 255, a);
        }
    }

    private static void pet(BufferBuilder buffer, Matrix4f m, String mode, int r, int g, int b, int a) {
        if ("Свинка".equals(mode)) {
            box(buffer, m, 0, 0.05f, 0, 0.70f, 0.48f, 0.85f, new int[]{240, 170, 170}, a);
            box(buffer, m, 0, 0.22f, 0.48f, 0.42f, 0.36f, 0.36f, new int[]{240, 170, 170}, a);
            box(buffer, m, 0, 0.16f, 0.68f, 0.28f, 0.20f, 0.16f, new int[]{230, 140, 150}, a);
            box(buffer, m, 0.22f, -0.22f, 0.22f, 0.14f, 0.22f, 0.14f, new int[]{240, 170, 170}, a);
            box(buffer, m, -0.22f, -0.22f, 0.22f, 0.14f, 0.22f, 0.14f, new int[]{240, 170, 170}, a);
            box(buffer, m, 0.22f, -0.22f, -0.22f, 0.14f, 0.22f, 0.14f, new int[]{240, 170, 170}, a);
            box(buffer, m, -0.22f, -0.22f, -0.22f, 0.14f, 0.22f, 0.14f, new int[]{240, 170, 170}, a);
        } else if ("Серый волчёнок".equals(mode)) {
            box(buffer, m, 0, 0.05f, 0, 0.42f, 0.38f, 0.75f, new int[]{150, 150, 155}, a);
            box(buffer, m, 0, 0.28f, 0.42f, 0.36f, 0.34f, 0.36f, new int[]{150, 150, 155}, a);
            box(buffer, m, 0, 0.22f, 0.60f, 0.22f, 0.16f, 0.16f, new int[]{90, 90, 95}, a);
            box(buffer, m, -0.12f, 0.46f, 0.38f, 0.10f, 0.16f, 0.08f, new int[]{120, 120, 125}, a);
            box(buffer, m, 0.12f, 0.46f, 0.38f, 0.10f, 0.16f, 0.08f, new int[]{120, 120, 125}, a);
            box(buffer, m, 0, 0.12f, -0.48f, 0.12f, 0.14f, 0.30f, new int[]{150, 150, 155}, a);
        } else {
            // Цыплёнок
            box(buffer, m, 0, 0.05f, 0, 0.55f, 0.42f, 0.60f, new int[]{240, 210, 70}, a);
            box(buffer, m, 0, 0.32f, 0.22f, 0.38f, 0.34f, 0.38f, new int[]{240, 210, 70}, a);
            box(buffer, m, 0, 0.28f, 0.42f, 0.16f, 0.10f, 0.14f, new int[]{230, 120, 40}, a);
            box(buffer, m, 0, 0.48f, 0.18f, 0.10f, 0.12f, 0.08f, new int[]{220, 40, 40}, a);
            box(buffer, m, 0.32f, 0.10f, 0, 0.08f, 0.28f, 0.35f, new int[]{240, 210, 70}, a);
            box(buffer, m, -0.32f, 0.10f, 0, 0.08f, 0.28f, 0.35f, new int[]{240, 210, 70}, a);
        }
    }

    private static void halo(BufferBuilder buffer, Matrix4f m, String mode, int r, int g, int b, int a) {
        int cr = r, cg = g, cb = b;
        if ("Классический".equals(mode) || "Святой".equals(mode)) {
            cr = 255;
            cg = 220;
            cb = 140;
        } else if ("Демон".equals(mode)) {
            cr = clamp(r + 40);
            cg = clamp(g / 5);
            cb = clamp(b / 6);
        }
        torus(buffer, m, 0, 0.35f, 0, 0.55f, 0.06f, 28, 10, cr, cg, cb, a);
        torus(buffer, m, 0, 0.35f, 0, 0.42f, 0.03f, 24, 8, 255, 250, 240, a);
        if ("Демон".equals(mode)) {
            for (int i = 0; i < 6; i++) {
                double ang = i * (Math.PI * 2.0 / 6.0);
                float ix = (float) Math.cos(ang) * 0.50f;
                float iz = (float) Math.sin(ang) * 0.50f;
                float ox = (float) Math.cos(ang) * 0.68f;
                float oz = (float) Math.sin(ang) * 0.68f;
                spike(buffer, m, ix, 0.35f, iz, ox, 0.55f, oz, 0.04f, cr, cg, cb, a);
            }
        }
    }

    private static void wings(BufferBuilder buffer, Matrix4f m, String mode, int r, int g, int b, int a) {
        wingSide(buffer, m, 1, mode, r, g, b, a);
        wingSide(buffer, m, -1, mode, r, g, b, a);
    }

    private static void wingSide(BufferBuilder buffer, Matrix4f m, float side, String mode, int r, int g, int b, int a) {
        float rx = side * 0.12f;
        float ry = 0.05f;
        float rz = 0.05f;
        if ("Демон".equals(mode) || "Дракон".equals(mode)) {
            int cr = clamp(r + 20);
            int cg = clamp(g / 5 + 8);
            int cb = clamp(b / 6 + 10);
            float[] angs = {-0.2f, 0.25f, 0.7f, 1.1f};
            float[] lens = {0.7f, 1.0f, 0.95f, 0.55f};
            for (int i = 0; i < angs.length - 1; i++) {
                float x0 = rx + side * (float) Math.cos(angs[i]) * lens[i];
                float y0 = ry + (float) Math.sin(angs[i]) * lens[i] * 0.55f;
                float z0 = rz - lens[i] * 0.35f;
                float x1 = rx + side * (float) Math.cos(angs[i + 1]) * lens[i + 1];
                float y1 = ry + (float) Math.sin(angs[i + 1]) * lens[i + 1] * 0.55f;
                float z1 = rz - lens[i + 1] * 0.35f;
                shadedTri(buffer, m, rx, ry, rz, x0, y0, z0, x1, y1, z1, cr, cg, cb, a);
                spike(buffer, m, rx, ry, rz, x0, y0, z0, 0.03f, 40, 12, 14, a);
            }
        } else if ("Бабочка".equals(mode)) {
            shadedTri(buffer, m, rx, ry, rz, rx + side * 0.85f, ry + 0.55f, rz - 0.2f, rx + side * 0.55f, ry - 0.15f, rz - 0.15f, r, g, b, a);
            shadedTri(buffer, m, rx, ry, rz, rx + side * 0.55f, ry - 0.45f, rz - 0.1f, rx + side * 0.25f, ry - 0.05f, rz - 0.05f, clamp(r / 2 + 40), clamp(g / 2 + 20), b, a);
            sphere(buffer, m, rx + side * 0.45f, ry + 0.25f, rz - 0.12f, 0.08f, 10, 20, 12, 28, a);
        } else if ("Феникс".equals(mode)) {
            for (int i = 0; i < 5; i++) {
                float t = i / 4.0f;
                float len = 0.45f + t * 0.55f;
                float ang = -0.1f + t * 0.95f;
                float x1 = rx + side * (float) Math.cos(ang) * len;
                float y1 = ry + (float) Math.sin(ang) * len * 0.7f + 0.1f;
                float z1 = rz - len * 0.4f;
                spike(buffer, m, rx, ry, rz, x1, y1, z1, 0.05f - t * 0.02f, 255, clamp(180 - i * 25), clamp(40 + i * 10), a);
            }
        } else {
            // angel
            for (int i = 0; i < 6; i++) {
                float t = i / 5.0f;
                float len = 0.40f + t * 0.70f;
                float ang = -0.05f + t * 0.95f;
                float x1 = rx + side * (float) Math.cos(ang) * len;
                float y1 = ry + (float) Math.sin(ang) * len * 0.55f + 0.05f;
                float z1 = rz - len * 0.35f;
                spike(buffer, m, rx, ry, rz, x1, y1, z1, 0.055f - t * 0.025f, 255, 246, 228, a);
            }
        }
    }

    private static void horn(BufferBuilder buffer, Matrix4f m, float side, int r, int g, int b, int a) {
        float x0 = side * 0.22f;
        spike(buffer, m, x0, 0.05f, 0.05f, side * 0.42f, 0.45f, -0.05f, 0.08f, clamp(r + 30), clamp(g / 5), clamp(b / 6), a);
        spike(buffer, m, side * 0.42f, 0.45f, -0.05f, side * 0.32f, 0.72f, -0.18f, 0.04f, 255, 240, 220, a);
    }

    private static void ear(BufferBuilder buffer, Matrix4f m, float side, int r, int g, int b, int a) {
        float bx = side * 0.22f;
        shadedTri(buffer, m, bx - side * 0.08f, 0.05f, 0.05f, bx + side * 0.12f, 0.05f, 0.05f, side * 0.32f, 0.48f, -0.02f, tint(r, g, b, 0.35f)[0], tint(r, g, b, 0.35f)[1], tint(r, g, b, 0.35f)[2], a);
        shadedTri(buffer, m, bx, 0.10f, 0.02f, bx + side * 0.06f, 0.10f, 0.02f, side * 0.28f, 0.38f, -0.01f, 255, 150, 180, a);
    }

    private static void wingMini(BufferBuilder buffer, Matrix4f m, float side, int r, int g, int b, int a) {
        shadedTri(buffer, m, side * 0.05f, 0.05f, -0.05f, side * 0.08f, 0.18f, -0.05f, side * 0.55f, 0.28f, -0.18f, r, g, b, Math.round(a * 0.85f));
    }

    private static void crystal(BufferBuilder buffer, Matrix4f m, float cx, float cy, float cz, float radius, float height, int r, int g, int b, int a) {
        float tip = cy + height * 0.55f;
        float bot = cy - height * 0.45f;
        int segs = 6;
        double step = (Math.PI * 2.0) / segs;
        for (int i = 0; i < segs; i++) {
            double a1 = i * step;
            double a2 = (i + 1) * step;
            float x1 = cx + (float) Math.cos(a1) * radius;
            float z1 = cz + (float) Math.sin(a1) * radius;
            float x2 = cx + (float) Math.cos(a2) * radius;
            float z2 = cz + (float) Math.sin(a2) * radius;
            shadedTri(buffer, m, cx, tip, cz, x1, cy, z1, x2, cy, z2, tint(r, g, b, 0.4f)[0], tint(r, g, b, 0.4f)[1], tint(r, g, b, 0.4f)[2], a);
            shadedTri(buffer, m, cx, bot, cz, x2, cy, z2, x1, cy, z1, r, g, b, a);
        }
    }

    private static void box(BufferBuilder buffer, Matrix4f m, float cx, float cy, float cz, float hx, float hy, float hz, int[] rgb, int a) {
        float x0 = cx - hx * 0.5f, x1 = cx + hx * 0.5f;
        float y0 = cy - hy * 0.5f, y1 = cy + hy * 0.5f;
        float z0 = cz - hz * 0.5f, z1 = cz + hz * 0.5f;
        face(buffer, m, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, lit(rgb, 1.00f), a); // +Z
        face(buffer, m, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, lit(rgb, 0.55f), a); // -Z
        face(buffer, m, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, lit(rgb, 1.15f), a); // +Y
        face(buffer, m, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, lit(rgb, 0.40f), a); // -Y
        face(buffer, m, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, lit(rgb, 0.70f), a); // -X
        face(buffer, m, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, lit(rgb, 0.85f), a); // +X
    }

    private static void cone(BufferBuilder buffer, Matrix4f m, float cx, float cy, float cz, float radius, float height, int segs, int r, int g, int b, int a) {
        double step = (Math.PI * 2.0) / segs;
        for (int i = 0; i < segs; i++) {
            double a1 = i * step;
            double a2 = (i + 1) * step;
            float x1 = cx + (float) Math.cos(a1) * radius;
            float z1 = cz + (float) Math.sin(a1) * radius;
            float x2 = cx + (float) Math.cos(a2) * radius;
            float z2 = cz + (float) Math.sin(a2) * radius;
            float shade = 0.55f + 0.45f * (0.5f + 0.5f * (float) Math.cos(a1));
            shadedTri(buffer, m, cx, cy + height, cz, x1, cy, z1, x2, cy, z2,
                    clamp((int) (r * shade)), clamp((int) (g * shade)), clamp((int) (b * shade)), a);
        }
    }

    private static void sphere(BufferBuilder buffer, Matrix4f m, float cx, float cy, float cz, float radius, int segs, int r, int g, int b, int a) {
        int stacks = Math.max(6, segs / 2);
        for (int i = 0; i < stacks; i++) {
            float v0 = (float) (Math.PI * i / stacks);
            float v1 = (float) (Math.PI * (i + 1) / stacks);
            for (int j = 0; j < segs; j++) {
                float u0 = (float) (Math.PI * 2.0 * j / segs);
                float u1 = (float) (Math.PI * 2.0 * (j + 1) / segs);
                float[] p00 = sph(cx, cy, cz, radius, u0, v0);
                float[] p10 = sph(cx, cy, cz, radius, u1, v0);
                float[] p11 = sph(cx, cy, cz, radius, u1, v1);
                float[] p01 = sph(cx, cy, cz, radius, u0, v1);
                float shade = 0.45f + 0.55f * (0.5f + 0.5f * (float) Math.cos(v0) * (float) Math.cos(u0));
                face(buffer, m, p00[0], p00[1], p00[2], p10[0], p10[1], p10[2], p11[0], p11[1], p11[2], p01[0], p01[1], p01[2],
                        lit(new int[]{r, g, b}, shade), a);
            }
        }
    }

    private static float[] sph(float cx, float cy, float cz, float radius, float u, float v) {
        float sv = (float) Math.sin(v);
        return new float[]{
                cx + radius * sv * (float) Math.cos(u),
                cy + radius * (float) Math.cos(v),
                cz + radius * sv * (float) Math.sin(u)
        };
    }

    private static void torus(BufferBuilder buffer, Matrix4f m, float cx, float cy, float cz, float major, float minor, int ring, int tube, int r, int g, int b, int a) {
        double us = (Math.PI * 2.0) / ring;
        double vs = (Math.PI * 2.0) / tube;
        for (int i = 0; i < ring; i++) {
            double u0 = i * us;
            double u1 = (i + 1) * us;
            for (int j = 0; j < tube; j++) {
                double v0 = j * vs;
                double v1 = (j + 1) * vs;
                float[] p00 = tor(cx, cy, cz, major, minor, u0, v0);
                float[] p10 = tor(cx, cy, cz, major, minor, u1, v0);
                float[] p11 = tor(cx, cy, cz, major, minor, u1, v1);
                float[] p01 = tor(cx, cy, cz, major, minor, u0, v1);
                float shade = 0.55f + 0.45f * (0.5f + 0.5f * (float) Math.cos(v0));
                face(buffer, m, p00[0], p00[1], p00[2], p10[0], p10[1], p10[2], p11[0], p11[1], p11[2], p01[0], p01[1], p01[2],
                        lit(new int[]{r, g, b}, shade), a);
            }
        }
    }

    private static float[] tor(float cx, float cy, float cz, float major, float minor, double u, double v) {
        float cv = (float) Math.cos(v);
        float sv = (float) Math.sin(v);
        float cu = (float) Math.cos(u);
        float su = (float) Math.sin(u);
        return new float[]{
                cx + (major + minor * cv) * cu,
                cy + minor * sv,
                cz + (major + minor * cv) * su
        };
    }

    private static void spike(BufferBuilder buffer, Matrix4f m, float x0, float y0, float z0, float x1, float y1, float z1, float radius, int r, int g, int b, int a) {
        float dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;
        float len = MathHelper.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1.0e-4f) {
            return;
        }
        dx /= len;
        dy /= len;
        dz /= len;
        float px = -dy, py = dx, pz = 0.15f;
        float plen = MathHelper.sqrt(px * px + py * py + pz * pz);
        px /= plen;
        py /= plen;
        pz /= plen;
        float qx = dy * pz - dz * py;
        float qy = dz * px - dx * pz;
        float qz = dx * py - dy * px;
        float qlen = MathHelper.sqrt(qx * qx + qy * qy + qz * qz);
        if (qlen > 1.0e-4f) {
            qx /= qlen;
            qy /= qlen;
            qz /= qlen;
        }
        int segs = 6;
        for (int i = 0; i < segs; i++) {
            double a0 = i * (Math.PI * 2.0 / segs);
            double a1 = (i + 1) * (Math.PI * 2.0 / segs);
            float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0);
            float c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
            float r0x = x0 + (px * c0 + qx * s0) * radius;
            float r0y = y0 + (py * c0 + qy * s0) * radius;
            float r0z = z0 + (pz * c0 + qz * s0) * radius;
            float r1x = x0 + (px * c1 + qx * s1) * radius;
            float r1y = y0 + (py * c1 + qy * s1) * radius;
            float r1z = z0 + (pz * c1 + qz * s1) * radius;
            float shade = 0.55f + 0.45f * (0.5f + 0.5f * c0);
            shadedTri(buffer, m, x1, y1, z1, r0x, r0y, r0z, r1x, r1y, r1z,
                    clamp((int) (r * shade)), clamp((int) (g * shade)), clamp((int) (b * shade)), a);
        }
    }

    private static void shadedTri(BufferBuilder buffer, Matrix4f m, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, int r, int g, int b, int a) {
        face(buffer, m, x1, y1, z1, x2, y2, z2, x3, y3, z3, x1, y1, z1, new int[]{r, g, b}, a);
    }

    private static void face(BufferBuilder buffer, Matrix4f m, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, int[] rgb, int a) {
        int alpha = Math.max(0, Math.min(255, a));
        buffer.vertex(m, x1, y1, z1).color(rgb[0], rgb[1], rgb[2], alpha);
        buffer.vertex(m, x2, y2, z2).color(rgb[0], rgb[1], rgb[2], alpha);
        buffer.vertex(m, x3, y3, z3).color(rgb[0], rgb[1], rgb[2], alpha);
        buffer.vertex(m, x4, y4, z4).color(rgb[0], rgb[1], rgb[2], alpha);
    }

    private static int[] lit(int[] rgb, float mul) {
        return new int[]{clamp((int) (rgb[0] * mul)), clamp((int) (rgb[1] * mul)), clamp((int) (rgb[2] * mul))};
    }

    private static int[] tint(int r, int g, int b, float towardsWhite) {
        return new int[]{
                clamp((int) (r * (1.0f - towardsWhite) + 255 * towardsWhite)),
                clamp((int) (g * (1.0f - towardsWhite) + 248 * towardsWhite)),
                clamp((int) (b * (1.0f - towardsWhite) + 240 * towardsWhite))
        };
    }

    private static int[] shade(int r, int g, int b) {
        return new int[]{r, g, b};
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    private static void beginSolid() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
    }

    private static void endSolid() {
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }
}
