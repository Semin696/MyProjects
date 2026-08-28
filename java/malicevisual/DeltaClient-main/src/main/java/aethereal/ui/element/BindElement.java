package aethereal.ui.element;


import aethereal.config.ThemeInfo;
import aethereal.config.ThemeProcessor;
import aethereal.core.Skeleton;
import aethereal.render.*;
import aethereal.setting.BindSetting;
import aethereal.util.KeyUtil;
import aethereal.util.MathUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Vector4f;

public class BindElement extends Element<BindSetting> {
    private boolean isListening;

    public BindElement(BindSetting setting) {
        super(setting);
        this.a.w = 11.0f;
    }

    @Override

    public boolean onMouseClick(double mouseX, double mouseY, int button) {
        Vector4f vector4f = this.a;
        var setting = this.b;
        if (this.isListening) {
            if (!(setting instanceof BindSetting)) {
                throw new ClassCastException();
            }
            setting.a(Integer.valueOf(-100 + button));
            this.isListening = false;
            return true;
        }
        if (!MathUtil.a(mouseX, mouseY, vector4f.x, vector4f.y, vector4f.z, vector4f.w)) {
            return false;
        }
        if (button == 0) {
            this.isListening = true;
            return true;
        }
        if (button != 2) {
            return false;
        }
        if (!(setting instanceof BindSetting)) {
            throw new ClassCastException();
        }
        setting.b();
        return true;
    }

    @Override
    public boolean onMouseScroll(double mouseX, double mouseY, double amount) {
        if (!this.isListening) {
            return false;
        }
        int wheel = KeyUtil.fromScroll(amount);
        if (wheel == KeyUtil.UNKNOWN.a()) {
            return false;
        }
        this.b.a(Integer.valueOf(wheel));
        this.isListening = false;
        return true;
    }

    @Override

    public boolean onKeyPress(int keyCode, int scanCode, int modifiers) {
        if (!this.isListening) {
            return false;
        }
        var setting = this.b;
        if (!(setting instanceof BindSetting)) {
            throw new ClassCastException();
        }
        if (keyCode == 256 || keyCode == 259) {
            setting.b();
        } else {
            setting.a(Integer.valueOf(keyCode));
        }
        this.isListening = false;
        return true;
    }

    @Override
    public void render(DrawContext context, double mouseX, double mouseY, float delta, float extend) {
        MatrixStack matrices = context.getMatrices();
        Draw2DProcessor draw = Skeleton.getInstance().getModuleProcessor().i();
        ThemeProcessor theme = Skeleton.getInstance().getModuleProcessor().o();
        getActivationAnimation().a(this.isListening);
        getActivationAnimation().a(0.0f, 1.0f, 0.4f, EasingList.p, delta);
        float centerY = this.a.y + (this.a.w / 2.0f);
        boolean hovered = MathUtil.a(mouseX, mouseY, this.a.x, this.a.y, this.a.z, this.a.w) && extend >= 1.0f;
        float anim = getActivationAnimation().c();
        float reverse = 1.0f - anim;
        String value = this.b.c().intValue() == -1 ? "None" : KeyUtil.b(this.b.c().intValue());
        float total = (Fonts.c.a(value, 6.0f) * reverse) + (Fonts.c.a("...", 6.0f) * anim);
        float boxWidth = total + 8.0f;
        float boxHeight = 9.0f;
        float boxX = (this.a.x + this.a.z) - boxWidth;
        float boxY = centerY - (boxHeight / 2.0f);
        float textY = (boxY + ((boxHeight - Fonts.c.a(6.0f)) / 2.0f)) - 0.75f;
        
        drawLabel(matrices, Fonts.c, this.b.i(), this.a.x, this.a.y, this.a.w, 6.25f, theme.a(ThemeInfo.TEXT).toIntColor(), (boxX - this.a.x) - 4.0f, hovered, extend, delta);
        
        int bgIdle = ColorUtil.applyAlphaToColor(ColorUtil.convertToARGB(25, 28, 38, 255), extend);
        draw.a(matrices, boxX, boxY, boxWidth, boxHeight, 2.5f, bgIdle);
        draw.a(matrices, boxX, boxY, boxWidth, boxHeight, 2.5f, 0.5f, ColorUtil.applyAlphaToColor(anim > 0.1f ? theme.a(ThemeInfo.PRIMARY).toIntColor() : theme.a(ThemeInfo.OUTLINE_SMALL).toIntColor(), (theme.a(ThemeInfo.OUTLINE_SMALL).getAlphaFloat() + 0.3f * anim) * extend));
        ScissorUtil.a(matrices, boxX, boxY, boxWidth, boxHeight);
        if (reverse > 0.0f) {
            Fonts.c.a(matrices, value, boxX + 4.0f, textY, 6.0f, ColorUtil.applyAlphaToColor(theme.a(ThemeInfo.TEXT).toIntColor(), extend * reverse));
        }
        if (anim > 0.0f) {
            Fonts.c.a(matrices, "...", boxX + 4.0f, textY, 6.0f, ColorUtil.applyAlphaToColor(theme.a(ThemeInfo.PRIMARY).toIntColor(), extend * anim));
        }
        ScissorUtil.a(matrices);
    }
}
