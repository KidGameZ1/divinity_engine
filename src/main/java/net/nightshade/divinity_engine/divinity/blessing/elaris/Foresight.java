package net.nightshade.divinity_engine.divinity.blessing.elaris;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals.Cue;
import net.nightshade.divinity_engine.util.divinity.narrative.BlessingSpecializations;

import java.awt.*;


public class Foresight extends Blessings {

    /**
     * Constructs a new Foresight blessing.
     *
     * @param neededFavor Amount of favor required to activate the blessing
     * @param cooldown    Duration before the blessing can be used again
     * @param isPassive    Current state of the blessing
     * @param canToggle   Whether the blessing can be toggled on/off
     * @param textColor   Color used for blessing text display
     */
    public Foresight(int neededFavor, int cooldown, boolean isPassive, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isPassive, isActive, canToggle, textColor);
    }

    @Override
    public <T extends BlessingsInstance> T createDefaultInstance() {
        T instance = super.createDefaultInstance();
        if (!instance.getOrCreateTag().contains("dodge_count"))
            instance.getOrCreateTag().putInt("dodge_count", 0);
        return instance;
    }

    /**
     *
     *
     *
     * @param instance The blessing instance
     * @param living   The living entity affected by the blessing
     * @return true activates the cooldown
     */
    @Override
    public boolean onProjectileHit(BlessingsInstance instance, LivingEntity living, ProjectileImpactEvent event) {
        if (event.isCanceled() || living.level().isClientSide || event.getProjectile().getOwner() == living) return false;
        if (instance.getOrCreateTag().getInt("dodge_count") < (BlessingSpecializations.isActive(living, "elaris", "a") ? 4 : 3)) {
            event.setCanceled(true);
            Vec3 incoming = event.getProjectile().getDeltaMovement().normalize().scale(0.3);
            event.getProjectile().setDeltaMovement(0,0,0);
            Vec3 contact = event.getRayTraceResult().getLocation();
            BlessingVisuals.link(instance, living, contact, contact.add(incoming), Cue.CONTROL);
            instance.getOrCreateTag().putInt("dodge_count", instance.getOrCreateTag().getInt("dodge_count")+1);
            instance.markDirty();
        } else {
            instance.getOrCreateTag().putInt("dodge_count", 0);
            instance.markDirty();
            return true;
        }
        return super.onProjectileHit(instance, living, event);
    }

    @Override
    public boolean onTakenDamage(BlessingsInstance instance, LivingDamageEvent event) {
        if (event.isCanceled() || event.getEntity().level().isClientSide || event.getAmount() <= 0) return false;
        if (instance.getOrCreateTag().getInt("dodge_count") < (BlessingSpecializations.isActive(event.getEntity(), "elaris", "a") ? 4 : 3)) {
            event.setCanceled(true);
            if (!event.getEntity().isInvisible()) BlessingVisuals.pulse(instance, event.getEntity(), Cue.WARD);
            instance.getOrCreateTag().putInt("dodge_count", instance.getOrCreateTag().getInt("dodge_count")+1);
            instance.markDirty();
        } else {
            instance.getOrCreateTag().putInt("dodge_count", 0);
            instance.markDirty();
            return true;
        }
        return super.onTakenDamage(instance, event);
    }
}
