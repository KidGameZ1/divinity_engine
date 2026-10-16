package net.nightshade.divinity_engine.util.content;

import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

/** Strict, bounded startup definitions. No executable strings or arbitrary entity NBT. */
public final class ContentSchema {
    public static final Set<String> HOOKS = Set.of("pressed", "tick", "damage_entity", "taken_damage",
            "kill_entity", "toggle_on", "toggle_off");
    public record Action(String type, String target, double amount, int ticks, int amplifier,
                         double radius, ResourceLocation effect) { }
    public record Blessing(int favor, int cooldown, String mode, int interval, int color,
                           Map<String, List<Action>> hooks, ResourceLocation texture) { }
    public record God(Set<String> domains, List<ResourceLocation> blessings, Map<ResourceLocation, Integer> offerings,
                      int prayerFavor, int color) { }
    public record Spawn(ResourceLocation entity, int count) { }
    public record Phase(String name, String objective, int ticks, String goal, List<Spawn> spawns) { }
    public record Trial(ResourceLocation god, int favor, String start, String complete, List<Phase> phases) { }

    private ContentSchema() { }

    public static Blessing blessing(JsonObject json) {
        keys(json, "schema_version", "favor", "cooldown_seconds", "mode", "tick_interval", "color", "hooks", "particle_texture");
        version(json);
        String mode = choice(json, "mode", "active", Set.of("active", "passive", "toggle"));
        Map<String, List<Action>> hooks = new LinkedHashMap<>();
        JsonObject configured = object(json, "hooks", new JsonObject());
        for (var entry : configured.entrySet()) {
            if (!HOOKS.contains(entry.getKey())) throw bad("hooks." + entry.getKey(), "unsupported hook");
            // Match ContentBlessing and the shared key, tick, and combat dispatchers.
            boolean reachable = switch (entry.getKey()) {
                case "pressed" -> mode.equals("active");
                case "toggle_on", "toggle_off" -> mode.equals("toggle");
                default -> !mode.equals("active");
            };
            if (!reachable) throw bad("hooks." + entry.getKey(), "not available in mode " + mode);
            List<Action> actions = new ArrayList<>();
            for (JsonElement action : array(entry.getValue(), "hooks." + entry.getKey(), 16)) actions.add(action(action.getAsJsonObject()));
            hooks.put(entry.getKey(), List.copyOf(actions));
        }
        ResourceLocation texture = json.has("particle_texture") ? id(text(json, "particle_texture")) : null;
        if (texture != null && (!texture.getPath().startsWith("textures/particle/") || !texture.getPath().endsWith(".png")))
            throw bad("particle_texture", "use a full textures/particle/*.png resource path");
        return new Blessing(integer(json, "favor", 0, -250, 250), integer(json, "cooldown_seconds", 10, 0, 3600),
                mode, integer(json, "tick_interval", 20, 5, 1200), color(json), Collections.unmodifiableMap(hooks), texture);
    }

    private static Action action(JsonObject json) {
        String type = choice(json, "type", "", Set.of("heal", "effect", "ignite", "damage", "dash"));
        Set<String> allowed = new HashSet<>(Set.of("type", "target", "radius"));
        switch (type) {
            case "heal", "damage", "dash" -> allowed.add("amount");
            case "ignite" -> allowed.add("ticks");
            case "effect" -> allowed.addAll(Set.of("effect", "ticks", "amplifier"));
            default -> throw bad("type", "unsupported action");
        }
        keys(json, allowed.toArray(String[]::new));
        String target = choice(json, "target", "self", Set.of("self", "target", "nearby_hostiles"));
        if (type.equals("dash") && !target.equals("self")) throw bad("target", "dash only supports self");
        return new Action(type, target, number(json, "amount", 1, .01, type.equals("dash") ? 3 : 40),
                integer(json, "ticks", 60, 1, 12000), integer(json, "amplifier", 0, 0, 4),
                number(json, "radius", 4, .5, 12), type.equals("effect") ? id(text(json, "effect")) : null);
    }

    public static God god(JsonObject json) {
        keys(json, "schema_version", "domains", "blessings", "offerings", "prayer_favor", "color");
        version(json);
        Set<String> domains = new LinkedHashSet<>();
        for (JsonElement domain : array(json.get("domains"), "domains", 16)) {
            String value = domain.getAsString();
            if (value.isBlank() || value.length() > 64) throw bad("domains", "names must contain 1..64 characters");
            domains.add(value);
        }
        List<ResourceLocation> blessings = new ArrayList<>();
        for (JsonElement blessing : array(json.get("blessings"), "blessings", 64)) blessings.add(id(blessing.getAsString()));
        if (new HashSet<>(blessings).size() != blessings.size()) throw bad("blessings", "duplicate ID");
        Map<ResourceLocation, Integer> offerings = new LinkedHashMap<>();
        JsonObject values = object(json, "offerings", new JsonObject());
        if (values.size() > 64) throw bad("offerings", "maximum 64 item IDs");
        values.entrySet().forEach(entry -> offerings.put(id(entry.getKey()), integer(values, entry.getKey(), 1, 1, 12)));
        return new God(Set.copyOf(domains), List.copyOf(blessings), Map.copyOf(offerings),
                integer(json, "prayer_favor", 1, 0, 2), color(json));
    }

