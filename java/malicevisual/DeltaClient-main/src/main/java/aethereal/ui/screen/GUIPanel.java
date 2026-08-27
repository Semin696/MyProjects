package aethereal.ui.screen;

import aethereal.config.ThemeInfo;
import aethereal.config.ThemeProcessor;
import aethereal.core.Category;
import aethereal.core.Module;
import aethereal.core.Skeleton;
import aethereal.render.*;
import aethereal.ui.element.Element;
import aethereal.util.KeyUtil;
import aethereal.util.MathUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Vector4f;

import java.util.List;

public class GUIPanel {
    private final Vector4f bounds = new Vector4f(0.0f, 0.0f, 410.0f, 290.0f);
    private final AnimationUtil scrollAnimation = new AnimationUtil();
    private final AnimationUtil openAnimation = new AnimationUtil();
    private final Category category;
    private List<Module> modules;
    private Module hoveredModule;
    private float targetScroll = 0.0f;
    private float totalContentHeight = 0.0f;

    public GUIPanel(Category category) {
        this.category = category;
    }

    public boolean a(final double mouseX, final double mouseY, final int button) {
        if (this.modules == null) return false;

        // Check if any module is listening for a keybind
        for (Module module : this.modules) {
            if (module.n()) {
                module.a(-100 + button);
                module.b(false);
                return true;
            }
        }

        // Check clicks on setting elements first (if module is expanded)
        for (Module module : this.modules) {
            if (module.o() && module.h().c() > 0.3f) {
                for (Element<?> element : module.d()) {
                    if (element.isEnabled() && element.onMouseClick(mouseX, mouseY, button)) {
                        return true;
                    }
                }
            }
        }

        // Check clicks on module cards
        float col0X = this.bounds.x + 8.0f;
        float col1X = this.bounds.x + 210.0f;
        float cardW = 192.0f;
        float col0Y = this.bounds.y + this.scrollAnimation.a();
        float col1Y = this.bounds.y + this.scrollAnimation.a();

        for (Module module : this.modules) {
            int col = col0Y <= col1Y ? 0 : 1;
            float cardX = col == 0 ? col0X : col1X;
            float cardY = col == 0 ? col0Y : col1Y;

            float baseH = 34.0f;
            float settingsH = 0.0f;
            if (module.o() || module.h().c() > 0.0f) {
                for (Element<?> elem : module.d()) {
                    if (elem.isEnabled()) {
                        settingsH += elem.getBounds().w() + 4.0f;
                    }
                }
            }
            float totalH = baseH + (settingsH > 0.0f ? (settingsH + 6.0f) * module.h().c() : 0.0f);

            // Only interact if card is within panel bounds
            if (cardY + totalH >= this.bounds.y && cardY <= this.bounds.y + this.bounds.w) {
                // Check keybind button click — same box as the rendered badge
                String bindLabel = bindLabel(module);
                float bindW = bindBoxWidth(bindLabel);
                float bindH = 11.0f;
                float bindX = bindBoxX(cardX, cardW, bindW);
                float bindY = cardY + 6.0f;
                if (MathUtil.a(mouseX, mouseY, bindX, bindY, bindW, bindH)) {
                    if (button == 0) {
                        module.b(true); // Listen for keybind
                        return true;
                    } else if (button == 1 || button == 2) {
                        module.a(-1); // Reset keybind
                        return true;
                    }
                }

                // Check toggle switch click
                float switchW = 18.0f;
                float switchH = 10.0f;
                float switchX = cardX + cardW - switchW - 6.0f;
                float switchY = cardY + 6.5f;
                if (MathUtil.a(mouseX, mouseY, switchX - 2.0f, switchY - 2.0f, switchW + 4.0f, switchH + 4.0f)) {
                    if (button == 0) {
                        module.a();
                        return true;
                    }
                }

                // Check main card area click
                if (MathUtil.a(mouseX, mouseY, cardX, cardY, cardW, baseH)) {
                    if (button == 0) {
                        module.a(); // Toggle module
                        return true;
                    } else if (button == 1) {
                        if (!module.d().isEmpty()) {
                            module.c(!module.o()); // Expand / Collapse settings
                        }
                        return true;
                    } else if (button == 2) {
                        module.b(true); // Bind on middle click
                        return true;
                    }
                }
            }

            if (col == 0) {
                col0Y += totalH + 8.0f;
            } else {
                col1Y += totalH + 8.0f;
            }
        }

        return false;
    }

