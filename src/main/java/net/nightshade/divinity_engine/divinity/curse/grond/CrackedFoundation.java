package net.nightshade.divinity_engine.divinity.curse.grond;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.nightshade.divinity_engine.divinity.curse.Curse;
import net.nightshade.divinity_engine.divinity.curse.CurseInstance;

public class CrackedFoundation extends Curse {
    @Override
    public void onTick(CurseInstance instance, LivingEntity living) {

        if (living.getY() <= 40){
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1, false, false, true));
            living.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 1, false, false, true));
        }

        super.onTick(instance, living);
    }
}
