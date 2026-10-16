package net.nightshade.divinity_engine.aspects.weapon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.CriticalHitEvent;
import net.nightshade.vanguards_kit.core.battle_aspects.BattleAspect;

public class ThunderclashAspect extends BattleAspect {
    public ThunderclashAspect(String id) {
        super(id, false);
    }

    @Override
    public void onDamageEntity(LivingEntity player, LivingHurtEvent event) {

        super.onDamageEntity(player, event);
    }

    @Override
    public void onCriticalHit(CriticalHitEvent event) {
        if (event.getTarget() instanceof LivingEntity living){
            if (living.level() instanceof ServerLevel serverLevel) {
                BlockPos pos = living.blockPosition();
                LightningBolt lightning = new LightningBolt(EntityType.LIGHTNING_BOLT, serverLevel);
                lightning.setPos(pos.getX(), pos.getY(), pos.getZ());
                lightning.setVisualOnly(true);
                serverLevel.addFreshEntity(lightning);

                serverLevel.playSound(null, pos, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 1.0F, 1.0F);
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX(), pos.getY(), pos.getZ(), 50, 0.5, 0.5, 0.5, 0.1);
            }
            event.setDamageModifier(event.getOldDamageModifier() + 2.5f);
        }

        super.onCriticalHit(event);
    }
}
