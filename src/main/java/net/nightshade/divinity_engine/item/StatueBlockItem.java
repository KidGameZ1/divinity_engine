package net.nightshade.divinity_engine.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.nightshade.divinity_engine.block.StatueBlock;

/** Compose translated names without returning localized text as a translation key. */
public final class StatueBlockItem extends BlockItem {
    public StatueBlockItem(StatueBlock block, Properties properties) {
        super(block, properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return getBlock().getName();
    }
}
