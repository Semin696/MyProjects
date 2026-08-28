package aethereal.ui.screen;

import aethereal.config.ThemeInfo;
import aethereal.config.ThemeProcessor;
import aethereal.core.Skeleton;
import aethereal.friend.FriendConstructor;
import aethereal.friend.FriendProcessor;
import aethereal.render.AnimationUtil;
import aethereal.render.ColorUtil;
import aethereal.render.Draw2DProcessor;
import aethereal.render.EasingList;
import aethereal.render.Fonts;
import aethereal.ui.element.TextField;
import aethereal.util.MathUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector2f;
import net.minecraft.entity.player.PlayerEntity;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class FriendsPanel {
    private final TextField addField = new TextField(TextField.type.GUI);
    private final AnimationUtil[] toggleAnims = {new AnimationUtil(), new AnimationUtil(), new AnimationUtil(), new AnimationUtil()};
    private final List<Row> rows = new ArrayList<>();
    private final List<Chip> chips = new ArrayList<>();
    private final List<Nearby> nearby = new ArrayList<>();
    private float targetScroll;
    private float scroll;
    private float listTop;
    private float listBottom;
    private float addBtnX;
    private float addBtnY;
    private float addBtnW;
    private float addBtnH;
    private float aimBtnX;
    private float aimBtnY;
    private float aimBtnW;
    private float aimBtnH;

    public FriendsPanel() {
        this.addField.setPlaceholder("Ник или заметка...");
    }

    public void render(DrawContext context, float x, float y, float width, float height, double mouseX, double mouseY, float delta, float alpha) {
        MatrixStack matrices = context.getMatrices();
        Draw2DProcessor draw = Skeleton.getInstance().getModuleProcessor().i();
        ThemeProcessor theme = Skeleton.getInstance().getModuleProcessor().o();
        FriendProcessor friends = Skeleton.getInstance().getModuleProcessor().e();
        int primary = theme.a(ThemeInfo.PRIMARY).toIntColor();
        this.chips.clear();
        this.nearby.clear();
        this.rows.clear();

        float pad = 10.0f;
        float gap = 6.0f;
        float innerW = width - pad * 2.0f;
        float innerX = x + pad;
        float cursorY = y + 8.0f;

        long total = friends.a().size();
        long online = friends.onlineCount();
        float statsH = 28.0f;
        draw.a(matrices, innerX, cursorY, innerW, statsH, 8.0f, ColorUtil.convertToARGB(16, 12, 26, (int) (170 * alpha)));
        Fonts.d.a(matrices, "Друзья", innerX + 12.0f, cursorY + 6.0f, 7.2f, ColorUtil.applyAlphaToColor(-1, alpha));
        String stats = total + " в списке  ·  " + online + " онлайн";
        Fonts.c.a(matrices, stats, innerX + 12.0f, cursorY + 16.5f, 5.3f,
                ColorUtil.applyAlphaToColor(theme.a(ThemeInfo.TEXT_DISABLED).toIntColor(), alpha));
        cursorY += statsH + 8.0f;

        float chipH = 20.0f;
        float chipW = (innerW - gap * 3.0f) / 4.0f;
        pill(draw, matrices, friends, 0, "Урон", "Не бить", friends.g(), innerX, cursorY, chipW, chipH, mouseX, mouseY, delta, alpha, primary);
        pill(draw, matrices, friends, 1, "Свечение", "Glow", friends.highlight(), innerX + chipW + gap, cursorY, chipW, chipH, mouseX, mouseY, delta, alpha, primary);
        pill(draw, matrices, friends, 2, "Не толкать", "Push", friends.noPush(), innerX + (chipW + gap) * 2.0f, cursorY, chipW, chipH, mouseX, mouseY, delta, alpha, primary);
        pill(draw, matrices, friends, 3, "Не цель", "ESP", friends.skipEsp(), innerX + (chipW + gap) * 3.0f, cursorY, chipW, chipH, mouseX, mouseY, delta, alpha, primary);
        cursorY += chipH + 8.0f;

        float fieldH = 24.0f;
        float btnW = 78.0f;
        float fieldW = innerW - btnW * 2.0f - gap * 2.0f;
        this.addField.setPosition(new Vector2f(innerX, cursorY));
        this.addField.setSize(new Vector2f(fieldW, fieldH));
        this.addField.render(context, mouseX, mouseY, delta, alpha);

        this.aimBtnX = innerX + fieldW + gap;
        this.aimBtnY = cursorY;
        this.aimBtnW = btnW;
        this.aimBtnH = fieldH;
        boolean aimHover = MathUtil.a(mouseX, mouseY, this.aimBtnX, this.aimBtnY, this.aimBtnW, this.aimBtnH);
        boolean canAim = friends.lookingAtPlayer() != null;
        draw.a(matrices, this.aimBtnX, this.aimBtnY, this.aimBtnW, this.aimBtnH, 7.0f,
                ColorUtil.convertToARGB(18, 22, 32, (int) ((aimHover ? 210 : 170) * alpha)));
        draw.a(matrices, this.aimBtnX, this.aimBtnY, this.aimBtnW, this.aimBtnH, 7.0f, 0.55f,
                ColorUtil.applyAlphaToColor(canAim ? primary : theme.a(ThemeInfo.OUTLINE_SMALL).toIntColor(), (canAim ? 0.7f : 0.35f) * alpha));
        Fonts.d.b(matrices, "Прицел", this.aimBtnX + this.aimBtnW / 2.0f, centerY(this.aimBtnY, this.aimBtnH, 6.2f), 6.2f,
                ColorUtil.applyAlphaToColor(canAim ? -1 : ColorUtil.convertToARGB(150, 160, 180, 255), alpha));

        this.addBtnX = this.aimBtnX + btnW + gap;
        this.addBtnY = cursorY;
        this.addBtnW = btnW;
        this.addBtnH = fieldH;
        boolean addHover = MathUtil.a(mouseX, mouseY, this.addBtnX, this.addBtnY, this.addBtnW, this.addBtnH);
        draw.a(matrices, this.addBtnX, this.addBtnY, this.addBtnW, this.addBtnH, 7.0f,
                ColorUtil.applyAlphaToColor(primary, (addHover ? 0.92f : 0.72f) * alpha));
        Fonts.d.b(matrices, "Добавить", this.addBtnX + this.addBtnW / 2.0f, centerY(this.addBtnY, this.addBtnH, 6.2f), 6.2f,
                ColorUtil.applyAlphaToColor(-1, alpha));
        cursorY += fieldH + 8.0f;

        float nearbyH = drawNearby(draw, matrices, friends, x + pad, cursorY, width - pad * 2.0f, mouseX, mouseY, alpha, primary, theme);
        if (nearbyH > 0.0f) {
            cursorY += nearbyH + 6.0f;
        }

        this.listTop = cursorY;
        this.listBottom = y + height - 8.0f;
        float listH = Math.max(0.0f, this.listBottom - this.listTop);
        float listX = x + pad;
        float listW = width - pad * 2.0f;

        List<FriendConstructor> list = friends.a();
        list.sort(Comparator
                .comparing((FriendConstructor friend) -> !friend.favorite())
                .thenComparing(friend -> !friends.isOnline(friend.a()))
                .thenComparing(FriendConstructor::a, String.CASE_INSENSITIVE_ORDER));

        float rowH = 44.0f;
        float contentH = list.isEmpty() ? 48.0f : list.size() * (rowH + 6.0f);
        float maxScroll = Math.max(0.0f, contentH - listH);
        this.targetScroll = MathUtil.b(this.targetScroll, 0.0f, maxScroll);
        this.scroll = MathUtil.c(this.scroll, this.targetScroll, delta * 0.28f);

        if (list.isEmpty()) {
            Fonts.c.b(matrices, "Добавьте друга по нику, из прицела или с сервера", listX + listW / 2.0f, this.listTop + 22.0f, 6.4f,
                    ColorUtil.applyAlphaToColor(theme.a(ThemeInfo.TEXT_DISABLED).toIntColor(), alpha));
            return;
        }

        PanelChrome.clipBegin(matrices, listX, this.listTop, listW, listH);
        float rowY = this.listTop - this.scroll;
        for (FriendConstructor friend : list) {
            if (rowY + rowH >= this.listTop && rowY <= this.listBottom) {
                boolean onlineNow = friends.isOnline(friend.a());
                boolean rowHover = MathUtil.a(mouseX, mouseY, listX, rowY, listW, rowH)
                        && mouseY >= this.listTop && mouseY <= this.listBottom;
                int cardBg = friend.favorite()
                        ? ColorUtil.convertToARGB(28, 18, 36, (int) ((rowHover ? 210 : 175) * alpha))
                        : ColorUtil.convertToARGB(14, 18, 26, (int) ((rowHover ? 200 : 155) * alpha));
                draw.a(matrices, listX, rowY, listW, rowH, 8.0f, cardBg);

                float avatar = 28.0f;
                float avatarX = listX + 10.0f;
                float avatarY = rowY + ((rowH - avatar) / 2.0f);
                PlayerListEntry entry = friends.getEntry(friend.a());
                if (entry != null) {
                    int skinId = MinecraftClient.getInstance().getTextureManager().getTexture(entry.getSkinTextures().texture()).getGlId();
                    draw.a(matrices, avatarX, avatarY, avatar, avatar, 6.0f,
                            ColorUtil.applyAlphaToColor(-1, alpha), 0.125f, 0.125f, 0.125f, 0.125f, skinId);
                } else {
                    draw.a(matrices, avatarX, avatarY, avatar, avatar, 6.0f, ColorUtil.convertToARGB(22, 26, 36, (int) (230 * alpha)));
                    Fonts.a.b(matrices, "L", avatarX + avatar / 2.0f, avatarY + 7.0f, 11.0f,
                            ColorUtil.applyAlphaToColor(theme.a(ThemeInfo.TEXT_DISABLED).toIntColor(), alpha));
                }
                draw.a(matrices, avatarX + avatar - 7.0f, avatarY + avatar - 7.0f, 7.0f, 7.0f, 3.5f,
                        ColorUtil.applyAlphaToColor(onlineNow ? ColorUtil.convertToARGB(80, 240, 140, 255) : ColorUtil.convertToARGB(90, 98, 112, 255), alpha));

                float textX = avatarX + avatar + 10.0f;
                String title = friend.a();
                if (friend.favorite()) {
                    title = "★  " + title;
                }
                Fonts.d.a(matrices, title, textX, rowY + 8.0f, 7.2f, ColorUtil.applyAlphaToColor(-1, alpha));

                String meta = metaLine(friends, friend);
                Fonts.c.a(matrices, meta, textX, rowY + 20.0f, 5.3f,
                        ColorUtil.applyAlphaToColor(onlineNow ? ColorUtil.convertToARGB(160, 230, 190, 255) : theme.a(ThemeInfo.TEXT_DISABLED).toIntColor(), alpha));

                PlayerEntity world = friends.getWorldPlayer(friend.a());
                if (world != null) {
                    float hp = Math.min(1.0f, (world.getHealth() + world.getAbsorptionAmount()) / Math.max(1.0f, world.getMaxHealth()));
                    float barW = 72.0f;
                    draw.a(matrices, textX, rowY + 30.0f, barW, 4.0f, 2.0f, ColorUtil.convertToARGB(20, 24, 32, (int) (200 * alpha)));
                    int hpColor = ColorUtil.lerpColor(ColorUtil.convertToARGB(230, 70, 80, 255), ColorUtil.convertToARGB(80, 230, 130, 255), hp);
                    draw.a(matrices, textX, rowY + 30.0f, barW * hp, 4.0f, 2.0f, ColorUtil.applyAlphaToColor(hpColor, alpha));
                } else if (!friend.note().isBlank()) {
                    Fonts.c.a(matrices, friend.note(), textX, rowY + 28.5f, 5.1f,
                            ColorUtil.applyAlphaToColor(primary, 0.85f * alpha));
                }

                float btn = 20.0f;
                float btnGap = 5.0f;
                float bx = listX + listW - 12.0f - btn;
                float by = rowY + (rowH - btn) / 2.0f;
                Row row = new Row();
                row.name = friend.a();
                row.remove = PanelChrome.icon(draw, matrices, bx, by, btn, "c", ColorUtil.convertToARGB(255, 80, 100, 255), mouseX, mouseY, alpha);
                bx -= btn + btnGap;
                row.copy = PanelChrome.icon(draw, matrices, bx, by, btn, "C", ColorUtil.convertToARGB(120, 150, 190, 255), mouseX, mouseY, alpha);
                bx -= btn + btnGap;
                row.msg = PanelChrome.icon(draw, matrices, bx, by, btn, "I", ColorUtil.convertToARGB(120, 190, 255, 255), mouseX, mouseY, alpha);
                bx -= btn + btnGap;
                row.star = PanelChrome.icon(draw, matrices, bx, by, btn, "\\",
                        friend.favorite() ? primary : ColorUtil.convertToARGB(170, 165, 180, 255), mouseX, mouseY, alpha);
                this.rows.add(row);
            }
            rowY += rowH + 6.0f;
        }
        PanelChrome.clipEnd(matrices);
        PanelChrome.scrollbar(draw, matrices, listX, listW, this.listTop, listH, contentH, this.scroll, maxScroll, alpha, primary);
    }

    private String metaLine(FriendProcessor friends, FriendConstructor friend) {
        boolean online = friends.isOnline(friend.a());
        StringBuilder line = new StringBuilder(online ? "Онлайн" : "Оффлайн");
        double dist = friends.distance(friend.a());
        if (dist >= 0.0d) {
            line.append("  ·  ").append(String.format(Locale.US, "%.1f м", dist));
        }
        int ping = friends.ping(friend.a());
        if (ping >= 0) {
            line.append("  ·  ").append(ping).append(" мс");
        }
        return line.toString();
    }

    private void pill(Draw2DProcessor draw, MatrixStack matrices, FriendProcessor friends, int index, String label, String id,
                      boolean on, float x, float y, float w, float h, double mouseX, double mouseY, float delta, float alpha, int primary) {
        AnimationUtil anim = this.toggleAnims[index];
        anim.a(on);
        anim.a(0.0f, 1.0f, 0.22f, EasingList.i, delta);
        float t = anim.c();
        boolean hover = MathUtil.a(mouseX, mouseY, x, y, w, h);
        int bg = ColorUtil.lerpColor(ColorUtil.convertToARGB(18, 22, 32, 220), ColorUtil.applyAlphaToColor(primary, 0.55f), t);
        draw.a(matrices, x, y, w, h, h / 2.0f, ColorUtil.applyAlphaToColor(bg, alpha));
        if (hover) {
            draw.a(matrices, x, y, w, h, h / 2.0f, 0.55f, ColorUtil.applyAlphaToColor(primary, 0.45f * alpha));
        }
        Fonts.c.b(matrices, label, x + w / 2.0f, centerY(y, h, 5.6f), 5.6f,
                ColorUtil.applyAlphaToColor(on ? -1 : ColorUtil.convertToARGB(170, 180, 200, 255), alpha));
        Chip chip = new Chip();
        chip.id = id;
        chip.x = x;
        chip.y = y;
        chip.w = w;
        chip.h = h;
        this.chips.add(chip);
    }

    private float drawNearby(Draw2DProcessor draw, MatrixStack matrices, FriendProcessor friends, float x, float y, float w,
                             double mouseX, double mouseY, float alpha, int primary, ThemeProcessor theme) {
        if (MinecraftClient.getInstance().world == null || MinecraftClient.getInstance().player == null) {
            return 0.0f;
        }
        List<? extends PlayerEntity> players = MinecraftClient.getInstance().world.getPlayers().stream()
                .filter(player -> player != MinecraftClient.getInstance().player && !friends.d(player.getName().getString()))
                .limit(5)
                .toList();
        if (players.isEmpty()) {
            return 0.0f;
        }
        Fonts.c.a(matrices, "Рядом на сервере — нажмите, чтобы добавить", x, y, 5.2f,
                ColorUtil.applyAlphaToColor(theme.a(ThemeInfo.TEXT_DISABLED).toIntColor(), alpha));
        float cx = x;
        float cy = y + 12.0f;
        for (PlayerEntity player : players) {
            String name = player.getName().getString();
            float chipW = Fonts.c.a(name, 5.5f) + 14.0f;
            if (cx + chipW > x + w) {
                break;
            }
            boolean hover = MathUtil.a(mouseX, mouseY, cx, cy, chipW, 16.0f);
            draw.a(matrices, cx, cy, chipW, 16.0f, 8.0f, ColorUtil.convertToARGB(18, 22, 32, (int) ((hover ? 220 : 160) * alpha)));
            draw.a(matrices, cx, cy, chipW, 16.0f, 8.0f, 0.5f, ColorUtil.applyAlphaToColor(primary, (hover ? 0.55f : 0.22f) * alpha));
            Fonts.c.a(matrices, name, cx + 7.0f, cy + 4.0f, 5.5f, ColorUtil.applyAlphaToColor(-1, alpha));
            Nearby item = new Nearby();
            item.name = name;
            item.x = cx;
            item.y = cy;
            item.w = chipW;
            item.h = 16.0f;
            this.nearby.add(item);
            cx += chipW + 6.0f;
        }
        return 30.0f;
    }

    private static float centerY(float y, float h, float fontSize) {
        return y + (h - fontSize) * 0.5f - 0.4f;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.addField.onMouseClick(mouseX, mouseY, button);
        if (button != 0) {
            return this.addField.isFocused();
        }
        FriendProcessor friends = Skeleton.getInstance().getModuleProcessor().e();
        for (Chip chip : this.chips) {
            if (MathUtil.a(mouseX, mouseY, chip.x, chip.y, chip.w, chip.h)) {
                switch (chip.id) {
                    case "Не бить" -> friends.a(!friends.g());
                    case "Glow" -> friends.highlight(!friends.highlight());
                    case "Push" -> friends.noPush(!friends.noPush());
                    case "ESP" -> friends.skipEsp(!friends.skipEsp());
                    default -> {
                    }
                }
                return true;
            }
        }
        if (MathUtil.a(mouseX, mouseY, this.aimBtnX, this.aimBtnY, this.aimBtnW, this.aimBtnH)) {
            PlayerEntity target = friends.lookingAtPlayer();
            if (target != null) {
                friends.b(target.getName().getString());
                friends.unSetup();
            }
            return true;
        }
        if (MathUtil.a(mouseX, mouseY, this.addBtnX, this.addBtnY, this.addBtnW, this.addBtnH)) {
            return addFriend();
        }
        for (Nearby item : this.nearby) {
            if (MathUtil.a(mouseX, mouseY, item.x, item.y, item.w, item.h)) {
                friends.b(item.name);
                friends.unSetup();
                return true;
            }
        }
        if (mouseY >= this.listTop && mouseY <= this.listBottom) {
            for (Row row : this.rows) {
                if (PanelChrome.inside(mouseX, mouseY, row.star)) {
                    FriendConstructor friend = friends.find(row.name);
                    if (friend != null) {
                        friend.favorite(!friend.favorite());
                        friends.unSetup();
                    }
                    return true;
                }
                if (PanelChrome.inside(mouseX, mouseY, row.msg)) {
                    MinecraftClient.getInstance().setScreen(new ChatScreen("/msg " + row.name + " "));
                    return true;
                }
                if (PanelChrome.inside(mouseX, mouseY, row.copy)) {
                    GLFW.glfwSetClipboardString(MinecraftClient.getInstance().getWindow().getHandle(), row.name);
                    return true;
                }
                if (PanelChrome.inside(mouseX, mouseY, row.remove)) {
                    friends.c(row.name);
                    friends.unSetup();
                    return true;
                }
            }
        }
        return this.addField.isFocused();
    }

    public void mouseDragged(double mouseX, double mouseY, int button) {
        this.addField.onMouseDrag(mouseX, mouseY, button);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (mouseY >= this.listTop && mouseY <= this.listBottom) {
            this.targetScroll -= (float) (amount * 22.0d);
            return true;
        }
        return false;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!this.addField.isFocused()) {
            return false;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            addFriend();
            return true;
        }
        this.addField.a(keyCode, scanCode, modifiers);
        return true;
    }

    public boolean charTyped(char character, int modifiers) {
        if (!this.addField.isFocused()) {
            return false;
        }
        this.addField.a(character, modifiers);
        return true;
    }

    public boolean isAddFocused() {
        return this.addField.isFocused();
    }

    public void unfocus() {
        this.addField.a(false);
    }

    private boolean addFriend() {
        String name = this.addField.getTextBuffer().toString().trim();
        if (name.isEmpty()) {
            this.addField.a(true);
            return true;
        }
        FriendProcessor friends = Skeleton.getInstance().getModuleProcessor().e();
        friends.b(name);
        friends.unSetup();
        this.addField.a();
        this.addField.a(true);
        return true;
    }

    private static final class Chip {
        private String id;
        private float x, y, w, h;
    }

    private static final class Nearby {
        private String name;
        private float x, y, w, h;
    }

    private static final class Row {
        private String name;
        private PanelChrome.Hit star;
        private PanelChrome.Hit msg;
        private PanelChrome.Hit copy;
        private PanelChrome.Hit remove;
    }
}
