package net.nightshade.divinity_engine.core.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.Level;
import net.nightshade.divinity_engine.network.messages.ModMessages;
import net.nightshade.divinity_engine.network.messages.gui.BlessingsButtonMessage;
import net.nightshade.divinity_engine.network.messages.gui.PlayerBlessingsButtonMessage;
import net.nightshade.divinity_engine.util.divinity.gods.GodHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Objects;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeBlessingButtonGUI extends AbstractContainerScreen<InventoryMenu> {

    @Shadow
    protected abstract void init();

    @Shadow
    private static CreativeModeTab selectedTab = CreativeModeTabs.getDefaultTab();

    private final static HashMap<String, Object> guistate = new HashMap<>();
    @Unique
    private Player entity;
    @Unique
    private final Level lev;
    @Unique
    private final int x, y, z;
    ImageButton imagebutton_brewing_stand_button;
    @Unique
    public ResourceLocation resourceLocation1 = ResourceLocation.tryParse("divinity_engine:textures/screens/blessings_gui_button.png");
    @Unique
    public ResourceLocation resourceLocation2 = ResourceLocation.tryParse("divinity_engine:textures/screens/blessings_gui_button.png");
    ImageButton imagebutton_enchantment_table_button;
    public CreativeBlessingButtonGUI(InventoryMenu inventoryMenu, Inventory inventory, Component component) {
        super(inventoryMenu, inventory, component);
        this.lev = entity.level();
        this.x = entity.getBlockX();
        this.y = entity.getBlockY();
        this.z = entity.getBlockZ();
    }


    @Inject(method = "containerTick", at = @At("HEAD"))
    public void tick(CallbackInfo ci){
        assert this.minecraft != null;
        Player player = this.minecraft.player;


    }

    @Unique
    private void updateButton() {
        if (GodHelper.getAllContactedGods(minecraft.player).isEmpty()){
            this.removeWidget(imagebutton_enchantment_table_button);
        }else {
            this.addRenderableWidget(imagebutton_enchantment_table_button);
        }
    }

    @Inject(method = "init", at = @At("TAIL"))
    public void init(CallbackInfo ci){
        Player player = this.minecraft.player;

        imagebutton_enchantment_table_button = new ImageButton(this.leftPos + 140, this.topPos + 17, 18, 18, 0, 0, 18, Objects.requireNonNull(resourceLocation1), 18, 36, e -> {
            if (

                    !GodHelper.getAllContactedGods(player).isEmpty() && selectedTab.getType() == CreativeModeTab.Type.INVENTORY
            ) {
                ModMessages.INSTANCE.sendToServer(new PlayerBlessingsButtonMessage(1, player.getBlockX(), player.getBlockY(), player.getBlockZ()));
                System.out.println(GodHelper.getAllContactedGods(player).size());
                updateButton();
            }else {
                updateButton();
            }


        }){
            @Override
            public void render(GuiGraphics guiGraphics, int gx, int gy, float ticks) {
                if (

                        !GodHelper.getAllContactedGods(player).isEmpty() && selectedTab.getType() == CreativeModeTab.Type.INVENTORY

                ) {
                    System.out.println(GodHelper.getAllContactedGods(player).size());
                    super.render(guiGraphics, gx, gy, ticks);
                }
            }
        };


        guistate.put("button:imagebutton_enchantment_table_button", imagebutton_enchantment_table_button);
        this.addRenderableWidget(imagebutton_enchantment_table_button);

        super.init();
    }
}