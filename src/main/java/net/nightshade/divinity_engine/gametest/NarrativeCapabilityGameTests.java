package net.nightshade.divinity_engine.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestRegistry;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.nightshade.divinity_engine.network.cap.player.gods.PlayerGodsCapability;
import net.nightshade.divinity_engine.network.cap.player.narrative.DivineNarrativeCapability;
import net.nightshade.divinity_engine.network.cap.player.narrative.DivineNarrativeData;
import net.nightshade.divinity_engine.registry.dimensions.DimensionRegistry;
import net.nightshade.divinity_engine.registry.divinity.gods.GodsRegistry;
import net.nightshade.divinity_engine.util.divinity.narrative.DivineNarrativeManager;
import net.nightshade.divinity_engine.util.divinity.narrative.DivineNarrativeRegistry;
import net.nightshade.divinity_engine.util.divinity.narrative.NarrativeProgress;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Uses real attached capabilities and vanilla teleportation, not FakePlayer capability/teleport overrides. */
@Mod.EventBusSubscriber(modid = "divinity_engine", bus = Mod.EventBusSubscriber.Bus.MOD)
@PrefixGameTestTemplate(false)
public final class NarrativeCapabilityGameTests {
    private static final String SUITE = "divinity_narrative_lifecycle";

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        if (!SUITE.equals(System.getProperty("forge.enabledGameTestNamespaces", ""))) return;
        for (var method : NarrativeCapabilityGameTests.class.getDeclaredMethods())
            if (method.isAnnotationPresent(GameTest.class)) GameTestRegistry.register(method, Set.of("divinity_engine"));
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", timeoutTicks = 100)
    public static void repeatedInvalidationRetainsStoryData(GameTestHelper h) {
        TestPlayer player = new TestPlayer(h.getLevel());
        try {
            var data = seed(h, player);
            var saved = data.serializeNBT();
            for (int i = 0; i < 4; i++) {
                var handle = player.getCapability(DivineNarrativeCapability.CAP);
                player.remove(Entity.RemovalReason.CHANGED_DIMENSION);
                h.assertFalse(handle.isPresent(), "Old narrative handle must invalidate");
                h.assertTrue(DivineNarrativeCapability.get(player) == null, "Removed player exposes live data");
                player.revive();
                h.assertTrue(DivineNarrativeCapability.get(player) == data, "Revival lost the attached story data");
                h.assertTrue(data.serializeNBT().equals(saved), "Revival reset saved story progress");
            }
            h.succeed();
        } finally { player.discard(); }
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", timeoutTicks = 100)
    public static void cloneListenersPreserveProgressAfterInvalidation(GameTestHelper h) {
        TestPlayer original = new TestPlayer(h.getLevel());
        try {
            var saved = seed(h, original).serializeNBT();
            for (boolean death : new boolean[]{true, false}) {
                TestPlayer replacement = new TestPlayer(h.getLevel());
                try {
                    original.invalidateCaps();
                    MinecraftForge.EVENT_BUS.post(new PlayerEvent.Clone(replacement, original, death));
                    var copied = DivineNarrativeCapability.get(replacement);
                    h.assertTrue(copied != null && copied.serializeNBT().equals(saved), "Clone listeners lost choices, counters or upgrades");
                } finally { replacement.discard(); }
            }
            h.succeed();
        } finally { original.discard(); }
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", timeoutTicks = 200)
    public static void actualTrialDimensionReturnKeepsStoryActions(GameTestHelper h) {
        TestPlayer player = new TestPlayer(h.getLevel());
        try {
            var data = seed(h, player);
            var id = GodsRegistry.SOLARIUS.getId();
            var memory = data.get(id);
            memory.story.restore(NarrativeProgress.Stage.TRIAL, "b", 16, "", "", Set.of());
            memory.branchSnapshot = DivineNarrativeRegistry.story(id).branches().get("b");
            var destination = h.getLevel().getServer().getLevel(DimensionRegistry.GODLY_TRIALS);
            h.assertTrue(destination != null, "Trials dimension is unavailable");
            var returnPos = h.absolutePos(new net.minecraft.core.BlockPos(2, 2, 2));
            player.setPos(returnPos.getX() + .5, returnPos.getY(), returnPos.getZ() + .5);
            for (boolean victory : new boolean[]{false, true}) {
                UUID session = UUID.randomUUID();
                DivineNarrativeManager.onTrialStarted(player, id, session);
                // No terrain is changed; both transfers execute Forge's actual remove/revive lifecycle.
                player.teleportTo(destination, 8.5, 100, 8.5, 0, 0);
                h.assertTrue(DivineNarrativeCapability.get(player) == data, "Entering trials lost narrative capability");
                DivineNarrativeManager.onTrialOutcome(player, id, session, victory);
                player.teleportTo(h.getLevel(), returnPos.getX() + .5, returnPos.getY(), returnPos.getZ() + .5, 0, 0);
                h.assertTrue(DivineNarrativeCapability.get(player) == data, "Returning from trials lost narrative capability");
                h.assertTrue(DivineNarrativeManager.action(player, id, "status", "", "") == 1, "Story status stopped working after return");
                h.assertTrue(data.lastJournal != null, "Dimension return failed to rebuild the journal");
                h.assertTrue(memory.story.stage() == (victory ? NarrativeProgress.Stage.RETURN : NarrativeProgress.Stage.TRIAL),
                        "Trial outcome did not advance or preserve the quest correctly");
            }
            h.assertTrue(memory.failures == 1 && memory.victories == 1, "Trial outcome counts changed during travel");
            h.assertFalse(player.messages.stream().anyMatch(text -> text.contains("No story is registered")), "False missing-story error");
            h.succeed();
        } finally { player.serverLevel().removePlayerImmediately(player, Entity.RemovalReason.DISCARDED); }
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", timeoutTicks = 100)
    public static void missingPlayerDataDoesNotClaimStoryIsUnregistered(GameTestHelper h) {
        TestPlayer player = new TestPlayer(h.getLevel());
        try {
            var data = seed(h, player);
            var id = GodsRegistry.SOLARIUS.getId();
            player.invalidateCaps();
            h.assertTrue(DivineNarrativeManager.action(player, id, "status", "", "") == 0, "Invalid player data was accepted");
            h.assertTrue(player.messages.get(player.messages.size() - 1).contains("story data is unavailable"), "Missing data reported as an unregistered story");
            player.reviveCaps();
            h.assertTrue(DivineNarrativeCapability.get(player) == data, "Recovery replaced the player's progress");
            h.assertTrue(DivineNarrativeManager.action(player, id, "status", "", "") == 1, "Revived story remained inaccessible");
            h.succeed();
        } finally { player.discard(); }
    }

    private static DivineNarrativeData seed(GameTestHelper h, TestPlayer player) {
        var god = GodsRegistry.SOLARIUS.get();
        var bound = god.createGodDefaultInstance(); bound.setFavor(100);
        PlayerGodsCapability.load(player).updateGodInstance(bound, false);
        var data = DivineNarrativeCapability.get(player);
        h.assertTrue(data != null, "Real player must receive the production narrative provider");
        var memory = data.get(god.getRegistryName());
        memory.contacts = 2; memory.contacted = true; memory.offerings = 3; memory.highestFavor = 180;
        memory.story.restore(NarrativeProgress.Stage.DEED, "b", 7, "", "", Set.of("previous_failure"));
        data.get(GodsRegistry.NYTHEA.getId()).story.restore(NarrativeProgress.Stage.COMPLETE, "a", 0,
                "divinity_engine:nythea_a", "", Set.of());
        data.epilogues.put("test:memory", "Keep this earned lore.");
        return data;
    }

    private static final class TestPlayer extends ServerPlayer {
        final List<String> messages = new ArrayList<>();
        TestPlayer(ServerLevel level) {
            super(level.getServer(), level, new GameProfile(UUID.randomUUID(), "NarrativeLifecycle"));
            connection = new ServerGamePacketListenerImpl(server, new Connection(PacketFlow.SERVERBOUND), this) {
                @Override public void send(Packet<?> packet) { /* No connected client in a server-side GameTest. */ }
            };
        }
        @Override public void sendSystemMessage(Component message) { messages.add(message.getString()); }
    }
}
