package net.nightshade.divinity_engine.util.divinity.narrative;

import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.profiling.ProfilerFiller;
import net.nightshade.divinity_engine.DivinityEngineMod;
import net.nightshade.divinity_engine.api.narrative.*;
import java.util.*;

/** Forge's reload apply stage executes on the server executor; malformed packs retain the previous snapshot. */
public final class NarrativeReloadListener extends SimplePreparableReloadListener<NarrativeReloadListener.Prepared> {
    public record Prepared(boolean valid, Map<ResourceLocation, JsonElement> resources) {}
    @Override protected Prepared prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, JsonElement> resources = new LinkedHashMap<>();
        var paths = net.minecraft.resources.FileToIdConverter.json("divine_narratives");
        for (var entry : paths.listMatchingResources(manager).entrySet()) {
            try (var reader = entry.getValue().openAsReader()) {
                var stream = new com.google.gson.stream.JsonReader(reader);
                stream.setLenient(false);
                var json = readStrict(stream, 0);
                if (stream.peek() != com.google.gson.stream.JsonToken.END_DOCUMENT) throw new IllegalArgumentException("Trailing JSON content");
                if (!json.isJsonObject()) throw new IllegalArgumentException("Expected a narrative JSON object");
                resources.put(paths.fileToId(entry.getKey()), json);
            } catch (java.io.IOException | RuntimeException error) {
                // Vanilla's JSON listener skips broken files, which could silently replace an accepted catalog.
                DivinityEngineMod.LOGGER.error("Narrative reload rejected at {}; retaining last valid definitions", entry.getKey(), error);
                return new Prepared(false, Map.of());
            }
        }
        return new Prepared(true, Map.copyOf(resources));
    }
    private static JsonElement readStrict(com.google.gson.stream.JsonReader reader, int depth) throws java.io.IOException {
        if (depth > 64) throw new IllegalArgumentException("JSON nesting exceeds 64 at " + reader.getPath());
        return switch (reader.peek()) {
            case BEGIN_OBJECT -> {
                JsonObject object = new JsonObject(); reader.beginObject();
                while (reader.hasNext()) {
                    String key = reader.nextName();
                    if (object.has(key)) throw new IllegalArgumentException("Duplicate key '" + key + "' at " + reader.getPath());
                    object.add(key, readStrict(reader, depth + 1));
                }
                reader.endObject(); yield object;
            }
            case BEGIN_ARRAY -> {
                JsonArray array = new JsonArray(); reader.beginArray();
                while (reader.hasNext()) array.add(readStrict(reader, depth + 1));
                reader.endArray(); yield array;
            }
            case STRING -> new JsonPrimitive(reader.nextString());
            case NUMBER -> new JsonPrimitive(new java.math.BigDecimal(reader.nextString()));
            case BOOLEAN -> new JsonPrimitive(reader.nextBoolean());
            case NULL -> { reader.nextNull(); yield JsonNull.INSTANCE; }
            default -> throw new IllegalArgumentException("Expected JSON value at " + reader.getPath());
        };
    }
    @Override protected void apply(Prepared prepared, ResourceManager manager, ProfilerFiller profiler) {
        if (!prepared.valid()) return;
        Map<ResourceLocation, JsonElement> resources = prepared.resources();
        Map<ResourceLocation, DivineStoryDefinition> stories = new LinkedHashMap<>();
        Map<ResourceLocation, GodPresenceDefinition> presences = new LinkedHashMap<>();
        Map<ResourceLocation, StoryChronicle> chronicles = new LinkedHashMap<>();
        Map<ResourceLocation, GodRelationshipDefinition> relationships = new LinkedHashMap<>();
        ResourceLocation currentFile = null;
        try {
            for (var entry : resources.entrySet()) {
                currentFile = entry.getKey();
                JsonObject json = entry.getValue().getAsJsonObject();
                int version = json.has("version") ? json.get("version").getAsInt() : 1;
                if (version != 1 && version != 2) throw new IllegalArgumentException("Unsupported narrative version: " + entry.getKey());
                String kind = json.get("kind").getAsString();
                if (kind.equals("story")) {
                    var story = NarrativeDefinitionJson.story(json);
                    if (stories.putIfAbsent(story.id(), story) != null) throw new IllegalArgumentException("Duplicate story " + story.id());
                } else if (kind.equals("presence")) {
                    var presence = NarrativeDefinitionJson.presence(json);
                    if (presences.putIfAbsent(presence.godId(), presence) != null) throw new IllegalArgumentException("Duplicate presence god " + presence.godId());
                } else if (kind.equals("chronicle") && version == 2) {
                    ResourceLocation id = new ResourceLocation(json.get("story").getAsString());
                    if (chronicles.putIfAbsent(id, NarrativeDefinitionJson.chronicle(json)) != null) throw new IllegalArgumentException("Duplicate chronicle " + id);
                } else if (kind.equals("relationship") && version == 2) {
                    var relation = NarrativeDefinitionJson.relationship(json);
                    if (relationships.putIfAbsent(relation.id(), relation) != null) throw new IllegalArgumentException("Duplicate relationship " + relation.id());
                } else throw new IllegalArgumentException("Unknown narrative kind " + kind);
            }
            DivineNarrativeRegistry.reload(stories, presences, chronicles, relationships);
            var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server != null) server.execute(() -> server.getPlayerList().getPlayers().forEach(player -> DivineNarrativeManager.sync(player, true)));
        } catch (RuntimeException error) {
            DivinityEngineMod.LOGGER.error("Narrative reload rejected at {}; retaining last valid definitions", currentFile, error);
        }
    }
}
