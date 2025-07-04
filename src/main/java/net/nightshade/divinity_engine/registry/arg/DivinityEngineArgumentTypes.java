package net.nightshade.divinity_engine.registry.arg;

import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.nightshade.divinity_engine.DivinityEngineMod;
import net.nightshade.divinity_engine.commands.arg.GodsArgument;

public class DivinityEngineArgumentTypes {
    private static final DeferredRegister<ArgumentTypeInfo<?, ?>> registry;
    private static final RegistryObject<SingletonArgumentInfo<GodsArgument>> GODS_ARGUMENT_REGISTRY;

    public DivinityEngineArgumentTypes() {
    }

    public static void init(IEventBus modEventBus) {
        registry.register(modEventBus);
    }

    static {
        registry = DeferredRegister.create(ForgeRegistries.COMMAND_ARGUMENT_TYPES, DivinityEngineMod.MODID);
        GODS_ARGUMENT_REGISTRY = registry.register("gods_argument", () -> (SingletonArgumentInfo) ArgumentTypeInfos.registerByClass(GodsArgument.class, SingletonArgumentInfo.contextFree(GodsArgument::god)));
    }
}