package net.nightshade.divinity_engine.network.cap.player.trials;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.nightshade.divinity_engine.DivinityEngineMod;
import net.nightshade.divinity_engine.network.cap.GenericCapabilityProvider;
import net.nightshade.divinity_engine.util.divinity.trials.TrialSessionState;
import org.jetbrains.annotations.Nullable;

public class TrialPlayerCapability {
    public static final Capability<TrialPlayerData> CAP = CapabilityManager.get(new CapabilityToken<>() {
    });
    private static final ResourceLocation ID = new ResourceLocation(DivinityEngineMod.MODID, "trial_player_data");

    private static void register(RegisterCapabilitiesEvent event) {
        event.register(TrialPlayerData.class);
    }

    private static void attach(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player && !(event.getObject() instanceof FakePlayer)) {
            event.addCapability(ID, new GenericCapabilityProvider(CAP, TrialPlayerData::new));
        }
    }

    private static void clonePlayer(PlayerEvent.Clone event) {
        event.getOriginal().reviveCaps();
        TrialPlayerData originalData = load(event.getOriginal());
        TrialPlayerData newData = load(event.getEntity());
        newData.deserializeNBT((CompoundTag) originalData.serializeNBT());
        event.getOriginal().invalidateCaps();
    }

    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(TrialPlayerCapability::register);
        IEventBus forgeEventBus = MinecraftForge.EVENT_BUS;
        forgeEventBus.addGenericListener(Entity.class, TrialPlayerCapability::attach);
        forgeEventBus.addListener(TrialPlayerCapability::clonePlayer);
    }

    public static TrialPlayerData load(Entity entity) {
        TrialPlayerData cap = entity.getCapability(CAP).orElseGet(TrialPlayerData::new);
        cap.setOwner(entity);
        return cap;
    }

    public static class TrialPlayerData implements INBTSerializable<CompoundTag> {
        private @Nullable Entity owner;
        private boolean active;
        private boolean cleanupPending;
        private String sessionId = "";
        private String trialId = "";
        private String godId = "";
        private String state = TrialSessionState.INACTIVE.name();
        private int phase;
        private String phaseName = "";
        private String objectiveText = "";
        private int objectiveCurrent;
        private int objectiveTarget;
        private int phaseDuration;
        private int phaseTicksRemaining;
        private int countdownTicksRemaining;
        private int returnTicksRemaining;
        private String returnDimension = "";
        private double returnX;
        private double returnY;
        private double returnZ;
        private float returnYRot;
        private float returnXRot;
        private String arenaDimension = "";
        private int arenaX;
        private int arenaY;
        private int arenaZ;
        private int arenaSlot = -1;

        public void setOwner(@Nullable Entity owner) {
            this.owner = owner;
        }

        public @Nullable Entity getOwner() {
            return owner;
        }

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        public boolean isCleanupPending() {
            return cleanupPending;
        }

        public void setCleanupPending(boolean cleanupPending) {
            this.cleanupPending = cleanupPending;
        }

        public String getSessionId() {
            return sessionId;
        }

        public void setSessionId(String sessionId) {
            this.sessionId = sessionId;
        }

        public String getTrialId() {
            return trialId;
        }

        public void setTrialId(String trialId) {
            this.trialId = trialId;
        }

        public String getGodId() {
            return godId;
        }

        public void setGodId(String godId) {
            this.godId = godId;
        }

        public TrialSessionState getState() {
            try {
                return TrialSessionState.valueOf(state);
            } catch (IllegalArgumentException ignored) {
                return TrialSessionState.INACTIVE;
            }
        }

        public void setState(TrialSessionState state) {
            this.state = state.name();
        }

        public int getPhase() {
            return phase;
        }

        public void setPhase(int phase) {
            this.phase = phase;
        }

        public String getPhaseName() {
            return phaseName;
        }

        public void setPhaseName(String phaseName) {
            this.phaseName = phaseName;
        }

        public String getObjectiveText() {
            return objectiveText;
        }

        public void setObjectiveText(String objectiveText) {
            this.objectiveText = objectiveText;
        }

        public int getObjectiveCurrent() {
            return objectiveCurrent;
        }

        public void setObjectiveCurrent(int objectiveCurrent) {
            this.objectiveCurrent = objectiveCurrent;
        }

        public int getObjectiveTarget() {
            return objectiveTarget;
        }

        public void setObjectiveTarget(int objectiveTarget) {
            this.objectiveTarget = objectiveTarget;
        }

        public int getPhaseDuration() {
            return phaseDuration;
        }

        public void setPhaseDuration(int phaseDuration) {
            this.phaseDuration = phaseDuration;
        }

        public int getPhaseTicksRemaining() {
            return phaseTicksRemaining;
        }

        public void setPhaseTicksRemaining(int phaseTicksRemaining) {
            this.phaseTicksRemaining = phaseTicksRemaining;
        }

        public int getCountdownTicksRemaining() {
            return countdownTicksRemaining;
        }

        public void setCountdownTicksRemaining(int countdownTicksRemaining) {
            this.countdownTicksRemaining = countdownTicksRemaining;
        }

        public int getReturnTicksRemaining() {
            return returnTicksRemaining;
        }

        public void setReturnTicksRemaining(int returnTicksRemaining) {
            this.returnTicksRemaining = returnTicksRemaining;
        }

        public String getReturnDimension() {
            return returnDimension;
        }

        public void setReturnDimension(ResourceKey<Level> returnDimension) {
            this.returnDimension = returnDimension.location().toString();
        }

        public double getReturnX() {
            return returnX;
        }

        public void setReturnX(double returnX) {
            this.returnX = returnX;
        }

        public double getReturnY() {
            return returnY;
        }

        public void setReturnY(double returnY) {
            this.returnY = returnY;
        }

        public double getReturnZ() {
            return returnZ;
        }

        public void setReturnZ(double returnZ) {
            this.returnZ = returnZ;
        }

        public float getReturnYRot() {
            return returnYRot;
        }

        public void setReturnYRot(float returnYRot) {
            this.returnYRot = returnYRot;
        }

        public float getReturnXRot() {
            return returnXRot;
        }

        public void setReturnXRot(float returnXRot) {
            this.returnXRot = returnXRot;
        }

        public String getArenaDimension() {
            return arenaDimension;
        }

        public void setArenaDimension(ResourceKey<Level> arenaDimension) {
            this.arenaDimension = arenaDimension.location().toString();
        }

        public int getArenaX() {
            return arenaX;
        }

        public void setArenaX(int arenaX) {
            this.arenaX = arenaX;
        }

        public int getArenaY() {
            return arenaY;
        }

        public void setArenaY(int arenaY) {
            this.arenaY = arenaY;
        }

        public int getArenaZ() {
            return arenaZ;
        }

        public void setArenaZ(int arenaZ) {
            this.arenaZ = arenaZ;
        }

        public int getArenaSlot() {
            return arenaSlot;
        }

        public void setArenaSlot(int arenaSlot) {
            this.arenaSlot = arenaSlot;
        }

        public boolean hasArenaOrigin() {
            return arenaSlot >= 0;
        }

        public void clear() {
            active = false;
            cleanupPending = false;
            sessionId = "";
            trialId = "";
            godId = "";
            state = TrialSessionState.INACTIVE.name();
            phase = 0;
            phaseName = "";
            objectiveText = "";
            objectiveCurrent = 0;
            objectiveTarget = 0;
            phaseDuration = 0;
            phaseTicksRemaining = 0;
            countdownTicksRemaining = 0;
            returnTicksRemaining = 0;
            returnDimension = "";
            returnX = 0;
            returnY = 0;
            returnZ = 0;
            returnYRot = 0;
            returnXRot = 0;
            arenaDimension = "";
            arenaX = 0;
            arenaY = 0;
            arenaZ = 0;
            arenaSlot = -1;
        }

        @Override
        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            tag.putBoolean("active", active);
            tag.putBoolean("cleanup_pending", cleanupPending);
            tag.putString("session_id", sessionId);
            tag.putString("trial_id", trialId);
            tag.putString("god_id", godId);
            tag.putString("state", state);
            tag.putInt("phase", phase);
            tag.putString("phase_name", phaseName);
            tag.putString("objective_text", objectiveText);
            tag.putInt("objective_current", objectiveCurrent);
            tag.putInt("objective_target", objectiveTarget);
            tag.putInt("phase_duration", phaseDuration);
            tag.putInt("phase_ticks_remaining", phaseTicksRemaining);
            tag.putInt("countdown_ticks_remaining", countdownTicksRemaining);
            tag.putInt("return_ticks_remaining", returnTicksRemaining);
            tag.putString("return_dimension", returnDimension);
            tag.putDouble("return_x", returnX);
            tag.putDouble("return_y", returnY);
            tag.putDouble("return_z", returnZ);
            tag.putFloat("return_y_rot", returnYRot);
            tag.putFloat("return_x_rot", returnXRot);
            tag.putString("arena_dimension", arenaDimension);
            tag.putInt("arena_x", arenaX);
            tag.putInt("arena_y", arenaY);
            tag.putInt("arena_z", arenaZ);
            tag.putInt("arena_slot", arenaSlot);
            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag tag) {
            active = tag.getBoolean("active");
            cleanupPending = tag.getBoolean("cleanup_pending");
            sessionId = tag.getString("session_id");
            trialId = tag.getString("trial_id");
            godId = tag.getString("god_id");
            state = tag.getString("state");
            phase = tag.getInt("phase");
            phaseName = tag.getString("phase_name");
            objectiveText = tag.getString("objective_text");
            objectiveCurrent = tag.getInt("objective_current");
            objectiveTarget = tag.getInt("objective_target");
            phaseDuration = tag.getInt("phase_duration");
            phaseTicksRemaining = tag.getInt("phase_ticks_remaining");
            countdownTicksRemaining = tag.getInt("countdown_ticks_remaining");
            returnTicksRemaining = tag.getInt("return_ticks_remaining");
            returnDimension = tag.getString("return_dimension");
            returnX = tag.getDouble("return_x");
            returnY = tag.getDouble("return_y");
            returnZ = tag.getDouble("return_z");
            returnYRot = tag.getFloat("return_y_rot");
            returnXRot = tag.getFloat("return_x_rot");
            arenaDimension = tag.getString("arena_dimension");
            arenaX = tag.getInt("arena_x");
            arenaY = tag.getInt("arena_y");
            arenaZ = tag.getInt("arena_z");
            arenaSlot = tag.getInt("arena_slot");
        }
    }
}
