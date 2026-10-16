package net.nightshade.divinity_engine.gametest;

import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestRegistry;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.nightshade.divinity_engine.network.cap.player.gods.PlayerGodsCapability;
import net.nightshade.divinity_engine.network.cap.player.gods.InternalGodsStorage;
import net.nightshade.divinity_engine.network.events.divinity.DivinityTickHandler;
import net.nightshade.divinity_engine.network.messages.cap.SyncGodsPacket;
import net.nightshade.divinity_engine.registry.divinity.blessing.BlessingsRegistry;
import net.nightshade.divinity_engine.registry.divinity.gods.GodsRegistry;
import net.nightshade.divinity_engine.util.DivinityEngineHelper;

import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = "divinity_engine", bus = Mod.EventBusSubscriber.Bus.MOD)
@PrefixGameTestTemplate(false)
public final class GodSyncGameTests {
    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        if (!"divinity_god_sync".equals(System.getProperty("forge.enabledGameTestNamespaces", ""))) return;
        for (var method : GodSyncGameTests.class.getDeclaredMethods())
            if (method.isAnnotationPresent(GameTest.class)) GameTestRegistry.register(method, Set.of("divinity_engine"));
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension")
    public static void packetDoesNotRedirtySerializedGods(GameTestHelper h) {
        var p = new TestPlayer(h.getLevel());
        try {
            var data = (InternalGodsStorage) PlayerGodsCapability.load(p);
            var god = GodsRegistry.DRAVAK.get().createGodDefaultInstance();
            god.setFavor(250);
            data.updateGodInstance(god, false);
            var blessing = god.getBlessing(BlessingsRegistry.WARLORDS_WILL.get());
            blessing.getOrCreateTag().putInt("retained", 42);
            data.updateBlessingInstance(god, blessing, false);
            var before = data.serializeNBT();
            var packet = new SyncGodsPacket(p, data, SyncGodsPacket.SyncType.CHANGES_ONLY);
            h.assertFalse(god.isDirty() || blessing.isDirty(), "Serialization dirtied an already-sent god");
            h.assertTrue(before.equals(data.serializeNBT()), "Sync changed saved favor, blessings or custom data");
            h.assertTrue(godCount(packet) == 1, "Changed god was not sent");
            h.assertTrue(godCount(new SyncGodsPacket(p, data, SyncGodsPacket.SyncType.CHANGES_ONLY)) == 0,
                    "Next update resent an unchanged god");
            data.updateBlessingInstance(god, blessing, false);
            h.assertTrue(godCount(new SyncGodsPacket(p, data, SyncGodsPacket.SyncType.CHANGES_ONLY)) == 1,
                    "A later legitimate change was lost");
            h.succeed();
        } finally { p.discard(); }
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension")
    public static void threeEquippedSlotsSendOneUpdateWithoutSlowingCooldowns(GameTestHelper h) {
        var p = new TestPlayer(h.getLevel());
        try {
            var pos = h.absolutePos(new net.minecraft.core.BlockPos(2, 3, 2));
            p.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
            // TRACKING_ENTITY_AND_SELF only sends for entities registered with ChunkMap.
            h.getLevel().addNewPlayer(p);
            var data = (InternalGodsStorage) PlayerGodsCapability.load(p);
            var dravak = GodsRegistry.DRAVAK.get().createGodDefaultInstance();
            var varun = GodsRegistry.VARUN.get().createGodDefaultInstance();
            dravak.setFavor(250); varun.setFavor(250);
            data.updateGodInstance(dravak, false); data.updateGodInstance(varun, false);
            var slots = new net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance[]{
                    dravak.getBlessing(BlessingsRegistry.WARLORDS_WILL.get()),
                    dravak.getBlessing(BlessingsRegistry.FURY_UNBOUND.get()),
                    varun.getBlessing(BlessingsRegistry.ALPHAS_CALL.get())};
            for (int i = 0; i < slots.length; i++) {
                slots[i].setCooldown(100);
                DivinityEngineHelper.setBlessingSlot(p, i, slots[i]);
            }
            p.packets = 0;
            p.tickCount = 2;
            DivinityTickHandler.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, p));
            h.assertTrue(p.packets == 1, "Equipped slots did not share one update packet: " + p.packets);
            for (var slot : slots) h.assertTrue(slot.getCooldown() == 98, "Cooldown timing changed");
            h.assertTrue(data.getDirtyContactedGods().isEmpty(), "Tick left already-synced gods dirty");
            h.succeed();
        } finally { h.getLevel().removePlayerImmediately(p, net.minecraft.world.entity.Entity.RemovalReason.DISCARDED); }
    }

    private static int godCount(SyncGodsPacket packet) {
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            packet.toBytes(buffer);
            buffer.readInt(); buffer.readEnum(SyncGodsPacket.SyncType.class);
            return buffer.readNbt().getList("contacted_gods", 10).size();
        } finally { buffer.release(); }
    }

    private static final class TestPlayer extends ServerPlayer {
        int packets;
        TestPlayer(ServerLevel level) {
            super(level.getServer(), level, new GameProfile(UUID.randomUUID(), "GodSyncTest"));
            connection = new ServerGamePacketListenerImpl(server, new Connection(PacketFlow.SERVERBOUND), this) {
                @Override public void send(Packet<?> packet) { packets++; }
            };
        }
    }
}
