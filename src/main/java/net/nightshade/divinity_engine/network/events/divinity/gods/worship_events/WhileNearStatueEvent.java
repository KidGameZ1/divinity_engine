package net.nightshade.divinity_engine.network.events.divinity.gods.worship_events;

import net.minecraft.world.entity.LivingEntity;
import net.nightshade.divinity_engine.divinity.gods.BaseGodInstance;
import net.nightshade.divinity_engine.network.events.divinity.gods.GodsEvent;

public class WhileNearStatueEvent extends GodsEvent {
    private final LivingEntity living;

    public WhileNearStatueEvent(BaseGodInstance instance, LivingEntity living) {
        super(instance);
        this.living = living;
    }

    public LivingEntity getLiving() {
        return living;
    }
}
