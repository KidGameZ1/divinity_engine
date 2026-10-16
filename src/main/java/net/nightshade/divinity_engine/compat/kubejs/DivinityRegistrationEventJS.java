package net.nightshade.divinity_engine.compat.kubejs;

import com.google.gson.JsonObject;
import dev.latvian.mods.kubejs.event.EventJS;
import dev.latvian.mods.kubejs.util.JsonIO;
import net.minecraft.resources.ResourceLocation;
import net.nightshade.divinity_engine.api.content.DivinityContent;

import java.util.function.BiConsumer;

/** Startup declarations use the same JSON schema and validation as Divinity content files. */
public final class DivinityRegistrationEventJS extends EventJS {
    private final Thread startupThread = Thread.currentThread();
    private boolean open = true;

    DivinityRegistrationEventJS() {}

    public void god(String id, Object spec) {
        register(id, spec, DivinityContent::registerGod);
    }

    public void blessing(String id, Object spec) {
        register(id, spec, DivinityContent::registerBlessing);
    }

    public void trial(String id, Object spec) {
        register(id, spec, DivinityContent::registerTrial);
    }

    private void register(String id, Object spec, BiConsumer<ResourceLocation, JsonObject> registrar) {
        if (Thread.currentThread() != startupThread || !open) {
            throw new IllegalStateException("Divinity declarations must run inside DivinityEvents.register; restart to change content");
        }

        int separator = id == null ? -1 : id.indexOf(':');
        ResourceLocation key = id == null ? null : ResourceLocation.tryParse(id);
        if (key == null || separator <= 0 || separator == id.length() - 1) {
            throw new IllegalArgumentException("Divinity content requires a namespaced id, such as pack:my_blessing: " + id);
        }
        if (!(JsonIO.of(spec) instanceof JsonObject json)) {
            throw new IllegalArgumentException("Divinity content spec for " + key + " must be a JSON object");
        }

        registrar.accept(key, json.deepCopy());
    }

    void close() {
        open = false;
    }
}
