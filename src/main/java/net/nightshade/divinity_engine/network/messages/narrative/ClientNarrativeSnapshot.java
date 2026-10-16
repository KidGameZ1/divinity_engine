package net.nightshade.divinity_engine.network.messages.narrative;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;

/** Queue state even before the client player exists; clear it at the connection boundary. */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = "divinity_engine", value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public final class ClientNarrativeSnapshot {
    private static final JournalReplica REPLICA = new JournalReplica();
    private static boolean needsFull;
    private static long ticks, nextRequest;
    static void apply(CompoundTag data) {
        if (!REPLICA.apply(data)) needsFull = true;
        else if (data.getBoolean("full")) needsFull = false;
        attach();
    }
    private static void attach() {
        var player = Minecraft.getInstance().player;
        if (player != null && REPLICA.snapshot() != null)
            player.getPersistentData().put("divinity_engine_narrative_journal", REPLICA.snapshot());
    }
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void tick(net.minecraftforge.event.TickEvent.ClientTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
        ticks++; attach();
        var mc = Minecraft.getInstance();
        if (mc.player != null && mc.getConnection() != null && ticks >= nextRequest
                && (needsFull || REPLICA.snapshot() == null && mc.screen instanceof net.nightshade.divinity_engine.client.screen.DivineCodexScreen)) {
            NarrativeNetwork.requestFull(); nextRequest = ticks + 40;
        }
    }
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void logout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        REPLICA.clear(); needsFull = false; ticks = 0; nextRequest = 0;
    }
}
