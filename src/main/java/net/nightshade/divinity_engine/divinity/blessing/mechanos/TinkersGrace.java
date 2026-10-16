package net.nightshade.divinity_engine.divinity.blessing.mechanos;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.phys.Vec3;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals.Cue;

import java.awt.*;

public class TinkersGrace extends Blessings {
    public TinkersGrace(int neededFavor, int cooldown, boolean isPassive, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isPassive, isActive, canToggle, textColor);
    }

    @Override
    public boolean onTick(BlessingsInstance instance, LivingEntity living) {
        // This placeholder has no durability mechanic; only visualize active tool use.
        if (living.swinging && living.getMainHandItem().getItem() instanceof TieredItem) {
            Vec3 right = Vec3.directionFromRotation(0, living.getYRot() + 90);
            Vec3 toolPosition = living.position().add(0, living.getBbHeight() * 0.6, 0)
                    .add(living.getLookAngle().scale(0.35))
                    .add(right.scale(living.getMainArm() == HumanoidArm.RIGHT ? 0.22 : -0.22));
            BlessingVisuals.at(instance, living, toolPosition, Cue.AURA, 0.4);
        }
        return super.onTick(instance, living);
    }
}
