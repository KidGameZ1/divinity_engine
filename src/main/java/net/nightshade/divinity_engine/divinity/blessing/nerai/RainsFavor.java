package net.nightshade.divinity_engine.divinity.blessing.nerai;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;

import java.awt.*;
import java.util.UUID;

public class RainsFavor extends Blessings {
    public RainsFavor(int neededFavor, int cooldown, boolean isPassive, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isPassive, isActive, canToggle, textColor);
    }

    @Override
    public boolean onTick(BlessingsInstance instance, LivingEntity living) {
        if (living.isInWaterRainOrBubble()){
            if (!living.hasEffect(MobEffects.REGENERATION)){
                living.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 0, false, false, true));
            }
        }
        return super.onTick(instance, living);
    }
}