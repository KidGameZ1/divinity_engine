package net.nightshade.divinity_engine.divinity.blessing.nashara;

import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals.Cue;

import java.awt.*;

public class ToxicSkin extends Blessings {
    public ToxicSkin(int neededFavor, int cooldown, boolean isPassive, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isPassive, isActive, canToggle, textColor);
    }

    @Override
    public boolean onTick(BlessingsInstance instance, LivingEntity living) {
        if (living.hasEffect(MobEffects.POISON)) {
            boolean removed = living.removeEffect(MobEffects.POISON);
            if (removed && !living.isInvisible()) {
                BlessingVisuals.at(instance, living, living.getBoundingBox().getCenter(), Cue.WARD, 0.4);
            }
            return false;
        }
        return false;
    }
}
