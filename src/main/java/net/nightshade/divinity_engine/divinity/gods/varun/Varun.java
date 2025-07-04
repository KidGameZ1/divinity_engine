package net.nightshade.divinity_engine.divinity.gods.varun;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
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
 * Represents Varun, a god that accepts offerings of meat, animal products and hunted entities.
 * Favors hunting with bows and crossbows.
 */
public class Varun extends BaseGod {
    /**
     * Creates a new god with specified domains, blessings and text color.
     * Automatically registers all domains with the GodsDomainRegistry.
     *
     * @param domains   The set of domain names this god controls
     * @param blessings The set of blessings this god can grant
     * @param curse
     * @param textColor The color used for god-related text in UI
     */
    public Varun(Set<String> domains, Set<RegistryObject<Blessings>> blessings, Curse curse, Color textColor) {
        super(domains, blessings, curse, textColor);
    }
    /**
     * Creates a new instance of Varun god.
     *
     * @param domains   The domains this god has influence over
     * @param blessings The blessings this god can bestow
     * @param textColor The color used for this god's text in the UI
     */


    /**
     * Handles when items are offered to Varun.
     * Accepts meat, leather, bones and other animal products.
     *
     * @param instance The god instance receiving the offering
     * @param event    The offer event containing the offered items
     * @return true if the offering was accepted, false otherwise
     */
    @Override
    public boolean onOfferedItems(BaseGodInstance instance, OfferEvent.OfferItemEvent event) {
        ItemStack stack = event.getOffer();
        if (stack.is(Items.BEEF) || stack.is(Items.PORKCHOP) || stack.is(Items.MUTTON) ||
                stack.is(Items.CHICKEN) || stack.is(Items.RABBIT) || stack.is(Items.LEATHER) ||
                stack.is(Items.BONE)) {
            GodHelper.increaseFavor(instance, event.getOfferer(), stack.getCount());
            return true;
        } else if (stack.is(Items.LEATHER) || stack.is(Items.RABBIT_HIDE) || stack.is(Items.RABBIT_FOOT)) {
            GodHelper.increaseFavor(instance, event.getOfferer(), stack.getCount());
            return true;
        }
        return super.onOfferedItems(instance, event);
    }

    /**
     * Handles when entities are offered to Varun.
     * Grants extra favor when entities are hunted with bows or crossbows.
     *
     * @param instance The god instance receiving the offering
     * @param event    The offer event containing the offered entity
     * @return true if the offering was accepted, false otherwise
     */
    @Override
    public boolean onOfferedEntity(BaseGodInstance instance, OfferEvent.OfferEntityEvent event) {
        Entity offer = event.getOffer();

        if (offer instanceof LivingEntity living) {
            if (event.getOfferer().getMainHandItem().is(Items.BOW) || event.getOfferer().getMainHandItem().is(Items.CROSSBOW)) {
                GodHelper.increaseFavor(instance, event.getOfferer(), 10);
                return true;
            }
        }


        return super.onOfferedEntity(instance, event);
    }
}
