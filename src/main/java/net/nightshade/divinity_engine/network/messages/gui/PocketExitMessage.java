package net.nightshade.divinity_engine.network.messages.gui;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.nightshade.divinity_engine.util.divinity.blessings.aethon.PocketDomainManager;
import java.util.function.Supplier;

/** Safety exit for visitors and owners, including after losing the equipped blessing. */
public record PocketExitMessage() {
    public static void encode(PocketExitMessage message, FriendlyByteBuf buffer) {}
    public static PocketExitMessage decode(FriendlyByteBuf buffer) { return new PocketExitMessage(); }
    public static void handle(PocketExitMessage message, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get(); context.enqueueWork(() -> apply(context.getSender())); context.setPacketHandled(true);
    }
    public static boolean apply(ServerPlayer player) {
        return player != null && player.isAlive() && PocketDomainManager.inPocket(player)
                && PocketDomainManager.takeAction(player) && PocketDomainManager.exit(player);
    }
}
