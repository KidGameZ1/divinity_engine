package net.nightshade.divinity_engine.divinity.curse.ignar;

import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.nightshade.divinity_engine.divinity.curse.Curse;
import net.nightshade.divinity_engine.divinity.curse.CurseInstance;
import net.nightshade.nightshade_core.util.MiscHelper;

public class FlameReversal extends Curse {
    @Override
    public void onTick(CurseInstance instance, LivingEntity living) {
        if (living.hasEffect(MobEffects.FIRE_RESISTANCE)){
            living.removeEffect(MobEffects.FIRE_RESISTANCE);
        }
        super.onTick(instance, living);
    }

    @Override
    public void onTakenDamage(CurseInstance instance, LivingDamageEvent event) {

        if (event.getSource().is(DamageTypes.ON_FIRE) || event.getSource().is(DamageTypes.IN_FIRE)){
            event.setAmount(event.getAmount() + 2);
            event.getEntity().setSecondsOnFire(MiscHelper.secondsToTick(event.getEntity().getRemainingFireTicks())+5);
        }

        super.onTakenDamage(instance, event);
    }
}
