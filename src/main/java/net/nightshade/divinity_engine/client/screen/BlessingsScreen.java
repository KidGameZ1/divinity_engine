package net.nightshade.divinity_engine.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.nightshade.divinity_engine.DivinityEngineMod;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.network.messages.BlessingsButtonMessage;
import net.nightshade.divinity_engine.network.messages.ModMessages;
import net.nightshade.divinity_engine.util.MainPlayerCapabilityHelper;
import net.nightshade.divinity_engine.util.divinity.gods.GodHelper;
import net.nightshade.divinity_engine.world.inventory.BlessingsMenu;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class BlessingsScreen extends AbstractContainerScreen<BlessingsMenu> {

	private final static HashMap<String, Object> guistate = BlessingsMenu.guistate;

	private final Level world;
	private final int x, y, z;
	private final Player entity;
	int centerX = this.leftPos + this.imageWidth / 2;
	int blessingNameX = centerX-16;
	int blessingNameY = 15;

	Button button_active;
	Button button_right;
	Button button_left;

	public BlessingsScreen(BlessingsMenu container, Inventory inventory, Component text) {
		super(container, inventory, text);
		this.world = container.world;
		this.x = container.x;
		this.y = container.y;
		this.z = container.z;
		this.entity = container.entity;
		this.imageWidth = 150;
		this.imageHeight = 95;
	}

	private static final ResourceLocation texture = new ResourceLocation(DivinityEngineMod.MODID +":textures/screens/blessings_screen.png");


	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		this.renderBackground(guiGraphics);
		super.render(guiGraphics, mouseX, mouseY, partialTicks);

		List<BlessingsInstance> blessingsInstances = GodHelper.getAllBlessingsInstances(this.minecraft.player);
		if (blessingsInstances != null && !blessingsInstances.isEmpty()) {
			int pageNum = MainPlayerCapabilityHelper.getBlessingsPageNum(minecraft.player);
			if (pageNum >= 0 && pageNum < blessingsInstances.size()) {
				BlessingsInstance selectedBlessingInstance = blessingsInstances.get(pageNum);
				Blessings selectedBlessing = selectedBlessingInstance.getBlessing();

				int blessingNameXX = leftPos + blessingNameX;
				int blessingNameY = topPos + 14;
				String blessingName = Component.translatable(selectedBlessing.getNameTranslationKey()).getString();

				if (mouseX >= blessingNameXX - font.width(blessingName)/2 &&
						mouseX <= blessingNameXX + font.width(blessingName)/2 &&
						mouseY >= blessingNameY &&
						mouseY <= blessingNameY + 10) {

					List<FormattedCharSequence> descriptionText = minecraft.font.split(
							Component.translatable(selectedBlessing.getDescriptionTranslationKey()),
							150
					);
					guiGraphics.renderTooltip(minecraft.font, descriptionText, mouseX, mouseY);
				}
			}
		}

		this.renderTooltip(guiGraphics, mouseX, mouseY);
	}



	@Override
	protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int gx, int gy) {
		RenderSystem.setShaderColor(1, 1, 1, 1);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();

		guiGraphics.blit(texture, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);

		RenderSystem.disableBlend();
	}

	private boolean waitingForKeyPress = false;

	@Override
	public boolean keyPressed(int key, int b, int c) {
		if (waitingForKeyPress) {
			// Only accept keys 1, 2, or 3
			if (key >= 49 && key <= 51) { // ASCII values for keys 1-3
				int slot = key - 49; // Convert to 0-based index
				int pageNum = MainPlayerCapabilityHelper.getBlessingsPageNum(minecraft.player);
				List<BlessingsInstance> blessingsInstances = GodHelper.getAllBlessingsInstances(minecraft.player);
				if (blessingsInstances != null && !blessingsInstances.isEmpty() && pageNum >= 0 && pageNum < blessingsInstances.size()) {
					ModMessages.INSTANCE.sendToServer(new BlessingsButtonMessage(10 + slot, x, y, z));
				}
				waitingForKeyPress = false;
				updateActiveButton();
				return true;
			}
			return false;
		}

		if (key == 256) {
			this.minecraft.player.closeContainer();
			return true;
		}
		return super.keyPressed(key, b, c);
	}


	@Override
	protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		guiGraphics.drawString(this.font, Component.translatable("gui.divinity_engine.blessings.label_blessings"), 48, 4, -16777216, false);

		List<BlessingsInstance> blessingsInstances = GodHelper.getAllBlessingsInstances(this.minecraft.player);
		if (blessingsInstances == null || blessingsInstances.isEmpty()) {
			guiGraphics.drawString(this.font, Component.translatable("gui.divinity_engine.blessings.label_blessing_name").getString(), 39, 14, -6710887, false);
			this.minecraft.player.closeContainer();
			return;
		}

		int pageNum = MainPlayerCapabilityHelper.getBlessingsPageNum(minecraft.player);
		if (pageNum >= 0 && pageNum < blessingsInstances.size()) {
			BlessingsInstance selectedBlessingInstance = blessingsInstances.get(pageNum);
			Blessings selectedBlessing = selectedBlessingInstance.getBlessing();

			// Draw blessing name

			String blessingName = Component.translatable(selectedBlessing.getNameTranslationKey()).getString();
			guiGraphics.drawCenteredString(font, blessingName, blessingNameX , blessingNameY, selectedBlessing.getColor().getRGB());

			// Draw other information
			guiGraphics.drawString(this.font, Component.translatable("gui.divinity_engine.blessings.label_given_by").getString()+Component.translatable(selectedBlessingInstance.getBoundGod().getNameTranslationKey()).getString(), 7, 25, selectedBlessingInstance.getBoundGod().getColor().getRGB(), false);
			guiGraphics.drawString(this.font, Component.translatable("gui.divinity_engine.blessings.label_required_favor").getString()+selectedBlessing.getNeededFavor(), 7, 40, -12829636, false);
			guiGraphics.drawString(this.font, Component.translatable("gui.divinity_engine.blessings.label_current_favor").getString()+GodHelper.getGodOrNull(minecraft.player,selectedBlessingInstance.getBoundGod()).getFavor(), 7, 54, -12829636, false);
		}
	}

	private void updateActiveButton() {
		int pageNum = MainPlayerCapabilityHelper.getBlessingsPageNum(minecraft.player);
		List<BlessingsInstance> blessingsInstances = GodHelper.getAllBlessingsInstances(minecraft.player);

		if (waitingForKeyPress) {
			// Remove old button
			this.removeWidget(button_active);

			// Create new wider button
			button_active = Button.builder(Component.literal("Press 1, 2, or 3"), e -> {
				// Clicking while waiting does nothing
			}).bounds(this.leftPos + 32, this.topPos + 69, 86, 20).build();

			this.addRenderableWidget(button_active);
		} else {
			// Remove wide button
			this.removeWidget(button_active);

			// Check if blessing is already equipped
			boolean isEquipped = false;
			if (blessingsInstances != null && !blessingsInstances.isEmpty() && pageNum >= 0 && pageNum < blessingsInstances.size()) {
				BlessingsInstance selectedBlessing = blessingsInstances.get(pageNum);
				isEquipped = isAnySlotEquipped(selectedBlessing);
			}

			// Restore original button with appropriate text
			Component buttonText = isEquipped ?
					Component.literal("Already Equipped") :
					Component.translatable("gui.divinity_engine.blessings.button_active");
			button_active = Button.builder(buttonText, e -> {
				if (blessingsInstances != null && !blessingsInstances.isEmpty() && pageNum >= 0 && pageNum < blessingsInstances.size()) {
					BlessingsInstance selectedBlessing = blessingsInstances.get(pageNum);
					boolean equipped = isAnySlotEquipped(selectedBlessing);
					if (!equipped) {
						waitingForKeyPress = true;
						updateActiveButton();
					}
				}
			}).bounds(this.leftPos + 47, this.topPos + 69, 56, 20).build();

			this.addRenderableWidget(button_active);
		}
	}
	private boolean isAnySlotEquipped(BlessingsInstance blessing) {
		BlessingsInstance slot0 = MainPlayerCapabilityHelper.getBlessingSlot1(minecraft.player);
		BlessingsInstance slot1 = MainPlayerCapabilityHelper.getBlessingSlot2(minecraft.player);
		BlessingsInstance slot2 = MainPlayerCapabilityHelper.getBlessingSlot3(minecraft.player);

		return (slot0 != null && slot0.getBlessing().equals(blessing.getBlessing())) ||
				(slot1 != null && slot1.getBlessing().equals(blessing.getBlessing())) ||
				(slot2 != null && slot2.getBlessing().equals(blessing.getBlessing()));
	}

	private Component getActiveButtonText() {
		if (isCurrentBlessingEquipped()) {
			return Component.literal("Already Equipped");
		}
		return Component.translatable("gui.divinity_engine.blessings.button_active");
	}

	private boolean isCurrentBlessingEquipped() {
		List<BlessingsInstance> blessingsInstances = GodHelper.getAllBlessingsInstances(minecraft.player);
		int pageNum = MainPlayerCapabilityHelper.getBlessingsPageNum(minecraft.player);

		if (blessingsInstances != null && !blessingsInstances.isEmpty() &&
				pageNum >= 0 && pageNum < blessingsInstances.size()) {
			BlessingsInstance selectedBlessing = blessingsInstances.get(pageNum);

			BlessingsInstance slot0 = MainPlayerCapabilityHelper.getBlessingSlot1(minecraft.player);
			BlessingsInstance slot1 = MainPlayerCapabilityHelper.getBlessingSlot2(minecraft.player);
			BlessingsInstance slot2 = MainPlayerCapabilityHelper.getBlessingSlot3(minecraft.player);

			return (slot0 != null && slot0.getBlessing().equals(selectedBlessing.getBlessing())) ||
					(slot1 != null && slot1.getBlessing().equals(selectedBlessing.getBlessing())) ||
					(slot2 != null && slot2.getBlessing().equals(selectedBlessing.getBlessing()));
		}
		return false;
	}


	@Override
	public void init() {
		super.init();

		List<BlessingsInstance> blessingsInstances = GodHelper.getAllBlessingsInstances(minecraft.player);
		int pageNum = MainPlayerCapabilityHelper.getBlessingsPageNum(minecraft.player);
		int maxPages = blessingsInstances != null ? blessingsInstances.size() - 1 : 0;

		button_active = Button.builder(getActiveButtonText(), e -> {
			if (!isCurrentBlessingEquipped()) {
				waitingForKeyPress = true;
				updateActiveButton();
			}
		}).bounds(this.leftPos + 47, this.topPos + 69, 56, 20).build();

		button_right = Button.builder(Component.literal(">"), e -> {
			ModMessages.INSTANCE.sendToServer(new BlessingsButtonMessage(1, x, y, z));
			BlessingsButtonMessage.handleButtonAction(entity, 1, x, y, z);
			updateActiveButton(); // Update button after page change
		}).bounds(this.leftPos + 155, this.topPos + 43, 30, 20).build();

		button_left = Button.builder(Component.literal("<"), e -> {
			ModMessages.INSTANCE.sendToServer(new BlessingsButtonMessage(2, x, y, z));
			BlessingsButtonMessage.handleButtonAction(entity, 2, x, y, z);
			updateActiveButton(); // Update button after page change
		}).bounds(this.leftPos + -34, this.topPos + 43, 30, 20).build();

		guistate.put("button:button_active", button_active);
		guistate.put("button:button_right", button_right);
		guistate.put("button:button_left", button_left);

		this.addRenderableWidget(button_active);
		this.addRenderableWidget(button_right);
		this.addRenderableWidget(button_left);
	}


}