package net.nightshade.divinity_engine.api.narrative;

import net.minecraft.resources.ResourceLocation;
import java.util.*;

/** Optional paired epilogues; prerequisites refer to stories, never global allegiance. */
public record GodRelationshipDefinition(ResourceLocation id, List<ResourceLocation> gods, List<ResourceLocation> stories,
                                        String title, String relation, String summary, Map<String, List<String>> epilogues) {
    public GodRelationshipDefinition {
        Objects.requireNonNull(id); Objects.requireNonNull(title); Objects.requireNonNull(relation); Objects.requireNonNull(summary);
        gods = List.copyOf(gods); stories = List.copyOf(stories);
        if (gods.size() != 2 || stories.size() != 2 || gods.get(0).equals(gods.get(1))) throw new IllegalArgumentException("Relationship requires two different gods and two story prerequisites");
        Map<String, List<String>> copy = new LinkedHashMap<>(); epilogues.forEach((key, value) -> copy.put(key, List.copyOf(value)));
        epilogues = Map.copyOf(copy);
        if (!epilogues.keySet().equals(Set.of("aa", "ab", "ba", "bb")) || epilogues.values().stream().anyMatch(List::isEmpty))
            throw new IllegalArgumentException("Relationship requires all four branch combinations");
    }
}
