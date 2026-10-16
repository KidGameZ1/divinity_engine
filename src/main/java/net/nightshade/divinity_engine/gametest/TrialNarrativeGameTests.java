package net.nightshade.divinity_engine.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.nightshade.divinity_engine.DivinityEngineMod;
import net.nightshade.divinity_engine.divinity.gods.BaseGod;
import net.nightshade.divinity_engine.divinity.trial.GodlyTrial;
import net.nightshade.divinity_engine.network.cap.player.gods.PlayerGodsCapability;
import net.nightshade.divinity_engine.network.cap.player.narrative.DivineNarrativeCapability;
import net.nightshade.divinity_engine.network.cap.player.narrative.DivineNarrativeData;
import net.nightshade.divinity_engine.network.cap.player.trials.TrialPlayerCapability;
import net.nightshade.divinity_engine.registry.dimensions.DimensionRegistry;
import net.nightshade.divinity_engine.registry.divinity.gods.GodsRegistry;
import net.nightshade.divinity_engine.util.divinity.narrative.DivineNarrativeManager;
import net.nightshade.divinity_engine.util.divinity.narrative.DivineNarrativeRegistry;
import net.nightshade.divinity_engine.util.divinity.narrative.NarrativeProgress.Stage;
import net.nightshade.divinity_engine.util.divinity.trials.GodsTrialStructureSpawner;
import net.nightshade.divinity_engine.util.divinity.trials.TrialFailureReason;
import net.nightshade.divinity_engine.util.divinity.trials.TrialSession;
import net.nightshade.divinity_engine.util.divinity.trials.TrialSessionManager;
import net.nightshade.divinity_engine.util.divinity.trials.TrialSessionState;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Run only in a disposable Forge GameTest world. The template is the real pocket_dimension asset;
 * trial structures use vacant, deterministic slots in that world's godly_trials dimension.
 * These are server-world integration tests, not connected-client/login/packet-delivery tests.
 * Deed completion is fixture setup, not an assertion that the actual deed events were exercised.
 */
@GameTestHolder(DivinityEngineMod.MODID)
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = "divinity_engine", bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD)
@PrefixGameTestTemplate(false)
public final class TrialNarrativeGameTests {
    private static final ResourceLocation GOD = new ResourceLocation(DivinityEngineMod.MODID, "kairoth");
    private static final ResourceLocation TRIAL = new ResourceLocation(DivinityEngineMod.MODID, "gametest_trial");

    private TrialNarrativeGameTests() {}

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void register(net.minecraftforge.event.RegisterGameTestsEvent event) {
        if (!System.getProperty("forge.enabledGameTestNamespaces", "").equals("divinity_trial_recovery")) return;
        for (var method : TrialNarrativeGameTests.class.getDeclaredMethods())
            if (method.isAnnotationPresent(GameTest.class)) net.minecraft.gametest.framework.GameTestRegistry.register(method, Set.of("divinity_engine"));
    }

    @GameTest(template = "pocket_dimension", timeoutTicks = 100)
    public static void onlyTrialStartedAfterDeedUnlocksEnding(GameTestHelper h) {
        try (Fixture f = new Fixture(h)) {
            TrialSession old = f.start(20);
            h.assertTrue(f.memory.story.expectedTrial().isEmpty(), "A trial begun during the deed is not the finale trial");
            f.finishDeed();
            old.completeTrial();
            h.assertTrue(f.memory.story.stage() == Stage.TRIAL, "A pre-deed session victory must not unlock the ending");
            f.assertClosed(old);

            TrialSession fresh = f.start(20);
            h.assertTrue(f.memory.story.expectedTrial().equals(fresh.getSessionId().toString()), "Session start binds the fresh trial ID");
            fresh.completeTrial();
            h.assertTrue(f.memory.story.stage() == Stage.RETURN, "Fresh session victory unlocks shrine return");
            h.assertFalse(DivineNarrativeManager.hasSpecialization(f.player, GOD, "b"), "Victory alone cannot grant the final shrine reward");
            h.assertTrue(f.memory.story.branch().equals("b") && f.memory.victories == 2, "Both outcomes recorded without changing the permanent choice");
            f.assertClosed(fresh);
        }
        h.succeed();
    }

    @GameTest(template = "pocket_dimension", timeoutTicks = 100)
    public static void failedTrialRetriesAndDeduplicatesOutcomes(GameTestHelper h) {
        try (Fixture f = new Fixture(h)) {
            f.finishDeed();
            TrialSession failed = f.start(20);
            TrialSessionManager.failSession(f.player, TrialFailureReason.GENERIC_FAILURE);
            failed.failAndClose(TrialFailureReason.PLAYER_LOGGED_OUT);
            DivineNarrativeManager.onTrialOutcome(f.player, GOD, failed.getSessionId(), false);
            DivineNarrativeManager.onTrialOutcome(f.player, GOD, failed.getSessionId(), true);
            h.assertTrue(f.memory.failures == 1 && f.memory.victories == 0, "Duplicate or contradictory outcome cannot rewrite a failed session");
            h.assertTrue(f.memory.story.stage() == Stage.TRIAL && f.memory.story.branch().equals("b"), "Failure preserves completed deed and chosen branch");
            f.assertClosed(failed);

            TrialSession retry = f.start(20);
            h.assertFalse(retry.getSessionId().equals(failed.getSessionId()), "Retry receives a fresh session identity");
            retry.completeTrial();
            retry.completeTrial();
            DivineNarrativeManager.onTrialOutcome(f.player, GOD, retry.getSessionId(), true);
            DivineNarrativeManager.onTrialOutcome(f.player, GOD, retry.getSessionId(), false);
            h.assertTrue(f.memory.story.stage() == Stage.RETURN && f.memory.failures == 1 && f.memory.victories == 1,
                    "Retry victory advances exactly once; later failure callbacks cannot undo it");
            h.assertTrue(f.memory.story.outcomes().size() == 2, "One narrative outcome per session");
            f.assertClosed(retry);
        }
        h.succeed();
    }

