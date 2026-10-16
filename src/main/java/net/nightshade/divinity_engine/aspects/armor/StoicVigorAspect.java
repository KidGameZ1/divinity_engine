package net.nightshade.divinity_engine.aspects.armor;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.nightshade.vanguards_kit.core.battle_aspects.BattleAspect;

public class StoicVigorAspect extends BattleAspect {
    public StoicVigorAspect(String id) {
        super(id, true);
    }

    @Override
    public void onPlayerTakingDamage(LivingDamageEvent event) {
        if (!event.getEntity().hasEffect(MobEffects.DAMAGE_BOOST)){
            event.getEntity().forceAddEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 0, false, true), event.getEntity());
        }
        super.onPlayerTakingDamage(event);
    }
}
