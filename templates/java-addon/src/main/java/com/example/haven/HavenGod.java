package com.example.haven;

import java.util.Set;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.RegistryObject;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.gods.BaseGod;
import net.nightshade.divinity_engine.divinity.gods.BaseGodInstance;
import net.nightshade.divinity_engine.network.events.divinity.gods.worship_events.OfferEvent;
import net.nightshade.divinity_engine.network.events.divinity.gods.worship_events.PrayEvent;
import net.nightshade.divinity_engine.util.divinity.gods.GodHelper;

public final class HavenGod extends BaseGod {
    public HavenGod(Set<RegistryObject<Blessings>> blessings) {
        super(Set.of("Shelter", "Wayfinding"), blessings, HavenAddon.COLOR);
    }

    @Override
    public void onPrayedTo(BaseGodInstance instance, PrayEvent event) {
        if (!event.isCanceled() && !event.getLiving().level().isClientSide) {
            GodHelper.increaseFavor(instance, event.getLiving(), 1);
        }
    }

    @Override
    public boolean onOfferedItems(BaseGodInstance instance, OfferEvent.OfferItemEvent event) {
        if (event.isCanceled() || event.getOfferer().level().isClientSide || event.getOffer().isEmpty()) return false;
        int favor = event.getOffer().is(Items.AMETHYST_SHARD) ? 2 : event.getOffer().is(Items.BREAD) ? 1 : 0;
        if (favor == 0) return false;
        // Shared shrine processing owns consumption, worship budgets and story credit.
        GodHelper.increaseFavor(instance, event.getOfferer(), favor * event.getOffer().getCount());
        return true;
    }
}
