package aethereal.ui.screen;

import aethereal.config.ThemeInfo;
import aethereal.config.ThemeProcessor;
import aethereal.core.Category;
import aethereal.core.Module;
import aethereal.core.Skeleton;
import aethereal.core.Interface;
import aethereal.render.*;
import aethereal.ui.element.TextField;
import aethereal.util.MathUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector2f;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GUIScreen extends Screen {
    private final TextField searchField;
    private final AnimationUtil openAnimation;
    private final List<GUIPanel> panels;
    private final Map<Category, AnimationUtil> categoryAnimations;
    private final FriendsPanel friendsPanel;
    private final MarksPanel marksPanel;
    private final ConfigsPanel configsPanel;
    private Category activeCategory = Category.Movement;
    private static final float SIDEBAR_W = 52.0f;
    private static final float SIDEBAR_TAB = 32.0f;
    private static final float SIDEBAR_TAB_STEP = 36.0f;
    private static final float SIDEBAR_TABS_TOP = 46.0f;

    public GUIScreen(Text title) {
        super(title);
        this.searchField = new TextField(TextField.type.GUI);
        this.searchField.setPlaceholder("Поиск модулей...");
        this.openAnimation = new AnimationUtil();
        this.panels = new ArrayList<>();
        this.categoryAnimations = new HashMap<>();
        this.friendsPanel = new FriendsPanel();
        this.marksPanel = new MarksPanel();
        this.configsPanel = new ConfigsPanel();

        for (Category category : Category.values()) {
            if (!category.isCustomPanel()) {
                this.panels.add(new GUIPanel(category));
            }
            this.categoryAnimations.put(category, new AnimationUtil());
        }
    }

    public GUIScreen() {
        this(Text.empty());
    }

    public static boolean f(GUIPanel panel) {
        return panel.d() != null;
    }

    public static boolean e(GUIPanel panel) {
        return panel.d() != null;
    }

    public static boolean d(GUIPanel panel) {
        return panel.d() != null;
    }

    public static boolean c(GUIPanel panel) {
        return panel.d() != null;
    }

    public static boolean b(GUIPanel panel) {
        return panel.d() != null;
    }

    public static boolean a(GUIPanel panel) {
        return panel.d() != null;
    }

    public static float getGuiWidth() {
        double scaleFactor = Interface.mc.getWindow().calculateScaleFactor(2, Interface.mc.forcesUnicodeFont());
        return (float) Math.ceil((double) Interface.mc.getWindow().getFramebufferWidth() / scaleFactor);
    }

    public static float getGuiHeight() {
        double scaleFactor = Interface.mc.getWindow().calculateScaleFactor(2, Interface.mc.forcesUnicodeFont());
        return (float) Math.ceil((double) Interface.mc.getWindow().getFramebufferHeight() / scaleFactor);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        double scaledMouseX = MathUtil.scale(mouseX, 2);
        double scaledMouseY = MathUtil.scale(mouseY, 2);
        ScaleUtil.a(context, 2);

        MatrixStack matrices = context.getMatrices();
        Draw2DProcessor draw = Skeleton.getInstance().getModuleProcessor().i();
        ThemeProcessor theme = Skeleton.getInstance().getModuleProcessor().o();
        int primary = theme.a(ThemeInfo.PRIMARY).toIntColor();

        // Background blur
        if (draw.e() != null) {
            draw.e().a(matrices);
        }

        // Window dimensions (560 x 350)
        float windowW = 560.0f;
        float windowH = 350.0f;
        float windowX = (getGuiWidth() - windowW) * 0.5f;
        float windowY = (getGuiHeight() - windowH) * 0.5f;

        // Opening animation (Scale & Alpha)
        this.openAnimation.a(true);
        this.openAnimation.a(0.0f, 1.0f, 0.35f, EasingList.s, delta);
        float openProgress = EasingList.s.ease(this.openAnimation.c());
        float openAlpha = EasingList.p.ease(this.openAnimation.c());
        float scale = 0.90f + (0.10f * openProgress);

        matrices.push();
        matrices.translate(windowX + (windowW * 0.5f), windowY + (windowH * 0.5f), 0.0f);
        matrices.scale(scale, scale, 1.0f);
        matrices.translate(-(windowX + (windowW * 0.5f)), -(windowY + (windowH * 0.5f)), 0.0f);

        // 1. Translucent Window Glow & Soft Shadow
        int shadowColor = ColorUtil.applyAlphaToColor(primary, 0.10f * openAlpha);
        draw.a(matrices, windowX, windowY, windowW, windowH, 12.0f, ColorUtil.convertToARGB(8, 10, 15, (int) (140 * openAlpha)), 1.0f, shadowColor, 20.0f);

        // 2. Translucent Glass Surface (Semi-transparent dark glass)
        int surfaceBg = ColorUtil.convertToARGB(10, 12, 18, (int) (125 * openAlpha));
        draw.a(matrices, windowX, windowY, windowW, windowH, 12.0f, surfaceBg);

        float sidebarW = SIDEBAR_W;
        draw.a(matrices, windowX, windowY, sidebarW, windowH, new org.joml.Vector4f(12.0f, 12.0f, 0.0f, 0.0f), ColorUtil.convertToARGB(6, 8, 12, (int) (90 * openAlpha)));
        draw.a(matrices, windowX + sidebarW, windowY, 0.5f, windowH, 0.0f, ColorUtil.applyAlphaToColor(theme.a(ThemeInfo.OUTLINE_SMALL).toIntColor(), 0.35f * openAlpha));

        float logoSize = 20.0f;
        float logoX = windowX + ((sidebarW - logoSize) * 0.5f);
        float logoY = windowY + 12.0f;
        draw.a(matrices, logoX, logoY, logoSize, logoSize, 5.0f, ColorUtil.applyAlphaToColor(ColorUtil.convertToARGB(16, 12, 24, 255), openAlpha));
        draw.a(matrices, Identifier.of("skeleton", "pictures/avatar.png"), logoX, logoY, logoSize, logoSize, 5.0f, ColorUtil.applyAlphaToColor(ColorUtil.convertToARGB(255, 255, 255, 255), openAlpha));

        draw.a(matrices, windowX + 10.0f, windowY + 38.0f, sidebarW - 20.0f, 0.5f, 0.0f, ColorUtil.applyAlphaToColor(theme.a(ThemeInfo.OUTLINE_SMALL).toIntColor(), 0.25f * openAlpha));

        float tabSize = SIDEBAR_TAB;
        float tabX = windowX + ((sidebarW - tabSize) * 0.5f);
        float tabY = windowY + SIDEBAR_TABS_TOP;

        for (Category cat : Category.values()) {
            boolean isActive = cat == this.activeCategory && this.searchField.getTextBuffer().isEmpty();
            boolean isHovered = MathUtil.a(scaledMouseX, scaledMouseY, tabX, tabY, tabSize, tabSize);

            AnimationUtil anim = this.categoryAnimations.get(cat);
            if (anim != null) {
                anim.a(isActive);
                anim.a(0.0f, 1.0f, 0.3f, EasingList.i, delta);
                float activeProgress = anim.c();

                if (isHovered && activeProgress < 0.99f) {
                    draw.a(matrices, tabX, tabY, tabSize, tabSize, 6.0f, ColorUtil.applyAlphaToColor(ColorUtil.convertToARGB(255, 255, 255, 255), 0.06f * openAlpha));
                }

                if (activeProgress > 0.01f) {
                    draw.a(matrices, tabX - 1.0f, tabY - 1.0f, tabSize + 2.0f, tabSize + 2.0f, 7.0f, ColorUtil.applyAlphaToColor(primary, 0.22f * activeProgress * openAlpha));
                    draw.a(matrices, tabX, tabY, tabSize, tabSize, 6.0f,
                            ColorUtil.applyAlphaToColor(primary, 0.28f * activeProgress * openAlpha),
                            ColorUtil.applyAlphaToColor(primary, 0.08f * activeProgress * openAlpha),
                            ColorUtil.applyAlphaToColor(primary, 0.08f * activeProgress * openAlpha),
                            ColorUtil.applyAlphaToColor(primary, 0.02f * activeProgress * openAlpha)
                    );
                    draw.a(matrices, tabX, tabY, tabSize, tabSize, 6.0f, 0.6f, ColorUtil.applyAlphaToColor(primary, 0.85f * activeProgress * openAlpha));
                }

                float iconSize = 11.0f;
                float tabCenterY = tabY + (tabSize / 2.0f);
                float iconW = Fonts.a.a(cat.a(), iconSize);
                float iconX = tabX + ((tabSize - iconW) * 0.5f);
                float iconY = Fonts.a.a(cat.a(), iconSize, tabCenterY);

                int iconColor = ColorUtil.applyAlphaToColor(
                        ColorUtil.lerpColor(ColorUtil.convertToARGB(180, 195, 220, 255), primary, activeProgress),
                        openAlpha
                );
                if (isHovered && activeProgress < 0.5f) {
                    iconColor = ColorUtil.applyAlphaToColor(ColorUtil.convertToARGB(255, 255, 255, 255), openAlpha);
                }

                Fonts.a.a(matrices, cat.a(), iconX, iconY, iconSize, iconColor);
            }

            tabY += SIDEBAR_TAB_STEP;
        }

        float avatarSize = 22.0f;
        float avatarX = windowX + ((sidebarW - avatarSize) * 0.5f);
        float avatarY = windowY + windowH - avatarSize - 10.0f;
        draw.a(matrices, avatarX, avatarY, avatarSize, avatarSize, 5.0f, ColorUtil.applyAlphaToColor(ColorUtil.convertToARGB(20, 24, 35, 255), openAlpha));
        draw.a(matrices, Identifier.of("skeleton", "pictures/avatar.png"), avatarX, avatarY, avatarSize, avatarSize, 5.0f, ColorUtil.applyAlphaToColor(ColorUtil.convertToARGB(255, 255, 255, 255), openAlpha));
        draw.a(matrices, avatarX, avatarY, avatarSize, avatarSize, 5.0f, 0.75f, ColorUtil.applyAlphaToColor(primary, 0.85f * openAlpha));

        // 7. Right Content Area: Top Header Bar
        float contentX = windowX + sidebarW;
        float contentW = windowW - sidebarW;
        float headerH = 42.0f;

        boolean searching = !this.searchField.getTextBuffer().isEmpty();
        String categoryTitle = searching ? "Результаты поиска" : this.activeCategory.b();
        String categoryDesc = searching ? "Модули по запросу \"" + this.searchField.getTextBuffer() + "\"" : getCategoryDescription(this.activeCategory);

        Fonts.d.a(matrices, categoryTitle, contentX + 14.0f, windowY + 11.0f, 10.0f, ColorUtil.applyAlphaToColor(ColorUtil.convertToARGB(255, 255, 255, 255), openAlpha));
        Fonts.c.a(matrices, categoryDesc, contentX + 14.0f, windowY + 24.0f, 6.0f, ColorUtil.applyAlphaToColor(theme.a(ThemeInfo.TEXT_DISABLED).toIntColor(), openAlpha));

        // Top Header Divider
        draw.a(matrices, contentX, windowY + headerH, contentW, 0.5f, 0.0f, ColorUtil.applyAlphaToColor(theme.a(ThemeInfo.OUTLINE_SMALL).toIntColor(), 0.3f * openAlpha));

        // 8. Top Header Search Bar
        float searchW = 138.0f;
        float searchH = 20.0f;
        float searchX = windowX + windowW - searchW - 14.0f;
        float searchY = windowY + 11.0f;

        if (!this.activeCategory.isCustomPanel() || searching) {
            this.searchField.setSize(new Vector2f(searchW, searchH));
            this.searchField.setPosition(new Vector2f(searchX, searchY));
            this.searchField.render(context, (int) scaledMouseX, (int) scaledMouseY, delta, openAlpha);
        }

        // 9. Right Content Panels (Module Grid / Friends)
        float panelX = contentX + 4.0f;
        float panelY = windowY + headerH + 2.0f;
        float panelW = contentW - 8.0f;
        float panelH = windowH - headerH - 6.0f;

        String searchQuery = this.searchField.getTextBuffer().toString().trim().toLowerCase();

        if (this.activeCategory.isFriends() && searchQuery.isEmpty()) {
            PanelChrome.clipBegin(matrices, panelX, panelY, panelW, panelH);
            this.friendsPanel.render(context, panelX, panelY, panelW, panelH, scaledMouseX, scaledMouseY, delta, openAlpha);
            PanelChrome.clipEnd(matrices);
        } else if (this.activeCategory.isMarks() && searchQuery.isEmpty()) {
            PanelChrome.clipBegin(matrices, panelX, panelY, panelW, panelH);
            this.marksPanel.render(context, panelX, panelY, panelW, panelH, scaledMouseX, scaledMouseY, delta, openAlpha);
            PanelChrome.clipEnd(matrices);
        } else if (this.activeCategory.isConfigs() && searchQuery.isEmpty()) {
            PanelChrome.clipBegin(matrices, panelX, panelY, panelW, panelH);
            this.configsPanel.render(context, panelX, panelY, panelW, panelH, scaledMouseX, scaledMouseY, delta, openAlpha);
            PanelChrome.clipEnd(matrices);
        } else {
            for (GUIPanel panel : this.panels) {
                if (panel.c() == this.activeCategory || !searchQuery.isEmpty()) {
                    panel.f().set(panelX, panelY, panelW, panelH);

                    List<Module> filteredModules = Skeleton.getInstance().getModuleProcessor().t().e().stream()
                            .filter(m -> {
                                if (!searchQuery.isEmpty()) {
                                    return m.j().toLowerCase().contains(searchQuery) || m.k().toLowerCase().contains(searchQuery);
                                }
                                return m.l() == panel.c();
                            })
                            .sorted(Comparator.comparing(Module::j, String.CASE_INSENSITIVE_ORDER))
                            .toList();

                    panel.a(filteredModules);
                    panel.a(context, (int) scaledMouseX, (int) scaledMouseY, delta);
                    break;
                }
            }
        }

        matrices.pop();

        // 10. Render Floating Color Picker Popups (Outside Window Scissor)
        for (GUIPanel panel : this.panels) {
            if (panel.c() == this.activeCategory || !searchQuery.isEmpty()) {
                panel.a(context, scaledMouseX, scaledMouseY, delta);
            }
        }

        ScaleUtil.a(context);
    }

    private String getCategoryDescription(Category category) {
        return switch (category) {
            case Movement -> "Модули перемещения и передвижения";
            case Player -> "Взаимодействие с персонажем и инвентарем";
            case Render -> "Визуальные эффекты, HUD и интерфейс";
            case Misc -> "Вспомогательные утилиты и автоматизация";
            case Friends -> "Список друзей, онлайн-статус и защита от урона";
            case Marks -> "Точки на координатах: поставить, назвать и удалить";
            case Configs -> "Сохранение, загрузка и папка конфигов";
        };
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        double scaledMouseX = MathUtil.scale(mouseX, 2);
        double scaledMouseY = MathUtil.scale(mouseY, 2);

        float windowW = 560.0f;
        float windowH = 350.0f;
        float windowX = (getGuiWidth() - windowW) * 0.5f;
        float windowY = (getGuiHeight() - windowH) * 0.5f;

        // Search Field Click
        if (!this.activeCategory.isCustomPanel() || !this.searchField.getTextBuffer().isEmpty()) {
            this.searchField.onMouseClick(scaledMouseX, scaledMouseY, button);
        }

        float tabX = windowX + ((SIDEBAR_W - SIDEBAR_TAB) * 0.5f);
        float tabY = windowY + SIDEBAR_TABS_TOP;

        for (Category cat : Category.values()) {
            if (MathUtil.a(scaledMouseX, scaledMouseY, tabX, tabY, SIDEBAR_TAB, SIDEBAR_TAB)) {
                if (button == 0) {
                    this.activeCategory = cat;
                    this.searchField.getTextBuffer().setLength(0);
                    this.searchField.a(false);
                    if (!cat.isFriends()) {
                        this.friendsPanel.unfocus();
                    }
                    if (!cat.isMarks()) {
                        this.marksPanel.unfocus();
                    }
                    if (!cat.isConfigs()) {
                        this.configsPanel.unfocus();
                    }
                    return true;
                }
            }
            tabY += SIDEBAR_TAB_STEP;
        }

        if (this.activeCategory.isFriends() && this.searchField.getTextBuffer().isEmpty()) {
            if (this.friendsPanel.mouseClicked(scaledMouseX, scaledMouseY, button)) {
                return true;
            }
        } else if (this.activeCategory.isMarks() && this.searchField.getTextBuffer().isEmpty()) {
            if (this.marksPanel.mouseClicked(scaledMouseX, scaledMouseY, button)) {
                return true;
            }
        } else if (this.activeCategory.isConfigs() && this.searchField.getTextBuffer().isEmpty()) {
            if (this.configsPanel.mouseClicked(scaledMouseX, scaledMouseY, button)) {
                return true;
            }
        } else {
            for (GUIPanel panel : this.panels) {
                if (panel.c() == this.activeCategory || !this.searchField.getTextBuffer().isEmpty()) {
                    if (panel.a(scaledMouseX, scaledMouseY, button)) {
                        return true;
                    }
                    break;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(final double mouseX, final double mouseY, final int button) {
        double scaledMouseX = MathUtil.scale(mouseX, 2);
        double scaledMouseY = MathUtil.scale(mouseY, 2);

        for (GUIPanel panel : this.panels) {
            if (panel.b(scaledMouseX, scaledMouseY, button)) {
                return true;
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(final double mouseX, final double mouseY, final int button, final double deltaX, final double deltaY) {
        double scaledMouseX = MathUtil.scale(mouseX, 2);
        double scaledMouseY = MathUtil.scale(mouseY, 2);
        double scaledDeltaX = MathUtil.scale(deltaX, 2);
        double scaledDeltaY = MathUtil.scale(deltaY, 2);

        this.searchField.onMouseDrag(scaledMouseX, scaledMouseY, button);
        this.friendsPanel.mouseDragged(scaledMouseX, scaledMouseY, button);
        this.marksPanel.mouseDragged(scaledMouseX, scaledMouseY, button);
        this.configsPanel.mouseDragged(scaledMouseX, scaledMouseY, button);

        for (GUIPanel panel : this.panels) {
            if (panel.a(scaledMouseX, scaledMouseY, button, scaledDeltaX, scaledDeltaY)) {
                return true;
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(final double mouseX, final double mouseY, double horizontalAmount, final double verticalAmount) {
        double scaledMouseX = MathUtil.scale(mouseX, 2);
        double scaledMouseY = MathUtil.scale(mouseY, 2);

        if (this.activeCategory.isFriends() && this.searchField.getTextBuffer().isEmpty()) {
            if (this.friendsPanel.mouseScrolled(scaledMouseX, scaledMouseY, verticalAmount)) {
                return true;
            }
        } else if (this.activeCategory.isMarks() && this.searchField.getTextBuffer().isEmpty()) {
            if (this.marksPanel.mouseScrolled(scaledMouseX, scaledMouseY, verticalAmount)) {
                return true;
            }
        } else if (this.activeCategory.isConfigs() && this.searchField.getTextBuffer().isEmpty()) {
            if (this.configsPanel.mouseScrolled(scaledMouseX, scaledMouseY, verticalAmount)) {
                return true;
            }
        } else {
            for (GUIPanel panel : this.panels) {
                if (panel.c() == this.activeCategory || !this.searchField.getTextBuffer().isEmpty()) {
                    if (panel.a(scaledMouseX, scaledMouseY, verticalAmount)) {
                        return true;
                    }
                    break;
                }
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(final int keyCode, final int scanCode, final int modifiers) {
        // Ctrl+F focus search
        if (keyCode == 70 && (modifiers & 2) != 0) {
            this.friendsPanel.unfocus();
            this.marksPanel.unfocus();
            this.configsPanel.unfocus();
            this.searchField.a(!this.searchField.isFocused());
            return true;
        }

        if (this.friendsPanel.isAddFocused()) {
            if (this.friendsPanel.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }

        if (this.marksPanel.isAddFocused()) {
            if (this.marksPanel.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }

        if (this.configsPanel.isAddFocused()) {
            if (this.configsPanel.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }

        if (this.searchField.isFocused()) {
            if (keyCode == 256) { // ESC unfocus search
                this.searchField.a(false);
                return true;
            }
            this.searchField.a(keyCode, scanCode, modifiers);
            return true;
        }

        for (GUIPanel panel : this.panels) {
            if (panel.a(keyCode, scanCode, modifiers)) {
                return true;
            }
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(final char character, final int modifiers) {
        if (this.friendsPanel.charTyped(character, modifiers)) {
            return true;
        }
        if (this.marksPanel.charTyped(character, modifiers)) {
            return true;
        }
        if (this.configsPanel.charTyped(character, modifiers)) {
            return true;
        }
        if (this.searchField.isFocused()) {
            this.searchField.a(character, modifiers);
            return true;
        }
        for (GUIPanel panel : this.panels) {
            if (panel.a(character, modifiers)) {
                return true;
            }
        }
        return super.charTyped(character, modifiers);
    }

    public TextField a() {
        return this.searchField;
    }

    public AnimationUtil b() {
        return this.openAnimation;
    }

    public List<GUIPanel> c() {
        return this.panels;
    }

    public String d() {
        return this.activeCategory.b();
    }

    @Override
    public void close() {
        super.close();
        this.openAnimation.c(0.0f);
        this.panels.forEach(panel -> {
            panel.b().c(0.0f);
        });
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    public boolean a(GUIPanel panel, Module module) {
        return module.l() == panel.c() && module.j().toLowerCase().contains(this.searchField.getTextBuffer().toString().toLowerCase());
    }
}

