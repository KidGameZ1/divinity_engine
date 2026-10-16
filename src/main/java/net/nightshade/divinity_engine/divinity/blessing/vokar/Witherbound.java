package net.nightshade.divinity_engine.divinity.blessing.vokar;

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
import net.nightshade.nightshade_core.util.MiscHelper;

import java.awt.*;
import java.util.List;

public class Witherbound extends Blessings {
    /**
     * Creates a new blessing with the specified parameters.
     *
     * @param neededFavor Amount of favor required to activate
     * @param cooldown    Cooldown period in seconds
     * @param isPassive   Is a passive blessing or not
     * @param canToggle   Whether blessing can be toggled
     * @param textColor   Color for blessing text
     */
    public Witherbound(int neededFavor, int cooldown, boolean isPassive, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isPassive, isActive, canToggle, textColor);
    }

    @Override
    public boolean onPressed(BlessingsInstance instance, LivingEntity living) {
        if (living.level() instanceof ServerLevel serverLevel) {

            Vec3 position = living.position();
            AABB affectedArea = new AABB(
                    position.x - 5,
                    position.y - 5,
                    position.z - 5,
                    position.x + 5,
                    position.y + 5,
                    position.z + 5
            );
            List<LivingEntity> livingEntityList = serverLevel.getEntitiesOfClass(LivingEntity.class, affectedArea);
            livingEntityList.remove(living);

            for (LivingEntity entity : livingEntityList){
                boolean applied = entity.addEffect(new MobEffectInstance(MobEffects.WITHER, MiscHelper.secondsToTick(3), 0, false, false, true));
                if (applied) {
                    BlessingVisuals.at(instance, living, entity.getBoundingBox().getCenter(), Cue.CONTROL, 0.85);
                }
            }
        }
        return super.onPressed(instance, living);
    }
}
