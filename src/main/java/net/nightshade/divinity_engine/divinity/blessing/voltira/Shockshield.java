package net.nightshade.divinity_engine.divinity.blessing.voltira;

import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals.Cue;

import java.awt.*;
import java.util.Random;

public class Shockshield extends Blessings {
    private static final Random random = new Random();

    public Shockshield(int neededFavor, int cooldown, boolean isPassive, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isPassive, isActive, canToggle, textColor);
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
            if (source.hurt(event.getEntity().damageSources().lightningBolt(), 4.0f)) {
                BlessingVisuals.link(instance, event.getEntity(), event.getEntity().getBoundingBox().getCenter(),
                        source.getBoundingBox().getCenter(), Cue.WARD);
                BlessingVisuals.at(instance, event.getEntity(), source.getBoundingBox().getCenter(), Cue.IMPACT,
                        Math.max(0.75, source.getBbWidth()));
            }
            return true;
        }
        return super.onTakenDamage(instance, event);
    }
}
