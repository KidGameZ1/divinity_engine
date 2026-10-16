package example.pantheon.connected;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.nightshade.divinity_engine.util.divinity.narrative.DivineNarrativeManager;
import net.nightshade.divinity_engine.util.divinity.trials.TrialSessionManager;

/** One completed crafting notification, not one inventory item, is one star-chart deed. */
public final class StarChartEvents {
    // Forge events use identity equality. Weak keys do not retain players after dispatch.
    private static final Set<PlayerEvent.ItemCraftedEvent> SEEN = Collections.newSetFromMap(new WeakHashMap<>());
    private static boolean registered;

    private StarChartEvents() {}

    public static synchronized void register() {
        if (registered) return;
        MinecraftForge.EVENT_BUS.register(StarChartEvents.class);
        registered = true;
    }

    @SubscribeEvent
    public static void crafted(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide
                || !player.server.isSameThread() || player.isRemoved() || !player.isAlive()
                || player.hasDisconnected() || player.isSpectator() || player.isCreative()
                || TrialSessionManager.getSession(player) != null
                || player.level().dimension().location().equals(new ResourceLocation("divinity_engine", "godly_trials"))) return;
        // ItemCraftedEvent is a non-cancelable successful output notification in Forge 1.20.1.
        // Count the notification once; getCrafting().getCount() is not a reliable batch delta.
        if (!event.getCrafting().is(Items.COMPASS) || !SEEN.add(event)) return;
        reportObjective(player);
    }

    private static void reportObjective(ServerPlayer player) {
        // The checked-in manager currently names its trusted reporting hook recordObjective.
        DivineNarrativeManager.recordObjective(player, ConnectedPantheonAddon.STAR_CHART,
            ConnectedPantheonAddon.CHART_CONDITION, 1);
        DivineNarrativeManager.sync(player);
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        SEEN.clear();
    }
}
