package net.nightshade.divinity_engine.divinity.blessing.varun;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals.Cue;
import net.nightshade.divinity_engine.util.divinity.narrative.BlessingSpecializations;

import java.awt.*;

public class SavageHunger extends Blessings {
    /**
     * Creates a new blessing with the specified parameters.
     *
     * @param neededFavor Amount of favor required to activate
     * @param cooldown    Cooldown period in seconds
     * @param isPassive   Is a passive blessing or not
     * @param canToggle   Whether blessing can be toggled
     * @param textColor   Color for blessing text
     */
    public SavageHunger(int neededFavor, int cooldown, boolean isPassive, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isPassive, isActive, canToggle, textColor);
    }

    @Override
    public boolean onKillEntity(BlessingsInstance instance, LivingEntity player, LivingDeathEvent event) {
        int duration = BlessingSpecializations.isActive(player, "varun", "b") ? 140 : 100;
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, 0, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, 0, false, false, true));
        if (!event.isCanceled()) {
            BlessingVisuals.at(instance, player, player.position().add(0, 0.08, 0), Cue.TRAIL, 0.75);
        }
        if (BlessingSpecializations.isActive(player, "varun", "a")) {
            float healthBefore = player.getHealth();
            player.heal(1.0f);
            if (!event.isCanceled() && player.getHealth() > healthBefore) {
                BlessingVisuals.pulse(instance, player, Cue.HEAL);
            }
        }
        return true;
    }
}