    @GameTest(template = "pocket_dimension", timeoutTicks = 100)
    public static void completionDeletesArenaAndOnlyOwnedMobs(GameTestHelper h) {
        try (Fixture f = new Fixture(h)) {
            f.finishDeed();
            TrialSession session = f.start(20);
            Zombie owned = f.spawn(session, true);
            Zombie unrelated = f.spawn(session, false);
            BlockPos outside = f.outsideMarker(session);
            int slot = f.player.trialData.getArenaSlot();
            session.completeTrial();
            h.assertTrue(owned.isRemoved() && !unrelated.isRemoved(), "Completion removes only this session's tracked mobs");
            h.assertTrue(f.arenaLevel.getBlockState(outside).is(Blocks.GOLD_BLOCK), "Cleanup must not delete outside its arena bounds");
            h.assertTrue(((ControlledTrial) session.getTrial()).completions == 1, "Encounter reward hook runs once");
            session.completeTrial();
            session.fail(TrialFailureReason.GENERIC_FAILURE);
            h.assertTrue(((ControlledTrial) session.getTrial()).completions == 1 && f.memory.failures == 0, "Repeated transitions are idempotent");
            f.assertClosed(session);
            TrialSession reused = f.start(20);
            h.assertTrue(f.player.trialData.getArenaSlot() == slot, "Released fixed slot is immediately reusable");
            reused.failAndClose(TrialFailureReason.GENERIC_FAILURE);
            f.assertClosed(reused);
        }
        h.succeed();
    }

    @GameTest(template = "pocket_dimension", timeoutTicks = 100)
    public static void logoutHandlerCleansArenaMobsAndRecoveryState(GameTestHelper h) {
        try (Fixture f = new Fixture(h)) {
            f.finishDeed();
            TrialSession session = f.start(20);
            Zombie mob = f.spawn(session, true);
            TrialSessionManager.handlePlayerLogout(f.player);
            h.assertTrue(mob.isRemoved() && f.memory.failures == 1, "Logout records failure and removes the tracked mob immediately");
            h.assertTrue(f.memory.story.stage() == Stage.TRIAL, "Logout permits retry without replaying the deed");
            f.assertClosed(session);
            TrialSessionManager.handlePlayerLogout(f.player);
            TrialSessionManager.handlePlayerLogin(f.player);
            h.assertTrue(f.memory.failures == 1 && !f.player.trialData.isActive(), "Duplicate logout and clean recovery cannot restart or refail the session");
        }
        h.succeed();
    }

    @GameTest(template = "pocket_dimension", timeoutTicks = 100)
    public static void leavingArenaFailsOnceAndDeletesItsContents(GameTestHelper h) {
        try (Fixture f = new Fixture(h)) {
            f.finishDeed();
            TrialSession session = f.start(20);
            Zombie mob = f.spawn(session, true);
            f.player.setPos(session.getArenaOrigin().getX() - 4, session.getArenaCenter().getY(), session.getArenaCenter().getZ());
            session.tick(1);
            session.tick(2);
            h.assertTrue(f.memory.failures == 1 && mob.isRemoved(), "Leaving bounds fails even during countdown, once only");
            f.assertClosed(session);
        }
        h.succeed();
    }

    @GameTest(template = "pocket_dimension", timeoutTicks = 100)
    public static void mobDeathRoutingIsIsolatedBetweenSessions(GameTestHelper h) {
        try (Fixture first = new Fixture(h); Fixture second = new Fixture(h)) {
            TrialSession a = first.start(20);
            TrialSession b = second.start(20);
            Zombie ownedA = first.spawn(a, true), ownedB = second.spawn(b, true), unrelated = first.spawn(a, false);
            LivingDeathEvent outside = new LivingDeathEvent(unrelated, unrelated.damageSources().playerAttack(first.player));
            TrialSessionManager.handleDeathEvent(outside);
            h.assertFalse(outside.isCanceled() || unrelated.isRemoved(), "Untracked deaths must not be intercepted");
            LivingDeathEvent death = new LivingDeathEvent(ownedA, ownedA.damageSources().playerAttack(first.player));
            TrialSessionManager.handleDeathEvent(death);
            h.assertTrue(death.isCanceled() && ownedA.isRemoved() && !a.ownsEntity(ownedA), "Owned death is routed and removed");
            h.assertTrue(((ControlledTrial) a.getTrial()).kills == 1 && ((ControlledTrial) b.getTrial()).kills == 0, "Kill objective belongs only to the victim's session owner");
            h.assertTrue(TrialSessionManager.getSession(ownedB) == b && !ownedB.isRemoved(), "Other player's entity ownership survives");
            TrialSessionManager.handleDeathEvent(death);
            h.assertTrue(((ControlledTrial) a.getTrial()).kills == 1, "Duplicate death cannot credit another kill");
            a.failAndClose(TrialFailureReason.GENERIC_FAILURE);
            h.assertTrue(TrialSessionManager.getSession(second.player) == b && !ownedB.isRemoved(), "First session cleanup cannot close the second");
            first.assertClosed(a);
            b.failAndClose(TrialFailureReason.GENERIC_FAILURE);
            second.assertClosed(b);
        }
        h.succeed();
    }

