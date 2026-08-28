package aethereal.render;

import aethereal.config.BaseProcessor;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.texture.Sprite;
import net.minecraft.item.ItemStack;

public class Draw3DProcessor extends BaseProcessor {
    @Override
    public void setup() {
    }

    @Override
    public void unSetup() {
    }

    public void a(DrawContext context, ItemStack stack, float x, float y, int z, float alpha, float scale, boolean overlay) {
        if (!stack.isEmpty()) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            context.getMatrices().push();
            context.getMatrices().translate(x, y, z);
            context.getMatrices().scale(scale, scale, 1.0f);
            RenderSystem.setShaderColor(alpha, alpha, alpha, alpha);
            context.drawItem(stack, 0, 0);
            if (overlay) {
                context.drawStackOverlay(mc.textRenderer, stack, 0, 0);
            }
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            context.getMatrices().pop();
        }
    }

    public void a(DrawContext context, Sprite sprite, float x, float y, float z, float scale, float alpha) {
        context.getMatrices().push();
        context.getMatrices().translate(x, y, z);
        context.getMatrices().scale(scale, scale, 1.0f);
        context.drawSpriteStretched(RenderLayer::getGuiTextured, sprite, 0, 0, 18, 18, ColorUtil.applyAlphaToColor(-1, alpha));
        context.getMatrices().pop();
    }
}
