package net.nightshade.divinity_engine.client.effects;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.BackupConfirmScreen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.nightshade.divinity_engine.network.messages.effects.PrivateBlessingEffectsPacket;
import net.nightshade.divinity_engine.gametest.BlessingParticleExpectations;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisualProfiles;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisualDesigns;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals.Cue;
import net.nightshade.renderlib.api.RenderAnchor;
import net.nightshade.renderlib.api.RenderEffectInstance;
import net.nightshade.renderlib.api.RenderEffectSpec;
import net.nightshade.renderlib.api.RenderEffects;
import net.nightshade.renderlib.api.RenderHandle;
import net.nightshade.renderlib.client.RenderLibClientState;
import net.nightshade.renderlib.client.RenderLibShaders;
import net.nightshade.renderlib.client.world.CustomWorldRendererRegistry;
import net.nightshade.renderlib.config.RenderLibConfig;
import net.nightshade.renderlib.network.RenderLibClientBridge;
import net.nightshade.renderlib.network.SyncRenderEffectsPayload;
import net.nightshade.renderlib.particle.ParticleBloomMode;
import net.nightshade.renderlib.particle.ParticleEmitterSpec;
import net.nightshade.renderlib.particle.RenderParticles;
import net.nightshade.renderlib.particle.client.RenderParticleEngine;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/** Opt-in effects-only QA of a prepared packaged client. Never invokes blessing gameplay callbacks. */
@Mod.EventBusSubscriber(modid = "divinity_engine", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class BlessingEffectsSmokeTest {
    private static final boolean ENABLED = Boolean.getBoolean("divinity_engine.effectsSmokeTest");
    private static final boolean AUTO_OPEN = Boolean.getBoolean("divinity_engine.effectsSmokeAutoOpen");
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation PRIMITIVE = new ResourceLocation("divinity_engine", "blessing_manifestation");
    private static final ResourceLocation PIXEL_MATERIAL = new ResourceLocation("divinity_engine", "particle_alpha");
    private static final ResourceLocation PIXEL_SHADER = new ResourceLocation("divinity_engine", "blessing_pixel");
    private static final ResourceLocation FALLBACK_SHADER = new ResourceLocation("minecraft", "position_tex_color");
    private static final List<String> GODS = List.of(("solarius nashara grond varun dravak elaris voltira vokar "
            + "nythea kairoth cryonis nerai zephra terra lumen mechanos ignar aethon").split(" "));
    private static final List<String> PASSES = List.of("gallery", "shader-fallback", "reduced-motion");

    private static final Vec3 FORWARD = Vec3.directionFromRotation(-12, 0);
    private static final Vec3 RIGHT = new Vec3(-1, 0, 0);
    private static final long BOOT_LIMIT = TimeUnit.MINUTES.toNanos(3);
    private static final long RUN_LIMIT = TimeUnit.MINUTES.toNanos(8);
    private static BlessingEffectsSmokeTest active;
    private static Startup startup;
    private static boolean terminal;
    private static long bootStarted;
    private static int readyTicks;

    private record Scope(Path game, Path world, IntegratedServer server) {}
    private record Fixture(ResourceLocation god, BlessingVisualProfiles.Theme theme, List<ResourceLocation> ids) {}
    private record Scene(String pass, Fixture fixture) {}
    private record Options(boolean pause, boolean hideGui, CameraType camera, ParticleStatus particles,
                           boolean shaders, boolean flashing, double motion, double screenEffects, boolean bob) {
        static Options capture(Minecraft mc) {
            return new Options(mc.options.pauseOnLostFocus, mc.options.hideGui, mc.options.getCameraType(),
                    mc.options.particles().get(), RenderLibConfig.CLIENT.shaders.get(),
                    RenderLibConfig.CLIENT.reducedFlashing.get(), RenderLibConfig.CLIENT.animationStrength.get(),
                    mc.options.screenEffectScale().get(), mc.options.bobView().get());
        }
        void restore(Minecraft mc) {
            mc.options.pauseOnLostFocus = pause;
            mc.options.hideGui = hideGui;
            mc.options.setCameraType(camera);
            mc.options.particles().set(particles);
            mc.options.screenEffectScale().set(screenEffects);
            mc.options.bobView().set(bob);
            RenderLibConfig.CLIENT.shaders.set(shaders);
            RenderLibConfig.CLIENT.reducedFlashing.set(flashing);
            RenderLibConfig.CLIENT.animationStrength.set(motion);
        }
    }

    private final Minecraft mc;
    private final Scope scope;
    private final LocalPlayer player;
    private final ClientLevel level;
    private final AbstractContainerMenu menu;
    private final Options options;
    private final Deque<Scene> scenes = new ArrayDeque<>();
    private final Map<RenderHandle, RenderEffectSpec> owned = new LinkedHashMap<>();
    private final Map<ResourceLocation, JsonObject> verifiedSprites = new LinkedHashMap<>();
    private final JsonObject report = new JsonObject();
    private final JsonArray results = new JsonArray();
    private final JsonObject privateCheck = new JsonObject();
    private final long started = System.nanoTime();
    private PrivateBlessingEffectsPacket privateFixture;
    private long privateStarted;
    private Path artifacts;
    private Scene scene;
    private JsonObject result;
    private Vec3 eye;
    private long sceneStarted, spawnedAt, expiresAt;
    private int frames;
    private boolean capturePending, captureInFlight, captured, finished;

    private BlessingEffectsSmokeTest(Minecraft mc, Scope scope) {
        this.mc = mc;
        this.scope = scope;
        player = mc.player;
        level = mc.level;
        menu = player.containerMenu;
        options = Options.capture(mc);
    }

    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (!ENABLED || terminal || event.phase != TickEvent.Phase.END) return;
        try {
            Minecraft mc = Minecraft.getInstance();
            if (active != null) { active.tick(); return; }
            if (bootStarted == 0) bootStarted = System.nanoTime();
            require(System.nanoTime() - bootStarted < BOOT_LIMIT, "Timed out awaiting copied Core-Smoke");
            require(mc.level == null || mc.hasSingleplayerServer(), "Multiplayer smoke is forbidden");
            if (startup == null) startup = new Startup(mc);
            startup.tick();
            if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null) return;
            Scope scope = guardedScope(mc);
            if (mc.screen != null || mc.getOverlay() != null || !mc.player.isAlive()
                    || mc.player.containerMenu != mc.player.inventoryMenu
                    || !mc.player.containerMenu.getCarried().isEmpty()
                    || !mc.level.hasChunkAt(mc.player.blockPosition())) { readyTicks = 0; return; }
            if (++readyTicks < 60) return;
            active = new BlessingEffectsSmokeTest(mc, scope);
            active.start();
        } catch (Throwable failure) { fail(failure); }
    }

    @SubscribeEvent
    public static void renderTick(TickEvent.RenderTickEvent event) {
        if (!ENABLED || terminal || event.phase != TickEvent.Phase.END) return;
        try {
            if (active != null) active.frame();
            else if (bootStarted != 0) require(System.nanoTime() - bootStarted < BOOT_LIMIT, "Startup timed out");
        } catch (Throwable failure) { fail(failure); }
    }

    // Frame the gallery without rotating, teleporting, spawning, or ticking any gameplay entity.
    private static boolean framing() {
        return ENABLED && !terminal && active != null && active.mc.level == active.level
                && active.mc.player == active.player && active.mc.screen == null;
    }

    @SubscribeEvent
    public static void camera(ViewportEvent.ComputeCameraAngles event) {
        if (!framing()) return;
        event.setYaw(0); event.setPitch(-12); event.setRoll(0);
    }

    @SubscribeEvent
    public static void fov(ViewportEvent.ComputeFov event) {
        if (framing() && event.usedConfiguredFov()) event.setFOV(70);
    }

    @SubscribeEvent
    public static void legend(RenderGuiEvent.Post event) {
        if (!framing() || active.scene == null) return;
        String title = active.scene.pass() + " / " + active.scene.fixture().god()
                + " / five distinct blessings, left to right";
        event.getGuiGraphics().drawString(active.mc.font, title, 8, 8, 0xFFFFFF, true);
    }

    private static Scope guardedScope(Minecraft mc) throws IOException {
        IntegratedServer server = mc.getSingleplayerServer();
        require(mc.isSingleplayer() && server != null, "Integrated server required");
        Path game = mc.gameDirectory.toPath().toRealPath();
        require(startup != null && game.equals(startup.game), "Approved effects startup manifest required");
        Path expected = game.resolve("saves/Core-Smoke");
        Path world = server.getWorldPath(LevelResource.ROOT).toRealPath();
        require(!Files.isSymbolicLink(game.resolve("saves")) && !Files.isSymbolicLink(expected)
                && world.equals(expected.toRealPath()) && world.startsWith(game)
                && world.equals(mc.getLevelSource().getBaseDir().resolve("Core-Smoke").toRealPath()), "Wrong or linked save");
        require(world.equals(startup.world), "Opened a different world from the approved copy");
        return new Scope(game, world, server);
    }

    /** Only the copied-world opener owns prompts; never accepts a general confirmation dialog. */
    private static final class Startup {
        private final Minecraft mc;
        private final Path game, world;
        private final JsonObject report = new JsonObject();
        private final JsonArray screens = new JsonArray();
        private final Set<String> accepted = new HashSet<>();
        private Screen observed, ownedWarning;
        private JsonObject row;
        private boolean opened, onboarding;

        private Startup(Minecraft mc) throws IOException {
            this.mc = mc;
            game = mc.gameDirectory.toPath().toRealPath();
            Path temp = Path.of(System.getProperty("java.io.tmpdir")).toRealPath();
            require(game.getFileName().toString().startsWith("nightshade-production-smoke-effects-")
                    && (game.getParent().equals(temp) || game.getParent().equals(Path.of("/tmp").toRealPath())),
                    "Effects preview requires a fresh OS-temp directory");
            Path manifestPath = game.resolve("effects-preview-manifest.json");
            require(!Files.isSymbolicLink(manifestPath), "Linked effects preview manifest");
            var manifest = JsonParser.parseString(Files.readString(manifestPath)).getAsJsonObject();
            require(manifest.get("kind").getAsString().equals("effects-preview-v1")
                    && manifest.get("worldName").getAsString().equals("Core-Smoke")
                    && Path.of(manifest.get("runDirectory").getAsString()).toRealPath().equals(game)
                    && manifest.get("plannedScenarios").getAsInt() == 54, "Invalid effects preview manifest");
            Path expected = game.resolve("saves/Core-Smoke");
            require(!Files.isSymbolicLink(game.resolve("saves")) && !Files.isSymbolicLink(expected), "Linked fixture save");
            world = expected.toRealPath();
            require(world.equals(expected) && !world.equals(Path.of(manifest.get("worldSource").getAsString()).toRealPath()),
                    "Effects fixture is not a disposable copy");
            try (var paths = Files.walk(world)) { require(paths.noneMatch(Files::isSymbolicLink), "Linked fixture descendant"); }
            require(Files.isRegularFile(world.resolve("level.dat")), "Missing fixture level.dat");
            require(System.getProperties().stringPropertyNames().stream().noneMatch(key -> !key.startsWith("divinity_engine.effectsSmoke")
                    && (key.startsWith("divinity_engine.") || key.startsWith("nightshade_core."))
                    && (key.contains("Smoke") || key.contains("Preview")) && Boolean.getBoolean(key)), "Another QA harness is enabled");
            require(Runtime.version().feature() == 17 && Runtime.getRuntime().maxMemory() <= 2L * 1024 * 1024 * 1024,
                    "Effects preview requires Java 17 and at most 2G heap");
            require(!Files.exists(game.resolve("divinity-effects-smoke")) && !Files.exists(game.resolve("effects-preview-startup.json")),
                    "Effects preview directory was already used");
            report.addProperty("status", "OPENING"); report.add("manifest", manifest); report.add("openingScreens", screens); write();
        }

        private void tick() throws IOException {
            if (row == null || observed != mc.screen) {
                require(screens.size() < 30, "Unexpected startup screen loop");
                observed = mc.screen; row = new JsonObject();
                row.addProperty("class", observed == null ? "none" : observed.getClass().getName());
                row.addProperty("titleKey", observed == null ? "" : translationKey(observed.getTitle()));
                screens.add(row); write();
            }
            if (mc.getSingleplayerServer() != null) {
                require(mc.getSingleplayerServer().getWorldPath(LevelResource.ROOT).toRealPath().equals(world), "Opened a non-fixture world");
                if (mc.player != null && mc.level != null && mc.screen == null && mc.getOverlay() == null) {
                    report.addProperty("status", "WORLD_READY"); write();
                }
                return;
            }
            if (!AUTO_OPEN || mc.level != null || mc.getOverlay() != null) return;
            Screen screen = mc.screen;
            String action = null;
            if (!opened && !onboarding && screen != null && screen.getClass() == AccessibilityOnboardingScreen.class) action = "gui.continue";
            else if (opened && screen == ownedWarning && experimental(screen)) {
                require(!accepted.contains(screen.getClass().getName()), "Repeated experimental warning");
                require(screen.children().stream().noneMatch(child -> child instanceof Checkbox), "Refusing upgrade/cache dialog");
                action = screen.getClass() == BackupConfirmScreen.class ? "selectWorld.backupJoinSkipButton" : "gui.proceed";
            }
            if (action != null) {
                recheck(); String expected = action;
                var buttons = screen.children().stream().filter(child -> child instanceof AbstractButton).map(child -> (AbstractButton) child)
                        .filter(button -> button.active && button.visible && translationKey(button.getMessage()).equals(expected)).toList();
                require(buttons.size() == 1, "No unique safe startup action: " + action);
                if (screen.getClass() == AccessibilityOnboardingScreen.class) onboarding = true; else accepted.add(screen.getClass().getName());
                row.addProperty("automaticAction", action); write(); buttons.get(0).onPress();
                ownedWarning = opened && experimental(mc.screen) ? mc.screen : null;
            } else if (!opened && screen instanceof TitleScreen) {
                recheck(); opened = true; report.addProperty("worldOpenRequested", true); write();
                mc.createWorldOpenFlows().loadLevel(screen, "Core-Smoke");
                ownedWarning = experimental(mc.screen) ? mc.screen : null;
            }
        }

        private void recheck() throws IOException {
            require(mc.level == null && mc.getSingleplayerServer() == null && mc.gameDirectory.toPath().toRealPath().equals(game)
                    && !Files.isSymbolicLink(game.resolve("saves")) && !Files.isSymbolicLink(game.resolve("saves/Core-Smoke"))
                    && mc.getLevelSource().getBaseDir().resolve("Core-Smoke").toRealPath().equals(world), "World opener escaped fixture");
        }

        private void write() throws IOException {
            Files.writeString(game.resolve("effects-preview-startup.json"), new GsonBuilder().setPrettyPrinting().create().toJson(report));
        }

        private void refused(String detail) {
            try {
                if (!mc.gameDirectory.toPath().toRealPath().equals(game)) return;
                report.addProperty("status", "REFUSED"); report.addProperty("failure", detail); write();
                if (Boolean.getBoolean("divinity_engine.effectsSmokeAutoExit") && (mc.getSingleplayerServer() == null && mc.level == null
                        || mc.getSingleplayerServer() != null && mc.getSingleplayerServer().getWorldPath(LevelResource.ROOT).toRealPath().equals(world))) mc.stop();
            } catch (IOException error) { LOGGER.error("Cannot record effects startup refusal", error); }
        }

        private static boolean experimental(Screen screen) {
            return screen != null && (screen.getClass() == ConfirmScreen.class || screen.getClass() == BackupConfirmScreen.class)
                    && translationKey(screen.getTitle()).equals("selectWorld.backupQuestion.experimental");
        }
    }

    private static String translationKey(Component text) {
        return text.getContents() instanceof TranslatableContents translated ? translated.getKey() : "";
    }

    private void start() throws IOException {
        Path root = scope.game().resolve("divinity-effects-smoke");
        require(!Files.isSymbolicLink(root), "Linked artifact directory");
        Files.createDirectories(root);
        require(root.toRealPath().getParent().equals(scope.game()), "Artifact directory escaped isolated run");
        artifacts = Files.createTempDirectory(root, "run-");
        report.addProperty("schemaVersion", 2);
        report.addProperty("testKind", "synthetic_local_blessing_effects_only");
        report.addProperty("status", "RUNNING");
        report.addProperty("startedAt", Instant.now().toString());
        report.addProperty("worldDirectory", scope.world().toString());
        report.addProperty("artifacts", artifacts.toString());
        report.addProperty("serverOrWorldMutationsIssued", false);
        report.addProperty("gameplayCallbacksInvoked", false);
        report.addProperty("plannedScenarios", 54);
        report.add("launchManifest", startup.report.get("manifest"));
        report.add("scenarios", results);
        JsonArray limitations = new JsonArray();
        limitations.add("Synthetic local recipes only, not a blessing gameplay/network integration test.");
        limitations.add("Minecraft sprite resources, palettes, zero combat mesh and live particle draw counts are asserted; PNGs still need visual review.");
        limitations.add("Sprite alpha and draw counts do not measure final framebuffer pixel visibility or reduced-motion trajectories.");
        limitations.add("The persistent pocket portal composer is retained, but no portal or gameplay is created by this harness.");
        limitations.add("Normal world ticking/autosaves continue in the disposable copy; no world-file immutability claim.");
        report.add("limitations", limitations);
        writeReport();
        require(CustomWorldRendererRegistry.get(PRIMITIVE).isPresent(), "Production custom renderer is not registered");
        require(RenderLibClientState.current().isEmpty() && RenderParticleEngine.metrics().alive() == 0
                && RenderParticleEngine.metrics().emitters() == 0, "Core state is not empty; refusing to clear unrelated effects");
        List<Fixture> fixtures = fixtures();
        report.add("diagnostics", diagnose(fixtures));
        for (String pass : PASSES) for (Fixture fixture : fixtures) scenes.add(new Scene(pass, fixture));
        mc.options.pauseOnLostFocus = false;
        mc.options.hideGui = false;
        mc.options.setCameraType(CameraType.FIRST_PERSON);
        mc.options.particles().set(ParticleStatus.ALL);
        mc.options.bobView().set(false);
        beginPrivateCheck(fixtures.stream().filter(fixture -> fixture.god().getPath().equals("aethon"))
                .findFirst().orElseThrow());
        writeReport();
    }

    private void beginPrivateCheck(Fixture fixture) {
        require(RenderLibClientState.current().isEmpty(), "Private preflight requires empty Core state");
        privateCheck.addProperty("status", "RUNNING");
        privateCheck.addProperty("networkTransportExercised", false);
        report.add("privateSnapshot", privateCheck);
        var recipe = BlessingVisuals.recipe(fixture.god(), fixture.ids().get(0), Cue.MARK,
                RenderAnchor.world(player.getEyePosition().add(FORWARD.scale(9))), FORWARD, null, 1.1);
        require(!recipe.renderMesh() && recipe.emitParticles(), "Private MARK is not particle-only");
        long now = level.getGameTime(), end = now + recipe.particles().durationTicks();
        privateFixture = PrivateBlessingEffectsPacket.spawn(level.dimension().location(),
                new RenderEffectInstance(UUID.randomUUID(), 0, recipe.particles().toEffectSpec(), now, end));
        privateStarted = System.nanoTime();
        // Exercise the real client bridges, without sending packets or installing a substitute handler.
        PrivateBlessingEffectsClient.apply(privateFixture);
        var before = RenderLibClientState.current();
        require(before.size() == 1 && privateFixture.effects().stream().allMatch(source ->
                        before.stream().anyMatch(local -> local.spec().equals(source.spec())
                                && local.startGameTime() == now && local.endGameTime() == end)),
                "Private MARK did not create one bounded local particle handle");
        RenderLibClientBridge.apply(new SyncRenderEffectsPayload(level.dimension().location(), true, List.of(), List.of()));
        require(RenderLibClientState.current().equals(before), "Public replacement wiped or changed private MARK handles");
        privateCheck.addProperty("handlesAfterPublicReplacement", before.size());
        privateCheck.addProperty("publicReplacementPreservedEmitter", true);
        privateCheck.addProperty("particleOnly", true);
        PrivateBlessingEffectsClient.apply(PrivateBlessingEffectsPacket.remove(privateFixture.dimension(),
                privateFixture.effects().get(0).id()));
        require(RenderLibClientState.current().isEmpty(), "Private removal IDs left Core handles behind");
        privateCheck.addProperty("individualRemovalVerified", true);
        PrivateBlessingEffectsClient.apply(privateFixture);
        require(RenderLibClientState.current().size() == 1, "Removed private ID could not be reused for expiry check");
    }

    private void awaitPrivateExpiry() throws IOException {
        require(System.nanoTime() - privateStarted < TimeUnit.SECONDS.toNanos(30), "Private preflight timed out");
        if (level.getGameTime() < privateFixture.effects().get(0).endGameTime() + 2) return;
        require(RenderLibClientState.current().isEmpty(), "Private MARK emitter failed natural expiry");
        var metrics = RenderParticleEngine.metrics();
        require(metrics.emitters() == 0 && metrics.alive() == 0, "Private particles survived expiry: " + metrics);
        PrivateBlessingEffectsClient.apply(privateFixture);
        require(RenderLibClientState.current().isEmpty(), "Expired private replay resurrected local handles");
        privateCheck.addProperty("naturalExpiryVerified", true);
        privateCheck.addProperty("expiredReplayIgnored", true);
        privateCheck.addProperty("status", "PASS");
        privateFixture = null;
        writeReport();
    }

    private static List<Fixture> fixtures() {
        var styles = BlessingVisualProfiles.styles();
        var themes = BlessingVisualProfiles.themes();
        List<Fixture> fixtures = new ArrayList<>();
        for (String name : GODS) {
            ResourceLocation god = new ResourceLocation("divinity_engine", name);
            require(themes.containsKey(god), "Missing god theme " + god);
            ResourceLocation[] ids = new ResourceLocation[5];
            styles.forEach((id, style) -> {
                if (!style.god().equals(god) || !id.getNamespace().equals("divinity_engine")) return;
                require(ids[style.variant()] == null, "Duplicate builtin variant for " + god);
                ids[style.variant()] = id;
            });
            require(Arrays.stream(ids).allMatch(id -> id != null), "Missing one of five registered recipes for " + god);
            fixtures.add(new Fixture(god, themes.get(god), List.of(ids)));
        }
        require(fixtures.size() == 18 && Cue.values().length == 10, "Update gallery coverage for changed pantheon/cues");
        return fixtures;
    }

    private JsonObject diagnose(List<Fixture> fixtures) throws IOException {
        JsonObject summary = new JsonObject();
        JsonArray palettes = new JsonArray();
        for (Fixture fixture : fixtures) {
            JsonObject row = new JsonObject();
            row.addProperty("god", fixture.god().toString());
            JsonArray colors = new JsonArray();
            for (int color : palette(fixture)) {
                require((color >>> 24) != 0, "Invisible god palette: " + fixture.god());
                colors.add(String.format(java.util.Locale.ROOT, "%08X", color));
            }
            row.add("argbPalette", colors); palettes.add(row);
        }
        int cases = 0;
        for (Fixture fixture : fixtures) for (Cue cue : Cue.values()) for (int detail = 0; detail <= 2; detail++) {
            Vec3 link = cue == Cue.TELEPORT || cue == Cue.RECALL || cue == Cue.TRAIL ? RIGHT.scale(2) : null;
            for (int variant = 0; variant < 5; variant++) {
                String label = fixture.god() + "/" + cue + "/" + variant + "/detail-" + detail;
                var design = BlessingVisualDesigns.get(fixture.ids().get(variant));
                require(!design.mesh() && design.particles(), "Built-in design is not particle-only: " + label);
                var recipe = BlessingVisuals.recipe(fixture.god(), fixture.ids().get(variant), cue,
                        RenderAnchor.world(Vec3.ZERO), FORWARD, link, 1.1);
                checkParticles(recipe, fixture, label);
                checkMesh(probe(fixture, cue, variant, 10, 1, detail, link), label, false);
                for (double age : new double[]{0, 40, 41})
                    checkMesh(probe(fixture, cue, variant, age, 1, detail, link), label + " age=" + age, false);
                var stillA = probe(fixture, cue, variant, 8, 0, detail, link);
                var stillB = probe(fixture, cue, variant, 16, 0, detail, link);
                checkMesh(stillA, label, false); checkMesh(stillB, label, false);
                require(stillA.geometryFingerprint() == stillB.geometryFingerprint()
                        && stillA.generatedVertices() == stillB.generatedVertices(), "Motion-zero geometry changed: " + label);
                cases++;
            }
        }
        ResourceLocation portal = new ResourceLocation("divinity_engine", "pocket_dimension");
        require(BlessingEffectRenderer.registeredBlessings().equals(Set.of(portal)), "Unexpected combat mesh composer registration");
        var persistent = BlessingEffectRenderer.diagnose(portal, "MARK", 1.1, 10, Long.MAX_VALUE, 1, 2, FORWARD, null);
        checkMesh(persistent, "persistent pocket portal (not combat)", true);
        summary.addProperty("persistentPortalComposerVerified", true);
        summary.addProperty("persistentPortalVertices", persistent.generatedVertices());
        require(Math.abs(BlessingEffectRenderer.segmentDistanceSquared(new Vec3(0, 10, 0), Vec3.ZERO,
                new Vec3(10, 10, 0)) - 50) < 1.0E-8, "Diagonal segment incorrectly uses its enclosing box");
        require(Math.abs(BlessingEffectRenderer.segmentDistanceSquared(new Vec3(5, 5, 0.1), Vec3.ZERO,
                new Vec3(10, 10, 0)) - 0.01) < 1.0E-8, "Near-segment distance is incorrect");
        require(Math.abs(BlessingEffectRenderer.triangleDistanceSquared(new Vec3(0.25, 0.25, 2), Vec3.ZERO,
                new Vec3(1, 0, 0), new Vec3(0, 1, 0)) - 4) < 1.0E-8, "Triangle-face distance is incorrect");
        require(Math.abs(BlessingEffectRenderer.triangleDistanceSquared(new Vec3(2, 2, 0), Vec3.ZERO,
                new Vec3(1, 0, 0), new Vec3(0, 1, 0)) - 4.5) < 1.0E-8, "Triangle-edge distance is incorrect");
        require(Math.abs(BlessingEffectRenderer.triangleDistanceSquared(new Vec3(0, 2, 0), Vec3.ZERO,
                new Vec3(1, 0, 0), new Vec3(2, 0, 0)) - 4) < 1.0E-8, "Degenerate triangle distance is incorrect");
        summary.addProperty("proximityGeometryVerified", true);
        summary.addProperty("combinations", 18 * 5 * 10);
        summary.addProperty("qualityCases", cases);
        summary.addProperty("expiryBoundaryChecks", cases * 3);
        summary.addProperty("motionZeroComparisons", cases);
        summary.addProperty("particleOnlyRecipes", fixtures.size() * 5);
        summary.addProperty("combatMeshVertices", 0);
        summary.addProperty("allGodPalettesVerified", true);
        summary.addProperty("minecraftSpritesVerified", true);
        summary.add("palettes", palettes);
        JsonArray sprites = new JsonArray(); verifiedSprites.values().forEach(sprites::add);
        summary.add("sprites", sprites);
        summary.addProperty("status", "PASS");
        return summary;
    }

    private static BlessingEffectRenderer.Diagnostics probe(Fixture fixture, Cue cue, int variant,
                                                           double age, double motion, int detail, Vec3 link) {
        return BlessingEffectRenderer.diagnose(fixture.ids().get(variant), cue.name(), 1.1,
                age, 40, motion, detail, FORWARD, link);
    }

    private static void checkMesh(BlessingEffectRenderer.Diagnostics mesh, String label, boolean expected) {
        if (!expected) {
            require(mesh.generatedVertices() == 0 && mesh.quadVertices() == 0 && mesh.lineEndpoints() == 0
                    && mesh.maximumAnchorDistance() == 0 && mesh.budgetDrops() == 0 && mesh.rejectedPrimitives() == 0,
                    "Particle-only blessing acquired a fallback mesh: " + label);
            return;
        }
        require(mesh.generatedVertices() > 0 && mesh.generatedVertices() <= 1920
                && mesh.generatedVertices() == mesh.quadVertices() + 2 * mesh.lineEndpoints()
                && mesh.quadVertices() % 4 == 0 && mesh.lineEndpoints() % 2 == 0
                && mesh.rejectedPrimitives() == 0 && mesh.budgetDrops() >= 0
                && Double.isFinite(mesh.maximumAnchorDistance()) && mesh.maximumAnchorDistance() > 0
                && mesh.maximumAnchorDistance() <= 8, "Invalid/beyond-budget production mesh: " + label + " " + mesh);
    }

    private static List<Integer> palette(Fixture fixture) {
        return List.of(fixture.theme().primary(), fixture.theme().accent(), fixture.theme().highlight());
    }

    private void checkParticles(BlessingVisuals.VisualRecipe recipe, Fixture fixture, String label) throws IOException {
        require(!recipe.renderMesh() && recipe.emitParticles() && recipe.handleCount() == 1,
                "Combat recipe must have exactly one particle channel: " + label);
        var particles = recipe.particles();
        var appearance = particles.appearance();
        ResourceLocation id = new ResourceLocation(recipe.visual().parameters().getString("BlessingId"));
        ResourceLocation texture = appearance.texture().orElseThrow();
        verifySprite(texture);
        List<Integer> expectedPalette = BlessingParticleExpectations.palette(id,
                Cue.valueOf(recipe.visual().parameters().getString("Cue")), palette(fixture));
        require(recipe.visual().palette().colors().equals(palette(fixture))
                && particles.palette().colors().equals(expectedPalette) && particles.palette().cycleTicks() == 40
                && particles.palette().colors().stream().allMatch(color -> (color >>> 24) != 0),
                "Lost god palette or explicit native/ash palette: " + label);
        require(appearance.renderer().equals(new ResourceLocation("nightshade_core", "textured_billboard"))
                && appearance.material().equals(PIXEL_MATERIAL) && appearance.glowStrength() == 0
                && appearance.bloom() == ParticleBloomMode.OFF, "Not a non-glowing Minecraft sprite renderer: " + label);
        require(!particles.collision().authoritative() && particles.durationTicks() >= 8 && particles.durationTicks() <= 32
                && particles.emission().maximumAlive() >= 1 && particles.emission().maximumAlive() <= 16
                && particles.emission().maximumLifetime() < particles.durationTicks()
                && Double.isFinite(particles.estimatedBoundsRadius()) && particles.estimatedBoundsRadius() <= 64,
                "Unbounded or gameplay-authoritative particle recipe: " + label);
        require(Math.abs(appearance.size().sample(0) - BlessingVisualDesigns.get(id).particleSize()) < 1.0E-8,
                "Sprite start size differs from authored design: " + label);
        for (double age : new double[]{0, 0.25, 0.5, 0.75, 1}) {
            double size = appearance.size().sample(age), alpha = appearance.alpha().sample(age);
            require(Double.isFinite(size) && size >= 0.015 && size <= 0.3
                    && Double.isFinite(alpha) && alpha >= 0 && alpha <= 0.92,
                    "Unbounded sprite size/alpha: " + label);
        }
        require(appearance.alpha().sample(0.25) > 0 && appearance.alpha().sample(1) == 0,
                "Invisible sprite or missing terminal fade: " + label);
    }

    private void verifySprite(ResourceLocation texture) throws IOException {
        if (verifiedSprites.containsKey(texture)) return;
        require(texture.getNamespace().equals("minecraft") && texture.getPath().startsWith("textures/particle/")
                && texture.getPath().endsWith(".png"), "Expected Minecraft particle sprite: " + texture);
        int visible = 0;
        JsonObject row = new JsonObject(); row.addProperty("sprite", texture.toString());
        try (var stream = mc.getResourceManager().getResourceOrThrow(texture).open(); var image = NativeImage.read(stream)) {
            for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++)
                if ((image.getPixelRGBA(x, y) >>> 24) != 0) visible++;
            row.addProperty("width", image.getWidth()); row.addProperty("height", image.getHeight());
        }
        require(visible > 0, "Minecraft sprite has no visible texels: " + texture);
        row.addProperty("visibleTexels", visible); verifiedSprites.put(texture, row);
    }

    private void tick() throws IOException {
        context();
        if (privateFixture != null) { awaitPrivateExpiry(); return; }
        if (scene == null) {
            scene = scenes.pollFirst();
            if (scene == null) { finish(true, "Gallery, diagnostics, PNG writes and natural expiry passed"); return; }
            beginScene();
            return;
        }
        if (mc.getOverlay() != null || mc.isPaused() || capturePending || captureInFlight) return;
        if (!captured) {
            // Capture after Core's first particle tick; an eight-tick burst can expire
            // while a cold client is waiting for several additional rendered frames.
            if (level.getGameTime() - spawnedAt >= 1 && frames >= 1) capturePending = true;
        } else if (level.getGameTime() >= expiresAt + 2) {
            require(RenderLibClientState.current().isEmpty(), "Core effects did not naturally expire or unrelated effects appeared");
            var metrics = RenderParticleEngine.metrics();
            require(metrics.emitters() == 0 && metrics.alive() == 0, "Core particles survived recipe expiry");
            for (RenderHandle handle : owned.keySet()) require(!RenderEffects.removeLocal(handle), "Expired handle was still stored");
            owned.clear();
            result.addProperty("naturalExpiryVerified", true);
            result.addProperty("particlesAfterExpiry", metrics.alive());
            result.addProperty("emittersAfterExpiry", metrics.emitters());
            result.addProperty("status", "PASS");
            writeReport();
            scene = null;
        }
    }

    private void beginScene() throws IOException {
        require(owned.isEmpty() && RenderLibClientState.current().isEmpty(), "Previous scene not empty");
        boolean fallback = scene.pass().equals("shader-fallback"), reduced = scene.pass().equals("reduced-motion");
        RenderLibConfig.CLIENT.shaders.set(!fallback);
        RenderLibConfig.CLIENT.animationStrength.set(reduced ? 0.0 : 1.0);
        RenderLibConfig.CLIENT.reducedFlashing.set(reduced);
        mc.options.screenEffectScale().set(reduced ? 0.0 : 1.0);
        sceneStarted = System.nanoTime();
        spawnedAt = expiresAt = level.getGameTime();
        frames = 0;
        captured = capturePending = captureInFlight = false;
        eye = player.getEyePosition();
        result = new JsonObject();
        result.addProperty("name", scene.pass() + "-" + scene.fixture().god().getPath());
        result.addProperty("pass", scene.pass());
        result.addProperty("god", scene.fixture().god().toString());
        result.addProperty("motif", scene.fixture().theme().motif());
        result.addProperty("status", "RUNNING");
        JsonArray recipes = new JsonArray();
        result.add("recipes", recipes);
        results.add(result);
        for (int variant = 0; variant < 5; variant++) {
            // Inspect small Minecraft sprites at combat distance, without moving the player/world.
            Vec3 position = eye.add(FORWARD.scale(5)).add(RIGHT.scale((variant - 2) * 1.25));
            require(level.hasChunkAt(BlockPos.containing(position)) && level.clip(new ClipContext(eye, position,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getType() == HitResult.Type.MISS,
                    "Gallery is obstructed; use an open Core-Smoke save, not world edits");
            ResourceLocation id = scene.fixture().ids().get(variant);
            Cue cue = displayCue(id);
            Vec3 link = displayLink(id);
            var recipe = BlessingVisuals.recipe(scene.fixture().god(), id, cue,
                    RenderAnchor.world(position), FORWARD, link, 1.1);
            checkParticles(recipe, scene.fixture(), id.toString());
            var spec = recipe.visual();
            var params = spec.parameters();
            require(spec.primitive().equals(PRIMITIVE) && spec.anchor().kind() == RenderAnchor.Kind.WORLD
                    && spec.anchor().position().equals(position) && params.getInt("Variant") == variant
                    && params.getString("GodId").equals(scene.fixture().god().toString())
                    && params.getString("God").equals(scene.fixture().theme().motif())
                    && params.getString("BlessingId").equals(id.toString()) && params.getString("Cue").equals(cue.name()),
                    "Recipe lost registered identity / world anchor: " + id);
            require(!recipe.particles().collision().authoritative(), "Synthetic emitter requested gameplay collision");
            var appearance = recipe.particles().appearance();
            require(appearance.material().equals(PIXEL_MATERIAL) && appearance.glowStrength() == 0
                    && appearance.bloom() == ParticleBloomMode.OFF
                    && appearance.texture().filter(texture -> texture.getNamespace().equals("minecraft")).isPresent(),
                    "Recipe lost its themed, non-glowing pixel material: " + id);
            track(RenderParticles.spawnLocal(recipe.particles()), recipe.particles().toEffectSpec());
            JsonObject entry = new JsonObject();
            entry.addProperty("id", id.toString()); entry.addProperty("variant", variant); entry.addProperty("cue", cue.name());
            entry.addProperty("mesh", recipe.renderMesh()); entry.addProperty("particles", recipe.emitParticles());
            entry.addProperty("x", position.x); entry.addProperty("y", position.y); entry.addProperty("z", position.z);
            entry.addProperty("particleMaterial", appearance.material().toString());
            entry.addProperty("particleGlowStrength", appearance.glowStrength());
            entry.addProperty("particleBloom", appearance.bloom().name());
            entry.addProperty("particleSprite", appearance.texture().orElseThrow().toString());
            entry.addProperty("godPaletteVerified", true);
            recipes.add(entry);
        }
        writeReport();
    }

    private static Cue displayCue(ResourceLocation id) {
        return switch (id.getPath()) {
            case "radiant_strike", "serpents_bite", "venomblood", "hammerhand", "echoing_strike",
                    "crushing_blow", "scholars_edge", "tempest_wrath", "lightning_conduit", "fateful_draw",
                    "threadsever", "thornskin", "lightbearers_oath", "clockwork_punch", "blazing_hands",
                    "kindling_fury" -> Cue.IMPACT;
            case "toxic_skin", "solar_ward", "coiled_retribution", "stoneform", "unyielding", "foresight",
                    "mind_mirror", "shockshield", "wraithwalk", "silver_mirror", "frozen_heart",
                    "windcallers_reflex", "divine_radiance", "fireproof", "celestial_ward", "starlit_mirror" -> Cue.WARD;
            case "sunskin", "savage_hunger", "soul_leech", "dread_consumption", "moondrinker",
                    "second_chance", "rains_favor", "cleansing_flame" -> Cue.HEAL;
            case "feral_step", "battle_rush", "stormstep", "wavebound", "gale_step", "cyclone_leap",
                    "shearwind_dash", "overdrive_loop", "gravity_fold" -> Cue.TRAIL;
            case "shadowstep", "phase_blink", "echo_step", "arcane_recall" -> Cue.TELEPORT;
            case "trackers_mark", "veilpiercer", "insightful_kill" -> Cue.MARK;
            case "earthbind_grip", "alphas_call", "warlords_will", "witherbound", "lull_of_the_lost", "icy_pulse",
                    "frostbite_mantle", "permafrost_surge", "undercurrent_grip", "tidecallers_wrath",
                    "tempest_spiral", "rootgrip" -> Cue.CONTROL;
            case "corona_ascendant", "forgeheart", "ghastly_aura", "twilight_veil", "predicted_path",
                    "aquatic_grace", "stonewardens_embrace", "wildheart_bloom", "beacons_grace",
                    "tinkers_grace", "inferno_mantle" -> Cue.AURA;
            case "charge_relay" -> Cue.RECALL;
            case "sunburst", "predators_focus", "fury_unbound", "snowveil", "verdant_touch",
                    "dawnshatter", "auto_smelter", "precision_impact", "flash_flare", "pocket_dimension" -> Cue.CAST;
            default -> throw new IllegalArgumentException("No gameplay cue specified for gallery blessing " + id);
        };
    }

    private static Vec3 displayLink(ResourceLocation id) {
        return switch (id.getPath()) {
            case "phase_blink", "echo_step", "arcane_recall", "soul_leech", "dread_consumption",
                    "stormstep", "wavebound", "shearwind_dash", "charge_relay", "icy_pulse",
                    "thornskin", "shockshield" -> RIGHT.scale(1.5);
            case "cyclone_leap" -> new Vec3(0, 1.5, 0);
            case "celestial_ward", "windcallers_reflex" -> RIGHT.scale(-0.4);
            case "starlit_mirror" -> RIGHT.scale(0.7);
            default -> null;
        };
    }

    private void track(RenderHandle handle, RenderEffectSpec spec) {
        owned.put(handle, spec); // Track before assertions so a rejected fixture can still be cleaned up.
        require(!handle.synchronizedEffect() && handle.dimension().equals(level.dimension()), "Not a local current-world handle");
        require(spec.durationTicks() >= 8 && spec.durationTicks() <= 32, "Unbounded / uncapturable recipe lifetime");
        expiresAt = Math.max(expiresAt, spawnedAt + spec.durationTicks());
    }

    private void frame() throws IOException {
        context();
        if (mc.getOverlay() != null || scene == null) return;
        frames++;
        if (!capturePending || captureInFlight) return;
        require(player.getEyePosition().distanceTo(eye) < 0.35, "Player moved during gallery capture");
        var state = RenderLibClientState.current();
        int expectedHandles = 5, expectedEmitters = 5;
        require(state.size() == expectedHandles && owned.size() == expectedHandles,
                "Authored channel count differs from Core handles: state=" + state.size()
                        + ", owned=" + owned.size() + ", elapsed=" + (level.getGameTime() - spawnedAt)
                        + ", frames=" + frames);
        require(state.stream().noneMatch(effect -> effect.spec().primitive().equals(PRIMITIVE)), "Combat mesh handle appeared");
        for (var entry : owned.entrySet()) {
            var effect = state.stream().filter(value -> value.id().equals(entry.getKey().id())).findFirst().orElseThrow();
            require(effect.spec().equals(entry.getValue()) && effect.revision() == 0
                    && effect.startGameTime() == spawnedAt && effect.endGameTime() == spawnedAt + effect.spec().durationTicks()
                    && !effect.expired(level.getGameTime()), "Core local state/lifetime differs from recipe");
        }
        var metrics = RenderParticleEngine.metrics();
        require(metrics.emitters() == expectedEmitters && metrics.alive() >= expectedEmitters
                && metrics.rendered() == metrics.alive() && metrics.vertices() == metrics.rendered() * 4,
                "Synthetic particles did not render: " + metrics);
        boolean reduced = scene.pass().equals("reduced-motion");
        require(RenderLibConfig.CLIENT.animationStrength.get() == (reduced ? 0.0 : 1.0)
                && RenderLibConfig.CLIENT.reducedFlashing.get() == reduced
                && mc.options.screenEffectScale().get() == (reduced ? 0.0 : 1.0), "Reduced-motion options mismatch");
        boolean shaders = !scene.pass().equals("shader-fallback");
        require(RenderLibShaders.particleShaderAvailable() == shaders, "Particle shader/fallback route mismatch");
        if (!shaders) for (var spec : owned.values()) require(!RenderLibShaders.customAvailable(spec.material()), "Custom shader survived fallback");
        var emitters = state.stream().map(effect -> ParticleEmitterSpec.fromEffect(effect.spec()))
                .flatMap(java.util.Optional::stream).toList();
        require(emitters.size() == expectedEmitters && emitters.stream().allMatch(emitter ->
                        emitter.appearance().material().equals(PIXEL_MATERIAL)
                                && emitter.appearance().glowStrength() == 0
                                && emitter.appearance().bloom() == ParticleBloomMode.OFF
                                && emitter.appearance().renderer().equals(new ResourceLocation("nightshade_core", "textured_billboard"))
                                && emitter.appearance().texture().filter(verifiedSprites::containsKey).isPresent()),
                "Live Core emitters are not using the non-glowing pixel material");
        // Core's particle batch uses this material-aware lookup. Query only; never bind/apply a shader here.
        var selected = RenderLibShaders.particle(PIXEL_MATERIAL);
        var generic = RenderLibShaders.particle();
        var vanilla = GameRenderer.getPositionTexColorShader();
        require(selected != null && generic != null && vanilla != null, "Missing particle shader instance");
        ResourceLocation selectedName = new ResourceLocation(selected.getName());
        require(selectedName.equals(shaders ? PIXEL_SHADER : FALLBACK_SHADER),
                "Pixel material selected the wrong crisp/fallback shader: " + selectedName);
        require(shaders ? selected != generic && selected != vanilla : selected == vanilla && selected == generic,
                "Pixel material registration/fallback route mismatch; Core halo or generic fallback selected unexpectedly");
        require(metrics.bloomPasses() == 0, "Non-glowing pixel particles unexpectedly requested bloom");
        result.addProperty("pixelParticleMaterial", PIXEL_MATERIAL.toString());
        result.addProperty("pixelParticleShader", selectedName.toString());
        result.addProperty("pixelMaterialOverrideSelected", selected != generic);
        result.addProperty("pixelVanillaFallbackSelected", selected == vanilla);
        result.addProperty("pixelShaderSelectionVerified", true);
        result.addProperty("pixelEmittersVerified", emitters.size());
        result.addProperty("combatMeshHandles", 0);
        result.addProperty("minecraftSpritesVisible", true);
        result.addProperty("godPaletteVerified", true);
        result.addProperty("reducedMotionOptionsVerified", true);
        result.addProperty("particleBloomPasses", metrics.bloomPasses());
        result.addProperty("localHandles", owned.size());
        result.addProperty("particleShaderAvailable", RenderLibShaders.particleShaderAvailable());
        result.addProperty("animationStrength", RenderLibConfig.CLIENT.animationStrength.get());
        result.addProperty("screenEffectScale", mc.options.screenEffectScale().get());
        result.addProperty("reducedFlashing", RenderLibConfig.CLIENT.reducedFlashing.get());
        result.addProperty("renderedParticles", metrics.rendered());
        result.addProperty("particleVertices", metrics.vertices());
        result.addProperty("renderedFrames", frames);
        result.addProperty("coreStateVerified", true);
        capturePending = false;
        captureInFlight = true;
        Scene capturedScene = scene;
        String name = String.format(java.util.Locale.ROOT, "%02d-%s.png", results.size(), result.get("name").getAsString());
        Path image = artifacts.resolve("screenshots").resolve(name);
        Screenshot.grab(artifacts.toFile(), name, mc.getMainRenderTarget(), message -> mc.execute(() -> {
            if (finished || scene != capturedScene) return;
            try {
                context();
                require(Files.isRegularFile(image) && Files.size(image) > 32, "Screenshot writer failed: " + message.getString());
                result.addProperty("screenshot", image.toString());
                result.addProperty("screenshotBytes", Files.size(image));
                captured = true;
                captureInFlight = false;
                writeReport();
            } catch (Throwable failure) { fail(failure); }
        }));
    }

    private void context() {
        require(System.nanoTime() - started < RUN_LIMIT, "Eight-minute effects smoke deadline exceeded");
        require(scene == null || System.nanoTime() - sceneStarted < TimeUnit.SECONDS.toNanos(30), "Scene timed out");
        require(mc.player == player && mc.level == level && mc.getSingleplayerServer() == scope.server()
                && player.isAlive() && !player.isRemoved(), "Isolated world/player changed");
        require(mc.screen == null && player.containerMenu == menu && menu.getCarried().isEmpty(), "Unrelated screen/menu opened");
    }

    private static void fail(Throwable failure) {
        LOGGER.error("DIVINITY_EFFECTS_SMOKE failure", failure);
        if (active == null) {
            terminal = true; // A rejected scope never changes options or closes an unrelated world.
            if (startup != null) startup.refused(failure.toString());
        } else active.finish(false, failure.toString());
    }

    private void finish(boolean passed, String detail) {
        if (finished) return;
        finished = terminal = true;
        JsonArray cleanupErrors = new JsonArray();
        if (privateFixture != null) {
            privateCheck.addProperty("status", "FAIL");
            privateCheck.addProperty("failure", detail);
            if (mc.level == level) for (var effect : privateFixture.effects()) {
                try {
                    PrivateBlessingEffectsClient.apply(PrivateBlessingEffectsPacket.remove(privateFixture.dimension(), effect.id()));
                } catch (Throwable failure) { cleanupErrors.add("Private fixture: " + failure); }
            }
            privateFixture = null;
        }
        if (!passed && result != null) { result.addProperty("status", "FAIL"); result.addProperty("failure", detail); }
        for (RenderHandle handle : owned.keySet()) {
            try { RenderEffects.removeLocal(handle); } catch (Throwable failure) { cleanupErrors.add(failure.toString()); }
        }
        owned.clear();
        try { options.restore(mc); } catch (Throwable failure) { cleanupErrors.add("Options: " + failure); }
        boolean restored = options.equals(Options.capture(mc));
        boolean sameWorld = mc.player == player && mc.level == level && mc.getSingleplayerServer() == scope.server();
        boolean clean = sameWorld && RenderLibClientState.current().isEmpty();
        boolean expired = passed && results.size() == 54 && scenes.isEmpty()
                && privateCheck.has("status") && privateCheck.get("status").getAsString().equals("PASS");
        passed &= restored && clean && expired && player.containerMenu == menu && cleanupErrors.isEmpty();
        report.addProperty("status", passed ? "PASS" : "FAIL");
        report.addProperty("detail", detail);
        report.addProperty("finishedAt", Instant.now().toString());
        report.addProperty("clientOptionsRestored", restored);
        report.addProperty("gameplayMenuUnchanged", player.containerMenu == menu);
        report.addProperty("coreStateEmpty", clean);
        report.addProperty("allScenesNaturallyExpired", expired);
        report.addProperty("remainingScenarios", scenes.size());
        report.add("cleanupErrors", cleanupErrors);
        boolean exit = Boolean.getBoolean("divinity_engine.effectsSmokeAutoExit") && sameWorld;
        report.addProperty("autoExitPerformed", exit);
        try { writeReport(); } catch (Throwable failure) { LOGGER.error("Could not write effects report", failure); }
        LOGGER.info("DIVINITY_EFFECTS_SMOKE {}: {}; artifacts={}", passed ? "PASS" : "FAIL", detail, artifacts);
        if (exit) mc.stop();
    }

    private void writeReport() throws IOException {
        if (artifacts != null) Files.writeString(artifacts.resolve("report.json"),
                new GsonBuilder().setPrettyPrinting().create().toJson(report));
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
