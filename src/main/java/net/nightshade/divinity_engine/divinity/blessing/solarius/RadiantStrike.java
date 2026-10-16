package net.nightshade.divinity_engine.divinity.blessing.solarius;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals.Cue;

import java.awt.*;

public class RadiantStrike extends Blessings {
    public RadiantStrike(int neededFavor, int cooldown, boolean isPassive, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isPassive, isActive, canToggle, textColor);
    }

    @Override
    public boolean onDamageEntity(BlessingsInstance instance, LivingEntity player, LivingHurtEvent event) {
        Level world = player.level();
        LivingEntity target = event.getEntity();
        
        if(world.isDay()){
            if (target.getMobType() == MobType.UNDEAD) {
                event.setAmount(event.getAmount() + 10);
                target.setSecondsOnFire(10);
                BlessingVisuals.at(instance, player, target.getBoundingBox().getCenter(), Cue.IMPACT, 1.2);
                return false;
            }else{
                target.setSecondsOnFire(3);
                if (target.isOnFire()) {
                    BlessingVisuals.at(instance, player, target.getBoundingBox().getCenter(), Cue.AURA, 0.8);
                }
                return true;
            }
        }
        return false;
    }
}
