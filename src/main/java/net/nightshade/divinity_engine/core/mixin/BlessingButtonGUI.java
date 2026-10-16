package net.nightshade.divinity_engine.core.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.nightshade.divinity_engine.client.ui.NightshadeIconButton;
import net.nightshade.divinity_engine.network.messages.ModMessages;
import net.nightshade.divinity_engine.network.messages.gui.PlayerBlessingsButtonMessage;
import net.nightshade.divinity_engine.util.divinity.gods.GodHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public abstract class BlessingButtonGUI extends AbstractContainerScreen<InventoryMenu> {
    @Shadow @Final private RecipeBookComponent recipeBookComponent;
    @Unique private NightshadeIconButton divinity$blessings;

    protected BlessingButtonGUI(InventoryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void divinity$init(CallbackInfo ci) {
        divinity$blessings = addRenderableWidget(new NightshadeIconButton(leftPos + 130, topPos + 61, button -> {
            var player = minecraft.player;
            if (player != null && divinity$blessings.visible) {
                ModMessages.sendToServer(new PlayerBlessingsButtonMessage(1, player.getBlockX(), player.getBlockY(), player.getBlockZ()));
            }
        }));
        divinity$update();
    }

    @Inject(method = "containerTick", at = @At("TAIL"))
    private void divinity$tick(CallbackInfo ci) { divinity$update(); }

    @Unique
    private void divinity$update() {
        if (divinity$blessings == null) return;
        divinity$blessings.setX(leftPos + 130);
        divinity$blessings.setY(topPos + 61);
        divinity$blessings.visible = minecraft.player != null && !recipeBookComponent.isVisible()
                && !GodHelper.getAllContactedGods(minecraft.player).isEmpty();
        divinity$blessings.active = divinity$blessings.visible;
    }
}
