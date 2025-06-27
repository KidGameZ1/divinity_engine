package net.nightshade.divinity_engine.registry;

import net.minecraftforge.eventbus.api.IEventBus;
import net.nightshade.divinity_engine.network.cap.CapabilityHandler;
import net.nightshade.divinity_engine.registry.blocks.BlocksRegistry;
import net.nightshade.divinity_engine.registry.divinity.blessing.BlessingsRegistry;
import net.nightshade.divinity_engine.registry.divinity.gods.GodsRegistry;
import net.nightshade.divinity_engine.registry.gui.MenusRegistry;
import net.nightshade.divinity_engine.registry.item.ItemsRegistry;

public class DivinityEngineRegistry {
    public DivinityEngineRegistry(){
    }

    public static void register(IEventBus bus) {
        BlessingsRegistry.register(bus);
        GodsRegistry.register(bus);
        CapabilityHandler.init(bus);


        DivinityEngineArgumentTypes.init(bus);
        ItemsRegistry.init(bus);
        BlocksRegistry.init(bus);
        MenusRegistry.init(bus);
    }

}
