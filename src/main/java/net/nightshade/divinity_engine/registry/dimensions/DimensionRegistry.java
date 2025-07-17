package net.nightshade.divinity_engine.registry.dimensions;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.nightshade.divinity_engine.DivinityEngineMod;

public class DimensionRegistry {
    public static final ResourceKey<Level> POCKET_DIMENSION = ResourceKey.create(
            ResourceKey.createRegistryKey(new ResourceLocation("minecraft", "dimension")),
            new ResourceLocation(DivinityEngineMod.MODID, "pocket_dimension")
    );
}
