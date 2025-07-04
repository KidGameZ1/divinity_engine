package net.nightshade.divinity_engine.divinity.blessing.aethon;

import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;

import java.awt.*;

/**
 * A blessing that creates a protective mirror effect, reflecting incoming projectiles back to their source.
 * The mirror effect is temporary and has a limited number of reflections.
 */
public class StarlitMirror extends Blessings {

    /**
     * Creates a new StarlitMirror blessing.
     *
     * @param neededFavor The amount of favor required to use this blessing
     * @param cooldown    The cooldown period in ticks
     * @param isActive    Whether the blessing is currently active
     * @param canToggle   Whether the blessing can be toggled on/off
     * @param textColor   The color used for blessing text in the UI
     */
    public StarlitMirror(int neededFavor, int cooldown, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isActive, canToggle, textColor);
    }

    /**
     * Creates a default instance of this blessing with initialized properties.
     * Sets up reflection count, activation time, maximum reflects, and duration.
     *
     * @return A new instance of the blessing with default values
     */
    @Override
    public <T extends BlessingsInstance> T createDefaultInstance() {
        T instance = super.createDefaultInstance();
        if (!instance.getOrCreateTag().contains("reflect_count"))
            instance.getOrCreateTag().putInt("reflect_count", 0);
        if (!instance.getOrCreateTag().contains("activation_time"))
            instance.getOrCreateTag().putLong("activation_time", 0);
        if (!instance.getOrCreateTag().contains("max_reflects"))
            instance.getOrCreateTag().putInt("max_reflects", 3);
        if (!instance.getOrCreateTag().contains("duration"))
            instance.getOrCreateTag().putInt("duration", 6000);
        return instance;
    }

    /**
     * Handles the activation of the blessing.
     * Resets the reflection counter and activation time, and spawns activation particles.
     *
     * @param instance The blessing instance being toggled
     * @param entity   The entity that activated the blessing
     */
    @Override
    public void onToggleOn(BlessingsInstance instance, LivingEntity entity) {
        instance.getOrCreateTag().putLong("activation_time", System.currentTimeMillis());
        instance.getOrCreateTag().putInt("reflect_count", 0);
        entity.level().addParticle(net.minecraft.core.particles.ParticleTypes.END_ROD,
                entity.getX(), entity.getY() + 2, entity.getZ(),
                0, 0, 0);
        super.onToggleOn(instance, entity);
    }

    /**
     * Handles projectile reflection when the blessing is active.
     * Reflects projectiles back to their source if conditions are met.
     *
     * @param instance The blessing instance
     * @param living   The entity with the blessing
     * @param event    The projectile impact event
     * @return true if the projectile was reflected, false otherwise
     */
    @Override
    public boolean onProjectileHit(BlessingsInstance instance, LivingEntity living, ProjectileImpactEvent event) {
        long currentTime = System.currentTimeMillis();
        long activationTime = instance.getOrCreateTag().getLong("activation_time");
        int reflectCount = instance.getOrCreateTag().getInt("reflect_count");
        int maxReflects = instance.getOrCreateTag().getInt("max_reflects");
        int duration = instance.getOrCreateTag().getInt("duration");
        if(event.getProjectile().getOwner() == living) return false;
        if (currentTime - activationTime <= duration && reflectCount < maxReflects) {
            instance.getOrCreateTag().putInt("reflect_count", reflectCount + 1);

            LivingEntity shooter = (LivingEntity) event.getProjectile().getOwner();
            if (shooter != null) {
                double dx = shooter.getX() - living.getX();
                double dy = shooter.getY() - living.getY();
                double dz = shooter.getZ() - living.getZ();

                event.getProjectile().setDeltaMovement(dx * 0.5, dy * 0.5, dz * 0.5);
                event.getProjectile().setGlowingTag(true);
                event.setCanceled(true);

                living.level().addParticle(net.minecraft.core.particles.ParticleTypes.ENCHANTED_HIT,
                        living.getX(), living.getY() + 1, living.getZ(),
                        0, 0, 0);
                living.playSound(net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 2.0F);
            }
            return true;
        } else if (currentTime - activationTime >= duration) {
            instance.setToggled(false);
            return true;
        }
        return super.onProjectileHit(instance, living, event);
    }
}