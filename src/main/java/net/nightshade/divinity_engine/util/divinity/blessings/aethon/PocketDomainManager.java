package net.nightshade.divinity_engine.util.divinity.blessings.aethon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.ITeleporter;
import net.minecraftforge.network.NetworkHooks;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.registry.blocks.BlocksRegistry;
import net.nightshade.divinity_engine.registry.dimensions.DimensionRegistry;
import net.nightshade.divinity_engine.registry.divinity.blessing.BlessingsRegistry;
import net.nightshade.divinity_engine.util.DivinityEngineHelper;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals;
import net.nightshade.divinity_engine.util.divinity.gods.GodHelper;
import net.nightshade.divinity_engine.util.divinity.narrative.BlessingSpecializations;
import net.nightshade.divinity_engine.util.divinity.trials.TrialSessionManager;
import net.nightshade.divinity_engine.world.inventory.PocketDimensionInvMenu;
import net.nightshade.divinity_engine.world.inventory.PocketDomainMenu;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;
import java.util.function.Function;

import static net.nightshade.divinity_engine.util.divinity.blessings.aethon.PocketDomainData.*;

/** All domain access and entity transfers are server-owned; client buttons only request actions. */
public final class PocketDomainManager {
    public static final String RESIDENT_OWNER = "divinity_engine.domain_owner";
    private static final Set<UUID> TRANSFERRING = new HashSet<>();
    public static boolean isTransferring(Entity entity) { return TRANSFERRING.contains(entity.getUUID()); }
    private PocketDomainManager() {}

    public static boolean inPocket(Entity entity) { return entity.level().dimension().equals(DimensionRegistry.POCKET_DIMENSION); }
    public static BlessingsInstance equipped(ServerPlayer player) {
        for (int i = 0; i < 3; i++) {
            BlessingsInstance instance = DivinityEngineHelper.getBlessingInSlot(player, i);
            if (instance != null && instance.getBlessing() == BlessingsRegistry.POCKET_DIMENSION.get()) return instance;
        }
        return null;
    }
    public static boolean eligible(ServerPlayer player) {
        return player.isAlive() && eligibleBlessing(player);
    }
    private static boolean eligibleBlessing(ServerPlayer player) {
        if (net.nightshade.divinity_engine.util.divinity.innerworld.CommunionManager.busy(player)) return false;
        BlessingsInstance instance = equipped(player);
        return !player.isSpectator() && instance != null
                && BlessingSpecializations.isEligible(player, instance)
                && !player.level().dimension().equals(DimensionRegistry.GODLY_TRIALS)
                && TrialSessionManager.getSession(player) == null;
    }
    public static Domain domain(ServerPlayer player) {
        PocketDomainData data = PocketDomainData.get(player.server);
        Domain domain = data.getOrCreate(player.getUUID());
        if (!domain.imported) {
            BlessingsInstance old = equipped(player);
            if (old == null) old = GodHelper.getContactedGodsFrom(player).getContactedGods().stream()
                    .flatMap(god -> god.getBlessingsInstances().stream())
                    .filter(instance -> instance.getBlessing() == BlessingsRegistry.POCKET_DIMENSION.get()).findFirst().orElse(null);
            if (old != null) data.importLegacy(domain, old.getOrCreateTag());
        }
        return domain;
    }
    public static boolean canManage(ServerPlayer player) {
        if (net.nightshade.divinity_engine.util.divinity.innerworld.CommunionManager.busy(player)) return false;
        if (!player.isAlive() || player.isSpectator()) return false;
        Domain domain = PocketDomainData.get(player.server).find(player.getUUID());
        return eligible(player) || inPocket(player) && domain != null && domain.contains(player.blockPosition());
    }

    public static void openControl(ServerPlayer player) {
        openControl(player, 0, domain(player).selected);
    }

    public static void openControl(ServerPlayer player, int tab, int room) {
        openControl(player, tab, room, 0);
    }

