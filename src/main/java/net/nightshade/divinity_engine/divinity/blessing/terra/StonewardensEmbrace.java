package net.nightshade.divinity_engine.divinity.blessing.terra;

import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;

import java.awt.*;

public class StonewardensEmbrace extends Blessings {

    public StonewardensEmbrace(int neededFavor, int cooldown, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isActive, canToggle, textColor);
    }

    @Override
    public <T extends BlessingsInstance> T createDefaultInstance() {
        T instance = super.createDefaultInstance();
        if (!instance.getOrCreateTag().contains("natural_blocks")) {
            ListTag blocks = new ListTag();
            blocks.add(StringTag.valueOf(Blocks.STONE.getDescriptionId()));
            blocks.add(StringTag.valueOf(Blocks.DIRT.getDescriptionId()));
            blocks.add(StringTag.valueOf(Blocks.GRASS_BLOCK.getDescriptionId()));
            blocks.add(StringTag.valueOf(Blocks.SAND.getDescriptionId()));
            blocks.add(StringTag.valueOf(Blocks.GRAVEL.getDescriptionId()));
            blocks.add(StringTag.valueOf(Blocks.GRANITE.getDescriptionId()));
            blocks.add(StringTag.valueOf(Blocks.DIORITE.getDescriptionId()));
            blocks.add(StringTag.valueOf(Blocks.ANDESITE.getDescriptionId()));
            blocks.add(StringTag.valueOf(Blocks.SANDSTONE.getDescriptionId()));
            blocks.add(StringTag.valueOf(Blocks.CLAY.getDescriptionId()));
            blocks.add(StringTag.valueOf(Blocks.TERRACOTTA.getDescriptionId()));
            instance.getOrCreateTag().put("natural_blocks", blocks);
        }
        return instance;
    }

    @Override
    public boolean onTick(BlessingsInstance instance, LivingEntity living) {
        Block standingBlock = living.level().getBlockState(living.blockPosition().below()).getBlock();
        ListTag blocks = instance.getOrCreateTag().getList("natural_blocks", 8);

        for (int i = 0; i < blocks.size(); i++) {
            if (standingBlock.getDescriptionId().equals(blocks.getString(i))) {
                living.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 0, false, false));
                return true;
            }
        }

        return super.onTick(instance, living);
    }
}