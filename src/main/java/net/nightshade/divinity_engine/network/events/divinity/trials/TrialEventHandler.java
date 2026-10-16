package net.nightshade.divinity_engine.network.events.divinity.trials;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.nightshade.divinity_engine.util.divinity.trials.TrialSessionManager;

@Mod.EventBusSubscriber
public class TrialEventHandler {

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        TrialSessionManager.tickSessions();
        net.nightshade.divinity_engine.util.divinity.trials.TrialArenaJobs.get(event.getServer()).tick(event.getServer());
    }

    @SubscribeEvent
    public static void onTakenDamage(LivingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            TrialSessionManager.handlePlayerDamage(serverPlayer, event);
        }
    }

    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        TrialSessionManager.handleDeathEvent(event);
    }

    @SubscribeEvent public static void stopped(net.minecraftforge.event.server.ServerStoppedEvent event) { TrialSessionManager.stop(); }
    @SubscribeEvent public static void stopping(net.minecraftforge.event.server.ServerStoppingEvent event) { TrialSessionManager.shutdown(); }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            TrialSessionManager.handleRespawn(serverPlayer, event);
        }
    }

    @SubscribeEvent
    public static void onHeal(LivingHealEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            TrialSessionManager.handleHeal(serverPlayer, event);
        }
    }
}
