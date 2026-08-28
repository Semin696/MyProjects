package platform.inject.mixin;


import aethereal.core.EventManager;
import aethereal.core.Interface;
import aethereal.event.PushEvent;
import aethereal.network.MaliceUsers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({PlayerEntity.class})
public class PlayerEntityMixin {
    @Inject(method = {"isPushedByFluids"}, at = {@At("HEAD")}, cancellable = true)
    private void removePushFromFluids(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this == Interface.mc.player) {
            PushEvent event = new PushEvent(PushEvent.type.FLUIDS);
            EventManager.a(event);
            if (event.a()) {
                cir.setReturnValue(false);
            }
        }
    }

    @Inject(method = {"getDisplayName"}, at = {@At("RETURN")}, cancellable = true)
    private void maliceDisplayName(CallbackInfoReturnable<Text> cir) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        if (!MaliceUsers.is(self.getUuid())) {
            return;
        }
        Text original = cir.getReturnValue();
        Text decorated = MaliceUsers.decorate(original);
        if (decorated != original) {
            cir.setReturnValue(decorated);
        }
    }
}
