package aethereal.render;

import aethereal.core.Interface;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public final class ParticleTextures {
    private static final int SIZE = 128;
    private static final Map<String, Identifier> BY_TYPE = new HashMap<>();
    private static Identifier glowId;
    private static boolean ready;

    private ParticleTextures() {
    }

    public static void ensure() {
        if (ready || Interface.mc == null || Interface.mc.getTextureManager() == null) {
            return;
        }
        glowId = register("glow", drawGlow());
        Identifier star = register("star", drawSparkle(false));
        Identifier starSoft = register("star_soft", drawSparkle(true));
        Identifier snow = register("snow", drawSnow(false));
        Identifier snowflake = register("snowflake", drawSnow(true));
        Identifier snowbag = register("snowbag", drawSnowbag());
        Identifier bloom = glowId;
        Identifier bucks = register("bucks", drawDollar(true));
        Identifier dollar = register("dollar", drawDollar(false));
        Identifier core = register("core", drawCore());
        Identifier crest = register("crest", drawCrest());
        Identifier crown = register("crown", drawCrown());
        Identifier cube = register("cube", drawCube(false));
        Identifier cubeBlast = register("cube_blast", drawCube(true));
        Identifier ded = register("ded", drawSkull());
        Identifier firefly = register("firefly", drawFirefly());
        Identifier heart = register("heart", drawHeart(false));
        Identifier heartAlt = register("heart_alt", drawHeart(true));
        Identifier lightning = register("lightning", drawLightning());
        Identifier sperm = register("sperm", drawSperm());

        map("Звезды", star);
        map("Star", star);
        map("Star1", starSoft);
        map("Star New", starSoft);
        map("Снег", snow);
        map("Snowflake", snowflake);
        map("Snow New", snowflake);
        map("Snowbag", snowbag);
        map("Snow Binsecure", snowbag);
        map("Snow Blast", snowbag);
        map("Блум", bloom);
        map("Glow", bloom);
        map("Bucks", bucks);
        map("Dollar", dollar);
        map("Core", core);
        map("Crest", crest);
        map("Crown", crown);
        map("Cube", cube);
        map("Cube Blast", cubeBlast);
        map("Ded", ded);
        map("Firefly", firefly);
        map("Heart", heart);
        map("Heart1", heartAlt);
        map("Lightning", lightning);
        map("Сперматозоиды", sperm);
        ready = true;
    }

    public static Identifier glow() {
        ensure();
        return glowId;
    }

    public static Identifier of(String typeName) {
        ensure();
        Identifier id = BY_TYPE.get(typeName);
        return id != null ? id : BY_TYPE.get("Звезды");
    }

    public static boolean usesSoftGlow(String typeName) {
        return !"Сперматозоиды".equals(typeName);
    }

    private static void map(String type, Identifier id) {
        BY_TYPE.put(type, id);
    }

    private static Identifier register(String key, NativeImage image) {
        Identifier id = Identifier.of("skeleton", "particles/" + key);
        NativeImageBackedTexture texture = new NativeImageBackedTexture(image);
        texture.upload();
        Interface.mc.getTextureManager().registerTexture(id, texture);
        return id;
    }

    private static NativeImage create() {
        NativeImage image = new NativeImage(SIZE, SIZE, true);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                image.setColorArgb(x, y, 0);
            }
        }
        return image;
    }

    private static float s(float v) {
        return v * (SIZE / 64.0f);
    }

    private static float clamp01(float v) {
        return v < 0.0f ? 0.0f : Math.min(1.0f, v);
    }

    private static float gauss(float d, float sigma) {
        return (float) Math.exp(-(d * d) / (2.0d * sigma * sigma));
    }

    private static void plot(NativeImage image, int x, int y, int a, int r, int g, int b) {
        if (x < 0 || y < 0 || x >= SIZE || y >= SIZE || a <= 0) {
            return;
        }
        int prev = image.getColorArgb(x, y);
        int pa = (prev >> 24) & 255;
        int pr = (prev >> 16) & 255;
        int pg = (prev >> 8) & 255;
        int pb = prev & 255;
        float src = a / 255.0f;
        float dst = (1.0f - src) * (pa / 255.0f);
        int na = Math.min(255, Math.round((src + dst) * 255.0f));
        int nr = Math.min(255, Math.round(r * src + pr * dst));
        int ng = Math.min(255, Math.round(g * src + pg * dst));
        int nb = Math.min(255, Math.round(b * src + pb * dst));
        image.setColorArgb(x, y, (na << 24) | (nr << 16) | (ng << 8) | nb);
    }

    private static void plotF(NativeImage image, int x, int y, float a, int r, int g, int b) {
        plot(image, x, y, Math.round(clamp01(a) * 255.0f), r, g, b);
    }

    private static NativeImage drawGlow() {
        NativeImage image = create();
        float cx = (SIZE - 1) * 0.5f;
        float cy = cx;
        float maxR = SIZE * 0.5f;
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                float d = (float) Math.hypot(x - cx, y - cy) / maxR;
                float core = gauss(d, 0.09f);
                float mid = gauss(d, 0.18f) * 0.72f;
                float halo = gauss(d, 0.34f) * 0.38f;
                plotF(image, x, y, core + mid + halo, 255, 255, 255);
            }
        }
        return image;
    }

    private static NativeImage drawSparkle(boolean soft) {
        NativeImage image = create();
        float cx = (SIZE - 1) * 0.5f;
        float cy = cx;
        float maxR = SIZE * (soft ? 0.48f : 0.46f);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                float dx = x - cx;
                float dy = y - cy;
                float r = (float) Math.hypot(dx, dy);
                float ang = (float) Math.atan2(dy, dx);
                float radial = (float) Math.pow(clamp01(1.0f - r / maxR), soft ? 1.35d : 1.7d);
                float spike = (float) Math.pow(Math.abs(Math.cos(2.0d * ang)), soft ? 5.2d : 7.4d);
                float diag = (float) Math.pow(Math.abs(Math.cos(2.0d * ang + Math.PI / 2.0d)), 11.0d) * 0.42f;
                float core = gauss(r / maxR, 0.07f);
                float halo = gauss(r / maxR, 0.22f) * 0.55f;
                float a = (spike * 0.95f + diag) * radial + core * 0.95f + halo;
                plotF(image, x, y, a, 255, 255, 255);
            }
        }
        return image;
    }

    private static NativeImage drawSnow(boolean flake) {
        NativeImage image = create();
        float cx = (SIZE - 1) * 0.5f;
        float cy = cx;
        float maxR = SIZE * 0.42f;
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                float dx = x - cx;
                float dy = y - cy;
                float r = (float) Math.hypot(dx, dy);
                if (r > maxR) {
                    continue;
                }
                float ang = (float) Math.atan2(dy, dx);
                float sector = (float) ((ang + Math.PI) % (Math.PI / 3.0d));
                if (sector > Math.PI / 6.0d) {
                    sector = (float) (Math.PI / 3.0d) - sector;
                }
                float arm = gauss(sector * r, flake ? 2.4f : 3.4f) * (float) Math.pow(clamp01(1.0f - r / maxR), 0.65d);
                float branch = 0.0f;
                if (flake) {
                    float t = r / maxR;
                    if (t > 0.32f && t < 0.82f) {
                        float bx = (float) (Math.cos(ang) * r);
                        float by = (float) (Math.sin(ang) * r);
                        for (int i = 0; i < 6; i++) {
                            double a = i * (Math.PI / 3.0d);
                            float px = (float) (Math.cos(a) * r);
                            float py = (float) (Math.sin(a) * r);
                            float nx = (float) Math.cos(a + 0.7d);
                            float ny = (float) Math.sin(a + 0.7d);
                            float along = ((bx - px) * nx + (by - py) * ny);
                            if (along > 0.0f && along < s(10.0f)) {
                                float dist = Math.abs((bx - px) * -ny + (by - py) * nx);
                                branch = Math.max(branch, gauss(dist, 1.6f) * (1.0f - along / s(10.0f)));
                            }
                        }
                    }
                }
                float core = gauss(r, s(3.4f));
                float halo = gauss(r / maxR, 0.28f) * 0.22f;
                plotF(image, x, y, arm * 0.95f + branch * 0.7f + core + halo, 255, 255, 255);
            }
        }
        return image;
    }

    private static NativeImage drawSnowbag() {
        NativeImage image = create();
        disc(image, s(32.0f), s(36.0f), s(16.0f), 230);
        disc(image, s(32.0f), s(24.0f), s(11.0f), 200);
        for (int i = 0; i < 8; i++) {
            float ang = (float) ((Math.PI * 2.0d * i) / 8.0d);
            disc(image, s(32.0f) + (float) Math.cos(ang) * s(10.0f), s(34.0f) + (float) Math.sin(ang) * s(8.0f), s(2.2f), 255);
        }
        return image;
    }

    private static NativeImage drawDollar(boolean bucks) {
        NativeImage image = create();
        line(image, s(32.0f), s(10.0f), s(32.0f), s(54.0f), s(bucks ? 2.4f : 1.8f), 255);
        if (bucks) {
            line(image, s(27.0f), s(10.0f), s(27.0f), s(54.0f), s(1.6f), 200);
            line(image, s(37.0f), s(10.0f), s(37.0f), s(54.0f), s(1.6f), 200);
        }
        for (int i = 0; i < 48; i++) {
            float t = i / 47.0f;
            float x = s(32.0f) + (float) Math.sin(t * Math.PI * 2.1d) * s(11.0f);
            float y = s(18.0f) + t * s(28.0f);
            disc(image, x, y, s(bucks ? 2.3f : 2.0f), 255);
        }
        return image;
    }

    private static NativeImage drawCore() {
        NativeImage image = create();
        float cx = (SIZE - 1) * 0.5f;
        float cy = cx;
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                float d = (float) Math.hypot(x - cx, y - cy);
                float ring = 1.0f - Math.abs(d - s(18.0f)) / s(4.2f);
                float inner = gauss(d, s(7.0f));
                float halo = gauss(d, s(22.0f)) * 0.28f;
                float a = Math.max(0.0f, Math.max(ring, 0.0f)) * 0.9f + inner * 0.85f + halo;
                plotF(image, x, y, a, 255, 255, 255);
            }
        }
        return image;
    }

    private static NativeImage drawCrest() {
        NativeImage image = create();
        line(image, s(32.0f), s(12.0f), s(14.0f), s(48.0f), s(2.4f), 255);
        line(image, s(32.0f), s(12.0f), s(50.0f), s(48.0f), s(2.4f), 255);
        line(image, s(20.0f), s(36.0f), s(44.0f), s(36.0f), s(2.0f), 220);
        disc(image, s(32.0f), s(14.0f), s(3.5f), 255);
        return image;
    }

    private static NativeImage drawCrown() {
        NativeImage image = create();
        int[][] pts = {{10, 42}, {16, 18}, {24, 34}, {32, 10}, {40, 34}, {48, 18}, {54, 42}};
        for (int i = 0; i < pts.length - 1; i++) {
            line(image, s(pts[i][0]), s(pts[i][1]), s(pts[i + 1][0]), s(pts[i + 1][1]), s(2.3f), 255);
        }
        line(image, s(10.0f), s(42.0f), s(54.0f), s(42.0f), s(2.6f), 255);
        line(image, s(12.0f), s(48.0f), s(52.0f), s(48.0f), s(2.2f), 200);
        disc(image, s(32.0f), s(12.0f), s(3.0f), 255);
        return image;
    }

    private static NativeImage drawCube(boolean blast) {
        NativeImage image = create();
        float[][] p = {{32, 10}, {52, 22}, {52, 42}, {32, 54}, {12, 42}, {12, 22}};
        for (int i = 0; i < 6; i++) {
            int n = (i + 1) % 6;
            line(image, s(p[i][0]), s(p[i][1]), s(p[n][0]), s(p[n][1]), s(2.1f), 255);
        }
        line(image, s(32.0f), s(10.0f), s(32.0f), s(32.0f), s(1.6f), 180);
        line(image, s(12.0f), s(22.0f), s(32.0f), s(32.0f), s(1.6f), 180);
        line(image, s(52.0f), s(22.0f), s(32.0f), s(32.0f), s(1.6f), 180);
        if (blast) {
            for (int i = 0; i < 8; i++) {
                double a = i * (Math.PI / 4.0d);
                line(image, s(32.0f), s(32.0f), s(32.0f) + (float) Math.cos(a) * s(28.0f), s(32.0f) + (float) Math.sin(a) * s(28.0f), s(1.2f), 140);
            }
        }
        return image;
    }

    private static NativeImage drawSkull() {
        NativeImage image = create();
        disc(image, s(32.0f), s(28.0f), s(16.0f), 240);
        disc(image, s(32.0f), s(42.0f), s(10.0f), 220);
        disc(image, s(24.0f), s(27.0f), s(4.2f), 0);
        disc(image, s(40.0f), s(27.0f), s(4.2f), 0);
        line(image, s(26.0f), s(46.0f), s(26.0f), s(50.0f), s(1.4f), 255);
        line(image, s(32.0f), s(46.0f), s(32.0f), s(50.0f), s(1.4f), 255);
        line(image, s(38.0f), s(46.0f), s(38.0f), s(50.0f), s(1.4f), 255);
        return image;
    }

    private static NativeImage drawFirefly() {
        NativeImage image = create();
        float cx = (SIZE - 1) * 0.5f;
        float cy = cx;
        float maxR = SIZE * 0.48f;
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                float d = (float) Math.hypot(x - cx, y - cy) / maxR;
                float a = gauss(d, 0.16f) * 0.7f + gauss(d, 0.06f);
                plotF(image, x, y, a, 255, 255, 210);
            }
        }
        return image;
    }

    private static NativeImage drawHeart(boolean alt) {
        NativeImage image = create();
        float cx = (SIZE - 1) * 0.5f;
        float cy = SIZE * 0.54f;
        float scale = (alt ? 1.12f : 1.0f) * (SIZE / 64.0f);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                float px = ((x - cx) / 18.0f) / scale;
                float py = ((cy - y) / 18.0f) / scale;
                float v = (px * px + py * py - 1.0f);
                float heart = v * v * v - (px * px * py * py * py);
                float d = (float) Math.hypot(x - cx, y - cy);
                float glow = gauss(d / (SIZE * 0.5f), 0.22f) * 0.32f;
                if (heart <= 0.08f) {
                    float edge = clamp01(-heart * 7.0f);
                    plotF(image, x, y, Math.max(edge, glow), 255, 255, 255);
                } else {
                    plotF(image, x, y, glow * 0.45f, 255, 255, 255);
                }
            }
        }
        return image;
    }

    private static NativeImage drawLightning() {
        NativeImage image = create();
        float[] xs = {36, 28, 34, 22, 30, 18};
        float[] ys = {8, 22, 22, 36, 36, 56};
        for (int i = 0; i < xs.length - 1; i++) {
            line(image, s(xs[i]), s(ys[i]), s(xs[i + 1]), s(ys[i + 1]), s(3.2f), 90);
            line(image, s(xs[i]), s(ys[i]), s(xs[i + 1]), s(ys[i + 1]), s(1.6f), 255);
        }
        return image;
    }

    private static NativeImage drawSperm() {
        NativeImage image = create();
        float hx = s(46.0f);
        float hy = s(32.0f);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                float dx = (x - hx) / s(8.2f);
                float dy = (y - hy) / s(6.0f);
                float d = dx * dx + dy * dy;
                if (d < 1.4f) {
                    plotF(image, x, y, clamp01(1.0f - d) * 1.15f, 255, 255, 255);
                }
            }
        }
        disc(image, s(40.0f), s(32.0f), s(3.2f), 230);
        for (int i = 0; i <= 80; i++) {
            float t = i / 80.0f;
            float x = s(38.0f) - t * s(34.0f);
            float y = s(32.0f) + (float) Math.sin(t * 7.4d) * s(2.4f + t * 3.6f);
            float w = s(1.85f) * (1.0f - t * 0.82f);
            disc(image, x, y, Math.max(s(0.7f), w), Math.round(255.0f * (1.0f - t * 0.25f)));
        }
        return image;
    }

    private static void disc(NativeImage image, float cx, float cy, float radius, int alpha) {
        int minX = Math.max(0, (int) Math.floor(cx - radius - 2.0f));
        int maxX = Math.min(SIZE - 1, (int) Math.ceil(cx + radius + 2.0f));
        int minY = Math.max(0, (int) Math.floor(cy - radius - 2.0f));
        int maxY = Math.min(SIZE - 1, (int) Math.ceil(cy + radius + 2.0f));
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                float d = (float) Math.hypot(x - cx, y - cy);
                float t = 1.0f - (d / Math.max(0.001f, radius));
                if (t > 0.0f) {
                    float a = clamp01(t * 1.35f) * (alpha / 255.0f);
                    plotF(image, x, y, a, 255, 255, 255);
                }
            }
        }
    }

    private static void line(NativeImage image, float x0, float y0, float x1, float y1, float width, int alpha) {
        float dx = x1 - x0;
        float dy = y1 - y0;
        int steps = Math.max(2, Math.round((float) Math.hypot(dx, dy) * 3.0f));
        for (int i = 0; i <= steps; i++) {
            float t = i / (float) steps;
            disc(image, x0 + dx * t, y0 + dy * t, width, alpha);
        }
    }
}
