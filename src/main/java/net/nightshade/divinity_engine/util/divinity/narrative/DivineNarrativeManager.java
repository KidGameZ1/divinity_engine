package net.nightshade.divinity_engine.util.divinity.narrative;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.registries.ForgeRegistries;
import net.nightshade.divinity_engine.api.narrative.*;
import net.nightshade.divinity_engine.block.StatueBlock;
import net.nightshade.divinity_engine.divinity.gods.BaseGod;
import net.nightshade.divinity_engine.divinity.gods.FavorTiers;
import net.nightshade.divinity_engine.network.cap.player.narrative.*;
import net.nightshade.divinity_engine.network.events.divinity.narrative.NarrativeDonationEvent;
import net.nightshade.divinity_engine.network.messages.narrative.NarrativeNetwork;
import net.nightshade.divinity_engine.registry.divinity.gods.GodsRegistry;
import net.nightshade.divinity_engine.util.divinity.gods.GodHelper;
import net.nightshade.divinity_engine.util.divinity.gods.DivineRelationship;
import java.util.*;
import java.util.function.BiConsumer;

import static net.nightshade.divinity_engine.util.divinity.narrative.NarrativeProgress.Stage;

/** All mutations run on the server thread; persisted memory never lives in the contacted-god instance. */
public final class DivineNarrativeManager {
    private DivineNarrativeManager() {}
    public static boolean hasSpecialization(ServerPlayer player, ResourceLocation godId, String branch) {
        var data = DivineNarrativeCapability.get(player);
        if (data == null) return false;
        var definition = DivineNarrativeRegistry.story(godId);
        var memory = definition == null ? data.memories().get(godId) : data.findStory(definition);
        if (memory == null) return false;
        if (!memory.story.branch().equals(branch)) return false;
        var snapshot = memory.resolveSnapshot();
        // Older earned records may lack a snapshot. Their saved reward ID remains authoritative.
        ResourceLocation reward = snapshot == null ? ResourceLocation.tryParse(memory.story.specialization()) : snapshot.reward().id();
        return hasEarnedReward(memory, reward);
    }
    /** Addon-facing query for any stable, namespaced reward ID, independent of its display name or god path. */
    public static boolean hasSpecialization(ServerPlayer player, ResourceLocation specializationId) {
        var data = DivineNarrativeCapability.get(player);
        return data != null && data.allMemories().stream().anyMatch(memory -> hasEarnedReward(memory, specializationId));
    }
    private static boolean hasEarnedReward(DivineNarrativeData.Memory memory, ResourceLocation reward) {
        var story = memory.story;
        if (reward == null || story.stage() != Stage.COMPLETE || story.branch().isEmpty()
            || !story.specialization().equals(reward.toString())) return false;
        var snapshot = memory.resolveSnapshot();
        return snapshot == null || snapshot.reward().id().equals(reward);
    }
    public static BaseGod god(ResourceLocation id) { return GodsRegistry.GODS_REGISTRY.get().getValue(id); }
    public static boolean contacted(ServerPlayer player, ResourceLocation id) {
        BaseGod god = god(id); return god != null && GodHelper.hasContactedGod(player, god);
    }
    public static long now(ServerPlayer player) { return player.server.overworld().getGameTime(); }
    public static BlockPos nearestStatue(ServerPlayer player, ResourceLocation godId) {
        if (net.nightshade.divinity_engine.util.divinity.innerworld.CommunionManager.inRealm(player)) return null;
        BlockPos origin = player.blockPosition(), nearest = null; double distance = 36.0001;
        // Statues are block entities. Search the loaded chunk indexes, not 2,197 terrain positions per shrine callback.
        for (int x = (origin.getX() - 6) >> 4; x <= (origin.getX() + 6) >> 4; x++) {
            for (int z = (origin.getZ() - 6) >> 4; z <= (origin.getZ() + 6) >> 4; z++) {
                var chunk = player.serverLevel().getChunkSource().getChunkNow(x, z);
                if (chunk == null) continue;
                // Includes pending saved block entities without creating them or loading another chunk.
                for (BlockPos pos : chunk.getBlockEntitiesPos()) {
                    if (Math.abs(pos.getX() - origin.getX()) > 6 || Math.abs(pos.getY() - origin.getY()) > 6
                            || Math.abs(pos.getZ() - origin.getZ()) > 6) continue;
                    double d = player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                    if (d > distance) continue;
                    // Keep the old scan's tie order, independent of the chunk index's iteration order.
                    if (d == distance && nearest != null && (pos.getZ() < nearest.getZ()
                            || pos.getZ() == nearest.getZ() && (pos.getY() < nearest.getY()
                            || pos.getY() == nearest.getY() && pos.getX() < nearest.getX()))) continue;
                    if (chunk.getBlockState(pos).getBlock() instanceof StatueBlock statue && statue.getGod() != null
                            && (godId == null || godId.equals(statue.getGod().getRegistryName()))) {
                        nearest = pos.immutable(); distance = d;
                    }
                }
            }
        }
        return nearest;
    }
    public static void onContact(ServerPlayer player, BaseGod god, boolean contact) {
        var data = DivineNarrativeCapability.get(player); if (data == null || god.getRegistryName() == null) return;
        if (contact) DivineRelationship.onContact(player, god);
        var m = data.get(god.getRegistryName());
        if (contact) {
            if (!m.contacted) { m.contacts++; m.contacted = true; }
            say(player, god.getRegistryName(), m.abandonments > 0 ? "return" : "greeting", false);
        } else if (m.contacted) {
            m.contacted = false; m.abandonments++; m.interrupt();
            for (var definition : DivineNarrativeRegistry.forGod(god.getRegistryName())) {
                var story = data.findStory(definition);
                if (story != null && story != m) story.interrupt();
            }
        }
        sync(player);
    }
    public static void onPrayer(ServerPlayer player, BaseGod god, BlockPos pos, int duration) {
        var data = DivineNarrativeCapability.get(player); if (data == null) return;
        for (var def : DivineNarrativeRegistry.forGod(god.getRegistryName())) {
            var m = data.story(def); m.prayerSeen = now(player);
            m.shrinePosition = pos.immutable(); m.shrineDimension = player.level().dimension().location().toString();
        }
        if (duration % 20 == 0) { data.get(god.getRegistryName()).prayers++; say(player, god.getRegistryName(), "prayer", false); }
    }
    public static void onAcceptedOffering(ServerPlayer player, BaseGod god) {
        var data = DivineNarrativeCapability.get(player); if (data == null) return;
        var contacted = GodHelper.getGodOrNull(player, god);
        if (DivineRelationship.manages(god) && contacted != null && contacted.getFavor() < 0) {
            contacted.setFavor(contacted.getFavor() < -100 ? -1 : 0);
            tell(player, god.getRegistryName(), contacted.getFavor() < 0
                    ? "Your offering is accepted. Our estrangement eases; return with another sincere offering to reconcile."
                    : "Our bond is restored. Your deeds and earned blessings were never forgotten.");
        }
        data.get(god.getRegistryName()).offerings++;
        boolean progressed = false;
        for (var def : DivineNarrativeRegistry.forGod(god.getRegistryName())) {
            var m = data.findStory(def);
            if (m != null && m.definition(def) != null && m.story.acceptOffering(true)) {
                chapterChanged(player, m.definition(def), m); progressed = true;
            }
        }
        if (!progressed) say(player, god.getRegistryName(), "offering", false);
        sync(player);
    }
    public static void nearShrine(ServerPlayer player, BaseGod god, BlockPos pos) {
        var data = DivineNarrativeCapability.get(player); ResourceLocation id = god.getRegistryName();
        if (data == null || id == null || !pos.equals(nearestStatue(player, id))) return;
        var m = data.get(id);
        for (var def : DivineNarrativeRegistry.forGod(id)) {
            var memory = data.story(def); memory.nearSeen = now(player);
            memory.shrinePosition = pos.immutable(); memory.shrineDimension = player.level().dimension().location().toString();
            if (memory.story.activeObjective() && memory.reachedHeight) {
                var path = path(def, memory);
                if (path != null && path.objective().equals(StoryObjectiveType.HEIGHT)) advance(player, def.id(), path.target());
            }
        }
        if (m.story.stage() == Stage.COMPLETE) say(player, id, "recognition", false);
        else if (m.story.stage() == Stage.TRIAL && m.failures >= 3) say(player, id, "failure", false);
        else say(player, id, "greeting", false);
    }
    public static void onTrialStarted(ServerPlayer player, ResourceLocation god, UUID session) {
        var data = DivineNarrativeCapability.get(player);
        if (data == null || !contacted(player, god)) return;
        for (var def : DivineNarrativeRegistry.forGod(god)) {
            var m = data.findStory(def); if (m != null && path(def, m) != null) m.story.trialStarted(session.toString());
        }
    }
    public static void interruptPrayer(ServerPlayer player, BaseGod god) {
        var data = DivineNarrativeCapability.get(player); if (data == null) return;
        for (var def : DivineNarrativeRegistry.forGod(god.getRegistryName())) {
            var memory = data.findStory(def); if (memory == null) continue; memory.prayerSeen = -1;
            var branch = path(def, memory);
            if (branch != null && branch.objective().equals(StoryObjectiveType.PRAY)) memory.resetAttempt();
        }
    }
    public static boolean atRememberedShrine(ServerPlayer player, ResourceLocation godId, DivineNarrativeData.Memory memory) {
        BlockPos pos = memory.shrinePosition;
        if (pos == null || !player.level().dimension().location().toString().equals(memory.shrineDimension)
            || !player.serverLevel().hasChunkAt(pos) || player.position().distanceToSqr(Vec3.atCenterOf(pos)) > 36) return false;
        return player.serverLevel().getBlockState(pos).getBlock() instanceof StatueBlock statue
            && statue.getGod() != null && godId.equals(statue.getGod().getRegistryName());
    }
    public static void onTrialOutcome(ServerPlayer player, ResourceLocation god, UUID session, boolean victory) {
        var data = DivineNarrativeCapability.get(player); if (data == null) return;
        var m = data.get(god);
        if (m.story.outcomes().contains(session.toString())) return;
        boolean progressed = false;
        for (var def : DivineNarrativeRegistry.forGod(god)) {
            var memory = data.findStory(def); if (memory == null) continue;
            Stage previous = memory.story.stage();
            memory.story.trialOutcome(session.toString(), victory && contacted(player, god) && path(def, memory) != null);
            if (previous != memory.story.stage()) { chapterChanged(player, def, memory); progressed = true; }
        }
        if (!m.story.outcomes().contains(session.toString())) m.story.trialOutcome(session.toString(), false);
        if (victory) m.victories++; else m.failures++;
        if (!progressed) say(player, god, victory ? "victory" : "failure", false);
        sync(player);
    }
    public static void onStatueOpened(ServerPlayer player, BaseGod god) {
        var data = DivineNarrativeCapability.get(player); if (data == null || god == null) return;
        var instance = GodHelper.getGodOrNull(player, god); if (instance == null) return;
        ResourceLocation godId = god.getRegistryName();
        for (var def : DivineNarrativeRegistry.forGod(godId)) data.story(def);
        sync(player);
    }
    public static int action(ServerPlayer player, ResourceLocation id, String action, String branch, String token) {
        return action(player, id, action, branch, token, false);
    }
    public static int communionAction(ServerPlayer player, ResourceLocation id, String action, String branch, String token) {
        if (!net.nightshade.divinity_engine.util.divinity.innerworld.CommunionManager.permitsStory(player, id)
                || !java.util.Set.of("request", "preview", "confirm", "status", "lore").contains(action)) return 0;
        return action(player, id, action, branch, token, true);
    }
    private static int action(ServerPlayer player, ResourceLocation id, String action, String branch, String token, boolean communion) {
        requireServerThread(player);
        if (action.equals("crossover")) return crossover(player, id);
        var data = DivineNarrativeCapability.get(player); var def = DivineNarrativeRegistry.story(id);
        if (def == null) return error(player, "No story is registered for that god.");
        if (data == null) return error(player, "Your story data is unavailable. Rejoin the world to reload it.");
        var m = data.story(def); def = m.definition(def);
        if (def == null || m.hasUnresolvedSnapshot()) return error(player, "The saved story's addon is unavailable; progress is preserved.");
        id = def.id(); ResourceLocation godId = def.godId();
        if (action.equals("status") || action.equals("lore")) { status(player, id, action.equals("lore")); return 1; }
        BlockPos shrine = nearestStatue(player, godId);
        if (!contacted(player, godId) || shrine == null && !communion) return error(player, "Stand within six blocks of a loaded matching statue and reconnect with this god.");
        switch (action) {
            case "request" -> {
                var instance = GodHelper.getGodOrNull(player, god(godId));
                if (!m.story.request(DivineRelationship.canStartStory(player, god(godId)), def.chronicle().expanded())) return error(player, "Contact this god at their statue first. A story can only be requested once.");
                m.snapshot(def);
                m.invalidatePending();
                chapterChanged(player, def, m);
            }
            case "preview" -> {
                if (m.story.stage() != Stage.CHOICE || !def.branches().containsKey(branch)) return error(player, "That choice is not available.");
                choices(player, id);
                m.preview = UUID.randomUUID().toString() + ":" + branch; m.previewExpires = now(player) + 1200;
                m.previewDefinition = def.toString();
                var path = def.branches().get(branch);
                tell(player, id, "Confirm " + path.name() + "? This branch is permanent. " + path.deed() + "; then win my trial and return.");
            }
            case "confirm" -> {
                boolean previewed = m.previewExpires >= now(player) && m.preview.equals(token + ":" + branch) && m.previewDefinition.equals(def.toString());
                if (!m.story.confirm(branch, true, previewed)) return error(player, "This confirmation is stale or your branch is already locked. Preview again if no branch was chosen.");
                m.invalidatePending();
                m.preview = ""; m.attemptStart = -1;
                m.branchSnapshot = def.branches().get(branch);
                var path = def.branches().get(branch);
                tell(player, id, path.name() + " is now your permanent path. " + path.deed() + ".");
                chapterChanged(player, def, m);
            }
            case "donate" -> { if (!donate(player, id, def, m)) return 0; }
            case "finish" -> {
                var path = path(def, m);
                if (path == null || !m.story.finish(path.reward().id().toString(), true)) return error(player, "Complete your branch deed and a fresh trial victory before returning for the ending.");
                m.invalidatePending();
                // Set the permanent reward marker first, so repeated requests or reentrant hooks cannot award twice.
                GodHelper.increaseFavor(god(godId), player, 25);
                var ending = def.chronicle().passages().get("ending_" + m.story.branch());
                tell(player, id, ending == null ? path.ending() : ending.dialogue()); tell(player, id, "Your bond and ending are recorded in the Codex. " + path.reward().description() + ".");
                DivineRelationship.refresh(player, god(godId));
                var presence = DivineNarrativeRegistry.presence(godId); if (presence != null) CorePresenceEffects.pulse(player.serverLevel(), shrine, presence);
            }
            default -> { return error(player, "Unknown story action."); }
        }
        sync(player); return 1;
    }
    private static boolean donate(ServerPlayer player, ResourceLocation id, DivineStoryDefinition def, DivineNarrativeData.Memory m) {
        if (m.donationInProgress) return false;
        m.donationInProgress = true;
        try {
            var path = path(def, m);
            if (!m.story.activeObjective() || path == null || !path.objective().equals(StoryObjectiveType.DONATION)) { error(player, "There is no active donation chapter."); return false; }
            Item item = ForgeRegistries.ITEMS.getValue(path.donationItem());
            if (item == null || item == net.minecraft.world.item.Items.AIR) { error(player, "The story donation item is unavailable."); return false; }
            int requested = path.target() - m.story.progress();
            String confirmedBranch = m.story.branch();
            int previousProgress = m.story.progress();
            Stage previousStage = m.story.stage();
            long generation = m.generation();
            if (donationSlots(player, item, requested) == null) { error(player, "Bring exactly the requested contribution: " + requested + " " + item.getDescription().getString() + "."); return false; }
            if (MinecraftForge.EVENT_BUS.post(new NarrativeDonationEvent(player, id, item, requested))) return false;
            var currentData = DivineNarrativeCapability.get(player);
            if (currentData == null || currentData.findStory(def) != m || m.definition(DivineNarrativeRegistry.byId(def.id())) != def
                || m.story.stage() != previousStage || !m.story.branch().equals(confirmedBranch)
                || m.story.progress() != previousProgress || m.generation() != generation || !Objects.equals(path(def, m), path)
                || !contacted(player, def.godId()) || nearestStatue(player, def.godId()) == null) return false;
            int[] slots = donationSlots(player, item, requested); if (slots == null) return false;
            // No public callbacks occur between committing the deed and consuming the verified slot counts.
            if (!m.story.advance(requested, path.target(), true)) return false;
            m.invalidatePending();
            for (int i = 0; i < slots.length; i++) if (slots[i] > 0) player.getInventory().getItem(i).shrink(slots[i]);
            player.getInventory().setChanged(); player.containerMenu.broadcastChanges();
            if (m.story.stage() != previousStage) GodHelper.increaseFavor(god(def.godId()), player, 5);
            chapterChanged(player, def, m);
            sync(player);
            return true;
        } finally { m.donationInProgress = false; }
    }
    private static int[] donationSlots(ServerPlayer player, Item item, int requested) {
        int[] counts = new int[player.getInventory().getContainerSize()];
        for (int i = 0; i < counts.length; i++) if (player.getInventory().getItem(i).is(item)) counts[i] = player.getInventory().getItem(i).getCount();
        return DonationPlan.plan(counts, requested);
    }
    public static void advance(ServerPlayer player, ResourceLocation id, int amount) {
        requireServerThread(player);
        var data = DivineNarrativeCapability.get(player); var def = DivineNarrativeRegistry.story(id);
        if (data == null || def == null || !contacted(player, def.godId())) return;
        var m = data.findStory(def); if (m == null) return;
        def = m.definition(def); if (def == null) return;
        var path = path(def, m); if (path == null) return;
        Stage previous = m.story.stage();
        if (m.story.advance(amount, path.target(), true)) {
            if (m.story.stage() != previous) {
                // Only real objective transitions earn this; retries, sync and old-save migration do not.
                GodHelper.increaseFavor(god(def.godId()), player, 5);
                chapterChanged(player, def, m); sync(player);
            }
        }
    }
    public static DivineStoryDefinition.Branch path(DivineStoryDefinition definition, DivineNarrativeData.Memory memory) {
        if (memory.hasUnresolvedSnapshot()) return null;
        definition = memory.definition(definition); if (definition == null) return null;
        StoryObjective objective = switch (memory.story.stage()) {
            case WITNESS -> definition.chronicle().witness();
            case RECKONING -> definition.chronicle().reckoning();
            default -> null;
        };
        if (objective != null) return new DivineStoryDefinition.Branch(NarrativeDefinitionJson.chapterTitle(memory.story.chapter()),
            objective.type(), objective.condition(), objective.target(), objective.item(), objective.description(), definition.branches().get("a").reward(), "");
        return memory.branchSnapshot != null ? memory.branchSnapshot : definition.branches().get(memory.story.branch());
    }
    /** Trusted server-only addon hook; never called from a packet carrying progress values. */
    public static void recordObjective(ServerPlayer player, StoryObjectiveType type, String condition, int amount) {
        requireServerThread(player);
        forActive(player, (def, memory) -> { var branch = path(def, memory);
            if (branch.objective().equals(type) && branch.condition().equals(condition)) advance(player, def.id(), amount);
        });
    }
    public static void forActive(ServerPlayer player, BiConsumer<DivineStoryDefinition, DivineNarrativeData.Memory> consumer) {
        var data = DivineNarrativeCapability.get(player); if (data == null) return;
        for (var def : DivineNarrativeRegistry.stories()) {
            var memory = data.findStory(def);
            if (memory == null || !memory.story.activeObjective() || !contacted(player, def.godId())) continue;
            if (path(def, memory) != null) consumer.accept(def, memory);
        }
    }
    public static void status(ServerPlayer player, ResourceLocation id, boolean lore) {
        var data = DivineNarrativeCapability.get(player); var def = DivineNarrativeRegistry.story(id); if (data == null || def == null) return;
        if (!discovered(player, def.godId())) { error(player, "Make contact at this god's statue to discover their story."); return; }
        var m = data.story(def); var path = path(def, m); def = m.definition(def);
        if (def == null || m.hasUnresolvedSnapshot()) {
            tell(player, id, "Paused because the saved story's addon definition is unavailable. Your original choice and data are preserved.");
            return;
        }
        if (lore) { tell(player, id, "Read your discovered chapters and earned endings in the Divine Codex's Chronicle."); sync(player, true); return; }
        tell(player, id, def.title() + ": " + NarrativeDefinitionJson.chapterTitle(m.story.chapter()) + (contacted(player, def.godId()) ? "" : " (paused: reconnect at a statue)"));
        if (path != null && m.story.activeObjective()) tell(player, id, path.deed() + " [" + m.story.progress() + "/" + path.target() + "].");
        tell(player, id, "Our bond: " + DivineRelationship.rank(player, god(def.godId())) + ". Your deeds, trial attempts, and returns are remembered.");
        if (m.story.activeObjective() && path != null && path.objective().equals(StoryObjectiveType.DONATION))
            tell(player, id, "Use Contribute quest items in my statue's Quests tab.");
        if (m.story.stage() == Stage.CHOICE) choices(player, id);
        if (m.story.stage() == Stage.RETURN) tell(player, id, "Use Claim ending and upgrade in my statue's Quests tab.");
    }
    private static void choices(ServerPlayer player, ResourceLocation id) {
        var def = DivineNarrativeRegistry.story(id); if (def == null) return;
        var data = DivineNarrativeCapability.get(player); if (data == null) return;
        def = data.story(def).definition(def); if (def == null) return;
        for (String key : List.of("a", "b")) { var path = def.branches().get(key);
            tell(player, id, path.name() + ": " + path.deed() + " -> " + path.reward().description());
        }
    }
    public static boolean say(ServerPlayer player, ResourceLocation id, String kind, boolean explicit) {
        var data = DivineNarrativeCapability.get(player); var presence = DivineNarrativeRegistry.presence(id);
        if (data == null || presence == null) return false;
        var m = data.get(id); long now = now(player);
        if (!explicit && (now < data.nextAmbientTick || now < m.nextDialogueTick)) return false;
        List<String> lines = presence.dialogue().getOrDefault(kind, presence.dialogue().get("greeting"));
        if (lines == null || lines.isEmpty()) return false;
        int index = player.getRandom().nextInt(lines.size());
        if (lines.size() > 1 && (kind + ":" + index).equals(m.lastDialogue)) index = (index + 1) % lines.size();
        m.lastDialogue = kind + ":" + index;
        if (!explicit) { data.nextAmbientTick = now + 1200; m.nextDialogueTick = now + 6000; }
        tell(player, id, lines.get(index));
        BlockPos shrine = nearestStatue(player, id);
        if (shrine != null) {
            CorePresenceEffects.pulse(player.serverLevel(), shrine, presence);
            var sound = ForgeRegistries.SOUND_EVENTS.getValue(presence.sound());
            if (sound != null) player.playNotifySound(sound, SoundSource.BLOCKS, 0.35F, 0.9F + player.getRandom().nextFloat() * 0.2F);
        }
        return true;
    }
    public static void tell(ServerPlayer player, ResourceLocation id, String line) {
        var definition = DivineNarrativeRegistry.byId(id); if (definition != null) id = definition.godId();
        BaseGod god = god(id);
        var name = god == null ? Component.literal(id.getPath()) : Component.translatable(god.getNameTranslationKey());
        var presence = DivineNarrativeRegistry.presence(id);
        player.sendSystemMessage(name.copy().append(": ").append(NarrativeText.component(line))
            .withStyle(style -> style.withColor(presence == null ? 0xDDDDDD : presence.color())));
    }
    private static int error(ServerPlayer player, String text) { player.sendSystemMessage(Component.literal(text).withStyle(ChatFormatting.RED)); return 0; }
    public static void sync(ServerPlayer player) { sync(player, false); }
    public static void sync(ServerPlayer player, boolean force) {
        var data = DivineNarrativeCapability.get(player); if (data == null) return;
        NarrativeJournal.sync(player, data, force);
    }
    public static boolean discovered(ServerPlayer player, ResourceLocation god) {
        var data = DivineNarrativeCapability.get(player); if (data == null) return false;
        if (contacted(player, god)) { var saved = data.get(god); saved.contacts = Math.max(1, saved.contacts); saved.contacted = true; return true; }
        var memory = data.memories().get(god);
        return memory != null && (memory.contacts > 0 || memory.contacted || memory.story.stage() != Stage.DORMANT);
    }
    private static void chapterChanged(ServerPlayer player, DivineStoryDefinition def, DivineNarrativeData.Memory memory) {
        def = memory.definition(def); if (def == null) return;
        DivineRelationship.refresh(player, god(def.godId()));
        memory.invalidatePending(); memory.attemptStart = -1; memory.reachedHeight = false;
        memory.placements.clear(); memory.distanceRemainder = 0;
        var path = path(def, memory);
        memory.resettableAttempt = memory.story.activeObjective() && path != null && (path.objective().continuous() || path.objective().timeLimitTicks() > 0);
        String key = memory.story.chapter();
        if (Set.of("deed", "reckoning", "ending").contains(key)) key += "_" + memory.story.branch();
        var passage = def.chronicle().passages().get(key);
        if (memory.story.stage() != Stage.RETURN && passage != null) tell(player, def.id(), passage.dialogue());
        switch (memory.story.stage()) {
            case OFFERING -> tell(player, def.id(), "Make an ordinary offering that this god accepts. Your story is in the Divine Codex.");
            case CHOICE -> choices(player, def.id());
            case TRIAL -> tell(player, def.id(), "Begin and win a NEW trial from the blessings screen opened at my statue.");
            case RETURN -> tell(player, def.id(), "Return to my statue to hear the ending and receive your permanent reward.");
            default -> {
                if (memory.story.activeObjective() && path != null) {
                    tell(player, def.id(), path.deed());
                    if (path.objective().equals(StoryObjectiveType.DONATION))
                        tell(player, def.id(), "Use Contribute quest items in my statue's Quests tab.");
                }
            }
        }
    }
    public static boolean crossoverReady(ServerPlayer player, GodRelationshipDefinition relation) {
        var data = DivineNarrativeCapability.get(player); if (data == null) return false;
        for (ResourceLocation id : relation.stories()) {
            var def = DivineNarrativeRegistry.byId(id); if (def == null) return false;
            var memory = data.findStory(def); if (memory == null || memory.story.stage() != Stage.COMPLETE) return false;
        }
        return true;
    }
    private static int crossover(ServerPlayer player, ResourceLocation id) {
        var data = DivineNarrativeCapability.get(player); var relation = DivineNarrativeRegistry.relationship(id);
        if (data == null || relation == null || !crossoverReady(player, relation)) return error(player, "Complete both stories before hearing this optional epilogue.");
        if (relation.gods().stream().noneMatch(god -> contacted(player, god) && nearestStatue(player, god) != null)) return error(player, "Return to either related god's loaded statue within six blocks.");
        String key = relation.stories().stream().map(story -> data.findStory(DivineNarrativeRegistry.byId(story)).story.branch()).reduce("", String::concat);
        if (data.epilogues.putIfAbsent(id.toString(), String.join("\n\n", relation.epilogues().get(key))) != null) return error(player, "This epilogue is already recorded in your Codex.");
        player.sendSystemMessage(NarrativeText.component(relation.title() + ": a new epilogue has been recorded in your Divine Codex."));
        sync(player); return 1;
    }
    private static void requireServerThread(ServerPlayer player) { if (!player.server.isSameThread()) throw new IllegalStateException("Narrative progress must be reported on the server thread"); }
}
