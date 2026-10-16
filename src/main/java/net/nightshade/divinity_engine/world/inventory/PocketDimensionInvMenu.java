package net.nightshade.divinity_engine.world.inventory;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.nightshade.divinity_engine.common.inventory.PocketInventory;
import net.nightshade.divinity_engine.registry.gui.MenusRegistry;
import net.nightshade.divinity_engine.util.divinity.blessings.aethon.PocketDomainManager;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/** One persistent inventory per owner, not one mutable copy per open menu. */
public class PocketDimensionInvMenu extends AbstractContainerMenu implements Supplier<Map<Integer, Slot>> {
    private final Player owner;
    private final Map<Integer, Slot> customSlots = new LinkedHashMap<>();

    public PocketDimensionInvMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        super(MenusRegistry.POCKET_DIMENSION.get(), id);
        owner = inventory.player;
        PocketInventory storage = owner instanceof ServerPlayer server
                ? PocketDomainManager.domain(server).inventory : new PocketInventory();
        if (extraData != null && extraData.readableBytes() >= Long.BYTES) extraData.readBlockPos();
        // Keep all 27 legacy storage slots and their celestial screen coordinates.
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) {
            int index = row * 9 + col;
            customSlots.put(index, addSlot(new Slot(storage, index, 7 + col * 18,
                    11 + row * 18 + (col == 0 || col == 8 ? 2 : 0))));
        }
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlot(new Slot(inventory, col + (row + 1) * 9, 8 + col * 18, 73 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 131));
    }
    @Override public boolean stillValid(Player player) {
        return player == owner && (player.level().isClientSide
                || player instanceof ServerPlayer server && PocketDomainManager.canManage(server));
    }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (!stillValid(player) || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), original = stack.copy();
        if (!moveItemStackTo(stack, index < 27 ? 27 : 0, index < 27 ? slots.size() : 27, index < 27))
            return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack);
        return original;
    }
    @Override public boolean clickMenuButton(Player player, int button) {
        if (button != 0 || !(player instanceof ServerPlayer server) || player.containerMenu != this || !stillValid(player)) return false;
        PocketDomainManager.openControl(server);
        return true;
    }
    @Override public Map<Integer, Slot> get() { return customSlots; }
}
