package net.nightshade.divinity_engine.network.messages.gui;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.nightshade.divinity_engine.util.divinity.blessings.aethon.PocketDomainData;
import net.nightshade.divinity_engine.util.divinity.blessings.aethon.PocketDomainManager;
import net.nightshade.divinity_engine.world.inventory.PocketDomainMenu;

import java.util.function.Supplier;

/** Numeric inputs never supply world coordinates; the open server menu identifies the owned room. */
public record PocketRoomSizeMessage(int containerId, int room, int style, int width, int height, int depth) {
    public PocketRoomSizeMessage(FriendlyByteBuf buf) {
        this(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
    }
    public static void encode(PocketRoomSizeMessage msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.containerId); buf.writeVarInt(msg.room); buf.writeVarInt(msg.style);
        buf.writeVarInt(msg.width); buf.writeVarInt(msg.height); buf.writeVarInt(msg.depth);
    }
    public static void handle(PocketRoomSizeMessage msg, Supplier<NetworkEvent.Context> supplier) {
        var ctx = supplier.get(); ctx.enqueueWork(() -> msg.apply(ctx.getSender())); ctx.setPacketHandled(true);
    }
    public boolean apply(ServerPlayer player) {
        var size = new PocketDomainData.Dimensions(width, height, depth);
        if (player == null || !size.valid() || room < -1 || room >= PocketDomainData.MAX_ROOMS
                || style < 0 || style >= PocketDomainData.Style.values().length
                || !(player.containerMenu instanceof PocketDomainMenu menu) || menu.containerId != containerId
                || !menu.stillValid(player) || !PocketDomainManager.eligible(player)) return false;
        var domain = PocketDomainManager.domain(player);
        if (room >= 0 && !menu.matchesRoom(domain, room)) return false;
        if (!PocketDomainManager.takeAction(player)) return false;
        boolean accepted = room < 0 ? PocketDomainManager.createRoom(player, PocketDomainData.Style.values()[style], size)
                : PocketDomainManager.resizeRoom(player, domain.rooms.get(room), size);
        if (accepted) PocketDomainManager.openControl(player, 0, room < 0 ? domain.rooms.size() - 1 : room);
        return accepted;
    }
}
