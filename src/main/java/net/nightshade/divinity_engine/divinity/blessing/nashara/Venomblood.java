package net.nightshade.divinity_engine.divinity.blessing.nashara;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals.Cue;

import java.awt.*;

public class Venomblood extends Blessings {
    /**
     * Creates a new blessing with the specified parameters.
     *
     * @param neededFavor Amount of favor required to activate
     * @param cooldown    Cooldown period in seconds
     * @param isPassive   Initial active state
     * @param canToggle   Whether blessing can be toggled
     * @param textColor   Color for blessing text
     */
    public Venomblood(int neededFavor, int cooldown, boolean isPassive, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isPassive, isActive, canToggle, textColor);
    }

    @Override
    public boolean onTakenDamage(BlessingsInstance instance, LivingDamageEvent event) {

        if (event.getSource().getDirectEntity() instanceof LivingEntity attacker && attacker.isOnFire()) {
            boolean applied = attacker.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0, false, false, true) );
            if (applied) {
                BlessingVisuals.at(instance, event.getEntity(), attacker.getBoundingBox().getCenter(), Cue.IMPACT, 0.8);
            }
        }

        return super.onTakenDamage(instance, event);
    }
}
