package aethereal.module.render;

import aethereal.config.ThemeInfo;
import aethereal.core.Category;
import aethereal.core.EventTarget;
import aethereal.core.Module;
import aethereal.core.ModuleRegister;
import aethereal.core.Skeleton;
import aethereal.event.DrawEvent;
import aethereal.render.ColorUtil;
import aethereal.setting.BooleanSetting;
import aethereal.setting.ColorSetting;
import aethereal.setting.MultiModeSetting;
import aethereal.setting.SliderSetting;
import aethereal.util.MathUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

@ModuleRegister(name = "Custom Hitbox", description = "Рисует хитбоксы игроков, мобов и предметов", category = Category.Render)
public class CustomHitbox extends Module {
    private final MultiModeSetting targets = new MultiModeSetting("Цели",
            new BooleanSetting("Игроки", true),
            new BooleanSetting("Мобы", true),
            new BooleanSetting("Предметы", false));
    private final BooleanSetting syncTheme = new BooleanSetting("Цвет из темы", true);
    private final ColorSetting customColor = new ColorSetting("Цвет", Integer.valueOf(ColorUtil.convertToARGB(224, 92, 208, 255))).a(() -> {
        return Boolean.valueOf(!this.syncTheme.c().booleanValue());
    });
    private final SliderSetting fillAlpha = new SliderSetting("Заливка", 0.18f, 0.0f, 0.6f, 0.02f);
    private final SliderSetting lineWidth = new SliderSetting("Толщина линий", 1.8f, 0.5f, 4.0f, 0.1f);
    private final BooleanSetting throughWalls = new BooleanSetting("Сквозь стены", false);

    public CustomHitbox() {
        a(this.targets, this.syncTheme, this.customColor, this.fillAlpha, this.lineWidth, this.throughWalls);
    }

    @EventTarget
    public void onDraw(DrawEvent event) {
        if (!event.c() || mc.world == null || mc.player == null || mc.gameRenderer == null) {
            return;
        }
        int base = this.syncTheme.c().booleanValue()
                ? Skeleton.getInstance().getModuleProcessor().o().a(ThemeInfo.PRIMARY).toIntColor()
                : this.customColor.c().intValue();
        int[] rgb = ColorUtil.b(base);
        int fillA = Math.round(this.fillAlpha.c().floatValue() * 255.0f);
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d cam = camera.getPos();
        MatrixStack matrices = event.h();
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        if (this.throughWalls.c().booleanValue()) {
            RenderSystem.disableDepthTest();
        } else {
            RenderSystem.enableDepthTest();
        }
        RenderSystem.depthMask(false);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        if (fillA > 0) {
            BufferBuilder fill = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            for (Entity entity : mc.world.getEntities()) {
                if (shouldDraw(entity)) {
                    Box box = interpolatedBox(entity, event.g()).offset(-cam.x, -cam.y, -cam.z).expand(0.002d);
                    writeFill(fill, matrix, box, rgb[0], rgb[1], rgb[2], fillA);
                }
            }
            BufferRenderer.drawWithGlobalProgram(fill.end());
        }

        RenderSystem.lineWidth(this.lineWidth.c().floatValue());
        BufferBuilder lines = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        for (Entity entity : mc.world.getEntities()) {
            if (shouldDraw(entity)) {
                Box box = interpolatedBox(entity, event.g()).offset(-cam.x, -cam.y, -cam.z).expand(0.002d);
                writeOutline(lines, matrix, box, rgb[0], rgb[1], rgb[2], 220);
            }
        }
        BufferRenderer.drawWithGlobalProgram(lines.end());

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.lineWidth(1.0f);
        RenderSystem.disableBlend();
    }

