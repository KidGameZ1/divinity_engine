package net.nightshade.divinity_engine.divinity.blessing.nerai;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals.Cue;

import java.awt.*;

public class Wavebound extends Blessings {
    public Wavebound(int neededFavor, int cooldown, boolean isPassive, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isPassive, isActive, canToggle, textColor);
    }

    @Override
    public <T extends BlessingsInstance> T createDefaultInstance() {
        T instance = super.createDefaultInstance();
        if (!instance.getOrCreateTag().contains("dash_speed"))
            instance.getOrCreateTag().putDouble("dash_speed", 2.5d);
        return instance;
    }

    @Override
    public boolean onPressed(BlessingsInstance instance, LivingEntity living) {
        if (living.isInWater()) {
            Vec3 lookVec = living.getLookAngle();
            double dashSpeed = instance.getOrCreateTag().getDouble("dash_speed");
            living.setDeltaMovement(lookVec.x * dashSpeed, lookVec.y * dashSpeed, lookVec.z * dashSpeed);

            Vec3 origin = living.getBoundingBox().getCenter();
            BlessingVisuals.link(instance, living, origin, origin.add(living.getDeltaMovement()), Cue.TRAIL);

            return true;
        }
        return false;
    }
}
