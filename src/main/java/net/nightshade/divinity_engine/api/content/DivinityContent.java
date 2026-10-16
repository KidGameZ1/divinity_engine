package net.nightshade.divinity_engine.api.content;

import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.*;
import net.minecraft.resources.ResourceKey;
import net.nightshade.divinity_engine.api.gods.GodsDeferredRegister;
import net.nightshade.divinity_engine.api.trials.GodTrial;
import net.nightshade.divinity_engine.api.trials.GodTrialsDeferredRegister;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.registry.divinity.blessing.BlessingsRegistry;
import net.nightshade.divinity_engine.registry.divinity.gods.GodsRegistry;
import net.nightshade.divinity_engine.registry.divinity.trial.GodlyTrialsRegistry;
import net.nightshade.divinity_engine.util.content.*;
import net.nightshade.divinity_engine.util.divinity.effects.*;
import net.nightshade.divinity_engine.DivinityEngineMod;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.function.Consumer;

/** Startup-only content declarations shared by JSON packs, Java addons and optional KubeJS. */
public final class DivinityContent {
    private record Entry(String kind, ResourceLocation id, JsonObject json, Object definition) { }
    private static final Map<String, Entry> ENTRIES = new TreeMap<>();
    private static final Map<String, GodsDeferredRegister> GOD_REGISTERS = new HashMap<>();
    private static final Map<String, DeferredRegister<Blessings>> BLESSING_REGISTERS = new HashMap<>();
    private static final Set<String> DECLARED = new HashSet<>();
    private static IEventBus bus;
    private static boolean frozen;
    private static boolean initialized;
    private static Consumer<ContentBehaviorContext> scripts = context -> { };
    private static boolean dispatcherInstalled;
    private static String fingerprint;
    private static long nextScriptWarning;

    private DivinityContent() { }

    public static synchronized void registerGod(ResourceLocation id, JsonObject spec) { add("gods", id, spec); }
    public static synchronized void registerBlessing(ResourceLocation id, JsonObject spec) { add("blessings", id, spec); }
    public static synchronized void registerTrial(ResourceLocation id, JsonObject spec) { add("trials", id, spec); }

    private static void add(String kind, ResourceLocation id, JsonObject json) {
        if (frozen) throw new IllegalStateException("Divinity content is frozen; use startup scripts and restart both sides");
        Objects.requireNonNull(id, "Content ID");
        Objects.requireNonNull(json, "Content definition");
        ContentSchema.id(id.toString());
        if (Set.of("minecraft", "forge", "divinity_engine").contains(id.getNamespace()))
            throw new IllegalArgumentException("Use your own content namespace, not " + id.getNamespace());
        String key = kind + "/" + id;
        if (ENTRIES.containsKey(key)) throw new IllegalArgumentException("Duplicate content " + key);
        if (ENTRIES.size() >= 512 || json.toString().length() > 262144) throw new IllegalArgumentException("Content limit exceeded: " + key);
        try {
            Object parsed = switch (kind) {
                case "gods" -> ContentSchema.god(json);
                case "blessings" -> ContentSchema.blessing(json);
                case "trials" -> ContentSchema.trial(json);
                default -> throw new IllegalArgumentException("Unknown content folder " + kind);
            };
            Entry entry = new Entry(kind, id, json.deepCopy(), parsed);
            ENTRIES.put(key, entry);
            if (bus != null) declare(key, entry);
        } catch (RuntimeException error) {
            throw new IllegalArgumentException(key + ": " + error.getMessage(), error);
        }
    }

    /** Called during mod construction, never during a world/datapack reload. */
    public static synchronized void bootstrap(IEventBus modBus) {
        if (bus != null) throw new IllegalStateException("Content bootstrap already attached");
        bus = Objects.requireNonNull(modBus);
        loadDirectory(FMLPaths.CONFIGDIR.get().resolve("divinity_engine/content"));
        ENTRIES.forEach(DivinityContent::declare);
        bus.addListener(EventPriority.HIGHEST, (RegisterEvent event) -> freeze());
    }

