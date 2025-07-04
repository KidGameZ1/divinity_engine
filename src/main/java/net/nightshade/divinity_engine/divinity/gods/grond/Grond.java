package net.nightshade.divinity_engine.divinity.gods.grond;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.RegistryObject;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.curse.Curse;
import net.nightshade.divinity_engine.divinity.gods.BaseGod;
import net.nightshade.divinity_engine.divinity.gods.BaseGodInstance;
import net.nightshade.divinity_engine.network.events.divinity.gods.worship_events.OfferEvent;
import net.nightshade.divinity_engine.util.divinity.gods.GodHelper;

import java.awt.*;
import java.util.Set;

/**
 * Represents Grond, the god of minerals and precious materials.
 * Accepts offerings of various ores, metals, and gems with different favor rewards based on their value.
 */
public class Grond extends BaseGod {
    /**
     * Creates a new god with specified domains, blessings and text color.
     * Automatically registers all domains with the GodsDomainRegistry.
     *
     * @param domains   The set of domain names this god controls
     * @param blessings The set of blessings this god can grant
     * @param curse
     * @param textColor The color used for god-related text in UI
     */
    public Grond(Set<String> domains, Set<RegistryObject<Blessings>> blessings, Curse curse, Color textColor) {
        super(domains, blessings, curse, textColor);
    }
    /**
     * Creates a new instance of Grond god.
     *
     * @param domains   Set of domains this god has influence over
     * @param blessings Set of blessings this god can bestow
     * @param textColor Color used for god-related text in UI
     */


    /**
     * Handles item offerings made to Grond.
     * Different materials provide different amounts of favor:
     * - Raw blocks and budding amethyst: base + 5
     * - High-value ores and blocks: base + 10
     * - Common ores: base + 2
     * - Precious materials: base + 7
     * - Processed blocks: base + 7
     * - Basic materials: base value only
     *
     * @param instance Current god instance
     * @param event    The offering event containing the offered item
     * @return true if offering was accepted, false otherwise
     */
    @Override
    public boolean onOfferedItems(BaseGodInstance instance, OfferEvent.OfferItemEvent event) {
        ItemStack stack = event.getOffer();
        if (stack.is(Items.RAW_IRON_BLOCK) || stack.is(Items.RAW_COPPER_BLOCK) || stack.is(Items.RAW_GOLD_BLOCK) || stack.is(Items.BUDDING_AMETHYST)) {
            GodHelper.increaseFavor(instance, event.getOfferer(), stack.getCount()+5);
            return true;
        } else if (stack.is(Items.DIAMOND_ORE) || stack.is(Items.ANCIENT_DEBRIS) || stack.is(Items.NETHERITE_BLOCK) || stack.is(Items.DIAMOND_BLOCK)) {
            GodHelper.increaseFavor(instance, event.getOfferer(), stack.getCount()+10);
            return true;
        } else if (stack.is(Items.GOLD_ORE) || stack.is(Items.IRON_ORE) || stack.is(Items.COPPER_ORE) || stack.is(Items.LAPIS_ORE) || stack.is(Items.REDSTONE_ORE) || stack.is(Items.EMERALD_ORE)) {
            GodHelper.increaseFavor(instance, event.getOfferer(), stack.getCount()+2);
            return true;
        } else if (stack.is(Items.DIAMOND) || stack.is(Items.NETHERITE_SCRAP) || stack.is(Items.NETHERITE_INGOT)) {
            GodHelper.increaseFavor(instance, event.getOfferer(), stack.getCount()+7);
            return true;
        } else if (stack.is(Items.GOLD_BLOCK) || stack.is(Items.IRON_BLOCK) || stack.is(Items.COPPER_BLOCK) || stack.is(Items.LAPIS_BLOCK) || stack.is(Items.EMERALD_BLOCK) || stack.is(Items.REDSTONE_BLOCK) || stack.is(Items.AMETHYST_BLOCK)) {
            GodHelper.increaseFavor(instance, event.getOfferer(), stack.getCount()+7);
            return true;
        } else if (stack.is(Items.GOLD_INGOT) || stack.is(Items.IRON_INGOT) || stack.is(Items.COPPER_INGOT) || stack.is(Items.LAPIS_LAZULI) || stack.is(Items.EMERALD) || stack.is(Items.REDSTONE) || stack.is(Items.AMETHYST_SHARD)) {
            GodHelper.increaseFavor(instance, event.getOfferer(), stack.getCount());
            return true;
        }
        return super.onOfferedItems(instance, event);
    }
}
