package net.nightshade.divinity_engine.util.divinity.narrative;

/** Exact slot transaction, computed again after any cancelable donation callbacks. */
public final class DonationPlan {
    public static int[] plan(int[] available, int requested) {
        if (requested <= 0) return null;
        int[] removed = new int[available.length]; int remaining = requested;
        for (int i = 0; i < available.length; i++) { removed[i] = Math.min(remaining, Math.max(0, available[i])); remaining -= removed[i]; }
        return remaining == 0 ? removed : null;
    }
    private DonationPlan() {}
}
