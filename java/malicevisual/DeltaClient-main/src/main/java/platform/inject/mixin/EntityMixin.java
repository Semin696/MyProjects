package platform.inject.mixin;

import aethereal.core.EventManager;
import aethereal.core.Interface;
import aethereal.core.Skeleton;
import aethereal.event.BoundingBoxEvent;
import aethereal.event.RemovalsEvent;
import aethereal.friend.FriendProcessor;
import aethereal.network.MaliceUsers;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Entity.class})
public abstract class EntityMixin {

    @Shadow
    private Box boundingBox;

    @Inject(method = {"getBoundingBox"}, at = {@At("HEAD")}, cancellable = true)
    public final void getBoundingBox(CallbackInfoReturnable<Box> cir) {
        BoundingBoxEvent event = new BoundingBoxEvent(this.boundingBox, (Entity) (Object) this);
        EventManager.a(event);
        cir.setReturnValue(event.getBox());
    }

    @Inject(method = {"isGlowing"}, at = {@At("RETURN")}, cancellable = true)
    private void onIsGlowing(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue().booleanValue()) {
            RemovalsEvent event = new RemovalsEvent(RemovalsEvent.type.GLOW);
            EventManager.a(event);
            if (event.a()) {
                cir.setReturnValue(false);
            }
        }
        Entity self = (Entity) (Object) this;
        if (self instanceof PlayerEntity player && Interface.mc != null && player != Interface.mc.player
                && Skeleton.getInstance() != null && Skeleton.getInstance().getModuleProcessor() != null) {
            FriendProcessor friends = Skeleton.getInstance().getModuleProcessor().e();
            if (friends != null && friends.highlight() && friends.d(player.getName().getString())) {
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = {"pushAwayFrom"}, at = {@At("HEAD")}, cancellable = true)
    private void onPushAwayFrom(Entity other, CallbackInfo ci) {
        if (Interface.mc == null || Interface.mc.player == null || Skeleton.getInstance() == null || Skeleton.getInstance().getModuleProcessor() == null) {
            return;
        }
        FriendProcessor friends = Skeleton.getInstance().getModuleProcessor().e();
        if (friends == null || !friends.noPush()) {
            return;
        }
        Entity self = (Entity) (Object) this;
        Entity candidate = self == Interface.mc.player ? other : (other == Interface.mc.player ? self : null);
        if (candidate instanceof PlayerEntity player && friends.d(player.getName().getString())) {
            ci.cancel();
        }
    }

    @Inject(method = {"getDisplayName"}, at = {@At("RETURN")}, cancellable = true)
    private void maliceDisplayName(CallbackInfoReturnable<Text> cir) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof PlayerEntity player)) {
            return;
        }
        if (!MaliceUsers.is(player.getUuid())) {
            return;
        }
        Text original = cir.getReturnValue();
        Text decorated = MaliceUsers.decorate(original);
        if (decorated != original) {
            cir.setReturnValue(decorated);
        }
    }
}
