package net.nightshade.divinity_engine.aspects.tool;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.ShieldBlockEvent;
import net.nightshade.vanguards_kit.core.battle_aspects.BattleAspect;
import net.nightshade.vanguards_kit.network.event.custom_events.PostureBreakEvent;

public class BulwarkAspect extends BattleAspect {
    public BulwarkAspect(String id) {
        super(id, false);
    }

    @Override
    public void onPlayerBlocksWithShield(ShieldBlockEvent event) {

        if (!event.getEntity().hasEffect(MobEffects.DAMAGE_RESISTANCE)){
            event.getEntity().forceAddEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 0, false, true), (LivingEntity) event.getEntity());
        }

        super.onPlayerBlocksWithShield(event);
    }
}
