package net.nightshade.divinity_engine.divinity.gods.dravak;

import net.minecraft.world.item.*;
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
 * A god of warfare and combat that favors weapon offerings.
 * Accepts various weapons as offerings and grants favor based on their tier and quantity.
 * Also accepts redstone blocks and grants favor for offerings during low health conditions.
 */
public class Dravak extends BaseGod {
    /**
     * Creates a new god with specified domains, blessings and text color.
     * Automatically registers all domains with the GodsDomainRegistry.
     *
     * @param domains   The set of domain names this god controls
     * @param blessings The set of blessings this god can grant
     * @param curse
     * @param textColor The color used for god-related text in UI
     */
    public Dravak(Set<String> domains, Set<RegistryObject<Blessings>> blessings, Curse curse, Color textColor) {
        super(domains, blessings, curse, textColor);
    }
    /**
     * Constructs a new Dravak god instance
     *
     * @param domains   Set of domains that define this god's sphere of influence
     * @param blessings Set of blessings that this god can grant
     * @param textColor Color used for visual representation of this god
     */


    /**
     * Processes item offerings made to Dravak.
     * Accepts weapons (swords, axes, bows, crossbows) and redstone blocks.
     * Favor granted depends on weapon tier, stack count, and item type.
     *
     * @param instance The god instance receiving the offering
     * @param event    The event containing offering information
     * @return true if offering is accepted, false otherwise
     */
    @Override
    public boolean onOfferedItems(BaseGodInstance instance, OfferEvent.OfferItemEvent event) {
        ItemStack stack = event.getOffer();
        if (stack.getItem() instanceof SwordItem item){
            GodHelper.increaseFavor(instance, event.getOfferer(), stack.getCount()+6+item.getTier().getLevel());
            return true;
        }else if (stack.getItem() instanceof AxeItem item){
            GodHelper.increaseFavor(instance, event.getOfferer(), stack.getCount()+6+item.getTier().getLevel());
            return true;
        }else if (stack.getItem() instanceof BowItem){
            GodHelper.increaseFavor(instance, event.getOfferer(), stack.getCount()+6);
            return true;
        }else if (stack.getItem() instanceof CrossbowItem){
            GodHelper.increaseFavor(instance, event.getOfferer(), stack.getCount()+6);
            return true;
        } else if (stack.is(Items.REDSTONE_BLOCK)){
            GodHelper.increaseFavor(instance, event.getOfferer(), stack.getCount()+5);
            return true;
        }
        return super.onOfferedItems(instance, event);
    }

    /**
     * Processes entity offerings made to Dravak.
     * Grants additional favor when the offerer is below 50% health.
     *
     * @param instance The god instance receiving the offering
     * @param event    The event containing offering information
     * @return true if offering is accepted, false otherwise
     */
    @Override
    public boolean onOfferedEntity(BaseGodInstance instance, OfferEvent.OfferEntityEvent event) {

        if (event.getOfferer().getHealth() < event.getOfferer().getMaxHealth() * 0.5) {
            GodHelper.increaseFavor(instance, event.getOfferer(), 5);
            return true;
        }

        return super.onOfferedEntity(instance, event);
    }
}
