package net.nightshade.divinity_engine.network.events.divinity;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.divinity.curse.CurseInstance;
import net.nightshade.divinity_engine.divinity.gods.BaseGod;
import net.nightshade.divinity_engine.divinity.gods.BaseGodInstance;
import net.nightshade.divinity_engine.divinity.gods.FavorTiers;
import net.nightshade.divinity_engine.network.cap.player.gods.GodsStorage;
import net.nightshade.divinity_engine.util.DivinityEngineHelper;
import net.nightshade.divinity_engine.util.divinity.gods.GodHelper;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Mod.EventBusSubscriber(modid = "divinity_engine", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DivinityTickHandler {

    private static final Queue<Player> playerQueue = new ConcurrentLinkedQueue<>();
    private static final int BATCH_SIZE = 5;

    // Enqueue player for processing
    public static void queuePlayer(Player player) {
        if (player != null && !playerQueue.contains(player) && player.isAlive()) {
            playerQueue.offer(player);
        }
    }

    // Add players on login to queue
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        queuePlayer(event.getEntity());
    }

    // Remove players from queue on logout to avoid memory leaks
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        playerQueue.remove(event.getEntity());
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        int processed = 0;
        while (processed < BATCH_SIZE && !playerQueue.isEmpty()) {
            Player player = playerQueue.poll();

            if (player != null && player.isAlive()) {
                processPlayerTicks(player);

                // Re-queue player for next batch if still alive
                playerQueue.offer(player);
            }

            processed++;
        }
    }

    private static void processPlayerTicks(Player player) {
        int tickCount = player.tickCount;

        GodsStorage godData = GodHelper.getContactedGodsFrom(player);
        // Start timing
        long start = System.nanoTime();
        // ===== Curse Tick (every 10 ticks) =====
        if (tickCount % 10 == 0) {
            for (CurseInstance curse : godData.getCurses()) {
                curse.onTick(player);
            }
        }

        // ===== God Tick (every 20 ticks ~1 second) =====
        if (tickCount % 20 == 0) {
            List<BaseGodInstance> contactedGods = godData.getContactedGods().stream().toList();
            if (!contactedGods.isEmpty()) {
                // Clamp favor
                for (BaseGodInstance instance : contactedGods) {
                    int favor = instance.getFavor();
                    if (favor > FavorTiers.Champion.getMaxFavor()) {
                        instance.setFavor(FavorTiers.Champion.getMaxFavor());
                        godData.updateGodInstance(instance);
                    } else if (favor < FavorTiers.Enemy.getMinFavor()) {
                        instance.setFavor(FavorTiers.Enemy.getMinFavor());
                        godData.updateGodInstance(instance);
                    }
                }

                // Curse/Uncurse logic
                for (BaseGodInstance godInstance : contactedGods) {
                    FavorTiers tier = FavorTiers.getByFavor(godInstance.getFavor());
                    BaseGod baseGod = godInstance.getBaseGod();

                    if (tier == FavorTiers.Enemy && baseGod.getCurse() != null) {
                        boolean alreadyCursed = godData.getCurse(baseGod.getCurse()).isPresent();
                        if (!alreadyCursed) {
                            GodHelper.curse(baseGod.getCurse(), baseGod, player);
                        }
                    } else if (!godData.getCurses().isEmpty()) {
                        for (CurseInstance curse : godData.getCurses()) {
                            BaseGod boundGod = curse.getBoundGod();
                            BaseGodInstance boundInstance = GodHelper.getGodOrNull(player, boundGod);

                            boolean invalid = boundInstance == null
                                    || FavorTiers.getByFavor(boundInstance.getFavor()) != FavorTiers.Enemy;

                            if (invalid) {
                                godData.uncurse(curse);
                                break;
                            }
                        }
                    }
                }
            }
        }

        // ===== Blessing Tick (every 2 ticks) =====
        if (tickCount % 2 == 0) {
            BlessingsInstance[] slots = {
                    DivinityEngineHelper.getBlessingSlot1(player),
                    DivinityEngineHelper.getBlessingSlot2(player),
                    DivinityEngineHelper.getBlessingSlot3(player)
            };

            for (BlessingsInstance slot : slots) {
                if (slot != null) {
                    GodHelper.validateBlessingSlot(player, slot);

                    if ((slot.getBlessing().isPassive() || slot.getBlessing().canToggle())) {
                        slot.onTick(player);
                    } if (slot.getCooldown() != 0){
                        slot.decreaseCooldown(2);
                        System.out.println("Cooldown: " + slot.getCooldown());
                    }

                    GodHelper.updateBlessingInstance(player, slot);
                }
            }
        }

        // End timing
        long duration = System.nanoTime() - start;
        if (duration > 500_000) { // Only log if it takes longer than 0.5ms
            System.out.println("[DEBUG] PlayerTickEvent took " + (duration / 1_000_000.0) + " ms for " + player.getName().getString());
        }
    }
}
