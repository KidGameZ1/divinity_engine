package net.nightshade.divinity_engine.divinity.blessing.vokar;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;

import java.awt.*;

public class Wraithwalk extends Blessings {
    public Wraithwalk(int neededFavor, int cooldown, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isActive, canToggle, textColor);
    }

    @Override
    public boolean onTakenDamage(BlessingsInstance instance, LivingDamageEvent event) {
        if (event.getEntity().isAlive()) {
            event.getEntity().addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 100, 0, false, false, true));
            return true;
        }
        return super.onTakenDamage(instance, event);
    }
}
