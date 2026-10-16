package net.nightshade.divinity_engine.divinity.blessing.vokar;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals.Cue;
import net.nightshade.divinity_engine.util.divinity.narrative.BlessingSpecializations;

import java.awt.*;

public class SoulLeech extends Blessings {
    public SoulLeech(int neededFavor, int cooldown, boolean isPassive, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isPassive, isActive, canToggle, textColor);
    }

    @Override
    public boolean onKillEntity(BlessingsInstance instance, LivingEntity player, LivingDeathEvent event) {
        float currentHealth = player.getHealth();
        float maxHealth = player.getMaxHealth();
        if (currentHealth < maxHealth) {
            float healing = BlessingSpecializations.isActive(player, "vokar", "a") ? 3.0F : 2.0F;
            player.setHealth(Math.min(currentHealth + healing, maxHealth));
            if (!event.isCanceled()) {
                BlessingVisuals.link(instance, player, event.getEntity().getBoundingBox().getCenter(),
                        player.getBoundingBox().getCenter(), Cue.HEAL);
            }
        }
        if (BlessingSpecializations.isActive(player, "vokar", "b")) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 0));
            if (!event.isCanceled()) {
                BlessingVisuals.at(instance, player, player.position().add(0, 0.08, 0), Cue.TRAIL, 0.75);
            }
        }
        return true;
    }
}
