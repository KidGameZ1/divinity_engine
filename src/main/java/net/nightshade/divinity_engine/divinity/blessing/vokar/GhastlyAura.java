package net.nightshade.divinity_engine.divinity.blessing.vokar;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals.Cue;

import java.awt.*;

public class GhastlyAura extends Blessings {
    public GhastlyAura(int neededFavor, int cooldown, boolean isPassive, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isPassive, isActive, canToggle, textColor);
    }

    @Override
    public boolean onTick(BlessingsInstance instance, LivingEntity living) {
        boolean affected = false;
        for (LivingEntity entity : living.level().getEntitiesOfClass(LivingEntity.class, living.getBoundingBox().inflate(6.0D))) {
            if (entity != living && !entity.isAlliedTo(living)) {

                entity.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 40, 0, false, false, true));
                entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, false, false, true));
                entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, false, true));
                affected = true;
            }
        }
        if (affected) {
            BlessingVisuals.at(instance, living, living.position().add(0, 0.08, 0), Cue.AURA, 6.0);
        }
        return true;
    }
}
