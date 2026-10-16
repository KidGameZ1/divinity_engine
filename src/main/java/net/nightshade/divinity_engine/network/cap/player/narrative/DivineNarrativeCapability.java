package net.nightshade.divinity_engine.network.cap.player.narrative;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;

public final class DivineNarrativeCapability {
    public static final Capability<DivineNarrativeData> CAP = CapabilityManager.get(new CapabilityToken<>() {});
    private static boolean registered;
    private DivineNarrativeCapability() {}
    public static void init(IEventBus bus) {
        if (registered) return; registered = true;
        bus.addListener((RegisterCapabilitiesEvent event) -> event.register(DivineNarrativeData.class));
        MinecraftForge.EVENT_BUS.addGenericListener(Entity.class, DivineNarrativeCapability::attach);
        MinecraftForge.EVENT_BUS.addListener(DivineNarrativeCapability::clonePlayer);
    }
    public static DivineNarrativeData get(Player player) { return player.getCapability(CAP).orElse(null); }
    private static void attach(AttachCapabilitiesEvent<Entity> event) {
        if (!(event.getObject() instanceof Player) || event.getObject() instanceof FakePlayer) return;
        Provider provider = new Provider();
        event.addCapability(new ResourceLocation("divinity_engine", "divine_narrative"), provider);
        event.addListener(provider::invalidate);
    }
    private static void clonePlayer(PlayerEvent.Clone event) {
        event.getOriginal().reviveCaps();
        try {
            DivineNarrativeData from = get(event.getOriginal()), to = get(event.getEntity());
            if (from != null && to != null) to.deserializeNBT(from.serializeNBT());
        } finally { event.getOriginal().invalidateCaps(); }
    }
    private static final class Provider implements ICapabilitySerializable<CompoundTag> {
        private final DivineNarrativeData data = new DivineNarrativeData();
        private LazyOptional<DivineNarrativeData> value = LazyOptional.of(() -> data);
        private void invalidate() { value.invalidate(); }
        @Override public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
            if (cap != CAP) return LazyOptional.empty();
            // Forge revives the same player/provider during dimension travel and clone callbacks.
            // It restores dispatcher access, but cannot revive a previously invalidated LazyOptional.
            if (!value.isPresent()) value = LazyOptional.of(() -> data);
            return value.cast();
        }
        @Override public CompoundTag serializeNBT() { return data.serializeNBT(); }
        @Override public void deserializeNBT(CompoundTag tag) { data.deserializeNBT(tag); }
    }
}
