package net.nightshade.divinity_engine.divinity.blessing.voltira;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.nightshade_core.util.MathHelper;

import java.awt.*;
import java.util.List;
import java.util.Random;

public class LightningConduit extends Blessings {
    /**
     * Creates a new blessing with the specified parameters.
     *
     * @param neededFavor Amount of favor required to activate
     * @param cooldown    Cooldown period in seconds
     * @param isPassive   Is a passive blessing or not
     * @param canToggle   Whether blessing can be toggled
     * @param textColor   Color for blessing text
     */
    public LightningConduit(int neededFavor, int cooldown, boolean isPassive, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isPassive, isActive, canToggle, textColor);
    }

    @Override
    public boolean onTick(BlessingsInstance instance, LivingEntity living) {
        Random random = new Random();

        if (living instanceof Player player) {
            if (player.isSprinting()){
                if (random.nextFloat() <= 0.01) {
                    if (living.level() instanceof ServerLevel serverLevel) {
                        Vec3 position = living.position();
                        AABB affectedArea = new AABB(
                                position.x - 6,
                                position.y - 6,
                                position.z - 6,
                                position.x + 6,
                                position.y + 6,
                                position.z + 6
                        );
                        List<LivingEntity> livingEntityList = serverLevel.getEntitiesOfClass(LivingEntity.class, affectedArea);
                        livingEntityList.remove(living);

                        if (!livingEntityList.isEmpty()) {
                            LivingEntity target = MathHelper.pickRandom(livingEntityList);
                            assert target != null;
                            BlockPos pos = target.blockPosition();
                            LightningBolt lightning = new LightningBolt(EntityType.LIGHTNING_BOLT, serverLevel);
                            if (target instanceof Creeper creeper) {
                                lightning.setVisualOnly(true);
                                target.hurt(new DamageSource(serverLevel.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.LIGHTNING_BOLT)), 5);
                            }

                            lightning.setPos(pos.getX(), pos.getY(), pos.getZ());
                            serverLevel.addFreshEntity(lightning);
                        }
                    }
                }
            }
        }

        return super.onTick(instance, living);
    }
}
