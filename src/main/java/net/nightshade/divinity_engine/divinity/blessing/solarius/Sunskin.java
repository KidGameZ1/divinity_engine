package net.nightshade.divinity_engine.divinity.blessing.solarius;

import net.minecraft.world.entity.LivingEntity;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals.Cue;

import java.awt.*;

public class Sunskin extends Blessings {
    public Sunskin(int neededFavor, int cooldown, boolean isPassive, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isPassive, isActive, canToggle, textColor);
    }

    @Override
    public boolean onTick(BlessingsInstance instance, LivingEntity living) {

        if (living.level().isDay()){
            if (living.getHealth() < living.getMaxHealth()){
                float healthBefore = living.getHealth();
                living.heal(0.02f);
                if (living.getHealth() > healthBefore && living.tickCount % 40 == 0 && !living.isInvisible()) {
                    BlessingVisuals.at(instance, living, living.getBoundingBox().getCenter(), Cue.HEAL, 0.3);
                }
            }
        }

        return false;
    }
}
