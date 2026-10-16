package net.nightshade.divinity_engine.util.divinity.trials;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.nightshade.divinity_engine.divinity.gods.BaseGod;
import net.nightshade.divinity_engine.divinity.trial.GodlyTrial;
import net.nightshade.divinity_engine.network.cap.player.trials.TrialPlayerCapability;
import net.nightshade.divinity_engine.network.messages.ModMessages;
import net.nightshade.divinity_engine.network.messages.trial.SyncTrialStatePacket;
import net.nightshade.divinity_engine.registry.dimensions.DimensionRegistry;
import net.nightshade.divinity_engine.registry.divinity.trial.GodlyTrialsRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TrialSessionManager {
    private static final Map<UUID, TrialSession> SESSIONS_BY_PLAYER = new ConcurrentHashMap<>();
    private static final Map<UUID, UUID> TRACKED_ENTITY_OWNERS = new ConcurrentHashMap<>();
    private static final Set<Integer> CLAIMED_ARENA_SLOTS = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, ResourceKey<Level>> EXPECTED_DIMENSION_CHANGES = new ConcurrentHashMap<>();
    private static long serverTickCounter;

    private TrialSessionManager() {
    }

    public static TrialStartResult startTrial(ServerPlayer player, BaseGod god) {
        if (net.nightshade.divinity_engine.util.divinity.innerworld.CommunionManager.busy(player)) return TrialStartResult.START_CONDITION_FAILED;
        if (!player.isAlive() || player.isSpectator() || player.level().dimension().equals(DimensionRegistry.GODLY_TRIALS))
            return TrialStartResult.START_CONDITION_FAILED;
        if (getSession(player) != null) {
            return TrialStartResult.ALREADY_ACTIVE;
        }

        var definition = GodlyTrialsRegistry.get(god);
        var trialId = GodlyTrialsRegistry.getId(god);
        if (definition == null || trialId == null) {
            return TrialStartResult.NO_TRIAL_REGISTERED;
        }

        if (!net.nightshade.divinity_engine.util.divinity.gods.DivineRelationship.canStartTrial(player, god)) {
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "Complete this god's reckoning chapter before starting the trial. Previously earned trial access is preserved."), false);
            return TrialStartResult.START_CONDITION_FAILED;
        }

        GodlyTrial trial = definition.getFactory().apply(player);
        if (!trial.startCondition()) {
            return TrialStartResult.START_CONDITION_FAILED;
        }

        ServerLevel trialLevel = player.server.getLevel(DimensionRegistry.GODLY_TRIALS);
        if (trialLevel == null) {
            return TrialStartResult.INTERNAL_ERROR;
        }

        int arenaSlot = claimArenaSlot(player.server);
        if (arenaSlot < 0) {
            return TrialStartResult.ARENA_UNAVAILABLE;
        }

        BlockPos arenaOrigin = GodsTrialStructureSpawner.getArenaOrigin(arenaSlot);

        TrialSession session = new TrialSession(UUID.randomUUID(), player, trialId, god, trial, arenaSlot, arenaOrigin, trialLevel);
        SESSIONS_BY_PLAYER.put(player.getUUID(), session);
        try {
            session.start();
            return getSession(player) == session ? TrialStartResult.STARTED : TrialStartResult.INTERNAL_ERROR;
        } catch (RuntimeException failure) {
            net.nightshade.divinity_engine.DivinityEngineMod.LOGGER.error("Trial preparation failed", failure);
            session.failAndClose(TrialFailureReason.GENERIC_FAILURE);
            return TrialStartResult.INTERNAL_ERROR;
        }
    }

    public static @Nullable TrialSession getSession(ServerPlayer player) {
        return player == null ? null : SESSIONS_BY_PLAYER.get(player.getUUID());
    }

    public static @Nullable TrialSession getSessionById(UUID id) {
        return SESSIONS_BY_PLAYER.values().stream().filter(session -> session.getSessionId().equals(id)).findFirst().orElse(null);
    }

    public static void stop() {
        SESSIONS_BY_PLAYER.clear(); TRACKED_ENTITY_OWNERS.clear(); CLAIMED_ARENA_SLOTS.clear();
        EXPECTED_DIMENSION_CHANGES.clear(); serverTickCounter = 0;
    }
    public static void shutdown() {
        for (TrialSession session : List.copyOf(SESSIONS_BY_PLAYER.values())) {
            try { session.failAndClose(TrialFailureReason.PLAYER_LOGGED_OUT); }
            catch (RuntimeException failure) { net.nightshade.divinity_engine.DivinityEngineMod.LOGGER.error("Trial shutdown cleanup failed", failure); }
        }
    }

    public static @Nullable TrialSession getSession(Entity entity) {
        if (entity instanceof ServerPlayer player) {
            return getSession(player);
        }

        UUID ownerId = TRACKED_ENTITY_OWNERS.get(entity.getUUID());
        return ownerId == null ? null : SESSIONS_BY_PLAYER.get(ownerId);
    }

    public static void failSession(ServerPlayer player, TrialFailureReason reason) {
        TrialSession session = getSession(player);
        if (session != null) {
            session.fail(reason);
        }
    }

    public static void tickSessions() {
        serverTickCounter++;
        List<TrialSession> sessions = new ArrayList<>(SESSIONS_BY_PLAYER.values());
        for (TrialSession session : sessions) {
            if (session != null) {
                long start = System.nanoTime();
                try { session.tick(serverTickCounter); }
                catch (RuntimeException failure) {
                    net.nightshade.divinity_engine.DivinityEngineMod.LOGGER.error("Trial {} tick failed", session.getSessionId(), failure);
                    try { session.failAndClose(TrialFailureReason.GENERIC_FAILURE); }
                    catch (RuntimeException cleanupFailure) { net.nightshade.divinity_engine.DivinityEngineMod.LOGGER.error("Trial failure callback failed", cleanupFailure); }
                }
                long elapsed = System.nanoTime() - start;
                if (elapsed > 50_000_000L) net.nightshade.divinity_engine.DivinityEngineMod.LOGGER.warn("Trial {} encounter tick took {} ms", session.getSessionId(), elapsed / 1_000_000);
            }
        }
    }

    public static void handlePlayerDamage(ServerPlayer player, LivingDamageEvent event) {
        TrialSession session = getSession(player);
        if (session != null) {
            session.getTrial().onTakenDamage(event);
        }
    }

    public static void handleHeal(ServerPlayer player, LivingHealEvent event) {
        TrialSession session = getSession(player);
        if (session != null) {
            session.getTrial().onHeal(event);
        }
    }

    public static void handleRespawn(ServerPlayer player, PlayerEvent.PlayerRespawnEvent event) {
        TrialSession session = getSession(player);
        if (session != null) {
            session.getTrial().onRespawn(event);
            session.syncState();
        } else {
            clearClientState(player);
        }
    }

    public static void handleDeathEvent(LivingDeathEvent event) {
        if (event.isCanceled()) return;
        TrialSession session = getSession(event.getEntity());
        if (session == null) {
            return;
        }

        if (event.getEntity() == session.getPlayer()) {
            event.setCanceled(true);
            event.getEntity().hurtMarked = false;
            event.getEntity().setHealth(event.getEntity().getMaxHealth());
            try { session.getTrial().onPlayerDeath(event); }
            finally { session.fail(TrialFailureReason.PLAYER_DIED); }
            return;
        }

        if (session.isTerminal()) return;
        if (event.getSource().getEntity() instanceof Player sourcePlayer
                && Objects.equals(sourcePlayer.getUUID(), session.getPlayerId())) {
            session.onTrackedEntityDeath(event.getEntity());
            session.getTrial().onKillEntity(sourcePlayer, event);
            session.getTrial().onDeath(event);
            event.setCanceled(true);
            event.getEntity().discard();
        }else{
            event.setCanceled(true);
            event.getEntity().setHealth(event.getEntity().getMaxHealth()/2);
        }

    }

    public static void handlePlayerLogin(ServerPlayer player) {
        if (getSession(player) == null) recoverStaleCapability(player);

        TrialSession session = getSession(player);
        if (session != null) {
            session.syncState();
        } else {
            clearClientState(player);
        }
    }

    public static void handlePlayerLogout(ServerPlayer player) {
        TrialSession session = getSession(player);
        if (session != null) {
            session.failAndClose(TrialFailureReason.PLAYER_LOGGED_OUT);
            return;
        }

        recoverStaleCapability(player);
    }

    public static void handlePlayerChangedDimension(ServerPlayer player, ResourceKey<Level> toDimension) {
        ResourceKey<Level> expected = EXPECTED_DIMENSION_CHANGES.get(player.getUUID());
        if (expected != null && expected.equals(toDimension)) {
            EXPECTED_DIMENSION_CHANGES.remove(player.getUUID());
            TrialSession session = getSession(player);
            if (session != null) {
                if (session.getState() == TrialSessionState.RETURNING) {
                    session.close();
                } else {
                    session.syncState();
                }
            } else {
                clearClientState(player);
            }
            return;
        }

        TrialSession session = getSession(player);
        if (session != null) {
            session.fail(TrialFailureReason.PLAYER_LEFT_TRIAL);
        } else {
            clearClientState(player);
        }
    }

    public static void registerTrackedEntity(UUID playerId, Entity entity) {
        TRACKED_ENTITY_OWNERS.put(entity.getUUID(), playerId);
    }

    public static void unregisterTrackedEntity(UUID entityId) {
        TRACKED_ENTITY_OWNERS.remove(entityId);
    }

    public static void unregisterSession(TrialSession session) {
        SESSIONS_BY_PLAYER.remove(session.getPlayerId(), session);
        EXPECTED_DIMENSION_CHANGES.remove(session.getPlayerId());
    }

    public static void releaseArenaSlot(int arenaSlot) {
        CLAIMED_ARENA_SLOTS.remove(arenaSlot);
    }

    public static void expectDimensionChange(UUID playerId, ResourceKey<Level> dimension) {
        EXPECTED_DIMENSION_CHANGES.put(playerId, dimension);
    }

    public static void clearClientState(ServerPlayer player) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("active", false);
        ModMessages.sendToPlayer(new SyncTrialStatePacket(tag), player);
    }

    public static boolean returnPlayer(ServerPlayer player, ResourceKey<Level> dimension, net.minecraft.world.phys.Vec3 position, float yaw, float pitch) {
        ServerLevel level = player.server.getLevel(dimension);
        if (level == null || dimension.equals(DimensionRegistry.GODLY_TRIALS)) level = player.server.overworld();
        net.minecraft.world.phys.Vec3 safe = net.nightshade.divinity_engine.util.divinity.blessings.aethon.PocketDomainManager.safePosition(level, player, position, null);
        if (safe == null) {
            level = player.server.overworld();
            var surface = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, level.getSharedSpawnPos());
            safe = net.nightshade.divinity_engine.util.divinity.blessings.aethon.PocketDomainManager.safePosition(level, player, net.minecraft.world.phys.Vec3.atBottomCenterOf(surface), null);
        }
        if (safe == null) return false;
        expectDimensionChange(player.getUUID(), level.dimension());
        player.teleportTo(level, safe.x, safe.y, safe.z, yaw, pitch);
        player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO); player.fallDistance = 0;
        return player.level() == level;
    }

    private static int claimArenaSlot(net.minecraft.server.MinecraftServer server) {
        for (int i = 0; i < GodsTrialStructureSpawner.getArenaSlotCount(); i++) {
            if (!TrialArenaJobs.get(server).reserved(i) && CLAIMED_ARENA_SLOTS.add(i)) {
                return i;
            }
        }
        return -1;
    }

    private static void recoverStaleCapability(ServerPlayer player) {
        TrialPlayerCapability.TrialPlayerData data = TrialPlayerCapability.load(player);
        if (!data.isActive() && !data.isCleanupPending()) {
            return;
        }

        if (data.hasArenaOrigin() && !data.getArenaDimension().isEmpty()) {
            ResourceLocation arenaId = ResourceLocation.tryParse(data.getArenaDimension());
            ServerLevel arenaLevel = arenaId == null ? null : player.server.getLevel(ResourceKey.create(Registries.DIMENSION, arenaId));
            if (arenaLevel != null && arenaLevel.dimension().equals(DimensionRegistry.GODLY_TRIALS)
                    && data.getState() != TrialSessionState.PREPARING) {
                try {
                    TrialArenaJobs.get(player.server).recover(data.getArenaSlot(), UUID.fromString(data.getSessionId()),
                            new BlockPos(data.getArenaX(), data.getArenaY(), data.getArenaZ()));
                } catch (IllegalArgumentException ignored) { /* Invalid legacy identity must never authorize deletion. */ }
            }
        }

        if (player.level().dimension().equals(DimensionRegistry.GODLY_TRIALS)) {
            ResourceLocation returnId = ResourceLocation.tryParse(data.getReturnDimension());
            if (!returnPlayer(player, returnId == null ? Level.OVERWORLD : ResourceKey.create(Registries.DIMENSION, returnId),
                    new net.minecraft.world.phys.Vec3(data.getReturnX(), data.getReturnY(), data.getReturnZ()), data.getReturnYRot(), data.getReturnXRot())) return;
        }

        data.clear();
    }
}
