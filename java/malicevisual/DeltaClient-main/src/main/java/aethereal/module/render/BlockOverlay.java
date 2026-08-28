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
import aethereal.render.EasingList;
import aethereal.setting.BooleanSetting;
import aethereal.setting.ColorSetting;
import aethereal.setting.ModeSetting;
import aethereal.setting.SliderSetting;
import aethereal.ui.shader.NoiseShader;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import org.joml.Matrix4f;

@ModuleRegister(name = "Block Overlay", description = "Подсвечивает и выделяет блок под прицелом", category = Category.Render)
public class BlockOverlay extends Module {
    private final ModeSetting mode = new ModeSetting("Режим", "Оба", "Оба", "Заливка", "Контур", "Плоский", "Шейдер");
    private final BooleanSetting syncTheme = new BooleanSetting("Цвет из темы", true);
    private final ColorSetting customColor = new ColorSetting("Цвет", Integer.valueOf(ColorUtil.convertToARGB(0, 230, 255, 255)));
    private final SliderSetting fillAlpha = new SliderSetting("Прозрачность заливки", 0.25f, 0.05f, 1.0f, 0.05f).a(() -> {
        return Boolean.valueOf(!this.mode.l("Шейдер") && !this.mode.l("Контур"));
    });
    private final SliderSetting outlineAlpha = new SliderSetting("Прозрачность контура", 0.85f, 0.1f, 1.0f, 0.05f).a(() -> {
        return Boolean.valueOf(!this.mode.l("Шейдер") && !this.mode.l("Заливка") && !this.mode.l("Плоский"));
    });
    private final SliderSetting lineWidth = new SliderSetting("Толщина линий", 1.5f, 0.5f, 4.0f, 0.25f).a(() -> {
        return Boolean.valueOf(!this.mode.l("Шейдер") && !this.mode.l("Заливка") && !this.mode.l("Плоский"));
    });
    private final SliderSetting shaderAlpha = new SliderSetting("Непрозрачность шейдера", 0.6f, 0.0f, 1.0f, 0.05f).a(() -> {
        return Boolean.valueOf(this.mode.l("Шейдер"));
    });
    private final SliderSetting shaderSpeed = new SliderSetting("Скорость шейдера", 1.0f, 0.1f, 5.0f, 0.05f);
    private final BooleanSetting smoothMove = new BooleanSetting("Плавный переход", true);
    private final SliderSetting smoothSpeed = new SliderSetting("Скорость перехода", 9.0f, 2.0f, 20.0f, 0.5f).a(() -> {
        return this.smoothMove.c();
    });
    private final BooleanSetting throughWalls = new BooleanSetting("Сквозь стены", false);

    private final AnimationUtil fadeAnim = new AnimationUtil();
    private Box currentBox = null;
    private long lastFrameNs = System.nanoTime();

    public BlockOverlay() {
        a(this.mode, this.syncTheme, this.customColor, this.fillAlpha, this.outlineAlpha, this.lineWidth, this.shaderAlpha, this.shaderSpeed, this.smoothMove, this.smoothSpeed, this.throughWalls);
    }

