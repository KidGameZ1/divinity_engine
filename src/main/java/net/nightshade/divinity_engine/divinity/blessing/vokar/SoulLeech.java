package net.nightshade.divinity_engine.divinity.blessing.vokar;

import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;

import java.awt.*;
import java.util.Random;

public class SoulLeech extends Blessings {
    public SoulLeech(int neededFavor, int cooldown, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isActive, canToggle, textColor);
    }

    @Override
    public boolean onKillEntity(BlessingsInstance instance, LivingEntity player, LivingDeathEvent event) {
        float currentHealth = player.getHealth();
        float maxHealth = player.getMaxHealth();
        if (currentHealth < maxHealth) {
            player.setHealth(Math.min(currentHealth + 2.0F, maxHealth));
        }
        return true;
    }
}
