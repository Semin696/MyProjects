package platform.inject.mixin;

import aethereal.module.misc.Optimization;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.world.WorldView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {
    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void maliceSkipEntity(Entity entity, Frustum frustum, double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
        if (Optimization.shouldHideEntity(entity)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "renderShadow", at = @At("HEAD"), cancellable = true)
    private static void maliceSkipShadow(MatrixStack matrices, VertexConsumerProvider vertexConsumers, EntityRenderState renderState, float opacity, float tickDelta, WorldView world, float radius, CallbackInfo ci) {
        if (Optimization.shouldSkipShadows()) {
            ci.cancel();
        }
    }
}
