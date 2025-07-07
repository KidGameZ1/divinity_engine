package net.nightshade.divinity_engine.divinity.blessing.kairoth;

import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;

import java.awt.*;

/**
 * A blessing that allows the entity to return to their previous position.
 * Records the entity's position every 10 seconds (200 ticks) and allows
 * teleportation back to the last recorded position when activated.
 */
public class EchoStep extends Blessings {
    /**
     * Creates a new EchoStep blessing.
     *
     * @param neededFavor The favor cost to use this blessing
     * @param cooldown    The cooldown period in ticks
     * @param isActive    Whether the blessing is initially active
     * @param canToggle   Whether the blessing can be toggled on/off
     * @param textColor   The color used for blessing text in UI
     */
    public EchoStep(int neededFavor, int cooldown, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isActive, canToggle, textColor);
    }

    /**
     * Called every tick to update the blessing state.
     * Records the entity's position every 200 ticks (10 seconds).
     *
     * @param instance The blessing instance containing saved data
     * @param living   The entity that has this blessing
     * @return true if the tick processing succeeded
     */
    @Override
    public boolean onTick(BlessingsInstance instance, LivingEntity living) {
        if (instance.getOrCreateTag().getInt("tickCounter") >= 200) {
            System.out.println("save position: " + living.getX() + " " + living.getY() + " " + living.getZ() + "");
            instance.getOrCreateTag().putDouble("oldPositionX", living.getX());
            instance.getOrCreateTag().putDouble("oldPositionY", living.getY());
            instance.getOrCreateTag().putDouble("oldPositionZ", living.getZ());
            instance.getOrCreateTag().putInt("tickCounter", 0);
        } else {
            instance.getOrCreateTag().putInt("tickCounter", instance.getOrCreateTag().getInt("tickCounter") + 1);
        }
        return super.onTick(instance, living);
    }

    /**
     * Triggered when the blessing is activated.
     * Teleports the entity back to their last recorded position if one exists.
     *
     * @param instance The blessing instance containing saved position data
     * @param living   The entity to teleport
     * @return true if teleportation was successful
     */
    @Override
    public boolean onPressed(BlessingsInstance instance, LivingEntity living) {
        if (instance.getTag() != null) {
            if (instance.getTag().getDouble("oldPositionX") != 0 && instance.getTag().getDouble("oldPositionY") != 0 && instance.getTag().getDouble("oldPositionZ") != 0) {}
                living.setPos(instance.getOrCreateTag().getDouble("oldPositionX"), instance.getOrCreateTag().getDouble("oldPositionY"), instance.getOrCreateTag().getDouble("oldPositionZ"));
            return true;
        }
        return super.onPressed(instance, living);
    }
}
