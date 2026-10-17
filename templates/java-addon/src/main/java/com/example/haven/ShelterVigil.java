package com.example.haven;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.nightshade.divinity_engine.divinity.trial.GodlyTrial;
import net.nightshade.divinity_engine.util.divinity.gods.GodHelper;

/** A deliberately short, noncombat example of the session lifecycle. */
public final class ShelterVigil extends GodlyTrial {
    private static final int VIGIL_TICKS = 100;

    public ShelterVigil(ServerPlayer player) { super(player); }

    @Override protected void onStart() {
        player.sendSystemMessage(Component.translatable("haven_addon.trial.start"));
    }

    @Override protected void runPhase(int phase) {
        phaseDuration = 200;
        setPhaseInfo("Shelter Vigil", "Keep watch for five seconds", 0, VIGIL_TICKS);
    }

    @Override public void tick() {
        int elapsed = getTicksElapsed();
        if (elapsed % 20 == 0) setObjectiveProgress(Math.min(elapsed, VIGIL_TICKS));
        if (elapsed >= VIGIL_TICKS) completeTrial();
    }

    @Override protected void onComplete() {
        GodHelper.increaseFavor(HavenAddon.HAVEN.get(), player, 5);
        player.sendSystemMessage(Component.translatable("haven_addon.trial.complete"));
    }

    @Override protected int getMaxPhases() { return 1; }
}
