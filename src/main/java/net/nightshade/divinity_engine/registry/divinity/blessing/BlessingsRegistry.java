package net.nightshade.divinity_engine.registry.divinity.blessing;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryBuilder;
import net.minecraftforge.registries.RegistryObject;
import net.nightshade.divinity_engine.DivinityEngineMod;
import net.nightshade.divinity_engine.core.mixin.RegistryBuilderAccessor;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.solarius.RadiantStrike;
import net.nightshade.divinity_engine.divinity.gods.solarius.Solarius;
import net.nightshade.divinity_engine.network.cap.player.gods.PlayerGodsCapability;

import java.util.Set;
import java.util.function.Supplier;

public class BlessingsRegistry {
    public static final ResourceLocation REGISTRY_KEY = new ResourceLocation(DivinityEngineMod.MODID, "blessings");
    private static final DeferredRegister<Blessings> registry = DeferredRegister.create(REGISTRY_KEY, DivinityEngineMod.MODID);
    public static final Supplier<IForgeRegistry<Blessings>> BLESSINGS_REGISTRY = registry.makeRegistry(() -> {
        RegistryBuilder<Blessings> builder = new RegistryBuilder();
        ((RegistryBuilderAccessor)builder).setHasWrapper(true);
        return builder;
    });
    public static final RegistryObject<Blessings> RADIANT_STRIKE = registry.register("radiant_strike",
            () -> new RadiantStrike(10, 20, false) // example values for neededFavor and cooldown
    );




    public BlessingsRegistry() {
    }
    public static void register(IEventBus modEventBus) {
//        PlayerBlessingsCapability.init(modEventBus);
        registry.register(modEventBus);
    }
    public static void init(IEventBus modEventBus) {
        register(modEventBus);
    }

    static {




    }
}
