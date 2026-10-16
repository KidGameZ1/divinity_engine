package net.nightshade.divinity_engine.divinity.blessing.nerai;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.tags.FluidTags;
import net.minecraftforge.common.ForgeMod;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals.Cue;
import net.nightshade.divinity_engine.util.divinity.narrative.BlessingSpecializations;

import java.awt.*;

public class AquaticGrace extends Blessings {
    public AquaticGrace(int neededFavor, int cooldown, boolean isPassive, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isPassive, isActive, canToggle, textColor);
    }

    @Override
    public boolean onTick(BlessingsInstance instance, LivingEntity living) {
        if (living.level().isClientSide) return false;
        BlessingSpecializations.modifier(living, ForgeMod.SWIM_SPEED.get(), BlessingSpecializations.AQUATIC_SPEED,
                "Tidal protection", living.isInWaterOrBubble() && BlessingSpecializations.isActive(living, "nerai", "b") ? 0.2 : 0);
        String healKey = "divinity_engine.aquatic_heal_at";
        if (living.isEyeInFluid(FluidTags.WATER) && BlessingSpecializations.isActive(living, "nerai", "a")) {
            long now = living.level().getGameTime();
            if (!living.getPersistentData().contains(healKey)) living.getPersistentData().putLong(healKey, now + 200);
            if (now >= living.getPersistentData().getLong(healKey)) {
                float healthBefore = living.getHealth();
                living.heal(1.0f);
                living.getPersistentData().putLong(healKey, now + 200);
                if (living.getHealth() > healthBefore) {
                    BlessingVisuals.pulse(instance, living, Cue.HEAL);
                }
            }
        } else {
            living.getPersistentData().remove(healKey);
        }
        if (living.isInWaterOrBubble()){
            if (!living.hasEffect(MobEffects.DOLPHINS_GRACE)){
                living.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 200, 0, false, false, true));
            }
            if (!living.hasEffect(MobEffects.WATER_BREATHING)){
                living.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 200, 0, false, false, true));
            }
            BlessingVisuals.at(instance, living, living.getEyePosition().add(0, -0.12, 0), Cue.AURA, 0.45);
        }
        return super.onTick(instance, living);
    }
}
