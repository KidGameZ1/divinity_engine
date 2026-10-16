package net.nightshade.divinity_engine.client.innerworld;

import com.google.gson.*;
import com.mojang.logging.LogUtils;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.nightshade.divinity_engine.api.innerworld.InnerWorldDefinition;
import net.nightshade.divinity_engine.entity.GodManifestationEntity;
import net.nightshade.divinity_engine.network.cap.player.gods.PlayerGodsCapability;
import net.nightshade.divinity_engine.registry.dimensions.DimensionRegistry;
import net.nightshade.divinity_engine.util.divinity.innerworld.*;
import net.nightshade.divinity_engine.util.divinity.narrative.DivineNarrativeManager;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/** Opt-in real entry/render smoke. Only a manifest-approved disposable Core-Smoke copy is eligible. */
@Mod.EventBusSubscriber(modid = "divinity_engine", value = Dist.CLIENT)
public final class CommunionSmokeTest {
    private static final String PROPERTY = "divinity_engine.communionSmokeTest";
    private static final boolean ENABLED = Boolean.getBoolean(PROPERTY);
    private static final boolean AUTO_OPEN = Boolean.getBoolean(PROPERTY + ".autoOpen");
    private static Preview active;
    private static boolean finished;

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (!ENABLED || finished || event.phase != TickEvent.Phase.END) return;
        try {
            if (active == null) active = new Preview(Minecraft.getInstance());
            active.tick();
        } catch (Throwable failure) { fail(failure); }
    }

    @SubscribeEvent public static void frame(TickEvent.RenderTickEvent event) {
        if (!ENABLED || finished || active == null || event.phase != TickEvent.Phase.END) return;
        try { active.frame(); } catch (Throwable failure) { fail(failure); }
    }

    private static void fail(Throwable failure) {
        LogUtils.getLogger().error("COMMUNION_SMOKE failed", failure);
        if (active != null) active.complete(failure);
        else finished = true; // Refused paths must not write artifacts or stop a normal client.
    }

    private enum Stage { OPENING, PREPARING, OFFER, ENTERING, INSPECTING, DIALOGUE, REALM, RETURNING, VERIFY_RETURN }
    private record Origin(ResourceLocation dimension, Vec3 position) {}
    private record Realm(BlockPos origin, UUID statue, UUID body, boolean nameAbsent, int air, int barriers) {}

    private static final class Preview {
        private final Minecraft mc;
        private final Path game, world, root;
        private final String filter;
        private final JsonObject report = new JsonObject();
        private final JsonArray captures = new JsonArray(), openingScreens = new JsonArray(), checks = new JsonArray();
        private final List<String> assertionFailures = new ArrayList<>();
        private final Set<String> acceptedWarnings = new HashSet<>();
        private final long started = System.nanoTime();
        private final boolean oldPause, oldHideGui, oldBob;
        private final int oldScale, oldFov;
        private final CameraType oldCamera;
        private Stage stage = Stage.OPENING;
        private long stageAt = started;
        private long activeSeenAt;
        private String lastEntryView;
        private int ticks, stageTick, index, readyTicks, renderedFrames;
        private boolean openRequested, onboardingAccepted, captureInFlight;
        private Screen observedScreen, ownedWarning;
        private JsonObject openingRow;
        private IntegratedServer server;
        private UUID playerId;
        private List<InnerWorldDefinition> definitions;
        private CompletableFuture<?> pending;
        private Origin origin;
        private Realm realm;
        private String captureName;
        private Runnable afterCapture;

        private Preview(Minecraft mc) throws IOException {
            this.mc = mc;
            game = mc.gameDirectory.toPath().toRealPath();
            Path temp = Path.of(System.getProperty("java.io.tmpdir")).toRealPath();
            require(game.getFileName().toString().startsWith("nightshade-production-smoke-communion-")
                    && (game.getParent().equals(temp) || game.getParent().equals(Path.of("/tmp").toRealPath())), "Not a disposable temporary communion directory");
            Path manifestPath = game.resolve("communion-preview-manifest.json");
            require(!Files.isSymbolicLink(manifestPath), "Linked manifest");
            var manifest = JsonParser.parseString(Files.readString(manifestPath)).getAsJsonObject();
            require(manifest.get("kind").getAsString().equals("communion-preview-v1")
                    && Path.of(manifest.get("runDirectory").getAsString()).toRealPath().equals(game)
                    && manifest.get("worldName").getAsString().equals("Core-Smoke"), "Invalid communion preview manifest");
            Path saves = game.resolve("saves"), expected = saves.resolve("Core-Smoke");
            require(!Files.isSymbolicLink(saves) && !Files.isSymbolicLink(expected), "Linked fixture save");
            world = expected.toRealPath();
            require(world.equals(game.resolve("saves/Core-Smoke"))
                    && !world.equals(Path.of(manifest.get("worldSource").getAsString()).toRealPath()), "Not a disposable fixture copy");
            try (var paths = Files.walk(world)) { require(paths.noneMatch(Files::isSymbolicLink), "Linked descendant in fixture world"); }
            require(Files.isRegularFile(world.resolve("level.dat")), "Missing fixture level.dat");
            require(System.getProperties().stringPropertyNames().stream().noneMatch(key -> !key.startsWith(PROPERTY)
                    && (key.startsWith("divinity_engine.") || key.startsWith("nightshade_core."))
                    && (key.contains("Smoke") || key.contains("Preview")) && Boolean.getBoolean(key)), "Another QA harness is enabled");
            filter = System.getProperty(PROPERTY + ".god", "");
            require(filter.equals(manifest.get("godFilter").getAsString()), "God filter differs from manifest");
            root = game.resolve("communion-smoke");
            require(!Files.exists(root), "Smoke artifacts already exist; use a fresh copy");
            Files.createDirectory(root);
            oldPause = mc.options.pauseOnLostFocus; oldHideGui = mc.options.hideGui; oldBob = mc.options.bobView().get();
            oldScale = mc.options.guiScale().get(); oldFov = mc.options.fov().get(); oldCamera = mc.options.getCameraType();
            report.addProperty("status", "RUNNING"); report.addProperty("godFilter", filter);
            report.addProperty("kind", "isolated real-entry void render smoke; not multiplayer, combat, all-god visual, or pixel-level certification");
            report.addProperty("limitations", "Fixture-only survival pad, peaceful difficulty and contact seeding. No sky/fog/particle overrides. Realm PNG hides HUD; inspect images manually. Startup accepts only onboarding and the owned experimental warning.");
            report.add("launchManifest", manifest); report.add("captures", captures); report.add("openingScreens", openingScreens); report.add("realmChecks", checks);
            write();
        }

        private void tick() throws Exception {
            ticks++;
            require(System.nanoTime() - started < TimeUnit.MINUTES.toNanos(10), "Ten-minute smoke deadline exceeded");
            require(System.nanoTime() - stageAt < TimeUnit.SECONDS.toNanos(stage == Stage.OPENING ? 180 : 90), "Timed out in " + stage);
            if (stage == Stage.OPENING) { opening(); return; }
            require(mc.getSingleplayerServer() == server && mc.player != null && mc.level != null
                    && mc.player.getUUID().equals(playerId), "Fixture connection/player changed");
            require(server.getWorldPath(LevelResource.ROOT).toRealPath().equals(world), "Active save escaped fixture");
            if (captureName != null || captureInFlight) return;
            if (pending != null && !pending.isDone()) return;
            if (pending != null) pending.join(); // Propagate failures from server-thread actions.
            var definition = definitions.get(index);
            switch (stage) {
                case PREPARING -> {
                    if (pending != null) { origin = (Origin) pending.join(); pending = null; }
                    if (ticks - stageTick > 40) { CommunionClient.open(definition.god); stage(Stage.OFFER); }
                }
                case OFFER -> {
                    if (!(mc.screen instanceof CommunionScreen) || ticks - stageTick < 20) return;
                    validateScreen();
                    capture("offer-" + definition.god.getPath(), () -> {
                        pending = server.submit(() -> {
                            var player = player(); player.getPersistentData().remove("DivinityCommunionCooldown");
                            String error = CommunionManager.eligibility(player, definition.god);
                            require(error.isEmpty(), "Entry ineligible: " + error);
                            require(CommunionManager.begin(player, definition.god), "Entry request refused");
                        });
                        stage(Stage.ENTERING);
                    });
                }
                case ENTERING -> {
                    boolean inRealm = CommunionClient.active();
                    String view = clientView();
                    if (!Objects.equals(lastEntryView, view)) {
                        lastEntryView = view; report.addProperty("lastEntryView", view);
                        report.addProperty("entryClientActive", inRealm); write();
                    }
                    if (!inRealm) { readyTicks = 0; return; }
                    if (activeSeenAt == 0) activeSeenAt = System.nanoTime();
                    boolean dialogueReady = mc.screen instanceof CommunionScreen && !mc.screen.isPauseScreen() && mc.getOverlay() == null;
                    readyTicks = dialogueReady ? readyTicks + 1 : 0;
                    // Windup/chunk transfer consume the old entry delay; ACTIVE alone does not mean the GUI has settled.
                    if (readyTicks < 20) {
                        if (System.nanoTime() - activeSeenAt > TimeUnit.SECONDS.toNanos(30) && mc.getOverlay() == null) {
                            report.addProperty("automaticDialogueObserved", false); write();
                            capture("entry-diagnostic-" + definition.god.getPath(), () -> complete(new IllegalStateException(
                                    "Active realm never settled on its automatic communion dialogue; " + clientView())));
                        }
                        return;
                    }
                    report.addProperty("automaticDialogueObserved", true);
                    report.addProperty("dialogueReadyTicks", readyTicks);
                    report.addProperty("activeToDialogueMillis", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - activeSeenAt));
                    validateScreen(); require(CommunionClient.motif().equals("void"), "Client received non-void motif");
                    pending = server.submit(() -> inspectRealm(definition)); stage(Stage.INSPECTING);
                }
                case INSPECTING -> {
                    realm = (Realm) pending.join(); pending = null;
                    var row = new JsonObject(); row.addProperty("god", definition.god.toString());
                    row.addProperty("air", realm.air); row.addProperty("barriers", realm.barriers); row.addProperty("serverNameAbsent", realm.nameAbsent); checks.add(row);
                    if (!realm.nameAbsent) assertionFailures.add(definition.god + ": server manifestation retains a custom name");
                    stage(Stage.DIALOGUE);
                }
                case DIALOGUE -> {
                    validateScreen();
                    capture("dialogue-" + definition.god.getPath(), () -> {
                        mc.setScreen(null); mc.options.hideGui = true; stage(Stage.REALM);
                    });
                }
                case REALM -> {
                    readyTicks = realmReady() ? readyTicks + 1 : 0;
                    if (readyTicks == 0) renderedFrames = 0;
                    if (readyTicks < 20 || renderedFrames < 20 || ticks - stageTick < 40) return;
                    var statue = mc.level.getEntitiesOfClass(GodManifestationEntity.class, mc.player.getBoundingBox().inflate(32)).get(0);
                    boolean nameAbsent = !statue.hasCustomName() && !statue.isCustomNameVisible();
                    checks.get(checks.size() - 1).getAsJsonObject().addProperty("clientNameAbsent", nameAbsent);
                    if (!nameAbsent) assertionFailures.add(definition.god + ": client manifestation retains a custom name");
                    capture("realm-" + definition.god.getPath(), () -> {
                        mc.options.hideGui = false;
                        pending = server.submit(() -> CommunionManager.end(player(), CommunionLedger.Exit.NORMAL));
                        stage(Stage.RETURNING);
                    });
                }
                case RETURNING -> {
                    if (CommunionClient.active() || !mc.level.dimension().location().equals(origin.dimension)
                            || mc.player.position().distanceToSqr(origin.position) > 1 || ticks - stageTick < 30) return;
                    pending = server.submit(() -> {
                        var player = player();
                        require(CommunionManager.session(player) == null && !player.getPersistentData().contains(CommunionLedger.PLAYER_KEY), "Session survived return");
                        require(player.serverLevel().dimension().location().equals(origin.dimension)
                                && player.position().distanceToSqr(origin.position) < 1, "Server did not return to origin");
                        require(player.serverLevel().getEntity(realm.body) == null, "Trance body survived return");
                        require(server.getLevel(DimensionRegistry.INNER_WORLD).getEntity(realm.statue) == null, "Manifestation survived return");
                        player.getPersistentData().remove("DivinityCommunionCooldown");
                    });
                    stage(Stage.VERIFY_RETURN);
                }
                case VERIFY_RETURN -> {
                    pending = null; checks.get(checks.size() - 1).getAsJsonObject().addProperty("returnedToOrigin", true); index++;
                    if (index == definitions.size()) {
                        require(captures.size() == definitions.size() * 3, "Missing required captures");
                        require(assertionFailures.isEmpty(), String.join("; ", assertionFailures));
                        complete(null);
                    } else { CommunionClient.open(definitions.get(index).god); stage(Stage.OFFER); }
                }
                default -> throw new IllegalStateException("Unexpected stage " + stage);
            }
        }

        private void opening() throws Exception {
            require(mc.level == null || mc.hasSingleplayerServer(), "Multiplayer is not eligible");
            if (openingRow == null || observedScreen != mc.screen) {
                require(openingScreens.size() < 30, "Unexpected startup prompt loop");
                observedScreen = mc.screen; openingRow = new JsonObject();
                openingRow.addProperty("class", mc.screen == null ? "none" : mc.screen.getClass().getName());
                openingRow.addProperty("title", mc.screen == null ? "" : mc.screen.getTitle().getString());
                openingRow.addProperty("titleKey", mc.screen == null ? "" : key(mc.screen.getTitle()));
                JsonArray buttons = new JsonArray();
                if (mc.screen != null) for (var child : mc.screen.children()) if (child instanceof AbstractButton button) {
                    JsonObject row = new JsonObject(); row.addProperty("key", key(button.getMessage())); row.addProperty("label", button.getMessage().getString()); buttons.add(row);
                }
                openingRow.add("buttons", buttons); openingScreens.add(openingRow); write();
            }
            if (AUTO_OPEN && mc.level == null && mc.getSingleplayerServer() == null && mc.getOverlay() == null) {
                Screen screen = mc.screen;
                String action = null;
                if (!openRequested && screen != null && screen.getClass() == AccessibilityOnboardingScreen.class && !onboardingAccepted) action = "gui.continue";
                else if (openRequested && screen == ownedWarning && experimental(screen)) {
                    require(!acceptedWarnings.contains(screen.getClass().getName()), "Repeated experimental warning");
                    require(screen.children().stream().noneMatch(child -> child instanceof Checkbox), "Refusing upgrade/cache dialog");
                    action = screen.getClass() == BackupConfirmScreen.class ? "selectWorld.backupJoinSkipButton" : "gui.proceed";
                }
                if (action != null) {
                    recheckOpening(); String expected = action;
                    var buttons = screen.children().stream().filter(child -> child instanceof AbstractButton).map(child -> (AbstractButton) child)
                            .filter(button -> button.active && button.visible && key(button.getMessage()).equals(expected)).toList();
                    require(buttons.size() == 1, "No unique expected startup action: " + action);
                    if (screen.getClass() == AccessibilityOnboardingScreen.class) onboardingAccepted = true;
                    else acceptedWarnings.add(screen.getClass().getName());
                    openingRow.addProperty("automaticAction", action); write(); buttons.get(0).onPress();
                    // Only a synchronous successor of our own prompt remains eligible.
                    ownedWarning = openRequested && experimental(mc.screen) ? mc.screen : null;
                    return;
                }
                if (screen instanceof TitleScreen && !openRequested) {
                    recheckOpening(); openRequested = true;
                    mc.createWorldOpenFlows().loadLevel(screen, "Core-Smoke");
                    ownedWarning = experimental(mc.screen) ? mc.screen : null;
                }
            }
            if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null) return;
            require(mc.getSingleplayerServer().getWorldPath(LevelResource.ROOT).toRealPath().equals(world), "Loaded world is not approved copy");
            if (mc.screen != null || mc.getOverlay() != null || !mc.player.isAlive()) { readyTicks = 0; return; }
            if (++readyTicks < 40) return;
            server = mc.getSingleplayerServer(); playerId = mc.player.getUUID();
            ResourceLocation god = filter.isEmpty() ? null : ResourceLocation.tryParse(filter);
            require(filter.isEmpty() || god != null, "Invalid god filter");
            definitions = InnerWorldsRegistry.values().stream().filter(d -> god == null || d.god.equals(god)).toList();
            require(!definitions.isEmpty(), "No Inner World matches " + filter);
            for (var definition : definitions) require(definition.width == 13 && definition.height == 8 && definition.depth == 13
                    && definition.template == null && definition.variants.isEmpty() && definition.visuals.motif().equals("void"), "Not a built-in statue-only void: " + definition.id);
            report.addProperty("realmsSelected", definitions.size()); report.addProperty("requiredCaptures", definitions.size() * 3);
            mc.options.pauseOnLostFocus = false; mc.options.hideGui = false; mc.options.guiScale().set(2);
            mc.options.fov().set(70); mc.options.bobView().set(false); mc.options.setCameraType(CameraType.FIRST_PERSON); mc.resizeDisplay();
            pending = server.submit(() -> {
                var player = player(); require(!CommunionManager.busy(player), "Fixture already has a communion session");
                server.setDifficulty(Difficulty.PEACEFUL, true);
                server.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
                player.setGameMode(GameType.SURVIVAL); player.getAbilities().flying = false; player.onUpdateAbilities();
                var base = player.blockPosition();
                for (int x = -5; x <= 5; x++) for (int z = -5; z <= 5; z++) {
                    player.serverLevel().setBlock(base.offset(x, -1, z), Blocks.STONE.defaultBlockState(), 2);
                    for (int y = 0; y < 4; y++) player.serverLevel().setBlock(base.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                }
                player.teleportTo(base.getX() + .5, base.getY(), base.getZ() + .5); player.setDeltaMovement(Vec3.ZERO); player.fallDistance = 0;
                player.getPersistentData().remove("DivinityCommunionCooldown");
                for (var definition : definitions) PlayerGodsCapability.load(player).updateGodInstance(DivineNarrativeManager.god(definition.god).createGodDefaultInstance(), false);
                return new Origin(player.level().dimension().location(), player.position());
            });
            stage(Stage.PREPARING);
        }

        private void recheckOpening() throws IOException {
            require(mc.level == null && mc.getSingleplayerServer() == null
                    && mc.gameDirectory.toPath().toRealPath().equals(game)
                    && !Files.isSymbolicLink(game.resolve("saves")) && !Files.isSymbolicLink(game.resolve("saves/Core-Smoke"))
                    && mc.getLevelSource().getBaseDir().resolve("Core-Smoke").toRealPath().equals(world), "World opener escaped fixture");
        }

        private ServerPlayer player() {
            require(server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize().equals(world), "Server action escaped fixture");
            var player = server.getPlayerList().getPlayer(playerId); require(player != null, "Fixture player disconnected"); return player;
        }

        private Realm inspectRealm(InnerWorldDefinition definition) {
            var player = player(); var session = CommunionManager.session(player);
            require(session != null && session.state == CommunionLedger.State.ACTIVE && session.god.equals(definition.god)
                    && player.level().dimension().equals(DimensionRegistry.INNER_WORLD), "Wrong active server realm");
            int air = 0, barriers = 0;
            for (BlockPos local : BlockPos.betweenClosed(0, 0, 0, 12, 7, 12)) {
                var state = player.serverLevel().getBlockState(session.origin().offset(local));
                require((state.isAir() || state.is(Blocks.BARRIER)) && state.equals(definition.scene.apply(local)), "Unexpected realm block at " + local);
                if (state.isAir()) air++; else barriers++;
            }
            var statues = player.serverLevel().getEntitiesOfClass(GodManifestationEntity.class, CommunionManager.bounds(session));
            require(statues.size() == 1 && statues.get(0).god().equals(definition.god.toString()), "Missing/duplicate/wrong manifestation");
            var statue = statues.get(0);
            require(statue.position().distanceToSqr(definition.manifestation.add(Vec3.atLowerCornerOf(session.origin()))) < .01, "Manifestation drifted");
            boolean nameAbsent = !statue.hasCustomName() && !statue.isCustomNameVisible();
            // Preserve screenshots on a name assertion failure; fail after capture and safe return.
            return new Realm(session.origin(), statue.getUUID(), session.body, nameAbsent, air, barriers);
        }

        private boolean realmReady() {
            if (!CommunionClient.active() || !CommunionClient.motif().equals("void") || mc.screen != null || mc.getOverlay() != null
                    || mc.getCameraEntity() != mc.player) return false;
            var definition = definitions.get(index);
            var statues = mc.level.getEntitiesOfClass(GodManifestationEntity.class, mc.player.getBoundingBox().inflate(32));
            if (statues.size() != 1 || !statues.get(0).getUUID().equals(realm.statue) || !statues.get(0).god().equals(definition.god.toString())
                    || statues.get(0).position().distanceToSqr(definition.manifestation.add(Vec3.atLowerCornerOf(realm.origin))) > .01
                    || mc.player.position().distanceToSqr(definition.spawn.add(Vec3.atLowerCornerOf(realm.origin))) > .25) return false;
            for (BlockPos local : BlockPos.betweenClosed(0, 0, 0, 12, 7, 12)) {
                var pos = realm.origin.offset(local);
                if (!mc.level.hasChunkAt(pos) || !mc.level.getBlockState(pos).equals(definition.scene.apply(local))) return false;
            }
            return mc.levelRenderer.isChunkCompiled(mc.player.blockPosition()) && mc.levelRenderer.isChunkCompiled(statues.get(0).blockPosition());
        }

        private void validateScreen() {
            require(mc.screen instanceof CommunionScreen && !mc.screen.isPauseScreen() && mc.getOverlay() == null,
                    "Missing/non-live communion screen; " + clientView());
            for (var child : mc.screen.children()) if (child instanceof AbstractWidget widget && widget.visible)
                require(widget.getX() >= 0 && widget.getY() >= 0 && widget.getX() + widget.getWidth() <= mc.screen.width
                        && widget.getY() + widget.getHeight() <= mc.screen.height, "Control outside screen");
        }

        private void capture(String name, Runnable next) { captureName = name; afterCapture = next; }

        private void frame() throws IOException {
            if (stage == Stage.REALM && readyTicks > 0) renderedFrames++;
            if (captureName == null || captureInFlight || mc.getOverlay() != null) return;
            if (stage == Stage.REALM && !realmReady()) return;
            if (stage == Stage.OFFER || stage == Stage.DIALOGUE) validateScreen();
            captureInFlight = true;
            String name = captureName; Runnable next = afterCapture;
            Screenshot.grab(root.toFile(), name + ".png", mc.getMainRenderTarget(), message -> mc.execute(() -> {
                if (finished) return;
                try {
                    Path image = root.resolve("screenshots").resolve(name + ".png");
                    require(Files.isRegularFile(image) && Files.size(image) > 0, "Screenshot failed: " + message.getString());
                    var row = new JsonObject(); row.addProperty("capture", name); row.addProperty("path", image.toString());
                    row.addProperty("bytes", Files.size(image)); row.addProperty("guiHidden", mc.options.hideGui);
                    row.addProperty("width", mc.getMainRenderTarget().width); row.addProperty("height", mc.getMainRenderTarget().height);
                    captures.add(row); captureName = null; afterCapture = null; captureInFlight = false; write(); next.run();
                } catch (Throwable failure) { fail(failure); }
            }));
        }

        private String clientView() {
            return "screen=" + (mc.screen == null ? "none" : mc.screen.getClass().getName())
                    + ", overlay=" + (mc.getOverlay() == null ? "none" : mc.getOverlay().getClass().getName());
        }

        private void stage(Stage next) {
            stage = next; stageAt = System.nanoTime(); stageTick = ticks; readyTicks = renderedFrames = 0;
            if (next == Stage.ENTERING) { activeSeenAt = 0; lastEntryView = null; }
        }
        private void write() throws IOException {
            report.addProperty("stage", stage.name()); report.addProperty("realmsVisited", index);
            Files.writeString(root.resolve("report.json"), new GsonBuilder().setPrettyPrinting().create().toJson(report));
        }

        private void complete(Throwable failure) {
            if (finished) return; finished = true;
            mc.options.pauseOnLostFocus = oldPause; mc.options.hideGui = oldHideGui; mc.options.guiScale().set(oldScale);
            mc.options.fov().set(oldFov); mc.options.bobView().set(oldBob); mc.options.setCameraType(oldCamera); mc.resizeDisplay();
            report.addProperty("status", failure == null ? "PASS" : "FAIL");
            report.addProperty("lastClientView", clientView());
            if (failure != null) report.addProperty("failure", failure.toString());
            JsonArray failures = new JsonArray(); assertionFailures.forEach(failures::add); report.add("assertionFailures", failures);
            try { write(); } catch (IOException error) { LogUtils.getLogger().error("Cannot write communion smoke report", error); }
            mc.stop();
        }
    }

    private static String key(Component text) { return text.getContents() instanceof TranslatableContents value ? value.getKey() : ""; }
    private static boolean experimental(Screen screen) {
        return screen != null && (screen.getClass() == ConfirmScreen.class || screen.getClass() == BackupConfirmScreen.class)
                && key(screen.getTitle()).equals("selectWorld.backupQuestion.experimental");
    }
    private static void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
    private CommunionSmokeTest() {}
}
