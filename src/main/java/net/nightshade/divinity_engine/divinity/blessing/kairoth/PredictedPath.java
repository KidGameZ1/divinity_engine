package net.nightshade.divinity_engine.divinity.blessing.kairoth;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraftforge.common.Tags;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.nightshade_core.util.MiscHelper;

import java.awt.*;

/**
 * A blessing that grants increased movement speed when dangerous entities are nearby.
 * Detects undead mobs, creepers, arthropods and players within a 10 block radius.
 */
public class PredictedPath extends Blessings {
    /**
     * Creates a new PredictedPath blessing.
     *
     * @param neededFavor The favor points required to activate this blessing
     * @param cooldown    The cooldown period in ticks before the blessing can be used again
     * @param isActive    Whether the blessing is currently active
     * @param canToggle   Whether the blessing can be toggled on/off
     * @param textColor   The color used for displaying blessing text in the UI
     */
    public PredictedPath(int neededFavor, int cooldown, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isActive, canToggle, textColor);
    }

    /**
     * Checks for nearby dangerous entities and grants speed boost if found.
     *
     * @param instance The blessing instance being ticked
     * @param living   The living entity that has this blessing
     * @return true if speed effect was applied, false otherwise
     */
    @Override
    public boolean onTick(BlessingsInstance instance, LivingEntity living) {
        boolean isDangerNearby = living.level().getEntities(living, living.getBoundingBox().inflate(10.0D))
                .stream()
                .filter(entity -> entity instanceof LivingEntity)
                .map(entity -> (LivingEntity) entity)
                .anyMatch(entity -> entity.getMobType() == MobType.UNDEAD ||
                        entity.getType() == EntityType.CREEPER ||
                        entity.getMobType() == MobType.ARTHROPOD ||
                        entity.getType() == EntityType.PLAYER);

        if (isDangerNearby) {
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, MiscHelper.secondsToTick(4), 1, false, false, true));
            return true;
        }

        return false;
    }
}
