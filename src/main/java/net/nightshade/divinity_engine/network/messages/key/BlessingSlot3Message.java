package net.nightshade.divinity_engine.network.messages.key;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Registered once by ModMessages, not again by a common-setup subscriber. */
public class BlessingSlot3Message {
    final int type;
    final int pressedms;

    public BlessingSlot3Message(int type, int pressedms) {
        this.type = type;
        this.pressedms = pressedms;
    }

    public BlessingSlot3Message(FriendlyByteBuf buffer) {
        this(buffer.readInt(), buffer.readInt());
    }

    public static void buffer(BlessingSlot3Message message, FriendlyByteBuf buffer) {
        buffer.writeInt(message.type);
        buffer.writeInt(message.pressedms);
    }

    public static void handler(BlessingSlot3Message message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> pressAction(context.getSender(), message.type, message.pressedms));
        context.setPacketHandled(true);
    }

    public static void pressAction(Player player, int type, int pressedms) {
        BlessingSlot1Message.dispatch(player, 2, type, pressedms);
    }
}
