package com.nightfallrealm.mod;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class TitleScreenMixin {
    @Inject(at = @At("HEAD"), method = "tick")
    private void onTick(CallbackInfo info) {
        MinecraftClient client = (MinecraftClient) (Object) this;
        if (client.currentScreen instanceof TitleScreen
                && !(client.currentScreen instanceof NightfallTitleScreen)) {
            client.setScreen(new NightfallTitleScreen());
        }
    }
}
