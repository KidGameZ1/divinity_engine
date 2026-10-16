package net.nightshade.divinity_engine.api.narrative;

import net.minecraft.resources.ResourceLocation;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Objects;

/** Immutable, reloadable story definition. Branch keys are permanently persisted, not display names. */
public record DivineStoryDefinition(ResourceLocation id, ResourceLocation godId, String title, String request,
                                    Map<String, Branch> branches, StoryChronicle chronicle) {
    public DivineStoryDefinition(ResourceLocation godId, String title, String request, Map<String, Branch> branches) {
        this(godId, godId, title, request, branches, StoryChronicle.EMPTY);
    }
    public DivineStoryDefinition {
        Objects.requireNonNull(id); Objects.requireNonNull(godId); Objects.requireNonNull(title); Objects.requireNonNull(request); Objects.requireNonNull(chronicle);
        branches = Map.copyOf(branches);
        if (!branches.keySet().equals(java.util.Set.of("a", "b"))) throw new IllegalArgumentException("Stories require branches a and b");
    }
    public DivineStoryDefinition withId(ResourceLocation value) { return new DivineStoryDefinition(value, godId, title, request, branches, chronicle); }
    public DivineStoryDefinition withChronicle(StoryChronicle value) { return new DivineStoryDefinition(id, godId, title, request, branches, value); }
    public record Branch(String name, StoryObjectiveType objective, String condition, int target,
                         ResourceLocation donationItem, String deed, BlessingSpecialization reward, String ending) {
        public Branch {
            Objects.requireNonNull(name); Objects.requireNonNull(objective); Objects.requireNonNull(condition);
            Objects.requireNonNull(deed); Objects.requireNonNull(reward); Objects.requireNonNull(ending);
            objective.validateCondition(condition);
            if (target < 1 || target > 1000000) throw new IllegalArgumentException("Invalid objective target");
            if (objective.equals(StoryObjectiveType.DONATION) && donationItem == null) throw new IllegalArgumentException("Donation item required");
        }
    }
    public static Builder builder(ResourceLocation godId) { return new Builder(godId); }
    public static final class Builder {
        private final ResourceLocation god;
        private ResourceLocation id;
        private StoryChronicle chronicle = StoryChronicle.EMPTY;
        private String title = "", request = "";
        private final Map<String, Branch> branches = new LinkedHashMap<>();
        private Builder(ResourceLocation god) { this.god = god; this.id = god; }
        public Builder id(ResourceLocation value) { id = value; return this; }
        public Builder chronicle(StoryChronicle value) { chronicle = value; return this; }
        public Builder title(String value) { title = value; return this; }
        public Builder request(String value) { request = value; return this; }
        public Builder branch(String key, Branch value) { branches.put(key, value); return this; }
        public DivineStoryDefinition build() { return new DivineStoryDefinition(id, god, title, request, branches, chronicle); }
    }
}
