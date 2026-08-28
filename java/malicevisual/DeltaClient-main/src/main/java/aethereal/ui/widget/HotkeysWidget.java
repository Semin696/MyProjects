package aethereal.ui.widget;

import aethereal.config.ThemeInfo;
import aethereal.core.Skeleton;
import aethereal.core.GlobalEvent;
import aethereal.core.Interface;
import aethereal.core.Module;
import aethereal.event.DrawEvent;
import aethereal.render.ColorUtil;
import aethereal.render.EasingList;
import aethereal.render.Fonts;
import aethereal.ui.element.DragInfo;
import aethereal.util.KeyUtil;
import aethereal.util.MathUtil;
import net.minecraft.client.gui.screen.ChatScreen;

public class HotkeysWidget extends Widget implements Interface {
    public HotkeysWidget() {
        super(new DragInfo("Клавиши", 0.0f, 0.0f, 0.0f, 0.0f));
        j().setWidget(this);
    }

    @Override
    public void a(DrawEvent event) {
        d().a(0.0f, 1.0f, 0.3f, EasingList.g, event.g());
        float x = j().getClampedX();
        float y = j().getClampedY();
        float targetWidth = 28.0f + Fonts.e.a("Клавиши", this.e) + 8.0f;
        float contentY = y + this.d + 3.0f;
        float rightWidth = Fonts.a.a("Q", 6.5f);
        boolean active = false;
        for (Module module : Skeleton.getInstance().getModuleProcessor().t().e()) {
            if (module.p() != -1 && module.f().c() > 0.0f) {
                active = true;
                targetWidth = Math.max(targetWidth, 19.0f + Fonts.e.a(module.j(), 6.5f) + 8.0f + Fonts.e.a(KeyUtil.b(module.p()), 6.5f) + 4.0f + rightWidth + 5.0f + 2.0f);
            }
        }
        float width = MathUtil.c(j().getWidth(), targetWidth, 0.5f);
        j().setWidth(width);
        a(event, "Q", "Клавиши", width, a());
        for (Module module2 : Skeleton.getInstance().getModuleProcessor().t().e()) {
            module2.f().a(0.0f, 1.0f, 0.3f, EasingList.g, event.g());
            float animation = module2.p() != -1 ? module2.f().c() * a() : 0.0f;
            if (animation > 0.0f) {
                float offsetX = (-8.0f) * (1.0f - animation);
                float offsetY = -(1.0f - animation);
                float drawY = contentY + offsetY;
                String keyName = KeyUtil.b(module2.p());
                float bindWidth = Fonts.c.a(keyName, 6.0f);
                float pillW = bindWidth + 6.0f;
                float pillH = 9.0f;
                float pillX = (x + offsetX + width - 6.0f) - pillW;
                float pillY = drawY + ((12.0f - pillH) / 2.0f);
                float textY = (drawY + ((12.0f - Fonts.d.a(6.5f)) / 2.0f)) - 0.5f;

                int primary = Skeleton.getInstance().getModuleProcessor().o().a(ThemeInfo.PRIMARY).toIntColor();

                a(event, x + offsetX, drawY, width, 12.0f, false, animation);
                a(event, x + offsetX + 15.0f, drawY, 12.0f, animation);

                // Category Icon
                Fonts.a.a(event.h(), module2.l().a(), x + offsetX + 5.0f, (drawY + ((12.0f - Fonts.a.a(6.5f)) / 2.0f)), 6.5f, ColorUtil.applyAlphaToColor(primary, animation));
                
                // Module Name
                Fonts.d.a(event.h(), module2.j(), x + offsetX + 19.0f, textY, 6.5f, ColorUtil.applyAlphaToColor(ColorUtil.convertToARGB(255, 255, 255, 255), animation));

                // Keybind Pill
                event.getDraw2DProcessor().a(event.h(), pillX, pillY, pillW, pillH, 2.5f, ColorUtil.convertToARGB(22, 28, 40, (int) (220 * animation)));
                Fonts.c.a(event.h(), keyName, pillX + 3.0f, pillY + 1.0f, 6.0f, ColorUtil.applyAlphaToColor(primary, animation));

                contentY += 14.0f * animation;
            }
        }
        j().setHeight(active ? (contentY - y) - 2.0f : this.d);
        super.a(event);
    }

    @Override
    public void a(GlobalEvent event) {
        boolean visible = mc.currentScreen instanceof ChatScreen;
        for (Module module : Skeleton.getInstance().getModuleProcessor().t().e()) {
            if (module.p() != -1 && module.f().c() > 0.0f) {
                visible = true;
                break;
            }
        }
        d().a(visible);
        super.a(event);
    }
}
