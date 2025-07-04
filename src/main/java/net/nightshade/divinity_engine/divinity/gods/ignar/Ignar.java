package net.nightshade.divinity_engine.divinity.gods.ignar;

import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.RegistryObject;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.curse.Curse;
import net.nightshade.divinity_engine.divinity.gods.BaseGod;
import net.nightshade.divinity_engine.divinity.gods.BaseGodInstance;
import net.nightshade.divinity_engine.network.events.divinity.gods.worship_events.OfferEvent;
import net.nightshade.divinity_engine.network.events.divinity.gods.worship_events.WhileNearStatueEvent;
import net.nightshade.divinity_engine.util.divinity.gods.GodHelper;

import java.awt.*;
import java.util.Random;
import java.util.Set;

/**
 * Represents Ignar, the god of fire and heat.
 * Accepts offerings related to fire and rewards worshippers who embrace flames.
 */
public class Ignar extends BaseGod {
    /**
     * Creates a new god with specified domains, blessings and text color.
     * Automatically registers all domains with the GodsDomainRegistry.
     *
     * @param domains   The set of domain names this god controls
     * @param blessings The set of blessings this god can grant
     * @param curse
     * @param textColor The color used for god-related text in UI
     */
    public Ignar(Set<String> domains, Set<RegistryObject<Blessings>> blessings, Curse curse, Color textColor) {
        super(domains, blessings, curse, textColor);
    }
    /**
     * Creates a new instance of Ignar god.
     *
     * @param domains   Set of domains this god has influence over
     * @param blessings Set of blessings this god can bestow
     * @param textColor Color used for text representation of this god
     */


    /**
     * Handles item offerings to Ignar. Accepts fire-related items like coal blocks, blaze rods, and magma blocks.
     *
     * @param instance The god instance receiving the offering
     * @param event    The offering event containing the offered item
     * @return true if the offering was accepted, false otherwise
     */
    @Override
    public boolean onOfferedItems(BaseGodInstance instance, OfferEvent.OfferItemEvent event) {
        Random random = new Random();
        if (event.getOffer().is(Items.COAL_BLOCK) || event.getOffer().is(Items.BLAZE_ROD) || event.getOffer().is(Items.MAGMA_BLOCK)) {
            GodHelper.increaseFavor(instance, event.getOfferer(), random.nextInt(4, 7));
            return true;
        }
        return super.onOfferedItems(instance, event);
    }

    /**
     * Handles entity offerings to Ignar. Accepts entities that are on fire.
     *
     * @param instance The god instance receiving the offering
     * @param event    The offering event containing the offered entity
     * @return true if the offering was accepted, false otherwise
     */
    @Override
    public boolean onOfferedEntity(BaseGodInstance instance, OfferEvent.OfferEntityEvent event) {
        if (event.getOffer().isOnFire()) {
            GodHelper.increaseFavor(instance, event.getOfferer(), 3);
            return true;
        }
        return super.onOfferedEntity(instance, event);
    }

    /**
     * Rewards worshippers who are near Ignar's statue while being on fire and having fire resistance.
     *
     * @param instance The god instance associated with the statue
     * @param event    The event containing information about the entity near the statue
     */
    @Override
    public void whileNearShrine(BaseGodInstance instance, WhileNearStatueEvent event) {
        if (event.getLiving().hasEffect(MobEffects.FIRE_RESISTANCE) && event.getLiving().isOnFire()) {
            GodHelper.increaseFavor(instance, event.getLiving(), 3);
        }
        super.whileNearShrine(instance, event);
    }
}
