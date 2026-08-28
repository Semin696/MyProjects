package aethereal.ui.widget;

import aethereal.config.ThemeInfo;
import aethereal.core.Skeleton;
import aethereal.core.GlobalEvent;
import aethereal.core.Interface;
import aethereal.event.DrawEvent;
import aethereal.mixin.IItemCooldownManager;
import aethereal.render.AnimationUtil;
import aethereal.render.ColorUtil;
import aethereal.render.EasingList;
import aethereal.render.Fonts;
import aethereal.ui.element.DragInfo;
import aethereal.util.InventoryUtil;
import aethereal.util.MathUtil;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import platform.inject.accessors.ItemCooldownEntryAccessor;
import platform.inject.accessors.ItemCooldownManagerAccessor;

import java.util.Locale;
import java.util.Map;

public class CooldownsWidget extends Widget implements Interface {
    public CooldownsWidget() {
        super(new DragInfo("Задержки", 0.0f, 0.0f, 0.0f, 0.0f));
        j().setWidget(this);
    }

    @Override
    public void a(DrawEvent event) {
        d().a(0.0f, 1.0f, 0.3f, EasingList.g, event.g());
        float x = j().getClampedX();
        float y = j().getClampedY();
        float targetWidth = 28.0f + Fonts.e.a("Задержки", this.e) + 8.0f;
        float contentY = y + this.d + 3.0f;
        ItemCooldownManagerAccessor accessor = (ItemCooldownManagerAccessor) mc.player.getItemCooldownManager();
        boolean active = false;
        for (Map.Entry<Identifier, Object> entry : accessor.getEntries().entrySet()) {
            IItemCooldownManager cooldown = (IItemCooldownManager) entry.getValue();
            ItemCooldownEntryAccessor end = (ItemCooldownEntryAccessor) entry.getValue();
            if (cooldown.getAnimation().c() > 0.0f) {
                Item item = Registries.ITEM.get(entry.getKey());
                active = true;
                int remaining = Math.max(end.getEndTick() - accessor.getTick(), 0);
                targetWidth = Math.max(targetWidth, 19.0f + Fonts.e.a(item.getName().getString(), 6.5f) + 8.0f + Fonts.e.a(String.format("%.1fс", Float.valueOf(remaining / 20.0f)), 6.5f) + 5.0f + 2.0f);
            }
        }
        float width = MathUtil.c(j().getWidth(), targetWidth, 0.5f);
        j().setWidth(width);
        if (a() > 0.0f) {
            a(event, "d", "Задержки", width, a());
        }
        for (Map.Entry<Identifier, Object> entry2 : accessor.getEntries().entrySet()) {
            IItemCooldownManager cooldown2 = (IItemCooldownManager) entry2.getValue();
            ItemCooldownEntryAccessor end2 = (ItemCooldownEntryAccessor) entry2.getValue();
            AnimationUtil animationUtil = cooldown2.getAnimation();
            animationUtil.a(0.0f, 1.0f, 0.3f, EasingList.g, event.g());
            float animation = animationUtil.c() * a();
            if (animation > 0.0f) {
                Item item2 = Registries.ITEM.get(entry2.getKey());
                int remaining2 = Math.max(end2.getEndTick() - accessor.getTick(), 0);
                String time = String.format(Locale.US, "%.1fс", Float.valueOf(remaining2 / 20.0f));
                float offsetX = (-8.0f) * (1.0f - animation);
                float offsetY = -(1.0f - animation);
                float drawY = contentY + offsetY;
                float timeWidth = Fonts.c.a(time, 6.5f);
                float textY = (drawY + ((12.0f - Fonts.d.a(6.5f)) / 2.0f)) - 0.5f;
                int primary = Skeleton.getInstance().getModuleProcessor().o().a(ThemeInfo.PRIMARY).toIntColor();

                a(event, x + offsetX, drawY, width, 12.0f, false, animation);
                a(event, x + offsetX + 15.0f, drawY, 12.0f, animation);
                event.getDraw3DProcessor().a(event.i(), item2.getDefaultStack(), x + offsetX + 5.0f, drawY + 2.0f, 0, animation, 0.45f, false);
                Fonts.d.a(event.h(), item2.getName().getString(), x + offsetX + 19.0f, textY, 6.5f, ColorUtil.applyAlphaToColor(ColorUtil.convertToARGB(255, 255, 255, 255), animation));
                Fonts.c.a(event.h(), time, ((((x + offsetX) + width) - 5.0f) - timeWidth) - 1.0f, textY, 6.5f, ColorUtil.applyAlphaToColor(primary, 0.9f * animation));
                contentY += 14.0f * animation;
            }
        }
        j().setHeight(active ? (contentY - y) - 2.0f : this.d);
        super.a(event);
    }

    @Override
    public void a(GlobalEvent event) {
        boolean visible = mc.currentScreen instanceof ChatScreen;
        ItemCooldownManagerAccessor accessor = (ItemCooldownManagerAccessor) mc.player.getItemCooldownManager();
        int tick = accessor.getTick();
        for (Map.Entry<Identifier, Object> entry : accessor.getEntries().entrySet()) {
            IItemCooldownManager cooldown = (IItemCooldownManager) entry.getValue();
            ItemCooldownEntryAccessor end = (ItemCooldownEntryAccessor) entry.getValue();
            boolean has = InventoryUtil.b(Registries.ITEM.get(entry.getKey())) != -1;
            cooldown.getAnimation().a(has && end.getEndTick() - 5 > tick);
            if (has && cooldown.getAnimation().c() > 0.0d) {
                visible = true;
            }
        }
        d().a(visible);
        super.a(event);
    }
}
