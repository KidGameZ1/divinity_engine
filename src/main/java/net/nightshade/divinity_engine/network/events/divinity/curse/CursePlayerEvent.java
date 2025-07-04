package net.nightshade.divinity_engine.network.events.divinity.curse;

import net.minecraft.world.entity.Entity;
import net.nightshade.divinity_engine.divinity.curse.CurseInstance;
import net.nightshade.divinity_engine.divinity.gods.BaseGod;
import net.nightshade.divinity_engine.divinity.gods.BaseGodInstance;
import net.nightshade.divinity_engine.network.events.divinity.gods.GodsEvent;
import net.nightshade.divinity_engine.util.divinity.gods.GodHelper;

public class CursePlayerEvent extends CurseEvent {
    private final Entity entity;
    public CursePlayerEvent(CurseInstance instance, Entity entity) {
        super(instance);
        this.entity = entity;
    }
    public CursePlayerEvent(CurseInstance instance, BaseGod toBound, Entity entity) {
        super(instance);
        this.entity = entity;
    }
    public Entity getEntity(){
        return this.entity;
    }
}
