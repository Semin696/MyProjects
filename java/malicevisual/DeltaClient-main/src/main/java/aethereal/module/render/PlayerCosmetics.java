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
import aethereal.setting.ModeSetting;
import aethereal.setting.SliderSetting;
import aethereal.network.MaliceUsers;
import aethereal.util.MathUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

@ModuleRegister(name = "Косметика", description = "Шапки, рюкзаки, крылья, нимб и питомец над левым плечом", category = Category.Render)
public class PlayerCosmetics extends Module {
    private final BooleanSetting hat = new BooleanSetting("Шапка", true);
    private final ModeSetting hatStyle = new ModeSetting("Стиль шапки", "Корона", "Корона", "Волшебник", "Рога", "Ушки", "Конус", "Кепка").a(() -> this.hat.c());
    private final BooleanSetting backpack = new BooleanSetting("Рюкзак", true);
    private final ModeSetting backpackStyle = new ModeSetting("Стиль рюкзака", "Кристалл", "Сумка", "Кристалл", "Джетпак", "Школьный", "Крылатый").a(() -> this.backpack.c());
    private final BooleanSetting pet = new BooleanSetting("Питомец", true);
    private final ModeSetting petStyle = new ModeSetting("Стиль питомца", "Орб", "Орб", "Лиса", "Дракончик", "Призрак", "Звезда").a(() -> this.pet.c());
    private final BooleanSetting halo = new BooleanSetting("Нимб", true);
    private final ModeSetting haloStyle = new ModeSetting("Стиль нимба", "Неон", "Классический", "Святой", "Неон", "Демон").a(() -> this.halo.c());
    private final BooleanSetting wings = new BooleanSetting("Крылья", true);
    private final ModeSetting wingStyle = new ModeSetting("Стиль крыльев", "Ангел", "Ангел", "Демон", "Дракон", "Бабочка", "Феникс").a(() -> this.wings.c());
    private final ModeSetting who = new ModeSetting("Показывать", "Себе и друзьям", "Только себе", "Себе и друзьям", "Всем с Malice");
    private final BooleanSetting syncTheme = new BooleanSetting("Цвет из темы", true);
    private final ColorSetting customColor = new ColorSetting("Цвет", Integer.valueOf(ColorUtil.convertToARGB(224, 92, 208, 255))).a(() -> {
        return Boolean.valueOf(!this.syncTheme.c().booleanValue());
    });
    private final SliderSetting scale = new SliderSetting("Размер", 1.0f, 0.6f, 1.8f, 0.05f);

    public PlayerCosmetics() {
        a(this.hat, this.hatStyle, this.backpack, this.backpackStyle, this.pet, this.petStyle,
                this.halo, this.haloStyle, this.wings, this.wingStyle, this.who, this.syncTheme, this.customColor, this.scale);
    }

    @EventTarget
    public void onDraw(DrawEvent event) {
        if (!event.c() || mc.world == null || mc.player == null) {
            return;
        }
        int base = this.syncTheme.c().booleanValue()
                ? Skeleton.getInstance().getModuleProcessor().o().a(ThemeInfo.PRIMARY).toIntColor()
                : this.customColor.c().intValue();
        float s = this.scale.c().floatValue();
        float time = (System.currentTimeMillis() % 100000L) / 1000.0f;
        boolean any = false;
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (visible(player)) {
                any = true;
                break;
            }
        }
        if (!any) {
            return;
        }

        MatrixStack matrices = event.h();
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d cam = camera.getPos();

