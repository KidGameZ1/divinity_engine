package net.nightshade.divinity_engine.divinity.blessing.vokar;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.nightshade_core.util.MiscHelper;

import java.awt.*;
import java.util.List;
import java.util.Random;

public class Witherbound extends Blessings {
    /**
     * Creates a new blessing with the specified parameters.
     *
     * @param neededFavor Amount of favor required to activate
     * @param cooldown    Cooldown period in seconds
     * @param isPassive   Is a passive blessing or not
     * @param canToggle   Whether blessing can be toggled
     * @param textColor   Color for blessing text
     */
    public Witherbound(int neededFavor, int cooldown, boolean isPassive, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isPassive, isActive, canToggle, textColor);
    }

    @Override
    public boolean onPressed(BlessingsInstance instance, LivingEntity living) {
        Random r = new Random();
        if (living.level() instanceof ServerLevel serverLevel) {

            Vec3 position = living.position();
            AABB affectedArea = new AABB(
                    position.x - 5,
                    position.y - 5,
                    position.z - 5,
                    position.x + 5,
                    position.y + 5,
                    position.z + 5
            );
            List<LivingEntity> livingEntityList = serverLevel.getEntitiesOfClass(LivingEntity.class, affectedArea);
            livingEntityList.remove(living);

            for (LivingEntity entity : livingEntityList){
                entity.addEffect(new MobEffectInstance(MobEffects.WITHER, MiscHelper.secondsToTick(3), 0, false, false, true));
            }

            if (!livingEntityList.isEmpty()) {
                for (int i = 0; i < 100; i++) {
                    double angle = serverLevel.getRandom().nextDouble() * 2 * Math.PI;
                    double radius = serverLevel.getRandom().nextDouble() * 6;
                    double x = living.getX() + Math.cos(angle) * radius;
                    double y = living.getY()+0.02;
                    double z = living.getZ() + Math.sin(angle) * radius;


                    // Spawn particles
                    serverLevel.sendParticles(ParticleTypes.SOUL, x, y, z, 1, 0, r.nextDouble(0,0.5), 0, r.nextDouble(0,0.2));
                    serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 1, 0, r.nextDouble(0,0.5), 0, r.nextDouble(0,0.2));
                }
            }
        }
        return super.onPressed(instance, living);
    }
}
