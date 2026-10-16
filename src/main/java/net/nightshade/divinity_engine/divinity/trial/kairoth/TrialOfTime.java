package net.nightshade.divinity_engine.divinity.trial.kairoth;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.AbstractSkeleton;
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

public class TrialOfTime extends GodlyTrial {
    private int killcount = 0;
    private int zombieWaveCount = 0;
    private int vexWaveCount = 0;
    private final List<LivingEntity> aliveMobs = new ArrayList<>();

    public TrialOfTime(ServerPlayer player) {
        super(player);
    }

    @Override
    protected void onStart() {
        player.displayClientMessage(Component.literal("Kairoth: Bend the sands of time and prove your destiny."), false);
    }

    @Override
    public void tick() {
        syncTrackedEntities(aliveMobs);

        if (phase == 1) {
            if (getTicksElapsed() > 0 && getTicksElapsed() % 80 == 0 && zombieWaveCount < 3) {
                spawnZombieWave();
            }
            if (killcount >= 12) {
                nextPhase();
            }
        } else if (phase == 2) {
            if (killcount >= 20) {
                nextPhase();
            }
        } else if (phase == 3) {
            if (getTicksElapsed() > 0 && getTicksElapsed() % 120 == 0 && vexWaveCount < 3) {
                spawnVexSwarm();
            }
            if (killcount >= 30) {
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
                zombieWaveCount = 0;
                setPhaseInfo("Phase 1", "Stop the zombie rush", killcount, 12);
                spawnZombieWave();
                break;
            case 2:
                phaseDuration = 800;
                setPhaseInfo("Phase 2", "Break the skeleton line", killcount, 20);
                for (int i = 0; i < 8; i++) {
                    spawnQuickDrawSkeleton();
                }
                break;
            case 3:
                phaseDuration = 1000;
                vexWaveCount = 0;
                setPhaseInfo("Phase 3", "Overcome the time swarm", killcount, 30);
                spawnEvoker();
                for (int i = 0; i < 3; i++) {
                    spawnVex();
                }
                break;
            default:
                break;
        }
    }

    @Override
    protected void onComplete() {
        if (GodHelper.getGodOrNull(player, GodsRegistry.KAIROTH.get()) != null) {
            GodHelper.increaseFavor(GodsRegistry.KAIROTH.get(), player, 50);
        }
        player.displayClientMessage(Component.literal("Kairoth: Your path through time honors my will."), false);
    }

    @Override
    protected int getMaxPhases() {
        return 3;
    }

    @Override
    protected boolean checkFailCondition() {
        return killcount < 30 && !aliveMobs.isEmpty();
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

    private void spawnZombieWave() {
        zombieWaveCount++;
        for (int i = 0; i < 4; i++) {
            LivingEntity spawned = EntityType.ZOMBIE.spawn(
                    trialLevel,
                    getArenaCenter().offset(
                            player.level().random.nextInt(-4, 4),
                            1,
                            player.level().random.nextInt(-4, 4)
                    ),
                    MobSpawnType.MOB_SUMMONED
            );
            if (spawned instanceof Mob mob) {
                mob.setTarget(player);
                mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 30, 1, false, false, true));
                trackEntity(spawned);
                aliveMobs.add(spawned);
            }
        }
    }

    private void spawnQuickDrawSkeleton() {
        LivingEntity spawned = EntityType.SKELETON.spawn(
                trialLevel,
                getArenaCenter().offset(
                        player.level().random.nextInt(-4, 4),
                        1,
                        player.level().random.nextInt(-4, 4)
                ),
                MobSpawnType.MOB_SUMMONED
        );

        if (spawned instanceof AbstractSkeleton skeleton) {
            ItemStack bow = new ItemStack(Items.BOW);
            bow.enchant(Enchantments.POWER_ARROWS, 2);
            bow.enchant(Enchantments.PUNCH_ARROWS, 1);
            skeleton.setItemSlot(EquipmentSlot.MAINHAND, bow);
            skeleton.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 40, 1, false, false, true));
            skeleton.setTarget(player);
            trackEntity(skeleton);
            aliveMobs.add(skeleton);
        }
    }

    private void spawnEvoker() {
        LivingEntity spawned = EntityType.EVOKER.spawn(
                trialLevel,
                getArenaCenter().offset(
                        player.level().random.nextInt(-3, 3),
                        1,
                        player.level().random.nextInt(-3, 3)
                ),
                MobSpawnType.MOB_SUMMONED
        );

        if (spawned instanceof Mob mob) {
            mob.setTarget(player);
            trackEntity(spawned);
            aliveMobs.add(spawned);
        }
    }

    private void spawnVexSwarm() {
        vexWaveCount++;
        for (int i = 0; i < 2; i++) {
            spawnVex();
        }
    }

    private void spawnVex() {
        LivingEntity spawned = EntityType.VEX.spawn(
                trialLevel,
                getArenaCenter().offset(
                        player.level().random.nextInt(-4, 4),
                        2,
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
