package net.nightshade.divinity_engine.divinity.gods.terra;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraftforge.registries.RegistryObject;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.curse.Curse;
import net.nightshade.divinity_engine.divinity.gods.BaseGod;
import net.nightshade.divinity_engine.divinity.gods.BaseGodInstance;
import net.nightshade.divinity_engine.network.events.divinity.gods.worship_events.OfferEvent;
import net.nightshade.divinity_engine.util.divinity.gods.GodHelper;

import java.awt.*;
import java.util.Random;
import java.util.Set;

/**
 * Represents Terra, the deity of nature and growth in the Divinity Engine.
 * Terra accepts offerings related to plant life and natural growth, such as azalea, moss blocks, and saplings.
 * When valid offerings are made, the deity rewards worshippers with favor points.
 */
public class Terra extends BaseGod {
    /**
     * Creates a new god with specified domains, blessings and text color.
     * Automatically registers all domains with the GodsDomainRegistry.
     *
     * @param domains   The set of domain names this god controls
     * @param blessings The set of blessings this god can grant
     * @param curse
     * @param textColor The color used for god-related text in UI
     */
    public Terra(Set<String> domains, Set<RegistryObject<Blessings>> blessings, Curse curse, Color textColor) {
        super(domains, blessings, curse, textColor);
    }
    /**
     * Constructs a new Terra deity instance.
     *
     * @param domains   The domains of influence for Terra, typically related to nature and growth
     * @param blessings The set of blessings that Terra can bestow upon worshippers
     * @param textColor The color used for Terra's text in the game interface
     */


    /**
     * Processes items offered to Terra and grants favor for acceptable offerings.
     * Accepted offerings include:
     * - Azalea
     * - Moss Block
     * - Any type of Sapling
     * Upon accepting an offering, grants 3-5 favor points to the worshipper.
     *
     * @param instance The current god instance receiving the offering
     * @param event    The event containing the offered item and offering player
     * @return true if the offering was accepted, false otherwise
     */
    @Override
    public boolean onOfferedItems(BaseGodInstance instance, OfferEvent.OfferItemEvent event) {
        if (event.getOffer().is(Items.AZALEA) ||
                event.getOffer().is(Items.MOSS_BLOCK) ||
                event.getOffer().getItem() instanceof BlockItem blockItem && blockItem.getBlock()instanceof SaplingBlock) {
            int favorAmount = new Random().nextInt(3, 5 + 1);
            GodHelper.increaseFavor(instance, event.getOfferer(), favorAmount);
            return true;
        }
        return super.onOfferedItems(instance, event);
    }
}
