package net.nightshade.divinity_engine.util.divinity.narrative;

import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import net.nightshade.divinity_engine.api.narrative.*;
import java.util.*;

/** Version 1/2 pack adapters; no executable scripts or mutable blessing parameters. */
public final class NarrativeDefinitionJson {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private NarrativeDefinitionJson() {}
    public static String encodeBranch(DivineStoryDefinition.Branch branch) { return GSON.toJson(branchJson(branch)); }
    public static DivineStoryDefinition.Branch decodeBranch(String text) { return branch(JsonParser.parseString(text).getAsJsonObject()); }
    public static DivineStoryDefinition.Branch branch(JsonObject json) {
        String item = optional(json, "item", "");
        JsonObject reward = json.getAsJsonObject("reward");
        return new DivineStoryDefinition.Branch(text(json, "name"),
            StoryObjectiveType.get(new ResourceLocation(text(json, "objective"))), optional(json, "condition", ""),
            integer(json, "target"), item.isEmpty() ? null : new ResourceLocation(item), text(json, "deed"),
            BlessingSpecialization.builder(new ResourceLocation(text(reward, "id")))
                .blessing(new ResourceLocation(text(reward, "blessing"))).description(text(reward, "description")).build(),
            text(json, "ending"));
    }
    public static JsonObject branchJson(DivineStoryDefinition.Branch path) {
        JsonObject json = new JsonObject();
        json.addProperty("name", path.name()); json.addProperty("objective", path.objective().id().toString());
        json.addProperty("condition", path.condition()); json.addProperty("target", path.target());
        if (path.donationItem() != null) json.addProperty("item", path.donationItem().toString());
        json.addProperty("deed", path.deed()); json.addProperty("ending", path.ending());
        JsonObject reward = new JsonObject(); reward.addProperty("id", path.reward().id().toString());
        reward.addProperty("blessing", path.reward().blessingId().toString()); reward.addProperty("description", path.reward().description());
        json.add("reward", reward); return json;
    }
    public static DivineStoryDefinition story(JsonObject json) {
        var builder = DivineStoryDefinition.builder(new ResourceLocation(text(json, "god"))).title(text(json, "title")).request(text(json, "request"));
        if (json.has("id")) builder.id(new ResourceLocation(text(json, "id")));
        if (json.has("chronicle")) builder.chronicle(chronicle(json.getAsJsonObject("chronicle")));
        JsonObject paths = json.getAsJsonObject("branches");
        builder.branch("a", branch(paths.getAsJsonObject("a"))).branch("b", branch(paths.getAsJsonObject("b")));
        return builder.build();
    }
    public static StoryObjective objective(JsonObject json) {
        String item = optional(json, "item", "");
        var type = StoryObjectiveType.get(new ResourceLocation(text(json, "objective")));
        String condition = optional(json, "condition", "");
        return new StoryObjective(type, condition, integer(json, "target"), item.isEmpty() ? null : new ResourceLocation(item), text(json, "deed"));
    }
    public static StoryChronicle chronicle(JsonObject json) {
        var builder = StoryChronicle.builder().premise(text(json, "premise"))
            .witness(objective(json.getAsJsonObject("witness"))).reckoning(objective(json.getAsJsonObject("reckoning")));
        if (json.has("offering_examples")) json.getAsJsonArray("offering_examples").forEach(item -> builder.offering(new ResourceLocation(item.getAsString())));
        var passages = json.getAsJsonObject("passages");
        var dialogue = json.has("dialogue") ? json.getAsJsonObject("dialogue") : new JsonObject();
        var titles = json.has("chapter_titles") ? json.getAsJsonObject("chapter_titles") : new JsonObject();
        for (String key : passages.keySet()) {
            try {
                List<String> paragraphs = paragraphs(passages.get(key));
                builder.chapter(new StoryChapterDefinition(key, optional(titles, key, chapterTitle(key)), paragraphs, optional(dialogue, key, "Read the new chapter in your Divine Codex.")));
            } catch (RuntimeException error) { throw new IllegalArgumentException("Chapter '" + key + "': " + error.getMessage(), error); }
        }
        return builder.build();
    }
    private static List<String> paragraphs(JsonElement value) {
        List<String> result = new ArrayList<>();
        if (value.isJsonArray()) value.getAsJsonArray().forEach(line -> result.add(textValue(line)));
        else result.add(textValue(value));
        if (result.size() > 32) throw new IllegalArgumentException("At most 32 paragraphs per passage");
        return List.copyOf(result);
    }
    public static String chapterTitle(String key) {
        return switch (key.replace("_a", "").replace("_b", "")) {
            case "call" -> "1. The Call"; case "witness" -> "2. A Witness";
            case "testimony" -> "3. Divided Testimony"; case "deed" -> "4. The Chosen Deed";
            case "reckoning" -> "5. The Reckoning"; case "trial" -> "6. The Trial"; case "ending" -> "7. The Return";
            default -> key;
        };
    }
    public static GodRelationshipDefinition relationship(JsonObject json) {
        List<ResourceLocation> gods = new ArrayList<>(), stories = new ArrayList<>();
        json.getAsJsonArray("gods").forEach(id -> gods.add(new ResourceLocation(id.getAsString())));
        if (json.has("stories")) json.getAsJsonArray("stories").forEach(id -> stories.add(new ResourceLocation(id.getAsString())));
        else stories.addAll(gods);
        Map<String, List<String>> endings = new LinkedHashMap<>();
        json.getAsJsonObject("epilogues").entrySet().forEach(entry -> endings.put(entry.getKey(), paragraphs(entry.getValue())));
        return new GodRelationshipDefinition(new ResourceLocation(text(json, "id")), gods, stories, text(json, "title"), text(json, "relation"), text(json, "summary"), endings);
    }
    public static String encodeStory(DivineStoryDefinition definition) {
        JsonObject json = new JsonObject(); json.addProperty("id", definition.id().toString()); json.addProperty("god", definition.godId().toString());
        json.addProperty("title", definition.title()); json.addProperty("request", definition.request());
        JsonObject branches = new JsonObject(); definition.branches().forEach((key, value) -> branches.add(key, branchJson(value))); json.add("branches", branches);
        if (definition.chronicle().expanded()) {
            StoryChronicle c = definition.chronicle(); JsonObject lore = new JsonObject(); lore.addProperty("premise", c.premise());
            JsonArray offerings = new JsonArray(); c.offeringExamples().forEach(item -> offerings.add(item.toString())); lore.add("offering_examples", offerings);
            lore.add("witness", objectiveJson(c.witness())); lore.add("reckoning", objectiveJson(c.reckoning()));
            JsonObject passages = new JsonObject(), dialogue = new JsonObject(), titles = new JsonObject();
            c.passages().forEach((key, value) -> { JsonArray lines = new JsonArray(); value.paragraphs().forEach(lines::add); passages.add(key, lines); dialogue.addProperty(key, value.dialogue()); titles.addProperty(key, value.title()); });
            lore.add("passages", passages); lore.add("dialogue", dialogue); lore.add("chapter_titles", titles); json.add("chronicle", lore);
        }
        return GSON.toJson(json);
    }
    public static DivineStoryDefinition decodeStory(String value) { return story(JsonParser.parseString(value).getAsJsonObject()); }
    private static JsonObject objectiveJson(StoryObjective objective) {
        JsonObject json = new JsonObject(); json.addProperty("objective", objective.type().id().toString()); json.addProperty("condition", objective.condition());
        json.addProperty("target", objective.target()); json.addProperty("deed", objective.description());
        if (objective.item() != null) json.addProperty("item", objective.item().toString()); return json;
    }
    public static GodPresenceDefinition presence(JsonObject json) {
        var builder = GodPresenceDefinition.builder(new ResourceLocation(text(json, "god")))
            .color(Integer.parseUnsignedInt(optional(json, "color", "ffffff").replace("#", ""), 16))
            .texture(new ResourceLocation(text(json, "texture"))).sound(new ResourceLocation(text(json, "sound")));
        JsonObject lines = json.getAsJsonObject("dialogue");
        for (String key : lines.keySet()) {
            JsonArray values = lines.getAsJsonArray(key);
            if (values.size() > 32) throw new IllegalArgumentException("At most 32 dialogue variants");
            List<String> entries = new ArrayList<>(); values.forEach(value -> entries.add(bounded(value.getAsString())));
            builder.lines(key, entries.toArray(String[]::new));
        }
        return builder.build();
    }
    private static String text(JsonObject json, String key) {
        if (json == null || !json.has(key)) throw new IllegalArgumentException("Missing narrative field " + key);
        return textValue(json.get(key));
    }
    private static int integer(JsonObject json, String key) {
        try { return json.get(key).getAsBigDecimal().intValueExact(); }
        catch (RuntimeException error) { throw new IllegalArgumentException("Field '" + key + "' must be an exact 32-bit integer", error); }
    }
    private static String textValue(JsonElement value) {
        if (value.isJsonObject() && value.getAsJsonObject().has("translate")) return NarrativeText.translatable(value.getAsJsonObject().get("translate").getAsString());
        return bounded(value.getAsString());
    }
    private static String optional(JsonObject json, String key, String fallback) { return json.has(key) ? text(json, key) : fallback; }
    private static String bounded(String value) { if (value.length() > 4096) throw new IllegalArgumentException("Narrative text too long"); return value; }
}
