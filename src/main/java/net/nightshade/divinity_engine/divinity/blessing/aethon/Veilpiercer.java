package net.nightshade.divinity_engine.divinity.blessing.aethon;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.LightLayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.nightshade_core.util.MiscHelper;

import java.awt.*;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A blessing that grants enhanced vision in dark environments,
 * causing nearby entities to glow and emit particles.
 */
public class Veilpiercer extends Blessings {
    /**
     * Creates a new Veilpiercer blessing.
     *
     * @param neededFavor Amount of favor required to activate
     * @param cooldown    Cooldown period in seconds
     * @param isActive    Whether blessing starts active
     * @param canToggle   Whether blessing can be toggled
     * @param textColor   Color for blessing text
     */
    public Veilpiercer(int neededFavor, int cooldown, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isActive, canToggle, textColor);
    }

    /**
     * Applies glowing effect and particles to nearby entities in dark areas.
     *
     * @param instance The blessing instance
     * @param living   The blessed entity
     * @return True for cooldown
     */
    @OnlyIn(Dist.CLIENT)
    @Override
    public boolean onTick(BlessingsInstance instance, LivingEntity living) {
        Random random = new Random();
        MiscHelper.executeClientOnlyCode(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.player.isShiftKeyDown()) {
                // Get all living entities in a larger range for glow removal
                List<LivingEntity> allEntities = mc.player.level().getEntitiesOfClass(
                        LivingEntity.class,
                        mc.player.getBoundingBox().inflate(32.0D),
                        entity -> entity != mc.player
                );

                // Track only entities within the 16-block effect radius
                Set<LivingEntity> affectedEntities = allEntities.stream()
                        .filter(entity -> entity.distanceTo(mc.player) <= 16.0D)
                        .collect(Collectors.toSet());

                for (LivingEntity entity : affectedEntities) {
                    entity.level().addParticle(
                            ParticleTypes.END_ROD,
                            entity.getX(), entity.getY() + entity.getBbHeight() / 2, entity.getZ(),
                            0, 0, 0
                    );
                    if (random.nextFloat() <= 0.4f) {
                        double offsetX = random.nextDouble() * 2 - 1;  // Random offset between -1 and 1
                        double offsetY = random.nextDouble();          // Random height offset
                        double offsetZ = random.nextDouble() * 2 - 1;  // Random offset between -1 and 1
                        entity.level().addParticle(ParticleTypes.END_ROD, entity.getX() + offsetX, entity.getY() + offsetY, entity.getZ() + offsetZ, 0, 0, 0);
                    }

                }
            }
        });

        return super.onTick(instance, living);
    }
}