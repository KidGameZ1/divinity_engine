package net.nightshade.divinity_engine.network.events.divinity.narrative;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

/** Explicit story donation; intentionally not an ordinary OfferEvent and grants no offering favor. */
@Cancelable
public final class NarrativeDonationEvent extends Event {
    public final ServerPlayer player;
    public final ResourceLocation god;
    public final Item item;
    public final int count;
    public NarrativeDonationEvent(ServerPlayer player, ResourceLocation god, Item item, int count) {
        this.player = player; this.god = god; this.item = item; this.count = count;
    }
}
