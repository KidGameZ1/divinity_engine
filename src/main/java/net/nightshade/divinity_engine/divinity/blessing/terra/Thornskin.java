package net.nightshade.divinity_engine.divinity.blessing.terra;

import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class Thornskin extends Blessings {
    private static final Random random = new Random();


    public Thornskin(int neededFavor, int cooldown, boolean isPassive, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isPassive, isActive, canToggle, textColor);
    }

    @Override
    public boolean onTakenDamage(BlessingsInstance instance, LivingDamageEvent event) {
        if (event.getSource().getEntity() instanceof LivingEntity attacker) {;
            float damage = 1.0f + random.nextFloat();
            attacker.hurt(attacker.damageSources().thorns(event.getEntity()), damage);
            return true;
        }
        return super.onTakenDamage(instance, event);
    }
}
