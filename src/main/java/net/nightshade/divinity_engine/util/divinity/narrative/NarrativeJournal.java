package net.nightshade.divinity_engine.util.divinity.narrative;

import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.registries.ForgeRegistries;
import net.nightshade.divinity_engine.api.narrative.*;
import net.nightshade.divinity_engine.util.divinity.gods.DivineRelationship;
import net.nightshade.divinity_engine.network.cap.player.narrative.*;
import net.nightshade.divinity_engine.network.messages.narrative.NarrativeNetwork;
import java.util.*;

/** Builds a player-visible journal, never a dump of persisted secrets or unchosen endings. */
public final class NarrativeJournal {
    private NarrativeJournal() {}
    public static CompoundTag build(ServerPlayer player) {
        CompoundTag result = new CompoundTag(); ListTag entries = new ListTag(); result.put("entries", entries);
        add(entries, "guide:start", "getting_started", "Beginning a Divine Story",
            "Find or place a god's statue and right-click it to make contact. That god's guide now appears under Discovered Gods. Contact is remembered even if you later leave.\n\n"
            + "Contact is enough to begin a built-in god's story. Right-click their statue again, open Quests and choose Begin quest. Stay within six blocks of the matching statue. You do not need to grind favor points first.\n\n"
            + "After accepting the story, drop an offering that god accepts directly in front of the statue and stay nearby. Ordinary accepted offerings advance The Call. Story donations are different: use Contribute quest items in the statue's Quests tab with the requested items in your inventory; only the requested quantity is consumed.\n\n"
            + "Sneak beside the matching statue to pray. Continuous challenges reset on interruption or logout, not the whole story. Return to the statue for choices and the finale. The Codex records progress but cannot perform remote shrine actions.", "", 0, 0);
        add(entries, "guide:chapters", "getting_started", "Chapters, Choices and Trials",
            "The Call -> A Witness -> Divided Testimony -> The Chosen Deed -> The Reckoning -> The Trial -> The Return.\n\n"
            + "Both permanent blessing rewards are previewed before confirmation. Either path proves your bond and can unlock every blessing; a choice never blocks another god's story. Specializations work only while the original blessing is equipped and eligible.\n\n"
            + "After The Reckoning, begin a NEW trial from the blessings screen opened by that god's statue. Inventory-opened menus cannot start trials. A fresh victory proves the final blessing milestone. Return to the statue's Quests tab and choose Claim ending and upgrade for your ending, shrine recognition and permanent specialization. Choosing a branch or winning the trial alone does not grant that upgrade. Failed trials can be retried without erasing completed deeds.\n\n"
            + "Related stories are optional. Completing both opens a paired epilogue at either god's statue, with no extra combat reward.\n\n"
            + "Use the Quests tab or Story Journal for guidance. Speak about quest at a statue opens path previews, permanent confirmation and available epilogues. Abandonment pauses objectives; reconnect to resume. An administrator alone can reset a permanent branch.", "", 0, 0);
        add(entries, "guide:favor", "getting_started", "Bonds, Deeds and Memories",
            "The gods judge what you have proven, not a filled favor meter. The Bond tab shows the server's current relationship rank, each blessing's required deed, remembered events and your next action.\n\n"
            + "FIVE BLESSING MILESTONES\nAccept the story and make its accepted offering. Complete A Witness. Complete your chosen branch deed. Complete The Reckoning. Then win a fresh trial after reaching The Trial. Each proof unlocks the corresponding blessing in that god's progression. These proofs come from the god's primary story; optional extra stories cannot bypass it.\n\n"
            + "Both paths count. Prayer and offerings are meaningful interactions and may be requested by a chapter, but repeating them cannot replace a required deed. Follow the exact active objective in Quests.\n\n"
            + "You can maintain contact with multiple gods and mix their blessings in your three slots. Abandonment pauses the relationship; reconnect at the matching statue to resume. Memories, confirmed choices, earned upgrades and positive blessing unlocks carried over from older saves are retained.\n\n"
            + "Legacy addons without a story can retain their existing favor API for compatibility. Their server-provided requirement remains authoritative; that fallback is not the built-in pantheon's progression system.", "", 0, 0);
        add(entries, "guide:blessings", "getting_started", "Blessings, Curses and Controls",
            "Use the blessings screen to inspect and equip your blessings. It opens from a contacted god's statue or the blessings button in your inventory.\n\n"
            + "R, G and V are the default active-blessing keys and can be rebound in Controls. You may equip three blessings, including a mixed-god loadout. Passive blessings apply their own conditions without a key press. Unlocks, contact, cooldowns and each blessing's activation conditions still apply to specialized blessings.\n\n"
            + "A damaged relationship can bring a god's curse. Inspect your Bond and blessing descriptions rather than assuming that every god responds to the same actions. Trial failure alone does not erase your proven deeds.\n\n"
            + "Trials can only be requested from a blessings screen opened at a statue. Story rewards enhance an existing blessing; they do not add another loadout, ascension, or permanent HUD.", "", 0, 0);
        var data = DivineNarrativeCapability.get(player); if (data == null) return result;
        for (var registered : DivineNarrativeRegistry.stories().stream().sorted(Comparator.comparing(def -> def.id().toString())).toList()) {
            if (!DivineNarrativeManager.discovered(player, registered.godId())) continue;
            var memory = data.story(registered); var def = memory.definition(registered);
            if (def == null || memory.hasUnresolvedSnapshot()) {
                var paused = add(entries, registered.id() + "/status", "story_journal", registered.title(), "Paused: this saved story requires an unavailable addon. Your choices and rewards are preserved.", "Paused", 0, 0);
                paused.putString("god", registered.godId().toString()); paused.putString("story", registered.id().toString());
                paused.putString("quest_title", registered.title()); paused.putString("stage", "PAUSED"); paused.putString("action", "status");
                paused.putString("next", "Restore the story's missing addon to continue. Your progress is preserved."); continue;
            }
            String name = godName(def.godId()), id = def.id().toString();
            boolean contacted = DivineNarrativeManager.contacted(player, def.godId());
            var godDefinition = DivineNarrativeManager.god(def.godId());
            var offeringIds = def.chronicle().offeringExamples().stream();
            String offerings = offeringIds.map(item -> {
                var value = ForgeRegistries.ITEMS.getValue(item); return value == null ? item.toString() : NarrativeText.translatable(value.getDescriptionId());
            }).reduce((a, b) -> a + ", " + b).orElse("An ordinary offering this god accepts");
            String rewards = "";
            for (String branch : List.of("a", "b")) {
                var path = def.branches().get(branch);
                rewards += "\n\n" + path.name() + ": " + path.reward().description();
            }
            add(entries, id + "/guide", "discovered_gods", name + ": " + def.title(),
                (def.chronicle().expanded() ? def.chronicle().premise() : def.request())
                + "\n\nHOW TO START\nContact " + name + ", reopen their statue and select Quests > Begin quest. No favor-point threshold is needed for a built-in story."
                + "\n\nAccepted offering examples: "
                + offerings + ". Drop an offering directly in front of the statue after starting the story.\n\nPERMANENT PATH REWARDS" + rewards,
                contacted ? "Contacted" : "Discovered; reconnect to progress", 0, 0);
            String stage = NarrativeDefinitionJson.chapterTitle(memory.story.chapter());
            String status = memory.story.stage() == NarrativeProgress.Stage.DORMANT ? "Not started" : memory.story.stage() == NarrativeProgress.Stage.COMPLETE ? "Complete" : stage;
            if (!contacted) status += " (paused: reconnect)";
            var path = DivineNarrativeManager.path(def, memory);
            String next = switch (memory.story.stage()) {
                case DORMANT -> "Make contact, then select Quests > Begin quest at the matching statue.";
                case OFFERING -> "Make an ordinary accepted offering at the matching statue.";
                case CHOICE -> "Return to the statue and select Quests > Speak about quest to preview both paths. Confirmation is permanent.";
                case TRIAL -> "Start and win a fresh trial from this god's statue blessings screen.";
                case RETURN -> "Return to the matching statue and select Quests > Claim ending and upgrade.";
                case COMPLETE -> "Your chosen ending is in Chronicle. You may discover optional paired epilogues.";
                default -> path == null ? "The required objective handler is unavailable." : path.deed();
            };
            CompoundTag quest = add(entries, id + "/status", "story_journal", name + ": " + def.title(),
                "CURRENT CHAPTER\n" + status + "\n\nNEXT ACTION\n" + next + (memory.story.branch().isEmpty() ? "" : "\n\nChosen path: " + def.branches().get(memory.story.branch()).name())
                + "\n\nProgress only counts after its chapter begins. Trial creatures do not count toward ordinary combat deeds.", status,
                memory.story.activeObjective() ? memory.story.progress() : 0, memory.story.activeObjective() && path != null ? path.target() : 0);
            quest.putString("god", def.godId().toString()); quest.putString("story", id);
            quest.putString("quest_title", def.title()); quest.putString("next", next);
            quest.putString("offering_examples", offerings);
            quest.putString("worship_requirement", "");
            quest.putString("stage", memory.story.stage().name()); quest.putString("branch", memory.story.branch());
            quest.putBoolean("contacted", contacted);
            quest.putBoolean("can_start", godDefinition != null && DivineRelationship.canStartStory(player, godDefinition));
            quest.putString("action", switch (memory.story.stage()) {
                case DORMANT -> "request"; case RETURN -> "finish";
                default -> path != null && memory.story.activeObjective() && path.objective().equals(StoryObjectiveType.DONATION) ? "donate" : "status";
            });
            ListTag upgrades = new ListTag();
            for (String branch : List.of("a", "b")) {
                var rewardPath = branch.equals(memory.story.branch()) && memory.resolveSnapshot() != null
                        ? memory.resolveSnapshot() : def.branches().get(branch);
                var reward = rewardPath.reward(); CompoundTag upgrade = new CompoundTag();
                boolean earned = memory.story.stage() == NarrativeProgress.Stage.COMPLETE && memory.story.specialization().equals(reward.id().toString());
                upgrade.putString("id", reward.id().toString()); upgrade.putString("blessing", reward.blessingId().toString());
                upgrade.putString("branch", branch); upgrade.putString("name", rewardPath.name());
                upgrade.putString("description", reward.description()); upgrade.putBoolean("earned", earned);
                upgrade.putBoolean("active", earned && BlessingSpecializations.hasEligibleBlessing(player, def.godId(), reward.blessingId()));
                upgrades.add(upgrade);
            }
            quest.put("upgrades", upgrades);
            int unlocked = chapterIndex(memory.story.stage());
            List<String> chapters = List.of("call", "witness", "testimony", "deed", "reckoning", "trial", "ending");
            for (int i = 0; i <= unlocked; i++) {
                String key = chapters.get(i);
                if (i == 3 || i == 4 || i == 6) { if (memory.story.branch().isEmpty()) continue; key += "_" + memory.story.branch(); }
                var chapter = def.chronicle().passages().get(key);
                if (chapter != null) add(entries, id + "/chapter/" + key, "chronicle", name + " - " + chapter.title(), chapter.text(), "Discovered", 0, 0);
            }
            if (!def.chronicle().expanded() && memory.story.stage() == NarrativeProgress.Stage.COMPLETE && path != null)
                add(entries, id + "/ending", "chronicle", def.title() + ": " + path.name(), path.ending(), "Earned ending", 0, 0);
        }
        for (var god : net.nightshade.divinity_engine.util.divinity.gods.GodHelper.getAllContactedGods(player)) {
            var id = god.getBaseGodId(); var memory = data.get(id); var ledger = memory.favorLedger;
            var entry = add(entries, id + "/favor", "favor", "Divine Bond", "", "", 0, 0);
            entry.putString("god", id.toString()); entry.putInt("highest", Math.max(memory.highestFavor, god.getFavor()));
            entry.put("relationship", DivineRelationship.snapshot(player, god.getBaseGod()));
            entry.putInt("victories", memory.victories); entry.putInt("offerings", memory.offerings);
            entry.putBoolean("foundation", ledger.getBoolean("foundation"));
            long now = DivineNarrativeManager.now(player);
            for (var source : net.nightshade.divinity_engine.util.divinity.gods.FavorRewards.Source.values())
                entry.putInt(source.name(), net.nightshade.divinity_engine.util.divinity.gods.FavorRewards.remaining(ledger, source, now));
        }
        for (var relation : DivineNarrativeRegistry.relationships().stream().sorted(Comparator.comparing(r -> r.id().toString())).toList()) {
            if (!relation.gods().stream().allMatch(god -> DivineNarrativeManager.discovered(player, god))) continue;
            String ending = data.epilogues.get(relation.id().toString());
            boolean complete = DivineNarrativeManager.crossoverReady(player, relation);
            String status = ending != null ? "Epilogue recorded" : complete ? "Return to either statue for the epilogue" : "Complete both stories to learn more";
            String history = complete || ending != null ? relation.relation() + "\n\n" + relation.summary()
                : godName(relation.gods().get(0)) + " and " + godName(relation.gods().get(1))
                    + " share an unfinished history. Their stories are independent. This journal will compare their accounts after you have heard both endings.";
            add(entries, relation.id() + "/relationship", "relationships", relation.title(), history + "\n\n" + status, status, 0, 0);
            if (ending != null) add(entries, relation.id() + "/epilogue", "chronicle", relation.title() + ": Epilogue", ending, "Earned epilogue", 0, 0);
        }
        return result;
    }
    private static int chapterIndex(NarrativeProgress.Stage stage) {
        return switch (stage) { case DORMANT -> -1; case OFFERING -> 0; case WITNESS -> 1; case CHOICE -> 2; case DEED -> 3; case RECKONING -> 4; case TRIAL, RETURN -> 5; case COMPLETE -> 6; };
    }
    private static String godName(ResourceLocation id) {
        var god = DivineNarrativeManager.god(id); return god == null ? id.toString() : NarrativeText.translatable(god.getNameTranslationKey());
    }

