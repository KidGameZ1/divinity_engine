package net.nightshade.divinity_engine.divinity.blessing.cryonis;

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
import java.util.Random;

public class PermafrostSurge extends Blessings {
    /**
     * Creates a new blessing with the specified parameters.
     *
     * @param neededFavor Amount of favor required to activate
     * @param cooldown    Cooldown period in seconds
     * @param isPassive   Is a passive blessing or not
     * @param canToggle   Whether blessing can be toggled
     * @param textColor   Color for blessing text
     */
    public PermafrostSurge(int neededFavor, int cooldown, boolean isPassive, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isPassive, isActive, canToggle, textColor);
    }

    @Override
    public boolean onPressed(BlessingsInstance instance, LivingEntity living) {
        Random r = new Random();
        if (living.level() instanceof ServerLevel serverLevel) {
            Vec3 position = living.position();
            AABB affectedArea = new AABB(
                    position.x - 6,
                    position.y - 6,
                    position.z - 6,
                    position.x + 6,
                    position.y + 6,
                    position.z + 6
            );
            List<LivingEntity> livingEntityList = serverLevel.getEntitiesOfClass(LivingEntity.class, affectedArea);
            livingEntityList.remove(living);
            for (LivingEntity entity : livingEntityList) {
                if (entity != living) {
                    entity.setTicksFrozen(600);
                    entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 0, false, false, true));
                }
            }
            if (!livingEntityList.isEmpty()) {
                for (int i = 0; i < 100; i++) {
                    double angle = serverLevel.getRandom().nextDouble() * 2 * Math.PI;
                    double radius = serverLevel.getRandom().nextDouble() * 7;
                    double x = living.getX() + Math.cos(angle) * radius;
                    double y = living.getY()+0.02;
                    double z = living.getZ() + Math.sin(angle) * radius;

                    serverLevel.sendParticles(ParticleTypes.SNOWFLAKE, x, y, z, 1, 0, r.nextDouble(0,0.5), 0, r.nextDouble(0,0.2));
                    serverLevel.sendParticles(ParticleTypes.ITEM_SNOWBALL, x, y, z, 1, 0, r.nextDouble(0,0.5), 0, r.nextDouble(0,0.2));
                }
            }
        }
        return super.onPressed(instance, living);
    }
}
