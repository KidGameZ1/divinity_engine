package net.nightshade.divinity_engine.aspects.armor;

import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.nightshade.divinity_engine.util.DivinityEngineHelper;
import net.nightshade.vanguards_kit.core.battle_aspects.BattleAspect;

public class EnduringSparkAspect extends BattleAspect {
    public EnduringSparkAspect(String id) {
        super(id, true);
    }

    @Override
    public void onPlayerTakingDamage(LivingDamageEvent event) {
        if (DivinityEngineHelper.getBlessingSlot1(event.getEntity()).isOnCooldown()){
           DivinityEngineHelper.getBlessingSlot1(event.getEntity()).decreaseCooldown(1);
        }if (DivinityEngineHelper.getBlessingSlot2(event.getEntity()).isOnCooldown()){
           DivinityEngineHelper.getBlessingSlot2(event.getEntity()).decreaseCooldown(1);
        }if (DivinityEngineHelper.getBlessingSlot3(event.getEntity()).isOnCooldown()){
           DivinityEngineHelper.getBlessingSlot3(event.getEntity()).decreaseCooldown(1);
        }
        super.onPlayerTakingDamage(event);
    }
}