    @EventTarget
    public void onDraw(DrawEvent event) {
        if (!event.c() || mc.world == null || mc.player == null) {
            this.lastFrameNs = System.nanoTime();
            return;
        }

        float dt = frameDelta();
        boolean hasTarget = false;
        Box targetBox = null;

        if (mc.crosshairTarget instanceof BlockHitResult hitResult && hitResult.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = hitResult.getBlockPos();
            BlockState state = mc.world.getBlockState(pos);
            if (!state.isAir() && mc.world.getWorldBorder().contains(pos)) {
                VoxelShape shape = state.getOutlineShape(mc.world, pos);
                if (!shape.isEmpty()) {
                    targetBox = shape.getBoundingBox().offset(pos);
                    hasTarget = true;
                }
            }
        }

        this.fadeAnim.a(hasTarget);
        this.fadeAnim.a(0.0f, 1.0f, 0.28f, EasingList.h, event.g());
        float alphaProgress = this.fadeAnim.c();

        if (alphaProgress <= 0.001f || (targetBox == null && this.currentBox == null)) {
            if (!hasTarget) {
                this.currentBox = null;
            }
            return;
        }

        if (targetBox != null) {
            this.currentBox = nextOverlayBox(this.currentBox, targetBox, dt);
        }

        if (this.currentBox == null) {
            return;
        }

        int baseColor = this.syncTheme.c().booleanValue()
                ? Skeleton.getInstance().getModuleProcessor().o().a(ThemeInfo.PRIMARY).toIntColor()
                : this.customColor.c().intValue();

        boolean isShader = this.mode.l("Шейдер");
        boolean isFill = this.mode.l("Оба") || this.mode.l("Заливка");
        boolean isOutline = this.mode.l("Оба") || this.mode.l("Контур");
        boolean isFlat = this.mode.l("Плоский");
        boolean wallHack = this.throughWalls.c().booleanValue();
        Box drawBox = this.currentBox.expand(0.002d);

        if (isShader) {
            float[] color = ColorUtil.a(baseColor);
            color[3] = this.shaderAlpha.c().floatValue() * alphaProgress;
            renderShaderOverlay(event.h(), drawBox, color, wallHack, this.shaderSpeed.c().floatValue());
            return;
        }

        float fillA = this.fillAlpha.c().floatValue() * alphaProgress;
        float outlineA = this.outlineAlpha.c().floatValue() * alphaProgress;
        int fillColor = ColorUtil.applyAlphaToColor(baseColor, fillA);
        int outlineColor = ColorUtil.applyAlphaToColor(baseColor, outlineA);
        renderOverlay(event.h(), drawBox, fillColor, outlineColor, this.lineWidth.c().floatValue(), isFill, isOutline, isFlat, wallHack);
    }

    private Box nextOverlayBox(Box current, Box target, float dt) {
        if (!this.smoothMove.c().booleanValue()) {
            return target;
        }
        if (current == null) {
            return scaledBox(target, 0.82d);
        }
        double speed = this.smoothSpeed.c().floatValue();
        double moveT = 1.0d - Math.exp(-speed * dt);
        double sizeT = 1.0d - Math.exp(-(speed * 0.82d) * dt);
        moveT = Math.min(1.0d, Math.max(0.0d, moveT));
        sizeT = Math.min(1.0d, Math.max(0.0d, sizeT));

        Vec3d from = current.getCenter();
        Vec3d to = target.getCenter();
        double cx = from.x + ((to.x - from.x) * moveT);
        double cy = from.y + ((to.y - from.y) * moveT);
        double cz = from.z + ((to.z - from.z) * moveT);

        double hx = lerp((current.maxX - current.minX) * 0.5d, (target.maxX - target.minX) * 0.5d, sizeT);
        double hy = lerp((current.maxY - current.minY) * 0.5d, (target.maxY - target.minY) * 0.5d, sizeT);
        double hz = lerp((current.maxZ - current.minZ) * 0.5d, (target.maxZ - target.minZ) * 0.5d, sizeT);

        Box next = new Box(cx - hx, cy - hy, cz - hz, cx + hx, cy + hy, cz + hz);
        double dx = next.getCenter().x - to.x;
        double dy = next.getCenter().y - to.y;
        double dz = next.getCenter().z - to.z;
        double sizeError = Math.abs((next.maxX - next.minX) - (target.maxX - target.minX))
                + Math.abs((next.maxY - next.minY) - (target.maxY - target.minY))
                + Math.abs((next.maxZ - next.minZ) - (target.maxZ - target.minZ));
        if (((dx * dx) + (dy * dy) + (dz * dz)) < 1.0E-6d && sizeError < 0.002d) {
            return target;
        }
        return next;
    }

    private float frameDelta() {
        long now = System.nanoTime();
        float dt = (now - this.lastFrameNs) / 1_000_000_000.0f;
        this.lastFrameNs = now;
        if (dt < 0.0f) {
            return 0.016f;
        }
        return Math.min(dt, 0.05f);
    }

    private static Box scaledBox(Box box, double scale) {
        Vec3d center = box.getCenter();
        double hx = (box.maxX - box.minX) * 0.5d * scale;
        double hy = (box.maxY - box.minY) * 0.5d * scale;
        double hz = (box.maxZ - box.minZ) * 0.5d * scale;
        return new Box(center.x - hx, center.y - hy, center.z - hz, center.x + hx, center.y + hy, center.z + hz);
    }

