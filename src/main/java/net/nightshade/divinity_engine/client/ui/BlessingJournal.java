package net.nightshade.divinity_engine.client.ui;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.nightshade.divinity_engine.api.narrative.NarrativeText;
import java.util.ArrayList;
import java.util.List;

/** Projection of the server journal; never guesses progress from client-side story definitions. */
public final class BlessingJournal {
    private BlessingJournal() {}
    public static List<CompoundTag> quests(CompoundTag journal, ResourceLocation god) {
        List<CompoundTag> result = new ArrayList<>();
        if (god == null) return result;
        for (Tag value : journal.getList("entries", Tag.TAG_COMPOUND)) {
            var entry = (CompoundTag) value;
            if (entry.getString("god").equals(god.toString()) && entry.getString("section").equals("story_journal")) result.add(entry);
        }
        return List.copyOf(result);
    }
    public static CompoundTag bond(CompoundTag journal, ResourceLocation god) {
        if (god != null) for (Tag value : journal.getList("entries", Tag.TAG_COMPOUND)) {
            var entry = (CompoundTag) value;
            if (entry.getString("god").equals(god.toString()) && entry.getString("section").equals("favor")) return entry;
        }
        return new CompoundTag();
    }
    public static String rewards(CompoundTag quest) {
        StringBuilder text = new StringBuilder("\n\nPERMANENT BLESSING UPGRADE\n");
        for (Tag value : quest.getList("upgrades", Tag.TAG_COMPOUND)) {
            var upgrade = (CompoundTag) value;
            if (!quest.getString("branch").isEmpty() && !quest.getString("branch").equals(upgrade.getString("branch"))) continue;
            text.append(upgrade.getString("name")).append(": ").append(upgrade.getString("description"));
            if (upgrade.getBoolean("earned")) text.append(upgrade.getBoolean("active") ? " [Earned / equipped]" : " [Earned / equip the eligible blessing]");
            else text.append(" [Granted at the ending]");
            text.append('\n');
        }
        return NarrativeText.resolve(text.toString());
    }
    public static String earnedUpgrade(List<CompoundTag> quests, ResourceLocation blessing) {
        StringBuilder text = new StringBuilder();
        for (var quest : quests) for (Tag value : quest.getList("upgrades", Tag.TAG_COMPOUND)) {
            var reward = (CompoundTag) value;
            if (reward.getBoolean("earned") && reward.getString("blessing").equals(blessing.toString()))
                text.append("QUEST UPGRADE: ").append(reward.getString("description")).append("\n\n");
        }
        return NarrativeText.resolve(text.toString());
    }
}
