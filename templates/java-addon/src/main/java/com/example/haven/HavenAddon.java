package com.example.haven;

import java.awt.Color;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.nightshade.divinity_engine.api.gods.GodsDeferredRegister;
import net.nightshade.divinity_engine.api.trials.GodTrial;
import net.nightshade.divinity_engine.api.trials.GodTrialsDeferredRegister;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.gods.BaseGod;
import net.nightshade.divinity_engine.registry.divinity.trial.GodlyTrialsRegistry;

@Mod(HavenAddon.ID)
public final class HavenAddon {
    public static final String ID = "haven_addon";
    public static final Color COLOR = new Color(0xB5D5EF);
    private static final DeferredRegister<Blessings> BLESSINGS = DeferredRegister.create(
            ResourceLocation.fromNamespaceAndPath("divinity_engine", "blessings"), ID);
    public static final RegistryObject<Blessings> GUIDING_BREATH = BLESSINGS.register(
            "guiding_breath", GuidingBreath::new);
    private static final GodsDeferredRegister GODS = GodsDeferredRegister.create(ID);
    public static final RegistryObject<BaseGod> HAVEN = GODS.register("haven",
            () -> new HavenGod(Set.of(GUIDING_BREATH)));
    private static final GodTrialsDeferredRegister TRIALS = new GodTrialsDeferredRegister(ID);

    public HavenAddon() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        BLESSINGS.register(bus);
        GODS.register(bus);
        TRIALS.register("shelter_vigil", () -> new GodTrial(HAVEN.get(), ShelterVigil::new));
        bus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> GodlyTrialsRegistry.addDeferredRegister(TRIALS));
    }
}
