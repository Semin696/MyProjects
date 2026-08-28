package aethereal.ui.widget;

import aethereal.config.ThemeInfo;
import aethereal.config.ThemeProcessor;
import aethereal.core.Interface;
import aethereal.core.Skeleton;
import aethereal.core.User;
import aethereal.event.DrawEvent;
import aethereal.render.ColorUtil;
import aethereal.render.Draw2DProcessor;
import aethereal.render.EasingList;
import aethereal.render.Fonts;
import aethereal.setting.BooleanSetting;
import aethereal.ui.element.DragInfo;
import aethereal.util.MathUtil;
import aethereal.util.ServerUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class WatermarkWidget extends Widget implements Interface {
    private static final Identifier AVATAR = Identifier.of("skeleton", "pictures/avatar.png");
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final BooleanSetting split;
    private final BooleanSetting fps;
    private final BooleanSetting ping;
    private final BooleanSetting time;
    private final BooleanSetting login;
    private final BooleanSetting coords;
    private final BooleanSetting tps;
    private final BooleanSetting speed;
    private float fpsSmooth;

    public WatermarkWidget() {
        super(new DragInfo("Инфо-панель", 6.0f, 6.0f, 0.0f, 0.0f));
        this.split = new BooleanSetting("Разделять элементы", true);
        this.fps = new BooleanSetting("Частота кадров", true);
        this.ping = new BooleanSetting("Задержка игрока", true);
        this.time = new BooleanSetting("Текущее время", true);
        this.login = new BooleanSetting("Логин в клиенте", true);
        this.coords = new BooleanSetting("Координаты", true);
        this.tps = new BooleanSetting("Задержка сервера", true);
        this.speed = new BooleanSetting("Скорость игрока", true);
        j().setWidget(this);
        j().setDragStatus(0);
        a(this.split, this.fps, this.ping, this.time, this.login, this.coords, this.tps, this.speed);
    }

    @Override
    public void a(DrawEvent event) {
        d().a(true);
        d().a(0.0f, 1.0f, 0.3f, EasingList.g, event.g());
        this.fpsSmooth = MathUtil.c(this.fpsSmooth, mc.getCurrentFps(), 0.12f);
        float anim = a();
        if (anim <= 0.01f) {
            super.a(event);
            return;
        }

        ThemeProcessor theme = Skeleton.getInstance().getModuleProcessor().o();
        int primary = theme.a(ThemeInfo.PRIMARY).toIntColor();
        Draw2DProcessor draw = event.getDraw2DProcessor();
        MatrixStack matrices = event.h();
        List<String[]> chips = chips();
        float x = j().getClampedX();
        float y = j().getClampedY();
        float h = 20.0f;
        float mark = 13.0f;
        float pad = 7.0f;
        float width = pad + mark + 5.0f + Fonts.d.a("MALICE", 6.4f) + 3.0f + Fonts.c.a("VISUALS", 5.1f) + 6.0f;
        for (String[] chip : chips) {
            width += (this.split.c().booleanValue() ? 8.0f : 6.0f) + Fonts.a.a(chip[0], 5.8f) + 3.0f + Fonts.d.a(chip[1], 6.1f);
        }
        width += pad;

        draw.a(matrices, x, y, width, h, 7.0f, ColorUtil.convertToARGB(16, 8, 22, (int) (175.0f * anim)), anim, ColorUtil.applyAlphaToColor(primary, 0.32f * anim), 11.0f);
        draw.a(matrices, x, y, width, h, 7.0f, ColorUtil.convertToARGB(14, 8, 20, (int) (170.0f * anim)));

        float cursor = x + pad;
        draw.a(matrices, AVATAR, cursor, y + ((h - mark) / 2.0f), mark, mark, 3.5f, ColorUtil.applyAlphaToColor(-1, anim));
        cursor += mark + 5.0f;
        Fonts.d.a(matrices, "MALICE", cursor, y + 5.6f, 6.4f, ColorUtil.applyAlphaToColor(-1, anim));
        Fonts.c.a(matrices, "VISUALS", cursor + Fonts.d.a("MALICE", 6.4f) + 3.0f, y + 6.6f, 5.1f, ColorUtil.applyAlphaToColor(primary, anim));
        cursor += Fonts.d.a("MALICE", 6.4f) + 3.0f + Fonts.c.a("VISUALS", 5.1f) + 6.0f;

        for (int i = 0; i < chips.size(); i++) {
            String[] chip = chips.get(i);
            if (this.split.c().booleanValue() || i == 0) {
                draw.a(matrices, cursor, y + 5.0f, 0.7f, h - 10.0f, 0.35f, ColorUtil.applyAlphaToColor(primary, 0.45f * anim));
                cursor += 6.0f;
            } else {
                cursor += 5.0f;
            }
            Fonts.a.a(matrices, chip[0], cursor, y + ((h - 5.8f) / 2.0f), 5.8f, ColorUtil.applyAlphaToColor(primary, anim));
            cursor += Fonts.a.a(chip[0], 5.8f) + 3.0f;
            Fonts.d.a(matrices, chip[1], cursor, y + 5.8f, 6.1f, ColorUtil.applyAlphaToColor(ColorUtil.convertToARGB(240, 228, 246, 255), anim));
            cursor += Fonts.d.a(chip[1], 6.1f);
        }

        j().setWidth(width);
        j().setHeight(h);
        super.a(event);
    }

    private List<String[]> chips() {
        List<String[]> list = new ArrayList<>();
        if (this.login.c().booleanValue()) {
            list.add(new String[]{"L", userName()});
        }
        if (this.fps.c().booleanValue()) {
            list.add(new String[]{"q", ((int) this.fpsSmooth) + " FPS"});
        }
        if (this.ping.c().booleanValue()) {
            list.add(new String[]{"P", pingValue() + "ms"});
        }
        if (this.time.c().booleanValue()) {
            list.add(new String[]{"T", LocalTime.now().format(CLOCK)});
        }
        if (this.coords.c().booleanValue() && mc.player != null) {
            list.add(new String[]{"b", ((int) mc.player.getX()) + " " + ((int) mc.player.getY()) + " " + ((int) mc.player.getZ())});
        }
        if (this.tps.c().booleanValue()) {
            list.add(new String[]{"g", String.format("%.1f", Float.valueOf(tpsValue()))});
        }
        if (this.speed.c().booleanValue() && mc.player != null) {
            list.add(new String[]{"e", String.format("%.2f", Double.valueOf(ServerUtil.c()))});
        }
        return list;
    }

    private String userName() {
        try {
            User user = Skeleton.getInstance().g();
            if (user != null && user.username() != null && !user.username().isBlank() && !"SkeletonUser".equals(user.username())) {
                return user.username();
            }
        } catch (Exception ignored) {
        }
        return mc.getSession() != null ? mc.getSession().getUsername() : "Player";
    }

    private int pingValue() {
        try {
            return ServerUtil.d();
        } catch (Exception ignored) {
            return 0;
        }
    }

    private float tpsValue() {
        try {
            return Skeleton.getInstance().getModuleProcessor().v().getTPSHandler().a();
        } catch (Exception ignored) {
            return 20.0f;
        }
    }
}
