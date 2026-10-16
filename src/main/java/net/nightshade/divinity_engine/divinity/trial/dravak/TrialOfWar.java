package net.nightshade.divinity_engine.divinity.trial.dravak;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.nightshade.divinity_engine.divinity.trial.GodlyTrial;
import net.nightshade.divinity_engine.registry.divinity.gods.GodsRegistry;
import net.nightshade.divinity_engine.util.divinity.gods.GodHelper;
import net.nightshade.vanguards_kit.network.cap.vanguard.player.VanguardPlayerCapability;
import net.nightshade.vanguards_kit.registry.battle_aspects.BattleAspectsRegistry;
import net.nightshade.vanguards_kit.util.VanguardHelper;
import net.nightshade.vanguards_kit.util.aspects.AspectsHelper;

import java.util.ArrayList;
import java.util.List;

public class TrialOfWar extends GodlyTrial {
    protected int killcount = 0;

    protected List<LivingEntity> aliveMobs = new ArrayList<>();

    public TrialOfWar(ServerPlayer player) {
        super(player);
    }

    @Override
    protected void onStart() {
        player.displayClientMessage(Component.literal("Dravak: Show your fury and mastery over war in my trial."), false);


        ItemStack _setstack = new ItemStack(Items.IRON_AXE).copy();
        _setstack.setCount(1);
        AspectsHelper.applyAspect(_setstack, "bloodthirst");
        player.setItemInHand(InteractionHand.MAIN_HAND, _setstack);

        player.getInventory().setChanged();
        {
            ItemStack stack = new ItemStack(Items.IRON_HELMET).copy();
            AspectsHelper.applyAspect(stack, "posture_hardened");
            player.getInventory().armor.set(EquipmentSlot.HEAD.getIndex(), stack);
            player.getInventory().setChanged();
        }
        {
            ItemStack stack = new ItemStack(Items.IRON_CHESTPLATE).copy();
            AspectsHelper.applyAspect(stack, "posture_hardened");
            player.getInventory().armor.set(EquipmentSlot.CHEST.getIndex(), stack);
            player.getInventory().setChanged();
        }
        {
            ItemStack stack = new ItemStack(Items.IRON_CHESTPLATE).copy();
            AspectsHelper.applyAspect(stack, "posture_hardened");
            player.getInventory().armor.set(EquipmentSlot.LEGS.getIndex(), stack);
            player.getInventory().setChanged();
        }
        {
            ItemStack stack = new ItemStack(Items.IRON_CHESTPLATE).copy();
            AspectsHelper.applyAspect(stack, "posture_hardened");
            player.getInventory().armor.set(EquipmentSlot.FEET.getIndex(), stack);
            player.getInventory().setChanged();
        }
    }

    @Override
    public void tick() {
        syncTrackedEntities(aliveMobs);
        if (phase == 1) {
            if (killcount >= 10) {
                nextPhase();
            }
        } else if (phase == 2) {
            if (killcount >= 15) {
                nextPhase();
            }else {
            }
        }else if (phase == 3) {
            if (killcount >= 20) {
                completeTrial();
            }
        }
        super.tick();
    }

