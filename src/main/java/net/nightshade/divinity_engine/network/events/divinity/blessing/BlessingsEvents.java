package net.nightshade.divinity_engine.network.events.divinity.blessing;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.nightshade.divinity_engine.DivinityEngineMod;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.util.DivinityEngineHelper;
import net.nightshade.divinity_engine.util.divinity.gods.GodHelper;

@Mod.EventBusSubscriber(modid = DivinityEngineMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BlessingsEvents {
    /*private static final WeakHashMap<Player, Integer> playerTickCounters = new WeakHashMap<>();

    @SubscribeEvent
    public static void onBlessingTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;

        Player player = event.player;
        int tickCount = playerTickCounters.getOrDefault(player, 0);

        // Run every 4 ticks (~0.2 seconds) - adjust frequency as needed
        if (tickCount % 4 != 0) {
            playerTickCounters.put(player, tickCount + 1);
            return;
        }

        // Single fetch per tick
        GodsStorage godData = GodHelper.getContactedGodsFrom(player);

        BlessingsInstance[] slots = {
                DivinityEngineHelper.getBlessingSlot1(player),
                DivinityEngineHelper.getBlessingSlot2(player),
                DivinityEngineHelper.getBlessingSlot3(player)
        };

        for (BlessingsInstance slot : slots) {
            if (slot != null) {
                GodHelper.validateBlessingSlot(player, slot);

                if (slot.getCooldown() == 0 && (slot.getBlessing().isPassive() || slot.getBlessing().canToggle())) {
                    slot.onTick(player);
                } else {
                    // Decrease cooldown by the tick interval (4 ticks)
                    slot.decreaseCooldown(4);
                }

                GodHelper.updateBlessingInstance(player, slot);
            }
        }

        playerTickCounters.put(player, tickCount + 1);
    }*/

    @SubscribeEvent
    public static void onBlessingRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (!GodHelper.getContactedGodsFrom(player).getContactedGods().isEmpty()){
            BlessingsInstance blessingSlot1 = DivinityEngineHelper.getBlessingSlot1(player);
            BlessingsInstance blessingSlot2 = DivinityEngineHelper.getBlessingSlot2(player);
            BlessingsInstance blessingSlot3 = DivinityEngineHelper.getBlessingSlot3(player);
            if (blessingSlot1 != null) {
                if (blessingSlot1.getCooldown() == 0) {
                    blessingSlot1.onRightClickBlock(event);
                }
            }
            if (blessingSlot2 != null) {
                if (blessingSlot2.getCooldown() == 0) {
                    blessingSlot2.onRightClickBlock(event);
                }
            }
            if (blessingSlot3 != null) {
                if (blessingSlot3.getCooldown() == 0) {
                    blessingSlot3.onRightClickBlock(event);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBlessingRightClickEntity(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        if (!GodHelper.getContactedGodsFrom(player).getContactedGods().isEmpty()){
            BlessingsInstance blessingSlot1 = DivinityEngineHelper.getBlessingSlot1(player);
            BlessingsInstance blessingSlot2 = DivinityEngineHelper.getBlessingSlot2(player);
            BlessingsInstance blessingSlot3 = DivinityEngineHelper.getBlessingSlot3(player);
            if (blessingSlot1 != null) {
                if (blessingSlot1.getCooldown() == 0) {
                    blessingSlot1.onRightClickEntity(event);
                }
            }
            if (blessingSlot2 != null) {
                if (blessingSlot2.getCooldown() == 0) {
                    blessingSlot2.onRightClickEntity(event);
                }
            }
            if (blessingSlot3 != null) {
                if (blessingSlot3.getCooldown() == 0) {
                    blessingSlot3.onRightClickEntity(event);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBlessingRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (!GodHelper.getContactedGodsFrom(player).getContactedGods().isEmpty()){
            BlessingsInstance blessingSlot1 = DivinityEngineHelper.getBlessingSlot1(player);
            BlessingsInstance blessingSlot2 = DivinityEngineHelper.getBlessingSlot2(player);
            BlessingsInstance blessingSlot3 = DivinityEngineHelper.getBlessingSlot3(player);
            if (blessingSlot1 != null) {
                if (blessingSlot1.getCooldown() == 0) {
                    blessingSlot1.onRightClickItem(event);
                }
            }
            if (blessingSlot2 != null) {
                if (blessingSlot2.getCooldown() == 0) {
                    blessingSlot2.onRightClickItem(event);
                }
            }
            if (blessingSlot3 != null) {
                if (blessingSlot3.getCooldown() == 0) {
                    blessingSlot3.onRightClickItem(event);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBlessingRightClickEmpty(PlayerInteractEvent.RightClickEmpty event) {
        Player player = event.getEntity();
        if (!GodHelper.getContactedGodsFrom(player).getContactedGods().isEmpty()){
            BlessingsInstance blessingSlot1 = DivinityEngineHelper.getBlessingSlot1(player);
            BlessingsInstance blessingSlot2 = DivinityEngineHelper.getBlessingSlot2(player);
            BlessingsInstance blessingSlot3 = DivinityEngineHelper.getBlessingSlot3(player);
            if (blessingSlot1 != null) {
                if (blessingSlot1.getCooldown() == 0) {
                    blessingSlot1.onRightClickEmpty(event);
                }
            }
            if (blessingSlot2 != null) {
                if (blessingSlot2.getCooldown() == 0) {
                    blessingSlot2.onRightClickEmpty(event);
                }
            }
            if (blessingSlot3 != null) {
                if (blessingSlot3.getCooldown() == 0) {
                    blessingSlot3.onRightClickEmpty(event);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBlessingBeingTargeted(LivingChangeTargetEvent event) {
        LivingEntity target = event.getNewTarget();
        if (target instanceof Player player) {
            if (!GodHelper.getContactedGodsFrom(target).getContactedGods().isEmpty()){
                BlessingsInstance blessingSlot1 = DivinityEngineHelper.getBlessingSlot1(target);
                BlessingsInstance blessingSlot2 = DivinityEngineHelper.getBlessingSlot2(target);
                BlessingsInstance blessingSlot3 = DivinityEngineHelper.getBlessingSlot3(target);
                if (blessingSlot1 != null) {
                    if (blessingSlot1.getCooldown() == 0) {
                        if (blessingSlot1.getBlessing().isPassive() || blessingSlot1.getBlessing().canToggle())
                            blessingSlot1.onBeingTargeted(target, event);
                    }
                }
                if (blessingSlot2 != null) {
                    if (blessingSlot2.getCooldown() == 0) {
                        if (blessingSlot2.getBlessing().isPassive() || blessingSlot2.getBlessing().canToggle())
                            blessingSlot2.onBeingTargeted(target, event);
                    }
                }
                if (blessingSlot3 != null) {
                    if (blessingSlot3.getCooldown() == 0) {
                        if (blessingSlot3.getBlessing().isPassive() || blessingSlot3.getBlessing().canToggle())
                            blessingSlot3.onBeingTargeted(target, event);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBlessingBeingDamaged(LivingAttackEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player player) {
            if (!GodHelper.getContactedGodsFrom(player).getContactedGods().isEmpty()){
                BlessingsInstance blessingSlot1 = DivinityEngineHelper.getBlessingSlot1(player);
                BlessingsInstance blessingSlot2 = DivinityEngineHelper.getBlessingSlot2(player);
                BlessingsInstance blessingSlot3 = DivinityEngineHelper.getBlessingSlot3(player);
                if (blessingSlot1 != null) {
                    if (blessingSlot1.getCooldown() == 0) {
                        if (blessingSlot1.getBlessing().isPassive() || blessingSlot1.getBlessing().canToggle())
                            blessingSlot1.onBeingDamaged(event);
                    }
                }
                if (blessingSlot2 != null) {
                    if (blessingSlot2.getCooldown() == 0) {
                        if (blessingSlot2.getBlessing().isPassive() || blessingSlot2.getBlessing().canToggle())
                            blessingSlot2.onBeingDamaged(event);
                    }
                }
                if (blessingSlot3 != null) {
                    if (blessingSlot3.getCooldown() == 0) {
                        if (blessingSlot3.getBlessing().isPassive() || blessingSlot3.getBlessing().canToggle())
                            blessingSlot3.onBeingDamaged(event);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBlessingDamageEntity(LivingHurtEvent event) {
        LivingEntity entity = event.getSource().getEntity() instanceof LivingEntity ? (LivingEntity) event.getSource().getEntity() : null;
        if (entity instanceof Player player) {
            if (!GodHelper.getContactedGodsFrom(player).getContactedGods().isEmpty()){
                BlessingsInstance blessingSlot1 = DivinityEngineHelper.getBlessingSlot1(player);
                BlessingsInstance blessingSlot2 = DivinityEngineHelper.getBlessingSlot2(player);
                BlessingsInstance blessingSlot3 = DivinityEngineHelper.getBlessingSlot3(player);
                if (blessingSlot1 != null) {
                    if (blessingSlot1.getCooldown() == 0) {
                        if (blessingSlot1.getBlessing().isPassive() || blessingSlot1.getBlessing().canToggle()) blessingSlot1.onDamageEntity(player, event);
                    }
                }
                if (blessingSlot2 != null) {
                    if (blessingSlot2.getCooldown() == 0) {
                        if (blessingSlot2.getBlessing().isPassive() || blessingSlot2.getBlessing().canToggle()) blessingSlot2.onDamageEntity(player, event);
                    }
                }
                if (blessingSlot3 != null) {
                    if (blessingSlot3.getCooldown() == 0) {
                        if (blessingSlot3.getBlessing().isPassive() || blessingSlot3.getBlessing().canToggle()) blessingSlot3.onDamageEntity(player, event);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBlessingCriticalHit(CriticalHitEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player player) {
            if (!GodHelper.getContactedGodsFrom(player).getContactedGods().isEmpty()){
                BlessingsInstance blessingSlot1 = DivinityEngineHelper.getBlessingSlot1(player);
                BlessingsInstance blessingSlot2 = DivinityEngineHelper.getBlessingSlot2(player);
                BlessingsInstance blessingSlot3 = DivinityEngineHelper.getBlessingSlot3(player);
                if (blessingSlot1 != null) {
                    if (blessingSlot1.getCooldown() == 0) {
                        if (blessingSlot1.getBlessing().isPassive() || blessingSlot1.getBlessing().canToggle()) blessingSlot1.onCriticalHit(event);
                    }
                }
                if (blessingSlot2 != null) {
                    if (blessingSlot2.getCooldown() == 0) {
                        if (blessingSlot2.getBlessing().isPassive() || blessingSlot2.getBlessing().canToggle()) blessingSlot2.onCriticalHit(event);
                    }
                }
                if (blessingSlot3 != null) {
                    if (blessingSlot3.getCooldown() == 0) {
                        if (blessingSlot3.getBlessing().isPassive() || blessingSlot3.getBlessing().canToggle()) blessingSlot3.onCriticalHit(event);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBlessingTakenDamage(LivingDamageEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player player) {
            if (!GodHelper.getContactedGodsFrom(player).getContactedGods().isEmpty()){
                BlessingsInstance blessingSlot1 = DivinityEngineHelper.getBlessingSlot1(player);
                BlessingsInstance blessingSlot2 = DivinityEngineHelper.getBlessingSlot2(player);
                BlessingsInstance blessingSlot3 = DivinityEngineHelper.getBlessingSlot3(player);
                if (blessingSlot1 != null) {
                    if (blessingSlot1.getCooldown() == 0) {
                        if (blessingSlot1.getBlessing().isPassive() || blessingSlot1.getBlessing().canToggle()) blessingSlot1.onTakenDamage(event);
                    }
                }
                if (blessingSlot2 != null) {
                    if (blessingSlot2.getCooldown() == 0) {
                        if (blessingSlot2.getBlessing().isPassive() || blessingSlot2.getBlessing().canToggle()) blessingSlot2.onTakenDamage(event);
                    }
                }
                if (blessingSlot3 != null) {
                    if (blessingSlot3.getCooldown() == 0) {
                        if (blessingSlot3.getBlessing().isPassive() || blessingSlot3.getBlessing().canToggle()) blessingSlot3.onTakenDamage(event);
                    }
                }
            }
        }

    }

    @SubscribeEvent
    public static void onBlessingProjectileHit(ProjectileImpactEvent event) {
        HitResult var2 = event.getRayTraceResult();
        if (var2 instanceof EntityHitResult result) {
            Entity var3 = result.getEntity();
            if (var3 instanceof Player player) {
                if (!GodHelper.getContactedGodsFrom(player).getContactedGods().isEmpty()){
                    BlessingsInstance blessingSlot1 = DivinityEngineHelper.getBlessingSlot1(player);
                    BlessingsInstance blessingSlot2 = DivinityEngineHelper.getBlessingSlot2(player);
                    BlessingsInstance blessingSlot3 = DivinityEngineHelper.getBlessingSlot3(player);
                    if (blessingSlot1 != null) {
                        if (blessingSlot1.getCooldown() == 0){
                            if (blessingSlot1.getBlessing().isPassive() || blessingSlot1.getBlessing().canToggle()) blessingSlot1.onProjectileHit(player, event);
                        }
                    }
                    if (blessingSlot2 != null) {
                        if (blessingSlot2.getCooldown() == 0){
                            if (blessingSlot2.getBlessing().isPassive() || blessingSlot2.getBlessing().canToggle()) blessingSlot2.onProjectileHit(player, event);
                        }
                    }
                    if (blessingSlot3 != null) {
                        if (blessingSlot3.getCooldown() == 0){
                            if (blessingSlot3.getBlessing().isPassive() || blessingSlot3.getBlessing().canToggle()) blessingSlot3.onProjectileHit(player, event);
                        }
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

    @SubscribeEvent
    public static void onBlessingDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player player) {
            if (!GodHelper.getContactedGodsFrom(player).getContactedGods().isEmpty()){
                BlessingsInstance blessingSlot1 = DivinityEngineHelper.getBlessingSlot1(player);
                BlessingsInstance blessingSlot2 = DivinityEngineHelper.getBlessingSlot2(player);
                BlessingsInstance blessingSlot3 = DivinityEngineHelper.getBlessingSlot3(player);
                if (blessingSlot1 != null) {
                    if (blessingSlot1.getCooldown() == 0) {
                        if (blessingSlot1.getBlessing().isPassive() || blessingSlot1.getBlessing().canToggle()) blessingSlot1.onDeath(event);
                    }
                }
                if (blessingSlot2 != null) {
                    if (blessingSlot2.getCooldown() == 0) {
                        if (blessingSlot2.getBlessing().isPassive() || blessingSlot2.getBlessing().canToggle()) blessingSlot2.onDeath(event);
                    }
                }
                if (blessingSlot3 != null) {
                    if (blessingSlot3.getCooldown() == 0) {
                        if (blessingSlot3.getBlessing().isPassive() || blessingSlot3.getBlessing().canToggle()) blessingSlot3.onDeath(event);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBlessingRespawn(PlayerEvent.PlayerRespawnEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player player) {
            if (!GodHelper.getContactedGodsFrom(player).getContactedGods().isEmpty()){
                BlessingsInstance blessingSlot1 = DivinityEngineHelper.getBlessingSlot1(player);
                BlessingsInstance blessingSlot2 = DivinityEngineHelper.getBlessingSlot2(player);
                BlessingsInstance blessingSlot3 = DivinityEngineHelper.getBlessingSlot3(player);
                if (blessingSlot1 != null) {
                    if (blessingSlot1.getCooldown() == 0) {
                        if (blessingSlot1.getBlessing().isPassive() || blessingSlot1.getBlessing().canToggle()) blessingSlot1.onRespawn(event);
                    }
                }
                if (blessingSlot2 != null) {
                    if (blessingSlot2.getCooldown() == 0) {
                        if (blessingSlot2.getBlessing().isPassive() || blessingSlot2.getBlessing().canToggle()) blessingSlot2.onRespawn(event);
                    }
                }
                if (blessingSlot3 != null) {
                    if (blessingSlot3.getCooldown() == 0) {
                        if (blessingSlot3.getBlessing().isPassive() || blessingSlot3.getBlessing().canToggle()) blessingSlot3.onRespawn(event);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBlessingHeal(LivingHealEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player player) {
            if (!GodHelper.getContactedGodsFrom(player).getContactedGods().isEmpty()){
                BlessingsInstance blessingSlot1 = DivinityEngineHelper.getBlessingSlot1(player);
                BlessingsInstance blessingSlot2 = DivinityEngineHelper.getBlessingSlot2(player);
                BlessingsInstance blessingSlot3 = DivinityEngineHelper.getBlessingSlot3(player);
                if (blessingSlot1 != null) {
                    if (blessingSlot1.getCooldown() == 0) {
                        if (blessingSlot1.getBlessing().isPassive() || blessingSlot1.getBlessing().canToggle()) blessingSlot1.onHeal(event);
                    }
                }
                if (blessingSlot2 != null) {
                    if (blessingSlot2.getCooldown() == 0) {
                        if (blessingSlot2.getBlessing().isPassive() || blessingSlot2.getBlessing().canToggle()) blessingSlot2.onHeal(event);
                    }
                }
                if (blessingSlot3 != null) {
                    if (blessingSlot3.getCooldown() == 0) {
                        if (blessingSlot3.getBlessing().isPassive() || blessingSlot3.getBlessing().canToggle()) blessingSlot3.onHeal(event);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBlessingKnockback(LivingKnockBackEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player player) {
            if (!GodHelper.getContactedGodsFrom(player).getContactedGods().isEmpty()){
                BlessingsInstance blessingSlot1 = DivinityEngineHelper.getBlessingSlot1(player);
                BlessingsInstance blessingSlot2 = DivinityEngineHelper.getBlessingSlot2(player);
                BlessingsInstance blessingSlot3 = DivinityEngineHelper.getBlessingSlot3(player);
                if (blessingSlot1 != null) {
                    if (blessingSlot1.getCooldown() == 0){
                        if (blessingSlot1.getBlessing().isPassive() || blessingSlot1.getBlessing().canToggle()) blessingSlot1.onKnockback(event);
                    }
                }
                if (blessingSlot2 != null) {
                    if (blessingSlot2.getCooldown() == 0){
                        if (blessingSlot2.getBlessing().isPassive() || blessingSlot2.getBlessing().canToggle()) blessingSlot2.onKnockback(event);
                    }
                }
                if (blessingSlot3 != null) {
                    if (blessingSlot3.getCooldown() == 0){
                        if (blessingSlot3.getBlessing().isPassive() || blessingSlot3.getBlessing().canToggle()) blessingSlot3.onKnockback(event);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBlessingFall(LivingFallEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player player) {
            if (!GodHelper.getContactedGodsFrom(player).getContactedGods().isEmpty()){
                BlessingsInstance blessingSlot1 = DivinityEngineHelper.getBlessingSlot1(player);
                BlessingsInstance blessingSlot2 = DivinityEngineHelper.getBlessingSlot2(player);
                BlessingsInstance blessingSlot3 = DivinityEngineHelper.getBlessingSlot3(player);
                if (blessingSlot1 != null) {
                    if (blessingSlot1.getCooldown() == 0){
                        if (blessingSlot1.getBlessing().isPassive() || blessingSlot1.getBlessing().canToggle()) blessingSlot1.onFall(event);
                    }
                }
                if (blessingSlot2 != null) {
                    if (blessingSlot2.getCooldown() == 0){
                        if (blessingSlot2.getBlessing().isPassive() || blessingSlot2.getBlessing().canToggle()) blessingSlot2.onFall(event);
                    }
                }
                if (blessingSlot3 != null) {
                    if (blessingSlot3.getCooldown() == 0){
                        if (blessingSlot3.getBlessing().isPassive() || blessingSlot3.getBlessing().canToggle()) blessingSlot3.onFall(event);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBlessingEXPDrop(LivingExperienceDropEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player player) {
            if (!GodHelper.getContactedGodsFrom(player).getContactedGods().isEmpty()){
                BlessingsInstance blessingSlot1 = DivinityEngineHelper.getBlessingSlot1(player);
                BlessingsInstance blessingSlot2 = DivinityEngineHelper.getBlessingSlot2(player);
                BlessingsInstance blessingSlot3 = DivinityEngineHelper.getBlessingSlot3(player);
                if (blessingSlot1 != null) {
                    if (blessingSlot1.getCooldown() == 0){
                        if (blessingSlot1.getBlessing().isPassive() || blessingSlot1.getBlessing().canToggle()) blessingSlot1.onEXPDrop(event);
                    }
                }
                if (blessingSlot2 != null) {
                    if (blessingSlot2.getCooldown() == 0){
                        if (blessingSlot2.getBlessing().isPassive() || blessingSlot2.getBlessing().canToggle()) blessingSlot2.onEXPDrop(event);
                    }
                }
                if (blessingSlot3 != null) {
                    if (blessingSlot3.getCooldown() == 0){
                        if (blessingSlot3.getBlessing().isPassive() || blessingSlot3.getBlessing().canToggle()) blessingSlot3.onEXPDrop(event);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBlessingEXPPickup(PlayerXpEvent.PickupXp event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player player) {
            if (!GodHelper.getContactedGodsFrom(player).getContactedGods().isEmpty()){
                BlessingsInstance blessingSlot1 = DivinityEngineHelper.getBlessingSlot1(player);
                BlessingsInstance blessingSlot2 = DivinityEngineHelper.getBlessingSlot2(player);
                BlessingsInstance blessingSlot3 = DivinityEngineHelper.getBlessingSlot3(player);
                if (blessingSlot1 != null) {
                    if (blessingSlot1.getCooldown() == 0){
                        if (blessingSlot1.getBlessing().isPassive() || blessingSlot1.getBlessing().canToggle()) blessingSlot1.onEXPPickup(event);
                    }
                }
                if (blessingSlot2 != null) {
                    if (blessingSlot2.getCooldown() == 0){
                        if (blessingSlot2.getBlessing().isPassive() || blessingSlot2.getBlessing().canToggle()) blessingSlot2.onEXPPickup(event);
                    }
                }
                if (blessingSlot3 != null) {
                    if (blessingSlot3.getCooldown() == 0){
                        if (blessingSlot3.getBlessing().isPassive() || blessingSlot3.getBlessing().canToggle()) blessingSlot3.onEXPPickup(event);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBlessingJump(LivingEvent.LivingJumpEvent event) {
        LivingEntity entity = event.getEntity();

        if (entity instanceof Player player) {
            if (!GodHelper.getContactedGodsFrom(player).getContactedGods().isEmpty()){
                BlessingsInstance blessingSlot1 = DivinityEngineHelper.getBlessingSlot1(player);
                BlessingsInstance blessingSlot2 = DivinityEngineHelper.getBlessingSlot2(player);
                BlessingsInstance blessingSlot3 = DivinityEngineHelper.getBlessingSlot3(player);
                if (blessingSlot1 != null) {

                    if (blessingSlot1.getCooldown() == 0) {
                        if (blessingSlot1.getBlessing().isPassive() || blessingSlot1.getBlessing().canToggle()) blessingSlot1.onJump(event);
                    }
                }
                if (blessingSlot2 != null) {
                    if (blessingSlot2.getCooldown() == 0) {
                        if (blessingSlot2.getBlessing().isPassive() || blessingSlot2.getBlessing().canToggle()) blessingSlot2.onJump(event);
                    }
                }
                if (blessingSlot3 != null) {
                    if (blessingSlot3.getCooldown() == 0) {
                        if (blessingSlot3.getBlessing().isPassive() || blessingSlot3.getBlessing().canToggle()) blessingSlot3.onJump(event);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBlessingDrowning(LivingDrownEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player player) {
            if (!GodHelper.getContactedGodsFrom(player).getContactedGods().isEmpty()) {
                BlessingsInstance blessingSlot1 = DivinityEngineHelper.getBlessingSlot1(player);
                BlessingsInstance blessingSlot2 = DivinityEngineHelper.getBlessingSlot2(player);
                BlessingsInstance blessingSlot3 = DivinityEngineHelper.getBlessingSlot3(player);
                if (blessingSlot1 != null) {
                    if (blessingSlot1.getCooldown() == 0) {
                        if (blessingSlot1.getBlessing().isPassive() || blessingSlot1.getBlessing().canToggle()) blessingSlot1.onDrowning(event);
                    }
                }
                if (blessingSlot2 != null) {
                    if (blessingSlot2.getCooldown() == 0) {
                        if (blessingSlot2.getBlessing().isPassive() || blessingSlot2.getBlessing().canToggle()) blessingSlot2.onDrowning(event);
                    }
                }
                if (blessingSlot3 != null) {
                    if (blessingSlot3.getCooldown() == 0) {
                        if (blessingSlot3.getBlessing().isPassive() || blessingSlot3.getBlessing().canToggle()) blessingSlot3.onDrowning(event);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBlessingBreathe(LivingBreatheEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player player) {
            if (!GodHelper.getContactedGodsFrom(player).getContactedGods().isEmpty()){
                BlessingsInstance blessingSlot1 = DivinityEngineHelper.getBlessingSlot1(player);
                BlessingsInstance blessingSlot2 = DivinityEngineHelper.getBlessingSlot2(player);
                BlessingsInstance blessingSlot3 = DivinityEngineHelper.getBlessingSlot3(player);
                if (blessingSlot1 != null) {
                    if (blessingSlot1.getCooldown() == 0){
                        if (blessingSlot1.getBlessing().isPassive() || blessingSlot1.getBlessing().canToggle()) blessingSlot1.onBreathe(event);
                    }
                }
                if (blessingSlot2 != null) {
                    if (blessingSlot2.getCooldown() == 0){
                        if (blessingSlot2.getBlessing().isPassive() || blessingSlot2.getBlessing().canToggle()) blessingSlot2.onBreathe(event);
                    }
                }
                if (blessingSlot3 != null) {
                    if (blessingSlot3.getCooldown() == 0){
                        if (blessingSlot3.getBlessing().isPassive() || blessingSlot3.getBlessing().canToggle()) blessingSlot3.onBreathe(event);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBlessingBlocksWithShield(ShieldBlockEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player player) {
            if (!GodHelper.getContactedGodsFrom(player).getContactedGods().isEmpty()){
                BlessingsInstance blessingSlot1 = DivinityEngineHelper.getBlessingSlot1(player);
                BlessingsInstance blessingSlot2 = DivinityEngineHelper.getBlessingSlot2(player);
                BlessingsInstance blessingSlot3 = DivinityEngineHelper.getBlessingSlot3(player);
                if (blessingSlot1 != null) {
                    if (blessingSlot1.getCooldown() == 0){
                        if (blessingSlot1.getBlessing().isPassive() || blessingSlot1.getBlessing().canToggle()) blessingSlot1.onBlocksWithShield(event);
                    }
                }
                if (blessingSlot2 != null) {
                    if (blessingSlot2.getCooldown() == 0){
                        if (blessingSlot2.getBlessing().isPassive() || blessingSlot2.getBlessing().canToggle()) blessingSlot2.onBlocksWithShield(event);
                    }
                }
                if (blessingSlot3 != null) {
                    if (blessingSlot3.getCooldown() == 0){
                        if (blessingSlot3.getBlessing().isPassive() || blessingSlot3.getBlessing().canToggle()) blessingSlot3.onBlocksWithShield(event);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBlessingBreakBlock(BlockEvent.BreakEvent event) {
        LivingEntity entity = event.getPlayer();
        if (entity instanceof Player player) {
            if (!GodHelper.getContactedGodsFrom(player).getContactedGods().isEmpty()){
                BlessingsInstance blessingSlot1 = DivinityEngineHelper.getBlessingSlot1(player);
                BlessingsInstance blessingSlot2 = DivinityEngineHelper.getBlessingSlot2(player);
                BlessingsInstance blessingSlot3 = DivinityEngineHelper.getBlessingSlot3(player);
                if (blessingSlot1 != null) {
                    if (blessingSlot1.getCooldown() == 0){
                        if (blessingSlot1.getBlessing().isPassive() || blessingSlot1.getBlessing().canToggle()) blessingSlot1.onBreakBlock(event);
                    }
                }
                if (blessingSlot2 != null) {
                    if (blessingSlot2.getCooldown() == 0){
                        if (blessingSlot2.getBlessing().isPassive() || blessingSlot2.getBlessing().canToggle()) blessingSlot2.onBreakBlock(event);
                    }
                }
                if (blessingSlot3 != null) {
                    if (blessingSlot3.getCooldown() == 0){
                        if (blessingSlot3.getBlessing().isPassive() || blessingSlot3.getBlessing().canToggle()) blessingSlot3.onBreakBlock(event);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBlessingPlaceBlock(BlockEvent.EntityPlaceEvent event) {
        LivingEntity entity = event.getEntity() instanceof LivingEntity ? (LivingEntity) event.getEntity() : null;
        if (entity instanceof Player player) {
            if (!GodHelper.getContactedGodsFrom(player).getContactedGods().isEmpty()){
                BlessingsInstance blessingSlot1 = DivinityEngineHelper.getBlessingSlot1(player);
                BlessingsInstance blessingSlot2 = DivinityEngineHelper.getBlessingSlot2(player);
                BlessingsInstance blessingSlot3 = DivinityEngineHelper.getBlessingSlot3(player);
                if (blessingSlot1 != null) {
                    if (blessingSlot1.getCooldown() == 0){
                        if (blessingSlot1.getBlessing().isPassive() || blessingSlot1.getBlessing().canToggle()) blessingSlot1.onPlaceBlock(event);
                    }
                }
                if (blessingSlot2 != null) {
                    if (blessingSlot2.getCooldown() == 0){
                        if (blessingSlot2.getBlessing().isPassive() || blessingSlot2.getBlessing().canToggle()) blessingSlot2.onPlaceBlock(event);
                    }
                }
                if (blessingSlot3 != null) {
                    if (blessingSlot3.getCooldown() == 0){
                        if (blessingSlot3.getBlessing().isPassive() || blessingSlot3.getBlessing().canToggle()) blessingSlot3.onPlaceBlock(event);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBlessingItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        LivingEntity entity = event.getEntity() instanceof LivingEntity ? (LivingEntity) event.getEntity() : null;
        if (entity instanceof Player player) {
            if (!GodHelper.getContactedGodsFrom(player).getContactedGods().isEmpty()){
                BlessingsInstance blessingSlot1 = DivinityEngineHelper.getBlessingSlot1(player);
                BlessingsInstance blessingSlot2 = DivinityEngineHelper.getBlessingSlot2(player);
                BlessingsInstance blessingSlot3 = DivinityEngineHelper.getBlessingSlot3(player);
                if (blessingSlot1 != null) {
                    if (blessingSlot1.getCooldown() == 0){
                        if (blessingSlot1.getBlessing().isPassive() || blessingSlot1.getBlessing().canToggle()) blessingSlot1.onItemCrafted(event);
                    }
                }
                if (blessingSlot2 != null) {
                    if (blessingSlot2.getCooldown() == 0){
                        if (blessingSlot2.getBlessing().isPassive() || blessingSlot2.getBlessing().canToggle()) blessingSlot2.onItemCrafted(event);
                    }
                }
                if (blessingSlot3 != null) {
                    if (blessingSlot3.getCooldown() == 0){
                        if (blessingSlot3.getBlessing().isPassive() || blessingSlot3.getBlessing().canToggle()) blessingSlot3.onItemCrafted(event);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBlessingItemPickup(PlayerEvent.ItemPickupEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player player) {
            if (!GodHelper.getContactedGodsFrom(player).getContactedGods().isEmpty()){
                BlessingsInstance blessingSlot1 = DivinityEngineHelper.getBlessingSlot1(player);
                BlessingsInstance blessingSlot2 = DivinityEngineHelper.getBlessingSlot2(player);
                BlessingsInstance blessingSlot3 = DivinityEngineHelper.getBlessingSlot3(player);
                if (blessingSlot1 != null) {
                    if (blessingSlot1.getCooldown() == 0){
                        if (blessingSlot1.getBlessing().isPassive() || blessingSlot1.getBlessing().canToggle()) blessingSlot1.onItemPickup(event);
                    }
                }
                if (blessingSlot2 != null) {
                    if (blessingSlot2.getCooldown() == 0){
                        if (blessingSlot2.getBlessing().isPassive() || blessingSlot2.getBlessing().canToggle()) blessingSlot2.onItemPickup(event);
                    }
                }
                if (blessingSlot3 != null) {
                    if (blessingSlot3.getCooldown() == 0){
                        if (blessingSlot3.getBlessing().isPassive() || blessingSlot3.getBlessing().canToggle()) blessingSlot3.onItemPickup(event);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBlessingItemSmelted(PlayerEvent.ItemSmeltedEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player player) {
            if (!GodHelper.getContactedGodsFrom(player).getContactedGods().isEmpty()){
                BlessingsInstance blessingSlot1 = DivinityEngineHelper.getBlessingSlot1(player);
                BlessingsInstance blessingSlot2 = DivinityEngineHelper.getBlessingSlot2(player);
                BlessingsInstance blessingSlot3 = DivinityEngineHelper.getBlessingSlot3(player);
                if (blessingSlot1 != null) {
                    if (blessingSlot1.getCooldown() == 0){
                        if (blessingSlot1.getBlessing().isPassive() || blessingSlot1.getBlessing().canToggle()) blessingSlot1.onItemSmelted(event);
                    }
                }
                if (blessingSlot2 != null) {
                    if (blessingSlot2.getCooldown() == 0){
                        if (blessingSlot2.getBlessing().isPassive() || blessingSlot2.getBlessing().canToggle()) blessingSlot2.onItemSmelted(event);
                    }
                }
                if (blessingSlot3 != null) {
                    if (blessingSlot3.getCooldown() == 0){
                        if (blessingSlot3.getBlessing().isPassive() || blessingSlot3.getBlessing().canToggle()) blessingSlot3.onItemSmelted(event);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBlessingKillEnitiy(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof LivingEntity living) {
            LivingEntity entity = living;
            if (entity instanceof Player player) {
                if (!GodHelper.getContactedGodsFrom(player).getContactedGods().isEmpty()){
                    BlessingsInstance blessingSlot1 = DivinityEngineHelper.getBlessingSlot1(player);
                    BlessingsInstance blessingSlot2 = DivinityEngineHelper.getBlessingSlot2(player);
                    BlessingsInstance blessingSlot3 = DivinityEngineHelper.getBlessingSlot3(player);
                    if (blessingSlot1 != null) {
                        if (blessingSlot1.getCooldown() == 0) {
                            if (blessingSlot1.getBlessing().isPassive() || blessingSlot1.getBlessing().canToggle()) blessingSlot1.onKillEntity(player, event);
                        }
                    }
                    if (blessingSlot2 != null) {
                        if (blessingSlot2.getCooldown() == 0) {
                            if (blessingSlot2.getBlessing().isPassive() || blessingSlot2.getBlessing().canToggle()) blessingSlot2.onKillEntity(player, event);
                        }
                    }
                    if (blessingSlot3 != null) {
                        if (blessingSlot3.getCooldown() == 0) {
                            if (blessingSlot3.getBlessing().isPassive() || blessingSlot3.getBlessing().canToggle()) blessingSlot3.onKillEntity(player, event);
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBlessingSleepInBed(PlayerSleepInBedEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player player) {
            if (!GodHelper.getContactedGodsFrom(player).getContactedGods().isEmpty()){
                BlessingsInstance blessingSlot1 = DivinityEngineHelper.getBlessingSlot1(player);
                BlessingsInstance blessingSlot2 = DivinityEngineHelper.getBlessingSlot2(player);
                BlessingsInstance blessingSlot3 = DivinityEngineHelper.getBlessingSlot3(player);
                if (blessingSlot1 != null) {
                    if (blessingSlot1.getCooldown() == 0) {
                        if (blessingSlot1.getBlessing().isPassive() || blessingSlot1.getBlessing().canToggle()) blessingSlot1.onSleepInBed(event);
                    }
                }
                if (blessingSlot2 != null) {
                    if (blessingSlot2.getCooldown() == 0) {
                        if (blessingSlot2.getBlessing().isPassive() || blessingSlot2.getBlessing().canToggle()) blessingSlot2.onSleepInBed(event);
                    }
                }
                if (blessingSlot3 != null) {
                    if (blessingSlot3.getCooldown() == 0) {
                        if (blessingSlot3.getBlessing().isPassive() || blessingSlot3.getBlessing().canToggle()) blessingSlot3.onSleepInBed(event);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBlessingWakeUp(PlayerWakeUpEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player player) {
            if (!GodHelper.getContactedGodsFrom(player).getContactedGods().isEmpty()){
                BlessingsInstance blessingSlot1 = DivinityEngineHelper.getBlessingSlot1(player);
                BlessingsInstance blessingSlot2 = DivinityEngineHelper.getBlessingSlot2(player);
                BlessingsInstance blessingSlot3 = DivinityEngineHelper.getBlessingSlot3(player);
                if (blessingSlot1 != null) {
                    if (blessingSlot1.getCooldown() == 0) {
                        if (blessingSlot1.getBlessing().isPassive() || blessingSlot1.getBlessing().canToggle()) blessingSlot1.onWakeUp(event);
                    }
                }
                if (blessingSlot2 != null) {
                    if (blessingSlot2.getCooldown() == 0) {
                        if (blessingSlot2.getBlessing().isPassive() || blessingSlot2.getBlessing().canToggle()) blessingSlot2.onWakeUp(event);
                    }
                }
                if (blessingSlot3 != null) {
                    if (blessingSlot3.getCooldown() == 0) {
                        if (blessingSlot3.getBlessing().isPassive() || blessingSlot3.getBlessing().canToggle()) blessingSlot3.onWakeUp(event);
                    }
                }
            }
        }
    }
}

