package net.nightshade.divinity_engine.divinity.blessing.kairoth;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.player.CriticalHitEvent;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;

import java.awt.*;

public class Threadsever extends Blessings {
    /**
     * Creates a new blessing with the specified parameters.
     *
     * @param neededFavor Amount of favor required to activate
     * @param cooldown    Cooldown period in seconds
     * @param isPassive   Is a passive blessing or not
     * @param canToggle   Whether blessing can be toggled
     * @param textColor   Color for blessing text
     */
    public Threadsever(int neededFavor, int cooldown, boolean isPassive, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isPassive, isActive, canToggle, textColor);
    }

    @Override
    public boolean onCriticalHit(BlessingsInstance instance, CriticalHitEvent event) {

        if (event.getTarget() instanceof LivingEntity living){
            living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1, false, false, true) );
            return true;
        }

        return super.onCriticalHit(instance, event);
    }
}
