package net.nightshade.divinity_engine.datagen;



import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.nightshade.divinity_engine.DivinityEngineMod;


import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

public class ModItemTagGenerator extends ItemTagsProvider {
    public ModItemTagGenerator(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> future,
                               CompletableFuture<TagLookup<Block>> completableFuture, @Nullable ExistingFileHelper existingFileHelper) {
        super(packOutput, future, completableFuture, DivinityEngineMod.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider pProvider) {
        // Add Item Tags here
//        var runesTag = this.tag(NightshadeRPGModTags.Items.RUNES);
//        NightshadeRPGModRunes.forEachElementRune(runesTag::add);
//        NightshadeRPGModRunes.forEachSpellTypeRune(runesTag::add);
//
//        var spellTag = this.tag(NightshadeRPGModTags.Items.SPELL_SCROLL);
//        NightshadeRPGModItems.forEachSpellScroll(spellTag::add);
//
//        var tomesTag = this.tag(NightshadeRPGModTags.Items.TOMES);
//        NightshadeRPGModItems.forEachGrimoireItem(tomesTag::add);
//        NightshadeRPGModItems.forEachCodexItem(tomesTag::add);
//        NightshadeRPGModItems.forEachSpellScroll(tomesTag::add);

    }

    @Override
    public String getName() {
        return "Item Tags";
    }
}