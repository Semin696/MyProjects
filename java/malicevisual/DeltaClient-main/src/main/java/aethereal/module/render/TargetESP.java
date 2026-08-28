package aethereal.module.render;

import aethereal.config.ThemeInfo;
import aethereal.core.Category;
import aethereal.core.EventTarget;
import aethereal.core.Module;
import aethereal.core.ModuleRegister;
import aethereal.core.Skeleton;
import aethereal.event.DrawEvent;
import aethereal.render.AnimationUtil;
import aethereal.render.ColorUtil;
import aethereal.render.Draw2DProcessor;
import aethereal.render.EasingList;
import aethereal.render.Fonts;
import aethereal.setting.BooleanSetting;
import aethereal.setting.ColorSetting;
import aethereal.setting.ModeSetting;
import aethereal.setting.MultiModeSetting;
import aethereal.setting.SliderSetting;
import aethereal.util.MathUtil;
import aethereal.util.ProjectUtil;
import aethereal.util.ServerUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

@ModuleRegister(name = "Target ESP", description = "Красивая подсветка сущности под прицелом", category = Category.Render)
public class TargetESP extends Module {
    private final ModeSetting mode = new ModeSetting("Маркер", "Кольцо",
            "Кольцо", "Орбита", "Клетка", "Крест", "Круг", "Ромб", "Углы", "Прицел", "Шестиугольник");
    private final MultiModeSetting targets = new MultiModeSetting("Цели",
            new BooleanSetting("Игроки", true),
            new BooleanSetting("Мобы", true),
            new BooleanSetting("Животные", false));
    private final BooleanSetting syncTheme = new BooleanSetting("Цвет из темы", true);
    private final ColorSetting customColor = new ColorSetting("Цвет", Integer.valueOf(ColorUtil.convertToARGB(220, 30, 55, 255))).a(() -> {
        return Boolean.valueOf(!this.syncTheme.c().booleanValue());
    });
    private final SliderSetting scale = new SliderSetting("Размер", 1.0f, 0.5f, 2.0f, 0.05f);
    private final SliderSetting thickness = new SliderSetting("Толщина", 1.35f, 0.5f, 3.0f, 0.05f).a(() -> {
        return Boolean.valueOf(!isWorldMode());
    });
    private final BooleanSetting rotate = new BooleanSetting("Вращение", true);
    private final SliderSetting rotateSpeed = new SliderSetting("Скорость вращения", 1.15f, 0.1f, 4.0f, 0.1f).a(() -> {
        return this.rotate.c();
    });
    private final BooleanSetting throughWalls = new BooleanSetting("Сквозь стены", true).a(() -> {
        return Boolean.valueOf(isWorldMode());
    });
    private final BooleanSetting showName = new BooleanSetting("Имя и здоровье", true);

    private final AnimationUtil fade = new AnimationUtil();
    private LivingEntity lastTarget;

    public TargetESP() {
        a(this.mode, this.targets, this.syncTheme, this.customColor, this.scale, this.thickness, this.rotate, this.rotateSpeed, this.throughWalls, this.showName);
    }

    private boolean isWorldMode() {
        return this.mode.l("Кольцо") || this.mode.l("Орбита") || this.mode.l("Клетка");
    }

