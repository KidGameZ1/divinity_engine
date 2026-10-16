package net.nightshade.divinity_engine.util.divinity.blessings.aethon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.nightshade.divinity_engine.registry.dimensions.DimensionRegistry;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals;
import net.nightshade.renderlib.api.*;
import net.nightshade.renderlib.server.RenderEffectSavedData;

import java.util.*;

import static net.nightshade.divinity_engine.util.divinity.blessings.aethon.PocketDomainData.*;
import static net.nightshade.divinity_engine.util.divinity.blessings.aethon.PocketDomainManager.*;

/** Saved, shared gateways. Only the owner can place or close them; idle destinations are not kept loaded. */
public final class PocketPortals {
    private record Gate(String key, ReturnPoint anchor, int destination) {}
    private record Crossing(Domain domain, Gate gate) {}
    private record Waiting(Entity entity, Crossing crossing, Vec3 position, Vec3 velocity, long expires) {}
    private record Visual(UUID owner, ServerLevel level, RenderHandle handle, ReturnPoint anchor) {}
    private static final Map<String, Visual> VISUALS = new HashMap<>();
    private static final Set<UUID> EXIT_REQUESTS = new HashSet<>();
    private static final Set<UUID> FALLBACK_EXITS = new HashSet<>();
    private static MinecraftServer visualServer;
    private static final Map<UUID, Entity> LATCHED = new HashMap<>();
    private static final Map<UUID, Waiting> WAITING = new LinkedHashMap<>();
    private static final Set<UUID> GATE_LOADS = new HashSet<>();
    private static int scanCursor;
    private static int waitCursor;
    private PocketPortals() {}

    public static boolean place(ServerPlayer player, boolean entrance) {
        if (!eligible(player) || entrance == inPocket(player)) {
            tell(player, entrance ? "Place your entrance outside the realm with the blessing equipped."
                    : "Place your exit inside one of your completed chambers."); return false;
        }
        Domain domain = domain(player);
        int roomIndex = currentRoom(player, domain);
        Room room = roomIndex < 0 ? null : domain.rooms.get(roomIndex);
        if (!entrance && room == null) {
            tell(player, "Stand inside your own completed chamber to place its exit."); return false;
        }
        if ((entrance ? domain.entrance : roomExit(domain, room)) != null) {
            tell(player, "Close the existing portal first. It stays where you placed it until you do."); return false;
        }
        HitResult hit = player.pick(6, 1, false);
        if (!(hit instanceof BlockHitResult block) || hit.getType() != HitResult.Type.BLOCK || block.getDirection() != Direction.UP) {
            tell(player, "Aim at the top of a nearby solid floor, then place the portal."); return false;
        }
        Vec3 pos = Vec3.atBottomCenterOf(block.getBlockPos().above());
        ReturnPoint anchor = new ReturnPoint(player.level().dimension().location().toString(), pos,
                player.getDirection().toYRot(), 0);
        if (!clear(player.serverLevel(), anchor) || !player.serverLevel().getWorldBorder().isWithinBounds(BlockPos.containing(pos))) {
            tell(player, "The portal needs a solid floor and clear space above it. No blocks were removed."); return false;
        }
        if (entrance) {
            if (domain.selectedRoom() == null || !domain.selectedRoom().ready()) {
                tell(player, "Create a completed chamber and select its arrival first."); return false;
            }
            domain.entrance = anchor;
        } else {
            if (!fits(room, room.size, pos)) {
                tell(player, "Place the exit fully inside your own completed chamber."); return false;
            }
            if (gates(domain).stream().anyMatch(gate -> gate.anchor.dimension().equals(anchor.dimension())
                    && box(gate.anchor).intersects(box(anchor)))) {
                tell(player, "Leave space between this exit and the floor gateways."); return false;
            }
            room.exit = anchor;
            room.exitClosed = false;
        }
        domain.nextPortal = player.server.overworld().getGameTime() + 30;
        domain.leavePortalFirst = true;
        PocketDomainData.get(player.server).setDirty();
        player.serverLevel().playSound(null, BlockPos.containing(pos), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.6f, 1.5f);
        tell(player, (entrance ? "Entrance" : "Exit") + " anchored. Walk through it to travel. Closing it will not damage terrain.");
        return true;
    }

