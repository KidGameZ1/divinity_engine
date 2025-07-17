package net.nightshade.divinity_engine.divinity.blessing.nashara;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;

import java.awt.*;
import java.util.Random;

public class Shadowstep extends Blessings {
    public Shadowstep(int neededFavor, int cooldown, boolean isPassive, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isPassive, isActive, canToggle, textColor);
    }

    @Override
    public boolean onTakenDamage(BlessingsInstance instance, LivingDamageEvent event) {
        LivingEntity entity = event.getEntity();
        Random random = new Random();

        // Calculate random position
        double radius = 5.0;
        double angle = random.nextDouble() * 2 * Math.PI;
        double x = entity.getX() + radius * Math.cos(angle);
        double z = entity.getZ() + radius * Math.sin(angle);

        // Teleport
        entity.teleportTo(x, entity.getY(), z);

        // Add effects
        entity.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 60, 0));
        if (event.getSource().getEntity() instanceof LivingEntity attacker) {
            attacker.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 2));
        }

        // Particles
        entity.level().addParticle(ParticleTypes.SMOKE, entity.getX(), entity.getY(), entity.getZ(), 0, 0, 0);

        return true;
    }
}
