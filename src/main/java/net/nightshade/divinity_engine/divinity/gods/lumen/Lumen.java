package net.nightshade.divinity_engine.divinity.gods.lumen;

import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.CandleBlock;
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
 * Represents the God of Light and Purification
 * Accepts offerings related to light sources and purifies zombie villagers
 * Favors those who stay in well-lit areas or during daylight without armor
 */
public class Lumen extends BaseGod {


    /**
     * Creates a new god with specified domains, blessings and text color.
     * Automatically registers all domains with the GodsDomainRegistry.
     *
     * @param domains   The set of domain names this god controls
     * @param blessings The set of blessings this god can grant
     * @param curse
     * @param textColor The color used for god-related text in UI
     */
    public Lumen(Set<String> domains, Set<RegistryObject<Blessings>> blessings, Curse curse, Color textColor) {
        super(domains, blessings, curse, textColor);
    }

    /**
     * Handles item offerings to Lumen
     * Accepts glowstone dust, blaze powder, and candle blocks
     *
     * @param instance The god instance receiving the offering
     * @param event    The offering event containing the offered item
     * @return true if the offering was accepted, false otherwise
     */
    @Override
    public boolean onOfferedItems(BaseGodInstance instance, OfferEvent.OfferItemEvent event) {
        if (event.getOffer().is(Items.GLOWSTONE_DUST) ||
                event.getOffer().is(Items.BLAZE_POWDER) ||
                event.getOffer().getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof CandleBlock) {
            int favorAmount = new Random().nextInt(4, 6 + 1);
            GodHelper.increaseFavor(instance, event.getOfferer(), favorAmount);
            return true;
        }
        return super.onOfferedItems(instance, event);
    }

    /**
     * Handles entity offerings to Lumen
     * Specifically handles zombie villager purification
     *
     * @param instance The god instance receiving the offering
     * @param event    The offering event containing the offered entity
     * @return true if the offering was accepted, false otherwise
     */
    @Override
    public boolean onOfferedEntity(BaseGodInstance instance, OfferEvent.OfferEntityEvent event) {
        if ((event.getOffer() instanceof ZombieVillager zombieVillager)) {
            zombieVillager.startConverting(event.getOffer().getUUID(), 1);
            GodHelper.increaseFavor(instance, event.getOfferer(), 5);
            return true;
        }
        return super.onOfferedEntity(instance, event);
    }

    /**
     * Handles effects while entities are near Lumen's statue
     * Grants favor for being in well-lit areas or in daylight without armor
     *
     * @param instance The god instance
     * @param event    The statue proximity event
     */
    @Override
    public void whileNearShrine(BaseGodInstance instance, WhileNearStatueEvent event) {
        // Grant favor if in well-lit area (light level >= 12) or during day without armor
        if (event.getLiving().level().getBrightness(LightLayer.BLOCK, event.getLiving().blockPosition()) >= 12 || event.getLiving().level().isDay() && event.getLiving().getArmorValue() == 0) {
            GodHelper.increaseFavor(instance, event.getLiving(), 3);
        }
        super.whileNearShrine(instance, event);
    }
}
