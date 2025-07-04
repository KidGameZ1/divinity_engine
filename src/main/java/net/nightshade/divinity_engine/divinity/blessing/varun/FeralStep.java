package net.nightshade.divinity_engine.divinity.blessing.varun;

import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;

import java.awt.*;

public class FeralStep extends Blessings {
    private final float SPEED_BOOST = 0.07f;
    private final float FALL_DAMAGE_REDUCTION = 0.4f;

    public FeralStep(int neededFavor, int cooldown, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isActive, canToggle, textColor);
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
        }

        return super.onTick(instance, living);
    }

    @Override
    public boolean onFall(BlessingsInstance instance, LivingFallEvent event) {
        event.setDamageMultiplier(event.getDamageMultiplier() * (1 - getFallDamageReduction(instance)));
        return super.onFall(instance, event);
    }

    public static float getSpeedBoost(BlessingsInstance instance) {
        return instance.getOrCreateTag().getFloat("speed_boost");
    }
    public static float getFallDamageReduction(BlessingsInstance instance) {
        return instance.getOrCreateTag().getFloat("fall_damage_reduction");
    }
}