    public boolean b(final double mouseX, final double mouseY, final int button) {
        if (this.modules == null) return false;
        return this.modules.stream()
                .filter(Module::o)
                .flatMap(module -> module.d().stream())
                .filter(Element::isEnabled)
                .anyMatch(element -> element.onMouseRelease(mouseX, mouseY, button));
    }

    public boolean a(final double mouseX, final double mouseY, final int button, final double deltaX, final double deltaY) {
        if (this.modules == null) return false;
        return this.modules.stream()
                .filter(Module::o)
                .flatMap(module -> module.d().stream())
                .filter(Element::isEnabled)
                .anyMatch(element -> element.onMouseDrag(mouseX, mouseY, button, deltaX, deltaY));
    }

    public boolean a(final int keyCode, final int scanCode, final int modifiers) {
        if (this.modules == null) return false;
        for (Module module : this.modules) {
            if (module.n()) {
                if (keyCode == 256) { // ESC clears bind
                    module.a(-1);
                } else {
                    module.a(keyCode);
                }
                module.b(false);
                return true;
            }
        }
        return this.modules.stream()
                .filter(Module::o)
                .flatMap(module -> module.d().stream())
                .filter(Element::isEnabled)
                .anyMatch(element -> element.onKeyPress(keyCode, scanCode, modifiers));
    }

    public boolean a(final char chr, final int modifiers) {
        if (this.modules == null) return false;
        return this.modules.stream()
                .filter(Module::o)
                .flatMap(module -> module.d().stream())
                .filter(Element::isEnabled)
                .anyMatch(element -> element.onCharTyped(chr, modifiers));
    }

    public boolean a(final double mouseX, final double mouseY, final double amount) {
        if (this.modules != null) {
            int wheel = KeyUtil.fromScroll(amount);
            if (wheel != KeyUtil.UNKNOWN.a()) {
                for (Module module : this.modules) {
                    if (module.n()) {
                        module.a(wheel);
                        module.b(false);
                        return true;
                    }
                    for (Element<?> el : module.d()) {
                        if (el.isEnabled() && el.onMouseScroll(mouseX, mouseY, amount)) {
                            return true;
                        }
                    }
                }
            }
        }
        if (!MathUtil.a(mouseX, mouseY, this.bounds.x, this.bounds.y, this.bounds.z, this.bounds.w)) {
            return false;
        }
        float maxScroll = Math.max(0.0f, this.totalContentHeight - this.bounds.w + 16.0f);
        this.targetScroll = MathUtil.b(this.targetScroll + (float) amount * 22.0f, -maxScroll, 0.0f);
        return true;
    }

    public void a(List<Module> modules) {
        this.modules = modules;
    }

    public void a(Module hovered) {
        this.hoveredModule = hovered;
    }

    public Vector4f f() {
        return this.bounds;
    }

    public AnimationUtil a() {
        return this.scrollAnimation;
    }

    public AnimationUtil b() {
        return this.openAnimation;
    }

    public Category c() {
        return this.category;
    }

    public List<Module> d() {
        return this.modules;
    }

    public Module e() {
        return this.hoveredModule;
    }

