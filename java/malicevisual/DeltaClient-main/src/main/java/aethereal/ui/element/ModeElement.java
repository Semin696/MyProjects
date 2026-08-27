package aethereal.ui.element;


import aethereal.config.ThemeInfo;
import aethereal.config.ThemeProcessor;
import aethereal.core.Skeleton;
import aethereal.render.*;
import aethereal.setting.ModeSetting;
import aethereal.util.MathUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Vector4f;

public class ModeElement extends Element<ModeSetting> {
    private final AnimationUtil[] modeAnimations;
    private String hoveredMode;
    private float hoverX;
    private float hoverY;
    private float spin;

    public ModeElement(ModeSetting setting) {
        super(setting);
        this.modeAnimations = new AnimationUtil[setting.k().size()];
        for (int i = 0; i < this.modeAnimations.length; i++) {
            this.modeAnimations[i] = new AnimationUtil();
        }
    }

    @Override
    public boolean onMouseClick(double mouseX, double mouseY, int button) {
        Vector4f vector4f = this.a;
        var setting = this.b;
        if (button != 0) {
            if (button != 2 || !MathUtil.a(mouseX, mouseY, vector4f.x, vector4f.y, vector4f.z, vector4f.w)) {
                return false;
            }
            if (!(setting instanceof ModeSetting)) {
                throw new ClassCastException();
            }
            setting.b();
            return true;
        }
        float f = vector4f.x;
        float fA = vector4f.y + Fonts.c.a(6.25f) + 5.0f;
        if (!(setting instanceof ModeSetting)) {
            throw new ClassCastException();
        }
        ModeSetting modeSetting = setting;
        for (String str : modeSetting.k()) {
            if (!(str instanceof String)) {
                throw new ClassCastException();
            }
            String str2 = str;
            float fA2 = Fonts.c.a(str2, 6.0f) + 8.0f;
            if (f + fA2 > vector4f.x + vector4f.z) {
                f = vector4f.x;
                fA += 13.0f;
            }
            if (MathUtil.a(mouseX, mouseY, f, fA, fA2, 11.0f)) {
                modeSetting.a(str2);
                return true;
            }
            f += fA2 + 3.0f;
        }
        return false;
    }

    @Override
    public void render(DrawContext context, double mouseX, double mouseY, float delta, float extend) {
        MatrixStack matrices = context.getMatrices();
        Draw2DProcessor draw = Skeleton.getInstance().getModuleProcessor().i();
        ThemeProcessor theme = Skeleton.getInstance().getModuleProcessor().o();
        Fonts.c.a(matrices, this.b.i(), this.a.x, this.a.y, 6.25f, ColorUtil.applyAlphaToColor(theme.a(ThemeInfo.TEXT).toIntColor(), extend));
        float x = this.a.x;
        float y = this.a.y + Fonts.c.a(6.25f) + 5.0f;
        int i = 0;
        this.hoveredMode = null;
        for (String mode : this.b.k()) {
            float width = Fonts.c.a(mode, 6.0f) + 8.0f;
            if (x + width > this.a.x + this.a.z) {
                x = this.a.x;
                y += 13.0f;
            }
            boolean hovered = MathUtil.a(mouseX, mouseY, x, y, width, 11.0f);
            if (hovered) {
                this.hoveredMode = mode;
                this.hoverX = x + width * 0.5f;
                this.hoverY = y;
            }
            this.modeAnimations[i].a(this.b.l(mode) || hovered);
            this.modeAnimations[i].a(0.0f, 1.0f, 0.3f, EasingList.i, delta);
            float value = this.modeAnimations[i].c();

            int activeBg = ColorUtil.applyAlphaToColor(theme.a(ThemeInfo.PRIMARY).toIntColor(), 0.20f * value * extend);
            int inactiveBg = ColorUtil.applyAlphaToColor(ColorUtil.convertToARGB(25, 28, 38, 255), extend);
            int bg = ColorUtil.lerpColor(inactiveBg, activeBg, value);

            draw.a(matrices, x, y, width, 11.0f, 3.0f, bg);
            draw.a(matrices, x, y, width, 11.0f, 3.0f, 0.5f, ColorUtil.applyAlphaToColor(value > 0.1f ? theme.a(ThemeInfo.PRIMARY).toIntColor() : theme.a(ThemeInfo.OUTLINE_SMALL).toIntColor(), (theme.a(ThemeInfo.OUTLINE_SMALL).getAlphaFloat() + 0.3f * value) * extend));
            int color = ColorUtil.lerpColor(theme.a(ThemeInfo.TEXT_DISABLED).toIntColor(), ColorUtil.convertToARGB(255, 255, 255, 255), value);
            Fonts.c.b(matrices, mode, x + (width / 2.0f), (y + ((11.0f - Fonts.c.a(6.0f)) / 2.0f)) - 0.75f, 6.0f, ColorUtil.applyAlphaToColor(color, extend));
            x += width + 3.0f;
            i++;
        }
        this.a.w = (y + 11.0f) - this.a.y;

        if (this.hoveredMode != null && CosmeticPreview.isCosmeticStyle(this.b.i()) && extend > 0.4f) {
            this.spin += delta * 48.0f;
            if (this.spin > 3600.0f) {
                this.spin -= 3600.0f;
            }
            CosmeticPreview.draw(draw, matrices, this.b.i(), this.hoveredMode, this.hoverX, this.hoverY, 54.0f, this.spin, theme.a(ThemeInfo.PRIMARY).toIntColor(), extend);
        }
    }
}
