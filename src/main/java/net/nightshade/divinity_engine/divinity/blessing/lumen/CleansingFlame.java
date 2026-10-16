package net.nightshade.divinity_engine.divinity.blessing.lumen;

import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals.Cue;

import java.awt.*;

/**
 * A blessing that removes negative status effects from the player when dealing damage.
 * Triggers a cleansing visual when an effect is removed.
 */
public class CleansingFlame extends Blessings {

    /**
     * @param neededFavor Amount of favor required to activate this blessing
     * @param cooldown    Time in ticks between activations
     * @param isPassive    Whether the blessing is currently active
     * @param canToggle   Whether the blessing can be toggled on/off
     * @param textColor   Color used for blessing text in UI
     */
    public CleansingFlame(int neededFavor, int cooldown, boolean isPassive, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isPassive, isActive, canToggle, textColor);
    }

    /**
     * Triggered when the player deals damage. Removes the listed negative status effects and
     * creates a visual around the player when cleansing succeeds.
     *
     * @param instance The blessing instance
     * @param player   The player entity
     * @param event    The damage event
     * @return true to indicate the event was handled
     */
    @Override
    public boolean onDamageEntity(BlessingsInstance instance, LivingEntity player, LivingHurtEvent event) {
        boolean cleansed = player.removeEffect(MobEffects.POISON);
        cleansed |= player.removeEffect(MobEffects.WITHER);
        cleansed |= player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        cleansed |= player.removeEffect(MobEffects.WEAKNESS);
        cleansed |= player.removeEffect(MobEffects.HUNGER);
        cleansed |= player.removeEffect(MobEffects.DIG_SLOWDOWN);
        cleansed |= player.removeEffect(MobEffects.CONFUSION);
        cleansed |= player.removeEffect(MobEffects.BLINDNESS);

        if (cleansed) {
            BlessingVisuals.pulse(instance, player, Cue.HEAL);
        }
        return true;
    }
}
