package net.nightshade.divinity_engine.api.narrative;

import java.util.List;
import java.util.Objects;

/** Stable local chapter key, Codex paragraphs and a short personal chat line. */
public record StoryChapterDefinition(String id, String title, List<String> paragraphs, String dialogue) {
    public StoryChapterDefinition {
        Objects.requireNonNull(id); Objects.requireNonNull(title); Objects.requireNonNull(dialogue);
        if (!id.matches("[a-z0-9_]+")) throw new IllegalArgumentException("Invalid chapter id " + id);
        paragraphs = List.copyOf(paragraphs);
        if (paragraphs.isEmpty()) throw new IllegalArgumentException("Chapter " + id + " has no text");
    }
    public String text() { return String.join("\n\n", paragraphs); }
}
