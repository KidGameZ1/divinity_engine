package net.nightshade.divinity_engine;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.api.distmarker.Dist;
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
import net.nightshade.divinity_engine.network.cap.CapabilityHandler;
import net.nightshade.divinity_engine.network.messages.ModMessages;
import net.nightshade.divinity_engine.registry.DivinityEngineRegistry;
import net.nightshade.divinity_engine.registry.divinity.gods.GodsRegistry;
import net.nightshade.divinity_engine.registry.keys.DivinityEngineKeyMappings;
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
    private static final Logger LOGGER = LogUtils.getLogger();
    public DivinityEngineMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);
        DivinityEngineRegistry.register(bus);
        bus.addListener(CapabilityHandler::registerCapabilities);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        ModMessages.register();
        settingGodStatue();
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
            // Register key handler
            MinecraftForge.EVENT_BUS.register(DivinityEngineKeyMappings.KeyEventListener.class);
        }
    }

    private static final Collection<AbstractMap.SimpleEntry<Runnable, Integer>> workQueue = new ConcurrentLinkedQueue<>();


    private void settingGodStatue() {
        var resources = GodsRegistry.GODS_REGISTRY.get().getValues();
        resources.stream().filter(Objects::nonNull).forEach(resource -> {
            var blocks = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(DivinityEngineMod.MODID, Objects.requireNonNull(resource.getRegistryName()).getPath().toLowerCase()+"_statue"));
            var blocksEntity = ForgeRegistries.BLOCK_ENTITY_TYPES.getValue(new ResourceLocation(DivinityEngineMod.MODID, Objects.requireNonNull(resource.getRegistryName()).getPath().toLowerCase()+"_statue"));

            resource.setStatueBlock(blocks);
            resource.setStatueBlockEntity(blocksEntity);
        });
    }

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
