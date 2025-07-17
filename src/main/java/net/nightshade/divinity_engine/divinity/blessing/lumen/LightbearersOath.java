package net.nightshade.divinity_engine.divinity.blessing.lumen;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;

import java.awt.*;
import java.util.Random;

public class LightbearersOath extends Blessings {
    /**
     * Creates a new blessing with the specified parameters.
     *
     * @param neededFavor Amount of favor required to activate
     * @param cooldown    Cooldown period in seconds
     * @param isPassive   Is a passive blessing or not
     * @param canToggle   Whether blessing can be toggled
     * @param textColor   Color for blessing text
     */
    public LightbearersOath(int neededFavor, int cooldown, boolean isPassive, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isPassive, isActive, canToggle, textColor);
    }

    @Override
    public boolean onDamageEntity(BlessingsInstance instance, LivingEntity player, LivingHurtEvent event) {
        Random r = new Random();

        Entity target = event.getEntity();
        System.out.println(target.getType());
        if (target instanceof LivingEntity livingTarget) {
            // Check if undead or dark-aligned
            boolean isUndead = livingTarget.getMobType() == MobType.UNDEAD;
            boolean isDarkAligned = (
                            livingTarget instanceof EnderMan
                                    || livingTarget instanceof Endermite
                                        || livingTarget instanceof EnderDragon
                                            || livingTarget instanceof Shulker
            );

            if (isUndead || isDarkAligned) {
                float boosted = event.getAmount() * 1.2f; // +20% damage
                event.setAmount(boosted);
                if (target.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.END_ROD, target.getX(), target.getY() + 1, target.getZ(), 10, r.nextDouble(-0.5, 0.5), r.nextDouble(-0.5, 0.5), r.nextDouble(-0.5, 0.5), 0.1);
                }
                return false;
            }
        }

        return false;
    }

}
