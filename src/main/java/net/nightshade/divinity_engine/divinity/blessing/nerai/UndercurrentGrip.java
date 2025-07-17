package net.nightshade.divinity_engine.divinity.blessing.nerai;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;

import java.awt.*;
import java.util.List;

public class UndercurrentGrip extends Blessings {
    /**
     * Creates a new blessing with the specified parameters.
     *
     * @param neededFavor Amount of favor required to activate
     * @param cooldown    Cooldown period in seconds
     * @param isPassive   Is a passive blessing or not
     * @param canToggle   Whether blessing can be toggled
     * @param textColor   Color for blessing text
     */
    public UndercurrentGrip(int neededFavor, int cooldown, boolean isPassive, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isPassive, isActive, canToggle, textColor);
    }

    @Override
    public boolean onPressed(BlessingsInstance instance, LivingEntity living) {
        Level level = living.level();

        // Only works in water
        if (!living.isInWaterOrBubble()) return false;

        if (!(level instanceof ServerLevel serverLevel)) return false;

        double radius = 6.0;
        AABB pullZone = living.getBoundingBox().inflate(radius);
        List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, pullZone).stream()
                .filter(e -> e != living && e.isAlive() && !(e instanceof Player && ((Player) e).isCreative()))
                .toList();

        for (LivingEntity target : targets) {
            // Vector from target to player
            Vec3 direction = living.position().subtract(target.position()).normalize();
            Vec3 velocity = direction.scale(0.5); // Pull strength

            // Apply pull
            target.push(velocity.x, velocity.y, velocity.z);

            // Apply Slowness
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
        }

        // Particle feedback
        for (int i = 0; i < 40; i++) {
            double angle = Math.random() * 2 * Math.PI;
            double distance = Math.random() * radius;
            double x = living.getX() + Math.cos(angle) * distance;
            double y = living.getY() + 0.5;
            double z = living.getZ() + Math.sin(angle) * distance;
            serverLevel.sendParticles(ParticleTypes.SPLASH, x, y, z, 1, 0, 0.1, 0, 0.01);
        }

        // Optional: Add sound effect here
        // level.playSound(null, living.blockPosition(), SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 1.0F, 1.0F);

        return true;
    }

}
