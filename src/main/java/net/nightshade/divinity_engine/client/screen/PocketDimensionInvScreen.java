package net.nightshade.divinity_engine.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.nightshade.divinity_engine.client.ui.DivinityUi;
import net.nightshade.divinity_engine.client.ui.GalaxyUi;
import net.nightshade.divinity_engine.client.ui.ConstellationButton;
import net.nightshade.divinity_engine.registry.gui.DivinityUiTypes;
import net.nightshade.divinity_engine.world.inventory.PocketDimensionInvMenu;
import net.nightshade.renderlib.client.ui.RenderLibContainerScreen;
import net.nightshade.renderlib.ui.UiNode;
import net.nightshade.renderlib.ui.UiRenderLayer;
import net.nightshade.renderlib.ui.UiStyle;
import net.nightshade.renderlib.ui.UiValue;

/** Celestial chrome around the original menu; Minecraft still owns every item interaction. */
public class PocketDimensionInvScreen extends RenderLibContainerScreen<PocketDimensionInvMenu> {
    private static final int SIDE = 8;
    private static final int HEADER = 32;
    private static final int FOOTER = 20;

    public PocketDimensionInvScreen(PocketDimensionInvMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 154;
    }

    @Override
    public void init() {
        super.init();
        int frameWidth = imageWidth + SIDE * 2;
        int frameHeight = imageHeight + HEADER + FOOTER;
        topPos = (height - frameHeight) / 2 + HEADER;
        int frameX = leftPos - SIDE;
        int frameY = topPos - HEADER;
        addRenderableWidget(new ConstellationButton(frameX + frameWidth - 62, frameY + frameHeight - 20, 54, 16,
                Component.literal("Domain"), "blessing", button -> {
                    if (minecraft != null && minecraft.gameMode != null)
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
                }));
        UiNode.Builder slots = UiNode.builder("inventory_slots", DivinityUiTypes.CELESTIAL_INVENTORY)
                .style(UiStyle.builder().size(imageWidth, imageHeight).clip(true).build())
                .property("x", UiValue.integer(leftPos)).property("y", UiValue.integer(topPos))
                .property("slot_count", UiValue.integer(menu.slots.size()));
        int storageBottom = 0, inventoryTop = imageHeight;
        for (int i = 0; i < menu.slots.size(); i++) {
            var slot = menu.slots.get(i);
            slots.property("slot_x_" + i, UiValue.integer(slot.x)).property("slot_y_" + i, UiValue.integer(slot.y));
            if (i < 27) storageBottom = Math.max(storageBottom, slot.y + 16);
            else inventoryTop = Math.min(inventoryTop, slot.y);
        }
        slots.property("divider_y", UiValue.integer((storageBottom + inventoryTop) / 2));
        String capacity = Component.translatable("gui.divinity_engine.pocket.capacity", 27).getString();
        setRenderlibRoot(UiRenderLayer.BACKGROUND, DivinityUi.canvas("pocket_inventory")
                .child(GalaxyUi.background(frameX, frameY, frameWidth, frameHeight, 0xB6C9ED))
                .child(GalaxyUi.ornament("vault_etching", DivinityUiTypes.CELESTIAL_VAULT,
                        frameX, frameY, frameWidth, frameHeight, "aethon", 0, 0))
                .child(GalaxyUi.ornament("engraving", DivinityUiTypes.CELESTIAL_FRAME,
                        frameX, frameY, frameWidth, frameHeight, "aethon", 0, 0))
                .child(GalaxyUi.ornament("divine_seal", DivinityUiTypes.DIVINE_SEAL,
                        frameX + frameWidth - 26, frameY + 6, 20, 20, "aethon", 0, 0))
                .child(DivinityUi.panel("inventory_surface", leftPos, topPos, imageWidth, imageHeight, 0xE008101D))
                .child(DivinityUi.text("title", tr("title").getString(), leftPos, frameY + 7, imageWidth - 28, 11, GalaxyUi.GOLD))
                .child(DivinityUi.text("subtitle", tr("subtitle").getString(), leftPos, frameY + 18, imageWidth - 28, 9, GalaxyUi.MUTED))
                .child(DivinityUi.text("capacity", font.plainSubstrByWidth(capacity, frameWidth - 80), frameX + 8,
                        frameY + frameHeight - 16, frameWidth - 80, 9, GalaxyUi.MUTED))
                .child(slots.build()).build());
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeft, int guiTop, int button) {
        // Decorative header/padding belong to the menu, not the outside-click item-drop area.
        return mouseX < guiLeft - SIDE || mouseY < guiTop - HEADER
                || mouseX >= guiLeft + imageWidth + SIDE || mouseY >= guiTop + imageHeight + FOOTER;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    private static Component tr(String key) {
        return Component.translatable("gui.divinity_engine.pocket." + key);
    }

    @Override public Component getNarrationMessage() { return tr("title"); }
    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {}
    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {}
}
