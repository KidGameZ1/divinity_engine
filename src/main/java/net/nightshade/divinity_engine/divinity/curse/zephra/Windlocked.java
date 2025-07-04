package net.nightshade.divinity_engine.divinity.curse.zephra;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.nightshade.divinity_engine.divinity.curse.Curse;
import net.nightshade.divinity_engine.divinity.curse.CurseInstance;

import java.util.Random;

public class Windlocked extends Curse {


    @Override
    public void onTick(CurseInstance instance, LivingEntity living) {
        if (living.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)){
            if (living instanceof Player player){
                player.stopFallFlying();
            }
        }

        super.onTick(instance, living);
    }

    @Override
    public void onJump(CurseInstance instance, LivingEvent.LivingJumpEvent event) {

        event.getEntity().setDeltaMovement(event.getEntity().getDeltaMovement().x, event.getEntity().getDeltaMovement().y-0.05, event.getEntity().getDeltaMovement().z);

        super.onJump(instance, event);
    }
}
