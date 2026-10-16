package net.nightshade.divinity_engine.divinity.blessing.ignar;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.util.RandomSource;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals.Cue;
import net.nightshade.divinity_engine.util.divinity.narrative.BlessingSpecializations;

import java.awt.*;

/**
 * A blessing that grants the ability to ignite enemies on hit.
 * Part of the Ignar blessing tree, this blessing provides a chance to set targets on fire
 * and creates flame particle effects for visual feedback.
 */
public class BlazingHands extends Blessings {
    /**
     * Constructs a new BlazingHands blessing.
     *
     * @param neededFavor Amount of favor points required to activate
     * @param cooldown    Cooldown duration in ticks
     * @param isPassive    Initial activation state
     * @param canToggle   Whether blessing can be toggled on/off
     * @param textColor   Color used for blessing UI elements
     */
    public BlazingHands(int neededFavor, int cooldown, boolean isPassive, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isPassive, isActive, canToggle, textColor);
    }

    /**
     * Handles entity damage events with a chance to apply fire effects.
     * When triggered, sets the target on fire and creates flame particles around them.
     *
     * @param instance Current blessing instance
     * @param player   Player entity causing the damage
     * @param event    The damage event being processed
     * @return true if fire effect was applied, false otherwise
     */
    @Override
    public boolean onDamageEntity(BlessingsInstance instance, LivingEntity player, LivingHurtEvent event) {
        RandomSource random = player.getRandom();
        float chance = BlessingSpecializations.isActive(player, "ignar", "a") ? 0.25f : 0.15f;
        if (random.nextFloat() <= chance) {
            LivingEntity target = event.getEntity();
            target.setRemainingFireTicks(BlessingSpecializations.isActive(player, "ignar", "b") ? 100 : 60);

            BlessingVisuals.at(instance, player, target.getBoundingBox().getCenter(), Cue.IMPACT,
                    Math.max(0.75, target.getBbWidth()));
            return true;
        }
        return super.onDamageEntity(instance, player, event);
    }
    
}
