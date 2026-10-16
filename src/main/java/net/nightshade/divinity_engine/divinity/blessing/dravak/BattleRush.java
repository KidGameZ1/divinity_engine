package net.nightshade.divinity_engine.divinity.blessing.dravak;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals.Cue;
import net.nightshade.divinity_engine.util.divinity.narrative.BlessingSpecializations;

import java.awt.*;

public class BattleRush extends Blessings {
    /**
     * Creates a battle rush blessing that grants speed and strength on kills
     *
     * @param neededFavor Amount of favor required to activate
     * @param cooldown    Cooldown period in seconds
     * @param isPassive    Initial active state
     * @param canToggle   Whether blessing can be toggled
     * @param textColor   Color for blessing text
     */
    public BattleRush(int neededFavor, int cooldown, boolean isPassive, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isPassive, isActive, canToggle, textColor);
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
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 0, false, false, true));
        int duration = BlessingSpecializations.isActive(player, "dravak", "a") ? 140 : 100;
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, 0, false, false, true));
        if (!event.isCanceled()) {
            BlessingVisuals.at(instance, player, player.position().add(0, 0.08, 0), Cue.TRAIL, 0.8);
        }
        if (BlessingSpecializations.isActive(player, "dravak", "b")) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 0));
            if (!event.isCanceled()) {
                BlessingVisuals.pulse(instance, player, Cue.WARD);
            }
        }
        return true;
    }
}
