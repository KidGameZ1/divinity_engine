package net.nightshade.divinity_engine.divinity.blessing.aethon;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals.Cue;
import net.nightshade.divinity_engine.util.divinity.narrative.BlessingSpecializations;

import java.awt.*;

/**
 * A blessing that provides protection against incoming projectiles by creating a celestial barrier.
 * When activated, it can block projectiles and create visual/audio effects.
 */
public class CelestialWard extends Blessings {
    private static final String READY_AT = "divinity_engine.celestial_ward_ready_at";

    /**
     * Creates a new CelestialWard blessing
     *
     * @param neededFavor Amount of favor required to activate
     * @param cooldown    Cooldown period between uses
     * @param isPassive    Whether the blessing is currently active
     * @param canToggle   Whether the blessing can be toggled on/off
     * @param textColor   Color used for UI display
     */
    public CelestialWard(int neededFavor, int cooldown, boolean isPassive, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isPassive, isActive, canToggle, textColor);
    }

    /**
     * Handles projectile impact events, blocking incoming projectiles if conditions are met
     *
     * @param instance The active blessing instance
     * @param living   The entity with the blessing
     * @param event    The projectile impact event
     * @return true if the projectile was blocked, false otherwise
     */
    @Override
    public boolean onProjectileHit(BlessingsInstance instance, LivingEntity living, ProjectileImpactEvent event) {
        if (!(living instanceof ServerPlayer player) || event.isCanceled()
                || event.getProjectile().getOwner() == living || instance.isOnCooldown()
                || !(event.getRayTraceResult() instanceof EntityHitResult hit) || hit.getEntity() != living) return false;
        // One clock per save, even across dimension travel; the deadline belongs to the player, not the registry singleton.
        long now = player.server.overworld().getGameTime();
        if (now < Math.max(player.getPersistentData().getLong(READY_AT), instance.getOrCreateTag().getLong(READY_AT))) return false;
        long deadline = now + BlessingSpecializations.cooldownTicks(player, instance);
        player.getPersistentData().putLong(READY_AT, deadline);
        instance.getOrCreateTag().putLong(READY_AT, deadline);
        instance.markDirty();
        ServerLevel level = player.serverLevel();
        level.playSound(null, living.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.0F);
        Vec3 incoming = event.getProjectile().getDeltaMovement().normalize().scale(0.6);
        event.getProjectile().setDeltaMovement(0, 0, 0);
        event.setCanceled(true);
        BlessingVisuals.link(instance, living, hit.getLocation(), hit.getLocation().add(incoming), Cue.WARD);
        if (BlessingSpecializations.isActive(player, "aethon", "b")) {
            float healthBefore = player.getHealth();
            player.heal(1.0f);
            if (player.getHealth() > healthBefore && !player.isInvisible()) {
                BlessingVisuals.pulse(instance, player, Cue.HEAL);
            }
        }
        return true;
    }
}
