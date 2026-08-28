package aethereal.ui.widget;

import aethereal.config.ThemeInfo;
import aethereal.config.ThemeProcessor;
import aethereal.core.Skeleton;
import aethereal.core.GlobalEvent;
import aethereal.core.Interface;
import aethereal.core.InterfaceC0020Opcode;
import aethereal.event.BackendEvent;
import aethereal.event.DrawEvent;
import aethereal.event.PacketEvent;
import aethereal.render.AnimationUtil;
import aethereal.render.ColorUtil;
import aethereal.render.EasingList;
import aethereal.render.Fonts;
import aethereal.setting.Setting;
import aethereal.ui.element.DragInfo;
import aethereal.ui.element.Element;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.List;

public class Widget {
    protected final AnimationUtil a = new AnimationUtil();
    protected final AnimationUtil b = new AnimationUtil();
    protected final AnimationUtil c = new AnimationUtil();
    protected final float d = 15.5f;
    protected final float e = 7.0f;
    private final List<Setting<?>> f = new ObjectArrayList<>();
    private final List<Element<?>> g = new ObjectArrayList<>();
    private final DragInfo i;
    private boolean h = false;

    public Widget(DragInfo dragInfo) {
        this.i = dragInfo;
        dragInfo.setWidget(this);
    }

    public List<Setting<?>> b() {
        return this.f;
    }

    public List<Element<?>> c() {
        return this.g;
    }

    public AnimationUtil d() {
        return this.a;
    }

    public AnimationUtil e() {
        return this.b;
    }

    public AnimationUtil f() {
        return this.c;
    }

    public void a(boolean status) {
        this.h = status;
    }

    public boolean g() {
        return this.h;
    }

    public float h() {
        return this.d;
    }

    public float i() {
        return this.e;
    }

    public DragInfo j() {
        return this.i;
    }

    protected final void a(Setting<?>... settings) {
        for (Setting<?> setting : settings) {
            this.f.add(setting);
            this.g.add(setting.createBooleanElement());
        }
    }

    public void a(DrawEvent event) {
        e().a(0.0f, 1.0f, 0.3f, EasingList.g, event.g());
        this.c.a(this.h && (Interface.mc.currentScreen instanceof ChatScreen));
        this.c.a(0.0f, 1.0f, 0.3f, EasingList.g, event.g());
        if (this.c.c() > 0.0f) {
            b(event);
        }
    }

    public void a(GlobalEvent event) {
        e().a(this.i == Skeleton.getInstance().getModuleProcessor().s().getActiveDragInfo());
    }

    public void a(PacketEvent event) {
    }

    public void a(BackendEvent event) {
    }

    protected void b(DrawEvent event) {
        List<Element<?>> visible = this.g.stream().filter(e -> {
            return e.getSetting().e().get().booleanValue();
        }).toList();
        if (!visible.isEmpty()) {
            float panelWidth = visible.stream().map(e2 -> {
                return Float.valueOf(19.5f + Fonts.e.a(e2.getSetting().i(), 6.5f) + 25.0f);
            }).reduce(Float.valueOf(0.0f), (v0, v1) -> {
                return Math.max(v0, v1);
            }).floatValue();
            float totalHeight = (12.0f * visible.size()) + (visible.size() - 1);
            float anim = this.c.c() * a();
            float baseX = (this.i.getClampedY() - totalHeight) - 2.0f >= 0.0f
                    ? (this.i.getClampedX() + (this.i.getWidth() / 2.0f)) - (panelWidth / 2.0f)
                    : this.i.getClampedX() + this.i.getWidth() + 2.0f;
            float baseY = (this.i.getClampedY() - totalHeight) - 2.0f >= 0.0f ? (this.i.getClampedY() - totalHeight) - 2.0f : this.i.getClampedY();
            float baseX2 = Math.min(Math.max(baseX, 0.0f),
                    (Interface.mc.getWindow().getScaledWidth() - panelWidth) - 2.0f);
            float baseY2 = Math.min(Math.max(baseY, 0.0f), Interface.mc.getWindow().getScaledHeight() - totalHeight);
            a(event, baseX2, baseY2, panelWidth, totalHeight, true, anim);
            float y = baseY2;
            for (Element<?> element : visible) {
                element.getBounds().set(baseX2, y, panelWidth, 12.0f);
                element.onDrawEvent(event, baseX2, y, panelWidth, anim);
                y += 12.0f + 1.0f;
                if (element != visible.getLast()) {
                    event.getDraw2DProcessor().a(event.i().getMatrices(), baseX2, y - 1.0f, panelWidth, 0.75f, 0.0f, ColorUtil.applyAlphaToColor(
                            ColorUtil.convertToARGB(InterfaceC0020Opcode.aN, InterfaceC0020Opcode.aN, InterfaceC0020Opcode.aN, 255),
                            0.2f * anim));
                }
            }
        }
    }

