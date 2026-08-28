package aethereal.module.misc;

import aethereal.core.Category;
import aethereal.core.EventTarget;
import aethereal.core.GlobalEvent;
import aethereal.core.Module;
import aethereal.core.ModuleRegister;
import aethereal.core.Processor;
import aethereal.core.Skeleton;
import aethereal.lib.json.JSONObject;
import aethereal.setting.BooleanSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.ParticlesMode;

import java.io.File;
import java.nio.file.Files;
import java.util.function.Consumer;

@ModuleRegister(name = "Optimization", description = "Снимает лишнюю графику и повышает FPS: частицы, погода, облака, тени, дистанция и быстрый режим", category = Category.Misc)
public class Optimization extends Module {
    private final BooleanSetting particles = new BooleanSetting("Частицы", true);
    private final BooleanSetting weather = new BooleanSetting("Погода", true);
    private final BooleanSetting clouds = new BooleanSetting("Облака", true);
    private final BooleanSetting shadows = new BooleanSetting("Тени сущностей", true);
    private final BooleanSetting entities = new BooleanSetting("Дальность сущностей", true);
    private final BooleanSetting drops = new BooleanSetting("Предметы на земле", true);
    private final BooleanSetting graphics = new BooleanSetting("Быстрая графика", true);
    private final BooleanSetting vsync = new BooleanSetting("Отключить VSync", true);

    private Snapshot snapshot;
    private boolean applied;

    public Optimization() {
        a(this.particles, this.weather, this.clouds, this.shadows, this.entities, this.drops, this.graphics, this.vsync);
        Consumer<Boolean> refresh = value -> refresh();
        this.particles.a(refresh);
        this.weather.a(refresh);
        this.clouds.a(refresh);
        this.shadows.a(refresh);
        this.entities.a(refresh);
        this.drops.a(refresh);
        this.graphics.a(refresh);
        this.vsync.a(refresh);
    }

