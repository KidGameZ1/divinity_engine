package net.nightshade.divinity_engine.util.divinity.blessings.aethon;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.nightshade.divinity_engine.registry.blocks.BlocksRegistry;
import net.nightshade.divinity_engine.registry.dimensions.DimensionRegistry;

import java.util.List;

import static net.nightshade.divinity_engine.util.divinity.blessings.aethon.PocketDomainData.*;

/** Two-pass, bounded construction. Only our recorded shell/floor may be removed by a resize. */
public final class PocketRoomBuilder {
    private PocketRoomBuilder() {}

    public static AABB bounds(Room room) {
        Dimensions size = room.workSize();
        return new AABB(room.origin, room.origin.offset(size.width(), size.height(), size.depth()));
    }

    public static void tick(MinecraftServer server) {
        ServerLevel level = server.getLevel(DimensionRegistry.POCKET_DIMENSION);
        if (level == null) return;
        PocketDomainData data = PocketDomainData.get(server);
        int operations = 0;
        long deadline = System.nanoTime() + 2_000_000L;
        for (Domain domain : data.domains()) for (Room room : List.copyOf(domain.rooms)) {
            if (room.ready() || room.legacy || room.blocked && level.getGameTime() % 20 != 0) continue;
            if (operations >= 512 || System.nanoTime() >= deadline) return;
            if (!chunksReady(level, room)) continue;
            while (!room.ready() && operations < 512 && System.nanoTime() < deadline) {
                BlockPos pos = room.at(room.cursor);
                BlockState current = level.getBlockState(pos);
                BlockState change = null;
                boolean conflict = false;
                if (room.stage == 0) conflict = !current.isAir() || level.getBlockEntity(pos) != null;
                else if (room.stage == 1) {
                    change = PocketDomainManager.constructionBlock(room, pos);
                    conflict = change != null && !current.isAir() && !current.equals(change);
                } else {
                    change = replacement(room, pos, current, room.stage == 4);
                    conflict = change == null || !current.equals(change) && level.getBlockEntity(pos) != null;
                }
                operations++;
                if (conflict) {
                    if (room.stage == 0 || room.stage == 3) {
                        boolean resizing = room.stage == 3;
                        if (resizing) cancelResize(room); else domain.rooms.remove(room);
                        release(level, room);
                        notify(server, domain, "Chamber " + (resizing ? "resize" : "site") + " blocked at "
                                + pos.toShortString() + ". Your blocks were not changed.");
                    } else {
                        if (!room.blocked) notify(server, domain, "Chamber work paused at " + pos.toShortString()
                                + "; an unexpected block is protected. Remove the obstruction to resume.");
                        room.blocked = true;
                    }
                    data.setDirty();
                    break;
                }
                room.blocked = false;
                if ((room.stage == 1 || room.stage == 4) && change != null && !current.equals(change))
                    level.setBlock(pos, change, 2);
                room.cursor++;
                data.setDirty();
                if (room.cursor < room.workSize().volume()) continue;
                room.cursor = 0;
                if (room.stage == 3 && !level.getEntities((net.minecraft.world.entity.Entity) null, bounds(room), entity -> !entity.isRemoved()).isEmpty()) {
                    cancelResize(room);
                    release(level, room);
                    notify(server, domain, "Leave the chamber and move its creatures/items out before resizing. Nothing was changed.");
                    break;
                }
                if (room.stage == 4) {
                    room.size = room.pendingSize;
                    room.pendingSize = null;
                    room.stage = 2;
                } else room.stage++;
                if (room.ready()) {
                    release(level, room);
                    notify(server, domain, "Your chamber is ready with its own exit. Separate up/down gateways link to adjacent completed chambers.");
                }
            }
        }
    }

    private static void cancelResize(Room room) {
        room.pendingSize = null; room.stage = 2; room.cursor = 0; room.blocked = false;
    }

    /** Null rejects the operation. Preserved interior blocks, including containers, are left untouched. */
    public static BlockState replacement(Room room, BlockPos pos, BlockState current, boolean applying) {
        boolean wasInside = room.contains(pos, room.size), inside = room.contains(pos, room.pendingSize);
        BlockState old = wasInside ? PocketDomainManager.constructionBlock(room, pos, room.size) : null;
        BlockState next = inside ? PocketDomainManager.constructionBlock(room, pos, room.pendingSize) : null;
        boolean generated = old != null && old.equals(current);
        if (!wasInside) {
            if (!current.isAir() && !(applying && current.equals(next))) return null;
            return next == null ? current : next;
        }
        if (!inside) return current.isAir() || generated ? Blocks.AIR.defaultBlockState() : null;
        if (next != null && next.is(BlocksRegistry.POCKET_DIMENSION.get()))
            return current.isAir() || generated || applying && current.equals(next) ? next : null;
        if (old != null && old.is(BlocksRegistry.POCKET_DIMENSION.get()) && generated)
            return next == null ? Blocks.AIR.defaultBlockState() : next;
        return current;
    }

    private static boolean chunksReady(ServerLevel level, Room room) {
        Dimensions size = room.workSize();
        for (int x = room.origin.getX() >> 4; x <= (room.origin.getX() + size.width() - 1) >> 4; x++)
            for (int z = room.origin.getZ() >> 4; z <= (room.origin.getZ() + size.depth() - 1) >> 4; z++) {
                ChunkPos chunk = new ChunkPos(x, z);
                room.tickets.add(chunk);
                level.getChunkSource().addRegionTicket(PocketChunkLoading.BUILD, chunk, 0, room.origin);
                if (level.getChunkSource().getChunkNow(x, z) == null) {
                    // getChunkFuture() also managed-blocks on the server thread in 1.20.1.
                    // The ticket schedules loading during normal chunk processing; poll next tick.
                    return false;
                }
            }
        return true;
    }

    private static void release(ServerLevel level, Room room) {
        for (ChunkPos chunk : room.tickets) level.getChunkSource().removeRegionTicket(PocketChunkLoading.BUILD, chunk, 0, room.origin);
        room.tickets.clear();
    }

    private static void notify(MinecraftServer server, Domain domain, String message) {
        var owner = server.getPlayerList().getPlayer(domain.owner);
        if (owner != null) PocketDomainManager.tell(owner, message);
    }
}
