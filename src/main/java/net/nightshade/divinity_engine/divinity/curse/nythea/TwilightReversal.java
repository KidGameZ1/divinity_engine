package net.nightshade.divinity_engine.divinity.curse.nythea;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.nightshade.divinity_engine.divinity.curse.Curse;
import net.nightshade.divinity_engine.divinity.curse.CurseInstance;

public class TwilightReversal extends Curse {

    @Override
    public void onTick(CurseInstance instance, LivingEntity living) {
        if (living.hasEffect(MobEffects.NIGHT_VISION)){
            living.removeEffect(MobEffects.NIGHT_VISION);
        }

        if (!living.level().isDay()){
            living.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0, false, false, true));
        }
        super.onTick(instance, living);
    }
}
