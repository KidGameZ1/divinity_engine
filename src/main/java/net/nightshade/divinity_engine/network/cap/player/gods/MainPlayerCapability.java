package net.nightshade.divinity_engine.network.cap.player.gods;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.IForgeRegistry;
import net.nightshade.divinity_engine.DivinityEngineMod;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.network.messages.ModMessages;
import net.nightshade.divinity_engine.network.messages.SyncMainPlayerCapabilityPacket;
import net.nightshade.divinity_engine.registry.divinity.blessing.BlessingsRegistry;
import net.nightshade.divinity_engine.util.MainPlayerCapabilityHelper;
import net.nightshade.divinity_engine.util.divinity.gods.GodHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class MainPlayerCapability {

	public static final Capability<IMainPlayerCapability> PLAYER_VARIABLES_CAPABILITY = CapabilityManager.get(new CapabilityToken<IMainPlayerCapability>() {
	});

	@Mod.EventBusSubscriber
	private static class PlayerVariablesProvider implements ICapabilitySerializable<CompoundTag> {
		@SubscribeEvent
		public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
			if (event.getObject() instanceof Player && !(event.getObject() instanceof FakePlayer))
				event.addCapability(new ResourceLocation(DivinityEngineMod.MODID, "player_main_variables"), new PlayerVariablesProvider());
		}

		private final PlayerVariables playerVariables = new PlayerVariables();
		private final LazyOptional<PlayerVariables> instance = LazyOptional.of(() -> playerVariables);

		@Override
		public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
			return cap == PLAYER_VARIABLES_CAPABILITY ? instance.cast() : LazyOptional.empty();
		}

		@Override
		public CompoundTag serializeNBT() {
			return playerVariables.serializeNBT();
		}

		@Override
		public void deserializeNBT(CompoundTag nbt) {
			playerVariables.deserializeNBT(nbt);
		}
	}

	public static class PlayerVariables implements IMainPlayerCapability {
		private static final Logger log = LogManager.getLogger(PlayerVariables.class);
		private static final ResourceLocation ID = new ResourceLocation(DivinityEngineMod.MODID, "player_main_cap");

		private @Nullable BlessingsInstance blessingSlot1 = null;
		private @Nullable BlessingsInstance blessingSlot2 = null;
		private @Nullable BlessingsInstance blessingSlot3 = null;

		private int blessingsPageNum = 0;
		private int maxBlessingsPageNum = 0;

		@SubscribeEvent
		public static void attach(AttachCapabilitiesEvent<Entity> e) {
			if (e.getObject() instanceof Player) {
				e.addCapability(ID, new PlayerVariablesProvider());
			}

		}

		@Override
		public CompoundTag serializeNBT() {
			CompoundTag nbt = new CompoundTag();

			if (blessingSlot1 != null) {
				CompoundTag slot1Tag = new CompoundTag();
				ResourceLocation blessingId = BlessingsRegistry.BLESSINGS_REGISTRY.get().getKey(blessingSlot1.getBlessing());
				slot1Tag.putString("blessing", blessingId.toString());
				slot1Tag.put("data", blessingSlot1.toNBT());
				nbt.put("blessingSlot1", slot1Tag);
			}else {
				nbt.putString("blessingSlot1", "none");
			}

			if (blessingSlot2 != null) {
				CompoundTag slot2Tag = new CompoundTag();
				ResourceLocation blessingId = BlessingsRegistry.BLESSINGS_REGISTRY.get().getKey(blessingSlot2.getBlessing());
				slot2Tag.putString("blessing", blessingId.toString());
				slot2Tag.put("data", blessingSlot2.toNBT());
				nbt.put("blessingSlot2", slot2Tag);
			}else {
				nbt.putString("blessingSlot2", "none");
			}

			if (blessingSlot3 != null) {
				CompoundTag slot3Tag = new CompoundTag();
				ResourceLocation blessingId = BlessingsRegistry.BLESSINGS_REGISTRY.get().getKey(blessingSlot3.getBlessing());
				slot3Tag.putString("blessing", blessingId.toString());
				slot3Tag.put("data", blessingSlot3.toNBT());
				nbt.put("blessingSlot3", slot3Tag);
			}else {
				nbt.putString("blessingSlot3", "none");
			}

			nbt.putInt("blessingsPageNum", this.blessingsPageNum);
			return nbt;
		}

		@Override
		public void deserializeNBT(CompoundTag nbt) {
			if (nbt.contains("blessingSlot1")) {
				if (nbt.getString("blessingSlot1").equals( "none" )){
					this.blessingSlot1 = null;
				}else {
					CompoundTag slot1Tag = nbt.getCompound("blessingSlot1");
					ResourceLocation blessingId = new ResourceLocation(slot1Tag.getString("blessing"));
					var blessing = BlessingsRegistry.BLESSINGS_REGISTRY.get().getValue(blessingId);
					if (blessing != null) {
						BlessingsInstance instance = blessing.createDefaultInstance();
						instance.deserialize(slot1Tag.getCompound("data"));
						this.blessingSlot1 = instance;
					}
				}
			}

			if (nbt.contains("blessingSlot2")) {
				if (nbt.getString("blessingSlot2").equals( "none" )){
					this.blessingSlot2 = null;
				}else {
					CompoundTag slot2Tag = nbt.getCompound("blessingSlot2");
					ResourceLocation blessingId = new ResourceLocation(slot2Tag.getString("blessing"));
					var blessing = BlessingsRegistry.BLESSINGS_REGISTRY.get().getValue(blessingId);
					if (blessing != null) {
						BlessingsInstance instance = blessing.createDefaultInstance();
						instance.deserialize(slot2Tag.getCompound("data"));
						this.blessingSlot2 = instance;
					}
				}

			}

			if (nbt.contains("blessingSlot3")) {
				if (nbt.getString("blessingSlot3").equals( "none" )){
					this.blessingSlot3 = null;
				}else {
					CompoundTag slot3Tag = nbt.getCompound("blessingSlot3");
					ResourceLocation blessingId = new ResourceLocation(slot3Tag.getString("blessing"));
					var blessing = BlessingsRegistry.BLESSINGS_REGISTRY.get().getValue(blessingId);
					if (blessing != null) {
						BlessingsInstance instance = blessing.createDefaultInstance();
						instance.deserialize(slot3Tag.getCompound("data"));
						this.blessingSlot3 = instance;
					}
				}
			}

			this.blessingsPageNum = nbt.contains("blessingsPageNum") ? nbt.getInt("blessingsPageNum") : 0;
		}


		public static LazyOptional<IMainPlayerCapability> getFrom(Player player) {
			return player.getCapability(MainPlayerCapability.PLAYER_VARIABLES_CAPABILITY);
		}
		public void syncPlayerVariables(Entity entity) {
			sync((Player) entity);
		}
		public static void sync(Player player) {
			if (player instanceof ServerPlayer serverPlayer) {
				getFrom(serverPlayer).ifPresent((data) -> ModMessages.INSTANCE.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> serverPlayer), new SyncMainPlayerCapabilityPacket(data, serverPlayer.getId())));
			}
		}


		

		public @Nullable BlessingsInstance getBlessingSlot1() {
			return blessingSlot1;
		}
		public void setBlessingSlot1(@Nullable BlessingsInstance blessingSlot1) {
			this.blessingSlot1 = blessingSlot1;
		}
		public @Nullable BlessingsInstance getBlessingSlot2() {
			return blessingSlot2;
		}
		public void setBlessingSlot2(@Nullable BlessingsInstance blessingSlot2) {
			this.blessingSlot2 = blessingSlot2;
		}
		public @Nullable BlessingsInstance getBlessingSlot3() {
			return blessingSlot3;
		}
		public void setBlessingSlot3(@Nullable BlessingsInstance blessingSlot3) {
			this.blessingSlot3 = blessingSlot3;
		}

		public @Nullable int getBlessingsPageNum() {
			return blessingsPageNum;
		}
		public void setBlessingsPageNum(int num) {
			this.blessingsPageNum = num;
		}
		public void increaseBlessingsPageNum(int num) {
			this.blessingsPageNum += num;
		}
		public void decreaseBlessingsPageNum(int num) {
			this.blessingsPageNum -= num;
			if (this.blessingsPageNum < 0) {
				this.blessingsPageNum = 0;
			}
		}
	}
}
