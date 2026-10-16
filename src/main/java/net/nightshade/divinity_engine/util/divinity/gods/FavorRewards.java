package net.nightshade.divinity_engine.util.divinity.gods;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.nightshade.divinity_engine.divinity.gods.BaseGod;
import net.nightshade.divinity_engine.divinity.gods.BaseGodInstance;
import net.nightshade.divinity_engine.network.cap.player.narrative.DivineNarrativeCapability;
import java.util.function.BooleanSupplier;

/** Server-side worship budgets. Saved independently of contact, so reconnecting cannot reset them. */
public final class FavorRewards {
    public enum Source {
        PRAYER(2, 12, 200), OFFERING(12, 30, 0), PRESENCE(1, 3, 1200);
        public final int perAction, dailyLimit, interval;
        Source(int perAction, int dailyLimit, int interval) {
            this.perAction = perAction; this.dailyLimit = dailyLimit; this.interval = interval;
        }
    }
    public static final int WINDOW_TICKS = 24000;
    private static final ThreadLocal<Pending> PENDING = new ThreadLocal<>();
    private static final class Pending {
        final LivingEntity player; final BaseGod god; int amount;
        Pending(LivingEntity player, BaseGod god) { this.player = player; this.god = god; }
    }
    private FavorRewards() {}

    /** Aggregate a god's callbacks; rejected offerings never receive captured favor. */
    public static boolean interaction(BaseGodInstance god, LivingEntity player, Source source, BooleanSupplier action) {
        if (net.nightshade.divinity_engine.util.divinity.innerworld.CommunionManager.inRealm(player)) return false;
        if (player.level().isClientSide) return false;
        Pending previous = PENDING.get(), current = new Pending(player, god.getBaseGod());
        PENDING.set(current);
        boolean accepted;
        try { accepted = action.getAsBoolean(); }
        finally { if (previous == null) PENDING.remove(); else PENDING.set(previous); }
        if (accepted && current.amount > 0) grant(player, god.getBaseGod(), source, current.amount);
        return accepted;
    }
    public static boolean capture(BaseGod god, LivingEntity player, int amount) {
        Pending pending = PENDING.get();
        if (pending == null || pending.player != player || pending.god != god) return false;
        pending.amount = (int) Math.min(Integer.MAX_VALUE, (long) pending.amount + Math.max(0, amount));
        return true;
    }
    public static int grant(LivingEntity entity, BaseGod god, Source source, int requested) {
        if (net.nightshade.divinity_engine.util.divinity.innerworld.CommunionManager.inRealm(entity)) return 0;
        if (!(entity instanceof ServerPlayer player) || !player.server.isSameThread()) return 0;
        var data = DivineNarrativeCapability.get(player);
        var instance = GodHelper.getGodOrNull(player, god);
        if (DivineRelationship.manages(god)) return 0;
        if (data == null || instance == null || requested <= 0 || instance.getFavor() >= 250) return 0;
        var ledger = data.get(god.getRegistryName()).favorLedger;
        long now = player.server.overworld().getGameTime();
        int earned = allowance(ledger, source, requested, now);
        if (earned > 0) GodHelper.increaseFavor(god, player, earned);
        return earned;
    }
    public static int allowance(CompoundTag ledger, Source source, int requested, long now) {
        if (requested <= 0) return 0;
        resetWindow(ledger, now);
        String key = source.name();
        if (ledger.contains(key + "_next") && now < ledger.getLong(key + "_next")) return 0;
        int used = ledger.getInt(key);
        // The second half of an offering budget has diminishing returns, including large stacks.
        int amount = Math.min(requested, source.perAction);
        if (source == Source.OFFERING && used >= source.dailyLimit / 2) amount = Math.max(1, amount / 2);
        amount = Math.max(0, Math.min(amount, source.dailyLimit - used));
        if (amount == 0) return 0;
        ledger.putInt(key, used + amount);
        ledger.putLong(key + "_next", now + source.interval);
        return amount;
    }
    private static void resetWindow(CompoundTag ledger, long now) {
        if (!ledger.contains("window") || now - ledger.getLong("window") >= WINDOW_TICKS) {
            ledger.putLong("window", now);
            for (Source source : Source.values()) { ledger.remove(source.name()); ledger.remove(source.name() + "_next"); }
        }
    }
    public static int remaining(CompoundTag ledger, Source source, long now) {
        return !ledger.contains("window") || now - ledger.getLong("window") >= WINDOW_TICKS
                ? source.dailyLimit : Math.max(0, source.dailyLimit - ledger.getInt(source.name()));
    }
    public static void foundation(LivingEntity entity, BaseGod god) {
        if (!(entity instanceof ServerPlayer player)) return;
        var data = DivineNarrativeCapability.get(player);
        if (data == null || GodHelper.getGodOrNull(player, god) == null) return;
        var ledger = data.get(god.getRegistryName()).favorLedger;
        if (ledger.getBoolean("foundation")) return;
        ledger.putBoolean("foundation", true);
        GodHelper.increaseFavor(god, player, 5);
        player.sendSystemMessage(Component.literal(DivineRelationship.manages(god)
                ? "First sanctuary established. This act of devotion is remembered. Begin this god's story in the statue's Quests tab."
                : "First sanctuary established: +5 favor. This honor is remembered permanently."));
    }
}
