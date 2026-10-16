package net.nightshade.divinity_engine.util.divinity.effects;

import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Register common-side definitions during enqueued setup; client mesh authors register separately. */
public final class BlessingVisualDesigns {
    private static final Map<ResourceLocation, BlessingVisualDesign> DESIGNS = new LinkedHashMap<>();
    static {
        CelestialBlessingDesigns.register();
        ElementalBlessingDesigns.register();
        EarthSpiritBlessingDesigns.register();
    }

    private BlessingVisualDesigns() { }

    public static synchronized void register(ResourceLocation blessing, BlessingVisualDesign design) {
        if (DESIGNS.putIfAbsent(Objects.requireNonNull(blessing), Objects.requireNonNull(design)) != null)
            throw new IllegalArgumentException("Duplicate blessing visual design: " + blessing);
    }

    public static BlessingVisualDesign get(ResourceLocation blessing) { return DESIGNS.get(blessing); }
    public static Map<ResourceLocation, BlessingVisualDesign> all() { return Map.copyOf(DESIGNS); }
}
