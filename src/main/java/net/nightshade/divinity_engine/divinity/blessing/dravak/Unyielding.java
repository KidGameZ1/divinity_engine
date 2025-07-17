package net.nightshade.divinity_engine.divinity.blessing.dravak;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;

import java.awt.*;

/**
 * A blessing that provides damage resistance and knockback reduction when health is low.
 * Activates its effects when the player's health falls below 50%.
 */
public class Unyielding extends Blessings {
    /**
     * Creates a new Unyielding blessing instance.
     *
     * @param neededFavor Amount of favor required to activate this blessing
     * @param cooldown    Cooldown period in ticks
     * @param isPassive    Whether the blessing starts active
     * @param canToggle   Whether the blessing can be toggled on/off
     * @param textColor   Color used for blessing text display
     */
    public Unyielding(int neededFavor, int cooldown, boolean isPassive, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isPassive, isActive, canToggle, textColor);
    }

    /**
     * Applies damage resistance effect when player health is below 50%.
     * Resistance effect is level 1 and lasts for 1 second (20 ticks).
     *
     * @param instance The blessing instance
     * @param player   The player entity
     * @return True if the tick was handled
     */
    @Override
    public boolean onTick(BlessingsInstance instance, LivingEntity player) {
        if (player.getHealth() <= player.getMaxHealth() * 0.5f) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20, 0, false, false, true));
        }
        return super.onTick(instance, player);
    }

    /**
     * Reduces knockback by 50% when player health is below 50%.
     *
     * @param instance The blessing instance
     * @param event    The knockback event
     * @return True if the knockback was modified
     */
    @Override
    public boolean onKnockback(BlessingsInstance instance, LivingKnockBackEvent event) {
        LivingEntity player = event.getEntity();

        if (player.getHealth() <= player.getMaxHealth() * 0.5f) {
            event.setStrength(event.getStrength() * 0.5f);
            event.setCanceled(true);
        }

        return super.onKnockback(instance, event);
    }
}