    @GameTest(template = "pocket_dimension", timeoutTicks = 100)
    public static void completedPhaseReceivesFullNextPhaseBudget(GameTestHelper h) {
        try (Fixture f = new Fixture(h)) {
            f.finishDeed();
            TrialSession session = f.start(2, 1);
            f.finishCountdown(session);
            ControlledTrial encounter = (ControlledTrial) session.getTrial();
            session.tick(101);
            h.assertTrue(session.getPhaseTicksElapsed() == 1, "First phase has one tick remaining");
            encounter.advanceOnNextTick = true;
            session.tick(102);
            h.assertTrue(session.getState() == TrialSessionState.ACTIVE && f.player.trialData.getPhase() == 2,
                    "Completing a phase on its last tick must not immediately time out the next phase");
            h.assertTrue(session.getPhaseTicksElapsed() == 0 && session.getPhaseTicksRemaining() == 1,
                    "New phase must receive its entire configured tick budget");
            encounter.advanceOnNextTick = true;
            session.tick(103);
            h.assertTrue(f.memory.story.stage() == Stage.RETURN && f.memory.failures == 0 && encounter.completions == 1,
                    "Final phase completion on its deadline wins instead of failing");
            f.assertClosed(session);
        }
        h.succeed();
    }

    @GameTest(template = "pocket_dimension", timeoutTicks = 100)
    public static void unfinishedPhaseTimesOutAndCleansUp(GameTestHelper h) {
        try (Fixture f = new Fixture(h)) {
            f.finishDeed();
            TrialSession session = f.start(2);
            Zombie mob = f.spawn(session, true);
            f.finishCountdown(session);
            session.tick(101);
            h.assertTrue(session.getState() == TrialSessionState.ACTIVE && f.memory.failures == 0, "Must not time out one tick early");
            session.tick(102);
            h.assertTrue(f.memory.failures == 1 && mob.isRemoved() && f.memory.story.stage() == Stage.TRIAL,
                    "Unfinished phase fails at its deadline and preserves retry eligibility");
            h.assertTrue(((ControlledTrial) session.getTrial()).completions == 0, "Timeout cannot grant encounter rewards");
            f.assertClosed(session);
        }
        h.succeed();
    }

    @GameTest(template = "pocket_dimension", timeoutTicks = 100)
    public static void projectileAndCanceledDeathsKeepCorrectOwnership(GameTestHelper h) {
        try (Fixture f = new Fixture(h)) {
            TrialSession session = f.start(20);
            Zombie mob = f.spawn(session, true);
            var canceled = new LivingDeathEvent(mob, mob.damageSources().playerAttack(f.player));
            canceled.setCanceled(true); TrialSessionManager.handleDeathEvent(canceled);
            h.assertTrue(session.ownsEntity(mob) && ((ControlledTrial) session.getTrial()).kills == 0, "Canceled death changed ownership or progress");
            var environment = new LivingDeathEvent(mob, mob.damageSources().generic());
            TrialSessionManager.handleDeathEvent(environment);
            h.assertTrue(environment.isCanceled() && session.ownsEntity(mob) && mob.getHealth() > 0, "Revived mob lost its owner");
            var arrow = new net.minecraft.world.entity.projectile.Arrow(f.arenaLevel, f.player);
            var projectile = new LivingDeathEvent(mob, mob.damageSources().arrow(arrow, f.player));
            TrialSessionManager.handleDeathEvent(projectile);
            h.assertTrue(((ControlledTrial) session.getTrial()).kills == 1 && mob.isRemoved(), "Player projectile did not credit kill");
            arrow.discard(); session.failAndClose(TrialFailureReason.GENERIC_FAILURE); f.assertClosed(session);
        }
        h.succeed();
    }

