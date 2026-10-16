package net.nightshade.divinity_engine.divinity.trial;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.nightshade.divinity_engine.util.divinity.trials.TrialFailureReason;
import net.nightshade.divinity_engine.util.divinity.trials.TrialSession;
import org.jetbrains.annotations.Nullable;

public abstract class GodlyTrial {

    protected final ServerPlayer player;
    protected int phase = 0;
    protected ServerLevel trialLevel;
    protected int phaseDuration = 200;
    private @Nullable TrialSession session;
    private String phaseName = "";
    private String objectiveText = "";
    private int objectiveCurrent;
    private int objectiveTarget;

    public GodlyTrial(ServerPlayer player) {
        this.player = player;
    }

    public final void attachSession(TrialSession session) {
        this.session = session;
        this.trialLevel = session.getTrialLevel();
        this.phase = 0;
        this.phaseDuration = 200;
        this.phaseName = "";
        this.objectiveText = "";
        this.objectiveCurrent = 0;
        this.objectiveTarget = 0;
    }

    public final void beginPhase(int phase) {
        this.phase = phase;
        this.phaseDuration = 200;
        this.phaseName = "Phase " + phase;
        this.objectiveText = "";
        this.objectiveCurrent = 0;
        this.objectiveTarget = 0;
        runPhase(phase);
    }

    public void tick() {
    }

    protected void nextPhase() {
        if (session != null) {
            session.nextPhase();
        }
    }

    protected boolean checkFailCondition() {
        return player.isDeadOrDying();
    }

    protected void completeTrial() {
        if (session != null) {
            session.completeTrial();
        }
    }

    public void failTrial() {
        if (session != null) {
            session.fail(TrialFailureReason.GENERIC_FAILURE);
        }
    }

    public void failTrial(TrialFailureReason reason) {
        if (session != null) {
            session.fail(reason);
        }
    }

    protected abstract void onStart();

    protected abstract void runPhase(int phase);

    protected abstract void onComplete();

    protected abstract int getMaxPhases();

    public final void onSessionStart() {
        onStart();
    }

    public final void onSessionComplete() {
        onComplete();
    }

    public final int getMaxPhaseCount() {
        return getMaxPhases();
    }

    public final boolean shouldFailSession() {
        return checkFailCondition();
    }

    public ServerPlayer getPlayer() {
        return player;
    }

    public BlockPos getArenaOrigin() {
        return session == null ? BlockPos.ZERO : session.getArenaOrigin();
    }

    public int getTicksElapsed() {
        return session == null ? 0 : session.getPhaseTicksElapsed();
    }

    public void onKillEntity(Player player, LivingDeathEvent event) {
    }

    public void onTakenDamage(LivingDamageEvent event) {
    }

    public void onPlayerDeath(LivingDeathEvent event) {
    }
    public void onDeath(LivingDeathEvent event) {
    }

    public void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
    }

    public void onHeal(LivingHealEvent event) {
    }

    public BlockPos getArenaCenter() {
        return session == null ? BlockPos.ZERO : session.getArenaCenter();
    }

    public boolean startCondition() {
        return true;
    }

    protected void setPhaseName(String phaseName) {
        this.phaseName = phaseName;
    }

    protected void setObjective(String objectiveText, int current, int target) {
        this.objectiveText = objectiveText;
        this.objectiveCurrent = current;
        this.objectiveTarget = target;
        syncSessionState();
    }

    protected void setObjectiveProgress(int current) {
        this.objectiveCurrent = current;
        syncSessionState();
    }

    protected void setPhaseInfo(String phaseName, String objectiveText, int current, int target) {
        this.phaseName = phaseName;
        this.objectiveText = objectiveText;
        this.objectiveCurrent = current;
        this.objectiveTarget = target;
        syncSessionState();
    }

    protected void clearObjective() {
        this.objectiveText = "";
        this.objectiveCurrent = 0;
        this.objectiveTarget = 0;
        syncSessionState();
    }

    protected void trackEntity(@Nullable Entity entity) {
        if (entity != null && session != null) {
            session.trackEntity(entity);
        }
    }

    protected boolean isTrackedEntity(@Nullable Entity entity) {
        return entity != null && session != null && session.ownsEntity(entity);
    }

    protected void syncTrackedEntities(@Nullable Iterable<? extends Entity> entities) {
        if (entities == null) {
            return;
        }
        for (Entity entity : entities) {
            trackEntity(entity);
        }
    }

    public int getPhaseDuration() {
        return phaseDuration;
    }

    public String getPhaseName() {
        return phaseName;
    }

    public String getObjectiveText() {
        return objectiveText;
    }

    public int getObjectiveCurrent() {
        return objectiveCurrent;
    }

    public int getObjectiveTarget() {
        return objectiveTarget;
    }

    private void syncSessionState() {
        if (session != null && session.getState() != net.nightshade.divinity_engine.util.divinity.trials.TrialSessionState.INACTIVE) {
            session.syncState();
        }
    }
}
