package net.nightshade.divinity_engine.divinity.blessing.voltira;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;

import java.awt.*;
import java.util.Random;

public class Stormstep extends Blessings {
    public Stormstep(int neededFavor, int cooldown, boolean isPassive, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isPassive, isActive, canToggle, textColor);
    }

    @Override
    public <T extends BlessingsInstance> T createDefaultInstance() {
        T instance = super.createDefaultInstance();
        if (!instance.getOrCreateTag().contains("horizontal_dash_power"))
            instance.getOrCreateTag().putDouble("horizontal_dash_power", 2.5);
        if (!instance.getOrCreateTag().contains("vertical_dash_power"))
            instance.getOrCreateTag().putDouble("vertical_dash_power", 1.5);
        if (!instance.getOrCreateTag().contains("fall_immunity_duration"))
            instance.getOrCreateTag().putInt("fall_immunity_duration", 100);
        return instance;
    }

    @Override
    public boolean onPressed(BlessingsInstance instance, LivingEntity living) {
        Vec3 lookVec = living.getLookAngle();
        Vec3 movement;
        boolean activateCooldown = true;

        if (living.isShiftKeyDown()) {
            movement = new Vec3(0, getVerticalDashPower(instance), 0);
            activateCooldown = false;
        } else {
            movement = new Vec3(
                    lookVec.x * getHorizontalDashPower(instance),
                    0.2,
                    lookVec.z * getHorizontalDashPower(instance)
            );
        }

        living.setDeltaMovement(movement);
        if (living.isShiftKeyDown()) {
            if (!living.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) {
                living.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, getFallImmunityDuration(instance), 0, false, false, true));
            }
        }

        if (living.level() instanceof ServerLevel serverLevel) {
            BlockPos pos = living.blockPosition();
            LightningBolt lightning = new LightningBolt(EntityType.LIGHTNING_BOLT, serverLevel);
            lightning.setPos(pos.getX(), pos.getY(), pos.getZ());
            lightning.setVisualOnly(true);
            serverLevel.addFreshEntity(lightning);

            serverLevel.playSound(null, pos, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 1.0F, 1.0F);
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX(), pos.getY(), pos.getZ(), 50, 0.5, 0.5, 0.5, 0.1);
        }

        return activateCooldown;
    }

    public static double getHorizontalDashPower(BlessingsInstance instance) {
        return instance.getOrCreateTag().getDouble("horizontal_dash_power");
    }

    public static double getVerticalDashPower(BlessingsInstance instance) {
        return instance.getOrCreateTag().getDouble("vertical_dash_power");
    }

    public static int getFallImmunityDuration(BlessingsInstance instance) {
        return instance.getOrCreateTag().getInt("fall_immunity_duration");
    }
}
