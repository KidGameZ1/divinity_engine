package net.nightshade.divinity_engine.divinity.blessing.lumen;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals.Cue;

import java.awt.*;
import java.util.List;

public class Dawnshatter extends Blessings {
    /**
     * Creates a new blessing with the specified parameters.
     *
     * @param neededFavor Amount of favor required to activate
     * @param cooldown    Cooldown period in seconds
     * @param isPassive   Is a passive blessing or not
     * @param canToggle   Whether blessing can be toggled
     * @param textColor   Color for blessing text
     */
    public Dawnshatter(int neededFavor, int cooldown, boolean isPassive, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isPassive, isActive, canToggle, textColor);
    }

    @Override
    public boolean onPressed(BlessingsInstance instance, LivingEntity living) {
        if (!(living.level() instanceof ServerLevel serverLevel)) return false;

        Vec3 forward = living.getLookAngle().normalize();
        double coneRadius = 6.0;
        double coneAngle = Math.toRadians(60); // 60-degree cone

        AABB area = living.getBoundingBox().inflate(coneRadius);
        List<LivingEntity> nearby = serverLevel.getEntitiesOfClass(LivingEntity.class, area, e -> e != living && e.isAlive());

        for (LivingEntity target : nearby) {
            Vec3 toTarget = target.position().subtract(living.position()).normalize();
            double angle = forward.dot(toTarget);

            if (angle > Math.cos(coneAngle / 2)) {
                if (EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(target)) {
                    if (target.getMobType() == MobType.UNDEAD || target instanceof Monster) {
                        // Damage dark-aligned enemies
                        target.hurt(living.damageSources().magic(), 4.0f);
                        target.setSecondsOnFire(3);
                        BlessingVisuals.at(instance, living, target.getBoundingBox().getCenter(), Cue.IMPACT,
                                Math.max(0.75, target.getBbWidth()));
                    }
                }
            }
        }

        Vec3 origin = living.position(); // The target-angle test is rooted at the caster's feet.
        BlessingVisuals.at(instance, living, origin, Cue.CAST, coneRadius);

        return true;
    }
}
