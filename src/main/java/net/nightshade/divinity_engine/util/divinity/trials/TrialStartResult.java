package net.nightshade.divinity_engine.util.divinity.trials;

import net.minecraft.network.chat.Component;

public enum TrialStartResult {
    STARTED("The trial has begun."),
    ALREADY_ACTIVE("You are already inside a godly trial."),
    NO_TRIAL_REGISTERED("That god does not have a registered trial."),
    START_CONDITION_FAILED("You do not meet the conditions to start this trial."),
    ARENA_UNAVAILABLE("No trial arena is available right now."),
    INTERNAL_ERROR("The trial could not be started.");

    private final String message;

    TrialStartResult(String message) {
        this.message = message;
    }

    public boolean isSuccess() {
        return this == STARTED;
    }

    public Component getMessage() {
        return Component.literal(message);
    }
}
