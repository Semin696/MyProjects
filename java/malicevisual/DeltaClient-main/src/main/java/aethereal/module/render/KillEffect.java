package aethereal.module.render;

import aethereal.config.ThemeInfo;
import aethereal.core.Category;
import aethereal.core.EventTarget;
import aethereal.core.Module;
import aethereal.core.ModuleRegister;
import aethereal.core.Skeleton;
import aethereal.event.AttackEvent;
import aethereal.event.DeathEvent;
import aethereal.event.DrawEvent;
import aethereal.event.TickEvent;
import aethereal.render.ColorUtil;
import aethereal.setting.BooleanSetting;
import aethereal.setting.ColorSetting;
import aethereal.setting.ModeSetting;
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
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

@ModuleRegister(name = "Kill Effect", description = "Молния и звук, когда вы убиваете игрока или моба", category = Category.Render)
public class KillEffect extends Module {
    private final ModeSetting style = new ModeSetting("Эффект", "Молния", "Молния", "Взрыв", "Тотем", "Искры");
    private final BooleanSetting syncTheme = new BooleanSetting("Цвет из темы", true);
    private final ColorSetting customColor = new ColorSetting("Цвет", Integer.valueOf(ColorUtil.convertToARGB(151, 71, 255, 255))).a(() -> {
        return Boolean.valueOf(!this.syncTheme.c().booleanValue());
    });
    private final BooleanSetting players = new BooleanSetting("Игроки", true);
    private final BooleanSetting mobs = new BooleanSetting("Мобы", false);
    private final BooleanSetting playSound = new BooleanSetting("Звук", true);
    private final ModeSetting soundType = new ModeSetting("Тип звука", "Молния", "Молния", "Взрыв", "Тотем", "Крит", "Случайный").a(() -> this.playSound.c());
    private final SliderSetting volume = new SliderSetting("Громкость", 70.0f, 0.0f, 100.0f, 1.0f).a(() -> this.playSound.c());
    private final List<Lightning> lightnings = new ArrayList<>();
    private final Random random = new Random();
    private LivingEntity lastAttacked;
    private long lastAttackAt;
    private int lastKillId = -1;
    private long lastKillAt;

    public KillEffect() {
        a(this.style, this.syncTheme, this.customColor, this.players, this.mobs, this.playSound, this.soundType, this.volume);
    }

    @Override
    public void c() {
        this.lightnings.clear();
        this.lastAttacked = null;
        super.c();
    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        Entity target = event.b();
        if (target instanceof LivingEntity living && living != mc.player) {
            this.lastAttacked = living;
            this.lastAttackAt = System.currentTimeMillis();
        }
    }

    @EventTarget
    public void onDeath(DeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (event.getSource() == null || event.getSource().getAttacker() != mc.player) {
            return;
        }
        spawnIfAllowed(entity);
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (mc.world == null || this.lastAttacked == null) {
            return;
        }
        if (System.currentTimeMillis() - this.lastAttackAt > 2500L) {
            this.lastAttacked = null;
            return;
        }
        if (this.lastAttacked.isRemoved() || this.lastAttacked.isDead() || this.lastAttacked.getHealth() <= 0.0f) {
            spawnIfAllowed(this.lastAttacked);
            this.lastAttacked = null;
        }
    }

