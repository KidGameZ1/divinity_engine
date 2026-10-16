package net.nightshade.divinity_engine.divinity.trial.terra;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.nightshade.divinity_engine.divinity.trial.GodlyTrial;
import net.nightshade.divinity_engine.registry.divinity.gods.GodsRegistry;
import net.nightshade.divinity_engine.util.divinity.gods.GodHelper;

import java.util.ArrayList;
import java.util.List;

public class TrialOfNature extends GodlyTrial {

    protected int killcount = 0;
    protected List<LivingEntity> aliveMobs = new ArrayList<>();

    public TrialOfNature(ServerPlayer player) {
        super(player);
    }

    @Override
    protected void onStart() {
        player.displayClientMessage(Component.literal("Terra: Endure the trial of growth and survive the earth’s test."), false);
    }

    @Override
    public void tick() {
        syncTrackedEntities(aliveMobs);

        if (phase == 1) {
            if (killcount >= 10) {
                nextPhase();
            }
        } else if (phase == 2) {
            if (killcount >= 21) {
                nextPhase();
            }
        }else if (phase == 3) {
            if (killcount >= 29) {
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
                setPhaseInfo("Phase 1", "Cull the invasive beasts", killcount, 10);
                for (int i = 0; i < 10; i++) {
                    LivingEntity entityToSpawn = EntityType.BEE.spawn(trialLevel, getArenaCenter().offset(player.level().random.nextInt(-4,4), 1, player.level().random.nextInt(-4,4)), MobSpawnType.MOB_SUMMONED);
                    if (entityToSpawn instanceof Mob living){
                        living.setTarget(player);
                        aliveMobs.add(entityToSpawn);
                    }
                }
                break;
            case 2:
                phaseDuration = 800;
                setPhaseInfo("Phase 2", "Defeat the corrupted wilds", killcount, 21);

                if (aliveMobs.size() > 0){
                    for (LivingEntity living: aliveMobs){
                        living.kill();
                    }
                    aliveMobs.clear();
                }
                
                for (int i = 0; i < 5; i++) {
                    LivingEntity entityToSpawn = EntityType.BEE.spawn(trialLevel, getArenaCenter().offset(player.level().random.nextInt(-4,4), 1, player.level().random.nextInt(-4,4)), MobSpawnType.MOB_SUMMONED);
                    if (entityToSpawn instanceof Mob living){
                        living.setTarget(player);
                        aliveMobs.add(entityToSpawn);
                    }

                }
                for (int i = 0; i < 6; i++) {
                    LivingEntity entityToSpawn = EntityType.WOLF.spawn(trialLevel, getArenaCenter().offset(player.level().random.nextInt(-4,4), 1, player.level().random.nextInt(-4,4)), MobSpawnType.MOB_SUMMONED);
                    if (entityToSpawn instanceof Mob living){
                        living.setTarget(player);
                        aliveMobs.add(entityToSpawn);
                    }

                }
                break;
            case 3:
                setPhaseInfo("Phase 3", "Defeat the final growth", killcount, 29);

                if (aliveMobs.size() > 0){
                    for (LivingEntity living: aliveMobs){
                        living.kill();
                    }
                    aliveMobs.clear();
                }

                for (int i = 0; i < 5; i++) {
                    LivingEntity entityToSpawn = EntityType.BEE.spawn(trialLevel, getArenaCenter().offset(player.level().random.nextInt(-4,4), 1, player.level().random.nextInt(-4,4)), MobSpawnType.MOB_SUMMONED);
                    if (entityToSpawn instanceof Mob living){
                        living.setTarget(player);
                        aliveMobs.add(entityToSpawn);
                    }

                }

                for (int i = 0; i < 3; i++) {
                    LivingEntity entityToSpawn = EntityType.RAVAGER.spawn(trialLevel, getArenaCenter().offset(player.level().random.nextInt(-4,4), 1, player.level().random.nextInt(-4,4)), MobSpawnType.MOB_SUMMONED);
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
        if (GodHelper.getGodOrNull(player, GodsRegistry.TERRA.get()) != null)
            GodHelper.increaseFavor(GodsRegistry.TERRA.get(), player, 50);
        player.displayClientMessage(Component.literal("Terra: You are rooted in strength and worthy of my favor."), false);
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
