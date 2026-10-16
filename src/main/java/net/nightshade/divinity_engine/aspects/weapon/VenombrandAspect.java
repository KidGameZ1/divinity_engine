package net.nightshade.divinity_engine.aspects.weapon;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.nightshade.vanguards_kit.core.battle_aspects.BattleAspect;

public class VenombrandAspect extends BattleAspect {
    public VenombrandAspect(String id) {
        super(id, false);
    }

    @Override
    public void onDamageEntity(LivingEntity player, LivingHurtEvent event) {
        LivingEntity living = event.getEntity();
        if (!living.hasEffect(MobEffects.POISON)){
            living.forceAddEffect(new MobEffectInstance(MobEffects.POISON, 40, 0, false, true), living);
        }


        super.onDamageEntity(player, event);
    }
}
