package net.nightshade.divinity_engine.client.codex;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.BackupConfirmScreen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.ChatFormatting;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.nightshade.divinity_engine.client.screen.DivineCodexScreen;
import net.nightshade.divinity_engine.client.screen.BlessingsScreen;
import net.nightshade.divinity_engine.client.screen.PocketDimensionInvScreen;
import net.nightshade.divinity_engine.client.innerworld.CommunionScreen;
import net.nightshade.divinity_engine.client.ui.NightshadeButton;
import net.nightshade.divinity_engine.client.ui.ConstellationButton;
import net.nightshade.divinity_engine.network.cap.player.gods.PlayerGodsCapability;
import net.nightshade.divinity_engine.util.divinity.gods.GodHelper;
import net.nightshade.divinity_engine.util.divinity.gods.DivineRelationship;
import net.nightshade.divinity_engine.divinity.gods.BaseGod;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.renderlib.ui.UiNode;
import net.nightshade.divinity_engine.world.inventory.BlessingsMenu;
import net.nightshade.divinity_engine.world.inventory.PocketDimensionInvMenu;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.nightshade.renderlib.client.ui.RenderLibContainerScreen;
import net.nightshade.renderlib.client.RenderLibShaders;
import net.nightshade.renderlib.config.RenderLibConfig;
import net.nightshade.divinity_engine.registry.gui.DivinityUiTypes;
import net.nightshade.renderlib.ui.UiRenderLayer;
import net.nightshade.divinity_engine.api.divine_codex_pages.DivineCodexPage;
import net.nightshade.divinity_engine.api.divine_codex_pages.DivineCodexPageDeferredRegister;
import net.nightshade.divinity_engine.registry.divinity.DivineCodexPagesRegistry;
import net.nightshade.divinity_engine.util.divinity.DivineCodexPages;
import net.nightshade.divinity_engine.world.inventory.DivineCodexMenu;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.regex.Pattern;

/**
 * Opt-in, synthetic client UI QA; never a server story/playthrough test.
 * Launch through docs/tools/run_ui_preview.mjs in a manifest-approved Core-Smoke copy.
 * -Ddivinity_engine.codexSmokeAutoExit=true additionally permits normal client shutdown.
 * No options, NBT, screens, artifacts or shutdown are touched if the isolation guard fails.
 */
