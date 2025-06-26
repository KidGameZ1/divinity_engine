package net.nightshade.divinity_engine.registry.screen;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.nightshade.divinity_engine.client.screen.BlessingsScreen;
import net.nightshade.divinity_engine.registry.gui.MenusRegistry;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ScreensRegistry {
	@SubscribeEvent
	public static void clientLoad(FMLClientSetupEvent event) {
		event.enqueueWork(() -> {
			MenuScreens.register(MenusRegistry.BLESSINGS_GUI.get(), BlessingsScreen::new);
		});
	}
}