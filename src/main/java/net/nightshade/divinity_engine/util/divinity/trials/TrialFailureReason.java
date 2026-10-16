package net.nightshade.divinity_engine.util.divinity.trials;

import net.minecraft.network.chat.Component;

public enum TrialFailureReason {
    PLAYER_LEFT_TRIAL("The trial collapsed because you left the arena."),
    PLAYER_LOGGED_OUT("The trial ended when you left the world."),
    PLAYER_DIED("The trial has judged you unworthy."),
    PHASE_TIMED_OUT("You ran out of time and failed the phase."),
    INVALID_STATE("The trial ended unexpectedly."),
    GENERIC_FAILURE("The trial has failed.");

    private final String message;

    TrialFailureReason(String message) {
        this.message = message;
    }

    public Component getMessage() {
        return Component.literal(message);
    }
}
