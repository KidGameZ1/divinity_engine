package net.nightshade.divinity_engine.divinity.blessing.elaris;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals.Cue;

import java.awt.*;
import java.util.Random;

public class MindMirror extends Blessings {
    /**
     * Creates a new blessing with the specified parameters.
     *
     * @param neededFavor Amount of favor required to activate
     * @param cooldown    Cooldown period in seconds
     * @param isPassive   Is a passive blessing or not
     * @param canToggle   Whether blessing can be toggled
     * @param textColor   Color for blessing text
     */
    public MindMirror(int neededFavor, int cooldown, boolean isPassive, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isPassive, isActive, canToggle, textColor);
    }

    @Override
    public boolean onDamageEntity(BlessingsInstance instance, LivingEntity player, LivingHurtEvent event) {
        Random random = new Random();
        for (MobEffectInstance effect : player.getActiveEffects()) {
            if (!effect.getEffect().isBeneficial()) {
                if (random.nextFloat() <= 0.45) {
                    boolean removed = player.removeEffect(effect.getEffect());
                    boolean applied = event.getEntity().addEffect(new MobEffectInstance(effect.getEffect(), effect.getDuration(), effect.getAmplifier(), effect.isAmbient(), effect.isVisible(), effect.showIcon()));
                    if (removed && applied && !player.isInvisible()) {
                        BlessingVisuals.link(instance, player, player.getBoundingBox().getCenter(),
                                event.getEntity().getBoundingBox().getCenter(), Cue.CONTROL);
                    } else if (applied) {
                        BlessingVisuals.at(instance, player, event.getEntity().getBoundingBox().getCenter(), Cue.IMPACT, 0.8);
                    } else if (removed && !player.isInvisible()) {
                        BlessingVisuals.pulse(instance, player, Cue.WARD);
                    }
                    return true;
                }
            }
        }

        return super.onDamageEntity(instance, player, event);
    }
}
