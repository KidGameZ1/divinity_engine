package net.nightshade.divinity_engine.core.mixin;

import net.minecraftforge.registries.RegistryBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({RegistryBuilder.class})
public interface RegistryBuilderAccessor {
    @Accessor(
        remap = false
    )
    void setHasWrapper(boolean var1);
}
