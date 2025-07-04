package net.nightshade.divinity_engine.divinity.gods.cryonis;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.RegistryObject;
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
 * Represents the Cryonis god of ice and frost.
 * Favors offerings of ice and snow blocks, and rewards those who endure freezing conditions.
 * Grants additional favor for offerings made in snowy environments.
 */
public class Cryonis extends BaseGod {
    /**
     * Creates a new god with specified domains, blessings and text color.
     * Automatically registers all domains with the GodsDomainRegistry.
     *
     * @param domains   The set of domain names this god controls
     * @param blessings The set of blessings this god can grant
     * @param curse
     * @param textColor The color used for god-related text in UI
     */
    public Cryonis(Set<String> domains, Set<RegistryObject<Blessings>> blessings, Curse curse, Color textColor) {
        super(domains, blessings, curse, textColor);
    }
    /**
     * Constructs a new Cryonis god instance.
     *
     * @param domains   Set of domain names this god has influence over
     * @param blessings Set of blessings this god can grant
     * @param textColor Color used for visual representation of this god
     */


    /**
     * Processes item offerings made to Cryonis.
     * Accepts blue ice, packed ice, and snow blocks as offerings.
     * Grants favor based on the quantity offered plus a random bonus.
     *
     * @param instance The god instance receiving the offering
     * @param event    The offering event containing the item
     * @return true if the offering was accepted, false otherwise
     */
    @Override
    public boolean onOfferedItems(BaseGodInstance instance, OfferEvent.OfferItemEvent event) {
        if (event.getOffer().is(Blocks.BLUE_ICE.asItem())
                || event.getOffer().is(Blocks.PACKED_ICE.asItem())
                || event.getOffer().is(Blocks.SNOW_BLOCK.asItem())) {
            GodHelper.increaseFavor(instance, event.getOfferer(), 4 + (int) (Math.random() * 3)+event.getOffer().getCount());
            return true;
        }
        return super.onOfferedItems(instance, event);
    }

    /**
     * Processes entity offerings made to Cryonis.
     * Grants additional favor when offerings are made while standing on snow or snow blocks.
     * Only accepts offerings from players.
     *
     * @param instance The god instance receiving the offering
     * @param event    The offering event containing the entity
     * @return true if the offering was accepted, false otherwise
     */
    @Override
    public boolean onOfferedEntity(BaseGodInstance instance, OfferEvent.OfferEntityEvent event) {
        LivingEntity living = event.getOfferer();
        Entity offer = event.getOffer();
        if (offer instanceof LivingEntity livingOffer) {
            if (living instanceof Player player) {
                if (livingOffer.level().getBlockState(player.getOnPos().below()).is(Blocks.SNOW_BLOCK) || livingOffer.level().getBlockState(player.getOnPos().below()).is(Blocks.SNOW)) {
                    if (living.level().getBlockState(player.getOnPos().below()).is(Blocks.SNOW_BLOCK) || living.level().getBlockState(player.getOnPos().below()).is(Blocks.SNOW)) {
                        GodHelper.increaseFavor(instance, event.getOfferer(), 10);
                        return true;
                    }
                }
            }
        }
        return super.onOfferedEntity(instance, event);
    }

    /**
     * Handles effects for entities near Cryonis's statue.
     * Grants favor to entities that are freezing while near the statue.
     *
     * @param instance The god instance whose statue is being approached
     * @param event    The event containing information about the nearby entity
     */
    @Override
    public void whileNearShrine(BaseGodInstance instance, WhileNearStatueEvent event) {

        if (event.getLiving().isFreezing()){
            GodHelper.increaseFavor(instance, event.getLiving(), 3);
        }

        super.whileNearShrine(instance, event);
    }
}
