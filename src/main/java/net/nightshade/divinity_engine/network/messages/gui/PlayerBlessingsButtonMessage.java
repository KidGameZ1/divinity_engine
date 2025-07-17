package net.nightshade.divinity_engine.network.messages.gui;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.network.cap.player.gods.MainPlayerCapability;
import net.nightshade.divinity_engine.network.cap.player.gods.PlayerGodsCapability;
import net.nightshade.divinity_engine.network.messages.ModMessages;
import net.nightshade.divinity_engine.util.DivinityEngineHelper;
import net.nightshade.divinity_engine.util.divinity.gods.GodHelper;
import net.nightshade.divinity_engine.world.inventory.BlessingsMenu;

import java.util.HashMap;
import java.util.function.Supplier;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class PlayerBlessingsButtonMessage {

	private final int buttonID, x, y, z;

	public PlayerBlessingsButtonMessage(FriendlyByteBuf buffer) {
		this.buttonID = buffer.readInt();
		this.x = buffer.readInt();
		this.y = buffer.readInt();
		this.z = buffer.readInt();
	}

	public PlayerBlessingsButtonMessage(int buttonID, int x, int y, int z) {
		this.buttonID = buttonID;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public static void buffer(PlayerBlessingsButtonMessage message, FriendlyByteBuf buffer) {
		buffer.writeInt(message.buttonID);
		buffer.writeInt(message.x);
		buffer.writeInt(message.y);
		buffer.writeInt(message.z);
	}

	public static void handler(PlayerBlessingsButtonMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
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

		if (buttonID == 1) {
			if (!GodHelper.getAllContactedGods(entity).isEmpty()) {
				if (entity instanceof ServerPlayer _ent) {
					BlockPos _bpos = BlockPos.containing(entity.getX(), entity.getY(), entity.getZ());
					NetworkHooks.openScreen((ServerPlayer) _ent, new MenuProvider() {
						@Override
						public Component getDisplayName() {
							return Component.literal("Blessings");
						}

						@Override
						public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
							return new BlessingsMenu(id, inventory, new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(entity.blockPosition()));
						}
					}, _bpos);
				}
			}else {
				entity.displayClientMessage(Component.literal("You haven't contacted any gods"), true);
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
		ModMessages.addNetworkMessage(PlayerBlessingsButtonMessage.class, PlayerBlessingsButtonMessage::buffer, PlayerBlessingsButtonMessage::new, PlayerBlessingsButtonMessage::handler);
	}

}