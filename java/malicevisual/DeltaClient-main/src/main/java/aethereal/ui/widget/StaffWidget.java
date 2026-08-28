package aethereal.ui.widget;

import aethereal.config.ThemeInfo;
import aethereal.core.Skeleton;
import aethereal.core.GlobalEvent;
import aethereal.core.Interface;
import aethereal.event.DrawEvent;
import aethereal.render.AnimationUtil;
import aethereal.render.ColorUtil;
import aethereal.render.EasingList;
import aethereal.render.Fonts;
import aethereal.staff.StaffConstructor;
import aethereal.ui.element.DragInfo;
import aethereal.util.MathUtil;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.PlayerListEntry;

public class StaffWidget extends Widget implements Interface {
    public StaffWidget() {
        super(new DragInfo("Стафф", 0.0f, 0.0f, 0.0f, 0.0f));
        j().setWidget(this);
    }

    @Override
    public void a(DrawEvent event) {
        d().a(0.0f, 1.0f, 0.3f, EasingList.g, event.g());
        float x = j().getClampedX();
        float y = j().getClampedY();
        float targetWidth = 28.0f + Fonts.e.a("Стафф", this.e) + 8.0f;
        float contentY = y + this.d + 3.0f;
        boolean active = false;
        for (StaffConstructor staff : Skeleton.getInstance().getModuleProcessor().f().a()) {
            if (staff.b().c() > 0.0f) {
                targetWidth = Math.max(targetWidth, 19.0f + Fonts.e.a(staff.a(), 6.5f) + 8.0f + Fonts.e.a(a(staff.a()) ? "Near" : "Online", 6.5f) + 5.0f + 2.0f);
                active = true;
            }
        }
        float width = MathUtil.c(j().getWidth(), targetWidth, 0.5f);
        j().setWidth(width);
        if (a() > 0.0f) {
            a(event, "i", "Стафф", width, a());
        }
        for (StaffConstructor staff2 : Skeleton.getInstance().getModuleProcessor().f().a()) {
            AnimationUtil animationUtil = staff2.b();
            animationUtil.a(0.0f, 1.0f, 0.3f, EasingList.g, event.g());
            float animation = animationUtil.c() * a();
            if (animation > 0.0f) {
                float offsetX = (-8.0f) * (1.0f - animation);
                float offsetY = -(1.0f - animation);
                float drawY = contentY + offsetY;
                float textY = (drawY + ((12.0f - Fonts.d.a(6.5f)) / 2.0f)) - 0.5f;
                a(event, x + offsetX, drawY, width, 12.0f, false, animation);
                a(event, x + offsetX + 15.0f, drawY, 12.0f, animation);
                PlayerListEntry entry = mc.getNetworkHandler() == null ? null : mc.getNetworkHandler().getPlayerList().stream().filter(e -> {
                    return e.getProfile().getName().equalsIgnoreCase(staff2.a());
                }).findFirst().orElse(null);
                if (entry != null) {
                    event.getDraw2DProcessor().a(event.h(), x + offsetX + 5.0f, drawY + 2.0f, 8.0f, 8.0f, 2.0f, ColorUtil.applyAlphaToColor(-1, animation), 0.125f, 0.125f, 0.125f, 0.125f, mc.getTextureManager().getTexture(entry.getSkinTextures().texture()).getGlId());
                } else {
                    Fonts.a.a(event.h(), "y", x + offsetX + 5.0f, drawY + ((12.0f - Fonts.a.a(8.0f)) / 2.0f), 8.0f, ColorUtil.applyAlphaToColor(Skeleton.getInstance().getModuleProcessor().o().a(ThemeInfo.PRIMARY).toIntColor(), animation));
                }
                Fonts.d.a(event.h(), staff2.a(), x + offsetX + 19.0f, textY, 6.5f, ColorUtil.applyAlphaToColor(ColorUtil.convertToARGB(255, 255, 255, 255), animation));
                
                boolean near = a(staff2.a());
                String statusStr = near ? "Near" : "Online";
                int statusColor = near ? ColorUtil.convertToARGB(255, 70, 90, 255) : ColorUtil.convertToARGB(60, 255, 140, 255);
                float statusW = Fonts.c.a(statusStr, 5.5f) + 5.0f;
                float statusX = (x + offsetX + width - 5.0f) - statusW;
                float statusY = drawY + ((12.0f - 8.5f) / 2.0f);
                
                event.getDraw2DProcessor().a(event.h(), statusX, statusY, statusW, 8.5f, 2.0f, ColorUtil.applyAlphaToColor(ColorUtil.convertToARGB(20, 26, 38, 220), animation));
                Fonts.c.a(event.h(), statusStr, statusX + 2.5f, statusY + 1.25f, 5.5f, ColorUtil.applyAlphaToColor(statusColor, animation));
                contentY += 14.0f * animation;
            }
        }
        j().setHeight(active ? (contentY - y) - 2.0f : this.d);
        super.a(event);
    }

    @Override
    public void a(GlobalEvent event) {
        boolean visible = mc.currentScreen instanceof ChatScreen;
        for (StaffConstructor staff : Skeleton.getInstance().getModuleProcessor().f().a()) {
            staff.b().a((mc.getNetworkHandler() != null && mc.getNetworkHandler().getPlayerList().stream().anyMatch(e -> {
                return e.getProfile().getName().equalsIgnoreCase(staff.a());
            })) || a(staff.a()));
            if (staff.b().c() > 0.0f) {
                visible = true;
            }
        }
        d().a(visible);
        super.a(event);
    }

    private boolean a(String name) {
        return mc.world != null && mc.world.getPlayers().stream().anyMatch(playerEntity -> {
            return playerEntity.getName().getString().equalsIgnoreCase(name);
        });
    }
}
