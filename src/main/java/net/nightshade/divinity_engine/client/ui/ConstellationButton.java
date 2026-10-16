package net.nightshade.divinity_engine.client.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.nightshade.divinity_engine.registry.gui.DivinityUiTypes;
import net.nightshade.renderlib.client.ui.RetainedUiTree;
import net.nightshade.renderlib.ui.UiNode;
import net.nightshade.renderlib.ui.UiStyle;
import net.nightshade.renderlib.ui.UiValue;

/** Native button input with Nightshade-rendered stars, inscriptions and binding seals. */
public final class ConstellationButton extends NightshadeButton {
    private final RetainedUiTree inscription = new RetainedUiTree(UiNode.builder("inscription", "canvas").build());
    private final String role;
    private boolean selected;

    public ConstellationButton(int x, int y, int width, int height, Component label, String role, OnPress action) {
        super(x, y, width, height, label, action);
        this.role = role;
    }

    @Override
    public void setSelected(boolean selected) {
        super.setSelected(selected);
        this.selected = selected;
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        inscription.setRoot(UiNode.builder("inscription", DivinityUiTypes.CELESTIAL_CONTROL).text(getMessage().getString())
                .style(UiStyle.builder().size(width, height).clip(true).build())
                .property("role", UiValue.text(role)).property("selected", UiValue.bool(selected))
                .property("highlighted", UiValue.bool(isHoveredOrFocused())).property("disabled", UiValue.bool(!active)).build());
        inscription.layout(getX(), getY(), width, height);
        inscription.render(graphics, mouseX, mouseY, partialTick);
    }
}
