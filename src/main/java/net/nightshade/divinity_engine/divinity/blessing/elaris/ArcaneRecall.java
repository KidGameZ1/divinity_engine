package net.nightshade.divinity_engine.divinity.blessing.elaris;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.nightshade.divinity_engine.DivinityEngineMod;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;

import java.awt.*;

public class ArcaneRecall extends Blessings {
    /**
     * Creates a new blessing with the specified parameters.
     *
     * @param neededFavor Amount of favor required to activate
     * @param cooldown    Cooldown period in seconds
     * @param isPassive   Is a passive blessing or not
     * @param canToggle   Whether blessing can be toggled
     * @param textColor   Color for blessing text
     */
    public ArcaneRecall(int neededFavor, int cooldown, boolean isPassive, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isPassive, isActive, canToggle, textColor);
    }

    @Override
    public boolean onTick(BlessingsInstance instance, LivingEntity living) {
        if (instance.getOrCreateTag().getInt("tickCounter") >= 40) {
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
            if (instance.getTag().getDouble("oldPositionX") != 0 && instance.getTag().getDouble("oldPositionY") != 0 && instance.getTag().getDouble("oldPositionZ") != 0) {
                if (living.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.ENCHANT, instance.getTag().getDouble("oldPositionX"), instance.getTag().getDouble("oldPositionY") + 1, instance.getTag().getDouble("oldPositionZ"), 25, 0.5, 0.5, 0.5, 0.1);
                }
                living.setPos(instance.getTag().getDouble("oldPositionX"), instance.getTag().getDouble("oldPositionY"), instance.getTag().getDouble("oldPositionZ"));
                instance.getOrCreateTag().putDouble("oldPositionX", 0);
                instance.getOrCreateTag().putDouble("oldPositionY", 0);
                instance.getOrCreateTag().putDouble("oldPositionZ", 0);

                return true;
            }
        }
        return super.onPressed(instance, living);
    }
}
