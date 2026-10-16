package net.nightshade.divinity_engine.compat.kubejs;

import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.ScriptType;
import net.nightshade.divinity_engine.api.content.DivinityContent;

/** Loaded only by KubeJS through kubejs.plugins.txt, never by the mod entry point. */
public final class DivinityKubeJSPlugin extends KubeJSPlugin {
    private boolean initialized;
    private boolean startupPosted;

    @Override
    public void init() {
        if (!initialized) {
            DivinityContent.setScriptDispatcher(DivinityKubeEvents::dispatch);
            initialized = true;
        }
    }

    @Override
    public void registerEvents() {
        DivinityKubeEvents.GROUP.register();
    }

    @Override
    public void initStartup() {
        if (startupPosted) return;
        startupPosted = true;

        // KubeJS 2001 calls this after startup scripts, inside its Forge constructor.
        var event = new DivinityRegistrationEventJS();
        try {
            DivinityKubeEvents.REGISTER.post(ScriptType.STARTUP, event);
        } finally {
            event.close();
        }
    }
}
