package net.nightshade.divinity_engine.network.events.divinity.curse;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.nightshade.divinity_engine.DivinityEngineMod;
import net.nightshade.divinity_engine.divinity.curse.CurseInstance;
import net.nightshade.divinity_engine.util.MainPlayerCapabilityHelper;
import net.nightshade.divinity_engine.util.divinity.gods.GodHelper;

@Mod.EventBusSubscriber(modid = DivinityEngineMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CurseEvents {
    @SubscribeEvent
    public static void onCurseTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            Player player = event.player;
            if (!GodHelper.getContactedGodsFrom(player).getCurses().isEmpty()){
                for (CurseInstance curseInstance : GodHelper.getContactedGodsFrom(player).getCurses()) {
                    curseInstance.onTick(player);
                }
            }
        }
    }
    @SubscribeEvent
    public static void onCurseRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (!GodHelper.getContactedGodsFrom(player).getCurses().isEmpty()){
            for (CurseInstance curseInstance : GodHelper.getContactedGodsFrom(player).getCurses()) {
                curseInstance.onRightClickBlock(event);
            }
        }
    }
    @SubscribeEvent
    public static void onCurseRightClickEntity(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        if (!GodHelper.getContactedGodsFrom(player).getCurses().isEmpty()){
            for (CurseInstance curseInstance : GodHelper.getContactedGodsFrom(player).getCurses()) {
                curseInstance.onRightClickEntity(event);
            }
        }
    }

    @SubscribeEvent
    public static void onCurseRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (!GodHelper.getContactedGodsFrom(player).getCurses().isEmpty()){
            for (CurseInstance curseInstance : GodHelper.getContactedGodsFrom(player).getCurses()) {
                curseInstance.onRightClickItem(event);
            }
        }
    }

    @SubscribeEvent
    public static void onCurseRightClickEmpty(PlayerInteractEvent.RightClickEmpty event) {
        Player player = event.getEntity();
        if (!GodHelper.getContactedGodsFrom(player).getCurses().isEmpty()){
            for (CurseInstance curseInstance : GodHelper.getContactedGodsFrom(player).getCurses()) {
                curseInstance.onRightClickEmpty(event);
            }
        }
    }

    @SubscribeEvent
    public static void onCurseBeingTargeted(LivingChangeTargetEvent event) {
        LivingEntity target = event.getNewTarget();
        if (target instanceof Player player) {
            if (!GodHelper.getContactedGodsFrom(player).getCurses().isEmpty()){
                for (CurseInstance curseInstance : GodHelper.getContactedGodsFrom(player).getCurses()) {
                    curseInstance.onBeingTargeted(player, event);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onCurseBeingDamaged(LivingAttackEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player player) {
            if (!GodHelper.getContactedGodsFrom(player).getCurses().isEmpty()){
                for (CurseInstance curseInstance : GodHelper.getContactedGodsFrom(player).getCurses()) {
                    curseInstance.onBeingDamaged(event);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onCurseDamageEntity(LivingHurtEvent event) {
        LivingEntity entity = event.getSource().getEntity() instanceof LivingEntity ? (LivingEntity) event.getSource().getEntity() : null;
        if (entity instanceof Player player) {
            if (!GodHelper.getContactedGodsFrom(player).getCurses().isEmpty()){
                for (CurseInstance curseInstance : GodHelper.getContactedGodsFrom(player).getCurses()) {
                    curseInstance.onDamageEntity(player, event);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onCurseCriticalHit(CriticalHitEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player player) {
            if (!GodHelper.getContactedGodsFrom(player).getCurses().isEmpty()){
                for (CurseInstance curseInstance : GodHelper.getContactedGodsFrom(player).getCurses()) {
                    curseInstance.onCriticalHit(event);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onCurseTakenDamage(LivingDamageEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player player) {
            if (!GodHelper.getContactedGodsFrom(player).getCurses().isEmpty()){
                for (CurseInstance curseInstance : GodHelper.getContactedGodsFrom(player).getCurses()) {
                    curseInstance.onTakenDamage(event);
                }
            }
        }

    }

    @SubscribeEvent
    public static void onCurseProjectileHit(ProjectileImpactEvent event) {
        HitResult var2 = event.getRayTraceResult();
        if (var2 instanceof EntityHitResult result) {
            Entity var3 = result.getEntity();
            if (var3 instanceof Player player) {
                if (!GodHelper.getContactedGodsFrom(player).getCurses().isEmpty()){
                    for (CurseInstance curseInstance : GodHelper.getContactedGodsFrom(player).getCurses()) {
                        curseInstance.onProjectileHit(player,event);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onCurseDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player player) {
            if (!GodHelper.getContactedGodsFrom(player).getCurses().isEmpty()){
                for (CurseInstance curseInstance : GodHelper.getContactedGodsFrom(player).getCurses()) {
                    curseInstance.onDeath(event);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onCurseRespawn(PlayerEvent.PlayerRespawnEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player player) {
            if (!GodHelper.getContactedGodsFrom(player).getCurses().isEmpty()){
                for (CurseInstance curseInstance : GodHelper.getContactedGodsFrom(player).getCurses()) {
                    curseInstance.onRespawn(event);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onCurseHeal(LivingHealEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player player) {
            if (!GodHelper.getContactedGodsFrom(player).getCurses().isEmpty()){
                for (CurseInstance curseInstance : GodHelper.getContactedGodsFrom(player).getCurses()) {
                    curseInstance.onHeal(event);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onCurseKnockback(LivingKnockBackEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player player) {
            if (!GodHelper.getContactedGodsFrom(player).getCurses().isEmpty()){
                for (CurseInstance curseInstance : GodHelper.getContactedGodsFrom(player).getCurses()) {
                    curseInstance.onKnockback(event);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onCurseFall(LivingFallEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player player) {
            if (!GodHelper.getContactedGodsFrom(player).getCurses().isEmpty()){
                for (CurseInstance curseInstance : GodHelper.getContactedGodsFrom(player).getCurses()) {
                    curseInstance.onFall(event);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onCurseEXPDrop(LivingExperienceDropEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player player) {
            if (!GodHelper.getContactedGodsFrom(player).getCurses().isEmpty()){
                for (CurseInstance curseInstance : GodHelper.getContactedGodsFrom(player).getCurses()) {
                    curseInstance.onEXPDrop(event);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onCurseEXPPickup(PlayerXpEvent.PickupXp event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player player) {
            if (!GodHelper.getContactedGodsFrom(player).getCurses().isEmpty()){
                for (CurseInstance curseInstance : GodHelper.getContactedGodsFrom(player).getCurses()) {
                    curseInstance.onEXPPickup(event);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onCurseJump(LivingEvent.LivingJumpEvent event) {
        LivingEntity entity = event.getEntity();

        if (entity instanceof Player player) {
            if (!GodHelper.getContactedGodsFrom(player).getCurses().isEmpty()){
                for (CurseInstance curseInstance : GodHelper.getContactedGodsFrom(player).getCurses()) {
                    curseInstance.onJump(event);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onCurseDrowning(LivingDrownEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player player) {
            if (!GodHelper.getContactedGodsFrom(player).getCurses().isEmpty()){
                for (CurseInstance curseInstance : GodHelper.getContactedGodsFrom(player).getCurses()) {
                    curseInstance.onDrowning(event);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onCurseBreathe(LivingBreatheEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player player) {
            if (!GodHelper.getContactedGodsFrom(player).getCurses().isEmpty()){
                for (CurseInstance curseInstance : GodHelper.getContactedGodsFrom(player).getCurses()) {
                    curseInstance.onBreathe(event);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onCurseBlocksWithShield(ShieldBlockEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player player) {
            if (!GodHelper.getContactedGodsFrom(player).getCurses().isEmpty()){
                for (CurseInstance curseInstance : GodHelper.getContactedGodsFrom(player).getCurses()) {
                    curseInstance.onBlocksWithShield(event);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onCurseBreakBlock(BlockEvent.BreakEvent event) {
        LivingEntity entity = event.getPlayer();
        if (entity instanceof Player player) {
            if (!GodHelper.getContactedGodsFrom(player).getCurses().isEmpty()){
                for (CurseInstance curseInstance : GodHelper.getContactedGodsFrom(player).getCurses()) {
                    curseInstance.onBreakBlock(event);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onCursePlaceBlock(BlockEvent.EntityPlaceEvent event) {
        LivingEntity entity = event.getEntity() instanceof LivingEntity ? (LivingEntity) event.getEntity() : null;
        if (entity instanceof Player player) {
            if (!GodHelper.getContactedGodsFrom(player).getCurses().isEmpty()){
                for (CurseInstance curseInstance : GodHelper.getContactedGodsFrom(player).getCurses()) {
                    curseInstance.onPlaceBlock(event);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onCurseItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        LivingEntity entity = event.getEntity() instanceof LivingEntity ? (LivingEntity) event.getEntity() : null;
        if (entity instanceof Player player) {
            if (!GodHelper.getContactedGodsFrom(player).getCurses().isEmpty()){
                for (CurseInstance curseInstance : GodHelper.getContactedGodsFrom(player).getCurses()) {
                    curseInstance.onItemCrafted(event);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onCurseItemPickup(PlayerEvent.ItemPickupEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player player) {
            if (!GodHelper.getContactedGodsFrom(player).getCurses().isEmpty()){
                for (CurseInstance curseInstance : GodHelper.getContactedGodsFrom(player).getCurses()) {
                    curseInstance.onItemPickup(event);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onCurseItemSmelted(PlayerEvent.ItemSmeltedEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player player) {
            if (!GodHelper.getContactedGodsFrom(player).getCurses().isEmpty()){
                for (CurseInstance curseInstance : GodHelper.getContactedGodsFrom(player).getCurses()) {
                    curseInstance.onItemSmelted(event);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onCurseKillEnitiy(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof LivingEntity living) {
            LivingEntity entity = living;
            if (entity instanceof Player player) {
                if (!GodHelper.getContactedGodsFrom(player).getCurses().isEmpty()){
                    for (CurseInstance curseInstance : GodHelper.getContactedGodsFrom(player).getCurses()) {
                        curseInstance.onKillEntity(player, event);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onCurseSleepInBed(PlayerSleepInBedEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player player) {
            if (!GodHelper.getContactedGodsFrom(player).getCurses().isEmpty()){
                for (CurseInstance curseInstance : GodHelper.getContactedGodsFrom(player).getCurses()) {
                    curseInstance.onSleepInBed(event);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onCurseWakeUp(PlayerWakeUpEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player player) {
            if (!GodHelper.getContactedGodsFrom(player).getCurses().isEmpty()){
                for (CurseInstance curseInstance : GodHelper.getContactedGodsFrom(player).getCurses()) {
                    curseInstance.onWakeUp(event);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onCursePlayer(CursePlayerEvent e) {
        Entity entity = e.getEntity();
        if (!entity.level().isClientSide()) {
            if (entity instanceof LivingEntity) {
                LivingEntity living = (LivingEntity)entity;

                e.getInstance().onCurse(e);
            }

        }
    }

    @SubscribeEvent
    public static void onUncursePlayer(UncursePlayerEvent e) {
        Entity entity = e.getEntity();
        if (!entity.level().isClientSide()) {
            if (entity instanceof LivingEntity) {
                LivingEntity living = (LivingEntity)entity;

                e.getInstance().onUncurse(e);
            }

        }
    }
}

