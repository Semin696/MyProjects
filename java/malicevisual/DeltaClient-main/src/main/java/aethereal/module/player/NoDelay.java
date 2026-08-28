package aethereal.module.player;

import aethereal.core.Category;
import aethereal.core.EventTarget;
import aethereal.core.GlobalEvent;
import aethereal.core.Module;
import aethereal.core.ModuleRegister;
import aethereal.event.TickEvent;
import aethereal.setting.BooleanSetting;
import platform.inject.accessors.LivingEntityAccessor;
import platform.inject.accessors.MinecraftClientAccessor;
import platform.inject.invokers.ClientPlayerInteractionManagerInvoker;

@ModuleRegister(name = "No Delay", description = "Убирает задержки прыжка, постановки и ломания блоков", category = Category.Player)
public class NoDelay extends Module {
    private final BooleanSetting jump = new BooleanSetting("Прыжок", true);
    private final BooleanSetting place = new BooleanSetting("Постановка", true);
    private final BooleanSetting breaking = new BooleanSetting("Ломание", true);

    public NoDelay() {
        a(this.jump, this.place, this.breaking);
    }

    @EventTarget
    public void onGlobal(GlobalEvent event) {
        applyPlaceAndBreak();
        applyJump();
    }

    @EventTarget
    public void onTick(TickEvent event) {
        applyJump();
    }

    private void applyPlaceAndBreak() {
        if (this.place.c().booleanValue() && mc instanceof MinecraftClientAccessor accessor) {
            accessor.setItemUseCooldown(0);
        }
        if (this.breaking.c().booleanValue() && mc.interactionManager instanceof ClientPlayerInteractionManagerInvoker manager) {
            manager.setBlockBreakingCooldown(0);
        }
    }

    private void applyJump() {
        if (this.jump.c().booleanValue() && mc.player instanceof LivingEntityAccessor player) {
            player.setJumpingCooldown(0);
        }
    }
}
