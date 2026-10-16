package net.nightshade.divinity_engine.divinity.blessing.nythea;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals.Cue;
import net.nightshade.divinity_engine.util.divinity.narrative.BlessingSpecializations;

import java.awt.*;

public class TwilightVeil extends Blessings {
    public TwilightVeil(int neededFavor, int cooldown, boolean isPassive, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isPassive, isActive, canToggle, textColor);
    }

    @Override
    public boolean onTick(BlessingsInstance instance, LivingEntity living) {
        if (living.level().isClientSide) return false;
        Level level = living.level();
        boolean isNight = !level.isDay() && BlessingSpecializations.isEligible(living, instance);
        double speed = BlessingSpecializations.isActive(living, "nythea", "b") ? 0.15 : 0.1;
        BlessingSpecializations.modifier(living, Attributes.MOVEMENT_SPEED,
                BlessingSpecializations.TWILIGHT_SPEED, "Twilight Speed", isNight ? speed : 0);
        BlessingSpecializations.effect(living, net.minecraft.world.effect.MobEffects.NIGHT_VISION,
                isNight && BlessingSpecializations.isActive(living, "nythea", "a"));

        if (isNight) {
            if (!living.isShiftKeyDown() && !living.isInvisible() && living.tickCount % 40 == 0
                    && living.getDeltaMovement().horizontalDistanceSqr() > 0.001) {
                Vec3 feet = living.position().add(0, 0.06, 0);
                Vec3 movement = living.getDeltaMovement().multiply(1, 0, 1).normalize().scale(0.25);
                BlessingVisuals.link(instance, living, feet, feet.add(movement), Cue.TRAIL);
            }
            if (living.isShiftKeyDown() && !living.hasEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY)) {
                if (!living.isInvisible()) {
                    living.setInvisible(true);
                    living.getPersistentData().putBoolean("divinity_engine.twilight_invisible", true);
                }
            } else {
                BlessingSpecializations.clearTwilightInvisibility(living);
            }
        } else {
            BlessingSpecializations.clearTwilightInvisibility(living);
        }


        return super.onTick(instance, living);
    }
}
