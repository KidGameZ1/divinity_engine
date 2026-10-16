package net.nightshade.divinity_engine.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** An opaque legacy payload, not a container or a divine menu provider. */
public final class LegacyElderStatueBlockEntity extends BlockEntity {
    private CompoundTag legacyData = new CompoundTag();

    public LegacyElderStatueBlockEntity(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(type, pos, state);
        if (!type.isValid(state)) throw new IllegalArgumentException("Invalid legacy statue state " + state);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        legacyData = tag.copy();
        // Metadata is supplied by BlockEntity at the current location when saving.
        for (String key : java.util.List.of("id", "x", "y", "z")) legacyData.remove(key);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        tag.merge(legacyData.copy());
        super.saveAdditional(tag);
        // Forge replaces this compound when capabilities are attached. Keep unavailable addons' entries.
        CompoundTag previousCaps = legacyData.getCompound("ForgeCaps");
        if (!previousCaps.isEmpty()) {
            CompoundTag currentCaps = tag.getCompound("ForgeCaps");
            for (String key : previousCaps.getAllKeys()) {
                if (!currentCaps.contains(key)) currentCaps.put(key, previousCaps.get(key).copy());
            }
            tag.put("ForgeCaps", currentCaps);
        }
    }

    @Override
    public CompoundTag getUpdateTag() { return saveWithFullMetadata(); }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
