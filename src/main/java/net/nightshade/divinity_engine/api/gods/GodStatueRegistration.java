package net.nightshade.divinity_engine.api.gods;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.nightshade.divinity_engine.block.StatueBlock;
import net.nightshade.divinity_engine.block.entity.StatueBlockEntity;
import net.nightshade.divinity_engine.divinity.gods.BaseGod;
import net.nightshade.divinity_engine.item.StatueBlockItem;

import java.util.Objects;
import java.util.function.Supplier;

/** Explicit, lazy links shared by placement, chunk loading, structures and addon code. */
public final class GodStatueRegistration<T extends BaseGod> {
    private final RegistryObject<T> god;
    private final RegistryObject<StatueBlock> block;
    private final RegistryObject<BlockItem> item;
    private final RegistryObject<BlockEntityType<StatueBlockEntity>> blockEntity;

    GodStatueRegistration(String godPath, String statuePath, Supplier<? extends T> factory,
                         DeferredRegister<BaseGod> gods, DeferredRegister<Block> blocks,
                         DeferredRegister<Item> items, DeferredRegister<BlockEntityType<?>> types) {
        god = gods.register(godPath, () -> createGod(factory));
        block = blocks.register(statuePath, () -> new StatueBlock(god, this::entityType));
        item = items.register(statuePath, () -> new StatueBlockItem(block.get(), new Item.Properties()));
        // Neither factory resolves the god or its entity type during block registration.
        blockEntity = types.register(statuePath, () -> BlockEntityType.Builder.of(
                (pos, state) -> new StatueBlockEntity(pos, state, entityType()), block.get()).build(null));
        if (!block.getId().getNamespace().equals(god.getId().getNamespace())
                || !block.getId().equals(item.getId()) || !block.getId().equals(blockEntity.getId()))
            throw new IllegalArgumentException("All content registers must use the god's namespace: " + god.getId());
    }

    private T createGod(Supplier<? extends T> factory) {
        T value = Objects.requireNonNull(factory.get(), "God factory returned null for " + god.getId());
        value.bindStatue(block, blockEntity);
        return value;
    }

    private BlockEntityType<StatueBlockEntity> entityType() { return blockEntity.get(); }
    public ResourceLocation id() { return block.getId(); }
    public RegistryObject<T> god() { return god; }
    public RegistryObject<StatueBlock> block() { return block; }
    public RegistryObject<BlockItem> item() { return item; }
    public RegistryObject<BlockEntityType<StatueBlockEntity>> blockEntity() { return blockEntity; }

    public void validate() {
        ResourceLocation id = id();
        if (!god.isPresent() || !block.isPresent() || !item.isPresent() || !blockEntity.isPresent())
            throw new IllegalStateException("Incomplete statue registration for " + god.getId());
        if (!id.equals(ForgeRegistries.BLOCKS.getKey(block.get()))
                || !id.equals(ForgeRegistries.ITEMS.getKey(item.get()))
                || !id.equals(ForgeRegistries.BLOCK_ENTITY_TYPES.getKey(blockEntity.get()))
                || block.get().getGod() != god.get() || item.get().getBlock() != block.get()
                || god.get().getStatueBlock() != block.get() || god.get().getStatueBlockEntity() != blockEntity.get()
                || !blockEntity.get().isValid(block.get().defaultBlockState()))
            throw new IllegalStateException("Mismatched statue registration for " + god.getId() + " at " + id);
    }
}
