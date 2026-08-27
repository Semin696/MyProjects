package aethereal.ui.screen;

import aethereal.config.ThemeInfo;
import aethereal.config.ThemeProcessor;
import aethereal.core.Interface;
import aethereal.core.Processor;
import aethereal.core.Skeleton;
import aethereal.render.AnimationUtil;
import aethereal.render.ColorUtil;
import aethereal.render.Draw2DProcessor;
import aethereal.render.EasingList;
import aethereal.render.Fonts;
import aethereal.render.ScaleUtil;
import aethereal.ui.shader.GradientUtil;
import aethereal.ui.widget.EffectMarker;
import aethereal.util.ClientRestart;
import aethereal.util.MathUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class MainScreen extends Screen {
    private static final Identifier BACKGROUND = Identifier.of("skeleton", "pictures/main.png");
    private static final Identifier AVATAR = Identifier.of("skeleton", "pictures/avatar.png");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final float[] PARALLAX = new float[2];

    private final AnimationUtil openAnimation;
    private final List<NavItem> actions;
    private final List<EffectMarker.a> markers;

    public MainScreen() {
        super(Text.empty());
        this.openAnimation = new AnimationUtil();
        this.markers = new ArrayList<>();
        if (Interface.mc.currentScreen instanceof MainScreen) {
            this.openAnimation.c(1.0f);
            this.openAnimation.d(1.0f);
            this.openAnimation.e(1.0f);
        }
        this.actions = List.of(
                new NavItem("L", "Одиночная игра", "Локальные миры", NavItem.Kind.PRIMARY, () -> Interface.mc.setScreen(new SelectWorldScreen(null))),
                new NavItem("I", "Сетевая игра", "Серверы и друзья", NavItem.Kind.PRIMARY, () -> Interface.mc.setScreen(new MultiplayerScreen(null))),
                new NavItem("D", "Аккаунты", "Смена профиля", NavItem.Kind.PRIMARY, () -> Interface.mc.setScreen(new AltScreen())),
                new NavItem("t", "Настройки", "Игра и видео", NavItem.Kind.PRIMARY, () -> Interface.mc.setScreen(new OptionsScreen(null, Interface.mc.options))),
                new NavItem("Q", "Перезапустить", "Новый запуск клиента", NavItem.Kind.RESTART, ClientRestart::restart),
                new NavItem("c", "Выйти", "Закрыть клиент", NavItem.Kind.QUIT, () -> Interface.mc.scheduleStop())
        );
    }

    public static void a(DrawContext context, int width, int height, int mouseX, int mouseY, float scale) {
        float marginX = width * 0.03f;
        float marginY = height * 0.03f;
        PARALLAX[0] += (MathHelper.clamp((((mouseX / (float) width) - 0.5f) * 2.0f) * marginX, (-marginX) * 0.9f, marginX * 0.9f) - PARALLAX[0]) * 0.03f;
        PARALLAX[1] += (MathHelper.clamp((((mouseY / (float) height) - 0.5f) * 2.0f) * marginY, (-marginY) * 0.9f, marginY * 0.9f) - PARALLAX[1]) * 0.03f;
        MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.translate(width / 2.0f, height / 2.0f, 0.0f);
        matrices.scale(scale, scale, 1.0f);
        matrices.translate((-width) / 2.0f, (-height) / 2.0f, 0.0f);
        Skeleton.getInstance().getModuleProcessor().i().a(matrices, BACKGROUND, (-marginX) + PARALLAX[0], (-marginY) + PARALLAX[1], width + (marginX * 2.0f), height + (marginY * 2.0f), 0.0f, -1);
        matrices.pop();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        this.openAnimation.a(Interface.mc.currentScreen instanceof MainScreen);
        this.openAnimation.a(0.0f, 1.0f, 0.18f, EasingList.g, delta);
        float open = Math.min(1.0f, this.openAnimation.c() / 0.9f);
        float eased = EasingList.s.ease(open);
        double scaledX = MathUtil.scale(mouseX, 2);
        double scaledY = MathUtil.scale(mouseY, 2);
        ScaleUtil.a(context, 2);

        int width = Interface.mc.getWindow().getScaledWidth();
        int height = Interface.mc.getWindow().getScaledHeight();
        a(context, width, height, (int) scaledX, (int) scaledY, 1.16f - (eased * 0.10f));

        Processor processor = Skeleton.getInstance().getModuleProcessor();
        Draw2DProcessor draw = processor.i();
        ThemeProcessor theme = processor.o();
        int primary = theme.a(ThemeInfo.PRIMARY).toIntColor();
        int violet = ColorUtil.convertToARGB(148, 72, 230, 255);
        if (draw.e() != null) {
            draw.e().a(context.getMatrices());
        }

        MatrixStack matrices = context.getMatrices();
        draw.a(matrices, 0.0f, 0.0f, width, height, 0.0f,
                ColorUtil.convertToARGB(4, 1, 7, (int) (230.0f * open)),
                ColorUtil.convertToARGB(2, 0, 5, (int) (210.0f * open)),
                ColorUtil.convertToARGB(6, 1, 9, (int) (240.0f * open)),
                ColorUtil.convertToARGB(1, 0, 3, (int) (225.0f * open)));
        draw.a(matrices, width * 0.18f, height * 0.08f, 140.0f, 140.0f, 70.0f, ColorUtil.applyAlphaToColor(primary, 0.035f * open));
        draw.a(matrices, width * 0.62f, height * 0.42f, 170.0f, 170.0f, 85.0f, ColorUtil.applyAlphaToColor(violet, 0.025f * open));

        layout(width, height);
        renderBrand(context, width, height, open, eased, primary, violet, draw);
        renderActions(context, (int) scaledX, (int) scaledY, delta, open, eased, primary, draw);
        renderFooter(context, width, height, open, primary);

        EffectMarker.a(context.getMatrices(), delta, this.markers);
        ScaleUtil.a(context);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double scaledX = MathUtil.scale(mouseX, 2);
        double scaledY = MathUtil.scale(mouseY, 2);
        EffectMarker.a(this.markers, (float) scaledX, (float) scaledY);
        if (button == 0) {
            for (NavItem item : this.actions) {
                if (item.hovered(scaledX, scaledY)) {
                    item.action.run();
                    return true;
                }
            }
        }
        return super.mouseClicked(scaledX, scaledY, button);
    }

    @Override
    public void close() {
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    private void layout(int width, int height) {
        float tileW = 128.0f;
        float tileH = 46.0f;
        float gap = 8.0f;
        float gridW = (tileW * 2.0f) + gap;
        float gridH = (tileH * 2.0f) + gap;
        float gridX = (width - gridW) * 0.5f;
        float gridY = height * 0.50f;
        for (int i = 0; i < 4; i++) {
            NavItem item = this.actions.get(i);
            item.x = gridX + ((i % 2) * (tileW + gap));
            item.y = gridY + ((i / 2) * (tileH + gap));
            item.w = tileW;
            item.h = tileH;
        }
        float pairY = gridY + gridH + 12.0f;
        float half = (gridW - gap) * 0.5f;
        NavItem restart = this.actions.get(4);
        restart.x = gridX;
        restart.y = pairY;
        restart.w = half;
        restart.h = 24.0f;
        NavItem quit = this.actions.get(5);
        quit.x = gridX + half + gap;
        quit.y = pairY;
        quit.w = half;
        quit.h = 24.0f;
    }

    private void renderBrand(DrawContext context, int width, int height, float open, float eased, int primary, int violet, Draw2DProcessor draw) {
        MatrixStack matrices = context.getMatrices();
        float scale = 0.90f + (0.10f * eased);
        float cx = width * 0.5f;
        float top = height * 0.12f;
        matrices.push();
        matrices.translate(cx, top + 40.0f, 0.0f);
        matrices.scale(scale, scale, 1.0f);
        matrices.translate(-cx, -(top + 40.0f), 0.0f);

        float avatar = 58.0f;
        float avatarX = cx - (avatar / 2.0f);
        draw.a(matrices, avatarX, top, avatar, avatar, 16.0f, ColorUtil.convertToARGB(8, 3, 12, 255), open, ColorUtil.applyAlphaToColor(primary, 0.28f * open), 18.0f);
        draw.a(matrices, AVATAR, avatarX, top, avatar, avatar, 16.0f, ColorUtil.applyAlphaToColor(-1, open));
        draw.a(matrices, avatarX, top, avatar, avatar, 16.0f, 1.1f, ColorUtil.applyAlphaToColor(primary, 0.38f * open));

        float titleY = top + avatar + 12.0f;
        Fonts.d.b(matrices, "MALICE", cx, titleY, 18.0f, ColorUtil.applyAlphaToColor(-1, open));
        Fonts.e.a(matrices, GradientUtil.a("VISUALS", primary, 4.2f, 0.4f), cx - (Fonts.e.a("VISUALS", 13.5f) / 2.0f), titleY + 20.0f, 13.5f, 0.0f, open);
        Fonts.c.b(matrices, "Minecraft 1.21.4", cx, titleY + 40.0f, 6.0f, ColorUtil.applyAlphaToColor(ColorUtil.convertToARGB(140, 118, 150, 255), open));
        matrices.pop();
    }

    private void renderActions(DrawContext context, int mouseX, int mouseY, float delta, float open, float eased, int primary, Draw2DProcessor draw) {
        MatrixStack matrices = context.getMatrices();
        for (NavItem item : this.actions) {
            item.hover.a(item.hovered(mouseX, mouseY));
            item.hover.a(0.0f, 1.0f, 0.26f, EasingList.i, delta);
            float hover = Math.min(1.0f, item.hover.c() / 0.9f);
            float scale = (0.90f + (0.10f * eased)) * (1.0f + (0.03f * hover));
            float cx = item.x + (item.w / 2.0f);
            float cy = item.y + (item.h / 2.0f);
            matrices.push();
            matrices.translate(cx, cy, 0.0f);
            matrices.scale(scale, scale, 1.0f);
            matrices.translate(-cx, -cy, 0.0f);

            int fill;
            int outline;
            int titleColor;
            int accent = primary;
            if (item.kind == NavItem.Kind.QUIT) {
                accent = ColorUtil.convertToARGB(230, 90, 130, 255);
                fill = ColorUtil.convertToARGB(14, 3, 8, (int) (210.0f * open));
                outline = ColorUtil.applyAlphaToColor(accent, (0.16f + (0.40f * hover)) * open);
                titleColor = ColorUtil.lerpColor(ColorUtil.convertToARGB(180, 130, 145, 255), accent, hover);
            } else if (item.kind == NavItem.Kind.RESTART) {
                accent = ColorUtil.convertToARGB(120, 210, 255, 255);
                fill = ColorUtil.convertToARGB(5, 7, 12, (int) (210.0f * open));
                outline = ColorUtil.applyAlphaToColor(accent, (0.12f + (0.40f * hover)) * open);
                titleColor = ColorUtil.lerpColor(ColorUtil.convertToARGB(140, 160, 180, 255), accent, hover);
            } else {
                fill = ColorUtil.convertToARGB(8, 3, 12, (int) (220.0f * open));
                outline = ColorUtil.lerpColor(ColorUtil.convertToARGB(255, 255, 255, (int) (8.0f * open)), ColorUtil.applyAlphaToColor(primary, 0.40f * open), hover);
                titleColor = ColorUtil.convertToARGB(230, 230, 235, 255);
                draw.a(matrices, item.x, item.y, item.w, item.h, 11.0f, fill, open, ColorUtil.applyAlphaToColor(primary, 0.22f * hover * open), 10.0f * hover);
            }

            if (item.kind != NavItem.Kind.PRIMARY) {
                draw.a(matrices, item.x, item.y, item.w, item.h, 8.0f, fill);
            }
            draw.a(matrices, item.x, item.y, item.w, item.h, item.kind == NavItem.Kind.PRIMARY ? 11.0f : 8.0f, 0.6f, outline);

            int iconColor = ColorUtil.applyAlphaToColor(ColorUtil.lerpColor(ColorUtil.convertToARGB(160, 130, 175, 255), accent, hover), open);
            if (item.kind == NavItem.Kind.PRIMARY) {
                Fonts.a.a(matrices, item.icon, item.x + 12.0f, Fonts.a.a(item.icon, 11.0f, item.y + (item.h / 2.0f)), 11.0f, iconColor);
                Fonts.d.a(matrices, item.title, item.x + 30.0f, item.y + 10.0f, 7.6f, ColorUtil.applyAlphaToColor(titleColor, open));
                Fonts.c.a(matrices, item.hint, item.x + 30.0f, item.y + 24.0f, 5.3f, ColorUtil.applyAlphaToColor(ColorUtil.convertToARGB(120, 100, 135, 255), open));
            } else {
                Fonts.d.b(matrices, item.title, cx, item.y + 6.5f, 6.5f, ColorUtil.applyAlphaToColor(titleColor, open));
            }
            matrices.pop();
        }
    }

    private void renderFooter(DrawContext context, int width, int height, float open, int primary) {
        MatrixStack matrices = context.getMatrices();
        String user = Interface.mc.getSession() != null ? Interface.mc.getSession().getUsername() : "Player";
        String left = "Malice Visuals  ·  " + user;
        String clock = LocalTime.now().format(TIME);
        Fonts.c.a(matrices, left, 18.0f, height - 14.0f, 5.4f, ColorUtil.applyAlphaToColor(ColorUtil.convertToARGB(120, 100, 135, 255), open));
        Fonts.c.a(matrices, clock, width - 18.0f - Fonts.c.a(clock, 5.4f), height - 14.0f, 5.4f, ColorUtil.applyAlphaToColor(primary, open));
    }

    private static final class NavItem {
        private final String icon;
        private final String title;
        private final String hint;
        private final Kind kind;
        private final Runnable action;
        private final AnimationUtil hover = new AnimationUtil();
        private float x;
        private float y;
        private float w;
        private float h;

        private NavItem(String icon, String title, String hint, Kind kind, Runnable action) {
            this.icon = icon;
            this.title = title;
            this.hint = hint;
            this.kind = kind;
            this.action = action;
        }

        private boolean hovered(double mouseX, double mouseY) {
            return MathUtil.a(mouseX, mouseY, this.x, this.y, this.w, this.h);
        }

        private enum Kind {
            PRIMARY,
            RESTART,
            QUIT
        }
    }
}
