package aethereal.module.render;

import aethereal.core.Category;
import aethereal.core.EventTarget;
import aethereal.core.Module;
import aethereal.core.ModuleRegister;
import aethereal.event.AttackEvent;
import aethereal.event.DrawEvent;
import aethereal.event.TickEvent;
import aethereal.render.ColorUtil;
import aethereal.render.ParticleTextures;
import aethereal.setting.BooleanSetting;
import aethereal.setting.ColorSetting;
import aethereal.setting.ModeSetting;
import aethereal.setting.MultiModeSetting;
import aethereal.setting.SliderSetting;
import com.mojang.blaze3d.platform.GlStateManager;
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
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

@ModuleRegister(name = "World Particle", description = "Кастомные частицы мира, атаки, ходьбы и снарядов", category = Category.Render)
public class WorldParticles extends Module {
    private static final String[] TYPES = {
            "Звезды", "Снег", "Блум", "Bucks", "Core", "Crest", "Crown", "Cube", "Cube Blast",
            "Ded", "Dollar", "Firefly", "Glow", "Heart", "Heart1", "Lightning", "Snowbag",
            "Snow Binsecure", "Snow Blast", "Snowflake", "Snow New", "Star", "Star1", "Star New",
            "Сперматозоиды"
    };

    private final MultiModeSetting modes = new MultiModeSetting("Режимы",
            new BooleanSetting("По миру", true),
            new BooleanSetting("При атаке", false),
            new BooleanSetting("При ходьбе", false),
            new BooleanSetting("Следование", false));
    private final ModeSetting attackMode = new ModeSetting("Тип удара", "Всегда", "Всегда", "Только крит", "Только обычный").a(() -> {
        return Boolean.valueOf(modeOn("При атаке"));
    });
    private final MultiModeSetting attackParticleType = new MultiModeSetting("Тип частиц (Атака)", typeSettings()).a(() -> {
        return Boolean.valueOf(modeOn("При атаке"));
    });
    private final SliderSetting attackParticleCount = new SliderSetting("Количество (Атака)", 10.0f, 1.0f, 50.0f, 1.0f).a(() -> {
        return Boolean.valueOf(modeOn("При атаке"));
    });
    private final SliderSetting attackParticleSize = new SliderSetting("Размер (Атака)", 1.0f, 0.5f, 3.0f, 0.05f).a(() -> {
        return Boolean.valueOf(modeOn("При атаке"));
    });
    private final SliderSetting attackParticleLifeTime = new SliderSetting("Время жизни (Атака)", 800.0f, 250.0f, 3000.0f, 10.0f).a(() -> {
        return Boolean.valueOf(modeOn("При атаке"));
    });
    private final BooleanSetting attackCollision = new BooleanSetting("Коллизия (Атака)", true).a(() -> {
        return Boolean.valueOf(modeOn("При атаке"));
    });
    private final SliderSetting attackGravity = new SliderSetting("Гравитация (Атака)", 0.5f, -10.0f, 10.0f, 0.1f).a(() -> {
        return Boolean.valueOf(modeOn("При атаке"));
    });
    private final ColorSetting attackParticleColor = new ColorSetting("Цвет (Атака)", Integer.valueOf(ColorUtil.convertToARGB(255, 255, 255, 255))).a(() -> {
        return Boolean.valueOf(modeOn("При атаке"));
    });
    private final MultiModeSetting walkParticleType = new MultiModeSetting("Тип частиц (Ходьба)", typeSettings()).a(() -> {
        return Boolean.valueOf(modeOn("При ходьбе"));
    });
    private final SliderSetting walkParticleCount = new SliderSetting("Количество (Ходьба)", 3.0f, 1.0f, 20.0f, 1.0f).a(() -> {
        return Boolean.valueOf(modeOn("При ходьбе"));
    });
    private final SliderSetting walkParticleSize = new SliderSetting("Размер (Ходьба)", 1.0f, 0.5f, 3.0f, 0.05f).a(() -> {
        return Boolean.valueOf(modeOn("При ходьбе"));
    });
    private final SliderSetting walkParticleLifeTime = new SliderSetting("Время жизни (Ходьба)", 600.0f, 250.0f, 3000.0f, 10.0f).a(() -> {
        return Boolean.valueOf(modeOn("При ходьбе"));
    });
    private final BooleanSetting walkCollision = new BooleanSetting("Коллизия (Ходьба)", true).a(() -> {
        return Boolean.valueOf(modeOn("При ходьбе"));
    });
    private final SliderSetting walkGravity = new SliderSetting("Гравитация (Ходьба)", 0.3f, -10.0f, 10.0f, 0.1f).a(() -> {
        return Boolean.valueOf(modeOn("При ходьбе"));
    });
    private final ColorSetting walkParticleColor = new ColorSetting("Цвет (Ходьба)", Integer.valueOf(ColorUtil.convertToARGB(255, 255, 255, 255))).a(() -> {
        return Boolean.valueOf(modeOn("При ходьбе"));
    });
    private final SliderSetting followParticleCount = new SliderSetting("Количество (Следование)", 5.0f, 1.0f, 20.0f, 1.0f).a(() -> {
        return Boolean.valueOf(modeOn("Следование"));
    });
    private final SliderSetting followParticleSize = new SliderSetting("Размер (Следование)", 1.0f, 0.5f, 3.0f, 0.05f).a(() -> {
        return Boolean.valueOf(modeOn("Следование"));
    });
    private final SliderSetting followParticleLifeTime = new SliderSetting("Время жизни (Следование)", 500.0f, 250.0f, 3000.0f, 10.0f).a(() -> {
        return Boolean.valueOf(modeOn("Следование"));
    });
    private final ColorSetting followParticleColor = new ColorSetting("Цвет (Следование)", Integer.valueOf(ColorUtil.convertToARGB(255, 255, 255, 255))).a(() -> {
        return Boolean.valueOf(modeOn("Следование"));
    });
    private final ModeSetting modetype = new ModeSetting("Мод", "2D", "2D", "3D").a(() -> {
        return Boolean.valueOf(modeOn("По миру"));
    });
    private final MultiModeSetting particleType = new MultiModeSetting("Тип частиц", typeSettings()).a(this::isWorld2D);
    private final BooleanSetting spawnFromGround = new BooleanSetting("От земли", true).a(this::isWorld2D);
    private final BooleanSetting collision = new BooleanSetting("Коллизия", true).a(this::isWorld2D);
    private final BooleanSetting scale = new BooleanSetting("Скейл", true).a(this::isWorld2D);
    private final SliderSetting particleCount = new SliderSetting("Количество", 50.0f, 10.0f, 200.0f, 1.0f).a(this::isWorld3D);
    private final SliderSetting range = new SliderSetting("Дальность", 32.0f, 8.0f, 64.0f, 1.0f).a(this::isWorld3D);
    private final SliderSetting size = new SliderSetting("Размер", 0.09f, 0.05f, 0.15f, 0.005f).a(this::isWorld3D);
    private final SliderSetting maxParticles = new SliderSetting("Макс количество", 50.0f, 10.0f, 200.0f, 1.0f).a(this::isWorld2D);
    private final SliderSetting spawnRate = new SliderSetting("Спавн/сек", 15.0f, 10.0f, 200.0f, 1.0f).a(this::isWorld2D);
    private final SliderSetting spawnHeight = new SliderSetting("Высота спавна", 10.0f, 0.05f, 30.0f, 0.05f).a(this::isWorld2D);
    private final SliderSetting particleGravity = new SliderSetting("Гравитация", 0.0f, -10.0f, 10.0f, 0.1f).a(this::isWorld2D);
    private final SliderSetting motionPower = new SliderSetting("Сила движения", 1.0f, 0.1f, 2.0f, 0.05f).a(this::isWorld2D);
    private final SliderSetting inclineX = new SliderSetting("Наклон X", 0.0f, -17.5f, 17.5f, 0.5f).a(this::isWorld2D);
    private final SliderSetting inclineZ = new SliderSetting("Наклон Z", 0.0f, -17.5f, 17.5f, 0.5f).a(this::isWorld2D);
    private final SliderSetting particleSize = new SliderSetting("Размер частиц", 1.0f, 0.5f, 2.0f, 0.05f).a(this::isWorld2D);
    private final SliderSetting lifeTime = new SliderSetting("Время жизни", 800.0f, 250.0f, 3000.0f, 10.0f).a(this::isWorld2D);
    private final SliderSetting spawnRange = new SliderSetting("Радиус спавна", 25.0f, 10.0f, 50.0f, 1.0f).a(this::isWorld2D);
    private final ColorSetting particleColor = new ColorSetting("Цвет", Integer.valueOf(ColorUtil.convertToARGB(255, 230, 255, 255))).a(() -> {
        return Boolean.valueOf(modeOn("По миру"));
    });

