package aethereal.event;

import aethereal.core.Event;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;

public class DeathEvent extends Event {
    private final LivingEntity entity;
    private final DamageSource source;

    public DeathEvent(LivingEntity entity, DamageSource source) {
        this.entity = entity;
        this.source = source;
    }

    public LivingEntity getEntity() {
        return this.entity;
    }

    public DamageSource getSource() {
        return this.source;
    }
}
