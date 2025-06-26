package net.nightshade.divinity_engine.network.events.divinity.blessing;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.nightshade.divinity_engine.DivinityEngineMod;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.network.cap.player.gods.GodsStorage;
import net.nightshade.divinity_engine.network.events.divinity.gods.ContactGodEvent;
import net.nightshade.divinity_engine.util.MainPlayerCapabilityHelper;
import net.nightshade.divinity_engine.util.divinity.gods.GodHelper;

import java.util.List;
import java.util.Optional;

@Mod.EventBusSubscriber(modid = DivinityEngineMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BlessingsEvents {
    @SubscribeEvent
    public static void onBlessingTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            Player player = event.player;
            if (!GodHelper.getContactedGodsFrom(player).getContactedGods().isEmpty()){
                BlessingsInstance blessingSlot1 = MainPlayerCapabilityHelper.getBlessingSlot1(player);
                BlessingsInstance blessingSlot2 = MainPlayerCapabilityHelper.getBlessingSlot2(player);
                BlessingsInstance blessingSlot3 = MainPlayerCapabilityHelper.getBlessingSlot3(player);
                if (blessingSlot1 != null && blessingSlot2 != null && blessingSlot3 != null) {
                    if (blessingSlot1.getCooldown() > 0){
                        blessingSlot1.onTick(player);
                    }
                    if (blessingSlot2.getCooldown() > 0){
                        blessingSlot2.onTick(player);
                    }
                    if (blessingSlot3.getCooldown() > 0){
                        blessingSlot3.onTick(player);
                    }
                }
            }
        }
    }
    @SubscribeEvent
    public static void onBlessingRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (!GodHelper.getContactedGodsFrom(player).getContactedGods().isEmpty()){
            BlessingsInstance blessingSlot1 = MainPlayerCapabilityHelper.getBlessingSlot1(player);
            BlessingsInstance blessingSlot2 = MainPlayerCapabilityHelper.getBlessingSlot2(player);
            BlessingsInstance blessingSlot3 = MainPlayerCapabilityHelper.getBlessingSlot3(player);
            if (blessingSlot1 != null && blessingSlot2 != null && blessingSlot3 != null) {
                if (blessingSlot1.getCooldown() > 0){
                    blessingSlot1.onRightClickBlock(event);
                }
                if (blessingSlot2.getCooldown() > 0){
                    blessingSlot2.onRightClickBlock(event);
                }
                if (blessingSlot3.getCooldown() > 0){
                    blessingSlot3.onRightClickBlock(event);
                }
            }
        }
    }
    @SubscribeEvent
    public static void onBlessingRightClickEntity(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        if (!GodHelper.getContactedGodsFrom(player).getContactedGods().isEmpty()){
            BlessingsInstance blessingSlot1 = MainPlayerCapabilityHelper.getBlessingSlot1(player);
            BlessingsInstance blessingSlot2 = MainPlayerCapabilityHelper.getBlessingSlot2(player);
            BlessingsInstance blessingSlot3 = MainPlayerCapabilityHelper.getBlessingSlot3(player);
            if (blessingSlot1 != null && blessingSlot2 != null && blessingSlot3 != null) {
                if (blessingSlot1.getCooldown() > 0){
                    blessingSlot1.onRightClickEntity(event);
                }
                if (blessingSlot2.getCooldown() > 0){
                    blessingSlot2.onRightClickEntity(event);
                }
                if (blessingSlot3.getCooldown() > 0){
                    blessingSlot3.onRightClickEntity(event);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBlessingRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (!GodHelper.getContactedGodsFrom(player).getContactedGods().isEmpty()){
            BlessingsInstance blessingSlot1 = MainPlayerCapabilityHelper.getBlessingSlot1(player);
            BlessingsInstance blessingSlot2 = MainPlayerCapabilityHelper.getBlessingSlot2(player);
            BlessingsInstance blessingSlot3 = MainPlayerCapabilityHelper.getBlessingSlot3(player);
            if (blessingSlot1 != null && blessingSlot2 != null && blessingSlot3 != null) {
                if (blessingSlot1.getCooldown() > 0){
                    blessingSlot1.onRightClickItem(event);
                }
                if (blessingSlot2.getCooldown() > 0){
                    blessingSlot2.onRightClickItem(event);
                }
                if (blessingSlot3.getCooldown() > 0){
                    blessingSlot3.onRightClickItem(event);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBlessingRightClickEmpty(PlayerInteractEvent.RightClickEmpty event) {
        Player player = event.getEntity();
        if (!GodHelper.getContactedGodsFrom(player).getContactedGods().isEmpty()){
            BlessingsInstance blessingSlot1 = MainPlayerCapabilityHelper.getBlessingSlot1(player);
            BlessingsInstance blessingSlot2 = MainPlayerCapabilityHelper.getBlessingSlot2(player);
            BlessingsInstance blessingSlot3 = MainPlayerCapabilityHelper.getBlessingSlot3(player);
            if (blessingSlot1 != null && blessingSlot2 != null && blessingSlot3 != null) {
                if (blessingSlot1.getCooldown() > 0){
                    blessingSlot1.onRightClickEmpty(event);
                }
                if (blessingSlot2.getCooldown() > 0){
                    blessingSlot2.onRightClickEmpty(event);
                }
                if (blessingSlot3.getCooldown() > 0){
                    blessingSlot3.onRightClickEmpty(event);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBlessingBeingTargeted(LivingChangeTargetEvent event) {
        LivingEntity target = event.getNewTarget();
        if (!GodHelper.getContactedGodsFrom(target).getContactedGods().isEmpty()){
            BlessingsInstance blessingSlot1 = MainPlayerCapabilityHelper.getBlessingSlot1(target);
            BlessingsInstance blessingSlot2 = MainPlayerCapabilityHelper.getBlessingSlot2(target);
            BlessingsInstance blessingSlot3 = MainPlayerCapabilityHelper.getBlessingSlot3(target);
            if (blessingSlot1 != null && blessingSlot2 != null && blessingSlot3 != null) {
                if (blessingSlot1.getCooldown() > 0){
                    blessingSlot1.onBeingTargeted(target, event);
                }
                if (blessingSlot2.getCooldown() > 0){
                    blessingSlot2.onBeingTargeted(target, event);
                }
                if (blessingSlot3.getCooldown() > 0){
                    blessingSlot3.onBeingTargeted(target, event);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBlessingBeingDamaged(LivingAttackEvent event) {
        LivingEntity player = event.getEntity();
        if (!GodHelper.getContactedGodsFrom(player).getContactedGods().isEmpty()){
            BlessingsInstance blessingSlot1 = MainPlayerCapabilityHelper.getBlessingSlot1(player);
            BlessingsInstance blessingSlot2 = MainPlayerCapabilityHelper.getBlessingSlot2(player);
            BlessingsInstance blessingSlot3 = MainPlayerCapabilityHelper.getBlessingSlot3(player);
            if (blessingSlot1 != null && blessingSlot2 != null && blessingSlot3 != null) {
                if (blessingSlot1.getCooldown() > 0){
                    blessingSlot1.onBeingDamaged(event);
                }
                if (blessingSlot2.getCooldown() > 0){
                    blessingSlot2.onBeingDamaged(event);
                }
                if (blessingSlot3.getCooldown() > 0){
                    blessingSlot3.onBeingDamaged(event);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBlessingDamageEntity(LivingHurtEvent event) {
        LivingEntity player = event.getSource().getEntity() instanceof LivingEntity ? (LivingEntity) event.getSource().getEntity() : null;
        if (!GodHelper.getContactedGodsFrom(player).getContactedGods().isEmpty()){
            BlessingsInstance blessingSlot1 = MainPlayerCapabilityHelper.getBlessingSlot1(player);
            BlessingsInstance blessingSlot2 = MainPlayerCapabilityHelper.getBlessingSlot2(player);
            BlessingsInstance blessingSlot3 = MainPlayerCapabilityHelper.getBlessingSlot3(player);
            if (blessingSlot1 != null && blessingSlot2 != null && blessingSlot3 != null) {
                if (blessingSlot1.getCooldown() > 0){
                    blessingSlot1.onDamageEntity(player,event);
                }
                if (blessingSlot2.getCooldown() > 0){
                    blessingSlot2.onDamageEntity(player,event);
                }
                if (blessingSlot3.getCooldown() > 0){
                    blessingSlot3.onDamageEntity(player,event);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBlessingDamageEntity(LivingDamageEvent event) {
        LivingEntity player = event.getEntity();
        if (!GodHelper.getContactedGodsFrom(player).getContactedGods().isEmpty()){
            BlessingsInstance blessingSlot1 = MainPlayerCapabilityHelper.getBlessingSlot1(player);
            BlessingsInstance blessingSlot2 = MainPlayerCapabilityHelper.getBlessingSlot2(player);
            BlessingsInstance blessingSlot3 = MainPlayerCapabilityHelper.getBlessingSlot3(player);
            if (blessingSlot1 != null && blessingSlot2 != null && blessingSlot3 != null) {
                if (blessingSlot1.getCooldown() > 0){
                    blessingSlot1.onTakenDamage(event);
                }
                if (blessingSlot2.getCooldown() > 0){
                    blessingSlot2.onTakenDamage(event);
                }
                if (blessingSlot3.getCooldown() > 0){
                    blessingSlot3.onTakenDamage(event);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBlessingProjectileHit(ProjectileImpactEvent event) {
        HitResult var2 = event.getRayTraceResult();
        if (var2 instanceof EntityHitResult result) {
            Entity var3 = result.getEntity();
            if (var3 instanceof LivingEntity player) {
                if (!GodHelper.getContactedGodsFrom(player).getContactedGods().isEmpty()){
                    BlessingsInstance blessingSlot1 = MainPlayerCapabilityHelper.getBlessingSlot1(player);
                    BlessingsInstance blessingSlot2 = MainPlayerCapabilityHelper.getBlessingSlot2(player);
                    BlessingsInstance blessingSlot3 = MainPlayerCapabilityHelper.getBlessingSlot3(player);
                    if (blessingSlot1 != null && blessingSlot2 != null && blessingSlot3 != null) {
                        if (blessingSlot1.getCooldown() > 0){
                            blessingSlot1.onProjectileHit(player, event);
                        }
                        if (blessingSlot2.getCooldown() > 0){
                            blessingSlot2.onProjectileHit(player, event);
                        }
                        if (blessingSlot3.getCooldown() > 0){
                            blessingSlot3.onProjectileHit(player, event);
                        }
                        if (event.isCanceled()) {
                            Projectile var9 = event.getProjectile();
                            if (var9 instanceof AbstractArrow) {
                                AbstractArrow arrow = (AbstractArrow)var9;
                                if (arrow.piercingIgnoreEntityIds == null) {
                                    arrow.piercingIgnoreEntityIds = new IntOpenHashSet(10);
                                }
                                arrow.piercingIgnoreEntityIds.add(player.getId());
                            }
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBlessingDeath(LivingDeathEvent event) {
        LivingEntity player = event.getEntity();
        if (!GodHelper.getContactedGodsFrom(player).getContactedGods().isEmpty()){
            BlessingsInstance blessingSlot1 = MainPlayerCapabilityHelper.getBlessingSlot1(player);
            BlessingsInstance blessingSlot2 = MainPlayerCapabilityHelper.getBlessingSlot2(player);
            BlessingsInstance blessingSlot3 = MainPlayerCapabilityHelper.getBlessingSlot3(player);
            if (blessingSlot1 != null && blessingSlot2 != null && blessingSlot3 != null) {
                if (blessingSlot1.getCooldown() > 0){
                    blessingSlot1.onDeath(event);
                }
                if (blessingSlot2.getCooldown() > 0){
                    blessingSlot2.onDeath(event);
                }
                if (blessingSlot3.getCooldown() > 0){
                    blessingSlot3.onDeath(event);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBlessingRespawn(PlayerEvent.PlayerRespawnEvent event) {
        LivingEntity player = event.getEntity();
        if (!GodHelper.getContactedGodsFrom(player).getContactedGods().isEmpty()){
            BlessingsInstance blessingSlot1 = MainPlayerCapabilityHelper.getBlessingSlot1(player);
            BlessingsInstance blessingSlot2 = MainPlayerCapabilityHelper.getBlessingSlot2(player);
            BlessingsInstance blessingSlot3 = MainPlayerCapabilityHelper.getBlessingSlot3(player);
            if (blessingSlot1 != null && blessingSlot2 != null && blessingSlot3 != null) {
                if (blessingSlot1.getCooldown() > 0){
                    blessingSlot1.onRespawn(event);
                }
                if (blessingSlot2.getCooldown() > 0){
                    blessingSlot2.onRespawn(event);
                }
                if (blessingSlot3.getCooldown() > 0){
                    blessingSlot3.onRespawn(event);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBlessingHeal(LivingHealEvent event) {
        LivingEntity player = event.getEntity();
        if (!GodHelper.getContactedGodsFrom(player).getContactedGods().isEmpty()){
            BlessingsInstance blessingSlot1 = MainPlayerCapabilityHelper.getBlessingSlot1(player);
            BlessingsInstance blessingSlot2 = MainPlayerCapabilityHelper.getBlessingSlot2(player);
            BlessingsInstance blessingSlot3 = MainPlayerCapabilityHelper.getBlessingSlot3(player);
            if (blessingSlot1 != null && blessingSlot2 != null && blessingSlot3 != null) {
                if (blessingSlot1.getCooldown() > 0){
                    blessingSlot1.onHeal(event);
                }
                if (blessingSlot2.getCooldown() > 0){
                    blessingSlot2.onHeal(event);
                }
                if (blessingSlot3.getCooldown() > 0){
                    blessingSlot3.onHeal(event);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBlessingKnockback(LivingKnockBackEvent event) {
        LivingEntity player = event.getEntity();
        if (!GodHelper.getContactedGodsFrom(player).getContactedGods().isEmpty()){
            BlessingsInstance blessingSlot1 = MainPlayerCapabilityHelper.getBlessingSlot1(player);
            BlessingsInstance blessingSlot2 = MainPlayerCapabilityHelper.getBlessingSlot2(player);
            BlessingsInstance blessingSlot3 = MainPlayerCapabilityHelper.getBlessingSlot3(player);
            if (blessingSlot1 != null && blessingSlot2 != null && blessingSlot3 != null) {
                if (blessingSlot1.getCooldown() > 0){
                    blessingSlot1.onKnockback(event);
                }
                if (blessingSlot2.getCooldown() > 0){
                    blessingSlot2.onKnockback(event);
                }
                if (blessingSlot3.getCooldown() > 0){
                    blessingSlot3.onKnockback(event);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBlessingFall(LivingFallEvent event) {
        LivingEntity player = event.getEntity();
        if (!GodHelper.getContactedGodsFrom(player).getContactedGods().isEmpty()){
            BlessingsInstance blessingSlot1 = MainPlayerCapabilityHelper.getBlessingSlot1(player);
            BlessingsInstance blessingSlot2 = MainPlayerCapabilityHelper.getBlessingSlot2(player);
            BlessingsInstance blessingSlot3 = MainPlayerCapabilityHelper.getBlessingSlot3(player);
            if (blessingSlot1 != null && blessingSlot2 != null && blessingSlot3 != null) {
                if (blessingSlot1.getCooldown() > 0){
                    blessingSlot1.onFall(event);
                }
                if (blessingSlot2.getCooldown() > 0){
                    blessingSlot2.onFall(event);
                }
                if (blessingSlot3.getCooldown() > 0){
                    blessingSlot3.onFall(event);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBlessingEXPDrop(LivingExperienceDropEvent event) {
        LivingEntity player = event.getEntity();
        if (!GodHelper.getContactedGodsFrom(player).getContactedGods().isEmpty()){
            BlessingsInstance blessingSlot1 = MainPlayerCapabilityHelper.getBlessingSlot1(player);
            BlessingsInstance blessingSlot2 = MainPlayerCapabilityHelper.getBlessingSlot2(player);
            BlessingsInstance blessingSlot3 = MainPlayerCapabilityHelper.getBlessingSlot3(player);
            if (blessingSlot1 != null && blessingSlot2 != null && blessingSlot3 != null) {
                if (blessingSlot1.getCooldown() > 0){
                    blessingSlot1.onEXPDrop(event);
                }
                if (blessingSlot2.getCooldown() > 0){
                    blessingSlot2.onEXPDrop(event);
                }
                if (blessingSlot3.getCooldown() > 0){
                    blessingSlot3.onEXPDrop(event);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBlessingEXPPickup(PlayerXpEvent.PickupXp event) {
        LivingEntity player = event.getEntity();
        if (!GodHelper.getContactedGodsFrom(player).getContactedGods().isEmpty()){
            BlessingsInstance blessingSlot1 = MainPlayerCapabilityHelper.getBlessingSlot1(player);
            BlessingsInstance blessingSlot2 = MainPlayerCapabilityHelper.getBlessingSlot2(player);
            BlessingsInstance blessingSlot3 = MainPlayerCapabilityHelper.getBlessingSlot3(player);
            if (blessingSlot1 != null && blessingSlot2 != null && blessingSlot3 != null) {
                if (blessingSlot1.getCooldown() > 0){
                    blessingSlot1.onEXPPickup(event);
                }
                if (blessingSlot2.getCooldown() > 0){
                    blessingSlot2.onEXPPickup(event);
                }
                if (blessingSlot3.getCooldown() > 0){
                    blessingSlot3.onEXPPickup(event);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBlessingJump(LivingEvent.LivingJumpEvent event) {
        LivingEntity player = event.getEntity();
        if (player instanceof Player player1){
            player1.displayClientMessage(Component.literal("jump"), false);
        }
        if (!GodHelper.getContactedGodsFrom(player).getContactedGods().isEmpty()){
            BlessingsInstance blessingSlot1 = MainPlayerCapabilityHelper.getBlessingSlot1(player);
            BlessingsInstance blessingSlot2 = MainPlayerCapabilityHelper.getBlessingSlot2(player);
            BlessingsInstance blessingSlot3 = MainPlayerCapabilityHelper.getBlessingSlot3(player);
            if (blessingSlot1 != null && blessingSlot2 != null && blessingSlot3 != null) {
                if (blessingSlot1.getCooldown() > 0){
                    blessingSlot1.onJump(event);
                }
                if (blessingSlot2.getCooldown() > 0){
                    blessingSlot2.onJump(event);
                }
                if (blessingSlot3.getCooldown() > 0){
                    blessingSlot3.onJump(event);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBlessingDrowning(LivingDrownEvent event) {
        LivingEntity player = event.getEntity();
        if (!GodHelper.getContactedGodsFrom(player).getContactedGods().isEmpty()){
            BlessingsInstance blessingSlot1 = MainPlayerCapabilityHelper.getBlessingSlot1(player);
            BlessingsInstance blessingSlot2 = MainPlayerCapabilityHelper.getBlessingSlot2(player);
            BlessingsInstance blessingSlot3 = MainPlayerCapabilityHelper.getBlessingSlot3(player);
            if (blessingSlot1 != null && blessingSlot2 != null && blessingSlot3 != null) {
                if (blessingSlot1.getCooldown() > 0){
                    blessingSlot1.onDrowning(event);
                }
                if (blessingSlot2.getCooldown() > 0){
                    blessingSlot2.onDrowning(event);
                }
                if (blessingSlot3.getCooldown() > 0){
                    blessingSlot3.onDrowning(event);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBlessingBreathe(LivingBreatheEvent event) {
        LivingEntity player = event.getEntity();
        if (!GodHelper.getContactedGodsFrom(player).getContactedGods().isEmpty()){
            BlessingsInstance blessingSlot1 = MainPlayerCapabilityHelper.getBlessingSlot1(player);
            BlessingsInstance blessingSlot2 = MainPlayerCapabilityHelper.getBlessingSlot2(player);
            BlessingsInstance blessingSlot3 = MainPlayerCapabilityHelper.getBlessingSlot3(player);
            if (blessingSlot1 != null && blessingSlot2 != null && blessingSlot3 != null) {
                if (blessingSlot1.getCooldown() > 0){
                    blessingSlot1.onBreathe(event);
                }
                if (blessingSlot2.getCooldown() > 0){
                    blessingSlot2.onBreathe(event);
                }
                if (blessingSlot3.getCooldown() > 0){
                    blessingSlot3.onBreathe(event);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBlessingBlocksWithShield(ShieldBlockEvent event) {
        LivingEntity player = event.getEntity();
        if (!GodHelper.getContactedGodsFrom(player).getContactedGods().isEmpty()){
            BlessingsInstance blessingSlot1 = MainPlayerCapabilityHelper.getBlessingSlot1(player);
            BlessingsInstance blessingSlot2 = MainPlayerCapabilityHelper.getBlessingSlot2(player);
            BlessingsInstance blessingSlot3 = MainPlayerCapabilityHelper.getBlessingSlot3(player);
            if (blessingSlot1 != null && blessingSlot2 != null && blessingSlot3 != null) {
                if (blessingSlot1.getCooldown() > 0){
                    blessingSlot1.onBlocksWithShield(event);
                }
                if (blessingSlot2.getCooldown() > 0){
                    blessingSlot2.onBlocksWithShield(event);
                }
                if (blessingSlot3.getCooldown() > 0){
                    blessingSlot3.onBlocksWithShield(event);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBlessingBreakBlock(BlockEvent.BreakEvent event) {
        LivingEntity player = event.getPlayer();
        if (!GodHelper.getContactedGodsFrom(player).getContactedGods().isEmpty()){
            BlessingsInstance blessingSlot1 = MainPlayerCapabilityHelper.getBlessingSlot1(player);
            BlessingsInstance blessingSlot2 = MainPlayerCapabilityHelper.getBlessingSlot2(player);
            BlessingsInstance blessingSlot3 = MainPlayerCapabilityHelper.getBlessingSlot3(player);
            if (blessingSlot1 != null && blessingSlot2 != null && blessingSlot3 != null) {
                if (blessingSlot1.getCooldown() > 0){
                    blessingSlot1.onBreakBlock(event);
                }
                if (blessingSlot2.getCooldown() > 0){
                    blessingSlot2.onBreakBlock(event);
                }
                if (blessingSlot3.getCooldown() > 0){
                    blessingSlot3.onBreakBlock(event);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBlessingPlaceBlock(BlockEvent.EntityPlaceEvent event) {
        LivingEntity player = event.getEntity() instanceof LivingEntity ? (LivingEntity) event.getEntity() : null;
        if (!GodHelper.getContactedGodsFrom(player).getContactedGods().isEmpty()){
            BlessingsInstance blessingSlot1 = MainPlayerCapabilityHelper.getBlessingSlot1(player);
            BlessingsInstance blessingSlot2 = MainPlayerCapabilityHelper.getBlessingSlot2(player);
            BlessingsInstance blessingSlot3 = MainPlayerCapabilityHelper.getBlessingSlot3(player);
            if (blessingSlot1 != null && blessingSlot2 != null && blessingSlot3 != null) {
                if (blessingSlot1.getCooldown() > 0){
                    blessingSlot1.onPlaceBlock(event);
                }
                if (blessingSlot2.getCooldown() > 0){
                    blessingSlot2.onPlaceBlock(event);
                }
                if (blessingSlot3.getCooldown() > 0){
                    blessingSlot3.onPlaceBlock(event);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBlessingItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        LivingEntity player = event.getEntity() instanceof LivingEntity ? (LivingEntity) event.getEntity() : null;
        if (!GodHelper.getContactedGodsFrom(player).getContactedGods().isEmpty()){
            BlessingsInstance blessingSlot1 = MainPlayerCapabilityHelper.getBlessingSlot1(player);
            BlessingsInstance blessingSlot2 = MainPlayerCapabilityHelper.getBlessingSlot2(player);
            BlessingsInstance blessingSlot3 = MainPlayerCapabilityHelper.getBlessingSlot3(player);
            if (blessingSlot1 != null && blessingSlot2 != null && blessingSlot3 != null) {
                if (blessingSlot1.getCooldown() > 0){
                    blessingSlot1.onItemCrafted(event);
                }
                if (blessingSlot2.getCooldown() > 0){
                    blessingSlot2.onItemCrafted(event);
                }
                if (blessingSlot3.getCooldown() > 0){
                    blessingSlot3.onItemCrafted(event);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBlessingItemPickup(PlayerEvent.ItemPickupEvent event) {
        LivingEntity player = event.getEntity();
        if (!GodHelper.getContactedGodsFrom(player).getContactedGods().isEmpty()){
            BlessingsInstance blessingSlot1 = MainPlayerCapabilityHelper.getBlessingSlot1(player);
            BlessingsInstance blessingSlot2 = MainPlayerCapabilityHelper.getBlessingSlot2(player);
            BlessingsInstance blessingSlot3 = MainPlayerCapabilityHelper.getBlessingSlot3(player);
            if (blessingSlot1 != null && blessingSlot2 != null && blessingSlot3 != null) {
                if (blessingSlot1.getCooldown() > 0){
                    blessingSlot1.onItemPickup(event);
                }
                if (blessingSlot2.getCooldown() > 0){
                    blessingSlot2.onItemPickup(event);
                }
                if (blessingSlot3.getCooldown() > 0){
                    blessingSlot3.onItemPickup(event);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBlessingItemSmelted(PlayerEvent.ItemSmeltedEvent event) {
        LivingEntity player = event.getEntity();
        if (!GodHelper.getContactedGodsFrom(player).getContactedGods().isEmpty()){
            BlessingsInstance blessingSlot1 = MainPlayerCapabilityHelper.getBlessingSlot1(player);
            BlessingsInstance blessingSlot2 = MainPlayerCapabilityHelper.getBlessingSlot2(player);
            BlessingsInstance blessingSlot3 = MainPlayerCapabilityHelper.getBlessingSlot3(player);
            if (blessingSlot1 != null && blessingSlot2 != null && blessingSlot3 != null) {
                if (blessingSlot1.getCooldown() > 0){
                    blessingSlot1.onItemSmelted(event);
                }
                if (blessingSlot2.getCooldown() > 0){
                    blessingSlot2.onItemSmelted(event);
                }
                if (blessingSlot3.getCooldown() > 0){
                    blessingSlot3.onItemSmelted(event);
                }
            }
        }
    }
}
