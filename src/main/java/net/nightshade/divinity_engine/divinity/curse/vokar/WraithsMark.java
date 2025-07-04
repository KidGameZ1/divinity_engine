package net.nightshade.divinity_engine.divinity.curse.vokar;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.nightshade.divinity_engine.divinity.curse.Curse;
import net.nightshade.divinity_engine.divinity.curse.CurseInstance;

public class WraithsMark extends Curse {

    @Override
    public void onTakenDamage(CurseInstance instance, LivingDamageEvent event) {

        if (event.getSource().getEntity() instanceof LivingEntity living){
            if (living.getMobType().equals(MobType.UNDEAD)){
                event.setAmount(event.getAmount() + event.getAmount()*0.25f);
            }
        }

        super.onTakenDamage(instance, event);
    }
}
