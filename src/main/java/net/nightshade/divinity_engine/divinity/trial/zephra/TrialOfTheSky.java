package net.nightshade.divinity_engine.divinity.trial.zephra;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.nightshade.divinity_engine.divinity.trial.GodlyTrial;
import net.nightshade.divinity_engine.registry.divinity.gods.GodsRegistry;
import net.nightshade.divinity_engine.util.divinity.gods.GodHelper;

import java.util.ArrayList;
import java.util.List;

public class TrialOfTheSky extends GodlyTrial {

    protected int killcount = 0;
    protected List<LivingEntity> aliveMobs = new ArrayList<>();

    public TrialOfTheSky(ServerPlayer player) {
        super(player);
    }

    @Override
    protected void onStart() {
        player.displayClientMessage(Component.literal("Zephra: Rise on the winds and prove your freedom and skill."), false);
    }

    @Override
    public void tick() {
        syncTrackedEntities(aliveMobs);

        if (phase == 1) {
            if (killcount >= 8) {
                nextPhase();
            }
        } else if (phase == 2) {
            if (killcount >= 14) {
                nextPhase();
            }else {
            }
        }else if (phase == 3) {
            if (killcount >= 22) {
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
                setPhaseInfo("Phase 1", "Defeat the sky raiders", killcount, 8);
                for (int i = 0; i < 8; i++) {
                    LivingEntity zombie = EntityType.ZOMBIE.spawn(trialLevel, getArenaCenter().offset(player.level().random.nextInt(-4,4), 1, player.level().random.nextInt(-4,4)), MobSpawnType.MOB_SUMMONED);
                    LivingEntity entityToSpawn = EntityType.CHICKEN.spawn(trialLevel, getArenaCenter().offset(player.level().random.nextInt(-4,4), 1, player.level().random.nextInt(-4,4)), MobSpawnType.MOB_SUMMONED);
                    if (zombie instanceof Mob living){
                        ((Zombie) zombie).setBaby(true);
                        assert entityToSpawn != null;
                        living.startRiding(entityToSpawn, true);
                        living.setTarget(player);
                        aliveMobs.add(zombie);
                    }
                }
                break;
            case 2:
                phaseDuration = 800;
                setPhaseInfo("Phase 2", "Defeat the windcallers", killcount, 14);

                if (aliveMobs.size() > 0){
                    for (LivingEntity living: aliveMobs){
                        living.kill();
                    }
                    aliveMobs.clear();
                }
                
                for (int i = 0; i < 3; i++) {
                    LivingEntity entityToSpawn = EntityType.SKELETON.spawn(trialLevel, getArenaCenter().offset(player.level().random.nextInt(-4,4), 1, player.level().random.nextInt(-4,4)), MobSpawnType.MOB_SUMMONED);
                    if (entityToSpawn instanceof Mob living){
                        ItemStack bow = new ItemStack(Items.BOW);
                        bow.enchant(Enchantments.PUNCH_ARROWS,1);
                        living.setItemSlot(EquipmentSlot.MAINHAND, bow);
                        living.setItemSlot(EquipmentSlot.HEAD, Items.CHAINMAIL_HELMET.getDefaultInstance());
                        aliveMobs.add(entityToSpawn);
                    }

                }
                for (int i = 0; i < 3; i++) {
                    LivingEntity entityToSpawn = EntityType.SKELETON.spawn(trialLevel, getArenaCenter().offset(player.level().random.nextInt(-4,4), 1, player.level().random.nextInt(-4,4)), MobSpawnType.MOB_SUMMONED);
                    if (entityToSpawn instanceof Mob living){
                        ItemStack bow = new ItemStack(Items.BOW);
                        living.setItemSlot(EquipmentSlot.MAINHAND, bow);
                        aliveMobs.add(entityToSpawn);
                    }

                }
                break;
            case 3:
                setPhaseInfo("Phase 3", "Master the stormfront", killcount, 22);

                if (aliveMobs.size() > 0){
                    for (LivingEntity living: aliveMobs){
                        living.kill();
                    }
                    aliveMobs.clear();
                }

                for (int i = 0; i < 5; i++) {
                    LivingEntity entityToSpawn = EntityType.SKELETON.spawn(trialLevel, getArenaCenter().offset(player.level().random.nextInt(-4,4), 1, player.level().random.nextInt(-4,4)), MobSpawnType.MOB_SUMMONED);
                    if (entityToSpawn instanceof Mob living){
                        ItemStack bow = new ItemStack(Items.BOW);
                        bow.enchant(Enchantments.PUNCH_ARROWS,1);
                        living.setItemSlot(EquipmentSlot.MAINHAND, bow);
                        living.setItemSlot(EquipmentSlot.HEAD, Items.CHAINMAIL_HELMET.getDefaultInstance());
                        aliveMobs.add(entityToSpawn);
                    }

                }

                for (int i = 0; i < 3; i++) {
                    LivingEntity entityToSpawn = EntityType.PHANTOM.spawn(trialLevel, getArenaCenter().offset(player.level().random.nextInt(-4,4), 1, player.level().random.nextInt(-4,4)), MobSpawnType.MOB_SUMMONED);
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
        return killcount < 22 && !aliveMobs.isEmpty();
    }

    @Override
    protected void onComplete() {
        if (GodHelper.getGodOrNull(player, GodsRegistry.ZEPHRA.get()) != null)
            GodHelper.increaseFavor(GodsRegistry.ZEPHRA.get(), player, 50);
        player.displayClientMessage(Component.literal("Zephra: You soar among the clouds with my blessing."), false);
    }


    @Override
    public void onKillEntity(Player player, LivingDeathEvent event) {
        if (!aliveMobs.contains(event.getEntity())) return;
        killcount++;
        aliveMobs.remove(event.getEntity());
        setObjectiveProgress(killcount);
        super.onKillEntity(player, event);
    }

    @Override
    public void onPlayerDeath(LivingDeathEvent event) {
        failTrial();
        super.onPlayerDeath(event);
    }
}
