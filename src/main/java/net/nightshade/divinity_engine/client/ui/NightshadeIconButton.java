package net.nightshade.divinity_engine.client.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.nightshade.renderlib.client.ui.RetainedUiTree;
import net.nightshade.renderlib.ui.UiStyle;
import net.nightshade.renderlib.ui.UiValue;

/** Original two-frame inventory icon, clipped and rendered by Nightshade. */
public final class NightshadeIconButton extends Button {
    private static final ResourceLocation ICON = new ResourceLocation("divinity_engine", "textures/screens/blessings_gui_button.png");
    private final RetainedUiTree tree = new RetainedUiTree(DivinityUi.canvas("icon_button").build());

    public NightshadeIconButton(int x, int y, OnPress action) {
        super(x, y, 18, 18, Component.translatable("gui.divinity_engine.blessings.tabs.title"), action, DEFAULT_NARRATION);
        setTooltip(Tooltip.create(getMessage()));
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        tree.setRoot(DivinityUi.canvas("icon_button").style(UiStyle.builder().size(18, 18).clip(true).build())
                .child(DivinityUi.at("icon", "sprite", 0, isHoveredOrFocused() ? -18 : 0, 18, 36)
                        .property("texture", UiValue.id(ICON)).build()).build());
        tree.layout(getX(), getY(), 18, 18);
        tree.render(graphics, mouseX, mouseY, partialTick);
    }
}
