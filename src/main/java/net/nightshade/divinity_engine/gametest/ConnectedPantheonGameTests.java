package net.nightshade.divinity_engine.gametest;

import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;
import net.nightshade.divinity_engine.api.narrative.*;
import net.nightshade.divinity_engine.network.cap.player.gods.PlayerGodsCapability;
import net.nightshade.divinity_engine.network.cap.player.narrative.DivineNarrativeCapability;
import net.nightshade.divinity_engine.network.cap.player.narrative.DivineNarrativeData;
import net.nightshade.divinity_engine.network.events.divinity.narrative.DivineNarrativeEvents;
import net.nightshade.divinity_engine.registry.divinity.gods.GodsRegistry;
import net.nightshade.divinity_engine.util.divinity.narrative.*;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

import static net.nightshade.divinity_engine.util.divinity.narrative.NarrativeProgress.Stage;

@GameTestHolder("divinity_engine")
@PrefixGameTestTemplate(false)
public final class ConnectedPantheonGameTests {
    private static final ResourceLocation SOLARIUS = DivineNarrativeRegistry.id("solarius");

    @GameTest(template = "pocket_dimension")
    public static void expandedSevenChapterFlowBothBranchesAndRewardOnce(GameTestHelper h) {
        DivineNarrativeRegistry.initialize();
        var definitions = DivineNarrativeRegistry.stories().stream().filter(def -> def.chronicle().expanded()).toList();
        h.assertFalse(definitions.isEmpty(), "Bundled seven-chapter chronicles are loaded");
        for (var def : definitions) for (String branch : List.of("a", "b")) {
            TestPlayer player = new TestPlayer(h.getLevel());
            try {
                placeAndContact(h, player, def.godId());
                var memory = request(h, player, def);
                stage(h, memory, Stage.OFFERING);
                h.assertTrue(memory.story.expanded(), "Request pins the expanded route");
                DivineNarrativeManager.onAcceptedOffering(player, DivineNarrativeManager.god(def.godId()));
                stage(h, memory, Stage.WITNESS);
                h.assertFalse(memory.story.confirm(branch, true, true), "Witness cannot be skipped");
                completeObjective(h, player, def, memory, Stage.CHOICE);
                choose(h, player, def, branch);
                stage(h, memory, Stage.DEED);
                h.assertFalse(memory.story.confirm(opposite(branch), true, true), "Choice is permanent");
                completeObjective(h, player, def, memory, Stage.RECKONING);
                completeObjective(h, player, def, memory, Stage.TRIAL);
                UUID stale = UUID.randomUUID(), fresh = UUID.randomUUID();
                DivineNarrativeManager.onTrialOutcome(player, def.godId(), stale, true);
                stage(h, memory, Stage.TRIAL);
                DivineNarrativeManager.onTrialStarted(player, def.godId(), fresh);
                DivineNarrativeManager.onTrialOutcome(player, def.godId(), fresh, true);
                stage(h, memory, Stage.RETURN);
                int victories = memory.victories;
                DivineNarrativeManager.onTrialOutcome(player, def.godId(), fresh, true);
                h.assertTrue(memory.victories == victories, "Duplicate trial callback is idempotent");
                int favor = favor(player, def.godId());
                h.assertTrue(action(player, def.id(), "finish") == 1, "Return grants the chosen reward");
                stage(h, memory, Stage.COMPLETE);
                h.assertTrue(favor(player, def.godId()) == favor + 25, "Exactly one favor reward");
                h.assertTrue(memory.story.specialization().equals(def.branches().get(branch).reward().id().toString()), "Correct branch reward");
                h.assertTrue(action(player, def.id(), "finish") == 0, "Repeated reward is rejected");
                player.narrative.deserializeNBT(player.narrative.serializeNBT());
                h.assertTrue(action(player, def.id(), "finish") == 0 && favor(player, def.godId()) == favor + 25,
                    "Reconnect cannot grant a second reward");
            } finally { dispose(player); }
        }
        h.succeed();
    }

    @GameTest(template = "pocket_dimension")
    public static void addonStoriesShareGodButNotProgress(GameTestHelper h) {
        try (RegistryScope registry = new RegistryScope()) {
            var first = fixture("first", StoryObjectiveType.XP, "", 5);
            var second = fixture("second", StoryObjectiveType.XP, "", 9);
            registry.install(first, second);
            TestPlayer player = new TestPlayer(h.getLevel()), other = new TestPlayer(h.getLevel());
            try {
                placeAndContact(h, player, SOLARIUS); placeAndContact(h, other, SOLARIUS);
                h.assertTrue(DivineNarrativeRegistry.byId(first.id()).equals(first), "Registry lookup uses story ID");
                h.assertTrue(DivineNarrativeRegistry.forGod(SOLARIUS).containsAll(List.of(first, second)), "A god can own several stories");
                h.assertTrue(DivineNarrativeRegistry.story(SOLARIUS).id().equals(SOLARIUS), "Primary compatibility lookup is unchanged");
                var a = request(h, player, first); var b = request(h, player, second);
                request(h, other, first);
                DivineNarrativeManager.onAcceptedOffering(player, DivineNarrativeManager.god(SOLARIUS));
                h.assertTrue(a != b && a != player.narrative.get(SOLARIUS), "Addon memories never alias primary memory");
                DivineNarrativeManager.advance(player, first.id(), 2);
                h.assertTrue(a.story.progress() == 2 && b.story.progress() == 0, "Direct credit is routed by story ID");
                DivineNarrativeEvents.experience(new PlayerXpEvent.XpChange(player, 1)); flush(h);
                h.assertTrue(a.story.progress() == 3 && b.story.progress() == 1, "Events visit both active addon stories");
                h.assertTrue(other.narrative.findStory(first).story.stage() == Stage.OFFERING, "Other player receives no credit");
                h.assertTrue(player.narrative.allMemories().containsAll(List.of(a, b)), "All memories includes addon stories");
                player.narrative.deserializeNBT(player.narrative.serializeNBT());
                h.assertTrue(player.narrative.findStory(first).story.progress() == 3
                    && player.narrative.findStory(second).story.progress() == 1, "Both independent counters survive NBT");
            } finally { dispose(player); dispose(other); }
        }
        h.succeed();
    }

