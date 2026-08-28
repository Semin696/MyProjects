package aethereal.module.render;

import aethereal.config.ThemeInfo;
import aethereal.core.Category;
import aethereal.core.EventTarget;
import aethereal.core.Module;
import aethereal.core.ModuleRegister;
import aethereal.core.Skeleton;
import aethereal.event.AmbienceEvent;
import aethereal.render.ColorUtil;
import aethereal.setting.BooleanSetting;
import aethereal.setting.ColorSetting;
import aethereal.setting.SliderSetting;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.FogShape;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.MathHelper;

@ModuleRegister(name = "Custom Fog", description = "Свой цвет и дальность тумана, отдельно от Ambience", category = Category.Render)
public class CustomFog extends Module {
    private final SliderSetting start = new SliderSetting("Начало", 10.0f, 1.0f, 100.0f, 1.0f);
    private final SliderSetting end = new SliderSetting("Конец", 50.0f, 1.0f, 100.0f, 1.0f);
    private final BooleanSetting syncTheme = new BooleanSetting("Цвет из темы", true);
    private final ColorSetting fogColor = new ColorSetting("Цвет тумана", Integer.valueOf(ColorUtil.convertToARGB(224, 92, 208, 180))).a(() -> {
        return Boolean.valueOf(!this.syncTheme.c().booleanValue());
    });

    public CustomFog() {
        a(this.start, this.end, this.syncTheme, this.fogColor);
    }

    @EventTarget(a = 4)
    public void onFogColor(AmbienceEvent.a event) {
        if (!shouldModifyFog(mc.gameRenderer == null ? null : mc.gameRenderer.getCamera())) {
            return;
        }
        float[] rgba = ColorUtil.a(color());
        event.setRed(rgba[0]);
        event.setGreen(rgba[1]);
        event.setBlue(rgba[2]);
        event.setAlpha(rgba[3]);
        event.a(true);
    }

    @EventTarget(a = 4)
    public void onFog(AmbienceEvent.b event) {
        if (!shouldModifyFog(event.getCamera())) {
            return;
        }
        float[] rgba = ColorUtil.a(color());
        float view = event.c();
        float fogStart = MathHelper.clamp(this.start.c().floatValue(), -8.0f, view);
        float fogEnd = MathHelper.clamp(this.end.c().floatValue(), 0.0f, view);
        if (fogStart > fogEnd) {
            float swap = fogStart;
            fogStart = fogEnd;
            fogEnd = swap;
        }
        event.a(fogStart, fogEnd, FogShape.SPHERE, rgba[0], rgba[1], rgba[2], rgba[3]);
    }

    private boolean shouldModifyFog(Camera camera) {
        if (camera == null || mc.world == null || mc.player == null) {
            return false;
        }
        CameraSubmersionType type = camera.getSubmersionType();
        if (type == CameraSubmersionType.WATER || type == CameraSubmersionType.LAVA || type == CameraSubmersionType.POWDER_SNOW) {
            return false;
        }
        Entity focused = camera.getFocusedEntity();
        if (focused instanceof LivingEntity living) {
            if (living.hasStatusEffect(StatusEffects.BLINDNESS) || living.hasStatusEffect(StatusEffects.DARKNESS)) {
                return false;
            }
        }
        return true;
    }

    private int color() {
        if (this.syncTheme.c().booleanValue()) {
            return Skeleton.getInstance().getModuleProcessor().o().a(ThemeInfo.PRIMARY).toIntColor();
        }
        return this.fogColor.c().intValue();
    }
}
