package platform.inject.mixin;

import aethereal.mixin.ICustomTotemState;
import aethereal.module.render.CustomTotem;
import aethereal.module.render.TotemModels;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ModelTransformationMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemRenderState.class)
public abstract class ItemRenderStateMixin implements ICustomTotemState {
    @Shadow
    ModelTransformationMode modelTransformationMode;

    @Shadow
    boolean leftHand;

    @Shadow
    public abstract net.minecraft.client.render.model.json.Transformation getTransformation();

    @Unique
    private boolean malice$totem;

    @Override
    public boolean malice$isTotem() {
        return this.malice$totem;
    }

    @Override
    public void malice$setTotem(boolean totem) {
        this.malice$totem = totem;
    }

    @Inject(method = "clear", at = @At("TAIL"))
    private void malice$clearTotem(CallbackInfo ci) {
        this.malice$totem = false;
    }

    @Inject(method = "hasDepth", at = @At("HEAD"), cancellable = true)
    private void malice$totemDepth(CallbackInfoReturnable<Boolean> cir) {
        if (this.malice$totem) {
            cir.setReturnValue(Boolean.TRUE);
        }
    }

    @Inject(method = "isSideLit", at = @At("HEAD"), cancellable = true)
    private void malice$totemLit(CallbackInfoReturnable<Boolean> cir) {
        if (this.malice$totem) {
            cir.setReturnValue(Boolean.TRUE);
        }
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void malice$renderTotem(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay, CallbackInfo ci) {
        if (!this.malice$totem) {
            return;
        }
        CustomTotem module = CustomTotem.current();
        if (module == null || !module.m()) {
            return;
        }
        matrices.push();
        this.getTransformation().apply(this.leftHand, matrices);
        TotemModels.render(module.style(), matrices, vertexConsumers, this.modelTransformationMode, module.scale(), module.animate(), module.accentColor(), light, overlay);
        matrices.pop();
        ci.cancel();
    }
}
