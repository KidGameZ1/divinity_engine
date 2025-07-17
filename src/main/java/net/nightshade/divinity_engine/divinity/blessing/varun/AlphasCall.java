package net.nightshade.divinity_engine.divinity.blessing.varun;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;

import java.awt.*;
import java.util.List;

public class AlphasCall extends Blessings {
    /**
     * Creates a new blessing with the specified parameters.
     *
     * @param neededFavor Amount of favor required to activate
     * @param cooldown    Cooldown period in seconds
     * @param isPassive   Is a passive blessing or not
     * @param canToggle   Whether blessing can be toggled
     * @param textColor   Color for blessing text
     */
    public AlphasCall(int neededFavor, int cooldown, boolean isPassive, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isPassive, isActive, canToggle, textColor);
    }

    @Override
    public boolean onPressed(BlessingsInstance instance, LivingEntity living) {
        if (living.level() instanceof ServerLevel serverLevel) {
            Vec3 position = living.position();
            AABB affectedArea = new AABB(
                    position.x - 2.5,
                    position.y - 2.5,
                    position.z - 2.5,
                    position.x + 2.5,
                    position.y + 2.5,
                    position.z + 2.5
            );
            List<LivingEntity> livingEntityList = serverLevel.getEntitiesOfClass(LivingEntity.class, affectedArea);
            livingEntityList.remove(living);
            for (LivingEntity entity : livingEntityList) {
                if (entity != living) {
                    entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 255, false, false, true));

                    Vec3 difference = entity.position().subtract(living.position());
                    double strength = 0.05;
                    entity.push(
                            difference.x * strength,
                            0.4D,
                            difference.z * strength
                    );
                }
            }
            if (!livingEntityList.isEmpty()) {
                for (int i = 0; i < 100; i++) {
                    double angle = living.level().random.nextDouble() * 2 * Math.PI;
                    double radius = living.level().random.nextDouble() * 3 + 0.5;
                    double x = living.getX() + Math.cos(angle) * radius;
                    double y = living.getY() + 0.2 + living.level().random.nextDouble() * living.level().random.nextDouble() ;
                    double z = living.getZ() + Math.sin(angle) * radius;

                    serverLevel.sendParticles(ParticleTypes.CLOUD, x, y, z, 1, 0, 0.01, 0, 0);
                    serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, x, y, z, 1, 0, 0.01, 0, 0);
                }
                return true;
            }
        }
        return super.onPressed(instance, living);
    }
}
