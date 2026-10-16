package net.nightshade.divinity_engine.aspects.armor;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.nightshade.vanguards_kit.core.battle_aspects.BattleAspect;

import java.util.List;

public class MomentumAspect extends BattleAspect {
    public MomentumAspect(String id) {
        super(id, true);
    }

    @Override
    public void onPlayerFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        float fallDistance = event.getDistance();

        if (fallDistance < 2.0f) return;
        event.setDamageMultiplier(0.5f);

        double aoeRadius = Math.min(fallDistance * 0.5, 5.0);
        float aoeDamage = fallDistance * 0.5f;

        AABB bounds = player.getBoundingBox().inflate(aoeRadius);
        List<LivingEntity> entities = player.level().getEntitiesOfClass(LivingEntity.class, bounds);

        for (LivingEntity target : entities) {
            if (target != player) {
                target.hurt(player.damageSources().fall(), aoeDamage);
            }
        }

        player.level().addParticle(ParticleTypes.EXPLOSION,
                player.getX(), player.getY(), player.getZ(),
                0.0D, 0.0D, 0.0D);

        for (int i = 0; i < 20; i++) {
            double x = player.getX() + (Math.random() - 0.5) * aoeRadius * 2;
            double y = player.getY() + 0.1;
            double z = player.getZ() + (Math.random() - 0.5) * aoeRadius * 2;
            player.level().addParticle(ParticleTypes.SMOKE,
                    x, y, z,
                    0.0D, 0.1D, 0.0D);
        }

        super.onPlayerFall(event);
    }
}
