package net.nightshade.divinity_engine.divinity.blessing.elaris;

import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.util.divinity.blessings.RewindHistory;

import java.awt.Color;

public class ArcaneRecall extends Blessings {
    public ArcaneRecall(int favor, int cooldown, boolean passive, boolean active, boolean toggle, Color color) {
        super(favor, cooldown, passive, active, toggle, color);
    }

    @Override
    public boolean onTick(BlessingsInstance instance, LivingEntity living) {
        RewindHistory.record(instance, living, 40);
        return false;
    }

    @Override
    public boolean onTakenDamage(BlessingsInstance instance, LivingDamageEvent event) {
        return !event.isCanceled() && event.getAmount() > 0 && instance.getCooldown() == 0
                && RewindHistory.rewind(instance, event.getEntity(), 40);
    }
}