    @GameTest(template = "pocket_dimension", timeoutTicks = 100)
    public static void cleanupKeepsReservationAndProtectsBlocks(GameTestHelper h) {
        try (Fixture f = new Fixture(h)) {
            TrialSession session = f.start(20);
            var jobs = net.nightshade.divinity_engine.util.divinity.trials.TrialArenaJobs.get(f.player.server);
            BlockPos pos = session.getArenaOrigin();
            var breaking = new net.minecraftforge.event.level.BlockEvent.BreakEvent(f.arenaLevel, pos, f.arenaLevel.getBlockState(pos), f.player);
            net.nightshade.divinity_engine.network.events.divinity.trials.TrialArenaProtection.breaking(breaking);
            h.assertTrue(breaking.isCanceled(), "Arena shell can be broken");
            session.fail(TrialFailureReason.PLAYER_LEFT_TRIAL);
            h.assertTrue(f.player.level() == h.getLevel() && TrialSessionManager.getSession(f.player) == null, "Player waits for deletion before being returned");
            h.assertTrue(jobs.reserved(session.getArenaSlot()) && jobs.protects(pos), "Cleaning slot was prematurely released/unprotected");
            jobs.recover(session.getArenaSlot(), UUID.randomUUID(), pos);
            h.assertTrue(jobs.reserved(session.getArenaSlot()), "Foreign recovery replaced reservation");
            f.assertClosed(session);
            h.assertFalse(jobs.protects(pos), "Deleted room remains protected");
            f.arenaLevel.setBlock(pos, Blocks.EMERALD_BLOCK.defaultBlockState(), 2);
            jobs.recover(session.getArenaSlot(), session.getSessionId(), pos);
            h.assertFalse(jobs.reserved(session.getArenaSlot()), "Old player capability scheduled deletion of a retired arena");
            h.assertTrue(f.arenaLevel.getBlockState(pos).is(Blocks.EMERALD_BLOCK), "Old recovery erased newer terrain");
            f.arenaLevel.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        }
        h.succeed();
    }

    @GameTest(template = "pocket_dimension", timeoutTicks = 100)
    public static void disconnectDuringPreparationDoesNotTeleportOrEraseForeignBlocks(GameTestHelper h) {
        try (Fixture f = new Fixture(h)) {
            TrialSession session = f.start(false, 20);
            TrialSessionManager.handlePlayerLogout(f.player);
            h.assertTrue(f.player.level() == h.getLevel(), "Preparation disconnect teleported player");
            f.assertClosed(session);
            h.assertTrue(f.memory.failures == 1, "Preparation failure not recorded once");
        }
        h.succeed();
    }

    @GameTest(template = "pocket_dimension", timeoutTicks = 100)
    public static void restartResumesOwnedCleanupOnly(GameTestHelper h) {
        try (Fixture f = new Fixture(h)) {
            TrialSession session = f.start(20);
            var jobs = net.nightshade.divinity_engine.util.divinity.trials.TrialArenaJobs.get(f.player.server);
            var restored = net.nightshade.divinity_engine.util.divinity.trials.TrialArenaJobs.load(jobs.save(new net.minecraft.nbt.CompoundTag()));
            h.assertTrue(restored.reserved(session.getArenaSlot()) && !restored.owns(session.getSessionId()), "Restart did not convert active arena to cleanup");
            BlockPos outside = f.outsideMarker(session);
            session.failAndClose(TrialFailureReason.GENERIC_FAILURE);
            for (int i = 0; i < 10000 && restored.reserved(session.getArenaSlot()); i++) restored.tick(f.player.server);
            h.assertFalse(restored.reserved(session.getArenaSlot()), "Recovered cleanup never finished");
            h.assertTrue(f.arenaLevel.getBlockState(outside).is(Blocks.GOLD_BLOCK), "Recovery erased outside arena");
            f.assertClosed(session);
        }
        h.succeed();
    }

    @GameTest(template = "pocket_dimension", timeoutTicks = 100)
    public static void journalReplicaQueuesAndRecoversMissingRevisions(GameTestHelper h) {
        var replica = new net.nightshade.divinity_engine.network.messages.narrative.JournalReplica();
        var packet = new net.minecraft.nbt.CompoundTag();
        packet.putLong("revision", 1);
        h.assertFalse(replica.apply(packet), "Delta accepted without a full baseline");
        packet.putBoolean("full", true);
        var entries = new net.minecraft.nbt.ListTag(); var entry = new net.minecraft.nbt.CompoundTag();
        entry.putString("id", "story/status"); entry.putString("text", "Preserved lore"); entry.putInt("progress", 0);
        entries.add(entry); packet.put("entries", entries);
        h.assertTrue(replica.apply(packet), "Initial snapshot requires a client player");
        packet.putBoolean("full", false); packet.putLong("revision", 3);
        h.assertFalse(replica.apply(packet), "Revision gap was applied");
        packet.putLong("revision", 2); entry.remove("text"); entry.putInt("progress", 5);
        h.assertTrue(replica.apply(packet), "Valid counter update rejected");
        var saved = replica.snapshot().getList("entries", net.minecraft.nbt.Tag.TAG_COMPOUND).getCompound(0);
        h.assertTrue(saved.getInt("progress") == 5 && saved.getString("text").equals("Preserved lore"), "Counter delta erased story text");
        packet.putLong("revision", 1); entry.putInt("progress", 0); replica.apply(packet);
        h.assertTrue(replica.snapshot().getLong("revision") == 2, "Stale delta rewound journal");
        replica.clear(); h.assertTrue(replica.snapshot() == null, "Disconnected journal leaked into next connection");
        h.succeed();
    }

