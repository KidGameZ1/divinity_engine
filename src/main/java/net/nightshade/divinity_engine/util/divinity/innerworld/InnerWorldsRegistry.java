package net.nightshade.divinity_engine.util.divinity.innerworld;

import net.minecraft.resources.ResourceLocation;
import net.nightshade.divinity_engine.api.innerworld.*;
import net.nightshade.divinity_engine.registry.divinity.gods.GodsRegistry;
import java.util.*;

public final class InnerWorldsRegistry {
    private static final Map<ResourceLocation,InnerWorldDefinition> DEFINITIONS=new LinkedHashMap<>();
    private static final List<InnerWorldsDeferredRegister> DEFERRED=new ArrayList<>();
    private static boolean initialized;
    public static void add(InnerWorldsDeferredRegister register){if(initialized)register.apply();else DEFERRED.add(register);}
    public static void register(InnerWorldDefinition value){
        if(!GodsRegistry.GODS_REGISTRY.get().containsKey(value.god))throw new IllegalArgumentException(value.id+": unknown god "+value.god);
        if(DEFINITIONS.containsKey(value.god)||DEFINITIONS.values().stream().anyMatch(d->d.id.equals(value.id)))throw new IllegalArgumentException(value.id+": duplicate Inner World/god "+value.god);
        validateVisuals(value.id,value.visuals);for(var variant:value.variants)validateVisuals(value.id,variant.visuals());
        DEFINITIONS.put(value.god,value);
    }
    private static void validateVisuals(ResourceLocation id,InnerWorldDefinition.Visuals v){
        if(!net.minecraftforge.registries.ForgeRegistries.SOUND_EVENTS.containsKey(v.sound()))throw new IllegalArgumentException(id+": missing ambience "+v.sound());
        if(!(net.minecraftforge.registries.ForgeRegistries.PARTICLE_TYPES.getValue(v.particle()) instanceof net.minecraft.core.particles.SimpleParticleType))throw new IllegalArgumentException(id+": particle must be a registered simple particle: "+v.particle());
    }
    public static InnerWorldDefinition get(ResourceLocation god){return DEFINITIONS.get(god);}
    public static Collection<InnerWorldDefinition> values(){return Collections.unmodifiableCollection(DEFINITIONS.values());}
    public static void initialize(){if(initialized)return;BuiltinInnerWorlds.register();for(var r:DEFERRED)r.apply();initialized=true;}
    private InnerWorldsRegistry(){}
}
