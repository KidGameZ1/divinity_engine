package net.nightshade.divinity_engine.divinity.blessing.nerai;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDrownEvent;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;

import java.awt.*;

public class AquaticGrace extends Blessings {
    public AquaticGrace(int neededFavor, int cooldown, boolean isPassive, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isPassive, isActive, canToggle, textColor);
    }

    @Override
    public boolean onTick(BlessingsInstance instance, LivingEntity living) {
        if (living.isInWaterOrBubble()){
            if (!living.hasEffect(MobEffects.DOLPHINS_GRACE)){
                living.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 200, 0, false, false, true));
            }
            if (!living.hasEffect(MobEffects.WATER_BREATHING)){
                living.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 200, 0, false, false, true));
            }
        }
        return super.onTick(instance, living);
    }
}