@Mod.EventBusSubscriber(modid = "divinity_engine", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class CodexSmokeTest {
    private static final boolean ENABLED = Boolean.getBoolean("divinity_engine.codexSmokeTest");
    private static final boolean AUTO_OPEN = Boolean.getBoolean("divinity_engine.codexSmokeAutoOpen");
    private static final boolean POCKET_ONLY = Boolean.getBoolean("divinity_engine.codexSmokePocketOnly");
    private static final boolean DOMAIN_ONLY = Boolean.getBoolean("divinity_engine.codexSmokeDomainOnly");
    private static final boolean OVERFLOW_ONLY = Boolean.getBoolean("divinity_engine.codexSmokeBlessingsOverflowOnly");
    private static final boolean SHRINE_ONLY = Boolean.getBoolean("divinity_engine.codexSmokeShrineOnly");
    private static final boolean BLESSINGS_HEADER_ONLY = Boolean.getBoolean("divinity_engine.codexSmokeBlessingsHeaderOnly");
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String ISOLATION_MARKER = "nightshade-production-smoke-ui-";
    private static final String GUIDE_TITLE = "Smoke Guide: Beginning the Journal";
    private static final String GOD_TITLE = "Solarius: Synthetic Contact Guide";
    private static final String STORY_TITLE = "Solarius: Synthetic Current Objective";
    private static final String LONG_TITLE = "Solarius - Chapter 3: A Witness at Dawn, an Intentionally Long Chapter Heading for the Readable Journal Smoke Test";
    private static final String NEXT_TITLE = "Solarius - Chapter 4: Synthetic Follow-up";
    private static final String RELATION_TITLE = "Synthetic Relationship Note";
    private static final String BODY_QUERY = "CODEXSMOKEEMBERGLASS";
    private static final String STORY_ID = "codex_smoke:solarius/objective";
    private static final String LONG_PROOF = "Complete the chosen deed at the matching statue after hearing the witness. "
            + "Keep the promise even when the easy path would abandon it. PROOF-END.";
    private static final Pattern POINT_REQUIREMENT = Pattern.compile(
            "(?i)(?:\\b\\d+[+]?\\s+favor\\b|\\bfavor\\s+(?:required|remaining)\\b|worship budget|highest favor|next threshold)");
    private static final Pattern PAGE = Pattern.compile("\\bPage (\\d+) of (\\d+)\\.");
    private static final long BOOT_LIMIT = TimeUnit.MINUTES.toNanos(3);
    private static final long RUN_LIMIT = TimeUnit.MINUTES.toNanos(8);
    private static CodexSmokeTest active;
    private static Startup startup;
    private static boolean terminal;
    private static long bootStarted;
    private static int readyTicks;

    private record Scope(Path game, Path world, IntegratedServer server) {}
    private record Step(String name, Runnable action, BooleanSupplier ready, Runnable verify,
                        boolean screenshot, long timeout) {}

    private final Minecraft mc;
    private final Scope scope;
    private final LocalPlayer player;
    private final ClientLevel level;
    private final AbstractContainerMenu originalMenu;
    private final Tag originalJournal;
    private final int originalScale;
    private final boolean originalPauseOnLostFocus;
    private final boolean originalShaders;
    private final double originalScreenEffects;
    private final Deque<Step> steps = new ArrayDeque<>();
    private final JsonObject report = new JsonObject();
    private final JsonArray scenarios = new JsonArray();
    private final long started = System.nanoTime();
    private Path artifacts;
    private net.minecraft.client.gui.screens.Screen screen;
    private CompoundTag originalGods;
    private CompoundTag expectedJournal;
    private boolean journalTouched;
    private int journalUiDepth;
    private boolean optionsTouched;
    private boolean finished;
    private Step current;
    private JsonObject scenario;
    private long stepStarted;
    private int stepTicks;
    private int stepFrames;
    private int requestedScale;
    private boolean capturePending;
    private boolean captureInFlight;
    private CompletableFuture<Void> reload;
    private long generationBeforeReload;
    private net.nightshade.divinity_engine.divinity.gods.BaseGodInstance largeGod;

    private CodexSmokeTest(Minecraft mc, Scope scope) {
        this.mc = mc;
        this.scope = scope;
        player = mc.player;
        level = mc.level;
        originalMenu = player.containerMenu;
        Tag journal = player.getPersistentData().get(CodexJournal.DATA_KEY);
        originalJournal = journal == null ? null : journal.copy();
        originalScale = mc.options.guiScale().get();
        originalPauseOnLostFocus = mc.options.pauseOnLostFocus;
        originalShaders = RenderLibConfig.CLIENT.shaders.get();
        originalScreenEffects = mc.options.screenEffectScale().get();
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (!ENABLED || terminal || event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        try {
            if (active == null) {
                if (bootStarted == 0) bootStarted = System.nanoTime();
                if (startup == null) startup = new Startup(mc);
                if (System.nanoTime() - bootStarted > BOOT_LIMIT) {
                    refuse("Timed out waiting for the isolated Core-Smoke world; nothing changed");
                    return;
                }
                if (mc.level != null && !mc.hasSingleplayerServer()) {
                    refuse("Multiplayer is not eligible for Codex smoke automation");
                    return;
                }
                startup.tick();
                if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null) return;
                Scope scope = guardedScope(mc);
                if (mc.screen != null || mc.getOverlay() != null || !mc.player.isAlive()
                        || mc.player.containerMenu != mc.player.inventoryMenu
                        || !mc.player.containerMenu.getCarried().isEmpty()
                        || !mc.level.hasChunkAt(mc.player.blockPosition())) {
                    readyTicks = 0;
                    return;
                }
                // Allow the initial real login snapshot to settle before borrowing its client tag.
                if (++readyTicks < 60) return;
                active = new CodexSmokeTest(mc, scope);
                active.start();
            } else {
                active.tick();
            }
        } catch (Throwable failure) {
            if (active == null) refuse("Isolation/readiness guard failed: " + failure);
            else active.fail(failure);
        }
    }

    @SubscribeEvent
    public static void renderTick(TickEvent.RenderTickEvent event) {
        if (!ENABLED || terminal || event.phase != TickEvent.Phase.END) return;
        try {
            if (active != null) active.frame();
            else if (bootStarted != 0 && System.nanoTime() - bootStarted > BOOT_LIMIT) {
                refuse("Startup deadline exceeded; no isolated test was started");
            }
        } catch (Throwable failure) {
            if (active != null) active.fail(failure);
            else refuse("Render readiness failed: " + failure);
        }
    }

    private static Scope guardedScope(Minecraft mc) throws IOException {
        IntegratedServer server = mc.getSingleplayerServer();
        require(mc.isSingleplayer() && server != null, "An integrated singleplayer server is required");
        require(startup != null, "Approved startup manifest is required");
        Path game = mc.gameDirectory.toPath().toRealPath();
        Path world = server.getWorldPath(LevelResource.ROOT).toRealPath();
        Path expectedWorld = mc.getLevelSource().getBaseDir().resolve("Core-Smoke").toRealPath();
        require(game.equals(startup.game) && world.equals(startup.world) && world.equals(expectedWorld),
                "Save directory must match the manifest-approved disposable Core-Smoke copy");
        return new Scope(game, world, server);
    }

    private static void refuse(String detail) {
        terminal = true;
        // In an unapproved world even artifact writes and auto-exit are deliberately refused.
        LOGGER.error("DIVINITY_CODEX_SMOKE REFUSED: {}", detail);
        if (startup != null) startup.refused(detail);
    }

    /** Startup automation owns only the exact warning returned by its own copied-world opener. */
    private static final class Startup {
        private final Minecraft mc;
        private final Path game, world;
        private final JsonObject report = new JsonObject();
        private final JsonArray screens = new JsonArray();
        private final java.util.Set<String> accepted = new java.util.HashSet<>();
        private Screen observed, ownedWarning;
        private JsonObject row;
        private boolean opened, onboarding;

        private Startup(Minecraft mc) throws IOException {
            this.mc = mc;
            game = mc.gameDirectory.toPath().toRealPath();
            Path temp = Path.of(System.getProperty("java.io.tmpdir")).toRealPath();
            require(game.getFileName().toString().startsWith(ISOLATION_MARKER)
                    && (game.getParent().equals(temp) || game.getParent().equals(Path.of("/tmp").toRealPath())),
                    "UI preview requires a fresh OS-temp directory");
            Path manifestPath = game.resolve("ui-preview-manifest.json");
            require(!Files.isSymbolicLink(manifestPath), "Linked UI preview manifest");
            var manifest = JsonParser.parseString(Files.readString(manifestPath)).getAsJsonObject();
            require(manifest.get("kind").getAsString().equals("ui-preview-v1")
                    && manifest.get("worldName").getAsString().equals("Core-Smoke")
                    && Path.of(manifest.get("runDirectory").getAsString()).toRealPath().equals(game), "Invalid UI preview manifest");
            require(manifest.get("suite").getAsString().equals(BLESSINGS_HEADER_ONLY ? "header" : "full")
                    && !POCKET_ONLY && !DOMAIN_ONLY && !OVERFLOW_ONLY && !SHRINE_ONLY, "Manifest does not describe the enabled UI suite");
            Path expected = game.resolve("saves/Core-Smoke");
            require(!Files.isSymbolicLink(game.resolve("saves")) && !Files.isSymbolicLink(expected), "Linked fixture save");
            world = expected.toRealPath();
            require(world.equals(expected) && !world.equals(Path.of(manifest.get("worldSource").getAsString()).toRealPath()),
                    "UI fixture is not a disposable copy");
            try (var paths = Files.walk(world)) { require(paths.noneMatch(Files::isSymbolicLink), "Linked fixture descendant"); }
            require(Files.isRegularFile(world.resolve("level.dat")), "Missing fixture level.dat");
            require(System.getProperties().stringPropertyNames().stream().noneMatch(key -> !key.startsWith("divinity_engine.codexSmoke")
                    && (key.startsWith("divinity_engine.") || key.startsWith("nightshade_core."))
                    && (key.contains("Smoke") || key.contains("Preview")) && Boolean.getBoolean(key)), "Another QA harness is enabled");
            require(!Files.exists(game.resolve("divinity-codex-smoke")) && !Files.exists(game.resolve("ui-preview-startup.json")),
                    "UI preview directory was already used");
            report.addProperty("status", "OPENING"); report.add("manifest", manifest); report.add("openingScreens", screens); write();
        }

        private void tick() throws IOException {
            if (row == null || observed != mc.screen) {
                require(screens.size() < 30, "Unexpected startup screen loop");
                observed = mc.screen; row = new JsonObject();
                row.addProperty("class", observed == null ? "none" : observed.getClass().getName());
                row.addProperty("title", observed == null ? "" : observed.getTitle().getString());
                row.addProperty("titleKey", observed == null ? "" : translationKey(observed.getTitle()));
                JsonArray buttons = new JsonArray();
                if (observed != null) for (var child : observed.children()) if (child instanceof AbstractButton button) {
                    JsonObject entry = new JsonObject(); entry.addProperty("label", button.getMessage().getString());
                    entry.addProperty("key", translationKey(button.getMessage())); entry.addProperty("active", button.active);
                    entry.addProperty("visible", button.visible); buttons.add(entry);
                }
                row.add("buttons", buttons); screens.add(row); write();
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
            Files.writeString(game.resolve("ui-preview-startup.json"), new GsonBuilder().setPrettyPrinting().create().toJson(report));
        }

        private void refused(String detail) {
            try {
                if (!mc.gameDirectory.toPath().toRealPath().equals(game)) return;
                report.addProperty("status", "REFUSED"); report.addProperty("failure", detail); write();
                if (autoExit() && (mc.getSingleplayerServer() == null && mc.level == null
                        || mc.getSingleplayerServer() != null && mc.getSingleplayerServer().getWorldPath(LevelResource.ROOT).toRealPath().equals(world))) mc.stop();
            } catch (IOException error) { LOGGER.error("Cannot record UI startup refusal", error); }
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
        Path root = scope.game().resolve("divinity-codex-smoke");
        require(!Files.isSymbolicLink(root), "Artifact directory must not be a symlink");
        Files.createDirectories(root);
        require(root.toRealPath().getParent().equals(scope.game()), "Artifact directory escaped the game directory");
        artifacts = Files.createTempDirectory(root, "run-");
        report.addProperty("schemaVersion", 1);
        report.addProperty("testKind", BLESSINGS_HEADER_ONLY ? "blessings_header_ui_only" : DOMAIN_ONLY ? "pocket_domain_ui_only" : SHRINE_ONLY ? "statue_blessings_ui_only" : POCKET_ONLY ? "pocket_inventory_ui_only" : OVERFLOW_ONLY ? "blessings_overflow_ui_only" : "simulated_client_codex_ui_not_playthrough");
        report.addProperty("status", "RUNNING");
        report.addProperty("startedAt", Instant.now().toString());
        report.addProperty("world", "Core-Smoke");
        report.addProperty("worldDirectory", scope.world().toString());
        report.addProperty("artifacts", artifacts.toString());
        report.addProperty("serverOrWorldMutationsIssued", false);
        report.addProperty("serverProgressSimulated", false);
        report.addProperty("clientJournalFixtureSimulated", BLESSINGS_HEADER_ONLY || !DOMAIN_ONLY && !POCKET_ONLY && !OVERFLOW_ONLY && !SHRINE_ONLY);
        report.addProperty("journalFixtureScope", "UI callbacks only; live replica stays attached between callbacks");
        report.addProperty("autoExitRequested", autoExit());
        report.add("launchManifest", startup.report.get("manifest"));
        report.add("openingScreens", startup.screens);
        JsonArray limitations = new JsonArray();
        limitations.add("Synthetic player-visible NBT only: no real discovery, narrative playthrough, server sync, shrine action or reward is tested.");
        limitations.add("PNG artifacts require manual visual review. Widget geometry and narrated pagination do not prove absence of clipped glyphs.");
        limitations.add("Minecraft can clamp GUI scales to fit the physical window; requested scale and actual dimensions are recorded separately.");
        limitations.add("No world APIs or server commands are mutated by this runner. Normal integrated-server ticking/autosaves and optional vanilla shutdown are not a world-file immutability test.");
        limitations.add("Communion views use detached client tags without a Session UUID. No communion entry, branch choice, reward or server action is exercised; use the separate real-entry smoke for that coverage.");
        report.add("limitations", limitations);
        report.add("scenarios", scenarios);
        writeReport();
        optionsTouched = true;
        mc.options.pauseOnLostFocus = false;
        buildSteps();
        LOGGER.info("DIVINITY_CODEX_SMOKE started; synthetic UI artifacts: {}", artifacts);
    }

    private void buildSteps() {
        if (BLESSINGS_HEADER_ONLY) {
            addBlessingsHeaderSteps();
            addStoryConversationSteps();
            report.addProperty("plannedScenarios", steps.size());
            return;
        }
        add("registered-pages-without-journal", () -> {
            changeScale(2);
            installJournal(null);
            openScreen();
            chapter(CodexJournal.Section.REGISTERED_PAGES);
            verifyRegisteredPageProjection();
        }, () -> {
            require(hasButton("Welcome, Seeker of Power"), "Original welcome pages disappeared before journal sync");
            require(!hasButton(STORY_TITLE), "Registered pages exposed a locked story");
        }, true);
        add("original-page-reader", () -> press("Welcome, Seeker of Power"),
                () -> assertReading("Welcome, Seeker of Power", 1), true);
        add("registered-page-arrows", () -> {
            require(narration().contains("Registered page 1 /"), "Original book did not begin at its first page");
            int[] textPages = pageNumbers();
            for (int i = textPages[0]; i < textPages[1]; i++) {
                press("Next text page");
                require(narration().contains("Registered page 1 /"), "Text continuation changed the contents entry");
            }
            press("Next text page");
            require(narration().contains("Registered page 2 /"), "Next arrow did not advance to the next registered page");
            press("Previous text page");
        }, () -> require(narration().contains("Registered page 1 /"), "Previous arrow did not restore the original page"), false);
        add("original-pantheon-search", () -> {
            search("The Known Pantheon");
            press("The Known Pantheon");
        }, () -> assertReading("The Known Pantheon", 1), true);
        add("waiting-scale-2", () -> {
            changeScale(2);
            installJournal(null);
            openScreen();
            chapter(CodexJournal.Section.GETTING_STARTED);
        }, () -> {
            require(narration().contains("Waiting for journal sync"), "Expected friendly waiting state");
            require(!hasButton(STORY_TITLE), "Waiting state exposed a synthetic story before sync");
        }, true);
        add("synthetic-snapshot-arrives", () -> installJournal(fixture()),
                () -> require(hasButton(GUIDE_TITLE), "Journal did not refresh after the client snapshot appeared"), false);

        for (int scale : new int[]{2, 3, 4}) {
            String suffix = "-scale-" + scale;
            add("contents" + suffix, () -> {
                changeScale(scale);
                if (scale > 2) assertReading(LONG_TITLE, 2);
                chapter(CodexJournal.Section.GETTING_STARTED);
            }, () -> require(hasButton(GUIDE_TITLE), "Getting Started guide missing"), true);
            add("guide" + suffix, () -> press(GUIDE_TITLE), () -> assertReading(GUIDE_TITLE, 1), false);
            add("discovered-gods" + suffix, () -> {
                chapter(CodexJournal.Section.DISCOVERED_GODS);
                press(GOD_TITLE);
            }, () -> assertReading(GOD_TITLE, 1), false);
            add("relationships" + suffix, () -> {
                chapter(CodexJournal.Section.RELATIONSHIPS);
                press(RELATION_TITLE);
            }, () -> assertReading(RELATION_TITLE, 1), false);
            add("body-search" + suffix, () -> search(BODY_QUERY), () -> {
                require(hasButton(LONG_TITLE), "Case-insensitive global body search did not find the long chapter");
                require(!hasButton(GUIDE_TITLE) && !hasButton(STORY_TITLE), "Search retained unrelated fixture entries");
                require(narration().contains("1 entries"), "Expected one synthetic body-search match");
            }, true);
            add("empty-search" + suffix, () -> search("codexsmokenonexistententry"),
                    () -> require(narration().contains("0 entries"), "Empty-search fallback did not report zero entries"), false);
            add("long-reader" + suffix, () -> {
                search(BODY_QUERY);
                key(GLFW.GLFW_KEY_ENTER);
            }, () -> {
                assertReading(LONG_TITLE, 1);
                require(pageNumbers()[1] > 2, "Long chapter did not paginate");
            }, true);
            add("pagination" + suffix, () -> {
                press("Next text page");
                assertReading(LONG_TITLE, 2);
                key(GLFW.GLFW_KEY_PAGE_UP);
                assertReading(LONG_TITLE, 1);
                key(GLFW.GLFW_KEY_PAGE_DOWN);
            }, () -> assertReading(LONG_TITLE, 2), true);
            add("history" + suffix, () -> {
                press("Contents");
                require(searchBox().isVisible(), "Contents view did not return");
                press("Back");
            }, () -> assertReading(LONG_TITLE, 2), false);
            add("entry-navigation" + suffix, () -> {
                chapter(CodexJournal.Section.CHRONICLE);
                press(LONG_TITLE);
                press("Entry >");
                assertReading(NEXT_TITLE, 1);
                press("Entry <");
            }, () -> assertReading(LONG_TITLE, 1), false);
            add("objective-progress" + suffix, () -> {
                chapter(CodexJournal.Section.STORY_JOURNAL);
                press(STORY_TITLE);
                updateProgress(scale + 3, true);
            }, () -> assertProgress(scale + 3), true);
            add("same-revision-progress" + suffix, () -> updateProgress(scale + 5, false),
                    () -> assertProgress(scale + 5), false);
            add("reopen-and-resize" + suffix, () -> {
                chapter(CodexJournal.Section.CHRONICLE);
                press(LONG_TITLE);
                press("Next text page");
                openScreen();
                mc.resizeDisplay();
            }, () -> assertReading(LONG_TITLE, 2), false);
        }
        add("last-page-clamping", () -> goToPage(pageNumbers()[1]), () -> {
            int last = pageNumbers()[1];
            assertReading(LONG_TITLE, last);
            require(narration().contains("SMOKE-END"), "The final paragraph was lost during pagination");
            require(!hasButton("Next text page"), "Next-page control is enabled after the final page");
            key(GLFW.GLFW_KEY_PAGE_DOWN);
            assertReading(LONG_TITLE, last);
        }, true);
        add("logical-minimum-320x240", () -> {
            goToPage(2);
            screen.resize(mc, 320, 240);
        }, () -> {
            assertReading(LONG_TITLE, 2);
            require(screen.width == 320 && screen.height == 240, "Minimum logical screen size was not applied");
            require(!searchBox().isVisible(), "Minimum size should split contents and reader views");
            scenario.addProperty("logicalResizeOnly", true);
        }, true);
        steps.add(new Step("real-resource-reload", () -> {
            mc.resizeDisplay();
            generationBeforeReload = CodexResources.generation();
            reload = mc.reloadResourcePacks();
        }, () -> reload != null && reload.isDone(), () -> {
            reload.join();
            require(CodexResources.generation() > generationBeforeReload, "Codex resource-reload generation did not advance");
            assertReading(LONG_TITLE, 2);
            scenario.addProperty("generationBefore", generationBeforeReload);
            scenario.addProperty("generationAfter", CodexResources.generation());
        }, true, TimeUnit.SECONDS.toNanos(90)));
        add("nightshade-blessings-empty", () -> {
            RenderLibConfig.CLIENT.shaders.set(true);
            originalGods = PlayerGodsCapability.load(player).serializeNBT().copy();
            var empty = new CompoundTag();
            empty.putBoolean("resetExistingData", true);
            PlayerGodsCapability.load(player).deserializeNBT(empty);
            openBlessings();
        }, () -> {
            require(narration().contains("Make contact"), "Empty pantheon must explain contact without closing");
            require(!hasButton("Enter Trial"), "Inventory-opened empty menu offered a trial");
            verifyNightshade();
            require(RenderLibShaders.customAvailable(DivinityUiTypes.GALAXY), "Galaxy shader failed to load");
        }, true);
        add("nightshade-blessings-all-gods", () -> {
            changeScale(2);
            for (var god : GodHelper.getGods()) {
                var instance = god.createGodDefaultInstance();
                instance.setFavor(250);
                PlayerGodsCapability.load(player).updateGodInstance(instance, false);
            }
            openBlessings();
        }, () -> {
            for (var god : GodHelper.getGods()) {
                String name = Component.translatable(god.getNameTranslationKey()).getString();
                searchBox().setValue(name);
                press(name);
                require(narration().contains(name), "Cannot select god tab " + name);
                verifyDivineSeal(god.getRegistryName().getPath());
                require(!hasButton("Enter Trial"), "Tab browsing enabled trials without a statue");
                verifyWidgetGeometry();
            }
            searchBox().setValue("");
            verifyNightshade();
        }, true);
        for (String god : new String[]{"Solarius", "Nythea"}) {
            add("nightshade-celestial-seal-" + god.toLowerCase(java.util.Locale.ROOT), () -> {
                searchBox().setValue(god);
                press(god);
                searchBox().setValue("");
            }, () -> {
                verifyDivineSeal(god.toLowerCase(java.util.Locale.ROOT));
                verifyNightshade();
            }, true);
        }
        add("nightshade-galaxy-no-shaders", () -> RenderLibConfig.CLIENT.shaders.set(false), () -> {
            require(!RenderLibShaders.customAvailable(DivinityUiTypes.GALAXY), "Shaders-disabled fallback was not selected");
            verifyNightshade();
        }, true);
        add("nightshade-galaxy-reduced-motion", () -> {
            RenderLibConfig.CLIENT.shaders.set(true);
            mc.options.screenEffectScale().set(0.0);
        }, () -> {
            require(RenderLibShaders.customAvailable(DivinityUiTypes.GALAXY), "Galaxy shader was not restored");
            require(RenderLibShaders.forMaterial(DivinityUiTypes.GALAXY).getUniform("UiTime").getFloatBuffer().get(0) == 0,
                    "Reduced motion did not freeze the galaxy");
            verifyNightshade();
        }, true);
        steps.add(new Step("nightshade-galaxy-resource-reload", () -> {
            mc.options.screenEffectScale().set(originalScreenEffects);
            reload = mc.reloadResourcePacks();
        }, () -> reload != null && reload.isDone(), () -> {
            reload.join();
            require(RenderLibShaders.customAvailable(DivinityUiTypes.GALAXY), "Galaxy shader did not survive resource reload");
            verifyNightshade();
        }, true, TimeUnit.SECONDS.toNanos(90)));
        add("nightshade-blessings-search-typing", () -> {
            searchBox().setValue("");
            screen.setFocused(searchBox());
            key(GLFW.GLFW_KEY_E);
            require(screen.charTyped('e', 0), "Search rejected typing");
        }, () -> {
            require(searchBox().getValue().equals("e"), "Inventory hotkey interrupted search");
            require(mc.screen == screen, "Typing closed blessings");
            searchBox().setValue("no-such-god");
            require(!hasButton("Solarius"), "Empty search retained a god tab");
        }, true);
        for (int scale : new int[]{3, 4}) {
            add("nightshade-blessings-scale-" + scale, () -> {
                searchBox().setValue("Solarius");
                press("Solarius");
                changeScale(scale);
            }, this::verifyNightshade, true);
        }
        add("nightshade-blessings-320x240", () -> screen.resize(mc, 320, 240), () -> {
            require(narration().contains("Solarius"), "Resizing lost the selected god");
            verifyNightshade();
        }, true);
        add("nightshade-blessings-small-scroll", () -> {
            var first = buttons().stream().filter(button -> button.getMessage().getString().equals("Radiant Strike"))
                    .findFirst().orElseThrow();
            require(screen.mouseScrolled(first.getX() + 2, first.getY() + 2, -1), "Blessing grid did not scroll");
        }, () -> {
            require(hasButton("Corona Ascendant"), "Fifth blessing is unreachable at minimum resolution");
            searchBox().setValue("");
            require(hasButton("Solarius"), "Clearing search lost the selected god's tab");
        }, true);
        add("nightshade-pocket-slots", () -> {
            RenderLibConfig.CLIENT.shaders.set(true);
            changeScale(3);
            var menu = new PocketDimensionInvMenu(0, player.getInventory(), null);
            // Only this detached storage is populated; no player inventory or server menu is changed.
            menu.getSlot(0).set(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND, 64));
            menu.getSlot(13).set(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ENDER_PEARL, 16));
            var sword = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_SWORD);
            sword.setDamageValue(80);
            menu.getSlot(26).set(sword);
            screen = new SmokePocketScreen(menu, player.getInventory());
            mc.setScreen(screen);
        }, () -> {
            require(RenderLibShaders.customAvailable(DivinityUiTypes.GALAXY), "Pocket galaxy shader is unavailable");
            verifyPocket();
        }, true);
        add("nightshade-pocket-tooltip", () -> ((SmokePocketScreen) screen).hoverIndex = 13, () -> {
            verifyPocket();
            var container = (AbstractContainerScreen<?>) screen;
            require(container.getSlotUnderMouse() == container.getMenu().getSlot(13), "Native item hover missed its themed slot");
        }, true);
        for (int scale : new int[]{2, 3}) {
            add("nightshade-pocket-scale-" + scale, () -> {
                ((SmokePocketScreen) screen).hoverIndex = -1;
                changeScale(scale);
            }, this::verifyPocket, true);
        }
        add("nightshade-pocket-320x240", () -> screen.resize(mc, 320, 240), this::verifyPocket, true);
        add("nightshade-pocket-no-shaders", () -> RenderLibConfig.CLIENT.shaders.set(false), () -> {
            require(!RenderLibShaders.customAvailable(DivinityUiTypes.GALAXY), "Pocket fallback was not selected");
            verifyPocket();
        }, true);
        add("nightshade-pocket-reduced-motion", () -> {
            RenderLibConfig.CLIENT.shaders.set(true);
            mc.options.screenEffectScale().set(0.0);
        }, () -> {
            require(RenderLibShaders.customAvailable(DivinityUiTypes.GALAXY), "Pocket shader was not restored");
            require(RenderLibShaders.forMaterial(DivinityUiTypes.GALAXY).getUniform("UiTime").getFloatBuffer().get(0) == 0,
                    "Reduced motion did not freeze the pocket galaxy");
            verifyPocket();
        }, true);
        add("nightshade-codex-return", () -> {
            RenderLibConfig.CLIENT.shaders.set(true);
            mc.resizeDisplay();
            restoreGods();
            openScreen();
        }, () -> {
            assertReading(LONG_TITLE, 2);
            verifyNightshade();
        }, true);
        addLargeBlessingSteps();
        addShrineSteps();
        addDomainSteps();
        addCommunionSteps();
        if (DOMAIN_ONLY) steps.removeIf(step -> !step.name().startsWith("domain-") && !step.name().startsWith("nightshade-pocket-"));
        if (POCKET_ONLY) steps.removeIf(step -> !step.name().startsWith("nightshade-pocket-"));
        if (OVERFLOW_ONLY) steps.removeIf(step -> !step.name().startsWith("large-blessings-"));
        if (SHRINE_ONLY) steps.removeIf(step -> !step.name().startsWith("shrine-"));
        report.addProperty("plannedScenarios", steps.size());
    }

    private void addCommunionSteps() {
        for (boolean compact : new boolean[]{false, true}) {
            for (String mode : List.of("offer", "choices")) {
                String suffix = mode + (compact ? "-compact" : "-desktop");
                add("communion-" + suffix, () -> {
                    changeScale(2);
                    screen = new CommunionScreen(communionFixture(mode));
                    mc.setScreen(screen);
                    if (compact) screen.resize(mc, 320, 240);
                }, () -> verifyCommunion(mode), true);
            }
        }
    }

    private CompoundTag communionFixture(String mode) {
        var data = new CompoundTag();
        data.putString("God", "divinity_engine:aethon"); data.putString("Name", "The Celestial Expanse");
        data.putString("State", mode.equals("offer") ? "OFFER" : "ACTIVE");
        data.putString("Eligibility", "");
        data.putString("Motif", "void"); data.putInt("Color", 0x8CADCE); data.putInt("Revision", 1);
        // Deliberately omit Session: these views must never authorize a story/return request.
        var stories = new ListTag();
        var story = new CompoundTag();
        story.putString("Id", "divinity_engine:aethon"); story.putString("Title", "Synthetic Celestial Conversation");
        story.putString("Text", "Synthetic UI fixture only. No real story is started or rewarded.\n\n"
                + "Listen to the stars, compare the two paths, and return safely to your body. ".repeat(12) + "COMMUNION-END.");
        story.putBoolean("Compatible", true); story.putString("Stage", "CHOICE");
        story.putString("Name_a", "Constellation"); story.putString("Name_b", "Starlight");
        story.putString("Reward_a", "Synthetic path A upgrade, preview only."); story.putString("Reward_b", "Synthetic path B upgrade, preview only.");
        stories.add(story);
        if (mode.equals("choices")) data.put("Stories", stories);
        return data;
    }

    private void verifyCommunion(String mode) {
        require(screen instanceof CommunionScreen && !screen.isPauseScreen(), "Communion view must remain live/non-pausing");
        String exitKey = mode.equals("offer") ? "gui.divinity_engine.communion.cancel" : "gui.divinity_engine.communion.return";
        require(hasButton(Component.translatable(exitKey).getString()), "Communion safety exit missing");
        if (mode.equals("offer")) {
            var begin = visibleButton(Component.translatable("gui.divinity_engine.communion.begin").getString());
            require(begin.active, "Eligible communion offer is disabled");
        } else require(hasButton("Constellation") && hasButton("Starlight"), "Both branch previews must remain available");
        for (var button : buttons()) if (button.visible)
            require(mc.font.width(button.getMessage()) <= button.getWidth() - (button.getWidth() < 26 ? 0 : 14), "Communion action label is clipped: " + button.getMessage().getString());
        verifyWidgetGeometry();
        scenario.addProperty("detachedCommunionFixture", true); scenario.addProperty("gameplayActionsSubmitted", false);
    }

    private void addDomainSteps() {
        add("domain-empty", () -> { changeScale(3); openDomain(0); }, () -> {
            verifyDomain(); require(uiText("summary").contains("0/8"), "Empty domain missing guidance");
        }, true);
        add("domain-create", () -> press("Build / Size"), () -> {
            verifyDomain(); require(hasButton("Weave chamber") && hasButton("Astral Workshop"), "Room creator missing controls");
            require(screen.children().stream().filter(child -> child instanceof net.nightshade.divinity_engine.client.ui.NightshadeEditBox).count() == 3,
                    "Room dimensions are not editable");
        }, true);
        add("domain-eight-chambers", () -> openDomain(8), () -> {
            verifyDomain(); require(hasButton("Next chambers"), "Extra rooms unreachable");
        }, true);
        add("domain-last-chambers", () -> press("Next chambers"), () -> {
            verifyDomain(); require(hasButton("Astral Arena 8"), "Last chamber unreachable");
        }, true);
        add("domain-resize", () -> press("Resize"), () -> {
            verifyDomain(); require(hasButton("Apply safe resize"), "Selected room resize missing");
        }, true);
        add("domain-portals", () -> press("Portals"), () -> {
            verifyDomain();
            var gateway = net.nightshade.divinity_engine.client.effects.BlessingEffectRenderer.diagnose(
                    "divinity_engine:pocket_dimension", "MARK", 2, 12_000, Long.MAX_VALUE, 1, 1,
                    new net.minecraft.world.phys.Vec3(0, 0, 1), null);
            require(gateway.generatedVertices() > 0, "Anchored portal geometry expired after a server stall");
            var sigils = new java.util.HashSet<Long>();
            for (var link : List.of(new net.minecraft.world.phys.Vec3(1, 0, 0), new net.minecraft.world.phys.Vec3(0, 1, 0), new net.minecraft.world.phys.Vec3(0, -1, 0))) {
                var mesh = net.nightshade.divinity_engine.client.effects.BlessingEffectRenderer.diagnose(
                        "divinity_engine:pocket_dimension", "MARK", 2, 12_000, Long.MAX_VALUE, 1, 1,
                        new net.minecraft.world.phys.Vec3(0, 0, 1), link);
                require(mesh.generatedVertices() > 0 && mesh.rejectedPrimitives() == 0 && mesh.budgetDrops() == 0, "Invalid room gateway sigil");
                sigils.add(mesh.geometryFingerprint());
            }
            require(sigils.size() == 3, "Exit/up/down gateways should have distinct sigils");
            var expired = net.nightshade.divinity_engine.client.effects.BlessingEffectRenderer.diagnose(
                    "divinity_engine:pocket_dimension", "TELEPORT", 2, 12_000, 24, 1, 1,
                    new net.minecraft.world.phys.Vec3(0, 0, 1), null);
            require(expired.generatedVertices() == 0, "One-shot teleport animation stopped expiring");
            for (String label : List.of("Place entrance", "Close entrance", "Room exit open", "Close room exit"))
                require(buttons().stream().anyMatch(button -> button.visible && button.getMessage().getString().equals(label)),
                        "Persistent gateway control missing: " + label);
            require(!hasButton("Close entrance") && hasButton("Close room exit"), "Room exit should exist before manual placement");
            require(uiText("floor_links").equals("Down: floor 4 | Up: floor 6"), "Middle-floor links incorrect");
        }, true);
        add("domain-portals-first-floor", () -> { openDomain(8); press("Portals"); }, () -> {
            verifyDomain(); require(uiText("floor_links").equals("Down: none | Up: floor 2"), "First-floor links incorrect");
        }, true);
        add("domain-portals-top-floor", () -> { for (int i = 0; i < 7; i++) press(">"); }, () -> {
            verifyDomain(); require(uiText("floor_links").equals("Down: floor 7 | Up: none"), "Top-floor links incorrect");
        }, true);
        add("domain-portals-closed-room", () -> {
            var menu = ((net.nightshade.divinity_engine.client.screen.PocketDomainScreen) screen).getMenu();
            menu.rooms().getCompound(7).putBoolean("ExitClosed", true);
            press("Residents"); press("Portals");
        }, () -> {
            verifyDomain(); require(hasButton("Close entrance") == false && hasButton("Close room exit") == false, "Closed exit remained active");
            require(buttons().stream().anyMatch(button -> button.getMessage().getString().equals("Place room exit")), "Closed-room placement control missing");
        }, true);
        add("domain-rules", () -> press("Rules"), () -> {
            verifyDomain(); require(hasButton("Resident revival: ON") && hasButton("Owner building: ON"), "Rules not projected");
        }, true);
        add("domain-residents", () -> press("Residents"), () -> {
            verifyDomain(); require(hasButton("Release"), "Resident controls missing");
            require(uiText("residents").contains("1000") && hasButton("Next residents"), "Uncapped resident list is not paged");
        }, true);
        add("domain-residents-next", () -> press("Next residents"), this::verifyDomain, true);
        add("domain-compact-residents", () -> screen.resize(mc, 320, 240), this::verifyDomain, true);
        add("domain-320x240", () -> { screen.resize(mc, 320, 240); press("Rules"); }, this::verifyDomain, true);
        add("domain-compact-create", () -> { press("Build / Size"); press("New chamber"); }, this::verifyDomain, true);
        add("domain-compact-portals", () -> press("Portals"), this::verifyDomain, true);
        add("domain-no-shaders", () -> RenderLibConfig.CLIENT.shaders.set(false), () -> {
            require(!RenderLibShaders.customAvailable(DivinityUiTypes.GALAXY), "Domain fallback not selected");
            verifyDomain();
        }, true);
    }

    private void openDomain(int count) {
        var snapshot = new CompoundTag();
        var rooms = new net.minecraft.nbt.ListTag();
        var residents = new net.minecraft.nbt.ListTag();
        var styles = net.nightshade.divinity_engine.util.divinity.blessings.aethon.PocketDomainData.Style.values();
        for (int i = 0; i < count; i++) {
            var room = new CompoundTag(); room.putString("Style", styles[i % styles.length].name());
            room.putInt("Stage", 2); room.putInt("Rules", 19); room.putLong("Origin", new net.minecraft.core.BlockPos(8192 + i * 128, 80, 8192).asLong()); rooms.add(room);
        }
        if (count > 0) {
            for (int i = 0; i < 12; i++) {
                var resident = new CompoundTag(); resident.putUUID("Id", UUID.randomUUID());
                resident.putString("Name", "Astral Resident " + (i + 1)); resident.putInt("Revivals", 3); residents.add(resident);
            }
            snapshot.putInt("ResidentCount", 1000); snapshot.putInt("ResidentPages", 84);
        }
        snapshot.put("Rooms", rooms); snapshot.put("Residents", residents);
        var menu = new net.nightshade.divinity_engine.world.inventory.PocketDomainMenu(0, player.getInventory(), snapshot);
        screen = new net.nightshade.divinity_engine.client.screen.PocketDomainScreen(menu, player.getInventory(), Component.literal("Domain"));
        mc.setScreen(screen);
    }

    private void verifyDomain() {
        verifyNightshade();
        var nodes = ((RenderLibContainerScreen<?>) screen).renderlibTree(UiRenderLayer.BACKGROUND).root().children();
        require(nodes.stream().anyMatch(node -> node.type().equals(DivinityUiTypes.GALAXY)), "Domain space background missing");
        require(buttons().stream().allMatch(button -> button instanceof ConstellationButton), "Domain controls lost celestial theme");
        for (var button : buttons()) if (button.visible)
            require(button.getX() >= 0 && button.getY() >= 0 && button.getX() + button.getWidth() <= screen.width
                    && button.getY() + button.getHeight() <= screen.height, "Domain control outside logical screen: " + button.getMessage().getString());
        for (var child : screen.children()) if (child instanceof net.nightshade.divinity_engine.client.ui.NightshadeEditBox box)
            require(box.getX() >= 4 && box.getY() >= 5 && box.getX() + box.getWidth() + 4 <= screen.width
                    && box.getY() + box.getHeight() + 5 <= screen.height, "Dimension field outside logical screen");
    }

    private void addLargeBlessingSteps() {
        add("large-blessings-37", () -> {
            changeScale(2);
            originalGods = PlayerGodsCapability.load(player).serializeNBT().copy();
            var empty = new CompoundTag();
            empty.putBoolean("resetExistingData", true);
            PlayerGodsCapability.load(player).deserializeNBT(empty);
            largeGod = net.nightshade.divinity_engine.registry.divinity.gods.GodsRegistry.SOLARIUS.get().createGodDefaultInstance();
            largeGod.setFavor(250);
            largeGod.getBlessingsInstances().clear();
            for (int i = 1; i <= 37; i++) addSyntheticBlessing(i, i);
            PlayerGodsCapability.load(player).updateGodInstance(largeGod, false);
            openBlessings();
        }, () -> {
            verifyNightshade();
            require(hasButton("Chart Blessing 01") && !hasButton("Chart Blessing 37"), "Large kit was not virtualized");
            require(uiText("blessing_range").contains("37"), "Large blessing count is missing");
            require(buttons().stream().filter(b -> b.visible && b.getMessage().getString().startsWith("Chart Blessing")).count() <= 6,
                    "Large kit created unbounded visible controls");
        }, true);
        add("large-blessings-pages", () -> {
            for (int i = 0; i < 20 && !hasButton("Chart Blessing 37"); i++) press(">");
            press("Chart Blessing 37");
        }, () -> require(uiText("blessing_name").equals("Chart Blessing 37"), "Last blessing is unreachable"), true);
        add("large-blessings-320x240", () -> screen.resize(mc, 320, 240), () -> {
            require(hasButton("Chart Blessing 37"), "Resize hid the selected blessing");
            require(uiText("blessing_name").equals("Chart Blessing 37"), "Resize changed selection");
        }, true);
        add("large-blessings-live-insert", () -> {
            for (int i = 101; i <= 110; i++) addSyntheticBlessing(i, 0);
        }, () -> {
            require(uiText("blessing_range").contains("47"), "Live additions did not update the total");
            require(hasButton("Chart Blessing 37") && uiText("blessing_name").equals("Chart Blessing 37"),
                    "Live additions lost the selected blessing's visibility or identity");
        }, true);
        add("large-blessings-keyboard", () -> {
            press("Chart Blessing 37");
            key(GLFW.GLFW_KEY_HOME);
            require(hasButton("Chart Blessing 101"), "Home did not reach the first blessing row");
            key(GLFW.GLFW_KEY_END);
            require(hasButton("Chart Blessing 37"), "End did not reach the final odd row");
            key(GLFW.GLFW_KEY_PAGE_UP);
            require(!hasButton("Chart Blessing 37"), "Page Up did not page blessings");
            key(GLFW.GLFW_KEY_PAGE_DOWN);
        }, () -> require(hasButton("Chart Blessing 37"), "Page Down did not return to the last blessing"), true);
        add("large-blessings-live-remove", () -> {
            largeGod.getBlessingsInstances().clear();
            for (int i = 1; i <= 3; i++) addSyntheticBlessing(i, i);
        }, () -> {
            require(hasButton("Chart Blessing 01") && hasButton("Chart Blessing 03"), "Shrinking the list left stale offsets");
            require(!hasButton("<") && !hasButton(">"), "Small kits retained unnecessary paging controls");
            require(uiText("blessing_name").equals("Chart Blessing 01"), "Removed selection was not replaced safely");
        }, true);
        add("large-blessings-empty", () -> largeGod.getBlessingsInstances().clear(), () -> {
            require(!uiText("no_blessings").isBlank(), "Empty kit has no friendly state");
            require(!hasButton("Chart Blessing 01") && !hasButton(">"), "Empty kit retained stale controls");
            verifyNightshade();
        }, true);
    }

    private void addBlessingsHeaderSteps() {
        add("blessings-header-setup", () -> {
            originalGods = PlayerGodsCapability.load(player).serializeNBT().copy();
            for (var god : GodHelper.getGods()) {
                var instance = god.createGodDefaultInstance();
                instance.setFavor(250);
                PlayerGodsCapability.load(player).updateGodInstance(instance, false);
            }
            changeScale(2);
            installJournal(null);
            openShrine(net.nightshade.divinity_engine.registry.divinity.gods.GodsRegistry.SOLARIUS.get());
        }, () -> {
            verifyBlessingsHeader();
            require(uiText("bond").startsWith("Waiting"), "Missing relationship sync did not show its waiting state");
            verifySelectedAvailability(false);
            require(!hasButton("Enter Trial"), "Maximum legacy favor bypassed missing relationship sync");
            require(GodHelper.getGods().stream().filter(god -> god.getRegistryName().getNamespace().equals("divinity_engine")).count() == 18,
                    "Expected 18 built-in gods for the UI fixture");
        }, true);
        add("blessings-header-zero-sync-bond", () -> press("Bond"), () -> {
            verifyBlessingsHeader();
            require(journalUiText().contains("Waiting for your relationship"), "Bond guessed a rank before sync");
        }, true);
        add("blessings-header-zero-sync-quests", () -> press("Quests"), () -> {
            verifyBlessingsHeader();
            require(journalUiText().contains("Waiting for"), "Quest view did not tolerate a missing baseline");
        }, true);
        add("blessings-header-legacy-favor-only", () -> {
            var legacy = relationshipFixture(0);
            for (Tag value : legacy.getList("entries", Tag.TAG_COMPOUND)) ((CompoundTag) value).remove("relationship");
            installJournal(legacy);
            press("Powers");
        }, () -> {
            verifyBlessingsHeader();
            verifySelectedAvailability(false);
            require(uiText("bond").startsWith("Waiting") && !hasButton("Enter Trial"),
                    "Legacy favor-only journal enabled a proof-gated action");
        }, true);
        add("blessings-header-contact-begins-story", () -> {
            GodHelper.getGodOrNull(player, net.nightshade.divinity_engine.registry.divinity.gods.GodsRegistry.SOLARIUS.get()).setFavor(0);
            installJournal(relationshipFixture(0));
            press("Quests");
        }, () -> {
            verifyBlessingsHeader();
            require(hasButton("Begin quest"), "Contacted synthetic story still requires numeric favor");
            require(uiText("bond").equals("Bond: Contacted"), "Contact-only bond was not projected");
        }, true);
        for (int proof = 0; proof <= 5; proof++) {
            int earned = proof;
            add("blessings-header-proof-" + proof, () -> {
                installJournal(relationshipFixture(earned));
                press("Powers");
            }, () -> {
                var god = net.nightshade.divinity_engine.registry.divinity.gods.GodsRegistry.SOLARIUS.get();
                for (var blessing : GodHelper.getAllBlessingsInstancesOfGod(player, god)) {
                    selectFixtureBlessing(blessing);
                    verifySelectedAvailability(DivineRelationship.order(blessing) <= earned);
                    verifyRequirementLayout();
                }
                require(hasButton("Enter Trial") == (earned >= 4), "Trial eligibility disagrees with the relationship snapshot");
                verifyBlessingsHeader();
                scenario.addProperty("syntheticProof", earned);
                scenario.addProperty("clientFavor", GodHelper.getGodOrNull(player, god).getFavor());
            }, true);
        }
        add("blessings-header-bond-guidance", () -> press("Bond"), () -> {
            verifyBlessingsHeader();
            require(uiText("bond").equals("Bond: Champion"), "Bond rank did not follow the proof snapshot");
            require(uiText("journal_heading").equals("Divine Bond") && journalUiText().contains("NEXT ACTION"),
                    "Bond lost its heading or next-deed guidance");
            require(((RenderLibContainerScreen<?>) screen).renderlibTree(UiRenderLayer.BACKGROUND).root().children().stream()
                    .noneMatch(node -> node.key().equals("journal_progress")), "Bond still displays a point meter");
        }, true);
        add("blessings-header-bond-memories", () -> {
            for (int i = 0; i < 64 && !journalUiText().contains("synthetic witness"); i++) {
                var line = uiNode("journal_line_0");
                require(screen.mouseScrolled(nodeX(line) + 2, nodeY(line) + 2, -1), "Bond memories cannot be scrolled into view");
            }
        }, () -> {
            verifyBlessingsHeader();
            require(journalUiText().contains("synthetic witness"), "Server-snapshot memories were lost from Bond");
        }, true);
        add("blessings-header-18-god-scroll", () -> {
            changeScale(4);
            openBlessings();
            screen.resize(mc, 320, 240);
        }, () -> {
            var names = GodHelper.getGods().stream().map(god -> Component.translatable(god.getNameTranslationKey()).getString())
                    .collect(java.util.stream.Collectors.toSet());
            var reached = new java.util.HashSet<String>();
            for (int i = 0; i < 64 && !reached.containsAll(names); i++) {
                buttons().stream().filter(b -> b.visible && names.contains(b.getMessage().getString()))
                        .forEach(b -> reached.add(b.getMessage().getString()));
                if (!reached.containsAll(names)) require(screen.mouseScrolled(searchBox().getX() + 2,
                        searchBox().getY() + searchBox().getHeight() + 10, -1), "God rail did not scroll");
            }
            require(reached.containsAll(names), "Scrolling lost registered gods: " + names.stream().filter(name -> !reached.contains(name)).toList());
            for (String elder : List.of("Aureth", "Malgrath", "Vaelith")) {
                searchBox().setValue(elder);
                require(!hasButton(elder), "Removed Elder god appeared in search");
            }
            searchBox().setValue("");
            require(!hasButton("Enter Trial"), "Inventory browsing enabled a statue-only trial");
            scenario.addProperty("godsReachedByScrolling", reached.size());
            verifyBlessingsHeader();
        }, true);
        for (boolean shrine : new boolean[]{false, true}) {
            for (int scale : new int[]{2, 3, 4}) {
                String layout = "blessings-header-" + (shrine ? "statue-" : "galaxy-") + scale;
                add(layout, () -> {
                    changeScale(scale);
                    if (shrine) openShrine(net.nightshade.divinity_engine.registry.divinity.gods.GodsRegistry.AETHON.get());
                    else openBlessings();
                    if (scale == 4) screen.resize(mc, 320, 240);
                }, this::verifyBlessingsHeader, true);
                for (String view : new String[]{"Quests", "Bond", "Powers"}) {
                    add(layout + "-" + view.toLowerCase(java.util.Locale.ROOT), () -> press(view),
                            this::verifyBlessingsHeader, view.equals("Bond"));
                }
            }
        }
        for (var god : List.of(net.nightshade.divinity_engine.registry.divinity.gods.GodsRegistry.SOLARIUS,
                net.nightshade.divinity_engine.registry.divinity.gods.GodsRegistry.IGNAR,
                net.nightshade.divinity_engine.registry.divinity.gods.GodsRegistry.AETHON)) {
            for (int scale : new int[]{2, 4}) {
                String suffix = god.getId().getPath() + "-scale-" + scale;
                add("blessings-header-pantheon-statue-" + suffix, () -> {
                    changeScale(scale); openShrine(god.get());
                    if (scale == 4) screen.resize(mc, 320, 240);
                }, () -> {
                    verifyBlessingsHeader();
                    var bound = ((BlessingsScreen) screen).getMenu().getGod();
                    require(bound == god.get(), "Shrine lost its selected god");
                    for (var other : GodHelper.getGods()) if (other != bound)
                        require(!journalUiText().contains(Component.translatable(other.getNameTranslationKey()).getString()),
                                "Another god's name leaked into the statue view");
                }, true);
            }
        }
        for (int scale : new int[]{2, 4}) {
            add("blessings-header-long-proof-" + scale, () -> {
                var fixture = relationshipFixture(2);
                for (Tag value : fixture.getList("entries", Tag.TAG_COMPOUND)) {
                    var entry = (CompoundTag) value;
                    if (entry.getString("id").equals("divinity_engine:solarius/favor"))
                        entry.getCompound("relationship").getCompound("requirements").putString("divinity_engine:radiant_strike", LONG_PROOF);
                }
                installJournal(fixture); changeScale(scale);
                openShrine(net.nightshade.divinity_engine.registry.divinity.gods.GodsRegistry.SOLARIUS.get());
                if (scale == 4) screen.resize(mc, 320, 240);
            }, () -> {
                verifyBlessingsHeader();
                var selected = visibleButton("Radiant Strike");
                require(selected.getTooltip() != null && normalized(tooltipText(selected)).contains(normalized(LONG_PROOF)),
                        "The full proof was lost from the blessing tooltip");
                require(selected.getTooltip().toCharSequence(mc).size() * 10 + 8 <= screen.height,
                        "The long-proof tooltip exceeds the compact screen height");
                require(mc.font.width(LONG_PROOF) > uiNode("requirement").style().width(), "Long-proof stress text is too short");
                scenario.addProperty("longProofPreservedInTooltip", true);
            }, true);
            add("blessings-header-long-proof-tooltip-" + scale,
                    () -> ((SmokeBlessingsScreen) screen).hoverLabel = "Radiant Strike", this::verifyBlessingsHeader, true);
            add("blessings-header-long-proof-scroll-" + scale, () -> {
                ((SmokeBlessingsScreen) screen).hoverLabel = null;
                var firstLine = uiNode("description_0");
                for (int i = 0; i < 32 && !journalUiText().replaceAll("\\s+", "").contains("PROOF-END"); i++)
                    require(screen.mouseScrolled(nodeX(firstLine) + 2, nodeY(firstLine) + 2, -1), "Proof description did not scroll");
            }, () -> {
                verifyBlessingsHeader();
                require(journalUiText().replaceAll("\\s+", "").contains("PROOF-END"), "Wrapped proof ending is unreachable in compact details");
            }, true);
        }
    }

    private void verifyBlessingsHeader() {
        verifyWidgetGeometry();
        var commune = buttons().stream().filter(b -> b.getMessage().getString().equals("Commune")).findFirst().orElseThrow();
        var bond = visibleButton("Bond");
        require(buttons().stream().noneMatch(b -> b.visible && b.getMessage().getString().equals("Favor")), "Numeric Favor tab returned");
        require(commune.active && commune.getX() + commune.getWidth() < bond.getX()
                && commune.getY() > bond.getY() + bond.getHeight(), "Commune did not stay in the god navigation rail");
        require(mc.font.width(commune.getMessage()) <= commune.getWidth() - 14, "Commune label is clipped");
        require(!POINT_REQUIREMENT.matcher(journalUiText()).find(), "Numeric favor requirements or budgets are still visible");
        verifyRequirementLayout();
        if (((BlessingsScreen) screen).getMenu().isOpenedFromStatue()) verifyShrine();
        else verifyNightshade();
    }

    private void verifySelectedAvailability(boolean available) {
        var slots = buttons().stream().filter(b -> b.visible && b.getMessage().getString().matches("[123]: (Equip|Remove)")).toList();
        require(slots.size() == 3, "Proof fixture must retain exactly three slot controls");
        boolean equipped = slots.stream().anyMatch(b -> b.getMessage().getString().endsWith("Remove"));
        require(slots.stream().anyMatch(b -> b.active && b.getMessage().getString().endsWith("Equip")) == (available && !equipped),
                "Equip availability did not follow the server relationship snapshot");
        if (equipped) require(slots.stream().anyMatch(b -> b.active && b.getMessage().getString().endsWith("Remove")),
                "An equipped blessing cannot be removed while its relationship is syncing");
        if (available) require(uiText("requirement").startsWith("Unlocked"), "Unlocked proof is not explained");
    }

    private void selectFixtureBlessing(BlessingsInstance blessing) {
        var names = GodHelper.getAllBlessingsInstancesOfGod(player, blessing.getBoundGod()).stream()
                .map(value -> Component.translatable(value.getBlessing().getNameTranslationKey()).getString()).toList();
        screen.setFocused(buttons().stream().filter(b -> b.visible && names.contains(b.getMessage().getString())).findFirst().orElseThrow());
        key(GLFW.GLFW_KEY_HOME);
        String name = Component.translatable(blessing.getBlessing().getNameTranslationKey()).getString();
        for (int i = 0; i < 32 && !hasButton(name); i++) key(GLFW.GLFW_KEY_PAGE_DOWN);
        press(name);
    }

    private void verifyRequirementLayout() {
        var nodes = ((RenderLibContainerScreen<?>) screen).renderlibTree(UiRenderLayer.BACKGROUND).root().children();
        var status = nodes.stream().filter(node -> node.key().equals("requirement")).findFirst();
        if (status.isEmpty()) return;
        var line = status.get();
        require(mc.font.width(line.text()) <= line.style().width() && mc.font.lineHeight <= line.style().height(),
                "Single-line blessing status exceeds its fitted row");
        var title = uiNode("blessing_name");
        require(nodeY(line) >= nodeY(title) + mc.font.lineHeight, "Proof status overlaps the blessing title");
        int previousBottom = nodeY(line) + line.style().height();
        int bindingY = nodeY(uiNode("binding_rule"));
        for (var detail : nodes.stream().filter(node -> node.key().matches("description_\\d+")).toList()) {
            require(nodeY(detail) >= previousBottom, "Wrapped proof overlaps its previous line");
            require(mc.font.width(detail.text()) <= detail.style().width(), "Wrapped proof exceeds the description width");
            previousBottom = nodeY(detail) + detail.style().height();
            require(nodeY(detail) + mc.font.lineHeight <= bindingY, "Proof text overlaps the slot divider");
        }
        String titleText = uiText("blessing_name");
        var blessing = visibleButton(titleText);
        require(blessing.getTooltip() != null, "Blessing has no full requirement tooltip");
        require(!POINT_REQUIREMENT.matcher(tooltipText(blessing)).find(), "Tooltip still requires numeric favor");
        scenario.addProperty("proofStatusFitsSingleRow", true);
        scenario.addProperty("wrappedProofDoesNotOverlapSlots", true);
    }

    private AbstractButton visibleButton(String label) {
        return buttons().stream().filter(b -> b.visible && b.getMessage().getString().equals(label)).findFirst()
                .orElseThrow(() -> new IllegalStateException("Visible button missing: " + label));
    }

    private String tooltipText(AbstractWidget widget) {
        if (widget.getTooltip() == null) return "";
        var text = new StringBuilder();
        for (var line : widget.getTooltip().toCharSequence(mc)) {
            line.accept((index, style, codepoint) -> { text.appendCodePoint(codepoint); return true; });
            text.append(' ');
        }
        return text.toString();
    }

    private static String normalized(String text) { return text.replaceAll("\\s+", " ").strip(); }
    private static int nodeX(UiNode node) { return node.properties().get("x").asInteger().orElseThrow(); }
    private static int nodeY(UiNode node) { return node.properties().get("y").asInteger().orElseThrow(); }

    /** Synthetic S2C shape only. No server relationship API or progression mutation is used. */
    private CompoundTag relationshipFixture(int proof) {
        CompoundTag journal = new CompoundTag();
        journal.putLong("revision", 200 + proof);
        ListTag entries = new ListTag(); journal.put("entries", entries);
        for (var god : GodHelper.getGods()) {
            String id = god.getRegistryName().toString();
            CompoundTag bond = entry(id + "/favor", "favor", "Divine Bond", "", "", 0, 0);
            bond.putString("god", id);
            // Deliberately inconsistent legacy values catch UI fallback to points.
            bond.putInt("highest", 250); bond.putInt("PRAYER", 12); bond.putInt("OFFERING", 30);
            var relationship = new CompoundTag();
            relationship.putString("rank", proof >= 5 ? "Champion" : proof >= 3 ? "Devoted" : proof >= 1 ? "Favorable" : "Contacted");
            relationship.putBoolean("trial_unlocked", proof >= 4);
            var unlocked = new ListTag(); var requirements = new CompoundTag();
            for (var blessing : GodHelper.getAllBlessingsInstancesOfGod(player, god)) {
                String blessingId = blessing.getBlessing().getRegistryName().toString();
                if (DivineRelationship.order(blessing) <= proof) unlocked.add(StringTag.valueOf(blessingId));
                requirements.putString(blessingId, DivineRelationship.requirement(blessing));
            }
            relationship.put("unlocked_blessings", unlocked); relationship.put("requirements", requirements);
            relationship.putString("next_action", proof >= 5 ? "Return to the matching statue to claim your ending and permanent upgrade."
                    : "Follow your primary story's next deed at the matching statue. Either branch is valid.");
            var memories = new ListTag(); memories.add(StringTag.valueOf("Remembers your synthetic witness. No actual story progress is awarded."));
            relationship.put("memories", memories); bond.put("relationship", relationship); entries.add(bond);
            var quest = entry(id + "/status", "story_journal", "Synthetic primary story", "Synthetic UI only.", "Contacted", 0, 0);
            quest.putString("god", id); quest.putString("story", id); quest.putString("quest_title", "Synthetic primary story");
            quest.putString("stage", proof == 0 ? "DORMANT" : proof >= 5 ? "RETURN" : "DEED");
            quest.putString("action", proof == 0 ? "request" : proof >= 5 ? "finish" : "status");
            quest.putString("next", "Begin at the matching statue. These controls must not be pressed by the fixture.");
            quest.putString("offering_examples", "Synthetic accepted offering");
            quest.putBoolean("contacted", true); quest.putBoolean("can_start", true); entries.add(quest);
        }
        return journal;
    }

    private void addStoryConversationSteps() {
        for (boolean compact : new boolean[]{false, true}) {
            for (String mode : List.of("choices", "confirmation", "epilogues")) {
                add("shrine-conversation-" + mode + (compact ? "-compact" : "-desktop"), () -> {
                    changeScale(2);
                    openShrine(net.nightshade.divinity_engine.registry.divinity.gods.GodsRegistry.SOLARIUS.get());
                    var parent = (BlessingsScreen) screen;
                    CompoundTag data = new CompoundTag();
                    data.putString("story", "divinity_engine:solarius");
                    data.putString("title", "Dawn's Judgment");
                    data.putString("text", "Divided Testimony\n\nCompare both paths. Previewing does not choose a path; confirmation is permanent.\n\n"
                            + "Judgment: defeat eight undead in daylight.\nBlessing upgrade: Solar Ward lasts fifteen seconds instead of ten.\n\n"
                            + "Sanctuary: contribute sixteen bread.\nBlessing upgrade: Solar Ward can activate below 65% health instead of 50%.\n\n"
                            + "Previewing: Sanctuary. Confirm below to lock this path permanently.");
                    var actions = new net.minecraft.nbt.ListTag();
                    int count = mode.equals("choices") ? 2 : mode.equals("confirmation") ? 3 : 5;
                    String[] labels = {"Preview Judgment", "Preview Sanctuary", "Permanently choose Sanctuary", "Hear The Last Light", "Hear Dawn and Dream"};
                    for (int i = 0; i < count; i++) { var action = new CompoundTag(); action.putString("label", labels[i]); actions.add(action); }
                    data.put("actions", actions);
                    // Detached visual fixtures do not touch the real container or send gameplay requests.
                    screen = new net.nightshade.divinity_engine.client.screen.ShrineStoryScreen(parent, data) {
                        @Override public void tick() {}
                    };
                    mc.setScreen(screen);
                    if (compact) screen.resize(mc, 320, 240);
                }, this::verifyWidgetGeometry, true);
                if (mode.equals("epilogues")) add("shrine-conversation-action-page-" + compact, () -> press(">"),
                        () -> require(buttons().stream().anyMatch(b -> b.getMessage().getString().equals("Hear Dawn and Dream")), "Addon epilogue paging failed"), true);
                else add("shrine-conversation-read-" + mode + compact, () -> screen.mouseScrolled(160, 120, -100),
                        this::verifyWidgetGeometry, true);
            }
        }
    }

    private void addShrineSteps() {
        add("shrine-solarius", () -> {
            restoreGods();
            originalGods = PlayerGodsCapability.load(player).serializeNBT().copy();
            for (var god : GodHelper.getGods()) {
                var instance = god.createGodDefaultInstance();
                instance.setFavor(250);
                PlayerGodsCapability.load(player).updateGodInstance(instance, false);
            }
            changeScale(2);
            openShrine(net.nightshade.divinity_engine.registry.divinity.gods.GodsRegistry.SOLARIUS.get());
        }, this::verifyShrine, true);
        add("shrine-nythea", () -> openShrine(net.nightshade.divinity_engine.registry.divinity.gods.GodsRegistry.NYTHEA.get()), this::verifyShrine, true);
        for (int scale : new int[]{3, 4}) {
            add("shrine-scale-" + scale, () -> changeScale(scale), this::verifyShrine, true);
        }
        add("shrine-320x240", () -> screen.resize(mc, 320, 240), this::verifyShrine, true);
        add("shrine-keyboard", () -> {
            key(GLFW.GLFW_KEY_PAGE_DOWN);
            key(GLFW.GLFW_KEY_PAGE_UP);
            key(GLFW.GLFW_KEY_TAB);
        }, this::verifyShrine, false);
        add("shrine-many-blessings", () -> {
            largeGod = GodHelper.getGodOrNull(player, net.nightshade.divinity_engine.registry.divinity.gods.GodsRegistry.SOLARIUS.get());
            largeGod.getBlessingsInstances().clear();
            for (int i = 1; i <= 37; i++) addSyntheticBlessing(i, i);
            openShrine(largeGod.getBaseGod());
            screen.resize(mc, 320, 240);
        }, this::verifyShrine, true);
        add("shrine-many-blessings-page", () -> key(GLFW.GLFW_KEY_PAGE_DOWN), () -> {
            require(uiText("blessing_range").contains("5-8 / 37"), "Statue blessing list could not advance");
            verifyShrine();
        }, true);
        add("shrine-shader-fallback", () -> RenderLibConfig.CLIENT.shaders.set(false), this::verifyShrine, true);
        add("shrine-quests-awaiting-sync", () -> {
            RenderLibConfig.CLIENT.shaders.set(true); changeScale(2); installJournal(null);
            PlayerGodsCapability.load(player).updateGodInstance(net.nightshade.divinity_engine.registry.divinity.gods.GodsRegistry.SOLARIUS.get().createGodDefaultInstance(), false);
            openShrine(net.nightshade.divinity_engine.registry.divinity.gods.GodsRegistry.SOLARIUS.get()); press("Quests");
        }, () -> { verifyShrine(); require(journalUiText().contains("Waiting for"), "Missing sync placeholder"); }, true);
        add("shrine-quests-selected-god", () -> installJournal(shrineQuestFixture(false)), () -> {
            verifyShrine(); require(journalUiText().contains("Dawn's Judgment"), "Missing selected quest");
            require(!journalUiText().contains("FOREIGN QUEST"), "Other god's quest leaked into shrine");
        }, true);
        add("shrine-quests-small", () -> { changeScale(4); screen.resize(mc, 320, 240); }, this::verifyShrine, true);
        add("shrine-favor-ledger", () -> press("Bond"), () -> {
            verifyShrine(); require(journalUiText().contains("Divine Bond"), "Bond view missing");
        }, true);
        add("shrine-quest-upgrade-earned", () -> {
            installJournal(shrineQuestFixture(true)); changeScale(2); press("Powers"); press("Solar Ward");
        }, () -> {
            verifyShrine(); require(journalUiText().contains("QUEST UPGRADE"), "Earned upgrade hidden in blessing details");
        }, true);
        add("shrine-quests-multiple", () -> {
            var fixture = shrineQuestFixture(false); var entries = fixture.getList("entries", Tag.TAG_COMPOUND);
            var second = entries.getCompound(0).copy(); second.putString("id", "addon:second/status"); second.putString("story", "addon:second"); second.putString("quest_title", "An Addon Pilgrimage"); entries.add(second);
            installJournal(fixture); press("Quests");
        }, () -> { press(">"); verifyShrine(); require(journalUiText().contains("Addon Pilgrimage"), "Cannot browse second story for same god"); }, true);
        add("shrine-quests-return-powers", () -> press("Powers"), this::verifyShrine, true);
    }

    private String journalUiText() {
        return ((RenderLibContainerScreen<?>) screen).renderlibTree(UiRenderLayer.BACKGROUND).root().children().stream()
                .map(net.nightshade.renderlib.ui.UiNode::text).collect(java.util.stream.Collectors.joining(" "));
    }

    private CompoundTag shrineQuestFixture(boolean complete) {
        CompoundTag journal = new CompoundTag(); ListTag entries = new ListTag(); journal.put("entries", entries);
        CompoundTag quest = new CompoundTag(); quest.putString("id", "divinity_engine:solarius/status");
        quest.putString("section", "story_journal"); quest.putString("god", "divinity_engine:solarius"); quest.putString("story", "divinity_engine:solarius");
        quest.putString("quest_title", "Dawn's Judgment"); quest.putString("stage", complete ? "COMPLETE" : "DEED");
        quest.putString("status", complete ? "Complete" : "The Chosen Deed"); quest.putString("next", "Defeat eight undead in daylight.");
        quest.putString("branch", "a"); quest.putString("action", "status"); quest.putBoolean("contacted", true); quest.putBoolean("can_start", true);
        quest.putInt("progress", 3); quest.putInt("target", 8);
        ListTag upgrades = new ListTag(); CompoundTag upgrade = new CompoundTag(); upgrade.putString("id", "divinity_engine:solarius_a");
        upgrade.putString("blessing", "divinity_engine:solar_ward"); upgrade.putString("branch", "a"); upgrade.putString("name", "Judgment");
        upgrade.putString("description", "Solar Ward lasts 15 seconds instead of 10."); upgrade.putBoolean("earned", complete); upgrades.add(upgrade);
        quest.put("upgrades", upgrades); entries.add(quest);
        var foreign = quest.copy(); foreign.putString("id", "divinity_engine:nythea/status"); foreign.putString("god", "divinity_engine:nythea"); foreign.putString("quest_title", "FOREIGN QUEST"); entries.add(foreign);
        for (Tag value : relationshipFixture(complete ? 5 : 2).getList("entries", Tag.TAG_COMPOUND)) {
            var bond = (CompoundTag) value;
            if (bond.getString("id").equals("divinity_engine:solarius/favor")) entries.add(bond.copy());
        }
        return journal;
    }

    private void openShrine(BaseGod god) {
        var buffer = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try {
            // Synthetic S2C opening data only; never places a block or opens a server menu.
            BlessingsMenu.writeOpening(buffer, player.blockPosition(), god);
            var menu = new BlessingsMenu(0, player.getInventory(), buffer);
            screen = new SmokeBlessingsScreen(menu, player.getInventory(), Component.literal("Sanctum"));
            mc.setScreen(screen);
        } finally { buffer.release(); }
    }

    private void verifyShrine() {
        var menu = ((BlessingsScreen) screen).getMenu();
        require(menu.isOpenedFromStatue() && menu.getVisibleGods().equals(List.of(menu.getGod())), "Shrine god scope changed");
        require(screen.children().stream().noneMatch(child -> child instanceof EditBox), "Shrine exposes a god search box");
        require(buttons().stream().allMatch(button -> button instanceof net.nightshade.divinity_engine.client.ui.ShrineUi.StoneButton), "Shrine control bypasses its Nightshade theme");
        for (var god : GodHelper.getGods()) {
            String name = Component.translatable(god.getNameTranslationKey()).getString();
            require(buttons().stream().noneMatch(button -> button.getMessage().getString().equals(name)), "Shrine exposes a god tab: " + name);
        }
        var nodes = ((RenderLibContainerScreen<?>) screen).renderlibTree(UiRenderLayer.BACKGROUND).root().children();
        require(nodes.stream().anyMatch(node -> node.type().equals(DivinityUiTypes.SHRINE_STONE)), "Stone backdrop missing");
        require(nodes.stream().noneMatch(node -> node.type().equals(DivinityUiTypes.GALAXY)), "Galaxy leaked into shrine theme");
        var stone = nodes.stream().filter(node -> node.type().equals(DivinityUiTypes.SHRINE_STONE)).findFirst().orElseThrow();
        var lines = nodes.stream().filter(node -> node.key().startsWith("inscription_")).toList();
        for (var line : lines) require(mc.font.width(line.text()) <= line.style().width() - 4, "Inscription will be ellipsized by Nightshade");
        String inscription = lines.stream().map(net.nightshade.renderlib.ui.UiNode::text).collect(java.util.stream.Collectors.joining(" "));
        String expected = Component.translatable("gui.divinity_engine.blessings.tabs."
                + (stone.style().height() < 280 ? "shrine_bound_short" : "shrine_bound")).getString();
        require(inscription.replaceAll("\\s+", " ").strip().equals(expected), "Shrine inscription lost words while wrapping");
        for (var sigil : nodes.stream().filter(node -> node.type().equals(DivinityUiTypes.SHRINE_SIGIL)).toList())
            require(sigil.properties().get("god").value().equals(menu.getGod().getRegistryName().getPath()), "Wrong shrine sigil");
        require(narration().contains(Component.translatable(menu.getGod().getNameTranslationKey()).getString()), "Shrine lost its bound god on resize");
        verifyWidgetGeometry();
    }

    private void addSyntheticBlessing(int index, int favor) {
        var blessing = new net.nightshade.divinity_engine.divinity.blessing.Blessings(favor, 1, false, true, false, java.awt.Color.WHITE) {
            @Override public net.minecraft.resources.ResourceLocation getRegistryName() {
                // The production menu now orders equal-proof entries by stable ID, not numeric favor.
                return new net.minecraft.resources.ResourceLocation("codex_smoke", "chart_" + (favor == 0 ? "0_" : "1_")
                        + String.format(java.util.Locale.ROOT, "%03d", index));
            }
            @Override public String getNameTranslationKey() { return String.format(java.util.Locale.ROOT, "Chart Blessing %02d", index); }
            @Override public String getDescriptionTranslationKey() { return "Synthetic addon blessing. UI browsing only; never equipped or registered on the server."; }
        };
        // Borrow a real holder for construction, but expose client-only identities to the UI.
        // Forge's frozen registry is never modified and none of these fixtures are equipped.
        var instance = new net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance(
                net.nightshade.divinity_engine.registry.divinity.blessing.BlessingsRegistry.SOLAR_WARD.get()) {
            @Override public net.nightshade.divinity_engine.divinity.blessing.Blessings getBlessing() { return blessing; }
            @Override public net.minecraft.resources.ResourceLocation getBlessingId() { return blessing.getRegistryName(); }
            @Override public int hashCode() { return getBlessingId().hashCode(); }
        };
        instance.setBoundGod(largeGod);
        largeGod.addBlessing(instance);
    }

    private String uiText(String key) {
        return uiNode(key).text();
    }

    private UiNode uiNode(String key) {
        return ((RenderLibContainerScreen<?>) screen).renderlibTree(UiRenderLayer.BACKGROUND).root().children().stream()
                .filter(node -> node.key().equals(key)).findFirst().orElseThrow(() -> new IllegalStateException("UI node missing: " + key));
    }

    private void openBlessings() {
        var menu = new BlessingsMenu(0, player.getInventory(), null);
        screen = new SmokeBlessingsScreen(menu, player.getInventory(), Component.literal("Blessings"));
        mc.setScreen(screen);
    }

    private void uiCallback(Runnable action) {
        if (finished) return;
        if (journalUiDepth > 0) {
            withFixtureJournal(action);
            return;
        }
        try { withFixtureJournal(action); }
        catch (Throwable failure) { fail(failure); }
    }

    private final class SmokeCodexScreen extends DivineCodexScreen {
        private SmokeCodexScreen(DivineCodexMenu menu, net.minecraft.world.entity.player.Inventory inventory, Component title) {
            super(menu, inventory, title);
        }

        @Override
        protected void init() {
            uiCallback(super::init);
        }

        @Override
        protected void containerTick() {
            uiCallback(super::containerTick);
        }

        @Override
        public void render(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            uiCallback(() -> super.render(graphics, mouseX, mouseY, partialTick));
        }
    }

    private final class SmokeBlessingsScreen extends BlessingsScreen {
        private String hoverLabel;

        private SmokeBlessingsScreen(BlessingsMenu menu, net.minecraft.world.entity.player.Inventory inventory, Component title) {
            super(menu, inventory, title);
        }

        @Override
        protected void init() {
            uiCallback(super::init);
        }

        @Override
        protected void containerTick() {
            uiCallback(super::containerTick);
        }

        @Override
        public void render(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            if (hoverLabel != null) {
                var hovered = children().stream().filter(child -> child instanceof AbstractButton)
                        .map(child -> (AbstractButton) child).filter(b -> b.visible && b.getMessage().getString().equals(hoverLabel)).findFirst();
                if (hovered.isPresent()) {
                    mouseX = hovered.get().getX() + hovered.get().getWidth() / 2;
                    mouseY = hovered.get().getY() + hovered.get().getHeight() / 2;
                }
            }
            int renderX = mouseX, renderY = mouseY;
            uiCallback(() -> super.render(graphics, renderX, renderY, partialTick));
        }
    }

    private void verifyNightshade() {
        require(screen instanceof RenderLibContainerScreen<?>, "Screen is not hosted by Nightshade");
        var host = (RenderLibContainerScreen<?>) screen;
        require(!host.renderlibTree(UiRenderLayer.BACKGROUND).root().children().isEmpty(), "Nightshade background is empty");
        require(buttons().stream().allMatch(button -> button instanceof NightshadeButton), "A custom control bypassed Nightshade");
        if (screen instanceof BlessingsScreen) {
            require(host.renderlibTree(UiRenderLayer.BACKGROUND).root().children().stream()
                    .anyMatch(node -> node.type().equals(DivinityUiTypes.GALAXY)), "Blessings galaxy node is missing");
            require(buttons().stream().allMatch(button -> button instanceof ConstellationButton),
                    "Blessings controls bypass the celestial renderer");
        }
    }

    private void verifyDivineSeal(String godId) {
        // Decorative seals are optional in the compact theme, but must never identify a different god.
        var seals = ((RenderLibContainerScreen<?>) screen).renderlibTree(UiRenderLayer.BACKGROUND).root().children().stream()
                .filter(node -> node.type().equals(DivinityUiTypes.DIVINE_SEAL)).toList();
        for (var seal : seals) require(seal.properties().get("god").value().equals(godId), "God tab did not update the divine seal");
    }

    private void verifyPocket() {
        verifyNightshade();
        var pocket = (SmokePocketScreen) screen;
        var nodes = pocket.renderlibTree(UiRenderLayer.BACKGROUND).root().children();
        var galaxy = nodes.stream().filter(node -> node.type().equals(DivinityUiTypes.GALAXY)).findFirst().orElseThrow();
        var slots = nodes.stream().filter(node -> node.type().equals(DivinityUiTypes.CELESTIAL_INVENTORY)).findFirst().orElseThrow();
        require(nodes.stream().anyMatch(node -> node.key().equals("capacity") && node.text().contains("27")), "Vault capacity is missing");
        verifyDivineSeal("aethon");
        require(pocket.getMenu().slots.size() == 63 && slots.properties().get("slot_count").asInteger().orElseThrow() == 63,
                "Pocket theme changed the number of slots");
        int frameX = nodeX(galaxy), frameY = nodeY(galaxy);
        int frameWidth = galaxy.style().width(), frameHeight = galaxy.style().height();
        require(frameX >= 0 && frameY >= 0, "Pocket frame left the screen");
        require(frameWidth > 0 && frameHeight > 0 && frameX + frameWidth <= screen.width && frameY + frameHeight <= screen.height, "Pocket frame exceeds the screen");
        for (var frame : nodes.stream().filter(node -> node.type().equals(DivinityUiTypes.CELESTIAL_FRAME)).toList())
            require(nodeX(frame) == frameX && nodeY(frame) == frameY && frame.style().width() == frameWidth
                    && frame.style().height() == frameHeight, "Pocket galaxy/frame bounds disagree");
        require(!pocket.outside(frameX + 8, frameY + 8), "Header is treated as an outside-click drop target");
        require(!pocket.outside(frameX + 2, frameY + 80), "Side engraving is treated as an outside-click drop target");
        require(pocket.outside(frameX - 1, frameY + 8), "Real outside clicks no longer work");
        require(!pocket.outside(frameX + frameWidth - 1, frameY + frameHeight - 1)
                && pocket.outside(frameX + frameWidth, frameY + frameHeight - 1), "Pocket right/bottom click boundary differs from its panel");
        for (int i = 0; i < 63; i++) {
            var slot = pocket.getMenu().getSlot(i);
            int expectedX, expectedY;
            if (i < 27) {
                int row = i / 9, col = i % 9;
                expectedX = 7 + col * 18;
                expectedY = 11 + row * 18 + (col == 0 || col == 8 ? 2 : 0);
            } else {
                expectedX = 8 + (i - 27) % 9 * 18;
                expectedY = i < 54 ? 73 + (i - 27) / 9 * 18 : 131;
            }
            require(slot.x == expectedX && slot.y == expectedY, "Legacy slot coordinates changed: " + i);
            require(slots.properties().get("slot_x_" + i).asInteger().orElseThrow() == slot.x
                    && slots.properties().get("slot_y_" + i).asInteger().orElseThrow() == slot.y, "Decoration moved away from slot " + i);
            require(pocket.hits(slot.x, slot.y, pocket.getGuiLeft() + slot.x + 8, pocket.getGuiTop() + slot.y + 8),
                    "Native slot hit test failed: " + i);
            require(pocket.getGuiLeft() + slot.x >= frameX && pocket.getGuiTop() + slot.y >= frameY
                    && pocket.getGuiLeft() + slot.x + 16 <= frameX + frameWidth
                    && pocket.getGuiTop() + slot.y + 16 <= frameY + frameHeight, "Slot escaped compact panel: " + i);
        }
    }

    private static final class SmokePocketScreen extends PocketDimensionInvScreen {
        private int hoverIndex = -1;

        private SmokePocketScreen(PocketDimensionInvMenu menu, net.minecraft.world.entity.player.Inventory inventory) {
            super(menu, inventory, Component.literal("Pocket Inventory"));
        }

        private boolean outside(double x, double y) { return hasClickedOutside(x, y, leftPos, topPos, 0); }
        private boolean hits(int x, int y, double mouseX, double mouseY) { return isHovering(x, y, 16, 16, mouseX, mouseY); }

        @Override
        public void render(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            if (hoverIndex >= 0) {
                var slot = menu.getSlot(hoverIndex);
                mouseX = leftPos + slot.x + 8;
                mouseY = topPos + slot.y + 8;
            }
            super.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    private void restoreGods() {
        if (originalGods == null) return;
        var restore = originalGods.copy();
        restore.putBoolean("resetExistingData", true);
        PlayerGodsCapability.load(player).deserializeNBT(restore);
        originalGods = null;
    }

    private void verifyRegisteredPageProjection() {
        var pages = DivineCodexPagesRegistry.getPages();
        int titlesBefore = pages.sizeTitles(), contentsBefore = pages.sizeContents();
        try {
            var addon = new DivineCodexPageDeferredRegister("codex_smoke");
            addon.register("first", () -> new DivineCodexPage(Component.literal("Repeated addon title"),
                    Component.literal("Formatted addon body").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)));
            addon.register("second", () -> new DivineCodexPage(Component.literal("Repeated addon title"),
                    Component.literal("Separate registered page")));
            addon.register("untitled", () -> new DivineCodexPage(Component.empty(), Component.literal("Untitled registered body")));
            addon.register("overflow", () -> new DivineCodexPage(Component.literal("Overflow addon page"),
                    Component.literal("Registered overflow must remain readable. ".repeat(50))));
            addon.registerAll();
            var entries = CodexJournal.registeredPages(pages);
            require(pages.sizeContents() == contentsBefore + 4,
                    "Long registered guide was split into duplicate contents entries");
            var longGuide = entries.get(contentsBefore + 3);
            require(longGuide.text().equals("Registered overflow must remain readable. ".repeat(50)),
                    "Long registered guide lost text");
            require(CodexPagination.wrap(mc.font, longGuide.textComponent(), 118, 110).pageCount() > 1,
                    "Reader did not paginate the full guide for a small layout");
            require(entries.size() == pages.sizeContents(), "A registered built-in/addon page was removed or merged");
            require(entries.stream().map(CodexJournal.Entry::id).distinct().count() == entries.size(), "Registered page IDs collided");
            for (int i = 0; i < entries.size(); i++) {
                var entry = entries.get(i);
                require(entry.section() == CodexJournal.Section.REGISTERED_PAGES, "Page was moved behind a story section");
                require(entry.textComponent().equals(pages.getContent(i)), "Page content, order or formatting was changed");
                require(!entry.title().isBlank(), "Untitled registered page is unreachable");
            }
            require(CodexJournal.read(new CompoundTag(), entries).entries().equals(entries), "Registered pages require narrative sync");
            var styled = entries.get(contentsBefore);
            boolean[] foundStyle = {false};
            CodexPagination.wrap(mc.font, styled.textComponent(), 200, 100).lines().forEach(line ->
                    line.accept((index, style, codepoint) -> {
                        if (style.isBold() && style.getColor() != null
                                && style.getColor().getValue() == ChatFormatting.GOLD.getColor()) foundStyle[0] = true;
                        return true;
                    }));
            require(foundStyle[0], "Registered component formatting was lost during pagination");
            scenario.addProperty("registeredPagesChecked", entries.size());
        } finally {
            // These fixtures only touch the isolated client's registry, never server progress.
            DivineCodexPages.titles.subList(titlesBefore, DivineCodexPages.titles.size()).clear();
            DivineCodexPages.contents.subList(contentsBefore, DivineCodexPages.contents.size()).clear();
        }
    }

    private void add(String name, Runnable action, Runnable verify, boolean screenshot) {
        steps.add(new Step(name, action, () -> true, verify, screenshot, TimeUnit.SECONDS.toNanos(30)));
    }

    private void tick() throws IOException {
        checkDeadline();
        ensureContext();
        if (current == null) {
            current = steps.pollFirst();
            if (current == null) {
                finish(true, "Synthetic UI assertions and screenshot writes completed; inspect PNGs for visual quality");
                return;
            }
            scenario = new JsonObject();
            scenario.addProperty("name", current.name());
            scenario.addProperty("status", "RUNNING");
            scenarios.add(scenario);
            stepStarted = System.nanoTime();
            stepTicks = stepFrames = 0;
            withFixtureJournal(current.action());
            writeReport();
            return;
        }
        if (mc.getOverlay() != null || capturePending || captureInFlight) return;
        if (++stepTicks < 4 || stepFrames < 2 || !current.ready().getAsBoolean()) return;
        require(mc.screen == screen, "Detached Codex screen was replaced during the test");
        verifyJournalOwnership();
        withFixtureJournal(current.verify());
        verifyWidgetGeometry();
        scenario.addProperty("requestedGuiScale", requestedScale);
        scenario.addProperty("actualGuiScale", mc.getWindow().getGuiScale());
        scenario.addProperty("scaleWasClamped", mc.getWindow().getGuiScale() != requestedScale);
        scenario.addProperty("windowGuiWidth", mc.getWindow().getGuiScaledWidth());
        scenario.addProperty("windowGuiHeight", mc.getWindow().getGuiScaledHeight());
        scenario.addProperty("screenWidth", screen.width);
        scenario.addProperty("screenHeight", screen.height);
        scenario.addProperty("narration", narration());
        scenario.addProperty("visibleControlsInBoundsAndNonOverlapping", true);
        if (current.screenshot()) capturePending = true;
        else completeStep();
    }

    private void frame() throws IOException {
        checkDeadline();
        if (mc.getOverlay() != null || current == null) return;
        stepFrames++;
        if (!capturePending || captureInFlight) return;
        ensureContext();
        require(mc.screen == screen, "Cannot capture a replaced Codex screen");
        verifyJournalOwnership();
        capturePending = false;
        captureInFlight = true;
        Step captured = current;
        String filename = String.format(java.util.Locale.ROOT, "%02d-%s.png", scenarios.size(), captured.name());
        Path image = artifacts.resolve("screenshots").resolve(filename);
        scenario.addProperty("screenshotRequested", image.toString());
        Screenshot.grab(artifacts.toFile(), filename, mc.getMainRenderTarget(), message -> mc.execute(() -> {
            if (finished || current != captured) return;
            try {
                checkDeadline();
                ensureContext();
                require(Files.isRegularFile(image) && Files.size(image) > 0,
                        "Screenshot writer failed: " + message.getString());
                scenario.addProperty("screenshot", image.toString());
                scenario.addProperty("screenshotBytes", Files.size(image));
                captureInFlight = false;
                completeStep();
            } catch (Throwable failure) {
                fail(failure);
            }
        }));
    }

    private void checkDeadline() {
        long now = System.nanoTime();
        require(now - started <= RUN_LIMIT, "Eight-minute overall UI smoke deadline exceeded");
        require(current == null || now - stepStarted <= current.timeout(),
                "Phase deadline exceeded: " + (current == null ? "between phases" : current.name()));
    }

    private void ensureContext() {
        require(mc.player == player && mc.level == level && mc.getSingleplayerServer() == scope.server(),
                "Isolated client world/player changed; refusing to continue");
        require(player.isAlive() && !player.isRemoved(), "Client player is no longer available");
        require(player.containerMenu == originalMenu, "A real gameplay menu opened; test will not interact with it");
        require(player.containerMenu.getCarried().isEmpty(), "Carried inventory state changed during UI-only QA");
        require(screen == null ? mc.screen == null : mc.screen == screen,
                "Another screen replaced the detached Codex; test will not interact with it");
        if (journalTouched) verifyJournalOwnership();
    }

    private void verifyJournalOwnership() {
        if (!journalTouched) return;
        Tag actual = player.getPersistentData().get(CodexJournal.DATA_KEY);
        Tag expected = journalUiDepth == 0 ? originalJournal : expectedJournal;
        require(java.util.Objects.equals(expected, actual),
                "Real sync or another client task replaced the synthetic journal; refusing to overwrite it repeatedly");
    }

    /** Lend fixture NBT only on the client thread's synchronous UI stack, never across a tick or packet. */
    private void withFixtureJournal(Runnable action) {
        if (finished) {
            action.run();
            return;
        }
        boolean outermost = journalUiDepth == 0;
        Tag liveJournal = null;
        if (outermost) {
            liveJournal = player.getPersistentData().get(CodexJournal.DATA_KEY);
            require(java.util.Objects.equals(originalJournal, liveJournal),
                    "Real journal changed before a UI fixture callback; refusing to overwrite it");
            journalTouched = true;
            putJournal(expectedJournal == null ? null : expectedJournal.copy());
        } else verifyJournalOwnership();
        journalUiDepth++;
        try {
            action.run();
        } finally {
            journalUiDepth--;
            if (outermost) {
                boolean stillOwned = java.util.Objects.equals(expectedJournal, player.getPersistentData().get(CodexJournal.DATA_KEY));
                // Preserve the exact live replica reference. Never restore over an unexpected writer.
                if (stillOwned) putJournal(liveJournal);
                require(stillOwned, "Real sync or another client task replaced the journal during a UI callback; preserving its data");
            }
        }
    }

    private void putJournal(Tag journal) {
        if (journal == null) player.getPersistentData().remove(CodexJournal.DATA_KEY);
        else player.getPersistentData().put(CodexJournal.DATA_KEY, journal);
    }

    private void completeStep() throws IOException {
        scenario.addProperty("status", "PASS");
        scenario.addProperty("durationMillis", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - stepStarted));
        LOGGER.info("DIVINITY_CODEX_SMOKE PASS {}", current.name());
        writeReport();
        current = null;
    }

    private void changeScale(int scale) {
        requestedScale = scale;
        mc.options.guiScale().set(scale);
        mc.resizeDisplay();
    }

    private void installJournal(CompoundTag journal) {
        verifyJournalOwnership();
        journalTouched = true;
        expectedJournal = journal == null ? null : journal.copy();
        if (journalUiDepth == 0) return;
        if (journal == null) player.getPersistentData().remove(CodexJournal.DATA_KEY);
        else player.getPersistentData().put(CodexJournal.DATA_KEY, journal.copy());
    }

    private void openScreen() {
        // Do not assign player.containerMenu, invoke onClose, or send an open/close menu packet.
        screen = new SmokeCodexScreen(new DivineCodexMenu(0, player.getInventory(), null),
                player.getInventory(), Component.literal("Divine Codex - Synthetic UI Smoke"));
        mc.setScreen(screen);
    }

    private List<AbstractButton> buttons() {
        return screen.children().stream().filter(child -> child instanceof AbstractButton)
                .map(child -> (AbstractButton) child).toList();
    }

    private boolean hasButton(String label) {
        return buttons().stream().anyMatch(button -> button.visible && button.active
                && button.getMessage().getString().equals(label));
    }

    private void press(String label) {
        if (screen instanceof CommunionScreen || screen instanceof net.nightshade.divinity_engine.client.screen.ShrineStoryScreen)
            require(label.equals("<") || label.equals(">"), "Detached conversation fixtures may only navigate locally, never submit gameplay actions");
        AbstractButton button = buttons().stream().filter(candidate -> candidate.visible && candidate.active
                && candidate.getMessage().getString().equals(label)).findFirst()
                .orElseThrow(() -> new IllegalStateException("Visible enabled button missing: " + label));
        screen.setFocused(button);
        button.onPress();
    }

    private EditBox searchBox() {
        return screen.children().stream().filter(child -> child instanceof EditBox).map(child -> (EditBox) child)
                .findFirst().orElseThrow(() -> new IllegalStateException("Journal search widget missing"));
    }

    private void search(String query) {
        press("Contents");
        EditBox box = searchBox();
        require(box.isVisible(), "Search is not visible in contents");
        screen.setFocused(box);
        key(GLFW.GLFW_KEY_END);
        int oldLength = box.getValue().length();
        for (int i = 0; i < oldLength; i++) key(GLFW.GLFW_KEY_BACKSPACE);
        for (int i = 0; i < query.length(); i++) {
            require(screen.charTyped(query.charAt(i), 0), "Search character was not handled");
        }
        require(box.getValue().equals(query), "Keypress-driven search text differs from the requested query");
    }

    private void chapter(CodexJournal.Section section) {
        search("");
        String prefix = "Divine Codex. " + section.title + ".";
        for (int i = 0; i < CodexJournal.Section.values().length && !narration().startsWith(prefix); i++) {
            AbstractButton next = buttons().stream().filter(button -> button.visible && button.active
                    && button.getMessage().getString().equals(">") && tooltipText(button).startsWith("Next chapter"))
                    .findFirst().orElseThrow(() -> new IllegalStateException("Next chapter button missing"));
            screen.setFocused(next);
            next.onPress();
        }
        require(narration().startsWith(prefix), "Chapter navigation failed: " + section.title);
    }

    private void key(int key) {
        require(screen.keyPressed(key, 0, 0), "Journal did not handle key " + key);
    }

    private String narration() {
        return screen.getNarrationMessage().getString();
    }

    private int[] pageNumbers() {
        var matcher = PAGE.matcher(narration());
        require(matcher.find(), "Reader narration is missing page numbers");
        return new int[]{Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2))};
    }

    private void goToPage(int target) {
        int[] pages = pageNumbers();
        require(target >= 1 && target <= pages[1] && Math.abs(target - pages[0]) < 512,
                "Requested pagination traversal exceeds the bounded smoke fixture");
        int direction = target > pages[0] ? GLFW.GLFW_KEY_PAGE_DOWN : GLFW.GLFW_KEY_PAGE_UP;
        for (int i = 0; i < Math.abs(target - pages[0]); i++) key(direction);
        assertReading(LONG_TITLE, target);
    }

    private void assertReading(String title, int page) {
        require(narration().contains(title), "Expected reader title: " + title);
        require(pageNumbers()[0] == page, "Expected text page " + page + "; actual " + pageNumbers()[0]);
    }

    private void assertProgress(int progress) {
        assertReading(STORY_TITLE, 1);
        require(narration().contains("Objective: " + progress + " / 12"), "Live objective progress was not refreshed");
    }

    private void updateProgress(int progress, boolean changeRevision) {
        verifyJournalOwnership();
        CompoundTag update = expectedJournal.copy();
        ListTag entries = update.getList("entries", Tag.TAG_COMPOUND);
        for (int i = 0; i < entries.size(); i++) {
            CompoundTag entry = entries.getCompound(i);
            if (entry.getString("id").equals(STORY_ID)) entry.putInt("progress", progress);
        }
        if (changeRevision) update.putLong("revision", update.getLong("revision") + 1);
        installJournal(update);
    }

    private void verifyWidgetGeometry() {
        List<AbstractWidget> widgets = screen.children().stream().filter(child -> child instanceof AbstractWidget)
                .map(child -> (AbstractWidget) child).filter(widget -> widget.visible).toList();
        for (int i = 0; i < widgets.size(); i++) {
            AbstractWidget a = widgets.get(i);
            require(a.getX() >= 0 && a.getY() >= 0 && a.getWidth() > 0 && a.getHeight() > 0
                            && a.getX() + a.getWidth() <= screen.width && a.getY() + a.getHeight() <= screen.height,
                    "Control outside logical screen: " + a.getMessage().getString());
            for (int j = i + 1; j < widgets.size(); j++) {
                AbstractWidget b = widgets.get(j);
                boolean overlap = a.getX() < b.getX() + b.getWidth() && a.getX() + a.getWidth() > b.getX()
                        && a.getY() < b.getY() + b.getHeight() && a.getY() + a.getHeight() > b.getY();
                require(!overlap, "Overlapping controls: " + a.getMessage().getString() + " / " + b.getMessage().getString());
            }
        }
    }

    private static CompoundTag fixture() {
        CompoundTag journal = new CompoundTag();
        journal.putLong("revision", 100);
        ListTag entries = new ListTag();
        entries.add(entry("codex_smoke:guide", "getting_started", GUIDE_TITLE,
                "This is a synthetic UI guide, not gameplay progress. Browse the sections, search unlocked text, and return through history. No shrine action can be performed here.", "Synthetic guide", 0, 0));
        entries.add(entry("codex_smoke:solarius/contact", "discovered_gods", GOD_TITLE,
                "Solarius is included solely as a player-visible test entry. This fixture does not discover a god on the server or reveal any actual story ending.", "Synthetic discovery", 0, 0));
        entries.add(entry(STORY_ID, "story_journal", STORY_TITLE,
                "CURRENT OBJECTIVE\nA purely simulated counter exercises the journal's live progress display. Nothing is contributed, completed, or awarded on the server.", "Synthetic objective", 2, 12));
        StringBuilder chapter = new StringBuilder("Synthetic chapter marker: codexsmokeemberglass.\n\n");
        for (int i = 1; i <= 48; i++) {
            chapter.append("Passage ").append(i).append(": The witness records a quiet dawn and a page of ordinary observations. ")
                    .append("This invented passage tests Minecraft font wrapping, long headings, paragraph breaks, and page navigation; it is not canonical lore or an earned ending.\n\n");
        }
        chapter.append("Final paragraph marker: SMOKE-END.");
        entries.add(entry("codex_smoke:solarius/chapter/3", "chronicle", LONG_TITLE, chapter.toString(), "Synthetic unlocked chapter", 0, 0));
        entries.add(entry("codex_smoke:solarius/chapter/4", "chronicle", NEXT_TITLE,
                "A second synthetic chapter exists only to exercise previous and next entry navigation.", "Synthetic chapter", 0, 0));
        entries.add(entry("codex_smoke:relationship", "relationships", RELATION_TITLE,
                "This invented relationship note tests section navigation. It changes no relationship, favor, choice or completion state.", "Synthetic note", 0, 0));
        journal.put("entries", entries);
        return journal;
    }

    private static CompoundTag entry(String id, String section, String title, String text, String status, int progress, int target) {
        CompoundTag entry = new CompoundTag();
        entry.putString("id", id);
        entry.putString("section", section);
        entry.putString("title", title);
        entry.putString("text", text);
        entry.putString("status", status);
        entry.putInt("progress", progress);
        entry.putInt("target", target);
        return entry;
    }

    private void fail(Throwable failure) {
        if (finished) return;
        LOGGER.error("DIVINITY_CODEX_SMOKE failure in " + (current == null ? "setup" : current.name()), failure);
        if (scenario != null && current != null) {
            scenario.addProperty("status", "FAIL");
            scenario.addProperty("failure", failure.toString());
        }
        finish(false, failure.toString());
    }

    private void finish(boolean passed, String detail) {
        if (finished) return;
        finished = terminal = true;
        capturePending = captureInFlight = false;
        JsonArray cleanupErrors = new JsonArray();
        boolean journalRestored = !journalTouched;
        boolean optionsRestored = !optionsTouched;
        try {
            restoreGods();
            if (journalTouched) {
                // Fixtures already restore per callback. Leave newer server data intact on failure.
                Tag restored = player.getPersistentData().get(CodexJournal.DATA_KEY);
                journalRestored = originalJournal == null ? restored == null : originalJournal.equals(restored);
                if (!journalRestored) report.addProperty("newerClientJournalPreserved", true);
            }
        } catch (Throwable failure) {
            cleanupErrors.add("Client NBT: " + failure);
        }
        try {
            // setScreen invokes only the Codex's client removed() hook, not its packet-sending onClose().
            if (screen != null && mc.screen == screen) mc.setScreen(null);
        } catch (Throwable failure) {
            cleanupErrors.add("Screen: " + failure);
        }
        try {
            RenderLibConfig.CLIENT.shaders.set(originalShaders);
            mc.options.screenEffectScale().set(originalScreenEffects);
            if (optionsTouched) {
                mc.options.pauseOnLostFocus = originalPauseOnLostFocus;
                mc.options.guiScale().set(originalScale);
                mc.resizeDisplay();
                optionsRestored = mc.options.guiScale().get() == originalScale
                        && mc.options.pauseOnLostFocus == originalPauseOnLostFocus
                        && mc.options.screenEffectScale().get() == originalScreenEffects
                        && RenderLibConfig.CLIENT.shaders.get() == originalShaders;
            }
        } catch (Throwable failure) {
            cleanupErrors.add("Client options: " + failure);
        }
        boolean menuUnchanged = player.containerMenu == originalMenu;
        passed &= journalRestored && optionsRestored && menuUnchanged && cleanupErrors.isEmpty();
        report.addProperty("status", passed ? "PASS" : "FAIL");
        report.addProperty("detail", detail);
        report.addProperty("finishedAt", Instant.now().toString());
        report.addProperty("durationMillis", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started));
        report.addProperty("clientJournalRestored", journalRestored);
        report.addProperty("clientOptionsRestored", optionsRestored);
        report.addProperty("gameplayMenuUnchanged", menuUnchanged);
        report.addProperty("remainingScenarios", steps.size());
        JsonArray notRun = new JsonArray();
        steps.forEach(step -> notRun.add(step.name()));
        report.add("notRun", notRun);
        report.add("cleanupErrors", cleanupErrors);
        boolean exit = autoExit() && mc.getSingleplayerServer() == scope.server() && mc.player == player && mc.level == level;
        report.addProperty("autoExitPerformed", exit);
        try {
            writeReport();
        } catch (Throwable failure) {
            passed = false;
            LOGGER.error("DIVINITY_CODEX_SMOKE could not write final report", failure);
        }
        LOGGER.info("DIVINITY_CODEX_SMOKE {}: {}; artifacts={}", passed ? "PASS" : "FAIL", detail, artifacts);
        if (exit) mc.stop();
    }

    private void writeReport() throws IOException {
        if (artifacts != null) Files.writeString(artifacts.resolve("report.json"),
                new GsonBuilder().setPrettyPrinting().create().toJson(report));
    }

    private static boolean autoExit() {
        return Boolean.getBoolean("divinity_engine.codexSmokeAutoExit");
    }

    private static void require(boolean condition, String detail) {
        if (!condition) throw new IllegalStateException(detail);
    }
}
