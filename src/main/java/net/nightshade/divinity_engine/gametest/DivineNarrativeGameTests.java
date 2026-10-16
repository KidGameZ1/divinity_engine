package net.nightshade.divinity_engine.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.entity.living.ShieldBlockEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;
import net.nightshade.divinity_engine.network.cap.player.gods.PlayerGodsCapability;
import net.nightshade.divinity_engine.network.cap.player.narrative.*;
import net.nightshade.divinity_engine.network.events.divinity.narrative.*;
import net.nightshade.divinity_engine.network.events.divinity.gods.worship_events.PrayEvent;
import net.nightshade.divinity_engine.block.StatueBlock;
import net.nightshade.divinity_engine.api.narrative.StoryObjectiveType;
import net.nightshade.divinity_engine.api.narrative.BlessingSpecialization;
import net.nightshade.divinity_engine.api.narrative.DivineStoryDefinition;
import net.nightshade.divinity_engine.registry.divinity.gods.GodsRegistry;
import net.nightshade.divinity_engine.divinity.gods.FavorTiers;
import net.nightshade.divinity_engine.util.divinity.narrative.*;
import java.util.*;
import java.util.function.Consumer;

@GameTestHolder("divinity_engine")
@PrefixGameTestTemplate(false)
public final class DivineNarrativeGameTests {
    @GameTest(template = "pocket_dimension")
    public static void storyRequestsRequireFavorableTier(GameTestHelper h) {
        DivineNarrativeRegistry.initialize();
        var id = DivineNarrativeRegistry.id("solarius");
        var god = GodsRegistry.GODS_REGISTRY.get().getValue(id);
        h.assertTrue(FavorTiers.Favorable.getMinFavor() == 50, "Current Favorable threshold is 50, not stale javadoc value 1");
        for (int favor : new int[] {-250, -1, 0, 1, 49, 50, 100, 176, 250}) {
            TestPlayer player = new TestPlayer(h.getLevel());
            try {
                placeAndContact(h, player, id);
                PlayerGodsCapability.load(player).getContactedGod(god).orElseThrow().setFavor(favor);
                boolean eligible = favor >= FavorTiers.Favorable.getMinFavor();
                player.messages.clear();
                DivineNarrativeManager.onStatueOpened(player, god);
                var dialogue = net.nightshade.divinity_engine.util.divinity.narrative.ShrineStoryDialogue.build(player, DivineNarrativeRegistry.story(id).id());
                h.assertTrue(!dialogue.getList("actions", 10).isEmpty() == eligible,
                    "Statue story invitation follows Favorable threshold at favor " + favor);
                h.assertTrue(DivineNarrativeManager.action(player, id, "request", "", "") == (eligible ? 1 : 0),
                    "Server request follows Favorable threshold at favor " + favor);
                h.assertTrue(player.narrative.get(id).story.stage() == (eligible ? NarrativeProgress.Stage.OFFERING : NarrativeProgress.Stage.DORMANT),
                    "Rejected request never starts a chapter at favor " + favor);
            } finally { player.discard(); }
        }
        TestPlayer uncontacted = new TestPlayer(h.getLevel());
        try {
            placeAndContact(h, uncontacted, id);
            PlayerGodsCapability.load(uncontacted).getContactedGods().clear();
            uncontacted.messages.clear();
            DivineNarrativeManager.onStatueOpened(uncontacted, god);
            h.assertTrue(uncontacted.messages.isEmpty(), "Uncontacted statue safely offers no story");
            h.assertTrue(DivineNarrativeManager.action(uncontacted, id, "request", "", "") == 0, "Uncontacted story request rejected");
        } finally { uncontacted.discard(); }
        h.succeed();
    }

