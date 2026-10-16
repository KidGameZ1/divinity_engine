package net.nightshade.divinity_engine.util.divinity.gods;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.divinity.gods.BaseGod;
import net.nightshade.divinity_engine.divinity.gods.FavorTiers;
import net.nightshade.divinity_engine.network.cap.player.narrative.DivineNarrativeCapability;
import net.nightshade.divinity_engine.network.cap.player.narrative.DivineNarrativeData;
import net.nightshade.divinity_engine.util.divinity.narrative.DivineNarrativeManager;
import net.nightshade.divinity_engine.util.divinity.narrative.DivineNarrativeRegistry;
import net.nightshade.divinity_engine.util.divinity.narrative.NarrativeProgress;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Server-owned, monotonic proofs. Legacy favor is retained for saves and addon compatibility, not new unlocks. */
public final class DivineRelationship {
    public enum Proof { INTRODUCTION, WITNESS, CHOSEN_DEED, RECKONING, TRIAL_VICTORY }
    private static final Map<net.minecraft.resources.ResourceLocation, Proof> CUSTOM_PROOFS = new ConcurrentHashMap<>();
    private static final Map<BaseGod, Map<net.minecraft.resources.ResourceLocation, Integer>> ORDER = new ConcurrentHashMap<>();
    private static final List<String> PROOFS = List.of(
            "Begin this god's story at their statue and make its accepted offering.",
            "Complete this god's witness chapter.",
            "Complete your permanently chosen branch deed. Either path is valid.",
            "Complete this god's reckoning chapter.",
            "Win a fresh trial after this god's reckoning. Either story path is valid.");
    private DivineRelationship() {}

    /** Register during common setup. Addons can choose a named milestone without inventing favor scores. */
    public static void registerBlessingProof(net.minecraft.resources.ResourceLocation blessing, Proof proof) {
        java.util.Objects.requireNonNull(blessing); java.util.Objects.requireNonNull(proof);
        Proof previous = CUSTOM_PROOFS.putIfAbsent(blessing, proof);
        if (previous != null && previous != proof) throw new IllegalArgumentException("Conflicting relationship proof for " + blessing);
    }

    public static boolean manages(BaseGod god) {
        return god != null && god.getRegistryName() != null &&
                (god.getRegistryName().getNamespace().equals("divinity_engine")
                        || DivineNarrativeRegistry.story(god.getRegistryName()) != null);
    }

    public static int proofFor(NarrativeProgress.Stage stage) {
        return switch (stage) {
            case DORMANT, OFFERING -> 0;
            case WITNESS -> 1;
            case CHOICE, DEED -> 2;
            case RECKONING -> 3;
            case TRIAL -> 4;
            case RETURN, COMPLETE -> 5;
        };
    }

    /** Stable legacy ordering for original/addon kits, unless an addon supplies explicit proofs. */
    public static int order(BlessingsInstance blessing) {
        if (blessing == null || blessing.getBoundGod() == null) return 1;
        Proof registered = CUSTOM_PROOFS.get(blessing.getBlessingId());
        if (registered != null) return registered.ordinal() + 1;
        BaseGod god = blessing.getBoundGod();
        return ORDER.computeIfAbsent(god, value -> {
            var kit = value.getBlessings().stream().map(entry -> entry.get())
                    .sorted(Comparator.comparingInt(net.nightshade.divinity_engine.divinity.blessing.Blessings::getNeededFavor)
                            .thenComparing(entry -> entry.getRegistryName().toString())).toList();
            Map<net.minecraft.resources.ResourceLocation, Integer> order = new java.util.HashMap<>();
            for (int i = 0; i < kit.size(); i++) order.put(kit.get(i).getRegistryName(), Math.min(5, i + 1));
            return Map.copyOf(order);
        }).getOrDefault(blessing.getBlessingId(), 5);
    }

    public static String requirement(BlessingsInstance blessing) {
        if (blessing != null && !manages(blessing.getBoundGod())) return "Fulfill this addon's relationship requirements.";
        return PROOFS.get(order(blessing) - 1);
    }

    public static void initialize(ServerPlayer player, BaseGod god, boolean importLegacy) {
        var data = DivineNarrativeCapability.get(player);
        if (data == null || !manages(god)) return;
        var memory = data.get(god.getRegistryName());
        var relation = memory.relationship;
        if (relation.getBoolean("initialized")) return;
        relation.putBoolean("initialized", true);
        var current = GodHelper.getGodOrNull(player, god);
        if (importLegacy && current != null) {
            int previous = Math.max(current.getFavor(), memory.highestFavor);
            ListTag unlocked = new ListTag();
            for (var blessing : current.getBlessingsInstances()) {
                if (previous >= blessing.getNeededFavor()) unlocked.add(StringTag.valueOf(blessing.getBlessingId().toString()));
            }
            relation.put("legacy_unlocks", unlocked);
            relation.putBoolean("legacy_trial", previous >= 50 || memory.victories > 0);
            relation.putString("legacy_rank", previous >= 176 ? "Champion" : previous >= 101 ? "Devoted" : previous >= 50 ? "Favorable" : "Contacted");
        }
        refresh(player, god);
    }

    public static void onContact(ServerPlayer player, BaseGod god) {
        var data = DivineNarrativeCapability.get(player);
        if (data == null || god == null) return;
        var old = data.memories().get(god.getRegistryName());
        initialize(player, god, old != null && old.contacts > 0);
    }

    public static void migrate(ServerPlayer player) {
        for (var god : GodHelper.getContactedGodsFrom(player).getContactedGods()) initialize(player, god.getBaseGod(), true);
    }

