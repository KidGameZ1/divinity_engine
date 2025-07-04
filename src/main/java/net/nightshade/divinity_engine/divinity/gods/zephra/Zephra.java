package net.nightshade.divinity_engine.divinity.gods.zephra;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.RegistryObject;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.curse.Curse;
import net.nightshade.divinity_engine.divinity.gods.BaseGod;
import net.nightshade.divinity_engine.divinity.gods.BaseGodInstance;
import net.nightshade.divinity_engine.network.events.divinity.gods.worship_events.OfferEvent;
import net.nightshade.divinity_engine.network.events.divinity.gods.worship_events.PrayEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.Items;
import net.nightshade.divinity_engine.util.divinity.gods.GodHelper;

import java.awt.*;
import java.util.Set;

/**
 * Represents Zephra, the god of air and wind.
 * Accepts offerings related to flight and height, granting increased favor for aerial items.
 */
public class Zephra extends BaseGod {
    /**
     * Creates a new god with specified domains, blessings and text color.
     * Automatically registers all domains with the GodsDomainRegistry.
     *
     * @param domains   The set of domain names this god controls
     * @param blessings The set of blessings this god can grant
     * @param curse
     * @param textColor The color used for god-related text in UI
     */
    public Zephra(Set<String> domains, Set<RegistryObject<Blessings>> blessings, Curse curse, Color textColor) {
        super(domains, blessings, curse, textColor);
    }
    /**
     * Creates a new instance of Zephra.
     *
     * @param domains   The domains this god presides over
     * @param blessings The set of blessings this god can grant
     * @param textColor The color used for this god's text in the UI
     */


    /**
     * Handles item offerings to Zephra.
     * Grants increased favor for phantom membranes, feathers, and scaffolding.
     *
     * @param instance The god instance receiving the offering
     * @param event    The offering event containing the offered item
     * @return true if the offering was accepted, false otherwise
     */
    @Override
    public boolean onOfferedItems(BaseGodInstance instance, OfferEvent.OfferItemEvent event) {
        ItemStack stack = event.getOffer();
        if (event.getOffer().is(Items.PHANTOM_MEMBRANE) ||
                event.getOffer().is(Items.FEATHER) ||
                event.getOffer().is(Items.SCAFFOLDING)) {
            GodHelper.increaseFavor(instance, event.getOfferer(), stack.getCount()+4);
            return true;
        }
        return super.onOfferedItems(instance, event);
    }

    /**
     * Handles prayer events to Zephra.
     * Grants favor when praying at high altitudes (Y >= 50).
     *
     * @param instance The god instance being prayed to
     * @param event    The prayer event
     */
    @Override
    public void onPrayedTo(BaseGodInstance instance, PrayEvent event) {
        Player player = (Player) event.getLiving();
        if (player.getY() >= 50) {
            GodHelper.increaseFavor(instance, event.getLiving(), 3);
        }
        super.onPrayedTo(instance, event);
    }
}
