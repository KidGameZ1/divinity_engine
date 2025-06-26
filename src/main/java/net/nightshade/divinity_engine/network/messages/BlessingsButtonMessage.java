package net.nightshade.divinity_engine.network.messages;

import net.minecraft.core.BlockPos;
import net.minecraft.data.Main;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkEvent;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.util.MainPlayerCapabilityHelper;
import net.nightshade.divinity_engine.util.divinity.gods.GodHelper;
import net.nightshade.divinity_engine.world.inventory.BlessingsMenu;

import java.util.HashMap;
import java.util.List;
import java.util.function.Supplier;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class BlessingsButtonMessage {

	private final int buttonID, x, y, z;

	public BlessingsButtonMessage(FriendlyByteBuf buffer) {
		this.buttonID = buffer.readInt();
		this.x = buffer.readInt();
		this.y = buffer.readInt();
		this.z = buffer.readInt();
	}

	public BlessingsButtonMessage(int buttonID, int x, int y, int z) {
		this.buttonID = buttonID;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public static void buffer(BlessingsButtonMessage message, FriendlyByteBuf buffer) {
		buffer.writeInt(message.buttonID);
		buffer.writeInt(message.x);
		buffer.writeInt(message.y);
		buffer.writeInt(message.z);
	}

	public static void handler(BlessingsButtonMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			Player entity = context.getSender();
			int buttonID = message.buttonID;
			int x = message.x;
			int y = message.y;
			int z = message.z;

			handleButtonAction(entity, buttonID, x, y, z);
		});
		context.setPacketHandled(true);
	}

	public static void handleButtonAction(Player entity, int buttonID, int x, int y, int z) {
		Level world = entity.level();
		HashMap guistate = BlessingsMenu.guistate;

		// security measure to prevent arbitrary chunk generation
		if (!world.hasChunkAt(new BlockPos(x, y, z)))
			return;

		List<BlessingsInstance> blessingsInstances = GodHelper.getAllBlessingsInstances(entity);
		int pageNum = MainPlayerCapabilityHelper.getBlessingsPageNum(entity);
		int maxPages = blessingsInstances != null ? blessingsInstances.size() - 1 : 0;

		if (buttonID == 1) {
			// Next page
			if (pageNum < maxPages) {
				MainPlayerCapabilityHelper.increaseBlessingsPageNum(entity, 1);
			}
		} else if (buttonID == 2) {
			// Previous page
			if (pageNum > 0) {
				MainPlayerCapabilityHelper.decreaseBlessingsPageNum(entity, 1);
			}
		} else if (buttonID >= 10 && buttonID <= 12 &&
				blessingsInstances != null && !blessingsInstances.isEmpty() &&
				pageNum >= 0 && pageNum < blessingsInstances.size()) {

			BlessingsInstance selectedBlessingInstance = blessingsInstances.get(pageNum);

			// Check if blessing is already equipped in any slot
			BlessingsInstance slot0 = MainPlayerCapabilityHelper.getBlessingSlot1(entity);
			BlessingsInstance slot1 = MainPlayerCapabilityHelper.getBlessingSlot2(entity);
			BlessingsInstance slot2 = MainPlayerCapabilityHelper.getBlessingSlot3(entity);

			if ((slot0 != null && slot0.getBlessing().equals(selectedBlessingInstance.getBlessing())) ||
					(slot1 != null && slot1.getBlessing().equals(selectedBlessingInstance.getBlessing())) ||
					(slot2 != null && slot2.getBlessing().equals(selectedBlessingInstance.getBlessing()))) {
				return; // Don't allow equipping if already in any slot
			}

			// Handle blessing slot binding (10 = slot 0, 11 = slot 1, 12 = slot 2)
			int slot = buttonID - 10;
			MainPlayerCapabilityHelper.setBlessingSlot(entity, slot, selectedBlessingInstance);
			entity.displayClientMessage(Component.literal("Bound blessing slot "+(slot+1)+" to "+
					Component.translatable(selectedBlessingInstance.getBlessing().getNameTranslationKey()).getString()+"."), false);
		}
	}



	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		ModMessages.addNetworkMessage(BlessingsButtonMessage.class, BlessingsButtonMessage::buffer, BlessingsButtonMessage::new, BlessingsButtonMessage::handler);
	}

}