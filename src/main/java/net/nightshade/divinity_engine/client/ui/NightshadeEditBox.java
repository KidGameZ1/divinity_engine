package net.nightshade.divinity_engine.client.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.nightshade.renderlib.client.ui.RetainedUiTree;
import net.nightshade.renderlib.ui.UiNode;
import net.nightshade.renderlib.ui.UiTheme;
import java.util.function.Function;

/** Keep vanilla selection, clipboard, cursor and narration; Nightshade owns the field skin. */
public final class NightshadeEditBox extends EditBox {
    private final RetainedUiTree frame = new RetainedUiTree(UiNode.builder("field", "panel").build());
    private Function<Boolean, UiTheme> theme = DivinityUi::searchTheme;

    public void setThemeResolver(Function<Boolean, UiTheme> theme) {
        this.theme = java.util.Objects.requireNonNull(theme);
    }

    public NightshadeEditBox(Font font, int x, int y, int width, int height, Component label) {
        super(font, x + 4, y + 5, width - 8, height - 10, label);
        setBordered(false);
        setTextColor(0xFFE8EEE7);
        setTextColorUneditable(0xFFA8BBC3);
    }

    @Override
    public void setHint(Component hint) {
        super.setHint(hint == null ? null : hint.copy().withStyle(style -> style.withColor(0xA8BBC3)));
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!isVisible()) return;
        frame.setTheme(theme.apply(isFocused()));
        frame.layout(getX() - 4, getY() - 5, width + 8, height + 10);
        frame.render(graphics, mouseX, mouseY, partialTick);
        super.renderWidget(graphics, mouseX, mouseY, partialTick);
    }
}
