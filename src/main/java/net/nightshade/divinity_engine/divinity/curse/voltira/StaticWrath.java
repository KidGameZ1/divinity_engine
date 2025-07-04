package net.nightshade.divinity_engine.divinity.curse.voltira;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.nightshade.divinity_engine.divinity.curse.Curse;
import net.nightshade.divinity_engine.divinity.curse.CurseInstance;

import java.util.Random;

public class StaticWrath extends Curse {


    @Override
    public void onTick(CurseInstance instance, LivingEntity living) {
        Random random = new Random();

        if (random.nextFloat() < 0.1f){
            if (living.level() instanceof ServerLevel serverLevel) {
                if (living.level().isThundering() || living.level().isRaining()){
                    BlockPos pos = living.blockPosition();
                    LightningBolt lightning = new LightningBolt(EntityType.LIGHTNING_BOLT, serverLevel);
                    if (random.nextFloat() < 0.05f){
                        lightning.setPos(pos.getX(), pos.getY(), pos.getZ());
                    }else {
                        lightning.setPos(pos.getX()+random.nextFloat(2,8), pos.getY(), pos.getZ()+random.nextFloat(2,8));
                    }
                    serverLevel.addFreshEntity(lightning);
                }
            }
        }

        super.onTick(instance, living);
    }
}
