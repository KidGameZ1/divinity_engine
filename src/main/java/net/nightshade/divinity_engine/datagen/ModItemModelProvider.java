package net.nightshade.divinity_engine.datagen;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.client.model.generators.ItemModelBuilder;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.RegistryObject;
import net.nightshade.divinity_engine.DivinityEngineMod;
import net.nightshade.divinity_engine.block.StatueBlock;
import net.nightshade.divinity_engine.registry.blocks.BlocksRegistry;
import net.nightshade.divinity_engine.registry.item.ItemsRegistry;


public class ModItemModelProvider extends ItemModelProvider {
    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, DivinityEngineMod.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        // Generate item models for all statue blocks
        BlocksRegistry.REGISTRY.getEntries().forEach(block -> {
            if (block.get() instanceof StatueBlock) {
                String path = block.getId().getPath();
                withExistingParent(path, modLoc("block/statue"));
            }
        });

        simpleItem(ItemsRegistry.DIVINE_CODEX);
    }




    private ItemModelBuilder simpleItem(RegistryObject<Item> item) {
        return withExistingParent(item.getId().getPath(),
                new ResourceLocation("item/generated")).texture("layer0",
                new ResourceLocation(DivinityEngineMod.MODID,"item/" + item.getId().getPath()));
    }
}