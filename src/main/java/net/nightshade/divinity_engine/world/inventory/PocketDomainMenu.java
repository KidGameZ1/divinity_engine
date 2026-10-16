package net.nightshade.divinity_engine.world.inventory;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.nightshade.divinity_engine.registry.gui.MenusRegistry;
import net.nightshade.divinity_engine.util.divinity.blessings.aethon.PocketDomainData;
import net.nightshade.divinity_engine.util.divinity.blessings.aethon.PocketDomainManager;
import net.nightshade.divinity_engine.util.divinity.blessings.aethon.PocketPortals;

/** Uses vanilla container button packets and data slots; every request is revalidated against its owner. */
public final class PocketDomainMenu extends AbstractContainerMenu {
    public final CompoundTag snapshot;
    private final Player owner;
    private final SimpleContainerData state = new SimpleContainerData(42);

    public PocketDomainMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, buffer == null ? new CompoundTag() : buffer.readNbt());
    }
    public PocketDomainMenu(int id, Inventory inventory, CompoundTag snapshot) {
        super(MenusRegistry.POCKET_DOMAIN.get(), id);
        owner = inventory.player;
        this.snapshot = snapshot == null ? new CompoundTag() : snapshot.copy();
        ListTag rooms = rooms();
        for (int i = 0; i < Math.min(8, rooms.size()); i++) {
            state.set(i, rooms.getCompound(i).getInt("Stage") == 2 ? 100 : 0);
            state.set(i + 8, rooms.getCompound(i).getInt("Rules"));
            var size = PocketDomainData.Dimensions.load(rooms.getCompound(i).getCompound("Size"));
            state.set(18 + i * 3, size.width()); state.set(19 + i * 3, size.height()); state.set(20 + i * 3, size.depth());
        }
        state.set(16, this.snapshot.getInt("Selected"));
        addDataSlots(state);
    }
    public ListTag rooms() { return snapshot.getList("Rooms", Tag.TAG_COMPOUND); }
    public ListTag residents() { return snapshot.getList("Residents", Tag.TAG_COMPOUND); }
    public int progress(int room) { return room >= 0 && room < 8 ? state.get(room) : 0; }
    public int rules(int room) { return room >= 0 && room < 8 ? state.get(room + 8) : 0; }
    public int selected() { return state.get(16); }
    public int recallSeconds() { return state.get(17); }
    public PocketDomainData.Dimensions size(int room) {
        return room >= 0 && room < 8 ? new PocketDomainData.Dimensions(state.get(18 + room * 3), state.get(19 + room * 3), state.get(20 + room * 3))
                : new PocketDomainData.Dimensions(PocketDomainData.SIZE, PocketDomainData.HEIGHT, PocketDomainData.SIZE);
    }

    @Override public void broadcastChanges() {
        if (owner instanceof ServerPlayer player) {
            var domain = PocketDomainManager.domain(player);
            for (int i = 0; i < 8; i++) {
                var room = i < domain.rooms.size() ? domain.rooms.get(i) : null;
                state.set(i, room == null ? -1 : room.progress());
                state.set(i + 8, room == null ? 0 : room.rules);
                if (room != null) {
                    state.set(18 + i * 3, room.size.width()); state.set(19 + i * 3, room.size.height()); state.set(20 + i * 3, room.size.depth());
                }
            }
            state.set(16, domain.selected);
            state.set(17, (int) Math.max(0, Math.min(300, (domain.recallAfter - player.server.overworld().getGameTime() + 19) / 20)));
        }
        super.broadcastChanges();
    }
    @Override public boolean stillValid(Player player) {
        return player == owner && (player.level().isClientSide || player instanceof ServerPlayer server && PocketDomainManager.canManage(server));
    }
    @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }

    @Override public boolean clickMenuButton(Player player, int action) {
        if (!(player instanceof ServerPlayer server) || player != owner || player.containerMenu != this || !stillValid(player)) return false;
        // Emergency exit never depends on a UI action throttle or current blessing eligibility.
        if (action == 2) return PocketDomainManager.exit(server);
        if (!PocketDomainManager.takeAction(server)) return false;
        var data = PocketDomainData.get(server.server);
        var domain = PocketDomainManager.domain(server);
        if (action == 0) PocketDomainManager.openStorage(server);
        else if (action == 1) { PocketDomainManager.capture(server); PocketDomainManager.openControl(server); }
        else if (action == 3) PocketDomainManager.openControl(server);
        else if (action >= 4 && action <= 7) {
            if (action == 4) PocketPortals.place(server, true);
            if (action == 5) PocketPortals.close(server, true);
            if (action == 6) PocketPortals.place(server, false);
            if (action == 7) PocketPortals.close(server, false);
            int current = PocketPortals.currentRoom(server, domain);
            PocketDomainManager.openControl(server, 4, current < 0 ? domain.selected : current);
        }
        else if (action >= 8 && action < 16 && matchesRoom(domain, action - 8)) return PocketDomainManager.enter(server, action - 8);
        else if (action >= 16 && action < 24 && matchesRoom(domain, action - 16) && domain.rooms.get(action - 16).ready()) {
            domain.selected = action - 16; data.setDirty();
        } else if (action >= 28 && action < 28 + residents().size()) {
            CompoundTag resident = residents().getCompound(action - 28);
            if (resident.hasUUID("Id")) PocketDomainManager.release(server, resident.getUUID("Id"));
            PocketDomainManager.openControl(server);
        } else if (action >= 24 && action < 24 + PocketDomainData.Style.values().length) {
            PocketDomainManager.createRoom(server, PocketDomainData.Style.values()[action - 24]);
            PocketDomainManager.openControl(server);
        } else if (action >= 120 && action < 128 && matchesRoom(domain, action - 120)) {
            if (!PocketPortals.closeRoom(server, action - 120)) return false;
            PocketDomainManager.openControl(server, 4, action - 120);
        } else if (action >= 40 && action < 120 && PocketDomainManager.eligible(server)) {
            // Vanilla's button packet uses a signed byte. Keep every action below 128.
            int encoded = action - 40, roomIndex = encoded / 10, ruleIndex = (encoded % 10) / 2;
            if (!matchesRoom(domain, roomIndex) || ruleIndex >= PocketDomainData.Rule.values().length) return false;
            var room = domain.rooms.get(roomIndex);
            if (!room.ready()) return false;
            // Explicit desired state, not toggling, makes duplicate packets harmless.
            room.setRule(PocketDomainData.Rule.values()[ruleIndex], (encoded & 1) != 0);
            data.setDirty();
        } else return false;
        broadcastChanges();
        return true;
    }
    public boolean matchesRoom(PocketDomainData.Domain domain, int index) {
        return index >= 0 && index < domain.rooms.size() && index < rooms().size()
                && domain.rooms.get(index).origin.asLong() == rooms().getCompound(index).getLong("Origin");
    }
}
