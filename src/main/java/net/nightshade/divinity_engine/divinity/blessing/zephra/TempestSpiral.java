package net.nightshade.divinity_engine.divinity.blessing.zephra;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;

import java.awt.*;
import java.util.List;

public class TempestSpiral extends Blessings {
    /**
     * Creates a new blessing with the specified parameters.
     *
     * @param neededFavor Amount of favor required to activate
     * @param cooldown    Cooldown period in seconds
     * @param isPassive   Is a passive blessing or not
     * @param canToggle   Whether blessing can be toggled
     * @param textColor   Color for blessing text
     */
    public TempestSpiral(int neededFavor, int cooldown, boolean isPassive, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isPassive, isActive, canToggle, textColor);
    }

    @Override
    public boolean onPressed(BlessingsInstance instance, LivingEntity living) {
        if (!(living.level() instanceof ServerLevel serverLevel)) return false;

        double radius = 4.0;
        AABB vortexArea = living.getBoundingBox().inflate(radius);

        List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, vortexArea, e -> e != living && e.isAlive());

        for (LivingEntity target : targets) {
            Vec3 direction = living.position().subtract(target.position()).normalize();
            Vec3 pull = direction.scale(0.4);
            target.setDeltaMovement(pull.x, 0.1, pull.z);
            target.hurt(living.damageSources().magic(), 2.0f);
            target.hurtMarked = true;
        }

        // Wind spiral particles
        for (int i = 0; i < 100; i++) {
            double angle = Math.random() * 2 * Math.PI;
            double spiral = angle + serverLevel.getGameTime() * 0.2;
            double r = Math.random() * radius;
            double x = living.getX() + Math.cos(spiral) * r;
            double y = living.getY() + Math.random() * 1.5;
            double z = living.getZ() + Math.sin(spiral) * r;
            serverLevel.sendParticles(ParticleTypes.CLOUD, x, y, z, 1, 0, 0.05, 0, 0.01);
        }

        return true;
    }

}