    protected void a(DrawEvent event, String icon, Object title, float width, float animation) {
        a(event, icon, title, width, animation, Skeleton.getInstance().getModuleProcessor().o().a(ThemeInfo.PRIMARY).toIntColor());
    }

    protected void a(DrawEvent event, String icon, Object title, float width, float animation, int iconColor) {
        a(event, this.i.getClampedX(), this.i.getClampedY(), icon, title, width, animation, iconColor);
    }

    protected void a(DrawEvent event, float x, float y, String icon, Object title, float width, float animation,
                     int iconColor) {
        a(event, x, y, icon, null, title, width, animation, iconColor);
    }

    protected void a(DrawEvent event, float x, float y, ItemStack icon, Object title, float width, float animation,
                     int iconColor) {
        a(event, x, y, null, icon, title, width, animation, iconColor);
    }

    protected void a(DrawEvent event, float x, float y, String icon, ItemStack stack, Object title, float width,
                      float animation, int iconColor) {
        if (animation > 0.0f) {
            a(event, x, y, width, this.d, true, animation);
            float badge = 10.0f;
            float badgeX = x + 6.0f;
            float badgeY = y + ((this.d - badge) / 2.0f);
            event.getDraw2DProcessor().a(event.h(), badgeX, badgeY, badge, badge, 2.75f, ColorUtil.applyAlphaToColor(iconColor, 0.2f * animation));
            if (stack != null) {
                Skeleton.getInstance().getModuleProcessor().j().a(event.i(), stack, badgeX + 1.0f, badgeY + 1.0f, 0,
                        animation, 0.5f, false);
            } else {
                float iconSize = this.e - 0.25f;
                float iconW = Fonts.a.a(icon, iconSize);
                float iconY = Fonts.a.a(icon, iconSize, badgeY + (badge / 2.0f));
                Fonts.a.a(event.h(), icon, badgeX + ((badge - iconW) / 2.0f), iconY, iconSize,
                        ColorUtil.applyAlphaToColor(iconColor, animation));
            }
            a(event, badgeX + badge + 3.5f, y, this.d, animation);
            float titleX = badgeX + badge + 7.5f;
            float titleY = (y + ((this.d - Fonts.d.a(this.e)) / 2.0f)) - 0.35f;
            if (title instanceof Text text) {
                Fonts.d.a(event.h(), text.getString(), titleX, titleY, this.e,
                        ColorUtil.applyAlphaToColor(ColorUtil.convertToARGB(248, 250, 255, 255), animation));
            } else {
                Fonts.d.a(event.h(), String.valueOf(title), titleX, titleY, this.e,
                        ColorUtil.applyAlphaToColor(ColorUtil.convertToARGB(248, 250, 255, 255), animation));
            }
        }
    }

    protected void a(DrawEvent event, float x, float y, float width, float height, boolean glow, float animation) {
        if (animation > 0.0f) {
            ThemeProcessor themeProcessor = Skeleton.getInstance().getModuleProcessor().o();
            int primary = themeProcessor.a(ThemeInfo.PRIMARY).toIntColor();
            int hud = themeProcessor.a(ThemeInfo.BACKGROUND_HUD).toIntColor();
            float hudAlpha = themeProcessor.a(ThemeInfo.BACKGROUND_HUD).getAlphaFloat();
            int background = ColorUtil.applyAlphaToColor(ColorUtil.lerpColor(hud, primary, 0.07f), Math.min(0.94f, hudAlpha + 0.12f) * animation);
            float radius = glow ? 5.0f : 3.5f;

            if (glow) {
                event.getDraw2DProcessor().a(event.h(), x - 1.0f, y - 1.0f, width + 2.0f, height + 2.0f, radius + 1.0f, ColorUtil.applyAlphaToColor(primary, 0.1f * animation));
                event.getDraw2DProcessor().b(event.h(), x, y, width, height, radius, background, animation);
            }

            event.getDraw2DProcessor().a(event.h(), x, y, width, height, radius, background);
        }
    }

    protected void a(DrawEvent event, float x, float y, float height, float animation) {
        float separatorHeight = height * 0.42f;
        float separatorY = y + ((height - separatorHeight) / 2.0f);
        event.getDraw2DProcessor().a(event.h(), x, separatorY, 0.75f, separatorHeight, 0.35f, ColorUtil.applyAlphaToColor(ColorUtil.convertToARGB(255, 255, 255, 255), 0.12f * animation));
    }

    public float a() {
        return this.a.c() * (1.0f - (0.1f * this.b.c()));
    }
}
