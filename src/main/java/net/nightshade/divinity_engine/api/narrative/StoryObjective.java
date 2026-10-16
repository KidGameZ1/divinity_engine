package net.nightshade.divinity_engine.api.narrative;

import net.minecraft.resources.ResourceLocation;
import java.util.Objects;

/** A server-validated deed, independent of its chapter's prose or reward. */
public record StoryObjective(StoryObjectiveType type, String condition, int target,
                             ResourceLocation item, String description) {
    public StoryObjective {
        Objects.requireNonNull(type); Objects.requireNonNull(condition); Objects.requireNonNull(description);
        type.validateCondition(condition);
        if (target < 1 || target > 1000000) throw new IllegalArgumentException("Objective target must be 1..1000000");
        if (type.equals(StoryObjectiveType.DONATION) && item == null) throw new IllegalArgumentException("Donation requires an item");
    }
    public static StoryObjective of(StoryObjectiveType type, String condition, int target, String description) {
        return new StoryObjective(type, condition, target, null, description);
    }
}
