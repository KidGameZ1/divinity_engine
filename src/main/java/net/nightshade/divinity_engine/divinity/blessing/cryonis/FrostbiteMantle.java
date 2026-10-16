package net.nightshade.divinity_engine.divinity.blessing.cryonis;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals.Cue;

import java.awt.*;

public class FrostbiteMantle extends Blessings {
    /**
     * Creates a new blessing with the specified parameters.
     *
     * @param neededFavor Amount of favor required to activate
     * @param cooldown    Cooldown period in seconds
     * @param isPassive   Is a passive blessing or not
     * @param canToggle   Whether blessing can be toggled
     * @param textColor   Color for blessing text
     */
    public FrostbiteMantle(int neededFavor, int cooldown, boolean isPassive, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isPassive, isActive, canToggle, textColor);
    }

    @Override
    public boolean onBeingDamaged(BlessingsInstance instance, LivingAttackEvent event) {
        if (event.getSource().getEntity() instanceof LivingEntity attacker){
            attacker.setTicksFrozen(200);
            attacker.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0, false, false, true));
            BlessingVisuals.at(instance, event.getEntity(), attacker.position().add(0, 0.08, 0), Cue.CONTROL,
                    Math.max(0.5, attacker.getBbWidth()));
        }
        return super.onBeingDamaged(instance, event);
    }
}
