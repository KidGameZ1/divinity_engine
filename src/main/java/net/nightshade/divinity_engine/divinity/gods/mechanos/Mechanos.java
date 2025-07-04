package net.nightshade.divinity_engine.divinity.gods.mechanos;

import net.minecraft.world.item.ArmorMaterials;
import net.minecraftforge.registries.RegistryObject;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.curse.Curse;
import net.nightshade.divinity_engine.divinity.gods.BaseGod;
import net.nightshade.divinity_engine.divinity.gods.BaseGodInstance;
import net.nightshade.divinity_engine.network.events.divinity.gods.worship_events.OfferEvent;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.entity.EquipmentSlot;
import net.nightshade.divinity_engine.network.events.divinity.gods.worship_events.PrayEvent;
import net.nightshade.divinity_engine.util.divinity.gods.GodHelper;

import java.awt.*;
import java.util.Set;
import java.util.Random;

/**
 * Represents Mechanos, the god of machinery and technology.
 * This god favors offerings of mechanical and technological items,
 * and rewards worshippers who wear iron armor.
 */
public class Mechanos extends BaseGod {
    /**
     * Creates a new god with specified domains, blessings and text color.
     * Automatically registers all domains with the GodsDomainRegistry.
     *
     * @param domains   The set of domain names this god controls
     * @param blessings The set of blessings this god can grant
     * @param curse
     * @param textColor The color used for god-related text in UI
     */
    public Mechanos(Set<String> domains, Set<RegistryObject<Blessings>> blessings, Curse curse, Color textColor) {
        super(domains, blessings, curse, textColor);
    }
    /**
     * Creates a new Mechanos god instance
     *
     * @param domains   Set of domains this god governs
     * @param blessings Set of blessings this god can bestow
     * @param textColor Color used for text representation of this god
     */


    /**
     * Handles item offerings to Mechanos.
     * Accepts iron blocks, comparators, and observers as favorable offerings,
     * granting 4-6 favor points.
     *
     * @param instance The god instance receiving the offering
     * @param event    The offering event containing offered item details
     * @return true if the offering was accepted, false otherwise
     */
    @Override
    public boolean onOfferedItems(BaseGodInstance instance, OfferEvent.OfferItemEvent event) {
        Random random = new Random();
        if (event.getOffer().is(Items.IRON_BLOCK) ||
                event.getOffer().is(Items.COMPARATOR) ||
                event.getOffer().is(Items.OBSERVER)) {
            int favor = random.nextInt(4, 7);
            GodHelper.increaseFavor(instance, event.getOfferer(), favor);
            return true;
        }
        return super.onOfferedItems(instance, event);
    }

    /**
     * Handles prayer events to Mechanos.
     * Grants additional favor points based on the amount of iron armor
     * pieces worn by the praying entity.
     *
     * @param instance The god instance being prayed to
     * @param event    The prayer event containing prayer details
     */
    @Override
    public void onPrayedTo(BaseGodInstance instance, PrayEvent event) {
        int ironArmorCount = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (event.getLiving().getItemBySlot(slot).getItem() instanceof ArmorItem armor
                    && armor.getMaterial() == ArmorMaterials.IRON) {
                ironArmorCount++;
            }
        }
        if (ironArmorCount > 0) {
            GodHelper.increaseFavor(instance, event.getLiving(), 2 + ironArmorCount);
        }

        super.onPrayedTo(instance, event);
    }
}
