package net.nightshade.divinity_engine.registry.item;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.nightshade.divinity_engine.DivinityEngineMod;
import net.nightshade.divinity_engine.item.Test;


public class ItemsRegistry {
    public static final DeferredRegister<Item> REGISTRY = DeferredRegister.create(ForgeRegistries.ITEMS, DivinityEngineMod.MODID);

    public static final RegistryObject<Item> TEST = REGISTRY.register("test", () -> new Test());

    private static RegistryObject<Item> block(RegistryObject<Block> block) {
        return REGISTRY.register(block.getId().getPath(), () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void init(IEventBus bus){
        REGISTRY.register(bus);
    }

//    public static void forEachWand(Consumer<TechniquesItem> function) {
//        for (Item item : ForgeRegistries.ITEMS) {
//            if (item instanceof TechniquesItem elementRuneItem) {
//                function.accept(elementRuneItem);
//            }
//        }
//    }
}
