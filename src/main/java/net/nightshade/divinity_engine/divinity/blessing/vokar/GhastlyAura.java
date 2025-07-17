package net.nightshade.divinity_engine.divinity.blessing.vokar;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;

import java.awt.*;

public class GhastlyAura extends Blessings {
    public GhastlyAura(int neededFavor, int cooldown, boolean isPassive, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isPassive, isActive, canToggle, textColor);
    }

    @Override
    public boolean onTick(BlessingsInstance instance, LivingEntity living) {
        living.level().getEntitiesOfClass(LivingEntity.class, living.getBoundingBox().inflate(6.0D)).forEach(entity -> {
            if (entity != living && !entity.isAlliedTo(living)) {

                entity.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 40, 0, false, false, true));
                entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, false, false, true));
                entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, false, true));
            }
        });
        return true;
    }
}
