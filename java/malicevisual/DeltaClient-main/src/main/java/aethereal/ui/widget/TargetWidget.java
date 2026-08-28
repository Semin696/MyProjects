package aethereal.ui.widget;

import aethereal.config.ThemeInfo;
import aethereal.core.Skeleton;
import aethereal.core.GlobalEvent;
import aethereal.core.Interface;
import aethereal.event.DrawEvent;
import aethereal.module.misc.StreamerMode;
import aethereal.render.*;
import aethereal.setting.BooleanSetting;
import aethereal.ui.element.DragInfo;
import aethereal.util.InventoryUtil;
import aethereal.util.MathUtil;
import aethereal.util.ServerUtil;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.EntityHitResult;

public class TargetWidget extends Widget implements Interface {
    private final BooleanSetting f;
    private final BooleanSetting g;
    private final AnimationUtil h;
    private final AnimationUtil i;
    private LivingEntity j;
    private String k;

    public TargetWidget() {
        super(new DragInfo("Таргет-худ", 0.0f, 0.0f, 0.0f, 0.0f));
        this.f = new BooleanSetting("Визуализация предметов", true);
        this.g = new BooleanSetting("Отображать при наводке", false);
        this.h = new AnimationUtil();
        this.i = new AnimationUtil();
        this.k = "";
        j().setWidget(this);
        a(this.g, this.f);
    }

    @Override
    public void a(DrawEvent event) {
        String string;
        d().a(0.0f, 1.0f, 0.3f, EasingList.g, event.g());
        if (a() > 0.0f && this.j != null) {
            float cardW = 148.0f;
            float cardH = 42.0f;
            j().setWidth(cardW);
            j().setHeight(cardH);
            float x = j().getClampedX();
            float y = j().getClampedY();

            int primary = Skeleton.getInstance().getModuleProcessor().o().a(ThemeInfo.PRIMARY).toIntColor();
            float anim = a();

            a(event, x, y, cardW, cardH, true, anim);

            float headSize = 24.0f;
            float headX = x + 8.0f;
            float headY = y + ((cardH - headSize) / 2.0f);

            event.getDraw2DProcessor().a(event.h(), headX, headY, headSize, headSize, 5.0f, ColorUtil.convertToARGB(16, 18, 26, (int) (255 * anim)));
            if (this.j instanceof AbstractClientPlayerEntity player && player.getSkinTextures() != null) {
                net.minecraft.client.gui.PlayerSkinDrawer.draw(event.i(), player.getSkinTextures(), (int) headX, (int) headY, (int) headSize);
            } else if (this.j != null) {
                Fonts.a.a(event.h(), "B", headX + ((headSize - 16.0f) / 2.0f), headY + ((headSize - 16.0f) / 2.0f), 16.0f, ColorUtil.applyAlphaToColor(primary, anim));
            }
            event.getDraw2DProcessor().a(event.h(), headX, headY, headSize, headSize, 5.0f, 0.6f, ColorUtil.applyAlphaToColor(primary, 0.55f * anim));

            float textX = headX + headSize + 7.0f;
            StreamerMode streamerMode = Skeleton.getInstance().getModuleProcessor().t().aE();
            if (streamerMode.m() && streamerMode.r().c().booleanValue()) {
                string = streamerMode.a(this.j.getName().getString());
            } else {
                string = this.j.getName().getString();
            }
            String name = string;

            float dist = (float) mc.player.distanceTo(this.j);
            String distBadge = String.format("%.1fm", Float.valueOf(dist));
            float distW = Fonts.c.a(distBadge, 5.25f) + 6.0f;

            float hpNow = ServerUtil.a.a$(this.j);
            float targetHP = MathUtil.b(MathUtil.b(hpNow, 0.0f, this.j.getMaxHealth()) / this.j.getMaxHealth(), 0.0f, 1.0f);
            float lineHP = this.h.a(targetHP, targetHP, 0.5f);
            int hpColor = targetHP > 0.5f
                    ? ColorUtil.lerpColor(ColorUtil.convertToARGB(255, 196, 72, 255), ColorUtil.convertToARGB(80, 220, 130, 255), (targetHP - 0.5f) * 2.0f)
                    : ColorUtil.lerpColor(ColorUtil.convertToARGB(230, 64, 78, 255), ColorUtil.convertToARGB(255, 196, 72, 255), targetHP * 2.0f);

            float maxNameW = (cardW - (textX - x)) - distW - 36.0f;
            Fonts.d.c(event.h(), name, textX, headY + 1.5f, 7.25f, ColorUtil.applyAlphaToColor(ColorUtil.convertToARGB(252, 253, 255, 255), anim), maxNameW);

            float distBadgeX = Math.min(textX + Math.min(Fonts.d.a(name, 7.25f), maxNameW) + 5.0f, (x + cardW) - distW - 32.0f);
            event.getDraw2DProcessor().a(event.h(), distBadgeX, headY + 1.5f, distW, 8.5f, 2.4f, ColorUtil.convertToARGB(18, 22, 32, (int) (210 * anim)));
            Fonts.c.a(event.h(), distBadge, distBadgeX + 3.0f, headY + 2.6f, 5.25f, ColorUtil.applyAlphaToColor(primary, 0.95f * anim));

            String hpValue = ((int) hpNow) + " HP";
            Fonts.d.a(event.h(), hpValue, (x + cardW - 8.0f) - Fonts.d.a(hpValue, 6.5f), headY + 1.5f, 6.5f, ColorUtil.applyAlphaToColor(hpColor, anim));

            float barW = (cardW - (textX - x)) - 8.0f;
            float barH = 4.0f;
            float barY = headY + 15.5f;

            event.getDraw2DProcessor().a(event.h(), textX, barY, barW, barH, 2.0f, ColorUtil.convertToARGB(22, 26, 36, (int) (245 * anim)));
            if (lineHP > 0.01f) {
                float activeW = Math.max(4.0f, barW * lineHP);
                event.getDraw2DProcessor().a(event.h(), textX, barY, activeW, barH, 2.0f, ColorUtil.applyAlphaToColor(hpColor, anim));
                event.getDraw2DProcessor().a(event.h(), textX + activeW - 2.2f, barY, 2.2f, barH, 1.1f, ColorUtil.applyAlphaToColor(ColorUtil.convertToARGB(255, 255, 255, 255), 0.7f * anim));
            }

            if (this.f.c().booleanValue()) {
                int itemIdx = 0;
                for (ItemStack stack : new ItemStack[]{this.j.getEquippedStack(EquipmentSlot.FEET), this.j.getEquippedStack(EquipmentSlot.LEGS), this.j.getEquippedStack(EquipmentSlot.CHEST), this.j.getEquippedStack(EquipmentSlot.HEAD), this.j.getOffHandStack(), this.j.getMainHandStack()}) {
                    if (!stack.isEmpty()) {
                        float itemSlotX = ((x + cardW) - 12.0f) - (itemIdx * 11.0f);
                        float itemSlotY = y + cardH + 3.0f;
                        event.getDraw3DProcessor().a(event.i(), InventoryUtil.a(stack), itemSlotX, itemSlotY, 0, anim, 0.55f, true);
                        itemIdx++;
                    }
                }
            }
        }
        super.a(event);
    }

