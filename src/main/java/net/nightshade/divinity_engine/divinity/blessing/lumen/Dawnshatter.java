package net.nightshade.divinity_engine.divinity.blessing.lumen;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;

import java.awt.*;
import java.util.List;
import java.util.Random;

public class Dawnshatter extends Blessings {
    /**
     * Creates a new blessing with the specified parameters.
     *
     * @param neededFavor Amount of favor required to activate
     * @param cooldown    Cooldown period in seconds
     * @param isPassive   Is a passive blessing or not
     * @param canToggle   Whether blessing can be toggled
     * @param textColor   Color for blessing text
     */
    public Dawnshatter(int neededFavor, int cooldown, boolean isPassive, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isPassive, isActive, canToggle, textColor);
    }

    @Override
    public boolean onPressed(BlessingsInstance instance, LivingEntity living) {
        if (!(living.level() instanceof ServerLevel serverLevel)) return false;

        Vec3 forward = living.getLookAngle().normalize();
        double coneRadius = 6.0;
        double coneAngle = Math.toRadians(60); // 60-degree cone

        AABB area = living.getBoundingBox().inflate(coneRadius);
        List<LivingEntity> nearby = serverLevel.getEntitiesOfClass(LivingEntity.class, area, e -> e != living && e.isAlive());

        for (LivingEntity target : nearby) {
            Vec3 toTarget = target.position().subtract(living.position()).normalize();
            double angle = forward.dot(toTarget);

            if (angle > Math.cos(coneAngle / 2)) {
                if (EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(target)) {
                    if (target.getMobType() == MobType.UNDEAD || target instanceof Monster) {
                        // Damage dark-aligned enemies
                        target.hurt(living.damageSources().magic(), 4.0f);
                        target.setSecondsOnFire(3);
                    }
                }
            }
        }

        Vec3 origin = living.position().add(0, 1.5, 0); // Eye height
        Vec3 look = living.getLookAngle().normalize();

        for (int i = 0; i < 150; i++) {
            double distance = serverLevel.random.nextDouble() * 6.0; // Cone length
            double spreadAngle = Math.toRadians(30); // Half-angle of cone

            // Random angle offset within the cone
            double theta = serverLevel.random.nextDouble() * 2 * Math.PI;
            double phi = serverLevel.random.nextDouble() * spreadAngle;

            // Convert spherical to Cartesian
            double x = Math.sin(phi) * Math.cos(theta);
            double y = Math.sin(phi) * Math.sin(theta);
            double z = Math.cos(phi);

            // Rotate to match look direction
            Vec3 direction = new Vec3(x, y, z);
            Vec3 rotated = rotateVectorToMatch(direction, look).normalize().scale(distance);

            Vec3 finalPos = origin.add(rotated);
            serverLevel.sendParticles(ParticleTypes.END_ROD, finalPos.x, finalPos.y, finalPos.z, 1, 0, 0, 0, 0);
        }


        return true;
    }
    public static Vec3 rotateVectorToMatch(Vec3 original, Vec3 targetDirection) {
        Vec3 axis = new Vec3(0, 0, 1).cross(targetDirection);
        double angle = Math.acos(targetDirection.normalize().dot(new Vec3(0, 0, 1)));
        if (axis.lengthSqr() == 0) return original;

        // Rodrigues' rotation formula
        axis = axis.normalize();
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double x = original.x * (cos + axis.x * axis.x * (1 - cos))
                + original.y * (axis.x * axis.y * (1 - cos) - axis.z * sin)
                + original.z * (axis.x * axis.z * (1 - cos) + axis.y * sin);

        double y = original.x * (axis.y * axis.x * (1 - cos) + axis.z * sin)
                + original.y * (cos + axis.y * axis.y * (1 - cos))
                + original.z * (axis.y * axis.z * (1 - cos) - axis.x * sin);

        double z = original.x * (axis.z * axis.x * (1 - cos) - axis.y * sin)
                + original.y * (axis.z * axis.y * (1 - cos) + axis.x * sin)
                + original.z * (cos + axis.z * axis.z * (1 - cos));

        return new Vec3(x, y, z);
    }


}
