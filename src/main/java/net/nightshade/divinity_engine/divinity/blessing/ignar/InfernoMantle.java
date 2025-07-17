package net.nightshade.divinity_engine.divinity.blessing.ignar;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;

import java.awt.*;
import java.util.List;
import java.util.Random;

public class InfernoMantle extends Blessings {
    /**
     * Creates a new blessing with the specified parameters.
     *
     * @param neededFavor Amount of favor required to activate
     * @param cooldown    Cooldown period in seconds
     * @param isPassive   Is a passive blessing or not
     * @param canToggle   Whether blessing can be toggled
     * @param textColor   Color for blessing text
     */
    public InfernoMantle(int neededFavor, int cooldown, boolean isPassive, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isPassive, isActive, canToggle, textColor);
    }

    @Override
    public boolean onTick(BlessingsInstance instance, LivingEntity living) {
        Random r = new Random();

        if(instance.isToggled()) {
            if (living.level() instanceof ServerLevel serverLevel) {
                Vec3 position = living.position();
                AABB affectedArea = new AABB(
                        position.x - 3,
                        position.y - 3,
                        position.z - 3,
                        position.x + 3,
                        position.y + 3,
                        position.z + 3
                );
                List<LivingEntity> livingEntityList = serverLevel.getEntitiesOfClass(LivingEntity.class, affectedArea);
                livingEntityList.remove(living);
                for (LivingEntity entity : livingEntityList) {
                    if (entity != living) {
                        if (!entity.isOnFire()) {
                            entity.setSecondsOnFire(2);
                        }
                    }
                }
                if (!livingEntityList.isEmpty()) {
                    for (int i = 0; i < 10; i++) {
                        double angle = serverLevel.getRandom().nextDouble() * 2 * Math.PI;
                        double radius = serverLevel.getRandom().nextDouble() * 4;
                        double x = living.getX() + Math.cos(angle) * radius;
                        double y = living.getY() + 0.02;
                        double z = living.getZ() + Math.sin(angle) * radius;

                        serverLevel.sendParticles(ParticleTypes.FLAME, x, y, z, 1, r.nextDouble(0, 0.5), r.nextDouble(0, 0.5), r.nextDouble(0, 0.5), r.nextDouble(0, 0.5));
                    }
                }
            }
        }


        return super.onTick(instance, living);
    }
}
