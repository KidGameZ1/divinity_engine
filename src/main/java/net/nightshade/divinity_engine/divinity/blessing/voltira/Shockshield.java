package net.nightshade.divinity_engine.divinity.blessing.voltira;

import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;

import java.awt.*;
import java.util.Random;

public class Shockshield extends Blessings {
    private static final Random random = new Random();

    public Shockshield(int neededFavor, int cooldown, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isActive, canToggle, textColor);
    }

    @Override
    public <T extends BlessingsInstance> T createDefaultInstance() {
        T instance = super.createDefaultInstance();
        if (!instance.getOrCreateTag().contains("activation_chance"))
            instance.getOrCreateTag().putFloat("activation_chance", 0.3f);
        return instance;
    }

    @Override
    public boolean onTakenDamage(BlessingsInstance instance, LivingDamageEvent event) {
        Entity source = event.getSource().getEntity();
        if (source != null && random.nextFloat() <= instance.getOrCreateTag().getFloat("activation_chance")) {
            source.hurt(event.getEntity().damageSources().lightningBolt(), 4.0f);
            return true;
        }
        return super.onTakenDamage(instance, event);
    }
}
