package net.nightshade.divinity_engine.divinity.blessing.zephra;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals.Cue;
import net.nightshade.divinity_engine.util.divinity.narrative.BlessingSpecializations;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.nightshade.nightshade_core.util.MiscHelper;

import java.awt.*;

public class GaleStep extends Blessings {
    public GaleStep(int neededFavor, int cooldown, boolean isPassive, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isPassive, isActive, canToggle, textColor);
    }

    @Override
    public boolean onTick(BlessingsInstance instance, LivingEntity living) {
        if (living.level().isClientSide) return false;
        BlessingSpecializations.modifier(living, Attributes.MOVEMENT_SPEED, BlessingSpecializations.GALE_SPEED,
                "Unbound journey", BlessingSpecializations.isActive(living, "zephra", "a") ? 0.05 : 0);
        living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, MiscHelper.secondsToTick(2), 0, true, false));
        if (living.getDeltaMovement().horizontalDistanceSqr() > 0.001) {
            Vec3 movement = living.getDeltaMovement();
            BlessingVisuals.at(instance, living, living.position().add(0, 0.04, 0), Cue.TRAIL, 0.55,
                    new Vec3(movement.x, 0, movement.z));
        }
        return super.onTick(instance, living);
    }

    @Override
    public boolean onFall(BlessingsInstance instance, LivingFallEvent event) {
        if (BlessingSpecializations.isActive(event.getEntity(), "zephra", "b")) {
            event.setDamageMultiplier(event.getDamageMultiplier() * 0.75f);
            if (event.getDistance() > 3.0f && event.getDamageMultiplier() > 0.0f) {
                BlessingVisuals.at(instance, event.getEntity(), event.getEntity().position().add(0, 0.04, 0), Cue.WARD, 0.7);
            }
        }
        return false;
    }
}
