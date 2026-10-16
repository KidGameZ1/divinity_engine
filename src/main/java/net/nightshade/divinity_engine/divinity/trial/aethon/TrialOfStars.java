package net.nightshade.divinity_engine.divinity.trial.aethon;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.nightshade.divinity_engine.divinity.trial.GodlyTrial;
import net.nightshade.divinity_engine.registry.divinity.gods.GodsRegistry;
import net.nightshade.divinity_engine.util.divinity.gods.GodHelper;

import java.util.ArrayList;
import java.util.List;

public class TrialOfStars extends GodlyTrial {
    private int killcount = 0;
    private final List<LivingEntity> aliveMobs = new ArrayList<>();

    public TrialOfStars(ServerPlayer player) {
        super(player);
    }

    @Override
    protected void onStart() {
        player.displayClientMessage(Component.literal("Aethon: Reach for the stars and prove your balance."), false);
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
            }
        } else if (phase == 3) {
            if (getTicksElapsed() % 60 == 0) {
                player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 80, 0, false, false, true));
            }
            if (killcount >= 18) {
                completeTrial();
            }
        }

        super.tick();
    }

    @Override
    protected void runPhase(int phase) {
        clearAliveMobs();

        switch (phase) {
            case 1:
                phaseDuration = 600;
                setPhaseInfo("Phase 1", "Defeat the endermites", killcount, 8);
                for (int i = 0; i < 8; i++) {
                    spawnMob(EntityType.ENDERMITE, 0);
                }
                break;
            case 2:
                phaseDuration = 800;
                setPhaseInfo("Phase 2", "Defeat the endermen", killcount, 14);
                for (int i = 0; i < 6; i++) {
                    spawnMob(EntityType.ENDERMAN, 0);
                }
                break;
            case 3:
                phaseDuration = 1000;
                setPhaseInfo("Phase 3", "Defeat the shulkers", killcount, 18);
                for (int i = 0; i < 4; i++) {
                    spawnMob(EntityType.SHULKER, 2);
                }
                break;
            default:
                break;
        }
    }

    @Override
    protected void onComplete() {
        if (GodHelper.getGodOrNull(player, GodsRegistry.AETHON.get()) != null) {
            GodHelper.increaseFavor(GodsRegistry.AETHON.get(), player, 50);
        }
        player.displayClientMessage(Component.literal("Aethon: The cosmos acknowledges your triumph."), false);
    }

    @Override
    protected int getMaxPhases() {
        return 3;
    }

    @Override
    protected boolean checkFailCondition() {
        return killcount < 18 && !aliveMobs.isEmpty();
    }

    @Override
    public void onKillEntity(Player player, LivingDeathEvent event) {
        if (!aliveMobs.contains(event.getEntity())) {
            return;
        }

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

    private void clearAliveMobs() {
        if (aliveMobs.isEmpty()) {
            return;
        }

        for (LivingEntity living : aliveMobs) {
            living.kill();
        }
        aliveMobs.clear();
    }

    private void spawnMob(EntityType<? extends Mob> type, int yOffset) {
        LivingEntity spawned = type.spawn(
                trialLevel,
                getArenaCenter().offset(
                        player.level().random.nextInt(-4, 4),
                        1 + yOffset,
                        player.level().random.nextInt(-4, 4)
                ),
                MobSpawnType.MOB_SUMMONED
        );
        if (spawned instanceof Mob mob) {
            mob.setTarget(player);
            trackEntity(spawned);
            aliveMobs.add(spawned);
        }
    }
}
