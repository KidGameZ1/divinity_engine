package net.nightshade.divinity_engine.divinity.blessing.nerai;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals.Cue;

import java.awt.*;
import java.util.List;

public class TidecallersWrath extends Blessings {
    /**
     * Creates a new blessing with the specified parameters.
     *
     * @param neededFavor Amount of favor required to activate
     * @param cooldown    Cooldown period in seconds
     * @param isPassive   Is a passive blessing or not
     * @param canToggle   Whether blessing can be toggled
     * @param textColor   Color for blessing text
     */
    public TidecallersWrath(int neededFavor, int cooldown, boolean isPassive, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isPassive, isActive, canToggle, textColor);
    }

    @Override
    public boolean onPressed(BlessingsInstance instance, LivingEntity living) {
        if (!(living.level() instanceof ServerLevel serverLevel)) return false;

        if (!living.isInWaterOrBubble()) return false;

        double radius = 6.0;
        AABB area = living.getBoundingBox().inflate(radius);

        List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, area).stream()
                .filter(e -> e != living && e.isAlive() && !(e instanceof Player && ((Player) e).isCreative()))
                .toList();

        for (LivingEntity target : targets) {
            Vec3 pushDirection = target.position().subtract(living.position()).normalize();
            Vec3 knockback = new Vec3(pushDirection.x * 1.3, 0.6, pushDirection.z * 1.3);

            target.setDeltaMovement(knockback);
            target.hurtMarked = true; // Ensures knockback is applied on next tick
            BlessingVisuals.at(instance, living, target.position().add(0, 0.5, 0), Cue.IMPACT,
                    Math.max(0.75, target.getBbWidth()), knockback);
        }

        BlessingVisuals.at(instance, living, living.position().add(0, 0.5, 0), Cue.CONTROL, radius);

        return true;
    }

}
