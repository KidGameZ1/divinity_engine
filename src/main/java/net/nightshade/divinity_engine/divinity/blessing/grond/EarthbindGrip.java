package net.nightshade.divinity_engine.divinity.blessing.grond;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;

import java.awt.*;
import java.util.List;

public class EarthbindGrip extends Blessings {

    /**
     * Creates a new blessing with the specified parameters.
     *
     * @param neededFavor Amount of favor required to activate
     * @param cooldown    Cooldown period in seconds
     * @param isPassive   Is a passive blessing or not
     * @param canToggle   Whether blessing can be toggled
     * @param textColor   Color for blessing text
     */
    public EarthbindGrip(int neededFavor, int cooldown, boolean isPassive, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isPassive, isActive, canToggle, textColor);
    }

    @Override
    public boolean onFall(BlessingsInstance instance, LivingFallEvent event) {
        LivingEntity living = event.getEntity();
        System.out.println(living.getHealth());
        if (living.level() instanceof ServerLevel serverLevel) {
            Vec3 position = living.position();
            AABB affectedArea = new AABB(
                    position.x - 5,
                    position.y - 5,
                    position.z - 5,
                    position.x + 5,
                    position.y + 5,
                    position.z + 5
            );
            List<LivingEntity> livingEntityList = serverLevel.getEntitiesOfClass(LivingEntity.class, affectedArea);
            livingEntityList.remove(living);
            for (LivingEntity entity : livingEntityList) {
                if (entity != living) {
                    entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0, false, false, true));
                    entity.setDeltaMovement(entity.getDeltaMovement().x, 0.25, entity.getDeltaMovement().z);
                }
            }
            if (!livingEntityList.isEmpty()) {
                for (int i = 0; i < 100; i++) {
                    double angle = serverLevel.getRandom().nextDouble() * 2 * Math.PI;
                    double radius = serverLevel.getRandom().nextDouble() * 6;
                    double x = living.getX() + Math.cos(angle) * radius;
                    double y = living.getY()+0.02;
                    double z = living.getZ() + Math.sin(angle) * radius;

                    serverLevel.sendParticles(ParticleTypes.CLOUD, x, y, z, 1, 0, 0, 0, 0);
                }
                return true;
            }
        }

        return super.onFall(instance, event);
    }
}