    public void a(DrawContext context, int mouseX, int mouseY, float delta) {
        if (this.modules == null || this.modules.isEmpty()) {
            MatrixStack matrices = context.getMatrices();
            Fonts.c.b(matrices, "Модули не найдены", this.bounds.x + (this.bounds.z / 2.0f), this.bounds.y + 100.0f, 8.0f, ColorUtil.convertToARGB(100, 105, 120, 255));
            return;
        }

        MatrixStack matrices = context.getMatrices();
        Draw2DProcessor draw = Skeleton.getInstance().getModuleProcessor().i();
        ThemeProcessor theme = Skeleton.getInstance().getModuleProcessor().o();
        int primary = theme.a(ThemeInfo.PRIMARY).toIntColor();

        // Smooth scroll interpolation
        float currentScroll = this.scrollAnimation.a();
        currentScroll = MathUtil.c(currentScroll, this.targetScroll, delta * 0.25f);
        this.scrollAnimation.c(currentScroll);

        // Scissor viewport for card grid
        ScissorUtil.a(matrices, this.bounds.x, this.bounds.y, this.bounds.z, this.bounds.w);

        float col0X = this.bounds.x + 8.0f;
        float col1X = this.bounds.x + 210.0f;
        float cardW = 192.0f;
        float col0Y = this.bounds.y + 6.0f + currentScroll;
        float col1Y = this.bounds.y + 6.0f + currentScroll;

        this.hoveredModule = null;

        for (Module module : this.modules) {
            int col = col0Y <= col1Y ? 0 : 1;
            float cardX = col == 0 ? col0X : col1X;
            float cardY = col == 0 ? col0Y : col1Y;

            float baseH = 34.0f;

            // Calculate settings drawer height
            float settingsH = 0.0f;
            for (Element<?> elem : module.d()) {
                elem.getVisibilityAnimation().a(elem.isEnabled());
                elem.getVisibilityAnimation().a(0.0f, 1.0f, 0.35f, EasingList.i, delta);
                if (elem.isEnabled()) {
                    settingsH += (elem.getBounds().w() + 4.0f) * elem.getVisibilityAnimation().c();
                }
            }

            // Animate expand
            module.h().a(module.o());
            module.h().a(0.0f, 1.0f, 0.35f, EasingList.i, delta);
            float expandProgress = module.h().c();

            float totalCardH = baseH + (settingsH > 0.0f ? (settingsH + 6.0f) * expandProgress : 0.0f);

            // Animate enable state
            module.f().a(module.m());
            module.f().a(0.0f, 1.0f, 0.3f, EasingList.i, delta);
            float enableProgress = module.f().c();

            // Hover check
            boolean hover = MathUtil.a(mouseX, mouseY, cardX, cardY, cardW, totalCardH)
                    && mouseY >= this.bounds.y && mouseY <= this.bounds.y + this.bounds.w;
            if (hover) {
                this.hoveredModule = module;
            }
            module.i().a(hover);
            module.i().a(0.0f, 1.0f, 0.25f, EasingList.i, delta);
            float hoverProgress = module.i().c();

            // Render Card if visible on screen
            if (cardY + totalCardH >= this.bounds.y - 10.0f && cardY <= this.bounds.y + this.bounds.w + 10.0f) {
                // Card Background (Vibrant Glassmorphism + Bright Neon Glow when active)
                int bgBase = ColorUtil.convertToARGB(18, 22, 32, 210);
                int bgHover = ColorUtil.applyAlphaToColor(ColorUtil.convertToARGB(255, 255, 255, 255), 0.05f * hoverProgress);

                draw.a(matrices, cardX, cardY, cardW, totalCardH, 6.0f, bgBase);
                if (hoverProgress > 0.01f) {
                    draw.a(matrices, cardX, cardY, cardW, totalCardH, 6.0f, bgHover);
                }
                if (enableProgress > 0.01f) {
                    draw.a(matrices, cardX - 1.0f, cardY - 1.0f, cardW + 2.0f, totalCardH + 2.0f, 7.0f, ColorUtil.applyAlphaToColor(primary, 0.10f * enableProgress));
                    draw.a(matrices, cardX, cardY, cardW, totalCardH, 6.0f,
                            ColorUtil.applyAlphaToColor(primary, 0.10f * enableProgress),
                            ColorUtil.applyAlphaToColor(primary, 0.04f * enableProgress),
                            ColorUtil.applyAlphaToColor(primary, 0.04f * enableProgress),
                            ColorUtil.applyAlphaToColor(primary, 0.02f * enableProgress)
                    );
                }

                // Status Indicator Dot (Vivid glowing neon orb with white core)
                float dotX = cardX + 7.5f;
                float dotY = cardY + 11.5f;
                if (enableProgress > 0.01f) {
                    draw.a(matrices, dotX - 2.0f, dotY - 2.0f, 8.0f, 8.0f, 4.0f, ColorUtil.applyAlphaToColor(primary, 0.65f * enableProgress));
                    draw.a(matrices, dotX, dotY, 4.0f, 4.0f, 2.0f, ColorUtil.convertToARGB(255, 255, 255, 255));
                } else {
                    draw.a(matrices, dotX, dotY, 4.0f, 4.0f, 2.0f, ColorUtil.convertToARGB(60, 70, 90, 255));
                }

                // Keybind Button Badge
                String bindLabel = bindLabel(module);
                float bindBoxW = bindBoxWidth(bindLabel);
                float bindBoxH = 9.0f;
                float bindBoxX = bindBoxX(cardX, cardW, bindBoxW);
                float bindBoxY = cardY + 6.5f;

                int bindBg = ColorUtil.convertToARGB(22, 26, 36, 245);
                draw.a(matrices, bindBoxX, bindBoxY, bindBoxW, bindBoxH, 2.5f, bindBg);
                if (module.n()) {
                    draw.a(matrices, bindBoxX, bindBoxY, bindBoxW, bindBoxH, 2.5f, 0.8f, ColorUtil.applyAlphaToColor(primary, 0.9f));
                }
                Fonts.c.b(matrices, bindLabel, bindBoxX + (bindBoxW / 2.0f), (bindBoxY + ((bindBoxH - Fonts.c.a(5.5f)) / 2.0f)) - 0.75f, 5.5f, module.n() ? primary : ColorUtil.convertToARGB(190, 205, 230, 255));

                // Module Title (Strictly clipped and bright white when active)
                int titleColor = ColorUtil.lerpColor(ColorUtil.convertToARGB(190, 205, 225, 255), ColorUtil.convertToARGB(255, 255, 255, 255), enableProgress);
                float maxTitleW = Math.max(20.0f, bindBoxX - (cardX + 16.0f) - 3.0f);
                String safeTitle = trimToWidth(Fonts.d, module.j(), 7.25f, maxTitleW);
                Fonts.d.a(matrices, safeTitle, cardX + 16.0f, cardY + 7.5f, 7.25f, titleColor);

                // iOS-Style Toggle Switch (Vibrant Neon Glow)
                float switchW = 18.0f;
                float switchH = 10.0f;
                float switchX = cardX + cardW - switchW - 6.0f;
                float switchY = cardY + 6.0f;

                int switchOffBg = ColorUtil.convertToARGB(30, 35, 48, 240);
                int switchTrackColor = ColorUtil.lerpColor(switchOffBg, primary, enableProgress);
                if (enableProgress > 0.05f) {
                    draw.a(matrices, switchX - 1.0f, switchY - 1.0f, switchW + 2.0f, switchH + 2.0f, 6.0f, ColorUtil.applyAlphaToColor(primary, 0.40f * enableProgress));
                }
                draw.a(matrices, switchX, switchY, switchW, switchH, 5.0f, switchTrackColor);

                // Switch Thumb (Circle)
                float thumbSize = 7.0f;
                float thumbX = switchX + 1.5f + ((switchW - thumbSize - 3.0f) * enableProgress);
                float thumbY = switchY + 1.5f;
                draw.a(matrices, thumbX, thumbY, thumbSize, thumbSize, 3.5f, ColorUtil.convertToARGB(255, 255, 255, 255));

                // Module Subtitle (Strictly clipped with trimToWidth to prevent overflow)
                int descColor = ColorUtil.lerpColor(ColorUtil.convertToARGB(130, 145, 170, 255), ColorUtil.convertToARGB(205, 225, 250, 255), enableProgress);
                float maxDescW = cardW - 34.0f;
                String safeDesc = trimToWidth(Fonts.c, module.k(), 5.25f, maxDescW);
                Fonts.c.a(matrices, safeDesc, cardX + 16.0f, cardY + 19.5f, 5.25f, descColor);

                // Settings icon indicator
                if (!module.d().isEmpty()) {
                    String expandIcon = module.o() ? "▲" : "▼";
                    Fonts.c.a(matrices, expandIcon, cardX + cardW - 14.0f, cardY + 20.0f, 5.0f, ColorUtil.applyAlphaToColor(descColor, 0.9f));
                }

                // Render Expanded Settings Container
                if (expandProgress > 0.01f && settingsH > 0.0f) {
                    float drawerY = cardY + baseH;
                    float drawerH = (settingsH + 4.0f) * expandProgress;

                    ScissorUtil.a(matrices, cardX, drawerY, cardW, drawerH);

                    // Inner drawer background & divider
                    draw.a(matrices, cardX + 4.0f, drawerY, cardW - 8.0f, drawerH - 2.0f, 4.0f, ColorUtil.convertToARGB(12, 14, 22, (int) (160 * expandProgress)));
                    draw.a(matrices, cardX + 8.0f, drawerY - 1.0f, cardW - 16.0f, 0.5f, 0.0f, ColorUtil.applyAlphaToColor(primary, 0.4f * expandProgress));

                    float elemY = drawerY + 4.0f;
                    float elemW = cardW - 16.0f;

                    for (Element<?> elem : module.d()) {
                        float elemVis = elem.getVisibilityAnimation().c();
                        if (elemVis > 0.01f) {
                            elem.getBounds().set(cardX + 8.0f, elemY, elemW, elem.getBounds().w());
                            elem.render(context, mouseX, mouseY, delta, expandProgress * elemVis);
                            elemY += (elem.getBounds().w() + 4.0f) * elemVis;
                        }
                    }

                    ScissorUtil.a(matrices);
                }
            }

            if (col == 0) {
                col0Y += totalCardH + 8.0f;
            } else {
                col1Y += totalCardH + 8.0f;
            }
        }

        this.totalContentHeight = Math.max(col0Y - (this.bounds.y + 6.0f + currentScroll), col1Y - (this.bounds.y + 6.0f + currentScroll));

        // Scrollbar
        if (this.totalContentHeight > this.bounds.w) {
            float barW = 3.0f;
            float barX = this.bounds.x + this.bounds.z - barW - 2.0f;
            float barH = Math.max(20.0f, (this.bounds.w / this.totalContentHeight) * this.bounds.w);
            float scrollRatio = -currentScroll / (this.totalContentHeight - this.bounds.w);
            float barY = this.bounds.y + (scrollRatio * (this.bounds.w - barH));

            draw.a(matrices, barX, this.bounds.y, barW, this.bounds.w, 1.5f, ColorUtil.convertToARGB(15, 18, 26, 180));
            draw.a(matrices, barX, barY, barW, barH, 1.5f, ColorUtil.applyAlphaToColor(primary, 0.5f));
        }

        ScissorUtil.a(matrices);
    }

