package net.nightshade.divinity_engine.world.inventory;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;

import net.nightshade.divinity_engine.common.inventory.PocketInventory;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.registry.divinity.blessing.BlessingsRegistry;
import net.nightshade.divinity_engine.registry.gui.MenusRegistry;
import net.nightshade.divinity_engine.util.DivinityEngineHelper;

import java.util.function.Supplier;
import java.util.Map;
import java.util.HashMap;

public class PocketDimensionInvMenu extends AbstractContainerMenu implements Supplier<Map<Integer, Slot>> {
	public final static HashMap<String, Object> guistate = new HashMap<>();
	public final Level world;
	public final Player entity;
	public int x, y, z;
	private ContainerLevelAccess access = ContainerLevelAccess.NULL;
	private IItemHandler internal;
	private final Map<Integer, Slot> customSlots = new HashMap<>();
	private boolean bound = false;
	private Supplier<Boolean> boundItemMatcher = null;
	private Entity boundEntity = null;
	private BlockEntity boundBlockEntity = null;
	private BlessingsInstance blessings = null;
	private final PocketInventory pocketInventory = new PocketInventory();

	public PocketDimensionInvMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
		super(MenusRegistry.POCKET_DIMENSION.get(), id);
		this.entity = inv.player;
		this.world = inv.player.level();
		this.internal = new ItemStackHandler(27);
		BlockPos pos = null;
		if (extraData != null) {
			pos = extraData.readBlockPos();
			this.x = pos.getX();
			this.y = pos.getY();
			this.z = pos.getZ();
			access = ContainerLevelAccess.create(world, pos);

		}if (DivinityEngineHelper.hasBlessingInSlot(BlessingsRegistry.POCKET_DIMENSION.get(), entity)){
			blessings = DivinityEngineHelper.getBlessingInSlot(entity, DivinityEngineHelper.getBlessingsSlot(entity, BlessingsRegistry.POCKET_DIMENSION.get()));
		}
		if (pos != null) {
			if (extraData.readableBytes() == 1) { // bound to item
				byte hand = extraData.readByte();
				ItemStack itemstack = hand == 0 ? this.entity.getMainHandItem() : this.entity.getOffhandItem();
				this.boundItemMatcher = () -> itemstack == (hand == 0 ? this.entity.getMainHandItem() : this.entity.getOffhandItem());
				itemstack.getCapability(ForgeCapabilities.ITEM_HANDLER, null).ifPresent(capability -> {
					this.internal = capability;
					this.bound = true;
				});
			} else if (extraData.readableBytes() > 1) { // bound to entity
				extraData.readByte(); // drop padding
				boundEntity = world.getEntity(extraData.readVarInt());
				if (boundEntity != null)
					boundEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, null).ifPresent(capability -> {
						this.internal = capability;
						this.bound = true;
					});
			} else { // might be bound to block
				boundBlockEntity = this.world.getBlockEntity(pos);
				if (boundBlockEntity != null)
					boundBlockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, null).ifPresent(capability -> {
						this.internal = capability;
						this.bound = true;
					});
			}
		}
		this.customSlots.put(0, this.addSlot(new Slot(pocketInventory, 0, 7, 13) {
			private final int slot = 0;
			private int x = PocketDimensionInvMenu.this.x;
			private int y = PocketDimensionInvMenu.this.y;
		}));
		this.customSlots.put(1, this.addSlot(new Slot(pocketInventory, 1, 25, 11) {
			private final int slot = 1;
			private int x = PocketDimensionInvMenu.this.x;
			private int y = PocketDimensionInvMenu.this.y;
		}));
		this.customSlots.put(2, this.addSlot(new Slot(pocketInventory, 2, 43, 11) {
			private final int slot = 2;
			private int x = PocketDimensionInvMenu.this.x;
			private int y = PocketDimensionInvMenu.this.y;
		}));
		this.customSlots.put(3, this.addSlot(new Slot(pocketInventory, 3, 61, 11) {
			private final int slot = 3;
			private int x = PocketDimensionInvMenu.this.x;
			private int y = PocketDimensionInvMenu.this.y;
		}));
		this.customSlots.put(4, this.addSlot(new Slot(pocketInventory, 4, 79, 11) {
			private final int slot = 4;
			private int x = PocketDimensionInvMenu.this.x;
			private int y = PocketDimensionInvMenu.this.y;
		}));
		this.customSlots.put(5, this.addSlot(new Slot(pocketInventory, 5, 97, 11) {
			private final int slot = 5;
			private int x = PocketDimensionInvMenu.this.x;
			private int y = PocketDimensionInvMenu.this.y;
		}));
		this.customSlots.put(6, this.addSlot(new Slot(pocketInventory, 6, 115, 11) {
			private final int slot = 6;
			private int x = PocketDimensionInvMenu.this.x;
			private int y = PocketDimensionInvMenu.this.y;
		}));
		this.customSlots.put(7, this.addSlot(new Slot(pocketInventory, 7, 133, 11) {
			private final int slot = 7;
			private int x = PocketDimensionInvMenu.this.x;
			private int y = PocketDimensionInvMenu.this.y;
		}));
		this.customSlots.put(8, this.addSlot(new Slot(pocketInventory, 8, 151, 13) {
			private final int slot = 8;
			private int x = PocketDimensionInvMenu.this.x;
			private int y = PocketDimensionInvMenu.this.y;
		}));
		this.customSlots.put(9, this.addSlot(new Slot(pocketInventory, 9, 7, 31) {
			private final int slot = 9;
			private int x = PocketDimensionInvMenu.this.x;
			private int y = PocketDimensionInvMenu.this.y;
		}));
		this.customSlots.put(10, this.addSlot(new Slot(pocketInventory, 10, 25, 29) {
			private final int slot = 10;
			private int x = PocketDimensionInvMenu.this.x;
			private int y = PocketDimensionInvMenu.this.y;
		}));
		this.customSlots.put(11, this.addSlot(new Slot(pocketInventory, 11, 43, 29) {
			private final int slot = 11;
			private int x = PocketDimensionInvMenu.this.x;
			private int y = PocketDimensionInvMenu.this.y;
		}));
		this.customSlots.put(12, this.addSlot(new Slot(pocketInventory, 12, 61, 29) {
			private final int slot = 12;
			private int x = PocketDimensionInvMenu.this.x;
			private int y = PocketDimensionInvMenu.this.y;
		}));
		this.customSlots.put(13, this.addSlot(new Slot(pocketInventory, 13, 79, 29) {
			private final int slot = 13;
			private int x = PocketDimensionInvMenu.this.x;
			private int y = PocketDimensionInvMenu.this.y;
		}));
		this.customSlots.put(14, this.addSlot(new Slot(pocketInventory, 14, 97, 29) {
			private final int slot = 14;
			private int x = PocketDimensionInvMenu.this.x;
			private int y = PocketDimensionInvMenu.this.y;
		}));
		this.customSlots.put(15, this.addSlot(new Slot(pocketInventory, 15, 115, 29) {
			private final int slot = 15;
			private int x = PocketDimensionInvMenu.this.x;
			private int y = PocketDimensionInvMenu.this.y;
		}));
		this.customSlots.put(16, this.addSlot(new Slot(pocketInventory, 16, 133, 29) {
			private final int slot = 16;
			private int x = PocketDimensionInvMenu.this.x;
			private int y = PocketDimensionInvMenu.this.y;
		}));
		this.customSlots.put(17, this.addSlot(new Slot(pocketInventory, 17, 151, 31) {
			private final int slot = 17;
			private int x = PocketDimensionInvMenu.this.x;
			private int y = PocketDimensionInvMenu.this.y;
		}));
		this.customSlots.put(18, this.addSlot(new Slot(pocketInventory, 18, 7, 49) {
			private final int slot = 18;
			private int x = PocketDimensionInvMenu.this.x;
			private int y = PocketDimensionInvMenu.this.y;
		}));
		this.customSlots.put(19, this.addSlot(new Slot(pocketInventory, 19, 25, 47) {
			private final int slot = 19;
			private int x = PocketDimensionInvMenu.this.x;
			private int y = PocketDimensionInvMenu.this.y;
		}));
		this.customSlots.put(20, this.addSlot(new Slot(pocketInventory, 20, 43, 47) {
			private final int slot = 20;
			private int x = PocketDimensionInvMenu.this.x;
			private int y = PocketDimensionInvMenu.this.y;
		}));
		this.customSlots.put(21, this.addSlot(new Slot(pocketInventory, 21, 61, 47) {
			private final int slot = 21;
			private int x = PocketDimensionInvMenu.this.x;
			private int y = PocketDimensionInvMenu.this.y;
		}));
		this.customSlots.put(22, this.addSlot(new Slot(pocketInventory, 22, 79, 47) {
			private final int slot = 22;
			private int x = PocketDimensionInvMenu.this.x;
			private int y = PocketDimensionInvMenu.this.y;
		}));
		this.customSlots.put(23, this.addSlot(new Slot(pocketInventory, 23, 97, 47) {
			private final int slot = 23;
			private int x = PocketDimensionInvMenu.this.x;
			private int y = PocketDimensionInvMenu.this.y;
		}));
		this.customSlots.put(24, this.addSlot(new Slot(pocketInventory, 24, 115, 47) {
			private final int slot = 24;
			private int x = PocketDimensionInvMenu.this.x;
			private int y = PocketDimensionInvMenu.this.y;
		}));
		this.customSlots.put(25, this.addSlot(new Slot(pocketInventory, 25, 133, 47) {
			private final int slot = 25;
			private int x = PocketDimensionInvMenu.this.x;
			private int y = PocketDimensionInvMenu.this.y;
		}));
		this.customSlots.put(26, this.addSlot(new Slot(pocketInventory, 26, 151, 49) {
			private final int slot = 26;
			private int x = PocketDimensionInvMenu.this.x;
			private int y = PocketDimensionInvMenu.this.y;
		}));
		for (int si = 0; si < 3; ++si)
			for (int sj = 0; sj < 9; ++sj)
				this.addSlot(new Slot(inv, sj + (si + 1) * 9, 0 + 8 + sj * 18, -11 + 84 + si * 18));
		for (int si = 0; si < 9; ++si)
			this.addSlot(new Slot(inv, si, 0 + 8 + si * 18, -11 + 142));
		if (blessings != null) {
			if (blessings.getOrCreateTag().contains("inventory")) {
				CompoundTag inventoryTag = blessings.getOrCreateTag().getCompound("inventory");
				pocketInventory.loadFromTag(inventoryTag);
			}
		}

	}

	@Override
	public boolean stillValid(Player player) {
		if (this.bound) {
			if (this.boundItemMatcher != null)
				return this.boundItemMatcher.get();
			else if (this.boundBlockEntity != null)
				return AbstractContainerMenu.stillValid(this.access, player, this.boundBlockEntity.getBlockState().getBlock());
			else if (this.boundEntity != null)
				return this.boundEntity.isAlive();
		}
		return true;
	}

	@Override
	public ItemStack quickMoveStack(Player playerIn, int index) {
		ItemStack itemstack = ItemStack.EMPTY;
		Slot slot = (Slot) this.slots.get(index);
		if (slot != null && slot.hasItem()) {
			ItemStack itemstack1 = slot.getItem();
			itemstack = itemstack1.copy();
			if (index < 27) {
				if (!this.moveItemStackTo(itemstack1, 27, this.slots.size(), true))
					return ItemStack.EMPTY;
				slot.onQuickCraft(itemstack1, itemstack);
			} else if (!this.moveItemStackTo(itemstack1, 0, 27, false)) {
				if (index < 27 + 27) {
					if (!this.moveItemStackTo(itemstack1, 27 + 27, this.slots.size(), true))
						return ItemStack.EMPTY;
				} else {
					if (!this.moveItemStackTo(itemstack1, 27, 27 + 27, false))
						return ItemStack.EMPTY;
				}
				return ItemStack.EMPTY;
			}
			if (itemstack1.getCount() == 0)
				slot.set(ItemStack.EMPTY);
			else
				slot.setChanged();
			if (itemstack1.getCount() == itemstack.getCount())
				return ItemStack.EMPTY;
			slot.onTake(playerIn, itemstack1);
		}
		return itemstack;
	}

	@Override
	protected boolean moveItemStackTo(ItemStack p_38904_, int p_38905_, int p_38906_, boolean p_38907_) {
		boolean flag = false;
		int i = p_38905_;
		if (p_38907_) {
			i = p_38906_ - 1;
		}
		if (p_38904_.isStackable()) {
			while (!p_38904_.isEmpty()) {
				if (p_38907_) {
					if (i < p_38905_) {
						break;
					}
				} else if (i >= p_38906_) {
					break;
				}
				Slot slot = this.slots.get(i);
				ItemStack itemstack = slot.getItem();
				if (slot.mayPlace(itemstack) && !itemstack.isEmpty() && ItemStack.isSameItemSameTags(p_38904_, itemstack)) {
					int j = itemstack.getCount() + p_38904_.getCount();
					int maxSize = Math.min(slot.getMaxStackSize(), p_38904_.getMaxStackSize());
					if (j <= maxSize) {
						p_38904_.setCount(0);
						itemstack.setCount(j);
						slot.set(itemstack);
						flag = true;
					} else if (itemstack.getCount() < maxSize) {
						p_38904_.shrink(maxSize - itemstack.getCount());
						itemstack.setCount(maxSize);
						slot.set(itemstack);
						flag = true;
					}
				}
				if (p_38907_) {
					--i;
				} else {
					++i;
				}
			}
		}
		if (!p_38904_.isEmpty()) {
			if (p_38907_) {
				i = p_38906_ - 1;
			} else {
				i = p_38905_;
			}
			while (true) {
				if (p_38907_) {
					if (i < p_38905_) {
						break;
					}
				} else if (i >= p_38906_) {
					break;
				}
				Slot slot1 = this.slots.get(i);
				ItemStack itemstack1 = slot1.getItem();
				if (itemstack1.isEmpty() && slot1.mayPlace(p_38904_)) {
					if (p_38904_.getCount() > slot1.getMaxStackSize()) {
						slot1.setByPlayer(p_38904_.split(slot1.getMaxStackSize()));
					} else {
						slot1.setByPlayer(p_38904_.split(p_38904_.getCount()));
					}
					slot1.setChanged();
					flag = true;
					break;
				}
				if (p_38907_) {
					--i;
				} else {
					++i;
				}
			}
		}
		return flag;
	}

	@Override
	public void removed(Player playerIn) {
		super.removed(playerIn);
		if (!bound && playerIn instanceof ServerPlayer serverPlayer) {
			blessings.getOrCreateTag().put("inventory", pocketInventory.saveToTag());
			if (!serverPlayer.isAlive() || serverPlayer.hasDisconnected()) {
				for (int j = 0; j < internal.getSlots(); ++j) {
					if (j == 0)
						continue;
					if (j == 1)
						continue;
					if (j == 2)
						continue;
					if (j == 3)
						continue;
					if (j == 4)
						continue;
					if (j == 5)
						continue;
					if (j == 6)
						continue;
					if (j == 7)
						continue;
					if (j == 8)
						continue;
					if (j == 9)
						continue;
					if (j == 10)
						continue;
					if (j == 11)
						continue;
					if (j == 12)
						continue;
					if (j == 13)
						continue;
					if (j == 14)
						continue;
					if (j == 15)
						continue;
					if (j == 16)
						continue;
					if (j == 17)
						continue;
					if (j == 18)
						continue;
					playerIn.getInventory().placeItemBackInInventory(internal.extractItem(j, internal.getStackInSlot(j).getCount(), false));
				}
			} else {
				for (int i = 0; i < internal.getSlots(); ++i) {
					if (i == 0)
						continue;
					if (i == 1)
						continue;
					if (i == 2)
						continue;
					if (i == 3)
						continue;
					if (i == 4)
						continue;
					if (i == 5)
						continue;
					if (i == 6)
						continue;
					if (i == 7)
						continue;
					if (i == 8)
						continue;
					if (i == 9)
						continue;
					if (i == 10)
						continue;
					if (i == 11)
						continue;
					if (i == 12)
						continue;
					if (i == 13)
						continue;
					if (i == 14)
						continue;
					if (i == 15)
						continue;
					if (i == 16)
						continue;
					if (i == 17)
						continue;
					if (i == 18)
						continue;
					playerIn.getInventory().placeItemBackInInventory(internal.extractItem(i, internal.getStackInSlot(i).getCount(), false));
				}
			}
		}
	}

	public Map<Integer, Slot> get() {
		return customSlots;
	}

    public BlessingsInstance getBlessings() {
        return blessings;
    }

    public void setBlessings(BlessingsInstance blessings) {
        this.blessings = blessings;
    }
}
