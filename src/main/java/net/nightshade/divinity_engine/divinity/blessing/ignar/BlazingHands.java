package net.nightshade.divinity_engine.divinity.blessing.ignar;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;

import java.awt.*;
import java.util.Random;

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
     * @param isActive    Initial activation state
     * @param canToggle   Whether blessing can be toggled on/off
     * @param textColor   Color used for blessing UI elements
     */
    public BlazingHands(int neededFavor, int cooldown, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isActive, canToggle, textColor);
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
        Random random = new Random();
        // 15% chance to trigger the fire effect
        if (random.nextFloat() <= 0.15f) {
            LivingEntity target = event.getEntity();
            // Set target on fire for 3 seconds (60 ticks)
            target.setRemainingFireTicks(60);

            // Create particle effects on server side only
            if (target.level() instanceof ServerLevel) {
                ServerLevel serverWorld = (ServerLevel) target.level();
                // Get target position with slight vertical offset
                double x = target.getX();
                double y = target.getY() + 0.5;
                double z = target.getZ();

                // Generate flame particles in random positions around the target
                for (int i = 0; i < 20; i++) {
                    double offsetX = random.nextDouble() * 2 - 1;  // Random offset between -1 and 1
                    double offsetY = random.nextDouble();          // Random height offset
                    double offsetZ = random.nextDouble() * 2 - 1;  // Random offset between -1 and 1
                    serverWorld.sendParticles(ParticleTypes.FLAME, x + offsetX, y + offsetY, z + offsetZ, 1, 0, 0, 0, 0);
                }
            }
            return true;
        }
        return super.onDamageEntity(instance, player, event);
    }
    
}