    private final List<WorldCrystal> crystalList = new ArrayList<>();
    private final List<Particle2D> particles = new ArrayList<>();
    private final Map<Integer, List<Particle2D>> followingParticles = new HashMap<>();
    private final Random random = new Random();
    private int previousParticleCount;
    private long lastSpawnTime;
    private Vec3d lastPlayerPos;

    public WorldParticles() {
        a(this.modes, this.attackMode, this.attackParticleType, this.attackParticleCount, this.attackParticleSize,
                this.attackParticleLifeTime, this.attackCollision, this.attackGravity, this.attackParticleColor,
                this.walkParticleType, this.walkParticleCount, this.walkParticleSize, this.walkParticleLifeTime,
                this.walkCollision, this.walkGravity, this.walkParticleColor,
                this.followParticleCount, this.followParticleSize, this.followParticleLifeTime, this.followParticleColor,
                this.modetype, this.particleType, this.spawnFromGround, this.collision, this.scale,
                this.particleCount, this.range, this.size, this.maxParticles, this.spawnRate, this.spawnHeight,
                this.particleGravity, this.motionPower, this.inclineX, this.inclineZ, this.particleSize, this.lifeTime,
                this.spawnRange, this.particleColor);
        this.previousParticleCount = this.particleCount.c().intValue();
        this.lastSpawnTime = System.currentTimeMillis();
    }

    @Override
    public void b() {
        super.b();
        if (isWorld3D().booleanValue()) {
            generateCrystals();
        }
        this.previousParticleCount = this.particleCount.c().intValue();
        this.lastSpawnTime = System.currentTimeMillis();
    }

    @Override
    public void c() {
        super.c();
        this.crystalList.clear();
        this.particles.clear();
        this.followingParticles.clear();
        this.lastPlayerPos = null;
    }

    private boolean modeOn(String name) {
        BooleanSetting setting = this.modes.a(name);
        return setting != null && setting.c().booleanValue();
    }