    private void a(DrawEvent event, String current, String previous, float right, float y, float size, int color, float progress) {
        float height = Fonts.e.a(size);
        float cursor = right + Fonts.e.a(current, size);
        int i = 0;
        while (i < current.length()) {
            char digit = current.charAt((current.length() - 1) - i);
            char old = i < previous.length() ? previous.charAt((previous.length() - 1) - i) : ' ';
            String value = String.valueOf(digit);
            cursor -= Fonts.e.a(value, size);
            if (digit == old || progress >= 1.0f) {
                Fonts.e.a(event.h(), value, cursor, y, size, ColorUtil.applyAlphaToColor(color, a()));
            } else {
                ScissorUtil.a(event.h(), cursor - 0.5f, y, Fonts.e.a(value, size) + 1.0f, height + 1.0f);
                Fonts.e.a(event.h(), String.valueOf(old), cursor, y - (height * progress), size, ColorUtil.applyAlphaToColor(color, (1.0f - progress) * a()));
                Fonts.e.a(event.h(), value, cursor, y + (height * (1.0f - progress)), size, ColorUtil.applyAlphaToColor(color, progress * a()));
                ScissorUtil.a(event.h());
            }
            i++;
        }
    }

    @Override
    public void a(GlobalEvent event) {
        LivingEntity crosshair = null;
        if (Interface.mc.crosshairTarget instanceof EntityHitResult hit && hit.getEntity() instanceof LivingEntity living) {
            if (living != Interface.mc.player) {
                crosshair = living;
            }
        }
        LivingEntity target;
        if (crosshair != null) {
            target = crosshair;
        } else {
            target = Interface.mc.currentScreen instanceof ChatScreen ? Interface.mc.player : null;
        }
        boolean visible = target != null;
        if (target != null) {
            this.j = target;
        }
        d().a(visible);
        if (!visible && d().a() <= 0.0f) {
            this.j = null;
        }
        super.a(event);
    }
}
