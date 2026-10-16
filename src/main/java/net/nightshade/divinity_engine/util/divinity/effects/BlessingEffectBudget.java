package net.nightshade.divinity_engine.util.divinity.effects;

import java.util.HashMap;
import java.util.Map;

/** Per-owner bounded admission windows. No world, packets, or client classes required. */
public final class BlessingEffectBudget {
    private record Window(long start, int count) { }
    private final Map<String, Window> cues = new HashMap<>();
    private long windowStart = Long.MIN_VALUE;
    private int count;

    public boolean acquire(String key, long tick, int interval, int burst) {
        if (key == null || interval < 1 || burst < 1 || burst > 12)
            throw new IllegalArgumentException("A visual budget needs a key, positive interval, and burst 1..12");
        if (windowStart == Long.MIN_VALUE || tick < windowStart || tick - windowStart >= 20) {
            windowStart = tick;
            count = 0;
            cues.entrySet().removeIf(entry -> tick < entry.getValue().start || tick - entry.getValue().start > 200);
        }
        if (count >= 12) return false;
        Window old = cues.get(key);
        boolean renew = old == null || tick < old.start || tick - old.start >= interval;
        if (!renew && old.count >= burst) return false;
        if (old == null && cues.size() >= 64) return false;
        cues.put(key, renew ? new Window(tick, 1) : new Window(old.start, old.count + 1));
        count++;
        return true;
    }
}