    @EventTarget
    public void onDraw(DrawEvent event) {
        if (!event.c() || this.lightnings.isEmpty() || mc.gameRenderer == null) {
            return;
        }
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d cam = camera.getPos();
        MatrixStack matrices = event.h();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SrcFactor.SRC_ALPHA, com.mojang.blaze3d.platform.GlStateManager.DstFactor.ONE);
        RenderSystem.enableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        Iterator<Lightning> iterator = this.lightnings.iterator();
        while (iterator.hasNext()) {
            Lightning bolt = iterator.next();
            float alpha = bolt.alpha();
            if (alpha <= 0.02f) {
                iterator.remove();
                continue;
            }
            bolt.render(buffer, matrices, camera, cam, alpha);
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private void spawnIfAllowed(LivingEntity entity) {
        if (entity == null || entity == mc.player || entity.isSpectator()) {
            return;
        }
        if (entity instanceof PlayerEntity) {
            if (!this.players.c().booleanValue()) {
                return;
            }
        } else if (!this.mobs.c().booleanValue()) {
            return;
        }
        long now = System.currentTimeMillis();
        if (entity.getId() == this.lastKillId && now - this.lastKillAt < 400L) {
            return;
        }
        this.lastKillId = entity.getId();
        this.lastKillAt = now;
        int color = this.syncTheme.c().booleanValue()
                ? Skeleton.getInstance().getModuleProcessor().o().a(ThemeInfo.PRIMARY).toIntColor()
                : this.customColor.c().intValue();
        Vec3d pos = entity.getPos();
        spawnVisual(pos, color);
        if (this.playSound.c().booleanValue()) {
            playKillSound(pos);
        }
    }

    private void spawnVisual(Vec3d pos, int color) {
        if (this.style.l("Молния")) {
            this.lightnings.add(new Lightning(pos, color, this.random));
            return;
        }
        if (mc.world == null) {
            return;
        }
        if (this.style.l("Взрыв")) {
            mc.world.addParticle(ParticleTypes.EXPLOSION_EMITTER, pos.x, pos.y + 1.0d, pos.z, 0.0d, 0.0d, 0.0d);
            for (int i = 0; i < 18; i++) {
                mc.world.addParticle(ParticleTypes.POOF, pos.x, pos.y + 0.6d, pos.z, this.random.nextGaussian() * 0.12d, 0.08d + this.random.nextDouble() * 0.18d, this.random.nextGaussian() * 0.12d);
            }
            return;
        }
        if (this.style.l("Тотем")) {
            for (int i = 0; i < 48; i++) {
                mc.world.addParticle(ParticleTypes.TOTEM_OF_UNDYING, pos.x, pos.y + 1.0d, pos.z, this.random.nextGaussian() * 0.25d, this.random.nextDouble() * 0.45d, this.random.nextGaussian() * 0.25d);
            }
            return;
        }
        for (int i = 0; i < 36; i++) {
            mc.world.addParticle(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y + 0.4d + this.random.nextDouble() * 1.6d, pos.z, this.random.nextGaussian() * 0.18d, 0.15d, this.random.nextGaussian() * 0.18d);
        }
    }

    private void playKillSound(Vec3d pos) {
        SoundEvent sound = selectedSound();
        float vol = this.volume.c().floatValue() / 100.0f;
        mc.world.playSound(mc.player, pos.x, pos.y, pos.z, sound, SoundCategory.PLAYERS, vol, 0.9f + this.random.nextFloat() * 0.2f);
    }

    private SoundEvent selectedSound() {
        if (this.soundType.l("Взрыв")) {
            return SoundEvents.ENTITY_GENERIC_EXPLODE.value();
        }
        if (this.soundType.l("Тотем")) {
            return SoundEvents.ITEM_TOTEM_USE;
        }
        if (this.soundType.l("Крит")) {
            return SoundEvents.ENTITY_PLAYER_ATTACK_CRIT;
        }
        if (this.soundType.l("Случайный")) {
            SoundEvent[] sounds = {
                    SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER,
                    SoundEvents.ENTITY_GENERIC_EXPLODE.value(),
                    SoundEvents.ITEM_TOTEM_USE,
                    SoundEvents.ENTITY_PLAYER_ATTACK_CRIT
            };
            return sounds[this.random.nextInt(sounds.length)];
        }
        return SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER;
    }

    private static final class Lightning {
        private final Vec3d origin;
        private final List<Vec3d> points = new ArrayList<>();
        private final int color;
        private final long started = System.currentTimeMillis();

        private Lightning(Vec3d origin, int color, Random random) {
            this.origin = origin;
            this.color = color;
            Vec3d last = origin;
            this.points.add(last);
            for (int i = 0; i < 48; i++) {
                last = last.add(MathUtil.a(-0.38f, 0.38f), 0.22d + random.nextDouble() * 0.08d, MathUtil.a(-0.38f, 0.38f));
                this.points.add(last);
            }
        }

        private float alpha() {
            float t = (System.currentTimeMillis() - this.started) / 650.0f;
            if (t >= 1.0f) {
                return 0.0f;
            }
            return 1.0f - t;
        }

        private void render(BufferBuilder buffer, MatrixStack matrices, Camera camera, Vec3d cam, float fade) {
            int[] rgb = ColorUtil.b(this.color);
            for (int i = 0; i < this.points.size(); i++) {
                Vec3d pos = this.points.get(i);
                float height = (float) (pos.y - this.origin.y);
                float size = 0.12f + height * 0.045f;
                int a = Math.round(255.0f * fade * (0.35f + 0.65f * (1.0f - i / (float) this.points.size())));
                matrices.push();
                matrices.translate(pos.x - cam.x, pos.y - cam.y, pos.z - cam.z);
                matrices.multiply(camera.getRotation());
                Matrix4f matrix = matrices.peek().getPositionMatrix();
                float h = size / 2.0f;
                buffer.vertex(matrix, -h, -h, 0.0f).color(rgb[0], rgb[1], rgb[2], a);
                buffer.vertex(matrix, h, -h, 0.0f).color(rgb[0], rgb[1], rgb[2], a);
                buffer.vertex(matrix, h, h, 0.0f).color(255, 255, 255, Math.max(20, a / 2));
                buffer.vertex(matrix, -h, h, 0.0f).color(rgb[0], rgb[1], rgb[2], a);
                matrices.pop();
            }
        }
    }
}
