package net.nightshade.divinity_engine.divinity.blessing.grond;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.TieredItem;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;

import java.awt.*;

/**
 * A blessing that enhances damage dealt with tiered weapons.
 * Increases damage by a flat amount when attacking with proper weapons.
 */
public class Hammerhand extends Blessings {
    /**
     * Creates a new Hammerhand blessing instance.
     *
     * @param neededFavor Amount of favor required to activate the blessing
     * @param cooldown    Cooldown duration in ticks
     * @param isPassive    Whether the blessing is currently active
     * @param canToggle   Whether the blessing can be toggled on/off
     * @param textColor   Color used for blessing text in UI
     */
    public Hammerhand(int neededFavor, int cooldown, boolean isPassive, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isPassive, isActive, canToggle, textColor);
    }

    /**
     * Handles damage modification when the player attacks an entity.
     * Adds 3 extra damage when attacking with a tiered weapon.
     *
     * @param instance The active blessing instance
     * @param player   The player dealing damage
     * @param event    The damage event
     * @return true if the event was modified
     */
    @Override
    public boolean onDamageEntity(BlessingsInstance instance, LivingEntity player, LivingHurtEvent event) {

        if (player.getMainHandItem().getItem() instanceof TieredItem)
            event.setAmount(event.getAmount() + 3);

        return super.onDamageEntity(instance, player, event);
    }
}
