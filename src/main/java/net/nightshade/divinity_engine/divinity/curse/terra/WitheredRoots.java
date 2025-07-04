package net.nightshade.divinity_engine.divinity.curse.terra;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.nightshade.divinity_engine.divinity.curse.Curse;
import net.nightshade.divinity_engine.divinity.curse.CurseInstance;
import net.nightshade.nightshade_core.util.MiscHelper;

import java.util.Random;

public class WitheredRoots extends Curse {


    @Override
    public void onTick(CurseInstance instance, LivingEntity living) {
        Random random = new Random();
        for (BlockPos pos : BlockPos.betweenClosed(
                living.blockPosition().offset(-6, -2, -6),
                living.blockPosition().offset(6, 2, 6))) {



            BlockState state = living.level().getBlockState(pos);
            if (state.getBlock() instanceof BonemealableBlock growable && random.nextInt(20) == 0 && living.level().getBlockState(pos.below()).is(Blocks.FARMLAND)) {
                if (random.nextFloat() < 0.1f){
                    living.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0, false, false, true));
                }
            }
        }

        super.onTick(instance, living);
    }
}
