package net.nightshade.divinity_engine.aspects.armor;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.nightshade.vanguards_kit.core.battle_aspects.BattleAspect;

public class SundererAspect extends BattleAspect {
    public SundererAspect(String id) {
        super(id, true);
    }

    @Override
    public void onPlayerBeingDamaged(LivingAttackEvent event) {
        if (event.getSource().getDirectEntity() instanceof LivingEntity living){
            if (!living.hasEffect(MobEffects.WEAKNESS)){
                living.forceAddEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, false, false), living);
            }
        }
        super.onPlayerBeingDamaged(event);
    }
}
