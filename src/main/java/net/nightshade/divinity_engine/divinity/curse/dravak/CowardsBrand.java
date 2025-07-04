package net.nightshade.divinity_engine.divinity.curse.dravak;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.nightshade.divinity_engine.divinity.curse.Curse;
import net.nightshade.divinity_engine.divinity.curse.CurseInstance;
import net.nightshade.nightshade_core.util.MiscHelper;

public class CowardsBrand extends Curse {

    @Override
    public void onTakenDamage(CurseInstance instance, LivingDamageEvent event) {

        event.getEntity().addEffect(new MobEffectInstance(MobEffects.WEAKNESS, MiscHelper.secondsToTick(5), 0, false, false, true));
        event.getEntity().addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, MiscHelper.secondsToTick(5), 0, false, false, true));

        super.onTakenDamage(instance, event);
    }
}
