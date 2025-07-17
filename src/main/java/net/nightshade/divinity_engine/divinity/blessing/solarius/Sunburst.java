package net.nightshade.divinity_engine.divinity.blessing.solarius;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.level.Level;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.nightshade_core.util.MiscHelper;

import java.awt.*;
import java.util.List;


public class Sunburst extends Blessings {
    public Sunburst(int neededFavor, int cooldown, boolean isPassive, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isPassive, isActive, canToggle, textColor);
    }

    @Override
    public boolean onPressed(BlessingsInstance instance, LivingEntity living) {
        Level level = living.level();
        double range = 5.0D;

        List<LivingEntity> entityList = level.getEntitiesOfClass(LivingEntity.class, living.getBoundingBox().inflate(range));
        entityList.remove(living);
        for (LivingEntity target : entityList) {
            if (target != living) {
                // Apply blindness
                target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, MiscHelper.secondsToTick(5), 0, false, false, true));

                // Set on fire
                target.setSecondsOnFire(5);

                // Calculate and apply knockback
                Vec3 difference = target.position().subtract(living.position());
                double strength = 0.5;
                target.push(
                        difference.x * strength,
                        0.4D,
                        difference.z * strength
                );
            }
        }

        if (!entityList.isEmpty()){
            if (level instanceof ServerLevel serverLevel) {
                for (int i = 0; i < 100; i++) {
                    double angle = level.getRandom().nextDouble() * 2 * Math.PI;
                    double radius = level.getRandom().nextDouble() * range;
                    double x = living.getX() + Math.cos(angle) * radius;
                    double y = living.getY() + level.getRandom().nextDouble() * 1.5;
                    double z = living.getZ() + Math.sin(angle) * radius;

                    serverLevel.sendParticles(ParticleTypes.FLAME, x, y, z, 1, 0, 0, 0, 0);
                }
            }
        }

        return !entityList.isEmpty();
    }
}