    @GameTest(template = "pocket_dimension", timeoutTicks = 100)
    public static void counterRefreshPreservesProseAndIdleRevision(GameTestHelper h) {
        try (Fixture f = new Fixture(h)) {
            DivineNarrativeManager.sync(f.player, true);
            var before = f.player.narrative.lastJournal.copy(); long revision = f.player.narrative.journalRevision;
            net.nightshade.divinity_engine.util.divinity.narrative.NarrativeJournal.syncCounters(f.player);
            // Initial highest-favor reconciliation may produce one metadata update.
            revision = f.player.narrative.journalRevision;
            net.nightshade.divinity_engine.util.divinity.narrative.NarrativeJournal.syncCounters(f.player);
            h.assertTrue(f.player.narrative.journalRevision == revision, "Idle journal sent another packet");
            DivineNarrativeManager.advance(f.player, GOD, 1);
            net.nightshade.divinity_engine.util.divinity.narrative.NarrativeJournal.syncCounters(f.player);
            h.assertTrue(f.player.narrative.journalRevision > revision, "Quest counter did not sync");
            var current = f.player.narrative.lastJournal.getList("entries", net.minecraft.nbt.Tag.TAG_COMPOUND);
            var old = before.getList("entries", net.minecraft.nbt.Tag.TAG_COMPOUND);
            h.assertTrue(current.size() == old.size(), "Counter refresh changed discovered chapters");
            for (int i = 0; i < old.size(); i++) h.assertTrue(current.getCompound(i).getString("text").equals(old.getCompound(i).getString("text")), "Counter refresh rewrote lore");
        }
        h.succeed();
    }

