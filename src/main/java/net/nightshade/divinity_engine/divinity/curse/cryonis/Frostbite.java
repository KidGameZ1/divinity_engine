package net.nightshade.divinity_engine.divinity.curse.cryonis;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.nightshade.divinity_engine.divinity.curse.Curse;
import net.nightshade.divinity_engine.divinity.curse.CurseInstance;

public class Frostbite extends Curse {
    @Override
    public void onTick(CurseInstance instance, LivingEntity living) {

        if (living.level().getBlockState(living.blockPosition().below()).is(Blocks.SNOW_BLOCK) || living.level().getBlockState(living.blockPosition().below()).is(Blocks.SNOW)
                || living.level().getBlockState(living.blockPosition().below()).is(Blocks.ICE) || living.level().getBlockState(living.blockPosition().below()).is(Blocks.PACKED_ICE)
                || living.level().getBlockState(living.blockPosition().below()).is(Blocks.BLUE_ICE)){
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 0, false, false, true));
        }else if (living.isInWaterRainOrBubble()){
            living.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 0, false, false, true));
        }

        super.onTick(instance, living);
    }
}
