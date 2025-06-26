package net.nightshade.divinity_engine.network.events.divinity.gods;

import net.minecraftforge.eventbus.api.Event;
import net.nightshade.divinity_engine.divinity.gods.BaseGodInstance;

public class GodsEvent extends Event {
    private final BaseGodInstance instance;

    public GodsEvent(BaseGodInstance instance) {
        this.instance = instance;
    }

    public BaseGodInstance getInstance() {
        return instance;
    }
}
