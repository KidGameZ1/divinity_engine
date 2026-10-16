package net.nightshade.divinity_engine.aspects.armor;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.nightshade.vanguards_kit.core.battle_aspects.BattleAspect;

public class SecondSkinAspect extends BattleAspect {
    public SecondSkinAspect(String id) {
        super(id, true);
    }

    @Override
    public void onPlayerBeingDamaged(LivingAttackEvent event) {
        LivingEntity living = event.getEntity();
        if (!living.hasEffect(MobEffects.ABSORPTION)){
            living.forceAddEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 1, false, true), (Player) living);
        }

        super.onPlayerBeingDamaged(event);
    }
}
