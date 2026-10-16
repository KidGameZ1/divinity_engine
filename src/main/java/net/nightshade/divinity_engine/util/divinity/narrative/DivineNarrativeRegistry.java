package net.nightshade.divinity_engine.util.divinity.narrative;

import net.minecraft.resources.ResourceLocation;
import net.nightshade.divinity_engine.api.narrative.*;
import net.nightshade.divinity_engine.registry.compat.LegacyElderContent;
import java.util.*;

/** Definition registry, not a runtime session store. Reloads replace an immutable snapshot atomically. */
public final class DivineNarrativeRegistry {
    private static final Map<ResourceLocation, DivineStoryDefinition> BASE_STORIES = new LinkedHashMap<>();
    private static final Map<ResourceLocation, GodPresenceDefinition> BASE_PRESENCES = new LinkedHashMap<>();
    private static final Map<ResourceLocation, GodRelationshipDefinition> BASE_RELATIONSHIPS = new LinkedHashMap<>();
    private static final List<DivineNarrativeDeferredRegister> DEFERRED = new ArrayList<>();
    private static volatile Map<ResourceLocation, DivineStoryDefinition> stories = Map.of();
    private static volatile Map<ResourceLocation, GodPresenceDefinition> presences = Map.of();
    private static volatile Map<ResourceLocation, GodRelationshipDefinition> relationships = Map.of();
    private static long revision;
    private static boolean initialized;
    public static ResourceLocation id(String path) { return new ResourceLocation("divinity_engine", path); }
    public static void addDeferredRegister(DivineNarrativeDeferredRegister register) {
        if (initialized) throw new IllegalStateException("Submit narrative definitions before common setup");
        if (!DEFERRED.contains(register)) DEFERRED.add(register);
    }
    public static void registerStory(ResourceLocation id, DivineStoryDefinition story) {
        if (BASE_STORIES.putIfAbsent(id, story.withId(id)) != null) throw new IllegalArgumentException("Duplicate story " + id);
    }
    public static void registerPresence(ResourceLocation id, GodPresenceDefinition presence) {
        if (BASE_PRESENCES.putIfAbsent(presence.godId(), presence) != null) throw new IllegalArgumentException("Duplicate presence for " + presence.godId());
    }
    public static DivineStoryDefinition story(ResourceLocation god) {
        var exact = stories.get(god);
        return exact != null ? exact : stories.values().stream().filter(def -> def.godId().equals(god)).sorted(Comparator.comparing(def -> def.id().toString())).findFirst().orElse(null);
    }
    public static DivineStoryDefinition byId(ResourceLocation id) { return stories.get(id); }
    public static List<DivineStoryDefinition> forGod(ResourceLocation god) { return stories.values().stream().filter(def -> def.godId().equals(god)).sorted(Comparator.comparing(def -> def.id().toString())).toList(); }
    public static boolean primary(DivineStoryDefinition def) { var primary = story(def.godId()); return primary != null && primary.id().equals(def.id()); }
    public static long revision() { return revision; }
    public static Collection<GodRelationshipDefinition> relationships() { return relationships.values(); }
    public static GodRelationshipDefinition relationship(ResourceLocation id) { return relationships.get(id); }
    public static void registerRelationship(ResourceLocation id, GodRelationshipDefinition definition) {
        if (!id.equals(definition.id()) || BASE_RELATIONSHIPS.putIfAbsent(id, definition) != null) throw new IllegalArgumentException("Duplicate or mismatched relationship " + id);
    }
    public static GodPresenceDefinition presence(ResourceLocation god) { return presences.get(god); }
    public static Collection<DivineStoryDefinition> stories() { return stories.values(); }
    public static void initialize() {
        if (initialized) return;
        for (BuiltinNarrativeCatalog.God god : BuiltinNarrativeCatalog.entries()) {
            var builder = DivineStoryDefinition.builder(id(god.id())).title(god.title()).request(god.request());
            builder.branch("a", branch(god, "a", god.a())).branch("b", branch(god, "b", god.b()));
            var lore = builtin(god.id() + "_chronicle");
            if (lore != null) builder.chronicle(NarrativeDefinitionJson.chronicle(lore));
            registerStory(id(god.id()), builder.build());
            registerPresence(id(god.id()), BuiltinPresenceDefinitions.create(god));
        }
        for (String pair : List.of("dawn_dream", "stars_time", "serpent_archive", "tide_winter", "storm_horizon", "stone_machine", "root_farewell", "pack_valor", "light_ember")) {
            var json = builtin(pair + "_relationship");
            if (json != null) { var relationship = NarrativeDefinitionJson.relationship(json); registerRelationship(relationship.id(), relationship); }
        }
        DEFERRED.forEach(DivineNarrativeDeferredRegister::registerAll);
        initialized = true; reload(Map.of(), Map.of());
    }
    private static com.google.gson.JsonObject builtin(String name) {
        String path = "/data/divinity_engine/divine_narratives/" + name + ".json";
        try (var stream = DivineNarrativeRegistry.class.getResourceAsStream(path)) {
            if (stream == null) return null;
            return com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (java.io.IOException | RuntimeException exception) { throw new IllegalStateException("Invalid built-in narrative " + path, exception); }
    }
    static DivineStoryDefinition.Branch branch(BuiltinNarrativeCatalog.God god, String key, BuiltinNarrativeCatalog.Path path) {
        return new DivineStoryDefinition.Branch(path.name(), StoryObjectiveType.get(id(path.type())), path.condition(), path.target(),
            path.item().isEmpty() ? null : new ResourceLocation("minecraft", path.item()), path.deed(),
            BlessingSpecialization.builder(id(god.id() + "_" + key)).blessing(id(god.blessing())).description(path.reward()).build(), path.ending());
    }
    public static void reload(Map<ResourceLocation, DivineStoryDefinition> storyOverrides, Map<ResourceLocation, GodPresenceDefinition> presenceOverrides) {
        reload(storyOverrides, presenceOverrides, Map.of(), Map.of());
    }
    public static void reload(Map<ResourceLocation, DivineStoryDefinition> storyOverrides, Map<ResourceLocation, GodPresenceDefinition> presenceOverrides,
                              Map<ResourceLocation, StoryChronicle> chronicles, Map<ResourceLocation, GodRelationshipDefinition> relationshipOverrides) {
        Map<ResourceLocation, DivineStoryDefinition> nextStories = new LinkedHashMap<>(BASE_STORIES); nextStories.putAll(storyOverrides);
        Set<ResourceLocation> retiredStories = new HashSet<>();
        nextStories.forEach((id, definition) -> {
            if (LegacyElderContent.isGod(definition.godId())) retiredStories.add(id);
        });
        // Old datapacks must not revive removed built-ins or expose their saved story snapshots.
        nextStories.keySet().removeAll(retiredStories);
        nextStories.forEach((id, definition) -> {
            if (!id.equals(definition.id())) throw new IllegalArgumentException("Story registration ID " + id + " does not match " + definition.id());
            var previous = stories.get(id);
            if (previous != null && !previous.godId().equals(definition.godId()))
                throw new IllegalArgumentException("Story " + id + " cannot change its bound god; use a new stable story ID");
        });
        chronicles.forEach((id, chronicle) -> {
            if (retiredStories.contains(id) || LegacyElderContent.isGod(id)) return;
            var definition = nextStories.get(id);
            if (definition == null) throw new IllegalArgumentException("Chronicle references missing story " + id);
            nextStories.put(id, definition.withChronicle(chronicle));
        });
        Map<ResourceLocation, GodPresenceDefinition> nextPresences = new LinkedHashMap<>(BASE_PRESENCES); nextPresences.putAll(presenceOverrides);
        nextPresences.entrySet().removeIf(entry -> LegacyElderContent.isGod(entry.getValue().godId()));
        Map<ResourceLocation, GodRelationshipDefinition> nextRelations = new LinkedHashMap<>(BASE_RELATIONSHIPS); nextRelations.putAll(relationshipOverrides);
        nextRelations.values().removeIf(relation -> relation.gods().stream().anyMatch(LegacyElderContent::isGod));
        nextRelations.forEach((id, relation) -> {
            for (int i = 0; i < relation.stories().size(); i++) {
                var story = nextStories.get(relation.stories().get(i));
                // Missing optional addons disable only their epilogue, never a base story.
                if (story != null && !story.godId().equals(relation.gods().get(i)))
                    throw new IllegalArgumentException("Relationship " + id + " binds story " + story.id() + " to the wrong god");
            }
        });
        stories = Map.copyOf(nextStories); presences = Map.copyOf(nextPresences); relationships = Map.copyOf(nextRelations); revision++;
    }
}