    public static void openControl(ServerPlayer player, int tab, int room, int residentPage) {
        if (!canManage(player)) { tell(player, "Equip an eligible Pocket Dimension blessing to command your domain."); return; }
        Domain domain = domain(player);
        CompoundTag snapshot = snapshot(domain, residentPage);
        snapshot.putInt("Tab", tab);
        snapshot.putInt("Room", room);
        snapshot.putInt("CurrentRoom", PocketPortals.currentRoom(player, domain));
        NetworkHooks.openScreen(player, new SimpleMenuProvider((id, inv, ignored) -> new PocketDomainMenu(id, inv, snapshot),
                Component.literal("Aethon's Small World")), buffer -> buffer.writeNbt(snapshot));
    }
    public static void openStorage(ServerPlayer player) {
        if (!canManage(player)) return;
        domain(player);
        NetworkHooks.openScreen(player, new SimpleMenuProvider((id, inv, ignored) -> new PocketDimensionInvMenu(id, inv, null),
                Component.literal("Astral Storage")), buffer -> buffer.writeBlockPos(player.blockPosition()));
    }

    public static CompoundTag snapshot(Domain domain) {
        return snapshot(domain, 0);
    }

    public static CompoundTag snapshot(Domain domain, int requestedPage) {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Owner", domain.owner);
        tag.putInt("Selected", domain.selected);
        if (domain.entrance != null) tag.put("Entrance", domain.entrance.save());
        if (domain.exit != null) tag.put("Exit", domain.exit.save());
        ListTag rooms = new ListTag();
        domain.rooms.forEach(room -> rooms.add(room.save()));
        tag.put("Rooms", rooms);
        ListTag residents = new ListTag();
        int pages = Math.max(1, (domain.residents.size() + RESIDENT_PAGE_SIZE - 1) / RESIDENT_PAGE_SIZE);
        int page = Math.max(0, Math.min(pages - 1, requestedPage));
        domain.residents.values().stream().skip((long) page * RESIDENT_PAGE_SIZE).limit(RESIDENT_PAGE_SIZE)
                .forEach(resident -> residents.add(resident.save()));
        tag.put("Residents", residents);
        tag.putInt("ResidentCount", domain.residents.size());
        tag.putInt("ResidentPage", page); tag.putInt("ResidentPages", pages);
        return tag;
    }

    public static boolean takeAction(ServerPlayer player) {
        Domain domain = domain(player);
        long now = player.server.overworld().getGameTime();
        if (now < domain.nextAction) return false;
        domain.nextAction = now + 5;
        return true;
    }

    public static void createRoom(ServerPlayer player, Style style) {
        createRoom(player, style, new Dimensions(SIZE, HEIGHT, SIZE));
    }

    public static boolean createRoom(ServerPlayer player, Style style, Dimensions size) {
        if (!eligible(player)) { tell(player, "Creating chambers requires the equipped, eligible blessing."); return false; }
        Domain domain = domain(player);
        Room room = PocketDomainData.get(player.server).planRoom(domain, style, size);
        tell(player, room == null ? "Finish current construction first. A domain supports up to eight chambers."
                : "Aethon is weaving a new chamber. It will never replace your existing rooms.");
        return room != null;
    }

    public static boolean resizeRoom(ServerPlayer player, Room room, Dimensions size) {
        if (!eligible(player) || !size.valid()) return false;
        Domain domain = domain(player);
        if (!domain.rooms.contains(room) || room.legacy) {
            tell(player, "Original sanctuary structures are preserved as-is. Create a sized chamber instead."); return false;
        }
        ReturnPoint exit = PocketPortals.customRoomExit(domain, room);
        if (exit != null && !PocketPortals.fits(room, size, exit.position())) {
            tell(player, "Close or move this chamber's exit portal before shrinking past it."); return false;
        }
        if (inPocket(player) && room.contains(player.blockPosition())) {
            tell(player, "Leave this chamber before resizing it. Your builds will be checked before any change."); return false;
        }
        if (!PocketDomainData.get(player.server).planResize(domain, room, size)) {
            tell(player, "Choose a different size and finish any current chamber work first."); return false;
        }
        tell(player, "Checking the resize. Furnishings are preserved; an obstructed shrink will be refused.");
        return true;
    }

