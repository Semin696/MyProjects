package aethereal.ui.widget;

import aethereal.core.Skeleton;
import aethereal.core.Interface;
import aethereal.core.InterfaceC0020Opcode;
import aethereal.event.DrawEvent;
import aethereal.render.ColorUtil;
import aethereal.render.ScaleUtil;
import aethereal.ui.element.DragInfo;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Identifier;

public class ArmorWidget extends Widget implements Interface {
    public ArmorWidget() {
        super(new DragInfo("Броня", 0.0f, 0.0f, 0.0f, 0.0f));
        j().setWidget(this);
    }

    @Override
    public void a(DrawEvent event) {
        if (event.b() && !mc.options.hudHidden && !mc.player.isSpectator()) {
            EquipmentSlot[] armorSlots = {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};
            int count = 0;
            for (EquipmentSlot slot : armorSlots) {
                if (!mc.player.getEquippedStack(slot).isEmpty()) {
                    count++;
                }
            }
            if (count > 0) {
                ScaleUtil.b(event.i());
                event.i().getMatrices().push();
                event.i().getMatrices().translate(0.0f, (-16.0f) * Skeleton.getInstance().getModuleProcessor().t().Q().s().c(), 0.0f);
                int totalW = (count * 24) - 2;
                int startX = (mc.getWindow().getScaledWidth() - totalW) / 2;
                int startY = mc.getWindow().getScaledHeight() - 60;
                int primary = Skeleton.getInstance().getModuleProcessor().o().a(aethereal.config.ThemeInfo.PRIMARY).toIntColor();

                int index = 0;
                for (EquipmentSlot slot2 : armorSlots) {
                    ItemStack stack = mc.player.getEquippedStack(slot2);
                    if (!stack.isEmpty()) {
                        float tileX = startX + (index * 24);
                        float tileY = startY;
                        float tileW = 22.0f;
                        float tileH = 22.0f;

                        // Modern glass tile background & border
                        event.getDraw2DProcessor().a(event.h(), tileX, tileY, tileW, tileH, 4.0f, ColorUtil.convertToARGB(12, 16, 26, 200));
                        event.getDraw2DProcessor().a(event.h(), tileX, tileY, tileW, tileH, 4.0f, 0.5f, ColorUtil.applyAlphaToColor(primary, 0.40f));

                        // Render Item
                        event.i().drawItem(stack, (int) (tileX + 3), (int) (tileY + 3));

                        // Durability Bar
                        if (stack.isDamageable()) {
                            float maxDamage = stack.getMaxDamage();
                            float damage = stack.getDamage();
                            float durRatio = Math.max(0.0f, Math.min(1.0f, 1.0f - (damage / maxDamage)));

                            int durColor = ColorUtil.lerpColor(ColorUtil.convertToARGB(255, 60, 60, 255), ColorUtil.convertToARGB(60, 255, 120, 255), durRatio);
                            event.getDraw2DProcessor().a(event.h(), tileX + 3.0f, tileY + 18.0f, 16.0f, 1.5f, 0.75f, ColorUtil.convertToARGB(25, 30, 42, 255));
                            if (durRatio > 0.0f) {
                                event.getDraw2DProcessor().a(event.h(), tileX + 3.0f, tileY + 18.0f, Math.max(1.5f, 16.0f * durRatio), 1.5f, 0.75f, durColor);
                            }
                        }

                        index++;
                    }
                }
                event.i().getMatrices().pop();
                ScaleUtil.c(event.i());
            }
        }
        super.a(event);
    }
}
