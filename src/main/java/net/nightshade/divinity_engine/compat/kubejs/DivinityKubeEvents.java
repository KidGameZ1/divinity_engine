package net.nightshade.divinity_engine.compat.kubejs;

import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;
import dev.latvian.mods.kubejs.event.Extra;
import dev.latvian.mods.kubejs.script.ScriptType;
import net.nightshade.divinity_engine.api.content.ContentBehaviorContext;

final class DivinityKubeEvents {
    static final EventGroup GROUP = EventGroup.of("DivinityEvents");
    static final EventHandler REGISTER = GROUP.startup("register", () -> DivinityRegistrationEventJS.class);
    static final EventHandler BLESSING = GROUP.server("blessing", () -> DivinityBlessingEventJS.class)
            .extra(Extra.REQUIRES_ID);

    private DivinityKubeEvents() {}

    static void dispatch(ContentBehaviorContext context) {
        if (!context.getLevel().getServer().isSameThread() || !BLESSING.hasListeners(context.getId())) return;

        var event = new DivinityBlessingEventJS(context);
        try {
            // Let KubeJS own listeners so server-script reloads cannot retain stale callbacks.
            BLESSING.post(ScriptType.SERVER, context.getId(), event);
        } finally {
            event.close();
        }
    }
}
