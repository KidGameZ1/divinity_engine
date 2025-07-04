package net.nightshade.divinity_engine.network.events.divinity.curse;

import net.minecraft.world.entity.Entity;
import net.nightshade.divinity_engine.divinity.curse.CurseInstance;

public class UncursePlayerEvent extends CurseEvent {
    private final Entity entity;
    public UncursePlayerEvent(CurseInstance instance, Entity entity) {
        super(instance);
        this.entity = entity;
    }

    public Entity getEntity(){
        return this.entity;
    }
}
