package net.nightshade.divinity_engine.client.codex;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.nightshade.divinity_engine.client.ui.NightshadeButton;

/** Parchment controls retain vanilla keyboard focus, narration and tooltip behavior. */
public final class CodexButton extends NightshadeButton {
    public CodexButton(int x, int y, int width, int height, String text, String tooltip, OnPress action) {
        super(x, y, width, height, Component.literal(text), action);
        setTooltip(Tooltip.create(Component.literal(tooltip)));
    }

    public static String fit(Font font, String text, int width) {
        if (font.width(text) <= width) return text;
        return font.plainSubstrByWidth(text, Math.max(0, width - font.width("..."))) + "...";
    }
}
