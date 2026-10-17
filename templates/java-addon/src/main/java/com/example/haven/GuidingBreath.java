package com.example.haven;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;

public final class GuidingBreath extends Blessings {
    public GuidingBreath() {
        // Favor, cooldown SECONDS, passive, active, toggle, text color.
        super(50, 12, false, true, false, HavenAddon.COLOR);
    }

    @Override
    public boolean onPressed(BlessingsInstance instance, LivingEntity owner) {
        if (owner.level().isClientSide || !owner.isInWater()) return false;
        float before = owner.getHealth();
        if (before >= owner.getMaxHealth()) return false;
        owner.heal(2.0F);
        if (owner.getHealth() <= before) return false;
        var data = instance.getOrCreateTag();
        data.putInt("haven_successful_heals", Math.min(1_000_000, data.getInt("haven_successful_heals") + 1));
        instance.markDirty();
        owner.level().playSound(null, owner.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS, 0.5F, 1.2F);
        // The blessing dispatcher starts the configured cooldown once on success.
        return true;
    }
}
