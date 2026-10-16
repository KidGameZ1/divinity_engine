package net.nightshade.divinity_engine.divinity.blessing.solarius;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.phys.Vec3;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals.Cue;
import net.nightshade.nightshade_core.util.MiscHelper;

import java.awt.*;
import java.util.List;


public class Sunburst extends Blessings {
    public Sunburst(int neededFavor, int cooldown, boolean isPassive, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isPassive, isActive, canToggle, textColor);
    }

    @Override
    public boolean onPressed(BlessingsInstance instance, LivingEntity living) {
        Level level = living.level();
        double range = 5.0D;

        List<LivingEntity> entityList = level.getEntitiesOfClass(LivingEntity.class, living.getBoundingBox().inflate(range));
        entityList.remove(living);
        for (LivingEntity target : entityList) {
            if (target != living) {
                // Apply blindness
                boolean blinded = target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, MiscHelper.secondsToTick(5), 0, false, false, true));

                // Set on fire
                target.setSecondsOnFire(5);

                // Calculate and apply knockback
                Vec3 difference = target.position().subtract(living.position());
                double strength = 0.5;
                target.push(
                        difference.x * strength,
                        0.4D,
                        difference.z * strength
                );
                Vec3 contact = target.getBoundingBox().getCenter();
                Vec3 outward = new Vec3(difference.x * strength, 0.4D, difference.z * strength).normalize();
                BlessingVisuals.link(instance, living, contact, contact.add(outward.scale(0.4)), Cue.IMPACT);
                if (blinded) {
                    BlessingVisuals.at(instance, living, target.getEyePosition(), Cue.CONTROL, 0.4);
                }
            }
        }

        if (!entityList.isEmpty()){
            BlessingVisuals.at(instance, living, living.position(), Cue.CAST, range);
        }

        return !entityList.isEmpty();
    }
}
