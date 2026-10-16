package net.nightshade.divinity_engine.divinity.trial.vokar;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.nightshade.divinity_engine.divinity.trial.GodlyTrial;
import net.nightshade.divinity_engine.registry.divinity.gods.GodsRegistry;
import net.nightshade.divinity_engine.util.divinity.gods.GodHelper;

import java.util.ArrayList;
import java.util.List;

public class TrialOfDeath extends GodlyTrial {
    protected int killcount = 0;

    protected List<LivingEntity> aliveMobs = new ArrayList<>();

    public TrialOfDeath(ServerPlayer player) {
        super(player);
    }

    @Override
    protected void onStart() {
        player.displayClientMessage(Component.literal("Vokar: Face death and the silence beyond; survive my trial."), false);
    }

    @Override
    public void tick() {
        syncTrackedEntities(aliveMobs);

        if (phase == 1) {
            if (killcount >= 7) {
                nextPhase();
            }
        } else if (phase == 2) {
            if (killcount >= 15) {
                nextPhase();
            }
        }else if (phase == 3) {
            if (killcount >= 21) {
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
                setPhaseInfo("Phase 1", "Defeat the shambling dead", killcount, 7);
                for (int i = 0; i < 5; i++) {
                    LivingEntity entityToSpawn = EntityType.ZOMBIE.spawn(trialLevel, getArenaCenter().offset(player.level().random.nextInt(-4,4), 1, player.level().random.nextInt(-4,4)), MobSpawnType.MOB_SUMMONED);
                    if (entityToSpawn instanceof Mob living){
                        aliveMobs.add(entityToSpawn);
                    }
                }
                for (int i = 0; i < 2; i++) {
                    LivingEntity entityToSpawn = EntityType.WITHER_SKELETON.spawn(trialLevel, getArenaCenter().offset(player.level().random.nextInt(-4,4), 1, player.level().random.nextInt(-4,4)), MobSpawnType.MOB_SUMMONED);
                    entityToSpawn.setItemInHand(InteractionHand.MAIN_HAND, Items.AIR.getDefaultInstance());
                    if (entityToSpawn instanceof Mob living){
                        aliveMobs.add(entityToSpawn);
                    }
                }
                break;
            case 2:
                phaseDuration = 800;
                setPhaseInfo("Phase 2", "Cut through the gravebound", killcount, 15);
                if (aliveMobs.size() > 0){
                    for (LivingEntity living: aliveMobs){
                        living.kill();
                    }
                    aliveMobs.clear();
                }
                for (int i = 0; i < 8; i++) {
                    LivingEntity entityToSpawn = EntityType.WITHER_SKELETON.spawn(trialLevel, getArenaCenter().offset(player.level().random.nextInt(-4,4), 1, player.level().random.nextInt(-4,4)), MobSpawnType.MOB_SUMMONED);
                    if (entityToSpawn instanceof Mob living){
                        aliveMobs.add(entityToSpawn);
                    }
                }
                break;
            case 3:
                setPhaseInfo("Phase 3", "Master the dead", killcount, 21);
                if (aliveMobs.size() > 0){
                    for (LivingEntity living: aliveMobs){
                        living.kill();
                    }
                    aliveMobs.clear();
                }

                for (int i = 0; i < 4; i++) {
                    LivingEntity entityToSpawn = EntityType.ZOMBIE.spawn(trialLevel, getArenaCenter().offset(player.level().random.nextInt(-4,4), 1, player.level().random.nextInt(-4,4)), MobSpawnType.MOB_SUMMONED);
                    entityToSpawn.setItemInHand(InteractionHand.MAIN_HAND, Items.STONE_SWORD.getDefaultInstance());
                    if (entityToSpawn instanceof Mob living){
                        aliveMobs.add(entityToSpawn);
                    }
                }

                for (int i = 0; i < 2; i++) {
                    LivingEntity entityToSpawn = EntityType.WITHER_SKELETON.spawn(trialLevel, getArenaCenter().offset(player.level().random.nextInt(-4,4), 1, player.level().random.nextInt(-4,4)), MobSpawnType.MOB_SUMMONED);
                    entityToSpawn.setItemInHand(InteractionHand.MAIN_HAND, Items.IRON_SWORD.getDefaultInstance());
                    if (entityToSpawn instanceof Mob living){
                        aliveMobs.add(entityToSpawn);
                    }
                }
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
        if (GodHelper.getGodOrNull(player, GodsRegistry.VOKAR.get()) != null)
            GodHelper.increaseFavor(GodsRegistry.VOKAR.get(), player, 50);
        player.displayClientMessage(Component.literal("Vokar: You have walked with death and earned my respect."), false);
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
        return super.startCondition();
    }
}
