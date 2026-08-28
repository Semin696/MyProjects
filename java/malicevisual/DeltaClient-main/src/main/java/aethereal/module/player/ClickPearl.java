package aethereal.module.player;

import aethereal.core.Category;
import aethereal.core.EventTarget;
import aethereal.core.Module;
import aethereal.core.ModuleRegister;
import aethereal.event.ClickEvent;
import aethereal.event.HotbarEvent;
import aethereal.event.TickEvent;
import aethereal.setting.BindSetting;
import aethereal.setting.ModeSetting;
import aethereal.util.InventoryUtil;
import aethereal.util.KeyUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import platform.inject.invokers.ClientPlayerInteractionManagerInvoker;

@ModuleRegister(name = "Click Pearl", description = "Кидает эндер-жемчуг по бинду: легитно в руку или пакетом из инвентаря", category = Category.Player)
public class ClickPearl extends Module {
    private final ModeSetting mode = new ModeSetting("Режим", "Легитный", "Легитный", "Пакетный");
    private final BindSetting throwBind;
    private int stage;
    private int waitTicks;
    private int originalHotbar;
    private int inventorySlot = -1;

    public ClickPearl() {
        this.throwBind = new BindSetting("Кнопка", Integer.valueOf(KeyUtil.MMB.a())).a(this::onThrow);
        a(this.mode, this.throwBind);
    }

    @Override
    public void c() {
        restore();
        super.c();
    }

    @EventTarget
    public void onClick(ClickEvent event) {
        if (!event.b()) {
            return;
        }
        if (this.throwBind.c().intValue() == (-100 + event.h())) {
            event.a(true);
        }
    }

    @EventTarget
    public void onHotbar(HotbarEvent event) {
        if (this.stage != 0) {
            event.a(true);
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (this.stage == 0 || mc.player == null || mc.interactionManager == null) {
            return;
        }
        if (!mc.player.isAlive() || mc.currentScreen != null) {
            restore();
            return;
        }
        if (this.waitTicks > 0) {
            this.waitTicks--;
            return;
        }
        if (this.stage == 1) {
            useLegit(Hand.MAIN_HAND);
            this.stage = 2;
            this.waitTicks = 1;
            return;
        }
        if (this.stage == 2) {
            restore();
        }
    }

    private void onThrow() {
        if (mc.player == null || mc.world == null || mc.interactionManager == null || mc.player.networkHandler == null) {
            return;
        }
        if (this.stage != 0 || mc.currentScreen != null || !mc.player.isAlive() || mc.player.isSpectator()) {
            return;
        }
        ItemStack probe = new ItemStack(Items.ENDER_PEARL);
        if (mc.player.getItemCooldownManager().isCoolingDown(probe)) {
            return;
        }
        if (this.mode.l("Пакетный")) {
            throwPacket();
        } else {
            throwLegit();
        }
    }

    private void throwLegit() {
        if (mc.player.getMainHandStack().isOf(Items.ENDER_PEARL)) {
            useLegit(Hand.MAIN_HAND);
            return;
        }
        if (mc.player.getOffHandStack().isOf(Items.ENDER_PEARL)) {
            useLegit(Hand.OFF_HAND);
            return;
        }
        int hotbar = InventoryUtil.a(Items.ENDER_PEARL, true);
        this.originalHotbar = mc.player.getInventory().selectedSlot;
        if (hotbar != -1) {
            this.inventorySlot = -1;
            if (hotbar != this.originalHotbar) {
                selectClient(hotbar);
            }
            armLegit();
            return;
        }
        int inv = InventoryUtil.a(Items.ENDER_PEARL, false);
        if (inv < 9) {
            return;
        }
        this.inventorySlot = inv;
        swapInventory(inv, this.originalHotbar);
        armLegit();
    }

    private void throwPacket() {
        if (mc.player.getMainHandStack().isOf(Items.ENDER_PEARL)) {
            usePacket(Hand.MAIN_HAND);
            return;
        }
        if (mc.player.getOffHandStack().isOf(Items.ENDER_PEARL)) {
            usePacket(Hand.OFF_HAND);
            return;
        }
        int selected = mc.player.getInventory().selectedSlot;
        int hotbar = InventoryUtil.a(Items.ENDER_PEARL, true);
        if (hotbar != -1) {
            if (hotbar != selected) {
                mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(hotbar));
            }
            usePacket(Hand.MAIN_HAND);
            if (hotbar != selected) {
                mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(selected));
            }
            return;
        }
        int inv = InventoryUtil.a(Items.ENDER_PEARL, false);
        if (inv < 9) {
            return;
        }
        swapInventory(inv, selected);
        if (mc.player.getMainHandStack().isOf(Items.ENDER_PEARL)) {
            usePacket(Hand.MAIN_HAND);
        }
        swapInventory(inv, selected);
        closeInventory();
    }

    private void armLegit() {
        this.stage = 1;
        this.waitTicks = 1;
    }

    private void restore() {
        this.waitTicks = 0;
        if (this.stage == 0) {
            this.inventorySlot = -1;
            return;
        }
        this.stage = 0;
        if (mc.player == null || mc.interactionManager == null) {
            this.inventorySlot = -1;
            return;
        }
        if (this.inventorySlot >= 9) {
            swapInventory(this.inventorySlot, this.originalHotbar);
            closeInventory();
        } else if (mc.player.getInventory().selectedSlot != this.originalHotbar) {
            selectClient(this.originalHotbar);
        }
        this.inventorySlot = -1;
    }

    private void swapInventory(int inventoryIndex, int hotbarIndex) {
        mc.interactionManager.clickSlot(
                mc.player.playerScreenHandler.syncId,
                toScreenSlot(inventoryIndex),
                hotbarIndex,
                SlotActionType.SWAP,
                mc.player
        );
    }

    private void closeInventory() {
        mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(mc.player.playerScreenHandler.syncId));
    }

    private static int toScreenSlot(int inventoryIndex) {
        return inventoryIndex < 9 ? 36 + inventoryIndex : inventoryIndex;
    }

    private void selectClient(int slot) {
        mc.player.getInventory().selectedSlot = slot;
        mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
    }

    private void useLegit(Hand hand) {
        mc.interactionManager.interactItem(mc.player, hand);
    }

    private void usePacket(Hand hand) {
        ((ClientPlayerInteractionManagerInvoker) mc.interactionManager).invokeSendSequencedPacket(mc.world, sequence ->
                new PlayerInteractItemC2SPacket(hand, sequence, mc.player.getYaw(), mc.player.getPitch()));
    }
}
