package platform.inject.mixin;

import aethereal.core.EventManager;
import aethereal.core.Interface;
import aethereal.event.RemovalsEvent;
import aethereal.module.misc.Optimization;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.render.Fog;
import net.minecraft.client.render.FrameGraphBuilder;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({WorldRenderer.class})
public class WorldRendererMixin implements Interface {
    @Inject(method = {"renderWeather"}, at = {@At("HEAD")}, cancellable = true)
    private void onRenderWeather(FrameGraphBuilder frameGraphBuilder, Vec3d pos, float tickDelta, Fog fog, CallbackInfo ci) {
        if (Optimization.shouldSkipWeather()) {
            ci.cancel();
            return;
        }
        RemovalsEvent event = new RemovalsEvent(RemovalsEvent.type.WEATHER);
        EventManager.a(event);
        if (event.a()) {
            ci.cancel();
        }
    }

    @Inject(method = {"renderClouds"}, at = {@At("HEAD")}, cancellable = true)
    private void onRenderClouds(FrameGraphBuilder builder, Matrix4f positionMatrix, Matrix4f projectionMatrix, CloudRenderMode cloudRenderMode, Vec3d cameraPos, float cloudsHeight, int ticks, float cloudiness, CallbackInfo ci) {
        if (Optimization.shouldSkipClouds()) {
            ci.cancel();
        }
    }
}
