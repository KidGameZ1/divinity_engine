package net.nightshade.divinity_engine.api.narrative;

import net.minecraft.resources.ResourceLocation;
import java.util.function.Supplier;

public final class DivineNarrativeRegistryObject<T> implements Supplier<T> {
    private final ResourceLocation id;
    private T value;
    DivineNarrativeRegistryObject(ResourceLocation id) { this.id = id; }
    void bind(T value) { this.value = value; }
    public ResourceLocation getId() { return id; }
    @Override public T get() { if (value == null) throw new IllegalStateException("Definition not registered: " + id); return value; }
}
