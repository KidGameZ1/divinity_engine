package net.nightshade.divinity_engine.divinity.blessing.elaris;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;

import java.awt.*;


/**
 * A blessing that provides additional experience orbs when killing entities.
 * This blessing extends the base Curse class to enhance kill rewards.
 */
public class InsightfulKill extends Blessings {
    /**
     * Constructs a new InsightfulKill blessing.
     *
     * @param neededFavor Amount of favor required to activate the blessing
     * @param cooldown    Duration before the blessing can be used again
     * @param isActive    Current state of the blessing
     * @param canToggle   Whether the blessing can be toggled on/off
     * @param textColor   Color used for blessing text display
     */
    public InsightfulKill(int neededFavor, int cooldown, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isActive, canToggle, textColor);
    }

    /**
     * Handles entity kill events by spawning additional experience orbs.
     * Adds 3 extra experience points to the normal experience drop.
     *
     * @param instance The blessing instance
     * @param player   The player entity that triggered the kill
     * @param event    The entity death event
     * @return Result from parent class implementation
     */
    @Override
    public boolean onKillEntity(BlessingsInstance instance, LivingEntity player, LivingDeathEvent event) {
        if (player instanceof Player playerEntity) {
            int experience = event.getEntity().getExperienceReward();
            Level level = event.getEntity().level();
            if (level instanceof ServerLevel _level)
                _level.addFreshEntity(new ExperienceOrb(_level, event.getEntity().getX(), event.getEntity().getY(), event.getEntity().getZ(), experience+3));
        }
        return super.onKillEntity(instance, player, event);
    }
}