    @Override
    protected void runPhase(int phase) {
        switch (phase) {
            case 1:
                phaseDuration = 600;
                setPhaseInfo("Phase 1", "Defeat the vanguard", killcount, 10);
                for (int i = 0; i < 6; i++) {
                    LivingEntity entityToSpawn = EntityType.PILLAGER.spawn(trialLevel, getArenaCenter().offset(player.level().random.nextInt(-4,4), 1, player.level().random.nextInt(-4,4)), MobSpawnType.MOB_SUMMONED);
                    if (entityToSpawn instanceof Mob living){
                        aliveMobs.add(entityToSpawn);
                    }
                }for (int i = 0; i < 4; i++) {
                    LivingEntity entityToSpawn = EntityType.SKELETON.spawn(trialLevel, getArenaCenter().offset(player.level().random.nextInt(-4,4), 1, player.level().random.nextInt(-4,4)), MobSpawnType.MOB_SUMMONED);
                    entityToSpawn.setItemSlot(EquipmentSlot.MAINHAND, Items.IRON_SWORD.getDefaultInstance());
                    if (entityToSpawn instanceof Mob living){
                        aliveMobs.add(entityToSpawn);
                    }
                }
                break;
            case 2:
                phaseDuration = 800;
                setPhaseInfo("Phase 2", "Break the warband", killcount, 15);

                if (aliveMobs.size() > 0){
                    for (LivingEntity living: aliveMobs){
                        living.kill();
                    }
                    aliveMobs.clear();
                }
                
                for (int i = 0; i < 3; i++) {
                    LivingEntity entityToSpawn = EntityType.EVOKER.spawn(trialLevel, getArenaCenter().offset(player.level().random.nextInt(-4,4), 1, player.level().random.nextInt(-4,4)), MobSpawnType.MOB_SUMMONED);
                    if (entityToSpawn instanceof Mob living){
                        living.setTarget(player);
                        aliveMobs.add(entityToSpawn);
                    }

                }for (int i = 0; i < 2; i++) {
                    LivingEntity entityToSpawn = EntityType.VINDICATOR.spawn(trialLevel, getArenaCenter().offset(player.level().random.nextInt(-4,4), 1, player.level().random.nextInt(-4,4)), MobSpawnType.MOB_SUMMONED);
                    if (entityToSpawn instanceof Mob living){
                        living.setTarget(player);
                        aliveMobs.add(entityToSpawn);
                    }
                }
                break;
            case 3:
                setPhaseInfo("Phase 3", "Defeat the ravagers", killcount, 20);

                if (aliveMobs.size() > 0){
                    for (LivingEntity living: aliveMobs){
                        living.kill();
                    }
                    aliveMobs.clear();
                }
                
                for (int i = 0; i < 2; i++) {
                    LivingEntity entityToSpawn = EntityType.RAVAGER.spawn(trialLevel, getArenaCenter().offset(player.level().random.nextInt(-4,4), 1, player.level().random.nextInt(-4,4)), MobSpawnType.MOB_SUMMONED);
                    if (entityToSpawn instanceof Mob living){
                        living.setTarget(player);
                        aliveMobs.add(entityToSpawn);
                    }
                }
                for (int i = 0; i < 3; i++) {
                    LivingEntity entityToSpawn = EntityType.VEX.spawn(trialLevel, getArenaCenter().offset(player.level().random.nextInt(-4,4), 1, player.level().random.nextInt(-4,4)), MobSpawnType.MOB_SUMMONED);
                    if (entityToSpawn instanceof Mob living){
                        living.setTarget(player);
                        aliveMobs.add(entityToSpawn);
                    }
                }
                phaseDuration = 1000;
                break;
        }
    }

    @Override
    protected int getMaxPhases() {
        return 3;
    }

    @Override
    protected boolean checkFailCondition() {
        return killcount < 20 && !aliveMobs.isEmpty();
    }

    @Override
    protected void onComplete() {
        if (GodHelper.getGodOrNull(player, GodsRegistry.DRAVAK.get()) != null)
            GodHelper.increaseFavor(GodsRegistry.DRAVAK.get(), player, 50);
        player.displayClientMessage(Component.literal("Dravak: Your blood-stained victories honor me."), false);
    }

    @Override
    protected void completeTrial() {
        player.getInventory().clearContent();
        super.completeTrial();
    }

    @Override
    public void failTrial() {
        player.getInventory().clearContent();
        super.failTrial();
    }

    @Override
    public void onKillEntity(Player player, LivingDeathEvent event) {
        killcount++;
        if(aliveMobs.contains(event.getEntity())){
            aliveMobs.remove(event.getEntity());
        }
        setObjectiveProgress(killcount);
        super.onKillEntity(player, event);
    }

    @Override
    public void onPlayerDeath(LivingDeathEvent event) {
        failTrial();
        super.onPlayerDeath(event);
    }

    @Override
    public boolean startCondition() {
        if(player.getInventory().isEmpty() && VanguardHelper.getVanguardPlayerVariables(player).getVanguardInventory().isEmpty()){
            return true;
        }
        player.displayClientMessage(Component.literal("Dravak: You must have nothing in your inventory to start this trial."), false);
        return false;
    }
}
