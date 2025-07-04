package net.nightshade.divinity_engine.divinity.blessing.nythea;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;

import java.awt.*;

public class PhaseBlink extends Blessings {
    public PhaseBlink(int neededFavor, int cooldown, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isActive, canToggle, textColor);
    }
    
    private void makeInvulnerable(LivingEntity living) {
        living.setInvulnerable(true);
    }

    private void makeVulnerable(LivingEntity living) {
        living.setInvulnerable(false);
    }

    @Override
    public boolean onPressed(BlessingsInstance instance, LivingEntity living) {
        if (!living.level().isDay()) {
            makeInvulnerable(living);
            double distance = 4.0;  // Distance for each dash
            double dx = -Math.sin(Math.toRadians(living.getYRot())) * Math.cos(Math.toRadians(living.getXRot()));
            double dz = Math.cos(Math.toRadians(living.getYRot())) * Math.cos(Math.toRadians(living.getXRot()));
            double dy = -Math.sin(Math.toRadians(living.getXRot()));

            // Check if there's a block in front of player
            double checkX = living.getX() + dx;
            double checkY = living.getY() + dy;
            double checkZ = living.getZ() + dz;

            // Check blocks at eye level and feet level
            if (!living.level().getBlockState(new net.minecraft.core.BlockPos(
                    (int) checkX,
                    (int) checkY,
                    (int) checkZ)).isAir() ||
                    !living.level().getBlockState(new net.minecraft.core.BlockPos(
                            (int) checkX,
                            (int) (checkY + 1),
                            (int) checkZ)).isAir()) {
                makeVulnerable(living);
                return false;
            }

            // First dash location
            double firstX = living.getX() + dx * distance;
            double firstY = living.getY() + dy * distance;
            double firstZ = living.getZ() + dz * distance;

            // Second dash location (zigzag by rotating 45 degrees)
            double angle = Math.PI / 4; // 45 degrees
            double dx2 = dx * Math.cos(angle) - dz * Math.sin(angle);
            double dz2 = dx * Math.sin(angle) + dz * Math.cos(angle);
            double secondX = firstX + dx2 * distance;
            double secondY = firstY + dy * distance;
            double secondZ = firstZ + dz2 * distance;

            // Third dash location (zigzag back)
            double dx3 = dx * Math.cos(-angle) - dz * Math.sin(-angle);
            double dz3 = dx * Math.sin(-angle) + dz * Math.cos(-angle);
            double thirdX = secondX + dx3 * distance;
            double thirdY = secondY + dy * distance;
            double thirdZ = secondZ + dz3 * distance;

            // Fourth dash location (forward)
            double fourthX = thirdX + dx * distance;
            double fourthY = thirdY + dy * distance;
            double fourthZ = thirdZ + dz * distance;

            // Check path for all four dashes
            for (int i = 1; i <= distance * 4; i++) {
                double checkPathX, checkPathY, checkPathZ;
                if (i <= distance) {
                    checkPathX = living.getX() + dx * i;
                    checkPathY = living.getY() + dy * i;
                    checkPathZ = living.getZ() + dz * i;
                } else if (i <= distance * 2) {
                    double j = i - distance;
                    checkPathX = firstX + dx2 * j;
                    checkPathY = firstY + dy * j;
                    checkPathZ = firstZ + dz2 * j;
                } else if (i <= distance * 3) {
                    double j = i - (distance * 2);
                    checkPathX = secondX + dx3 * j;
                    checkPathY = secondY + dy * j;
                    checkPathZ = secondZ + dz3 * j;
                } else {
                    double j = i - (distance * 3);
                    checkPathX = thirdX + dx * j;
                    checkPathY = thirdY + dy * j;
                    checkPathZ = thirdZ + dz * j;
                }

                if (!living.level().getBlockState(new net.minecraft.core.BlockPos(
                        (int) checkPathX,
                        (int) checkPathY,
                        (int) checkPathZ)).isAir()) {
                    makeVulnerable(living);
                    return false;
                }
            }

            // First dash
            if (living.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.PORTAL, firstX, firstY + 1, firstZ, 25, 0.5, 0.5, 0.5, 0.1);
            }
            living.teleportTo(firstX, firstY, firstZ);
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Second dash  
            if (living.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.PORTAL, secondX, secondY + 1, secondZ, 25, 0.5, 0.5, 0.5, 0.1);
            }
            living.teleportTo(secondX, secondY, secondZ);
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Third dash
            if (living.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.PORTAL, thirdX, thirdY + 1, thirdZ, 25, 0.5, 0.5, 0.5, 0.1);
            }
            living.teleportTo(thirdX, thirdY, thirdZ);
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Fourth dash
            if (living.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.PORTAL, fourthX, fourthY + 1, fourthZ, 25, 0.5, 0.5, 0.5, 0.1);
            }
            living.teleportTo(fourthX, fourthY, fourthZ);

            makeVulnerable(living);
            return true;
        }
        return false;
    }
}
