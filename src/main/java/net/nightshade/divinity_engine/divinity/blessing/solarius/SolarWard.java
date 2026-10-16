package net.nightshade.divinity_engine.divinity.blessing.solarius;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals.Cue;
import net.nightshade.divinity_engine.util.divinity.narrative.BlessingSpecializations;
import net.nightshade.nightshade_core.util.MiscHelper;

import java.awt.*;

public class SolarWard extends Blessings {
    /**
     * Creates a new blessing with the specified parameters.
     *
     * @param neededFavor Amount of favor required to activate
     * @param cooldown    Cooldown period in seconds
     * @param isPassive   Initial active state
     * @param canToggle   Whether blessing can be toggled
     * @param textColor   Color for blessing text
     */
    public SolarWard(int neededFavor, int cooldown, boolean isPassive, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isPassive, isActive, canToggle, textColor);
    }

    @Override
    public boolean onPressed(BlessingsInstance instance, LivingEntity living) {
        if (living.level().isDay()){
            float threshold = BlessingSpecializations.isActive(living, "solarius", "b") ? 0.65f : 0.5f;
            if (living.getHealth() < living.getMaxHealth() * threshold){
                int duration = MiscHelper.secondsToTick(BlessingSpecializations.isActive(living, "solarius", "a") ? 15 : 10);
                boolean warded = living.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, 0, false, false, true));
                living.addEffect(new MobEffectInstance(MobEffects.GLOWING, duration, 0, false, false, true));
                if (warded && !living.isInvisible()) BlessingVisuals.pulse(instance, living, Cue.WARD);
                return true;
            }
        }
        return super.onPressed(instance, living);
    }
}