        begin(true);
        BufferBuilder glow = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (visible(player)) {
                renderOn(glow, matrices, player, cam, base, s, time, event.g(), true);
            }
        }
        BufferRenderer.drawWithGlobalProgram(glow.end());

        begin(false);
        BufferBuilder mesh = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (visible(player)) {
                renderOn(mesh, matrices, player, cam, base, s, time, event.g(), false);
            }
        }
        BufferRenderer.drawWithGlobalProgram(mesh.end());
        endBatch();
    }

    private boolean visible(PlayerEntity player) {
        if (player == null || !player.isAlive() || player.isInvisible()) {
            return false;
        }
        if (player == mc.player) {
            return mc.options.getPerspective() != Perspective.FIRST_PERSON;
        }
        if (npc(player)) {
            return false;
        }
        if (this.who.l("Только себе")) {
            return false;
        }
        if (MaliceUsers.is(player.getUuid())) {
            return true;
        }
        if (this.who.l("Всем") || this.who.l("Всем с Malice")) {
            return false;
        }
        return Skeleton.getInstance().getModuleProcessor().e().d(player.getName().getString());
    }

    private static boolean npc(PlayerEntity player) {
        String name = player.getName().getString();
        if (name.regionMatches(true, 0, "[NPC]", 0, 5) || name.startsWith("NPC")) {
            return true;
        }
        if (mc.getNetworkHandler() == null) {
            return false;
        }
        return mc.getNetworkHandler().getPlayerListEntry(player.getUuid()) == null;
    }

    private void renderOn(BufferBuilder buffer, MatrixStack matrices, PlayerEntity player, Vec3d cam, int base, float scale, float time, float tickDelta, boolean glowPass) {
        Vec3d pos = MathUtil.a(player, tickDelta);
        float yaw = player.getYaw(tickDelta);
        float height = player.getHeight();
        int[] rgb = ColorUtil.b(base);
        int r = rgb[0];
        int g = rgb[1];
        int b = rgb[2];

        matrices.push();
        matrices.translate(pos.x - cam.x, pos.y - cam.y, pos.z - cam.z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-yaw));
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        if (this.hat.c().booleanValue()) {
            writeHat(buffer, matrix, height, scale, time, r, g, b, glowPass);
        }
        if (this.backpack.c().booleanValue()) {
            writeBackpack(buffer, matrix, height, scale, time, r, g, b, glowPass);
        }
        if (this.pet.c().booleanValue()) {
            writePet(buffer, matrix, height, scale, time, r, g, b, glowPass);
        }
        if (this.halo.c().booleanValue()) {
            writeHalo(buffer, matrix, height, scale, time, r, g, b, glowPass);
        }
        if (this.wings.c().booleanValue()) {
            writeWing(buffer, matrix, 1.0f, height, scale, time, r, g, b, glowPass);
            writeWing(buffer, matrix, -1.0f, height, scale, time, r, g, b, glowPass);
        }
        matrices.pop();
    }

    private void writeHat(BufferBuilder buffer, Matrix4f matrix, float height, float scale, float time, int r, int g, int b, boolean glow) {
        float y = height - 0.08f * scale;
        String style = this.hatStyle.c();
        if ("Волшебник".equals(style)) {
            writeWizardHat(buffer, matrix, y, scale, r, g, b, glow);
        } else if ("Рога".equals(style)) {
            writeHorns(buffer, matrix, y, scale, time, r, g, b, glow);
        } else if ("Ушки".equals(style)) {
            writeCatEars(buffer, matrix, y, scale, time, r, g, b, glow);
        } else if ("Конус".equals(style)) {
            writeConeHat(buffer, matrix, y, scale, time, r, g, b, glow);
        } else if ("Кепка".equals(style)) {
            writeCap(buffer, matrix, y, scale, r, g, b, glow);
        } else {
            writeCrown(buffer, matrix, y, scale, time, r, g, b, glow);
        }
    }

    private void writeCrown(BufferBuilder buffer, Matrix4f matrix, float y, float scale, float time, int r, int g, int b, boolean glow) {
        float radius = 0.28f * scale;
        int[] gold = new int[]{255, 214, 110};
        int[] jewel = new int[]{clamp(r + 20), clamp(g / 2 + 40), clamp(b + 40)};
        if (glow) {
            writeDiscAt(buffer, matrix, 0.0f, y + 0.06f * scale, 0.0f, radius * 1.35f, 24, gold[0], gold[1], gold[2], 28);
            return;
        }
        writeBand(buffer, matrix, 0.0f, y, 0.0f, radius, 0.05f * scale, 0.035f * scale, 28, gold[0], gold[1], gold[2], 220);
        int spikes = 7;
        for (int i = 0; i < spikes; i++) {
            double a = i * (Math.PI * 2.0d / spikes) - Math.PI * 0.5d;
            float bx = (float) (Math.cos(a) * radius);
            float bz = (float) (Math.sin(a) * radius);
            float tipH = (i % 2 == 0 ? 0.22f : 0.14f) * scale;
            writeSpike(buffer, matrix, bx * 0.82f, y + 0.02f * scale, bz * 0.82f, bx, y + tipH, bz, 0.022f * scale, gold[0], gold[1], gold[2], 230);
            if (i % 2 == 0) {
                float bob = (float) Math.sin(time * 3.4d + i) * 0.01f * scale;
                writeDiscAt(buffer, matrix, bx, y + tipH + bob, bz, 0.028f * scale, 10, jewel[0], jewel[1], jewel[2], 235);
            }
        }
    }

    private void writeWizardHat(BufferBuilder buffer, Matrix4f matrix, float y, float scale, int r, int g, int b, boolean glow) {
        int[] cloth = tint(r, g, b, 0.08f);
        int[] brim = new int[]{clamp(r / 3), clamp(g / 4), clamp(b / 2 + 20)};
        if (glow) {
            writeDiscAt(buffer, matrix, 0.0f, y + 0.35f * scale, 0.0f, 0.22f * scale, 18, cloth[0], cloth[1], cloth[2], 30);
            return;
        }
        writeDiscAt(buffer, matrix, 0.0f, y, 0.0f, 0.42f * scale, 28, brim[0], brim[1], brim[2], 210);
        writeCone(buffer, matrix, 0.0f, y, 0.0f, 0.26f * scale, 0.55f * scale, 18, cloth[0], cloth[1], cloth[2], 220);
        writeDiscAt(buffer, matrix, 0.02f * scale, y + 0.56f * scale, -0.02f * scale, 0.04f * scale, 10, 255, 220, 120, 230);
    }

    private void writeHorns(BufferBuilder buffer, Matrix4f matrix, float y, float scale, float time, int r, int g, int b, boolean glow) {
        int cr = clamp(r + 30);
        int cg = clamp(g / 5);
        int cb = clamp(b / 6);
        float pulse = 0.02f * (float) Math.sin(time * 4.0d);
        if (glow) {
            writeDiscAt(buffer, matrix, 0.18f * scale, y + 0.18f * scale, 0.0f, 0.12f * scale, 12, cr, cg, cb, 35);
            writeDiscAt(buffer, matrix, -0.18f * scale, y + 0.18f * scale, 0.0f, 0.12f * scale, 12, cr, cg, cb, 35);
            return;
        }
        writeCurvedHorn(buffer, matrix, 1.0f, y, scale, pulse, cr, cg, cb);
        writeCurvedHorn(buffer, matrix, -1.0f, y, scale, pulse, cr, cg, cb);
    }

    private void writeCurvedHorn(BufferBuilder buffer, Matrix4f matrix, float side, float y, float scale, float pulse, int r, int g, int b) {
        float x0 = side * 0.16f * scale;
        float y0 = y + 0.02f * scale;
        float z0 = 0.04f * scale;
        float x1 = side * 0.28f * scale;
        float y1 = y + (0.22f + pulse) * scale;
        float z1 = -0.02f * scale;
        float x2 = side * 0.22f * scale;
        float y2 = y + (0.38f + pulse) * scale;
        float z2 = -0.10f * scale;
        writeSpike(buffer, matrix, x0, y0, z0, x1, y1, z1, 0.034f * scale, r, g, b, 230);
        writeSpike(buffer, matrix, x1, y1, z1, x2, y2, z2, 0.018f * scale, 255, 240, 220, 220);
    }

    private void writeCatEars(BufferBuilder buffer, Matrix4f matrix, float y, float scale, float time, int r, int g, int b, boolean glow) {
        float twitch = (float) Math.sin(time * 5.2d) * 0.02f * scale;
        int[] fur = tint(r, g, b, 0.35f);
        int[] pink = new int[]{255, 150, 180};
        if (glow) {
            writeDiscAt(buffer, matrix, 0.18f * scale, y + 0.16f * scale, 0.0f, 0.10f * scale, 10, fur[0], fur[1], fur[2], 28);
            writeDiscAt(buffer, matrix, -0.18f * scale, y + 0.16f * scale, 0.0f, 0.10f * scale, 10, fur[0], fur[1], fur[2], 28);
            return;
        }
        writeEar(buffer, matrix, 1.0f, y, scale, twitch, fur, pink);
        writeEar(buffer, matrix, -1.0f, y, scale, -twitch, fur, pink);
    }

    private void writeEar(BufferBuilder buffer, Matrix4f matrix, float side, float y, float scale, float twitch, int[] fur, int[] pink) {
        float bx = side * 0.16f * scale;
        float by = y + 0.02f * scale;
        float tipX = side * 0.22f * scale;
        float tipY = y + (0.22f + twitch) * scale;
        float tipZ = -0.02f * scale;
        float w = 0.07f * scale;
        quad(buffer, matrix, bx - w * 0.2f, by, 0.02f * scale, bx + w, by, 0.02f * scale, tipX, tipY, tipZ, tipX - side * 0.04f * scale, tipY - 0.02f * scale, tipZ, fur[0], fur[1], fur[2], 230);
        quad(buffer, matrix, bx, by + 0.02f * scale, 0.01f * scale, bx + side * 0.04f * scale, by + 0.02f * scale, 0.01f * scale, tipX * 0.92f, tipY - 0.04f * scale, tipZ, tipX * 0.78f, tipY - 0.06f * scale, tipZ, pink[0], pink[1], pink[2], 210);
    }

    private void writeConeHat(BufferBuilder buffer, Matrix4f matrix, float y, float scale, float time, int r, int g, int b, boolean glow) {
        if (glow) {
            writeDiscAt(buffer, matrix, 0.0f, y + 0.08f * scale, 0.0f, 0.55f * scale, 24, r, g, b, 26);
            return;
        }
        writeCone(buffer, matrix, 0.0f, y, 0.0f, 0.48f * scale, 0.28f * scale, 28, r, g, b, 200);
        writeBand(buffer, matrix, 0.0f, y - 0.01f * scale, 0.0f, 0.50f * scale, 0.02f * scale, 0.018f * scale, 28, 255, 236, 160, 180);
        for (int i = 0; i < 8; i++) {
            double a = i * (Math.PI * 2.0d / 8.0d) + time * 0.4d;
            float px = (float) (Math.cos(a) * 0.36f * scale);
            float pz = (float) (Math.sin(a) * 0.36f * scale);
            writeDiscAt(buffer, matrix, px, y + 0.04f * scale, pz, 0.018f * scale, 8, 255, 255, 255, 160);
        }
    }

    private void writeCap(BufferBuilder buffer, Matrix4f matrix, float y, float scale, int r, int g, int b, boolean glow) {
        int[] cloth = tint(r, g, b, 0.15f);
        if (glow) {
            writeDiscAt(buffer, matrix, 0.0f, y + 0.06f * scale, 0.0f, 0.30f * scale, 18, cloth[0], cloth[1], cloth[2], 24);
            return;
        }
        writeBand(buffer, matrix, 0.0f, y, 0.0f, 0.27f * scale, 0.10f * scale, 0.08f * scale, 24, cloth[0], cloth[1], cloth[2], 220);
        writeBox(buffer, matrix, 0.0f, y + 0.01f * scale, 0.22f * scale, 0.30f * scale, 0.02f * scale, 0.14f * scale, cloth[0], cloth[1], cloth[2], 210);
        writeDiscAt(buffer, matrix, 0.0f, y + 0.11f * scale, 0.0f, 0.08f * scale, 12, 255, 255, 255, 90);
    }

    private void writeBackpack(BufferBuilder buffer, Matrix4f matrix, float height, float scale, float time, int r, int g, int b, boolean glow) {
        String style = this.backpackStyle.c();
        float y = height * 0.48f;
        if ("Сумка".equals(style)) {
            writeSatchel(buffer, matrix, y, scale, r, g, b, glow);
        } else if ("Джетпак".equals(style)) {
            writeJetpack(buffer, matrix, y, scale, time, r, g, b, glow);
        } else if ("Школьный".equals(style)) {
            writeSchoolBag(buffer, matrix, y, scale, r, g, b, glow);
        } else if ("Крылатый".equals(style)) {
            writeWingedPack(buffer, matrix, y, scale, time, r, g, b, glow);
        } else {
            writeCrystalPack(buffer, matrix, y, scale, time, r, g, b, glow);
        }
    }

    private void writeSatchel(BufferBuilder buffer, Matrix4f matrix, float y, float scale, int r, int g, int b, boolean glow) {
        int[] leather = new int[]{clamp(90 + r / 8), clamp(55 + g / 10), clamp(35 + b / 12)};
        if (glow) {
            writeBox(buffer, matrix, 0.0f, y, -0.22f * scale, 0.28f * scale, 0.34f * scale, 0.14f * scale, leather[0], leather[1], leather[2], 30);
            return;
        }
        writeBox(buffer, matrix, 0.0f, y, -0.20f * scale, 0.26f * scale, 0.32f * scale, 0.12f * scale, leather[0], leather[1], leather[2], 225);
        writeBox(buffer, matrix, 0.0f, y + 0.10f * scale, -0.20f * scale, 0.22f * scale, 0.06f * scale, 0.04f * scale, 40, 28, 22, 230);
        writeSpike(buffer, matrix, -0.14f * scale, y + 0.16f * scale, -0.08f * scale, -0.18f * scale, heightStrap(y, scale), 0.06f * scale, 0.012f * scale, 60, 40, 30, 220);
        writeSpike(buffer, matrix, 0.14f * scale, y + 0.16f * scale, -0.08f * scale, 0.18f * scale, heightStrap(y, scale), 0.06f * scale, 0.012f * scale, 60, 40, 30, 220);
    }

    private float heightStrap(float y, float scale) {
        return y + 0.34f * scale;
    }

    private void writeCrystalPack(BufferBuilder buffer, Matrix4f matrix, float y, float scale, float time, int r, int g, int b, boolean glow) {
        float pulse = 0.5f + 0.5f * (float) Math.sin(time * 3.6d);
        int[] core = new int[]{r, g, b};
        int[] shell = tint(r, g, b, 0.45f);
        if (glow) {
            writeDiscAt(buffer, matrix, 0.0f, y + 0.06f * scale, -0.22f * scale, 0.22f * scale, 16, core[0], core[1], core[2], Math.round(40 + 30 * pulse));
            return;
        }
        writeBox(buffer, matrix, 0.0f, y, -0.18f * scale, 0.18f * scale, 0.28f * scale, 0.10f * scale, 28, 18, 36, 220);
        writeCrystal(buffer, matrix, 0.0f, y + 0.08f * scale, -0.28f * scale, 0.14f * scale, 0.22f * scale, core, shell, 210);
        writeDiscAt(buffer, matrix, 0.0f, y + 0.08f * scale, -0.28f * scale, 0.05f * scale * (0.8f + 0.3f * pulse), 12, 255, 255, 255, Math.round(140 + 80 * pulse));
    }

    private void writeJetpack(BufferBuilder buffer, Matrix4f matrix, float y, float scale, float time, int r, int g, int b, boolean glow) {
        int[] metal = new int[]{140, 148, 168};
        int[] flame = new int[]{255, clamp(90 + (int) (80 * Math.sin(time * 12.0d))), 20};
        if (glow) {
            writeDiscAt(buffer, matrix, -0.10f * scale, y - 0.18f * scale, -0.20f * scale, 0.10f * scale, 10, flame[0], flame[1], flame[2], 50);
            writeDiscAt(buffer, matrix, 0.10f * scale, y - 0.18f * scale, -0.20f * scale, 0.10f * scale, 10, flame[0], flame[1], flame[2], 50);
            return;
        }
        writeBox(buffer, matrix, -0.10f * scale, y, -0.20f * scale, 0.10f * scale, 0.30f * scale, 0.10f * scale, metal[0], metal[1], metal[2], 230);
        writeBox(buffer, matrix, 0.10f * scale, y, -0.20f * scale, 0.10f * scale, 0.30f * scale, 0.10f * scale, metal[0], metal[1], metal[2], 230);
        writeBox(buffer, matrix, 0.0f, y + 0.04f * scale, -0.16f * scale, 0.08f * scale, 0.14f * scale, 0.06f * scale, r, g, b, 210);
        float flicker = 0.08f * scale * (0.55f + 0.45f * (float) Math.sin(time * 14.0d));
        writeSpike(buffer, matrix, -0.10f * scale, y - 0.14f * scale, -0.20f * scale, -0.10f * scale, y - 0.14f * scale - flicker - 0.10f * scale, -0.20f * scale, 0.028f * scale, flame[0], flame[1], flame[2], 210);
        writeSpike(buffer, matrix, 0.10f * scale, y - 0.14f * scale, -0.20f * scale, 0.10f * scale, y - 0.14f * scale - flicker - 0.10f * scale, -0.20f * scale, 0.028f * scale, flame[0], flame[1], flame[2], 210);
    }

    private void writeSchoolBag(BufferBuilder buffer, Matrix4f matrix, float y, float scale, int r, int g, int b, boolean glow) {
        int[] body = tint(r, g, b, 0.2f);
        if (glow) {
            writeBox(buffer, matrix, 0.0f, y, -0.22f * scale, 0.30f * scale, 0.36f * scale, 0.14f * scale, body[0], body[1], body[2], 28);
            return;
        }
        writeBox(buffer, matrix, 0.0f, y, -0.20f * scale, 0.28f * scale, 0.34f * scale, 0.12f * scale, body[0], body[1], body[2], 225);
        writeBox(buffer, matrix, 0.0f, y + 0.02f * scale, -0.26f * scale, 0.18f * scale, 0.16f * scale, 0.04f * scale, clamp(body[0] - 30), clamp(body[1] - 30), clamp(body[2] - 20), 220);
        writeDiscAt(buffer, matrix, 0.0f, y - 0.02f * scale, -0.26f * scale, 0.035f * scale, 10, 255, 220, 90, 230);
        writeSpike(buffer, matrix, -0.16f * scale, y + 0.16f * scale, -0.06f * scale, -0.20f * scale, y + 0.36f * scale, 0.08f * scale, 0.012f * scale, 255, 255, 255, 180);
        writeSpike(buffer, matrix, 0.16f * scale, y + 0.16f * scale, -0.06f * scale, 0.20f * scale, y + 0.36f * scale, 0.08f * scale, 0.012f * scale, 255, 255, 255, 180);
    }

    private void writeWingedPack(BufferBuilder buffer, Matrix4f matrix, float y, float scale, float time, int r, int g, int b, boolean glow) {
        writeCrystalPack(buffer, matrix, y, scale, time, r, g, b, glow);
        float flap = (float) Math.sin(time * 3.0d) * 0.18f;
        if (!glow) {
            writeMiniWing(buffer, matrix, 1.0f, y + 0.04f * scale, -0.22f * scale, scale, flap, r, g, b);
            writeMiniWing(buffer, matrix, -1.0f, y + 0.04f * scale, -0.22f * scale, scale, flap, r, g, b);
        }
    }

    private void writeMiniWing(BufferBuilder buffer, Matrix4f matrix, float side, float y, float z, float scale, float flap, int r, int g, int b) {
        float tipX = side * (0.28f + flap * 0.1f) * scale;
        float tipY = y + 0.10f * scale;
        float tipZ = z - 0.08f * scale;
        quad(buffer, matrix, side * 0.04f * scale, y, z, side * 0.06f * scale, y + 0.06f * scale, z,
                tipX, tipY, tipZ, tipX * 0.7f, y - 0.04f * scale, tipZ + 0.02f * scale, r, g, b, 170);
    }

    private void writePet(BufferBuilder buffer, Matrix4f matrix, float height, float scale, float time, int r, int g, int b, boolean glow) {
        // Left shoulder orbit (Pulse-style companion)
        float bob = (float) Math.sin(time * 2.4d) * 0.05f * scale;
        float orbit = (float) Math.sin(time * 1.6d) * 0.03f * scale;
        float px = -0.42f * scale + orbit;
        float py = height * 0.72f + bob;
        float pz = 0.08f * scale;
        String style = this.petStyle.c();
        if ("Лиса".equals(style)) {
            writeFoxPet(buffer, matrix, px, py, pz, scale, time, r, g, b, glow);
        } else if ("Дракончик".equals(style)) {
            writeDragonPet(buffer, matrix, px, py, pz, scale, time, r, g, b, glow);
        } else if ("Призрак".equals(style)) {
            writeGhostPet(buffer, matrix, px, py, pz, scale, time, r, g, b, glow);
        } else if ("Звезда".equals(style)) {
            writeStarPet(buffer, matrix, px, py, pz, scale, time, r, g, b, glow);
        } else {
            writeOrbPet(buffer, matrix, px, py, pz, scale, time, r, g, b, glow);
        }
    }

    private void writeOrbPet(BufferBuilder buffer, Matrix4f matrix, float x, float y, float z, float scale, float time, int r, int g, int b, boolean glow) {
        float pulse = 0.5f + 0.5f * (float) Math.sin(time * 4.2d);
        if (glow) {
            writeDiscAt(buffer, matrix, x, y, z, 0.16f * scale, 16, r, g, b, Math.round(35 + 25 * pulse));
            return;
        }
        writeDiscAt(buffer, matrix, x, y, z, 0.09f * scale, 16, r, g, b, 210);
        writeDiscAt(buffer, matrix, x, y, z, 0.05f * scale, 12, 255, 255, 255, Math.round(160 + 60 * pulse));
        writeTorus(buffer, matrix, x, y, z, 0.13f * scale, 0.012f * scale, 24, 8, time * 1.4f, r, g, b, 180);
        for (int i = 0; i < 5; i++) {
            double a = time * 2.0d + i * (Math.PI * 2.0d / 5.0d);
            float mx = x + (float) Math.cos(a) * 0.13f * scale;
            float mz = z + (float) Math.sin(a) * 0.13f * scale;
            writeDiscAt(buffer, matrix, mx, y + (float) Math.sin(a * 2.0d) * 0.02f * scale, mz, 0.016f * scale, 8, 255, 255, 255, 200);
        }
    }

    private void writeFoxPet(BufferBuilder buffer, Matrix4f matrix, float x, float y, float z, float scale, float time, int r, int g, int b, boolean glow) {
        int[] fur = new int[]{255, clamp(120 + g / 6), clamp(40 + b / 8)};
        int[] white = new int[]{255, 245, 235};
        float look = (float) Math.sin(time * 1.8d) * 0.03f * scale;
        if (glow) {
            writeDiscAt(buffer, matrix, x, y, z, 0.14f * scale, 12, fur[0], fur[1], fur[2], 30);
            return;
        }
        writeBox(buffer, matrix, x, y, z, 0.10f * scale, 0.08f * scale, 0.12f * scale, fur[0], fur[1], fur[2], 225);
        writeBox(buffer, matrix, x + look, y + 0.08f * scale, z + 0.02f * scale, 0.08f * scale, 0.07f * scale, 0.08f * scale, fur[0], fur[1], fur[2], 230);
        writeSpike(buffer, matrix, x - 0.04f * scale, y + 0.12f * scale, z, x - 0.06f * scale, y + 0.20f * scale, z - 0.01f * scale, 0.018f * scale, fur[0], fur[1], fur[2], 230);
        writeSpike(buffer, matrix, x + 0.04f * scale, y + 0.12f * scale, z, x + 0.06f * scale, y + 0.20f * scale, z - 0.01f * scale, 0.018f * scale, fur[0], fur[1], fur[2], 230);
        writeBox(buffer, matrix, x + look, y + 0.06f * scale, z + 0.06f * scale, 0.04f * scale, 0.03f * scale, 0.03f * scale, white[0], white[1], white[2], 230);
        writeSpike(buffer, matrix, x, y - 0.02f * scale, z - 0.04f * scale, x - 0.02f * scale, y - 0.02f * scale, z - 0.16f * scale, 0.016f * scale, fur[0], fur[1], fur[2], 220);
        writeDiscAt(buffer, matrix, x - 0.02f * scale + look, y + 0.09f * scale, z + 0.06f * scale, 0.012f * scale, 8, 20, 20, 20, 240);
        writeDiscAt(buffer, matrix, x + 0.02f * scale + look, y + 0.09f * scale, z + 0.06f * scale, 0.012f * scale, 8, 20, 20, 20, 240);
    }

    private void writeDragonPet(BufferBuilder buffer, Matrix4f matrix, float x, float y, float z, float scale, float time, int r, int g, int b, boolean glow) {
        int[] scaleC = new int[]{clamp(r / 2 + 40), clamp(g / 3 + 90), clamp(b / 2 + 40)};
        float wing = (float) Math.sin(time * 4.5d) * 0.08f * scale;
        if (glow) {
            writeDiscAt(buffer, matrix, x, y, z, 0.15f * scale, 12, scaleC[0], scaleC[1], scaleC[2], 32);
            return;
        }
        writeBox(buffer, matrix, x, y, z, 0.09f * scale, 0.07f * scale, 0.14f * scale, scaleC[0], scaleC[1], scaleC[2], 225);
        writeBox(buffer, matrix, x, y + 0.07f * scale, z + 0.04f * scale, 0.07f * scale, 0.06f * scale, 0.08f * scale, scaleC[0], scaleC[1], scaleC[2], 230);
        writeSpike(buffer, matrix, x, y + 0.10f * scale, z + 0.08f * scale, x, y + 0.10f * scale, z + 0.14f * scale, 0.014f * scale, 255, 80, 40, 220);
        writeSpike(buffer, matrix, x, y + 0.02f * scale, z - 0.06f * scale, x, y + 0.04f * scale, z - 0.18f * scale, 0.014f * scale, scaleC[0], scaleC[1], scaleC[2], 220);
        quad(buffer, matrix, x, y + 0.02f * scale, z, x + 0.02f * scale, y + 0.06f * scale, z,
                x + 0.16f * scale, y + 0.08f * scale + wing, z - 0.02f * scale, x + 0.10f * scale, y - 0.02f * scale, z, r, g, b, 170);
        quad(buffer, matrix, x, y + 0.02f * scale, z, x - 0.02f * scale, y + 0.06f * scale, z,
                x - 0.16f * scale, y + 0.08f * scale + wing, z - 0.02f * scale, x - 0.10f * scale, y - 0.02f * scale, z, r, g, b, 170);
        for (int i = 0; i < 3; i++) {
            float t = i / 2.0f;
            writeSpike(buffer, matrix, x, y + 0.04f * scale + t * 0.04f * scale, z - 0.02f * scale - t * 0.06f * scale,
                    x, y + 0.10f * scale + t * 0.02f * scale, z - 0.02f * scale - t * 0.06f * scale, 0.01f * scale, 255, 220, 80, 210);
        }
    }

    private void writeGhostPet(BufferBuilder buffer, Matrix4f matrix, float x, float y, float z, float scale, float time, int r, int g, int b, boolean glow) {
        float sway = (float) Math.sin(time * 2.2d) * 0.03f * scale;
        int[] mist = tint(r, g, b, 0.55f);
        if (glow) {
            writeDiscAt(buffer, matrix, x, y, z, 0.16f * scale, 14, mist[0], mist[1], mist[2], 40);
            return;
        }
        writeDiscAt(buffer, matrix, x + sway, y + 0.04f * scale, z, 0.09f * scale, 14, mist[0], mist[1], mist[2], 150);
        writeBox(buffer, matrix, x + sway, y - 0.02f * scale, z, 0.10f * scale, 0.12f * scale, 0.08f * scale, mist[0], mist[1], mist[2], 140);
        for (int i = 0; i < 4; i++) {
            float ox = (i - 1.5f) * 0.03f * scale;
            float tip = 0.06f * scale + 0.02f * scale * (float) Math.sin(time * 3.0d + i);
            writeSpike(buffer, matrix, x + sway + ox, y - 0.08f * scale, z, x + sway + ox, y - 0.08f * scale - tip, z, 0.016f * scale, mist[0], mist[1], mist[2], 130);
        }
        writeDiscAt(buffer, matrix, x + sway - 0.025f * scale, y + 0.05f * scale, z + 0.04f * scale, 0.014f * scale, 8, 20, 20, 30, 230);
        writeDiscAt(buffer, matrix, x + sway + 0.025f * scale, y + 0.05f * scale, z + 0.04f * scale, 0.014f * scale, 8, 20, 20, 30, 230);
    }

    private void writeStarPet(BufferBuilder buffer, Matrix4f matrix, float x, float y, float z, float scale, float time, int r, int g, int b, boolean glow) {
        float spin = time * 1.8f;
        float pulse = 0.5f + 0.5f * (float) Math.sin(time * 5.0d);
        if (glow) {
            writeDiscAt(buffer, matrix, x, y, z, 0.18f * scale, 16, 255, 230, 120, Math.round(30 + 25 * pulse));
            return;
        }
        int points = 5;
        for (int i = 0; i < points; i++) {
            double a0 = spin + i * (Math.PI * 2.0d / points) - Math.PI * 0.5d;
            double a1 = spin + (i + 0.5d) * (Math.PI * 2.0d / points) - Math.PI * 0.5d;
            double a2 = spin + (i + 1) * (Math.PI * 2.0d / points) - Math.PI * 0.5d;
            float x0 = x + (float) Math.cos(a0) * 0.12f * scale;
            float y0 = y + (float) Math.sin(a0) * 0.12f * scale;
            float x1 = x + (float) Math.cos(a1) * 0.05f * scale;
            float y1 = y + (float) Math.sin(a1) * 0.05f * scale;
            float x2 = x + (float) Math.cos(a2) * 0.12f * scale;
            float y2 = y + (float) Math.sin(a2) * 0.12f * scale;
            quad(buffer, matrix, x, y, z, x0, y0, z, x1, y1, z, x2, y2, z, 255, 230, 120, 220);
        }
        writeDiscAt(buffer, matrix, x, y, z, 0.035f * scale * (0.9f + 0.2f * pulse), 10, 255, 255, 255, 230);
        writeTorus(buffer, matrix, x, y, z, 0.16f * scale, 0.008f * scale, 20, 6, -spin, r, g, b, 140);
    }

    private void writeCone(BufferBuilder buffer, Matrix4f matrix, float cx, float cy, float cz, float radius, float height, int segs, int r, int g, int b, int a) {
        double step = (Math.PI * 2.0d) / segs;
        for (int i = 0; i < segs; i++) {
            double a1 = i * step;
            double a2 = (i + 1) * step;
            float x1 = cx + (float) (Math.cos(a1) * radius);
            float z1 = cz + (float) (Math.sin(a1) * radius);
            float x2 = cx + (float) (Math.cos(a2) * radius);
            float z2 = cz + (float) (Math.sin(a2) * radius);
            quad(buffer, matrix, cx, cy + height, cz, x1, cy, z1, x2, cy, z2, cx, cy + height, cz, r, g, b, a);
        }
    }

    private void writeBand(BufferBuilder buffer, Matrix4f matrix, float cx, float cy, float cz, float radius, float height, float thickness, int segs, int r, int g, int b, int a) {
        double step = (Math.PI * 2.0d) / segs;
        float y0 = cy;
        float y1 = cy + height;
        float inner = Math.max(0.01f, radius - thickness);
        for (int i = 0; i < segs; i++) {
            double a1 = i * step;
            double a2 = (i + 1) * step;
            float ox1 = cx + (float) (Math.cos(a1) * radius);
            float oz1 = cz + (float) (Math.sin(a1) * radius);
            float ox2 = cx + (float) (Math.cos(a2) * radius);
            float oz2 = cz + (float) (Math.sin(a2) * radius);
            float ix1 = cx + (float) (Math.cos(a1) * inner);
            float iz1 = cz + (float) (Math.sin(a1) * inner);
            float ix2 = cx + (float) (Math.cos(a2) * inner);
            float iz2 = cz + (float) (Math.sin(a2) * inner);
            quad(buffer, matrix, ox1, y0, oz1, ox2, y0, oz2, ox2, y1, oz2, ox1, y1, oz1, r, g, b, a);
            quad(buffer, matrix, ix1, y1, iz1, ix2, y1, iz2, ix2, y0, iz2, ix1, y0, iz1, r, g, b, Math.max(40, a - 40));
            quad(buffer, matrix, ox1, y1, oz1, ox2, y1, oz2, ix2, y1, iz2, ix1, y1, iz1, r, g, b, a);
        }
    }

    private void writeBox(BufferBuilder buffer, Matrix4f matrix, float cx, float cy, float cz, float hx, float hy, float hz, int r, int g, int b, int a) {
        float x0 = cx - hx * 0.5f;
        float x1 = cx + hx * 0.5f;
        float y0 = cy - hy * 0.5f;
        float y1 = cy + hy * 0.5f;
        float z0 = cz - hz * 0.5f;
        float z1 = cz + hz * 0.5f;
        quad(buffer, matrix, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, r, g, b, a);
        quad(buffer, matrix, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, r, g, b, a);
        quad(buffer, matrix, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, r, g, b, a);
        quad(buffer, matrix, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, r, g, b, Math.max(40, a - 50));
        quad(buffer, matrix, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, r, g, b, Math.max(40, a - 30));
        quad(buffer, matrix, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, r, g, b, Math.max(40, a - 30));
    }

    private void writeCrystal(BufferBuilder buffer, Matrix4f matrix, float cx, float cy, float cz, float radius, float height, int[] core, int[] shell, int a) {
        float tipY = cy + height * 0.55f;
        float botY = cy - height * 0.45f;
        int segs = 6;
        double step = (Math.PI * 2.0d) / segs;
        for (int i = 0; i < segs; i++) {
            double a1 = i * step;
            double a2 = (i + 1) * step;
            float x1 = cx + (float) (Math.cos(a1) * radius);
            float z1 = cz + (float) (Math.sin(a1) * radius);
            float x2 = cx + (float) (Math.cos(a2) * radius);
            float z2 = cz + (float) (Math.sin(a2) * radius);
            quad(buffer, matrix, cx, tipY, cz, x1, cy, z1, x2, cy, z2, cx, tipY, cz, shell[0], shell[1], shell[2], a);
            quad(buffer, matrix, cx, botY, cz, x2, cy, z2, x1, cy, z1, cx, botY, cz, core[0], core[1], core[2], a);
        }
    }

    private void writeHalo(BufferBuilder buffer, Matrix4f matrix, float height, float scale, float time, int r, int g, int b, boolean glow) {
        float y = height + 0.34f * scale;
        String style = this.haloStyle.c();
        float major = "Святой".equals(style) ? 0.34f * scale : ("Демон".equals(style) ? 0.25f * scale : 0.29f * scale);
        int cr = r;
        int cg = g;
        int cb = b;
        if ("Классический".equals(style)) {
            cr = 255;
            cg = 214;
            cb = 120;
        } else if ("Святой".equals(style)) {
            cr = 255;
            cg = 236;
            cb = 186;
        } else if ("Демон".equals(style)) {
            cr = clamp(r + 40);
            cg = clamp(g / 5);
            cb = clamp(b / 6);
        }

        if (glow) {
            writeDisc(buffer, matrix, y, major * 2.15f, 40, cr, cg, cb, 22);
            writeDisc(buffer, matrix, y, major * 1.45f, 36, cr, cg, cb, 40);
            writeDisc(buffer, matrix, y, major * 0.62f, 28, 255, 250, 240, 55);
            writeTorus(buffer, matrix, 0.0f, y, 0.0f, major, 0.046f * scale, 48, 10, time * 0.25f, cr, cg, cb, 70);
            return;
        }

        writeTorus(buffer, matrix, 0.0f, y, 0.0f, major, 0.022f * scale, 56, 12, time * 0.35f, cr, cg, cb, 230);
        writeTorus(buffer, matrix, 0.0f, y, 0.0f, major * 0.82f, 0.011f * scale, 48, 8, -time * 0.5f, 255, 252, 245, 200);

        int motes = "Святой".equals(style) ? 10 : 7;
        for (int i = 0; i < motes; i++) {
            double a = time * 1.15d + (i * (Math.PI * 2.0d / motes));
            float mx = (float) (Math.cos(a) * major);
            float mz = (float) (Math.sin(a) * major);
            float bob = (float) Math.sin(time * 3.2d + i) * 0.018f * scale;
            writeDiscAt(buffer, matrix, mx, y + bob, mz, 0.028f * scale, 10, 255, 255, 255, 210);
        }

        if ("Святой".equals(style) || "Неон".equals(style)) {
            int rays = "Святой".equals(style) ? 12 : 8;
            for (int i = 0; i < rays; i++) {
                double a = i * (Math.PI * 2.0d / rays) + time * 0.12d;
                float x0 = (float) (Math.cos(a) * major * 0.72f);
                float z0 = (float) (Math.sin(a) * major * 0.72f);
                float x1 = (float) (Math.cos(a) * major * ("Святой".equals(style) ? 1.55f : 1.28f));
                float z1 = (float) (Math.sin(a) * major * ("Святой".equals(style) ? 1.55f : 1.28f));
                float px = (float) (-Math.sin(a) * 0.012f * scale);
                float pz = (float) (Math.cos(a) * 0.012f * scale);
                quad(buffer, matrix, x0 + px, y, z0 + pz, x0 - px, y, z0 - pz, x1 - px, y, z1 - pz, x1 + px, y, z1 + pz, cr, cg, cb, 120);
            }
        }
        if ("Демон".equals(style)) {
            for (int i = 0; i < 6; i++) {
                double a = i * (Math.PI * 2.0d / 6.0d) + 0.22d;
                float ix = (float) (Math.cos(a) * major * 0.9f);
                float iz = (float) (Math.sin(a) * major * 0.9f);
                float ox = (float) (Math.cos(a) * major * 1.18f);
                float oz = (float) (Math.sin(a) * major * 1.18f);
                writeSpike(buffer, matrix, ix, y, iz, ox, y + 0.16f * scale, oz, 0.018f * scale, cr, cg, cb, 220);
            }
        }
    }

    private void writeWing(BufferBuilder buffer, Matrix4f matrix, float side, float height, float scale, float time, int r, int g, int b, boolean glow) {
        float rootX = side * 0.16f * scale;
        float rootY = height * 0.56f;
        float rootZ = 0.10f * scale;
        float raise = (float) Math.sin(time * 2.05d) * 0.16f;
        float spread = 0.96f + (float) Math.sin(time * 2.05d - 0.4d) * 0.07f;
        String style = this.wingStyle.c();
        if ("Демон".equals(style)) {
            writeBatWing(buffer, matrix, side, rootX, rootY, rootZ, scale, raise, spread, r, g, b, glow);
        } else if ("Дракон".equals(style)) {
            writeDragonWing(buffer, matrix, side, rootX, rootY, rootZ, scale, raise, spread, r, g, b, glow);
        } else if ("Бабочка".equals(style)) {
            writeButterflyWing(buffer, matrix, side, rootX, rootY, rootZ, scale, raise, spread, time, r, g, b, glow);
        } else if ("Феникс".equals(style)) {
            writePhoenixWing(buffer, matrix, side, rootX, rootY, rootZ, scale, raise, spread, time, r, g, b, glow);
        } else {
            writeAngelWing(buffer, matrix, side, rootX, rootY, rootZ, scale, raise, spread, r, g, b, glow);
        }
    }

    private void writeAngelWing(BufferBuilder buffer, Matrix4f matrix, float side, float rx, float ry, float rz, float scale, float raise, float spread, int r, int g, int b, boolean glow) {
        int[] cream = new int[]{255, 246, 228};
        int[] silk = new int[]{248, 244, 255};
        int[] gold = new int[]{255, 226, 168};
        int[] vane = tint(r, g, b, 0.22f);
        if (glow) {
            writeLobe(buffer, matrix, side, rx, ry, rz, scale, 16, 0.82f, 0.02f, 1.05f, 0.12f, 0.36f, raise, spread, cream, 36);
            writeLobe(buffer, matrix, side, rx, ry, rz, scale, 12, 1.08f, 0.18f, 1.18f, 0.04f, 0.48f, raise, spread, gold, 22);
            return;
        }
        writeLobe(buffer, matrix, side, rx, ry, rz, scale, 10, 0.42f, 0.00f, 0.55f, 0.10f, 0.18f, raise, spread, gold, 70);
        writeFeatherLayer(buffer, matrix, side, rx, ry, rz, scale, 6, 0.50f, -0.04f, 0.48f, 0.14f, 0.22f, 0.048f, 0.016f, raise, spread, cream, 155, 4);
        writeFeatherLayer(buffer, matrix, side, rx, ry, rz, scale, 8, 0.78f, 0.10f, 0.82f, 0.10f, 0.38f, 0.058f, 0.016f, raise, spread, silk, 180, 5);
        writeFeatherLayer(buffer, matrix, side, rx, ry, rz, scale, 9, 1.08f, 0.28f, 1.16f, 0.02f, 0.52f, 0.046f, 0.011f, raise, spread, vane, 200, 6);
        writeFeatherLayer(buffer, matrix, side, rx, ry, rz, scale, 5, 0.62f, 0.18f, 0.58f, 0.18f, 0.26f, 0.028f, 0.010f, raise, spread, gold, 140, 4);
    }

    private void writeBatWing(BufferBuilder buffer, Matrix4f matrix, float side, float rx, float ry, float rz, float scale, float raise, float spread, int r, int g, int b, boolean glow) {
        int cr = clamp(r + 24);
        int cg = clamp(g / 5 + 8);
        int cb = clamp(b / 6 + 10);
        float[] angs = {-0.38f, 0.08f, 0.52f, 0.92f, 1.28f};
        float[] lens = {0.88f, 1.16f, 1.12f, 0.84f, 0.52f};
        Vec3d[] tips = new Vec3d[angs.length];
        for (int i = 0; i < angs.length; i++) {
            tips[i] = boneTip(side, lens[i], angs[i], 0.05f, 0.26f, raise, spread, scale);
        }
        if (glow) {
            for (int i = 0; i < tips.length - 1; i++) {
                quad(buffer, matrix, rx, ry, rz,
                        rx + (float) tips[i].x, ry + (float) tips[i].y, rz + (float) tips[i].z,
                        rx + (float) tips[i + 1].x, ry + (float) tips[i + 1].y, rz + (float) tips[i + 1].z,
                        rx + side * 0.03f * scale, ry - 0.02f * scale, rz,
                        cr, cg, cb, 45);
            }
            return;
        }
        for (int i = 0; i < tips.length - 1; i++) {
            float shade = 0.55f + 0.1f * i;
            quad(buffer, matrix, rx, ry, rz,
                    rx + (float) tips[i].x, ry + (float) tips[i].y, rz + (float) tips[i].z,
                    rx + (float) tips[i + 1].x, ry + (float) tips[i + 1].y, rz + (float) tips[i + 1].z,
                    rx + side * 0.03f * scale, ry - 0.02f * scale, rz,
                    clamp((int) (cr * shade)), clamp((int) (cg * shade)), clamp((int) (cb * shade)), 150);
            Vec3d mid = boneTip(side, lens[i] * 0.55f, angs[i], 0.03f, 0.14f, raise, spread, scale);
            writeSpike(buffer, matrix, rx, ry, rz, rx + (float) tips[i].x, ry + (float) tips[i].y, rz + (float) tips[i].z, 0.010f * scale, 40, 10, 12, 230);
            writeDiscAt(buffer, matrix, rx + (float) mid.x, ry + (float) mid.y, rz + (float) mid.z, 0.016f * scale, 8, cr, cg, cb, 220);
        }
        Vec3d last = tips[tips.length - 1];
        writeSpike(buffer, matrix, rx, ry, rz, rx + (float) last.x, ry + (float) last.y, rz + (float) last.z, 0.010f * scale, 40, 10, 12, 230);
    }

    private void writeDragonWing(BufferBuilder buffer, Matrix4f matrix, float side, float rx, float ry, float rz, float scale, float raise, float spread, int r, int g, int b, boolean glow) {
        writeBatWing(buffer, matrix, side, rx, ry, rz, scale * 1.08f, raise, spread, r, g, b, glow);
        if (!glow) {
            for (int i = 0; i < 6; i++) {
                float t = i / 5.0f;
                Vec3d spine = boneTip(side, 0.20f + t * 1.05f, -0.16f + t * 0.10f, 0.18f, 0.08f, raise, spread, scale);
                Vec3d tip = spine.add(0.0d, 0.07f * scale * (1.0f - t * 0.35f), 0.0d);
                writeSpike(buffer, matrix, rx + (float) spine.x, ry + (float) spine.y, rz + (float) spine.z,
                        rx + (float) tip.x, ry + (float) tip.y, rz + (float) tip.z, 0.012f * scale, clamp(r + 30), clamp(g / 3), 24, 230);
            }
        }
    }

    private void writeButterflyWing(BufferBuilder buffer, Matrix4f matrix, float side, float rx, float ry, float rz, float scale, float raise, float spread, float time, int r, int g, int b, boolean glow) {
        float shimmer = (float) ((Math.sin(time * 2.2d) + 1.0d) * 0.5d);
        int[] upper = new int[]{clamp((int) (r * (0.55f + 0.45f * shimmer))), clamp((int) (g * (0.35f + 0.5f * (1.0f - shimmer)))), clamp((int) (b * (0.7f + 0.3f * shimmer)))};
        int[] lower = new int[]{clamp(r / 2 + 50), clamp(g / 2 + 24), clamp(b)};
        writeLobe(buffer, matrix, side, rx, ry, rz, scale, 14, 0.82f, 0.18f, 1.32f, 0.16f, 0.10f, raise, spread, upper, glow ? 50 : 165);
        writeLobe(buffer, matrix, side, rx, ry, rz, scale, 10, 0.54f, -0.58f, 0.18f, -0.04f, 0.12f, raise, spread, lower, glow ? 40 : 150);
        if (!glow) {
            Vec3d eye = boneTip(side, 0.50f, 0.78f, 0.16f, 0.10f, raise, spread, scale);
            writeDiscAt(buffer, matrix, rx + (float) eye.x, ry + (float) eye.y, rz + (float) eye.z, 0.058f * scale, 12, 18, 12, 28, 210);
            writeDiscAt(buffer, matrix, rx + (float) eye.x, ry + (float) eye.y, rz + (float) eye.z, 0.026f * scale, 10, 255, 230, 120, 230);
        }
    }

    private void writePhoenixWing(BufferBuilder buffer, Matrix4f matrix, float side, float rx, float ry, float rz, float scale, float raise, float spread, float time, int r, int g, int b, boolean glow) {
        int[] gold = new int[]{255, 220, 110};
        int[] orange = new int[]{255, 132, 28};
        int[] ember = new int[]{255, 52, 16};
        int[] tip = new int[]{255, 236, 120};
        float flicker = 0.06f * (float) Math.sin(time * 6.4d);
        if (glow) {
            writeLobe(buffer, matrix, side, rx, ry, rz, scale, 14, 0.95f + flicker, 0.08f, 1.18f, 0.18f, 0.42f, raise, spread, orange, 40);
            writeLobe(buffer, matrix, side, rx, ry, rz, scale, 10, 1.18f, 0.28f, 1.32f, 0.08f, 0.56f, raise, spread, ember, 28);
            int embers = 10;
            for (int i = 0; i < embers; i++) {
                float t = i / (float) embers;
                Vec3d p = boneTip(side, 0.38f + t * 0.92f, 0.12f + t * 0.95f, 0.18f + (float) Math.sin(time * 5.8d + i) * 0.07f, 0.40f, raise, spread, scale);
                float pulse = 0.50f + 0.50f * (float) Math.sin(time * 8.0d + i * 1.15d);
                writeDiscAt(buffer, matrix, rx + (float) p.x, ry + (float) p.y, rz + (float) p.z, (0.020f + 0.018f * pulse) * scale, 8, 255, clamp(90 + (int) (110 * pulse)), 28, Math.round(170 * pulse));
            }
            return;
        }
        writeFeatherLayer(buffer, matrix, side, rx, ry, rz, scale, 6, 0.58f, -0.02f, 0.52f, 0.16f, 0.22f, 0.050f, 0.016f, raise, spread, gold, 150, 4);
        writeFeatherLayer(buffer, matrix, side, rx, ry, rz, scale, 8, 0.86f + flicker, 0.10f, 0.88f, 0.14f, 0.38f, 0.060f, 0.016f, raise, spread, orange, 185, 5);
        writeFeatherLayer(buffer, matrix, side, rx, ry, rz, scale, 9, 1.14f + flicker, 0.28f, 1.22f, 0.06f, 0.52f, 0.048f, 0.010f, raise, spread, ember, 205, 6);
        writeFeatherLayer(buffer, matrix, side, rx, ry, rz, scale, 6, 1.02f, 0.42f, 1.08f, 0.22f, 0.46f, 0.032f, 0.008f, raise, spread, tip, 160, 4);
    }

    private void writeFeatherLayer(BufferBuilder buffer, Matrix4f matrix, float side, float rx, float ry, float rz, float scale, int count, float len, float ang0, float ang1, float lift, float back, float rootW, float tipW, float raise, float spread, int[] rgb, int alpha, int segs) {
        for (int i = 0; i < count; i++) {
            float t = count <= 1 ? 0.5f : i / (float) (count - 1);
            float ang = ang0 + (ang1 - ang0) * t + raise * (0.16f + 0.84f * t);
            float length = len * (0.78f + 0.22f * (float) Math.sin(t * Math.PI)) * spread;
            Vec3d p0 = boneTip(side, length * 0.08f, ang, lift * 0.2f, back * 0.15f, 0.0f, 1.0f, scale);
            Vec3d ctrl = boneTip(side, length * 0.52f, ang - 0.08f, lift * 0.7f + 0.08f, back * 0.55f, 0.0f, 1.0f, scale);
            Vec3d p1 = boneTip(side, length, ang, lift, back, 0.0f, 1.0f, scale);
            int sr = clamp(rgb[0] + (int) ((255 - rgb[0]) * (1.0f - t) * 0.28f));
            int sg = clamp(rgb[1] + (int) ((248 - rgb[1]) * (1.0f - t) * 0.20f));
            int sb = clamp(rgb[2] + (int) ((255 - rgb[2]) * (1.0f - t) * 0.16f));
            int a = clamp((int) (alpha * (0.75f + 0.25f * (1.0f - t))));
            Vec3d prev = p0;
            for (int s = 1; s <= segs; s++) {
                float u = s / (float) segs;
                Vec3d next = bezier(p0, ctrl, p1, u);
                float w0 = rootW * (1.0f - (u - 1.0f / segs)) + tipW * (u - 1.0f / segs);
                float w1 = rootW * (1.0f - u) + tipW * u;
                writeFeather(buffer, matrix,
                        rx + (float) prev.x, ry + (float) prev.y, rz + (float) prev.z,
                        rx + (float) next.x, ry + (float) next.y, rz + (float) next.z,
                        w0 * scale, w1 * scale, sr, sg, sb, a);
                prev = next;
            }
        }
    }

    private void writeLobe(BufferBuilder buffer, Matrix4f matrix, float side, float rx, float ry, float rz, float scale, int segs, float len, float ang0, float ang1, float lift, float back, float raise, float spread, int[] rgb, int alpha) {
        Vec3d prev = boneTip(side, len * 0.16f * spread, ang0 + raise * 0.18f, lift, back, 0.0f, 1.0f, scale);
        for (int i = 1; i <= segs; i++) {
            float t = i / (float) segs;
            float ang = ang0 + (ang1 - ang0) * t + raise * 0.22f;
            float lobe = (float) Math.sin(t * Math.PI);
            Vec3d next = boneTip(side, len * (0.32f + 0.68f * lobe) * spread, ang, lift + lobe * 0.10f, back, 0.0f, 1.0f, scale);
            quad(buffer, matrix, rx, ry, rz,
                    rx + (float) prev.x, ry + (float) prev.y, rz + (float) prev.z,
                    rx + (float) next.x, ry + (float) next.y, rz + (float) next.z,
                    rx + side * 0.025f * scale, ry, rz,
                    rgb[0], rgb[1], rgb[2], Math.round(alpha * (0.58f + 0.42f * lobe)));
            prev = next;
        }
    }

    private Vec3d boneTip(float side, float length, float ang, float lift, float back, float raise, float spread, float scale) {
        float use = length * spread;
        float x = use * (float) Math.cos(ang);
        float y = lift + use * (float) Math.sin(ang) + x * raise * 0.55f;
        float z = back + use * (0.42f + 0.28f * (float) Math.abs(Math.sin(ang)));
        double fold = Math.toRadians(54.0);
        double cos = Math.cos(fold);
        double sin = Math.sin(fold);
        double lx = x * cos;
        double lz = z + x * sin;
        return new Vec3d(side * lx * scale, y * scale, -lz * scale);
    }

    private static Vec3d bezier(Vec3d a, Vec3d b, Vec3d c, float t) {
        float u = 1.0f - t;
        return a.multiply(u * u).add(b.multiply(2.0f * u * t)).add(c.multiply(t * t));
    }

    private void writeFeather(BufferBuilder buffer, Matrix4f matrix, float x0, float y0, float z0, float x1, float y1, float z1, float rootW, float tipW, int r, int g, int b, int a) {
        float dx = x1 - x0;
        float dy = y1 - y0;
        float dz = z1 - z0;
        float len = MathHelper.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 0.001f) {
            return;
        }
        dx /= len;
        dy /= len;
        dz /= len;
        float px = dy * 0.18f - dz;
        float py = dz * 0.12f - dx * 0.18f;
        float pz = dx - dy * 0.12f;
        float plen = MathHelper.sqrt(px * px + py * py + pz * pz);
        if (plen < 0.001f) {
            px = -dy;
            py = dx;
            pz = 0.12f;
            plen = MathHelper.sqrt(px * px + py * py + pz * pz);
        }
        px /= plen;
        py /= plen;
        pz /= plen;
        quad(buffer, matrix,
                x0 + px * rootW, y0 + py * rootW, z0 + pz * rootW,
                x0 - px * rootW, y0 - py * rootW, z0 - pz * rootW,
                x1 - px * tipW, y1 - py * tipW, z1 - pz * tipW,
                x1 + px * tipW, y1 + py * tipW, z1 + pz * tipW,
                r, g, b, a);
        quad(buffer, matrix,
                x0 + px * rootW * 0.28f, y0 + py * rootW * 0.28f, z0 + pz * rootW * 0.28f,
                x0 - px * rootW * 0.28f, y0 - py * rootW * 0.28f, z0 - pz * rootW * 0.28f,
                x1 - px * tipW * 0.16f, y1 - py * tipW * 0.16f, z1 - pz * tipW * 0.16f,
                x1 + px * tipW * 0.16f, y1 + py * tipW * 0.16f, z1 + pz * tipW * 0.16f,
                255, 252, 245, Math.max(30, a / 4));
    }

    private static int[] tint(int r, int g, int b, float towardsWhite) {
        return new int[]{
                clamp((int) (r * (1.0f - towardsWhite) + 255 * towardsWhite)),
                clamp((int) (g * (1.0f - towardsWhite) + 248 * towardsWhite)),
                clamp((int) (b * (1.0f - towardsWhite) + 240 * towardsWhite))
        };
    }

    private void writeTorus(BufferBuilder buffer, Matrix4f matrix, float cx, float cy, float cz, float major, float minor, int ring, int tube, float rot, int r, int g, int b, int a) {
        double us = (Math.PI * 2.0d) / ring;
        double vs = (Math.PI * 2.0d) / tube;
        for (int i = 0; i < ring; i++) {
            double u0 = rot + i * us;
            double u1 = rot + (i + 1) * us;
            for (int j = 0; j < tube; j++) {
                double v0 = j * vs;
                double v1 = (j + 1) * vs;
                float[] p00 = torus(cx, cy, cz, major, minor, u0, v0);
                float[] p10 = torus(cx, cy, cz, major, minor, u1, v0);
                float[] p11 = torus(cx, cy, cz, major, minor, u1, v1);
                float[] p01 = torus(cx, cy, cz, major, minor, u0, v1);
                int shade = (int) (a * (0.62f + 0.38f * (float) ((Math.cos(v0) + 1.0d) * 0.5d)));
                quad(buffer, matrix, p00[0], p00[1], p00[2], p10[0], p10[1], p10[2], p11[0], p11[1], p11[2], p01[0], p01[1], p01[2], r, g, b, shade);
            }
        }
    }

    private float[] torus(float cx, float cy, float cz, float major, float minor, double u, double v) {
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

    private void writeDisc(BufferBuilder buffer, Matrix4f matrix, float y, float radius, int segments, int r, int g, int b, int a) {
        writeDiscAt(buffer, matrix, 0.0f, y, 0.0f, radius, segments, r, g, b, a);
    }

    private void writeDiscAt(BufferBuilder buffer, Matrix4f matrix, float cx, float cy, float cz, float radius, int segments, int r, int g, int b, int a) {
        double step = (Math.PI * 2.0d) / segments;
        for (int i = 0; i < segments; i++) {
            double a1 = i * step;
            double a2 = (i + 1) * step;
            quad(buffer, matrix,
                    cx, cy, cz,
                    cx + (float) (Math.cos(a1) * radius), cy, cz + (float) (Math.sin(a1) * radius),
                    cx + (float) (Math.cos(a2) * radius), cy, cz + (float) (Math.sin(a2) * radius),
                    cx, cy, cz,
                    r, g, b, a);
        }
    }

    private void writeSpike(BufferBuilder buffer, Matrix4f matrix, float x0, float y0, float z0, float x1, float y1, float z1, float radius, int r, int g, int b, int a) {
        writeFeather(buffer, matrix, x0, y0, z0, x1, y1, z1, radius, radius * 0.15f, r, g, b, a);
    }

    private void quad(BufferBuilder buffer, Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, int r, int g, int b, int a) {
        int alpha = Math.max(0, Math.min(255, a));
        buffer.vertex(matrix, x1, y1, z1).color(r, g, b, alpha);
        buffer.vertex(matrix, x2, y2, z2).color(r, g, b, alpha);
        buffer.vertex(matrix, x3, y3, z3).color(r, g, b, alpha);
        buffer.vertex(matrix, x4, y4, z4).color(r, g, b, alpha);
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    private static void begin(boolean additive) {
        RenderSystem.enableBlend();
        if (additive) {
            RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SrcFactor.SRC_ALPHA, com.mojang.blaze3d.platform.GlStateManager.DstFactor.ONE);
        } else {
            RenderSystem.defaultBlendFunc();
        }
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
    }

    private static void endBatch() {
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }
}
