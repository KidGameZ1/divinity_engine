package net.nightshade.divinity_engine.divinity.gods;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;

/**
 * Represents different tiers of favor between entities and gods.
 * Each tier has an identifier and a specific range of favor values.
 */
public enum FavorTiers {
    /**
     * Represents hostile relationship with favor range -250 to -101
     */
    Enemy("enemy", -250, -101),
    /**
     * Represents unfavorable relationship with favor range -50 to -1
     */
    Displeased("displeased", -100, -1),
    /**
     * Represents neutral relationship with favor value 0
     */
    Neutral("neutral", 0, 0),
    /**
     * Represents positive relationship with favor range 1 to 50
     */
    Favorable("favorable", 1, 100),
    /**
     * Represents strong positive relationship with favor range 51 to 80
     */
    Devoted("devoted", 101, 175),
    /**
     * Represents highest positive relationship with favor range 81 to 100
     */
    Champion("champion",176,250);


    private final String id;

    private final int minFavor;

    private final int maxFavor;

    /**
     * Constructs a FavorTier with specified parameters.
     *
     * @param id       The identifier string for the favor tier
     * @param minFavor The minimum favor value for this tier
     * @param maxFavor The maximum favor value for this tier
     */
    FavorTiers(String id, int minFavor, int maxFavor) {
        this.id = id;
        this.minFavor = minFavor;
        this.maxFavor = maxFavor;
    }

    /**
     * Gets the identifier of this favor tier.
     *
     * @return The string identifier of the favor tier
     */
    public String getId() {
        return this.id;
    }

    /**
     * Finds a FavorTiers enum constant by its identifier.
     *
     * @param id The identifier to search for
     * @return The matching FavorTiers constant, or null if not found
     */
    public static FavorTiers getById(String id) {
        for(FavorTiers favorTier : values()) {
            if(favorTier.getId().equals(id)) {
                return favorTier;
            }
        }
        return null;
    }

    /**
     * Finds the FavorTiers enum constant that matches a given favor value.
     * Searches through all tiers to find one where the favor value falls within its range.
     *
     * @param favor The favor value to find a matching tier for
     * @return The matching FavorTiers constant, or null if no tier contains the given favor value
     */
    public static FavorTiers getByFavor(int favor) {
        for (FavorTiers favorTier : values()) {
            if (favorTier.getMinFavor() <= favor && favor <= favorTier.getMaxFavor()) {
                return favorTier;
            }
        }
        return null;
    }

    public MutableComponent getName(){
        return Component.translatable(String.format("divinity_engine.gods.favor_tier.%s", this.getId()));
    }
    public MutableComponent getName(int favor){
        return Component.translatable(String.format("divinity_engine.gods.favor_tier."+getByFavor(favor).id));
    }
    public MutableComponent getName(String id){
        return Component.translatable(String.format("divinity_engine.gods.favor_tier."+getById(id).id));
    }
    public String getNameTranslationKey() {
        return ((TranslatableContents)this.getName().getContents()).getKey();
    }
    public String getNameTranslationKey(int favor) {
        return ((TranslatableContents)this.getName(favor).getContents()).getKey();
    }
    public String getNameTranslationKey(String id) {
        return ((TranslatableContents)this.getName(id).getContents()).getKey();
    }
    /**
     * Gets the minimum favor value for this tier.
     *
     * @return The minimum favor value
     */
    public int getMinFavor() {
        return this.minFavor;
    }

    /**
     * Gets the maximum favor value for this tier.
     *
     * @return The maximum favor value
     */
    public int getMaxFavor() {
        return this.maxFavor;
    }
}
