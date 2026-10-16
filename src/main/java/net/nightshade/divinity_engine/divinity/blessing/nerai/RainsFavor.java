package net.nightshade.divinity_engine.divinity.blessing.nerai;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals.Cue;

import java.awt.*;

public class RainsFavor extends Blessings {
    public RainsFavor(int neededFavor, int cooldown, boolean isPassive, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isPassive, isActive, canToggle, textColor);
    }

    @Override
    public boolean onTick(BlessingsInstance instance, LivingEntity living) {
        if (living.isInWaterRainOrBubble()){
            if (!living.hasEffect(MobEffects.REGENERATION)){
                if (living.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 0, false, false, true))) {
                    BlessingVisuals.pulse(instance, living, Cue.HEAL);
                }
            }
            if (living.hasEffect(MobEffects.REGENERATION)) {
                BlessingVisuals.at(instance, living, living.position().add(0, living.getBbHeight() + 0.1, 0), Cue.AURA, 0.6);
            }
        }
        return super.onTick(instance, living);
    }
}
