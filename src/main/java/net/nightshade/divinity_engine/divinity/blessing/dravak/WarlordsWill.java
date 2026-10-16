package net.nightshade.divinity_engine.divinity.blessing.dravak;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.util.divinity.blessings.WarlordConfusion;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals;

import java.awt.Color;

public class WarlordsWill extends Blessings {
    public WarlordsWill(int favor, int cooldown, boolean passive, boolean active, boolean toggle, Color color) {
        super(favor, cooldown, passive, active, toggle, color);
    }

    @Override
    public boolean onPressed(BlessingsInstance instance, LivingEntity living) {
        if (!(living.level() instanceof ServerLevel level)) return false;
        var enemies = level.getEntitiesOfClass(Mob.class, living.getBoundingBox().inflate(7),
                mob -> mob.isAlive() && living.distanceToSqr(mob) <= 49 && !mob.isAlliedTo(living)
                        && (mob instanceof Enemy || mob.getTarget() == living));
        for (Mob mob : enemies) {
            Mob opponent = enemies.stream().filter(other -> other != mob && !mob.isAlliedTo(other))
                    .min(java.util.Comparator.comparingDouble(mob::distanceToSqr)).orElse(null);
            mob.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0, false, false, true));
            WarlordConfusion.apply(mob, opponent, 100);
            BlessingVisuals.at(instance, living, mob.getEyePosition(), BlessingVisuals.Cue.CONTROL, 1);
        }
        return !enemies.isEmpty();
    }
}
