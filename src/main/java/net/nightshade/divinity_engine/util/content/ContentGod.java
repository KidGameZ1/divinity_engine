package net.nightshade.divinity_engine.util.content;

import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.gods.BaseGod;
import net.nightshade.divinity_engine.divinity.gods.BaseGodInstance;
import net.nightshade.divinity_engine.network.events.divinity.gods.worship_events.OfferEvent;
import net.nightshade.divinity_engine.network.events.divinity.gods.worship_events.PrayEvent;
import net.nightshade.divinity_engine.registry.divinity.blessing.BlessingsRegistry;
import net.nightshade.divinity_engine.util.divinity.gods.GodHelper;

import java.awt.Color;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;

public final class ContentGod extends BaseGod {
    private final ContentSchema.God definition;

    public ContentGod(ContentSchema.God definition) {
        super(definition.domains(), definition.blessings().stream()
                .map(id -> RegistryObject.<Blessings, Blessings>create(id, BlessingsRegistry.BLESSINGS_REGISTRY.get()))
                .collect(Collectors.toCollection(LinkedHashSet::new)), new Color(definition.color()));
        this.definition = definition;
    }

    @Override public void onPrayedTo(BaseGodInstance instance, PrayEvent event) {
        if (!event.getLiving().level().isClientSide && !event.isCanceled())
            GodHelper.increaseFavor(instance, event.getLiving(), definition.prayerFavor());
    }

    @Override public boolean onOfferedItems(BaseGodInstance instance, OfferEvent.OfferItemEvent event) {
        if (event.getOfferer().level().isClientSide || event.isCanceled() || event.getOffer().isEmpty()) return false;
        Integer value = definition.offerings().get(ForgeRegistries.ITEMS.getKey(event.getOffer().getItem()));
        if (value == null) return false;
        // The shared shrine offering processor owns consumption, budgets and story attribution.
        GodHelper.increaseFavor(instance, event.getOfferer(), value * event.getOffer().getCount());
        return true;
    }
}
