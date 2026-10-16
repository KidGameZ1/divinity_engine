package net.nightshade.divinity_engine.divinity.blessing.ignar;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals.Cue;

import java.awt.*;
import java.util.List;

public class InfernoMantle extends Blessings {
    /**
     * Creates a new blessing with the specified parameters.
     *
     * @param neededFavor Amount of favor required to activate
     * @param cooldown    Cooldown period in seconds
     * @param isPassive   Is a passive blessing or not
     * @param canToggle   Whether blessing can be toggled
     * @param textColor   Color for blessing text
     */
    public InfernoMantle(int neededFavor, int cooldown, boolean isPassive, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isPassive, isActive, canToggle, textColor);
    }

    @Override
    public boolean onTick(BlessingsInstance instance, LivingEntity living) {
        if(instance.isToggled()) {
            if (living.level() instanceof ServerLevel serverLevel) {
                Vec3 position = living.position();
                AABB affectedArea = new AABB(
                        position.x - 3,
                        position.y - 3,
                        position.z - 3,
                        position.x + 3,
                        position.y + 3,
                        position.z + 3
                );
                List<LivingEntity> livingEntityList = serverLevel.getEntitiesOfClass(LivingEntity.class, affectedArea);
                livingEntityList.remove(living);
                for (LivingEntity entity : livingEntityList) {
                    if (entity != living) {
                        if (!entity.isOnFire()) {
                            entity.setSecondsOnFire(2);
                            BlessingVisuals.at(instance, living, entity.getBoundingBox().getCenter(), Cue.IMPACT,
                                    Math.max(0.5, entity.getBbWidth()));
                        }
                    }
                }
                if (!livingEntityList.isEmpty()) {
                    BlessingVisuals.at(instance, living, position, Cue.AURA, 3.0);
                }
            }
        }


        return super.onTick(instance, living);
    }
}
