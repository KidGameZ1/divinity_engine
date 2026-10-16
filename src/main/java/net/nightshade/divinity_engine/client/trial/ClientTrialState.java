package net.nightshade.divinity_engine.client.trial;

import net.minecraft.nbt.CompoundTag;
import net.nightshade.divinity_engine.util.divinity.trials.TrialSessionState;

public final class ClientTrialState {
    private static boolean active;
    private static String sessionState = TrialSessionState.INACTIVE.name();
    private static String godName = "";
    private static String trialName = "";
    private static int phase;
    private static String phaseName = "";
    private static String objectiveText = "";
    private static int objectiveCurrent;
    private static int objectiveTarget;
    private static int phaseDuration;
    private static int phaseTicksRemaining;
    private static int countdownTicksRemaining;
    private static int returnTicksRemaining;
    private static String statusText = "";

    private ClientTrialState() {
    }

    public static void apply(CompoundTag tag) {
        if (tag == null || !tag.getBoolean("active")) {
            clear();
            return;
        }

        active = true;
        sessionState = tag.getString("session_state");
        godName = tag.getString("god_name");
        trialName = tag.getString("trial_name");
        phase = tag.getInt("phase");
        phaseName = tag.getString("phase_name");
        objectiveText = tag.getString("objective_text");
        objectiveCurrent = tag.getInt("objective_current");
        objectiveTarget = tag.getInt("objective_target");
        phaseDuration = tag.getInt("phase_duration");
        phaseTicksRemaining = tag.getInt("phase_ticks_remaining");
        countdownTicksRemaining = tag.getInt("countdown_ticks_remaining");
        returnTicksRemaining = tag.getInt("return_ticks_remaining");
        statusText = tag.getString("status_text");
    }

    public static void clear() {
        active = false;
        sessionState = TrialSessionState.INACTIVE.name();
        godName = "";
        trialName = "";
        phase = 0;
        phaseName = "";
        objectiveText = "";
        objectiveCurrent = 0;
        objectiveTarget = 0;
        phaseDuration = 0;
        phaseTicksRemaining = 0;
        countdownTicksRemaining = 0;
        returnTicksRemaining = 0;
        statusText = "";
    }

    public static boolean shouldRender() {
        return active;
    }

    public static TrialSessionState getSessionState() {
        try {
            return TrialSessionState.valueOf(sessionState);
        } catch (IllegalArgumentException ignored) {
            return TrialSessionState.INACTIVE;
        }
    }

    public static String getGodName() {
        return godName;
    }

    public static String getTrialName() {
        return trialName;
    }

    public static int getPhase() {
        return phase;
    }

    public static String getPhaseName() {
        return phaseName;
    }

    public static String getObjectiveText() {
        return objectiveText;
    }

    public static int getObjectiveCurrent() {
        return objectiveCurrent;
    }

    public static int getObjectiveTarget() {
        return objectiveTarget;
    }

    public static int getPhaseDuration() {
        return phaseDuration;
    }

    public static int getPhaseTicksRemaining() {
        return phaseTicksRemaining;
    }

    public static int getCountdownTicksRemaining() {
        return countdownTicksRemaining;
    }

    public static int getReturnTicksRemaining() {
        return returnTicksRemaining;
    }

    public static String getStatusText() {
        return statusText;
    }
}
