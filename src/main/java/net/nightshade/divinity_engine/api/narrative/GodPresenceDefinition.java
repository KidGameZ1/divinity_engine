package net.nightshade.divinity_engine.api.narrative;

import net.minecraft.resources.ResourceLocation;
import java.util.Map;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Objects;

/** Dialogue keys: greeting, prayer, offering, return, failure, victory, recognition and favor tier names. */
public record GodPresenceDefinition(ResourceLocation godId, int color, ResourceLocation texture,
                                    ResourceLocation sound, Map<String, List<String>> dialogue) {
    public GodPresenceDefinition {
        Objects.requireNonNull(godId); Objects.requireNonNull(texture); Objects.requireNonNull(sound);
        Map<String, List<String>> copy = new LinkedHashMap<>();
        dialogue.forEach((key, lines) -> { if (!lines.isEmpty()) copy.put(key, List.copyOf(lines)); });
        dialogue = Map.copyOf(copy);
        for (String key : List.of("greeting", "prayer", "offering"))
            if (dialogue.getOrDefault(key, List.of()).size() < 3) throw new IllegalArgumentException("Three variants required for " + key);
    }
    public static Builder builder(ResourceLocation god) { return new Builder(god); }
    public static final class Builder {
        private final ResourceLocation god;
        private int color = 0xFFFFFF;
        private ResourceLocation texture = new ResourceLocation("minecraft", "textures/particle/glitter_0.png");
        private ResourceLocation sound = new ResourceLocation("minecraft", "block.amethyst_block.chime");
        private final Map<String, List<String>> dialogue = new LinkedHashMap<>();
        private Builder(ResourceLocation god) { this.god = god; }
        public Builder color(int value) { color = value & 0xFFFFFF; return this; }
        public Builder texture(ResourceLocation value) { texture = value; return this; }
        public Builder sound(ResourceLocation value) { sound = value; return this; }
        public Builder lines(String event, String... lines) { dialogue.put(event, List.of(lines)); return this; }
        public GodPresenceDefinition build() { return new GodPresenceDefinition(god, color, texture, sound, dialogue); }
    }
}