    private static CompoundTag add(ListTag list, String id, String section, String title, String text, String status, int progress, int target) {
        CompoundTag entry = new CompoundTag(); entry.putString("id", id); entry.putString("section", section); entry.putString("title", title);
        entry.putString("text", text); entry.putString("status", status); entry.putInt("progress", progress); entry.putInt("target", target); list.add(entry);
        return entry;
    }
    public static void sync(ServerPlayer player, DivineNarrativeData data, boolean force) {
        long start = System.nanoTime();
        CompoundTag current = build(player);
        long elapsed = System.nanoTime() - start;
        if (elapsed > 50_000_000L) net.nightshade.divinity_engine.DivinityEngineMod.LOGGER.warn("Journal build for {} took {} ms", player.getUUID(), elapsed / 1_000_000);
        if (!force && current.equals(data.lastJournal)) return;
        CompoundTag packet = new CompoundTag(); packet.putLong("revision", ++data.journalRevision);
        boolean full = force || data.lastJournal == null; packet.putBoolean("full", full);
        if (full) packet.put("entries", current.getList("entries", Tag.TAG_COMPOUND).copy());
        else {
            Map<String, CompoundTag> previous = index(data.lastJournal), next = index(current);
            ListTag updates = new ListTag(), removed = new ListTag();
            next.forEach((id, entry) -> {
                CompoundTag old = previous.get(id);
                if (entry.equals(old)) return;
                CompoundTag delta = new CompoundTag(); delta.putString("id", id);
                for (String key : entry.getAllKeys()) if (old == null || !Objects.equals(old.get(key), entry.get(key))) delta.put(key, entry.get(key).copy());
                updates.add(delta);
            });
            previous.keySet().stream().filter(id -> !next.containsKey(id)).forEach(id -> removed.add(StringTag.valueOf(id)));
            packet.put("entries", updates); packet.put("removed", removed);
        }
        data.lastJournal = current; NarrativeNetwork.sync(player, packet);
    }

