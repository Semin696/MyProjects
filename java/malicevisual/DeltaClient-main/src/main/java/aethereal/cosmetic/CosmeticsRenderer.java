package aethereal.cosmetic;

import aethereal.core.EventManager;
import aethereal.core.Skeleton;
import aethereal.core.Interface;
import aethereal.event.PlayerCosmeticFeatureEvent;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.UUID;

public class CosmeticsRenderer extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> implements Interface {
    public CosmeticsRenderer(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> context) {
        super(context);
    }

    public void render(MatrixStack matrices, VertexConsumerProvider buffers, int light, PlayerEntityRenderState state, float limbAngle, float limbDistance) {
        if (mc.world == null) {
            return;
        }
        Entity entity = mc.world.getEntityById(state.id);
        if (entity == null) {
            return;
        }
        float tickDelta = mc.getRenderTickCounter().getTickDelta(false);
        if (entity instanceof PlayerEntity player) {
            EventManager.a(new PlayerCosmeticFeatureEvent(matrices, buffers, player, getContextModel(), light, tickDelta));
        }
        UUID uuid = entity.getUuid();
        for (Cosmetic cosmetic : Skeleton.getInstance().getModuleProcessor().r().getCosmetics()) {
            if (uuid.equals(cosmetic.getUuid()) && cosmetic.getType() == CosmeticsType.COSMETIC) {
                matrices.push();
                cosmetic.getCategory().transform(matrices, getContextModel(), cosmetic);
                cosmetic.getRenderer().render(matrices, cosmetic, buffers, null, null, light, tickDelta);
                matrices.pop();
            }
        }
    }
}
