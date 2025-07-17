package net.nightshade.divinity_engine.divinity.blessing.kairoth;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.nightshade_core.util.MathHelper;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FatefulDraw extends Blessings {
    /**
     * Creates a new blessing with the specified parameters.
     *
     * @param neededFavor Amount of favor required to activate
     * @param cooldown    Cooldown period in seconds
     * @param isPassive   Is a passive blessing or not
     * @param canToggle   Whether blessing can be toggled
     * @param textColor   Color for blessing text
     */
    public FatefulDraw(int neededFavor, int cooldown, boolean isPassive, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isPassive, isActive, canToggle, textColor);
    }

    @Override
    public boolean onTick(BlessingsInstance instance, LivingEntity living) {

        List<MobEffect> effects = new ArrayList<>();
        if (!living.hasEffect(MobEffects.DAMAGE_BOOST))
            effects.add(MobEffects.DAMAGE_BOOST);
        if (!living.hasEffect(MobEffects.MOVEMENT_SPEED))
            effects.add(MobEffects.MOVEMENT_SPEED);
        if (!living.hasEffect(MobEffects.DAMAGE_RESISTANCE))
            effects.add(MobEffects.DAMAGE_RESISTANCE);

        if (!effects.isEmpty()) {
            if (!living.level().isClientSide)
                living.addEffect(new MobEffectInstance(Objects.requireNonNull(MathHelper.pickRandom(effects)), 100, 1, false, false, true));
            return true;
        }

        return super.onTick(instance, living);
    }
}