    private Boolean isWorld2D() {
        return Boolean.valueOf(modeOn("По миру") && this.modetype.l("2D"));
    }

    private Boolean isWorld3D() {
        return Boolean.valueOf(modeOn("По миру") && this.modetype.l("3D"));
    }

    private static BooleanSetting[] typeSettings() {
        BooleanSetting[] settings = new BooleanSetting[TYPES.length];
        for (int i = 0; i < TYPES.length; i++) {
            settings[i] = new BooleanSetting(TYPES[i], Boolean.valueOf(TYPES[i].equals("Звезды")));
        }
        return settings;
    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        if (event.a() || !modeOn("При атаке") || mc.player == null || mc.world == null) {
            return;
        }
        Entity target = event.b();
        if (target == null) {
            return;
        }
        boolean crit = isCriticalHit();
        boolean spawn = this.attackMode.l("Всегда") || (this.attackMode.l("Только крит") && crit) || (this.attackMode.l("Только обычный") && !crit);
        if (!spawn) {
            return;
        }
        spawnAttackParticles(target.getPos().add(0.0d, target.getHeight() / 2.0d, 0.0d), this.attackParticleCount.c().intValue());
        updateNewParticles();
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) {
            return;
        }
        if (modeOn("При ходьбе")) {
            Vec3d currentPos = mc.player.getPos();
            if (this.lastPlayerPos != null && currentPos.distanceTo(this.lastPlayerPos) > 0.01d) {
                spawnWalkParticles();
                updateNewParticles();
            }
            this.lastPlayerPos = currentPos;
        }
        if (modeOn("Следование")) {
            scanProjectiles();
            updateFollowingParticles();
        } else {
            this.followingParticles.clear();
        }
    }

    @EventTarget
    public void onDraw(DrawEvent event) {
        if (!event.c() || mc.player == null || mc.world == null) {
            return;
        }
        ParticleTextures.ensure();
        if (modeOn("По миру")) {
            if (this.modetype.l("3D")) {
                int currentCount = this.particleCount.c().intValue();
                if (this.crystalList.isEmpty() || currentCount != this.previousParticleCount) {
                    if (this.crystalList.isEmpty()) {
                        generateCrystals();
                    } else {
                        adjustCrystalCount(currentCount);
                    }
                    this.previousParticleCount = currentCount;
                }
                updateCrystals();
                renderCrystals(event.h());
            } else {
                this.crystalList.clear();
                long now = System.currentTimeMillis();
                double spawnInterval = 1000.0d / Math.max(1.0f, this.spawnRate.c().floatValue());
                if (now - this.lastSpawnTime >= spawnInterval && this.particles.size() < this.maxParticles.c().intValue()) {
                    int before = this.particles.size();
                    for (int attempt = 0; attempt < 8 && this.particles.size() == before; attempt++) {
                        spawnParticle();
                    }
                    this.lastSpawnTime = now;
                }
                while (this.particles.size() > this.maxParticles.c().intValue()) {
                    this.particles.remove(0);
                }
            }
        } else {
            this.crystalList.clear();
        }

        Iterator<Particle2D> iterator = this.particles.iterator();
        while (iterator.hasNext()) {
            Particle2D particle = iterator.next();
            particle.update();
            if (particle.isDead()) {
                iterator.remove();
            }
        }

        if (modeOn("По миру") || modeOn("При атаке") || modeOn("При ходьбе")) {
            renderParticles(event.h());
        }
        if (modeOn("Следование")) {
            renderFollowingParticles(event.h());
        }
    }

    private boolean isCriticalHit() {
        if (mc.player == null) {
            return false;
        }
        boolean notUsingItem = !mc.player.isUsingItem() || mc.player.getActiveItem().isOf(Items.SHIELD);
        return !mc.player.isOnGround() && mc.player.fallDistance > 0.0f && notUsingItem;
    }

    private void scanProjectiles() {
        Set<Integer> seen = new HashSet<>();
        for (Entity entity : mc.world.getEntities()) {
            if (!isPlayerProjectile(entity)) {
                continue;
            }
            seen.add(Integer.valueOf(entity.getId()));
            this.followingParticles.computeIfAbsent(Integer.valueOf(entity.getId()), id -> new ArrayList<>());
        }
        this.followingParticles.keySet().removeIf(id -> !seen.contains(id));
    }

    private boolean isPlayerProjectile(Entity entity) {
        if (!(entity instanceof ProjectileEntity projectile) || mc.player == null) {
            return false;
        }
        return projectile.getOwner() == mc.player;
    }

    private void adjustCrystalCount(int targetCount) {
        int currentSize = this.crystalList.size();
        if (targetCount > currentSize) {
            addCrystals(targetCount - currentSize);
        } else if (targetCount < currentSize) {
            markCrystalsForRemoval(currentSize - targetCount);
        }
    }

    private void addCrystals(int count) {
        if (mc.player == null) {
            return;
        }
        Vec3d playerPos = mc.player.getPos();
        float rangeValue = this.range.c().floatValue();
        for (int i = 0; i < count; i++) {
            this.crystalList.add(new WorldCrystal(randomCrystalPos(playerPos, rangeValue), randomCrystalVel(), randomCrystalRot()));
        }
    }

    private void markCrystalsForRemoval(int count) {
        int marked = 0;
        for (WorldCrystal crystal : this.crystalList) {
            if (marked >= count) {
                break;
            }
            if (!crystal.markedForDeath && !crystal.isFadingOut) {
                crystal.markedForDeath = true;
                crystal.isFadingOut = true;
                marked++;
            }
        }
    }

    private void generateCrystals() {
        this.crystalList.clear();
        addCrystals(this.particleCount.c().intValue());
    }

    private Vec3d randomCrystalPos(Vec3d playerPos, float rangeValue) {
        Vec3d position = playerPos;
        for (int attempts = 0; attempts < 20; attempts++) {
            position = new Vec3d(
                    playerPos.x + (this.random.nextDouble() - 0.5d) * 2.0d * rangeValue,
                    playerPos.y + (this.random.nextDouble() - 0.5d) * rangeValue,
                    playerPos.z + (this.random.nextDouble() - 0.5d) * 2.0d * rangeValue);
            if (isInPlayerView(position)) {
                break;
            }
        }
        return position;
    }

    private Vec3d randomCrystalVel() {
        return new Vec3d((this.random.nextDouble() - 0.5d) * 0.02d, (this.random.nextDouble() - 0.5d) * 0.02d, (this.random.nextDouble() - 0.5d) * 0.02d);
    }

    private Vec3d randomCrystalRot() {
        return new Vec3d(this.random.nextDouble() * 360.0d, this.random.nextDouble() * 360.0d, this.random.nextDouble() * 360.0d);
    }

    private boolean isBlockOccluding(Vec3d crystalPos) {
        if (mc.world == null || mc.player == null) {
            return false;
        }
        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
        Vec3d direction = crystalPos.subtract(cameraPos);
        double distance = direction.length();
        if (distance < 0.2d) {
            return false;
        }
        direction = direction.normalize();
        for (double d = 0.5d; d < distance; d += 0.5d) {
            Vec3d checkPos = cameraPos.add(direction.multiply(d));
            if (!mc.world.getBlockState(BlockPos.ofFloored(checkPos)).isAir()) {
                return true;
            }
        }
        return false;
    }

    private void updateCrystals() {
        if (mc.player == null) {
            return;
        }
        Vec3d playerPos = mc.player.getPos();
        float rangeValue = this.range.c().floatValue();
        float fadeSpeedValue = 0.05f;
        Iterator<WorldCrystal> iterator = this.crystalList.iterator();
        while (iterator.hasNext()) {
            WorldCrystal crystal = iterator.next();
            crystal.prevPosition = crystal.position;
            crystal.position = crystal.position.add(crystal.velocity);
            boolean isOccluded = isBlockOccluding(crystal.position);
            boolean inView = isInPlayerView(crystal.position);
            if (crystal.markedForDeath) {
                crystal.fadeAlpha -= fadeSpeedValue;
                if (crystal.fadeAlpha <= 0.0f) {
                    iterator.remove();
                }
                continue;
            }
            crystal.isFadingOut = isOccluded || !inView;
            if (crystal.isFadingOut) {
                crystal.fadeAlpha -= fadeSpeedValue;
                if (crystal.fadeAlpha <= 0.0f) {
                    crystal.position = randomCrystalPos(playerPos, rangeValue);
                    crystal.prevPosition = crystal.position;
                    crystal.fadeAlpha = 0.0f;
                    crystal.isFadingOut = false;
                }
            } else {
                crystal.fadeAlpha = Math.min(1.0f, crystal.fadeAlpha + fadeSpeedValue);
            }
            if (crystal.position.distanceTo(playerPos) > rangeValue * 1.5d) {
                crystal.position = randomCrystalPos(playerPos, rangeValue);
                crystal.prevPosition = crystal.position;
                crystal.fadeAlpha = 0.0f;
                crystal.isFadingOut = false;
            }
        }
    }

    private float getCameraYaw() {
        return mc.gameRenderer.getCamera().getYaw();
    }

    private boolean isInPlayerView(Vec3d pos) {
        Camera cam = mc.gameRenderer.getCamera();
        if (cam == null) {
            return true;
        }
        Vec3d toParticle = pos.subtract(cam.getPos());
        if (toParticle.lengthSquared() < 1.0E-6d) {
            return true;
        }
        Vec3d look = Vec3d.fromPolar(cam.getPitch(), cam.getYaw());
        return look.dotProduct(toParticle.normalize()) > 0.1d;
    }

    private void renderCrystals(MatrixStack matrices) {
        if (this.crystalList.isEmpty()) {
            return;
        }
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d camPos = camera.getPos();
        float tickDelta = mc.getRenderTickCounter().getTickDelta(false);
        int baseColor = this.particleColor.c().intValue();
        float crystalSize = this.size.c().floatValue();
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(515);
        for (WorldCrystal crystal : this.crystalList) {
            if (crystal.fadeAlpha <= 0.0f) {
                continue;
            }
            Vec3d renderPos = crystal.prevPosition.lerp(crystal.position, tickDelta);
            if (!isInPlayerView(renderPos) && !crystal.isFadingOut) {
                continue;
            }
            matrices.push();
            matrices.translate(renderPos.x - camPos.x, renderPos.y - camPos.y, renderPos.z - camPos.z);
            float pulsation = 1.0f + (float) (Math.sin(System.currentTimeMillis() / 500.0d) * 0.1d);
            matrices.scale(pulsation, pulsation, pulsation);
            float selfRotation = ((System.currentTimeMillis() % 36000L) / 100.0f) * crystal.rotationSpeed;
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees((float) crystal.rotation.x));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) crystal.rotation.y + selfRotation));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) crystal.rotation.z));
            crystal.render(matrices, baseColor, camera, crystalSize);
            matrices.pop();
        }
    }

    private Vec3d getRandomMotion() {
        return new Vec3d((this.random.nextDouble() - 0.5d) * 0.08d, this.random.nextDouble() * 0.05d, (this.random.nextDouble() - 0.5d) * 0.08d);
    }

    private void spawnParticle() {
        if (mc.player == null || mc.world == null) {
            return;
        }
        double value = this.spawnRange.c().floatValue();
        double offsetX = (this.random.nextDouble() - 0.5d) * 2.0d * value;
        double offsetZ = (this.random.nextDouble() - 0.5d) * 2.0d * value;
        Vec3d additional = mc.player.getPos().add(offsetX, 0.0d, offsetZ);
        BlockPos bpos;
        if (this.spawnFromGround.c().booleanValue()) {
            bpos = mc.world.getTopPosition(Heightmap.Type.MOTION_BLOCKING, BlockPos.ofFloored(additional));
        } else {
            bpos = BlockPos.ofFloored(mc.player.getPos().add(offsetX, this.random.nextDouble() * this.spawnHeight.c().floatValue(), offsetZ));
        }
        Vec3d pos = new Vec3d(bpos.getX() + 0.5d, bpos.getY() + 0.25d, bpos.getZ() + 0.5d);
        if (!mc.world.getBlockState(BlockPos.ofFloored(pos)).isAir()) {
            return;
        }
        int lifetime = Math.max(150, this.lifeTime.c().intValue() + this.random.nextInt(50) - 25);
        this.particles.add(new Particle2D(pos, getRandomMotion().multiply(this.motionPower.c().floatValue()), lifetime, this.particleColor.c().intValue(), randomType(this.particleType)));
    }

    private String randomType(MultiModeSetting setting) {
        List<String> selected = new ArrayList<>();
        for (BooleanSetting option : setting.c()) {
            if (option.c().booleanValue()) {
                selected.add(option.i());
            }
        }
        if (selected.isEmpty()) {
            return "Звезды";
        }
        return selected.get(this.random.nextInt(selected.size()));
    }

    private void renderParticles(MatrixStack matrices) {
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d camPos = camera.getPos();
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(515);
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        for (Particle2D particle : this.particles) {
            renderSingleParticle(matrices, particle, camera, camPos);
        }
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private void renderSingleParticle(MatrixStack matrices, Particle2D particle, Camera camera, Vec3d camPos) {
        float alpha = particle.fade();
        if (alpha <= 0.01f) {
            return;
        }
        int[] rgb = ColorUtil.b(particle.colorInt);
        int a = Math.max(0, Math.min(255, Math.round(alpha * 255.0f)));
        if (a <= 0) {
            return;
        }
        float baseScale = particle.customSize > 0.0f ? particle.customSize : this.particleSize.c().floatValue();
        float finalScale = Math.max(0.12f, baseScale * particle.scaleFactor());
        Identifier texture = ParticleTextures.of(particle.particleTypeName);
        boolean sperm = particle.isSperm();
        float hw = sperm ? finalScale * 0.92f : finalScale * 0.48f;
        float hh = sperm ? finalScale * 0.28f : finalScale * 0.48f;
        org.joml.Vector3f right = camera.getRotation().transform(new org.joml.Vector3f(1.0f, 0.0f, 0.0f));
        org.joml.Vector3f up = camera.getRotation().transform(new org.joml.Vector3f(0.0f, 1.0f, 0.0f));
        float spin = sperm ? (float) Math.toRadians(particle.swimAngle()) : particle.spinRadians();
        if (spin != 0.0f) {
            float cos = MathHelper.cos(spin);
            float sin = MathHelper.sin(spin);
            org.joml.Vector3f rotatedRight = new org.joml.Vector3f(
                    right.x * cos + up.x * sin,
                    right.y * cos + up.y * sin,
                    right.z * cos + up.z * sin);
            org.joml.Vector3f rotatedUp = new org.joml.Vector3f(
                    -right.x * sin + up.x * cos,
                    -right.y * sin + up.y * cos,
                    -right.z * sin + up.z * cos);
            right = rotatedRight;
            up = rotatedUp;
        }
        float px = (float) (particle.pos.x - camPos.x);
        float py = (float) (particle.pos.y - camPos.y);
        float pz = (float) (particle.pos.z - camPos.z);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        if (ParticleTextures.usesSoftGlow(particle.particleTypeName)) {
            Identifier glow = ParticleTextures.glow();
            if (glow != null) {
                float glowScale = particle.isBloom() ? 1.35f : 2.05f;
                int glowA = Math.max(0, Math.min(255, Math.round(a * (particle.isBloom() ? 0.55f : 0.42f))));
                drawBillboard(matrix, glow, px, py, pz, right, up, hw * glowScale, hh * glowScale, rgb[0], rgb[1], rgb[2], glowA);
            }
        }
        drawBillboard(matrix, texture, px, py, pz, right, up, hw, hh, rgb[0], rgb[1], rgb[2], a);
    }

    private void drawBillboard(Matrix4f matrix, Identifier texture, float px, float py, float pz, org.joml.Vector3f right, org.joml.Vector3f up, float hw, float hh, int r, int g, int b, int a) {
        RenderSystem.setShaderTexture(0, mc.getTextureManager().getTexture(texture).getGlId());
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        emitBillboard(buffer, matrix, px, py, pz, right, up, -hw, -hh, 0.0f, 1.0f, r, g, b, a);
        emitBillboard(buffer, matrix, px, py, pz, right, up, hw, -hh, 1.0f, 1.0f, r, g, b, a);
        emitBillboard(buffer, matrix, px, py, pz, right, up, hw, hh, 1.0f, 0.0f, r, g, b, a);
        emitBillboard(buffer, matrix, px, py, pz, right, up, -hw, hh, 0.0f, 0.0f, r, g, b, a);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private static void emitBillboard(BufferBuilder buffer, Matrix4f matrix, float px, float py, float pz, org.joml.Vector3f right, org.joml.Vector3f up, float x, float y, float u, float v, int r, int g, int b, int a) {
        buffer.vertex(matrix, px + right.x * x + up.x * y, py + right.y * x + up.y * y, pz + right.z * x + up.z * y).texture(u, v).color(r, g, b, a);
    }

    private void updateNewParticles() {
        int startIndex = Math.max(0, this.particles.size() - 50);
        for (int i = startIndex; i < this.particles.size(); i++) {
            this.particles.get(i).update();
        }
    }

    private void spawnAttackParticles(Vec3d pos, int count) {
        for (int i = 0; i < count; i++) {
            Vec3d spawnPos = pos.add((this.random.nextDouble() - 0.5d) * 0.5d, (this.random.nextDouble() - 0.5d) * 0.5d, (this.random.nextDouble() - 0.5d) * 0.5d);
            Vec3d vel = new Vec3d((this.random.nextDouble() - 0.5d) * 0.1d, this.random.nextDouble() * 0.1d, (this.random.nextDouble() - 0.5d) * 0.1d);
            int lifetime = Math.max(150, this.attackParticleLifeTime.c().intValue() + this.random.nextInt(200) - 100);
            Particle2D particle = new Particle2D(spawnPos, vel, lifetime, this.attackParticleColor.c().intValue(), randomType(this.attackParticleType));
            particle.customSize = this.attackParticleSize.c().floatValue();
            particle.useCollision = this.attackCollision.c().booleanValue();
            particle.customGravity = this.attackGravity.c().floatValue();
            particle.viewFadeAlpha = 1.0f;
            this.particles.add(particle);
        }
    }

    private void spawnWalkParticles() {
        if (mc.player == null) {
            return;
        }
        double radYaw = Math.toRadians(mc.player.getYaw());
        Vec3d spawnPos = mc.player.getPos().add(-Math.sin(radYaw) * 0.3d, 0.1d, Math.cos(radYaw) * 0.3d);
        int count = this.walkParticleCount.c().intValue();
        for (int i = 0; i < count; i++) {
            Vec3d finalPos = spawnPos.add((this.random.nextDouble() - 0.5d) * 0.2d, 0.0d, (this.random.nextDouble() - 0.5d) * 0.2d);
            Vec3d vel = new Vec3d((this.random.nextDouble() - 0.5d) * 0.05d, this.random.nextDouble() * 0.05d, (this.random.nextDouble() - 0.5d) * 0.05d);
            int lifetime = Math.max(150, this.walkParticleLifeTime.c().intValue() + this.random.nextInt(100) - 50);
            Particle2D particle = new Particle2D(finalPos, vel, lifetime, this.walkParticleColor.c().intValue(), randomType(this.walkParticleType));
            particle.customSize = this.walkParticleSize.c().floatValue();
            particle.useCollision = this.walkCollision.c().booleanValue();
            particle.customGravity = this.walkGravity.c().floatValue();
            particle.viewFadeAlpha = 1.0f;
            this.particles.add(particle);
        }
    }

    private void updateFollowingParticles() {
        Iterator<Map.Entry<Integer, List<Particle2D>>> entityIterator = this.followingParticles.entrySet().iterator();
        while (entityIterator.hasNext()) {
            Map.Entry<Integer, List<Particle2D>> entry = entityIterator.next();
            Entity entity = mc.world.getEntityById(entry.getKey().intValue());
            List<Particle2D> entityParticles = entry.getValue();
            if (entity == null || !entity.isAlive() || entity.isRemoved()) {
                entityIterator.remove();
                continue;
            }
            if (entityParticles.size() < this.followParticleCount.c().intValue()) {
                Vec3d vel = new Vec3d((this.random.nextDouble() - 0.5d) * 0.02d, (this.random.nextDouble() - 0.5d) * 0.02d, (this.random.nextDouble() - 0.5d) * 0.02d);
                int lifetime = Math.max(150, this.followParticleLifeTime.c().intValue() + this.random.nextInt(200) - 100);
                Particle2D particle = new Particle2D(entity.getPos(), vel, lifetime, this.followParticleColor.c().intValue(), randomType(this.particleType));
                particle.customSize = this.followParticleSize.c().floatValue();
                entityParticles.add(particle);
            }
            Iterator<Particle2D> particleIterator = entityParticles.iterator();
            while (particleIterator.hasNext()) {
                Particle2D particle = particleIterator.next();
                particle.update();
                if (particle.isDead()) {
                    particleIterator.remove();
                }
            }
        }
    }

    private void renderFollowingParticles(MatrixStack matrices) {
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d camPos = camera.getPos();
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(515);
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        for (List<Particle2D> entityParticles : this.followingParticles.values()) {
            for (Particle2D particle : entityParticles) {
                renderSingleParticle(matrices, particle, camera, camPos);
            }
        }
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static class WorldCrystal {
        Vec3d position;
        Vec3d prevPosition;
        final Vec3d velocity;
        final Vec3d rotation;
        final float rotationSpeed;
        float fadeAlpha;
        boolean isFadingOut;
        boolean markedForDeath;

        WorldCrystal(Vec3d position, Vec3d velocity, Vec3d rotation) {
            this.position = position;
            this.prevPosition = position;
            this.velocity = velocity;
            this.rotation = rotation;
            this.rotationSpeed = 0.5f + (float) (Math.random() * 1.5d);
            this.fadeAlpha = 0.0f;
        }

        void render(MatrixStack matrices, int baseColor, Camera camera, float crystalSize) {
            RenderSystem.disableCull();
            RenderSystem.enableBlend();
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
            drawCrystal(matrices, baseColor, crystalSize);
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
            drawCrystal(matrices, baseColor, crystalSize);
            RenderSystem.depthMask(false);
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
            matrices.push();
            matrices.scale(1.2f, 1.2f, 1.2f);
            drawCrystal(matrices, baseColor, crystalSize);
            matrices.pop();
            drawBloom(matrices, baseColor, camera, crystalSize);
            RenderSystem.depthMask(true);
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
        }

        private void drawBloom(MatrixStack matrices, int baseColor, Camera camera, float crystalSize) {
            Identifier glow = ParticleTextures.glow();
            if (glow == null) {
                return;
            }
            RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
            RenderSystem.setShaderTexture(0, net.minecraft.client.MinecraftClient.getInstance().getTextureManager().getTexture(glow).getGlId());
            int[] rgb = ColorUtil.b(baseColor);
            int a = Math.max(0, Math.min(255, Math.round(90.0f * this.fadeAlpha)));
            float bloomSize = crystalSize * 11.0f;
            matrices.push();
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
            Matrix4f matrix = matrices.peek().getPositionMatrix();
            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            float h = bloomSize * 0.5f;
            buffer.vertex(matrix, -h, -h, 0.0f).texture(0.0f, 1.0f).color(rgb[0], rgb[1], rgb[2], a);
            buffer.vertex(matrix, h, -h, 0.0f).texture(1.0f, 1.0f).color(rgb[0], rgb[1], rgb[2], a);
            buffer.vertex(matrix, h, h, 0.0f).texture(1.0f, 0.0f).color(rgb[0], rgb[1], rgb[2], a);
            buffer.vertex(matrix, -h, h, 0.0f).texture(0.0f, 0.0f).color(rgb[0], rgb[1], rgb[2], a);
            BufferRenderer.drawWithGlobalProgram(buffer.end());
            matrices.pop();
        }

        private void drawCrystal(MatrixStack matrices, int baseColor, float crystalSize) {
            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
            float s = crystalSize;
            float hPrism = crystalSize;
            float hPyramid = crystalSize * 1.5f;
            int sides = 8;
            Vec3d[] top = new Vec3d[sides];
            Vec3d[] bottom = new Vec3d[sides];
            for (int i = 0; i < sides; i++) {
                float angle = (float) (Math.PI * 2.0d * i / sides);
                float x = (float) (s * Math.cos(angle));
                float z = (float) (s * Math.sin(angle));
                top[i] = new Vec3d(x, hPrism / 2.0f, z);
                bottom[i] = new Vec3d(x, -hPrism / 2.0f, z);
            }
            Vec3d vTop = new Vec3d(0.0d, (hPrism / 2.0f) + hPyramid, 0.0d);
            Vec3d vBottom = new Vec3d(0.0d, (-hPrism / 2.0f) - hPyramid, 0.0d);
            int[] rgb = ColorUtil.b(baseColor);
            int a = Math.max(0, Math.min(255, Math.round(150.0f * this.fadeAlpha)));
            for (int i = 0; i < sides; i++) {
                int n = (i + 1) % sides;
                triangle(buffer, matrices, bottom[i], bottom[n], top[n], rgb[0], rgb[1], rgb[2], a);
                triangle(buffer, matrices, bottom[i], top[n], top[i], rgb[0], rgb[1], rgb[2], a);
                triangle(buffer, matrices, vTop, top[i], top[n], rgb[0], rgb[1], rgb[2], a);
                triangle(buffer, matrices, vBottom, bottom[n], bottom[i], rgb[0], rgb[1], rgb[2], a);
            }
            BufferRenderer.drawWithGlobalProgram(buffer.end());
        }

        private static void triangle(BufferBuilder buffer, MatrixStack matrices, Vec3d v1, Vec3d v2, Vec3d v3, int r, int g, int b, int a) {
            Matrix4f matrix = matrices.peek().getPositionMatrix();
            buffer.vertex(matrix, (float) v1.x, (float) v1.y, (float) v1.z).color(r, g, b, a);
            buffer.vertex(matrix, (float) v2.x, (float) v2.y, (float) v2.z).color(r, g, b, a);
            buffer.vertex(matrix, (float) v3.x, (float) v3.y, (float) v3.z).color(r, g, b, a);
        }
    }

    private class Particle2D {
        Vec3d pos;
        Vec3d vel;
        final int colorInt;
        final String particleTypeName;
        float viewFadeAlpha;
        float customSize = -1.0f;
        boolean useCollision;
        float customGravity = Float.NaN;
        private final long birthTime;
        private final int totalLife;

        Particle2D(Vec3d pos, Vec3d vel, int life, int colorInt, String particleTypeName) {
            this.pos = pos;
            this.vel = vel;
            this.colorInt = colorInt;
            this.particleTypeName = particleTypeName;
            this.birthTime = System.currentTimeMillis();
            this.totalLife = 2 * life;
            this.viewFadeAlpha = 1.0f;
        }

        boolean isDead() {
            return System.currentTimeMillis() - this.birthTime >= this.totalLife;
        }

        float fade() {
            long ageMs = System.currentTimeMillis() - this.birthTime;
            float progress = Math.min(1.0f, (float) ageMs / (float) this.totalLife);
            float fadeIn = MathHelper.clamp(progress / 0.14f, 0.0f, 1.0f);
            fadeIn = fadeIn * fadeIn * (3.0f - (2.0f * fadeIn));
            float fadeOut = MathHelper.clamp((1.0f - progress) / 0.28f, 0.0f, 1.0f);
            fadeOut = fadeOut * fadeOut * (3.0f - (2.0f * fadeOut));
            return fadeIn * fadeOut * this.viewFadeAlpha;
        }

        float scaleFactor() {
            if (!WorldParticles.this.scale.c().booleanValue()) {
                return 1.0f;
            }
            return 0.82f + (0.18f * fade());
        }

        boolean isSperm() {
            return "Сперматозоиды".equals(this.particleTypeName);
        }

        boolean isBloom() {
            return "Блум".equals(this.particleTypeName) || "Glow".equals(this.particleTypeName);
        }

        float swimAngle() {
            double age = (System.currentTimeMillis() - this.birthTime) / 70.0d;
            float heading = (float) Math.toDegrees(Math.atan2(this.vel.z, this.vel.x));
            return heading + (float) Math.sin(age) * 22.0f;
        }

        float spinRadians() {
            if (isSperm()) {
                return 0.0f;
            }
            double age = (System.currentTimeMillis() - this.birthTime) / 900.0d;
            return (float) (age * (this.particleTypeName.hashCode() % 2 == 0 ? 1.0d : -1.0d));
        }

        void update() {
            if (isDead() || mc.world == null) {
                return;
            }
            float deltaTime = MathHelper.clamp(mc.getRenderTickCounter().getLastFrameDuration() * 0.05f, 0.001f, 0.05f);
            float speed = deltaTime / 0.05f;
            float motionMultiplier = WorldParticles.this.motionPower.c().floatValue();
            float yaw = getCameraYaw();
            this.pos = this.pos.add(this.vel.multiply(speed * motionMultiplier, speed * motionMultiplier, speed * motionMultiplier));
            double xY = Math.sin(Math.toRadians(yaw));
            double zY = -Math.cos(Math.toRadians(yaw));
            double xX = -Math.sin(Math.toRadians(yaw + 90.0f));
            double zX = Math.cos(Math.toRadians(yaw + 90.0f));
            Vec3d addMotion = new Vec3d(
                    xY * WorldParticles.this.inclineZ.c().floatValue() / 50.0d + xX * WorldParticles.this.inclineX.c().floatValue() / 50.0d,
                    0.0d,
                    zY * WorldParticles.this.inclineZ.c().floatValue() / 50.0d + zX * WorldParticles.this.inclineX.c().floatValue() / 50.0d);
            float gravityValue = !Float.isNaN(this.customGravity) ? this.customGravity : WorldParticles.this.particleGravity.c().floatValue();
            this.vel = this.vel.add(addMotion.x * deltaTime * motionMultiplier, (gravityValue / 80.0f) * deltaTime * motionMultiplier, addMotion.z * deltaTime * motionMultiplier);
            if (isSperm()) {
                double age = (System.currentTimeMillis() - this.birthTime) / 90.0d;
                Vec3d lateral = new Vec3d(-this.vel.z, 0.0d, this.vel.x);
                if (lateral.lengthSquared() > 1.0E-8d) {
                    this.pos = this.pos.add(lateral.normalize().multiply(Math.sin(age) * 0.018d));
                }
                this.vel = this.vel.add(Math.cos(age) * 0.002d, Math.sin(age * 0.65d) * 0.003d, Math.sin(age) * 0.002d);
            }
            this.pos = this.pos.add(this.vel.multiply(speed, speed, speed));
            boolean shouldUseCollision = !Float.isNaN(this.customGravity) ? this.useCollision : WorldParticles.this.collision.c().booleanValue();
            if (shouldUseCollision && !mc.world.getBlockState(BlockPos.ofFloored(this.pos)).isAir()) {
                double dot = this.vel.dotProduct(new Vec3d(0.0d, 1.0d, 0.0d));
                this.vel = this.vel.subtract(new Vec3d(0.0d, 1.0d, 0.0d).multiply(2.0d * dot)).multiply(0.8d);
            }
            if (isInPlayerView(this.pos)) {
                this.viewFadeAlpha = Math.min(1.0f, this.viewFadeAlpha + deltaTime * 2.0f);
            } else {
                this.viewFadeAlpha = Math.max(0.0f, this.viewFadeAlpha - deltaTime);
            }
        }
    }
}