    @GameTest(template = "pocket_dimension")
    public static void everyStageRoundTripsAndLegacyMigratesWithoutReset(GameTestHelper h) {
        for (String branch : List.of("a", "b")) {
            NarrativeProgress seven = new NarrativeProgress();
            h.assertTrue(seven.request(true, true) && seven.acceptOffering(true), "Pure test explicitly requests seven chapters");
            h.assertTrue(seven.stage() == Stage.WITNESS && seven.activeObjective(), "Offering begins witness");
            seven.advance(5, 5, true);
            h.assertTrue(seven.stage() == Stage.CHOICE && !seven.activeObjective(), "Witness reveals testimony");
            h.assertTrue(seven.confirm(branch, true, true), "Both branches are reachable");
            seven.advance(5, 5, true);
            h.assertTrue(seven.stage() == Stage.RECKONING && seven.activeObjective(), "Deed begins reckoning");
            seven.advance(5, 5, true);
            h.assertTrue(seven.stage() == Stage.TRIAL && !seven.activeObjective(), "Reckoning unlocks trial");
            seven.trialStarted("fresh"); seven.trialOutcome("fresh", true);
            h.assertTrue(seven.stage() == Stage.RETURN && seven.finish("connected_test:" + branch, true), "Trial leads to earned return");
            h.assertTrue(seven.stage() == Stage.COMPLETE && !seven.finish("connected_test:" + branch, true), "Pure seven-chapter reward is one-time");
        }
        try (RegistryScope registry = new RegistryScope()) {
            var def = fixture("stages", StoryObjectiveType.XP, "", 5); registry.install(def);
            for (Stage stage : Stage.values()) for (String branch : List.of("a", "b")) {
                DivineNarrativeData data = new DivineNarrativeData(); var memory = data.story(def);
                String chosen = stage.ordinal() >= Stage.DEED.ordinal() ? branch : "";
                memory.snapshot(def); memory.story.expanded(true);
                memory.story.restore(stage, chosen, 2, stage == Stage.COMPLETE ? def.branches().get(branch).reward().id().toString() : "",
                    stage == Stage.TRIAL ? "pending-session" : "", Set.of("old-session"));
                if (!chosen.isEmpty()) memory.branchSnapshot = def.branches().get(branch);
                CompoundTag nbt = data.serializeNBT(); data.deserializeNBT(nbt);
                var restored = data.findStory(def);
                stage(h, restored, stage);
                h.assertTrue(restored.story.expanded() && restored.story.branch().equals(chosen), "Expanded route and choice persist at " + stage);
                h.assertTrue(restored.story.progress() == 2 && restored.story.outcomes().contains("old-session"), "Counters persist at " + stage);
                h.assertTrue(restored.definition(def).chronicle().equals(def.chronicle()), "Chronicle snapshot persists at " + stage);
                h.assertTrue(restored.story.expectedTrial().equals(stage == Stage.TRIAL ? "pending-session" : ""), "Expected trial persists at " + stage);
            }
            TestPlayer player = new TestPlayer(h.getLevel());
            try {
                var primary = fixture("legacy", StoryObjectiveType.XP, "", 5).withId(SOLARIUS); registry.install(primary);
                placeAndContact(h, player, SOLARIUS);
                CompoundTag oldSave = new CompoundTag(), gods = new CompoundTag(), memory = new CompoundTag();
                memory.putString("stage", "DEED"); memory.putString("branch", "b"); memory.putInt("progress", 4);
                gods.put(SOLARIUS.toString(), memory); oldSave.put("gods", gods); oldSave.putInt("version", 1);
                player.narrative.deserializeNBT(oldSave);
                var legacy = player.narrative.findStory(primary);
                h.assertTrue(legacy.story.expanded() && legacy.story.progress() == 4, "Legacy built-in deed maps into the expanded arc without resetting progress");
                DivineNarrativeManager.advance(player, primary.id(), 1);
                stage(h, legacy, Stage.RECKONING);
                for (Stage oldStage : List.of(Stage.DORMANT, Stage.OFFERING, Stage.CHOICE, Stage.DEED, Stage.TRIAL, Stage.RETURN, Stage.COMPLETE)) {
                    memory.putString("stage", oldStage.name());
                    memory.putString("branch", oldStage.ordinal() >= Stage.DEED.ordinal() ? "b" : "");
                    memory.putString("specialization", oldStage == Stage.COMPLETE ? primary.branches().get("b").reward().id().toString() : "");
                    player.narrative.deserializeNBT(oldSave);
                    var migrated = player.narrative.findStory(primary);
                    stage(h, migrated, oldStage);
                    h.assertTrue(migrated.story.progress() == 4, "Legacy counter retained at " + oldStage);
                    if (oldStage != Stage.DORMANT) h.assertTrue(migrated.story.expanded(), "Legacy chapter mapped at " + oldStage);
                    if (oldStage == Stage.COMPLETE) h.assertFalse(migrated.story.finish("duplicate", true), "Completed legacy save cannot receive a second reward");
                }
                NarrativeProgress oldApi = new NarrativeProgress();
                h.assertTrue(oldApi.request(true) && oldApi.acceptOffering(true), "Old request overload is supported");
                h.assertTrue(oldApi.stage() == Stage.CHOICE && !oldApi.expanded(), "Old route skips witness");
                oldApi.confirm("a", true, true); oldApi.advance(5, 5, true);
                h.assertTrue(oldApi.stage() == Stage.TRIAL, "Old route skips reckoning");
            } finally { dispose(player); }
        }
        h.succeed();
    }

