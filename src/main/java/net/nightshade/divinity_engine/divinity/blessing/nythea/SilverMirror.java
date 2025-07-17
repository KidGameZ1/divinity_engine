package net.nightshade.divinity_engine.divinity.blessing.nythea;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.nightshade_core.util.MiscHelper;

import java.awt.*;

public class SilverMirror extends Blessings {
    /**
     * Creates a new blessing with the specified parameters.
     *
     * @param neededFavor Amount of favor required to activate
     * @param cooldown    Cooldown period in seconds
     * @param isPassive   Is a passive blessing or not
     * @param canToggle   Whether blessing can be toggled
     * @param textColor   Color for blessing text
     */
    public SilverMirror(int neededFavor, int cooldown, boolean isPassive, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isPassive, isActive, canToggle, textColor);
    }

    @Override
    public <T extends BlessingsInstance> T createDefaultInstance() {
        T instance = super.createDefaultInstance();
        if (!instance.getOrCreateTag().contains("reflect_count"))
            instance.getOrCreateTag().putInt("reflect_count", 0);
        if (!instance.getOrCreateTag().contains("activation_time"))
            instance.getOrCreateTag().putLong("activation_time", 0);
        if (!instance.getOrCreateTag().contains("max_reflects"))
            instance.getOrCreateTag().putInt("max_reflects", 5);
        if (!instance.getOrCreateTag().contains("duration"))
            instance.getOrCreateTag().putInt("duration", MiscHelper.secondsToTick(20));
        return instance;
    }

    @Override
    public void onToggleOn(BlessingsInstance instance, LivingEntity entity) {
        instance.getOrCreateTag().putLong("activation_time", System.currentTimeMillis());
        instance.getOrCreateTag().putInt("reflect_count", 0);
        entity.level().addParticle(ParticleTypes.ENCHANT,
                entity.getX(), entity.getY() + 2, entity.getZ(),
                0, 0, 0);
        super.onToggleOn(instance, entity);
    }

    @Override
    public boolean onToggleOff(BlessingsInstance instance, LivingEntity entity) {
        instance.getOrCreateTag().putInt("reflect_count", 0);
        instance.getOrCreateTag().putLong("activation_time", 0);
        instance.getOrCreateTag().putInt("duration", MiscHelper.secondsToTick(20));

        return super.onToggleOff(instance, entity);
    }

    @Override
    public boolean onTakenDamage(BlessingsInstance instance, LivingDamageEvent event) {
        if(!instance.isToggled()) return false;
        long currentTime = System.currentTimeMillis();
        long activationTime = instance.getOrCreateTag().getLong("activation_time");
        int reflectCount = instance.getOrCreateTag().getInt("reflect_count");
        int maxReflects = instance.getOrCreateTag().getInt("max_reflects");
        int duration = instance.getOrCreateTag().getInt("duration");
        if (currentTime - activationTime <= duration && reflectCount < maxReflects) {
            if (event.getSource().getDirectEntity() instanceof LivingEntity living) {
                instance.getOrCreateTag().putInt("reflect_count", reflectCount + 1);

                event.setAmount(event.getAmount() * 0.75f);
                living.hurt(event.getSource(), event.getAmount()*0.25f);
                living.level().addParticle(ParticleTypes.ENCHANT,
                        living.getX(), living.getY() + 1, living.getZ(),
                        0, 0, 0);
                living.playSound(SoundEvents.ENCHANTMENT_TABLE_USE, 1.0F, 2.0F);

            }
        }else if (reflectCount == maxReflects) {
            instance.setToggled(false);
            return true;
        }
        return super.onTakenDamage(instance, event);
    }
}
