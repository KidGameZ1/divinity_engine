package net.nightshade.divinity_engine.divinity.curse.varun;

import net.minecraft.world.entity.animal.Animal;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.nightshade.divinity_engine.divinity.curse.Curse;
import net.nightshade.divinity_engine.divinity.curse.CurseInstance;

public class ScentOfPrey extends Curse {
    @Override
    public void onTakenDamage(CurseInstance instance, LivingDamageEvent event) {
        if (event.getSource().getEntity() instanceof Animal){
            event.setAmount(event.getAmount() + 2);
        }
        super.onTakenDamage(instance, event);
    }
}
