package platform.inject.mixin;


import aethereal.core.EventManager;
import aethereal.event.RemovalsEvent;
import aethereal.module.misc.Optimization;
import net.minecraft.block.BlockState;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ParticleManager.class})
public abstract class ParticleManagerMixin {
    @Inject(method = {"addBlockBreakParticles"}, at = {@At("HEAD")}, cancellable = true)
    private void onAddBlockBreakParticles(BlockPos blockPos, BlockState state, CallbackInfo info) {
        if (Optimization.shouldSkipBreakParticles()) {
            info.cancel();
            return;
        }
        RemovalsEvent event = new RemovalsEvent(RemovalsEvent.type.BREAK_PARTICLES);
        EventManager.a(event);
        if (event.a()) {
            info.cancel();
        }
    }

    @Inject(method = {"addBlockBreakingParticles"}, at = {@At("HEAD")}, cancellable = true)
    private void onAddBlockBreakingParticles(BlockPos blockPos, Direction direction, CallbackInfo info) {
        if (Optimization.shouldSkipBreakParticles()) {
            info.cancel();
            return;
        }
        RemovalsEvent event = new RemovalsEvent(RemovalsEvent.type.BREAK_PARTICLES);
        EventManager.a(event);
        if (event.a()) {
            info.cancel();
        }
    }

    @Inject(method = {"tickParticle"}, at = {@At("HEAD")}, cancellable = true)
    private void maliceSafeTickParticle(Particle particle, CallbackInfo ci) {
        ci.cancel();
        if (particle == null) {
            return;
        }
        try {
            particle.tick();
        } catch (Throwable ignored) {
            particle.markDead();
        }
    }

    @Inject(method = {"addParticle*"}, at = {@At("HEAD")}, cancellable = true)
    private void onAddParticle(ParticleEffect parameters, double x, double y, double z, double velocityX, double velocityY, double velocityZ, CallbackInfoReturnable<Particle> cir) {
        if (Optimization.shouldCancelParticle(parameters)) {
            cir.cancel();
            return;
        }
        if (parameters.getType() == ParticleTypes.RAIN) {
            RemovalsEvent event = new RemovalsEvent(RemovalsEvent.type.WEATHER);
            EventManager.a(event);
            if (event.a()) {
                cir.cancel();
            }
        }
    }
}
