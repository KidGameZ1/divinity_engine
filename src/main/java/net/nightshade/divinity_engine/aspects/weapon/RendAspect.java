package net.nightshade.divinity_engine.aspects.weapon;

import net.minecraftforge.event.entity.player.CriticalHitEvent;
import net.nightshade.vanguards_kit.core.battle_aspects.BattleAspect;

public class RendAspect extends BattleAspect {
    public RendAspect(String id) {
        super(id, false);
    }

    @Override
    public void onCriticalHit(CriticalHitEvent event) {
        event.setDamageModifier(event.getDamageModifier() + 1.5f);
        super.onCriticalHit(event);
    }
}
