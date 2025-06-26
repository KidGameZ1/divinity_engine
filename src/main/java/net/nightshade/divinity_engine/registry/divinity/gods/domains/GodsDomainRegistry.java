package net.nightshade.divinity_engine.registry.divinity.gods.domains;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryBuilder;
import net.nightshade.divinity_engine.DivinityEngineMod;
import net.nightshade.divinity_engine.core.mixin.RegistryBuilderAccessor;
import net.nightshade.divinity_engine.divinity.gods.BaseGod;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

public class GodsDomainRegistry {
    private static final Set<String> REGISTERED_DOMAINS = new HashSet<>();

    public static void registerDomain(String domainId) {
        REGISTERED_DOMAINS.add(domainId.toLowerCase());
    }

    public static boolean isRegistered(String domainId) {
        return REGISTERED_DOMAINS.contains(domainId.toLowerCase());
    }

    public static Set<String> getAllDomains() {
        return Collections.unmodifiableSet(REGISTERED_DOMAINS);
    }
}
