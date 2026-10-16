package net.nightshade.divinity_engine.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.nightshade.divinity_engine.network.messages.ModMessages;
import net.nightshade.divinity_engine.network.messages.gui.PocketExitMessage;
import net.nightshade.divinity_engine.registry.dimensions.DimensionRegistry;

@Mod.EventBusSubscriber(modid = "divinity_engine", value = Dist.CLIENT)
public final class PocketExitControl {
    @SubscribeEvent public static void pauseMenu(ScreenEvent.Init.Post event) {
        var mc = Minecraft.getInstance();
        if (mc.level == null || !mc.level.dimension().equals(DimensionRegistry.POCKET_DIMENSION)
                || !(event.getScreen() instanceof PauseScreen screen)) return;
        event.addListener(new ConstellationButton(screen.width / 2 - 100, screen.height - 28, 200, 20,
                Component.translatable("gui.divinity_engine.pocket.leave"), "trial", button -> {
                    ModMessages.sendToServer(new PocketExitMessage()); mc.setScreen(null);
                }));
    }
    private PocketExitControl() {}
}
