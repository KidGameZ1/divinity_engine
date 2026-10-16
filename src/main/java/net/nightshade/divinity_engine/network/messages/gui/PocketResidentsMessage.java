package net.nightshade.divinity_engine.network.messages.gui;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.nightshade.divinity_engine.util.divinity.blessings.aethon.PocketDomainManager;
import net.nightshade.divinity_engine.world.inventory.PocketDomainMenu;

import java.util.UUID;
import java.util.function.Supplier;

/** Bounded resident pages and stable UUID actions, independent of vanilla's byte-sized button IDs. */
public record PocketResidentsMessage(int containerId, int page, UUID release) {
    public PocketResidentsMessage(FriendlyByteBuf buffer) {
        this(buffer.readVarInt(), buffer.readVarInt(), buffer.readBoolean() ? buffer.readUUID() : null);
    }
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(containerId); buffer.writeVarInt(page); buffer.writeBoolean(release != null);
        if (release != null) buffer.writeUUID(release);
    }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> apply(context.getSender())); context.setPacketHandled(true);
    }
    public boolean apply(ServerPlayer player) {
        if (player == null || !(player.containerMenu instanceof PocketDomainMenu menu) || menu.containerId != containerId
                || !menu.snapshot.hasUUID("Owner") || !menu.snapshot.getUUID("Owner").equals(player.getUUID())
                || !menu.stillValid(player) || !PocketDomainManager.canManage(player)
                || page < 0 || !PocketDomainManager.takeAction(player)) return false;
        if (release != null) {
            boolean listed = false;
            for (int i = 0; i < menu.residents().size(); i++) if (release.equals(menu.residents().getCompound(i).getUUID("Id"))) listed = true;
            if (!listed || !PocketDomainManager.release(player, release)) return false;
        }
        PocketDomainManager.openControl(player, 3, PocketDomainManager.domain(player).selected, page);
        return true;
    }
}
