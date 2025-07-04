package net.nightshade.divinity_engine.divinity.gods.voltira;

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
 * Represents Voltira, the god of lightning and storms.
 * Accepts offerings related to electrical and storm elements.
 */
public class Voltira extends BaseGod {
    /**
     * Creates a new god with specified domains, blessings and text color.
     * Automatically registers all domains with the GodsDomainRegistry.
     *
     * @param domains   The set of domain names this god controls
     * @param blessings The set of blessings this god can grant
     * @param curse
     * @param textColor The color used for god-related text in UI
     */
    public Voltira(Set<String> domains, Set<RegistryObject<Blessings>> blessings, Curse curse, Color textColor) {
        super(domains, blessings, curse, textColor);
    }
    /**
     * Creates a new Voltira god instance.
     *
     * @param domains   Set of domains this god controls
     * @param blessings Set of blessings this god can grant
     * @param textColor Color used for text related to this god
     */


    /**
     * Handles item offerings to Voltira.
     * Accepts redstone blocks, lightning rods and tridents with increased favor.
     *
     * @param instance The god instance receiving the offering
     * @param event    The offering event
     * @return true if the offering was accepted, false otherwise
     */
    @Override
    public boolean onOfferedItems(BaseGodInstance instance, OfferEvent.OfferItemEvent event) {
        ItemStack stack = event.getOffer();
        if (stack.is(Items.REDSTONE_BLOCK) || stack.is(Items.LIGHTNING_ROD) || stack.is(Items.TRIDENT)) {
            GodHelper.increaseFavor(instance, event.getOfferer(), stack.getCount()+7);
            return true;
        }
        return super.onOfferedItems(instance, event);
    }

    /**
     * Handles entity offerings to Voltira.
     * Grants extra favor for sacrifices during thunderstorms.
     *
     * @param instance The god instance receiving the offering
     * @param event    The offering event
     * @return true if the offering was accepted, false otherwise
     */
    @Override
    public boolean onOfferedEntity(BaseGodInstance instance, OfferEvent.OfferEntityEvent event) {
        Entity offer = event.getOffer();
        if (offer instanceof LivingEntity living) {
            if (living.level().isRaining() && living.level().isThundering()) {
                GodHelper.increaseFavor(instance, event.getOfferer(), 10);
                return true;
            }
        }
        return super.onOfferedEntity(instance, event);
    }
}
