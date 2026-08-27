package aethereal.module.render;

import aethereal.render.ColorUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * Shoulder pets as real animal models (baby chicken / pig / gray wolf).
 * Uses InventoryScreen entity drawing for GUI; solid Ultima-style mesh fallback in-world
 * plus attempts to keep living baby entities for authentic silhouettes.
 */
public final class CosmeticAnimalPets {
    private static ChickenEntity chicken;
    private static PigEntity pig;
    private static WolfEntity wolf;

    private CosmeticAnimalPets() {
    }

    public static void renderWorld(MatrixStack matrices, net.minecraft.entity.player.PlayerEntity player, Vec3d cam,
                                   String style, float scale, float time, float tickDelta) {
        if (player == null || matrices == null) {
            return;
        }
        Vec3d pos = aethereal.util.MathUtil.a(player, tickDelta);
        float yaw = player.getYaw(tickDelta);
        float height = player.getHeight();
        float bob = (float) Math.sin(time * 2.4d) * 0.04f * scale;
        float orbit = (float) Math.sin(time * 1.5d) * 0.025f * scale;
        // left shoulder
        double lx = -0.42f * scale + orbit;
        double ly = height * 0.72f + bob;
        double lz = 0.10f * scale;

        matrices.push();
        matrices.translate(pos.x - cam.x, pos.y - cam.y, pos.z - cam.z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-yaw));
        matrices.translate(lx, ly, lz);
        matrices.scale(0.45f * scale, 0.45f * scale, 0.45f * scale);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(time * 25.0f));

        if (!tryRenderEntity(matrices, style, tickDelta, 0xF000F0)) {
            renderSolidAnimal(matrices, style, time);
        }
        matrices.pop();
    }

    /** Call after bone attach + user offsets are already applied to matrices. */
    public static void renderAttached(MatrixStack matrices, net.minecraft.entity.player.PlayerEntity player,
                                      String style, float time, float tickDelta, int light) {
        if (matrices == null || style == null) {
            return;
        }
        matrices.push();
        float bob = (float) Math.sin(time * 2.4d) * 0.03f;
        matrices.translate(0.0f, bob, 0.0f);
        matrices.scale(0.42f, 0.42f, 0.42f);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(time * 28.0f));
        if (!tryRenderEntity(matrices, style, tickDelta, light)) {
            renderSolidAnimal(matrices, style, time);
        }
        matrices.pop();
    }

    public static void renderGui(DrawContext context, String style, float x, float y, float size, float rotationDeg, float alpha) {
        if (context == null || style == null || alpha < 0.05f) {
            return;
        }
        LivingEntity entity = ensure(style);
        if (entity != null) {
            try {
                int x1 = Math.round(x - size * 0.5f);
                int y1 = Math.round(y - size * 0.85f);
                int x2 = Math.round(x + size * 0.5f);
                int y2 = Math.round(y + size * 0.15f);
                float lookX = x + MathHelper.sin((float) Math.toRadians(rotationDeg)) * 40.0f;
                float lookY = y - 10.0f;
                InventoryScreen.drawEntity(context, x1, y1, x2, y2, Math.round(size * 0.55f), 0.0625f, lookX, lookY, entity);
                return;
            } catch (Throwable ignored) {
            }
        }
        MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.translate(x, y - size * 0.15f, 150.0f);
        matrices.scale(size * 0.35f, -size * 0.35f, size * 0.35f);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(18.0f));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotationDeg));
        renderSolidAnimal(matrices, style, (System.currentTimeMillis() % 100000L) / 1000.0f);
        matrices.pop();
    }

    private static boolean tryRenderEntity(MatrixStack matrices, String style, float tickDelta, int light) {
        LivingEntity entity = ensure(style);
        if (entity == null) {
            return false;
        }
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            var dispatcher = client.getEntityRenderDispatcher();
            var consumers = client.getBufferBuilders().getEntityVertexConsumers();
            dispatcher.setRenderShadows(false);
            entity.prevYaw = entity.getYaw();
            entity.setYaw(0.0f);
            entity.setBodyYaw(0.0f);
            entity.setHeadYaw(0.0f);
            dispatcher.render(entity, 0.0, 0.0, 0.0, 0.0f, matrices, consumers, light);
            consumers.draw();
            dispatcher.setRenderShadows(true);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static LivingEntity ensure(String style) {
        MinecraftClient client = MinecraftClient.getInstance();
        World world = client.world;
        if (world == null) {
            return null;
        }
        try {
            if ("Свинка".equals(style)) {
                if (pig == null || pig.getWorld() != world) {
                    pig = EntityType.PIG.create(world, net.minecraft.entity.SpawnReason.LOAD);
                    if (pig != null) {
                        pig.setBaby(true);
                    }
                }
                return pig;
            }
            if ("Серый волчёнок".equals(style)) {
                if (wolf == null || wolf.getWorld() != world) {
                    wolf = EntityType.WOLF.create(world, net.minecraft.entity.SpawnReason.LOAD);
                    if (wolf != null) {
                        wolf.setBaby(true);
                    }
                }
                return wolf;
            }
            if (chicken == null || chicken.getWorld() != world) {
                chicken = EntityType.CHICKEN.create(world, net.minecraft.entity.SpawnReason.LOAD);
                if (chicken != null) {
                    chicken.setBaby(true);
                }
            }
            return chicken;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void renderSolidAnimal(MatrixStack matrices, String style, float time) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        Matrix4f m = matrices.peek().getPositionMatrix();
        if ("Свинка".equals(style)) {
            pigMesh(buffer, m);
        } else if ("Серый волчёнок".equals(style)) {
            wolfMesh(buffer, m, time);
        } else {
            chickMesh(buffer, m, time);
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    private static void chickMesh(BufferBuilder b, Matrix4f m, float time) {
        int[] body = new int[]{240, 210, 70};
        int[] beak = new int[]{230, 120, 40};
        int[] comb = new int[]{220, 40, 40};
        int[] leg = new int[]{220, 160, 60};
        box(b, m, 0, 0.05f, 0, 0.55f, 0.42f, 0.60f, body, 235);
        box(b, m, 0, 0.32f, 0.22f, 0.38f, 0.34f, 0.38f, body, 240);
        box(b, m, 0, 0.28f, 0.42f, 0.16f, 0.10f, 0.14f, beak, 245);
        box(b, m, 0, 0.48f, 0.18f, 0.10f, 0.12f, 0.08f, comb, 245);
        float flap = (float) Math.sin(time * 8.0d) * 0.12f;
        box(b, m, 0.32f, 0.10f + flap, 0, 0.08f, 0.28f, 0.35f, body, 210);
        box(b, m, -0.32f, 0.10f - flap, 0, 0.08f, 0.28f, 0.35f, body, 210);
        box(b, m, 0.10f, -0.22f, 0.05f, 0.08f, 0.22f, 0.08f, leg, 230);
        box(b, m, -0.10f, -0.22f, 0.05f, 0.08f, 0.22f, 0.08f, leg, 230);
    }

    private static void pigMesh(BufferBuilder b, Matrix4f m) {
        int[] pink = new int[]{240, 170, 170};
        int[] snout = new int[]{230, 140, 150};
        int[] hoof = new int[]{60, 40, 40};
        box(b, m, 0, 0.05f, 0, 0.70f, 0.48f, 0.85f, pink, 235);
        box(b, m, 0, 0.22f, 0.48f, 0.42f, 0.36f, 0.36f, pink, 240);
        box(b, m, 0, 0.16f, 0.68f, 0.28f, 0.20f, 0.16f, snout, 245);
        box(b, m, 0.22f, -0.22f, 0.22f, 0.14f, 0.22f, 0.14f, pink, 230);
        box(b, m, -0.22f, -0.22f, 0.22f, 0.14f, 0.22f, 0.14f, pink, 230);
        box(b, m, 0.22f, -0.22f, -0.22f, 0.14f, 0.22f, 0.14f, pink, 230);
        box(b, m, -0.22f, -0.22f, -0.22f, 0.14f, 0.22f, 0.14f, pink, 230);
        box(b, m, 0.22f, -0.34f, 0.22f, 0.12f, 0.06f, 0.12f, hoof, 240);
        box(b, m, -0.22f, -0.34f, 0.22f, 0.12f, 0.06f, 0.12f, hoof, 240);
        box(b, m, 0.22f, -0.34f, -0.22f, 0.12f, 0.06f, 0.12f, hoof, 240);
        box(b, m, -0.22f, -0.34f, -0.22f, 0.12f, 0.06f, 0.12f, hoof, 240);
        box(b, m, 0, 0.05f, -0.48f, 0.10f, 0.12f, 0.18f, pink, 220);
    }

    private static void wolfMesh(BufferBuilder b, Matrix4f m, float time) {
        int[] fur = new int[]{150, 150, 155};
        int[] dark = new int[]{90, 90, 95};
        int[] ear = new int[]{120, 120, 125};
        float look = (float) Math.sin(time * 1.8d) * 0.04f;
        box(b, m, 0, 0.05f, 0, 0.42f, 0.38f, 0.75f, fur, 235);
        box(b, m, look, 0.28f, 0.42f, 0.36f, 0.34f, 0.36f, fur, 240);
        box(b, m, look, 0.22f, 0.60f, 0.22f, 0.16f, 0.16f, dark, 240);
        box(b, m, look - 0.12f, 0.46f, 0.38f, 0.10f, 0.16f, 0.08f, ear, 240);
        box(b, m, look + 0.12f, 0.46f, 0.38f, 0.10f, 0.16f, 0.08f, ear, 240);
        box(b, m, 0.14f, -0.20f, 0.18f, 0.10f, 0.24f, 0.10f, fur, 230);
        box(b, m, -0.14f, -0.20f, 0.18f, 0.10f, 0.24f, 0.10f, fur, 230);
        box(b, m, 0.14f, -0.20f, -0.22f, 0.10f, 0.24f, 0.10f, fur, 230);
        box(b, m, -0.14f, -0.20f, -0.22f, 0.10f, 0.24f, 0.10f, fur, 230);
        box(b, m, 0, 0.12f, -0.48f, 0.12f, 0.14f, 0.30f, fur, 225);
    }

    private static void box(BufferBuilder buffer, Matrix4f matrix, float cx, float cy, float cz, float hx, float hy, float hz, int[] rgb, int a) {
        float x0 = cx - hx * 0.5f, x1 = cx + hx * 0.5f;
        float y0 = cy - hy * 0.5f, y1 = cy + hy * 0.5f;
        float z0 = cz - hz * 0.5f, z1 = cz + hz * 0.5f;
        face(buffer, matrix, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, lit(rgb, 1.00f), a);
        face(buffer, matrix, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, lit(rgb, 0.55f), a);
        face(buffer, matrix, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, lit(rgb, 1.15f), a);
        face(buffer, matrix, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, lit(rgb, 0.40f), a);
        face(buffer, matrix, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, lit(rgb, 0.70f), a);
        face(buffer, matrix, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, lit(rgb, 0.85f), a);
    }

    private static void face(BufferBuilder buffer, Matrix4f m, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, int[] rgb, int a) {
        buffer.vertex(m, x1, y1, z1).color(rgb[0], rgb[1], rgb[2], a);
        buffer.vertex(m, x2, y2, z2).color(rgb[0], rgb[1], rgb[2], a);
        buffer.vertex(m, x3, y3, z3).color(rgb[0], rgb[1], rgb[2], a);
        buffer.vertex(m, x4, y4, z4).color(rgb[0], rgb[1], rgb[2], a);
    }

    private static int[] lit(int[] rgb, float mul) {
        return new int[]{
                Math.max(0, Math.min(255, (int) (rgb[0] * mul))),
                Math.max(0, Math.min(255, (int) (rgb[1] * mul))),
                Math.max(0, Math.min(255, (int) (rgb[2] * mul)))
        };
    }
}
