package net.nightshade.divinity_engine.divinity.curse.kairoth;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.nightshade.divinity_engine.divinity.curse.Curse;
import net.nightshade.divinity_engine.divinity.curse.CurseInstance;
import net.nightshade.nightshade_core.util.MathHelper;
import net.nightshade.nightshade_core.util.MiscHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public class FracturedFate extends Curse {

    @Override
    public void onTick(CurseInstance instance, LivingEntity living) {
        Random random = new Random();
        List<MobEffectInstance> effectInstanceList = new ArrayList<>();
        for (int i = 0; i < BuiltInRegistries.MOB_EFFECT.size(); i++){
            MobEffect effect = BuiltInRegistries.MOB_EFFECT.byId(i);
            if (effect != null && !effect.isBeneficial()){
                MobEffectInstance effectInstance = new MobEffectInstance(effect, MiscHelper.secondsToTick(10), 0, false, false, true);
                if (!effectInstanceList.contains(effectInstance))
                    effectInstanceList.add(new MobEffectInstance(effect, MiscHelper.secondsToTick(10), 0, false, false, true));
            }
        }
        if (!effectInstanceList.isEmpty()){
            if (random.nextFloat() < 0.001f){
                for (MobEffectInstance effectInstance : effectInstanceList){
                    if (living.hasEffect(effectInstance.getEffect())){
                        break;
                    }
                }
                living.addEffect(Objects.requireNonNull(MathHelper.pickRandom(effectInstanceList)));
            }
        }
        super.onTick(instance, living);
    }
}
