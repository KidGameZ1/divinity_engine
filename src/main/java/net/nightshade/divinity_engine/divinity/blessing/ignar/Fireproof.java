package net.nightshade.divinity_engine.divinity.blessing.ignar;

import net.minecraft.world.damagesource.DamageTypes;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;

import java.awt.*;

/**
 * Represents a blessing that grants immunity to all fire-related damage sources.
 * This blessing cancels damage from being in fire, standing on fire, walking on hot blocks,
 * and being in lava.
 */
public class Fireproof extends Blessings {
    /**
     * Constructs a new Fireproof blessing with specified parameters.
     *
     * @param neededFavor The amount of favor points required to activate this blessing
     * @param cooldown    The cooldown duration in ticks before the blessing can be used again
     * @param isActive    Whether the blessing starts in an active state
     * @param canToggle   Whether the blessing can be toggled on and off by the player
     * @param textColor   The color used to display this blessing in the UI
     */
    public Fireproof(int neededFavor, int cooldown, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isActive, canToggle, textColor);
    }

    /**
     * Handles incoming damage events for entities with this blessing.
     * Cancels all fire-related damage types including direct fire damage, fire damage over time,
     * hot floor damage, and lava damage.
     *
     * @param instance The blessing instance associated with the entity
     * @param event    The damage event containing damage type and amount information
     * @return true if the damage event was handled, false otherwise
     */
    @Override
    public boolean onTakenDamage(BlessingsInstance instance, LivingDamageEvent event) {
        if (event.getSource().is(DamageTypes.IN_FIRE) || event.getSource().is(DamageTypes.ON_FIRE) || event.getSource().is(DamageTypes.HOT_FLOOR) || event.getSource().is(DamageTypes.LAVA)) {
            event.setCanceled(true);
        }

        return super.onTakenDamage(instance, event);
    }
}