package net.nightshade.divinity_engine.network.messages.cap;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.nightshade.divinity_engine.network.cap.player.gods.IMainPlayerCapability;

import java.util.function.Supplier;

public class SyncMainPlayerCapabilityPacket {
    private final CompoundTag tag;
    private final int entityId;

    public SyncMainPlayerCapabilityPacket(FriendlyByteBuf buf) {
        this.tag = buf.readAnySizeNbt();
        this.entityId = buf.readInt();
    }

    public SyncMainPlayerCapabilityPacket(IMainPlayerCapability data, int entityId) {
        this.tag = (CompoundTag)data.serializeNBT();
        this.entityId = entityId;
    }

    public void buffer(FriendlyByteBuf buf) {
        buf.writeNbt(this.tag);
        buf.writeInt(this.entityId);
    }

    public void handler(Supplier<NetworkEvent.Context> ctx) {
        ((NetworkEvent.Context)ctx.get()).enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientAccess.updatePlayerCapability(this.entityId, this.tag)));
        ((NetworkEvent.Context)ctx.get()).setPacketHandled(true);
    }
}