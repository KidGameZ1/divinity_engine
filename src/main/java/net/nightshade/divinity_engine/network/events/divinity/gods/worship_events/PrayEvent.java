package net.nightshade.divinity_engine.network.events.divinity.gods.worship_events;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.Cancelable;
import net.nightshade.divinity_engine.divinity.gods.BaseGodInstance;
import net.nightshade.divinity_engine.network.events.divinity.gods.GodsEvent;

@Cancelable
public class PrayEvent extends GodsEvent {
    private final LivingEntity living;
    private int duration;


    public PrayEvent(BaseGodInstance instance, LivingEntity living, int duration) {
        super(instance);
        this.living = living;
        this.duration = duration;
    }

    public LivingEntity getLiving() {
        return living;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }
}
