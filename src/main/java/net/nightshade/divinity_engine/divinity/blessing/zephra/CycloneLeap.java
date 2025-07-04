package net.nightshade.divinity_engine.divinity.blessing.zephra;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;

import java.awt.*;

public class CycloneLeap extends Blessings {
    public CycloneLeap(int neededFavor, int cooldown, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isActive, canToggle, textColor);
    }

    @Override
    public boolean onPressed(BlessingsInstance instance, LivingEntity living) {
        double upwardVelocity = 1.0;
        living.setDeltaMovement(living.getDeltaMovement().x, upwardVelocity, living.getDeltaMovement().z);
        living.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 80, 0));
        return true;
    }
}
