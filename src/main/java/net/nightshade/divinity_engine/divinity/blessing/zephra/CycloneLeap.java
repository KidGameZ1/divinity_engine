package net.nightshade.divinity_engine.divinity.blessing.zephra;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals.Cue;

import java.awt.*;

public class CycloneLeap extends Blessings {
    public CycloneLeap(int neededFavor, int cooldown, boolean isPassive, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isPassive, isActive, canToggle, textColor);
    }

    @Override
    public boolean onPressed(BlessingsInstance instance, LivingEntity living) {
        double upwardVelocity = 0.7;
        living.setDeltaMovement(living.getDeltaMovement().x, upwardVelocity, living.getDeltaMovement().z);
        living.hurtMarked = true;
        living.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 80, 0));
        Vec3 origin = living.position();
        BlessingVisuals.link(instance, living, origin, origin.add(living.getDeltaMovement()), Cue.TRAIL);
        return true;
    }
}
