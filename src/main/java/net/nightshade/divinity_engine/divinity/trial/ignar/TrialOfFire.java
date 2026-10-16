package net.nightshade.divinity_engine.divinity.trial.ignar;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.MagmaBlock;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.nightshade.divinity_engine.divinity.trial.GodlyTrial;
import net.nightshade.divinity_engine.registry.divinity.gods.GodsRegistry;
import net.nightshade.divinity_engine.util.divinity.gods.GodHelper;

import java.util.ArrayList;
import java.util.List;

public class TrialOfFire extends GodlyTrial {
    public TrialOfFire(ServerPlayer player) {
        super(player);
    }

    protected int killcount = 0;
    protected List<LivingEntity> aliveMobs = new ArrayList<>();

    @Override
    protected void onStart() {
        player.displayClientMessage(Component.literal("Ignar: Endure the flames and show your unrelenting power."), false);
    }

    @Override
    public void tick() {
        syncTrackedEntities(aliveMobs);
        if (phase == 1) {
            for(LivingEntity living: aliveMobs){
                living.setSecondsOnFire(1);
            }
            if (killcount == 5) {
                nextPhase();
            }
        } else if (phase == 2) {
            if (killcount == 10) {
                nextPhase();
            }else {
            }
        }else if (phase == 3) {
            if (killcount == 20) {
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
                setPhaseInfo("Phase 1", "Defeat the burning undead", killcount, 5);
                for (int i = 0; i < 5; i++) {
                    LivingEntity entityToSpawn = EntityType.ZOMBIE.spawn(trialLevel, getArenaCenter().offset(player.level().random.nextInt(-4,4), 1, player.level().random.nextInt(-4,4)), MobSpawnType.MOB_SUMMONED);
                    entityToSpawn.setItemSlot(EquipmentSlot.MAINHAND, Items.BLAZE_ROD.getDefaultInstance());
                    if (entityToSpawn instanceof Mob living){
                        living.setTarget(player);
                        living.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 100, 999999999, false, false));
                        aliveMobs.add(living);
                    }
                }
                break;
            case 2:
                phaseDuration = 800;
                setPhaseInfo("Phase 2", "Defeat the blazes", killcount, 10);
                if (aliveMobs.size() > 0){
                    for (LivingEntity living: aliveMobs){
                        living.kill();
                    }
                    aliveMobs.clear();
                }
                for (int i = 0; i < 5; i++) {
                    LivingEntity entityToSpawn = EntityType.BLAZE.spawn(trialLevel, getArenaCenter().offset(player.level().random.nextInt(-4,4), 1, player.level().random.nextInt(-4,4)), MobSpawnType.MOB_SUMMONED);
                    if (entityToSpawn instanceof Mob living){
                        living.setTarget(player);
                        aliveMobs.add(living);
                    }
                }
                break;
            case 3:
                setPhaseInfo("Phase 3", "Survive the inferno", killcount, 20);
                if (!aliveMobs.isEmpty()){
                    for (LivingEntity living: aliveMobs){
                        living.kill();
                    }
                    aliveMobs.clear();
                }

                for (int i = 0; i < 5; i++) {
                    LivingEntity entityToSpawn = EntityType.BLAZE.spawn(trialLevel, getArenaCenter().offset(player.level().random.nextInt(-4,4), 1, player.level().random.nextInt(-4,4)), MobSpawnType.MOB_SUMMONED);
                    if (entityToSpawn instanceof Mob living){
                        living.setTarget(player);
                        aliveMobs.add(living);
                    }
                }
                for (int i = 0; i < 5; i++) {
                    LivingEntity entityToSpawn = EntityType.ZOMBIFIED_PIGLIN.spawn(trialLevel, getArenaCenter().offset(player.level().random.nextInt(-4,4), 1, player.level().random.nextInt(-4,4)), MobSpawnType.MOB_SUMMONED);
                    if (entityToSpawn instanceof Mob living){
                        if (entityToSpawn instanceof MagmaCube magmaBlock){
                            magmaBlock.setSize(5, true);
                        }
                        ItemStack stack = Items.GOLDEN_SWORD.getDefaultInstance();

                        stack.enchant(Enchantments.SHARPNESS, 1);
                        stack.enchant(Enchantments.FIRE_ASPECT, 1);

                        living.setItemSlot(EquipmentSlot.MAINHAND, stack);
                        living.setTarget(player);
                        aliveMobs.add(living);
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
        return killcount < 14 && !aliveMobs.isEmpty();
    }

    @Override
    protected void onComplete() {
        if (GodHelper.getGodOrNull(player, GodsRegistry.IGNAR.get()) != null)
            GodHelper.increaseFavor(GodsRegistry.IGNAR.get(), player, 50);
        player.displayClientMessage(Component.literal("Ignar: Your fire burns bright and earns my favor."), false);
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
