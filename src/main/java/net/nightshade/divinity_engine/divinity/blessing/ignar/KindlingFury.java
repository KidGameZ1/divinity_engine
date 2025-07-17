package net.nightshade.divinity_engine.divinity.blessing.ignar;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;

import java.awt.*;
import java.util.Random;

public class KindlingFury extends Blessings {
    /**
     * Creates a new blessing with the specified parameters.
     *
     * @param neededFavor Amount of favor required to activate
     * @param cooldown    Cooldown period in seconds
     * @param isPassive   Is a passive blessing or not
     * @param canToggle   Whether blessing can be toggled
     * @param textColor   Color for blessing text
     */
    public KindlingFury(int neededFavor, int cooldown, boolean isPassive, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isPassive, isActive, canToggle, textColor);
    }

    @Override
    public boolean onDamageEntity(BlessingsInstance instance, LivingEntity player, LivingHurtEvent event) {
        Random random = new Random();
        if(event.getEntity().isOnFire()){
            if (event.getEntity().level() instanceof ServerLevel) {
                ServerLevel serverWorld = (ServerLevel) event.getEntity().level();
                double x = event.getEntity().getX();
                double y = event.getEntity().getY() + 0.5;
                double z = event.getEntity().getZ();
                for (int i = 0; i < 20; i++) {
                    double offsetX = random.nextDouble() * 2 - 1;  // Random offset between -1 and 1
                    double offsetY = random.nextDouble();          // Random height offset
                    double offsetZ = random.nextDouble() * 2 - 1;  // Random offset between -1 and 1
                    serverWorld.sendParticles(ParticleTypes.FLAME, x + offsetX, y + offsetY, z + offsetZ, 1, 0, 0, 0, 0);
                }
            }
            event.setAmount(event.getAmount()*1.5f);
        }
        return super.onDamageEntity(instance, player, event);
    }
}
