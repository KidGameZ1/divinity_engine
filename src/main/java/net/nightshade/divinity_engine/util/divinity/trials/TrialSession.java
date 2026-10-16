package net.nightshade.divinity_engine.util.divinity.trials;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.nightshade.divinity_engine.divinity.gods.BaseGod;
import net.nightshade.divinity_engine.divinity.trial.GodlyTrial;
import net.nightshade.divinity_engine.network.cap.player.trials.TrialPlayerCapability;
import net.nightshade.divinity_engine.network.messages.ModMessages;
import net.nightshade.divinity_engine.network.messages.trial.SyncTrialStatePacket;
import net.nightshade.divinity_engine.registry.divinity.trial.GodlyTrialsRegistry;
import net.nightshade.divinity_engine.util.divinity.narrative.DivineNarrativeManager;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class TrialSession {
    private static final int COUNTDOWN_TICKS = 100;
    private static final int RETURN_TICKS = 100;

    private final UUID sessionId;
    private final UUID playerId;
    private final ResourceLocation trialId;
    private final BaseGod god;
    private final GodlyTrial trial;
    private final int arenaSlot;
    private final BlockPos arenaOrigin;
    private final BlockPos arenaCenter;
    private final ServerLevel trialLevel;
    private final MinecraftServer server;
    private final ResourceKey<Level> returnDimension;
    private final Vec3 returnPosition;
    private final float returnYRot;
    private final float returnXRot;
    private final Set<UUID> trackedEntityIds = new LinkedHashSet<>();

    private TrialSessionState state = TrialSessionState.INACTIVE;
    private int phase;
    private int phaseTicksElapsed;
    private int countdownTicksRemaining = COUNTDOWN_TICKS;
    private int returnTicksRemaining;
    private String statusText = "Prepare yourself.";
    private boolean closed;
    private boolean arenaReleased;
    private boolean terminal;

    public TrialSession(UUID sessionId, ServerPlayer player, ResourceLocation trialId, BaseGod god, GodlyTrial trial,
                        int arenaSlot, BlockPos arenaOrigin, ServerLevel trialLevel) {
        this.sessionId = sessionId;
        this.playerId = player.getUUID();
        this.trialId = trialId;
        this.god = god;
        this.trial = trial;
        this.arenaSlot = arenaSlot;
        this.arenaOrigin = arenaOrigin;
        this.arenaCenter = GodsTrialStructureSpawner.getArenaCenter(arenaOrigin);
        this.trialLevel = trialLevel;
        this.server = player.server;
        this.returnDimension = player.level().dimension();
        this.returnPosition = player.position();
        this.returnYRot = player.getYRot();
        this.returnXRot = player.getXRot();
    }

    public void start() {
        ServerPlayer player = getPlayer();
        if (player == null) {
            close();
            return;
        }

        trial.attachSession(this);
        state = TrialSessionState.PREPARING;
        statusText = "Preparing trial arena...";
        updateCapability(player);
        if (!TrialArenaJobs.get(server).prepare(this)) {
            fail(TrialFailureReason.GENERIC_FAILURE);
            return;
        }
        syncState();
    }

    public void arenaReady() {
        if (closed || terminal || state != TrialSessionState.PREPARING) return;
        ServerPlayer player = getPlayer();
        if (player == null || !player.isAlive() || player.level().dimension() != returnDimension) {
            failAndClose(TrialFailureReason.PLAYER_LEFT_TRIAL);
            return;
        }
        state = TrialSessionState.COUNTDOWN;
        countdownTicksRemaining = COUNTDOWN_TICKS;
        phase = 1;
        phaseTicksElapsed = 0;
        statusText = "Prepare yourself.";

        TrialSessionManager.expectDimensionChange(playerId, trialLevel.dimension());
        player.teleportTo(trialLevel, arenaCenter.getX() + 0.5D, arenaCenter.getY(), arenaCenter.getZ() + 0.5D,
                player.getYRot(), player.getXRot());
        trial.onSessionStart();
        DivineNarrativeManager.onTrialStarted(player, god.getRegistryName(), sessionId);
        updateCapability(player);
        syncState();
    }

    public void tick(long gameTime) {
        if (closed) {
            return;
        }

        ServerPlayer player = getPlayer();
        if (player == null) {
            close();
            return;
        }

        switch (state) {
            case PREPARING -> {
                if (!player.isAlive() || player.level().dimension() != returnDimension) fail(TrialFailureReason.PLAYER_LEFT_TRIAL);
            }
            case COUNTDOWN -> tickCountdown(gameTime, player);
            case ACTIVE -> tickActive(gameTime, player);
            case RETURNING -> tickReturning(gameTime, player);
            default -> {
            }
        }
    }

    private void tickCountdown(long gameTime, ServerPlayer player) {
        if (!validatePlayerPosition(player)) {
            return;
        }

        countdownTicksRemaining--;

        if (countdownTicksRemaining <= 0) {
            startPhase(1);
            return;
        }

        if (gameTime % 20L == 0L) {
            syncState();
        }
    }

    private void tickActive(long gameTime, ServerPlayer player) {
        if (!validatePlayerPosition(player)) {
            return;
        }

        int tickingPhase = phase;
        trial.tick();
        // A phase entered by the encounter starts counting on the next server tick.
        if (state != TrialSessionState.ACTIVE || phase != tickingPhase) {
            return;
        }

        phaseTicksElapsed++;

        if (trial.getPhaseDuration() >= 1 && phaseTicksElapsed >= trial.getPhaseDuration()) {
            fail(TrialFailureReason.PHASE_TIMED_OUT);
            return;
        }

        if (gameTime % 20L == 0L) {
            retargetTrackedMobs(player);
            syncState();
        }
    }

    private void tickReturning(long gameTime, ServerPlayer player) {
        if (player.level() != trialLevel) {
            close();
            return;
        }

        returnTicksRemaining--;
        updateCapability(player);
        if (returnTicksRemaining <= 0) {
            if (teleportToReturn(player) && player.level() != trialLevel) close();
            else returnTicksRemaining = 20;
            return;
        }

        if (gameTime % 20L == 0L) {
            syncState();
        }
    }

    private boolean validatePlayerPosition(ServerPlayer player) {
        if (player.level() != trialLevel) {
            fail(TrialFailureReason.PLAYER_LEFT_TRIAL);
            return false;
        }

        AABB bounds = new AABB(
                arenaOrigin.getX() - 2.0D,
                arenaOrigin.getY() - 2.0D,
                arenaOrigin.getZ() - 2.0D,
                arenaOrigin.getX() + GodsTrialStructureSpawner.STRUCTURE_WIDTH + 2.0D,
                arenaOrigin.getY() + GodsTrialStructureSpawner.STRUCTURE_HEIGHT + 6.0D,
                arenaOrigin.getZ() + GodsTrialStructureSpawner.STRUCTURE_LENGTH + 2.0D
        );

        if (!bounds.contains(player.position())) {
            fail(TrialFailureReason.PLAYER_LEFT_TRIAL);
            return false;
        }
        return true;
    }

    public void startPhase(int phase) {
        if (closed || terminal) {
            return;
        }

        if (phase > trial.getMaxPhaseCount()) {
            completeTrial();
            return;
        }

        this.phase = phase;
        this.phaseTicksElapsed = 0;
        this.state = TrialSessionState.ACTIVE;
        this.statusText = "";

        trial.beginPhase(phase);
        ServerPlayer player = getPlayer();
        if (player != null) {
            updateCapability(player);
        }
        syncState();
    }

    public void nextPhase() {
        if (closed || state != TrialSessionState.ACTIVE) {
            return;
        }
        startPhase(phase + 1);
    }

    public void completeTrial() {
        if (closed || terminal) {
            return;
        }
        terminal = true;
        state = TrialSessionState.RETURNING;
        try {
            ServerPlayer completedPlayer = getPlayer();
            if (completedPlayer != null) recordOutcome(completedPlayer, true);
            trial.onSessionComplete();
            statusText = "Trial completed";
        } catch (RuntimeException failure) {
            net.nightshade.divinity_engine.DivinityEngineMod.LOGGER.error("Trial completion callback failed for {}", sessionId, failure);
        } finally { beginReturn(); }
    }

    public void fail(TrialFailureReason reason) {
        if (closed || terminal) {
            return;
        }
        terminal = true;
        state = TrialSessionState.RETURNING;
        statusText = reason.getMessage().getString();
        ServerPlayer player = getPlayer();
        try {
            if (player != null) {
                player.displayClientMessage(reason.getMessage(), false);
                recordOutcome(player, false);
            }
        } finally { beginReturn(); }
    }

    public void failAndClose(TrialFailureReason reason) {
        if (closed) {
            return;
        }

        try { fail(reason); } finally { close(); }
    }

    private void beginReturn() {
        state = TrialSessionState.RETURNING;
        returnTicksRemaining = RETURN_TICKS;
        clearTrackedEntities();

        ServerPlayer player = getPlayer();
        if (player != null) {
            if (player.level() != trialLevel || teleportToReturn(player) && player.level() != trialLevel) {
                close();
                return;
            }
            updateCapability(player);
        }

        syncState();
    }

    public void close() {
        if (closed) {
            return;
        }

        ServerPlayer player = getPlayer();
        if (player != null && player.level() == trialLevel && !player.hasDisconnected()
                && (!teleportToReturn(player) || player.level() == trialLevel)) return;
        if (closed) return; // Dimension-change callbacks can close synchronously during teleport.
        closed = true;
        clearTrackedEntities();
        releaseArena();
        TrialSessionManager.unregisterSession(this);

        if (player != null) {
            TrialPlayerCapability.load(player).clear();
            TrialSessionManager.clearClientState(player);
        }
    }

    public void trackEntity(Entity entity) {
        if (entity == null || entity.isRemoved() || closed || terminal) {
            return;
        }
        if (!trackedEntityIds.add(entity.getUUID())) return;
        entity.getPersistentData().putUUID("divinity_engine.trial_session", sessionId);
        if (entity instanceof Mob mob && getPlayer() != null) mob.setTarget(getPlayer());
        TrialSessionManager.registerTrackedEntity(playerId, entity);
    }

    public void onTrackedEntityDeath(Entity entity) {
        if (entity == null) {
            return;
        }
        trackedEntityIds.remove(entity.getUUID());
        TrialSessionManager.unregisterTrackedEntity(entity.getUUID());
    }

    public boolean ownsEntity(Entity entity) {
        return entity != null && trackedEntityIds.contains(entity.getUUID());
    }

    public void clearTrackedEntities() {
        List<UUID> ids = new ArrayList<>(trackedEntityIds);
        trackedEntityIds.clear();
        for (UUID entityId : ids) {
            Entity entity = trialLevel.getEntity(entityId);
            if (entity != null && !entity.isRemoved()) {
                entity.discard();
            }
            TrialSessionManager.unregisterTrackedEntity(entityId);
        }
    }

    private void retargetTrackedMobs(ServerPlayer player) {
        for (UUID entityId : trackedEntityIds) {
            Entity entity = trialLevel.getEntity(entityId);
            if (entity instanceof Mob mob && !mob.isRemoved() && mob.getTarget() != player) {
                mob.setTarget(player);
            }
        }
    }

    private void releaseArena() {
        if (arenaReleased) {
            return;
        }
        arenaReleased = true;
        TrialArenaJobs.get(server).cleanup(arenaSlot, sessionId);
        if (!TrialArenaJobs.get(server).reserved(arenaSlot)) TrialSessionManager.releaseArenaSlot(arenaSlot);
    }

    private boolean teleportToReturn(ServerPlayer player) {
        return TrialSessionManager.returnPlayer(player, returnDimension, returnPosition, returnYRot, returnXRot);
    }

    private void recordOutcome(ServerPlayer player, boolean victory) {
        try { DivineNarrativeManager.onTrialOutcome(player, god.getRegistryName(), sessionId, victory); }
        catch (RuntimeException failure) {
            net.nightshade.divinity_engine.DivinityEngineMod.LOGGER.error("Trial narrative outcome failed for {}", sessionId, failure);
        }
    }

    private void updateCapability(ServerPlayer player) {
        TrialPlayerCapability.TrialPlayerData data = TrialPlayerCapability.load(player);
        data.setActive(state != TrialSessionState.INACTIVE);
        data.setCleanupPending(state == TrialSessionState.RETURNING);
        data.setSessionId(sessionId.toString());
        data.setTrialId(trialId.toString());
        data.setGodId(god.getRegistryName() != null ? god.getRegistryName().toString() : "");
        data.setState(state);
        data.setPhase(phase);
        data.setPhaseName(trial.getPhaseName());
        data.setObjectiveText(trial.getObjectiveText());
        data.setObjectiveCurrent(trial.getObjectiveCurrent());
        data.setObjectiveTarget(trial.getObjectiveTarget());
        data.setPhaseDuration(trial.getPhaseDuration());
        data.setPhaseTicksRemaining(getPhaseTicksRemaining());
        data.setCountdownTicksRemaining(countdownTicksRemaining);
        data.setReturnTicksRemaining(returnTicksRemaining);
        data.setReturnDimension(returnDimension);
        data.setReturnX(returnPosition.x);
        data.setReturnY(returnPosition.y);
        data.setReturnZ(returnPosition.z);
        data.setReturnYRot(returnYRot);
        data.setReturnXRot(returnXRot);
        data.setArenaDimension(trialLevel.dimension());
        data.setArenaX(arenaOrigin.getX());
        data.setArenaY(arenaOrigin.getY());
        data.setArenaZ(arenaOrigin.getZ());
        data.setArenaSlot(arenaSlot);
    }

    public void syncState() {
        if (closed) return;
        ServerPlayer player = getPlayer();
        if (player == null) {
            return;
        }
        updateCapability(player);
        ModMessages.sendToPlayer(new SyncTrialStatePacket(buildSyncTag()), player);
    }

    private CompoundTag buildSyncTag() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("active", state != TrialSessionState.INACTIVE);
        tag.putString("session_state", state.name());
        tag.putString("god_name", net.minecraft.network.chat.Component.translatable(god.getNameTranslationKey()).getString());
        tag.putString("trial_name", GodlyTrialsRegistry.getDisplayName(trialId));
        tag.putInt("phase", phase);
        tag.putString("phase_name", trial.getPhaseName());
        tag.putString("objective_text", trial.getObjectiveText());
        tag.putInt("objective_current", trial.getObjectiveCurrent());
        tag.putInt("objective_target", trial.getObjectiveTarget());
        tag.putInt("phase_duration", trial.getPhaseDuration());
        tag.putInt("phase_ticks_remaining", getPhaseTicksRemaining());
        tag.putInt("countdown_ticks_remaining", countdownTicksRemaining);
        tag.putInt("return_ticks_remaining", returnTicksRemaining);
        tag.putString("status_text", statusText);
        return tag;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public int getArenaSlot() { return arenaSlot; }
    public boolean isTerminal() { return terminal || closed; }

    public UUID getPlayerId() {
        return playerId;
    }

    public ServerPlayer getPlayer() {
        return server.getPlayerList().getPlayer(playerId);
    }

    public GodlyTrial getTrial() {
        return trial;
    }

    public TrialSessionState getState() {
        return state;
    }

    public BlockPos getArenaOrigin() {
        return arenaOrigin;
    }

    public BlockPos getArenaCenter() {
        return arenaCenter;
    }

    public ServerLevel getTrialLevel() {
        return trialLevel;
    }

    public int getPhaseTicksElapsed() {
        return phaseTicksElapsed;
    }

    public int getPhaseTicksRemaining() {
        return Math.max(trial.getPhaseDuration() - phaseTicksElapsed, 0);
    }

    public ResourceLocation getTrialId() {
        return trialId;
    }
}
