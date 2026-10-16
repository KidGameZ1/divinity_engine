package net.nightshade.divinity_engine.client.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.nightshade.renderlib.client.ui.RetainedUiTree;
import net.nightshade.renderlib.ui.UiNode;
import net.nightshade.renderlib.ui.UiStyle;
import net.nightshade.renderlib.ui.UiValue;
import net.nightshade.renderlib.ui.UiTheme;

/** Native focus, narration and click semantics, rendered exclusively by Nightshade. */
public class NightshadeButton extends Button {
    private final RetainedUiTree tree = new RetainedUiTree(UiNode.builder("control", "button").build());
    private boolean selected;
    private String visualLabel;
    private UiStyle.Alignment textAlignment = UiStyle.Alignment.CENTER;
    private ThemeResolver theme = DivinityUi::controlTheme;

    @FunctionalInterface
    public interface ThemeResolver {
        UiTheme resolve(boolean selected, boolean focused, boolean active);
    }

    public void setThemeResolver(ThemeResolver theme) {
        this.theme = java.util.Objects.requireNonNull(theme);
    }

    public NightshadeButton(int x, int y, int width, int height, Component label, OnPress action) {
        super(x, y, width, height, label, action, DEFAULT_NARRATION);
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public void setVisualLabel(String label) {
        visualLabel = label;
    }

    public void setTextAlignment(UiStyle.Alignment alignment) {
        textAlignment = java.util.Objects.requireNonNull(alignment);
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        tree.setRoot(UiNode.builder("control", "tab").text(visualLabel == null ? getMessage().getString() : visualLabel)
                .style(UiStyle.builder().size(width, height).padding(4).alignment(textAlignment).clip(true).build())
                .property("selected", UiValue.bool(selected)).property("disabled", UiValue.bool(!active)).build());
        tree.setTheme(theme.resolve(selected, isHoveredOrFocused(), active));
        tree.layout(getX(), getY(), width, height);
        tree.render(graphics, mouseX, mouseY, partialTick);
    }
}
