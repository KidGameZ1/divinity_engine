package net.nightshade.divinity_engine.divinity.gods.nashara;

import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraftforge.registries.RegistryObject;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.curse.Curse;
import net.nightshade.divinity_engine.divinity.gods.BaseGod;
import net.nightshade.divinity_engine.divinity.gods.BaseGodInstance;
import net.nightshade.divinity_engine.network.events.divinity.gods.worship_events.WhileNearStatueEvent;
import net.nightshade.divinity_engine.network.events.divinity.gods.worship_events.OfferEvent;
import net.nightshade.divinity_engine.util.divinity.gods.GodHelper;
import net.nightshade.nightshade_core.util.MiscHelper;

import java.awt.*;
import java.util.Set;

public class Nashara extends BaseGod {
    /**
     * Creates a new god with specified domains, blessings and text color.
     * Automatically registers all domains with the GodsDomainRegistry.
     *
     * @param domains   The set of domain names this god controls
     * @param blessings The set of blessings this god can grant
     * @param curse
     * @param textColor The color used for god-related text in UI
     */
    public Nashara(Set<String> domains, Set<RegistryObject<Blessings>> blessings, Curse curse, Color textColor) {
        super(domains, blessings, curse, textColor);
    }
    /**
     * Creates a new Nashara god instance
     *
     * @param domains   Set of domains this god governs
     * @param blessings Set of blessings this god can bestow
     * @param textColor Color used for text representation of this god
     */


    /**
     * Handles effects when a living entity is near Nashara's statue.
     * If the entity has poison effect, removes it and grants favor based on remaining duration
     *
     * @param instance The god instance handling the statue effect
     * @param event    The event containing details about the entity near statue
     */
    @Override
    public void whileNearShrine(BaseGodInstance instance, WhileNearStatueEvent event) {
        LivingEntity living = event.getLiving();
        if (living.hasEffect(MobEffects.POISON)) {
            GodHelper.increaseFavor(instance, event.getLiving(), 3 + MiscHelper.tickToSeconds(living.getEffect(MobEffects.POISON).getDuration() / 2));
            living.removeEffect(MobEffects.POISON);
        }
        super.whileNearShrine(instance, event);
    }

    /**
     * Handles item offerings to Nashara.
     * Accepts spider eyes and poison potions as favorable offerings
     *
     * @param instance The god instance receiving the offering
     * @param event    The offering event containing offered item details
     * @return true if the offering was accepted, false otherwise
     */
    @Override
    public boolean onOfferedItems(BaseGodInstance instance, OfferEvent.OfferItemEvent event) {
        ItemStack stack = event.getOffer();
        if (stack.is(Items.SPIDER_EYE) || stack.is(Items.FERMENTED_SPIDER_EYE)) {
            GodHelper.increaseFavor(instance, event.getOfferer(), stack.getCount() + 3);
            return true;
        } else if (stack.getItem() == (PotionUtils.setPotion(Items.POTION.getDefaultInstance(), Potions.POISON)).getItem()) {
            GodHelper.increaseFavor(instance, event.getOfferer(), stack.getCount() * 2);
            return true;
        }
        return super.onOfferedItems(instance, event);
    }

    /**
     * Handles entity offerings to Nashara.
     * Accepts animal sacrifices
     *
     * @param instance The god instance receiving the offering
     * @param event    The offering event containing offered entity details
     * @return true if the offering was accepted, false otherwise
     */
    @Override
    public boolean onOfferedEntity(BaseGodInstance instance, OfferEvent.OfferEntityEvent event) {
        Entity entity = event.getOffer();
        if (entity instanceof Animal) {
            GodHelper.increaseFavor(instance, event.getOfferer(), 3);
            return true;
        }
        return super.onOfferedEntity(instance, event);
    }

}
