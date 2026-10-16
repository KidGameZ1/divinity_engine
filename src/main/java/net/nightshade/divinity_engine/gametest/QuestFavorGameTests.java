package net.nightshade.divinity_engine.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;
import net.nightshade.divinity_engine.network.cap.player.gods.*;
import net.nightshade.divinity_engine.network.cap.player.narrative.*;
import net.nightshade.divinity_engine.network.events.divinity.narrative.DivineNarrativeEvents;
import net.nightshade.divinity_engine.network.messages.narrative.QuestMenuAction;
import net.nightshade.divinity_engine.registry.divinity.blessing.BlessingsRegistry;
import net.nightshade.divinity_engine.registry.divinity.gods.GodsRegistry;
import net.nightshade.divinity_engine.util.divinity.gods.*;
import net.nightshade.divinity_engine.util.divinity.narrative.*;
import net.nightshade.divinity_engine.world.inventory.BlessingsMenu;
import java.util.*;

@Mod.EventBusSubscriber(modid = "divinity_engine", bus = Mod.EventBusSubscriber.Bus.MOD)
@PrefixGameTestTemplate(false)
public final class QuestFavorGameTests {
    @SubscribeEvent
    @SuppressWarnings("deprecation")
    public static void register(RegisterGameTestsEvent event) {
        if (!Arrays.asList(System.getProperty("forge.enabledGameTestNamespaces", "").split(",")).contains("divinity_quest_favor")) return;
        for (var method : QuestFavorGameTests.class.getDeclaredMethods())
            if (method.isAnnotationPresent(GameTest.class)) GameTestRegistry.register(method, Set.of("divinity_engine"));
        try {
            GameTestRegistry.register(BlessingSpecializationGameTests.class.getMethod("all36PermanentBranches", GameTestHelper.class), Set.of("divinity_engine"));
            for (String name : List.of("exactDonationsAndCanceledTurnIns", "shrineConfirmationAndReentrantDonation", "prayerRevalidatesListenerMutations", "customRewardIdsRemainEarnedAfterReconnect"))
                GameTestRegistry.register(DivineNarrativeGameTests.class.getMethod(name, GameTestHelper.class), Set.of("divinity_engine"));
        }
        catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", timeoutTicks = 200)
    public static void everyEndingGrantsItsActualUpgradeExactlyOnce(GameTestHelper h) {
        int tested = 0;
        for (var definition : DivineNarrativeRegistry.stories().stream()
                .filter(def -> def.godId().getNamespace().equals("divinity_engine") && DivineNarrativeRegistry.primary(def)).toList())
            for (String branch : List.of("a", "b")) {
            var p = new TestPlayer(h.getLevel());
            try {
                var god = DivineNarrativeManager.god(definition.godId());
                var instance = god.createGodDefaultInstance(); instance.setFavor(0);
                PlayerGodsCapability.load(p).updateGodInstance(instance, false);
                DivineRelationship.onContact(p, god);
                var memory = p.narrative.story(definition); memory.snapshot(definition); memory.branchSnapshot = definition.branches().get(branch);
                memory.story.restore(NarrativeProgress.Stage.RETURN, branch, 0, "", "", Set.of());
                var reward = memory.branchSnapshot.reward();
                var blessing = BlessingsRegistry.BLESSINGS_REGISTRY.get().getValue(reward.blessingId()).createDefaultInstance();
                blessing.setBoundGod(instance); p.variables.setBlessingSlot1(blessing);
                shrine(h, p, definition.godId());
                h.assertFalse(BlessingSpecializations.isActive(p, definition.godId().getPath(), branch), "Reward was granted before the ending");
                var action = new QuestMenuAction(27, definition.id(), "finish");
                h.assertTrue(action.apply(p), "Could not conclude " + definition.id() + branch);
                h.assertTrue(instance.getFavor() == 0, "Deed-based finale must not add numeric favor");
                h.assertTrue(memory.story.stage() == NarrativeProgress.Stage.COMPLETE
                        && memory.story.specialization().equals(reward.id().toString()), "Finale did not permanently record the chosen upgrade");
                h.assertTrue(p.narrative.get(definition.godId()).relationship.getInt("proof") == 5, "Finale lost the fresh trial proof");
                h.assertFalse(action.apply(p), "Repeated ending awarded twice");
                h.assertTrue(instance.getFavor() == 0, "Repeated ending changed favor");
                h.assertTrue(BlessingSpecializations.isActive(p, definition.godId().getPath(), branch), "Earned upgrade not active");
                CompoundTag saved = p.narrative.serializeNBT(); p.narrative.deserializeNBT(saved);
                h.assertTrue(BlessingSpecializations.isActive(p, definition.godId().getPath(), branch), "Upgrade lost after reload");
                // Exercise the finale guard directly so the menu's short rate limiter cannot hide a replay bug.
                h.assertTrue(DivineNarrativeManager.action(p, definition.id(), "finish", "", "") == 0, "Reload reopened an already claimed ending");
                var journal = NarrativeJournal.build(p); boolean visible = false;
                for (Tag tag : journal.getList("entries", Tag.TAG_COMPOUND)) {
                    var entry = (CompoundTag) tag;
                    if (!entry.getString("story").equals(definition.id().toString())) continue;
                    for (Tag upgrade : entry.getList("upgrades", Tag.TAG_COMPOUND)) {
                        var value = (CompoundTag) upgrade;
                        if (value.getString("id").equals(reward.id().toString())) visible = value.getBoolean("earned") && value.getBoolean("active");
                    }
                }
                h.assertTrue(visible, "Journal hid earned upgrade"); tested++;
            } finally { p.discard(); }
        }
        h.assertTrue(tested == 36, "Expected all 36 original-pantheon endings"); h.succeed();
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension")
    public static void legacyBudgetsPersistWhileManagedWorshipAwardsDeedsNotPoints(GameTestHelper h) {
        var ledger = new CompoundTag(); int sum = 0;
        for (int i = 0; i < 100; i++) sum += FavorRewards.allowance(ledger, FavorRewards.Source.OFFERING, 1000, 100);
        h.assertTrue(sum == 30, "Large or split offerings bypassed budget");
        h.assertTrue(FavorRewards.allowance(ledger.copy(), FavorRewards.Source.OFFERING, 10, 101) == 0, "Reload reset budget");
        h.assertTrue(FavorRewards.allowance(ledger, FavorRewards.Source.PRAYER, 100, 100) == 2, "Prayer per-action cap");
        h.assertTrue(FavorRewards.allowance(ledger, FavorRewards.Source.PRAYER, 100, 101) == 0, "Repeated prayer tick earned favor");
        h.assertTrue(FavorRewards.allowance(ledger, FavorRewards.Source.OFFERING, 100, 24100) == 12, "New server-time window did not reset");
        h.assertTrue(FavorRewards.allowance(ledger, FavorRewards.Source.OFFERING, 0, 24100) == 0, "Zero reward generated favor");
        var p = new TestPlayer(h.getLevel());
        try {
            var instance = GodsRegistry.SOLARIUS.get().createGodDefaultInstance(); instance.setFavor(0);
            PlayerGodsCapability.load(p).updateGodInstance(instance, false);
            DivineRelationship.onContact(p, instance.getBaseGod());
            var memory = p.narrative.story(DivineNarrativeRegistry.story(instance.getBaseGodId()));
            h.assertTrue(memory.story.request(true, true), "Fresh contacted player could not request story");
            h.assertFalse(FavorRewards.interaction(instance, p, FavorRewards.Source.OFFERING,
                    () -> { GodHelper.increaseFavor(instance, p, 60); return false; }), "Rejected offering was accepted");
            h.assertTrue(instance.getFavor() == 0, "Rejected offering earned favor");
            h.assertTrue(memory.story.stage() == NarrativeProgress.Stage.OFFERING && memory.relationship.getInt("proof") == 0,
                    "Rejected offering advanced the story");
            h.assertTrue(FavorRewards.interaction(instance, p, FavorRewards.Source.OFFERING,
                    () -> { GodHelper.increaseFavor(instance, p, 60); return true; }), "Valid offering was rejected");
            DivineNarrativeManager.onAcceptedOffering(p, instance.getBaseGod());
            h.assertTrue(instance.getFavor() == 0, "Accepted managed offering still awards numeric favor");
            h.assertTrue(memory.story.stage() == NarrativeProgress.Stage.WITNESS && memory.relationship.getInt("proof") == 1,
                    "Accepted offering did not earn the introductory proof");
            GodHelper.increaseFavor(instance, p, 250);
            for (var source : FavorRewards.Source.values())
                h.assertTrue(FavorRewards.grant(p, instance.getBaseGod(), source, 250) == 0, "Managed worship path still awards points: " + source);
            h.assertTrue(instance.getFavor() == 0 && memory.relationship.getInt("proof") == 1, "Raw points bypassed deed progression");
            FavorRewards.foundation(p, instance.getBaseGod());
            CompoundTag foundation = memory.favorLedger.copy();
            h.assertTrue(foundation.getBoolean("foundation"), "First sanctuary was not remembered");
            FavorRewards.foundation(p, instance.getBaseGod());
            h.assertTrue(memory.favorLedger.equals(foundation) && instance.getFavor() == 0, "Repeated foundation changed rewards");
            var saved = p.narrative.serializeNBT(); p.narrative.deserializeNBT(saved);
            FavorRewards.foundation(p, instance.getBaseGod());
            h.assertTrue(p.narrative.get(instance.getBaseGodId()).favorLedger.equals(foundation)
                    && instance.getFavor() == 0, "Reload granted foundation twice");
        } finally { p.discard(); }
        h.succeed();
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension")
    public static void questMenuRejectsForeignAndStaleActions(GameTestHelper h) {
        var p = new TestPlayer(h.getLevel());
        try {
            for (var god : List.of(GodsRegistry.SOLARIUS.get(), GodsRegistry.NYTHEA.get())) {
                var instance = god.createGodDefaultInstance(); instance.setFavor(0); PlayerGodsCapability.load(p).updateGodInstance(instance, false);
                DivineRelationship.onContact(p, god);
            }
            var own = DivineNarrativeRegistry.story(GodsRegistry.SOLARIUS.getId());
            var foreign = DivineNarrativeRegistry.story(GodsRegistry.NYTHEA.getId());
            shrine(h, p, own.godId());
            h.assertFalse(new QuestMenuAction(27, foreign.id(), "request").apply(p), "Foreign story accepted");
            h.assertFalse(new QuestMenuAction(26, own.id(), "request").apply(p), "Stale container accepted");
            h.assertFalse(new QuestMenuAction(27, own.id(), "confirm").apply(p), "Menu bypassed permanent-choice confirmation");
            h.assertTrue(new QuestMenuAction(27, own.id(), "request").apply(p), "Matching request rejected");
            h.assertFalse(new QuestMenuAction(27, own.id(), "finish").apply(p), "Unfinished quest claimed a reward");
            p.setPos(p.getX() + 20, p.getY(), p.getZ());
            h.assertFalse(new QuestMenuAction(27, own.id(), "status").apply(p), "Distant statue remained valid");
        } finally { p.discard(); }
        h.succeed();
    }
    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension")
    public static void objectiveMilestoneRewardsOnlyTheTransition(GameTestHelper h) {
        var p = new TestPlayer(h.getLevel());
        try {
            var god = GodsRegistry.SOLARIUS.get(); var instance = god.createGodDefaultInstance(); instance.setFavor(0);
            PlayerGodsCapability.load(p).updateGodInstance(instance, false);
            DivineRelationship.onContact(p, god);
            var def = DivineNarrativeRegistry.story(god.getRegistryName()); var memory = p.narrative.story(def);
            memory.snapshot(def); memory.story.expanded(true);
            memory.story.restore(NarrativeProgress.Stage.WITNESS, "", def.chronicle().witness().target() - 1, "", "", Set.of());
            DivineRelationship.refresh(p, god);
            h.assertTrue(memory.relationship.getInt("proof") == 1, "Witness fixture did not own introductory proof");
            DivineNarrativeManager.advance(p, def.id(), 1);
            h.assertTrue(memory.story.stage() == NarrativeProgress.Stage.CHOICE && memory.relationship.getInt("proof") == 2,
                    "Completed witness chapter did not award the second proof");
            h.assertTrue(instance.getFavor() == 0, "Completed chapter still awards numeric favor");
            DivineNarrativeManager.advance(p, def.id(), 1);
            p.narrative.deserializeNBT(p.narrative.serializeNBT());
            DivineNarrativeManager.advance(p, def.id(), 1);
            var restored = p.narrative.story(def);
            h.assertTrue(restored.story.stage() == NarrativeProgress.Stage.CHOICE && restored.relationship.getInt("proof") == 2
                    && restored.story.progress() == 0 && instance.getFavor() == 0, "Repeat callback or reload repeated milestone reward");
        } finally { p.discard(); }
        h.succeed();
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension")
    public static void foundationWaitsForSuccessfulPlacement(GameTestHelper h) {
        var p = new TestPlayer(h.getLevel());
        try {
            var god = GodsRegistry.SOLARIUS.get(); var instance = god.createGodDefaultInstance();
            instance.setFavor(0); PlayerGodsCapability.load(p).updateGodInstance(instance, false);
            DivineRelationship.onContact(p, god);
            var memory = p.narrative.get(god.getRegistryName());
            BlockPos pos = h.absolutePos(new BlockPos(2, 2, 2));
            var level = h.getLevel(); var air = Blocks.AIR.defaultBlockState();
            var statue = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("divinity_engine", "solarius_statue")).defaultBlockState();
            var endTick = new TickEvent.ServerTickEvent(TickEvent.Phase.END, () -> true, level.getServer());
            level.setBlockAndUpdate(pos, air);
            var snapshot = BlockSnapshot.create(level.dimension(), level, pos);
            level.setBlockAndUpdate(pos, statue);
            var canceled = new BlockEvent.EntityPlaceEvent(snapshot, air, p);
            DivineNarrativeEvents.placed(canceled);
            h.assertFalse(memory.favorLedger.getBoolean("foundation"), "Foundation granted before placement completed");
            canceled.setCanceled(true);
            DivineNarrativeEvents.serverTick(endTick);
            h.assertFalse(memory.favorLedger.getBoolean("foundation"), "Canceled placement earned foundation");

            var reverted = new BlockEvent.EntityPlaceEvent(snapshot, air, p);
            DivineNarrativeEvents.placed(reverted);
            level.setBlockAndUpdate(pos, air);
            DivineNarrativeEvents.serverTick(endTick);
            h.assertFalse(memory.favorLedger.getBoolean("foundation"), "Removed placement earned foundation");

            level.setBlockAndUpdate(pos, statue);
            var accepted = new BlockEvent.EntityPlaceEvent(snapshot, air, p);
            DivineNarrativeEvents.placed(accepted);
            DivineNarrativeEvents.placed(accepted);
            DivineNarrativeEvents.serverTick(endTick);
            h.assertTrue(memory.favorLedger.getBoolean("foundation"), "Successful placement must remember the foundation");
            h.assertTrue(instance.getFavor() == 0 && memory.relationship.getInt("proof") == 0, "Foundation bypassed story proofs or added points");
            CompoundTag remembered = memory.favorLedger.copy();
            DivineNarrativeEvents.placed(accepted);
            DivineNarrativeEvents.serverTick(endTick);
            h.assertTrue(memory.favorLedger.equals(remembered), "Repeated accepted event granted foundation twice");
        } finally { p.discard(); }
        h.succeed();
    }

    private static void shrine(GameTestHelper h, TestPlayer p, ResourceLocation god) {
        BlockPos pos = h.absolutePos(new BlockPos(2, 2, 2));
        h.getLevel().setBlockAndUpdate(pos, ForgeRegistries.BLOCKS.getValue(new ResourceLocation(god.getNamespace(), god.getPath() + "_statue")).defaultBlockState());
        p.setPos(pos.getX() + .5, pos.getY() + 1, pos.getZ() + .5);
        p.containerMenu = BlessingsMenu.fromStatue(27, p.getInventory(), pos);
    }
    private static final class TestPlayer extends FakePlayer {
        final DivineNarrativeData narrative = new DivineNarrativeData();
        final MainPlayerCapability.PlayerVariables variables = new MainPlayerCapability.PlayerVariables();
        final LazyOptional<DivineNarrativeData> narrativeCap = LazyOptional.of(() -> narrative);
        final LazyOptional<MainPlayerCapability.PlayerVariables> variablesCap = LazyOptional.of(() -> variables);
        TestPlayer(ServerLevel level) { super(level, new GameProfile(UUID.randomUUID(), "quest-favor-test")); }
        @Override public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
            if (cap == DivineNarrativeCapability.CAP && narrativeCap != null) return narrativeCap.cast();
            if (cap == MainPlayerCapability.PLAYER_VARIABLES_CAPABILITY && variablesCap != null) return variablesCap.cast();
            return super.getCapability(cap, side);
        }
    }
}
