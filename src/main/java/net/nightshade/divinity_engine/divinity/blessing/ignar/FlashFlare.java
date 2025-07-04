package net.nightshade.divinity_engine.divinity.blessing.ignar;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.server.level.ServerLevel;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;

import java.awt.*;
import java.util.List;

/**
 * A blessing that creates a powerful flash of light and fire around the user,
 * damaging and blinding nearby entities while creating flame particles.
 */
public class FlashFlare extends Blessings {
    /**
     * Constructs a new FlashFlare blessing.
     *
     * @param neededFavor The amount of favor required to use this blessing
     * @param cooldown    The cooldown period in ticks before the blessing can be used again
     * @param isActive    Whether the blessing is currently active
     * @param canToggle   Whether the blessing can be toggled on/off
     * @param textColor   The color used for displaying the blessing in the UI
     */
    public FlashFlare(int neededFavor, int cooldown, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isActive, canToggle, textColor);
    }

    /**
     * Activates the FlashFlare blessing, creating an explosive burst of light and fire.
     * Affects entities within a 5-block radius, dealing 8 damage, setting them on fire,
     * and temporarily blinding them. Also creates flame and flash particle effects.
     *
     * @param instance The blessing instance being activated
     * @param living   The entity activating the blessing
     * @return true if the blessing was successfully activated
     */
    @Override
    public boolean onPressed(BlessingsInstance instance, LivingEntity living) {
        Level level = living.level();
        AABB areaOfEffect = living.getBoundingBox().inflate(5.0D);
        List<LivingEntity> nearbyEntities = level.getEntitiesOfClass(LivingEntity.class, areaOfEffect);

        for (LivingEntity target : nearbyEntities) {
            if (target != living && !target.isDeadOrDying()) {
                target.hurt(level.damageSources().generic(), 8.0F);
                target.setSecondsOnFire(4);
                target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
            }
        }

        if (level instanceof ServerLevel serverLevel) {
            double x = living.getX();
            double y = living.getY();
            double z = living.getZ();

            for (int i = 0; i < 50; i++) {
                double offsetX = living.getRandom().nextGaussian() * 3.0D;
                double offsetY = living.getRandom().nextGaussian() * 3.0D;
                double offsetZ = living.getRandom().nextGaussian() * 3.0D;
                serverLevel.sendParticles(ParticleTypes.FLAME, x + offsetX, y + offsetY, z + offsetZ, 1, 0, 0, 0, 0.1);
            }
            serverLevel.sendParticles(ParticleTypes.FLASH, x, y, z, 10, 0.5, 0.5, 0.5, 0.1);
        }

        return true;
    }
}
