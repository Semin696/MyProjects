package aethereal.module.player;

import aethereal.core.Category;
import aethereal.core.EventTarget;
import aethereal.core.Module;
import aethereal.core.ModuleRegister;
import aethereal.core.Processor;
import aethereal.core.Skeleton;
import aethereal.event.TickEvent;
import aethereal.setting.BindSetting;
import aethereal.setting.ModeSetting;
import aethereal.setting.SliderSetting;
import aethereal.util.MathUtil;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

@ModuleRegister(name = "Zoom", description = "Приближает камеру по бинду с настраиваемой плавностью и дальностью", category = Category.Player)
public class Zoom extends Module {
    private final ModeSetting mode = new ModeSetting("Режим", "По зажатию", "По зажатию", "По нажатию");
    private final BindSetting bind;
    private final SliderSetting smoothness = new SliderSetting("Плавность", 6.0f, 1.0f, 10.0f, 0.5f);
    private final SliderSetting distance = new SliderSetting("Дальность", 30.0f, 10.0f, 70.0f, 1.0f);
    private boolean zooming;
    private float animated = -1.0f;
    private float lastOriginal = 70.0f;
    private float lastAnimated = 70.0f;
    private float scrollOffset;

    public Zoom() {
        this.bind = new BindSetting("Кнопка зума", Integer.valueOf(GLFW.GLFW_KEY_C), 0).a(() -> {
            if (this.mode.l("По зажатию")) {
                setZooming(true);
            } else {
                setZooming(!this.zooming);
            }
        }).b(() -> {
            if (this.mode.l("По зажатию")) {
                setZooming(false);
            }
        });
        a(this.mode, this.bind, this.smoothness, this.distance);
    }

    public static Zoom current() {
        try {
            Processor processor = Skeleton.getInstance().getModuleProcessor();
            if (processor == null || processor.t() == null) {
                return null;
            }
            return processor.t().getZoom();
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static float apply(float original, boolean update) {
        Zoom zoom = current();
        if (zoom == null) {
            return original;
        }
        return zoom.modify(original, update);
    }

    public static float sensitivity() {
        Zoom zoom = current();
        if (zoom == null || zoom.lastOriginal < 1.0f) {
            return 1.0f;
        }
        return MathHelper.clamp(zoom.lastAnimated / zoom.lastOriginal, 0.12f, 1.0f);
    }

    public static boolean onScroll(double vertical) {
        Zoom zoom = current();
        if (zoom == null || !zoom.m() || !zoom.zooming || mc.currentScreen != null) {
            return false;
        }
        zoom.scrollOffset -= (float) vertical * 4.0f;
        float base = zoom.distance.c().floatValue();
        zoom.scrollOffset = MathHelper.clamp(zoom.scrollOffset, 10.0f - base, 70.0f - base);
        return true;
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (this.zooming && (mc.currentScreen != null || mc.player == null)) {
            setZooming(false);
        }
    }

    @Override
    public void c() {
        setZooming(false);
        this.scrollOffset = 0.0f;
        super.c();
    }

    private void setZooming(boolean value) {
        this.zooming = value;
        if (!value) {
            this.scrollOffset = 0.0f;
        }
    }

    private float modify(float original, boolean update) {
        if (!update) {
            return original;
        }
        this.lastOriginal = original;
        if (this.animated < 0.0f) {
            this.animated = original;
        }
        boolean active = m() && this.zooming && mc.currentScreen == null && mc.player != null;
        float target = active ? MathHelper.clamp(this.distance.c().floatValue() + this.scrollOffset, 10.0f, 70.0f) : original;
        float speed = Math.max(0.08f, (11.0f - this.smoothness.c().floatValue()) * 0.16f);
        this.animated = MathUtil.c(this.animated, target, speed);
        if (!active && Math.abs(this.animated - original) < 0.12f) {
            this.animated = original;
        }
        this.lastAnimated = this.animated;
        return this.animated;
    }
}