    @GameTest(template = "pocket_dimension")
    public static void all36PathsPersistAndRemainIndependent(GameTestHelper h) {
        DivineNarrativeRegistry.initialize();
        DivineNarrativeData first = new DivineNarrativeData(), second = new DivineNarrativeData();
        for (var def : DivineNarrativeRegistry.stories()) for (String branch : List.of("a", "b")) {
            var id = def.godId(); first.adminReset(id); var m = first.get(id); var path = def.branches().get(branch);
            h.assertTrue(m.story.request(true) && m.story.acceptOffering(true) && m.story.confirm(branch, true, true), "Story start");
            m.branchSnapshot = path; m.highestFavor = 250; m.abandonments = 2; m.failures = 3;
            m.story.advance(path.target(), path.target(), true); m.story.trialStarted("fresh");
            h.assertTrue(m.story.trialOutcome("fresh", true), "Victory");
            h.assertTrue(m.story.finish(path.reward().id().toString(), true), "Ending");
            DivineNarrativeData restored = new DivineNarrativeData(); restored.deserializeNBT(first.serializeNBT());
            var saved = restored.get(id);
            h.assertTrue(saved.story.specialization().equals(path.reward().id().toString()), "Permanent specialization NBT");
            h.assertTrue(saved.branchSnapshot.equals(path), "Chosen branch definition snapshot persists");
            h.assertTrue(saved.abandonments == 2 && saved.failures == 3 && saved.highestFavor == 250, "Memories survive");
            h.assertFalse(saved.story.finish(path.reward().id().toString(), true), "One-time reward after reconnect");
            h.assertTrue(second.get(id).story.stage() == NarrativeProgress.Stage.DORMANT, "Independent multiplayer memory");
        }
        DivineNarrativeData oldSave = new DivineNarrativeData(); oldSave.deserializeNBT(new net.minecraft.nbt.CompoundTag());
        h.assertTrue(oldSave.memories().isEmpty(), "Old saves initialize empty");
        h.succeed();
    }

