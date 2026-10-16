package net.nightshade.divinity_engine.divinity.blessing.zephra;

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

public class TempestSpiral extends Blessings {
    /**
     * Creates a new blessing with the specified parameters.
     *
     * @param neededFavor Amount of favor required to activate
     * @param cooldown    Cooldown period in seconds
     * @param isPassive   Is a passive blessing or not
     * @param canToggle   Whether blessing can be toggled
     * @param textColor   Color for blessing text
     */
    public TempestSpiral(int neededFavor, int cooldown, boolean isPassive, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isPassive, isActive, canToggle, textColor);
    }

    @Override
    public boolean onPressed(BlessingsInstance instance, LivingEntity living) {
        if (!(living.level() instanceof ServerLevel serverLevel)) return false;

        double radius = 4.0;
        AABB vortexArea = living.getBoundingBox().inflate(radius);

        List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, vortexArea, e -> e != living && e.isAlive());

        for (LivingEntity target : targets) {
            Vec3 direction = living.position().subtract(target.position()).normalize();
            Vec3 pull = direction.scale(0.4);
            target.setDeltaMovement(pull.x, 0.1, pull.z);
            target.hurt(living.damageSources().magic(), 2.0f);
            target.hurtMarked = true;
            BlessingVisuals.at(instance, living, target.position().add(0, 0.15, 0), Cue.IMPACT,
                    Math.max(0.5, target.getBbWidth()), new Vec3(pull.x, 0.1, pull.z));
        }

        BlessingVisuals.at(instance, living, living.position(), Cue.CONTROL, radius);

        return true;
    }

}
