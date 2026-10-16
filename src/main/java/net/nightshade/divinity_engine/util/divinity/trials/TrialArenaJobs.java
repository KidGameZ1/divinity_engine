package net.nightshade.divinity_engine.util.divinity.trials;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.nightshade.divinity_engine.DivinityEngineMod;
import net.nightshade.divinity_engine.registry.dimensions.DimensionRegistry;

import java.util.*;

/** World-owned reservations survive disconnects and restarts. Only recorded bounds are ever erased. */
public final class TrialArenaJobs extends SavedData {
    public static final int BLOCK_BUDGET = 512;
    public static final int VOLUME = 26 * 13 * 25;
    private final Map<Integer, Job> jobs = new LinkedHashMap<>();
    private final Set<Integer> managedSlots = new HashSet<>();
    private int lastOperations;
    private long lastNanos;

    private static final class Job {
        final int slot;
        final UUID session;
        final BlockPos origin;
        int stage; // 0: check vacant site, 1: place, 2: occupied, 3: erase, 4: foreign occupied site
        int cursor;
        CompoundTag template;
        Map<BlockPos, CompoundTag> blocks;
        List<BlockState> palette;
        final Set<net.minecraft.world.level.ChunkPos> tickets = new HashSet<>();
        ServerLevel loadingLevel;
        Job(int slot, UUID session, BlockPos origin) { this.slot = slot; this.session = session; this.origin = origin; }
    }

