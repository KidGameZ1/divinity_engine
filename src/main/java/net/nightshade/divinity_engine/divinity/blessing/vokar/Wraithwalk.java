package net.nightshade.divinity_engine.divinity.blessing.vokar;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;

import java.awt.*;

public class Wraithwalk extends Blessings {
    public Wraithwalk(int neededFavor, int cooldown, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isActive, canToggle, textColor);
    }

    @Override
    public boolean onTakenDamage(BlessingsInstance instance, LivingDamageEvent event) {
        Level level = event.getEntity().level();
        if (event.getEntity().isAlive()) {
            event.getEntity().addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 100, 0, false, false, true));
            if (level instanceof ServerLevel serverLevel) {
                for (int i = 0; i < 100; i++) {
                    double angle = level.getRandom().nextDouble() * 2 * Math.PI;
                    double radius = level.getRandom().nextDouble() * 5.0d;
                    double x = event.getEntity().getX() + Math.cos(angle) * radius;
                    double y = event.getEntity().getY() + level.getRandom().nextDouble() * 1.5;
                    double z = event.getEntity().getZ() + Math.sin(angle) * radius;

                    serverLevel.sendParticles(ParticleTypes.SOUL, x, y, z, 1, 0, 0, 0, 0);
                }
            }
            return true;
        }
        return super.onTakenDamage(instance, event);
    }
}
