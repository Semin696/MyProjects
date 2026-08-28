package platform.inject.mixin;

import aethereal.core.EventManager;
import aethereal.core.Interface;
import aethereal.event.AttackEvent;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ClientPlayerInteractionManager.class})
public class ClientPlayerInteractionManagerMixin implements Interface {

    @Inject(method = {"attackEntity"}, at = {@At("HEAD")}, cancellable = true)
    private void onAttackEntity(PlayerEntity player, Entity target, CallbackInfo ci) {
        AttackEvent event = new AttackEvent(target);
        EventManager.a(event);
        if (event.a()) {
            ci.cancel();
        }
    }
}
