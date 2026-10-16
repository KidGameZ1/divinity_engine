package net.nightshade.divinity_engine.api.innerworld;

import net.minecraft.resources.ResourceLocation;
import net.nightshade.divinity_engine.util.divinity.innerworld.InnerWorldsRegistry;
import java.util.*;
import java.util.function.Supplier;

/** Submit during your mod constructor, just like the trial/narrative deferred registers. */
public final class InnerWorldsDeferredRegister {
    private final String namespace;
    private final List<Entry> entries=new ArrayList<>();
    private boolean submitted;
    public InnerWorldsDeferredRegister(String namespace){this.namespace=namespace;}
    public Entry register(String path,Supplier<InnerWorldDefinition> supplier){
        if(submitted) throw new IllegalStateException("Register entries before submitting "+namespace);
        var entry=new Entry(new ResourceLocation(namespace,path),supplier);entries.add(entry);return entry;
    }
    public void register(){if(!submitted){submitted=true;InnerWorldsRegistry.add(this);}}
    public void apply(){for(var entry:entries){var value=entry.supplier.get();if(!entry.id.equals(value.id))throw new IllegalArgumentException(entry.id+": definition ID mismatch");InnerWorldsRegistry.register(value);entry.value=value;}}
    public static final class Entry implements Supplier<InnerWorldDefinition>{
        private final ResourceLocation id;private final Supplier<InnerWorldDefinition> supplier;private InnerWorldDefinition value;
        private Entry(ResourceLocation id,Supplier<InnerWorldDefinition> supplier){this.id=id;this.supplier=supplier;}
        public ResourceLocation id(){return id;}
        @Override public InnerWorldDefinition get(){if(value==null)throw new IllegalStateException(id+" has not been registered yet");return value;}
    }
}
