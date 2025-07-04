package net.nightshade.divinity_engine.divinity.blessing.voltira;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;

import java.awt.*;

public class TempestWrath extends Blessings {
    public TempestWrath(int neededFavor, int cooldown, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isActive, canToggle, textColor);
    }

    @Override
    public <T extends BlessingsInstance> T createDefaultInstance() {
        T instance = super.createDefaultInstance();
        if (!instance.getOrCreateTag().contains("effect_radius"))
            instance.getOrCreateTag().putDouble("effect_radius", 5.0);
        if (!instance.getOrCreateTag().contains("base_damage"))
            instance.getOrCreateTag().putFloat("base_damage", 4.0f);
        if (!instance.getOrCreateTag().contains("storm_multiplier"))
            instance.getOrCreateTag().putFloat("storm_multiplier", 1.5f);
        if (!instance.getOrCreateTag().contains("knockback_strength"))
            instance.getOrCreateTag().putDouble("knockback_strength", 1.5f);
        return instance;
    }

    @Override
    public boolean onKillEntity(BlessingsInstance instance, LivingEntity player, LivingDeathEvent event) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return false;
        }

        Vec3 position = player.position();
        boolean isThunderstorm = serverLevel.isThundering();
        float damage = calculateDamage(instance, isThunderstorm);

        // Play thunder sound and create particles
        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER,
                1.0F, 1.0F);

        for (int i = 0; i < 20; i++) {
            double offsetX = (Math.random() - 0.5) * instance.getOrCreateTag().getDouble("effect_radius");
            double offsetZ = (Math.random() - 0.5) * instance.getOrCreateTag().getDouble("effect_radius");
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    position.x + offsetX, position.y, position.z + offsetZ,
                    1, 0, 0, 0, 0);
        }

        // Damage and knockback nearby entities
        AABB affectedArea = new AABB(
                position.x - instance.getOrCreateTag().getDouble("effect_radius"),
                position.y - instance.getOrCreateTag().getDouble("effect_radius"),
                position.z - instance.getOrCreateTag().getDouble("effect_radius"),
                position.x + instance.getOrCreateTag().getDouble("effect_radius"),
                position.y + instance.getOrCreateTag().getDouble("effect_radius"),
                position.z + instance.getOrCreateTag().getDouble("effect_radius")
        );

        for (LivingEntity entity : serverLevel.getEntitiesOfClass(LivingEntity.class, affectedArea)) {
            if (entity != player) {
                entity.hurt(player.damageSources().lightningBolt(), damage);
                applyKnockback(instance, entity, position);
            }
        }

        return true;
    }

    private float calculateDamage(BlessingsInstance instance, boolean isThunderstorm) {
        return isThunderstorm ? instance.getOrCreateTag().getFloat("base_damage") * instance.getOrCreateTag().getFloat("storm_multiplier") :
                instance.getOrCreateTag().getFloat("base_damage");
    }

    private void applyKnockback(BlessingsInstance instance, LivingEntity target, Vec3 source) {
        Vec3 knockbackDir = target.position().subtract(source).normalize();
            target.setDeltaMovement(knockbackDir.x * instance.getOrCreateTag().getDouble("knockback_strength"),
                    0.5,
                    knockbackDir.z * instance.getOrCreateTag().getDouble("knockback_strength"));
    }
}

