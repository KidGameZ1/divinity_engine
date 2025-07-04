package net.nightshade.divinity_engine.divinity.blessing.solarius;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.level.Level;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.nightshade_core.util.MiscHelper;

import java.awt.*;

public class Sunburst extends Blessings {
    public Sunburst(int neededFavor, int cooldown, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isActive, canToggle, textColor);
    }

    @Override
    public boolean onPressed(BlessingsInstance instance, LivingEntity living) {
        Level level = living.level();
        double range = 5.0D;

        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, living.getBoundingBox().inflate(range))) {
            if (target != living) {
                // Apply blindness
                target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, MiscHelper.secondsToTick(5), 0, false, false, true));

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
            }
        }


        return true;
    }
}
