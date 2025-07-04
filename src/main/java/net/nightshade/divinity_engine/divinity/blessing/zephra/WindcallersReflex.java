package net.nightshade.divinity_engine.divinity.blessing.zephra;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.nightshade_core.util.MiscHelper;

import java.awt.*;

public class WindcallersReflex extends Blessings {
    public WindcallersReflex(int neededFavor, int cooldown, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isActive, canToggle, textColor);
    }

    @Override
    public boolean onProjectileHit(BlessingsInstance instance, LivingEntity living, ProjectileImpactEvent event) {
        if (!instance.isOnCooldown()) {
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, MiscHelper.secondsToTick(5), 1, false, false, true));
            event.setCanceled(true);

            return true;
        }
        return false;
    }
}
