package net.nightshade.divinity_engine.divinity.blessing.mechanos;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals.Cue;
import net.nightshade.divinity_engine.util.divinity.narrative.BlessingSpecializations;

import java.awt.*;

public class ClockworkPunch extends Blessings {

    public ClockworkPunch(int neededFavor, int cooldown, boolean isPassive, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isPassive, isActive, canToggle, textColor);
    }

    @Override
    public <T extends BlessingsInstance> T createDefaultInstance() {
        T instance = super.createDefaultInstance();
        if (!instance.getOrCreateTag().contains("knockback_chance"))
            instance.getOrCreateTag().putFloat("knockback_chance", 0.3f);
        if (!instance.getOrCreateTag().contains("knockback_strength"))
            instance.getOrCreateTag().putDouble("knockback_strength", 2.0);
        return instance;
    }

    @Override
    public boolean onDamageEntity(BlessingsInstance instance, LivingEntity player, LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        float chance = instance.getOrCreateTag().getFloat("knockback_chance")
                + (BlessingSpecializations.isActive(player, "mechanos", "a") ? 0.1f : 0);
        if (player.getRandom().nextFloat() < Math.min(1.0f, chance)) {
            double strength = instance.getOrCreateTag().getDouble("knockback_strength")
                    * (BlessingSpecializations.isActive(player, "mechanos", "b") ? 1.25 : 1.0);
            Vec3 knockbackDir = target.position().subtract(player.position()).normalize();
            target.setDeltaMovement(knockbackDir.x * strength,
                    0.5,
                    knockbackDir.z * strength);
            target.hurtMarked = true;

            if (player.level() instanceof ServerLevel serverLevel) {
                if (!event.isCanceled() && event.getAmount() > 0) {
                    BlessingVisuals.at(instance, player, target.getBoundingBox().getCenter(), Cue.IMPACT, 1.25);
                }
                playPunchSound(serverLevel, target);
            }

            return true;
        }
        return super.onDamageEntity(instance, player, event);
    }

    private void playPunchSound(ServerLevel level, Entity target) {
        level.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.PISTON_EXTEND, SoundSource.PLAYERS,
                1.0F, 1.0F);
    }

}