    public static boolean close(ServerPlayer player, boolean entrance) {
        // Removing your own gateway is always allowed, even after losing access to the blessing.
        Domain domain = PocketDomainData.get(player.server).find(player.getUUID());
        if (domain == null || !player.isAlive() || player.isSpectator()) return false;
        if (!entrance) {
            int room = currentRoom(player, domain);
            if (room < 0) { tell(player, "Stand in the chamber whose exit you want to close, or select it in the Portals tab."); return false; }
            return closeRoom(player, room);
        }
        domain.entrance = null;
        PocketDomainData.get(player.server).setDirty();
        recoverVisuals(player.server);
        clearVisual(domain.owner + "/entrance");
        tell(player, "Portal closed. You can now place it somewhere else.");
        return true;
    }

    public static int currentRoom(ServerPlayer player, Domain domain) {
        if (!inPocket(player)) return -1;
        for (int i = 0; i < domain.rooms.size(); i++)
            if (domain.rooms.get(i).ready() && domain.rooms.get(i).contains(player.blockPosition())) return i;
        return -1;
    }

    public static boolean closeRoom(ServerPlayer player, int index) {
        Domain domain = PocketDomainData.get(player.server).find(player.getUUID());
        if (domain == null || !player.isAlive() || player.isSpectator() || index < 0 || index >= domain.rooms.size()) return false;
        Room room = domain.rooms.get(index);
        if (!room.ready()) return false;
        room.exit = null;
        room.exitClosed = true;
        if (legacyExit(domain, room)) domain.exit = null;
        PocketDomainData.get(player.server).setDirty();
        recoverVisuals(player.server);
        clearVisual(domain.owner + "/exit_" + room.origin.asLong());
        clearVisual(domain.owner + "/exit");
        tell(player, "This chamber's exit is closed. Stand here and aim at a floor to place it elsewhere. Other gateways are unchanged.");
        return true;
    }

    private static boolean legacyExit(Domain domain, Room room) {
        return domain.exit != null && domain.exit.dimension().equals(DimensionRegistry.POCKET_DIMENSION.location().toString())
                && room.contains(BlockPos.containing(domain.exit.position()));
    }

    public static ReturnPoint customRoomExit(Domain domain, Room room) {
        if (room.exitClosed) return null;
        return room.exit != null ? room.exit : legacyExit(domain, room) ? domain.exit : null;
    }

    /** Default exits are derived from bounds; player-placed and legacy exits retain exact saved positions. */
    public static ReturnPoint roomExit(Domain domain, Room room) {
        if (room == null || !room.ready() || room.exitClosed) return null;
        ReturnPoint custom = customRoomExit(domain, room);
        if (custom != null) return custom;
        int width = room.legacy ? 29 : room.size.width(), depth = room.legacy ? 31 : room.size.depth();
        Vec3 position = Vec3.atLowerCornerOf(room.origin).add(width / 2 + 0.5, room.legacy ? 1 : 2, depth - 2.5);
        return new ReturnPoint(DimensionRegistry.POCKET_DIMENSION.location().toString(), position, 180, 0);
    }

    public static boolean fits(Room room, Dimensions size, Vec3 pos) {
        int width = room.legacy ? 29 : size.width(), depth = room.legacy ? 31 : size.depth(), height = room.legacy ? 17 : size.height();
        double x = pos.x - room.origin.getX(), y = pos.y - room.origin.getY(), z = pos.z - room.origin.getZ();
        return x >= 2 && x <= width - 2 && z >= 2 && z <= depth - 2 && y >= 1 && y + 2.3 < height - 1;
    }

    public static Vec3 roomGate(Room room, boolean above) {
        int width = room.legacy ? 29 : room.size.width();
        return Vec3.atLowerCornerOf(room.origin).add(above ? width - 2.5 : 2.5, room.legacy ? 1 : 2, 2.5);
    }

    private static List<Gate> gates(Domain domain) {
        List<Gate> result = new ArrayList<>();
        if (domain.entrance != null) result.add(new Gate("entrance", domain.entrance, domain.selected));
        // Preserve even an unmatched old anchor rather than silently relocating it.
        if (domain.exit != null && domain.rooms.stream().noneMatch(room -> legacyExit(domain, room)))
            result.add(new Gate("exit", domain.exit, -1));
        String dimension = DimensionRegistry.POCKET_DIMENSION.location().toString();
        for (int i = 0; i < domain.rooms.size(); i++) {
            Room room = domain.rooms.get(i);
            if (!room.ready()) continue;
            ReturnPoint exit = roomExit(domain, room);
            if (exit != null) result.add(new Gate("exit_" + room.origin.asLong(), exit, -1));
            if (i > 0 && domain.rooms.get(i - 1).ready())
                result.add(new Gate("down_" + room.origin.asLong(), new ReturnPoint(dimension, roomGate(room, false), 0, 0), i - 1));
            if (i + 1 < domain.rooms.size() && domain.rooms.get(i + 1).ready())
                result.add(new Gate("up_" + room.origin.asLong(), new ReturnPoint(dimension, roomGate(room, true), 0, 0), i + 1));
        }
        return result;
    }

