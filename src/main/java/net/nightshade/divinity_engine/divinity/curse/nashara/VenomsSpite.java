package net.nightshade.divinity_engine.divinity.curse.nashara;

import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.nightshade.divinity_engine.divinity.curse.Curse;
import net.nightshade.divinity_engine.divinity.curse.CurseInstance;

import java.util.Random;

public class VenomsSpite extends Curse {

    @Override
    public void onTakenDamage(CurseInstance instance, LivingDamageEvent event) {
        Random random = new Random();
        if (event.getSource().getEntity() instanceof LivingEntity){
            if (random.nextFloat() < 0.4f){
                event.getEntity().addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1, false, false, true));
            }
        }else if (event.getSource().is(DamageTypes.MAGIC)){
            if (event.getEntity().hasEffect(MobEffects.POISON)){
                event.setAmount(event.getAmount() + 5);
            }
        }


        super.onTakenDamage(instance, event);
    }
}