    public static TrialArenaJobs get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TrialArenaJobs::load, TrialArenaJobs::new, "divinity_engine_trial_arenas");
    }

    public static TrialArenaJobs load(CompoundTag tag) {
        TrialArenaJobs data = new TrialArenaJobs();
        for (int slot : tag.getIntArray("managed_slots"))
            if (slot >= 0 && slot < GodsTrialStructureSpawner.getArenaSlotCount()) data.managedSlots.add(slot);
        for (Tag value : tag.getList("arenas", Tag.TAG_COMPOUND)) {
            CompoundTag saved = (CompoundTag) value;
            if (!saved.hasUUID("session")) continue;
            int slot = saved.getInt("slot");
            BlockPos origin = BlockPos.of(saved.getLong("origin"));
            if (slot < 0 || slot >= GodsTrialStructureSpawner.getArenaSlotCount()
                    || !origin.equals(GodsTrialStructureSpawner.getArenaOrigin(slot))) continue;
            data.managedSlots.add(slot);
            // A scan has not changed terrain and must not erase an occupied site after restart.
            if (saved.getInt("stage") == 0) continue;
            Job job = new Job(slot, saved.getUUID("session"), origin);
            job.stage = saved.getInt("stage") == 4 ? 4 : 3;
            data.jobs.put(slot, job);
        }
        data.setDirty();
        return data;
    }

    @Override public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Job job : jobs.values()) {
            CompoundTag entry = new CompoundTag();
            entry.putInt("slot", job.slot); entry.putUUID("session", job.session);
            entry.putLong("origin", job.origin.asLong()); entry.putInt("stage", job.stage);
            list.add(entry);
        }
        tag.put("arenas", list);
        tag.putIntArray("managed_slots", managedSlots.stream().mapToInt(Integer::intValue).toArray());
        return tag;
    }

    public boolean reserved(int slot) { return jobs.containsKey(slot); }
    public boolean owns(UUID session) { return jobs.values().stream().anyMatch(j -> j.session.equals(session) && j.stage < 3); }
    public boolean protects(BlockPos pos) {
        return jobs.values().stream().anyMatch(j -> j.stage != 4 && pos.getX() >= j.origin.getX() && pos.getX() < j.origin.getX() + 26
                && pos.getY() >= j.origin.getY() && pos.getY() < j.origin.getY() + 13
                && pos.getZ() >= j.origin.getZ() && pos.getZ() < j.origin.getZ() + 25);
    }
    public boolean intersects(net.minecraft.world.phys.AABB bounds) {
        return jobs.values().stream().anyMatch(j -> j.stage != 4 && bounds.intersects(new net.minecraft.world.phys.AABB(
                j.origin.getX(), j.origin.getY(), j.origin.getZ(), j.origin.getX() + 26, j.origin.getY() + 13, j.origin.getZ() + 25)));
    }
    public boolean prepare(TrialSession session) {
        int slot = session.getArenaSlot();
        if (reserved(slot)) return false;
        var template = session.getTrialLevel().getStructureManager().get(GodsTrialStructureSpawner.STRUCTURE_ID).orElse(null);
        if (template == null || template.getSize().getX() != 26 || template.getSize().getY() != 13 || template.getSize().getZ() != 25) return false;
        Job job = new Job(slot, session.getSessionId(), session.getArenaOrigin());
        job.template = template.save(new CompoundTag());
        ListTag palette = job.template.getList("palette", Tag.TAG_COMPOUND);
        if (palette.isEmpty()) return false;
        job.palette = new ArrayList<>();
        for (Tag entry : palette) job.palette.add(NbtUtils.readBlockState(session.getTrialLevel().holderLookup(Registries.BLOCK), (CompoundTag) entry));
        job.blocks = new HashMap<>();
        for (Tag entry : job.template.getList("blocks", Tag.TAG_COMPOUND)) {
            CompoundTag block = (CompoundTag) entry;
            ListTag p = block.getList("pos", Tag.TAG_INT);
            job.blocks.put(new BlockPos(p.getInt(0), p.getInt(1), p.getInt(2)), block);
        }
        jobs.put(slot, job); managedSlots.add(slot); setDirty(); return true;
    }

    public void cleanup(int slot, UUID session) {
        Job job = jobs.get(slot);
        if (job == null || !job.session.equals(session) || job.stage == 4) return;
        if (job.stage == 0) {
            jobs.remove(slot);
            if (job.loadingLevel != null) releaseTickets(job.loadingLevel, job);
            TrialSessionManager.releaseArenaSlot(slot);
        }
        else if (job.stage != 3) { job.stage = 3; job.cursor = 0; job.blocks = null; job.palette = null; job.template = null; }
        setDirty();
    }

    /** Capability-only legacy recovery is accepted only for an unclaimed, exact fixed slot. */
    public void recover(int slot, UUID session, BlockPos origin) {
        if (slot < 0 || slot >= GodsTrialStructureSpawner.getArenaSlotCount()
                || !origin.equals(GodsTrialStructureSpawner.getArenaOrigin(slot))) return;
        if (!reserved(slot)) {
            // Once managed here, absent means already cleaned. An old player save cannot reclaim it for deletion.
            if (managedSlots.contains(slot)) return;
            Job job = new Job(slot, session, origin); job.stage = 3; jobs.put(slot, job); managedSlots.add(slot); setDirty();
        }
        else cleanup(slot, session);
    }

    public int lastOperations() { return lastOperations; }
    public long lastNanos() { return lastNanos; }
    private static boolean chunksReady(ServerLevel level, Job job) {
        job.loadingLevel = level;
        for (int x = job.origin.getX() >> 4; x <= (job.origin.getX() + 25) >> 4; x++) {
            for (int z = job.origin.getZ() >> 4; z <= (job.origin.getZ() + 24) >> 4; z++) {
                var chunk = new net.minecraft.world.level.ChunkPos(x, z);
                job.tickets.add(chunk);
                level.getChunkSource().addRegionTicket(net.minecraft.server.level.TicketType.PORTAL, chunk, 3, job.origin);
                if (level.getChunkSource().getChunkNow(x, z) == null) {
                    // Request generation/loading, but never join its future on the server tick thread.
                    level.getChunkSource().getChunkFuture(x, z, net.minecraft.world.level.chunk.ChunkStatus.FULL, true);
                    return false;
                }
            }
        }
        return true;
    }
    private static void releaseTickets(ServerLevel level, Job job) {
        for (var chunk : job.tickets) level.getChunkSource().removeRegionTicket(net.minecraft.server.level.TicketType.PORTAL, chunk, 3, job.origin);
        job.tickets.clear();
    }
    public void tick(MinecraftServer server) {
        ServerLevel level = server.getLevel(DimensionRegistry.GODLY_TRIALS);
        if (level == null) return;
        long start = System.nanoTime(); lastOperations = 0;
        for (Job job : List.copyOf(jobs.values())) {
            if (job.stage == 2 || job.stage == 4) continue;
            TrialSession session = TrialSessionManager.getSessionById(job.session);
            if (job.stage < 2 && session == null) { cleanup(job.slot, job.session); continue; }
            if (!chunksReady(level, job)) break;
            while (lastOperations < BLOCK_BUDGET && System.nanoTime() - start < 2_000_000L) {
                BlockPos local = new BlockPos(job.cursor % 26, job.cursor / (26 * 25), job.cursor / 26 % 25);
                BlockPos pos = job.origin.offset(local);
                lastOperations++;
                if (job.stage == 0 && (!level.isEmptyBlock(pos) || level.getBlockEntity(pos) != null)) {
                    job.stage = 4; job.blocks = null; job.palette = null; job.template = null; setDirty();
                    releaseTickets(level, job);
                    session.fail(TrialFailureReason.GENERIC_FAILURE);
                    break;
                }
                if (job.stage == 1) {
                    CompoundTag entry = job.blocks.get(local);
                    if (entry != null) {
                        BlockState state = job.palette.get(entry.getInt("state"));
                        if (!state.is(Blocks.STRUCTURE_BLOCK)) {
                            level.setBlock(pos, state, 2 | 16);
                            if (entry.contains("nbt") && level.getBlockEntity(pos) != null) {
                                level.getBlockEntity(pos).load(entry.getCompound("nbt").copy());
                                level.getBlockEntity(pos).setChanged();
                            }
                        }
                    }
                } else if (job.stage == 3 && !level.isEmptyBlock(pos)) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2 | 16);
                job.cursor++; setDirty();
                if (job.cursor == VOLUME) {
                    job.cursor = 0;
                    if (job.stage == 3) { jobs.remove(job.slot); releaseTickets(level, job); TrialSessionManager.releaseArenaSlot(job.slot); }
                    else if (++job.stage == 2) {
                        job.blocks = null; job.palette = null; job.template = null;
                        try { session.arenaReady(); }
                        catch (RuntimeException failure) {
                            DivinityEngineMod.LOGGER.error("Trial arena entry failed for {}", job.session, failure);
                            session.failAndClose(TrialFailureReason.GENERIC_FAILURE);
                        } finally { releaseTickets(level, job); }
                    }
                    break;
                }
            }
            if (lastOperations >= BLOCK_BUDGET || System.nanoTime() - start >= 2_000_000L) break;
        }
        lastNanos = System.nanoTime() - start;
        if (lastNanos > 50_000_000L) DivinityEngineMod.LOGGER.warn("Trial arena job took {} ms for {} operations (chunk loading included)", lastNanos / 1_000_000, lastOperations);
    }
}
