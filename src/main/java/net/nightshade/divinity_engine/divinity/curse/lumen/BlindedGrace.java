package net.nightshade.divinity_engine.divinity.curse.lumen;

import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.nightshade.divinity_engine.divinity.curse.Curse;
import net.nightshade.divinity_engine.divinity.curse.CurseInstance;

public class BlindedGrace extends Curse {
    @Override
    public void onHeal(CurseInstance instance, LivingHealEvent event) {
        if (event.getEntity().hasEffect(MobEffects.REGENERATION)){
            event.setAmount(event.getAmount() * 0.7f);
        }else {
            event.setAmount(event.getAmount() * 0.5f);
        }
        super.onHeal(instance, event);
    }
}
