package net.nightshade.divinity_engine;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.util.thread.SidedThreadGroups;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.nightshade.divinity_engine.block.StatueBlock;
import net.nightshade.divinity_engine.client.trial.TrialHudOverlay;
import net.nightshade.divinity_engine.network.cap.CapabilityHandler;
import net.nightshade.divinity_engine.network.messages.ModMessages;
import net.nightshade.divinity_engine.registry.DivinityEngineRegistry;
import net.nightshade.divinity_engine.registry.blocks.BlocksRegistry;
import net.nightshade.divinity_engine.registry.divinity.gods.GodsRegistry;
import net.nightshade.divinity_engine.registry.divinity.trial.GodlyTrialsRegistry;
import net.nightshade.divinity_engine.registry.gui.DivinityUiTypes;
import net.nightshade.divinity_engine.util.divinity.narrative.NarrativeBootstrap;
import net.nightshade.nightshade_core.util.MiscHelper;
import org.slf4j.Logger;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(DivinityEngineMod.MODID)
public class DivinityEngineMod {

    public static final String MODID = "divinity_engine";
    public static final Logger LOGGER = LogUtils.getLogger();
    public DivinityEngineMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);
        DivinityEngineRegistry.register(bus);
        net.nightshade.divinity_engine.api.content.DivinityContent.bootstrap(bus);
        net.nightshade.divinity_engine.gametest.ContentDefinitionGameTests.registerFixtures(bus);
        net.nightshade.divinity_engine.gametest.StatuePersistenceGameTests.registerFixtures(bus);
        bus.addListener(CapabilityHandler::registerCapabilities);
        NarrativeBootstrap.register(bus);
        net.nightshade.divinity_engine.util.divinity.innerworld.CommunionBootstrap.register(bus);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            DivinityUiTypes.register();
            net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals.register();
            registerDCPages();
            GodlyTrialsRegistry.initializeTrials();
            net.nightshade.divinity_engine.api.content.DivinityContent.initialize();
        });
        ModMessages.register();

    }

    private static void registerDCPages(){

    }


    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {

    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
        }

        @SubscribeEvent
        public static void registerGuiOverlays(RegisterGuiOverlaysEvent event) {
            event.registerAboveAll("trial_hud", TrialHudOverlay.OVERLAY);
        }
    }

    private static final Collection<AbstractMap.SimpleEntry<Runnable, Integer>> workQueue = new ConcurrentLinkedQueue<>();


    public static void waitInTicks(int tick, Runnable action) {
        if (Thread.currentThread().getThreadGroup() == SidedThreadGroups.SERVER)
            workQueue.add(new AbstractMap.SimpleEntry<>(action, tick));
    }

    public static void waitInSeconds(int second, Runnable action) {
        if (Thread.currentThread().getThreadGroup() == SidedThreadGroups.SERVER)
            workQueue.add(new AbstractMap.SimpleEntry<>(action, MiscHelper.secondsToTick(second)));
    }


    public static void repeatForSeconds (int seconds, Runnable action){
        for(int i = 0; i <= MiscHelper.secondsToTick(seconds); i++){
            action.run();
        }
    }

    public static void repeatForTicks (int ticks, Runnable action){
        for(int i = 0; i <= ticks; i++){
            action.run();
        }
    }
}
