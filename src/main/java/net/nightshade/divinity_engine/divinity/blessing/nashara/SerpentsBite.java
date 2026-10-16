package net.nightshade.divinity_engine.divinity.blessing.nashara;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals.Cue;
import net.nightshade.divinity_engine.util.divinity.narrative.BlessingSpecializations;
import net.nightshade.nightshade_core.util.MiscHelper;

import java.awt.*;

public class SerpentsBite extends Blessings {
    public SerpentsBite(int neededFavor, int cooldown, boolean isPassive, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isPassive, isActive, canToggle, textColor);
    }

    @Override
    public boolean onDamageEntity(BlessingsInstance instance, LivingEntity player, LivingHurtEvent event) {
        LivingEntity target = event.getEntity();

        if (!target.hasEffect(MobEffects.POISON)){
            int duration = MiscHelper.secondsToTick(BlessingSpecializations.isActive(player, "nashara", "a") ? 7 : 5);
            boolean applied = target.addEffect(new MobEffectInstance(MobEffects.POISON, duration, 1, false, false));
            if (applied && BlessingSpecializations.isActive(player, "nashara", "b")) {
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0));
            }
            if (applied) {
                BlessingVisuals.at(instance, player, target.getBoundingBox().getCenter(), Cue.IMPACT, 0.8);
            }
        }

        return super.onDamageEntity(instance, player, event);
    }
}
