package net.nightshade.divinity_engine.network.events.divinity.gods;

import net.minecraft.world.entity.Entity;
import net.nightshade.divinity_engine.divinity.gods.BaseGodInstance;

public class ContactGodEvent extends GodsEvent{
    private final Entity entity;
    public ContactGodEvent(BaseGodInstance instance, Entity entity) {
        super(instance);
        this.entity = entity;
    }

    public Entity getEntity(){
        return this.entity;
    }
}
