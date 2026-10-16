package net.nightshade.divinity_engine.divinity.blessing.aethon;

import net.minecraft.world.entity.LivingEntity;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals;

import java.awt.*;
import java.util.List;

/**
 * A blessing that marks nearby entities while the owner is crouching.
 */
public class Veilpiercer extends Blessings {
    /**
     * Creates a new Veilpiercer blessing.
     *
     * @param neededFavor Amount of favor required to activate
     * @param cooldown    Cooldown period in seconds
     * @param isPassive    Whether blessing starts active
     * @param canToggle   Whether blessing can be toggled
     * @param textColor   Color for blessing text
     */
    public Veilpiercer(int neededFavor, int cooldown, boolean isPassive, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isPassive, isActive, canToggle, textColor);
    }

    /**
     * Emits visual marks at nearby entities while crouching.
     *
     * @param instance The blessing instance
     * @param living   The blessed entity
     * @return True for cooldown
     */
    @Override
    public boolean onTick(BlessingsInstance instance, LivingEntity living) {
        if (!living.level().isClientSide && living.tickCount % 20 == 0 && living.isShiftKeyDown()) {
            List<LivingEntity> allEntities = living.level().getEntitiesOfClass(
                    LivingEntity.class,
                    living.getBoundingBox().inflate(32.0D),
                    entity -> entity != living
            );

            // Track only entities within the 16-block effect radius
            List<LivingEntity> affectedEntities = allEntities.stream()
                    .filter(entity -> entity.distanceTo(living) <= 16.0D)
                    .toList();

            BlessingVisuals.privateMarks(instance, living, affectedEntities);
        }


        return super.onTick(instance, living);
    }
}
