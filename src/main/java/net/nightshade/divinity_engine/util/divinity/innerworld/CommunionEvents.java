package net.nightshade.divinity_engine.util.divinity.innerworld;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.*;
import net.minecraftforge.event.entity.*;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.event.level.*;
import net.minecraftforge.event.server.*;
import net.minecraftforge.eventbus.api.*;
import net.nightshade.divinity_engine.registry.dimensions.DimensionRegistry;
import net.nightshade.divinity_engine.entity.TranceBodyEntity;
import net.minecraftforge.server.ServerLifecycleHooks;

public final class CommunionEvents {
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e){if(e.phase==TickEvent.Phase.END){var s=ServerLifecycleHooks.getCurrentServer();if(s!=null)CommunionManager.tick(s);}}
    @SubscribeEvent public static void started(ServerStartedEvent e){CommunionManager.started(e.getServer());}
    @SubscribeEvent public static void stopped(ServerStoppingEvent e){CommunionManager.stop(e.getServer());CommunionDamage.clear();}
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent e){if(e.getEntity() instanceof ServerPlayer p)CommunionManager.recover(p);}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p)CommunionManager.logout(p);}
    @SubscribeEvent public static void clone(PlayerEvent.Clone e){
        // Carry only pending recovery on a non-death clone. Never reactivate a respawned projection.
        if(!e.isWasDeath()&&e.getOriginal().getPersistentData().contains(CommunionLedger.PLAYER_KEY))e.getEntity().getPersistentData().put(CommunionLedger.PLAYER_KEY,e.getOriginal().getPersistentData().getCompound(CommunionLedger.PLAYER_KEY).copy());
        e.getEntity().getPersistentData().putLong("DivinityCommunionCooldown",e.getOriginal().getPersistentData().getLong("DivinityCommunionCooldown"));
    }
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent e){if(e.getEntity() instanceof ServerPlayer p)CommunionNetwork.clear(p);}
    @SubscribeEvent public static void changed(PlayerEvent.PlayerChangedDimensionEvent e){if(e.getEntity() instanceof ServerPlayer p&&!CommunionManager.transporting()&&CommunionManager.session(p)!=null)CommunionManager.end(p,CommunionLedger.Exit.EXTERNAL_TELEPORT);}
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void death(LivingDeathEvent e){if(!e.isCanceled()&&e.getEntity() instanceof ServerPlayer p&&CommunionManager.session(p)!=null)CommunionManager.end(p,CommunionLedger.Exit.DEATH);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void travel(EntityTravelToDimensionEvent e){if(e.getEntity() instanceof TranceBodyEntity)e.setCanceled(true);else if(e.getEntity() instanceof Player p&&CommunionManager.busy(p)&&!CommunionManager.transporting())e.setCanceled(true);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void breakBlock(BlockEvent.BreakEvent e){if(e.getLevel().getBlockState(e.getPos())!=null&&CommunionManager.inRealm(e.getPlayer()))e.setCanceled(true);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void place(BlockEvent.EntityPlaceEvent e){if(e.getLevel() instanceof net.minecraft.world.level.Level l&&l.dimension().equals(DimensionRegistry.INNER_WORLD))e.setCanceled(true);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void rightBlock(PlayerInteractEvent.RightClickBlock e){if(CommunionManager.inRealm(e.getEntity())){e.setCanceled(true);e.setCancellationResult(InteractionResult.FAIL);}}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void rightItem(PlayerInteractEvent.RightClickItem e){if(CommunionManager.inRealm(e.getEntity())){e.setCanceled(true);e.setCancellationResult(InteractionResult.FAIL);}}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void interactEntity(PlayerInteractEvent.EntityInteract e){if(CommunionManager.inRealm(e.getEntity())&&!(e.getTarget() instanceof net.nightshade.divinity_engine.entity.GodManifestationEntity)){e.setCanceled(true);e.setCancellationResult(InteractionResult.FAIL);}}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void interactEntityAt(PlayerInteractEvent.EntityInteractSpecific e){if(CommunionManager.inRealm(e.getEntity())){e.setCanceled(true);e.setCancellationResult(InteractionResult.PASS);}}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void attack(AttackEntityEvent e){if(CommunionManager.inRealm(e.getEntity()))e.setCanceled(true);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void toss(ItemTossEvent e){if(CommunionManager.inRealm(e.getPlayer())){e.setCanceled(true);e.getPlayer().getInventory().placeItemBackInInventory(e.getEntity().getItem());}}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void pickup(EntityItemPickupEvent e){if(CommunionManager.inRealm(e.getEntity()))e.setCanceled(true);}
    @SubscribeEvent public static void explode(ExplosionEvent.Detonate e){if(e.getLevel().dimension().equals(DimensionRegistry.INNER_WORLD)){e.getAffectedBlocks().clear();e.getAffectedEntities().removeIf(x->!(x instanceof Player));}}
    @SubscribeEvent public static void grief(EntityMobGriefingEvent e){if(CommunionManager.inRealm(e.getEntity()))e.setResult(Event.Result.DENY);}
    @SubscribeEvent public static void spawn(MobSpawnEvent.PositionCheck e){if(e.getLevel().getLevel().dimension().equals(DimensionRegistry.INNER_WORLD))e.setResult(Event.Result.DENY);}
    private CommunionEvents(){}
}
