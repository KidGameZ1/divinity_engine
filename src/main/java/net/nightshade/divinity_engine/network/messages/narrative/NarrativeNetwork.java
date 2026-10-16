package net.nightshade.divinity_engine.network.messages.narrative;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
import java.util.Optional;
import java.util.function.Supplier;

/** Versioned narrative sync and validated shrine-menu requests. */
public final class NarrativeNetwork {
    private static SimpleChannel channel;
    public static void register() {
        if (channel != null) return;
        channel = NetworkRegistry.newSimpleChannel(new ResourceLocation("divinity_engine", "narrative"), () -> "5", "5"::equals, "5"::equals);
        channel.registerMessage(0, Snapshot.class, Snapshot::encode, Snapshot::decode, Snapshot::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        channel.registerMessage(1, QuestMenuAction.class, QuestMenuAction::encode, QuestMenuAction::decode, QuestMenuAction::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        channel.registerMessage(2, Refresh.class, (packet, buffer) -> {}, buffer -> new Refresh(), Refresh::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        channel.registerMessage(3, Dialogue.class, Dialogue::encode, Dialogue::decode, Dialogue::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }
    public static void request(QuestMenuAction action) { if (channel != null) channel.sendToServer(action); }
    public static void requestFull() { if (channel != null) channel.sendToServer(new Refresh()); }
    public static void dialogue(ServerPlayer player, int containerId, CompoundTag data) {
        if (channel != null && !(player instanceof net.minecraftforge.common.util.FakePlayer))
            channel.send(PacketDistributor.PLAYER.with(() -> player), new Dialogue(containerId, data.copy()));
    }
    public record Dialogue(int containerId, CompoundTag data) {
        static void encode(Dialogue packet, FriendlyByteBuf buffer) { buffer.writeVarInt(packet.containerId); buffer.writeNbt(packet.data); }
        static Dialogue decode(FriendlyByteBuf buffer) {
            int id = buffer.readVarInt(); CompoundTag data = buffer.readNbt();
            return new Dialogue(id, data == null ? new CompoundTag() : data);
        }
        static void handle(Dialogue packet, Supplier<NetworkEvent.Context> supplier) {
            var context = supplier.get();
            context.enqueueWork(() -> net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> net.nightshade.divinity_engine.client.screen.ShrineStoryScreen.receive(packet.containerId, packet.data)));
            context.setPacketHandled(true);
        }
    }
    public record Refresh() {
        static void handle(Refresh packet, Supplier<NetworkEvent.Context> supplier) {
            var context = supplier.get();
            context.enqueueWork(() -> {
                var player = context.getSender();
                if (player == null) return;
                var data = net.nightshade.divinity_engine.network.cap.player.narrative.DivineNarrativeCapability.get(player);
                long now = player.server.overworld().getGameTime();
                if (data == null || now < data.nextJournalRequest) return;
                data.nextJournalRequest = now + 40;
                net.nightshade.divinity_engine.util.divinity.narrative.DivineNarrativeManager.sync(player, true);
            });
            context.setPacketHandled(true);
        }
    }
    public static void sync(ServerPlayer player, CompoundTag tag) {
        if (channel != null && !(player instanceof net.minecraftforge.common.util.FakePlayer))
            channel.send(PacketDistributor.PLAYER.with(() -> player), new Snapshot(tag.copy()));
    }
    public record Snapshot(CompoundTag data) {
        static void encode(Snapshot packet, FriendlyByteBuf buffer) { buffer.writeNbt(packet.data); }
        static Snapshot decode(FriendlyByteBuf buffer) { CompoundTag data = buffer.readNbt(); return new Snapshot(data == null ? new CompoundTag() : data); }
        static void handle(Snapshot packet, Supplier<NetworkEvent.Context> supplier) {
            var context = supplier.get();
            context.enqueueWork(() -> net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> ClientNarrativeSnapshot.apply(packet.data)));
            context.setPacketHandled(true);
        }
    }
}
