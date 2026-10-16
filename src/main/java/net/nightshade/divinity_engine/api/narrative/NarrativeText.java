package net.nightshade.divinity_engine.api.narrative;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import java.util.regex.Pattern;

/** Translation markers survive saves and network sync; clients resolve their own language. */
public final class NarrativeText {
    private static final Pattern KEYS = Pattern.compile("\\{\\{lang:([a-zA-Z0-9_.:-]+)}}");
    private NarrativeText() {}
    public static String translatable(String key) {
        if (!key.matches("[a-zA-Z0-9_.:-]+")) throw new IllegalArgumentException("Invalid translation key");
        return "{{lang:" + key + "}}";
    }
    public static MutableComponent component(String text) {
        MutableComponent result = Component.empty(); var matcher = KEYS.matcher(text); int offset = 0;
        while (matcher.find()) { result.append(text.substring(offset, matcher.start())); result.append(Component.translatable(matcher.group(1))); offset = matcher.end(); }
        return result.append(text.substring(offset));
    }
    public static String resolve(String text) { return component(text).getString(); }
}
