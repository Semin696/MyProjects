package aethereal.module.render;

import aethereal.config.ThemeInfo;
import aethereal.config.ThemeProcessor;
import aethereal.config.ThemeType;
import aethereal.core.*;
import aethereal.core.Module;
import aethereal.event.BackendEvent;
import aethereal.event.DrawEvent;
import aethereal.event.PacketEvent;
import aethereal.setting.BooleanSetting;
import aethereal.setting.ColorSetting;
import aethereal.setting.ModeSetting;
import aethereal.setting.MultiModeSetting;
import aethereal.ui.widget.*;

import java.util.ArrayList;
import java.util.List;

@ModuleRegister(name = "Interface", description = "Отображает выбранные элементы интерфейса на экране", category = Category.Render)
public class Interface extends Module {
    private final ModeSetting themeMode = new ModeSetting("Тема оформления", "Тёмная", "Тёмная", "Светлая")
            .a(selected -> {
                Skeleton.getInstance().getModuleProcessor().o().a(this.themeMode.l("Светлая") ? ThemeType.LIGHT : ThemeType.DARK);
            });
    private final ColorSetting globalColor = new ColorSetting("Глобальный цвет интерфейса",
            Integer.valueOf(Skeleton.getInstance().getModuleProcessor().o().a(ThemeInfo.PRIMARY).toIntColor()));
    private final MultiModeSetting widgetToggles = new MultiModeSetting("Элементы интерфейса",
            new BooleanSetting("Клавиши", true), new BooleanSetting("Таргет-худ", true),
            new BooleanSetting("Задержки", true), new BooleanSetting("Инфо-панель", true),
            new BooleanSetting("Уведомления", true), new BooleanSetting("Зелья", true),
            new BooleanSetting("Броня", true), new BooleanSetting("Окружение", true));
    private final List<Widget> widgets = new ArrayList<>();

    public Interface() {
        ThemeType current = Skeleton.getInstance().getModuleProcessor().o().a();
        this.themeMode.a(current == ThemeType.LIGHT ? "Светлая" : "Тёмная");
        a(this.themeMode, this.globalColor, this.widgetToggles);
        this.widgets.add(new ArmorWidget());
        this.widgets.add(new HotkeysWidget());
        this.widgets.add(new CooldownsWidget());
        this.widgets.add(new TargetWidget());
        this.widgets.add(new WatermarkWidget());
        this.widgets.add(new PotionWidget());
        this.widgets.add(new NotificationWidget());
        this.widgets.add(new EnvironmentWidget());
    }

    public List<Widget> q() {
        return this.widgets;
    }

    public void applyBrandColor() {
        int color = ThemeProcessor.migrateLegacyPrimary(this.globalColor.c().intValue());
        this.globalColor.a(Integer.valueOf(color));
        Skeleton.getInstance().getModuleProcessor().o().a(ThemeInfo.PRIMARY).fromIntColor(color);
    }

    @EventTarget
    public void onDraw(DrawEvent event) {
        if (event.b()) {
            Skeleton.getInstance().getModuleProcessor().o().a(ThemeInfo.PRIMARY).fromIntColor(this.globalColor.c().intValue());
            for (Widget widget : this.widgets) {
                if (enabled(widget)) {
                    widget.a(event);
                }
            }
        }
    }

    @EventTarget
    public void a(GlobalEvent event) {
        for (Widget widget : this.widgets) {
            if (enabled(widget)) {
                widget.a(event);
            }
        }
    }

    @EventTarget
    public void a(PacketEvent event) {
        for (Widget widget : this.widgets) {
            if (enabled(widget)) {
                widget.a(event);
            }
        }
    }

    @EventTarget
    public void a(BackendEvent event) {
        for (Widget widget : this.widgets) {
            if (enabled(widget)) {
                widget.a(event);
            }
        }
    }

    private boolean enabled(Widget widget) {
        BooleanSetting toggle = this.widgetToggles.a(widget.j().getName());
        return toggle != null && toggle.c().booleanValue();
    }
}
