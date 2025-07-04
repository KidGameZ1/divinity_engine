package net.nightshade.divinity_engine.divinity.blessing.elaris;

import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;

import java.awt.*;

/**
 * A blessing that increases damage against entities with full health.
 * This blessing provides a 15% damage bonus when attacking targets at maximum health.
 */
public class ScholarsEdge extends Blessings {
    /**
     * Constructs a new ScholarsEdge blessing.
     *
     * @param neededFavor Amount of favor required to activate the blessing
     * @param cooldown    Duration before the blessing can be used again
     * @param isActive    Current state of the blessing
     * @param canToggle   Whether the blessing can be toggled on/off
     * @param textColor   Color used for blessing text display
     */
    public ScholarsEdge(int neededFavor, int cooldown, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isActive, canToggle, textColor);
    }

    /**
     * Increases damage by 15% when attacking entities with full health.
     *
     * @param instance The blessing instance
     * @param player   The player entity dealing damage
     * @param event    The entity damage event
     * @return Result from parent class implementation
     */
    @Override
    public boolean onDamageEntity(BlessingsInstance instance, LivingEntity player, LivingHurtEvent event) {
        LivingEntity target = (LivingEntity) event.getEntity();
        if (target.getHealth() >= target.getMaxHealth()) {
            float currentDamage = event.getAmount();
            event.setAmount(currentDamage * 1.15f);
        }

        return super.onDamageEntity(instance, player, event);
    }
}
