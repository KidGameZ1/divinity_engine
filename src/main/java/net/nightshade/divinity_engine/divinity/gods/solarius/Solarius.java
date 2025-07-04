package net.nightshade.divinity_engine.divinity.gods.solarius;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
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
 * Represents Solarius, the god of sun and light.
 * Accepts offerings related to light and gold, and rewards praying during daylight while unarmored.
 */
public class Solarius extends BaseGod {


    /**
     * Creates a new god with specified domains, blessings and text color.
     * Automatically registers all domains with the GodsDomainRegistry.
     *
     * @param domains   The set of domain names this god controls
     * @param blessings The set of blessings this god can grant
     * @param curse
     * @param textColor The color used for god-related text in UI
     */
    public Solarius(Set<String> domains, Set<RegistryObject<Blessings>> blessings, Curse curse, Color textColor) {
        super(domains, blessings, curse, textColor);
    }

    /**
     * Handles prayer events for Solarius.
     * Grants 3 favor points if praying during day for at least 100 ticks while wearing no armor.
     */
    @Override
    public void onPrayedTo(BaseGodInstance instance, PrayEvent event) {
        LivingEntity living = event.getLiving();
        if (living.level().isDay()) {
            if (event.getDuration() >= 100) {
                if (living.getItemBySlot(EquipmentSlot.HEAD).isEmpty() && living.getItemBySlot(EquipmentSlot.CHEST).isEmpty() && living.getItemBySlot(EquipmentSlot.LEGS).isEmpty() && living.getItemBySlot(EquipmentSlot.FEET).isEmpty())
                    GodHelper.increaseFavor(instance, event.getLiving(), 3);
            }
        }
        super.onPrayedTo(instance, event);
    }

    /**
     * Handles item offerings to Solarius.
     * Accepts:
     * - Gold ingots and sunflowers (1 favor per item)
     * - Glowstone dust (3 favor per item)
     * - Glowstone blocks (5 favor per item)
     */
    @Override
    public boolean onOfferedItems(BaseGodInstance instance, OfferEvent.OfferItemEvent event) {
        ItemStack stack = event.getOffer();
        if (stack.is(Items.GOLD_INGOT) || stack.is(Items.SUNFLOWER)){
            GodHelper.increaseFavor(instance, event.getOfferer(), stack.getCount());
            return true;
        }else if (stack.is(Items.GLOWSTONE) || stack.is(Items.GLOWSTONE_DUST)){
            if (stack.is(Items.GLOWSTONE_DUST)){
                GodHelper.increaseFavor(instance, event.getOfferer(), stack.getCount()*3);
            }else {
                GodHelper.increaseFavor(instance, event.getOfferer(), stack.getCount()*5);
            }
            return true;
        }
        return super.onOfferedItems(instance, event);
    }

    /**
     * Handles entity offerings to Solarius.
     * Grants 10 favor points when offering undead entities during daytime.
     */
    @Override
    public boolean onOfferedEntity(BaseGodInstance instance, OfferEvent.OfferEntityEvent event) {
        Entity entity = event.getOffer();
        if (entity instanceof LivingEntity living){
            if (living.getMobType() == MobType.UNDEAD){
                if (living.level().isDay()){
                    GodHelper.increaseFavor(instance, event.getOfferer(), 10);
                    return true;
                }
            }
        }
        return super.onOfferedEntity(instance, event);
    }
}
