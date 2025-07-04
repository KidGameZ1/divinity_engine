package net.nightshade.divinity_engine.divinity.gods.vokar;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.RegistryObject;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.curse.Curse;
import net.nightshade.divinity_engine.divinity.gods.BaseGod;
import net.nightshade.divinity_engine.divinity.gods.BaseGodInstance;
import net.nightshade.divinity_engine.network.events.divinity.gods.worship_events.OfferEvent;
import net.nightshade.divinity_engine.network.events.divinity.gods.worship_events.PrayEvent;
import net.nightshade.divinity_engine.util.divinity.gods.GodHelper;

import java.awt.*;
import java.util.Set;

/**
 * Represents Vokar, a god associated with darkness and death.
 * Accepts offerings related to death and the nether realm.
 */
public class Vokar extends BaseGod {
    /**
     * Creates a new god with specified domains, blessings and text color.
     * Automatically registers all domains with the GodsDomainRegistry.
     *
     * @param domains   The set of domain names this god controls
     * @param blessings The set of blessings this god can grant
     * @param curse
     * @param textColor The color used for god-related text in UI
     */
    public Vokar(Set<String> domains, Set<RegistryObject<Blessings>> blessings, Curse curse, Color textColor) {
        super(domains, blessings, curse, textColor);
    }
    /**
     * Creates a new instance of Vokar god.
     *
     * @param domains   The domains this god has influence over
     * @param blessings The blessings this god can bestow
     * @param textColor The color associated with this god in the UI
     */


    /**
     * Handles items offered to Vokar. Accepts nether and death-related items.
     * Increases favor by item count plus 5 for acceptable offerings.
     *
     * @param instance The god instance receiving the offering
     * @param event    The offering event containing the offered item
     * @return true if the offering was accepted, false otherwise
     */
    @Override
    public boolean onOfferedItems(BaseGodInstance instance, OfferEvent.OfferItemEvent event) {
        ItemStack stack = event.getOffer();
        if (stack.is(Items.WITHER_ROSE) || stack.is(Items.SOUL_SAND) || stack.is(Items.SOUL_SOIL) || stack.is(Items.BONE_BLOCK)) {
            GodHelper.increaseFavor(instance, event.getOfferer(), stack.getCount()+5);
            return true;
        }
        return super.onOfferedItems(instance, event);
    }

    /**
     * Handles prayer events to Vokar.
     * Grants favor during daytime for prayers lasting at least 100 ticks.
     *
     * @param instance The god instance being prayed to
     * @param event    The prayer event
     */
    @Override
    public void onPrayedTo(BaseGodInstance instance, PrayEvent event) {
        LivingEntity living = event.getLiving();
        if (living.level().isDay()) {
            if (event.getDuration() >= 100) {
                GodHelper.increaseFavor(instance, event.getLiving(), 1);
            }
        }
        super.onPrayedTo(instance, event);
    }

    /**
     * Handles entity sacrifices to Vokar.
     * Grants 2 favor points for any entity sacrifice.
     *
     * @param instance The god instance receiving the sacrifice
     * @param event    The entity offering event
     * @return true indicating the sacrifice is always accepted
     */
    @Override
    public boolean onOfferedEntity(BaseGodInstance instance, OfferEvent.OfferEntityEvent event) {
        GodHelper.increaseFavor(instance, event.getOfferer(), 2);
        return true;
    }
}
