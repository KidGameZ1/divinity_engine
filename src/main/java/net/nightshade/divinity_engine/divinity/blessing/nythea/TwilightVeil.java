package net.nightshade.divinity_engine.divinity.blessing.nythea;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;

import java.awt.*;
import java.util.UUID;

public class TwilightVeil extends Blessings {
    public TwilightVeil(int neededFavor, int cooldown, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isActive, canToggle, textColor);
    }

    @Override
    public boolean onTick(BlessingsInstance instance, LivingEntity living) {
        Level level = living.level();
        boolean isNight = !level.isDay();

        UUID speedModifierUUID = UUID.fromString("7E0E1E5E-8C0D-4F9A-B8A1-F9A9E9B9E9E9");
        AttributeModifier speedModifier = new AttributeModifier(speedModifierUUID, "Twilight Speed", 0.1, AttributeModifier.Operation.MULTIPLY_TOTAL);

        if (isNight) {
            if (!living.getAttribute(Attributes.MOVEMENT_SPEED).hasModifier(speedModifier)) {
                living.getAttribute(Attributes.MOVEMENT_SPEED).addTransientModifier(speedModifier);
            }
            if (living.isShiftKeyDown() && !living.hasEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY)) {
                living.setInvisible(true);
            } else {
                living.setInvisible(false);
            }
        } else {
            living.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(speedModifier);
            if (!living.hasEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY)) {}
                living.setInvisible(false);
        }


        return super.onTick(instance, living);
    }
}
