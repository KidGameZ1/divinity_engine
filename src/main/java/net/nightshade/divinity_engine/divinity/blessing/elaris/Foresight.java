package net.nightshade.divinity_engine.divinity.blessing.elaris;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.nightshade_core.util.MiscHelper;

import java.awt.*;


public class Foresight extends Blessings {

    /**
     * Constructs a new Foresight blessing.
     *
     * @param neededFavor Amount of favor required to activate the blessing
     * @param cooldown    Duration before the blessing can be used again
     * @param isActive    Current state of the blessing
     * @param canToggle   Whether the blessing can be toggled on/off
     * @param textColor   Color used for blessing text display
     */
    public Foresight(int neededFavor, int cooldown, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isActive, canToggle, textColor);
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
        if (instance.getOrCreateTag().getInt("dodge_count") < 3) {
            event.setCanceled(true);
            event.getProjectile().setDeltaMovement(0,0,0);
            instance.getOrCreateTag().putInt("dodge_count", instance.getOrCreateTag().getInt("dodge_count")+1);
        } else {
            instance.getOrCreateTag().putInt("dodge_count", 0);
            return true;
        }
        return super.onProjectileHit(instance, living, event);
    }

    @Override
    public boolean onTakenDamage(BlessingsInstance instance, LivingDamageEvent event) {
        if (instance.getOrCreateTag().getInt("dodge_count") < 3) {
            event.setCanceled(true);
            instance.getOrCreateTag().putInt("dodge_count", instance.getOrCreateTag().getInt("dodge_count")+1);
        } else {
            instance.getOrCreateTag().putInt("dodge_count", 0);
            return true;
        }
        return super.onTakenDamage(instance, event);
    }
}
