package aethereal.ui.screen;

import aethereal.config.ThemeInfo;
import aethereal.core.Interface;
import aethereal.core.Skeleton;
import aethereal.network.AccountConstructor;
import aethereal.render.AnimationUtil;
import aethereal.render.ColorUtil;
import aethereal.render.Draw2DProcessor;
import aethereal.render.EasingList;
import aethereal.render.Fonts;
import aethereal.render.ScaleUtil;
import aethereal.render.ScissorUtil;
import aethereal.ui.element.TextField;
import aethereal.ui.shader.GradientUtil;
import aethereal.util.MathUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector2f;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class AltScreen extends Screen {
    private static final float PANEL_W = 248.0f;
    private static final float PANEL_H = 268.0f;
    private static final float ROW_H = 30.0f;

    private final AnimationUtil openAnimation;
    private final AnimationUtil scroll;
    private final TextField nameField;
    private final List<Row> rows;
    private Row hovered;
    private Row dragging;
    private AccountConstructor lastSelected;
    private float dragGrab;
    private boolean moved;
    private float addX;
    private float addY;
    private float addW;
    private float randomX;
    private float randomW;
    private float deleteX;
    private float deleteY;
    private float deleteW;
    private float panelX;
    private float panelY;

    public AltScreen() {
        super(Text.empty());
        this.openAnimation = new AnimationUtil();
        this.scroll = new AnimationUtil();
        this.nameField = new TextField(TextField.type.ALT_MANAGER);
        this.rows = new ArrayList<>();
        this.nameField.setPlaceholder("Никнейм");
        accounts().forEach(account -> this.rows.add(new Row(account)));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        this.openAnimation.a(Interface.mc.currentScreen instanceof AltScreen);
        this.openAnimation.a(0.0f, 1.0f, 0.16f, EasingList.g, delta);
        float open = Math.min(1.0f, this.openAnimation.c() / 0.9f);
        float eased = EasingList.s.ease(open);
        double mx = MathUtil.scale(mouseX, 2);
        double my = MathUtil.scale(mouseY, 2);
        ScaleUtil.a(context, 2);

        int width = Interface.mc.getWindow().getScaledWidth();
        int height = Interface.mc.getWindow().getScaledHeight();
        MainScreen.a(context, width, height, (int) mx, (int) my, 1.08f + (eased * 0.08f));

        Draw2DProcessor draw = Skeleton.getInstance().getModuleProcessor().i();
        if (draw.e() != null) {
            draw.e().a(context.getMatrices());
        }
        int primary = Skeleton.getInstance().getModuleProcessor().o().a(ThemeInfo.PRIMARY).toIntColor();
        MatrixStack matrices = context.getMatrices();
        draw.a(matrices, 0.0f, 0.0f, width, height, 0.0f,
                ColorUtil.convertToARGB(18, 6, 22, (int) (140.0f * open)),
                ColorUtil.convertToARGB(10, 4, 18, (int) (80.0f * open)),
                ColorUtil.convertToARGB(20, 6, 26, (int) (160.0f * open)),
                ColorUtil.convertToARGB(8, 4, 16, (int) (110.0f * open)));

        this.panelX = (width - PANEL_W) * 0.5f;
        this.panelY = (height - PANEL_H) * 0.5f + 8.0f;
        float scale = 0.90f + (0.10f * eased);
        matrices.push();
        matrices.translate(width * 0.5f, height * 0.5f, 0.0f);
        matrices.scale(scale, scale, 1.0f);
        matrices.translate((-width) * 0.5f, (-height) * 0.5f, 0.0f);

        draw.a(matrices, this.panelX, this.panelY, PANEL_W, PANEL_H, 14.0f, ColorUtil.convertToARGB(14, 8, 20, (int) (175.0f * open)), open, ColorUtil.applyAlphaToColor(primary, 0.28f * open), 16.0f);
        draw.a(matrices, this.panelX, this.panelY, PANEL_W, PANEL_H, 14.0f, ColorUtil.convertToARGB(16, 8, 22, (int) (160.0f * open)));
        draw.a(matrices, this.panelX, this.panelY, PANEL_W, PANEL_H, 14.0f, 0.75f, ColorUtil.applyAlphaToColor(primary, 0.4f * open));

        Fonts.d.a(matrices, GradientUtil.a("Аккаунты", primary, 4.8f, 0.4f), this.panelX + 16.0f, this.panelY + 12.0f, 11.0f, 0.0f, open);
        AccountConstructor selected = Skeleton.getInstance().getModuleProcessor().h().a();
        String subtitle = (selected != null ? selected.b() : "Не выбран") + "  ·  " + accounts().size();
        Fonts.c.a(matrices, subtitle, this.panelX + 16.0f, this.panelY + 26.0f, 5.6f, ColorUtil.applyAlphaToColor(ColorUtil.convertToARGB(200, 170, 215, 255), open));
        Fonts.c.a(matrices, "ESC назад", this.panelX + PANEL_W - 16.0f - Fonts.c.a("ESC назад", 5.4f), this.panelY + 14.0f, 5.4f, ColorUtil.applyAlphaToColor(primary, 0.8f * open));

        renderList(matrices, draw, primary, (int) mx, (int) my, open, width, height, scale);
        renderFooter(context, draw, primary, (int) mx, (int) my, delta, open);
        matrices.pop();
        ScaleUtil.a(context);
    }

    private void renderList(MatrixStack matrices, Draw2DProcessor draw, int primary, int mx, int my, float open, int width, int height, float scale) {
        float listTop = this.panelY + 42.0f;
        float listBottom = this.panelY + 208.0f;
        float listH = listBottom - listTop;
        float overflow = Math.min(0.0f, listH - (this.rows.size() * ROW_H));
        float offset = this.scroll.a(overflow, 0.0f, 0.5f);
        boolean drag = this.dragging != null && this.moved;
        float baseY = listTop + offset;
        List<Row> visual = this.rows.stream().sorted(Comparator.comparing(row -> Boolean.valueOf(!row.account.d()))).collect(Collectors.toCollection(ArrayList::new));
        if (drag) {
            reorder(visual, (my - this.dragGrab) - baseY);
        }
        this.hovered = null;
        float selectedSlot = -1.0f;
        int index = 0;
        ScissorUtil.a(matrices, (width / 2.0f) + ((this.panelX - (width / 2.0f)) * scale), (height / 2.0f) + (((listTop - 2.0f) - (height / 2.0f)) * scale), PANEL_W * scale, (listH + 2.0f) * scale);
        AccountConstructor selected = Skeleton.getInstance().getModuleProcessor().h().a();
        for (Row row : visual) {
            float target = row.removing ? row.slot : index * ROW_H;
            if (row != this.dragging || !drag) {
                row.render(matrices, draw, primary, this.panelX, baseY, target, listTop, listBottom, mx, my, open);
            }
            if (row.account == selected) {
                selectedSlot = target;
            }
            if (!row.removing) {
                index++;
            }
        }
        if (drag) {
            this.dragging.slot = (my - this.dragGrab) - baseY;
            this.dragging.render(matrices, draw, primary, this.panelX, baseY, this.dragging.slot, listTop, listBottom, mx, my, open);
        }
        ScissorUtil.a(matrices);
        this.rows.removeIf(Row::expired);
        if (selected != this.lastSelected) {
            this.lastSelected = selected;
            if (selectedSlot >= 0.0f) {
                float top = selectedSlot + offset;
                float desired = top < 0.0f ? -selectedSlot : top + ROW_H > listH ? (listH - ROW_H) - selectedSlot : offset;
                this.scroll.a(MathUtil.b(desired, overflow, 0.0f) - offset);
            }
        }
        float content = Math.max(listH, this.rows.size() * ROW_H);
        float thumb = (listH * listH) / content;
        draw.a(matrices, this.panelX + PANEL_W - 9.0f, listTop, 1.6f, listH, 0.8f, ColorUtil.convertToARGB(255, 255, 255, (int) (16.0f * open)));
        draw.a(matrices, this.panelX + PANEL_W - 9.0f, listTop - ((offset / Math.max(1.0f, content - listH)) * (listH - thumb)), 1.6f, thumb, 0.8f, ColorUtil.applyAlphaToColor(primary, open));
    }

    private void renderFooter(DrawContext context, Draw2DProcessor draw, int primary, int mx, int my, float delta, float open) {
        MatrixStack matrices = context.getMatrices();
        float fieldY = this.panelY + 216.0f;
        this.randomW = Fonts.a.a("H", 8.0f) + 4.0f + Fonts.d.a("Рандом", 6.5f) + 12.0f;
        this.addW = 22.0f;
        float fieldW = PANEL_W - 32.0f - this.addW - 6.0f - this.randomW;
        this.nameField.setPosition(new Vector2f(this.panelX + 14.0f, fieldY));
        this.nameField.setSize(new Vector2f(fieldW, 20.0f));
        this.nameField.render(context, mx, my, delta, open);
        this.addX = this.panelX + 16.0f + fieldW;
        this.addY = fieldY;
        this.randomX = this.addX + this.addW + 5.0f;
        this.deleteX = this.panelX + 14.0f;
        this.deleteY = fieldY + 26.0f;
        this.deleteW = PANEL_W - 28.0f;

        boolean addHover = MathUtil.a(mx, my, this.addX, this.addY, this.addW, 20.0f);
        boolean randomHover = MathUtil.a(mx, my, this.randomX, this.addY, this.randomW, 20.0f);
        boolean deleteHover = MathUtil.a(mx, my, this.deleteX, this.deleteY, this.deleteW, 18.0f);

        draw.a(matrices, this.addX, this.addY, this.addW, 20.0f, 6.0f, ColorUtil.applyAlphaToColor(primary, (addHover ? 0.28f : 0.16f) * open));
        draw.a(matrices, this.addX, this.addY, this.addW, 20.0f, 6.0f, 0.6f, ColorUtil.applyAlphaToColor(primary, open));
        Fonts.d.b(matrices, "+", this.addX + (this.addW / 2.0f), this.addY + 4.0f, 9.0f, ColorUtil.applyAlphaToColor(-1, open));

        draw.a(matrices, this.randomX, this.addY, this.randomW, 20.0f, 6.0f, ColorUtil.convertToARGB(255, 255, 255, (int) ((randomHover ? 18 : 10) * open)));
        draw.a(matrices, this.randomX, this.addY, this.randomW, 20.0f, 6.0f, 0.55f, ColorUtil.applyAlphaToColor(primary, (randomHover ? 0.7f : 0.35f) * open));
        Fonts.a.a(matrices, "H", this.randomX + 7.0f, this.addY + 5.0f, 8.0f, ColorUtil.applyAlphaToColor(primary, open));
        Fonts.d.a(matrices, "Рандом", this.randomX + 18.0f, this.addY + 5.5f, 6.5f, ColorUtil.applyAlphaToColor(-1, open));

        int delete = ColorUtil.convertToARGB(230, 80, 120, 255);
        draw.a(matrices, this.deleteX, this.deleteY, this.deleteW, 18.0f, 6.0f, ColorUtil.applyAlphaToColor(delete, (deleteHover ? 0.22f : 0.10f) * open));
        draw.a(matrices, this.deleteX, this.deleteY, this.deleteW, 18.0f, 6.0f, 0.55f, ColorUtil.applyAlphaToColor(delete, open));
        Fonts.d.b(matrices, "Удалить все", this.deleteX + (this.deleteW / 2.0f), this.deleteY + 4.5f, 6.4f, ColorUtil.applyAlphaToColor(delete, open));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double mx = MathUtil.scale(mouseX, 2);
        double my = MathUtil.scale(mouseY, 2);
        this.nameField.onMouseClick(mx, my, button);
        if (button != 0 && button != 1) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (button == 0 && MathUtil.a(mx, my, this.addX, this.addY, this.addW, 20.0f)) {
            addAccount(this.nameField.getTextBuffer().toString());
            return true;
        }
        if (button == 0 && MathUtil.a(mx, my, this.randomX, this.addY, this.randomW, 20.0f)) {
            addAccount(randomName());
            return true;
        }
        if (button == 0 && MathUtil.a(mx, my, this.deleteX, this.deleteY, this.deleteW, 18.0f)) {
            this.rows.forEach(row -> row.removing = true);
            accounts().clear();
            Skeleton.getInstance().getModuleProcessor().h().save();
            return true;
        }
        if (this.hovered == null) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (button == 1) {
            removeRow(this.hovered);
            return true;
        }
        this.dragging = this.hovered;
        this.dragGrab = ((float) my) - this.hovered.y;
        this.moved = false;
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        this.nameField.onMouseDrag(MathUtil.scale(mouseX, 2), MathUtil.scale(mouseY, 2), button);
        if (this.dragging == null) {
            return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        }
        double my = MathUtil.scale(mouseY, 2) - this.dragGrab;
        if (Math.abs(my - this.dragging.y) > 8.0d) {
            this.moved = true;
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.dragging == null) {
            return super.mouseReleased(mouseX, mouseY, button);
        }
        if (!this.moved) {
            if (this.dragging.starHovered) {
                this.dragging.account.b(!this.dragging.account.d());
                Skeleton.getInstance().getModuleProcessor().h().save();
            } else {
                select(this.dragging.account);
            }
        } else {
            commitOrder();
        }
        this.dragging = null;
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        this.scroll.a(((float) vertical) * 29.0f);
        return true;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (!this.nameField.isFocused()) {
            return super.charTyped(chr, modifiers);
        }
        this.nameField.a(chr, modifiers);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if ((modifiers & 2) != 0 && keyCode == 86) {
            paste();
            return true;
        }
        if (!this.nameField.isFocused()) {
            if (keyCode == 256) {
                Interface.mc.setScreen(new MainScreen());
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        if (keyCode == 257) {
            addAccount(this.nameField.getTextBuffer().toString());
            return true;
        }
        this.nameField.a(keyCode, scanCode, modifiers);
        return true;
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    private List<AccountConstructor> accounts() {
        return Skeleton.getInstance().getModuleProcessor().h().e();
    }

    private void select(AccountConstructor account) {
        accounts().forEach(other -> other.a(other == account));
        Skeleton.getInstance().getModuleProcessor().h().save();
    }

    private void addAccount(String raw) {
        String name = raw == null ? "" : raw.trim();
        if (name.isEmpty() || accounts().stream().anyMatch(other -> other.b().equalsIgnoreCase(name))) {
            return;
        }
        AccountConstructor account = new AccountConstructor(name);
        select(account);
        accounts().add(account);
        this.rows.add(new Row(account));
        this.nameField.a();
        Skeleton.getInstance().getModuleProcessor().h().save();
    }

    private void removeRow(Row row) {
        row.removing = true;
        boolean wasSelected = row.account.c();
        accounts().remove(row.account);
        if (wasSelected) {
            select(accounts().stream().findFirst().orElse(null));
        }
        Skeleton.getInstance().getModuleProcessor().h().save();
    }

    private void reorder(List<Row> visual, float draggedY) {
        int from = visual.indexOf(this.dragging);
        int favorites = (int) visual.stream().filter(row -> row.account.d()).count();
        int lo = this.dragging.account.d() ? 0 : favorites;
        int hi = this.dragging.account.d() ? favorites - 1 : visual.size() - 1;
        int to = Math.max(lo, Math.min(hi, Math.round(draggedY / ROW_H)));
        if (from < 0 || from == to) {
            return;
        }
        visual.remove(from);
        visual.add(to, this.dragging);
        this.rows.clear();
        this.rows.addAll(visual);
    }

    private void commitOrder() {
        List<AccountConstructor> list = accounts();
        List<AccountConstructor> ordered = this.rows.stream().map(row -> row.account).filter(list::contains).toList();
        list.clear();
        list.addAll(ordered);
        Skeleton.getInstance().getModuleProcessor().h().save();
    }

    private void paste() {
        String clip = GLFW.glfwGetClipboardString(Interface.mc.getWindow().getHandle());
        if (clip == null) {
            return;
        }
        String name = clip.replaceAll("[^a-zA-Z0-9_]", "");
        addAccount(name.substring(0, Math.min(16, name.length())));
    }

    private String randomName() {
        StringBuilder name = new StringBuilder();
        int syllables = 2 + ((int) (Math.random() * 3.0d));
        for (int i = 0; i < syllables; i++) {
            char cons = "bcdfghjklmnpqrstvwz".charAt((int) (Math.random() * 19.0d));
            name.append(cons);
            if (Math.random() < 0.12d) {
                name.append(cons);
            }
            char vowel = "aeiouy".charAt((int) (Math.random() * 6.0d));
            name.append(vowel);
            if (Math.random() < 0.1d) {
                name.append(vowel);
            }
        }
        if (Math.random() < 0.3d) {
            name.setCharAt(0, Character.toUpperCase(name.charAt(0)));
        }
        if (Math.random() < 0.15d) {
            name.append('_');
        }
        if (Math.random() < 0.25d) {
            int digits = 1 + ((int) (Math.random() * 3.0d));
            for (int i = 0; i < digits; i++) {
                name.append((char) (48 + ((int) (Math.random() * 10.0d))));
            }
        }
        if (name.length() < 5) {
            return randomName();
        }
        return name.length() > 16 ? name.substring(0, 16) : name.toString();
    }

    private final class Row {
        private final AccountConstructor account;
        private final float[] anim = new float[4];
        private float slot = Float.NaN;
        private float y;
        private boolean starHovered;
        private boolean removing;

        private Row(AccountConstructor account) {
            this.account = account;
        }

        private boolean expired() {
            return this.removing && this.anim[3] < 0.01f;
        }

        private void render(MatrixStack matrices, Draw2DProcessor draw, int primary, float panelX, float baseY, float targetSlot, float listTop, float listBottom, int mx, int my, float open) {
            this.slot = Float.isNaN(this.slot) ? targetSlot : MathUtil.c(this.slot, targetSlot, 1.4f);
            this.y = baseY + this.slot;
            float x = panelX + 12.0f;
            float w = PANEL_W - 32.0f;
            boolean over = !this.removing && my > listTop && my < listBottom && MathUtil.a(mx, my, x, this.y, w, 26.0f);
            this.starHovered = over && mx > x + w - 22.0f;
            if (over) {
                AltScreen.this.hovered = this;
            }
            float[] target = {over ? 1.0f : 0.0f, this.account.c() ? 1.0f : 0.0f, this.account.d() ? 1.0f : 0.0f, this.removing ? 0.0f : 1.0f};
            for (int i = 0; i < 4; i++) {
                this.anim[i] = MathUtil.c(this.anim[i], target[i], 1.5f);
            }
            float hover = this.anim[0];
            float select = this.anim[1];
            float fav = this.anim[2];
            float alpha = open * this.anim[3];
            if (this.y + 26.0f < listTop - 2.0f || this.y > listBottom + 2.0f) {
                return;
            }
            draw.a(matrices, x, this.y, w, 26.0f, 7.0f, ColorUtil.lerpColor(
                    ColorUtil.convertToARGB(255, 255, 255, (int) (8.0f * hover * alpha)),
                    ColorUtil.applyAlphaToColor(primary, 0.16f * select * alpha),
                    select));
            draw.a(matrices, x, this.y, w, 26.0f, 7.0f, 0.6f, ColorUtil.lerpColor(
                    ColorUtil.convertToARGB(255, 255, 255, (int) (10.0f * alpha)),
                    ColorUtil.applyAlphaToColor(primary, 0.7f * alpha),
                    Math.max(select, fav * 0.5f)));
            draw.a(matrices, this.account.a(), null, x + 6.0f, this.y + 4.5f, 17.0f, 17.0f, 4.0f, alpha);
            Fonts.d.a(matrices, this.account.b(), x + 28.0f, this.y + 8.0f, 7.4f, ColorUtil.applyAlphaToColor(-1, alpha));
            if (select > 0.05f) {
                Fonts.c.a(matrices, "выбран", x + 28.0f + Fonts.d.a(this.account.b(), 7.4f) + 6.0f, this.y + 9.5f, 5.2f, ColorUtil.applyAlphaToColor(primary, select * alpha));
            }
            if (fav > 0.02f || hover > 0.02f) {
                Fonts.a.a(matrices, "\\", x + w - 16.0f, this.y + 7.5f, 9.0f, ColorUtil.lerpColor(
                        ColorUtil.convertToARGB(255, 255, 255, (int) ((this.starHovered ? 200 : 50) * hover * alpha)),
                        ColorUtil.convertToARGB(255, 196, 80, (int) (255.0f * alpha)),
                        fav));
            }
        }
    }
}
