package net.nightshade.divinity_engine.divinity.blessing.lumen;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LightLayer;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals.Cue;
import net.nightshade.divinity_engine.util.divinity.narrative.BlessingSpecializations;

import java.awt.*;

/**
 * Represents a blessing that provides speed boost and protection effects in well-lit areas or during daylight.
 * This blessing is part of the Lumen (light-based) blessing category.
 */
public class BeaconsGrace extends Blessings {
    /**
     * Constructs a new BeaconsGrace blessing with specified parameters.
     *
     * @param neededFavor The amount of favor required to activate this blessing
     * @param cooldown    The cooldown period between uses
     * @param isPassive    Whether the blessing is currently active
     * @param canToggle   Whether the blessing can be toggled on/off
     * @param textColor   The color used for displaying blessing information
     */
    public BeaconsGrace(int neededFavor, int cooldown, boolean isPassive, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isPassive, isActive, canToggle, textColor);
    }

    /**
     * Handles the blessing's tick event, applying movement speed boost in well-lit areas or during daylight.
     *
     * @param instance The blessing instance being processed
     * @param living   The living entity affected by the blessing
     * @return false to continue processing other blessings
     */
    @Override
    public boolean onTick(BlessingsInstance instance, LivingEntity living) {
        if (living.level().isClientSide) return false;
        int threshold = qualifyingLightThreshold(living);
        boolean bright = BlessingSpecializations.isBright(living, threshold);
        if (bright) {
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 1, false, false, true));
            BlessingVisuals.at(instance, living, living.position().add(0, 0.04, 0), Cue.AURA, 0.6);
        }
        BlessingSpecializations.effect(living, MobEffects.DAMAGE_RESISTANCE,
                bright && BlessingSpecializations.isActive(living, "lumen", "b"));
        return false;
    }

    /**
     * Handles targeting events, preventing entities from targeting the blessed entity in well-lit areas or during daylight.
     *
     * @param instance The blessing instance being processed
     * @param target   The entity being targeted
     * @param event    The targeting event
     * @return false to continue processing other blessings
     */
    @Override
    public boolean onBeingTargeted(BlessingsInstance instance, LivingEntity target, LivingChangeTargetEvent event) {
        int threshold = qualifyingLightThreshold(target);
        boolean protectedTarget = false;
        if (target.level().getBrightness(LightLayer.BLOCK, target.blockPosition()) >= threshold && event.getNewTarget() != null && event.getNewTarget().distanceTo(target) <= 2) {
            event.setCanceled(true);
            protectedTarget = true;
        }
        if (target.level().isDay() && target.level().canSeeSkyFromBelowWater(target.blockPosition())) {
            event.setCanceled(true);
            protectedTarget = true;
        }
        if (protectedTarget) {
            BlessingVisuals.at(instance, target, target.getBoundingBox().getCenter(), Cue.WARD, 0.85);
        }
        return false;
    }

    public static int qualifyingLightThreshold(LivingEntity player) {
        return BlessingSpecializations.isActive(player, "lumen", "a") ? 9 : 12;
    }
}
