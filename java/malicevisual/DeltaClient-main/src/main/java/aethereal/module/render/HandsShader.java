package aethereal.module.render;

import aethereal.config.ThemeInfo;
import aethereal.core.*;
import aethereal.core.Module;
import aethereal.event.HandEvent;
import aethereal.render.ColorUtil;
import aethereal.setting.BooleanSetting;
import aethereal.setting.ColorSetting;
import aethereal.setting.SliderSetting;
import aethereal.ui.shader.NoiseShader;
import net.minecraft.client.option.Perspective;

@ModuleRegister(name = "Hands Shader", description = "Накладывает шейдер на руку от первого лица", category = Category.Render)
public class HandsShader extends Module {
    private final BooleanSetting syncTheme = new BooleanSetting("Цвет из темы", true);
    private final ColorSetting customColor = new ColorSetting("Цвет", Integer.valueOf(ColorUtil.convertToARGB(180, 20, 45, 255))).a(() -> {
        return Boolean.valueOf(!this.syncTheme.c().booleanValue());
    });
    private final SliderSetting opacity = new SliderSetting("Непрозрачность", 0.6f, 0.0f, 1.0f, 0.05f);
    private final SliderSetting speed = new SliderSetting("Скорость шейдера", 1.0f, 0.1f, 5.0f, 0.05f);

    public HandsShader() {
        a(this.syncTheme, this.customColor, this.opacity, this.speed);
    }

    @EventTarget
    public void a(HandEvent event) {
        NoiseShader shader = Skeleton.getInstance().getModuleProcessor().i().f();
        if (mc.options.getPerspective() == Perspective.FIRST_PERSON) {
            if (event.isPreEvent()) {
                shader.e();
            }
            if (event.isPostEvent()) {
                int baseColor = this.syncTheme.c().booleanValue()
                        ? Skeleton.getInstance().getModuleProcessor().o().a(ThemeInfo.PRIMARY).toIntColor()
                        : this.customColor.c().intValue();
                float[] color = ColorUtil.a(baseColor);
                color[3] = this.opacity.c().floatValue();
                shader.a(color, this.speed.c().floatValue());
            }
        }
    }
}
