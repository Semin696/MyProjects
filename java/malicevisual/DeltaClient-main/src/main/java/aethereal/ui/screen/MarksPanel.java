package aethereal.ui.screen;

import aethereal.command.GPSCommand;
import aethereal.config.ThemeInfo;
import aethereal.config.ThemeProcessor;
import aethereal.core.Skeleton;
import aethereal.mark.MarkConstructor;
import aethereal.mark.MarksProcessor;
import aethereal.render.ColorUtil;
import aethereal.render.Draw2DProcessor;
import aethereal.render.Fonts;
import aethereal.ui.element.TextField;
import aethereal.util.MathUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector2f;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class MarksPanel {
    private final TextField nameField = new TextField(TextField.type.GUI);
    private final TextField xField = new TextField(TextField.type.GUI, true);
    private final TextField yField = new TextField(TextField.type.GUI, true);
    private final TextField zField = new TextField(TextField.type.GUI, true);
    private final List<Row> rows = new ArrayList<>();
    private float targetScroll;
    private float scroll;
    private float listTop;
    private float listBottom;
    private float addX, addY, addW, addH;
    private float hereX, hereY, hereW, hereH;
    private String renaming;

    public MarksPanel() {
        this.nameField.setPlaceholder("Название метки...");
        this.xField.setPlaceholder("X");
        this.yField.setPlaceholder("Y");
        this.zField.setPlaceholder("Z");
    }

    public void render(DrawContext context, float x, float y, float width, float height, double mouseX, double mouseY, float delta, float alpha) {
        MatrixStack matrices = context.getMatrices();
        Draw2DProcessor draw = Skeleton.getInstance().getModuleProcessor().i();
        ThemeProcessor theme = Skeleton.getInstance().getModuleProcessor().o();
        MarksProcessor marks = Skeleton.getInstance().getModuleProcessor().getMarks();
        int primary = theme.a(ThemeInfo.PRIMARY).toIntColor();
        this.rows.clear();

        float pad = 10.0f;
        float gap = 6.0f;
        float innerW = width - pad * 2.0f;
        float innerX = x + pad;
        float cursorY = y + 8.0f;

        float statsH = 28.0f;
        draw.a(matrices, innerX, cursorY, innerW, statsH, 8.0f, ColorUtil.convertToARGB(16, 12, 26, (int) (170 * alpha)));
        Fonts.d.a(matrices, "Метки", innerX + 12.0f, cursorY + 6.0f, 7.2f, ColorUtil.applyAlphaToColor(-1, alpha));
        Fonts.c.a(matrices, marks.a().size() + " точек  ·  сейчас " + marks.currentCoords(), innerX + 12.0f, cursorY + 16.5f, 5.3f,
                ColorUtil.applyAlphaToColor(theme.a(ThemeInfo.TEXT_DISABLED).toIntColor(), alpha));
        cursorY += statsH + 8.0f;

        float fieldH = 24.0f;
        float btnW = 88.0f;
        float fieldW = innerW - btnW - gap;
        this.nameField.setPosition(new Vector2f(innerX, cursorY));
        this.nameField.setSize(new Vector2f(fieldW, fieldH));
        this.nameField.render(context, mouseX, mouseY, delta, alpha);

        this.addX = innerX + fieldW + gap;
        this.addY = cursorY;
        this.addW = btnW;
        this.addH = fieldH;
        boolean addHover = MathUtil.a(mouseX, mouseY, this.addX, this.addY, this.addW, this.addH);
        String action = this.renaming != null ? "Сохранить" : "Добавить";
        draw.a(matrices, this.addX, this.addY, this.addW, this.addH, 7.0f,
                ColorUtil.applyAlphaToColor(primary, (addHover ? 0.92f : 0.72f) * alpha));
        Fonts.d.b(matrices, action, this.addX + this.addW / 2.0f, centerY(this.addY, this.addH, 6.2f), 6.2f,
                ColorUtil.applyAlphaToColor(-1, alpha));
        cursorY += fieldH + 6.0f;

        this.hereW = 72.0f;
        this.hereH = fieldH;
        float coordW = (innerW - this.hereW - gap * 3.0f) / 3.0f;
        this.xField.setPosition(new Vector2f(innerX, cursorY));
        this.xField.setSize(new Vector2f(coordW, fieldH));
        this.xField.render(context, mouseX, mouseY, delta, alpha);
        this.yField.setPosition(new Vector2f(innerX + coordW + gap, cursorY));
        this.yField.setSize(new Vector2f(coordW, fieldH));
        this.yField.render(context, mouseX, mouseY, delta, alpha);
        this.zField.setPosition(new Vector2f(innerX + (coordW + gap) * 2.0f, cursorY));
        this.zField.setSize(new Vector2f(coordW, fieldH));
        this.zField.render(context, mouseX, mouseY, delta, alpha);

        this.hereX = innerX + innerW - this.hereW;
        this.hereY = cursorY;
        boolean hereHover = MathUtil.a(mouseX, mouseY, this.hereX, this.hereY, this.hereW, this.hereH);
        boolean canPlace = MinecraftClient.getInstance().player != null;
        draw.a(matrices, this.hereX, this.hereY, this.hereW, this.hereH, 7.0f,
                ColorUtil.convertToARGB(18, 22, 32, (int) ((hereHover ? 210 : 170) * alpha)));
        draw.a(matrices, this.hereX, this.hereY, this.hereW, this.hereH, 7.0f, 0.55f,
                ColorUtil.applyAlphaToColor(canPlace ? primary : theme.a(ThemeInfo.OUTLINE_SMALL).toIntColor(), (canPlace ? 0.7f : 0.35f) * alpha));
        Fonts.d.b(matrices, "Здесь", this.hereX + this.hereW / 2.0f, centerY(this.hereY, this.hereH, 6.2f), 6.2f,
                ColorUtil.applyAlphaToColor(canPlace ? -1 : ColorUtil.convertToARGB(150, 160, 180, 255), alpha));
        cursorY += fieldH + 6.0f;

        Fonts.c.a(matrices, this.renaming != null
                        ? "Измените имя или координаты «" + this.renaming + "»"
                        : "Имя и координаты, либо «Здесь» — текущая позиция",
                innerX, cursorY, 5.2f,
                ColorUtil.applyAlphaToColor(theme.a(ThemeInfo.TEXT_DISABLED).toIntColor(), alpha));
        cursorY += 14.0f;

        this.listTop = cursorY;
        this.listBottom = y + height - 8.0f;
        float listH = Math.max(0.0f, this.listBottom - this.listTop);
        float listX = innerX;
        float listW = innerW;

        List<MarkConstructor> list = marks.a();
        list.sort(Comparator.comparing(MarkConstructor::name, String.CASE_INSENSITIVE_ORDER));

        float rowH = 46.0f;
        float contentH = list.isEmpty() ? 48.0f : list.size() * (rowH + 6.0f);
        float maxScroll = Math.max(0.0f, contentH - listH);
        this.targetScroll = MathUtil.b(this.targetScroll, 0.0f, maxScroll);
        this.scroll = MathUtil.c(this.scroll, this.targetScroll, delta * 0.28f);

        if (list.isEmpty()) {
            Fonts.c.b(matrices, "Пока нет меток — укажите координаты или нажмите «Здесь»", listX + listW / 2.0f, this.listTop + 22.0f, 6.4f,
                    ColorUtil.applyAlphaToColor(theme.a(ThemeInfo.TEXT_DISABLED).toIntColor(), alpha));
            return;
        }

        PanelChrome.clipBegin(matrices, listX, this.listTop, listW, listH);
        float rowY = this.listTop - this.scroll;
        for (MarkConstructor mark : list) {
            if (rowY + rowH >= this.listTop && rowY <= this.listBottom) {
                boolean editing = this.renaming != null && this.renaming.equalsIgnoreCase(mark.name());
                boolean rowHover = MathUtil.a(mouseX, mouseY, listX, rowY, listW, rowH)
                        && mouseY >= this.listTop && mouseY <= this.listBottom;
                int cardBg = editing
                        ? ColorUtil.convertToARGB(28, 18, 36, (int) ((rowHover ? 210 : 175) * alpha))
                        : ColorUtil.convertToARGB(14, 18, 26, (int) ((rowHover ? 200 : 155) * alpha));
                draw.a(matrices, listX, rowY, listW, rowH, 8.0f, cardBg);

                Fonts.a.a(matrices, "G", listX + 12.0f, rowY + 15.0f, 11.0f, ColorUtil.applyAlphaToColor(primary, alpha));
                Fonts.d.a(matrices, mark.name(), listX + 32.0f, rowY + 8.0f, 7.2f, ColorUtil.applyAlphaToColor(-1, alpha));

                String dim = MarksProcessor.dimensionLabel(mark.dimension());
                double dist = marks.distance(mark);
                StringBuilder meta = new StringBuilder(mark.coords());
                if (!dim.isEmpty()) {
                    meta.append("  ·  ").append(dim);
                }
                if (dist >= 0.0d) {
                    meta.append("  ·  ").append(String.format(Locale.US, "%.0f м", dist));
                }
                Fonts.c.a(matrices, meta.toString(), listX + 32.0f, rowY + 24.0f, 5.3f,
                        ColorUtil.applyAlphaToColor(theme.a(ThemeInfo.TEXT_DISABLED).toIntColor(), alpha));

                float btn = 20.0f;
                float btnGap = 5.0f;
                float bx = listX + listW - 12.0f - btn;
                float by = rowY + (rowH - btn) / 2.0f;
                Row row = new Row();
                row.name = mark.name();
                row.remove = PanelChrome.icon(draw, matrices, bx, by, btn, "c", ColorUtil.convertToARGB(255, 80, 100, 255), mouseX, mouseY, alpha);
                bx -= btn + btnGap;
                row.copy = PanelChrome.icon(draw, matrices, bx, by, btn, "C", ColorUtil.convertToARGB(120, 150, 190, 255), mouseX, mouseY, alpha);
                bx -= btn + btnGap;
                row.gps = PanelChrome.icon(draw, matrices, bx, by, btn, "F", ColorUtil.convertToARGB(120, 190, 255, 255), mouseX, mouseY, alpha);
                bx -= btn + btnGap;
                row.rename = PanelChrome.icon(draw, matrices, bx, by, btn, "t", editing ? primary : ColorUtil.convertToARGB(170, 165, 180, 255), mouseX, mouseY, alpha);
                this.rows.add(row);
            }
            rowY += rowH + 6.0f;
        }
        PanelChrome.clipEnd(matrices);
        PanelChrome.scrollbar(draw, matrices, listX, listW, this.listTop, listH, contentH, this.scroll, maxScroll, alpha, primary);
    }

    private static float centerY(float y, float h, float fontSize) {
        return y + (h - fontSize) * 0.5f - 0.4f;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        handleFieldClicks(mouseX, mouseY, button);
        if (button != 0) {
            return isAddFocused();
        }
        MarksProcessor marks = Skeleton.getInstance().getModuleProcessor().getMarks();
        if (MathUtil.a(mouseX, mouseY, this.addX, this.addY, this.addW, this.addH)) {
            return confirm();
        }
        if (MathUtil.a(mouseX, mouseY, this.hereX, this.hereY, this.hereW, this.hereH)) {
            return fillHere();
        }
        if (mouseY >= this.listTop && mouseY <= this.listBottom) {
            for (Row row : this.rows) {
                if (PanelChrome.inside(mouseX, mouseY, row.rename)) {
                    startRename(row.name);
                    return true;
                }
                if (PanelChrome.inside(mouseX, mouseY, row.gps)) {
                    MarkConstructor mark = marks.find(row.name);
                    if (mark != null) {
                        GPSCommand gps = Skeleton.getInstance().getModuleProcessor().u().d();
                        if (gps != null) {
                            gps.a(mark.pos(), mark.name());
                        }
                    }
                    return true;
                }
                if (PanelChrome.inside(mouseX, mouseY, row.copy)) {
                    MarkConstructor mark = marks.find(row.name);
                    if (mark != null) {
                        GLFW.glfwSetClipboardString(MinecraftClient.getInstance().getWindow().getHandle(), mark.coords().replace("  ", " "));
                    }
                    return true;
                }
                if (PanelChrome.inside(mouseX, mouseY, row.remove)) {
                    marks.remove(row.name);
                    if (this.renaming != null && this.renaming.equalsIgnoreCase(row.name)) {
                        this.renaming = null;
                        clearFields();
                    }
                    return true;
                }
            }
        }
        return isAddFocused();
    }

    public void mouseDragged(double mouseX, double mouseY, int button) {
        for (TextField field : fields()) {
            field.onMouseDrag(mouseX, mouseY, button);
        }
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (mouseY >= this.listTop && mouseY <= this.listBottom) {
            this.targetScroll -= (float) (amount * 22.0d);
            return true;
        }
        return false;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!isAddFocused()) {
            return false;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            confirm();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && this.renaming != null) {
            this.renaming = null;
            clearFields();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_TAB) {
            cycleFocus((modifiers & GLFW.GLFW_MOD_SHIFT) != 0);
            return true;
        }
        for (TextField field : fields()) {
            if (field.isFocused()) {
                field.a(keyCode, scanCode, modifiers);
                return true;
            }
        }
        return true;
    }

    public boolean charTyped(char character, int modifiers) {
        for (TextField field : fields()) {
            if (field.isFocused()) {
                field.a(character, modifiers);
                return true;
            }
        }
        return false;
    }

    public boolean isAddFocused() {
        for (TextField field : fields()) {
            if (field.isFocused()) {
                return true;
            }
        }
        return false;
    }

    public void unfocus() {
        for (TextField field : fields()) {
            field.a(false);
        }
    }

    private void startRename(String name) {
        this.renaming = name;
        write(this.nameField, name);
        MarkConstructor mark = Skeleton.getInstance().getModuleProcessor().getMarks().find(name);
        if (mark != null) {
            write(this.xField, String.valueOf((int) Math.floor(mark.x())));
            write(this.yField, String.valueOf((int) Math.floor(mark.y())));
            write(this.zField, String.valueOf((int) Math.floor(mark.z())));
        }
        this.nameField.a(true);
        this.xField.a(false);
        this.yField.a(false);
        this.zField.a(false);
    }

    private boolean fillHere() {
        if (MinecraftClient.getInstance().player == null) {
            return true;
        }
        Vec3d pos = MinecraftClient.getInstance().player.getPos();
        write(this.xField, String.valueOf((int) Math.floor(pos.x)));
        write(this.yField, String.valueOf((int) Math.floor(pos.y)));
        write(this.zField, String.valueOf((int) Math.floor(pos.z)));
        return true;
    }

    private boolean confirm() {
        MarksProcessor marks = Skeleton.getInstance().getModuleProcessor().getMarks();
        String typed = this.nameField.getTextBuffer().toString().trim();
        if (this.renaming != null) {
            String keep = this.renaming;
            if (!typed.isEmpty()) {
                marks.rename(this.renaming, typed);
                MarkConstructor renamed = marks.find(typed);
                keep = renamed != null ? renamed.name() : typed;
            }
            if (hasTypedCoords()) {
                Vec3d moved = resolvePos();
                if (moved != null) {
                    marks.move(keep, moved);
                }
            }
            this.renaming = null;
            clearFields();
            this.nameField.a(true);
            return true;
        }
        Vec3d pos = resolvePos();
        if (pos == null) {
            fillHere();
            pos = resolvePos();
        }
        if (pos == null) {
            return true;
        }
        marks.add(typed, pos);
        clearFields();
        this.nameField.a(true);
        return true;
    }

    private Vec3d resolvePos() {
        Double x = parseCoord(this.xField);
        Double y = parseCoord(this.yField);
        Double z = parseCoord(this.zField);
        Vec3d fallback = MinecraftClient.getInstance().player != null ? MinecraftClient.getInstance().player.getPos() : null;
        if (x == null && y == null && z == null) {
            return fallback;
        }
        if (fallback == null && (x == null || y == null || z == null)) {
            return null;
        }
        return new Vec3d(
                x != null ? x : fallback.x,
                y != null ? y : fallback.y,
                z != null ? z : fallback.z
        );
    }

    private boolean hasTypedCoords() {
        return parseCoord(this.xField) != null || parseCoord(this.yField) != null || parseCoord(this.zField) != null;
    }

    private void handleFieldClicks(double mouseX, double mouseY, int button) {
        TextField hit = null;
        for (TextField field : fields()) {
            if (MathUtil.a(mouseX, mouseY, field.getPosition().getX(), field.getPosition().getY(), field.getSize().getX(), field.getSize().getY())) {
                hit = field;
                break;
            }
        }
        for (TextField field : fields()) {
            if (field == hit) {
                field.onMouseClick(mouseX, mouseY, button);
            } else if (field.isFocused()) {
                field.a(false);
            }
        }
    }

    private void cycleFocus(boolean reverse) {
        TextField[] all = fields();
        int index = -1;
        for (int i = 0; i < all.length; i++) {
            if (all[i].isFocused()) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            return;
        }
        all[index].a(false);
        int next = reverse ? (index + all.length - 1) % all.length : (index + 1) % all.length;
        all[next].a(true);
    }

    private void clearFields() {
        write(this.nameField, "");
        write(this.xField, "");
        write(this.yField, "");
        write(this.zField, "");
        unfocus();
    }

    private TextField[] fields() {
        return new TextField[]{this.nameField, this.xField, this.yField, this.zField};
    }

    private static void write(TextField field, String text) {
        field.getTextBuffer().setLength(0);
        if (text != null && !text.isEmpty()) {
            field.getTextBuffer().append(text);
        }
        field.a(field.isFocused());
    }

    private static Double parseCoord(TextField field) {
        String raw = field.getTextBuffer().toString().trim().replace(',', '.');
        if (raw.isEmpty() || "-".equals(raw) || ".".equals(raw) || "-.".equals(raw)) {
            return null;
        }
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static final class Row {
        private String name;
        private PanelChrome.Hit rename;
        private PanelChrome.Hit gps;
        private PanelChrome.Hit copy;
        private PanelChrome.Hit remove;
    }
}
