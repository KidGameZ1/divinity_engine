package net.nightshade.divinity_engine.registry.tabs;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.nightshade.divinity_engine.DivinityEngineMod;
import net.nightshade.divinity_engine.registry.item.ItemsRegistry;

import java.util.Objects;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class TabsRegistry {
    public static final DeferredRegister<CreativeModeTab> REGISTRY = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, DivinityEngineMod.MODID);

    public static final RegistryObject<CreativeModeTab> SHRINES = REGISTRY.register("statues",
            () -> CreativeModeTab.builder().title(Component.translatable("item_group.divinity_engine.statues")).icon(() -> new ItemStack(Items.STONE)).displayItems((parameters, tabData) -> {
                ItemsRegistry.forEachShrineBlockItem(tabData::accept);
            }).withSearchBar().build());

    public static void init(IEventBus bus){
        REGISTRY.register(bus);
    }
}
