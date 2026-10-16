package net.nightshade.divinity_engine.network.messages.gui;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkEvent;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.divinity.trial.GodlyTrial;
import net.nightshade.divinity_engine.network.cap.player.gods.MainPlayerCapability;
import net.nightshade.divinity_engine.network.cap.player.gods.PlayerGodsCapability;
import net.nightshade.divinity_engine.network.messages.ModMessages;
import net.nightshade.divinity_engine.registry.divinity.gods.GodsRegistry;
import net.nightshade.divinity_engine.registry.divinity.trial.GodlyTrialsRegistry;
import net.nightshade.divinity_engine.util.DivinityEngineHelper;
import net.nightshade.divinity_engine.util.divinity.gods.GodHelper;
import net.nightshade.divinity_engine.util.divinity.trials.TrialSessionManager;
import net.nightshade.divinity_engine.util.divinity.trials.TrialStartResult;
import net.nightshade.divinity_engine.world.inventory.BlessingsMenu;
import net.nightshade.vanguards_kit.VanguardMod;
import net.nightshade.vanguards_kit.common.inv.slot.VanguardSlot;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.function.Supplier;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class TrialButtonMessage {

	private final int buttonID, x, y, z;

	public TrialButtonMessage(FriendlyByteBuf buffer) {
		this.buttonID = buffer.readInt();
		this.x = buffer.readInt();
		this.y = buffer.readInt();
		this.z = buffer.readInt();
	}

	public TrialButtonMessage(int buttonID, int x, int y, int z) {
		this.buttonID = buttonID;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public static void buffer(TrialButtonMessage message, FriendlyByteBuf buffer) {
		buffer.writeInt(message.buttonID);
		buffer.writeInt(message.x);
		buffer.writeInt(message.y);
		buffer.writeInt(message.z);
	}

	public static void handler(TrialButtonMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
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
		if (!(entity instanceof ServerPlayer) || buttonID != 1) return;
		Level world = entity.level();
		HashMap guistate = BlessingsMenu.guistate;

		BlessingsMenu blessingsMenu = entity.containerMenu instanceof BlessingsMenu ? (BlessingsMenu) entity.containerMenu : null;

		// security measure to prevent arbitrary chunk generation
		if (!world.hasChunkAt(new BlockPos(x, y, z)))
			return;

        if (blessingsMenu == null || !blessingsMenu.isOpenedFromStatue() || !blessingsMenu.stillValid(entity)
                || !new BlockPos(x, y, z).equals(blessingsMenu.getOpeningPos())) {
            if (entity instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(Component.literal("Trials can only be started from a god's statue."), false);
            }
            return;
        }

		if (blessingsMenu.getGod() != null) {
            if (entity instanceof ServerPlayer _ent) {
                TrialStartResult result = TrialSessionManager.startTrial(_ent, blessingsMenu.getGod());
                if (!result.isSuccess()) {
                    _ent.displayClientMessage(result.getMessage(), false);
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
		ModMessages.addNetworkMessage(TrialButtonMessage.class, TrialButtonMessage::buffer, TrialButtonMessage::new, TrialButtonMessage::handler);
	}

}
