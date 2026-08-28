package platform.inject.mixin;

import aethereal.mixin.ICustomTotemState;
import aethereal.module.render.CustomTotem;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemModelManager.class)
public class ItemModelManagerMixin {
    @Inject(method = "update(Lnet/minecraft/client/render/item/ItemRenderState;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ModelTransformationMode;ZLnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;I)V", at = @At("TAIL"))
    private void malice$tagTotemHand(ItemRenderState state, ItemStack stack, ModelTransformationMode mode, boolean leftHand, World world, LivingEntity entity, int seed, CallbackInfo ci) {
        ((ICustomTotemState) state).malice$setTotem(CustomTotem.shouldReplace(stack, mode));
    }

    @Inject(method = "update(Lnet/minecraft/client/render/item/ItemRenderState;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ModelTransformationMode;Lnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;I)V", at = @At("TAIL"))
    private void malice$tagTotem(ItemRenderState state, ItemStack stack, ModelTransformationMode mode, World world, LivingEntity entity, int seed, CallbackInfo ci) {
        ((ICustomTotemState) state).malice$setTotem(CustomTotem.shouldReplace(stack, mode));
    }
}
