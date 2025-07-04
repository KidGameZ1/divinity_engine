package net.nightshade.divinity_engine.divinity.blessing.varun;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.nightshade_core.util.MiscHelper;

import java.awt.*;
import java.util.List;

public class PredatorsFocus extends Blessings {

    public PredatorsFocus(int neededFavor, int cooldown, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isActive, canToggle, textColor);
    }

    @Override
    public <T extends BlessingsInstance> T createDefaultInstance() {
        T instance = super.createDefaultInstance();
        if (!instance.getOrCreateTag().contains("crouch_ticks"))
            instance.getOrCreateTag().putFloat("crouch_ticks", 0);
        if (!instance.getOrCreateTag().contains("effects_active"))
            instance.getOrCreateTag().putBoolean("effect_active", false);
        return instance;
    }

    @Override
    public boolean onTick(BlessingsInstance instance, LivingEntity living) {
        if (!(living instanceof Player player)) {
            return super.onTick(instance, living);
        }

        boolean isMoving = player.getDeltaMovement().x != 0 || player.getDeltaMovement().z != 0;

        if (player.isCrouching() && !isMoving && !isEffectsActive(instance) ) {
            increaseCrouchTick(instance);
            if (getCrouchTicks(instance) >= 60) { // 3 seconds * 20 ticks
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, MiscHelper.secondsToTick(10), 0, false, false, true)); // 10 seconds * 20 ticks
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, MiscHelper.secondsToTick(10), 0, false, false, true));
                setEffectsActive(instance, true);
                setCrouchTick(instance, 0);
                return true;
            }
        } else {
            setCrouchTick(instance, 0);
            if (isEffectsActive(instance) && !player.hasEffect(MobEffects.DAMAGE_BOOST) || player.hasEffect(MobEffects.NIGHT_VISION)) {
                setEffectsActive(instance, false);
            }
        }

        return super.onTick(instance, living);
    }

    public static float getCrouchTicks(BlessingsInstance instance) {
        return instance.getOrCreateTag().getFloat("crouch_ticks");
    }
    public static boolean isEffectsActive(BlessingsInstance instance) {
        return instance.getOrCreateTag().getBoolean("effect_active");
    }

    public static void setEffectsActive(BlessingsInstance instance, boolean active) {
        instance.getOrCreateTag().putBoolean("effect_active", active);
    }

    public static void setCrouchTick(BlessingsInstance instance, float ticks){
        instance.getOrCreateTag().putFloat("crouch_ticks", ticks);
    }

    public static void increaseCrouchTick(BlessingsInstance instance){
        instance.getOrCreateTag().putFloat("crouch_ticks", getCrouchTicks(instance) + 1);
    }
}
