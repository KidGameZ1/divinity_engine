package net.nightshade.divinity_engine.api.narrative;

import net.minecraft.resources.ResourceLocation;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Addons may register objective types and report verified server events through the narrative manager. */
public record StoryObjectiveType(ResourceLocation id, boolean continuous, int timeLimitTicks) {
    private static final Map<ResourceLocation, StoryObjectiveType> TYPES = new LinkedHashMap<>();
    private static volatile long registrationRevision;
    public static final StoryObjectiveType DONATION = builtin("donation", false, 0);
    public static final StoryObjectiveType KILL = builtin("kill", false, 0);
    public static final StoryObjectiveType TIMED_KILL = builtin("timed_kill", false, 2400);
    public static final StoryObjectiveType PRAY = builtin("pray", true, 0);
    public static final StoryObjectiveType HIDDEN = builtin("hidden", true, 0);
    public static final StoryObjectiveType SUBMERGED = builtin("submerged", true, 0);
    public static final StoryObjectiveType SMELT = builtin("smelt", false, 0);
    public static final StoryObjectiveType MINE = builtin("mine", false, 0);
    public static final StoryObjectiveType BREED = builtin("breed", false, 0);
    public static final StoryObjectiveType SHIELD = builtin("shield", false, 0);
    public static final StoryObjectiveType XP = builtin("xp", false, 0);
    public static final StoryObjectiveType SPRINT = builtin("sprint", false, 0);
    public static final StoryObjectiveType HEIGHT = builtin("height", false, 0);
    public static final StoryObjectiveType PLANT = builtin("plant", false, 0);
    public static final StoryObjectiveType HARVEST = builtin("harvest", false, 0);
    public static final StoryObjectiveType OBSERVE = builtin("observe", true, 0);

    public StoryObjectiveType { Objects.requireNonNull(id); if (timeLimitTicks < 0) throw new IllegalArgumentException("time limit"); }
    public static synchronized StoryObjectiveType register(StoryObjectiveType type) {
        if (TYPES.putIfAbsent(type.id(), type) != null) throw new IllegalArgumentException("Duplicate objective " + type.id());
        registrationRevision++;
        return type;
    }
    public static long registrationRevision() { return registrationRevision; }
    public void validateCondition(String condition) {
        java.util.Set<String> supported = equals(OBSERVE)
            ? java.util.Set.of("day_sky", "night_sky", "forest", "rain", "thunder", "underground", "high_sky", "water", "snow", "firelight")
            : equals(KILL) || equals(TIMED_KILL)
                ? java.util.Set.of("undead_day", "undead_night", "poisoned", "forest", "melee", "projectile", "phantom", "stray", "drowned", "witch", "burning", "any")
                : equals(PRAY) ? java.util.Set.of("", "still", "night_safe", "sky_night") : null;
        if (supported != null && !supported.contains(condition))
            throw new IllegalArgumentException("Unsupported condition '" + condition + "' for " + id + "; expected " + supported);
    }
    public static StoryObjectiveType get(ResourceLocation id) {
        StoryObjectiveType type = TYPES.get(id);
        if (type == null) throw new IllegalArgumentException("Unknown objective " + id);
        return type;
    }
    private static StoryObjectiveType builtin(String id, boolean continuous, int limit) {
        return register(new StoryObjectiveType(new ResourceLocation("divinity_engine", id), continuous, limit));
    }
}