    private static void declare(String key, Entry entry) {
        if (!DECLARED.add(key)) return;
        String namespace = entry.id.getNamespace();
        if (entry.definition instanceof ContentSchema.Blessing definition) {
            var register = BLESSING_REGISTERS.computeIfAbsent(namespace, ns -> {
                DeferredRegister<Blessings> created = DeferredRegister.create(
                        ResourceKey.<Blessings>createRegistryKey(new ResourceLocation("divinity_engine", "blessings")), ns);
                created.register(bus);
                return created;
            });
            register.register(entry.id.getPath(), () -> new ContentBlessing(entry.id, definition));
        } else if (entry.definition instanceof ContentSchema.God definition) {
            var register = GOD_REGISTERS.computeIfAbsent(namespace, ns -> {
                var created = GodsDeferredRegister.create(ns);
                created.register(bus);
                return created;
            });
            register.register(entry.id.getPath(), () -> new ContentGod(definition));
        }
    }

    public static void loadDirectory(Path root) {
        if (!Files.exists(root)) return;
        try (var paths = Files.walk(root, 16)) {
            List<Path> files = paths.filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
                    .filter(path -> path.toString().endsWith(".json")).sorted().limit(513).toList();
            if (files.size() > 512) throw new IllegalArgumentException("Maximum 512 content files under " + root);
            for (Path path : files) {
                Path relative = root.relativize(path);
                try {
                    if (relative.getNameCount() < 3) throw new IllegalArgumentException("Expected <namespace>/<gods|blessings|trials>/<id>.json");
                    String name = relative.subpath(2, relative.getNameCount()).toString().replace('\\', '/');
                    ResourceLocation id = ContentSchema.id(relative.getName(0) + ":" + name.substring(0, name.length() - 5));
                    if (Files.size(path) > 262144) throw new IllegalArgumentException("Maximum file size is 256 KiB");
                    JsonObject json = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
                    add(relative.getName(1).toString(), id, json);
                } catch (RuntimeException error) {
                    throw new IllegalArgumentException("Invalid content file " + relative + ": " + error.getMessage(), error);
                }
            }
        } catch (IOException error) { throw new IllegalStateException("Cannot read content directory " + root, error); }
    }

    private static synchronized void freeze() {
        if (frozen) return;
        frozen = true;
        JsonObject manifest = new JsonObject();
        ENTRIES.forEach((key, entry) -> manifest.add(key, entry.json));
        fingerprint = canonicalFingerprint(manifest);
    }

    /** Handshake also detects changed behavior fields when registry IDs still match. */
    public static synchronized String fingerprint() {
        if (!frozen) throw new IllegalStateException("Content fingerprint requested before registry freeze");
        return fingerprint;
    }

    public static JsonElement canonical(JsonElement value) {
        if (value.isJsonObject()) {
            JsonObject result = new JsonObject();
            new TreeSet<>(value.getAsJsonObject().keySet()).forEach(key -> result.add(key, canonical(value.getAsJsonObject().get(key))));
            return result;
        }
        if (value.isJsonArray()) {
            JsonArray result = new JsonArray();
            value.getAsJsonArray().forEach(element -> result.add(canonical(element)));
            return result;
        }
        return value.deepCopy();
    }

    public static String canonicalFingerprint(JsonElement manifest) {
        return "content-1-" + digest(canonical(manifest).toString());
    }

