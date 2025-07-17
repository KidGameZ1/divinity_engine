package net.nightshade.divinity_engine.divinity.blessing.dravak;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.nightshade_core.util.MiscHelper;

import java.awt.*;
import java.util.List;

public class FuryUnbound extends Blessings {
    /**
     * Creates a new blessing with the specified parameters.
     *
     * @param neededFavor Amount of favor required to activate
     * @param cooldown    Cooldown period in seconds
     * @param isPassive   Is a passive blessing or not
     * @param canToggle   Whether blessing can be toggled
     * @param textColor   Color for blessing text
     */
    public FuryUnbound(int neededFavor, int cooldown, boolean isPassive, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isPassive, isActive, canToggle, textColor);
    }

    @Override
    public boolean onPressed(BlessingsInstance instance, LivingEntity living) {

        if (!living.hasEffect(MobEffects.DAMAGE_BOOST) && !living.hasEffect(MobEffects.DAMAGE_RESISTANCE)) {
            living.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, MiscHelper.secondsToTick(15), 1, false, false, true));
            living.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, MiscHelper.secondsToTick(15), 0, false, false, true));
        }

        return super.onPressed(instance, living);
    }
}
