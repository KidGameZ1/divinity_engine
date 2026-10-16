package net.nightshade.divinity_engine.util.divinity.trials;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.nightshade.divinity_engine.divinity.trial.GodlyTrial;
import net.nightshade.divinity_engine.util.DivinityEngineHelper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public record GodlyTrials() {
    public static List<GodlyTrial> godlyTrials = new ArrayList<>();

    public void add(GodlyTrial trial) {
        godlyTrials.add(trial);
    }

    public int size() {
        return godlyTrials.size();
    }

    public GodlyTrial getTrial(int index) {
        return godlyTrials.get(index);
    }

    public boolean contains(GodlyTrial trial) {
        for(GodlyTrial godlyTrial : godlyTrials) {
            if(godlyTrial == trial) {
                return true;
            }
        }
        return false;
    }
}