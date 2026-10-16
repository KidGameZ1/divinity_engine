package net.nightshade.divinity_engine.api.divine_codex_pages;

import net.minecraft.world.inventory.Slot;
import net.nightshade.divinity_engine.registry.divinity.DivineCodexPagesRegistry;


import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class DivineCodexPageDeferredRegister {
    private final String modid;
    private final List<DivineCodexPageRegistryObject<?>> entries = new ArrayList<>();

    public DivineCodexPageDeferredRegister(String modid) {
        this.modid = modid;
    }

    public <T extends DivineCodexPage> DivineCodexPageRegistryObject<T> register(String id, Supplier<DivineCodexPage> supplier) {
        var obj = new DivineCodexPageRegistryObject<T>(id, supplier);
        entries.add(obj);
        return obj;
    }

    public void registerAll() {
        for (var obj : entries) {
            DivineCodexPage slot = obj.get();
            DivineCodexPagesRegistry.addPage(slot.getTitle(), slot.getContent());
        }
    }
    public void submit() { DivineCodexPagesRegistry.addDeferredRegister(this); }
}
