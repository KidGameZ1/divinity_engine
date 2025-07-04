package net.nightshade.divinity_engine.divinity.gods.kairoth;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.RegistryObject;

import java.util.UUID;

import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.curse.Curse;
import net.nightshade.divinity_engine.divinity.gods.BaseGod;
import net.nightshade.divinity_engine.divinity.gods.BaseGodInstance;
import net.nightshade.divinity_engine.network.events.divinity.gods.worship_events.OfferEvent;
import net.nightshade.divinity_engine.network.events.divinity.gods.worship_events.WhileNearStatueEvent;
import net.nightshade.divinity_engine.util.divinity.gods.GodHelper;

import java.awt.*;
import java.util.Set;

/**
 * Represents Kairoth, a god associated with time and dimensional travel.
 * This god favors offerings related to time (clocks) and dimensional items (ender pearls, chorus fruit).
 */
public class Kairoth extends BaseGod {
    /**
     * Creates a new god with specified domains, blessings and text color.
     * Automatically registers all domains with the GodsDomainRegistry.
     *
     * @param domains   The set of domain names this god controls
     * @param blessings The set of blessings this god can grant
     * @param curse
     * @param textColor The color used for god-related text in UI
     */
    public Kairoth(Set<String> domains, Set<RegistryObject<Blessings>> blessings, Curse curse, Color textColor) {
        super(domains, blessings, curse, textColor);
    }

    /**
     * Creates a new instance of Kairoth god.
     *
     * @param domains   Set of domains this god controls
     * @param blessings Set of blessings this god can bestow
     * @param textColor Color used for text related to this god
     */


    /**
     * Creates a default instance of this god with initialized statue wait times tracking.
     *
     * @return A new instance of the god with default settings
     */
    @Override
    public <T extends BaseGodInstance> T createGodDefaultInstance() {
        T instance = super.createGodDefaultInstance();
        if (!instance.getOrCreateTag().contains("statueWaitStartTimes"))
            instance.getOrCreateTag().put("statueWaitStartTimes", new ListTag());
        return instance;
    }

    /**
     * Handles items offered to this god, with special favor for time-related and dimensional items.
     *
     * @param instance The god instance receiving the offering
     * @param event    The offering event containing the offered item
     * @return true if the offering was accepted, false otherwise
     */
    @Override
    public boolean onOfferedItems(BaseGodInstance instance, OfferEvent.OfferItemEvent event) {
        ItemStack stack = event.getOffer();
        if (stack.is(Items.CLOCK) || stack.is(Items.ENDER_PEARL) || stack.is(Items.CHORUS_FRUIT)) {
            GodHelper.increaseFavor(instance, event.getOfferer(), stack.getCount()+5);
            return true;
        }
        return super.onOfferedItems(instance, event);
    }

    /**
     * Handles effects and rewards while players are near this god's statue.
     * Grants favor for holding time-related items and for extended statue visits.
     *
     * @param instance The god instance associated with the statue
     * @param event    The event triggered while near statue
     */
    @Override
    public void whileNearShrine(BaseGodInstance instance, WhileNearStatueEvent event) {
        if (event.getLiving().getMainHandItem().is(Items.CLOCK) || event.getLiving().getOffhandItem().is(Items.CLOCK)) {
            GodHelper.increaseFavor(instance, event.getLiving(), 2);
        } else if (event.getLiving().getMainHandItem().is(Items.COMPASS) || event.getLiving().getOffhandItem().is(Items.COMPASS)) {
            GodHelper.increaseFavor(instance, event.getLiving(), 2);
        }

        UUID playerUUID = event.getLiving().getUUID();
        long currentTime = event.getLiving().level().getDayTime();
        ListTag statueWaitTimes = instance.getOrCreateTag().getList("statueWaitStartTimes", 10);

        boolean found = false;
        for (int i = 0; i < statueWaitTimes.size(); i++) {
            if (statueWaitTimes.getCompound(i).getUUID("uuid").equals(playerUUID)) {
                found = true;
                long startTime = statueWaitTimes.getCompound(i).getLong("time");
                if (currentTime - startTime >= 24000) {
                    GodHelper.increaseFavor(instance, event.getLiving(), 50);
                    statueWaitTimes.remove(i);
                }
                break;
            }
        }

        if (!found) {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("uuid", playerUUID);
            tag.putLong("time", currentTime);
            statueWaitTimes.add(tag);
        }

        super.whileNearShrine(instance, event);
    }
}