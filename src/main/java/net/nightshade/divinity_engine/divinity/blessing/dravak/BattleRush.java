package net.nightshade.divinity_engine.divinity.blessing.dravak;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;

import java.awt.*;

public class BattleRush extends Blessings {
    /**
     * Creates a battle rush blessing that grants speed and strength on kills
     *
     * @param neededFavor Amount of favor required to activate
     * @param cooldown    Cooldown period in seconds
     * @param isActive    Initial active state
     * @param canToggle   Whether blessing can be toggled
     * @param textColor   Color for blessing text
     */
    public BattleRush(int neededFavor, int cooldown, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isActive, canToggle, textColor);
    }

    /**
     * Grants speed and strength buffs when the player kills an entity
     *
     * @param instance The blessing instance
     * @param player   The player that killed the entity
     * @param event    The entity death event
     * @return True if buffs were applied
     */
    @Override
    public boolean onKillEntity(BlessingsInstance instance, LivingEntity player, LivingDeathEvent event) {

        if (!event.getEntity().isAlive()) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 0, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 100, 0, false, false, true));
            return true;
        }

        return super.onKillEntity(instance, player, event);
    }
}
