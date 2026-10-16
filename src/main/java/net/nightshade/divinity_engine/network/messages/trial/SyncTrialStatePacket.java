package net.nightshade.divinity_engine.network.messages.trial;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.nightshade.divinity_engine.client.trial.ClientTrialState;

import java.util.function.Supplier;

public class SyncTrialStatePacket {
    private final CompoundTag tag;

    public SyncTrialStatePacket(FriendlyByteBuf buffer) {
        this.tag = buffer.readAnySizeNbt();
    }

    public SyncTrialStatePacket(CompoundTag tag) {
        this.tag = tag.copy();
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeNbt(tag);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientTrialState.apply(tag)));
        context.setPacketHandled(true);
    }
}
