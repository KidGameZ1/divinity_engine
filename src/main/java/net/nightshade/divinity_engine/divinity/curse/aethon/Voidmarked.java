package net.nightshade.divinity_engine.divinity.curse.aethon;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.eventbus.api.Event;
import net.nightshade.divinity_engine.divinity.curse.Curse;
import net.nightshade.divinity_engine.divinity.curse.CurseInstance;

public class Voidmarked extends Curse {
    @Override
    public void onSleepInBed(CurseInstance instance, PlayerSleepInBedEvent event) {
        event.setResult(Player.BedSleepingProblem.OTHER_PROBLEM);
        Player player = event.getEntity();
        player.displayClientMessage(Component.literal("You feel watched by distant eyes..."), true);
        super.onSleepInBed(instance, event);
    }

    @Override
    public void onTick(CurseInstance instance, LivingEntity living) {

        if (living.level() instanceof ServerLevel serverLevel) {
            Vec3 position = living.position();
            AABB affectedArea = new AABB(
                    position.x - 5.0,
                    position.y - 5.0,
                    position.z - 5.0,
                    position.x + 5.0,
                    position.y + 5.0,
                    position.z + 5.0
            );

            for (LivingEntity entity : serverLevel.getEntitiesOfClass(LivingEntity.class, affectedArea)) {
                if (entity != living) {
                    if (entity instanceof EnderMan enderMan) {
                        enderMan.setTarget(living);
                    }
                }
            }
        }
        super.onTick(instance, living);
    }

    @Override
    public void onTakenDamage(CurseInstance instance, LivingDamageEvent event) {
        if (event.getSource().getEntity() instanceof LivingEntity living && living instanceof EnderMan){
            event.setAmount(event.getAmount() + 2);
        }
        super.onTakenDamage(instance, event);
    }
}
