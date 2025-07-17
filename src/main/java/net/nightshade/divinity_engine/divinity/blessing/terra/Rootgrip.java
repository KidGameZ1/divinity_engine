package net.nightshade.divinity_engine.divinity.blessing.terra;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;

import java.awt.*;

public class Rootgrip extends Blessings {
    /**
     * Creates a new blessing with the specified parameters.
     *
     * @param neededFavor Amount of favor required to activate
     * @param cooldown    Cooldown period in seconds
     * @param isPassive   Is a passive blessing or not
     * @param canToggle   Whether blessing can be toggled
     * @param textColor   Color for blessing text
     */
    public Rootgrip(int neededFavor, int cooldown, boolean isPassive, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isPassive, isActive, canToggle, textColor);
    }

    @Override
    public <T extends BlessingsInstance> T createDefaultInstance() {
        T instance = super.createDefaultInstance();
        if (instance.getOrCreateTag().contains("hit_count"))
            instance.getOrCreateTag().putInt("hit_count", 0);
        return super.createDefaultInstance();
    }

    @Override
    public boolean onTick(BlessingsInstance instance, LivingEntity living) {
        if (instance.getOrCreateTag().getInt("hit_count") >= 5) {
            instance.getOrCreateTag().putInt("hit_count", 0);
            return true;
        }
        return super.onTick(instance, living);
    }

    @Override
    public boolean onDamageEntity(BlessingsInstance instance, LivingEntity player, LivingHurtEvent event) {
        if (instance.getOrCreateTag().getInt("hit_count") < 5) {
            LivingEntity attacker = event.getEntity();
            if (!attacker.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) {
                attacker.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1, false, false, true));
                instance.getOrCreateTag().putInt("hit_count", instance.getOrCreateTag().getInt("hit_count") + 1);
            }
        }
        return super.onDamageEntity(instance, player, event);
    }
}
