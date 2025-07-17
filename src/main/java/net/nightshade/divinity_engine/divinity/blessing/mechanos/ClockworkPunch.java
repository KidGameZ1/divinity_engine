package net.nightshade.divinity_engine.divinity.blessing.mechanos;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;

import java.awt.*;
import java.util.Random;

public class ClockworkPunch extends Blessings {

    public ClockworkPunch(int neededFavor, int cooldown, boolean isPassive, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isPassive, isActive, canToggle, textColor);
    }

    @Override
    public <T extends BlessingsInstance> T createDefaultInstance() {
        T instance = super.createDefaultInstance();
        if (!instance.getOrCreateTag().contains("knockback_chance"))
            instance.getOrCreateTag().putFloat("knockback_chance", 0.3f);
        if (!instance.getOrCreateTag().contains("knockback_strength"))
            instance.getOrCreateTag().putDouble("knockback_strength", 2.0);
        return instance;
    }

    @Override
    public boolean onDamageEntity(BlessingsInstance instance, LivingEntity player, LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        if (target instanceof LivingEntity && new Random().nextFloat() < instance.getOrCreateTag().getFloat("knockback_chance")) {
            Vec3 knockbackDir = target.position().subtract(player.position()).normalize();
            target.setDeltaMovement(knockbackDir.x * instance.getOrCreateTag().getDouble("knockback_strength"),
                    0.5,
                    knockbackDir.z * instance.getOrCreateTag().getDouble("knockback_strength"));

            if (player.level() instanceof ServerLevel serverLevel) {
                spawnParticles(serverLevel, target);
                playPunchSound(serverLevel, target);
            }

            return true;
        }
        return super.onDamageEntity(instance, player, event);
    }

    private void spawnParticles(ServerLevel level, Entity target) {
        level.sendParticles(ParticleTypes.CRIT,
                target.getX(), target.getY() + 1.0, target.getZ(),
                15, 0.3, 0.3, 0.3, 0.1);
    }

    private void playPunchSound(ServerLevel level, Entity target) {
        level.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.PISTON_EXTEND, SoundSource.PLAYERS,
                1.0F, 1.0F);
    }

}