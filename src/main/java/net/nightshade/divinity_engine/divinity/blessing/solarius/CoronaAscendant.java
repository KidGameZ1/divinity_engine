package net.nightshade.divinity_engine.divinity.blessing.solarius;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals.Cue;
import net.nightshade.nightshade_core.util.MiscHelper;

import java.awt.*;
import java.util.List;


public class CoronaAscendant extends Blessings {
    /**
     * Creates a new blessing with the specified parameters.
     *
     * @param neededFavor Amount of favor required to activate
     * @param cooldown    Cooldown period in seconds
     * @param isPassive   Initial active state
     * @param canToggle   Whether blessing can be toggled
     * @param textColor   Color for blessing text
     */
    public CoronaAscendant(int neededFavor, int cooldown, boolean isPassive, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isPassive, isActive, canToggle, textColor);
    }

    @Override
    public boolean onTick(BlessingsInstance instance, LivingEntity living) {
        if(instance.isToggled()) {
            if (living.level().isDay()){
                if (living.level() instanceof ServerLevel serverLevel) {
                    Vec3 position = living.position();
                    AABB affectedArea = new AABB(
                            position.x - 2.5,
                            position.y - 2.5,
                            position.z - 2.5,
                            position.x + 2.5,
                            position.y + 2.5,
                            position.z + 2.5
                    );
                    List<LivingEntity> livingEntityList = serverLevel.getEntitiesOfClass(LivingEntity.class, affectedArea);
                    livingEntityList.remove(living);
                    for (LivingEntity entity : livingEntityList) {
                        if (entity != living) {
                            if (!entity.isOnFire()) {
                                entity.setSecondsOnFire(2);
                                if (entity.isOnFire()) {
                                    BlessingVisuals.at(instance, living, entity.position(), Cue.AURA, 0.65);
                                }
                            }
                        }
                    }
                }
            }else {
                instance.onToggleOff(living);
            }
        }


        return super.onTick(instance, living);
    }

    @Override
    public boolean onDamageEntity(BlessingsInstance instance, LivingEntity player, LivingHurtEvent event) {
        if (instance.isToggled()) {
            if (player.level().isDay()){
                boolean applied = event.getEntity().addEffect(new MobEffectInstance(MobEffects.BLINDNESS, MiscHelper.secondsToTick(3), 0, false, false, true));
                if (applied) {
                    BlessingVisuals.at(instance, player, event.getEntity().getEyePosition(), Cue.IMPACT, 0.55);
                }
            }else {
                instance.onToggleOff(player);
            }
        }
        return super.onDamageEntity(instance, player, event);
    }
}