    private static String digest(String text) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }

    /** Enqueued common setup, after built-in trial registration and all Forge registry values exist. */
    public static synchronized void initialize() {
        if (initialized) return;
        validateReferences();
        for (Entry entry : ENTRIES.values()) {
            if (entry.definition instanceof ContentSchema.Trial definition) {
                var register = new GodTrialsDeferredRegister(entry.id.getNamespace());
                register.register(entry.id.getPath(), () -> new GodTrial(GodsRegistry.GODS_REGISTRY.get().getValue(definition.god()),
                        player -> new ContentTrial(player, definition)));
                GodlyTrialsRegistry.addDeferredRegister(register);
            } else if (entry.definition instanceof ContentSchema.God definition) {
                int color = 0xFF000000 | definition.color();
                BlessingVisualProfiles.registerTheme(entry.id, new BlessingVisualProfiles.Theme(entry.id.toString(), color, color,
                        0xFFFFFFFF, new ResourceLocation("minecraft", "textures/particle/spark_0.png")));
            } else if (entry.definition instanceof ContentSchema.Blessing definition && definition.texture() != null) {
                BlessingVisualDesigns.register(entry.id, BlessingVisualDesign.builder().duration(20)
                        .particles(6, .1, (particles, context) -> particles.texture(definition.texture())
                                .motion(new net.nightshade.renderlib.particle.ParticleMotion(
                                        new net.minecraft.world.phys.Vec3(0, .035, 0), new net.minecraft.world.phys.Vec3(.035, .035, .035),
                                        net.minecraft.world.phys.Vec3.ZERO, 0, .95, 0, 0))).build());
            }
        }
        initialized = true;
        DivinityEngineMod.LOGGER.info("Loaded {} startup content definitions ({})", ENTRIES.size(), fingerprint());
    }

    private static void validateReferences() {
        Set<ResourceLocation> trialGods = new HashSet<>();
        for (Entry entry : ENTRIES.values()) {
            validateEntry(entry, trialGods);
        }
    }

    /** Read-only validation for tools and tests, after Forge registries are populated. */
    public static void validateReferences(String kind, ResourceLocation id, JsonObject spec) {
        Object definition = switch (kind) {
            case "gods" -> ContentSchema.god(spec);
            case "blessings" -> ContentSchema.blessing(spec);
            case "trials" -> ContentSchema.trial(spec);
            default -> throw new IllegalArgumentException("Unknown content kind " + kind);
        };
        validateEntry(new Entry(kind, id, spec, definition), new HashSet<>());
    }

    private static void validateEntry(Entry entry, Set<ResourceLocation> trialGods) {
        try {
            if (entry.definition instanceof ContentSchema.God definition) {
                for (var id : definition.blessings()) if (!BlessingsRegistry.BLESSINGS_REGISTRY.get().containsKey(id))
                    throw new IllegalArgumentException("Unknown blessing " + id);
                for (var id : definition.offerings().keySet()) if (!ForgeRegistries.ITEMS.containsKey(id))
                    throw new IllegalArgumentException("Unknown offering item " + id);
            } else if (entry.definition instanceof ContentSchema.Blessing definition) {
                for (var actions : definition.hooks().values()) for (var action : actions)
                    if (action.effect() != null && !ForgeRegistries.MOB_EFFECTS.containsKey(action.effect()))
                        throw new IllegalArgumentException("Unknown effect " + action.effect());
            } else if (entry.definition instanceof ContentSchema.Trial definition) {
                var god = GodsRegistry.GODS_REGISTRY.get().getValue(definition.god());
                if (god == null) throw new IllegalArgumentException("Unknown god " + definition.god());
                if (GodlyTrialsRegistry.get(entry.id) != null || GodlyTrialsRegistry.get(god) != null || !trialGods.add(definition.god()))
                    throw new IllegalArgumentException("A trial already exists for " + definition.god() + "; definitions cannot replace released trials");
                for (var phase : definition.phases()) for (var spawn : phase.spawns()) {
                    var type = ForgeRegistries.ENTITY_TYPES.getValue(spawn.entity());
                    if (type == null || !type.canSummon() || type.getCategory() != net.minecraft.world.entity.MobCategory.MONSTER
                            || Set.of("minecraft:wither", "minecraft:ender_dragon", "minecraft:warden").contains(spawn.entity().toString()))
                        throw new IllegalArgumentException("Trial spawn must be a summonable non-boss monster: " + spawn.entity());
                }
            }
        } catch (RuntimeException error) { throw new IllegalArgumentException(entry.kind + "/" + entry.id + ": " + error.getMessage(), error); }
    }

    public static synchronized void setScriptDispatcher(Consumer<ContentBehaviorContext> dispatcher) {
        if (dispatcherInstalled) throw new IllegalStateException("Script dispatcher already installed");
        scripts = Objects.requireNonNull(dispatcher);
        dispatcherInstalled = true;
    }

    public static void dispatch(ContentBehaviorContext context) {
        try { scripts.accept(context); }
        catch (RuntimeException error) {
            long now = context.getLevel().getGameTime();
            if (now >= nextScriptWarning) {
                nextScriptWarning = now + 200;
                DivinityEngineMod.LOGGER.error("Content callback failed for {}/{}", context.getId(), context.getHook(), error);
            }
        }
    }
}