    @GameTest(template = "pocket_dimension", timeoutTicks = 100)
    public static void arenaProtectionStopsPlacementFluidsPistonsAndExplosionTerrain(GameTestHelper h) {
        try (Fixture f = new Fixture(h)) {
            var session = f.start(20);
            var pos = session.getArenaCenter();
            var snapshot = net.minecraftforge.common.util.BlockSnapshot.create(f.arenaLevel.dimension(), f.arenaLevel, pos);
            var place = new net.minecraftforge.event.level.BlockEvent.EntityPlaceEvent(snapshot, Blocks.STONE.defaultBlockState(), f.player);
            net.nightshade.divinity_engine.network.events.divinity.trials.TrialArenaProtection.placing(place);
            h.assertTrue(place.isCanceled(), "Player can place arena blocks");
            var piston = new net.minecraftforge.event.level.PistonEvent.Pre(f.arenaLevel, pos, Direction.UP,
                    net.minecraftforge.event.level.PistonEvent.PistonMoveType.EXTEND);
            net.nightshade.divinity_engine.network.events.divinity.trials.TrialArenaProtection.piston(piston);
            h.assertTrue(piston.isCanceled(), "Piston can change protected arena");
            var explosion = new net.minecraft.world.level.Explosion(f.arenaLevel, null, pos.getX(), pos.getY(), pos.getZ(), 4, false,
                    net.minecraft.world.level.Explosion.BlockInteraction.DESTROY);
            BlockPos outside = f.outsideMarker(session);
            explosion.getToBlow().add(pos); explosion.getToBlow().add(outside);
            var detonate = new net.minecraftforge.event.level.ExplosionEvent.Detonate(f.arenaLevel, explosion, new ArrayList<>());
            net.nightshade.divinity_engine.network.events.divinity.trials.TrialArenaProtection.explosion(detonate);
            h.assertTrue(!explosion.getToBlow().contains(pos) && explosion.getToBlow().contains(outside), "Explosion filtering changed unrelated blocks");
            f.arenaLevel.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 2);
            f.arenaLevel.setBlock(pos.east(), Blocks.AIR.defaultBlockState(), 2);
            f.arenaLevel.setBlock(pos, Blocks.WATER.defaultBlockState(), 2);
            net.minecraft.world.level.material.Fluids.WATER.tick(f.arenaLevel, pos, f.arenaLevel.getFluidState(pos));
            h.assertTrue(f.arenaLevel.getFluidState(pos.east()).isEmpty(), "Water spread inside protected arena");
            h.assertFalse(net.nightshade.divinity_engine.network.events.divinity.trials.TrialArenaProtection.protectedAt(h.getLevel(), pos), "Protection leaked outside trial dimension");
            session.failAndClose(TrialFailureReason.GENERIC_FAILURE); f.assertClosed(session);
        }
        h.succeed();
    }

    @GameTest(template = "pocket_dimension", timeoutTicks = 100)
    public static void summonsAndReloadedEntitiesRemainSessionOwned(GameTestHelper h) {
        try (Fixture f = new Fixture(h)) {
            var session = f.start(20); Zombie owner = f.spawn(session, true);
            var vex = EntityType.VEX.create(f.arenaLevel); vex.setOwner(owner); vex.setPos(owner.position());
            f.entities.add(vex); f.arenaLevel.addFreshEntity(vex);
            h.assertTrue(session.ownsEntity(vex), "Summoned vex was not tracked");
            var oldTag = vex.getPersistentData().copy();
            session.failAndClose(TrialFailureReason.PLAYER_LOGGED_OUT);
            h.assertTrue(vex.isRemoved(), "Summon survived cleanup");
            var loaded = EntityType.VEX.create(f.arenaLevel); loaded.getPersistentData().merge(oldTag); loaded.setPos(owner.position());
            var join = new net.minecraftforge.event.entity.EntityJoinLevelEvent(loaded, f.arenaLevel);
            net.nightshade.divinity_engine.network.events.divinity.trials.TrialArenaProtection.join(join);
            h.assertTrue(join.isCanceled() && loaded.isRemoved(), "Unloaded mob returned after its session closed");
            f.assertClosed(session);
        }
        h.succeed();
    }

    @GameTest(template = "pocket_dimension", timeoutTicks = 100)
    public static void compareLegacyDeletionWithBoundedCleanup(GameTestHelper h) {
        try (Fixture f = new Fixture(h)) {
            var session = f.start(20); session.failAndClose(TrialFailureReason.GENERIC_FAILURE);
            long start = System.nanoTime();
            // Baseline only: the old synchronous API runs solely against this disposable fixture's owned arena.
            GodsTrialStructureSpawner.destroyStructure(f.arenaLevel, session.getArenaOrigin());
            DivinityEngineMod.LOGGER.info("Legacy trial deletion baseline: {} ms in one call (8450 block operations)", (System.nanoTime() - start) / 1_000_000.0);
            f.assertClosed(session);
            var queued = f.start(20); queued.failAndClose(TrialFailureReason.GENERIC_FAILURE); f.assertClosed(queued);
        }
        h.succeed();
    }

    @GameTest(template = "pocket_dimension", timeoutTicks = 600, batch = "trial_cold_chunks")
    public static void coldArenaPreparationAndCleanupAdvanceAcrossRealTicks(GameTestHelper h) {
        Fixture f = new Fixture(h);
        int slot = GodsTrialStructureSpawner.getArenaSlotCount() - 1;
        var jobs = net.nightshade.divinity_engine.util.divinity.trials.TrialArenaJobs.get(f.player.server);
        h.assertFalse(jobs.reserved(slot) || f.slots.contains(slot), "Cold test slot is not vacant");
        BlockPos origin = GodsTrialStructureSpawner.getArenaOrigin(slot);
        f.slots.add(slot); f.ownedArenas.put(slot, origin);
        var session = new TrialSession(UUID.randomUUID(), f.player, TRIAL, f.god, new ControlledTrial(f.player, new int[]{10000}), slot, origin, f.arenaLevel) {
            @Override public ServerPlayer getPlayer() { return f.player; }
        };
        f.ownedSessions.add(session); f.sessions.put(f.player.getUUID(), session);
        session.start();
        h.assertTrue(f.player.level() == h.getLevel(), "Preparation teleported before room existed");
        h.onEachTick(() -> {
            h.assertTrue(jobs.lastOperations() <= 512, "Real server tick exceeded arena budget");
            if (session.getState() == TrialSessionState.COUNTDOWN && !session.isTerminal()) session.fail(TrialFailureReason.PLAYER_LEFT_TRIAL);
        });
        h.succeedWhen(() -> {
            h.assertTrue(session.isTerminal() && !jobs.reserved(slot), "Waiting for preparation, return and cleanup");
            f.assertClosed(session); f.close();
        });
    }

    private static final class Fixture implements AutoCloseable {
        final GameTestHelper h;
        final TestPlayer player;
        final ServerLevel arenaLevel;
        final BaseGod god;
        final DivineNarrativeData.Memory memory;
        final Vec3 returnPosition;
        final List<TrialSession> ownedSessions = new ArrayList<>();
        final List<Entity> entities = new ArrayList<>();
        final Map<BlockPos, BlockState> outsideBlocks = new LinkedHashMap<>();
        final Map<Integer, BlockPos> ownedArenas = new LinkedHashMap<>();
        final Map<UUID, TrialSession> sessions = managerField("SESSIONS_BY_PLAYER");
        final Map<UUID, UUID> owners = managerField("TRACKED_ENTITY_OWNERS");
        final Set<Integer> slots = managerField("CLAIMED_ARENA_SLOTS");
        final Map<UUID, ResourceKey<Level>> expectedChanges = managerField("EXPECTED_DIMENSION_CHANGES");

        Fixture(GameTestHelper h) {
            this.h = h;
            DivineNarrativeRegistry.initialize();
            arenaLevel = h.getLevel().getServer().getLevel(DimensionRegistry.GODLY_TRIALS);
            h.assertTrue(arenaLevel != null, "GameTest world must load the actual godly_trials dimension");
            god = GodsRegistry.GODS_REGISTRY.get().getValue(GOD);
            h.assertTrue(god != null && DivineNarrativeRegistry.story(GOD) != null, "Built-in Kairoth god and story must be registered");
            player = new TestPlayer(h.getLevel());
            returnPosition = h.absoluteVec(new Vec3(2.5, 2, 2.5));
            player.setPos(returnPosition);
            var bound = god.createGodDefaultInstance();
            bound.setFavor(100);
            PlayerGodsCapability.load(player).updateGodInstance(bound, false);
            memory = player.narrative.get(GOD);
            memory.contacted = true;
            memory.story.request(true);
            memory.story.acceptOffering(true);
            memory.story.confirm("b", true, true);
            memory.branchSnapshot = DivineNarrativeRegistry.story(GOD).branches().get("b");
            h.assertTrue(memory.story.stage() == Stage.DEED, "Fixture must start with a confirmed deed");
        }

        void finishDeed() {
            DivineNarrativeManager.advance(player, GOD, memory.branchSnapshot.target());
            h.assertTrue(memory.story.stage() == Stage.TRIAL, "Completed fixture deed must require a fresh trial");
        }

        TrialSession start(int... durations) {
            return start(true, durations);
        }

        TrialSession start(boolean prepare, int... durations) {
            h.assertTrue(TrialSessionManager.getSession(player) == null, "Fixture must not replace a live session");
            int slot = -1;
            for (int candidate = 0; candidate < GodsTrialStructureSpawner.getArenaSlotCount(); candidate++) {
                if (!net.nightshade.divinity_engine.util.divinity.trials.TrialArenaJobs.get(player.server).reserved(candidate)
                        && slots.add(candidate)) { slot = candidate; break; }
            }
            h.assertTrue(slot >= 0, "A deterministic arena slot must be available");
            BlockPos origin = GodsTrialStructureSpawner.getArenaOrigin(slot);
            try {
                h.assertTrue(arenaIsAir(origin), "Refusing to overwrite a nonempty arena; use a disposable GameTest world");
                ownedArenas.put(slot, origin);
                ControlledTrial encounter = new ControlledTrial(player, durations);
                TrialSession session = new TrialSession(UUID.randomUUID(), player, TRIAL, god, encounter, slot, origin, arenaLevel) {
                    @Override public ServerPlayer getPlayer() { return player; }
                };
                ownedSessions.add(session);
                sessions.put(player.getUUID(), session);
                session.start();
                h.assertTrue(session.getState() == TrialSessionState.PREPARING && player.level() == h.getLevel(), "Preparing must not teleport into an incomplete room");
                if (!prepare) return session;
                drainJobs(() -> session.getState() != TrialSessionState.PREPARING);
                h.assertTrue(!arenaIsAir(origin), "Real gods_trial structure asset must have been placed");
                h.assertTrue(player.level() == arenaLevel && player.trialData.isActive(), "Start moves the controlled player and persists active state");
                h.assertTrue(TrialSessionManager.getSession(player) == session, "Manager must resolve the controlled session");
                return session;
            } catch (RuntimeException | Error failure) {
                slots.remove(slot);
                throw failure;
            }
        }

        void finishCountdown(TrialSession session) {
            for (int tick = 1; tick < 100; tick++) session.tick(tick);
            h.assertTrue(session.getState() == TrialSessionState.COUNTDOWN, "Countdown must last all 100 ticks");
            session.tick(100);
            h.assertTrue(session.getState() == TrialSessionState.ACTIVE && session.getPhaseTicksElapsed() == 0,
                    "Countdown enters phase one with an untouched timer");
        }

        Zombie spawn(TrialSession session, boolean tracked) {
            Zombie mob = new Zombie(EntityType.ZOMBIE, arenaLevel);
            entities.add(mob);
            mob.setNoAi(true);
            mob.setPersistenceRequired();
            mob.setPos(Vec3.atCenterOf(session.getArenaCenter()));
            h.assertTrue(arenaLevel.addFreshEntity(mob), "Test mob must exist in the real server level");
            if (tracked) {
                session.trackEntity(mob);
                h.assertTrue(TrialSessionManager.getSession(mob) == session, "Tracked entity must resolve to its owning session");
            }
            return mob;
        }

        BlockPos outsideMarker(TrialSession session) {
            BlockPos pos = session.getArenaOrigin().offset(-1, 1, 0);
            outsideBlocks.putIfAbsent(pos, arenaLevel.getBlockState(pos));
            arenaLevel.setBlock(pos, Blocks.GOLD_BLOCK.defaultBlockState(), 3);
            return pos;
        }

        boolean arenaIsAir(BlockPos origin) {
            BlockPos end = origin.offset(GodsTrialStructureSpawner.STRUCTURE_WIDTH - 1,
                    GodsTrialStructureSpawner.STRUCTURE_HEIGHT - 1, GodsTrialStructureSpawner.STRUCTURE_LENGTH - 1);
            for (BlockPos pos : BlockPos.betweenClosed(origin, end)) if (!arenaLevel.getBlockState(pos).isAir()) return false;
            return true;
        }

        void assertClosed(TrialSession session) {
            h.assertTrue(TrialSessionManager.getSession(player) == null && !sessions.containsKey(player.getUUID()), "Player/session routing must be removed");
            drainJobs(() -> !net.nightshade.divinity_engine.util.divinity.trials.TrialArenaJobs.get(player.server).reserved(session.getArenaSlot()));
            h.assertTrue(arenaIsAir(session.getArenaOrigin()), "Entire placed arena must be deleted after bounded cleanup");
            h.assertFalse(owners.containsValue(player.getUUID()) || expectedChanges.containsKey(player.getUUID()), "Owned entity and expected-teleport routes must not leak");
            int slot = GodsTrialStructureSpawner.getArenaSlotCount();
            for (int i = 0; i < slot; i++) if (GodsTrialStructureSpawner.getArenaOrigin(i).equals(session.getArenaOrigin()))
                h.assertFalse(slots.contains(i), "Arena slot must be released");
            h.assertFalse(player.trialData.isActive() || player.trialData.isCleanupPending(), "Recovery flags must clear");
            h.assertTrue(player.trialData.getSessionId().isEmpty(), "Persisted session ID must clear");
            h.assertTrue(player.level() == h.getLevel() && player.position().distanceToSqr(returnPosition) < 0.0001,
                    "Return teleport must target the captured dimension and position");
        }

        void drainJobs(java.util.function.BooleanSupplier finished) {
            var jobs = net.nightshade.divinity_engine.util.divinity.trials.TrialArenaJobs.get(player.server);
            int calls = 0; long total = 0, worst = 0;
            while (!finished.getAsBoolean() && calls++ < 10000) {
                jobs.tick(player.server);
                h.assertTrue(jobs.lastOperations() <= 512, "Arena jobs exceeded their global block budget");
                total += jobs.lastNanos(); worst = Math.max(worst, jobs.lastNanos());
            }
            h.assertTrue(finished.getAsBoolean(), "Arena jobs did not finish");
            DivinityEngineMod.LOGGER.info("Trial job sample: {} slices, {} ms total, {} ms worst slice", calls, total / 1_000_000.0, worst / 1_000_000.0);
        }

        @Override public void close() {
            RuntimeException cleanupFailure = null;
            try {
                for (TrialSession session : ownedSessions) {
                    try { session.close(); }
                    catch (RuntimeException failure) {
                        if (cleanupFailure == null) cleanupFailure = failure;
                        else cleanupFailure.addSuppressed(failure);
                    }
                }
                drainJobs(() -> ownedSessions.stream().noneMatch(session -> net.nightshade.divinity_engine.util.divinity.trials.TrialArenaJobs.get(player.server).reserved(session.getArenaSlot())));
            } finally {
                // Remove only entries belonging to this fixture, even if a runtime assertion failed.
                for (TrialSession session : ownedSessions) sessions.remove(player.getUUID(), session);
                expectedChanges.remove(player.getUUID());
                owners.entrySet().removeIf(entry -> entry.getValue().equals(player.getUUID()));
                for (Entity entity : entities) entity.discard();
                for (var arena : ownedArenas.entrySet()) {
                    boolean reusedElsewhere = sessions.values().stream().anyMatch(session -> session.getTrialLevel() == arenaLevel
                            && session.getArenaOrigin().equals(arena.getValue()));
                    if (!reusedElsewhere) {
                        try {
                            if (!arenaIsAir(arena.getValue())) GodsTrialStructureSpawner.destroyStructure(arenaLevel, arena.getValue());
                        } finally { slots.remove(arena.getKey()); }
                    }
                }
                for (var entry : outsideBlocks.entrySet()) arenaLevel.setBlock(entry.getKey(), entry.getValue(), 3);
                player.discard();
                player.narrativeCap.invalidate();
                player.trialCap.invalidate();
            }
            if (cleanupFailure != null) throw cleanupFailure;
        }
    }

    /** Only the fixture inserts entries: production exposes no registration seam for controlled sessions. */
    @SuppressWarnings("unchecked")
    private static <T> T managerField(String name) {
        try {
            Field field = TrialSessionManager.class.getDeclaredField(name);
            field.setAccessible(true);
            return (T) field.get(null);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Trial manager test seam changed: " + name, e);
        }
    }

    private static final class ControlledTrial extends GodlyTrial {
        final int[] durations;
        boolean advanceOnNextTick;
        int completions;
        int kills;

        ControlledTrial(ServerPlayer player, int[] durations) { super(player); this.durations = durations.clone(); }
        @Override protected void onStart() {}
        @Override protected void runPhase(int phase) { phaseDuration = durations[phase - 1]; }
        @Override protected void onComplete() { completions++; }
        @Override protected int getMaxPhases() { return durations.length; }
        @Override public void onKillEntity(Player player, LivingDeathEvent event) { kills++; }
        @Override public void tick() {
            if (advanceOnNextTick) { advanceOnNextTick = false; nextPhase(); }
        }
    }

    /** No player-list insertion, socket, world entity registration, or real client dimension transition. */
    private static final class TestPlayer extends FakePlayer {
        final DivineNarrativeData narrative = new DivineNarrativeData();
        final TrialPlayerCapability.TrialPlayerData trialData = new TrialPlayerCapability.TrialPlayerData();
        final LazyOptional<DivineNarrativeData> narrativeCap = LazyOptional.of(() -> narrative);
        final LazyOptional<TrialPlayerCapability.TrialPlayerData> trialCap = LazyOptional.of(() -> trialData);

        TestPlayer(ServerLevel level) { super(level, new GameProfile(UUID.randomUUID(), "trial-test")); }
        @Override public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
            if (cap == DivineNarrativeCapability.CAP && narrativeCap != null) return narrativeCap.cast();
            if (cap == TrialPlayerCapability.CAP && trialCap != null) return trialCap.cast();
            return super.getCapability(cap, side);
        }
        @Override public void teleportTo(ServerLevel level, double x, double y, double z, float yaw, float pitch) {
            setServerLevel(level);
            setPos(x, y, z);
            setYRot(yaw);
            setXRot(pitch);
        }
    }
}
