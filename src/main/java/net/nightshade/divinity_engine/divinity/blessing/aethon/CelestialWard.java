package net.nightshade.divinity_engine.divinity.blessing.aethon;

import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;

import java.awt.*;

/**
 * A blessing that provides protection against incoming projectiles by creating a celestial barrier.
 * When activated, it can block projectiles and create visual/audio effects.
 */
public class CelestialWard extends Blessings {
    /**
     * Tracks the last time a projectile was blocked to enforce cooldown (in milliseconds)
     */
    private long lastBlockTime = 0;

    /**
     * Creates a new CelestialWard blessing
     *
     * @param neededFavor Amount of favor required to activate
     * @param cooldown    Cooldown period between uses
     * @param isActive    Whether the blessing is currently active
     * @param canToggle   Whether the blessing can be toggled on/off
     * @param textColor   Color used for UI display
     */
    public CelestialWard(int neededFavor, int cooldown, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isActive, canToggle, textColor);
    }

    /**
     * Handles projectile impact events, blocking incoming projectiles if conditions are met
     *
     * @param instance The active blessing instance
     * @param living   The entity with the blessing
     * @param event    The projectile impact event
     * @return true if the projectile was blocked, false otherwise
     */
    @Override
    public boolean onProjectileHit(BlessingsInstance instance, LivingEntity living, ProjectileImpactEvent event) {
        if(event.getProjectile().getOwner() == living) return false;
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastBlockTime >= 30000) {
            lastBlockTime = currentTime;
            living.level().addParticle(net.minecraft.core.particles.ParticleTypes.END_ROD,
                    living.getX(), living.getY() + 1, living.getZ(),
                    0, 0, 0);
            living.playSound(net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 1.0F);
            event.getProjectile().setDeltaMovement(0,0,0);
            event.setCanceled(true);
        }
        return true;
    }
}
