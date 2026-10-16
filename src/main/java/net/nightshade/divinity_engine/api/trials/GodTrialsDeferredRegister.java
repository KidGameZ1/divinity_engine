package net.nightshade.divinity_engine.api.trials;

import net.nightshade.divinity_engine.api.divine_codex_pages.DivineCodexPage;
import net.nightshade.divinity_engine.registry.divinity.DivineCodexPagesRegistry;
import net.nightshade.divinity_engine.registry.divinity.trial.GodlyTrialsRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class GodTrialsDeferredRegister {
    private final String modid;
    private final List<GodTrialsRegistryObject<?>> entries = new ArrayList<>();

    public GodTrialsDeferredRegister(String modid) {
        this.modid = modid;
    }

    public <T extends GodTrial> GodTrialsRegistryObject<T> register(String id, Supplier<GodTrial> supplier) {
        var obj = new GodTrialsRegistryObject<T>(id, supplier);
        entries.add(obj);
        return obj;
    }

    public void registerAll() {
        for (var obj : entries) {
            GodTrial slot = obj.get();
            GodlyTrialsRegistry.register(new net.minecraft.resources.ResourceLocation(modid, obj.getId()), slot);
        }
    }
}
