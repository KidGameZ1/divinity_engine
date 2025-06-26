package net.nightshade.divinity_engine.registry.divinity.gods;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryBuilder;
import net.minecraftforge.registries.RegistryObject;
import net.nightshade.divinity_engine.DivinityEngineMod;
import net.nightshade.divinity_engine.block.ShrineBlock;
import net.nightshade.divinity_engine.core.mixin.RegistryBuilderAccessor;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.gods.BaseGod;
import net.nightshade.divinity_engine.divinity.gods.solarius.Solarius;
import net.nightshade.divinity_engine.network.cap.player.gods.PlayerGodsCapability;
import net.nightshade.divinity_engine.registry.blocks.BlocksRegistry;
import net.nightshade.divinity_engine.registry.divinity.blessing.BlessingsRegistry;
import net.nightshade.divinity_engine.registry.item.ItemsRegistry;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

public class GodsRegistry {
    public static final ResourceLocation REGISTRY_KEY = new ResourceLocation(DivinityEngineMod.MODID, "gods");
    private static final DeferredRegister<BaseGod> registry = DeferredRegister.create(REGISTRY_KEY, DivinityEngineMod.MODID);
    public static final Supplier<IForgeRegistry<BaseGod>> GODS_REGISTRY = registry.makeRegistry(() -> {
        RegistryBuilder<BaseGod> builder = new RegistryBuilder();
        ((RegistryBuilderAccessor)builder).setHasWrapper(true);
        return builder;
    });
    public static final RegistryObject<BaseGod> SOLARIUS = register("solarius", () -> {
        Set<RegistryObject<Blessings>> blessings = new HashSet<>();
        blessings.add(BlessingsRegistry.RADIANT_STRIKE);
        return new Solarius(
                Set.of("light", "justice", "day"),
                blessings
        );
    });




    public GodsRegistry() {
    }
    public static void register(IEventBus modEventBus) {
        PlayerGodsCapability.init(modEventBus);
        registry.register(modEventBus);
    }
    public static void init(IEventBus modEventBus) {
        register(modEventBus);
    }

    public static RegistryObject<BaseGod> register(String name, Supplier<? extends BaseGod> supplier) {
        RegistryObject<BaseGod> reg = registry.register(name, supplier);

        RegistryObject<Block> shrine = BlocksRegistry.REGISTRY.register(name + "_shrine", () -> new ShrineBlock(reg));
        ItemsRegistry.REGISTRY.register(shrine.getId().getPath(), () -> new BlockItem(shrine.get(), new Item.Properties()));

        return reg;
    }

    static {




    }
}
