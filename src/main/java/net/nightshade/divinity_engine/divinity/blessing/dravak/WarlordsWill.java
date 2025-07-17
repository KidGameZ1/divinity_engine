package net.nightshade.divinity_engine.divinity.blessing.dravak;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.nightshade.divinity_engine.DivinityEngineMod;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;

import java.awt.*;
import java.util.List;

public class WarlordsWill extends Blessings {
    /**
     * Creates a new blessing with the specified parameters.
     *
     * @param neededFavor Amount of favor required to activate
     * @param cooldown    Cooldown period in seconds
     * @param isPassive   Is a passive blessing or not
     * @param canToggle   Whether blessing can be toggled
     * @param textColor   Color for blessing text
     */
    public WarlordsWill(int neededFavor, int cooldown, boolean isPassive, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isPassive, isActive, canToggle, textColor);
    }

    @Override
    public boolean onPressed(BlessingsInstance instance, LivingEntity living) {

        if (living.level() instanceof ServerLevel serverLevel) {
            Vec3 position = living.position();
            AABB affectedArea = new AABB(
                    position.x - 10,
                    position.y - 10,
                    position.z - 10,
                    position.x + 10,
                    position.y + 10,
                    position.z + 10
            );
            List<LivingEntity> livingEntityList = serverLevel.getEntitiesOfClass(LivingEntity.class, affectedArea);
            livingEntityList.remove(living);
            for (int r = 0; r < livingEntityList.size(); r++) {
                LivingEntity entity = livingEntityList.get(r);
                if (entity != living) {
                    entity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 2, false, false, true));
                    entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 255, false, false, true));
                    double x = entity.getX();
                    double y = entity.getY();
                    double z = entity.getZ();
                    if (entity instanceof Mob mob)
                        if (r != livingEntityList.size()-1) {
                            mob.setTarget(livingEntityList.get(r + 1));
                        }else {
                            if (r-1!=0)
                                mob.setTarget(livingEntityList.get(r - 1));
                        }
                }
            }
            if (!livingEntityList.isEmpty()) {
                for (int i = 0; i < 3000; i++) {
                    double maxRadius = 20; // 11-block wide dome
                    double radius = serverLevel.getRandom().nextDouble() * maxRadius;

                    double theta = serverLevel.getRandom().nextDouble() * 2 * Math.PI; // horizontal angle
                    double phi = serverLevel.getRandom().nextDouble() * (Math.PI / 2); // vertical (0 to 90° for dome)

                    // Spherical to Cartesian conversion
                    double x = radius * Math.sin(phi) * Math.cos(theta);
                    double y = radius * Math.cos(phi); // height component
                    double z = radius * Math.sin(phi) * Math.sin(theta);

                    // Position relative to player
                    double px = living.getX() + x;
                    double py = living.getY() + y + 0.01f; // raise dome slightly above player
                    double pz = living.getZ() + z;

                    // Spawn particles
                    serverLevel.sendParticles(ParticleTypes.FLAME, px, py, pz, 1, 0, 0, 0, 0);
                    serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, px, py, pz, 1, 0, 0, 0, 0);
                }
                return true;
            }
        }

        return super.onPressed(instance, living);
    }
}
