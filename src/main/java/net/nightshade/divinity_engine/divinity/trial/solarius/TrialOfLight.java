package net.nightshade.divinity_engine.divinity.trial.solarius;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.nightshade.divinity_engine.divinity.trial.GodlyTrial;
import net.nightshade.divinity_engine.registry.divinity.gods.GodsRegistry;
import net.nightshade.divinity_engine.util.divinity.gods.GodHelper;

import java.util.ArrayList;
import java.util.List;

public class TrialOfLight extends GodlyTrial {
    protected int killcount = 0;

    protected List<LivingEntity> aliveMobs = new ArrayList<>();

    public TrialOfLight(ServerPlayer player) {
        super(player);
    }

    @Override
    protected void onStart() {
        player.displayClientMessage(Component.literal("Solarius: Step into the light and show your worth in the Trial of Light."), false);
    }

    @Override
    public void tick() {
        syncTrackedEntities(aliveMobs);

        if (phase == 1) {
            if (killcount >= 10) {
                nextPhase();
            }
        } else if (phase == 2) {
            if (killcount >= 19) {
                nextPhase();
            }else {
            }
        }else if (phase == 3) {
            if (killcount >= 23) {
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
                setPhaseInfo("Phase 1", "Defeat the first wave", killcount, 10);
                for (int i = 0; i < 6; i++) {
                    LivingEntity entityToSpawn = EntityType.ZOMBIE.spawn(trialLevel, getArenaCenter().offset(player.level().random.nextInt(-4,4), 1, player.level().random.nextInt(-4,4)), MobSpawnType.MOB_SUMMONED);
                    if (entityToSpawn instanceof Mob living){
                        living.setTarget(player);
                        aliveMobs.add(entityToSpawn);
                    }
                }for (int i = 0; i < 4; i++) {
                    LivingEntity entityToSpawn = EntityType.SKELETON.spawn(trialLevel, getArenaCenter().offset(player.level().random.nextInt(-4,4), 1, player.level().random.nextInt(-4,4)), MobSpawnType.MOB_SUMMONED);
                    if (entityToSpawn instanceof Mob living){
                        living.setTarget(player);
                        aliveMobs.add(entityToSpawn);
                    }
                }
                break;
            case 2:
                phaseDuration = 800;
                setPhaseInfo("Phase 2", "Defeat the raiders", killcount, 19);

                if (aliveMobs.size() > 0){
                    for (LivingEntity living: aliveMobs){
                        living.kill();
                    }
                    aliveMobs.clear();
                }
                
                for (int i = 0; i < 7; i++) {
                    LivingEntity entityToSpawn = EntityType.PILLAGER.spawn(trialLevel, getArenaCenter().offset(player.level().random.nextInt(-4,4), 1, player.level().random.nextInt(-4,4)), MobSpawnType.MOB_SUMMONED);
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
                setPhaseInfo("Phase 3", "Defeat the evokers", killcount, 23);

                if (aliveMobs.size() > 0){
                    for (LivingEntity living: aliveMobs){
                        living.kill();
                    }
                    aliveMobs.clear();
                }
                
                for (int i = 0; i < 4; i++) {
                    LivingEntity entityToSpawn = EntityType.EVOKER.spawn(trialLevel, getArenaCenter().offset(player.level().random.nextInt(-4,4), 1, player.level().random.nextInt(-4,4)), MobSpawnType.MOB_SUMMONED);
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
        return killcount < 23 && !aliveMobs.isEmpty();
    }

    @Override
    protected void onComplete() {
        if (GodHelper.getGodOrNull(player, GodsRegistry.SOLARIUS.get()) != null)
            GodHelper.increaseFavor(GodsRegistry.SOLARIUS.get(), player, 50);
        player.displayClientMessage(Component.literal("Solarius: Your courage shines brighter than the noon sun."), false);
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
}
