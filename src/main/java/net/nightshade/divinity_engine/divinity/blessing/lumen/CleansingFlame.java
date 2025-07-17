package net.nightshade.divinity_engine.divinity.blessing.lumen;

import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;

import java.awt.*;

/**
 * A blessing that removes negative status effects from the player when taking damage.
 * Triggers a visual particle effect when activated.
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
     * Triggered when the player takes damage. Removes all negative status effects and creates
     * a visual particle effect around the player.
     *
     * @param instance The blessing instance
     * @param player   The player entity
     * @param event    The damage event
     * @return true to indicate the event was handled
     */
    @Override
    public boolean onDamageEntity(BlessingsInstance instance, LivingEntity player, LivingHurtEvent event) {
        player.removeEffect(MobEffects.POISON);
        player.removeEffect(MobEffects.WITHER);
        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        player.removeEffect(MobEffects.WEAKNESS);
        player.removeEffect(MobEffects.HUNGER);
        player.removeEffect(MobEffects.DIG_SLOWDOWN);
        player.removeEffect(MobEffects.CONFUSION);
        player.removeEffect(MobEffects.BLINDNESS);

        if (player.level() instanceof ServerLevel) {
            ((ServerLevel) player.level()).sendParticles(ParticleTypes.END_ROD,
                    player.getX(), player.getY() + 1, player.getZ(),
                    20, 0.5, 0.5, 0.5, 0.1);
        }
        return true;
    }
}