package net.nightshade.divinity_engine.api.narrative;

import net.minecraft.resources.ResourceLocation;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.nightshade.divinity_engine.util.divinity.narrative.DivineNarrativeRegistry;

public final class DivineNarrativeDeferredRegister {
    private final String namespace;
    private final List<Runnable> entries = new ArrayList<>();
    private boolean applied;
    public DivineNarrativeDeferredRegister(String namespace) { this.namespace = namespace; }
    public DivineNarrativeRegistryObject<DivineStoryDefinition> story(String name, Supplier<DivineStoryDefinition> supplier) {
        checkMutable();
        var object = new DivineNarrativeRegistryObject<DivineStoryDefinition>(new ResourceLocation(namespace, name));
            entries.add(() -> { var value = supplier.get().withId(object.getId()); DivineNarrativeRegistry.registerStory(object.getId(), value); object.bind(value); });
        return object;
    }
    public DivineNarrativeRegistryObject<GodPresenceDefinition> presence(String name, Supplier<GodPresenceDefinition> supplier) {
        checkMutable();
        var object = new DivineNarrativeRegistryObject<GodPresenceDefinition>(new ResourceLocation(namespace, name));
        entries.add(() -> { var value = supplier.get(); DivineNarrativeRegistry.registerPresence(object.getId(), value); object.bind(value); });
        return object;
    }
    public void submit() { DivineNarrativeRegistry.addDeferredRegister(this); }
    public DivineNarrativeRegistryObject<GodRelationshipDefinition> relationship(String name, Supplier<GodRelationshipDefinition> supplier) {
        checkMutable(); var object = new DivineNarrativeRegistryObject<GodRelationshipDefinition>(new ResourceLocation(namespace, name));
        entries.add(() -> { var value = supplier.get(); DivineNarrativeRegistry.registerRelationship(object.getId(), value); object.bind(value); }); return object;
    }
    public void registerAll() { if (!applied) { applied = true; entries.forEach(Runnable::run); } }
    private void checkMutable() { if (applied) throw new IllegalStateException("Register already applied"); }
}