    public void a(DrawContext context, double mouseX, double mouseY, float delta) {
        if (this.modules == null) return;
        for (Module module : this.modules) {
            for (Element<?> element : module.d()) {
                element.renderColorPicker(context, mouseX, mouseY, delta);
                element.renderOverlay(context, mouseX, mouseY, delta);
            }
        }
    }

    public void i(Module module) {
        module.b(module == this.hoveredModule && !module.n());
    }

    private static String bindLabel(Module module) {
        if (module.n()) {
            return "...";
        }
        if (module.p() == -1) {
            return "[-]";
        }
        return "[" + KeyUtil.b(module.p()) + "]";
    }

    private static float bindBoxWidth(String label) {
        return Math.min(52.0f, Math.max(16.0f, Fonts.c.a(label, 5.5f) + 6.0f));
    }

    private static float bindBoxX(float cardX, float cardW, float bindBoxW) {
        return (cardX + cardW - 24.0f) - bindBoxW - 4.0f;
    }

    private String trimToWidth(Font font, String text, float size, float maxWidth) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        if (font.a(text, size) <= maxWidth) {
            return text;
        }
        String dots = "...";
        float dotsW = font.a(dots, size);
        if (dotsW >= maxWidth) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (font.a(sb.toString() + c, size) + dotsW > maxWidth) {
                break;
            }
            sb.append(c);
        }
        return sb.toString().trim() + dots;
    }
}
