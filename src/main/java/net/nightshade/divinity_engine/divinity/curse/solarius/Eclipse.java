package net.nightshade.divinity_engine.divinity.curse.solarius;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.nightshade.divinity_engine.divinity.curse.Curse;
import net.nightshade.divinity_engine.divinity.curse.CurseInstance;

public class Eclipse extends Curse {

    @Override
    public void onTick(CurseInstance instance, LivingEntity living) {
        if (living.level().isDay()){
            if (living.level().canSeeSky(living.blockPosition())){
                living.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0, false, false, true));
                living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0, false, false, true));
            }
        }
        super.onTick(instance, living);
    }
}
