package net.nightshade.divinity_engine.network.messages.cap;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.network.NetworkEvent;
import net.nightshade.divinity_engine.divinity.curse.CurseInstance;
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
        this.syncType = buf.readEnum(SyncType.class);
        this.godsTag = buf.readAnySizeNbt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(this.entityId);
        buf.writeEnum(this.syncType);
        buf.writeNbt(this.godsTag);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> this.syncType.handler.update(this.entityId, this.godsTag));
        ctx.get().setPacketHandled(true);
    }

    public static enum SyncType {
        FULL(INBTSerializable::serializeNBT, (entityId1, updateTag) -> ClientLevelAccessor.execute((level) -> {
            Entity target = level.getEntity(entityId1);
            if (target != null) {
                PlayerGodsCapability.load(target).deserializeNBT(updateTag);
            }
        })),

        CHANGES_ONLY((godsStorage) -> {
            CompoundTag tag = new CompoundTag();
            ListTag gods = new ListTag();

            godsStorage.getDirtyContactedGods().forEach(instance -> {
                instance.resetDirty();

                instance.getDirtyBlessings().forEach(blessing -> {
                    blessing.resetDirty();
                    instance.addBlessing(blessing); // Blessings stored inside the instance
                });

                gods.add(instance.toNBT());
            });

            tag.put("contacted_gods", gods);

            ListTag curse = new ListTag();
            godsStorage.getDirtyCurses().forEach(instance -> {
                instance.resetDirty();
                curse.add(instance.toNBT());
            });
            tag.put("curses", curse);

            return tag;
        }, (entityId1, updateTag) -> ClientLevelAccessor.execute((level) -> {
            Entity target = level.getEntity(entityId1);
            if (target != null) {
                if (updateTag.contains("contacted_gods")) {
                    List<BaseGodInstance> updatedGods = new ArrayList<>();

                    for (Tag tagElement : updateTag.getList("contacted_gods", Tag.TAG_COMPOUND)) {
                        if (tagElement instanceof CompoundTag compoundTag) {
                            updatedGods.add(BaseGodInstance.fromNBT(compoundTag)); // This includes blessings
                        }
                    }

                    PlayerGodsCapability.load(target).updateGodInstances(updatedGods);
                }

                if (updateTag.contains("curses")) {
                    List<CurseInstance> updatedCurses = new ArrayList<>();

                    for (Tag tagElement : updateTag.getList("curses", Tag.TAG_COMPOUND)) {
                        if (tagElement instanceof CompoundTag compoundTag) {
                            updatedCurses.add(CurseInstance.fromNBT(compoundTag));
                        }
                    }

                    PlayerGodsCapability.load(target).updateCurseInstances(updatedCurses);
                }
            }
        }));

        private final UpdateFactory factory;
        private final UpdateHandler handler;

        SyncType(UpdateFactory factory, UpdateHandler handler) {
            this.factory = factory;
            this.handler = handler;
        }
    }

    @FunctionalInterface
    private interface UpdateFactory {
        CompoundTag create(InternalGodsStorage storage);
    }

    @FunctionalInterface
    private interface UpdateHandler {
        void update(int entityId, CompoundTag tag);
    }
}
