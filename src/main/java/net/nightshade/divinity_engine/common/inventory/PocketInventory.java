package net.nightshade.divinity_engine.common.inventory;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class PocketInventory implements Container {

    private final NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int index) {
        if (index >= 0 && index < items.size()) {
            return items.get(index);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        if (index < 0 || index >= items.size() || items.get(index).isEmpty() || count <= 0)
            return ItemStack.EMPTY;

        ItemStack existing = items.get(index);
        ItemStack split = existing.split(count);

        if (!split.isEmpty()) {
            setChanged();
        }

        return split;
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        if (index >= 0 && index < items.size()) {
            ItemStack removed = items.get(index);
            items.set(index, ItemStack.EMPTY);
            return removed;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        if (index >= 0 && index < items.size()) {
            items.set(index, stack);
            if (stack.getCount() > getMaxStackSize()) {
                stack.setCount(getMaxStackSize());
            }
            setChanged();
        }
    }

    @Override
    public void setChanged() {
        // Hook for notifying UI or saving
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        items.clear();
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }

    // -------- Save / Load to NBT --------

    public CompoundTag saveToTag() {
        CompoundTag tag = new CompoundTag();
        ListTag itemList = new ListTag();

        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (!stack.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putInt("Slot", i);
                stack.save(itemTag);
                itemList.add(itemTag);
            }
        }

        tag.put("Items", itemList);
        return tag;
    }

    public void loadFromTag(CompoundTag tag) {
        ListTag itemList = tag.getList("Items", Tag.TAG_COMPOUND);
        for (int i = 0; i < itemList.size(); i++) {
            CompoundTag itemTag = itemList.getCompound(i);
            int slot = itemTag.getInt("Slot");
            if (slot >= 0 && slot < items.size()) {
                items.set(slot, ItemStack.of(itemTag));
            }
        }
    }
}
