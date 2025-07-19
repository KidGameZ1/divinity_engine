package net.nightshade.divinity_engine.registry.gui;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.nightshade.divinity_engine.DivinityEngineMod;
import net.nightshade.divinity_engine.world.inventory.BlessingsMenu;
import net.nightshade.divinity_engine.world.inventory.DivineCodexMenu;
import net.nightshade.divinity_engine.world.inventory.PocketDimensionInvMenu;

public class MenusRegistry {
	public static final DeferredRegister<MenuType<?>> REGISTRY = DeferredRegister.create(ForgeRegistries.MENU_TYPES, DivinityEngineMod.MODID);
	public static final RegistryObject<MenuType<BlessingsMenu>> BLESSINGS_GUI = REGISTRY.register("blessings_gui", () -> IForgeMenuType.create(BlessingsMenu::new));
	public static final RegistryObject<MenuType<PocketDimensionInvMenu>> POCKET_DIMENSION = REGISTRY.register("pocket_dimension", () -> IForgeMenuType.create(PocketDimensionInvMenu::new));
	public static final RegistryObject<MenuType<DivineCodexMenu>> DIVINE_CODEX = REGISTRY.register("divine_codex", () -> IForgeMenuType.create(DivineCodexMenu::new));

	public static void init(IEventBus bus){
		REGISTRY.register(bus);
	}
}
