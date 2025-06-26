package net.nightshade.divinity_engine.network.messages;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.network.NetworkEvent;
import net.nightshade.divinity_engine.divinity.gods.BaseGodInstance;
import net.nightshade.divinity_engine.network.cap.player.gods.InternalGodsStorage;
import net.nightshade.divinity_engine.network.cap.player.gods.PlayerGodsCapability;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class SyncGodsPacket {
    private final int entityId;
    private final SyncType syncType;
    private final CompoundTag godsTag;

    public SyncGodsPacket(Entity source, InternalGodsStorage internalGodsStorage, SyncType syncType) {
        this.entityId = source.getId();
        this.syncType = syncType;
        this.godsTag = syncType.factory.create(internalGodsStorage);
    }

    public SyncGodsPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.syncType = (SyncType)buf.readEnum(SyncType.class);
        this.godsTag = buf.readAnySizeNbt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(this.entityId);
        buf.writeEnum(this.syncType);
        buf.writeNbt(this.godsTag);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ((NetworkEvent.Context)ctx.get()).enqueueWork(() -> this.syncType.handler.update(this.entityId, this.godsTag));
        ((NetworkEvent.Context)ctx.get()).setPacketHandled(true);
    }

    public static enum SyncType {
        FULL(INBTSerializable::serializeNBT, (entityId1, updateTag) -> ClientLevelAccessor.execute((level) -> {
            Entity target = level.getEntity(entityId1);
            if (target != null) {
                PlayerGodsCapability.load(target).deserializeNBT(updateTag);
            }
        })),
        CHANGES_ONLY((magicStorage) -> {
            CompoundTag tag = new CompoundTag();
            ListTag magic = new ListTag();
            magicStorage.getDirtyContactedGods().forEach((magicInstance) -> {
                magicInstance.resetDirty();
                magic.add(magicInstance.toNBT());
            });
            tag.put("contacted_gods", magic);
            return tag;
        }, (entityId1, updateTag) -> ClientLevelAccessor.execute((level) -> {
            Entity target = level.getEntity(entityId1);
            if (target != null) {
                if (updateTag.contains("contacted_gods")) {
                    List<BaseGodInstance> updatedMagics = new ArrayList();

                    for(Tag tag : updateTag.getList("contacted_gods", 10)) {
                        if (tag instanceof CompoundTag) {
                            CompoundTag compoundTag = (CompoundTag)tag;
                            updatedMagics.add(BaseGodInstance.fromNBT(compoundTag));
                        }
                    }

                    PlayerGodsCapability.load(target).updateGodInstances(updatedMagics);
                }
            }
        }));

        private final UpdateFactory factory;
        private final UpdateHandler handler;

        private SyncType(UpdateFactory factory, UpdateHandler handler) {
            this.factory = factory;
            this.handler = handler;
        }
    }

    @FunctionalInterface
    private interface UpdateFactory {
        CompoundTag create(InternalGodsStorage var1);
    }

    @FunctionalInterface
    private interface UpdateHandler {
        void update(int var1, CompoundTag var2);
    }
}