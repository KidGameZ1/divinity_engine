package net.nightshade.divinity_engine.network.messages.key;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkEvent;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.network.messages.ModMessages;
import net.nightshade.divinity_engine.util.MainPlayerCapabilityHelper;
import net.nightshade.nightshade_core.util.MiscHelper;

import java.util.function.Supplier;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class BlessingSlot3Message {

	int type, pressedms;

	public BlessingSlot3Message(int type, int pressedms) {
		this.type = type;
		this.pressedms = pressedms;
	}

	public BlessingSlot3Message(FriendlyByteBuf buffer) {
		this.type = buffer.readInt();
		this.pressedms = buffer.readInt();
	}

	public static void buffer(BlessingSlot3Message message, FriendlyByteBuf buffer) {
		buffer.writeInt(message.type);
		buffer.writeInt(message.pressedms);
	}

	public static void handler(BlessingSlot3Message message, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			pressAction(context.getSender(), message.type, message.pressedms);
		});
		context.setPacketHandled(true);
	}

	public static void pressAction(Player player, int type, int pressedms) {
		Level world = player.level();
		double x = player.getX();
		double y = player.getY();
		double z = player.getZ();

		// security measure to prevent arbitrary chunk generation
		if (!world.hasChunkAt(player.blockPosition()))
			return;
		BlessingsInstance blessingSlot3 = MainPlayerCapabilityHelper.getBlessingSlot3(player);
		if (blessingSlot3 != null) {
			if (!blessingSlot3.getBlessing().isPassive()) {
				if (blessingSlot3.getCooldown() == 0) {
					if (type == 0) {
						if (blessingSlot3.getBlessing().canToggle()) {
							if (blessingSlot3.isToggled()) {
								player.displayClientMessage(Component.literal("Blessing Toggled Off: " + Component.translatable(blessingSlot3.getBlessing().getNameTranslationKey()).getString()), true);
								blessingSlot3.onToggleOff(player);
							} else {
								player.displayClientMessage(Component.literal("Blessing Toggled On: " + Component.translatable(blessingSlot3.getBlessing().getNameTranslationKey()).getString()), true);
								blessingSlot3.onToggleOn(player);
							}
						} else {
							player.displayClientMessage(Component.literal("Blessing Activated: " + Component.translatable(blessingSlot3.getBlessing().getNameTranslationKey()).getString()), true);
							blessingSlot3.onPressed(player);
						}
					} else if (type == 1) {
						if (!blessingSlot3.getBlessing().canToggle()) {
							blessingSlot3.onRelease(player, pressedms);
						}
					} else if (type == 3) {
						if (!blessingSlot3.getBlessing().canToggle())
							blessingSlot3.onHeld(player, pressedms);
					}
				} else {
					// Show cooldown remaining
					player.displayClientMessage(net.minecraft.network.chat.Component.literal("Blessing on cooldown: " +
							(MiscHelper.tickToSeconds(blessingSlot3.getCooldown())) + "s"), true);
				}
			}
		}else {
			player.displayClientMessage(net.minecraft.network.chat.Component.literal("No blessing equipped in slot 1"), true);
		}
	}


	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		ModMessages.addNetworkMessage(BlessingSlot3Message.class, BlessingSlot3Message::buffer, BlessingSlot3Message::new, BlessingSlot3Message::handler);
	}

}