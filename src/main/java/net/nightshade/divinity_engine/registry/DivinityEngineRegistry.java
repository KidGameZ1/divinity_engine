package net.nightshade.divinity_engine.registry;

import net.minecraftforge.eventbus.api.IEventBus;
import net.nightshade.divinity_engine.network.cap.CapabilityHandler;
import net.nightshade.divinity_engine.network.cap.player.trials.TrialPlayerCapability;
import net.nightshade.divinity_engine.registry.arg.DivinityEngineArgumentTypes;
import net.nightshade.divinity_engine.registry.aspects.AspectsRegistry;
import net.nightshade.divinity_engine.registry.divinity.blessing.BlessingsRegistry;
import net.nightshade.divinity_engine.registry.divinity.curse.CurseRegistry;
import net.nightshade.divinity_engine.registry.divinity.gods.GodsRegistry;
import net.nightshade.divinity_engine.registry.gui.MenusRegistry;
import net.nightshade.divinity_engine.registry.tabs.TabsRegistry;

public class DivinityEngineRegistry {
    public DivinityEngineRegistry(){
    }

    public static void register(IEventBus bus) {
        BlessingsRegistry.register(bus);
        CurseRegistry.register(bus);
        GodsRegistry.register(bus);

        DivinityEngineArgumentTypes.init(bus);
        // GodsRegistry attaches the shared content registers, including automatic statues.
        TabsRegistry.init(bus);
        MenusRegistry.init(bus);
        CapabilityHandler.init(bus);
        TrialPlayerCapability.init(bus);
        AspectsRegistry.init();

    }

}
