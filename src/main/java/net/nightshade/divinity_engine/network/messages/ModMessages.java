package net.nightshade.divinity_engine.network.messages;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.nightshade.divinity_engine.DivinityEngineMod;
import net.nightshade.divinity_engine.network.messages.cap.SyncGodsPacket;
import net.nightshade.divinity_engine.network.messages.cap.SyncMainPlayerCapabilityPacket;
import net.nightshade.divinity_engine.network.messages.gui.BlessingsButtonMessage;
import net.nightshade.divinity_engine.network.messages.gui.PlayerBlessingsButtonMessage;
import net.nightshade.divinity_engine.network.messages.key.BlessingSlot1Message;
import net.nightshade.divinity_engine.network.messages.key.BlessingSlot2Message;
import net.nightshade.divinity_engine.network.messages.key.BlessingSlot3Message;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;


public class ModMessages {
    public static SimpleChannel INSTANCE;

    private static int packetId = 0;
    private static int id() {
        return packetId++;
    }

    public static void register() {
        SimpleChannel net = NetworkRegistry.ChannelBuilder
                .named(new ResourceLocation(DivinityEngineMod.MODID, "messages"))
                .networkProtocolVersion(() -> "1.0")
                .clientAcceptedVersions(s -> true)
                .serverAcceptedVersions(s -> true)
                .simpleChannel();

        INSTANCE = net;

        INSTANCE.registerMessage(id(), SyncGodsPacket.class, SyncGodsPacket::toBytes, SyncGodsPacket::new, SyncGodsPacket::handle);
        INSTANCE.registerMessage(id(), SyncMainPlayerCapabilityPacket.class, SyncMainPlayerCapabilityPacket::buffer, SyncMainPlayerCapabilityPacket::new, SyncMainPlayerCapabilityPacket::handler);
        INSTANCE.registerMessage(id(), BlessingsButtonMessage.class, BlessingsButtonMessage::buffer, BlessingsButtonMessage::new, BlessingsButtonMessage::handler);
        INSTANCE.registerMessage(id(), BlessingSlot1Message.class, BlessingSlot1Message::buffer, BlessingSlot1Message::new, BlessingSlot1Message::handler);
        INSTANCE.registerMessage(id(), BlessingSlot2Message.class, BlessingSlot2Message::buffer, BlessingSlot2Message::new, BlessingSlot2Message::handler);
        INSTANCE.registerMessage(id(), BlessingSlot3Message.class, BlessingSlot3Message::buffer, BlessingSlot3Message::new, BlessingSlot3Message::handler);
        INSTANCE.registerMessage(id(), PlayerBlessingsButtonMessage.class, PlayerBlessingsButtonMessage::buffer, PlayerBlessingsButtonMessage::new, PlayerBlessingsButtonMessage::handler);
    }

    public static <T> void addNetworkMessage(Class<T> messageType, BiConsumer<T, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, T> decoder, BiConsumer<T, Supplier<NetworkEvent.Context>> messageConsumer) {
        INSTANCE.registerMessage(id(), messageType, encoder, decoder, messageConsumer);
    }

    public static <MSG> void sendToServer(MSG message) {
         INSTANCE.sendToServer(message);
    }

    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }
    public static <T> void toAll(T message) {
        for(ServerPlayer player : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
            INSTANCE.sendTo(message, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
        }

    }
}