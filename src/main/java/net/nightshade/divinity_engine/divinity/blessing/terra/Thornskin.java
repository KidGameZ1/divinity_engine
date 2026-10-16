package net.nightshade.divinity_engine.divinity.blessing.terra;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals.Cue;
import net.nightshade.divinity_engine.util.divinity.narrative.BlessingSpecializations;

import java.awt.*;
import java.util.Random;

public class Thornskin extends Blessings {
    private static final Random random = new Random();
    private static final ThreadLocal<Boolean> RETALIATING = ThreadLocal.withInitial(() -> false);


    public Thornskin(int neededFavor, int cooldown, boolean isPassive, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isPassive, isActive, canToggle, textColor);
    }

    @Override
    public boolean onTakenDamage(BlessingsInstance instance, LivingDamageEvent event) {
        if (event.isCanceled() || event.getAmount() <= 0 || event.getEntity().level().isClientSide
                || event.getSource().is(DamageTypes.THORNS) || RETALIATING.get()) return false;
        if (event.getSource().getEntity() instanceof LivingEntity attacker && attacker != event.getEntity()) {
            float damage = 1.0f + random.nextFloat()
                    + (BlessingSpecializations.isActive(event.getEntity(), "terra", "b") ? 1.0f : 0);
            RETALIATING.set(true);
            try {
                boolean hit = attacker.hurt(attacker.damageSources().thorns(event.getEntity()), damage);
                if (hit && BlessingSpecializations.isActive(event.getEntity(), "terra", "a")) {
                    attacker.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0));
                }
                if (hit) {
                    BlessingVisuals.link(instance, event.getEntity(), event.getEntity().getBoundingBox().getCenter(),
                            attacker.getBoundingBox().getCenter(), Cue.IMPACT);
                }
                return true;
            } finally {
                RETALIATING.remove();
            }
        }
        return super.onTakenDamage(instance, event);
    }
}
