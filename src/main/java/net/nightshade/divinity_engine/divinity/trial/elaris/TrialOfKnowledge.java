package net.nightshade.divinity_engine.divinity.trial.elaris;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.nightshade.divinity_engine.divinity.trial.GodlyTrial;
import net.nightshade.divinity_engine.registry.divinity.gods.GodsRegistry;
import net.nightshade.divinity_engine.util.divinity.gods.GodHelper;
import net.nightshade.vanguards_kit.core.battle_aspects.BattleAspect;
import net.nightshade.vanguards_kit.registry.battle_aspects.BattleAspectsRegistry;
import net.nightshade.vanguards_kit.util.VanguardHelper;
import net.nightshade.vanguards_kit.util.aspects.AspectsHelper;

public class TrialOfKnowledge extends GodlyTrial {
    public TrialOfKnowledge(ServerPlayer player) {
        super(player);
    }

    @Override
    protected void onStart() {
        player.displayClientMessage(Component.literal("Elaris: Gain access to my endless knowledge, but before use your knowledge to complete my trial."), false);

        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.SHIELD).copy());
        AspectsHelper.applyAspect(player.getItemInHand(InteractionHand.OFF_HAND), "iron_wall");

        player.forceAddEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, 40, 2, false, false, true), player);
        player.heal(player.getMaxHealth());
    }

    @Override
    public void tick() {
        player.forceAddEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, 40, 2, false, false, true), player);
        int healthPercent = Math.round((player.getHealth() / player.getMaxHealth()) * 100.0F);
        setObjective("Stay above 25% health", healthPercent, 100);

        if (player.getHealth() <= player.getMaxHealth() * 0.25F) {
            failTrial();
            return;
        }

        if (phase == 1 && getTicksElapsed() >= phaseDuration - 1) {
            nextPhase();
            return;
        }

        super.tick();
    }

    @Override
    protected void runPhase(int phase) {

        switch (phase) {
            case 1:
                phaseDuration = 600;
                setPhaseInfo("Phase 1", "Stay above 25% health", Math.round((player.getHealth() / player.getMaxHealth()) * 100.0F), 100);
                for (int i = 0; i < 3; i++) {
                    LivingEntity entityToSpawn = EntityType.IRON_GOLEM.spawn(trialLevel, getArenaCenter().offset(player.level().random.nextInt(-4,4), 1, player.level().random.nextInt(-4,4)), MobSpawnType.MOB_SUMMONED);
                    if (entityToSpawn instanceof Mob living){
                        living.setTarget(player);
                        trackEntity(entityToSpawn);
                    }
                }
                break;
            case 2:
                setPhaseInfo("Phase 2", "Remain above 25% health", Math.round((player.getHealth() / player.getMaxHealth()) * 100.0F), 100);
                if (player.getHealth() > player.getMaxHealth()*0.25)
                    completeTrial();
        }
    }

    @Override
    protected int getMaxPhases() {
        return 2;
    }

    @Override
    protected boolean checkFailCondition() {
        return player.getHealth() <= player.getMaxHealth()*0.25;
    }

    @Override
    protected void onComplete() {
        if (GodHelper.getGodOrNull(player, GodsRegistry.ELARIS.get()) != null)
            GodHelper.increaseFavor(GodsRegistry.ELARIS.get(), player, 50);
        player.displayClientMessage(Component.literal("Elaris: Your insight has uncovered the truths I guard."), false);
    }

    @Override
    protected void completeTrial() {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() == Items.SHIELD) {
                if (AspectsHelper.hasAspects(stack)){
                    if (stack.getOrCreateTag().getString("vanguards_kit_aspect").equals("iron_wall")){
                        stack.shrink(1);
                    }
                }
            }
        }
        super.completeTrial();
    }

    @Override
    public void failTrial() {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() == Items.SHIELD) {
                if (AspectsHelper.hasAspects(stack)){
                    if (stack.getOrCreateTag().getString("vanguards_kit_aspect").equals("iron_wall")){
                        stack.shrink(1);
                    }
                }
            }
        }
        super.failTrial();
    }

    @Override
    public void onKillEntity(Player player, LivingDeathEvent event) {
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
        player.displayClientMessage(Component.literal("Elaris: You must have nothing in your inventory to start this trial."), false);
        return false;
    }

    @Override
    public void onHeal(LivingHealEvent event) {

        event.setCanceled(true);
        super.onHeal(event);
    }
}
