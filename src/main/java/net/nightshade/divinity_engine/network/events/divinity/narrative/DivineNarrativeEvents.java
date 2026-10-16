package net.nightshade.divinity_engine.network.events.divinity.narrative;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.nightshade.divinity_engine.api.narrative.*;
import net.nightshade.divinity_engine.divinity.gods.FavorTiers;
import net.nightshade.divinity_engine.network.cap.player.narrative.*;
import net.nightshade.divinity_engine.util.divinity.gods.GodHelper;
import net.nightshade.divinity_engine.util.divinity.narrative.*;
import net.nightshade.divinity_engine.util.divinity.trials.TrialSessionManager;
import java.util.*;
import java.util.function.Predicate;

/** Explicitly registered by NarrativeBootstrap, never an automatic subscriber. */
public final class DivineNarrativeEvents {
    private static final Queue<Runnable> AFTER_EVENTS = new ArrayDeque<>();
    private static final Map<ServerPlayer, Vec3> LAST_POSITION = new WeakHashMap<>();
    private static final Map<ServerPlayer, NearbyObservations> NEARBY = new WeakHashMap<>();
    private static final Set<UUID> COUNTED_DEATHS = new LinkedHashSet<>();
    private static final Set<ServerPlayer> SUSPENDED = Collections.newSetFromMap(new WeakHashMap<>());
    private record Target(DivineStoryDefinition definition, DivineNarrativeData.Memory memory, long generation,
                          NarrativeProgress.Stage stage, DivineStoryDefinition.Branch path, ResourceLocation dimension) {}
    private record NearbyObservations(long sampledAt, ResourceLocation dimension, List<BlockPos> fire, List<BlockPos> snow) {}
    private static List<Target> capture(ServerPlayer player, Predicate<DivineStoryDefinition.Branch> condition) {
        List<Target> result = new ArrayList<>();
        if (!available(player)) return result;
        DivineNarrativeManager.forActive(player, (def, memory) -> {
            var path = DivineNarrativeManager.path(def, memory);
            if (memory.story.activeObjective() && path != null && condition.test(path))
                result.add(new Target(def, memory, memory.generation(), memory.story.stage(), path, player.level().dimension().location()));
        });
        return result;
    }
    private static void award(ServerPlayer player, List<Target> targets, int amount) {
        for (var target : targets) if (validTarget(player, target))
            DivineNarrativeManager.advance(player, target.definition.id(), amount);
    }
    private static boolean available(ServerPlayer player) {
        return player.isAlive() && !player.isRemoved() && !player.hasDisconnected() && !player.isSpectator() && !inTrials(player);
    }
    private static boolean validTarget(ServerPlayer player, Target target) {
        var data = DivineNarrativeCapability.get(player);
        var registered = DivineNarrativeRegistry.byId(target.definition.id());
        return data != null && registered != null && available(player)
            && player.level().dimension().location().equals(target.dimension)
            && data.findStory(registered) == target.memory
            && target.memory.generation() == target.generation
            && target.memory.story.activeObjective() && target.memory.story.stage() == target.stage
            && DivineNarrativeManager.contacted(player, target.definition.godId())
            && target.memory.definition(registered) != null
            && target.path.equals(DivineNarrativeManager.path(registered, target.memory));
    }
    private static void later(Runnable action) { if (AFTER_EVENTS.size() < 8192) AFTER_EVENTS.add(action); }