    @EventTarget
    public void onDraw(DrawEvent event) {
        if (mc.world == null || mc.player == null) {
            return;
        }

        LivingEntity target = resolveTarget();
        if (target != null) {
            this.lastTarget = target;
        }
        this.fade.a(target != null);
        this.fade.a(0.0f, 1.0f, 0.28f, EasingList.h, event.g());
        float alpha = this.fade.c();
        if (alpha <= 0.01f || this.lastTarget == null || this.lastTarget.isRemoved()) {
            if (alpha <= 0.01f) {
                this.lastTarget = null;
            }
            return;
        }

        LivingEntity living = this.lastTarget;
        int base = this.syncTheme.c().booleanValue()
                ? Skeleton.getInstance().getModuleProcessor().o().a(ThemeInfo.PRIMARY).toIntColor()
                : this.customColor.c().intValue();

        if (event.c() && isWorldMode()) {
            renderWorld(event.h(), living, base, alpha, event.g());
            return;
        }

        if (!event.b() || mc.options.hudHidden) {
            return;
        }

        Vec3d interp = MathUtil.a(living, event.g());
        Box box = living.getBoundingBox().offset(interp.subtract(living.getPos()));
        float[] bounds = ProjectUtil.getBounds(box);
        if (bounds == null) {
            return;
        }

        float cx = (bounds[0] + bounds[2]) * 0.5f;
        float cy = (bounds[1] + bounds[3]) * 0.5f;
        float bw = Math.max(18.0f, bounds[2] - bounds[0]);
        float bh = Math.max(22.0f, bounds[3] - bounds[1]);
        float pulse = 1.0f + (0.045f * (float) Math.sin(System.currentTimeMillis() / 260.0d));
        float size = Math.max(bw, bh) * 0.52f * this.scale.c().floatValue() * pulse;
        float angle = this.rotate.c().booleanValue()
                ? ((System.currentTimeMillis() % 20000L) / 20000.0f) * 360.0f * this.rotateSpeed.c().floatValue()
                : 0.0f;

        int color = ColorUtil.applyAlphaToColor(base, alpha);
        int glow = ColorUtil.applyAlphaToColor(base, 0.28f * alpha);
        int soft = ColorUtil.applyAlphaToColor(base, 0.12f * alpha);
        float line = this.thickness.c().floatValue();
        Draw2DProcessor draw = event.getDraw2DProcessor();
        MatrixStack matrices = event.h();

        if (!isWorldMode()) {
            if (this.mode.l("Углы")) {
                drawCorners(draw, matrices, bounds[0], bounds[1], bounds[2], bounds[3], size * 0.32f, line, color, glow, soft);
            } else {
                matrices.push();
                matrices.translate(cx, cy, 0.0f);
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(angle));
                if (this.mode.l("Крест")) {
                    drawCross(draw, matrices, size, line, color, glow, soft);
                } else if (this.mode.l("Круг")) {
                    drawPolygon(draw, matrices, size, 36, line, color, glow, soft);
                    drawPolygon(draw, matrices, size * 0.18f, 16, line * 0.7f, color, glow, soft);
                } else if (this.mode.l("Ромб")) {
                    drawPolygon(draw, matrices, size, 4, line, color, glow, soft);
                } else if (this.mode.l("Прицел")) {
                    drawScope(draw, matrices, size, line, color, glow, soft);
                } else if (this.mode.l("Шестиугольник")) {
                    drawPolygon(draw, matrices, size, 6, line, color, glow, soft);
                }
                matrices.pop();
            }
        }

