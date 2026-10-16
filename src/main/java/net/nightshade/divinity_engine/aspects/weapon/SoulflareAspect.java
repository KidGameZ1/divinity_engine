package net.nightshade.divinity_engine.aspects.weapon;

import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.nightshade.vanguards_kit.core.battle_aspects.BattleAspect;

public class SoulflareAspect extends BattleAspect {
    public SoulflareAspect(String id) {
        super(id, false);
    }

    @Override
    public void onDamageEntity(LivingEntity player, LivingHurtEvent event) {
        LivingEntity living = event.getEntity();
        if (!living.isOnFire()){
            living.setSecondsOnFire(3);
        }

        super.onDamageEntity(player, event);
    }
}
