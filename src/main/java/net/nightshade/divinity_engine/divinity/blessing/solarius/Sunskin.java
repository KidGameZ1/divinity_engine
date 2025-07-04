package net.nightshade.divinity_engine.divinity.blessing.solarius;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;

import java.awt.*;

public class Sunskin extends Blessings {
    public Sunskin(int neededFavor, int cooldown, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isActive, canToggle, textColor);
    }

    @Override
    public boolean onTick(BlessingsInstance instance, LivingEntity living) {

        if (living.level().isDay()){
            if (living.getHealth() < living.getMaxHealth()){
                living.heal(0.02f);
            }
        }

        return false;
    }
}
