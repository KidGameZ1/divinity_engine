package net.nightshade.divinity_engine.divinity.blessing.nashara;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;

import java.awt.*;

public class CoiledRetribution extends Blessings {
    /**
     * Creates a new blessing with the specified parameters.
     *
     * @param neededFavor Amount of favor required to activate
     * @param cooldown    Cooldown period in seconds
     * @param isPassive   Initial active state
     * @param canToggle   Whether blessing can be toggled
     * @param textColor   Color for blessing text
     */
    public CoiledRetribution(int neededFavor, int cooldown, boolean isPassive, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isPassive, isActive, canToggle, textColor);
    }

    @Override
    public boolean onTakenDamage(BlessingsInstance instance, LivingDamageEvent event) {
        if (event.getEntity().getHealth() <= event.getEntity().getMaxHealth()*0.5){
            if (event.getSource().getEntity() instanceof LivingEntity living){
                event.setAmount(event.getAmount()*0.75f);
                living.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0, false, false, true));
            }
        }
        return super.onTakenDamage(instance, event);
    }
}
