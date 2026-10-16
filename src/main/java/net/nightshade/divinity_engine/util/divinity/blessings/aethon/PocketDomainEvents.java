package net.nightshade.divinity_engine.util.divinity.blessings.aethon;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityMobGriefingEvent;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.nightshade.divinity_engine.registry.blocks.BlocksRegistry;
import net.nightshade.divinity_engine.registry.dimensions.DimensionRegistry;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

import static net.nightshade.divinity_engine.util.divinity.blessings.aethon.PocketDomainData.*;
import static net.nightshade.divinity_engine.util.divinity.blessings.aethon.PocketDomainManager.*;

/** No world-wide combat scans. Domain rules are checked only for events inside the pocket dimension. */
@Mod.EventBusSubscriber(modid = "divinity_engine")
public final class PocketDomainEvents {
    private static final Queue<LivingDeathEvent> DEATHS = new ArrayDeque<>();
    private PocketDomainEvents() {}

    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { DEATHS.clear(); PocketPortals.stopped(); PocketChunkLoading.stopped(); }
    @SubscribeEvent public static void stopping(net.minecraftforge.event.server.ServerStoppingEvent event) { PocketPortals.cancelWaiting(); }

    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) { PocketChunkLoading.tick(event.getServer()); return; }
        while (!DEATHS.isEmpty()) {
            LivingDeathEvent death = DEATHS.poll();
            if (!death.isCanceled() && !death.getEntity().isAlive()) {
                var data = PocketDomainData.get(event.getServer());
                var tag = death.getEntity().getPersistentData();
                Domain owner = tag.hasUUID(RESIDENT_OWNER) ? data.find(tag.getUUID(RESIDENT_OWNER)) : null;
                if (owner != null && owner.residents.remove(death.getEntity().getUUID()) != null) data.setDirty();
            }
        }
        buildTick(event.getServer());
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) PocketPortals.tick(player);
        PocketPortals.tickEntities(event.getServer());
        if (event.getServer().overworld().getGameTime() % 20 != 0) return;
        PocketPortals.visuals(event.getServer());
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            if (inPocket(player) && player.isAlive() && PocketDomainData.get(event.getServer()).at(player.blockPosition()) == null) exit(player);
            else if (!inPocket(player)) PocketDomainData.get(event.getServer()).leave(player.getUUID());
        }
        ServerLevel pocket = event.getServer().getLevel(DimensionRegistry.POCKET_DIMENSION);
        if (pocket == null) return;
        // An unlimited saved resident list must not become an unlimited per-tick scan.
        // Loaded entities include those intentionally kept awake by another mod's chunk loader.
        for (Entity entity : pocket.getAllEntities()) {
            if (!(entity instanceof Mob mob)) continue;
            Room room = residentRoom(mob);
            if (room == null || !pocket.getChunkSource().isPositionTicking(mob.chunkPosition().toLong())) continue;
            if (!room.allows(Rule.RESIDENT_COMBAT) && mob.getTarget() != null) { mob.setTarget(null); mob.getNavigation().stop(); }
            if (!room.contains(mob.blockPosition())) {
                Vec3 safe = safePosition(pocket, mob, room.arrival().add(3, 0, 0), room);
                if (safe != null) { mob.teleportTo(safe.x, safe.y, safe.z); mob.setDeltaMovement(Vec3.ZERO); }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST) public static void death(LivingDeathEvent event) {
        if (!inPocket(event.getEntity()) || event.getEntity().level().isClientSide) return;
        if (!event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                && (reviveResident(event.getEntity()) || event.getEntity() instanceof ServerPlayer player && emergencyReturn(player))) {
            event.setCanceled(true);
        } else if (event.getEntity().getPersistentData().hasUUID(RESIDENT_OWNER)) DEATHS.add(event);
    }

    public static Room residentRoom(Entity entity) {
        if (entity == null || !inPocket(entity) || !(entity.level() instanceof ServerLevel level)
                || !entity.getPersistentData().hasUUID(RESIDENT_OWNER)) return null;
        Domain owner = PocketDomainData.get(level.getServer()).find(entity.getPersistentData().getUUID(RESIDENT_OWNER));
        Resident resident = owner == null ? null : owner.residents.get(entity.getUUID());
        return resident == null || resident.room() < 0 || resident.room() >= owner.rooms.size() ? null : owner.rooms.get(resident.room());
    }
    private static boolean peaceful(Entity entity) {
        Room room = residentRoom(entity);
        return room != null && !room.allows(Rule.RESIDENT_COMBAT);
    }
    @SubscribeEvent public static void residentPortal(EntityTravelToDimensionEvent event) {
        if (residentRoom(event.getEntity()) != null && !isTransferring(event.getEntity())) event.setCanceled(true);
    }
    @SubscribeEvent public static void removed(EntityLeaveLevelEvent event) {
        Entity entity = event.getEntity();
        // Discard/conversion is not a reversible lethal hit. Retain records on ordinary chunk unloads.
        if (!(event.getLevel() instanceof ServerLevel level) || !inPocket(entity) || isTransferring(entity)
                || entity.getRemovalReason() != Entity.RemovalReason.DISCARDED || !entity.getPersistentData().hasUUID(RESIDENT_OWNER)) return;
        var data = PocketDomainData.get(level.getServer());
        Domain owner = data.find(entity.getPersistentData().getUUID(RESIDENT_OWNER));
        if (owner != null && owner.residents.remove(entity.getUUID()) != null) data.setDirty();
    }
    @SubscribeEvent public static void pickup(EntityItemPickupEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && inPocket(player)
                && !domain(player).contains(event.getItem().blockPosition())) event.setCanceled(true);
    }
    @SubscribeEvent public static void residentTeleport(EntityTeleportEvent.EnderEntity event) {
        Room room = residentRoom(event.getEntity());
        if (room != null && !room.contains(BlockPos.containing(event.getTarget()))) event.setCanceled(true);
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST) public static void attack(LivingAttackEvent event) {
        if (inPocket(event.getEntity()) && event.getSource().getEntity() != null
                && (peaceful(event.getEntity()) || peaceful(event.getSource().getEntity())
                    || event.getSource().getEntity() instanceof ServerPlayer player && !domain(player).contains(event.getEntity().blockPosition())))
            event.setCanceled(true);
    }
    @SubscribeEvent(priority = EventPriority.LOWEST) public static void target(LivingChangeTargetEvent event) {
        if (peaceful(event.getEntity()) || peaceful(event.getNewTarget())) event.setNewTarget(null);
    }

    public static boolean mayBuild(ServerPlayer player, BlockPos pos) {
        if (!inPocket(player)) return true;
        if (player.level().getBlockState(pos).is(BlocksRegistry.POCKET_DIMENSION.get())) return false;
        return domain(player).rooms.stream().anyMatch(room -> room.ready() && room.contains(pos) && room.allows(Rule.OWNER_BUILDING));
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST) public static void breakBlock(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player && !mayBuild(player, event.getPos())) event.setCanceled(true);
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST) public static void placeBlock(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !level.dimension().equals(DimensionRegistry.POCKET_DIMENSION)) return;
        boolean allowed = event.getEntity() instanceof ServerPlayer player && mayBuild(player, event.getPos());
        if (event instanceof BlockEvent.EntityMultiPlaceEvent multi && event.getEntity() instanceof ServerPlayer player)
            allowed &= multi.getReplacedBlockSnapshots().stream().allMatch(snapshot -> mayBuild(player, snapshot.getPos()));
        if (!allowed) event.setCanceled(true);
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST) public static void interactBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player && inPocket(player)
                && (!mayBuild(player, event.getPos()) || event.getFace() != null && !domain(player).contains(event.getPos().relative(event.getFace()))))
            event.setCanceled(true);
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST) public static void useItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity() instanceof ServerPlayer player && inPocket(player) && !mayBuild(player, player.blockPosition())) event.setCanceled(true);
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST) public static void interactEntity(PlayerInteractEvent.EntityInteract event) {
        if (event.getEntity() instanceof ServerPlayer player && inPocket(player)
                && !domain(player).contains(event.getTarget().blockPosition())) event.setCanceled(true);
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST) public static void interactSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (event.getEntity() instanceof ServerPlayer player && inPocket(player)
                && !domain(player).contains(event.getTarget().blockPosition())) event.setCanceled(true);
    }
    @SubscribeEvent public static void grief(EntityMobGriefingEvent event) {
        if (inPocket(event.getEntity())) event.setResult(Event.Result.DENY);
    }
    @SubscribeEvent public static void explosion(ExplosionEvent.Detonate event) {
        if (event.getLevel().dimension().equals(DimensionRegistry.POCKET_DIMENSION)) event.getAffectedBlocks().clear();
    }
    @SubscribeEvent public static void spawn(MobSpawnEvent.PositionCheck event) {
        if (!inPocket(event.getEntity()) || event.getSpawnType() != MobSpawnType.NATURAL) return;
        boolean allow = PocketDomainData.get(event.getLevel().getLevel().getServer()).domains().stream()
                .flatMap(domain -> domain.rooms.stream()).anyMatch(room -> room.ready() && room.contains(event.getEntity().blockPosition())
                        && room.allows(Rule.NATURAL_SPAWNING));
        if (!allow) event.setResult(Event.Result.DENY);
    }
}
