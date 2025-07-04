package net.nightshade.divinity_engine.divinity.blessing.terra;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.nightshade_core.util.MiscHelper;

import java.awt.*;
import java.util.Random;

public class VerdantTouch extends Blessings {
    private static final Random random = new Random();

    public VerdantTouch(int neededFavor, int cooldown, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isActive, canToggle, textColor);
    }

    @Override
    public <T extends BlessingsInstance> T createDefaultInstance() {
        T instance = super.createDefaultInstance();
        if (!instance.getOrCreateTag().contains("growth_radius"))
            instance.getOrCreateTag().putInt("growth_radius", 6);
        if (!instance.getOrCreateTag().contains("tick_delay_at_pos"))
            instance.getOrCreateTag().put("tick_delay_at_pos", new ListTag());
        return instance;
    }

    @Override
    public boolean onTick(BlessingsInstance instance, LivingEntity living) {
        if (!(living.level() instanceof ServerLevel level)) {
            return false;
        }

        BlockPos playerPos = living.blockPosition();
        int growthRadius = instance.getOrCreateTag().getInt("growth_radius");
        ListTag tickDelayAtPos = instance.getOrCreateTag().getList("tick_delay_at_pos", 10);

        for (BlockPos pos : BlockPos.betweenClosed(
                playerPos.offset(-growthRadius, -2, -growthRadius),
                playerPos.offset(growthRadius, 2, growthRadius))) {

            int posIndex = tickDelayAtPos.size();
            for (int i = 0; i < tickDelayAtPos.size(); i++) {
                if (tickDelayAtPos.getCompound(i).getString("pos").equals(pos.toString())) {
                    posIndex = i;
                    break;
                }
            }

            if (posIndex < tickDelayAtPos.size()) {
                int tickDelay = tickDelayAtPos.getCompound(posIndex).getInt("delay");
                if (tickDelay > 0) {
                    tickDelayAtPos.getCompound(posIndex).putInt("delay", tickDelay - 1);
                    continue;
                }
            }

            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof BonemealableBlock growable && random.nextInt(20) == 0 && level.getBlockState(pos.below()).is(Blocks.FARMLAND)) {
                BlockPos _bp = pos;
                if (growable.isValidBonemealTarget(level, _bp, state, level.isClientSide())) {
                    if (level instanceof ServerLevel) {
                        if (growable.isBonemealSuccess(level, level.random, _bp, state)) {
                            growable.performBonemeal((ServerLevel) level, level.random, _bp, state);
                        }
                    }
                }
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        1, 0.1, 0.1, 0.1, 0);

                CompoundTag delayTag = new net.minecraft.nbt.CompoundTag();
                delayTag.putString("pos", pos.toString());
                delayTag.putInt("delay", MiscHelper.secondsToTick(10));
                if (posIndex < tickDelayAtPos.size()) {
                    tickDelayAtPos.set(posIndex, delayTag);
                } else {
                    tickDelayAtPos.add(delayTag);
                }
            }
        }
        return false;
    }

}