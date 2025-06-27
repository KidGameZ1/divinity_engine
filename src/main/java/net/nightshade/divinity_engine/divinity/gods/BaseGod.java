package net.nightshade.divinity_engine.divinity.gods;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryObject;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.network.events.divinity.gods.ContactGodEvent;
import net.nightshade.divinity_engine.network.events.divinity.gods.LoseFaithEvent;
import net.nightshade.divinity_engine.network.events.divinity.gods.worship_events.OfferEvent;
import net.nightshade.divinity_engine.network.events.divinity.gods.worship_events.PrayEvent;
import net.nightshade.divinity_engine.registry.divinity.gods.GodsRegistry;
import net.nightshade.divinity_engine.registry.divinity.gods.domains.GodsDomainRegistry;

import javax.annotation.Nullable;
import java.awt.*;
import java.util.List;
import java.util.Set;

public class BaseGod {

    private final Set<String> domains;

    private final Set<RegistryObject<Blessings>> blessings;

    public BaseGod(Set<String> domains, Set<RegistryObject<Blessings>> blessings) {
        this.domains = domains;
        for (int i = 0; i < domains.size(); i++) {
            GodsDomainRegistry.registerDomain(domains.stream().toList().get(i));
        }
        this.blessings = blessings;
    }

    public Set<String> getDomains() {
        return domains;
    }

    public Set<RegistryObject<Blessings>> getBlessings() {
        return blessings;
    }

    public void onPrayedTo(BaseGodInstance instance, PrayEvent event) {}

    public void onOfferedItems(BaseGodInstance instance, OfferEvent.OfferItemEvent event) {}

    public void onOfferedEntity(BaseGodInstance instance, OfferEvent.OfferEntityEvent event) {}

    public void onContacted(LivingEntity living, ContactGodEvent event) {}

    public void onLoseFaith(LivingEntity living, LoseFaithEvent event) {}

    public <T extends BaseGodInstance> T createGodDefaultInstance() {
        BaseGodInstance baseGodInstance = new BaseGodInstance(this);
        // Initialize blessings
        return (T) baseGodInstance;
    }


    @Nullable
    public ResourceLocation getRegistryName() {
        return ((IForgeRegistry) GodsRegistry.GODS_REGISTRY.get()).getKey(this);
    }

    @Nullable
    public MutableComponent getName() {
        ResourceLocation id = this.getRegistryName();
        return id == null ? null : Component.translatable(String.format("%s.gods.%s", id.getNamespace(), id.getPath().replace('/', '.')));
    }

    public String getNameTranslationKey() {
        return ((TranslatableContents)this.getName().getContents()).getKey();
    }

    public String getContactMessageTranslationKey() {
        return String.format("%s.gods.%s.contact", this.getRegistryName().getNamespace(), this.getRegistryName().getPath().replace('/', '.'));
    }

    public String getLossContactMessageTranslationKey() {
        return String.format("%s.gods.%s.loss", this.getRegistryName().getNamespace(), this.getRegistryName().getPath().replace('/', '.'));
    }

    public Color getColor() {
        return Color.WHITE;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass() == o.getClass()) {
            BaseGod gods = (BaseGod) o;
            return this.getRegistryName().equals(gods.getRegistryName());
        } else {
            return false;
        }
    }
}
