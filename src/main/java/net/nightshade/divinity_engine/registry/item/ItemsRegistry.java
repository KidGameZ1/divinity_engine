package net.nightshade.divinity_engine.registry.item;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.nightshade.divinity_engine.DivinityEngineMod;
import net.nightshade.divinity_engine.block.StatueBlock;
import net.nightshade.divinity_engine.item.DivineCodex;
import net.nightshade.divinity_engine.registry.blocks.BlocksRegistry;

import java.util.function.Consumer;


public class ItemsRegistry {
    public static final DeferredRegister<Item> REGISTRY = DeferredRegister.create(ForgeRegistries.ITEMS, DivinityEngineMod.MODID);

    public static final RegistryObject<Item> DIVINE_CODEX = REGISTRY.register("divine_codex", DivineCodex::new);
    public static final RegistryObject<Item> POCKET_DIMENSION = block(BlocksRegistry.POCKET_DIMENSION);

    private static RegistryObject<Item> block(RegistryObject<Block> block) {
        return REGISTRY.register(block.getId().getPath(), () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void init(IEventBus bus){
        REGISTRY.register(bus);
    }

    public static ItemStack getAShrineBlockItem() {
        for (Item item : ForgeRegistries.ITEMS) {
            if (item instanceof BlockItem blockItem && blockItem.getBlock() instanceof StatueBlock) {
                return REGISTRY.getEntries().stream().findAny().get().get().getDefaultInstance();
            }
        }
        return null;
    }

    public static void forEachShrineBlockItem(Consumer<Item> function) {
        for (Item item : ForgeRegistries.ITEMS) {
            if (item instanceof BlockItem blockItem && blockItem.getBlock() instanceof StatueBlock) {
                function.accept(blockItem);
            }
        }
    }
}