        if (this.showName.c().booleanValue()) {
            drawTag(draw, matrices, living, cx, bounds[3] + 5.0f, base, alpha);
        }
    }

    private void renderWorld(MatrixStack matrices, LivingEntity living, int base, float alpha, float tickDelta) {
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d camPos = camera.getPos();
        Vec3d interp = MathUtil.a(living, tickDelta);
        float width = living.getWidth() * 0.78f * this.scale.c().floatValue();
        float height = living.getHeight();
        float time = ((System.currentTimeMillis() % 100000L) / 1000.0f) * (this.rotate.c().booleanValue() ? this.rotateSpeed.c().floatValue() : 0.0f);
        float[] rgb = ColorUtil.a(base);
        int r = Math.round(rgb[0] * 255.0f);
        int g = Math.round(rgb[1] * 255.0f);
        int b = Math.round(rgb[2] * 255.0f);
        int aFill = Math.round(70.0f * alpha);
        int aRing = Math.round(235.0f * alpha);
        int aSoft = Math.round(110.0f * alpha);
        int aGlow = Math.round(55.0f * alpha);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        if (this.throughWalls.c().booleanValue()) {
            RenderSystem.disableDepthTest();
        } else {
            RenderSystem.enableDepthTest();
        }
        RenderSystem.depthMask(false);
        RenderSystem.setShader(net.minecraft.client.gl.ShaderProgramKeys.POSITION_COLOR);

        matrices.push();
        matrices.translate(interp.x - camPos.x, interp.y - camPos.y, interp.z - camPos.z);

        BufferBuilder quads = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float wave = (float) Math.sin(time * 2.4d) * 0.04f;

        if (this.mode.l("Клетка")) {
            writeDisc(quads, matrix, 0.02f, width * 0.92f, 28, r, g, b, aFill);
            writeAnnulus(quads, matrix, 0.04f, width * 0.82f, width * 0.98f, 32, 0.0f, r, g, b, aRing, aSoft);
            writeAnnulus(quads, matrix, height - 0.04f, width * 0.78f, width * 0.94f, 32, 0.0f, r, g, b, aRing, aSoft);
            writeBeams(quads, matrix, width * 0.9f, height, 8, r, g, b, aRing);
        } else {
            writeDisc(quads, matrix, 0.015f, width * 1.18f, 36, r, g, b, aFill);
            writeTorus(quads, matrix, 0.04f + wave, width * 1.12f, 0.16f, 40, 10, time, r, g, b, aGlow);
            writeTorus(quads, matrix, 0.05f, width * 0.96f, 0.085f, 42, 12, time, r, g, b, aRing);
            writeTorus(quads, matrix, 0.055f, width * 0.82f, 0.038f, 36, 8, -time * 1.15f, 255, 255, 255, Math.round(160.0f * alpha));
            writeAnnulus(quads, matrix, height * 0.52f + wave * 0.5f, width * 0.62f, width * 0.82f, 36, -time * 1.35f, r, g, b, Math.round(150.0f * alpha), aSoft);
            writeTorus(quads, matrix, height * 0.08f, width * 0.55f, 0.045f, 28, 8, time * 1.8f, r, g, b, Math.round(120.0f * alpha));
            writeSouls(quads, matrix, width, height, time, r, g, b, aRing, this.mode.l("Орбита") ? 10 : 7);
        }
        BufferRenderer.drawWithGlobalProgram(quads.end());

        matrices.pop();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private void writeDisc(BufferBuilder buffer, Matrix4f matrix, float y, float radius, int segments, int r, int g, int b, int a) {
        double step = (Math.PI * 2.0d) / segments;
        for (int i = 0; i < segments; i++) {
            double a1 = i * step;
            double a2 = (i + 1) * step;
            vert(buffer, matrix, 0.0f, y, 0.0f, r, g, b, a);
            vert(buffer, matrix, (float) (Math.cos(a1) * radius), y, (float) (Math.sin(a1) * radius), r, g, b, 0);
            vert(buffer, matrix, (float) (Math.cos(a2) * radius), y, (float) (Math.sin(a2) * radius), r, g, b, 0);
            vert(buffer, matrix, 0.0f, y, 0.0f, r, g, b, a);
        }
    }

    private void writeAnnulus(BufferBuilder buffer, Matrix4f matrix, float y, float inner, float outer, int segments, float rot, int r, int g, int b, int aInner, int aOuter) {
        double step = (Math.PI * 2.0d) / segments;
        float hi = 0.018f;
        for (int i = 0; i < segments; i++) {
            double a1 = rot + (i * step);
            double a2 = rot + ((i + 1) * step);
            float i1x = (float) (Math.cos(a1) * inner);
            float i1z = (float) (Math.sin(a1) * inner);
            float o1x = (float) (Math.cos(a1) * outer);
            float o1z = (float) (Math.sin(a1) * outer);
            float i2x = (float) (Math.cos(a2) * inner);
            float i2z = (float) (Math.sin(a2) * inner);
            float o2x = (float) (Math.cos(a2) * outer);
            float o2z = (float) (Math.sin(a2) * outer);

            vert(buffer, matrix, i1x, y, i1z, r, g, b, aInner);
            vert(buffer, matrix, o1x, y, o1z, r, g, b, aOuter);
            vert(buffer, matrix, o2x, y, o2z, r, g, b, aOuter);
            vert(buffer, matrix, i2x, y, i2z, r, g, b, aInner);

            vert(buffer, matrix, o1x, y, o1z, r, g, b, aOuter);
            vert(buffer, matrix, o1x, y + hi, o1z, r, g, b, aInner);
            vert(buffer, matrix, o2x, y + hi, o2z, r, g, b, aInner);
            vert(buffer, matrix, o2x, y, o2z, r, g, b, aOuter);
        }
    }

    private void writeBeams(BufferBuilder buffer, Matrix4f matrix, float radius, float height, int count, int r, int g, int b, int a) {
        float beam = 0.018f;
        for (int i = 0; i < count; i++) {
            double angle = i * (Math.PI * 2.0d / count);
            float x = (float) (Math.cos(angle) * radius);
            float z = (float) (Math.sin(angle) * radius);
            writeBox(buffer, matrix, x, height * 0.5f, z, beam, height * 0.5f, beam, r, g, b, a);
        }
    }

    private void writeOrbit(BufferBuilder buffer, Matrix4f matrix, float radius, float height, float time, int r, int g, int b, int a) {
        writeSouls(buffer, matrix, radius, height, time, r, g, b, a, 8);
    }

    private void writeSouls(BufferBuilder buffer, Matrix4f matrix, float radius, float height, float time, int r, int g, int b, int a, int count) {
        for (int i = 0; i < count; i++) {
            double angle = time * 1.55f + (i * (Math.PI * 2.0d / count));
            float orbit = radius * (0.92f + 0.12f * (i % 2));
            float y = 0.10f + (height * (0.12f + (0.78f * ((float) (Math.sin(time * 1.7f + i * 0.9f) * 0.5d + 0.5d)))));
            float x = (float) (Math.cos(angle) * orbit);
            float z = (float) (Math.sin(angle) * orbit);
            float size = 0.055f + (0.018f * (i % 3));
            writeSphere(buffer, matrix, x, y, z, size * 1.85f, 6, r, g, b, Math.max(40, a / 4));
            writeSphere(buffer, matrix, x, y, z, size, 6, 255, 245, 255, a);
            writeBox(buffer, matrix, x, y + size * 1.6f, z, size * 0.22f, size * 0.9f, size * 0.22f, r, g, b, Math.round(a * 0.7f));
        }
        int wisps = Math.max(4, count - 2);
        for (int i = 0; i < wisps; i++) {
            double angle = -time * 1.1f + (i * (Math.PI * 2.0d / wisps));
            float y = 0.08f + (height * ((i / (float) wisps) * 0.85f));
            float x = (float) (Math.cos(angle) * radius * 0.7f);
            float z = (float) (Math.sin(angle) * radius * 0.7f);
            writeBox(buffer, matrix, x, y, z, 0.018f, 0.07f, 0.018f, r, g, b, Math.round(a * 0.55f));
        }
    }

    private void writeTorus(BufferBuilder buffer, Matrix4f matrix, float y, float major, float minor, int ring, int tube, float rot, int r, int g, int b, int a) {
        double us = (Math.PI * 2.0d) / ring;
        double vs = (Math.PI * 2.0d) / tube;
        for (int i = 0; i < ring; i++) {
            double u0 = rot + i * us;
            double u1 = rot + (i + 1) * us;
            float wave0 = (float) Math.sin(u0 * 3.0d + rot * 2.0d) * 0.035f;
            float wave1 = (float) Math.sin(u1 * 3.0d + rot * 2.0d) * 0.035f;
            for (int j = 0; j < tube; j++) {
                double v0 = j * vs;
                double v1 = (j + 1) * vs;
                float[] p00 = torusPoint(0.0f, y + wave0, 0.0f, major, minor, u0, v0);
                float[] p10 = torusPoint(0.0f, y + wave1, 0.0f, major, minor, u1, v0);
                float[] p11 = torusPoint(0.0f, y + wave1, 0.0f, major, minor, u1, v1);
                float[] p01 = torusPoint(0.0f, y + wave0, 0.0f, major, minor, u0, v1);
                int shade = (int) (a * (0.5f + 0.5f * (float) ((Math.cos(v0) + 1.0d) * 0.5d)));
                vert(buffer, matrix, p00[0], p00[1], p00[2], r, g, b, shade);
                vert(buffer, matrix, p10[0], p10[1], p10[2], r, g, b, shade);
                vert(buffer, matrix, p11[0], p11[1], p11[2], r, g, b, shade);
                vert(buffer, matrix, p01[0], p01[1], p01[2], r, g, b, shade);
            }
        }
    }

    private float[] torusPoint(float cx, float cy, float cz, float major, float minor, double u, double v) {
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

    private void writeSphere(BufferBuilder buffer, Matrix4f matrix, float cx, float cy, float cz, float radius, int seg, int r, int g, int b, int a) {
        for (int i = 0; i < seg; i++) {
            double t0 = (i / (double) seg) * Math.PI;
            double t1 = ((i + 1) / (double) seg) * Math.PI;
            for (int j = 0; j < seg; j++) {
                double p0 = (j / (double) seg) * Math.PI * 2.0d;
                double p1 = ((j + 1) / (double) seg) * Math.PI * 2.0d;
                vert(buffer, matrix, cx + radius * (float) (Math.sin(t0) * Math.cos(p0)), cy + radius * (float) Math.cos(t0), cz + radius * (float) (Math.sin(t0) * Math.sin(p0)), r, g, b, a);
                vert(buffer, matrix, cx + radius * (float) (Math.sin(t1) * Math.cos(p0)), cy + radius * (float) Math.cos(t1), cz + radius * (float) (Math.sin(t1) * Math.sin(p0)), r, g, b, a);
                vert(buffer, matrix, cx + radius * (float) (Math.sin(t1) * Math.cos(p1)), cy + radius * (float) Math.cos(t1), cz + radius * (float) (Math.sin(t1) * Math.sin(p1)), r, g, b, a);
                vert(buffer, matrix, cx + radius * (float) (Math.sin(t0) * Math.cos(p1)), cy + radius * (float) Math.cos(t0), cz + radius * (float) (Math.sin(t0) * Math.sin(p1)), r, g, b, a);
            }
        }
    }

    private void writeBox(BufferBuilder buffer, Matrix4f matrix, float cx, float cy, float cz, float hx, float hy, float hz, int r, int g, int b, int a) {
        float minX = cx - hx;
        float maxX = cx + hx;
        float minY = cy - hy;
        float maxY = cy + hy;
        float minZ = cz - hz;
        float maxZ = cz + hz;
        vert(buffer, matrix, minX, minY, minZ, r, g, b, a);
        vert(buffer, matrix, maxX, minY, minZ, r, g, b, a);
        vert(buffer, matrix, maxX, minY, maxZ, r, g, b, a);
        vert(buffer, matrix, minX, minY, maxZ, r, g, b, a);

        vert(buffer, matrix, minX, maxY, minZ, r, g, b, a);
        vert(buffer, matrix, minX, maxY, maxZ, r, g, b, a);
        vert(buffer, matrix, maxX, maxY, maxZ, r, g, b, a);
        vert(buffer, matrix, maxX, maxY, minZ, r, g, b, a);

        vert(buffer, matrix, minX, minY, minZ, r, g, b, a);
        vert(buffer, matrix, minX, maxY, minZ, r, g, b, a);
        vert(buffer, matrix, maxX, maxY, minZ, r, g, b, a);
        vert(buffer, matrix, maxX, minY, minZ, r, g, b, a);

        vert(buffer, matrix, minX, minY, maxZ, r, g, b, a);
        vert(buffer, matrix, maxX, minY, maxZ, r, g, b, a);
        vert(buffer, matrix, maxX, maxY, maxZ, r, g, b, a);
        vert(buffer, matrix, minX, maxY, maxZ, r, g, b, a);

        vert(buffer, matrix, minX, minY, minZ, r, g, b, a);
        vert(buffer, matrix, minX, minY, maxZ, r, g, b, a);
        vert(buffer, matrix, minX, maxY, maxZ, r, g, b, a);
        vert(buffer, matrix, minX, maxY, minZ, r, g, b, a);

        vert(buffer, matrix, maxX, minY, minZ, r, g, b, a);
        vert(buffer, matrix, maxX, maxY, minZ, r, g, b, a);
        vert(buffer, matrix, maxX, maxY, maxZ, r, g, b, a);
        vert(buffer, matrix, maxX, minY, maxZ, r, g, b, a);
    }

    private void vert(BufferBuilder buffer, Matrix4f matrix, float x, float y, float z, int r, int g, int b, int a) {
        buffer.vertex(matrix, x, y, z).color(r, g, b, Math.max(0, Math.min(255, a)));
    }

    private void drawTag(Draw2DProcessor draw, MatrixStack matrices, LivingEntity living, float cx, float y, int base, float alpha) {
        String name = living.getName().getString();
        float hp = ServerUtil.a.a$(living);
        float maxHp = Math.max(1.0f, living.getMaxHealth());
        float ratio = MathUtil.b(hp / maxHp, 0.0f, 1.0f);
        String hpText = ((int) hp) + " HP";
        String dist = String.format("%.1fm", Float.valueOf(mc.player.distanceTo(living)));
        float nameW = Fonts.d.a(name, 7.0f);
        float hpW = Fonts.c.a(hpText, 6.0f);
        float distW = Fonts.c.a(dist, 5.5f);
        float width = Math.max(86.0f, nameW + hpW + 22.0f);
        float x = cx - (width * 0.5f);
        int hpColor = hpColor(ratio);

        draw.a(matrices, x, y, width, 20.0f, 4.5f, ColorUtil.convertToARGB(8, 9, 13, (int) (200 * alpha)));
        draw.a(matrices, x, y, width, 20.0f, 4.5f, 0.55f, ColorUtil.applyAlphaToColor(base, 0.35f * alpha));
        draw.a(matrices, x + 1.5f, y + 3.0f, 1.6f, 14.0f, 0.8f, ColorUtil.applyAlphaToColor(base, 0.95f * alpha));
        Fonts.d.a(matrices, name, x + 6.0f, y + 2.5f, 7.0f, ColorUtil.applyAlphaToColor(ColorUtil.convertToARGB(255, 255, 255, 255), alpha));
        Fonts.c.a(matrices, hpText, (x + width) - hpW - 6.0f, y + 3.0f, 6.0f, ColorUtil.applyAlphaToColor(hpColor, alpha));
        draw.a(matrices, x + 6.0f, y + 12.5f, width - 12.0f, 3.2f, 1.5f, ColorUtil.convertToARGB(22, 24, 32, (int) (230 * alpha)));
        if (ratio > 0.01f) {
            draw.a(matrices, x + 6.0f, y + 12.5f, Math.max(3.2f, (width - 12.0f) * ratio), 3.2f, 1.5f, ColorUtil.applyAlphaToColor(hpColor, alpha));
        }
        Fonts.c.a(matrices, dist, cx - (distW * 0.5f), y + 21.0f, 5.5f, ColorUtil.applyAlphaToColor(base, 0.88f * alpha));
    }

    private static int hpColor(float ratio) {
        if (ratio > 0.5f) {
            return ColorUtil.lerpColor(ColorUtil.convertToARGB(255, 196, 72, 255), ColorUtil.convertToARGB(80, 220, 130, 255), (ratio - 0.5f) * 2.0f);
        }
        return ColorUtil.lerpColor(ColorUtil.convertToARGB(230, 64, 78, 255), ColorUtil.convertToARGB(255, 196, 72, 255), ratio * 2.0f);
    }

    private LivingEntity resolveTarget() {
        if (!(mc.crosshairTarget instanceof EntityHitResult hit) || !(hit.getEntity() instanceof LivingEntity living)) {
            return null;
        }
        if (living == mc.player || !living.isAlive()) {
            return null;
        }
        if (living instanceof PlayerEntity player) {
            if (Skeleton.getInstance().getModuleProcessor().e().skipEsp() && Skeleton.getInstance().getModuleProcessor().e().d(player.getName().getString())) {
                return null;
            }
            return this.targets.a("Игроки").c().booleanValue() ? living : null;
        }
        if (living instanceof HostileEntity) {
            return this.targets.a("Мобы").c().booleanValue() ? living : null;
        }
        if (living instanceof PassiveEntity) {
            return this.targets.a("Животные").c().booleanValue() ? living : null;
        }
        return this.targets.a("Мобы").c().booleanValue() ? living : null;
    }

    private void drawCross(Draw2DProcessor draw, MatrixStack matrices, float size, float line, int color, int glow, int soft) {
        float arm = size * 0.74f;
        float gap = size * 0.18f;
        fill(draw, matrices, -line * 1.8f, -arm, line * 3.6f, arm - gap, soft);
        fill(draw, matrices, -arm, -line * 1.8f, arm - gap, line * 3.6f, soft);
        fill(draw, matrices, -line, -arm, line * 2.0f, arm - gap, glow);
        fill(draw, matrices, -line, gap, line * 2.0f, arm - gap, glow);
        fill(draw, matrices, -arm, -line, arm - gap, line * 2.0f, glow);
        fill(draw, matrices, gap, -line, arm - gap, line * 2.0f, glow);
        fill(draw, matrices, -line * 0.5f, -arm, line, arm - gap, color);
        fill(draw, matrices, -line * 0.5f, gap, line, arm - gap, color);
        fill(draw, matrices, -arm, -line * 0.5f, arm - gap, line, color);
        fill(draw, matrices, gap, -line * 0.5f, arm - gap, line, color);
        fill(draw, matrices, -2.1f, -2.1f, 4.2f, 4.2f, color);
    }

    private void drawScope(Draw2DProcessor draw, MatrixStack matrices, float size, float line, int color, int glow, int soft) {
        drawPolygon(draw, matrices, size * 0.46f, 28, line, color, glow, soft);
        drawPolygon(draw, matrices, size * 0.22f, 20, line * 0.75f, color, glow, soft);
        float arm = size * 0.8f;
        fill(draw, matrices, -line * 0.5f, -arm, line, size * 0.2f, color);
        fill(draw, matrices, -line * 0.5f, arm - size * 0.2f, line, size * 0.2f, color);
        fill(draw, matrices, -arm, -line * 0.5f, size * 0.2f, line, color);
        fill(draw, matrices, arm - size * 0.2f, -line * 0.5f, size * 0.2f, line, color);
    }

    private void drawPolygon(Draw2DProcessor draw, MatrixStack matrices, float radius, int segments, float line, int color, int glow, int soft) {
        double step = (Math.PI * 2.0d) / segments;
        double start = segments == 4 ? (Math.PI / 4.0d) : 0.0d;
        for (int i = 0; i < segments; i++) {
            double a1 = start + (i * step);
            double a2 = start + ((i + 1) * step);
            float x1 = (float) (Math.cos(a1) * radius);
            float y1 = (float) (Math.sin(a1) * radius);
            float x2 = (float) (Math.cos(a2) * radius);
            float y2 = (float) (Math.sin(a2) * radius);
            line(draw, matrices, x1, y1, x2, y2, line + 2.4f, soft);
            line(draw, matrices, x1, y1, x2, y2, line + 1.2f, glow);
            line(draw, matrices, x1, y1, x2, y2, line, color);
        }
    }

    private void drawCorners(Draw2DProcessor draw, MatrixStack matrices, float x1, float y1, float x2, float y2, float len, float line, int color, int glow, int soft) {
        float l = Math.max(7.0f, len);
        corner(draw, matrices, x1, y1, l, line, 1.0f, 1.0f, color, glow, soft);
        corner(draw, matrices, x2, y1, l, line, -1.0f, 1.0f, color, glow, soft);
        corner(draw, matrices, x1, y2, l, line, 1.0f, -1.0f, color, glow, soft);
        corner(draw, matrices, x2, y2, l, line, -1.0f, -1.0f, color, glow, soft);
    }

    private void corner(Draw2DProcessor draw, MatrixStack matrices, float x, float y, float len, float line, float dx, float dy, int color, int glow, int soft) {
        fill(draw, matrices, x, y, (len + 2.0f) * dx, (line + 2.2f) * dy, soft);
        fill(draw, matrices, x, y, (line + 2.2f) * dx, (len + 2.0f) * dy, soft);
        fill(draw, matrices, x, y, len * dx, (line + 1.1f) * dy, glow);
        fill(draw, matrices, x, y, (line + 1.1f) * dx, len * dy, glow);
        fill(draw, matrices, x, y, len * dx, line * dy, color);
        fill(draw, matrices, x, y, line * dx, len * dy, color);
    }

    private void line(Draw2DProcessor draw, MatrixStack matrices, float x1, float y1, float x2, float y2, float thickness, int color) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = (float) Math.hypot(dx, dy);
        if (len < 0.01f) {
            return;
        }
        matrices.push();
        matrices.translate(x1, y1, 0.0f);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotation((float) Math.atan2(dy, dx)));
        fill(draw, matrices, 0.0f, -thickness * 0.5f, len, thickness, color);
        matrices.pop();
    }

    private void fill(Draw2DProcessor draw, MatrixStack matrices, float x, float y, float w, float h, int color) {
        float rx = w < 0.0f ? x + w : x;
        float ry = h < 0.0f ? y + h : y;
        draw.a(matrices, rx, ry, Math.abs(w), Math.abs(h), 0.35f, color);
    }
}
