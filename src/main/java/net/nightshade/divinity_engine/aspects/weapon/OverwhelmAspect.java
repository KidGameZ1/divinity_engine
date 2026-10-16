package net.nightshade.divinity_engine.aspects.weapon;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.player.CriticalHitEvent;
import net.nightshade.vanguards_kit.core.battle_aspects.BattleAspect;
import net.nightshade.vanguards_kit.network.event.custom_events.PostureBreakEvent;

public class OverwhelmAspect extends BattleAspect {
    public OverwhelmAspect(String id) {
        super(id, false);
    }

    @Override
    public void onPostureBreak(PostureBreakEvent event) {
        if (event.getDamageSource().getDirectEntity() instanceof LivingEntity living){
            if(!living.hasEffect(MobEffects.DAMAGE_BOOST) && !living.hasEffect(MobEffects.DAMAGE_RESISTANCE)){
                living.forceAddEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 0, false, true), living);
                living.forceAddEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 0, false, true), living);
            }
        }
        super.onPostureBreak(event);
    }
}
