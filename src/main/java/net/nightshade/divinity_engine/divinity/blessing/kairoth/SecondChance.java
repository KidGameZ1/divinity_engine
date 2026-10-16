package net.nightshade.divinity_engine.divinity.blessing.kairoth;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals.Cue;
import net.nightshade.divinity_engine.util.divinity.narrative.BlessingSpecializations;

import java.awt.*;

/**
 * A blessing that prevents death once by restoring half of the player's maximum health.
 * Only activates if the player does not have a Totem of Undying in their offhand.
 */
public class SecondChance extends Blessings {
    /**
     * @param neededFavor Amount of favor required to activate this blessing
     * @param cooldown    Cooldown duration in ticks before the blessing can be used again
     * @param isPassive    Whether the blessing is currently active
     * @param canToggle   Whether the blessing can be toggled on/off
     * @param textColor   Color used for blessing text in the UI
     */
    public SecondChance(int neededFavor, int cooldown, boolean isPassive, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isPassive, isActive, canToggle, textColor);
    }

    /**
     * Handles the death event by preventing death and restoring health.
     *
     * @param instance The blessing instance
     * @param event    The death event to handle
     * @return true if death was prevented, false if player has Totem of Undying
     */
    @Override
    public boolean onDeath(BlessingsInstance instance, LivingDeathEvent event) {
        if (event.getEntity().getOffhandItem().getItem().toString().contains("totem_of_undying")) return false;
        event.setCanceled(true);
        float healthRatio = BlessingSpecializations.isActive(event.getEntity(), "kairoth", "a") ? 0.6f : 0.5f;
        event.getEntity().setHealth(event.getEntity().getMaxHealth() * healthRatio);
        if (!event.getEntity().isInvisible()) BlessingVisuals.pulse(instance, event.getEntity(), Cue.HEAL);
        if (BlessingSpecializations.isActive(event.getEntity(), "kairoth", "b")) {
            boolean warded = event.getEntity().addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 0));
            if (warded && !event.getEntity().isInvisible()) BlessingVisuals.pulse(instance, event.getEntity(), Cue.WARD);
        }
        return true;
    }
}
