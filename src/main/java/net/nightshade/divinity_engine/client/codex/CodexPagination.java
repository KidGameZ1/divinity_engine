package net.nightshade.divinity_engine.client.codex;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

public record CodexPagination(List<FormattedCharSequence> lines, int linesPerPage, int lineHeight) {
    public static CodexPagination wrap(Font font, String text, int width, int height) {
        return wrap(font, Component.literal(text), width, height);
    }

    public static CodexPagination wrap(Font font, Component text, int width, int height) {
        int lineHeight = font.lineHeight + 2;
        return new CodexPagination(List.copyOf(font.split(text, Math.max(1, width))),
                Math.max(1, height / lineHeight), lineHeight);
    }

    public int pageCount() {
        return Math.max(1, (lines.size() + linesPerPage - 1) / linesPerPage);
    }

    public int clamp(int page) {
        return Math.max(0, Math.min(page, pageCount() - 1));
    }

    public List<FormattedCharSequence> page(int page) {
        int start = clamp(page) * linesPerPage;
        return lines.subList(start, Math.min(lines.size(), start + linesPerPage));
    }
}
