package net.nightshade.divinity_engine.divinity.blessing.cryonis;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals;
import net.nightshade.divinity_engine.util.divinity.narrative.BlessingSpecializations;

import java.awt.Color;
import java.util.Map;
import java.util.WeakHashMap;

public class Snowveil extends Blessings {
    private static final int REFRESH_TICKS = 6;
    // Remember the actual effect object so another source's invisibility is never removed.
    private static final Map<LivingEntity, MobEffectInstance> OWNED = new WeakHashMap<>();
    private static final Map<LivingEntity, Integer> REVEALED_UNTIL = new WeakHashMap<>();

    public Snowveil(int favor, int cooldown, boolean passive, boolean active, boolean toggle, Color color) {
        super(favor, cooldown, passive, active, toggle, color);
    }

    @Override
    public boolean onTakenDamage(BlessingsInstance instance, LivingDamageEvent event) {
        if (!event.isCanceled() && event.getAmount() > 0) reveal(event.getEntity());
        return false;
    }

    @Override
    public boolean onDamageEntity(BlessingsInstance instance, LivingEntity living, LivingHurtEvent event) {
        if (!event.isCanceled() && event.getAmount() > 0) reveal(living);
        return false;
    }

    private static void reveal(LivingEntity living) {
        if (living.level().isClientSide) return;
        REVEALED_UNTIL.put(living, living.tickCount + 40);
        removeOwned(living);
    }

    private static void removeOwned(LivingEntity living) {
        var owned = OWNED.remove(living);
        var current = living.getEffect(MobEffects.INVISIBILITY);
        if (owned != null && current == owned && current.getDuration() <= REFRESH_TICKS
                && current.getAmplifier() == 0 && !current.isVisible() && !current.showIcon())
            living.removeEffect(MobEffects.INVISIBILITY);
    }

    @Override
    public boolean onTick(BlessingsInstance instance, LivingEntity living) {
        if (living.level().isClientSide) return false;
        var pos = living.blockPosition();
        var world = living.level();
        boolean snow = world.getBiome(pos).value().coldEnoughToSnow(pos)
                || world.getBlockState(pos).is(Blocks.SNOW) || world.getBlockState(pos).is(Blocks.POWDER_SNOW)
                || world.getBlockState(pos.below()).is(Blocks.SNOW_BLOCK)
                || world.getBlockState(pos.below()).is(Blocks.SNOW);
        if (living.swinging) reveal(living);
        if (!snow || living.isSpectator() || !BlessingSpecializations.isEligible(living, instance)
                || living.tickCount < REVEALED_UNTIL.getOrDefault(living, 0)) {
            removeOwned(living);
            return false;
        }
        var existing = living.getEffect(MobEffects.INVISIBILITY);
        if (existing == null || existing == OWNED.get(living) && existing.getDuration() <= REFRESH_TICKS) {
            boolean entering = existing == null;
            living.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, REFRESH_TICKS, 0, false, false, false));
            OWNED.put(living, living.getEffect(MobEffects.INVISIBILITY));
            if (entering) BlessingVisuals.at(instance, living, living.position().add(0, living.getBbHeight() * .45, 0),
                    BlessingVisuals.Cue.CAST, .65);
        }
        // Without refresh, only our six-tick effect expires when the blessing is unequipped.
        return false;
    }
}
