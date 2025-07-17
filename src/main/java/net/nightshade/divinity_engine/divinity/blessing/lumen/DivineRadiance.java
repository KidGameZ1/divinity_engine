package net.nightshade.divinity_engine.divinity.blessing.lumen;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;

import java.awt.*;

public class DivineRadiance extends Blessings {
    public DivineRadiance(int neededFavor, int cooldown, boolean isPassive, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isPassive, isActive, canToggle, textColor);
    }

    @Override
    public boolean onPressed(BlessingsInstance instance, LivingEntity living) {
        living.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 0));

        living.level().getEntitiesOfClass(LivingEntity.class, living.getBoundingBox().inflate(6.0D),
                        entity -> entity != living && entity.isAlive())
                .forEach(target -> target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0)));

        if (living.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    living.getX(), living.getY() + 1, living.getZ(),
                    50, 3.0, 0.5, 3.0, 0.1);

            living.level().playSound(null, living.getX(), living.getY(), living.getZ(),
                    SoundEvents.NOTE_BLOCK_CHIME.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }

        return true;
    }
}