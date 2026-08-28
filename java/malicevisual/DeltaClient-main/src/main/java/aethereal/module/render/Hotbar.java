package aethereal.module.render;

import aethereal.core.Category;
import aethereal.core.EventTarget;
import aethereal.core.Module;
import aethereal.core.ModuleRegister;
import aethereal.core.Skeleton;
import aethereal.event.DrawEvent;
import aethereal.render.Animations;
import aethereal.setting.SliderSetting;
import aethereal.util.MathUtil;

@ModuleRegister(name = "Хотбар", description = "Кастомный хотбар вместо ванильного: размер и скорость слотов", category = Category.Render)
public class Hotbar extends Module {
    private final SliderSetting scale = new SliderSetting("Размер", 0.88f, 0.55f, 1.5f, 0.05f);
    private final SliderSetting slotSpeed = new SliderSetting("Скорость слотов", 1.25f, 0.2f, 4.0f, 0.05f);
    private float animatedSlot = -1.0f;

    public Hotbar() {
        a(this.scale, this.slotSpeed);
    }

    public float hudScale() {
        return m() ? this.scale.c().floatValue() : 1.0f;
    }

    public static boolean customEnabled() {
        Hotbar hotbar = Skeleton.getInstance().getModuleProcessor().t().getHotbar();
        return hotbar != null && hotbar.m();
    }

    public float selectionIndex() {
        if (mc.player == null) {
            return 0.0f;
        }
        int selected = mc.player.getInventory().selectedSlot;
        if (m()) {
            return this.animatedSlot < 0.0f ? selected : this.animatedSlot;
        }
        Animations animations = Skeleton.getInstance().getModuleProcessor().t().Q();
        if (animations != null && animations.m() && animations.q().a("Слот хотбара").c().booleanValue()) {
            return animations.v();
        }
        return selected;
    }

    @Override
    public void c() {
        super.c();
        this.animatedSlot = -1.0f;
    }

    @EventTarget
    public void onDraw(DrawEvent event) {
        if (!event.b() || mc.player == null) {
            return;
        }
        int selected = mc.player.getInventory().selectedSlot;
        this.animatedSlot = this.animatedSlot < 0.0f
                ? selected
                : MathUtil.c(this.animatedSlot, selected, this.slotSpeed.c().floatValue());
    }
}
