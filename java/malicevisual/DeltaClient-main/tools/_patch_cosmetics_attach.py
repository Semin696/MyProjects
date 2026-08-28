# -*- coding: utf-8 -*-
from pathlib import Path

p = Path(r"D:\mycode\java\malicevisual\DeltaClient-main\src\main\java\aethereal\module\render\PlayerCosmetics.java")
text = p.read_text(encoding="utf-8")
start = text.index("@ModuleRegister")
end = text.index("    private boolean visible(PlayerEntity player)")

new = r'''@ModuleRegister(name = "Косметика", description = "3D косметика, привязанная к частям тела игрока", category = Category.Render)
public class PlayerCosmetics extends Module {
    private static final String[] ATTACH = {"Голова", "Тело", "Спина", "Левое плечо", "Правое плечо", "Левая рука", "Правая рука"};

    private final BooleanSetting hat = new BooleanSetting("Шапка", true);
    private final ModeSetting hatStyle = new ModeSetting("Стиль шапки", "Сундук", "Сундук", "Шалкер", "Корона", "Волшебник", "Рога", "Ушки", "Конус", "Кепка").a(() -> this.hat.c());
    private final ModeSetting hatAttach = new ModeSetting("Крепление шапки", "Голова", ATTACH).a(() -> this.hat.c());
    private final SliderSetting hatX = new SliderSetting("Шапка X", 0.0f, -1.0f, 1.0f, 0.01f).a(() -> this.hat.c());
    private final SliderSetting hatY = new SliderSetting("Шапка Y", 0.0f, -1.0f, 1.0f, 0.01f).a(() -> this.hat.c());
    private final SliderSetting hatZ = new SliderSetting("Шапка Z", 0.0f, -1.0f, 1.0f, 0.01f).a(() -> this.hat.c());

    private final BooleanSetting backpack = new BooleanSetting("Рюкзак", true);
    private final ModeSetting backpackStyle = new ModeSetting("Стиль рюкзака", "Кристалл", "Сумка", "Кристалл", "Джетпак", "Школьный", "Крылатый").a(() -> this.backpack.c());
    private final ModeSetting backpackAttach = new ModeSetting("Крепление рюкзака", "Спина", ATTACH).a(() -> this.backpack.c());
    private final SliderSetting backpackX = new SliderSetting("Рюкзак X", 0.0f, -1.0f, 1.0f, 0.01f).a(() -> this.backpack.c());
    private final SliderSetting backpackY = new SliderSetting("Рюкзак Y", 0.05f, -1.0f, 1.0f, 0.01f).a(() -> this.backpack.c());
    private final SliderSetting backpackZ = new SliderSetting("Рюкзак Z", 0.18f, -1.0f, 1.0f, 0.01f).a(() -> this.backpack.c());

    private final BooleanSetting pet = new BooleanSetting("Питомец", true);
    private final ModeSetting petStyle = new ModeSetting("Стиль питомца", "Цыплёнок", "Цыплёнок", "Свинка", "Серый волчонок").a(() -> this.pet.c());
    private final ModeSetting petAttach = new ModeSetting("Крепление питомца", "Левое плечо", ATTACH).a(() -> this.pet.c());
    private final SliderSetting petX = new SliderSetting("Питомец X", 0.0f, -1.0f, 1.0f, 0.01f).a(() -> this.pet.c());
    private final SliderSetting petY = new SliderSetting("Питомец Y", 0.12f, -1.0f, 1.0f, 0.01f).a(() -> this.pet.c());
    private final SliderSetting petZ = new SliderSetting("Питомец Z", 0.0f, -1.0f, 1.0f, 0.01f).a(() -> this.pet.c());

    private final BooleanSetting halo = new BooleanSetting("Нимб", true);
    private final ModeSetting haloStyle = new ModeSetting("Стиль нимба", "Неон", "Классический", "Святой", "Неон", "Демон").a(() -> this.halo.c());
    private final ModeSetting haloAttach = new ModeSetting("Крепление нимба", "Голова", ATTACH).a(() -> this.halo.c());
    private final SliderSetting haloX = new SliderSetting("Нимб X", 0.0f, -1.0f, 1.0f, 0.01f).a(() -> this.halo.c());
    private final SliderSetting haloY = new SliderSetting("Нимб Y", 0.35f, -1.0f, 1.0f, 0.01f).a(() -> this.halo.c());
    private final SliderSetting haloZ = new SliderSetting("Нимб Z", 0.0f, -1.0f, 1.0f, 0.01f).a(() -> this.halo.c());

    private final BooleanSetting wings = new BooleanSetting("Крылья", true);
    private final ModeSetting wingStyle = new ModeSetting("Стиль крыльев", "Ангел", "Ангел", "Демон", "Дракон", "Бабочка", "Феникс").a(() -> this.wings.c());
    private final ModeSetting wingAttach = new ModeSetting("Крепление крыльев", "Спина", ATTACH).a(() -> this.wings.c());
    private final SliderSetting wingX = new SliderSetting("Крылья X", 0.0f, -1.0f, 1.0f, 0.01f).a(() -> this.wings.c());
    private final SliderSetting wingY = new SliderSetting("Крылья Y", 0.05f, -1.0f, 1.0f, 0.01f).a(() -> this.wings.c());
    private final SliderSetting wingZ = new SliderSetting("Крылья Z", 0.12f, -1.0f, 1.0f, 0.01f).a(() -> this.wings.c());

    private final ModeSetting who = new ModeSetting("Показывать", "Себе и друзьям", "Только себе", "Себе и друзьям", "Всем с Malice");
    private final BooleanSetting syncTheme = new BooleanSetting("Цвет из темы", true);
    private final ColorSetting customColor = new ColorSetting("Цвет", Integer.valueOf(ColorUtil.convertToARGB(224, 92, 208, 255))).a(() -> Boolean.valueOf(!this.syncTheme.c().booleanValue()));
    private final SliderSetting scale = new SliderSetting("Размер", 1.0f, 0.6f, 1.8f, 0.05f);

    public PlayerCosmetics() {
        a(this.hat, this.hatStyle, this.hatAttach, this.hatX, this.hatY, this.hatZ,
                this.backpack, this.backpackStyle, this.backpackAttach, this.backpackX, this.backpackY, this.backpackZ,
                this.pet, this.petStyle, this.petAttach, this.petX, this.petY, this.petZ,
                this.halo, this.haloStyle, this.haloAttach, this.haloX, this.haloY, this.haloZ,
                this.wings, this.wingStyle, this.wingAttach, this.wingX, this.wingY, this.wingZ,
                this.who, this.syncTheme, this.customColor, this.scale);
    }

    @EventTarget
    public void onFeature(aethereal.event.PlayerCosmeticFeatureEvent event) {
        if (!n() || event.getPlayer() == null || event.getModel() == null) {
            return;
        }
        PlayerEntity player = event.getPlayer();
        if (!visible(player)) {
            return;
        }
        int base = this.syncTheme.c().booleanValue()
                ? Skeleton.getInstance().getModuleProcessor().o().a(ThemeInfo.PRIMARY).toIntColor()
                : this.customColor.c().intValue();
        float s = this.scale.c().floatValue();
        float time = (System.currentTimeMillis() % 100000L) / 1000.0f;
        int[] rgb = ColorUtil.b(base);
        MatrixStack matrices = event.getMatrices();

        if (this.hat.c().booleanValue()) {
            renderAttached(matrices, event.getModel(), this.hatAttach.c(),
                    this.hatX.c().floatValue(), this.hatY.c().floatValue(), this.hatZ.c().floatValue(), s,
                    (buffer, matrix, glow) -> writeHat(buffer, matrix, 0.55f, 1.0f, time, rgb[0], rgb[1], rgb[2], glow));
        }
        if (this.halo.c().booleanValue()) {
            renderAttached(matrices, event.getModel(), this.haloAttach.c(),
                    this.haloX.c().floatValue(), this.haloY.c().floatValue(), this.haloZ.c().floatValue(), s,
                    (buffer, matrix, glow) -> writeHalo(buffer, matrix, 0.55f, 1.0f, time, rgb[0], rgb[1], rgb[2], glow));
        }
        if (this.backpack.c().booleanValue()) {
            renderAttached(matrices, event.getModel(), this.backpackAttach.c(),
                    this.backpackX.c().floatValue(), this.backpackY.c().floatValue(), this.backpackZ.c().floatValue(), s,
                    (buffer, matrix, glow) -> writeBackpack(buffer, matrix, 0.45f, 1.0f, time, rgb[0], rgb[1], rgb[2], glow));
        }
        if (this.wings.c().booleanValue()) {
            renderAttached(matrices, event.getModel(), this.wingAttach.c(),
                    this.wingX.c().floatValue(), this.wingY.c().floatValue(), this.wingZ.c().floatValue(), s,
                    (buffer, matrix, glow) -> {
                        writeWing(buffer, matrix, 1.0f, 0.55f, 1.0f, time, rgb[0], rgb[1], rgb[2], glow);
                        writeWing(buffer, matrix, -1.0f, 0.55f, 1.0f, time, rgb[0], rgb[1], rgb[2], glow);
                    });
        }
        if (this.pet.c().booleanValue()) {
            matrices.push();
            applyAttach(matrices, event.getModel(), this.petAttach.c(),
                    this.petX.c().floatValue(), this.petY.c().floatValue(), this.petZ.c().floatValue());
            matrices.scale(s * 0.85f, s * 0.85f, s * 0.85f);
            CosmeticAnimalPets.renderAttached(matrices, player, this.petStyle.c(), time, event.getTickDelta(), event.getLight());
            matrices.pop();
        }
    }

    @FunctionalInterface
    private interface MeshWriter {
        void write(BufferBuilder buffer, Matrix4f matrix, boolean glow);
    }

    private void renderAttached(MatrixStack matrices, net.minecraft.client.render.entity.model.PlayerEntityModel model,
                                String attach, float ox, float oy, float oz, float scale, MeshWriter writer) {
        matrices.push();
        applyAttach(matrices, model, attach, ox, oy, oz);
        matrices.scale(scale, scale, scale);
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        begin(true);
        BufferBuilder glow = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        writer.write(glow, matrix, true);
        BufferRenderer.drawWithGlobalProgram(glow.end());

        begin(false);
        BufferBuilder mesh = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        writer.write(mesh, matrix, false);
        BufferRenderer.drawWithGlobalProgram(mesh.end());
        endBatch();
        matrices.pop();
    }

    private void applyAttach(MatrixStack matrices, net.minecraft.client.render.entity.model.PlayerEntityModel model,
                             String attach, float ox, float oy, float oz) {
        net.minecraft.client.model.ModelPart part = model.body;
        if ("Голова".equals(attach)) {
            part = model.head;
        } else if ("Левая рука".equals(attach)) {
            part = model.leftArm;
        } else if ("Правая рука".equals(attach)) {
            part = model.rightArm;
        }
        part.rotate(matrices);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0f));
        if ("Голова".equals(attach)) {
            matrices.translate(0.0f, -0.25f, 0.0f);
        } else if ("Левое плечо".equals(attach)) {
            matrices.translate(0.32f, 0.18f, 0.0f);
        } else if ("Правое плечо".equals(attach)) {
            matrices.translate(-0.32f, 0.18f, 0.0f);
        } else if ("Спина".equals(attach)) {
            matrices.translate(0.0f, 0.05f, 0.14f);
        } else if ("Левая рука".equals(attach) || "Правая рука".equals(attach)) {
            matrices.translate(0.0f, 0.20f, 0.0f);
        }
        matrices.translate(ox, oy, oz);
    }

'''

# Also remove obsolete renderOn world-space method if still present after visible()
rest = text[end:]
# Drop old renderOn that used world camera if present
marker = "    private void renderOn(BufferBuilder buffer, MatrixStack matrices, PlayerEntity player, Vec3d cam,"
if marker in rest:
    # keep visible/npc, remove renderOn until writeHat
    vis = rest
    i = vis.index(marker)
    j = vis.index("    private void writeHat(")
    rest = vis[:i] + vis[j:]

# Fix imports: remove DrawEvent/Camera if unused, ensure feature event imports not required (FQCN used)
if "import aethereal.event.DrawEvent;" in text[:start]:
    head = text[:start].replace("import aethereal.event.DrawEvent;\n", "")
    head = head.replace("import net.minecraft.client.render.Camera;\n", "")
else:
    head = text[:start]

p.write_text(head + new + rest, encoding="utf-8")
print("OK")