    /** Per-second counters never rebuild story prose. Chapter transitions still use sync. */
    public static void syncCounters(ServerPlayer player) {
        var data = DivineNarrativeCapability.get(player);
        if (data == null) return;
        if (data.lastJournal == null) { sync(player, data, true); return; }
        ListTag updates = new ListTag();
        for (Tag value : data.lastJournal.getList("entries", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) value, changes = new CompoundTag();
            if (entry.contains("story")) {
                var id = ResourceLocation.tryParse(entry.getString("story"));
                var definition = id == null ? null : DivineNarrativeRegistry.byId(id);
                var memory = definition == null ? null : data.findStory(definition);
                if (memory != null) {
                    var god = DivineNarrativeManager.god(definition.godId());
                    boolean canStart = god != null && DivineRelationship.canStartStory(player, god);
                    if (canStart != entry.getBoolean("can_start")) changes.putBoolean("can_start", canStart);
                    int progress = memory.story.activeObjective() ? memory.story.progress() : 0;
                    if (progress != entry.getInt("progress")) changes.putInt("progress", progress);
                    ListTag upgrades = entry.getList("upgrades", Tag.TAG_COMPOUND).copy();
                    for (Tag upgrade : upgrades) {
                        CompoundTag reward = (CompoundTag) upgrade;
                        var blessing = ResourceLocation.tryParse(reward.getString("blessing"));
                        reward.putBoolean("active", reward.getBoolean("earned") && blessing != null
                                && BlessingSpecializations.hasEligibleBlessing(player, definition.godId(), blessing));
                    }
                    if (!upgrades.equals(entry.getList("upgrades", Tag.TAG_COMPOUND))) changes.put("upgrades", upgrades);
                }
            }
            if (entry.getString("section").equals("favor")) {
                var id = ResourceLocation.tryParse(entry.getString("god"));
                var m = id == null ? null : data.memories().get(id);
                if (m != null) {
                    CompoundTag current = new CompoundTag();
                    current.putInt("highest", m.highestFavor); current.putInt("victories", m.victories); current.putInt("offerings", m.offerings);
                    current.putBoolean("foundation", m.favorLedger.getBoolean("foundation"));
                    var god = DivineNarrativeManager.god(id);
                    if (god != null) current.put("relationship", DivineRelationship.snapshot(player, god));
                    for (var source : net.nightshade.divinity_engine.util.divinity.gods.FavorRewards.Source.values())
                        current.putInt(source.name(), net.nightshade.divinity_engine.util.divinity.gods.FavorRewards.remaining(m.favorLedger, source, DivineNarrativeManager.now(player)));
                    for (String key : current.getAllKeys()) if (!Objects.equals(entry.get(key), current.get(key))) changes.put(key, current.get(key).copy());
                }
            }
            if (!changes.isEmpty()) {
                entry.merge(changes); changes.putString("id", entry.getString("id")); updates.add(changes);
            }
        }
        if (updates.isEmpty()) return;
        CompoundTag packet = new CompoundTag(); packet.putLong("revision", ++data.journalRevision);
        packet.putBoolean("full", false); packet.put("entries", updates); NarrativeNetwork.sync(player, packet);
    }
    private static Map<String, CompoundTag> index(CompoundTag tag) {
        Map<String, CompoundTag> result = new LinkedHashMap<>();
        for (Tag entry : tag.getList("entries", Tag.TAG_COMPOUND)) { CompoundTag value = (CompoundTag) entry; result.put(value.getString("id"), value); }
        return result;
    }
}