    public static boolean enter(ServerPlayer player, int index) {
        if (net.nightshade.divinity_engine.util.divinity.innerworld.CommunionManager.busy(player)) return false;
        if (!eligible(player)) { tell(player, "You cannot enter or travel through the domain without the eligible blessing."); return false; }
        if (player.isPassenger() || player.isVehicle()) { tell(player, "Dismount before crossing the domain boundary."); return false; }
        Domain domain = domain(player);
        if (!inPocket(player) && !PocketPortals.atEntrance(player, domain)) {
            tell(player, "Place your entrance portal in this world, then walk through it to enter."); return false;
        }
        return enterGateway(player, domain, index);
    }

    /** Gateway access is shared; domain management remains owner-only. */
    public static boolean enterGateway(ServerPlayer player, Domain domain, int index) {
        if (net.nightshade.divinity_engine.util.divinity.innerworld.CommunionManager.busy(player)) return false;
        if (!player.isAlive() || player.isSpectator() || player.isPassenger() || player.isVehicle()
                || player.level().dimension().equals(DimensionRegistry.GODLY_TRIALS)
                || TrialSessionManager.getSession(player) != null) return false;
        if (domain.rooms.isEmpty()) { createRoom(player, Style.SANCTUM); return false; }
        if (index < 0 || index >= domain.rooms.size()) return false;
        Room room = domain.rooms.get(index);
        if (!room.ready()) { tell(player, "That chamber is still forming. Refresh the domain menu shortly."); return false; }
        ServerLevel level = player.server.getLevel(DimensionRegistry.POCKET_DIMENSION);
        if (level == null) return false;
        if (!PocketChunkLoading.prepare(player, level, room.arrival())) {
            long now = player.server.overworld().getGameTime();
            if (player.getPersistentData().getLong("PocketPortalNoticeAfter") <= now) {
                tell(player, "Your destination is waking. Stay in the gateway; it will open when ready.");
                player.getPersistentData().putLong("PocketPortalNoticeAfter", now + 40);
            }
            return false;
        }
        Vec3 destination = safePosition(level, player, room.arrival(), room);
        if (destination == null) { tell(player, "The arrival area is obstructed. No blocks were removed."); return false; }
        if (!inPocket(player)) {
            if (domain.owner.equals(player.getUUID())) domain.returnPoint = point(player);
            else PocketDomainData.get(player.server).admit(player.getUUID(), domain.owner, point(player));
        }
        if (domain.owner.equals(player.getUUID())) domain.selected = index;
        PocketDomainData.get(player.server).setDirty();
        teleport(player, level, destination, player.getYRot(), player.getXRot());
        tell(player, "Each chamber has a gold-marked exit at the far wall. Front-left goes down; front-right goes up when that floor exists. Sneak-use remains a safe escape.");
        return true;
    }

