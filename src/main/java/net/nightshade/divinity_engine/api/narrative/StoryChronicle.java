package net.nightshade.divinity_engine.api.narrative;

import net.minecraft.resources.ResourceLocation;
import java.util.*;

/** The seven-chapter extension. Empty chronicles keep version-one quests compatible. */
public record StoryChronicle(String premise, List<ResourceLocation> offeringExamples, StoryObjective witness,
                             StoryObjective reckoning, Map<String, StoryChapterDefinition> passages) {
    public static final List<String> REQUIRED = List.of("call", "witness", "testimony", "deed_a", "deed_b",
        "reckoning_a", "reckoning_b", "trial", "ending_a", "ending_b");
    public static final StoryChronicle EMPTY = new StoryChronicle("", List.of(), null, null, Map.of());
    public StoryChronicle {
        Objects.requireNonNull(premise); offeringExamples = List.copyOf(offeringExamples); passages = Map.copyOf(passages);
        if (!passages.isEmpty() && (witness == null || reckoning == null || !passages.keySet().containsAll(REQUIRED)))
            throw new IllegalArgumentException("Seven-chapter chronicle requires witness, reckoning, and all ten branch passages");
    }
    public boolean expanded() { return !passages.isEmpty(); }
    public static Builder builder() { return new Builder(); }
    public static final class Builder {
        private String premise = "";
        private final List<ResourceLocation> offerings = new ArrayList<>();
        private StoryObjective witness, reckoning;
        private final Map<String, StoryChapterDefinition> passages = new LinkedHashMap<>();
        public Builder premise(String value) { premise = value; return this; }
        public Builder offering(ResourceLocation value) { offerings.add(value); return this; }
        public Builder witness(StoryObjective value) { witness = value; return this; }
        public Builder reckoning(StoryObjective value) { reckoning = value; return this; }
        public Builder chapter(StoryChapterDefinition value) {
            if (passages.putIfAbsent(value.id(), value) != null) throw new IllegalArgumentException("Duplicate chapter " + value.id());
            return this;
        }
        public StoryChronicle build() { return new StoryChronicle(premise, offerings, witness, reckoning, passages); }
    }
}
