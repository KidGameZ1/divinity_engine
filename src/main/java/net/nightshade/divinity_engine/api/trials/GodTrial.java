package net.nightshade.divinity_engine.api.trials;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.nightshade.divinity_engine.divinity.gods.BaseGod;
import net.nightshade.divinity_engine.divinity.trial.GodlyTrial;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Function;

@ApiStatus.NonExtendable
public class GodTrial {
    public BaseGod getGod() {
        return god;
    }

    public Function<ServerPlayer, GodlyTrial> getFactory() {
        return factory;
    }

    private final BaseGod god;
    private final Function<ServerPlayer, GodlyTrial> factory;
    public GodTrial(BaseGod god, Function<ServerPlayer, GodlyTrial> factory) {
        this.god = god;
        this.factory = factory;
    }

}
