package aethereal.module.render;

import aethereal.config.ThemeInfo;
import aethereal.core.Category;
import aethereal.core.Module;
import aethereal.core.ModuleRegister;
import aethereal.core.Processor;
import aethereal.core.Skeleton;
import aethereal.render.ColorUtil;
import aethereal.setting.BooleanSetting;
import aethereal.setting.ColorSetting;
import aethereal.setting.ModeSetting;
import aethereal.setting.SliderSetting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ModelTransformationMode;

@ModuleRegister(name = "Кастомный тотем", description = "Заменяет тотем бессмертия на 3D-модель: Малис, кристалл, неон, ангел, демон, феникс, руна", category = Category.Render)
public class CustomTotem extends Module {
    private final ModeSetting model = new ModeSetting("Модель", "Малис",
            "Малис", "Кристалл", "Неон", "Ангел", "Демон", "Феникс", "Руна");
    private final SliderSetting scale = new SliderSetting("Размер", 1.0f, 0.65f, 1.7f, 0.05f);
    private final BooleanSetting inventory = new BooleanSetting("В инвентаре", true);
    private final BooleanSetting animate = new BooleanSetting("Анимация", true);
    private final BooleanSetting syncTheme = new BooleanSetting("Цвет из темы", true);
    private final ColorSetting customColor = new ColorSetting("Акцент", Integer.valueOf(ColorUtil.convertToARGB(224, 92, 208, 255))).a(() -> {
        return Boolean.valueOf(!this.syncTheme.c().booleanValue());
    });

    public CustomTotem() {
        a(this.model, this.scale, this.inventory, this.animate, this.syncTheme, this.customColor);
    }

    public String style() {
        return this.model.c();
    }

    public float scale() {
        return this.scale.c().floatValue();
    }

    public boolean animate() {
        return this.animate.c().booleanValue();
    }

    public boolean showInInventory() {
        return this.inventory.c().booleanValue();
    }

    public int accentColor() {
        if (this.syncTheme.c().booleanValue()) {
            return Skeleton.getInstance().getModuleProcessor().o().a(ThemeInfo.PRIMARY).toIntColor();
        }
        return this.customColor.c().intValue();
    }

    public static CustomTotem current() {
        Skeleton skeleton = Skeleton.getInstance();
        if (skeleton == null) {
            return null;
        }
        Processor processor = skeleton.getModuleProcessor();
        if (processor == null || processor.t() == null) {
            return null;
        }
        return processor.t().getCustomTotem();
    }

    public static boolean shouldReplace(ItemStack stack, ModelTransformationMode mode) {
        if (stack == null || stack.isEmpty() || !stack.isOf(Items.TOTEM_OF_UNDYING)) {
            return false;
        }
        CustomTotem module = current();
        if (module == null || !module.m()) {
            return false;
        }
        if (mode == ModelTransformationMode.GUI && !module.showInInventory()) {
            return false;
        }
        return true;
    }
}