    public static void refresh(ServerPlayer player, BaseGod god) {
        var data = DivineNarrativeCapability.get(player);
        if (data == null || !manages(god)) return;
        var memory = data.get(god.getRegistryName());
        var definition = DivineNarrativeRegistry.story(god.getRegistryName());
        var story = definition == null ? null : data.findStory(definition);
        if (story != null) memory.relationship.putInt("proof", Math.max(memory.relationship.getInt("proof"), proofFor(story.story.stage())));
    }

    private static DivineNarrativeData.Memory memory(LivingEntity entity, BaseGod god) {
        if (!(entity instanceof ServerPlayer player) || !manages(god)) return null;
        var data = DivineNarrativeCapability.get(player);
        if (data == null) return null;
        initialize(player, god, true);
        refresh(player, god);
        return data.get(god.getRegistryName());
    }

    public static boolean canUse(LivingEntity entity, BlessingsInstance blessing) {
        if (entity == null || blessing == null || blessing.getBoundGod() == null) return false;
        var god = GodHelper.getGodOrNull(entity, blessing.getBoundGod());
        if (god == null || god.getFavor() < 0) return false;
        if (!manages(god.getBaseGod())) return god.getFavor() >= blessing.getNeededFavor();
        var memory = memory(entity, god.getBaseGod());
        return memory != null && (memory.relationship.getInt("proof") >= order(blessing)
                || memory.relationship.getList("legacy_unlocks", Tag.TAG_STRING).contains(StringTag.valueOf(blessing.getBlessingId().toString())));
    }

    public static boolean canStartStory(LivingEntity entity, BaseGod god) {
        var contacted = GodHelper.getGodOrNull(entity, god);
        return contacted != null && (manages(god) || contacted.getFavor() >= 50);
    }

    public static boolean canStartTrial(LivingEntity entity, BaseGod god) {
        var contacted = GodHelper.getGodOrNull(entity, god);
        if (contacted == null || contacted.getFavor() < 0) return false;
        if (!manages(god)) return true;
        var memory = memory(entity, god);
        return memory != null && (memory.relationship.getInt("proof") >= 4 || memory.relationship.getBoolean("legacy_trial"));
    }

    public static String rank(LivingEntity entity, BaseGod god) {
        var current = GodHelper.getGodOrNull(entity, god);
        if (current == null) return "Disconnected";
        if (!manages(god)) {
            FavorTiers legacy = FavorTiers.getByFavor(Math.max(FavorTiers.Enemy.getMinFavor(),
                    Math.min(FavorTiers.Champion.getMaxFavor(), current.getFavor())));
            return legacy == null ? "Contacted" : legacy.name();
        }
        if (current.getFavor() < -100) return "Enemy";
        if (current.getFavor() < 0) return "Displeased";
        var memory = memory(entity, god);
        if (memory == null) return "Contacted";
        int proof = memory.relationship.getInt("proof");
        String legacy = memory.relationship.getString("legacy_rank");
        if (proof >= 5 || legacy.equals("Champion")) return "Champion";
        if (proof >= 3 || legacy.equals("Devoted")) return "Devoted";
        if (proof >= 1 || legacy.equals("Favorable")) return "Favorable";
        return "Contacted";
    }

    /** A compact journal view; full story text continues to use the existing journal pipeline. */
    public static CompoundTag snapshot(ServerPlayer player, BaseGod god) {
        CompoundTag tag = new CompoundTag();
        tag.putString("rank", rank(player, god));
        tag.putBoolean("trial_unlocked", canStartTrial(player, god));
        ListTag unlocked = new ListTag(), memories = new ListTag();
        CompoundTag requirements = new CompoundTag();
        var current = GodHelper.getGodOrNull(player, god);
        if (current != null) for (var blessing : current.getBlessingsInstances()) {
            if (canUse(player, blessing)) unlocked.add(StringTag.valueOf(blessing.getBlessingId().toString()));
            requirements.putString(blessing.getBlessingId().toString(), requirement(blessing));
        }
        tag.put("unlocked_blessings", unlocked); tag.put("requirements", requirements);
        var memory = memory(player, god);
        String next = "Reconnect at this god's statue. Your deeds and permanent choices are preserved.";
        if (current != null && memory != null) {
            int proof = memory.relationship.getInt("proof");
            next = proof < 5 ? PROOFS.get(Math.max(0, proof)) : "Return to the statue to claim your ending and permanent blessing specialization.";
            if (memory.story.stage() == NarrativeProgress.Stage.COMPLETE) next = "Your story is complete. Your god remembers your chosen ending.";
            if (current.getFavor() < 0) next = "Make an accepted offering at the statue to reconcile. Earned deeds are not lost.";
            if (!memory.relationship.getList("legacy_unlocks", Tag.TAG_STRING).isEmpty()) memories.add(StringTag.valueOf("Your previously earned blessings remain yours."));
            if (memory.prayers > 0) memories.add(StringTag.valueOf("Remembers your prayers."));
            if (memory.offerings > 0) memories.add(StringTag.valueOf("Remembers your accepted offerings."));
            if (memory.abandonments > 0) memories.add(StringTag.valueOf("Remembers your departure, and your return."));
            if (memory.failures > 0) memories.add(StringTag.valueOf("Remembers your resolve after a failed trial. You may always retry."));
            if (memory.victories > 0) memories.add(StringTag.valueOf("Acknowledges your trial victory."));
            if (!memory.story.branch().isEmpty()) memories.add(StringTag.valueOf("Honors your permanently chosen path."));
        }
        tag.putString("next_action", next); tag.put("memories", memories);
        return tag;
    }
}
