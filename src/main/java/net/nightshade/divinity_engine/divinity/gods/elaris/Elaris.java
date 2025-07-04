package net.nightshade.divinity_engine.divinity.gods.elaris;

import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
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
 * Elaris represents the god of knowledge and wisdom.
 * This god favors offerings related to books, enchanted items, and magical artifacts.
 */
public class Elaris extends BaseGod {
    /**
     * Creates a new god with specified domains, blessings and text color.
     * Automatically registers all domains with the GodsDomainRegistry.
     *
     * @param domains   The set of domain names this god controls
     * @param blessings The set of blessings this god can grant
     * @param curse
     * @param textColor The color used for god-related text in UI
     */
    public Elaris(Set<String> domains, Set<RegistryObject<Blessings>> blessings, Curse curse, Color textColor) {
        super(domains, blessings, curse, textColor);
    }
    /**
     * Creates a new instance of Elaris god
     *
     * @param domains   Set of domains this god has influence over
     * @param blessings Set of blessings this god can bestow
     * @param textColor Color used for text representation
     */


    /**
     * Handles item offerings made to Elaris.
     * Books and magical items provide favor, with enchanted items providing additional favor based on enchantment levels.
     * Cursed items reduce favor.
     *
     * @param instance The god instance receiving the offering
     * @param event    The offering event containing the offered item
     * @return true if the offering was accepted, false otherwise
     */
    @Override
    public boolean onOfferedItems(BaseGodInstance instance, OfferEvent.OfferItemEvent event) {
        ItemStack stack = event.getOffer();

        if (stack.getItem() instanceof BookItem){
            GodHelper.increaseFavor(instance, event.getOfferer(), stack.getCount()+10);
            return true;
        }
        else if (stack.getItem() instanceof EnchantedBookItem enchantedBookItem){
            // Base favor from item count
            int favor = stack.getCount();
            // Calculate additional favor based on enchantments
            for (Enchantment enchantment : enchantedBookItem.getAllEnchantments(stack).keySet()) {
                if (enchantment.isCurse()) {
                    // Cursed enchantments reduce favor
                    if (EnchantmentHelper.getTagEnchantmentLevel(enchantment, stack) != 0) {
                        favor -= 30 * EnchantmentHelper.getTagEnchantmentLevel(enchantment, stack);
                    } else {
                        favor -= 30;
                    }
                } else {
                    // Normal enchantments increase favor
                    if (EnchantmentHelper.getTagEnchantmentLevel(enchantment, stack) != 0) {
                        favor += 30 * EnchantmentHelper.getTagEnchantmentLevel(enchantment, stack);
                    }else {
                        favor += 30;
                    }
                }
            }
            GodHelper.increaseFavor(instance, event.getOfferer(), favor);
            return true;
        }
        else if (stack.getItem() instanceof WrittenBookItem) {
            GodHelper.increaseFavor(instance, event.getOfferer(), stack.getCount()+10);
            return true;
        }
        else if (stack.getItem() instanceof EnchantedGoldenAppleItem) {
            GodHelper.increaseFavor(instance, event.getOfferer(), stack.getCount()*30);
            return true;
        }

        return super.onOfferedItems(instance, event);
    }
}
