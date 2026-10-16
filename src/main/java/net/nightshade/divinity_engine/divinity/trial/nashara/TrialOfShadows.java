package net.nightshade.divinity_engine.divinity.trial.nashara;

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

public class TrialOfShadows extends GodlyTrial {

    protected int killcount = 0;
    protected List<LivingEntity> aliveMobs = new ArrayList<>();

    public TrialOfShadows(ServerPlayer player) {
        super(player);
    }

    @Override
    protected void onStart() {
        player.displayClientMessage(Component.literal("Nashara: Prove your cunning and survive the shadows I command."), false);
    }

    @Override
    public void tick() {
        syncTrackedEntities(aliveMobs);

        if (phase == 1) {
            if (killcount >= 8) {
                nextPhase();
            }
        } else if (phase == 2) {
            if (killcount >= 18) {
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
                setPhaseInfo("Phase 1", "Defeat the hidden stalkers", killcount, 8);
                for (int i = 0; i < 3; i++) {
                    LivingEntity entityToSpawn = EntityType.CAVE_SPIDER.spawn(trialLevel, getArenaCenter().offset(player.level().random.nextInt(-4,4), 1, player.level().random.nextInt(-4,4)), MobSpawnType.MOB_SUMMONED);
                    if (entityToSpawn instanceof Mob living){
                        living.setTarget(player);
                        aliveMobs.add(entityToSpawn);
                    }
                }for (int i = 0; i < 5; i++) {
                    LivingEntity entityToSpawn = EntityType.SPIDER.spawn(trialLevel, getArenaCenter().offset(player.level().random.nextInt(-4,4), 1, player.level().random.nextInt(-4,4)), MobSpawnType.MOB_SUMMONED);
                    if (entityToSpawn instanceof Mob living){
                        living.setTarget(player);
                        aliveMobs.add(entityToSpawn);
                    }
                }
                break;
            case 2:
                phaseDuration = 800;
                setPhaseInfo("Phase 2", "Survive the venomous wave", killcount, 18);

                if (aliveMobs.size() > 0){
                    for (LivingEntity living: aliveMobs){
                        living.kill();
                    }
                    aliveMobs.clear();
                }
                
                for (int i = 0; i < 7; i++) {
                    LivingEntity entityToSpawn = EntityType.SPIDER.spawn(trialLevel, getArenaCenter().offset(player.level().random.nextInt(-4,4), 1, player.level().random.nextInt(-4,4)), MobSpawnType.MOB_SUMMONED);
                    if (entityToSpawn instanceof Mob living){
                        living.setTarget(player);
                        aliveMobs.add(entityToSpawn);
                    }

                }for (int i = 0; i < 3; i++) {
                    LivingEntity entityToSpawn = EntityType.WITCH.spawn(trialLevel, getArenaCenter().offset(player.level().random.nextInt(-4,4), 1, player.level().random.nextInt(-4,4)), MobSpawnType.MOB_SUMMONED);
                    if (entityToSpawn instanceof Mob living){
                        living.setTarget(player);
                        aliveMobs.add(entityToSpawn);
                    }

                }
                break;
            case 3:
                setPhaseInfo("Phase 3", "Master the shadows", killcount, 23);

                if (aliveMobs.size() > 0){
                    for (LivingEntity living: aliveMobs){
                        living.kill();
                    }
                    aliveMobs.clear();
                }
                
                for (int i = 0; i < 2; i++) {
                    LivingEntity entityToSpawn = EntityType.VINDICATOR.spawn(trialLevel, getArenaCenter().offset(player.level().random.nextInt(-4,4), 1, player.level().random.nextInt(-4,4)), MobSpawnType.MOB_SUMMONED);
                    if (entityToSpawn instanceof Mob living){
                        living.setTarget(player);
                        aliveMobs.add(entityToSpawn);
                    }
                }
                for (int i = 0; i < 3; i++) {
                    LivingEntity entityToSpawn = EntityType.WITCH.spawn(trialLevel, getArenaCenter().offset(player.level().random.nextInt(-4,4), 1, player.level().random.nextInt(-4,4)), MobSpawnType.MOB_SUMMONED);
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
        if (GodHelper.getGodOrNull(player, GodsRegistry.NASHARA.get()) != null)
            GodHelper.increaseFavor(GodsRegistry.NASHARA.get(), player, 50);
        player.displayClientMessage(Component.literal("Nashara: Your cleverness has earned my dark favor."), false);
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
