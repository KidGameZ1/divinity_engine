package net.nightshade.divinity_engine.divinity.gods;

public enum FavorTiers {
    Enemy("enemy",-100,-51),
    Displeased("displeased",-50,-1),
    Neutral("neutral",0,0),
    Favorable("favorable",1,50),
    Devoted("devoted",51,80),
    Champion("champion",81,100);


    private final String id;

    private final int minFavor;

    private final int maxFavor;

    FavorTiers(String id, int minFavor, int maxFavor) {
        this.id = id;
        this.minFavor = minFavor;
        this.maxFavor = maxFavor;
    }

    public String getId() {
        return this.id;
    }

    public static FavorTiers getById(String id) {
        for(FavorTiers favorTier : values()) {
            if(favorTier.getId().equals(id)) {
                return favorTier;
            }
        }
        return null;
    }

    public int getMinFavor() {
        return this.minFavor;
    }

    public int getMaxFavor() {
        return this.maxFavor;
    }
}