    private boolean shouldDraw(Entity entity) {
        if (entity == null || entity == mc.player || !entity.isAlive()) {
            return false;
        }
        if (entity instanceof PlayerEntity) {
            return this.targets.a("Игроки").c().booleanValue();
        }
        if (entity instanceof MobEntity) {
            return this.targets.a("Мобы").c().booleanValue();
        }
        return entity instanceof ItemEntity && this.targets.a("Предметы").c().booleanValue();
    }

    private static Box interpolatedBox(Entity entity, float tickDelta) {
        Vec3d pos = MathUtil.a(entity, tickDelta);
        Box box = entity.getBoundingBox();
        double hx = (box.maxX - box.minX) * 0.5d;
        double hz = (box.maxZ - box.minZ) * 0.5d;
        double height = box.maxY - box.minY;
        return new Box(pos.x - hx, pos.y, pos.z - hz, pos.x + hx, pos.y + height, pos.z + hz);
    }

    private static void writeFill(BufferBuilder buffer, Matrix4f matrix, Box box, int r, int g, int b, int a) {
        float minX = (float) box.minX;
        float minY = (float) box.minY;
        float minZ = (float) box.minZ;
        float maxX = (float) box.maxX;
        float maxY = (float) box.maxY;
        float maxZ = (float) box.maxZ;
        quad(buffer, matrix, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ, r, g, b, a);
        quad(buffer, matrix, minX, maxY, minZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, r, g, b, a);
        quad(buffer, matrix, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, maxX, minY, minZ, r, g, b, a);
        quad(buffer, matrix, minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, r, g, b, a);
        quad(buffer, matrix, minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, r, g, b, a);
        quad(buffer, matrix, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, maxX, minY, maxZ, r, g, b, a);
    }

    private static void writeOutline(BufferBuilder buffer, Matrix4f matrix, Box box, int r, int g, int b, int a) {
        float minX = (float) box.minX;
        float minY = (float) box.minY;
        float minZ = (float) box.minZ;
        float maxX = (float) box.maxX;
        float maxY = (float) box.maxY;
        float maxZ = (float) box.maxZ;
        line(buffer, matrix, minX, minY, minZ, maxX, minY, minZ, r, g, b, a);
        line(buffer, matrix, maxX, minY, minZ, maxX, minY, maxZ, r, g, b, a);
        line(buffer, matrix, maxX, minY, maxZ, minX, minY, maxZ, r, g, b, a);
        line(buffer, matrix, minX, minY, maxZ, minX, minY, minZ, r, g, b, a);
        line(buffer, matrix, minX, maxY, minZ, maxX, maxY, minZ, r, g, b, a);
        line(buffer, matrix, maxX, maxY, minZ, maxX, maxY, maxZ, r, g, b, a);
        line(buffer, matrix, maxX, maxY, maxZ, minX, maxY, maxZ, r, g, b, a);
        line(buffer, matrix, minX, maxY, maxZ, minX, maxY, minZ, r, g, b, a);
        line(buffer, matrix, minX, minY, minZ, minX, maxY, minZ, r, g, b, a);
        line(buffer, matrix, maxX, minY, minZ, maxX, maxY, minZ, r, g, b, a);
        line(buffer, matrix, maxX, minY, maxZ, maxX, maxY, maxZ, r, g, b, a);
        line(buffer, matrix, minX, minY, maxZ, minX, maxY, maxZ, r, g, b, a);
    }

    private static void quad(BufferBuilder buffer, Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, int r, int g, int b, int a) {
        buffer.vertex(matrix, x1, y1, z1).color(r, g, b, a);
        buffer.vertex(matrix, x2, y2, z2).color(r, g, b, a);
        buffer.vertex(matrix, x3, y3, z3).color(r, g, b, a);
        buffer.vertex(matrix, x4, y4, z4).color(r, g, b, a);
    }

    private static void line(BufferBuilder buffer, Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2, int r, int g, int b, int a) {
        buffer.vertex(matrix, x1, y1, z1).color(r, g, b, a);
        buffer.vertex(matrix, x2, y2, z2).color(r, g, b, a);
    }
}