    public static ReturnPoint outside(ReturnPoint entrance) {
        return new ReturnPoint(entrance.dimension(), entrance.position().add(forward(entrance).scale(-2)), entrance.yaw() + 180, 0);
    }

    private static Vec3 forward(ReturnPoint anchor) { return Vec3.directionFromRotation(0, anchor.yaw()); }
    private static AABB box(ReturnPoint anchor) {
        boolean alongZ = Math.abs(forward(anchor).z) > 0.5;
        return new AABB(anchor.position().x - (alongZ ? 0.85 : 0.28), anchor.position().y, anchor.position().z - (alongZ ? 0.28 : 0.85),
                anchor.position().x + (alongZ ? 0.85 : 0.28), anchor.position().y + 2.2, anchor.position().z + (alongZ ? 0.28 : 0.85));
    }

    public static boolean clear(ServerLevel level, ReturnPoint anchor) {
        BlockPos feet = BlockPos.containing(anchor.position());
        if (feet.getY() < level.getMinBuildHeight() + 1 || feet.getY() + 3 >= level.getMaxBuildHeight()) return false;
        AABB box = box(anchor);
        if (!PocketChunkLoading.loaded(level, box)) return false;
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY, box.minZ), BlockPos.containing(box.maxX, box.maxY, box.maxZ)))
            if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                    || !level.getFluidState(pos).isEmpty()) return false;
        return level.getBlockState(feet.below()).isFaceSturdy(level, feet.below(), Direction.UP);
    }

    private static boolean touching(Entity player, ReturnPoint anchor) {
        return player.level().dimension().location().toString().equals(anchor.dimension()) && box(anchor).intersects(player.getBoundingBox());
    }
    public static boolean atEntrance(ServerPlayer player, Domain domain) {
        return domain.entrance != null && touching(player, domain.entrance) && clear(player.serverLevel(), domain.entrance);
    }

    public static boolean queueExit(ServerPlayer player) { return EXIT_REQUESTS.add(player.getUUID()); }
    public static boolean exitPending(ServerPlayer player) { return EXIT_REQUESTS.contains(player.getUUID()); }
    public static boolean fallbackExit(ServerPlayer player) { return FALLBACK_EXITS.contains(player.getUUID()); }
    public static void useFallbackExit(ServerPlayer player) { FALLBACK_EXITS.add(player.getUUID()); }
    public static void traveled(ServerPlayer player) {
        EXIT_REQUESTS.remove(player.getUUID());
        FALLBACK_EXITS.remove(player.getUUID());
        GATE_LOADS.remove(player.getUUID());
        PocketChunkLoading.release(player.getUUID());
    }

    public static void tick(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator() || player.isPassenger() || player.isVehicle()) return;
        if (EXIT_REQUESTS.contains(player.getUUID())) {
            if (!inPocket(player)) traveled(player);
            else if (player.tickCount % 10 == 0) exit(player);
            return;
        }
        Crossing crossing = crossings(player.server).stream().filter(value -> touching(player, value.gate.anchor)).findFirst().orElse(null);
        if (crossing == null) {
            player.getPersistentData().remove("PocketPortalLatched");
            if (GATE_LOADS.remove(player.getUUID())) PocketChunkLoading.release(player.getUUID());
            return;
        }
        cross(player, crossing);
    }

    private static List<Crossing> crossings(MinecraftServer server) {
        List<Crossing> result = new ArrayList<>();
        for (Domain domain : PocketDomainData.get(server).domains()) for (Gate gate : gates(domain)) result.add(new Crossing(domain, gate));
        return result;
    }

    private static boolean cross(Entity entity, Crossing crossing) {
        CompoundTag state = entity.getPersistentData();
        if (state.getBoolean("PocketPortalLatched")) { LATCHED.put(entity.getUUID(), entity); return false; }
        ServerLevel level = (ServerLevel) entity.level();
        long now = level.getServer().overworld().getGameTime();
        if (state.getLong("PocketPortalCooldown") > now || !clear(level, crossing.gate.anchor)) return false;
        if (entity instanceof ServerPlayer) GATE_LOADS.add(entity.getUUID());
        Entity moved = crossGateway(entity, crossing.domain, crossing.gate.destination);
        if (moved == null) {
            if (!(entity instanceof ServerPlayer) && PocketChunkLoading.pending(entity.getUUID()) && WAITING.size() < 128)
                WAITING.putIfAbsent(entity.getUUID(), new Waiting(entity, crossing, entity.position(), entity.getDeltaMovement(), now + 200));
            return false;
        }
        Waiting waiting = WAITING.remove(moved.getUUID());
        if (waiting != null && moved instanceof net.minecraft.world.entity.projectile.Projectile) moved.setDeltaMovement(waiting.velocity);
        moved.getPersistentData().putLong("PocketPortalCooldown", now + 40);
        moved.getPersistentData().putBoolean("PocketPortalLatched", true);
        LATCHED.put(moved.getUUID(), moved);
        return true;
    }

    public static void tickEntities(MinecraftServer server) {
        List<Crossing> crossings = crossings(server);
        long now = server.overworld().getGameTime();
        for (Waiting waiting : List.copyOf(WAITING.values())) {
            Entity entity = waiting.entity;
            if (entity.isRemoved() || now >= waiting.expires || !entity.level().dimension().location().toString().equals(waiting.crossing.gate.anchor.dimension())
                    || !crossings.contains(waiting.crossing)) {
                WAITING.remove(entity.getUUID()); PocketChunkLoading.release(entity.getUUID());
                if (!entity.isRemoved()) entity.setDeltaMovement(waiting.velocity);
            } else {
                // Hold a crossing item/projectile safely while its destination wakes; never drop or clone it.
                entity.setPos(waiting.position); entity.setDeltaMovement(Vec3.ZERO);
            }
        }
        LATCHED.entrySet().removeIf(entry -> {
            Entity entity = entry.getValue();
            if (entity.isRemoved()) return true;
            if (crossings.stream().noneMatch(crossing -> touching(entity, crossing.gate.anchor))) {
                entity.getPersistentData().remove("PocketPortalLatched"); return true;
            }
            return false;
        });
        if (crossings.isEmpty()) return;
        int attempts = 0;
        List<Waiting> waiting = List.copyOf(WAITING.values());
        for (int i = 0; i < Math.min(4, waiting.size()); i++) {
            Waiting next = waiting.get(Math.floorMod(waitCursor++, waiting.size()));
            cross(next.entity, next.crossing); attempts++;
        }
        for (int checked = 0; checked < Math.min(16, crossings.size()) && attempts < 4; checked++) {
            Crossing crossing = crossings.get(Math.floorMod(scanCursor++, crossings.size()));
            ResourceLocation dimension = ResourceLocation.tryParse(crossing.gate.anchor.dimension());
            if (dimension == null) continue;
            ServerLevel level = server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, dimension));
            AABB portal = box(crossing.gate.anchor);
            if (level == null || !PocketChunkLoading.loaded(level, portal)) continue;
            // Local loaded-entity queries only; a portal never loads an empty source or destination by itself.
            for (Entity entity : level.getEntities((Entity) null, portal.inflate(4), value -> !(value instanceof ServerPlayer) && value.isAlive())) {
                Vec3 previous = new Vec3(entity.xOld, entity.yOld, entity.zOld);
                boolean crossed = portal.intersects(entity.getBoundingBox()) || previous.distanceToSqr(entity.position()) <= 256
                        && portal.clip(previous, entity.position()).isPresent();
                if (crossed && !WAITING.containsKey(entity.getUUID()) && !entity.isPassenger() && !entity.isVehicle() && entity.canChangeDimensions()
                        && !entity.getPersistentData().getBoolean("PocketPortalLatched")
                        && entity.getPersistentData().getLong("PocketPortalRetry") <= server.overworld().getGameTime()) {
                    entity.getPersistentData().putLong("PocketPortalRetry", server.overworld().getGameTime() + 10);
                    cross(entity, crossing);
                    if (++attempts >= 4) break;
                } else if (crossed && entity.getPersistentData().getBoolean("PocketPortalLatched")) LATCHED.put(entity.getUUID(), entity);
            }
        }
    }

    public static void visuals(MinecraftServer server) {
        recoverVisuals(server);
        int budget = 48;
        Set<String> visible = new HashSet<>();
        for (Domain domain : PocketDomainData.get(server).domains()) for (Gate gate : gates(domain)) {
            if (budget == 0) break;
            ResourceLocation dimension = ResourceLocation.tryParse(gate.anchor.dimension());
            if (dimension == null) continue;
            ServerLevel level = server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, dimension));
            if (level == null || level.getChunkSource().getChunkNow(net.minecraft.util.Mth.floor(gate.anchor.position().x) >> 4,
                    net.minecraft.util.Mth.floor(gate.anchor.position().z) >> 4) == null
                    || level.players().stream().noneMatch(player -> player.distanceToSqr(gate.anchor.position()) < 40 * 40)) continue;
            String key = domain.owner + "/" + gate.key;
            visible.add(key);
            Visual old = VISUALS.get(key);
            budget--;
            if (old != null && old.level == level && old.anchor.equals(gate.anchor)
                    && RenderEffectSavedData.get(level).get(old.handle.id()).isPresent()) continue;
            if (old != null) RenderEffects.remove(old.level, old.handle);
            CompoundTag params = new CompoundTag();
            params.putUUID("PocketPortalOwner", domain.owner);
            params.putString("PocketPortalKey", gate.key);
            params.putFloat("PocketPortalYaw", gate.anchor.yaw());
            params.putString("BlessingId", "divinity_engine:pocket_dimension");
            params.putString("Cue", "MARK");
            params.putDouble("ShapeRadius", 2); params.putDouble("Radius", 3);
            Vec3 direction = forward(gate.anchor);
            params.putDouble("DirectionX", direction.x); params.putDouble("DirectionY", 0); params.putDouble("DirectionZ", direction.z);
            params.putDouble("LinkX", gate.destination < 0 ? 1 : 0); params.putDouble("LinkZ", 0);
            params.putDouble("LinkY", gate.key.startsWith("up_") ? 1 : gate.key.startsWith("down_") ? -1 : 0);
            params.putInt("Accent", gate.destination < 0 ? 0xFFE7C578 : 0xFF76BCE0);
            params.putInt("Highlight", gate.destination < 0 ? 0xFFFFE7AA : 0xFFE2EBFF);
            try {
                RenderHandle handle = RenderEffects.spawn(level, RenderEffectSpec.builder(BlessingVisuals.TYPE, BlessingVisuals.TYPE)
                        .anchor(RenderAnchor.world(gate.anchor.position())).duration(Long.MAX_VALUE)
                        .palette(new RenderPalette(List.of(0xFF6086D6, 0xFF83DBDE), 40))
                        .visibility(new RenderVisibility(40, false, false, true)).parameters(params).build());
                VISUALS.put(key, new Visual(domain.owner, level, handle, gate.anchor));
            } catch (RuntimeException exhaustedBudget) { budget = 0; }
        }
        VISUALS.entrySet().removeIf(entry -> {
            if (visible.contains(entry.getKey())) return false;
            RenderEffects.remove(entry.getValue().level, entry.getValue().handle); return true;
        });
    }

    private static void recoverVisuals(MinecraftServer server) {
        if (visualServer == server) return;
        VISUALS.clear(); visualServer = server;
        // Core persists indefinite effects. Adopt our tagged handles after restart instead of duplicating them.
        for (ServerLevel level : server.getAllLevels()) for (RenderEffectInstance effect : RenderEffectSavedData.get(level).snapshot(level.getGameTime())) {
            CompoundTag params = effect.spec().parameters();
            if (!effect.spec().type().equals(BlessingVisuals.TYPE) || !params.hasUUID("PocketPortalOwner") || !params.contains("PocketPortalKey")) continue;
            UUID owner = params.getUUID("PocketPortalOwner");
            String key = owner + "/" + params.getString("PocketPortalKey");
            RenderHandle handle = new RenderHandle(effect.id(), level.dimension(), true);
            Visual duplicate = VISUALS.putIfAbsent(key, new Visual(owner, level, handle,
                    new ReturnPoint(level.dimension().location().toString(), effect.spec().anchor().position(), params.getFloat("PocketPortalYaw"), 0)));
            if (duplicate != null) RenderEffects.remove(level, handle);
        }
    }

    private static void clearVisual(String key) {
        Visual visual = VISUALS.remove(key);
        if (visual != null) RenderEffects.remove(visual.level, visual.handle);
    }
    public static void cancelWaiting() {
        for (Waiting waiting : WAITING.values()) {
            if (!waiting.entity.isRemoved()) waiting.entity.setDeltaMovement(waiting.velocity);
            PocketChunkLoading.release(waiting.entity.getUUID());
        }
        WAITING.clear();
    }
    public static void stopped() { cancelWaiting(); VISUALS.clear(); EXIT_REQUESTS.clear(); FALLBACK_EXITS.clear(); GATE_LOADS.clear(); LATCHED.clear(); scanCursor = 0; waitCursor = 0; visualServer = null; }
}
