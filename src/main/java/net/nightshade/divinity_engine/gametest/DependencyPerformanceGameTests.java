package net.nightshade.divinity_engine.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestRegistry;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.nightshade.vanguards_kit.network.cap.vanguard.posture.PostureCapability;
import net.nightshade.vanguards_kit.network.event.AspectsEvents;
import net.nightshade.vanguards_kit.network.event.custom_events.PostureRecoverEvent;

import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = "divinity_engine", bus = Mod.EventBusSubscriber.Bus.MOD)
@PrefixGameTestTemplate(false)
public final class DependencyPerformanceGameTests {
    @SubscribeEvent public static void register(RegisterGameTestsEvent event) {
        if (!"divinity_dependency_performance".equals(System.getProperty("forge.enabledGameTestNamespaces", ""))) return;
        for (var method : DependencyPerformanceGameTests.class.getDeclaredMethods())
            if (method.isAnnotationPresent(GameTest.class)) GameTestRegistry.register(method, Set.of("divinity_engine"));
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension")
    public static void vanguardDoesNotRevisitRequeuedPlayer(GameTestHelper h) {
        var player = new TestPlayer(h.getLevel());
        try {
            player.tickCount = 1;
            AspectsEvents.queuePlayer(player);
            player.aliveChecks = 0;
            AspectsEvents.onServerTick(new TickEvent.ServerTickEvent(TickEvent.Phase.END, () -> true, h.getLevel().getServer()));
            h.assertTrue(player.aliveChecks == 1, "Same player processed multiple times: " + player.aliveChecks);
            AspectsEvents.onServerTick(new TickEvent.ServerTickEvent(TickEvent.Phase.END, () -> true, h.getLevel().getServer()));
            h.assertTrue(player.aliveChecks == 2, "Player was lost from next batch");
            h.succeed();
        } finally {
            AspectsEvents.onPlayerLogout(new PlayerEvent.PlayerLoggedOutEvent(player));
            player.discard();
        }
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension")
    public static void postureSkipsFullRecoveryButRetainsRecoveryAndCancellation(GameTestHelper h) {
        var player = new TestPlayer(h.getLevel());
        var observer = new RecoveryObserver(player);
        MinecraftForge.EVENT_BUS.register(observer);
        try {
            var posture = new PostureCapability.EntityVariables();
            posture.setEntity(player);
            for (int i = 0; i < 100; i++) posture.tick(player);
            h.assertTrue(observer.calls == 0, "Full posture emitted recovery events");
            posture.setPosture(90);
            posture.tick(player);
            h.assertTrue(observer.calls == 1 && posture.getPosture() == 90.5f, "Normal recovery changed");
            observer.cancel = true;
            posture.tick(player);
            h.assertTrue(posture.getPosture() == 90.5f, "Canceled recovery changed posture");
            observer.cancel = false;
            player.setHealth(10);
            posture.setBroken(true);
            posture.tick(player);
            h.assertTrue(Math.abs(posture.getPosture() - (90.5f + player.getMaxHealth() / 9)) < .001,
                    "Broken-posture recovery rate changed");
            h.succeed();
        } finally { MinecraftForge.EVENT_BUS.unregister(observer); player.discard(); }
    }

    public static final class RecoveryObserver {
        private final ServerPlayer player;
        int calls;
        boolean cancel;
        RecoveryObserver(ServerPlayer player) { this.player = player; }
        @SubscribeEvent public void recover(PostureRecoverEvent event) {
            if (event.getEntity() == player) {
                calls++;
                if (cancel) event.setCanceled(true);
            }
        }
    }

    private static final class TestPlayer extends ServerPlayer {
        int aliveChecks;
        TestPlayer(ServerLevel level) {
            super(level.getServer(), level, new GameProfile(UUID.randomUUID(), "DependencyPerf"));
            connection = new ServerGamePacketListenerImpl(server, new Connection(PacketFlow.SERVERBOUND), this) {
                @Override public void send(Packet<?> packet) {}
            };
        }
        @Override public boolean isAlive() { aliveChecks++; return super.isAlive(); }
    }
}
