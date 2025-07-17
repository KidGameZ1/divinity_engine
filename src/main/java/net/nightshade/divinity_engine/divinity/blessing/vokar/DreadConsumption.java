package net.nightshade.divinity_engine.divinity.blessing.vokar;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;

import java.awt.*;

public class DreadConsumption extends Blessings {
    /**
     * Creates a new blessing with the specified parameters.
     *
     * @param neededFavor Amount of favor required to activate
     * @param cooldown    Cooldown period in seconds
     * @param isPassive   Is a passive blessing or not
     * @param canToggle   Whether blessing can be toggled
     * @param textColor   Color for blessing text
     */
    public DreadConsumption(int neededFavor, int cooldown, boolean isPassive, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isPassive, isActive, canToggle, textColor);
    }

    @Override
    public boolean onKillEntity(BlessingsInstance instance, LivingEntity player, LivingDeathEvent event) {

        if(event.getEntity().hasEffect(MobEffects.WEAKNESS)){
            float currentHealth = player.getHealth();
            float maxHealth = player.getMaxHealth();
            if (currentHealth < maxHealth) {
                player.setHealth(Math.min(currentHealth + 2.0F, maxHealth));
                if (player.level() instanceof ServerLevel serverLevel)
                    serverLevel.sendParticles(ParticleTypes.SOUL, player.getX(), player.getY() + 1, player.getZ(), 25, 0.5, 0.5, 0.5, 0.1);
                return true;
            }
        }

        return super.onKillEntity(instance, player, event);
    }
}