    @GameTest(template = "pocket_dimension")
    public static void canceledEventsAndQueuedCreditRespectChapterGeneration(GameTestHelper h) {
        try (RegistryScope registry = new RegistryScope()) {
            var def = fixture("events", StoryObjectiveType.XP, "", 5); registry.install(def);
            TestPlayer player = new TestPlayer(h.getLevel()), other = new TestPlayer(h.getLevel());
            try {
                placeAndContact(h, player, SOLARIUS); placeAndContact(h, other, SOLARIUS);
                for (Stage stage : List.of(Stage.WITNESS, Stage.DEED, Stage.RECKONING)) {
                    var memory = atStage(player, def, stage, "a");
                    var isolated = atStage(other, def, stage, "a");
                    var canceled = new PlayerXpEvent.XpChange(player, 4);
                    DivineNarrativeEvents.experience(canceled); canceled.setCanceled(true);
                    DivineNarrativeEvents.experience(new PlayerXpEvent.XpChange(player, 2)); flush(h);
                    h.assertTrue(memory.story.progress() == 2 && isolated.story.progress() == 0, "Cancellation and player isolation at " + stage);
                }
                var memory = atStage(player, def, Stage.DEED, "a");
                long generation = memory.generation();
                DivineNarrativeEvents.experience(new PlayerXpEvent.XpChange(player, 5));
                DivineNarrativeEvents.experience(new PlayerXpEvent.XpChange(player, 2)); flush(h);
                stage(h, memory, Stage.RECKONING);
                h.assertTrue(memory.story.progress() == 0 && memory.generation() != generation, "Queued old-deed XP cannot count in reckoning");
                for (String interruption : List.of("logout", "death", "disconnect", "reset")) {
                    memory = atStage(player, def, Stage.WITNESS, "");
                    DivineNarrativeEvents.experience(new PlayerXpEvent.XpChange(player, 2));
                    switch (interruption) {
                        case "logout" -> DivineNarrativeEvents.logout(new PlayerEvent.PlayerLoggedOutEvent(player));
                        case "death" -> player.setHealth(0);
                        case "disconnect" -> player.disconnected = true;
                        case "reset" -> player.narrative.adminReset(def.id());
                    }
                    flush(h);
                    h.assertTrue(memory.story.progress() == 0, "Late callback rejected after " + interruption);
                    player.setHealth(player.getMaxHealth()); player.disconnected = false;
                }
            } finally { dispose(player); dispose(other); }
        }
        h.succeed();
    }

    @GameTest(template = "pocket_dimension")
    public static void observationsAreConsecutiveAndAllMemoriesInterrupt(GameTestHelper h) {
        try (RegistryScope registry = new RegistryScope()) {
            var first = fixture("observe_first", StoryObjectiveType.OBSERVE, "water", 3);
            var second = fixture("observe_second", StoryObjectiveType.OBSERVE, "water", 3);
            registry.install(first, second);
            TestPlayer player = new TestPlayer(h.getLevel());
            try {
                placeAndContact(h, player, SOLARIUS); player.underwater = true;
                var a = atStage(player, first, Stage.WITNESS, "");
                var b = atStage(player, second, Stage.WITNESS, "");
                tick(player); tick(player);
                h.assertTrue(a.story.progress() == 2 && b.story.progress() == 2, "Each consecutive observation tick counts once");
                player.underwater = false; tick(player);
                h.assertTrue(a.story.progress() == 0 && b.story.progress() == 0, "Leaving water resets every active observation");
                player.underwater = true; tick(player); tick(player); tick(player);
                stage(h, a, Stage.CHOICE); stage(h, b, Stage.CHOICE);
                choose(h, player, first, "b"); tick(player); tick(player); tick(player);
                stage(h, a, Stage.RECKONING);
                h.assertTrue(a.story.progress() == 0, "Final deed tick cannot also count in reckoning");
                tick(player);
                h.assertTrue(a.story.progress() == 1, "Reckoning begins on a fresh tick");
                for (String interruption : List.of("logout", "dimension", "respawn")) {
                    a = atStage(player, first, Stage.WITNESS, ""); b = atStage(player, second, Stage.RECKONING, "b");
                    tick(player); long ga = a.generation(), gb = b.generation();
                    switch (interruption) {
                        case "logout" -> DivineNarrativeEvents.logout(new PlayerEvent.PlayerLoggedOutEvent(player));
                        case "dimension" -> DivineNarrativeEvents.dimension(new PlayerEvent.PlayerChangedDimensionEvent(player, Level.OVERWORLD, Level.NETHER));
                        case "respawn" -> DivineNarrativeEvents.respawn(new PlayerEvent.PlayerRespawnEvent(player, false));
                    }
                    h.assertTrue(a.story.progress() == 0 && b.story.progress() == 0, "All addon continuous attempts reset on " + interruption);
                    h.assertTrue(a.generation() != ga && b.generation() != gb, "All addon generations invalidate on " + interruption);
                }
                tick(player); player.narrative.deserializeNBT(player.narrative.serializeNBT());
                h.assertTrue(player.narrative.findStory(first).story.progress() == 0
                    && player.narrative.findStory(second).story.progress() == 0, "Reconnect resets partial observations, not chapters");
                stage(h, player.narrative.findStory(second), Stage.RECKONING);
            } finally { dispose(player); }
        }
        h.succeed();
    }

