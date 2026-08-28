package aethereal.ui.screen;

import aethereal.config.ModuleProcessor;
import aethereal.config.ThemeInfo;
import aethereal.config.ThemeProcessor;
import aethereal.core.Skeleton;
import aethereal.notification.Notification;
import aethereal.render.ColorUtil;
import aethereal.render.Draw2DProcessor;
import aethereal.render.Fonts;
import aethereal.ui.element.TextField;
import aethereal.util.MathUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector2f;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ConfigsPanel {
    private final TextField nameField = new TextField(TextField.type.GUI);
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM HH:mm");
    private final List<Row> rows = new ArrayList<>();
    private float targetScroll;
    private float scroll;
    private float listTop;
    private float listBottom;
    private float saveX, saveY, saveW, saveH;
    private float folderX, folderY, folderW, folderH;

    public ConfigsPanel() {
        this.nameField.setPlaceholder("Имя конфига...");
    }

    public void render(DrawContext context, float x, float y, float width, float height, double mouseX, double mouseY, float delta, float alpha) {
        MatrixStack matrices = context.getMatrices();
        Draw2DProcessor draw = Skeleton.getInstance().getModuleProcessor().i();
        ThemeProcessor theme = Skeleton.getInstance().getModuleProcessor().o();
        ModuleProcessor configs = Skeleton.getInstance().getModuleProcessor().t();
        int primary = theme.a(ThemeInfo.PRIMARY).toIntColor();
        this.rows.clear();

        float pad = 10.0f;
        float gap = 6.0f;
        float innerW = width - pad * 2.0f;
        float innerX = x + pad;
        float cursorY = y + 8.0f;

        List<File> files = configs.configFiles();
        float statsH = 28.0f;
        draw.a(matrices, innerX, cursorY, innerW, statsH, 8.0f, ColorUtil.convertToARGB(16, 12, 26, (int) (170 * alpha)));
        Fonts.d.a(matrices, "Конфиги", innerX + 12.0f, cursorY + 6.0f, 7.2f, ColorUtil.applyAlphaToColor(-1, alpha));
        Fonts.c.a(matrices, files.size() + " файлов  ·  текущий " + configs.currentConfig(), innerX + 12.0f, cursorY + 16.5f, 5.3f,
                ColorUtil.applyAlphaToColor(theme.a(ThemeInfo.TEXT_DISABLED).toIntColor(), alpha));
        cursorY += statsH + 8.0f;

        float fieldH = 24.0f;
        float btnW = 88.0f;
        float fieldW = innerW - btnW * 2.0f - gap * 2.0f;
        this.nameField.setPosition(new Vector2f(innerX, cursorY));
        this.nameField.setSize(new Vector2f(fieldW, fieldH));
        this.nameField.render(context, mouseX, mouseY, delta, alpha);

        this.saveX = innerX + fieldW + gap;
        this.saveY = cursorY;
        this.saveW = btnW;
        this.saveH = fieldH;
        boolean saveHover = MathUtil.a(mouseX, mouseY, this.saveX, this.saveY, this.saveW, this.saveH);
        draw.a(matrices, this.saveX, this.saveY, this.saveW, this.saveH, 7.0f,
                ColorUtil.applyAlphaToColor(primary, (saveHover ? 0.92f : 0.72f) * alpha));
        Fonts.d.b(matrices, "Сохранить", this.saveX + this.saveW / 2.0f, centerY(this.saveY, this.saveH, 6.2f), 6.2f,
                ColorUtil.applyAlphaToColor(-1, alpha));

        this.folderX = this.saveX + btnW + gap;
        this.folderY = cursorY;
        this.folderW = btnW;
        this.folderH = fieldH;
        boolean folderHover = MathUtil.a(mouseX, mouseY, this.folderX, this.folderY, this.folderW, this.folderH);
        draw.a(matrices, this.folderX, this.folderY, this.folderW, this.folderH, 7.0f,
                ColorUtil.convertToARGB(18, 22, 32, (int) ((folderHover ? 210 : 170) * alpha)));
        draw.a(matrices, this.folderX, this.folderY, this.folderW, this.folderH, 7.0f, 0.55f,
                ColorUtil.applyAlphaToColor(primary, (folderHover ? 0.7f : 0.35f) * alpha));
        Fonts.d.b(matrices, "Папка", this.folderX + this.folderW / 2.0f, centerY(this.folderY, this.folderH, 6.2f), 6.2f,
                ColorUtil.applyAlphaToColor(-1, alpha));
        cursorY += fieldH + 6.0f;

        Fonts.c.a(matrices, "Сохраните текущие настройки или загрузите готовый конфиг", innerX, cursorY, 5.2f,
                ColorUtil.applyAlphaToColor(theme.a(ThemeInfo.TEXT_DISABLED).toIntColor(), alpha));
        cursorY += 14.0f;

        this.listTop = cursorY;
        this.listBottom = y + height - 8.0f;
        float listH = Math.max(0.0f, this.listBottom - this.listTop);
        float listX = innerX;
        float listW = innerW;

        float rowH = 46.0f;
        float contentH = files.isEmpty() ? 48.0f : files.size() * (rowH + 6.0f);
        float maxScroll = Math.max(0.0f, contentH - listH);
        this.targetScroll = MathUtil.b(this.targetScroll, 0.0f, maxScroll);
        this.scroll = MathUtil.c(this.scroll, this.targetScroll, delta * 0.28f);

        if (files.isEmpty()) {
            Fonts.c.b(matrices, "Пока нет конфигов — сохраните текущие настройки", listX + listW / 2.0f, this.listTop + 22.0f, 6.4f,
                    ColorUtil.applyAlphaToColor(theme.a(ThemeInfo.TEXT_DISABLED).toIntColor(), alpha));
            return;
        }

        PanelChrome.clipBegin(matrices, listX, this.listTop, listW, listH);
        float rowY = this.listTop - this.scroll;
        String current = configs.currentConfig();
        for (File file : files) {
            if (rowY + rowH >= this.listTop && rowY <= this.listBottom) {
                String name = ModuleProcessor.configDisplayName(file);
                boolean active = current.equalsIgnoreCase(name);
                boolean rowHover = MathUtil.a(mouseX, mouseY, listX, rowY, listW, rowH)
                        && mouseY >= this.listTop && mouseY <= this.listBottom;
                int cardBg = active
                        ? ColorUtil.convertToARGB(28, 18, 36, (int) ((rowHover ? 210 : 175) * alpha))
                        : ColorUtil.convertToARGB(14, 18, 26, (int) ((rowHover ? 200 : 155) * alpha));
                draw.a(matrices, listX, rowY, listW, rowH, 8.0f, cardBg);

                Fonts.a.a(matrices, "C", listX + 12.0f, rowY + 15.0f, 11.0f, ColorUtil.applyAlphaToColor(primary, alpha));
                Fonts.d.a(matrices, name, listX + 32.0f, rowY + 8.0f, 7.2f, ColorUtil.applyAlphaToColor(-1, alpha));
                if (active) {
                    Fonts.c.a(matrices, "активен", listX + 32.0f + Fonts.d.a(name, 7.2f) + 8.0f, rowY + 10.0f, 5.1f,
                            ColorUtil.applyAlphaToColor(primary, alpha));
                }

                String meta = this.dateFormat.format(new Date(file.lastModified())) + "  ·  " + sizeLabel(file.length());
                Fonts.c.a(matrices, meta, listX + 32.0f, rowY + 24.0f, 5.3f,
                        ColorUtil.applyAlphaToColor(theme.a(ThemeInfo.TEXT_DISABLED).toIntColor(), alpha));

                float btn = 20.0f;
                float btnGap = 5.0f;
                float bx = listX + listW - 12.0f - btn;
                float by = rowY + (rowH - btn) / 2.0f;
                Row row = new Row();
                row.name = name;
                row.remove = PanelChrome.icon(draw, matrices, bx, by, btn, "c", ColorUtil.convertToARGB(255, 80, 100, 255), mouseX, mouseY, alpha);
                bx -= btn + btnGap;
                row.save = PanelChrome.icon(draw, matrices, bx, by, btn, "t", ColorUtil.convertToARGB(120, 190, 255, 255), mouseX, mouseY, alpha);
                bx -= btn + btnGap;
                row.load = PanelChrome.icon(draw, matrices, bx, by, btn, "Q", active ? primary : ColorUtil.convertToARGB(170, 165, 180, 255), mouseX, mouseY, alpha);
                this.rows.add(row);
            }
            rowY += rowH + 6.0f;
        }
        PanelChrome.clipEnd(matrices);
        PanelChrome.scrollbar(draw, matrices, listX, listW, this.listTop, listH, contentH, this.scroll, maxScroll, alpha, primary);
    }

    private static String sizeLabel(long bytes) {
        if (bytes < 1024L) {
            return bytes + " Б";
        }
        return String.format(java.util.Locale.US, "%.1f КБ", bytes / 1024.0d);
    }

    private static float centerY(float y, float h, float fontSize) {
        return y + (h - fontSize) * 0.5f - 0.4f;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.nameField.onMouseClick(mouseX, mouseY, button);
        if (button != 0) {
            return this.nameField.isFocused();
        }
        ModuleProcessor configs = Skeleton.getInstance().getModuleProcessor().t();
        if (MathUtil.a(mouseX, mouseY, this.saveX, this.saveY, this.saveW, this.saveH)) {
            return saveFromField();
        }
        if (MathUtil.a(mouseX, mouseY, this.folderX, this.folderY, this.folderW, this.folderH)) {
            configs.openConfigFolder();
            return true;
        }
        if (mouseY >= this.listTop && mouseY <= this.listBottom) {
            for (Row row : this.rows) {
                if (PanelChrome.inside(mouseX, mouseY, row.load)) {
                    if (configs.c(row.name)) {
                        notify("Конфиг " + row.name + " загружен", true);
                    } else {
                        notify("Не удалось загрузить " + row.name, false);
                    }
                    return true;
                }
                if (PanelChrome.inside(mouseX, mouseY, row.save)) {
                    configs.b(row.name);
                    notify("Конфиг " + row.name + " сохранён", true);
                    return true;
                }
                if (PanelChrome.inside(mouseX, mouseY, row.remove)) {
                    if (configs.d(row.name)) {
                        notify("Конфиг " + row.name + " удалён", true);
                    } else {
                        notify("Нельзя удалить " + row.name, false);
                    }
                    return true;
                }
            }
        }
        return this.nameField.isFocused();
    }

    public void mouseDragged(double mouseX, double mouseY, int button) {
        this.nameField.onMouseDrag(mouseX, mouseY, button);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (mouseY >= this.listTop && mouseY <= this.listBottom) {
            this.targetScroll -= (float) (amount * 22.0d);
            return true;
        }
        return false;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!this.nameField.isFocused()) {
            return false;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            saveFromField();
            return true;
        }
        this.nameField.a(keyCode, scanCode, modifiers);
        return true;
    }

    public boolean charTyped(char character, int modifiers) {
        if (!this.nameField.isFocused()) {
            return false;
        }
        this.nameField.a(character, modifiers);
        return true;
    }

    public boolean isAddFocused() {
        return this.nameField.isFocused();
    }

    public void unfocus() {
        this.nameField.a(false);
    }

    private boolean saveFromField() {
        String name = ModuleProcessor.sanitizeConfigName(this.nameField.getTextBuffer().toString());
        if (name.isEmpty()) {
            this.nameField.a(true);
            return true;
        }
        Skeleton.getInstance().getModuleProcessor().t().b(name);
        notify("Конфиг " + name + " сохранён", true);
        this.nameField.getTextBuffer().setLength(0);
        this.nameField.a(true);
        return true;
    }

    private static void notify(String message, boolean ok) {
        int color = ok
                ? ColorUtil.convertToARGB(80, 220, 140, 255)
                : ColorUtil.convertToARGB(230, 80, 90, 255);
        Skeleton.getInstance().getModuleProcessor().m().a(new Notification("Q", color, message, 1500));
    }

    private static final class Row {
        private String name;
        private PanelChrome.Hit load;
        private PanelChrome.Hit save;
        private PanelChrome.Hit remove;
    }
}
