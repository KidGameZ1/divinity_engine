package net.nightshade.divinity_engine.util.divinity.narrative;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.nightshade.divinity_engine.api.narrative.StoryObjectiveType;
import net.nightshade.divinity_engine.network.cap.player.narrative.DivineNarrativeCapability;
import net.nightshade.divinity_engine.util.divinity.gods.DivineRelationship;

/** On-demand conversation data. Hidden chapters and unchosen endings never enter the packet. */
public final class ShrineStoryDialogue {
    public static CompoundTag build(ServerPlayer player, ResourceLocation storyId) {
        var data = DivineNarrativeCapability.get(player);
        var registered = DivineNarrativeRegistry.byId(storyId);
        CompoundTag result = new CompoundTag();
        if (data == null || registered == null || !DivineNarrativeManager.discovered(player, registered.godId())) return result;
        var memory = data.story(registered);
        var definition = memory.definition(registered);
        result.putString("story", storyId.toString());
        result.putString("title", registered.title());
        result.putString("god", registered.godId().toString());
        ListTag actions = new ListTag();
        result.put("actions", actions);
        if (definition == null || memory.hasUnresolvedSnapshot()) {
            result.putString("text", "This story's addon is unavailable. Your progress and choices are preserved.");
            return result;
        }
        var stage = memory.story.stage();
        var path = DivineNarrativeManager.path(definition, memory);
        String chapter = memory.story.chapter();
        if (java.util.Set.of("deed", "reckoning", "ending").contains(chapter)) chapter += "_" + memory.story.branch();
        var passage = definition.chronicle().passages().get(chapter);
        StringBuilder text = new StringBuilder(NarrativeDefinitionJson.chapterTitle(memory.story.chapter()));
        if (stage == NarrativeProgress.Stage.DORMANT) text.append("\n\n").append(definition.request());
        else if (passage != null && stage != NarrativeProgress.Stage.RETURN) text.append("\n\n").append(passage.dialogue());
        if (memory.story.activeObjective() && path != null)
            text.append("\n\n").append(path.deed()).append(" [").append(memory.story.progress()).append('/').append(path.target()).append(']');
        boolean contacted = DivineNarrativeManager.contacted(player, definition.godId());
        if (!contacted) text.append("\n\nReconnect with this god at their statue to continue.");
        else switch (stage) {
            case DORMANT -> {
                if (DivineRelationship.canStartStory(player, DivineNarrativeManager.god(definition.godId())))
                    action(actions, "Begin quest", "request", "", "");
                else text.append("\n\nThis story is not available yet. Check your Bond and contact with this god.");
            }
            case OFFERING -> text.append("\n\nMake an accepted offering at this statue to continue.");
            case CHOICE -> {
                text.append("\n\nCompare both upgrades below. Both paths prove your bond and can unlock every blessing. Previewing does not choose a path; confirmation is permanent.");
                for (String branch : java.util.List.of("a", "b")) {
                    var option = definition.branches().get(branch);
                    text.append("\n\n").append(option.name()).append(": ").append(option.deed())
                        .append("\nBlessing upgrade: ").append(option.reward().description());
                    action(actions, "Preview " + option.name(), "preview", branch, "");
                }
                if (memory.previewExpires >= DivineNarrativeManager.now(player)
                        && memory.previewDefinition.equals(definition.toString()) && memory.preview.length() == 38) {
                    String branch = memory.preview.substring(37);
                    var option = definition.branches().get(branch);
                    if (option != null) {
                        text.append("\n\nPreviewing: ").append(option.name()).append(". Confirm below to lock this path permanently.");
                        action(actions, "Permanently choose " + option.name(), "confirm", branch, memory.preview.substring(0, 36));
                    }
                }
            }
            case TRIAL -> text.append("\n\nStart a fresh trial from this statue's Powers tab. Victory completes this trial chapter. Your primary story controls blessing proofs; optional stories do not bypass it. Failed trials can be retried without erasing completed deeds.");
            case RETURN -> {
                text.append("\n\nYour trial is complete. Claim your ending, shrine recognition and permanent blessing upgrade.");
                action(actions, "Claim ending and upgrade", "finish", "", "");
            }
            case COMPLETE -> {
                if (path != null) text.append("\n\nEarned upgrade: ").append(path.reward().description());
                text.append("\n\nYour full ending is recorded in the Divine Codex.");
            }
            default -> {
                if (path != null && path.objective().equals(StoryObjectiveType.DONATION))
                    action(actions, "Contribute quest items", "donate", "", "");
            }
        }
        if (contacted) for (var relation : DivineNarrativeRegistry.relationships()) {
            if (relation.gods().contains(definition.godId()) && DivineNarrativeManager.crossoverReady(player, relation)
                    && !data.epilogues.containsKey(relation.id().toString()))
                action(actions, "Hear " + relation.title(), "crossover", relation.id().toString(), "");
        }
        result.putString("text", text.toString());
        return result;
    }

    private static void action(ListTag actions, String label, String action, String branch, String token) {
        CompoundTag entry = new CompoundTag();
        entry.putString("label", label); entry.putString("action", action);
        entry.putString("branch", branch); entry.putString("token", token); actions.add(entry);
    }

    private ShrineStoryDialogue() {}
}
