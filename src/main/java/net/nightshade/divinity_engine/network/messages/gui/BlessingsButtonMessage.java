package net.nightshade.divinity_engine.network.messages.gui;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkEvent;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.network.cap.player.gods.MainPlayerCapability;
import net.nightshade.divinity_engine.network.cap.player.gods.PlayerGodsCapability;
import net.nightshade.divinity_engine.network.messages.ModMessages;
import net.nightshade.divinity_engine.util.DivinityEngineHelper;
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

		BlessingsMenu blessingsMenu = entity.containerMenu instanceof BlessingsMenu ? (BlessingsMenu) entity.containerMenu : null;

		// security measure to prevent arbitrary chunk generation
		if (!world.hasChunkAt(new BlockPos(x, y, z)))
			return;

		List<BlessingsInstance> blessingsInstances;
		if (blessingsMenu.getGod() == null) {
			blessingsInstances = GodHelper.getAllBlessingsInstances(entity);
		}else {
			blessingsInstances = GodHelper.getAllBlessingsInstancesOfGod(entity, blessingsMenu.getGod());
		}

		int pageNum = DivinityEngineHelper.getBlessingsPageNum(entity);
		int maxPages = blessingsInstances != null ? blessingsInstances.size() -1: 0;

		if (buttonID == 1) {
			if (pageNum < maxPages) {
				DivinityEngineHelper.increaseBlessingsPageNum(entity, 1);
			}else {
				DivinityEngineHelper.setBlessingsPageNum(entity, 0);
			}
		} else if (buttonID == 2) {
			if (pageNum > 0) {
				DivinityEngineHelper.decreaseBlessingsPageNum(entity, 1);
			}else if (pageNum == 0) {
				DivinityEngineHelper.setBlessingsPageNum(entity, maxPages);
			}
		} else if (buttonID >= 10 && buttonID <= 12 &&
				blessingsInstances != null && !blessingsInstances.isEmpty() &&
				pageNum >= 0 && pageNum < blessingsInstances.size()) {

			BlessingsInstance selectedBlessingInstance = blessingsInstances.get(pageNum);
			int slot = buttonID - 10;

			// Check if blessing is already equipped in this specific slot
			BlessingsInstance currentSlotBlessing = null;
			switch(slot) {
				case 0:
					currentSlotBlessing = DivinityEngineHelper.getBlessingSlot1(entity);
					break;
				case 1:
					currentSlotBlessing = DivinityEngineHelper.getBlessingSlot2(entity);
					break;
				case 2:
					currentSlotBlessing = DivinityEngineHelper.getBlessingSlot3(entity);
					break;
			}

			if (currentSlotBlessing != null &&
					currentSlotBlessing.getBlessing().equals(selectedBlessingInstance.getBlessing())) {
				// Unequip the blessing
				DivinityEngineHelper.setBlessingSlot(entity, slot, null);
				entity.displayClientMessage(Component.literal("Unequipped blessing from slot " + (slot + 1) + "."), false);
			} else if (!isAnySlotEquipped(entity, selectedBlessingInstance)) {
				// Equip the blessing if it has enough favor
				if (GodHelper.getGodOrNull(entity, selectedBlessingInstance.getBoundGod())
						.getFavor() >= selectedBlessingInstance.getBlessing().getNeededFavor()) {
					DivinityEngineHelper.setBlessingSlot(entity, slot, selectedBlessingInstance);
					entity.displayClientMessage(Component.literal("Bound blessing slot " + (slot + 1) + " to " +
							Component.translatable(selectedBlessingInstance.getBlessing().getNameTranslationKey())
									.getString() + "."), false);
				}else {
					entity.displayClientMessage(Component.literal("You are not worthy to equip blessing " +
							Component.translatable(selectedBlessingInstance.getBlessing().getNameTranslationKey()).getString()
					), false);
					GodHelper.decreaseFavor(selectedBlessingInstance.getBoundGod(), entity, 10);
				}
			}
		}

		// Sync the changes
		if (!entity.level().isClientSide()) {
			// Sync both player capabilities and gods data
			MainPlayerCapability.PlayerVariables.sync(entity);
			PlayerGodsCapability.load(entity).syncAll();
		}

	}

	private static boolean isAnySlotEquipped(Player entity, BlessingsInstance blessing) {
		BlessingsInstance slot0 = DivinityEngineHelper.getBlessingSlot1(entity);
		BlessingsInstance slot1 = DivinityEngineHelper.getBlessingSlot2(entity);
		BlessingsInstance slot2 = DivinityEngineHelper.getBlessingSlot3(entity);

		return (slot0 != null && slot0.getBlessing().equals(blessing.getBlessing())) ||
				(slot1 != null && slot1.getBlessing().equals(blessing.getBlessing())) ||
				(slot2 != null && slot2.getBlessing().equals(blessing.getBlessing()));
	}



	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		ModMessages.addNetworkMessage(BlessingsButtonMessage.class, BlessingsButtonMessage::buffer, BlessingsButtonMessage::new, BlessingsButtonMessage::handler);
	}

}