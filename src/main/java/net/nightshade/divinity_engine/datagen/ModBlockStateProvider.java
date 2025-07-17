package net.nightshade.divinity_engine.datagen;


import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.RegistryObject;
import net.nightshade.divinity_engine.DivinityEngineMod;
import net.nightshade.divinity_engine.block.StatueBlock;
import net.nightshade.divinity_engine.registry.blocks.BlocksRegistry;

public class ModBlockStateProvider extends BlockStateProvider {
    public ModBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, DivinityEngineMod.MODID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        // Get all statue blocks from the registry
        BlocksRegistry.REGISTRY.getEntries().forEach(block -> {
            if (block.get() instanceof StatueBlock) {
                // Create the blockstate with rotations
                Block statueBlock = block.get();
                horizontalBlock(statueBlock,
                        models().getExistingFile(modLoc("block/statue")));
            }
        });

        blockWithItem(BlocksRegistry.POCKET_DIMENSION);
    }


    private void blockWithItem(RegistryObject<Block> blockRegistryObject) {
        simpleBlockWithItem(blockRegistryObject.get(), cubeAll(blockRegistryObject.get()));
    }
}