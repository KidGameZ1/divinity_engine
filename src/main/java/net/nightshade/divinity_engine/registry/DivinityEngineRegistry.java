package net.nightshade.divinity_engine.registry;

import net.minecraftforge.eventbus.api.IEventBus;
import net.nightshade.divinity_engine.network.cap.CapabilityHandler;
import net.nightshade.divinity_engine.registry.arg.DivinityEngineArgumentTypes;
import net.nightshade.divinity_engine.registry.blocks.BlocksRegistry;
import net.nightshade.divinity_engine.registry.blocks.entity.BlockEntitesRegistry;
import net.nightshade.divinity_engine.registry.divinity.blessing.BlessingsRegistry;
import net.nightshade.divinity_engine.registry.divinity.curse.CurseRegistry;
import net.nightshade.divinity_engine.registry.divinity.gods.GodsRegistry;
import net.nightshade.divinity_engine.registry.gui.MenusRegistry;
import net.nightshade.divinity_engine.registry.item.ItemsRegistry;
import net.nightshade.divinity_engine.registry.keys.DivinityEngineKeyMappings;
import net.nightshade.divinity_engine.registry.tabs.TabsRegistry;

public class DivinityEngineRegistry {
    public DivinityEngineRegistry(){
    }

    public static void register(IEventBus bus) {
        BlessingsRegistry.register(bus);
        CurseRegistry.register(bus);
        GodsRegistry.register(bus);

        // Register key mappings
        bus.register(DivinityEngineKeyMappings.class);


        DivinityEngineArgumentTypes.init(bus);
        ItemsRegistry.init(bus);
        BlocksRegistry.init(bus);
        BlockEntitesRegistry.init(bus);
        TabsRegistry.init(bus);
        MenusRegistry.init(bus);
        CapabilityHandler.init(bus);
    }

}
