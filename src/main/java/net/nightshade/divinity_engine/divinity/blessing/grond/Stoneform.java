package net.nightshade.divinity_engine.divinity.blessing.grond;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.nightshade_core.util.MiscHelper;

import java.awt.*;

/**
 * Stoneform blessing that provides damage resistance when player's health is low.
 * Part of Grond's blessing collection.
 */
public class Stoneform extends Blessings {
    /**
     * @param neededFavor Amount of favor required to activate the blessing
     * @param cooldown    Cooldown period in ticks before blessing can be used again
     * @param isPassive    Whether the blessing is currently active
     * @param canToggle   Whether the blessing can be toggled on/off
     * @param textColor   Color used for blessing text in UI
     */
    public Stoneform(int neededFavor, int cooldown, boolean isPassive, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isPassive, isActive, canToggle, textColor);
    }


    /**
     * Triggers when player takes damage. Applies damage resistance effect when health is low.
     *
     * @param instance Current blessing instance
     * @param event    The damage event
     * @return true if effect was applied, false otherwise
     */
    @Override
    public boolean onTakenDamage(BlessingsInstance instance, LivingDamageEvent event) {
        LivingEntity player = event.getEntity();
        if (player.getHealth() <= 15){
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, MiscHelper.secondsToTick(10), 2, false, false, true));
            return true;
        }

        return super.onTakenDamage(instance, event);
    }
}
