package net.nightshade.divinity_engine.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;
import net.nightshade.divinity_engine.api.narrative.DivineStoryDefinition;
import net.nightshade.divinity_engine.block.StatueBlock;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.divinity.gods.BaseGod;
import net.nightshade.divinity_engine.divinity.gods.BaseGodInstance;
import net.nightshade.divinity_engine.network.cap.player.gods.PlayerGodsCapability;
import net.nightshade.divinity_engine.network.cap.player.narrative.DivineNarrativeCapability;
import net.nightshade.divinity_engine.network.cap.player.narrative.DivineNarrativeData;
import net.nightshade.divinity_engine.registry.divinity.blessing.BlessingsRegistry;
import net.nightshade.divinity_engine.registry.divinity.curse.CurseRegistry;
import net.nightshade.divinity_engine.registry.divinity.gods.GodsRegistry;
import net.nightshade.divinity_engine.registry.divinity.trial.GodlyTrialsRegistry;
import net.nightshade.divinity_engine.util.divinity.gods.DivineRelationship;
import net.nightshade.divinity_engine.util.divinity.narrative.DivineNarrativeRegistry;
import net.nightshade.divinity_engine.util.divinity.narrative.NarrativeProgress.Stage;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/** Opt-in relationship contract checks; never starts trials or loads a player's saved world. */
@Mod.EventBusSubscriber(modid = "divinity_engine", bus = Mod.EventBusSubscriber.Bus.MOD)
@PrefixGameTestTemplate(false)
public final class DivineRelationshipGameTests {
    private static final String SUITE = "divinity_relationship";

    private DivineRelationshipGameTests() {}

