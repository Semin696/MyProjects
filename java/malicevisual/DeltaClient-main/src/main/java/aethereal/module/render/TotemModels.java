package aethereal.module.render;

import aethereal.render.ColorUtil;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

public final class TotemModels {
    private static final Identifier WHITE = Identifier.ofVanilla("textures/block/white_concrete.png");
    private static int light;
    private static int overlay;

    private TotemModels() {
    }

    public static void render(String style, MatrixStack matrices, VertexConsumerProvider consumers, ModelTransformationMode mode, float scale, boolean animate, int accent, int packedLight, int packedOverlay) {
        light = packedLight;
        overlay = packedOverlay;
        matrices.push();
        matrices.scale(scale, scale, scale);
        if (mode == ModelTransformationMode.GUI || mode == ModelTransformationMode.FIXED) {
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(22.0f));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-42.0f));
        } else if (mode == ModelTransformationMode.GROUND) {
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(18.0f));
        }
        float time = (System.currentTimeMillis() % 100000L) / 1000.0f;
        if (animate) {
            float bob = MathHelper.sin(time * 2.35f) * (mode == ModelTransformationMode.GUI ? 0.045f : 0.016f);
            matrices.translate(0.0f, bob, 0.0f);
            if (mode == ModelTransformationMode.GUI || mode == ModelTransformationMode.GROUND) {
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(time * 22.0f));
            }
        }
        VertexConsumer buffer = consumers.getBuffer(RenderLayer.getEntityCutoutNoCull(WHITE));
        switch (style) {
            case "Кристалл" -> crystal(buffer, matrices, time);
            case "Неон" -> neon(buffer, matrices, time, accent);
            case "Ангел" -> angel(buffer, matrices, time);
            case "Демон" -> demon(buffer, matrices, time);
            case "Феникс" -> phoenix(buffer, matrices, time);
            case "Руна" -> rune(buffer, matrices, time, accent);
            default -> malice(buffer, matrices, time, accent);
        }
        matrices.pop();
    }

    private static void malice(VertexConsumer v, MatrixStack matrices, float time, int accent) {
        int[] ac = ColorUtil.b(accent);
        int ar = ac[0];
        int ag = ac[1];
        int ab = ac[2];
        Matrix4f m = matrices.peek().getPositionMatrix();
        boxC(v, m, 0.0f, -0.38f, 0.0f, 0.30f, 0.07f, 0.30f, 28, 14, 36, 255);
        boxC(v, m, 0.0f, -0.33f, 0.0f, 0.22f, 0.04f, 0.22f, 48, 22, 58, 255);
        boxC(v, m, 0.0f, -0.18f, 0.0f, 0.22f, 0.26f, 0.16f, 72, 28, 92, 255);
        boxC(v, m, 0.0f, 0.04f, 0.0f, 0.17f, 0.20f, 0.13f, 96, 36, 118, 255);
        boxC(v, m, 0.0f, 0.22f, 0.0f, 0.16f, 0.14f, 0.14f, 168, 92, 168, 255);
        boxC(v, m, 0.0f, 0.30f, 0.0f, 0.12f, 0.08f, 0.12f, 210, 140, 200, 255);
        int pulse = pulse(210, 110, 220, 255, 255, 255, time * 4.2f);
        boxC(v, m, 0.0f, -0.08f, 0.09f, 0.08f, 0.08f, 0.04f, red(pulse), green(pulse), blue(pulse), 255);
        boxC(v, m, 0.0f, -0.08f, 0.11f, 0.045f, 0.045f, 0.03f, ar, ag, ab, 255);
        halo(v, m, 0.42f, 0.16f, 0.028f, ar, ag, ab, 18);
        boxC(v, m, 0.0f, 0.42f, 0.0f, 0.04f, 0.04f, 0.04f, 255, 210, 245, 255);
        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(28.0f));
        boxC(v, matrices.peek().getPositionMatrix(), 0.18f, -0.02f, 0.0f, 0.07f, 0.20f, 0.06f, ar, ag, ab, 255);
        matrices.pop();
        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-28.0f));
        boxC(v, matrices.peek().getPositionMatrix(), -0.18f, -0.02f, 0.0f, 0.07f, 0.20f, 0.06f, mix(ar, 40), mix(ag, 20), mix(ab, 70), 255);
        matrices.pop();
    }

    private static void crystal(VertexConsumer v, MatrixStack matrices, float time) {
        Matrix4f m = matrices.peek().getPositionMatrix();
        boxC(v, m, 0.0f, -0.36f, 0.0f, 0.20f, 0.06f, 0.20f, 40, 70, 92, 255);
        boxC(v, m, 0.0f, -0.30f, 0.0f, 0.12f, 0.05f, 0.12f, 90, 160, 180, 255);
        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45.0f));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(36.0f));
        boxC(v, matrices.peek().getPositionMatrix(), 0.0f, 0.04f, 0.0f, 0.20f, 0.20f, 0.20f, 70, 210, 230, 255);
        boxC(v, matrices.peek().getPositionMatrix(), 0.0f, 0.04f, 0.0f, 0.10f, 0.10f, 0.10f, 230, 255, 255, 255);
        matrices.pop();
        shard(v, matrices, 0.0f, 22.0f, 0.16f, 90, 220, 240);
        shard(v, matrices, 90.0f, 18.0f, 0.15f, 50, 170, 210);
        shard(v, matrices, 180.0f, 26.0f, 0.17f, 140, 240, 255);
        shard(v, matrices, 270.0f, 20.0f, 0.15f, 70, 190, 220);
        float spark = 0.22f + MathHelper.sin(time * 5.0f) * 0.03f;
        boxC(v, matrices.peek().getPositionMatrix(), spark, 0.18f, 0.08f, 0.03f, 0.03f, 0.03f, 255, 255, 255, 255);
        boxC(v, matrices.peek().getPositionMatrix(), -0.16f, 0.26f, -0.06f, 0.025f, 0.025f, 0.025f, 180, 255, 255, 255);
        boxC(v, matrices.peek().getPositionMatrix(), 0.10f, 0.32f, 0.12f, 0.022f, 0.022f, 0.022f, 255, 255, 255, 255);
    }

    private static void neon(VertexConsumer v, MatrixStack matrices, float time, int accent) {
        int[] ac = ColorUtil.b(accent);
        Matrix4f m = matrices.peek().getPositionMatrix();
        boxC(v, m, 0.0f, -0.02f, 0.0f, 0.13f, 0.62f, 0.13f, 8, 8, 14, 255);
        boxC(v, m, 0.0f, -0.36f, 0.0f, 0.22f, 0.08f, 0.22f, 12, 12, 20, 255);
        int glow = pulse(ac[0], ac[1], ac[2], 80, 255, 255, time * 5.5f);
        ring(v, m, -0.18f, 0.14f, 0.025f, red(glow), green(glow), blue(glow), 14);
        ring(v, m, 0.08f, 0.16f, 0.022f, 255, 70, 210, 14);
        ring(v, m, 0.30f, 0.12f, 0.020f, 90, 240, 255, 12);
        boxC(v, m, 0.0f, 0.36f, 0.0f, 0.11f, 0.11f, 0.11f, 10, 10, 16, 255);
        boxC(v, m, 0.0f, 0.36f, 0.0f, 0.07f, 0.07f, 0.07f, red(glow), green(glow), blue(glow), 255);
        boxC(v, m, 0.075f, -0.02f, 0.0f, 0.018f, 0.50f, 0.018f, 255, 90, 220, 255);
        boxC(v, m, -0.075f, -0.02f, 0.0f, 0.018f, 0.50f, 0.018f, 70, 230, 255, 255);
        boxC(v, m, 0.0f, -0.02f, 0.075f, 0.018f, 0.50f, 0.018f, ac[0], ac[1], ac[2], 255);
    }

    private static void angel(VertexConsumer v, MatrixStack matrices, float time) {
        Matrix4f m = matrices.peek().getPositionMatrix();
        boxC(v, m, 0.0f, -0.36f, 0.0f, 0.24f, 0.06f, 0.18f, 196, 168, 92, 255);
        boxC(v, m, 0.0f, -0.16f, 0.0f, 0.18f, 0.30f, 0.12f, 236, 228, 214, 255);
        boxC(v, m, 0.0f, 0.08f, 0.0f, 0.14f, 0.12f, 0.10f, 210, 186, 120, 255);
        boxC(v, m, 0.0f, 0.22f, 0.0f, 0.15f, 0.14f, 0.13f, 250, 244, 232, 255);
        boxC(v, m, 0.0f, 0.30f, 0.0f, 0.11f, 0.08f, 0.11f, 255, 250, 240, 255);
        boxC(v, m, 0.0f, -0.08f, 0.07f, 0.06f, 0.06f, 0.03f, 232, 196, 96, 255);
        halo(v, m, 0.44f, 0.15f, 0.026f, 255, 220, 110, 16);
        angelWing(v, matrices, 1.0f, time);
        angelWing(v, matrices, -1.0f, time);
        boxC(v, matrices.peek().getPositionMatrix(), 0.0f, 0.44f, 0.0f, 0.035f, 0.035f, 0.035f, 255, 240, 160, 255);
    }

    private static void demon(VertexConsumer v, MatrixStack matrices, float time) {
        Matrix4f m = matrices.peek().getPositionMatrix();
        boxC(v, m, 0.0f, -0.38f, 0.0f, 0.28f, 0.07f, 0.22f, 28, 8, 10, 255);
        boxC(v, m, 0.0f, -0.16f, 0.0f, 0.20f, 0.32f, 0.14f, 72, 14, 18, 255);
        boxC(v, m, 0.0f, 0.08f, 0.0f, 0.16f, 0.14f, 0.12f, 48, 10, 12, 255);
        boxC(v, m, 0.0f, 0.22f, 0.0f, 0.15f, 0.14f, 0.13f, 92, 18, 22, 255);
        boxC(v, m, 0.0f, 0.30f, 0.0f, 0.11f, 0.08f, 0.11f, 40, 8, 10, 255);
        int ember = pulse(255, 70, 20, 255, 180, 40, time * 6.0f);
        boxC(v, m, 0.0f, -0.06f, 0.08f, 0.08f, 0.09f, 0.04f, red(ember), green(ember), blue(ember), 255);
        boxC(v, m, -0.035f, 0.24f, 0.07f, 0.03f, 0.025f, 0.02f, 255, 40, 30, 255);
        boxC(v, m, 0.035f, 0.24f, 0.07f, 0.03f, 0.025f, 0.02f, 255, 40, 30, 255);
        horn(v, matrices, 1.0f);
        horn(v, matrices, -1.0f);
        wing(v, matrices, 1.0f, 38.0f, 36, 8, 12);
        wing(v, matrices, -1.0f, 38.0f, 36, 8, 12);
        boxC(v, matrices.peek().getPositionMatrix(), 0.0f, -0.38f, 0.12f, 0.05f, 0.10f, 0.05f, 18, 6, 6, 255);
    }

    private static void phoenix(VertexConsumer v, MatrixStack matrices, float time) {
        Matrix4f m = matrices.peek().getPositionMatrix();
        boxC(v, m, 0.0f, -0.34f, 0.0f, 0.22f, 0.06f, 0.18f, 90, 30, 12, 255);
        boxC(v, m, 0.0f, -0.14f, 0.0f, 0.17f, 0.28f, 0.13f, 196, 72, 28, 255);
        boxC(v, m, 0.0f, 0.08f, 0.0f, 0.13f, 0.14f, 0.11f, 230, 110, 36, 255);
        boxC(v, m, 0.0f, 0.22f, 0.0f, 0.14f, 0.12f, 0.12f, 255, 150, 50, 255);
        boxC(v, m, 0.0f, 0.28f, 0.06f, 0.05f, 0.04f, 0.08f, 255, 210, 80, 255);
        int flame = pulse(255, 90, 20, 255, 220, 80, time * 7.0f);
        boxC(v, m, 0.0f, 0.40f, 0.0f, 0.07f, 0.16f, 0.07f, red(flame), green(flame), blue(flame), 255);
        boxC(v, m, -0.06f, 0.36f, 0.0f, 0.05f, 0.12f, 0.05f, 255, 140, 30, 255);
        boxC(v, m, 0.06f, 0.36f, 0.0f, 0.05f, 0.12f, 0.05f, 255, 170, 40, 255);
        boxC(v, m, 0.0f, 0.50f, 0.0f, 0.04f, 0.10f, 0.04f, 255, 240, 140, 255);
        phoenixWing(v, matrices, 1.0f, time);
        phoenixWing(v, matrices, -1.0f, time);
        boxC(v, matrices.peek().getPositionMatrix(), 0.12f, 0.02f, 0.10f, 0.03f, 0.03f, 0.03f, 255, 200, 80, 255);
        boxC(v, matrices.peek().getPositionMatrix(), -0.14f, -0.10f, 0.08f, 0.025f, 0.025f, 0.025f, 255, 160, 40, 255);
    }

    private static void rune(VertexConsumer v, MatrixStack matrices, float time, int accent) {
        int[] ac = ColorUtil.b(accent);
        Matrix4f m = matrices.peek().getPositionMatrix();
        boxC(v, m, 0.0f, -0.08f, 0.0f, 0.22f, 0.56f, 0.10f, 22, 18, 28, 255);
        boxC(v, m, 0.0f, -0.38f, 0.0f, 0.28f, 0.08f, 0.16f, 14, 12, 18, 255);
        boxC(v, m, 0.0f, 0.24f, 0.0f, 0.18f, 0.10f, 0.12f, 32, 24, 40, 255);
        int glow = pulse(ac[0], ac[1], ac[2], 255, 180, 255, time * 3.8f);
        boxC(v, m, 0.0f, -0.04f, 0.06f, 0.03f, 0.22f, 0.02f, red(glow), green(glow), blue(glow), 255);
        boxC(v, m, 0.0f, 0.04f, 0.06f, 0.12f, 0.03f, 0.02f, red(glow), green(glow), blue(glow), 255);
        boxC(v, m, 0.0f, -0.10f, 0.06f, 0.10f, 0.03f, 0.02f, red(glow), green(glow), blue(glow), 255);
        boxC(v, m, -0.05f, -0.04f, 0.06f, 0.03f, 0.10f, 0.02f, red(glow), green(glow), blue(glow), 255);
        boxC(v, m, 0.05f, -0.04f, 0.06f, 0.03f, 0.10f, 0.02f, red(glow), green(glow), blue(glow), 255);
        halo(v, m, 0.38f, 0.13f, 0.022f, ac[0], ac[1], ac[2], 12);
        boxC(v, m, 0.0f, 0.38f, 0.0f, 0.06f, 0.06f, 0.06f, red(glow), green(glow), blue(glow), 255);
        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(32.0f));
        boxC(v, matrices.peek().getPositionMatrix(), 0.16f, 0.04f, 0.0f, 0.05f, 0.22f, 0.05f, 18, 14, 24, 255);
        matrices.pop();
        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-32.0f));
        boxC(v, matrices.peek().getPositionMatrix(), -0.16f, 0.04f, 0.0f, 0.05f, 0.22f, 0.05f, 18, 14, 24, 255);
        matrices.pop();
    }

    private static void horn(VertexConsumer v, MatrixStack matrices, float side) {
        matrices.push();
        matrices.translate(side * 0.06f, 0.34f, -0.01f);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * -28.0f));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-18.0f));
        Matrix4f m = matrices.peek().getPositionMatrix();
        boxC(v, m, 0.0f, 0.08f, 0.0f, 0.055f, 0.16f, 0.055f, 24, 6, 8, 255);
        boxC(v, m, 0.0f, 0.18f, 0.0f, 0.032f, 0.10f, 0.032f, 16, 4, 6, 255);
        boxC(v, m, 0.0f, 0.25f, 0.0f, 0.018f, 0.07f, 0.018f, 255, 70, 40, 255);
        matrices.pop();
    }

    private static void wing(VertexConsumer v, MatrixStack matrices, float side, float fold, int r, int g, int b) {
        matrices.push();
        matrices.translate(side * 0.10f, 0.04f, -0.03f);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * fold));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * -12.0f));
        Matrix4f m = matrices.peek().getPositionMatrix();
        boxC(v, m, side * 0.14f, 0.02f, 0.0f, 0.26f, 0.22f, 0.03f, r, g, b, 255);
        boxC(v, m, side * 0.28f, -0.05f, 0.0f, 0.16f, 0.14f, 0.025f, mix(r, 20), mix(g, 20), mix(b, 30), 255);
        matrices.pop();
    }

    private static void angelWing(VertexConsumer v, MatrixStack matrices, float side, float time) {
        float flap = MathHelper.sin(time * 2.65f) * 7.5f;
        matrices.push();
        matrices.translate(side * 0.055f, 0.07f, -0.05f);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * (14.0f + flap)));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * -24.0f));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-10.0f));
        Matrix4f bone = matrices.peek().getPositionMatrix();
        boxC(v, bone, side * 0.11f, 0.025f, 0.0f, 0.22f, 0.042f, 0.034f, 228, 196, 128, 255);
        boxC(v, bone, side * 0.22f, 0.038f, 0.0f, 0.18f, 0.030f, 0.026f, 255, 226, 168, 255);
        for (int i = 0; i < 5; i++) {
            float t = i / 4.0f;
            feather(v, matrices, side, 0.05f + t * 0.09f, 0.01f + t * 0.035f, 0.012f,
                    lerp(4.0f, 24.0f, t), lerp(10.0f, -4.0f, t), 0.10f + t * 0.035f, 0.052f,
                    255, 246, 228);
        }
        for (int i = 0; i < 6; i++) {
            float t = i / 5.0f;
            feather(v, matrices, side, 0.10f + t * 0.13f, 0.02f + t * 0.02f, 0.004f,
                    lerp(8.0f, 36.0f, t), lerp(6.0f, -16.0f, t), 0.15f + t * 0.055f, 0.044f,
                    248, 244, 255);
        }
        for (int i = 0; i < 8; i++) {
            float t = i / 7.0f;
            feather(v, matrices, side, 0.14f + t * 0.16f, 0.01f - t * 0.015f, -0.006f,
                    lerp(12.0f, 54.0f, t), lerp(2.0f, -34.0f, t), 0.19f + t * 0.11f, 0.032f,
                    255, 252, 255);
        }
        matrices.pop();
    }

    private static void phoenixWing(VertexConsumer v, MatrixStack matrices, float side, float time) {
        float flap = MathHelper.sin(time * 3.35f + side) * 9.0f;
        matrices.push();
        matrices.translate(side * 0.05f, 0.05f, -0.045f);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * (18.0f + flap)));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * -30.0f));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-8.0f));
        Matrix4f bone = matrices.peek().getPositionMatrix();
        boxC(v, bone, side * 0.10f, 0.03f, 0.0f, 0.20f, 0.038f, 0.030f, 255, 170, 48, 255);
        int[][] layers = {
                {5, 255, 228, 120},
                {6, 255, 150, 36},
                {7, 255, 78, 18},
                {8, 255, 210, 70}
        };
        for (int layer = 0; layer < layers.length; layer++) {
            int count = layers[layer][0];
            for (int i = 0; i < count; i++) {
                float t = count == 1 ? 0.5f : i / (float) (count - 1);
                float flicker = MathHelper.sin(time * 8.2f + i * 0.9f + layer) * 0.018f;
                float len = 0.11f + layer * 0.055f + t * 0.13f + flicker;
                float yaw = lerp(6.0f, 46.0f, t) + layer * 3.5f;
                float pitch = lerp(14.0f, -10.0f, t) - layer * 7.0f;
                int r = layers[layer][1];
                int g = layers[layer][2];
                int b = layers[layer][3];
                if (layer == 3) {
                    int glow = pulse(255, 90, 20, 255, 240, 140, time * 7.0f + i);
                    r = red(glow);
                    g = green(glow);
                    b = blue(glow);
                }
                feather(v, matrices, side,
                        0.07f + t * 0.12f + layer * 0.018f,
                        0.02f + t * 0.04f + layer * 0.012f,
                        0.008f - layer * 0.004f,
                        yaw, pitch, len, 0.048f - layer * 0.0045f,
                        r, g, b);
            }
        }
        matrices.pop();
    }

    private static void feather(VertexConsumer v, MatrixStack matrices, float side, float x, float y, float z,
                                float yaw, float pitch, float length, float width, int r, int g, int b) {
        matrices.push();
        matrices.translate(side * x, y, z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * yaw));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * pitch));
        Matrix4f m = matrices.peek().getPositionMatrix();
        float along = side * length * 0.48f;
        boxC(v, m, along, 0.0f, 0.0f, length, width, 0.015f, r, g, b, 255);
        boxC(v, m, side * length * 0.40f, 0.007f, 0.0f, length * 0.70f, width * 0.52f, 0.011f, mix(r, 12), mix(g, 10), mix(b, 8), 255);
        boxC(v, m, side * length * 0.88f, 0.0f, 0.0f, length * 0.22f, width * 0.28f, 0.010f, mix(r, 24), mix(g, 18), mix(b, 12), 255);
        boxC(v, m, side * length * 0.38f, 0.011f, 0.0f, length * 0.72f, width * 0.12f, 0.017f, 232, 196, 118, 255);
        matrices.pop();
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static void shard(VertexConsumer v, MatrixStack matrices, float yaw, float pitch, float dist, int r, int g, int b) {
        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yaw));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(pitch));
        matrices.translate(dist, 0.02f, 0.0f);
        Matrix4f m = matrices.peek().getPositionMatrix();
        boxC(v, m, 0.0f, 0.0f, 0.0f, 0.07f, 0.26f, 0.07f, r, g, b, 255);
        boxC(v, m, 0.0f, 0.14f, 0.0f, 0.04f, 0.12f, 0.04f, 220, 255, 255, 255);
        matrices.pop();
    }

    private static void halo(VertexConsumer v, Matrix4f m, float y, float radius, float thick, int r, int g, int b, int segments) {
        for (int i = 0; i < segments; i++) {
            float ang = i / (float) segments * MathHelper.TAU;
            float px = MathHelper.sin(ang) * radius;
            float pz = MathHelper.cos(ang) * radius;
            boxC(v, m, px, y, pz, thick, thick * 0.42f, thick, r, g, b, 255);
        }
    }

    private static void ring(VertexConsumer v, Matrix4f m, float y, float radius, float thick, int r, int g, int b, int segments) {
        halo(v, m, y, radius, thick, r, g, b, segments);
    }

    private static void boxC(VertexConsumer v, Matrix4f m, float cx, float cy, float cz, float sx, float sy, float sz, int r, int g, int b, int a) {
        box(v, m, cx - sx * 0.5f, cy - sy * 0.5f, cz - sz * 0.5f, cx + sx * 0.5f, cy + sy * 0.5f, cz + sz * 0.5f, r, g, b, a);
    }

    private static void box(VertexConsumer v, Matrix4f m, float x0, float y0, float z0, float x1, float y1, float z1, int r, int g, int b, int a) {
        quad(v, m, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, shade(r, g, b, 1.18f), a, 0.0f, 1.0f, 0.0f);
        quad(v, m, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, shade(r, g, b, 0.52f), a, 0.0f, -1.0f, 0.0f);
        quad(v, m, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, shade(r, g, b, 1.05f), a, 0.0f, 0.0f, 1.0f);
        quad(v, m, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, shade(r, g, b, 0.62f), a, 0.0f, 0.0f, -1.0f);
        quad(v, m, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, shade(r, g, b, 0.92f), a, 1.0f, 0.0f, 0.0f);
        quad(v, m, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, shade(r, g, b, 0.72f), a, -1.0f, 0.0f, 0.0f);
    }

    private static void quad(VertexConsumer v, Matrix4f m,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float x3, float y3, float z3,
                             float x4, float y4, float z4,
                             int rgb, int a, float nx, float ny, float nz) {
        int r = (rgb >> 16) & 255;
        int g = (rgb >> 8) & 255;
        int b = rgb & 255;
        vert(v, m, x1, y1, z1, r, g, b, a, 0.0f, 0.0f, nx, ny, nz);
        vert(v, m, x2, y2, z2, r, g, b, a, 1.0f, 0.0f, nx, ny, nz);
        vert(v, m, x3, y3, z3, r, g, b, a, 1.0f, 1.0f, nx, ny, nz);
        vert(v, m, x4, y4, z4, r, g, b, a, 0.0f, 1.0f, nx, ny, nz);
    }

    private static void vert(VertexConsumer v, Matrix4f m, float x, float y, float z, int r, int g, int b, int a, float u, float vt, float nx, float ny, float nz) {
        v.vertex(m, x, y, z).color(r, g, b, a).texture(u, vt).overlay(overlay).light(light).normal(nx, ny, nz);
    }

    private static int shade(int r, int g, int b, float f) {
        return (clamp((int) (r * f)) << 16) | (clamp((int) (g * f)) << 8) | clamp((int) (b * f));
    }

    private static int pulse(int r1, int g1, int b1, int r2, int g2, int b2, float t) {
        float f = MathHelper.sin(t) * 0.5f + 0.5f;
        int r = (int) (r1 + (r2 - r1) * f);
        int g = (int) (g1 + (g2 - g1) * f);
        int b = (int) (b1 + (b2 - b1) * f);
        return (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
    }

    private static int red(int rgb) {
        return (rgb >> 16) & 255;
    }

    private static int green(int rgb) {
        return (rgb >> 8) & 255;
    }

    private static int blue(int rgb) {
        return rgb & 255;
    }

    private static int mix(int c, int add) {
        return clamp(c + add);
    }

    private static int clamp(int v) {
        return MathHelper.clamp(v, 0, 255);
    }
}