    public static Trial trial(JsonObject json) {
        keys(json, "schema_version", "god", "reward_favor", "start_message", "complete_message", "phases");
        version(json);
        List<Phase> phases = new ArrayList<>();
        for (JsonElement entry : array(json.get("phases"), "phases", 8)) {
            JsonObject phase = entry.getAsJsonObject();
            keys(phase, "name", "objective", "duration_ticks", "goal", "spawns");
            String goal = choice(phase, "goal", "defeat_all", Set.of("defeat_all", "survive"));
            List<Spawn> spawns = new ArrayList<>();
            for (JsonElement mob : array(phase.has("spawns") ? phase.get("spawns") : new JsonArray(), "spawns", 8)) {
                JsonObject spawn = mob.getAsJsonObject();
                keys(spawn, "entity", "count");
                spawns.add(new Spawn(id(text(spawn, "entity")), integer(spawn, "count", 1, 1, 16)));
            }
            int count = spawns.stream().mapToInt(Spawn::count).sum();
            if (count > 32 || goal.equals("defeat_all") && count == 0) throw bad("spawns", "defeat_all requires 1..32 mobs; survive permits 0..32");
            phases.add(new Phase(text(phase, "name"), text(phase, "objective"),
                    integer(phase, "duration_ticks", 600, 20, 12000), goal, List.copyOf(spawns)));
        }
        if (phases.isEmpty()) throw bad("phases", "at least one phase is required");
        return new Trial(id(text(json, "god")), integer(json, "reward_favor", 25, 0, 50),
                optionalText(json, "start_message", "The trial begins."), optionalText(json, "complete_message", "Trial completed."), List.copyOf(phases));
    }

    public static ResourceLocation id(String value) {
        ResourceLocation id = ResourceLocation.tryParse(value);
        if (id == null || !value.contains(":") || value.length() > 200 || value.contains(".."))
            throw bad("id", "expected an explicit namespace:path: " + value);
        return id;
    }

    private static int color(JsonObject json) {
        String color = optionalText(json, "color", "#C5D9EB");
        if (!color.matches("#[0-9a-fA-F]{6}")) throw bad("color", "expected #RRGGBB");
        return Integer.parseInt(color.substring(1), 16);
    }

    private static void version(JsonObject json) {
        if (integer(json, "schema_version", 1, 1, 1) != 1) throw bad("schema_version", "only version 1 is supported");
    }

    public static void keys(JsonObject json, String... allowed) {
        Set<String> fields = Set.of(allowed);
        for (String key : json.keySet()) if (!fields.contains(key)) throw bad(key, "unknown field");
    }

    private static JsonObject object(JsonObject json, String key, JsonObject fallback) {
        if (!json.has(key)) return fallback;
        if (!json.get(key).isJsonObject()) throw bad(key, "expected object");
        return json.getAsJsonObject(key);
    }

    private static JsonArray array(JsonElement json, String key, int maximum) {
        if (json == null || !json.isJsonArray()) throw bad(key, "expected array");
        JsonArray values = json.getAsJsonArray();
        if (values.size() > maximum) throw bad(key, "maximum " + maximum + " entries");
        return values;
    }

    private static String text(JsonObject json, String key) {
        if (!json.has(key) || !json.get(key).isJsonPrimitive() || !json.getAsJsonPrimitive(key).isString()) throw bad(key, "expected string");
        String value = json.get(key).getAsString();
        if (value.isBlank() || value.length() > 512) throw bad(key, "expected 1..512 characters");
        return value;
    }

    private static String optionalText(JsonObject json, String key, String fallback) { return json.has(key) ? text(json, key) : fallback; }
    private static String choice(JsonObject json, String key, String fallback, Set<String> allowed) {
        String value = optionalText(json, key, fallback);
        if (!allowed.contains(value)) throw bad(key, "expected one of " + new TreeSet<>(allowed));
        return value;
    }
    private static int integer(JsonObject json, String key, int fallback, int min, int max) {
        double number = number(json, key, fallback, min, max);
        if (number != Math.rint(number)) throw bad(key, "expected integer");
        return (int) number;
    }
    private static double number(JsonObject json, String key, double fallback, double min, double max) {
        if (!json.has(key)) return fallback;
        if (!json.get(key).isJsonPrimitive() || !json.getAsJsonPrimitive(key).isNumber()) throw bad(key, "expected number");
        double value = json.get(key).getAsDouble();
        if (!Double.isFinite(value) || value < min || value > max) throw bad(key, "must be " + min + ".." + max);
        return value;
    }
    private static IllegalArgumentException bad(String field, String message) { return new IllegalArgumentException(field + ": " + message); }
}
