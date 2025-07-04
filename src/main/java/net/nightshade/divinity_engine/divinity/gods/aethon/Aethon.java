package net.nightshade.divinity_engine.divinity.gods.aethon;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
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
 * Represents Aethon, a god in the Divinity Engine
 */
public class Aethon extends BaseGod {
    /**
     * Creates a new god with specified domains, blessings and text color.
     * Automatically registers all domains with the GodsDomainRegistry.
     *
     * @param domains   The set of domain names this god controls
     * @param blessings The set of blessings this god can grant
     * @param curse
     * @param textColor The color used for god-related text in UI
     */
    public Aethon(Set<String> domains, Set<RegistryObject<Blessings>> blessings, Curse curse, Color textColor) {
        super(domains, blessings, curse, textColor);
    }
    /**
     * Constructor for Aethon god
     *
     * @param domains   The domains this god has influence over
     * @param blessings The blessings this god can bestow
     * @param textColor The color associated with this god's text
     */


    /**
     * Handles when items are offered to Aethon
     * Accepts amethyst shards, glowstone dust, and ender eyes as offerings
     */
    @Override
    public boolean onOfferedItems(BaseGodInstance instance, OfferEvent.OfferItemEvent event) {
        ItemStack offeredItem = event.getOffer();
        if (offeredItem.is(Items.AMETHYST_SHARD) ||
                offeredItem.is(Items.GLOWSTONE_DUST) ||
                offeredItem.is(Items.ENDER_EYE)) {
            GodHelper.increaseFavor(instance, event.getOfferer(), offeredItem.getCount() + event.getOfferer().level().getRandom().nextInt(3));
            return true;
        }
        return false;
    }

    /**
     * Handles when entities are offered to Aethon
     * Accepts phantoms and endermen as offerings
     */
    @Override
    public boolean onOfferedEntity(BaseGodInstance instance, OfferEvent.OfferEntityEvent event) {
        Entity offered = event.getOffer();
        if (offered instanceof Phantom || offered instanceof EnderMan) {
            GodHelper.increaseFavor(instance, event.getOfferer(), 5);
            return true;
        }
        return false;
    }

    /**
     * Handles when players pray to Aethon
     * Grants favor only during nighttime
     */
    @Override
    public void onPrayedTo(BaseGodInstance instance, PrayEvent event) {
        Level level = event.getLiving().level();
        if (!level.isDay()) {
            GodHelper.increaseFavor(instance, event.getLiving(), 1);
        }
        super.onPrayedTo(instance, event);
    }
}