package net.nightshade.divinity_engine.util.divinity.blessings.aethon;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.nightshade.divinity_engine.registry.dimensions.DimensionRegistry;

import java.util.*;

/** Temporary FULL-only tickets. Never waits for chunk futures on the game thread. */
public final class PocketChunkLoading {
    public static final TicketType<BlockPos> BUILD = TicketType.create("divinity_pocket_build", BlockPos::compareTo, 40);
    private static final TicketType<UUID> ARRIVAL = TicketType.create("divinity_pocket_arrival", UUID::compareTo, 60);
    private static final Map<UUID, Arrival> ARRIVALS = new HashMap<>();
    private record Arrival(Entity entity, ServerLevel source, Vec3 departure, ServerLevel level, BlockPos center, Set<ChunkPos> chunks, long expires) {}
    private PocketChunkLoading() {}

    public static void tick(MinecraftServer server) {
        ServerLevel pocket = server.getLevel(DimensionRegistry.POCKET_DIMENSION);
        if (pocket != null) {
            // Enclosed rooms do not need the overworld's large terrain-loading radius.
            pocket.getChunkSource().setViewDistance(Math.min(4, server.getPlayerList().getViewDistance()));
            pocket.getChunkSource().setSimulationDistance(Math.min(3, server.getPlayerList().getSimulationDistance()));
        }
        long now = server.overworld().getGameTime();
        for (UUID owner : List.copyOf(ARRIVALS.keySet())) {
            Arrival request = ARRIVALS.get(owner);
            if (now >= request.expires || request.entity.isRemoved() || request.entity.level() != request.source
                    || request.entity.position().distanceToSqr(request.departure) > 4) release(owner);
        }
    }

    public static boolean prepare(Entity player, ServerLevel level, Vec3 position) {
        BlockPos center = BlockPos.containing(position);
        Arrival current = ARRIVALS.get(player.getUUID());
        if (current == null || current.level != level || !current.center.equals(center)) {
            release(player.getUUID());
            Set<ChunkPos> chunks = new HashSet<>();
            for (int x = (center.getX() - 6) >> 4; x <= (center.getX() + 6) >> 4; x++)
                for (int z = (center.getZ() - 6) >> 4; z <= (center.getZ() + 6) >> 4; z++) chunks.add(new ChunkPos(x, z));
            current = new Arrival(player, (ServerLevel) player.level(), player.position(), level, center, chunks,
                    level.getServer().overworld().getGameTime() + 60);
            ARRIVALS.put(player.getUUID(), current);
        }
        boolean ready = true;
        for (ChunkPos chunk : current.chunks) {
            level.getChunkSource().addRegionTicket(ARRIVAL, chunk, 0, player.getUUID());
            if (level.getChunkSource().getChunkNow(chunk.x, chunk.z) == null) ready = false;
        }
        ARRIVALS.put(player.getUUID(), new Arrival(player, current.source, current.departure, level, center, current.chunks,
                level.getServer().overworld().getGameTime() + 60));
        return ready;
    }

    public static boolean loaded(ServerLevel level, AABB bounds) {
        for (int x = net.minecraft.util.Mth.floor(bounds.minX) >> 4; x <= net.minecraft.util.Mth.floor(bounds.maxX) >> 4; x++)
            for (int z = net.minecraft.util.Mth.floor(bounds.minZ) >> 4; z <= net.minecraft.util.Mth.floor(bounds.maxZ) >> 4; z++)
                if (level.getChunkSource().getChunkNow(x, z) == null) return false;
        return true;
    }

    public static void release(UUID owner) {
        Arrival request = ARRIVALS.remove(owner);
        if (request != null) for (ChunkPos chunk : request.chunks)
            request.level.getChunkSource().removeRegionTicket(ARRIVAL, chunk, 0, owner);
    }

    public static int pendingArrivals() { return ARRIVALS.size(); }
    public static boolean pending(UUID entity) { return ARRIVALS.containsKey(entity); }
    public static void stopped() { ARRIVALS.clear(); }
}
