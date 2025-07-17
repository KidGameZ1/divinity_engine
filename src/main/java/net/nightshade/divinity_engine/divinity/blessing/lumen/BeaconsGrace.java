package net.nightshade.divinity_engine.divinity.blessing.lumen;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.LightLayer;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;

import java.awt.*;
import java.util.UUID;

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
        if (living.level().getBrightness(LightLayer.BLOCK, living.blockPosition()) >= 12) {
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 1, false, false, true));
        } else if (living.level().isDay() && living.level().canSeeSkyFromBelowWater(living.blockPosition())) {
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 1, false, false, true));
        }
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
        if (target.level().getBrightness(LightLayer.BLOCK, target.blockPosition()) >= 12 && event.getNewTarget() != null && event.getNewTarget().distanceTo(target) <= 2) {
            event.setCanceled(true);
        }
        if (target.level().isDay() && target.level().canSeeSkyFromBelowWater(target.blockPosition())) {
            event.setCanceled(true);
        }
        return false;
    }
}
