package net.nightshade.divinity_engine.divinity.gods.nythea;

import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.RegistryObject;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.curse.Curse;
import net.nightshade.divinity_engine.divinity.gods.BaseGod;
import net.nightshade.divinity_engine.divinity.gods.BaseGodInstance;
import net.nightshade.divinity_engine.network.events.divinity.gods.worship_events.OfferEvent;
import net.nightshade.divinity_engine.network.events.divinity.gods.worship_events.WhileNearStatueEvent;
import net.nightshade.divinity_engine.util.divinity.gods.GodHelper;
import net.nightshade.nightshade_core.util.MiscHelper;

import java.awt.*;
import java.util.Objects;
import java.util.Set;

/**
 * Represents Nythea, a god associated with night, dreams and illusions.
 * Accepts offerings of phantom membrane, amethyst shards and glow ink sacs.
 * Grants favor for sleeping near statues and consuming invisibility effects.
 */
public class Nythea extends BaseGod {
    /**
     * Creates a new god with specified domains, blessings and text color.
     * Automatically registers all domains with the GodsDomainRegistry.
     *
     * @param domains   The set of domain names this god controls
     * @param blessings The set of blessings this god can grant
     * @param curse
     * @param textColor The color used for god-related text in UI
     */
    public Nythea(Set<String> domains, Set<RegistryObject<Blessings>> blessings, Curse curse, Color textColor) {
        super(domains, blessings, curse, textColor);
    }
    /**
     * Creates a new instance of Nythea god.
     *
     * @param domains   Set of domains this god has influence over
     * @param blessings Set of blessings this god can grant
     * @param textColor Color used for text associated with this god
     */


    /**
     * Handles item offerings to Nythea.
     * Accepts phantom membrane, amethyst shards and glow ink sacs.
     * Grants favor equal to item count plus 5.
     *
     * @param instance The god instance receiving the offering
     * @param event    The offering event containing offered item
     * @return true if offering was accepted, false otherwise
     */
    @Override
    public boolean onOfferedItems(BaseGodInstance instance, OfferEvent.OfferItemEvent event) {
        ItemStack stack = event.getOffer();
        if (stack.is(Items.PHANTOM_MEMBRANE) || stack.is(Items.AMETHYST_SHARD) || stack.is(Items.GLOW_INK_SAC)){
            GodHelper.increaseFavor(instance, event.getOfferer(), stack.getCount()+5);
            return true;
        }
        return super.onOfferedItems(instance, event);
    }

    /**
     * Handles effects when entities are near Nythea's statue.
     * Grants favor when sleeping or consuming invisibility effects.
     *
     * @param instance The god instance associated with the statue
     * @param event    The event containing information about nearby entity
     */
    @Override
    public void whileNearShrine(BaseGodInstance instance, WhileNearStatueEvent event) {
        if (event.getLiving().isSleeping()){
            GodHelper.increaseFavor(instance, event.getLiving(), 2);
        }
        else if (event.getLiving().hasEffect(MobEffects.INVISIBILITY)) {
            GodHelper.increaseFavor(instance, event.getLiving(), 4+ MiscHelper.tickToSeconds(Objects.requireNonNull(event.getLiving().getEffect(MobEffects.INVISIBILITY)).getDuration()/2));
            event.getLiving().removeEffect(MobEffects.INVISIBILITY);
        }
        super.whileNearShrine(instance, event);
    }
}
