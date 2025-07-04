package net.nightshade.divinity_engine.datagen;

import net.minecraft.data.PackOutput;
import net.minecraftforge.client.model.generators.BlockModelBuilder;
import net.minecraftforge.client.model.generators.ModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.nightshade.divinity_engine.DivinityEngineMod;
import net.nightshade.divinity_engine.block.StatueBlock;
import net.nightshade.divinity_engine.registry.blocks.BlocksRegistry;

public class ModBlockModelProvider extends ModelProvider<BlockModelBuilder> {


    public ModBlockModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, DivinityEngineMod.MODID, "block", BlockModelBuilder::new, existingFileHelper);
    }



    @Override
    protected void registerModels() {
        // Create models for all statue blocks
        BlocksRegistry.REGISTRY.getEntries().forEach(block -> {
            if (block.get() instanceof StatueBlock) {
                String path = block.getId().getPath();
                getBuilder(path)
                        .parent(getExistingFile(modLoc("block/statue")))
                        .texture("all", mcLoc("block/stone_bricks"))
                        .texture("particle", mcLoc("block/stone_bricks"))
                        .texture("0", modLoc("block/gods_statue"))
                        .renderType("solid");
            }
        });
    }

    @Override
    public String getName() {
        return "Block Models";
    }

}