    @SubscribeEvent
    @SuppressWarnings("deprecation")
    public static void register(RegisterGameTestsEvent event) {
        boolean enabled = Arrays.stream(System.getProperty("forge.enabledGameTestNamespaces", "").split(","))
                .map(String::trim).anyMatch(SUITE::equals);
        if (!enabled) return;
        for (var method : DivineRelationshipGameTests.class.getDeclaredMethods()) {
            if (method.isAnnotationPresent(GameTest.class)) GameTestRegistry.register(method, Set.of("divinity_engine"));
        }
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", batch = SUITE)
    public static void builtinsHaveEighteenGodsAndNinetyDistinctBlessings(GameTestHelper h) {
        var gods = builtins();
        h.assertTrue(gods.size() == 18, "Expected 18 built-in gods, excluding addon namespaces");
        h.assertTrue(BlessingsRegistry.BLESSINGS_REGISTRY.get().getKeys().stream().filter(DivineRelationshipGameTests::builtin).count() == 90,
                "Expected 90 built-in blessing registrations");
        h.assertTrue(CurseRegistry.CURSES_REGISTRY.get().getKeys().stream().filter(DivineRelationshipGameTests::builtin).count() == 18,
                "Expected 18 built-in curse registrations");
        Set<ResourceLocation> blessings = new HashSet<>(), curses = new HashSet<>(), trials = new HashSet<>();
        for (var god : gods) {
            h.assertTrue(DivineRelationship.manages(god), "Built-in god omitted from deed progression: " + god.getRegistryName());
            h.assertTrue(god.getBlessings().size() == 5, "Each god must retain five blessings: " + god.getRegistryName());
            for (var blessing : god.getBlessings()) {
                h.assertTrue(blessings.add(blessing.getId()), "A blessing is bound to two built-in gods: " + blessing.getId());
            }
            var curse = CurseRegistry.CURSES_REGISTRY.get().getKey(god.getCurse());
            h.assertTrue(curse != null && curses.add(curse), "Missing or shared curse for " + god.getRegistryName());
            var trial = GodlyTrialsRegistry.get(god);
            h.assertTrue(trial != null && trial.getGod() == god, "Missing or foreign trial for " + god.getRegistryName());
            h.assertTrue(trials.add(GodlyTrialsRegistry.getId(god)), "Two gods share a trial ID");
        }
        h.assertTrue(blessings.size() == 90 && curses.size() == 18 && trials.size() == 18, "Incomplete or aliased pantheon bindings");
        h.succeed();
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", batch = SUITE)
    public static void freshContactNeverImportsEvenMaximumFavor(GameTestHelper h) {
        try (var player = player(h)) {
            for (var god : builtins()) {
                var instance = attach(player, god, 250);
                DivineRelationship.onContact(player, god);
                var state = relationship(player, god);
                h.assertTrue(state.getBoolean("initialized") && state.getInt("proof") == 0, "New contact imported proofs: " + god.getRegistryName());
                h.assertTrue(state.getList("legacy_unlocks", Tag.TAG_STRING).isEmpty() && !state.getBoolean("legacy_trial"), "New contact imported legacy access");
                h.assertTrue(DivineRelationship.canStartStory(player, god), "Contact must allow the first story request without favor grinding");
                assertUnlocked(h, player, instance, 0);
                h.assertFalse(DivineRelationship.canStartTrial(player, god), "New contact bypassed trial proof");
            }
        }
        h.succeed();
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", batch = SUITE)
    public static void legacyFavorImportsExactlyEarnedBlessingsAtEachBoundary(GameTestHelper h) {
        var god = god("solarius");
        Set<Integer> boundaries = new TreeSet<>(Set.of(0, 250));
        for (var entry : god.getBlessings()) {
            boundaries.add(Math.max(0, entry.get().getNeededFavor() - 1));
            boundaries.add(entry.get().getNeededFavor());
        }
        for (int favor : boundaries) try (var player = player(h)) {
            var instance = attach(player, god, favor);
            DivineRelationship.initialize(player, god, true);
            var imported = strings(relationship(player, god), "legacy_unlocks");
            Set<String> expected = new HashSet<>();
            for (var blessing : instance.getBlessingsInstances()) {
                boolean earned = blessing.getNeededFavor() <= favor;
                if (earned) expected.add(blessing.getBlessingId().toString());
                h.assertTrue(DivineRelationship.canUse(player, blessing) == earned, "Wrong migrated eligibility at favor " + favor + ": " + blessing.getBlessingId());
            }
            h.assertTrue(imported.equals(expected), "Migration imported too many or too few stable IDs at " + favor);
            h.assertTrue(relationship(player, god).getInt("proof") == 0, "Legacy access invented completed story deeds");
        }
        h.succeed();
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", batch = SUITE)
    public static void originalChampionSavesKeepAllBlessingsAndTrialAccess(GameTestHelper h) {
        try (var player = player(h)) {
            int count = 0;
            for (var god : builtins()) {
                var instance = attach(player, god, 250);
                DivineRelationship.initialize(player, god, true);
                h.assertTrue(strings(relationship(player, god), "legacy_unlocks").size() == 5, "Legacy Champion lost a blessing: " + god.getRegistryName());
                h.assertTrue(relationship(player, god).getBoolean("legacy_trial") && DivineRelationship.canStartTrial(player, god), "Legacy Champion lost trial access");
                instance.setFavor(0);
                DivineRelationship.refresh(player, god);
                assertUnlocked(h, player, instance, 5);
                h.assertTrue(DivineRelationship.canStartTrial(player, god), "Imported trial access still requires positive favor");
                count++;
            }
            h.assertTrue(count == 18, "Migration must cover the original 18 gods");
        }
        h.succeed();
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", batch = SUITE)
    public static void importIsOnceOnlyAcrossFavorChangesAndReload(GameTestHelper h) {
        var god = god("solarius");
        try (var player = player(h)) {
            var instance = attach(player, god, 0);
            int firstThreshold = ordered(instance).get(0).getNeededFavor();
            instance.setFavor(firstThreshold);
            DivineRelationship.initialize(player, god, true);
            CompoundTag before = relationship(player, god).copy();
            instance.setFavor(250);
            DivineRelationship.initialize(player, god, true);
            player.narrative.deserializeNBT(player.narrative.serializeNBT());
            DivineRelationship.initialize(player, god, true);
            h.assertTrue(relationship(player, god).equals(before), "Second initialization or reconnect re-imported increased favor");
            assertUnlocked(h, player, instance, 1);
        }
        h.succeed();
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", batch = SUITE)
    public static void initializedProgressIgnoresPositiveFavorChanges(GameTestHelper h) {
        try (var player = player(h)) {
            var god = god("solarius");
            var instance = attach(player, god, 0);
            DivineRelationship.onContact(player, god);
            restore(player, god, Stage.WITNESS, "a", 2);
            DivineRelationship.refresh(player, god);
            var rank = DivineRelationship.rank(player, god);
            for (int favor : List.of(0, 49, 100, 175, 250, 0)) {
                instance.setFavor(favor);
                DivineRelationship.initialize(player, god, true);
                DivineRelationship.refresh(player, god);
                assertUnlocked(h, player, instance, 1);
                h.assertTrue(java.util.Objects.equals(rank, DivineRelationship.rank(player, god)), "Rank changed from favor points instead of deeds");
                h.assertFalse(DivineRelationship.canStartTrial(player, god), "Positive favor bypassed trial proof");
            }
        }
        h.succeed();
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", batch = SUITE)
    public static void everySavedStoryStageMapsToProofWithoutLosingProgress(GameTestHelper h) {
        var god = god("solarius");
        for (Stage stage : Stage.values()) for (String branch : List.of("a", "b")) try (var player = player(h)) {
            h.assertTrue(DivineRelationship.proofFor(stage) == expectedProof(stage), "Public proof mapping disagrees at " + stage);
            var instance = attach(player, god, 0);
            var memory = restore(player, god, stage, branch, 7);
            CompoundTag legacy = player.narrative.serializeNBT();
            legacy.getCompound("gods").getCompound(god.getRegistryName().toString()).remove("relationship");
            player.narrative.deserializeNBT(legacy);
            DivineRelationship.initialize(player, god, true);
            DivineRelationship.refresh(player, god);
            var restored = player.narrative.story(story(god));
            h.assertTrue(restored.story.stage() == stage && restored.story.progress() == 7, "Migration reset stage/counter at " + stage);
            h.assertTrue(restored.story.branch().equals(memory.story.branch()), "Migration reset permanent branch at " + stage);
            h.assertTrue(restored.story.specialization().equals(memory.story.specialization()), "Migration reset permanent reward at " + stage);
            h.assertTrue(relationship(player, god).getInt("proof") == expectedProof(stage), "Wrong proof for old stage " + stage);
            assertUnlocked(h, player, instance, expectedProof(stage));
            h.assertTrue(instance.getFavor() == 0, "State migration must not grant fresh rewards");
        }
        h.succeed();
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", batch = SUITE)
    public static void failedStaleAndDuplicateTrialsCannotAwardFinalProof(GameTestHelper h) {
        try (var player = player(h)) {
            var god = god("solarius");
            var instance = attach(player, god, 0);
            DivineRelationship.onContact(player, god);
            var memory = restore(player, god, Stage.TRIAL, "a", 0);
            memory.story.trialStarted("failed");
            memory.story.trialOutcome("failed", false);
            memory.story.trialOutcome("unrelated", true);
            h.assertFalse(memory.story.trialOutcome("failed", true), "Previously failed outcome can be replayed as victory");
            DivineRelationship.refresh(player, god);
            assertUnlocked(h, player, instance, 4);
            h.assertTrue(DivineRelationship.canStartTrial(player, god), "Failed trial blocks retry");
            memory.story.trialStarted("retry");
            memory.story.trialOutcome("retry", true);
            DivineRelationship.refresh(player, god);
            assertUnlocked(h, player, instance, 5);
            CompoundTag before = relationship(player, god).copy();
            h.assertFalse(memory.story.trialOutcome("retry", true), "Duplicate victory accepted");
            DivineRelationship.refresh(player, god);
            h.assertTrue(relationship(player, god).equals(before), "Duplicate outcome changed proof");
        }
        h.succeed();
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", batch = SUITE)
    public static void proofsAndLegacyUnlocksSurviveNbtAndNeverRegress(GameTestHelper h) {
        try (var player = player(h); var replacement = player(h)) {
            var god = god("solarius");
            attach(player, god, 250);
            DivineRelationship.initialize(player, god, true);
            var memory = restore(player, god, Stage.RECKONING, "b", 3);
            memory.favorLedger.putInt("unrelated_budget", 7);
            DivineRelationship.refresh(player, god);
            CompoundTag before = relationship(player, god).copy();
            var replacementInstance = attach(replacement, god, 0);
            replacement.narrative.deserializeNBT(player.narrative.serializeNBT());
            h.assertTrue(relationship(replacement, god).equals(before), "Relationship NBT was lost on player replacement");
            var restored = replacement.narrative.story(story(god));
            h.assertTrue(restored.story.stage() == Stage.RECKONING && restored.story.progress() == 3 && restored.story.branch().equals("b"), "Story state changed on player replacement");
            h.assertTrue(restored.favorLedger.getInt("unrelated_budget") == 7, "Relationship persistence overwrote existing ledger");
            restore(replacement, god, Stage.WITNESS, "a", 1);
            DivineRelationship.refresh(replacement, god);
            h.assertTrue(relationship(replacement, god).getInt("proof") == 3, "Older snapshot regressed earned proof");
            assertUnlocked(h, replacement, replacementInstance, 5);
            h.assertTrue(relationship(player, god).equals(before), "Replacement and original share mutable relationship data");
        }
        h.succeed();
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", batch = SUITE)
    public static void losingContactPausesAccessAndReconnectKeepsProof(GameTestHelper h) {
        try (var player = player(h)) {
            var god = god("solarius");
            var instance = attach(player, god, 0);
            DivineRelationship.onContact(player, god);
            restore(player, god, Stage.TRIAL, "b", 6);
            DivineRelationship.refresh(player, god);
            CompoundTag before = relationship(player, god).copy();
            // Isolate the access gate from abandonment effects and network callbacks.
            PlayerGodsCapability.load(player).getContactedGods().remove(instance);
            h.assertFalse(DivineRelationship.canStartStory(player, god), "Uncontacted player can request story");
            h.assertFalse(DivineRelationship.canStartTrial(player, god), "Uncontacted player can enter trial");
            assertUnlocked(h, player, instance, 0);
            h.assertTrue(relationship(player, god).equals(before), "Losing contact erased memories");
            PlayerGodsCapability.load(player).updateGodInstance(instance, false);
            DivineRelationship.onContact(player, god);
            assertUnlocked(h, player, instance, 4);
            h.assertTrue(DivineRelationship.canStartTrial(player, god), "Reconnect did not restore trial eligibility");
            h.assertTrue(relationship(player, god).equals(before), "Reconnect reinitialized permanent proofs");
            h.assertTrue(player.narrative.story(story(god)).story.branch().equals("b"), "Reconnect erased branch");
        }
        h.succeed();
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", batch = SUITE)
    public static void negativeStandingSuspendsOriginalBlessingsWithoutErasingProof(GameTestHelper h) {
        try (var player = player(h)) {
            var god = god("solarius");
            var instance = attach(player, god, 0);
            DivineRelationship.onContact(player, god);
            restore(player, god, Stage.RETURN, "a", 0);
            DivineRelationship.refresh(player, god);
            assertUnlocked(h, player, instance, 5);
            instance.setFavor(-1);
            DivineRelationship.refresh(player, god);
            assertUnlocked(h, player, instance, 0);
            h.assertTrue(relationship(player, god).getInt("proof") == 5, "Suspension deleted earned proof");
            instance.setFavor(0);
            DivineRelationship.refresh(player, god);
            assertUnlocked(h, player, instance, 5);
        }
        h.succeed();
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", batch = SUITE)
    public static void secondaryStoryMemoriesCannotCompletePrimaryProofs(GameTestHelper h) {
        try (var player = player(h)) {
            var god = god("solarius");
            var instance = attach(player, god, 0);
            DivineRelationship.onContact(player, god);
            var primary = story(god);
            var extra = primary.withId(ResourceLocation.fromNamespaceAndPath("relationship_test", "side_story"));
            // An orphaned addon save exercises primary routing without mutating the live registry.
            var memory = player.narrative.story(extra);
            memory.story.restore(Stage.COMPLETE, "a", 0, "relationship_test:side_reward", "", Set.of());
            DivineRelationship.refresh(player, god);
            assertUnlocked(h, player, instance, 0);
            restore(player, god, Stage.WITNESS, "a", 0);
            DivineRelationship.refresh(player, god);
            assertUnlocked(h, player, instance, 1);
            h.assertTrue(memory.story.stage() == Stage.COMPLETE, "Primary refresh modified addon story data");
        }
        h.succeed();
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", batch = SUITE)
    public static void proofIsIsolatedBetweenPlayersAndGods(GameTestHelper h) {
        try (var first = player(h); var second = player(h)) {
            var sun = god("solarius");
            var moon = god("nythea");
            var firstSun = attach(first, sun, 0);
            var firstMoon = attach(first, moon, 0);
            var secondSun = attach(second, sun, 250);
            DivineRelationship.onContact(first, sun);
            DivineRelationship.onContact(first, moon);
            DivineRelationship.onContact(second, sun);
            restore(first, sun, Stage.RETURN, "a", 0);
            restore(second, sun, Stage.WITNESS, "b", 0);
            DivineRelationship.refresh(first, sun);
            DivineRelationship.refresh(first, moon);
            DivineRelationship.refresh(second, sun);
            assertUnlocked(h, first, firstSun, 5);
            assertUnlocked(h, first, firstMoon, 0);
            assertUnlocked(h, second, secondSun, 1);
        }
        h.succeed();
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", batch = SUITE)
    public static void repeatedRefreshCannotRegrantEndingOrMutateStory(GameTestHelper h) {
        try (var player = player(h)) {
            var god = god("solarius");
            var instance = attach(player, god, 0);
            DivineRelationship.onContact(player, god);
            var memory = restore(player, god, Stage.COMPLETE, "b", 0);
            DivineRelationship.refresh(player, god);
            CompoundTag before = player.narrative.serializeNBT();
            for (int i = 0; i < 5; i++) {
                DivineRelationship.refresh(player, god);
                DivineRelationship.initialize(player, god, true);
            }
            h.assertTrue(player.narrative.serializeNBT().equals(before), "Repeated refresh mutated persistent story state");
            h.assertTrue(instance.getFavor() == 0, "Relationship refresh granted numeric rewards");
            h.assertFalse(memory.story.finish("relationship_test:duplicate", true), "Relationship refresh reopened an earned ending");
            assertUnlocked(h, player, instance, 5);
        }
        h.succeed();
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", batch = SUITE)
    public static void journalSnapshotMatchesAccessAndDoesNotAliasPersistentState(GameTestHelper h) {
        try (var player = player(h)) {
            var god = god("aethon");
            var instance = attach(player, god, 0);
            DivineRelationship.onContact(player, god);
            restore(player, god, Stage.TRIAL, "a", 0);
            DivineRelationship.refresh(player, god);
            CompoundTag before = relationship(player, god).copy();
            CompoundTag snapshot = DivineRelationship.snapshot(player, god);
            h.assertTrue(snapshot.getString("rank").equals(DivineRelationship.rank(player, god)), "Snapshot shows stale relationship rank");
            h.assertTrue(snapshot.getBoolean("trial_unlocked"), "Snapshot hides earned trial access");
            var ordered = ordered(instance);
            var unlocked = strings(snapshot, "unlocked_blessings");
            h.assertTrue(unlocked.size() == 4, "Snapshot must show exactly four pre-trial blessings");
            for (int i = 0; i < ordered.size(); i++) {
                String id = ordered.get(i).getBlessingId().toString();
                h.assertTrue(unlocked.contains(id) == (i < 4), "Snapshot differs from server eligibility: " + id);
                h.assertFalse(snapshot.getCompound("requirements").getString(id).isBlank(), "Missing deed explanation: " + id);
            }
            h.assertFalse(snapshot.getString("next_action").isBlank(), "Snapshot omits next action");
            snapshot.getList("unlocked_blessings", Tag.TAG_STRING).clear();
            snapshot.getCompound("requirements").putString(ordered.get(0).getBlessingId().toString(), "client mutation");
            h.assertTrue(relationship(player, god).equals(before), "Journal snapshot aliases persistent proof data");
            assertUnlocked(h, player, instance, 4);
            h.assertTrue(strings(DivineRelationship.snapshot(player, god), "unlocked_blessings").size() == 4,
                    "Modifying a previous snapshot damaged subsequent sync");
        }
        h.succeed();
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("divinity_engine", path);
    }

    private static boolean builtin(ResourceLocation id) {
        return id.getNamespace().equals("divinity_engine");
    }

    private static List<BaseGod> builtins() {
        return GodsRegistry.GODS_REGISTRY.get().getValues().stream()
                .filter(god -> builtin(god.getRegistryName()))
                .sorted(Comparator.comparing(god -> god.getRegistryName().toString())).toList();
    }

    private static BaseGod god(String name) {
        var god = GodsRegistry.GODS_REGISTRY.get().getValue(id(name));
        if (god == null) throw new IllegalStateException("Missing built-in god: " + name);
        return god;
    }

    private static DivineStoryDefinition story(BaseGod god) {
        var def = DivineNarrativeRegistry.story(god.getRegistryName());
        if (def == null) throw new IllegalStateException("Missing primary story for " + god.getRegistryName());
        return def;
    }

    private static TestPlayer player(GameTestHelper h) {
        return new TestPlayer(h.getLevel());
    }

    private static BaseGodInstance attach(TestPlayer player, BaseGod god, int favor) {
        var instance = god.createGodDefaultInstance();
        instance.setFavor(favor);
        PlayerGodsCapability.load(player).updateGodInstance(instance, false);
        return instance;
    }

    private static CompoundTag relationship(TestPlayer player, BaseGod god) {
        return player.narrative.get(god.getRegistryName()).relationship;
    }

    private static Set<String> strings(CompoundTag state, String key) {
        Set<String> values = new HashSet<>();
        state.getList(key, Tag.TAG_STRING).forEach(tag -> values.add(tag.getAsString()));
        return values;
    }

    private static DivineNarrativeData.Memory restore(TestPlayer player, BaseGod god, Stage stage, String branch, int progress) {
        var def = story(god);
        var memory = player.narrative.story(def);
        memory.snapshot(def);
        memory.story.expanded(true);
        String chosen = stage.ordinal() >= Stage.DEED.ordinal() ? branch : "";
        memory.story.restore(stage, chosen, progress, stage == Stage.COMPLETE ? def.branches().get(branch).reward().id().toString() : "",
                stage == Stage.TRIAL ? "awaiting-session" : "", Set.of("previous-session"));
        if (!chosen.isEmpty()) memory.branchSnapshot = def.branches().get(chosen);
        return memory;
    }

    private static int expectedProof(Stage stage) {
        return switch (stage) {
            case DORMANT, OFFERING -> 0;
            case WITNESS -> 1;
            case CHOICE, DEED -> 2;
            case RECKONING -> 3;
            case TRIAL -> 4;
            case RETURN, COMPLETE -> 5;
        };
    }

    private static List<BlessingsInstance> ordered(BaseGodInstance instance) {
        return instance.getBlessingsInstances().stream().sorted(Comparator.comparingInt(BlessingsInstance::getNeededFavor)
                .thenComparing(b -> b.getBlessingId().toString())).toList();
    }

    private static void assertUnlocked(GameTestHelper h, TestPlayer player, BaseGodInstance instance, int count) {
        var blessings = ordered(instance);
        h.assertTrue(blessings.size() == 5, "Fixture requires five god-bound blessings");
        for (int i = 0; i < blessings.size(); i++) {
            h.assertTrue(DivineRelationship.canUse(player, blessings.get(i)) == (i < count),
                    "Expected first " + count + " blessings available; wrong eligibility for " + blessings.get(i).getBlessingId());
        }
    }

    private static final class TestPlayer extends FakePlayer implements AutoCloseable {
        final DivineNarrativeData narrative = new DivineNarrativeData();
        private final LazyOptional<DivineNarrativeData> narrativeCap = LazyOptional.of(() -> narrative);

        TestPlayer(ServerLevel level) {
            super(level, new GameProfile(UUID.randomUUID(), "relationship-qa"));
        }

        @Override public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction side) {
            return capability == DivineNarrativeCapability.CAP && narrativeCap != null
                    ? narrativeCap.cast() : super.getCapability(capability, side);
        }

        @Override public void sendSystemMessage(Component message) {}

        @Override public void close() {
            discard();
            narrativeCap.invalidate();
        }
    }
}
