package net.nightshade.divinity_engine.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.nightshade.divinity_engine.DivinityEngineMod;
import net.nightshade.divinity_engine.network.cap.player.gods.MainPlayerCapability;
import net.nightshade.divinity_engine.registry.divinity.DivineCodexPagesRegistry;
import net.nightshade.divinity_engine.util.DivinityEngineHelper;
import net.nightshade.divinity_engine.util.divinity.DivineCodexPages;
import net.nightshade.divinity_engine.world.inventory.DivineCodexMenu;

import java.awt.*;
import java.util.HashMap;

public class DivineCodexScreen extends AbstractContainerScreen<DivineCodexMenu> {
    private final static HashMap<String, Object> guistate = DivineCodexMenu.guistate;

    private static final ResourceLocation BACKGROUND = new ResourceLocation("divinity_engine", "textures/screens/divine_codex.png");
    private final Level world;
    private final int x, y, z;
    private final Player entity;

    private int currentPage = 0;
    private int centerX;
    private int centerY;
    private int titleY;
    private int contentY;

    private final DivineCodexPages divineCodexPages = DivineCodexPagesRegistry.getPages();
    private ImageButton nextButton;
    private ImageButton prevButton;

    public DivineCodexScreen(DivineCodexMenu container, Inventory inventory, Component text) {
        super(container, inventory, text);
        this.world = container.world;
        this.x = container.x;
        this.y = container.y;
        this.z = container.z;
        imageWidth = 146;
        imageHeight = 180;
        this.entity = container.entity;
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
        this.centerX = this.leftPos + this.imageWidth / 2;
        this.centerY = this.topPos + this.imageHeight / 2;
        this.titleY = 10;
        this.contentY = 30;
        this.currentPage = DivinityEngineHelper.getMainPlayerVariables(this.entity).getDcPageNum();

        nextButton = new ImageButton(this.leftPos + 99, this.topPos + 149, 24, 15, 0, 0, 15, new ResourceLocation(DivinityEngineMod.MODID +":textures/screens/atlas/forward.png"), 24, 30, e -> {
            turnPage(1);
            updateButtonVisibility();
        });

        guistate.put("button:nextButton", nextButton);
        this.addRenderableWidget(nextButton);

        prevButton = new ImageButton(this.leftPos + 18, this.topPos + 149, 24, 15, 0, 0, 15, new ResourceLocation(DivinityEngineMod.MODID+":textures/screens/atlas/backward.png"), 24, 30, e -> {
            turnPage(-1);
            updateButtonVisibility();
        });

        guistate.put("button:prevButton", prevButton);
        this.addRenderableWidget(prevButton);

    }

    @Override
    protected void containerTick() {

        updateButtonVisibility();

        super.containerTick();
    }

    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        this.renderBackground(pGuiGraphics);
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float v, int i, int i1) {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        guiGraphics.blit(BACKGROUND, this.leftPos, this.topPos, 0, 0, 146, 180, 146, 180);
        RenderSystem.disableBlend();
    }

    @Override
    protected void renderLabels(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY) {
        if (divineCodexPages.getTitle(currentPage) != null) {
            Component title = divineCodexPages.getTitle(currentPage);
            DivinityEngineHelper.drawWrappedCenteredWithScale(
                    pGuiGraphics, font, title, 70, titleY, 118, 1, Color.DARK_GRAY.getRGB()
            );
        }
        if (divineCodexPages.getContent(currentPage) != null) {
            Component content = divineCodexPages.getContent(currentPage);
            DivinityEngineHelper.drawWrappedWithScale(
                    pGuiGraphics, font, content, 13, contentY, 118, 0.95f, Color.lightGray.getRGB()
            );
        }
    }

    private void turnPage(int direction) {
        int currentPage = 0;
        currentPage += direction;
        if (currentPage == 1) DivinityEngineHelper.getMainPlayerVariables(entity).increaseDCPageNum(1);
        if (currentPage == -1) DivinityEngineHelper.getMainPlayerVariables(entity).decreaseDCPageNum(1);
        MainPlayerCapability.PlayerVariables.sync(entity);
        updateButtonVisibility();
    }

    private void updateButtonVisibility() {
        currentPage = DivinityEngineHelper.getMainPlayerVariables(entity).getDcPageNum();
        prevButton.visible = currentPage > 0;
        nextButton.visible = currentPage < divineCodexPages.getMaxPages();
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }


}
