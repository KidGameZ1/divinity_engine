package net.nightshade.divinity_engine.divinity.gods.nerai;

import net.minecraft.world.InteractionHand;
import net.minecraftforge.registries.RegistryObject;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.curse.Curse;
import net.nightshade.divinity_engine.divinity.gods.BaseGod;
import net.nightshade.divinity_engine.divinity.gods.BaseGodInstance;
import net.nightshade.divinity_engine.network.events.divinity.gods.worship_events.OfferEvent;
import net.minecraft.world.item.Items;
import net.nightshade.divinity_engine.network.events.divinity.gods.worship_events.PrayEvent;
import net.nightshade.divinity_engine.network.events.divinity.gods.worship_events.WhileNearStatueEvent;
import net.nightshade.divinity_engine.util.divinity.gods.GodHelper;

import java.util.Random;

import java.awt.*;
import java.util.Set;

/**
 * Represents Nerai, a deity associated with water and oceanic elements.
 * Accepts offerings related to marine items and rewards water-based activities.
 */
public class Nerai extends BaseGod {
    /**
     * Creates a new god with specified domains, blessings and text color.
     * Automatically registers all domains with the GodsDomainRegistry.
     *
     * @param domains   The set of domain names this god controls
     * @param blessings The set of blessings this god can grant
     * @param curse
     * @param textColor The color used for god-related text in UI
     */
    public Nerai(Set<String> domains, Set<RegistryObject<Blessings>> blessings, Curse curse, Color textColor) {
        super(domains, blessings, curse, textColor);
    }
    /**
     * Creates a new instance of Nerai.
     *
     * @param domains   The divine domains this god presides over
     * @param blessings The blessings this god can bestow
     * @param textColor The color associated with this god in the UI
     */


    /**
     * Handles item offerings to Nerai.
     * Accepts prismarine, nautilus shells, and kelp as favorable offerings.
     *
     * @param instance The god instance receiving the offering
     * @param event    The offering event containing the offered item
     * @return true if the offering was accepted, false otherwise
     */
    @Override
    public boolean onOfferedItems(BaseGodInstance instance, OfferEvent.OfferItemEvent event) {
        Random random = new Random();
        if (event.getOffer().is(Items.PRISMARINE) ||
                event.getOffer().is(Items.NAUTILUS_SHELL) ||
                event.getOffer().is(Items.KELP)) {
            GodHelper.increaseFavor(instance, event.getOfferer(), 4 + random.nextInt(3));
            return true;
        }
        return super.onOfferedItems(instance, event);
    }

    /**
     * Handles entity offerings to Nerai.
     * Accepts drowned and squids when they are in water.
     *
     * @param instance The god instance receiving the offering
     * @param event    The offering event containing the offered entity
     * @return true if the offering was accepted, false otherwise
     */
    @Override
    public boolean onOfferedEntity(BaseGodInstance instance, OfferEvent.OfferEntityEvent event) {
        if ((event.getOfferer() instanceof net.minecraft.world.entity.monster.Drowned
                || event.getOfferer() instanceof net.minecraft.world.entity.animal.Squid)
                && event.getOfferer().isInWater()) {
            GodHelper.increaseFavor(instance, event.getOfferer(), 3);
            return true;
        }
        return super.onOfferedEntity(instance, event);
    }

    /**
     * Handles effects when entities are near Nerai's statue.
     * Grants favor when the entity is in rain.
     *
     * @param instance The god instance whose statue is being approached
     * @param event    The statue proximity event
     */
    @Override
    public void whileNearShrine(BaseGodInstance instance, WhileNearStatueEvent event) {
        if (event.getLiving().isSwimming() || event.getLiving().isInWaterOrBubble()) {
            GodHelper.increaseFavor(instance, event.getLiving(), 1);
        } else if (event.getLiving().level().isRaining()) {
            GodHelper.increaseFavor(instance, event.getLiving(), 1);
        }
        super.whileNearShrine(instance, event);
    }

    /**
     * Handles prayers to Nerai.
     * Accepts water buckets as offerings during prayer, converting them to empty buckets.
     *
     * @param instance The god instance being prayed to
     * @param event    The prayer event
     */
    @Override
    public void onPrayedTo(BaseGodInstance instance, PrayEvent event) {
        if (event.getLiving().getMainHandItem().is(Items.WATER_BUCKET)) {
            event.getLiving().setItemInHand(InteractionHand.MAIN_HAND, Items.BUCKET.getDefaultInstance());
            GodHelper.increaseFavor(instance, event.getLiving(), 5);
        }else if (event.getLiving().getOffhandItem().is(Items.WATER_BUCKET)) {
            event.getLiving().setItemInHand(InteractionHand.OFF_HAND, Items.BUCKET.getDefaultInstance());
            GodHelper.increaseFavor(instance, event.getLiving(), 5);
        }
        super.onPrayedTo(instance, event);
    }
}
