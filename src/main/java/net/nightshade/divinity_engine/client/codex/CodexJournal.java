package net.nightshade.divinity_engine.client.codex;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.nightshade.divinity_engine.api.narrative.NarrativeText;
import net.nightshade.divinity_engine.util.divinity.DivineCodexPages;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Read-only journal and registered-page projections; the screen never executes click actions. */
public final class CodexJournal {
    public static final String DATA_KEY = "divinity_engine_narrative_journal";

    private CodexJournal() {}

    public enum Section {
        REGISTERED_PAGES("registered_pages", "Codex Pages"),
        GETTING_STARTED("getting_started", "Getting Started"),
        DISCOVERED_GODS("discovered_gods", "Discovered Gods"),
        STORY_JOURNAL("story_journal", "Story Journal"),
        RELATIONSHIPS("relationships", "Relationships"),
        CHRONICLE("chronicle", "Chronicle");

        public final String id;
        public final String title;

        Section(String id, String title) {
            this.id = id;
            this.title = title;
        }

        public static Section byId(String id) {
            for (Section section : values()) {
                if (section.id.equals(id)) return section;
            }
            return null;
        }

        public Section next(int direction) {
            return values()[Math.floorMod(ordinal() + direction, values().length)];
        }
    }

    public record Entry(String id, Section section, String title, String text,
                        String status, int progress, int target,
                        Component titleComponent, Component textComponent) {
        public Entry(String id, Section section, String title, String text,
                     String status, int progress, int target) {
            this(id, section, title, text, status, progress, target,
                    Component.literal(title), Component.literal(text));
        }

        public boolean matches(String query) {
            String needle = query.strip().toLowerCase(Locale.ROOT);
            return needle.isEmpty() || (title + "\n" + text + "\n" + status)
                    .toLowerCase(Locale.ROOT).contains(needle);
        }

        public String progressLabel() {
            return "Objective: " + progress + " / " + target;
        }

        public String statusLabel() {
            return status.replace('_', ' ');
        }
    }

    public record Snapshot(long revision, boolean synced, List<Entry> entries) {}

    public static Snapshot read(CompoundTag data, List<Entry> registeredPages) {
        List<Entry> entries = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        boolean synced = data.contains("entries", Tag.TAG_LIST);
        ListTag list = data.getList("entries", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            Section section = Section.byId(tag.getString("section"));
            String id = tag.getString("id");
            if (section == null || id.isBlank() || !tag.contains("title", Tag.TAG_STRING)
                    || !tag.contains("text", Tag.TAG_STRING) || !ids.add(id)) continue;
            String title = NarrativeText.resolve(tag.getString("title"));
            entries.add(new Entry(id, section, title.isBlank() ? "Untitled entry" : title,
                    NarrativeText.resolve(tag.getString("text")), NarrativeText.resolve(tag.getString("status")),
                    Math.max(0, tag.getInt("progress")), Math.max(0, tag.getInt("target"))));
        }
        for (Entry entry : registeredPages) {
            if (ids.add(entry.id())) entries.add(entry);
        }
        return new Snapshot(data.getLong("revision"), synced, List.copyOf(entries));
    }

    public static List<Entry> registeredPages(DivineCodexPages pages) {
        return pageEntries(pages, 0, Section.REGISTERED_PAGES);
    }

    /** Compatibility helper for callers that explicitly want only addon guides. */
    public static List<Entry> addonGuides(DivineCodexPages pages, int addonStartPage) {
        return pageEntries(pages, addonStartPage, Section.GETTING_STARTED);
    }

    private static List<Entry> pageEntries(DivineCodexPages pages, int start, Section section) {
        List<Entry> guides = new ArrayList<>();
        Map<String, Integer> occurrences = new HashMap<>();
        int size = Math.min(pages.sizeTitles(), pages.sizeContents());
        for (int i = Math.max(0, start); i < size; i++) {
            Component titleComponent = pages.getTitle(i);
            String title = titleComponent == null ? "" : titleComponent.getString();
            String identity = titleComponent == null ? "" : Component.Serializer.toJson(titleComponent);
            Component content = pages.getContent(i);
            if (title.isBlank()) {
                title = "Page " + (i + 1);
                titleComponent = Component.literal(title);
            }
            if (content == null) content = Component.empty();
            int occurrence = occurrences.merge(identity, 1, Integer::sum);
            String id = "legacy:" + UUID.nameUUIDFromBytes(identity.getBytes(StandardCharsets.UTF_8))
                    + ":" + occurrence;
            // Separate registrations stay distinct even when their titles match or are blank.
            guides.add(new Entry(id, section, title, content.getString(),
                    "Registered page " + (i + 1) + " / " + size, 0, 0,
                    titleComponent.copy(), content.copy()));
        }
        return List.copyOf(guides);
    }
}
