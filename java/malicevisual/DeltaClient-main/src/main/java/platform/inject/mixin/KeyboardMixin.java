package platform.inject.mixin;

import aethereal.core.EventManager;
import aethereal.core.Interface;
import aethereal.event.KeyEvent;
import net.minecraft.client.Keyboard;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Keyboard.class})
public class KeyboardMixin {
    @Inject(method = {"onKey"}, at = {@At("HEAD")}, cancellable = true)
    public void onKey(long window, int key, int scanCode, int action, int modifiers, CallbackInfo ci) {
        if (Interface.mc.currentScreen == null || (Interface.mc.currentScreen instanceof HandledScreen)) {
            KeyEvent event = new KeyEvent(key, scanCode, action, modifiers);
            EventManager.a(event);
            if (event.a()) {
                ci.cancel();
            }
        }
    }
}
