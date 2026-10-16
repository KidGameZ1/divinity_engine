package net.nightshade.divinity_engine.registry.compat;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.MissingMappingsEvent;
import net.minecraftforge.registries.RegistryObject;
import net.nightshade.divinity_engine.DivinityEngineMod;
import net.nightshade.divinity_engine.block.LegacyElderStatueBlock;
import net.nightshade.divinity_engine.block.entity.LegacyElderStatueBlockEntity;
import net.nightshade.divinity_engine.registry.blocks.BlocksRegistry;
import net.nightshade.divinity_engine.registry.blocks.entity.BlockEntitesRegistry;
import net.nightshade.divinity_engine.registry.divinity.blessing.BlessingsRegistry;
import net.nightshade.divinity_engine.registry.divinity.curse.CurseRegistry;
import net.nightshade.divinity_engine.registry.divinity.gods.GodsRegistry;
import net.nightshade.divinity_engine.registry.item.ItemsRegistry;

import java.util.List;
import java.util.Set;

/** Save compatibility only. No gods, powers, stories, or creative entries are registered here. */
@Mod.EventBusSubscriber(modid = DivinityEngineMod.MODID)
public final class LegacyElderContent {
    public static final Set<String> GODS = Set.of("aureth", "malgrath", "vaelith");
    public static final Set<String> BLESSINGS = Set.of(
            "royal_decree", "unbroken_will", "kingsguard", "sovereigns_reach", "throne_of_the_world",
            "ruinbrand", "sacrificial_might", "ruinous_grasp", "ashen_tribute", "worldbreaker",
            "rift_lance", "impossible_angle", "boundary_prison", "unwritten_form", "unmaking");
    public static final Set<String> CURSES = Set.of("broken_crown", "ruin_within", "fractured_existence");
    public static final Set<String> TRIALS = Set.of("trial_of_sovereignty", "trial_of_ruin", "trial_of_unmaking");
    private static final List<Statue> STATUES = List.of(new Statue("aureth"), new Statue("malgrath"), new Statue("vaelith"));

    private LegacyElderContent() { }

    /** Force declarations before the shared block/item/entity registers are attached to the bus. */
    public static void declareStatues() { }

    public static boolean isGod(ResourceLocation id) { return contains(GODS, id); }
    public static boolean isBlessing(ResourceLocation id) { return contains(BLESSINGS, id); }
    public static boolean isCurse(ResourceLocation id) { return contains(CURSES, id); }

    private static boolean contains(Set<String> paths, ResourceLocation id) {
        return id != null && DivinityEngineMod.MODID.equals(id.getNamespace()) && paths.contains(id.getPath());
    }

    @SubscribeEvent
    public static void missingMappings(MissingMappingsEvent event) {
        ignoreRemoved(event, GodsRegistry.REGISTRY_KEY, GODS);
        ignoreRemoved(event, BlessingsRegistry.REGISTRY_KEY, BLESSINGS);
        ignoreRemoved(event, CurseRegistry.REGISTRY_KEY, CURSES);
    }

    private static <T> void ignoreRemoved(MissingMappingsEvent event, ResourceLocation registry, Set<String> paths) {
        ResourceKey<Registry<T>> key = ResourceKey.createRegistryKey(registry);
        for (var mapping : event.getMappings(key, DivinityEngineMod.MODID)) {
            // Never ignore arbitrary missing addon IDs or block/item/entity mappings.
            if (contains(paths, mapping.getKey())) mapping.ignore();
        }
    }

    private static final class Statue {
        private final RegistryObject<LegacyElderStatueBlock> block;
        private final RegistryObject<BlockEntityType<LegacyElderStatueBlockEntity>> type;

        private Statue(String name) {
            String path = name + "_statue";
            block = BlocksRegistry.REGISTRY.register(path, LegacyElderStatueBlock::new);
            ItemsRegistry.REGISTRY.register(path, () -> new BlockItem(block.get(), new Item.Properties()));
            type = BlockEntitesRegistry.REGISTRY.register(path, () -> BlockEntityType.Builder.of(
                    (pos, state) -> new LegacyElderStatueBlockEntity(pos, state, entityType()), block.get()).build(null));
        }

        private BlockEntityType<LegacyElderStatueBlockEntity> entityType() { return type.get(); }
    }
}
