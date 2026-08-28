package aethereal.module.movement;

import aethereal.core.Category;
import aethereal.core.EventTarget;
import aethereal.core.Module;
import aethereal.core.ModuleRegister;
import aethereal.event.TickEvent;
import aethereal.setting.SliderSetting;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.hit.EntityHitResult;

@ModuleRegister(name = "Shift Tap", description = "На крите коротко жмёт шифт, чтобы сбросить спринт", category = Category.Movement)
public class ShiftTap extends Module {
    private final SliderSetting holdTicks = new SliderSetting("Тики удержания", 3.0f, 1.0f, 10.0f, 1.0f);
    private boolean wasAttackPressed;
    private int sneakTicksRemaining;
    private boolean shouldReleaseNext;
    private boolean forcedSneak;

    public ShiftTap() {
        a(this.holdTicks);
    }

    @Override
    public void c() {
        releaseSneak();
        this.wasAttackPressed = false;
        super.c();
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.options == null) {
            return;
        }
        if (mc.player.isUsingItem()) {
            this.sneakTicksRemaining = 0;
            this.shouldReleaseNext = false;
            this.wasAttackPressed = mc.options.attackKey.isPressed();
            return;
        }
        if (this.shouldReleaseNext) {
            releaseSneak();
            this.shouldReleaseNext = false;
        }
        boolean attackPressed = mc.options.attackKey.isPressed();
        if (attackPressed && !this.wasAttackPressed && canCrit() && mc.crosshairTarget instanceof EntityHitResult) {
            int hold = Math.max(1, Math.round(this.holdTicks.c().floatValue()));
            if (!mc.options.sneakKey.isPressed()) {
                pressSneak();
                if (hold == 1) {
                    this.shouldReleaseNext = true;
                } else {
                    this.sneakTicksRemaining = hold - 1;
                }
            }
        }
        this.wasAttackPressed = attackPressed;
        if (this.sneakTicksRemaining > 0) {
            this.sneakTicksRemaining--;
            if (this.sneakTicksRemaining == 0) {
                releaseSneak();
            }
        }
    }

    private boolean canCrit() {
        return !mc.player.isOnGround()
                && mc.player.getVelocity().y < 0.0d
                && !mc.player.isTouchingWater()
                && !mc.player.isClimbing()
                && !mc.player.hasVehicle()
                && !mc.player.hasStatusEffect(StatusEffects.BLINDNESS)
                && !mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING)
                && !mc.player.hasStatusEffect(StatusEffects.LEVITATION);
    }

    private void pressSneak() {
        mc.options.sneakKey.setPressed(true);
        this.forcedSneak = true;
    }

    private void releaseSneak() {
        this.sneakTicksRemaining = 0;
        this.shouldReleaseNext = false;
        if (this.forcedSneak && mc.options != null && mc.player != null && !mc.player.isUsingItem()) {
            mc.options.sneakKey.setPressed(false);
        }
        this.forcedSneak = false;
    }
}
