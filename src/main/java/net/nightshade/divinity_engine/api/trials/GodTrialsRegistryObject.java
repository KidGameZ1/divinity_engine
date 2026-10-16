package net.nightshade.divinity_engine.api.trials;



import net.nightshade.divinity_engine.api.divine_codex_pages.DivineCodexPage;

import java.util.function.Supplier;

public class GodTrialsRegistryObject<T extends GodTrial> {
    private final String id;
    private final Supplier<GodTrial> supplier;

    public GodTrialsRegistryObject(String id, Supplier<GodTrial> supplier) {
        this.id = id;
        this.supplier = supplier;
    }

    public GodTrial get() {
        return supplier.get();
    }

    public String getId() {
        return id;
    }
}
