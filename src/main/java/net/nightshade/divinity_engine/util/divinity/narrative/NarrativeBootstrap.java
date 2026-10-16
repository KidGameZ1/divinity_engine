package net.nightshade.divinity_engine.util.divinity.narrative;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.nightshade.divinity_engine.commands.DivineStoryCommands;
import net.nightshade.divinity_engine.network.cap.player.narrative.DivineNarrativeCapability;
import net.nightshade.divinity_engine.network.events.divinity.narrative.DivineNarrativeEvents;
import net.nightshade.divinity_engine.network.messages.narrative.NarrativeNetwork;

/** Integration gate: call once from the mod constructor only after the standalone Core stage is validated. */
public final class NarrativeBootstrap {
    private static boolean registered;
    public static void register(IEventBus modBus) {
        if (registered) return; registered = true;
        DivineNarrativeCapability.init(modBus);
        modBus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(() -> {
            DivineNarrativeRegistry.initialize();
            NarrativeNetwork.register();
        }));
        MinecraftForge.EVENT_BUS.register(DivineNarrativeEvents.class);
        MinecraftForge.EVENT_BUS.addListener(DivineStoryCommands::register);
        MinecraftForge.EVENT_BUS.addListener((AddReloadListenerEvent event) -> event.addListener(new NarrativeReloadListener()));
    }
}
