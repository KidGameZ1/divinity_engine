package net.nightshade.divinity_engine.network.cap.player.narrative;

import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.util.INBTSerializable;
import net.nightshade.divinity_engine.util.divinity.narrative.NarrativeProgress;
import java.util.*;

/** Lives independently of contacted gods. Abandonment never erases choices or earned endings. */
public final class DivineNarrativeData implements INBTSerializable<CompoundTag> {
    private final Map<ResourceLocation, Memory> gods = new LinkedHashMap<>();
    private final Map<ResourceLocation, Memory> stories = new LinkedHashMap<>();
    public final Map<String, String> epilogues = new LinkedHashMap<>();
    public transient CompoundTag lastJournal;
    public transient long journalRevision;
    public transient long nextJournalRequest;
    public long nextAmbientTick;
    public Memory get(ResourceLocation god) { return gods.computeIfAbsent(god, ignored -> new Memory()); }
    public Map<ResourceLocation, Memory> memories() { return Collections.unmodifiableMap(gods); }
    public Collection<Memory> allMemories() { var result = new ArrayList<>(gods.values()); result.addAll(stories.values()); return result; }
    public Memory findStory(net.nightshade.divinity_engine.api.narrative.DivineStoryDefinition def) {
        Memory primary = gods.get(def.godId());
        if (primary != null && (primary.storyId.equals(def.id().toString()) || primary.storyId.isEmpty()
            && net.nightshade.divinity_engine.util.divinity.narrative.DivineNarrativeRegistry.primary(def))) return primary;
        return stories.get(def.id());
    }
    public Memory story(net.nightshade.divinity_engine.api.narrative.DivineStoryDefinition def) {
        Memory memory = findStory(def);
        if (memory == null) {
            if (net.nightshade.divinity_engine.util.divinity.narrative.DivineNarrativeRegistry.primary(def) && !gods.containsKey(def.godId())) memory = get(def.godId());
            else memory = stories.computeIfAbsent(def.id(), ignored -> new Memory());
        }
        memory.storyId = def.id().toString(); memory.godId = def.godId().toString(); return memory;
    }
    public void adminReset(ResourceLocation god) {
        gods.entrySet().removeIf(entry -> {
            if (!entry.getKey().equals(god) && !entry.getValue().storyId.equals(god.toString())) return false;
            entry.getValue().invalidatePending(); return true;
        });
        stories.entrySet().removeIf(entry -> { if (!entry.getValue().godId.equals(god.toString()) && !entry.getKey().equals(god)) return false; entry.getValue().invalidatePending(); return true; });
        net.nightshade.divinity_engine.util.divinity.narrative.DivineNarrativeRegistry.relationships().stream()
            .filter(relation -> relation.gods().contains(god) || relation.stories().contains(god)).forEach(relation -> epilogues.remove(relation.id().toString()));
    }
    public static final class Memory {
        public CompoundTag relationship = new CompoundTag();
        public CompoundTag favorLedger = new CompoundTag();
        public final NarrativeProgress story = new NarrativeProgress();
        public net.nightshade.divinity_engine.api.narrative.DivineStoryDefinition.Branch branchSnapshot;
        public String storyId = "", godId = "";
        private String rawStorySnapshot = "";
        private net.nightshade.divinity_engine.api.narrative.DivineStoryDefinition storySnapshot;
        private long storyResolutionRevision = -1;
        public void snapshot(net.nightshade.divinity_engine.api.narrative.DivineStoryDefinition definition) {
            storySnapshot = definition;
            rawStorySnapshot = net.nightshade.divinity_engine.util.divinity.narrative.NarrativeDefinitionJson.encodeStory(definition);
        }
        public net.nightshade.divinity_engine.api.narrative.DivineStoryDefinition definition(net.nightshade.divinity_engine.api.narrative.DivineStoryDefinition fallback) {
            if (rawStorySnapshot.isEmpty()) return fallback;
            if (storySnapshot == null) {
                long revision = net.nightshade.divinity_engine.api.narrative.StoryObjectiveType.registrationRevision();
                if (storyResolutionRevision == revision) return null;
                storyResolutionRevision = revision;
                try { storySnapshot = net.nightshade.divinity_engine.util.divinity.narrative.NarrativeDefinitionJson.decodeStory(rawStorySnapshot); }
                catch (RuntimeException ignored) { return null; }
            }
            return storySnapshot;
        }
        private String rawBranchSnapshot = "";
        private long snapshotResolutionRevision = -1;
        private long generation;
        public boolean donationInProgress;
        public int contacts, abandonments, prayers, offerings, failures, victories, highestFavor = -250;
        public boolean contacted, reachedHeight, resettableAttempt;
        public long nextDialogueTick, attemptStart = -1, prayerSeen = -1, nearSeen = -1, lastDamage = -1;
        public String lastDialogue = "", preview = "", previewDefinition = "";
        public net.minecraft.core.BlockPos shrinePosition;
        public String shrineDimension = "";
        public long previewExpires;
        public final Set<String> placements = new LinkedHashSet<>();
        public double distanceRemainder;
        public long generation() { return generation; }
        public void invalidatePending() { generation++; }
        public void resetAttempt() { story.resetAttempt(); attemptStart = -1; invalidatePending(); }
        public void interrupt() { if (resettableAttempt) story.resetAttempt(); invalidatePending(); attemptStart = -1; prayerSeen = -1; nearSeen = -1; shrinePosition = null; shrineDimension = ""; preview = ""; distanceRemainder = 0; }
        public boolean hasUnresolvedSnapshot() { return !rawBranchSnapshot.isEmpty() && resolveSnapshot() == null; }
        public net.nightshade.divinity_engine.api.narrative.DivineStoryDefinition.Branch resolveSnapshot() {
            if (branchSnapshot != null || rawBranchSnapshot.isEmpty()) return branchSnapshot;
            long revision = net.nightshade.divinity_engine.api.narrative.StoryObjectiveType.registrationRevision();
            if (snapshotResolutionRevision == revision) return null;
            snapshotResolutionRevision = revision;
            try { branchSnapshot = net.nightshade.divinity_engine.util.divinity.narrative.NarrativeDefinitionJson.decodeBranch(rawBranchSnapshot); }
            catch (RuntimeException ignored) { /* The exact original data must survive until its addon can decode it. */ }
            return branchSnapshot;
        }
    }
    @Override public CompoundTag serializeNBT() {
        CompoundTag root = new CompoundTag();
        root.putInt("version", 2);
        root.putLong("next_ambient_tick", nextAmbientTick);
        CompoundTag entries = new CompoundTag();
        gods.forEach((id, m) -> entries.put(id.toString(), writeMemory(m)));
        root.put("gods", entries);
        CompoundTag extra = new CompoundTag(); stories.forEach((id, m) -> extra.put(id.toString(), writeMemory(m))); root.put("stories", extra);
        CompoundTag lore = new CompoundTag(); epilogues.forEach(lore::putString); root.put("epilogues", lore);
        return root;
    }
    private static CompoundTag writeMemory(Memory m) {
            CompoundTag tag = new CompoundTag();
            tag.put("relationship", m.relationship.copy());
            tag.put("favor_ledger", m.favorLedger.copy());
            tag.putString("story_id", m.storyId); tag.putString("god_id", m.godId);
            tag.putBoolean("expanded", m.story.expanded());
            if (!m.rawStorySnapshot.isEmpty()) tag.putString("story_snapshot", m.rawStorySnapshot);
            tag.putString("stage", m.story.stage().name()); tag.putString("branch", m.story.branch());
            if (!m.rawBranchSnapshot.isEmpty()) tag.putString("branch_snapshot", m.rawBranchSnapshot);
            else if (m.branchSnapshot != null) tag.putString("branch_snapshot", net.nightshade.divinity_engine.util.divinity.narrative.NarrativeDefinitionJson.encodeBranch(m.branchSnapshot));
            tag.putInt("progress", m.story.progress()); tag.putString("specialization", m.story.specialization());
            tag.putString("expected_trial", m.story.expectedTrial());
            ListTag outcomes = new ListTag(); m.story.outcomes().forEach(s -> outcomes.add(StringTag.valueOf(s))); tag.put("trial_outcomes", outcomes);
            tag.putInt("contacts", m.contacts); tag.putInt("abandonments", m.abandonments); tag.putInt("prayers", m.prayers);
            tag.putInt("offerings", m.offerings); tag.putInt("failures", m.failures); tag.putInt("victories", m.victories);
            tag.putInt("highest_favor", m.highestFavor); tag.putBoolean("contacted", m.contacted);
            tag.putBoolean("reached_height", m.reachedHeight); tag.putBoolean("resettable_attempt", m.resettableAttempt);
            tag.putString("last_dialogue", m.lastDialogue);
            tag.putLong("next_dialogue_tick", m.nextDialogueTick);
            ListTag positions = new ListTag(); m.placements.forEach(s -> positions.add(StringTag.valueOf(s))); tag.put("placements", positions);
            return tag;
    }
    @Override public void deserializeNBT(CompoundTag root) {
        allMemories().forEach(Memory::invalidatePending);
        gods.clear(); stories.clear(); epilogues.clear(); lastJournal = null; nextAmbientTick = root.getLong("next_ambient_tick");
        readMemories(root.getCompound("gods"), gods); readMemories(root.getCompound("stories"), stories);
        CompoundTag lore = root.getCompound("epilogues"); lore.getAllKeys().forEach(key -> epilogues.put(key, lore.getString(key)));
    }
    private static void readMemories(CompoundTag entries, Map<ResourceLocation, Memory> destination) {
        for (String key : entries.getAllKeys()) {
            ResourceLocation id = ResourceLocation.tryParse(key); if (id == null) continue;
            CompoundTag tag = entries.getCompound(key); Memory m = new Memory();
            m.relationship = tag.getCompound("relationship").copy();
            m.favorLedger = tag.getCompound("favor_ledger").copy();
            m.storyId = tag.getString("story_id"); m.godId = tag.getString("god_id"); m.rawStorySnapshot = tag.getString("story_snapshot");
            m.story.expanded(tag.getBoolean("expanded"));
            NarrativeProgress.Stage stage;
            try { stage = NarrativeProgress.Stage.valueOf(tag.getString("stage")); } catch (IllegalArgumentException ignored) { stage = NarrativeProgress.Stage.DORMANT; }
            Set<String> outcomes = new LinkedHashSet<>(); tag.getList("trial_outcomes", Tag.TAG_STRING).forEach(t -> outcomes.add(t.getAsString()));
            m.story.restore(stage, tag.getString("branch"), tag.getInt("progress"), tag.getString("specialization"), tag.getString("expected_trial"), outcomes);
            if (tag.contains("branch_snapshot")) {
                m.rawBranchSnapshot = tag.getString("branch_snapshot");
                m.resolveSnapshot();
            }
            if (!tag.contains("expanded") && m.story.stage() != NarrativeProgress.Stage.DORMANT) {
                ResourceLocation savedId = m.storyId.isBlank() ? null : ResourceLocation.tryParse(m.storyId);
                var definition = net.nightshade.divinity_engine.util.divinity.narrative.DivineNarrativeRegistry.story(savedId == null ? id : savedId);
                if (definition != null && definition.chronicle().expanded()) {
                    // Stage names are stable: prior chapters become discovered, never new prerequisites.
                    // The original chosen branch snapshot continues to own its deed and reward.
                    m.story.expanded(true); m.storyId = definition.id().toString(); m.godId = definition.godId().toString();
                    m.snapshot(definition);
                }
            }
            m.contacts = tag.getInt("contacts"); m.abandonments = tag.getInt("abandonments"); m.prayers = tag.getInt("prayers");
            m.offerings = tag.getInt("offerings"); m.failures = tag.getInt("failures"); m.victories = tag.getInt("victories");
            m.highestFavor = tag.contains("highest_favor") ? tag.getInt("highest_favor") : -250; m.contacted = tag.getBoolean("contacted");
            m.reachedHeight = tag.getBoolean("reached_height"); m.resettableAttempt = tag.getBoolean("resettable_attempt");
            m.lastDialogue = tag.getString("last_dialogue");
            m.nextDialogueTick = tag.getLong("next_dialogue_tick");
            tag.getList("placements", Tag.TAG_STRING).stream().limit(10000).forEach(t -> m.placements.add(t.getAsString()));
            m.interrupt(); destination.put(id, m);
        }
    }
}
