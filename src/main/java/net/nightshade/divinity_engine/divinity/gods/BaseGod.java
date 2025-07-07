package net.nightshade.divinity_engine.divinity.gods;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryObject;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.curse.Curse;
import net.nightshade.divinity_engine.network.events.divinity.gods.ContactGodEvent;
import net.nightshade.divinity_engine.network.events.divinity.gods.LoseFaithEvent;
import net.nightshade.divinity_engine.network.events.divinity.gods.worship_events.WhileNearStatueEvent;
import net.nightshade.divinity_engine.network.events.divinity.gods.worship_events.OfferEvent;
import net.nightshade.divinity_engine.network.events.divinity.gods.worship_events.PrayEvent;
import net.nightshade.divinity_engine.registry.divinity.gods.GodsRegistry;
import net.nightshade.divinity_engine.registry.divinity.gods.domains.GodsDomainRegistry;
import net.nightshade.divinity_engine.util.divinity.gods.GodHelper;

import javax.annotation.Nullable;
import java.awt.*;
import java.util.Set;

/**
 * Base class for all gods in the divinity system.
 * Provides core functionality for handling domains, blessings, and divine interactions.
 */
public class BaseGod {

    /**
     * The domains (spheres of influence) this god has authority over
     */
    private final Set<String> domains;
    /**
     * The blessings this god can bestow upon followers
     */
    private final Set<RegistryObject<Blessings>> blessings;
    /**
     * The color used for text related to this god in the UI
     */
    private final Color textColor;

    private final @Nullable Curse curse;
    private Block statueBlock;
    private BlockEntityType statueBlockEntity;

    public Block getStatueBlock() {
        return statueBlock;
    }

    public void setStatueBlock(Block statueBlock) {
        this.statueBlock = statueBlock;
    }

    public BlockEntityType getStatueBlockEntity() {
        return statueBlockEntity;
    }

    public void setStatueBlockEntity(BlockEntityType statueBlockEntity) {
        this.statueBlockEntity = statueBlockEntity;
    }

    /**
     * Creates a new god with specified domains, blessings and text color.
     * Automatically registers all domains with the GodsDomainRegistry.
     *
     * @param domains   The set of domain names this god controls
     * @param blessings The set of blessings this god can grant
     * @param textColor The color used for god-related text in UI
     */
    public BaseGod(Set<String> domains, Set<RegistryObject<Blessings>> blessings, @Nullable Curse curse, Color textColor) {
        this.domains = domains;
        this.textColor = textColor;
        for (int i = 0; i < domains.size(); i++) {
            GodsDomainRegistry.registerDomain(domains.stream().toList().get(i));
        }
        this.blessings = blessings;
        this.curse = curse;
    }
    public BaseGod(Set<String> domains, Set<RegistryObject<Blessings>> blessings, Color textColor) {
        this.domains = domains;
        this.textColor = textColor;
        for (int i = 0; i < domains.size(); i++) {
            GodsDomainRegistry.registerDomain(domains.stream().toList().get(i));
        }
        this.blessings = blessings;
        this.curse = null;
    }

    public Set<String> getDomains() {
        return domains;
    }

    public Set<RegistryObject<Blessings>> getBlessings() {
        return blessings;
    }

    public Curse getCurse() {
        return curse;
    }

    /**
     * Called when a player prays to this god
     */
    public void onPrayedTo(BaseGodInstance instance, PrayEvent event) {
        GodHelper.increaseFavor(instance,event.getLiving(), 1);
    }

    /**
     * Called when items are offered to this god. Returns true if offering is accepted
     */
    public boolean onOfferedItems(BaseGodInstance instance, OfferEvent.OfferItemEvent event) {
        return false;
    }

    /**
     * Called when an entity is offered to this god. Returns true if offering is accepted
     */
    public boolean onOfferedEntity(BaseGodInstance instance, OfferEvent.OfferEntityEvent event) {
        return false;
    }

    /**
     * Called periodically while a player is near this god's statue
     */
    public void whileNearShrine(BaseGodInstance instance, WhileNearStatueEvent event) {
    }

    /**
     * Called when a player first makes contact with this god
     */
    public void onContacted(LivingEntity living, ContactGodEvent event) {
    }

    /**
     * Called when a player loses faith in this god
     */
    public void onLoseFaith(LivingEntity living, LoseFaithEvent event) {}

    /**
     * Creates a new instance of this god's handler class.
     * @return A new BaseGodInstance for this god
     */
    public <T extends BaseGodInstance> T createGodDefaultInstance() {
        BaseGodInstance baseGodInstance = new BaseGodInstance(this);
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
    public String getCurseMessageTranslationKey() {
        return String.format("%s.gods.%s.curse", this.getRegistryName().getNamespace(), this.getRegistryName().getPath().replace('/', '.'));
    }
    public String getUncurseMessageTranslationKey() {
        return String.format("%s.gods.%s.uncurse", this.getRegistryName().getNamespace(), this.getRegistryName().getPath().replace('/', '.'));
    }

    public Color getColor() {
        return textColor;
    }

    /**
     * Checks if another god is equal to this one based on registry name.
     * @param o The object to compare with
     * @return true if the objects are the same god
     */
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
