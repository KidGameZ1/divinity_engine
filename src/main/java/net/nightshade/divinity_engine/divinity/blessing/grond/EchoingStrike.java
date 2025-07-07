package net.nightshade.divinity_engine.divinity.blessing.grond;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;

import java.awt.*;

/**
 * A blessing that creates a shockwave effect when attacking entities,
 * dealing damage and knockback to nearby entities.
 */
public class EchoingStrike extends Blessings {
    /**
     * Creates a new EchoingStrike blessing
     *
     * @param neededFavor The favor points required to use this blessing
     * @param cooldown    The cooldown period in ticks
     * @param isActive    Whether the blessing is initially active
     * @param canToggle   Whether the blessing can be toggled on/off
     * @param textColor   The color used for blessing text in the UI
     */
    public EchoingStrike(int neededFavor, int cooldown, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isActive, canToggle, textColor);
    }

    /**
     * Creates a default instance of this blessing with standard configuration values
     *
     * @return A new blessing instance with default values for shockwave chance, radius, knockback and damage
     */
    @Override
    public <T extends BlessingsInstance> T createDefaultInstance() {
        T instance = super.createDefaultInstance();
        if (!instance.getOrCreateTag().contains("shockwave_chance"))
            instance.getOrCreateTag().putDouble("shockwave_chance", 1);
        if (!instance.getOrCreateTag().contains("shockwave_radius"))
            instance.getOrCreateTag().putDouble("shockwave_radius", 3.0);
        if (!instance.getOrCreateTag().contains("knockback_strength"))
            instance.getOrCreateTag().putFloat("knockback_strength", 1.0f);
        if (!instance.getOrCreateTag().contains("damage_amount"))
            instance.getOrCreateTag().putFloat("damage_amount", 4.0f);
        return instance;
    }

    /**
     * Handles the damage event by creating a shockwave that affects nearby entities
     *
     * @param instance The current blessing instance
     * @param player   The player using the blessing
     * @param event    The damage event
     * @return true if shockwave was triggered, false otherwise
     */
    @Override
    public boolean onDamageEntity(BlessingsInstance instance, LivingEntity player, LivingHurtEvent event) {
        double chance = instance.getOrCreateTag().getDouble("shockwave_chance");
        double radius = instance.getOrCreateTag().getDouble("shockwave_radius");
        float damage = instance.getOrCreateTag().getFloat("damage_amount");
        float knockback = instance.getOrCreateTag().getFloat("knockback_strength");

        if (player.level().random.nextDouble() < chance) {
            Level level = player.level();

            // Damage & knockback nearby entities
            level.getEntities(player, player.getBoundingBox().inflate(radius))
                    .stream()
                    .filter(entity -> entity instanceof LivingEntity && entity != player && entity != event.getEntity())
                    .forEach(entity -> {
                        entity.hurt(player.damageSources().generic(), damage);

                        double dx = entity.getX() - player.getX();
                        double dz = entity.getZ() - player.getZ();
                        double distance = Math.max(Math.sqrt(dx * dx + dz * dz), 0.1);
                        double strength = knockback / distance;

                        entity.setDeltaMovement(dx * strength, 0.5, dz * strength);
                    });

            // Particle shockwave (server-side only)
            if (level instanceof ServerLevel serverLevel) {
                for (int i = 0; i < 100; i++) {
                    double angle = level.random.nextDouble() * 2 * Math.PI;
                    double dist = level.random.nextDouble() * radius;
                    double x = player.getX() + Math.cos(angle) * dist;
                    double y = player.getY() + 0.5;
                    double z = player.getZ() + Math.sin(angle) * dist;

                    serverLevel.sendParticles(ParticleTypes.EXPLOSION, x, y, z, 1, 0, 0, 0, 0);
                    serverLevel.sendParticles(ParticleTypes.SMOKE, x, y, z, 1, 0, 0.02, 0, 0);
                }
            }

            return true;
        }
        return super.onDamageEntity(instance, player, event);
    }
}