    public static Optimization current() {
        try {
            Processor processor = Skeleton.getInstance().getModuleProcessor();
            if (processor == null || processor.t() == null) {
                return null;
            }
            return processor.t().getOptimization();
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static boolean shouldSkipBreakParticles() {
        Optimization opt = current();
        return opt != null && opt.m() && opt.particles.c().booleanValue();
    }

    public static boolean shouldCancelParticle(ParticleEffect effect) {
        Optimization opt = current();
        if (opt == null || !opt.m() || !opt.particles.c().booleanValue() || effect == null) {
            return false;
        }
        var type = effect.getType();
        return type != ParticleTypes.TOTEM_OF_UNDYING
                && type != ParticleTypes.EXPLOSION
                && type != ParticleTypes.EXPLOSION_EMITTER
                && type != ParticleTypes.FLASH
                && type != ParticleTypes.FIREWORK
                && type != ParticleTypes.SWEEP_ATTACK
                && type != ParticleTypes.DAMAGE_INDICATOR;
    }

    public static boolean shouldSkipWeather() {
        Optimization opt = current();
        return opt != null && opt.m() && opt.weather.c().booleanValue();
    }

    public static boolean shouldSkipClouds() {
        Optimization opt = current();
        return opt != null && opt.m() && opt.clouds.c().booleanValue();
    }

    public static boolean shouldSkipShadows() {
        Optimization opt = current();
        return opt != null && opt.m() && opt.shadows.c().booleanValue();
    }

    public static boolean shouldHideEntity(Entity entity) {
        Optimization opt = current();
        if (opt == null || !opt.m() || entity == null) {
            return false;
        }
        MinecraftClient client = mc;
        if (client == null || client.player == null) {
            return false;
        }
        if (entity == client.player || entity == client.getCameraEntity()) {
            return false;
        }
        if (entity instanceof PlayerEntity) {
            return false;
        }
        Entity vehicle = client.player.getVehicle();
        if (vehicle != null && (entity == vehicle || entity.hasPassenger(client.player))) {
            return false;
        }
        double distance = client.player.squaredDistanceTo(entity);
        if (opt.drops.c().booleanValue() && entity instanceof ItemEntity) {
            return distance > 400.0d;
        }
        if (opt.entities.c().booleanValue() && !(entity instanceof ItemEntity)) {
            return distance > 2304.0d;
        }
        return false;
    }

    @Override
    public void b() {
        super.b();
        tryApply();
    }

    @EventTarget
    public void onGlobal(GlobalEvent event) {
        tryApply();
    }

    private void tryApply() {
        if (!m() || this.applied || mc == null || mc.options == null) {
            return;
        }
        captureIfNeeded();
        if (this.snapshot == null) {
            return;
        }
        applyNow();
        this.applied = true;
    }

    @Override
    public void c() {
        restoreNow();
        this.applied = false;
        super.c();
    }

    private void refresh() {
        if (this.applied && m()) {
            applyNow();
        }
    }

    private void captureIfNeeded() {
        if (this.snapshot != null) {
            return;
        }
        File file = snapshotFile();
        if (file != null && file.exists() && loadSnapshot(file)) {
            return;
        }
        if (mc == null || mc.options == null) {
            return;
        }
        this.snapshot = Snapshot.from(mc.options);
        saveSnapshot();
    }

    private void applyNow() {
        if (mc == null || mc.options == null || this.snapshot == null) {
            return;
        }
        GameOptions options = mc.options;
        Snapshot original = this.snapshot;
        try {
            options.getGraphicsMode().setValue(this.graphics.c().booleanValue() ? GraphicsMode.FAST : original.graphics);
            options.getAo().setValue(this.graphics.c().booleanValue() ? Boolean.FALSE : original.ao);
            options.getBiomeBlendRadius().setValue(this.graphics.c().booleanValue() ? 0 : original.biomeBlend);
            options.getBobView().setValue(this.graphics.c().booleanValue() ? Boolean.FALSE : original.bobView);
            options.getParticles().setValue(original.particles);
            options.getCloudRenderMode().setValue(this.clouds.c().booleanValue() ? CloudRenderMode.OFF : original.clouds);
            options.getEntityShadows().setValue(this.shadows.c().booleanValue() ? Boolean.FALSE : original.shadows);
            options.getEntityDistanceScaling().setValue(this.entities.c().booleanValue() ? 0.5d : original.entityDistance);
            options.getEnableVsync().setValue(this.vsync.c().booleanValue() ? Boolean.FALSE : original.vsync);
            options.write();
        } catch (Exception ignored) {
        }
    }

    private void restoreNow() {
        if (mc == null || mc.options == null || this.snapshot == null) {
            deleteSnapshot();
            return;
        }
        GameOptions options = mc.options;
        Snapshot original = this.snapshot;
        try {
            options.getGraphicsMode().setValue(original.graphics);
            options.getAo().setValue(original.ao);
            options.getBiomeBlendRadius().setValue(original.biomeBlend);
            options.getBobView().setValue(original.bobView);
            options.getParticles().setValue(original.particles);
            options.getCloudRenderMode().setValue(original.clouds);
            options.getEntityShadows().setValue(original.shadows);
            options.getEntityDistanceScaling().setValue(original.entityDistance);
            options.getEnableVsync().setValue(original.vsync);
            options.write();
        } catch (Exception ignored) {
        }
        deleteSnapshot();
        this.snapshot = null;
    }

    private File snapshotFile() {
        try {
            Processor processor = Skeleton.getInstance().getModuleProcessor();
            if (processor == null || processor.t() == null || processor.t().d() == null) {
                return null;
            }
            File dir = processor.t().d();
            if (!dir.exists()) {
                dir.mkdirs();
            }
            return new File(dir, "optimization-snapshot.json");
        } catch (Exception ignored) {
            return null;
        }
    }

    private void saveSnapshot() {
        File file = snapshotFile();
        if (file == null || this.snapshot == null) {
            return;
        }
        try {
            Files.writeString(file.toPath(), this.snapshot.toJson().a(2));
        } catch (Exception ignored) {
        }
    }

    private boolean loadSnapshot(File file) {
        try {
            this.snapshot = Snapshot.fromJson(new JSONObject(Files.readString(file.toPath())));
            return this.snapshot != null;
        } catch (Exception ignored) {
            return false;
        }
    }

    private void deleteSnapshot() {
        File file = snapshotFile();
        if (file != null && file.exists()) {
            file.delete();
        }
    }

    private static final class Snapshot {
        private final ParticlesMode particles;
        private final CloudRenderMode clouds;
        private final GraphicsMode graphics;
        private final Boolean shadows;
        private final Boolean ao;
        private final Boolean bobView;
        private final Boolean vsync;
        private final Double entityDistance;
        private final Integer biomeBlend;

        private Snapshot(ParticlesMode particles, CloudRenderMode clouds, GraphicsMode graphics, Boolean shadows, Boolean ao, Boolean bobView, Boolean vsync, Double entityDistance, Integer biomeBlend) {
            this.particles = particles;
            this.clouds = clouds;
            this.graphics = graphics;
            this.shadows = shadows;
            this.ao = ao;
            this.bobView = bobView;
            this.vsync = vsync;
            this.entityDistance = entityDistance;
            this.biomeBlend = biomeBlend;
        }

        private static Snapshot from(GameOptions options) {
            if (options == null) {
                return null;
            }
            return new Snapshot(
                    options.getParticles().getValue(),
                    options.getCloudRenderMode().getValue(),
                    options.getGraphicsMode().getValue(),
                    options.getEntityShadows().getValue(),
                    options.getAo().getValue(),
                    options.getBobView().getValue(),
                    options.getEnableVsync().getValue(),
                    options.getEntityDistanceScaling().getValue(),
                    options.getBiomeBlendRadius().getValue()
            );
        }

        private static Snapshot fromJson(JSONObject json) {
            if (json == null) {
                return null;
            }
            return new Snapshot(
                    ParticlesMode.valueOf(json.a("particles", ParticlesMode.ALL.name())),
                    CloudRenderMode.valueOf(json.a("clouds", CloudRenderMode.FANCY.name())),
                    GraphicsMode.valueOf(json.a("graphics", GraphicsMode.FANCY.name())),
                    Boolean.valueOf(json.a("shadows", true)),
                    Boolean.valueOf(json.a("ao", true)),
                    Boolean.valueOf(json.a("bobView", true)),
                    Boolean.valueOf(json.a("vsync", true)),
                    Double.valueOf(json.m("entityDistance") ? json.e("entityDistance") : 1.0d),
                    Integer.valueOf(json.a("biomeBlend", 2))
            );
        }

        private JSONObject toJson() {
            JSONObject json = new JSONObject();
            json.c("particles", this.particles.name());
            json.c("clouds", this.clouds.name());
            json.c("graphics", this.graphics.name());
            json.b("shadows", this.shadows.booleanValue());
            json.b("ao", this.ao.booleanValue());
            json.b("bobView", this.bobView.booleanValue());
            json.b("vsync", this.vsync.booleanValue());
            json.b("entityDistance", this.entityDistance.doubleValue());
            json.c("biomeBlend", this.biomeBlend);
            return json;
        }
    }
}
