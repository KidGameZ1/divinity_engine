package net.nightshade.divinity_engine.util.divinity.innerworld;

import net.minecraftforge.registries.*;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraft.world.entity.*;
import net.nightshade.divinity_engine.entity.*;

public final class CommunionBootstrap {
    private static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(ForgeRegistries.ENTITY_TYPES,"divinity_engine");
    public static final RegistryObject<EntityType<TranceBodyEntity>> BODY=ENTITIES.register("trance_body",()->EntityType.Builder.of(TranceBodyEntity::new,MobCategory.MISC).sized(.6F,1.8F).clientTrackingRange(8).updateInterval(3).build("divinity_engine:trance_body"));
    public static final RegistryObject<EntityType<GodManifestationEntity>> GOD=ENTITIES.register("god_manifestation",()->EntityType.Builder.of(GodManifestationEntity::new,MobCategory.MISC).sized(1,2.5F).clientTrackingRange(8).build("divinity_engine:god_manifestation"));
    public static void register(IEventBus bus){
        ENTITIES.register(bus);
        bus.addListener((EntityAttributeCreationEvent e)->{e.put(BODY.get(),TranceBodyEntity.attributes().build());e.put(GOD.get(),TranceBodyEntity.attributes().build());});
        bus.addListener((FMLCommonSetupEvent e)->e.enqueueWork(()->{
            InnerWorldsRegistry.initialize();CommunionNetwork.register();CommunionTickets.register();
        }));
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER,CommunionConfig.SERVER,"divinity-communion-server.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT,CommunionConfig.CLIENT,"divinity-communion-client.toml");
        MinecraftForge.EVENT_BUS.register(CommunionEvents.class);
        MinecraftForge.EVENT_BUS.addListener(CommunionCommands::register);
    }
    private CommunionBootstrap(){}
}
