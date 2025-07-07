package net.nightshade.divinity_engine.datagen.loot;

import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.RegistryObject;
import net.nightshade.divinity_engine.block.StatueBlock;
import net.nightshade.divinity_engine.registry.blocks.BlocksRegistry;

import java.util.Set;

public class ModBlockLootTables extends BlockLootSubProvider {
    public ModBlockLootTables() {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags());
    }

    @Override
    protected void generate() {
        for (Block block : getKnownBlocks()) {
            if (block instanceof StatueBlock){
                this.dropSelf(block);
            }
        }

    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return BlocksRegistry.REGISTRY.getEntries().stream().map(RegistryObject::get)::iterator;
    }
}