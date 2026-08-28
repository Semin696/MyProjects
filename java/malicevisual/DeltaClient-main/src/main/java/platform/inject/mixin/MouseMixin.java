package platform.inject.mixin;

import aethereal.core.EventManager;
import aethereal.core.Interface;
import aethereal.event.ClickEvent;
import aethereal.event.KeyEvent;
import aethereal.event.LookEvent;
import aethereal.event.ScrollEvent;
import aethereal.module.player.Zoom;
import aethereal.util.KeyUtil;
import net.minecraft.client.Mouse;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Mouse.class})
public class MouseMixin {
    @Inject(method = {"onMouseButton"}, at = {@At("HEAD")}, cancellable = true)
    public void onMouseButton(long window, int button, int action, int modifiers, CallbackInfo ci) {
        if (Interface.mc.currentScreen == null) {
            EventManager.a(new KeyEvent((button < 0 || button > 7) ? button : (-100) + button, 0, action, modifiers));
        }
        if (action == 1) {
            ClickEvent event = new ClickEvent(Interface.mc.mouse.getX() / 2.0d, Interface.mc.mouse.getY() / 2.0d, button, ClickEvent.a.PRESS);
            EventManager.a(event);
            if (event.a()) {
                ci.cancel();
                return;
            }
            return;
        }
        if (action == 0) {
            ClickEvent event2 = new ClickEvent(Interface.mc.mouse.getX() / 2.0d, Interface.mc.mouse.getY() / 2.0d, button, ClickEvent.a.RELEASE);
            EventManager.a(event2);
            if (event2.a()) {
                ci.cancel();
            }
        }
    }

    @Inject(method = {"onCursorPos"}, at = {@At("HEAD")}, cancellable = true)
    public void onCursorPos(long window, double x, double y, CallbackInfo ci) {
        ClickEvent event = new ClickEvent(Interface.mc.mouse.getX() / 2.0d, Interface.mc.mouse.getY() / 2.0d, 0, ClickEvent.a.DRAG);
        EventManager.a(event);
        if (event.a()) {
            ci.cancel();
        }
    }

    @Inject(method = {"onMouseScroll"}, at = {@At("HEAD")}, cancellable = true)
    private void maliceZoomScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (vertical != 0.0d && Interface.mc.currentScreen == null) {
            int key = KeyUtil.fromScroll(vertical);
            KeyEvent press = new KeyEvent(key, 0, 1, 0);
            EventManager.a(press);
            KeyEvent release = new KeyEvent(key, 0, 0, 0);
            EventManager.a(release);
            if (press.a() || release.a()) {
                ci.cancel();
                return;
            }
        }
        if (Zoom.onScroll(vertical)) {
            ci.cancel();
        }
    }

    @Inject(method = {"onMouseScroll"}, at = {@At("RETURN")})
    private void onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        EventManager.a(new ScrollEvent(horizontal, vertical));
    }

    @Redirect(method = {"updateMouse"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;changeLookDirection(DD)V"))
    private void redirectChangeLookDirection(ClientPlayerEntity player, double yaw, double pitch) {
        LookEvent event = new LookEvent((float) yaw, (float) pitch);
        EventManager.a(event);
        if (!event.a()) {
            float factor = Zoom.sensitivity();
            player.changeLookDirection(yaw * ((double) factor), pitch * ((double) factor));
        }
    }
}
