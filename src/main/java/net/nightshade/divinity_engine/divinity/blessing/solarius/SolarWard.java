package net.nightshade.divinity_engine.divinity.blessing.solarius;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
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
            if (living.getHealth() < living.getMaxHealth()*0.5f){
                living.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, MiscHelper.secondsToTick(10), 0, false, false, true));
                living.addEffect(new MobEffectInstance(MobEffects.GLOWING, MiscHelper.secondsToTick(10), 0, false, false, true));
                return true;
            }
        }
        return super.onPressed(instance, living);
    }
}