    public static boolean exit(ServerPlayer player) {
        if (!inPocket(player)) return false;
        PocketDomainData data = PocketDomainData.get(player.server);
        Visit visit = data.visit(player.getUUID());
        Domain domain = visit == null ? data.at(player.blockPosition()) : data.find(visit.host());
        if (domain == null) domain = domain(player);
        ReturnPoint back = domain.entrance == null ? (visit == null ? domain.returnPoint : visit.origin()) : PocketPortals.outside(domain.entrance);
        if (PocketPortals.fallbackExit(player)) back = null;
        ServerLevel destination = back == null ? null : resolve(player.server, back.dimension());
        if (destination != null && !destination.dimension().equals(DimensionRegistry.POCKET_DIMENSION)
                && !destination.dimension().equals(DimensionRegistry.GODLY_TRIALS)
                && !PocketChunkLoading.prepare(player, destination, back.position())) {
            if (PocketPortals.queueExit(player)) tell(player, "Preparing your return safely. You will leave as soon as it is ready.");
            return false;
        }
        Vec3 position = destination == null || destination.dimension().equals(DimensionRegistry.POCKET_DIMENSION)
                || destination.dimension().equals(DimensionRegistry.GODLY_TRIALS) ? null
                : safePosition(destination, player, back.position(), null);
        if (position == null) {
            PocketPortals.useFallbackExit(player);
            destination = player.server.overworld();
            BlockPos spawn = destination.getSharedSpawnPos();
            if (!PocketChunkLoading.prepare(player, destination, Vec3.atBottomCenterOf(spawn))) {
                if (PocketPortals.queueExit(player)) tell(player, "Preparing a safe return. You will leave as soon as it is ready.");
                return false;
            }
            BlockPos surface = destination.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawn);
            position = safePosition(destination, player, Vec3.atBottomCenterOf(surface), null);
        }
        if (position == null) { tell(player, "No safe return position is available. Ask an administrator for help; no terrain was changed."); return false; }
        teleport(player, destination, position, back == null ? 0 : back.yaw(), back == null ? 0 : back.pitch());
        data.leave(player.getUUID());
        tell(player, "You return from your domain.");
        return true;
    }

    private static void teleport(ServerPlayer player, ServerLevel level, Vec3 position, float yaw, float pitch) {
        BlessingsInstance instance = equipped(player);
        player.closeContainer();
        if (instance != null && !player.isInvisible()) BlessingVisuals.at(instance, player, player.position(), BlessingVisuals.Cue.TELEPORT, 1.5);
        player.teleportTo(level, position.x, position.y, position.z, yaw, pitch);
        PocketPortals.traveled(player);
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0;
        if (instance != null && !player.isInvisible()) BlessingVisuals.at(instance, player, position, BlessingVisuals.Cue.TELEPORT, 1.5);
    }

    public static Mob aimedCreature(ServerPlayer player) {
        Vec3 start = player.getEyePosition(), end = start.add(player.getViewVector(1).scale(6));
        return player.serverLevel().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(6),
                        mob -> mob.isAlive() && mob.getBoundingBox().inflate(0.25).clip(start, end).isPresent() && player.hasLineOfSight(mob))
                .stream().min(Comparator.comparingDouble(player::distanceToSqr)).orElse(null);
    }

    public static boolean capturable(Mob mob) {
        return mob.isAlive() && mob.canChangeDimensions() && !mob.isPassenger() && !mob.isVehicle() && !mob.isLeashed()
                && !(mob instanceof AbstractVillager) && !(mob instanceof OwnableEntity owned && owned.getOwnerUUID() != null)
                && mob.getMaxHealth() <= 80 && mob.getBbWidth() <= 2 && mob.getBbHeight() <= 3
                && !mob.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("forge", "bosses")))
                && !mob.getPersistentData().hasUUID(RESIDENT_OWNER)
                && !(mob instanceof Enemy && mob.getHealth() > mob.getMaxHealth() * 0.3f);
    }

    public static boolean capture(ServerPlayer player) {
        if (!eligible(player) || inPocket(player)) { tell(player, "Capture creatures outside the domain with Pocket Dimension equipped."); return false; }
        Domain domain = domain(player);
        Room room = domain.selectedRoom();
        Mob mob = aimedCreature(player);
        if (room == null || !room.ready()) {
            tell(player, "Select a completed chamber first."); return false;
        }
        if (mob == null || !capturable(mob) || TrialSessionManager.getSession(mob) != null) {
            tell(player, "Look at an ordinary creature within six blocks. Hostiles must be at 30% health; pets, bosses, traders, mounts and trial mobs are protected."); return false;
        }
        ServerLevel level = player.server.getLevel(DimensionRegistry.POCKET_DIMENSION);
        if (level == null) return false;
        if (!PocketChunkLoading.prepare(player, level, room.arrival().add(3, 0, 0))) {
            tell(player, "The chamber is waking. Keep aiming at the creature and try capture again shortly."); return false;
        }
        Vec3 landing = safePosition(level, mob, room.arrival().add(3, 0, 0), room);
        if (landing == null) { tell(player, "Your chamber has no safe space for that creature."); return false; }
        ReturnPoint origin = point(mob);
        Entity moved = transfer(mob, level, landing);
        if (!(moved instanceof Mob resident)) { tell(player, "The creature could not cross the boundary."); return false; }
        PocketChunkLoading.release(player.getUUID());
        resident.setPersistenceRequired();
        resident.setTarget(null);
        resident.getNavigation().stop();
        resident.getPersistentData().putUUID(RESIDENT_OWNER, player.getUUID());
        domain.residents.put(resident.getUUID(), new Resident(resident.getUUID(), resident.getName().getString(), domain.selected, origin, 0));
        PocketDomainData.get(player.server).setDirty();
        tell(player, resident.getName().getString() + " is now a resident. Its identity and equipment are preserved.");
        return true;
    }

    public static boolean release(ServerPlayer player, UUID id) {
        if (!canManage(player)) return false;
        Domain domain = domain(player);
        Resident record = domain.residents.get(id);
        if (record == null) return false;
        ServerLevel pocket = player.server.getLevel(DimensionRegistry.POCKET_DIMENSION);
        Entity found = pocket == null ? null : pocket.getEntity(id);
        if (!(found instanceof Mob mob)) { tell(player, "Visit the resident's chamber so it loads, then release it. Its record has been retained."); return false; }
        if (mob.isPassenger() || mob.isVehicle() || mob.isLeashed()) { tell(player, "Dismount and unleash the resident before releasing it."); return false; }
        if (!mob.getPersistentData().hasUUID(RESIDENT_OWNER) || !mob.getPersistentData().getUUID(RESIDENT_OWNER).equals(player.getUUID())) return false;
        ServerLevel level = resolve(player.server, record.origin().dimension());
        if (level == null || level.dimension().equals(DimensionRegistry.GODLY_TRIALS) || level.dimension().equals(DimensionRegistry.POCKET_DIMENSION)) return false;
        if (!PocketChunkLoading.prepare(player, level, record.origin().position())) {
            tell(player, "The release location is waking. Try release again shortly; your resident remains safe."); return false;
        }
        Vec3 landing = safePosition(level, mob, record.origin().position(), null);
        if (landing == null) { tell(player, "The original release location is obstructed. Your resident remains safe."); return false; }
        Entity moved = transfer(mob, level, landing);
        if (moved == null) return false;
        PocketChunkLoading.release(player.getUUID());
        moved.getPersistentData().remove(RESIDENT_OWNER);
        domain.residents.remove(id);
        PocketDomainData.get(player.server).setDirty();
        tell(player, record.name() + " has returned to its original world.");
        return true;
    }

    public static Entity transfer(Entity entity, ServerLevel destination, Vec3 position) {
        if (entity.level() == destination) { entity.teleportTo(position.x, position.y, position.z); return entity; }
        TRANSFERRING.add(entity.getUUID());
        try {
            return entity.changeDimension(destination, new ITeleporter() {
            @Override public PortalInfo getPortalInfo(Entity moving, ServerLevel level, Function<ServerLevel, PortalInfo> fallback) {
                return new PortalInfo(position, Vec3.ZERO, moving.getYRot(), moving.getXRot());
            }
            @Override public Entity placeEntity(Entity moving, ServerLevel old, ServerLevel level, float yaw, Function<Boolean, Entity> reposition) {
                return reposition.apply(false);
            }
            });
        } finally { TRANSFERRING.remove(entity.getUUID()); }
    }

    /** Commits resident ownership only after a successful, uncanceled entity transfer. */
    public static Entity crossGateway(Entity entity, Domain host, int destination) {
        if (entity instanceof ServerPlayer player) return (destination < 0 ? exit(player) : enterGateway(player, host, destination)) ? player : null;
        if (!(entity.level() instanceof ServerLevel source) || !entity.isAlive() || entity.isPassenger() || entity.isVehicle()
                || !entity.canChangeDimensions() || TrialSessionManager.getSession(entity) != null) return null;
        PocketDomainData data = PocketDomainData.get(source.getServer());
        UUID oldOwner = entity.getPersistentData().hasUUID(RESIDENT_OWNER) ? entity.getPersistentData().getUUID(RESIDENT_OWNER) : null;
        Domain oldDomain = oldOwner == null ? null : data.find(oldOwner);
        Resident previous = oldDomain == null ? null : oldDomain.residents.get(entity.getUUID());
        Room room = destination < 0 || destination >= host.rooms.size() ? null : host.rooms.get(destination);
        if (destination >= 0 && (room == null || !room.ready())) return null;
        ReturnPoint origin = previous == null ? point(entity) : previous.origin();
        ReturnPoint back = host.entrance != null ? PocketPortals.outside(host.entrance)
                : entity.getPersistentData().contains("PocketPortalReturn") ? ReturnPoint.load(entity.getPersistentData().getCompound("PocketPortalReturn")) : host.returnPoint;
        ServerLevel target = destination < 0 ? back == null ? null : resolve(source.getServer(), back.dimension())
                : source.getServer().getLevel(DimensionRegistry.POCKET_DIMENSION);
        if (target == null || destination < 0 && (target.dimension().equals(DimensionRegistry.POCKET_DIMENSION)
                || target.dimension().equals(DimensionRegistry.GODLY_TRIALS))) return null;
        Vec3 desired = destination < 0 ? back.position() : room.arrival();
        if (!PocketChunkLoading.prepare(entity, target, desired)) return null;
        Vec3 landing = safePosition(target, entity, desired, room);
        if (landing == null) return null;
        boolean entering = !inPocket(entity);
        Vec3 velocity = entity.getDeltaMovement();
        Entity moved = transfer(entity, target, landing);
        if (moved == null) return null;
        PocketChunkLoading.release(moved.getUUID());
        moved.fallDistance = 0;
        moved.setDeltaMovement(moved instanceof net.minecraft.world.entity.projectile.Projectile ? velocity : Vec3.ZERO);
        if (entering) moved.getPersistentData().put("PocketPortalReturn", origin.save());
        if (oldDomain != null && previous != null) oldDomain.residents.remove(moved.getUUID());
        if (destination >= 0 && moved instanceof Mob mob) {
            mob.setPersistenceRequired(); mob.setTarget(null); mob.getNavigation().stop();
            mob.getPersistentData().putUUID(RESIDENT_OWNER, host.owner);
            host.residents.put(mob.getUUID(), new Resident(mob.getUUID(), mob.getName().getString(), destination, origin,
                    previous == null ? 0 : previous.revivals()));
        } else if (destination < 0) {
            moved.getPersistentData().remove(RESIDENT_OWNER);
            moved.getPersistentData().remove("PocketPortalReturn");
        }
        data.setDirty();
        return moved;
    }

    public static boolean reviveResident(LivingEntity living) {
        if (!(living instanceof Mob mob) || !(mob.level() instanceof ServerLevel level) || !inPocket(mob)
                || !mob.getPersistentData().hasUUID(RESIDENT_OWNER)) return false;
        PocketDomainData data = PocketDomainData.get(level.getServer());
        Domain owner = data.find(mob.getPersistentData().getUUID(RESIDENT_OWNER));
        Resident resident = owner == null ? null : owner.residents.get(mob.getUUID());
        if (resident == null || resident.room() < 0 || resident.room() >= owner.rooms.size()) return false;
        Room room = owner.rooms.get(resident.room());
        if (!room.ready() || !room.allows(Rule.RESIDENT_REVIVAL) || !room.contains(mob.blockPosition())) return false;
        Vec3 landing = safePosition(level, mob, room.arrival().add(3, 0, 0), room);
        if (landing == null) return false;
        mob.setHealth(mob.getMaxHealth());
        mob.clearFire(); mob.fallDistance = 0; mob.invulnerableTime = 40;
        mob.setTarget(null); mob.getNavigation().stop();
        mob.teleportTo(landing.x, landing.y, landing.z);
        mob.setDeltaMovement(Vec3.ZERO);
        owner.residents.put(resident.id(), resident.revived());
        data.setDirty();
        return true;
    }

    public static boolean emergencyReturn(ServerPlayer player) {
        if (!inPocket(player) || !eligibleBlessing(player)) return false;
        Domain domain = domain(player);
        long now = player.server.overworld().getGameTime();
        if (now < domain.recallAfter || domain.rooms.stream().noneMatch(room -> room.ready()
                && room.contains(player.blockPosition()) && room.allows(Rule.OWNER_RECALL))) return false;
        // LivingDeath fires at zero health; exit itself intentionally has no blessing/health gate.
        if (!exit(player) && !PocketPortals.exitPending(player)) return false;
        player.setHealth(Math.max(1, player.getMaxHealth() * 0.5f));
        player.clearFire(); player.invulnerableTime = 60;
        domain.recallAfter = now + 6000;
        PocketDomainData.get(player.server).setDirty();
        tell(player, "The domain recalled your soul. Emergency return recharges in five minutes.");
        return true;
    }

    /** Never removes terrain to make a landing spot. Checks the entity's actual collision box. */
    public static Vec3 safePosition(ServerLevel level, Entity entity, Vec3 desired, Room room) {
        if (!Double.isFinite(desired.x) || !Double.isFinite(desired.y) || !Double.isFinite(desired.z)
                || Math.abs(desired.x) > 29_000_000 || Math.abs(desired.z) > 29_000_000) return null;
        for (int dy : new int[]{0, 1, 2, 3, -1, -2, 4}) for (int radius = 0; radius <= 4; radius++) {
            for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                Vec3 p = new Vec3(Math.floor(desired.x) + dx + 0.5, Math.floor(desired.y) + dy, Math.floor(desired.z) + dz + 0.5);
                BlockPos feet = BlockPos.containing(p);
                if (p.y < level.getMinBuildHeight() + 1 || p.y + entity.getBbHeight() >= level.getMaxBuildHeight()
                        || !level.getWorldBorder().isWithinBounds(feet) || room != null && !room.contains(feet)) continue;
                AABB box = entity.getBoundingBox().move(p.subtract(entity.position()));
                // Include the collision query's neighboring blocks; never wake an empty room from a safety scan.
                if (!PocketChunkLoading.loaded(level, box.inflate(1))) continue;
                BlockPos floor = feet.below();
                if (level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP) && level.noCollision(entity, box)
                        && (room == null || room.contains(BlockPos.containing(box.minX, box.minY, box.minZ))
                            && room.contains(BlockPos.containing(box.maxX, box.maxY, box.maxZ)))
                        && !level.containsAnyLiquid(box) && !level.getBlockState(feet).is(net.minecraft.tags.BlockTags.FIRE)
                        && !level.getBlockState(floor).is(Blocks.MAGMA_BLOCK)
                        && !level.getBlockState(floor).is(net.minecraft.tags.BlockTags.CAMPFIRES)) return p;
            }
        }
        return null;
    }

    public static void buildTick(MinecraftServer server) {
        PocketRoomBuilder.tick(server);
    }

    public static BlockState constructionBlock(Room room, BlockPos pos) {
        return constructionBlock(room, pos, room.size);
    }

    public static BlockState constructionBlock(Room room, BlockPos pos, Dimensions size) {
        int x = pos.getX() - room.origin.getX(), y = pos.getY() - room.origin.getY(), z = pos.getZ() - room.origin.getZ();
        if (y == 0 || x == 0 || z == 0 || x == size.width() - 1 || z == size.depth() - 1 || y == size.height() - 1)
            return BlocksRegistry.POCKET_DIMENSION.get().defaultBlockState();
        if (y == 1) {
            if ((x == 4 || x == size.width() - 5) && (z == 4 || z == size.depth() - 5)) return Blocks.SEA_LANTERN.defaultBlockState();
            return switch (room.style) {
                case GROVE -> Blocks.MOSS_BLOCK.defaultBlockState();
                case WORKSHOP -> Blocks.POLISHED_ANDESITE.defaultBlockState();
                case ARENA -> (x % 4 == 0 || z % 4 == 0 ? Blocks.DEEPSLATE_TILES : Blocks.SMOOTH_STONE).defaultBlockState();
                case SANCTUM -> (x == size.width() / 2 || z == size.depth() / 2 ? Blocks.QUARTZ_BLOCK : Blocks.END_STONE_BRICKS).defaultBlockState();
            };
        }
        return null;
    }

    public static ReturnPoint point(Entity entity) { return new ReturnPoint(entity.level().dimension().location().toString(), entity.position(), entity.getYRot(), entity.getXRot()); }
    private static ServerLevel resolve(MinecraftServer server, String id) {
        ResourceLocation location = ResourceLocation.tryParse(id);
        return location == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, location));
    }
    public static void tell(ServerPlayer player, String text) { player.displayClientMessage(Component.literal(text), false); }
}
