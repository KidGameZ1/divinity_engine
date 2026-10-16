package net.nightshade.divinity_engine.divinity.blessing.varun;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals.Cue;

import java.awt.*;
import java.util.List;

public class AlphasCall extends Blessings {
    /**
     * Creates a new blessing with the specified parameters.
     *
     * @param neededFavor Amount of favor required to activate
     * @param cooldown    Cooldown period in seconds
     * @param isPassive   Is a passive blessing or not
     * @param canToggle   Whether blessing can be toggled
     * @param textColor   Color for blessing text
     */
    public AlphasCall(int neededFavor, int cooldown, boolean isPassive, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isPassive, isActive, canToggle, textColor);
    }

    @Override
    public boolean onPressed(BlessingsInstance instance, LivingEntity living) {
        if (living.level() instanceof ServerLevel serverLevel) {
            Vec3 position = living.position();
            AABB affectedArea = new AABB(
                    position.x - 2.5,
                    position.y - 2.5,
                    position.z - 2.5,
                    position.x + 2.5,
                    position.y + 2.5,
                    position.z + 2.5
            );
            List<LivingEntity> livingEntityList = serverLevel.getEntitiesOfClass(LivingEntity.class, affectedArea);
            livingEntityList.remove(living);
            for (LivingEntity entity : livingEntityList) {
                if (entity != living) {
                    entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 255, false, false, true));

                    Vec3 difference = entity.position().subtract(living.position());
                    double strength = 0.05;
                    entity.push(
                            difference.x * strength,
                            0.4D,
                            difference.z * strength
                    );
                }
            }
            if (!livingEntityList.isEmpty()) {
                BlessingVisuals.at(instance, living, position.add(0, 0.08, 0), Cue.CONTROL, 2.5);
                return true;
            }
        }
        return super.onPressed(instance, living);
    }
}