    private static double lerp(double from, double to, double t) {
        return from + ((to - from) * t);
    }

    private void renderShaderOverlay(MatrixStack matrices, Box box, float[] color, boolean wallHack, float speed) {
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d camPos = camera.getPos();
        double minX = box.minX - camPos.x;
        double minY = box.minY - camPos.y;
        double minZ = box.minZ - camPos.z;
        double maxX = box.maxX - camPos.x;
        double maxY = box.maxY - camPos.y;
        double maxZ = box.maxZ - camPos.z;
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        if (wallHack) {
            RenderSystem.disableDepthTest();
        } else {
            RenderSystem.enableDepthTest();
        }
        RenderSystem.depthMask(false);

        NoiseShader shader = Skeleton.getInstance().getModuleProcessor().i().f();
        shader.b(color, speed);
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        writeFillQuads(buffer, matrix, minX, minY, minZ, maxX, maxY, maxZ, 255, 255, 255, 255, false);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        shader.g();

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private void renderOverlay(MatrixStack matrices, Box box, int fillColor, int outlineColor, float lineW, boolean fill, boolean outline, boolean flat, boolean wallHack) {
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d camPos = camera.getPos();

        double minX = box.minX - camPos.x;
        double minY = box.minY - camPos.y;
        double minZ = box.minZ - camPos.z;
        double maxX = box.maxX - camPos.x;
        double maxY = flat ? (minY + 0.02) : (box.maxY - camPos.y);
        double maxZ = box.maxZ - camPos.z;

        Matrix4f matrix = matrices.peek().getPositionMatrix();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        if (wallHack) {
            RenderSystem.disableDepthTest();
        } else {
            RenderSystem.enableDepthTest();
        }
        RenderSystem.depthMask(false);

        if ((fill || flat) && (fillColor >> 24 & 0xFF) > 0) {
            RenderSystem.setShader(net.minecraft.client.gl.ShaderProgramKeys.POSITION_COLOR);
            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            int r = (fillColor >> 16) & 0xFF;
            int g = (fillColor >> 8) & 0xFF;
            int b = fillColor & 0xFF;
            int a = (fillColor >> 24) & 0xFF;
            writeFillQuads(buffer, matrix, minX, minY, minZ, maxX, maxY, maxZ, r, g, b, a, flat);
            BufferRenderer.drawWithGlobalProgram(buffer.end());
        }

        if (outline && (outlineColor >> 24 & 0xFF) > 0) {
            RenderSystem.setShader(net.minecraft.client.gl.ShaderProgramKeys.POSITION_COLOR);
            RenderSystem.lineWidth(lineW);
            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

            int r = (outlineColor >> 16) & 0xFF;
            int g = (outlineColor >> 8) & 0xFF;
            int b = outlineColor & 0xFF;
            int a = (outlineColor >> 24) & 0xFF;

            buffer.vertex(matrix, (float) minX, (float) minY, (float) minZ).color(r, g, b, a);
            buffer.vertex(matrix, (float) maxX, (float) minY, (float) minZ).color(r, g, b, a);

            buffer.vertex(matrix, (float) maxX, (float) minY, (float) minZ).color(r, g, b, a);
            buffer.vertex(matrix, (float) maxX, (float) minY, (float) maxZ).color(r, g, b, a);

            buffer.vertex(matrix, (float) maxX, (float) minY, (float) maxZ).color(r, g, b, a);
            buffer.vertex(matrix, (float) minX, (float) minY, (float) maxZ).color(r, g, b, a);

            buffer.vertex(matrix, (float) minX, (float) minY, (float) maxZ).color(r, g, b, a);
            buffer.vertex(matrix, (float) minX, (float) minY, (float) minZ).color(r, g, b, a);

            if (!flat) {
                buffer.vertex(matrix, (float) minX, (float) maxY, (float) minZ).color(r, g, b, a);
                buffer.vertex(matrix, (float) maxX, (float) maxY, (float) minZ).color(r, g, b, a);

                buffer.vertex(matrix, (float) maxX, (float) maxY, (float) minZ).color(r, g, b, a);
                buffer.vertex(matrix, (float) maxX, (float) maxY, (float) maxZ).color(r, g, b, a);

                buffer.vertex(matrix, (float) maxX, (float) maxY, (float) maxZ).color(r, g, b, a);
                buffer.vertex(matrix, (float) minX, (float) maxY, (float) maxZ).color(r, g, b, a);

                buffer.vertex(matrix, (float) minX, (float) maxY, (float) maxZ).color(r, g, b, a);
                buffer.vertex(matrix, (float) minX, (float) maxY, (float) minZ).color(r, g, b, a);

                buffer.vertex(matrix, (float) minX, (float) minY, (float) minZ).color(r, g, b, a);
                buffer.vertex(matrix, (float) minX, (float) maxY, (float) minZ).color(r, g, b, a);

                buffer.vertex(matrix, (float) maxX, (float) minY, (float) minZ).color(r, g, b, a);
                buffer.vertex(matrix, (float) maxX, (float) maxY, (float) minZ).color(r, g, b, a);

                buffer.vertex(matrix, (float) maxX, (float) minY, (float) maxZ).color(r, g, b, a);
                buffer.vertex(matrix, (float) maxX, (float) maxY, (float) maxZ).color(r, g, b, a);

                buffer.vertex(matrix, (float) minX, (float) minY, (float) maxZ).color(r, g, b, a);
                buffer.vertex(matrix, (float) minX, (float) maxY, (float) maxZ).color(r, g, b, a);
            }

            BufferRenderer.drawWithGlobalProgram(buffer.end());
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private void writeFillQuads(BufferBuilder buffer, Matrix4f matrix, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, int r, int g, int b, int a, boolean flat) {
        buffer.vertex(matrix, (float) minX, (float) minY, (float) minZ).color(r, g, b, a);
        buffer.vertex(matrix, (float) maxX, (float) minY, (float) minZ).color(r, g, b, a);
        buffer.vertex(matrix, (float) maxX, (float) minY, (float) maxZ).color(r, g, b, a);
        buffer.vertex(matrix, (float) minX, (float) minY, (float) maxZ).color(r, g, b, a);

        buffer.vertex(matrix, (float) minX, (float) maxY, (float) minZ).color(r, g, b, a);
        buffer.vertex(matrix, (float) minX, (float) maxY, (float) maxZ).color(r, g, b, a);
        buffer.vertex(matrix, (float) maxX, (float) maxY, (float) maxZ).color(r, g, b, a);
        buffer.vertex(matrix, (float) maxX, (float) maxY, (float) minZ).color(r, g, b, a);

        if (!flat) {
            buffer.vertex(matrix, (float) minX, (float) minY, (float) minZ).color(r, g, b, a);
            buffer.vertex(matrix, (float) minX, (float) maxY, (float) minZ).color(r, g, b, a);
            buffer.vertex(matrix, (float) maxX, (float) maxY, (float) minZ).color(r, g, b, a);
            buffer.vertex(matrix, (float) maxX, (float) minY, (float) minZ).color(r, g, b, a);

            buffer.vertex(matrix, (float) minX, (float) minY, (float) maxZ).color(r, g, b, a);
            buffer.vertex(matrix, (float) maxX, (float) minY, (float) maxZ).color(r, g, b, a);
            buffer.vertex(matrix, (float) maxX, (float) maxY, (float) maxZ).color(r, g, b, a);
            buffer.vertex(matrix, (float) minX, (float) maxY, (float) maxZ).color(r, g, b, a);

            buffer.vertex(matrix, (float) minX, (float) minY, (float) minZ).color(r, g, b, a);
            buffer.vertex(matrix, (float) minX, (float) minY, (float) maxZ).color(r, g, b, a);
            buffer.vertex(matrix, (float) minX, (float) maxY, (float) maxZ).color(r, g, b, a);
            buffer.vertex(matrix, (float) minX, (float) maxY, (float) minZ).color(r, g, b, a);

            buffer.vertex(matrix, (float) maxX, (float) minY, (float) minZ).color(r, g, b, a);
            buffer.vertex(matrix, (float) maxX, (float) maxY, (float) minZ).color(r, g, b, a);
            buffer.vertex(matrix, (float) maxX, (float) maxY, (float) maxZ).color(r, g, b, a);
            buffer.vertex(matrix, (float) maxX, (float) minY, (float) maxZ).color(r, g, b, a);
        }
    }
}
