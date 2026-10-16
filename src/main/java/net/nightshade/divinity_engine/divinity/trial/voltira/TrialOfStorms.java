package net.nightshade.divinity_engine.divinity.trial.voltira;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.nightshade.divinity_engine.divinity.trial.GodlyTrial;
import net.nightshade.divinity_engine.registry.divinity.gods.GodsRegistry;
import net.nightshade.divinity_engine.util.divinity.gods.GodHelper;

import java.util.ArrayList;
import java.util.List;

public class TrialOfStorms extends GodlyTrial {



    public TrialOfStorms(ServerPlayer player) {
        super(player);
    }

    protected int killcount = 0;

    protected List<Entity> aliveMobs = new ArrayList<>();

    @Override
    protected void onStart() {
        player.displayClientMessage(Component.literal("Voltira: Prove yourself worthy to join me in the Kingdom of Storms"), false);
    }

    @Override
    public void tick() {
        syncTrackedEntities(aliveMobs);
        if (getTicksElapsed() % 10 == 0) { // Every second
            double offsetX = (Math.random() - 0.5) * 26;
            double offsetZ = (Math.random() - 0.5) * 25;
            Vec3 strikePos = new Vec3(
                    (getArenaCenter().getX() + offsetX),
                    getArenaCenter().getY(),
                    (getArenaCenter().getZ() + offsetZ)
            );
            LightningBolt lightning = new LightningBolt(EntityType.LIGHTNING_BOLT, trialLevel);
            lightning.setPos(strikePos);
            lightning.setVisualOnly(true);
            trialLevel.addFreshEntity(lightning);
        }
        if (phase == 1) {
            if (killcount >= 6) {
                nextPhase();
            }
        } else if (phase == 2) {
            if (killcount >= 13) {
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
                setPhaseInfo("Phase 1", "Defeat the storm skeletons", killcount, 6);
                for (int i = 0; i < 6; i++) {
                    Entity entityToSpawn = EntityType.SKELETON.spawn(trialLevel, getArenaCenter().offset(player.level().random.nextInt(-4,4), 1, player.level().random.nextInt(-4,4)), MobSpawnType.MOB_SUMMONED);
                    aliveMobs.add(entityToSpawn);

                }
                break;
            case 2:
                phaseDuration = 800;
                setPhaseInfo("Phase 2", "Defeat the strays", killcount, 13);

                for (int i = 0; i < 7; i++) {
                    Entity entityToSpawn = EntityType.STRAY.spawn(trialLevel, getArenaCenter().offset(player.level().random.nextInt(-4,4), 1, player.level().random.nextInt(-4,4)), MobSpawnType.MOB_SUMMONED);
                    aliveMobs.add(entityToSpawn);

                }
                break;
            case 3:
                setPhaseInfo("Phase 3", "Endure the thunder", killcount, 21);

                for (int i = 0; i < 5; i++) {
                    Entity drowned = EntityType.DROWNED.spawn(trialLevel, getArenaCenter().offset(player.level().random.nextInt(-4,4), 1, player.level().random.nextInt(-4,4)), MobSpawnType.MOB_SUMMONED);
                    aliveMobs.add(drowned);
                }for (int i = 0; i < 3; i++) {
                    Creeper creeper = EntityType.CREEPER.spawn(trialLevel, getArenaCenter().offset(player.level().random.nextInt(-4,4), 1, player.level().random.nextInt(-4,4)), MobSpawnType.MOB_SUMMONED);
                    creeper.getEntityData().set(creeper.DATA_IS_POWERED, true);
                    aliveMobs.add(creeper);
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
        return killcount < 21 && !aliveMobs.isEmpty();
    }

    @Override
    protected void onComplete() {
        GodHelper.increaseFavor(GodsRegistry.VOLTIRA.get(), player, 50);
        player.displayClientMessage(Component.literal("Voltira: The storm recognizes your power and grants you favor."), false);
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
