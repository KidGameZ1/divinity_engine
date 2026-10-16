package net.nightshade.divinity_engine.divinity.blessing.cryonis;

import net.minecraft.world.entity.LivingEntity;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals.Cue;

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
     * @param isPassive    Whether this blessing starts active
     * @param canToggle   Whether this blessing can be toggled on/off
     * @param textColor   Color used for blessing text in UI
     */
    public FrozenHeart(int neededFavor, int cooldown, boolean isPassive, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isPassive, isActive, canToggle, textColor);
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
        boolean thawed = living.getTicksFrozen() > 0;
        living.setTicksFrozen(0);
        if (thawed) {
            BlessingVisuals.pulse(instance, living, Cue.WARD);
        }
        return super.onTick(instance, living);
    }
}
