package net.nightshade.divinity_engine.divinity.blessing.nashara;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals.Cue;

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
        Vec3 origin = entity.position();
        entity.teleportTo(x, entity.getY(), z);
        // Departure only: a path or arrival burst would disclose the invisible escape location.
        if (!entity.position().equals(origin) && !entity.isInvisible()) {
            BlessingVisuals.at(instance, entity, origin.add(0, 0.15, 0), Cue.TELEPORT, 0.35);
        }

        // Add effects
        entity.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 60, 0));
        if (event.getSource().getEntity() instanceof LivingEntity attacker) {
            boolean applied = attacker.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 2));
            if (applied) {
                BlessingVisuals.at(instance, entity, attacker.getBoundingBox().getCenter(), Cue.IMPACT, 0.8);
            }
        }

        return true;
    }
}
