package net.nightshade.divinity_engine.api.divine_codex_pages;



import java.util.function.Supplier;

public class DivineCodexPageRegistryObject<T extends DivineCodexPage> {
    private final String id;
    private final Supplier<DivineCodexPage> supplier;

    public DivineCodexPageRegistryObject(String id, Supplier<DivineCodexPage> supplier) {
        this.id = id;
        this.supplier = supplier;
    }

    public DivineCodexPage get() {
        return supplier.get();
    }

    public String getId() {
        return id;
    }
}
