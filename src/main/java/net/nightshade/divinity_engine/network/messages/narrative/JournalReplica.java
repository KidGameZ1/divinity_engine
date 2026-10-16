package net.nightshade.divinity_engine.network.messages.narrative;

import net.minecraft.nbt.*;
import java.util.LinkedHashMap;
import java.util.Map;

/** Connection-local replica. Deltas require their exact baseline. */
public final class JournalReplica {
    private CompoundTag journal;
    public boolean apply(CompoundTag packet) {
        long revision = packet.getLong("revision");
        if (!packet.getBoolean("full")) {
            if (journal == null) return false;
            if (revision <= journal.getLong("revision")) return true;
            if (revision != journal.getLong("revision") + 1) return false;
        }
        Map<String, CompoundTag> entries = new LinkedHashMap<>();
        if (!packet.getBoolean("full") && journal != null)
            for (Tag tag : journal.getList("entries", Tag.TAG_COMPOUND)) {
                CompoundTag entry = (CompoundTag) tag; entries.put(entry.getString("id"), entry.copy());
            }
        for (Tag tag : packet.getList("removed", Tag.TAG_STRING)) entries.remove(tag.getAsString());
        for (Tag tag : packet.getList("entries", Tag.TAG_COMPOUND)) {
            CompoundTag update = (CompoundTag) tag;
            entries.computeIfAbsent(update.getString("id"), ignored -> new CompoundTag()).merge(update.copy());
        }
        journal = new CompoundTag();
        ListTag list = new ListTag(); entries.values().forEach(list::add);
        journal.put("entries", list); journal.putLong("revision", revision);
        return true;
    }
    public CompoundTag snapshot() { return journal; }
    public void clear() { journal = null; }
}
