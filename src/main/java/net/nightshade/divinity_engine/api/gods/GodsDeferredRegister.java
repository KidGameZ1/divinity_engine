package net.nightshade.divinity_engine.api.gods;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryBuilder;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.RegisterEvent;
import net.nightshade.divinity_engine.divinity.gods.BaseGod;
import net.nightshade.divinity_engine.registry.divinity.gods.GodsRegistry;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/** Registers a god and its statue together, before Forge starts registering blocks. */
public final class GodsDeferredRegister {
    private final String namespace;
    private final DeferredRegister<BaseGod> gods;
    private final DeferredRegister<Block> blocks;
    private final DeferredRegister<Item> items;
    private final DeferredRegister<BlockEntityType<?>> blockEntities;
    private final Map<ResourceLocation, GodStatueRegistration<?>> statues = new LinkedHashMap<>();
    private IEventBus registeredBus;
    private boolean declarationsClosed;

    private GodsDeferredRegister(String namespace, DeferredRegister<Block> blocks,
                                 DeferredRegister<Item> items, DeferredRegister<BlockEntityType<?>> blockEntities) {
        this.namespace = ResourceLocation.fromNamespaceAndPath(namespace, "validation").getNamespace();
        this.gods = DeferredRegister.create(GodsRegistry.REGISTRY_KEY, namespace);
        this.blocks = Objects.requireNonNull(blocks);
        this.items = Objects.requireNonNull(items);
        this.blockEntities = Objects.requireNonNull(blockEntities);
    }

    /** Addons use this factory and register only this object on their mod event bus. */
    public static GodsDeferredRegister create(String namespace) {
        return using(namespace, DeferredRegister.create(ForgeRegistries.BLOCKS, namespace),
                DeferredRegister.create(ForgeRegistries.ITEMS, namespace),
                DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, namespace));
    }

    /** Reuses existing content registers. This helper owns their event-bus registration. */
    public static GodsDeferredRegister using(String namespace, DeferredRegister<Block> blocks,
                                            DeferredRegister<Item> items,
                                            DeferredRegister<BlockEntityType<?>> blockEntities) {
        return new GodsDeferredRegister(namespace, blocks, items, blockEntities);
    }

    public <T extends BaseGod> RegistryObject<T> register(String name, Supplier<? extends T> factory) {
        return register(name, name + "_statue", factory);
    }

    /** Keep a previously released statue path when migrating an addon. Never rename saved IDs. */
    public <T extends BaseGod> RegistryObject<T> register(String name, String statuePath, Supplier<? extends T> factory) {
        if (declarationsClosed) throw new IllegalStateException("Declare gods before Forge registration events: " + namespace);
        ResourceLocation godId = ResourceLocation.fromNamespaceAndPath(namespace, name);
        ResourceLocation statueId = ResourceLocation.fromNamespaceAndPath(namespace, statuePath);
        Objects.requireNonNull(factory, "God factory for " + godId);
        if (statues.containsKey(godId)) throw new IllegalArgumentException("Duplicate god " + godId);
        if (statues.values().stream().anyMatch(entry -> entry.id().equals(statueId)))
            throw new IllegalArgumentException("Duplicate statue " + statueId);
        checkAvailable(blocks, statueId);
        checkAvailable(items, statueId);
        checkAvailable(blockEntities, statueId);
        GodStatueRegistration<T> entry = new GodStatueRegistration<>(name, statuePath, factory, gods, blocks, items, blockEntities);
        statues.put(godId, entry);
        return entry.god();
    }

    private static void checkAvailable(DeferredRegister<?> register, ResourceLocation id) {
        if (register.getEntries().stream().anyMatch(entry -> entry.getId().equals(id)))
            throw new IllegalArgumentException("Statue ID already declared in " + register.getRegistryName() + ": " + id);
    }

    public GodStatueRegistration<?> statue(RegistryObject<? extends BaseGod> god) {
        GodStatueRegistration<?> entry = statues.get(god.getId());
        if (entry == null) throw new IllegalArgumentException("God is not declared by this register: " + god.getId());
        return entry;
    }

    public Collection<GodStatueRegistration<?>> statues() {
        return List.copyOf(statues.values());
    }

    public Collection<RegistryObject<BaseGod>> getEntries() {
        return gods.getEntries();
    }

    /** Used by Divinity Engine to create the shared god registry, not by addons. */
    public Supplier<IForgeRegistry<BaseGod>> makeRegistry(Supplier<RegistryBuilder<BaseGod>> factory) {
        return gods.makeRegistry(factory);
    }

    public void register(IEventBus bus) {
        Objects.requireNonNull(bus);
        if (registeredBus == bus) return;
        if (registeredBus != null) throw new IllegalStateException("God register already attached to another mod bus: " + namespace);
        registeredBus = bus;
        gods.register(bus);
        blocks.register(bus);
        items.register(bus);
        blockEntities.register(bus);
        bus.addListener(EventPriority.HIGHEST, this::closeDeclarations);
        bus.addListener(this::validateSetup);
        bus.addListener(this::addPacks);
    }

    private void closeDeclarations(RegisterEvent event) {
        declarationsClosed = true;
    }

    private void addPacks(AddPackFindersEvent event) {
        StatueResources.addPack(event, namespace, statues());
    }

    private void validateSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> statues.values().forEach(GodStatueRegistration::validate));
    }
}
