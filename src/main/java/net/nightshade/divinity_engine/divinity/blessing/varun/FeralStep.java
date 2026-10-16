package net.nightshade.divinity_engine.divinity.blessing.varun;

import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals.Cue;

import java.awt.*;

public class FeralStep extends Blessings {
    private final float SPEED_BOOST = 0.07f;
    private final float FALL_DAMAGE_REDUCTION = 0.4f;

    public FeralStep(int neededFavor, int cooldown, boolean isPassive, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isPassive, isActive, canToggle, textColor);
    }


    @Override
    public <T extends BlessingsInstance> T createDefaultInstance() {
        T instance = super.createDefaultInstance();
        if (!instance.getOrCreateTag().contains("speed_boost"))
            instance.getOrCreateTag().putFloat("speed_boost", SPEED_BOOST);
        if (!instance.getOrCreateTag().contains("fall_damage_reduction"))
            instance.getOrCreateTag().putFloat("fall_damage_reduction", FALL_DAMAGE_REDUCTION);
        return instance;
    }

    @Override
    public boolean onTick(BlessingsInstance instance, LivingEntity living) {

        if (living.isSprinting()) {
            living.setDeltaMovement(
                    living.getDeltaMovement().multiply(1 + getSpeedBoost(instance), 1, 1 + getSpeedBoost(instance))
            );
            if (living.getDeltaMovement().horizontalDistanceSqr() > 1.0E-4) {
                BlessingVisuals.at(instance, living, living.position().add(0, 0.08, 0)
                        .subtract(living.getDeltaMovement().multiply(1, 0, 1).normalize().scale(0.2)), Cue.TRAIL, 0.75);
            }
        }

        return super.onTick(instance, living);
    }

    @Override
    public boolean onFall(BlessingsInstance instance, LivingFallEvent event) {
        event.setDamageMultiplier(event.getDamageMultiplier() * (1 - getFallDamageReduction(instance)));
        if (!event.isCanceled() && event.getDistance() > 0 && getFallDamageReduction(instance) > 0) {
            BlessingVisuals.at(instance, event.getEntity(), event.getEntity().position().add(0, 0.08, 0), Cue.WARD, 1.0);
        }
        return super.onFall(instance, event);
    }

    public static float getSpeedBoost(BlessingsInstance instance) {
        return instance.getOrCreateTag().getFloat("speed_boost");
    }
    public static float getFallDamageReduction(BlessingsInstance instance) {
        return instance.getOrCreateTag().getFloat("fall_damage_reduction");
    }
}