    @GameTest(template = "pocket_dimension")
    public static void exactDonationsAndCanceledTurnIns(GameTestHelper h) {
        DivineNarrativeRegistry.initialize();
        TestPlayer player = new TestPlayer(h.getLevel());
        try {
            for (var def : DivineNarrativeRegistry.stories()) for (String branch : List.of("a", "b")) {
                var path = def.branches().get(branch);
                if (path.donationItem() == null) continue;
                placeAndContact(h, player, def.godId());
                player.narrative.adminReset(def.godId()); var m = player.narrative.get(def.godId());
                m.story.request(true); m.story.acceptOffering(true); m.story.confirm(branch, true, true); m.branchSnapshot = path;
                var item = ForgeRegistries.ITEMS.getValue(path.donationItem());
                player.getInventory().clearContent();
                player.getInventory().setItem(0, new ItemStack(item, path.target() + 2));
                Consumer<NarrativeDonationEvent> cancel = event -> { if (event.player == player) event.setCanceled(true); };
                MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, NarrativeDonationEvent.class, cancel);
                try {
                    h.assertTrue(DivineNarrativeManager.action(player, def.godId(), "donate", "", "") == 0, "Canceled donation rejected");
                    h.assertTrue(player.getInventory().getItem(0).getCount() == path.target() + 2 && m.story.progress() == 0, "Canceled donation consumes nothing");
                } finally { MinecraftForge.EVENT_BUS.unregister(cancel); }
                h.assertTrue(DivineNarrativeManager.action(player, def.godId(), "donate", "", "") == 1, "Donation accepted");
                h.assertTrue(player.getInventory().getItem(0).getCount() == 2, "Exact amount consumed");
                h.assertTrue(DivineNarrativeManager.action(player, def.godId(), "donate", "", "") == 0, "Repeated donation rejected");
                h.assertTrue(player.getInventory().getItem(0).getCount() == 2, "Repeated action cannot consume extras");
            }
        } finally { player.discard(); }
        h.succeed();
    }

    @GameTest(template = "pocket_dimension")
    public static void shrineConfirmationAndReentrantDonation(GameTestHelper h) {
        DivineNarrativeRegistry.initialize(); TestPlayer player = new TestPlayer(h.getLevel()); var id = DivineNarrativeRegistry.id("solarius");
        placeAndContact(h, player, id);
        try {
            var m = player.narrative.get(id); m.story.request(true); m.story.acceptOffering(true);
            h.assertTrue(DivineNarrativeManager.action(player, id, "confirm", "b", "not-previewed") == 0, "Forged confirmation");
            DivineNarrativeManager.action(player, id, "preview", "b", "");
            String token = m.preview.substring(0, 36);
            player.setPos(player.getX() + 10, player.getY(), player.getZ());
            h.assertTrue(DivineNarrativeManager.action(player, id, "confirm", "b", token) == 0, "Remote confirmation");
            placeAndContact(h, player, id);
            h.assertTrue(DivineNarrativeManager.action(player, id, "confirm", "b", token) == 1, "Loaded shrine confirmation");
            h.assertTrue(DivineNarrativeManager.action(player, id, "confirm", "a", token) == 0, "Permanent opposite branch blocked");
            player.getInventory().setItem(0, new ItemStack(Items.BREAD, 20));
            Consumer<NarrativeDonationEvent> reentrant = event -> { if (event.player == player) m.story.advance(1, 16, true); };
            MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, NarrativeDonationEvent.class, reentrant);
            try {
                h.assertTrue(DivineNarrativeManager.action(player, id, "donate", "", "") == 0, "Stale transaction rejected after callbacks");
                h.assertTrue(player.getInventory().getItem(0).getCount() == 20, "Reentrancy cannot consume stale count");
            } finally { MinecraftForge.EVENT_BUS.unregister(reentrant); }
        } finally { player.discard(); }
        h.succeed();
    }

    @GameTest(template = "pocket_dimension", timeoutTicks = 40)
    public static void successfulAndCanceledShieldEvents(GameTestHelper h) {
        DivineNarrativeRegistry.initialize(); TestPlayer player = new TestPlayer(h.getLevel()); var id = DivineNarrativeRegistry.id("dravak");
        placeAndContact(h, player, id);
        var m = player.narrative.get(id); m.story.request(true); m.story.acceptOffering(true); m.story.confirm("b", true, true);
        Zombie enemy = new Zombie(EntityType.ZOMBIE, h.getLevel());
        ShieldBlockEvent canceled = new ShieldBlockEvent(player, h.getLevel().damageSources().mobAttack(enemy), 4);
        DivineNarrativeEvents.shield(canceled); canceled.setCanceled(true);
        ShieldBlockEvent accepted = new ShieldBlockEvent(player, h.getLevel().damageSources().mobAttack(enemy), 4);
        DivineNarrativeEvents.shield(accepted);
        h.runAfterDelay(2, () -> {
            h.assertTrue(m.story.progress() == 1, "Only successful block event counts after all cancellation callbacks");
            player.discard(); enemy.discard(); h.succeed();
        });
    }

    @GameTest(template = "pocket_dimension")
    public static void interruptedAttemptsDoNotEraseCompletedChapters(GameTestHelper h) {
        DivineNarrativeData data = new DivineNarrativeData(); var id = DivineNarrativeRegistry.id("nythea"); var m = data.get(id);
        m.story.request(true); m.story.acceptOffering(true); m.story.confirm("a", true, true); m.resettableAttempt = true;
        m.story.advance(600, 1200, true);
        DivineNarrativeData reconnect = new DivineNarrativeData(); reconnect.deserializeNBT(data.serializeNBT());
        h.assertTrue(reconnect.get(id).story.progress() == 0 && reconnect.get(id).story.branch().equals("a"), "Logout resets attempt, not permanent branch");
        m.story.advance(600, 1200, true);
        reconnect.deserializeNBT(data.serializeNBT());
        h.assertTrue(reconnect.get(id).story.stage() == NarrativeProgress.Stage.TRIAL, "Completed chapter retained");
        h.succeed();
    }

    @GameTest(template = "pocket_dimension")
    public static void prayerRevalidatesListenerMutations(GameTestHelper h) {
        TestPlayer player = new TestPlayer(h.getLevel()); var id = DivineNarrativeRegistry.id("solarius");
        BlockPos shrine = h.absolutePos(new BlockPos(2, 2, 2));
        try {
            for (int mode = 0; mode < 3; mode++) {
                placeAndContact(h, player, id); player.setShiftKeyDown(true);
                var god = GodsRegistry.GODS_REGISTRY.get().getValue(id);
                var instance = PlayerGodsCapability.load(player).getContactedGod(god).orElseThrow();
                StatueBlock statue = (StatueBlock) h.getLevel().getBlockState(shrine).getBlock();
                int mutation = mode;
                Consumer<PrayEvent> listener = event -> {
                    if (event.getLiving() != player) return;
                    if (mutation == 0) PlayerGodsCapability.load(player).getContactedGods().clear();
                    if (mutation == 1) h.getLevel().setBlock(shrine, Blocks.AIR.defaultBlockState(), 3);
                    if (mutation == 2) player.levelOverride = h.getLevel().getServer().getLevel(Level.NETHER);
                };
                MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, PrayEvent.class, listener);
                try {
                    h.assertFalse(statue.tryPray(h.getLevel(), shrine, player, instance, 120), "Prayer rejects callback mutation " + mode);
                    h.assertTrue(instance.getFavor() == 100, "Stale god instance must not receive prayer favor");
                } finally { MinecraftForge.EVENT_BUS.unregister(listener); player.levelOverride = null; }
            }
        } finally { player.discard(); }
        h.succeed();
    }

    @GameTest(template = "pocket_dimension")
    public static void unavailableAddonSnapshotSurvivesAndResumes(GameTestHelper h) {
        DivineNarrativeRegistry.initialize();
        var id = DivineNarrativeRegistry.id("solarius"); var def = DivineNarrativeRegistry.story(id);
        var missingType = new ResourceLocation("narrative_test", "temporarily_missing_" + UUID.randomUUID());
        var json = NarrativeDefinitionJson.branchJson(def.branches().get("b"));
        json.addProperty("objective", missingType.toString()); json.addProperty("target", 37);
        String original = json.toString();
        DivineNarrativeData data = new DivineNarrativeData(); var m = data.get(id);
        m.story.request(true); m.story.acceptOffering(true); m.story.confirm("b", true, true);
        var tag = data.serializeNBT(); tag.getCompound("gods").getCompound(id.toString()).putString("branch_snapshot", original);
        data.deserializeNBT(tag); m = data.get(id);
        h.assertTrue(m.hasUnresolvedSnapshot(), "Missing addon objective pauses snapshot resolution");
        h.assertTrue(DivineNarrativeManager.path(def, m) == null, "Never substitute the current base-god branch");
        h.assertTrue(data.serializeNBT().getCompound("gods").getCompound(id.toString()).getString("branch_snapshot").equals(original), "Save preserves exact unresolved snapshot");
        data.deserializeNBT(data.serializeNBT());
        h.assertTrue(data.get(id).story.branch().equals("b"), "Second reconnect preserves permanent branch");
        StoryObjectiveType.register(new StoryObjectiveType(missingType, false, 0));
        var recovered = DivineNarrativeManager.path(def, data.get(id));
        h.assertTrue(recovered != null && recovered.objective().id().equals(missingType) && recovered.target() == 37, "Returning addon resolves original deed, not current target 16");
        h.succeed();
    }

    @GameTest(template = "pocket_dimension", timeoutTicks = 40)
    public static void queuedKillsCannotCrossInterruptions(GameTestHelper h) {
        DivineNarrativeRegistry.initialize(); var id = DivineNarrativeRegistry.id("kairoth");
        TestPlayer[] players = {new TestPlayer(h.getLevel()), new TestPlayer(h.getLevel()), new TestPlayer(h.getLevel())};
        for (int index = 0; index < players.length; index++) {
            TestPlayer player = players[index]; placeAndContact(h, player, id);
            var m = player.narrative.get(id); m.story.request(true); m.story.acceptOffering(true); m.story.confirm("b", true, true);
            m.branchSnapshot = DivineNarrativeRegistry.story(id).branches().get("b"); m.resettableAttempt = true;
            Zombie victim = new Zombie(EntityType.ZOMBIE, h.getLevel()); victim.setHealth(0);
            DivineNarrativeEvents.death(new LivingDeathEvent(victim, h.getLevel().damageSources().playerAttack(player)));
            long generation = m.generation();
            if (index == 0) DivineNarrativeEvents.dimension(new PlayerEvent.PlayerChangedDimensionEvent(player, Level.OVERWORLD, Level.NETHER));
            if (index == 1) DivineNarrativeEvents.logout(new PlayerEvent.PlayerLoggedOutEvent(player));
            if (index < 2) h.assertTrue(m.generation() != generation, "Interruption invalidates captured generation");
        }
        h.runAfterDelay(2, () -> {
            for (int index = 0; index < players.length; index++) {
                var m = players[index].narrative.get(id);
                h.assertTrue(m.story.progress() == (index == 2 ? 1 : 0), "Only uninterrupted player's queued kill counts");
                if (index < 2) h.assertTrue(m.attemptStart == -1, "Stale kill cannot restart timed attempt");
                players[index].discard();
            }
            h.succeed();
        });
    }

    @GameTest(template = "pocket_dimension")
    public static void donationCommitsBeforeInventoryCallbacks(GameTestHelper h) {
        DivineNarrativeRegistry.initialize(); TestPlayer player = new TestPlayer(h.getLevel()); var id = DivineNarrativeRegistry.id("solarius");
        placeAndContact(h, player, id); var m = player.narrative.get(id);
        m.story.request(true); m.story.acceptOffering(true); m.story.confirm("b", true, true);
        m.branchSnapshot = DivineNarrativeRegistry.story(id).branches().get("b");
        player.getInventory().setItem(0, new ItemStack(Items.BREAD, 34));
        int[] duplicate = {-1}; boolean[] committed = {false};
        player.containerMenu = new AbstractContainerMenu(null, 7) {
            @Override public ItemStack quickMoveStack(Player owner, int slot) { return ItemStack.EMPTY; }
            @Override public boolean stillValid(Player owner) { return true; }
            @Override public void broadcastChanges() {
                committed[0] = m.story.stage() == NarrativeProgress.Stage.TRIAL;
                duplicate[0] = DivineNarrativeManager.action(player, id, "donate", "", "");
                PlayerGodsCapability.load(player).getContactedGods().clear();
                m.interrupt();
            }
        };
        try {
            h.assertTrue(DivineNarrativeManager.action(player, id, "donate", "", "") == 1, "Donation commits successfully");
            h.assertTrue(committed[0] && duplicate[0] == 0, "Broadcast sees committed chapter and cannot reenter donation");
            h.assertTrue(player.getInventory().getItem(0).getCount() == 18, "Exactly one contribution consumed");
            h.assertTrue(m.story.stage() == NarrativeProgress.Stage.TRIAL && m.story.progress() == 16, "Contact loss inside broadcast cannot erase committed credit");
            h.assertFalse(m.donationInProgress, "Transaction guard released");
        } finally { player.discard(); }
        h.succeed();
    }

    @GameTest(template = "pocket_dimension")
    public static void customRewardIdsRemainEarnedAfterReconnect(GameTestHelper h) {
        DivineNarrativeRegistry.initialize();
        TestPlayer player = new TestPlayer(h.getLevel()), other = new TestPlayer(h.getLevel());
        var god = DivineNarrativeRegistry.id("solarius");
        var rewardId = new ResourceLocation("example_pantheon", "dawn_shelter_veteran");
        var base = DivineNarrativeRegistry.story(god); var original = base.branches().get("a");
        var custom = new DivineStoryDefinition.Branch(original.name(), original.objective(), original.condition(), original.target(),
            original.donationItem(), original.deed(),
            BlessingSpecialization.builder(rewardId).blessing(original.reward().blessingId()).description("An addon-defined permanent reward").build(), original.ending());
        var definition = DivineStoryDefinition.builder(god).title(base.title()).request(base.request())
            .branch("a", custom).branch("b", base.branches().get("b")).build();
        try {
            var m = player.narrative.get(god); m.branchSnapshot = definition.branches().get("a");
            m.story.request(true); m.story.acceptOffering(true); m.story.confirm("a", true, true);
            m.story.advance(custom.target(), custom.target(), true); m.story.trialStarted("custom-trial"); m.story.trialOutcome("custom-trial", true);
            h.assertFalse(DivineNarrativeManager.hasSpecialization(player, rewardId), "Chosen but unearned custom reward");
            m.story.finish(rewardId.toString(), true);
            h.assertTrue(DivineNarrativeManager.hasSpecialization(player, god, "a"), "Branch query resolves arbitrary saved reward ID");
            h.assertTrue(DivineNarrativeManager.hasSpecialization(player, rewardId), "Generic addon reward query");
            h.assertFalse(DivineNarrativeManager.hasSpecialization(player, god, "b"), "Opposite branch remains unearned");
            h.assertFalse(DivineNarrativeManager.hasSpecialization(player, DivineNarrativeRegistry.id("solarius_a")), "No invented convention-based reward");
            h.assertFalse(DivineNarrativeManager.hasSpecialization(other, rewardId), "Reward remains player-specific");
            player.narrative.deserializeNBT(player.narrative.serializeNBT());
            h.assertTrue(DivineNarrativeManager.hasSpecialization(player, god, "a") && DivineNarrativeManager.hasSpecialization(player, rewardId), "Custom reward survives reconnect");
            h.assertTrue(player.narrative.get(god).story.specialization().equals(rewardId.toString()), "Saved earned ID is unchanged");
            var json = new com.google.gson.JsonObject();
            json.addProperty("god", god.toString()); json.addProperty("title", base.title()); json.addProperty("request", base.request());
            var branches = new com.google.gson.JsonObject();
            branches.add("a", NarrativeDefinitionJson.branchJson(custom)); branches.add("b", NarrativeDefinitionJson.branchJson(base.branches().get("b")));
            json.add("branches", branches);
            h.assertTrue(NarrativeDefinitionJson.story(json).branches().get("a").reward().id().equals(rewardId), "Datapacks accept the same reward IDs as the public builder");
        } finally { player.discard(); other.discard(); }
        h.succeed();
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
    private static final class TestPlayer extends FakePlayer {
        final DivineNarrativeData narrative = new DivineNarrativeData();
        final List<String> messages = new ArrayList<>();
        private final LazyOptional<DivineNarrativeData> cap = LazyOptional.of(() -> narrative);
        ServerLevel levelOverride;
        TestPlayer(ServerLevel level) { super(level, new GameProfile(UUID.randomUUID(), "narrative-test")); }
        @Override public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction side) {
            if (capability == DivineNarrativeCapability.CAP && cap != null) return cap.cast();
            return super.getCapability(capability, side);
        }
        @Override public ServerLevel serverLevel() { return levelOverride == null ? super.serverLevel() : levelOverride; }
        @Override public void sendSystemMessage(Component message) { messages.add(message.getString()); }
    }
}
