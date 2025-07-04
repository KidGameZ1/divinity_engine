package net.nightshade.divinity_engine.network.events.divinity.curse;

import net.minecraftforge.eventbus.api.Event;
import net.nightshade.divinity_engine.divinity.curse.CurseInstance;
import net.nightshade.divinity_engine.divinity.gods.BaseGodInstance;

public class CurseEvent extends Event {
    private final CurseInstance instance;

    public CurseEvent(CurseInstance instance) {
        this.instance = instance;
    }

    public CurseInstance getInstance() {
        return instance;
    }
}