    @SubscribeEvent public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        // Unlike MinecraftServer.execute, this cannot run inline inside the cancelable event.
        int count = AFTER_EVENTS.size(); for (int i = 0; i < count; i++) { Runnable task = AFTER_EVENTS.poll(); if (task != null) task.run(); }
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) tickPlayer(player);
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST) public static void death(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (!(event.getSource().getEntity() instanceof ServerPlayer player) || !hostile(victim) || victim.isAlliedTo(player) || player.isAlliedTo(victim)
            || TrialSessionManager.getSession(victim) != null || inTrials(victim)) return;
        boolean projectile = event.getSource().is(DamageTypeTags.IS_PROJECTILE);
        boolean melee = !projectile && event.getSource().getDirectEntity() == player
                && event.getSource().is(DamageTypes.PLAYER_ATTACK);
        List<Target> targets = capture(player, branch -> {
            if (!(branch.objective().equals(StoryObjectiveType.KILL) || branch.objective().equals(StoryObjectiveType.TIMED_KILL))) return false;
            return switch (branch.condition()) {
                case "undead_day" -> victim.getMobType() == MobType.UNDEAD && victim.level().isDay();
                case "undead_night" -> victim.getMobType() == MobType.UNDEAD && victim.level().isNight();
                case "poisoned" -> victim.hasEffect(MobEffects.POISON);
                case "forest" -> victim.level().hasChunkAt(victim.blockPosition())
                    && victim.level().getBiome(victim.blockPosition()).is(BiomeTags.IS_FOREST);
                case "melee" -> melee;
                case "projectile" -> projectile;
                case "phantom" -> victim instanceof Phantom;
                case "stray" -> victim instanceof Stray;
                case "drowned" -> victim instanceof Drowned;
                case "witch" -> victim instanceof Witch;
                case "burning" -> victim.isOnFire();
                case "any" -> true;
                default -> false;
            };
        });
        if (targets.isEmpty()) return;
        long occurred = DivineNarrativeManager.now(player);
        later(() -> {
            if (event.isCanceled() || victim.isAlive() || !hostile(victim) || victim.isAlliedTo(player) || player.isAlliedTo(victim) || inTrials(victim)
                || TrialSessionManager.getSession(victim) != null || !COUNTED_DEATHS.add(victim.getUUID())) return;
            if (COUNTED_DEATHS.size() > 4096) COUNTED_DEATHS.remove(COUNTED_DEATHS.iterator().next());
            for (Target target : targets) {
                if (!validTarget(player, target)) continue;
                var path = DivineNarrativeManager.path(target.definition, target.memory);
                if (path.objective().timeLimitTicks() > 0) {
                    if (target.memory.attemptStart < 0 || occurred - target.memory.attemptStart > path.objective().timeLimitTicks()) {
                        target.memory.story.resetAttempt(); target.memory.attemptStart = occurred;
                    }
                }
                award(player, List.of(target), 1);
            }
        });
    }
    private static boolean hostile(Entity entity) {
        return entity instanceof Enemy && !(entity instanceof net.minecraft.world.entity.player.Player)
            && !(entity instanceof OwnableEntity owned && owned.getOwnerUUID() != null);
    }
    private static boolean inTrials(Entity entity) { return net.nightshade.divinity_engine.util.divinity.innerworld.CommunionManager.inRealm(entity)
            || entity.level().dimension().location().equals(new ResourceLocation("divinity_engine", "godly_trials")); }

    @SubscribeEvent(priority = EventPriority.LOWEST) public static void damage(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var targets = capture(player, path -> path.condition().equals("night_safe")
            || (path.objective().equals(StoryObjectiveType.SUBMERGED) && event.getSource().is(DamageTypes.DROWN)));
        later(() -> {
            if (event.isCanceled() || event.getAmount() <= 0) return;
            for (Target target : targets) if (validTarget(player, target)) {
                target.memory.resetAttempt(); target.memory.lastDamage = DivineNarrativeManager.now(player);
            }
        });
    }
    @SubscribeEvent(priority = EventPriority.LOWEST) public static void shield(ShieldBlockEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !hostile(event.getDamageSource().getEntity())
            || event.getDamageSource().getEntity().isAlliedTo(player) || player.isAlliedTo(event.getDamageSource().getEntity())
            || inTrials(player) || inTrials(event.getDamageSource().getEntity())
            || TrialSessionManager.getSession(event.getDamageSource().getEntity()) != null) return;
        var targets = capture(player, path -> path.objective().equals(StoryObjectiveType.SHIELD));
        later(() -> { if (!event.isCanceled() && event.getBlockedDamage() > 0) award(player, targets, 1); });
    }
    @SubscribeEvent(priority = EventPriority.LOWEST) public static void experience(PlayerXpEvent.XpChange event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var targets = capture(player, path -> path.objective().equals(StoryObjectiveType.XP));
        later(() -> { if (!event.isCanceled() && event.getAmount() > 0) award(player, targets, event.getAmount()); });
    }
    @SubscribeEvent(priority = EventPriority.LOWEST) public static void breed(BabyEntitySpawnEvent event) {
        if (!(event.getCausedByPlayer() instanceof ServerPlayer player) || inTrials(player)) return;
        var targets = capture(player, path -> path.objective().equals(StoryObjectiveType.BREED));
        later(() -> { if (!event.isCanceled() && event.getChild() != null && event.getChild().isAlive()
            && player.serverLevel().getEntity(event.getChild().getUUID()) == event.getChild()) award(player, targets, 1); });
    }
    @SubscribeEvent public static void smelt(PlayerEvent.ItemSmeltedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !event.getSmelting().is(Items.IRON_INGOT)) return;
        var targets = capture(player, path -> path.objective().equals(StoryObjectiveType.SMELT));
        int count = event.getSmelting().getCount(); later(() -> award(player, targets, count));
    }
    @SubscribeEvent(priority = EventPriority.LOWEST) public static void broken(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || inTrials(player) || player.isCreative()) return;
        boolean stone = event.getPos().getY() < 0 && event.getState().is(Blocks.STONE);
        boolean crop = event.getState().getBlock() instanceof CropBlock block && block.isMaxAge(event.getState());
        if (!stone && !crop) return;
        var targets = capture(player, path -> (stone && path.objective().equals(StoryObjectiveType.MINE)) || (crop && path.objective().equals(StoryObjectiveType.HARVEST)));
        BlockPos pos = event.getPos().immutable();
        later(() -> { if (!event.isCanceled() && event.getLevel().hasChunkAt(pos) && !event.getLevel().getBlockState(pos).equals(event.getState())) award(player, targets, 1); });
    }
    @SubscribeEvent(priority = EventPriority.LOWEST) public static void placed(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || inTrials(player)) return;
        if (event.getPlacedBlock().getBlock() instanceof net.nightshade.divinity_engine.block.StatueBlock statue) {
            BlockPos shrine = event.getPos().immutable();
            later(() -> {
                if (!event.isCanceled() && player.isAlive() && !player.isRemoved() && event.getLevel() == player.level()
                        && event.getLevel().hasChunkAt(shrine) && event.getLevel().getBlockState(shrine).equals(event.getPlacedBlock()))
                    net.nightshade.divinity_engine.util.divinity.gods.FavorRewards.foundation(player, statue.getGod());
            });
            return;
        }
        if (!event.getPlacedBlock().is(BlockTags.SAPLINGS)) return;
        var targets = capture(player, path -> path.objective().equals(StoryObjectiveType.PLANT));
        BlockPos pos = event.getPos().immutable(); String key = player.level().dimension().location() + ":" + pos.asLong();
        later(() -> {
            if (event.isCanceled() || !event.getLevel().hasChunkAt(pos) || !event.getLevel().getBlockState(pos).is(BlockTags.SAPLINGS)) return;
            for (Target target : targets) if (validTarget(player, target) && target.memory.placements.add(key)) award(player, List.of(target), 1);
        });
    }
    private static void tickPlayer(ServerPlayer player) {
        var data = DivineNarrativeCapability.get(player); if (data == null) return;
        if (!available(player)) { if (SUSPENDED.add(player)) interrupt(player); return; }
        SUSPENDED.remove(player);
        long now = DivineNarrativeManager.now(player); Vec3 previous = LAST_POSITION.put(player, player.position());
        double distance = previous == null ? 0 : previous.distanceTo(player.position());
        boolean moved = distance > 0.003;
        // Capture every chapter before processing any of them; a transition cannot reuse this tick.
        List<Target> active = capture(player, path -> true);
        for (Target target : active) {
            if (!validTarget(player, target)) continue;
            var def = target.definition; var m = target.memory; var path = target.path;
            m.resettableAttempt = path.objective().continuous() || path.objective().timeLimitTicks() > 0;
            if (path.objective().equals(StoryObjectiveType.SPRINT) && player.isSprinting() && player.onGround()
                && !player.isPassenger() && !player.getAbilities().flying && distance < 2) {
                m.distanceRemainder += distance;
                int whole = (int) m.distanceRemainder;
                if (whole > 0) { m.distanceRemainder -= whole; award(player, List.of(target), whole); }
                if (!validTarget(player, target)) continue;
            }
            if (path.objective().equals(StoryObjectiveType.HEIGHT) && player.getY() >= 160) m.reachedHeight = true;
            if (path.objective().timeLimitTicks() > 0 && m.attemptStart >= 0 && now - m.attemptStart > path.objective().timeLimitTicks()) {
                m.resetAttempt();
            }
            if (!path.objective().continuous()) continue;
            boolean valid;
            if (path.objective().equals(StoryObjectiveType.HIDDEN)) valid = player.isInvisible() && player.isShiftKeyDown() && m.nearSeen >= now - 10 && m.nearSeen >= 0
                && DivineNarrativeManager.atRememberedShrine(player, def.godId(), m);
            else if (path.objective().equals(StoryObjectiveType.SUBMERGED)) valid = player.isUnderWater();
            else if (path.objective().equals(StoryObjectiveType.PRAY)) {
                valid = player.isShiftKeyDown() && m.prayerSeen >= now - 10 && m.prayerSeen >= 0
                    && DivineNarrativeManager.atRememberedShrine(player, def.godId(), m);
                if (path.condition().equals("still")) valid &= !moved;
                if (path.condition().equals("night_safe")) valid &= player.level().isNight();
                if (path.condition().equals("sky_night")) valid &= player.level().isNight() && player.getY() > 100 && player.level().canSeeSky(player.blockPosition());
            } else if (path.objective().equals(StoryObjectiveType.OBSERVE)) valid = observing(player, path.condition(), now);
            else continue;
            if (!valid) {
                if (m.story.progress() > 0 || m.attemptStart >= 0) m.resetAttempt();
            } else {
                if (m.attemptStart < 0) m.attemptStart = now;
                award(player, List.of(target), 1);
            }
        }
        if (player.tickCount % 20 != 0) return;
        for (var instance : GodHelper.getContactedGodsFrom(player).getContactedGods()) {
            var id = instance.getBaseGodId(); var m = data.get(id); m.contacted = true;
            if (net.nightshade.divinity_engine.util.divinity.gods.DivineRelationship.manages(instance.getBaseGod())) {
                String rank = net.nightshade.divinity_engine.util.divinity.gods.DivineRelationship.rank(player, instance.getBaseGod());
                String previousRank = m.relationship.getString("last_rank");
                if (!previousRank.equals(rank)) {
                    m.relationship.putString("last_rank", rank);
                    if (!previousRank.isEmpty()) DivineNarrativeManager.say(player, id, rank, false);
                }
                continue;
            }
            if (instance.getFavor() > m.highestFavor) {
                FavorTiers before = FavorTiers.getByFavor(m.highestFavor), after = FavorTiers.getByFavor(instance.getFavor());
                m.highestFavor = instance.getFavor();
                if (before != after) DivineNarrativeManager.say(player, id, after.name(), false);
            }
        }
        // The manager emits compact deltas; idle/completed stories need no periodic packet.
        if (!active.isEmpty() || player.containerMenu instanceof net.nightshade.divinity_engine.world.inventory.BlessingsMenu)
            net.nightshade.divinity_engine.util.divinity.narrative.NarrativeJournal.syncCounters(player);
    }
    private static boolean observing(ServerPlayer player, String condition, long now) {
        var level = player.serverLevel(); BlockPos pos = player.blockPosition();
        if (!level.hasChunkAt(pos)) return false;
        return switch (condition) {
            case "day_sky" -> level.isDay() && level.canSeeSky(pos);
            case "night_sky" -> level.isNight() && level.canSeeSky(pos);
            case "forest" -> level.getBiome(pos).is(BiomeTags.IS_FOREST);
            case "rain" -> level.isRainingAt(pos);
            case "thunder" -> level.isThundering() && level.isRainingAt(pos);
            case "underground" -> player.getY() < 0;
            case "high_sky" -> player.getY() > 100 && level.canSeeSky(pos);
            case "water" -> player.isUnderWater();
            case "snow" -> level.getBiome(pos).value().getPrecipitationAt(pos) == Biome.Precipitation.SNOW
                || nearby(player, now, false);
            case "firelight" -> nearby(player, now, true);
            default -> false;
        };
    }
    private static boolean nearby(ServerPlayer player, long now, boolean fire) {
        var level = player.serverLevel(); BlockPos origin = player.blockPosition();
        var cached = NEARBY.get(player);
        if (cached == null || now < cached.sampledAt || now - cached.sampledAt >= 20
            || !cached.dimension.equals(level.dimension().location())) {
            List<BlockPos> flames = new ArrayList<>(), snow = new ArrayList<>();
            for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-2, -2, -2), origin.offset(2, 2, 2))) {
                if (!level.hasChunkAt(pos)) continue;
                BlockState state = level.getBlockState(pos);
                if (firelight(state)) flames.add(pos.immutable());
                if (snow(state)) snow.add(pos.immutable());
            }
            cached = new NearbyObservations(now, level.dimension().location(), List.copyOf(flames), List.copyOf(snow));
            NEARBY.put(player, cached);
        }
        // Recheck cached sources without another scan: extinguishing or leaving breaks continuity immediately.
        for (BlockPos pos : fire ? cached.fire : cached.snow) {
            if (Math.abs(pos.getX() - origin.getX()) > 2 || Math.abs(pos.getY() - origin.getY()) > 2
                || Math.abs(pos.getZ() - origin.getZ()) > 2 || !level.hasChunkAt(pos)) continue;
            BlockState state = level.getBlockState(pos);
            if (fire ? firelight(state) : snow(state)) return true;
        }
        return false;
    }
    private static boolean firelight(BlockState state) {
        return state.is(BlockTags.FIRE) || CampfireBlock.isLitCampfire(state) || state.getFluidState().is(FluidTags.LAVA);
    }
    private static boolean snow(BlockState state) {
        return state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.POWDER_SNOW);
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { if (event.getEntity() instanceof ServerPlayer player) interrupt(player); }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) { if (event.getEntity() instanceof ServerPlayer player) { net.nightshade.divinity_engine.util.divinity.gods.DivineRelationship.migrate(player); interrupt(player); DivineNarrativeManager.sync(player, true); } }
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent event) { if (event.getEntity() instanceof ServerPlayer player) { interrupt(player); DivineNarrativeManager.sync(player, true); } }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) { if (event.getEntity() instanceof ServerPlayer player) { interrupt(player); DivineNarrativeManager.sync(player, true); } }
    private static void interrupt(ServerPlayer player) {
        LAST_POSITION.remove(player); NEARBY.remove(player); var data = DivineNarrativeCapability.get(player);
        if (data != null) data.allMemories().forEach(DivineNarrativeData.Memory::interrupt);
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { AFTER_EVENTS.clear(); LAST_POSITION.clear(); NEARBY.clear(); COUNTED_DEATHS.clear(); SUSPENDED.clear(); }
}
