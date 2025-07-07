package net.nightshade.divinity_engine.divinity.blessing.zephra;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;

import java.awt.*;

public class CycloneLeap extends Blessings {
    public CycloneLeap(int neededFavor, int cooldown, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isActive, canToggle, textColor);
    }

    @Override
    public boolean onPressed(BlessingsInstance instance, LivingEntity living) {
        Level level = living.level();
        double upwardVelocity = 1.0;
        living.setDeltaMovement(living.getDeltaMovement().x, upwardVelocity, living.getDeltaMovement().z);
        living.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 80, 0));
        if (level instanceof ServerLevel serverLevel) {
            for (int i = 0; i < 40; i++) {
                double angle = level.random.nextDouble() * 2 * Math.PI;
                double radius = level.random.nextDouble() * 1.5 + 0.5;
                double x = living.getX() + Math.cos(angle) * radius;
                double y = living.getY() + 0.2 + level.random.nextDouble() * 1.0;
                double z = living.getZ() + Math.sin(angle) * radius;

                serverLevel.sendParticles(ParticleTypes.CLOUD, x, y, z, 1, 0, 0.01, 0, 0);
                serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, x, y, z, 1, 0, 0.01, 0, 0);
            }
        }
        return true;
    }
}
