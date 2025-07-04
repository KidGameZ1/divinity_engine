package net.nightshade.divinity_engine.divinity.blessing.nerai;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;

import java.awt.*;

public class Wavebound extends Blessings {
    public Wavebound(int neededFavor, int cooldown, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isActive, canToggle, textColor);
    }

    @Override
    public <T extends BlessingsInstance> T createDefaultInstance() {
        T instance = super.createDefaultInstance();
        if (!instance.getOrCreateTag().contains("dash_speed"))
            instance.getOrCreateTag().putDouble("dash_speed", 2.5d);
        return instance;
    }

    @Override
    public boolean onPressed(BlessingsInstance instance, LivingEntity living) {
        if (living.isInWater()) {
            Vec3 lookVec = living.getLookAngle();
            double dashSpeed = instance.getOrCreateTag().getDouble("dash_speed");
            living.setDeltaMovement(lookVec.x * dashSpeed, lookVec.y * dashSpeed, lookVec.z * dashSpeed);

            Level level = living.level();
            for (int i = 0; i < 20; i++) {
                double offsetX = living.getX() + (level.random.nextDouble() - 0.5D) * 0.5D;
                double offsetY = living.getY() + (level.random.nextDouble() - 0.5D) * 0.5D;
                double offsetZ = living.getZ() + (level.random.nextDouble() - 0.5D) * 0.5D;
                level.addParticle(ParticleTypes.BUBBLE, offsetX, offsetY, offsetZ, 0, 0, 0);
            }

            return true;
        }
        return false;
    }
}