    @GameTest(template = "pocket_dimension")
    public static void canceledDamageDoesNotResetContinuousWitness(GameTestHelper h) {
        try (RegistryScope registry = new RegistryScope()) {
            var def = fixture("drowning", StoryObjectiveType.SUBMERGED, "", 8); registry.install(def);
            TestPlayer player = new TestPlayer(h.getLevel());
            try {
                placeAndContact(h, player, SOLARIUS); player.underwater = true;
                var memory = atStage(player, def, Stage.WITNESS, ""); tick(player); tick(player);
                var canceled = new LivingDamageEvent(player, h.getLevel().damageSources().drown(), 1);
                DivineNarrativeEvents.damage(canceled); canceled.setCanceled(true); flush(h);
                h.assertTrue(memory.story.progress() == 2, "Canceled drowning damage preserves consecutive progress");
                DivineNarrativeEvents.damage(new LivingDamageEvent(player, h.getLevel().damageSources().drown(), 1)); flush(h);
                h.assertTrue(memory.story.progress() == 0 && memory.attemptStart == -1, "Accepted drowning damage resets witness");
            } finally { dispose(player); }
        }
        h.succeed();
    }

    @GameTest(template = "pocket_dimension")
    public static void firelightSnowAndObservationPredicatesUseLoadedSources(GameTestHelper h) {
        TestPlayer player = new TestPlayer(h.getLevel());
        try {
            BlockPos origin = h.absolutePos(new BlockPos(4, 4, 4));
            for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-2, -2, -2), origin.offset(2, 2, 2)))
                h.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            player.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
            BlockPos source = origin.east(); long now = DivineNarrativeManager.now(player);
            h.getLevel().setBlock(source, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false), 3);
            h.assertFalse(observing(player, "firelight", now), "Unlit campfire is not firelight");
            h.getLevel().setBlock(source, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true), 3);
            h.assertFalse(observing(player, "firelight", now + 1), "No rescan before twenty ticks");
            h.assertTrue(observing(player, "firelight", now + 20), "Lit campfire is discovered by next small scan");
            h.getLevel().setBlock(source, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false), 3);
            h.assertFalse(observing(player, "firelight", now + 21), "Extinguishing immediately invalidates cached source");
            h.getLevel().setBlock(source, Blocks.LAVA.defaultBlockState(), 3);
            h.assertTrue(observing(player, "firelight", now + 40), "Loaded lava is firelight");
            h.getLevel().setBlock(source.below(), Blocks.SOUL_SOIL.defaultBlockState(), 3);
            h.getLevel().setBlock(source, Blocks.SOUL_FIRE.defaultBlockState(), 3);
            h.assertTrue(h.getLevel().getBlockState(source).is(net.minecraft.tags.BlockTags.FIRE), "Supported fire fixture remains present: " + h.getLevel().getBlockState(source));
            h.assertTrue(observing(player, "firelight", now + 60), "Loaded fire is firelight");
            h.getLevel().setBlock(source, Blocks.SNOW_BLOCK.defaultBlockState(), 3);
            h.assertTrue(observing(player, "snow", now + 80), "Nearby snow counts even outside a snowy biome");
            h.assertFalse(observing(player, "firelight", now + 80), "Snow cannot act as firelight");
            var level = h.getLevel();
            h.assertTrue(observing(player, "day_sky", now) == (level.isDay() && level.canSeeSky(origin)), "Day requires visible sky");
            h.assertTrue(observing(player, "night_sky", now) == (level.isNight() && level.canSeeSky(origin)), "Night requires visible sky");
            h.assertTrue(observing(player, "forest", now) == level.getBiome(origin).is(BiomeTags.IS_FOREST), "Forest uses biome tags");
            h.assertTrue(observing(player, "rain", now) == level.isRainingAt(origin), "Rain must reach the observer");
            h.assertTrue(observing(player, "thunder", now) == (level.isThundering() && level.isRainingAt(origin)), "Thunder also requires local rain");
            player.underwater = true; h.assertTrue(observing(player, "water", now), "Water requires submersion");
            player.underwater = false; h.assertFalse(observing(player, "water", now), "Dry observer does not qualify");
            player.setPos(player.getX(), -1, player.getZ()); h.assertTrue(observing(player, "underground", now), "Below zero qualifies");
            player.setPos(player.getX(), 0, player.getZ()); h.assertFalse(observing(player, "underground", now), "Zero is not underground");
            player.setPos(player.getX(), 100, player.getZ()); h.assertFalse(observing(player, "high_sky", now), "High sky is strictly above 100");
            player.setPos(player.getX(), 101, player.getZ());
            h.assertTrue(observing(player, "high_sky", now) == level.canSeeSky(player.blockPosition()), "High sky still requires sky exposure");
            player.setPos(29000000, 80, 29000000); BlockPos unloaded = player.blockPosition();
            h.assertFalse(level.hasChunkAt(unloaded), "Remote test chunk begins unloaded");
            for (String condition : List.of("day_sky", "night_sky", "forest", "rain", "thunder", "underground", "high_sky", "water", "snow", "firelight"))
                h.assertFalse(observing(player, condition, now + 100), "Unloaded observation is rejected: " + condition);
            h.assertFalse(level.hasChunkAt(unloaded), "Observation never forces a chunk load");
        } finally { dispose(player); }
        h.succeed();
    }

    @GameTest(template = "pocket_dimension")
    public static void ownedCanceledDuplicateAndForeignKillsDoNotCount(GameTestHelper h) {
        try (RegistryScope registry = new RegistryScope()) {
            var def = fixture("kills", StoryObjectiveType.KILL, "any", 8); registry.install(def);
            TestPlayer player = new TestPlayer(h.getLevel()), other = new TestPlayer(h.getLevel());
            List<Zombie> victims = new ArrayList<>();
            try {
                placeAndContact(h, player, SOLARIUS); placeAndContact(h, other, SOLARIUS);
                var memory = atStage(player, def, Stage.WITNESS, ""); var isolated = atStage(other, def, Stage.WITNESS, "");
                Zombie owned = new OwnedZombie(h.getLevel(), player.getUUID()); owned.setHealth(0); victims.add(owned);
                DivineNarrativeEvents.death(new LivingDeathEvent(owned, h.getLevel().damageSources().playerAttack(player)));
                Zombie canceled = deadZombie(h, victims);
                var canceledEvent = new LivingDeathEvent(canceled, h.getLevel().damageSources().playerAttack(player));
                DivineNarrativeEvents.death(canceledEvent); canceledEvent.setCanceled(true);
                Zombie accepted = deadZombie(h, victims);
                DivineNarrativeEvents.death(new LivingDeathEvent(accepted, h.getLevel().damageSources().playerAttack(player)));
                DivineNarrativeEvents.death(new LivingDeathEvent(accepted, h.getLevel().damageSources().playerAttack(player)));
                Zombie foreign = deadZombie(h, victims);
                DivineNarrativeEvents.death(new LivingDeathEvent(foreign, h.getLevel().damageSources().playerAttack(other)));
                flush(h);
                h.assertTrue(memory.story.progress() == 1 && isolated.story.progress() == 1, "Only each player's unique uncanceled unowned kill counts");
                var trialLevel = h.getLevel().getServer().getLevel(net.minecraft.resources.ResourceKey.create(
                    net.minecraft.core.registries.Registries.DIMENSION, DivineNarrativeRegistry.id("godly_trials")));
                h.assertTrue(trialLevel != null, "Trial dimension exists");
                Zombie trialMob = new Zombie(EntityType.ZOMBIE, trialLevel); trialMob.setHealth(0); victims.add(trialMob);
                DivineNarrativeEvents.death(new LivingDeathEvent(trialMob, h.getLevel().damageSources().playerAttack(player))); flush(h);
                h.assertTrue(memory.story.progress() == 1, "Trial mobs are excluded");
            } finally { victims.forEach(Zombie::discard); dispose(player); dispose(other); }
        }
        h.succeed();
    }

    @GameTest(template = "pocket_dimension")
    public static void definitionPinningSurvivesReloadAndMissingHandlers(GameTestHelper h) {
        try (RegistryScope registry = new RegistryScope()) {
            var original = fixture("pinned", StoryObjectiveType.XP, "", 5); registry.install(original);
            TestPlayer player = new TestPlayer(h.getLevel());
            try {
                placeAndContact(h, player, SOLARIUS); var memory = request(h, player, original);
                DivineNarrativeManager.onAcceptedOffering(player, DivineNarrativeManager.god(SOLARIUS));
                var replacement = fixture("replacement", StoryObjectiveType.XP, "", 77).withId(original.id()); registry.install(replacement);
                h.assertTrue(DivineNarrativeManager.path(replacement, memory).target() == 5, "Reload cannot change an accepted witness target");
                h.assertTrue(memory.definition(replacement).title().equals(original.title()), "Accepted prose is pinned too");
                player.narrative.deserializeNBT(player.narrative.serializeNBT()); memory = player.narrative.findStory(original);
                h.assertTrue(DivineNarrativeManager.path(replacement, memory).target() == 5, "Whole-story snapshot survives reconnect");
                completeObjective(h, player, original, memory, Stage.CHOICE); choose(h, player, original, "b");
                h.assertTrue(DivineNarrativeManager.path(replacement, memory).reward().id().equals(original.branches().get("b").reward().id()), "Reload cannot replace the promised reward");
                completeObjective(h, player, original, memory, Stage.RECKONING);
                h.assertTrue(DivineNarrativeManager.path(replacement, memory).target() == 5, "Reckoning is pinned before it becomes active");
                CompoundTag saved = player.narrative.serializeNBT();
                var entry = saved.getCompound("stories").getCompound(original.id().toString());
                var json = JsonParser.parseString(entry.getString("story_snapshot")).getAsJsonObject();
                ResourceLocation missing = new ResourceLocation("connected_test", "missing_" + UUID.randomUUID());
                json.getAsJsonObject("chronicle").getAsJsonObject("reckoning").addProperty("objective", missing.toString());
                String raw = json.toString(); entry.putString("story_snapshot", raw);
                player.narrative.deserializeNBT(saved); memory = player.narrative.findStory(original);
                h.assertTrue(memory.definition(replacement) == null && DivineNarrativeManager.path(replacement, memory) == null,
                    "Missing handler pauses instead of substituting new definitions");
                DivineNarrativeManager.advance(player, original.id(), 100);
                stage(h, memory, Stage.RECKONING);
                h.assertTrue(memory.story.progress() == 0, "Unavailable handler cannot receive credit");
                h.assertTrue(player.narrative.serializeNBT().getCompound("stories").getCompound(original.id().toString())
                    .getString("story_snapshot").equals(raw), "Missing handler preserves exact snapshot JSON");
                StoryObjectiveType.register(new StoryObjectiveType(missing, false, 0));
                h.assertTrue(DivineNarrativeManager.path(replacement, memory).objective().id().equals(missing), "Returning handler restores the saved chapter");
            } finally { dispose(player); }
        }
        h.succeed();
    }

    @GameTest(template = "pocket_dimension")
    public static void journalGatesUnreachedAndOppositeBranchPassages(GameTestHelper h) {
        try (RegistryScope registry = new RegistryScope()) {
            var def = fixture("journal", StoryObjectiveType.XP, "", 5); registry.install(def);
            for (String branch : List.of("a", "b")) {
                TestPlayer player = new TestPlayer(h.getLevel());
                try {
                    h.assertFalse(journalText(h, player).contains(marker(def, "ending_" + branch)), "Undiscovered ending is hidden");
                    placeAndContact(h, player, SOLARIUS); var memory = request(h, player, def);
                    String text = journalText(h, player);
                    h.assertTrue(text.contains(marker(def, "call")), "Requested call is readable");
                    h.assertFalse(text.contains(marker(def, "testimony")), "Future testimony is hidden");
                    DivineNarrativeManager.onAcceptedOffering(player, DivineNarrativeManager.god(SOLARIUS));
                    h.assertTrue(journalText(h, player).contains(marker(def, "witness")), "Active witness is readable");
                    completeObjective(h, player, def, memory, Stage.CHOICE);
                    h.assertTrue(journalText(h, player).contains(marker(def, "testimony")), "Reached testimony is readable");
                    choose(h, player, def, branch);
                    text = journalText(h, player);
                    h.assertTrue(text.contains(marker(def, "deed_" + branch)), "Chosen deed is readable");
                    h.assertFalse(text.contains(marker(def, "deed_" + opposite(branch))), "Opposite deed is hidden");
                    h.assertFalse(text.contains(marker(def, "reckoning_" + branch)), "Future reckoning is hidden");
                    completeObjective(h, player, def, memory, Stage.RECKONING);
                    h.assertTrue(journalText(h, player).contains(marker(def, "reckoning_" + branch)), "Reached reckoning is readable");
                    completeObjective(h, player, def, memory, Stage.TRIAL);
                    h.assertTrue(journalText(h, player).contains(marker(def, "trial")), "Reached trial is readable");
                    UUID session = UUID.randomUUID(); DivineNarrativeManager.onTrialStarted(player, SOLARIUS, session);
                    DivineNarrativeManager.onTrialOutcome(player, SOLARIUS, session, true);
                    h.assertFalse(journalText(h, player).contains(marker(def, "ending_" + branch)), "Victory alone does not reveal ending before statue return");
                    h.assertTrue(action(player, def.id(), "finish") == 1, "Ending claimed at matching statue");
                    text = journalText(h, player);
                    h.assertTrue(text.contains(marker(def, "ending_" + branch)), "Earned ending is readable");
                    h.assertFalse(text.contains(marker(def, "ending_" + opposite(branch))), "Other ending stays hidden after completion");
                } finally { dispose(player); }
            }
        }
        h.succeed();
    }

    @GameTest(template = "pocket_dimension")
    public static void crossoversRequireTwoEndingsAndMatchAllFourChoices(GameTestHelper h) {
        DivineNarrativeRegistry.initialize();
        var relations = List.copyOf(DivineNarrativeRegistry.relationships());
        h.assertFalse(relations.isEmpty(), "Bundled crossover relationships are loaded");
        for (var relation : relations) for (String combination : List.of("aa", "ab", "ba", "bb")) {
            TestPlayer player = new TestPlayer(h.getLevel()), other = new TestPlayer(h.getLevel());
            try {
                var first = DivineNarrativeRegistry.byId(relation.stories().get(0));
                var second = DivineNarrativeRegistry.byId(relation.stories().get(1));
                h.assertTrue(first != null && second != null, "Both crossover story prerequisites resolve");
                placeAndContact(h, player, first.godId());
                h.assertTrue(action(player, relation.id(), "crossover") == 0, "No endings cannot claim crossover");
                placeAndContact(h, player, second.godId());
                h.assertFalse(journalText(h, player).contains(relation.summary()), "Contact alone cannot reveal the historical summary");
                placeAndContact(h, player, first.godId());
                completed(player, first, combination.substring(0, 1));
                h.assertTrue(action(player, relation.id(), "crossover") == 0, "One ending is insufficient");
                completed(player, second, combination.substring(1, 2));
                for (var ending : relation.epilogues().values())
                    h.assertFalse(journalText(h, player).contains(ending.get(0)), "Unclaimed crossover prose is hidden");
                player.setPos(player.getX() + 32, player.getY(), player.getZ());
                h.assertTrue(action(player, relation.id(), "crossover") == 0, "Both endings still require a nearby matching statue");
                ResourceLocation unrelated = DivineNarrativeRegistry.stories().stream().map(DivineStoryDefinition::godId)
                    .filter(god -> !relation.gods().contains(god)).findFirst().orElseThrow();
                placeAndContact(h, player, unrelated);
                h.assertTrue(action(player, relation.id(), "crossover") == 0, "Unrelated statue cannot claim epilogue");
                placeAndContact(h, player, combination.charAt(0) == 'a' ? first.godId() : second.godId());
                int favor = favor(player, combination.charAt(0) == 'a' ? first.godId() : second.godId());
                h.assertTrue(action(player, relation.id(), "crossover") == 1, "Either related statue can claim " + combination);
                String expected = String.join("\n\n", relation.epilogues().get(combination));
                h.assertTrue(expected.equals(player.narrative.epilogues.get(relation.id().toString())), "Saved epilogue matches " + combination);
                String text = journalText(h, player);
                h.assertTrue(text.contains(relation.epilogues().get(combination).get(0)), "Claimed epilogue is readable");
                for (var entry : relation.epilogues().entrySet()) if (!entry.getKey().equals(combination))
                    h.assertFalse(text.contains(entry.getValue().get(0)), "Other crossover outcomes stay hidden");
                h.assertTrue(action(player, relation.id(), "crossover") == 0, "Repeated epilogue claim is rejected");
                h.assertTrue(favor(player, combination.charAt(0) == 'a' ? first.godId() : second.godId()) == favor,
                    "Optional lore does not grant another mechanical reward");
                h.assertTrue(other.narrative.epilogues.isEmpty(), "Crossover remains player-specific");
                player.narrative.deserializeNBT(player.narrative.serializeNBT());
                h.assertTrue(expected.equals(player.narrative.epilogues.get(relation.id().toString())), "Epilogue survives reconnect");
                h.assertTrue(action(player, relation.id(), "crossover") == 0, "Reconnect cannot reclaim epilogue");
            } finally { dispose(player); dispose(other); }
        }
        h.succeed();
    }

    private static DivineStoryDefinition fixture(String name, StoryObjectiveType type, String condition, int target) {
        ResourceLocation id = new ResourceLocation("connected_test", name);
        var chronicle = StoryChronicle.builder().premise(name + " premise")
            .witness(StoryObjective.of(type, condition, target, name + " witness objective"))
            .reckoning(StoryObjective.of(type, condition, target, name + " reckoning objective"));
        for (String key : StoryChronicle.REQUIRED) chronicle.chapter(new StoryChapterDefinition(key,
            NarrativeDefinitionJson.chapterTitle(key), List.of("PASSAGE[" + id + "/" + key + "]"), name + " " + key));
        var builder = DivineStoryDefinition.builder(SOLARIUS).id(id).title(name).request(name + " request").chronicle(chronicle.build());
        for (String branch : List.of("a", "b")) builder.branch(branch, new DivineStoryDefinition.Branch(name + " " + branch,
            type, condition, target, null, name + " deed " + branch,
            BlessingSpecialization.builder(new ResourceLocation("connected_test", name + "_" + branch))
                .blessing(DivineNarrativeRegistry.id("solar_ward")).description(name + " reward " + branch).build(), name + " ending " + branch));
        return builder.build();
    }
    private static String marker(DivineStoryDefinition def, String chapter) { return def.chronicle().passages().get(chapter).paragraphs().get(0); }
    private static String opposite(String branch) { return branch.equals("a") ? "b" : "a"; }
    private static int action(TestPlayer player, ResourceLocation id, String action) { return DivineNarrativeManager.action(player, id, action, "", ""); }
    private static void stage(GameTestHelper h, DivineNarrativeData.Memory memory, Stage expected) {
        h.assertTrue(memory != null && memory.story.stage() == expected, "Expected chapter " + expected + ", got " + (memory == null ? "missing" : memory.story.stage()));
    }
    private static DivineNarrativeData.Memory request(GameTestHelper h, TestPlayer player, DivineStoryDefinition def) {
        h.assertTrue(action(player, def.id(), "request") == 1, "Request accepted for " + def.id());
        return player.narrative.findStory(def);
    }
    private static void choose(GameTestHelper h, TestPlayer player, DivineStoryDefinition def, String branch) {
        h.assertTrue(DivineNarrativeManager.action(player, def.id(), "preview", branch, "") == 1, "Preview available");
        var memory = player.narrative.findStory(def);
        h.assertTrue(DivineNarrativeManager.action(player, def.id(), "confirm", branch, memory.preview.substring(0, 36)) == 1, "Preview token confirms choice");
    }
    private static void completeObjective(GameTestHelper h, TestPlayer player, DivineStoryDefinition def, DivineNarrativeData.Memory memory, Stage next) {
        var path = DivineNarrativeManager.path(def, memory);
        h.assertTrue(memory.story.activeObjective() && path != null, "Chapter has an active objective");
        DivineNarrativeManager.advance(player, def.id(), path.target()); stage(h, memory, next);
    }
    private static DivineNarrativeData.Memory atStage(TestPlayer player, DivineStoryDefinition def, Stage stage, String branch) {
        var memory = player.narrative.story(def); memory.interrupt(); memory.snapshot(def); memory.story.expanded(true);
        memory.story.restore(stage, branch, 0, "", "", Set.of());
        memory.branchSnapshot = branch.isEmpty() ? null : def.branches().get(branch);
        var path = DivineNarrativeManager.path(def, memory);
        memory.resettableAttempt = path != null && (path.objective().continuous() || path.objective().timeLimitTicks() > 0);
        return memory;
    }
    private static void completed(TestPlayer player, DivineStoryDefinition def, String branch) {
        var memory = atStage(player, def, Stage.RETURN, branch);
        memory.story.finish(def.branches().get(branch).reward().id().toString(), true);
    }
    private static int favor(TestPlayer player, ResourceLocation id) {
        return PlayerGodsCapability.load(player).getContactedGod(DivineNarrativeManager.god(id)).orElseThrow().getFavor();
    }
    private static void placeAndContact(GameTestHelper h, TestPlayer player, ResourceLocation id) {
        var god = GodsRegistry.GODS_REGISTRY.get().getValue(id);
        var instance = god.createGodDefaultInstance(); instance.setFavor(100);
        PlayerGodsCapability.load(player).updateGodInstance(instance, false);
        BlockPos shrine = h.absolutePos(new BlockPos(2, 2, 2));
        var block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(id.getNamespace(), id.getPath() + "_statue"));
        h.getLevel().setBlock(shrine, block.defaultBlockState(), 3);
        player.setPos(shrine.getX() + 1.5, shrine.getY(), shrine.getZ() + 0.5);
    }
    private static String journalText(GameTestHelper h, TestPlayer player) {
        CompoundTag journal = NarrativeJournal.build(player); StringBuilder text = new StringBuilder();
        for (Tag entry : journal.getList("entries", Tag.TAG_COMPOUND)) {
            CompoundTag row = (CompoundTag) entry;
            for (String field : List.of("id", "section", "title", "text", "status"))
                h.assertTrue(row.contains(field, Tag.TAG_STRING), "Journal entry has string " + field);
            h.assertTrue(row.contains("progress", Tag.TAG_INT) && row.contains("target", Tag.TAG_INT), "Journal counters use compact integer fields");
            text.append(row.getString("text")).append('\n');
        }
        return text.toString();
    }
    private static void flush(GameTestHelper h) {
        DivineNarrativeEvents.serverTick(new TickEvent.ServerTickEvent(TickEvent.Phase.END, () -> true, h.getLevel().getServer()));
    }
    private static void tick(TestPlayer player) {
        player.tickCount++;
        invokeEventHelper("tickPlayer", new Class<?>[] {ServerPlayer.class}, player);
    }
    private static boolean observing(TestPlayer player, String condition, long now) {
        return (boolean) invokeEventHelper("observing", new Class<?>[] {ServerPlayer.class, String.class, long.class}, player, condition, now);
    }
    private static Object invokeEventHelper(String name, Class<?>[] parameters, Object... args) {
        // Exercise the real tick adapter without registering fake players in the live server player list.
        try {
            var method = DivineNarrativeEvents.class.getDeclaredMethod(name, parameters); method.setAccessible(true);
            return method.invoke(null, args);
        } catch (InvocationTargetException error) { throw new AssertionError("Event helper failed: " + name, error.getCause()); }
        catch (ReflectiveOperationException error) { throw new AssertionError("Missing event helper: " + name, error); }
    }
    private static Zombie deadZombie(GameTestHelper h, List<Zombie> victims) {
        Zombie victim = new Zombie(EntityType.ZOMBIE, h.getLevel()); victim.setHealth(0); victims.add(victim); return victim;
    }
    private static void dispose(TestPlayer player) {
        DivineNarrativeEvents.logout(new PlayerEvent.PlayerLoggedOutEvent(player)); player.discard();
    }
    private static final class OwnedZombie extends Zombie implements OwnableEntity {
        private final UUID owner;
        OwnedZombie(ServerLevel level, UUID owner) { super(EntityType.ZOMBIE, level); this.owner = owner; }
        @Override public UUID getOwnerUUID() { return owner; }
    }
    private static final class TestPlayer extends FakePlayer {
        final DivineNarrativeData narrative = new DivineNarrativeData();
        private final LazyOptional<DivineNarrativeData> cap = LazyOptional.of(() -> narrative);
        boolean underwater, disconnected;
        TestPlayer(ServerLevel level) { super(level, new GameProfile(UUID.randomUUID(), "connected-test")); }
        @Override public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction side) {
            if (capability == DivineNarrativeCapability.CAP && cap != null) return cap.cast();
            return super.getCapability(capability, side);
        }
        @Override public boolean isUnderWater() { return underwater; }
        @Override public boolean hasDisconnected() { return disconnected; }
        @Override public void sendSystemMessage(Component message) {}
    }
    private static final class RegistryScope implements AutoCloseable {
        private final Map<ResourceLocation, DivineStoryDefinition> original = new LinkedHashMap<>();
        private final Map<ResourceLocation, GodPresenceDefinition> presences = new LinkedHashMap<>();
        private final Map<ResourceLocation, GodRelationshipDefinition> relations = new LinkedHashMap<>();
        private final Map<ResourceLocation, DivineStoryDefinition> current = new LinkedHashMap<>();
        RegistryScope() {
            DivineNarrativeRegistry.initialize();
            DivineNarrativeRegistry.stories().forEach(def -> original.put(def.id(), def)); current.putAll(original);
            for (ResourceLocation god : GodsRegistry.GODS_REGISTRY.get().getKeys()) {
                var presence = DivineNarrativeRegistry.presence(god); if (presence != null) presences.put(god, presence);
            }
            DivineNarrativeRegistry.relationships().forEach(relation -> relations.put(relation.id(), relation));
        }
        void install(DivineStoryDefinition... definitions) {
            for (var def : definitions) current.put(def.id(), def);
            DivineNarrativeRegistry.reload(current, presences, Map.of(), relations);
        }
        @Override public void close() { DivineNarrativeRegistry.reload(original, presences, Map.of(), relations); }
    }
}
