package net.nightshade.divinity_engine.divinity.blessing.mechanos;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals.Cue;

import java.awt.*;

public class OverdriveLoop extends Blessings {
    /**
     * Creates a new blessing with the specified parameters.
     *
     * @param neededFavor Amount of favor required to activate
     * @param cooldown    Cooldown period in seconds
     * @param isPassive   Is a passive blessing or not
     * @param canToggle   Whether blessing can be toggled
     * @param textColor   Color for blessing text
     */
    public OverdriveLoop(int neededFavor, int cooldown, boolean isPassive, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isPassive, isActive, canToggle, textColor);
    }

    @Override
    public boolean onTick(BlessingsInstance instance, LivingEntity living) {
        if (!(living instanceof Player player)) return false;
        if (player.level().isClientSide) return false;

        CompoundTag tag = instance.getOrCreateTag();
        long currentTime = player.level().getGameTime();

        boolean active = player.isSprinting() || player.swinging || player.containerMenu != null;

        if (active) {
            tag.putLong("overdrive_active_until", currentTime + 200); // 10 sec duration
        }

        if (tag.getLong("overdrive_active_until") > currentTime) {
            // Apply mining speed and movement speed
            applyOrRefreshEffect(player, MobEffects.DIG_SPEED, 40, 0);
            applyOrRefreshEffect(player, MobEffects.MOVEMENT_SPEED, 40, 0);
            if (player.isSprinting()) {
                BlessingVisuals.at(instance, player, player.position().add(0, 0.08, 0)
                        .subtract(player.getDeltaMovement().multiply(1, 0, 1).normalize().scale(0.18)), Cue.TRAIL, 0.7);
            } else {
                BlessingVisuals.at(instance, player, player.position().add(0, player.getBbHeight() * 0.55, 0), Cue.AURA, 0.65);
            }
        }

        return false;
    }

    private void applyOrRefreshEffect(Player player, MobEffect effect, int duration, int amplifier) {
        MobEffectInstance current = player.getEffect(effect);
        if (current == null || current.getDuration() < duration / 2) {
            player.addEffect(new MobEffectInstance(effect, duration, amplifier, false, true, true));
        }
    }

}
