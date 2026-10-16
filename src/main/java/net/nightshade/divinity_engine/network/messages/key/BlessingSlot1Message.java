package net.nightshade.divinity_engine.network.messages.key;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.util.DivinityEngineHelper;
import net.nightshade.divinity_engine.util.divinity.gods.GodHelper;
import net.nightshade.divinity_engine.util.divinity.narrative.BlessingSpecializations;

import java.util.function.Supplier;

/** Registered once by ModMessages, not again by a common-setup subscriber. */
public class BlessingSlot1Message {
    final int type;
    final int pressedms;

    public BlessingSlot1Message(int type, int pressedms) {
        this.type = type;
        this.pressedms = pressedms;
    }

    public BlessingSlot1Message(FriendlyByteBuf buffer) {
        this(buffer.readInt(), buffer.readInt());
    }

    public static void buffer(BlessingSlot1Message message, FriendlyByteBuf buffer) {
        buffer.writeInt(message.type);
        buffer.writeInt(message.pressedms);
    }

    public static void handler(BlessingSlot1Message message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> pressAction(context.getSender(), message.type, message.pressedms));
        context.setPacketHandled(true);
    }

    public static void pressAction(Player player, int type, int pressedms) {
        BlessingSlot1Message.dispatch(player, 0, type, pressedms);
    }

    static void dispatch(Player player, int slot, int type, int pressedms) {
        if (player != null && net.nightshade.divinity_engine.util.divinity.innerworld.CommunionManager.busy(player)) return;
        if (!(player instanceof ServerPlayer) || !player.isAlive() || player.isSpectator()
                || !player.level().hasChunkAt(player.blockPosition()) || (type != 0 && type != 1 && type != 3)) return;
        BlessingsInstance blessing = DivinityEngineHelper.getBlessingInSlot(player, slot);
        if (blessing == null) {
            if (type == 0) player.displayClientMessage(Component.literal("No blessing equipped in slot " + (slot + 1)), true);
            return;
        }
        if (!blessing.getBlessing().isActive()) return;
        if (type == 0 && player.isShiftKeyDown()
                && blessing.getBlessing() instanceof net.nightshade.divinity_engine.divinity.blessing.aethon.PocketDimension
                && net.nightshade.divinity_engine.util.divinity.blessings.aethon.PocketDomainManager.inPocket(player)) {
            net.nightshade.divinity_engine.util.divinity.blessings.aethon.PocketDomainManager.exit((ServerPlayer) player);
            return;
        }
        // Release must clear the input latch even when the press has already started a cooldown.
        if (type == 1) {
            if (!blessing.getBlessing().canToggle()) blessing.onRelease(player, 0);
            GodHelper.updateBlessingInstance(player, blessing);
            return;
        }
        if (!BlessingSpecializations.isEligible(player, blessing)) {
            if (type == 0) player.displayClientMessage(Component.literal(
                    net.nightshade.divinity_engine.util.divinity.gods.DivineRelationship.requirement(blessing)), true);
            return;
        }
        if (blessing.isOnCooldown()) {
            if (type == 0) player.displayClientMessage(Component.literal("Blessing on cooldown: "
                    + (int) Math.ceil(blessing.getCooldown() / 20.0) + "s"), true);
            return;
        }
        if (type == 0) {
            if (blessing.getBlessing().canToggle()) {
                if (blessing.isToggled()) blessing.onToggleOff(player);
                else blessing.onToggleOn(player);
                player.displayClientMessage(Component.literal("Blessing Toggled " + (blessing.isToggled() ? "On: " : "Off: "))
                        .append(Component.translatable(blessing.getBlessing().getNameTranslationKey())), true);
            } else {
                blessing.onPressed(player);
            }
        } else if (!blessing.getBlessing().canToggle()) {
            blessing.onHeld(player, 0);
        }
        GodHelper.updateBlessingInstance(player, blessing);
    }
}
