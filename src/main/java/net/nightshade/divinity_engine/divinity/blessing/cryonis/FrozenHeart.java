package net.nightshade.divinity_engine.divinity.blessing.cryonis;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;

import java.awt.*;

/**
 * A blessing that prevents the entity from being frozen.
 * Extends the base Curse class to provide immunity to freezing effects.
 */
public class FrozenHeart extends Blessings {
    /**
     * Creates a new FrozenHeart blessing.
     *
     * @param neededFavor Amount of favor required to activate this blessing
     * @param cooldown    Cooldown period between activations in seconds
     * @param isActive    Whether this blessing starts active
     * @param canToggle   Whether this blessing can be toggled on/off
     * @param textColor   Color used for blessing text in UI
     */
    public FrozenHeart(int neededFavor, int cooldown, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isActive, canToggle, textColor);
    }

    /**
     * Called every tick while the blessing is active.
     * Prevents the entity from being frozen by setting frozen ticks to 0.
     *
     * @param instance The blessing instance
     * @param living   The affected living entity
     * @return True if the tick was handled
     */
    @Override
    public boolean onTick(BlessingsInstance instance, LivingEntity living) {
        living.setTicksFrozen(0);
        return super.onTick(instance, living);
    }
}
