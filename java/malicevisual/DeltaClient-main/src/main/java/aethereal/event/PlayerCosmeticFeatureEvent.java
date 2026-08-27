package aethereal.event;

import aethereal.core.Event;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;

public class PlayerCosmeticFeatureEvent extends Event {
    private final MatrixStack matrices;
    private final VertexConsumerProvider buffers;
    private final PlayerEntity player;
    private final PlayerEntityModel model;
    private final int light;
    private final float tickDelta;

    public PlayerCosmeticFeatureEvent(MatrixStack matrices, VertexConsumerProvider buffers, PlayerEntity player,
                                      PlayerEntityModel model, int light, float tickDelta) {
        this.matrices = matrices;
        this.buffers = buffers;
        this.player = player;
        this.model = model;
        this.light = light;
        this.tickDelta = tickDelta;
    }

    public MatrixStack getMatrices() {
        return this.matrices;
    }

    public VertexConsumerProvider getBuffers() {
        return this.buffers;
    }

    public PlayerEntity getPlayer() {
        return this.player;
    }

    public PlayerEntityModel getModel() {
        return this.model;
    }

    public int getLight() {
        return this.light;
    }

    public float getTickDelta() {
        return this.tickDelta;
    }
}
