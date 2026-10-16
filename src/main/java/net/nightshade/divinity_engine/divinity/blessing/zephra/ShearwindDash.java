package net.nightshade.divinity_engine.divinity.blessing.zephra;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals.Cue;

import java.awt.*;
import java.util.List;

public class ShearwindDash extends Blessings {
    /**
     * Creates a new blessing with the specified parameters.
     *
     * @param neededFavor Amount of favor required to activate
     * @param cooldown    Cooldown period in seconds
     * @param isPassive   Is a passive blessing or not
     * @param canToggle   Whether blessing can be toggled
     * @param textColor   Color for blessing text
     */
    public ShearwindDash(int neededFavor, int cooldown, boolean isPassive, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isPassive, isActive, canToggle, textColor);
    }

    @Override
    public boolean onPressed(BlessingsInstance instance, LivingEntity living) {
        Level level = living.level();
        if (!(level instanceof ServerLevel serverLevel)) return false;

        if (living.onGround()) return false; // Only usable midair

        Vec3 look = living.getLookAngle().normalize();
        Vec3 dash = look.scale(1.5); // 3 blocks total (1.5 x 2 ticks)

        // Dash forward
        living.setDeltaMovement(dash.x, living.getDeltaMovement().y, dash.z);
        living.hurtMarked = true;
        Vec3 origin = living.getBoundingBox().getCenter();
        BlessingVisuals.link(instance, living, origin, origin.add(living.getDeltaMovement()), Cue.TRAIL);

        // Damage & knockback along path
        double range = 2.0;
        AABB hitbox = living.getBoundingBox().inflate(range);
        List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, hitbox, e -> e != living && e.isAlive());

        for (LivingEntity target : targets) {
            target.hurt(living.damageSources().mobAttack(living), 3.0f);
            Vec3 knock = target.position().subtract(living.position()).normalize().scale(0.6);
            target.setDeltaMovement(knock.x, 0.3, knock.z);
            target.hurtMarked = true;
            BlessingVisuals.at(instance, living, target.getBoundingBox().getCenter(), Cue.IMPACT,
                    Math.max(0.75, target.getBbWidth()), new Vec3(knock.x, 0.3, knock.z));
        }

        return true;
    }

}
