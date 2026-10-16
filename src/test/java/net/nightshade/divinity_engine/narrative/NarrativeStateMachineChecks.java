package net.nightshade.divinity_engine.narrative;

import net.nightshade.divinity_engine.util.divinity.narrative.NarrativeProgress;
import net.nightshade.divinity_engine.util.divinity.narrative.BuiltinNarrativeCatalog;
import net.nightshade.divinity_engine.util.divinity.narrative.DonationPlan;
import java.util.Set;

/** Dependency-free checks: run with Java 17, assertions are explicit rather than relying on -ea. */
public final class NarrativeStateMachineChecks {
    private static int checks;
    private static void check(boolean value, String message) { checks++; if (!value) throw new AssertionError(message); }
    public static void main(String[] args) {
        int paths = 0;
        check(BuiltinNarrativeCatalog.entries().size() == 18, "All 18 gods");
        for (var god : BuiltinNarrativeCatalog.entries()) for (String branch : new String[]{"a", "b"}) {
            var path = branch.equals("a") ? god.a() : god.b();
            NarrativeProgress p = new NarrativeProgress();
            check(!p.request(false), "Non-favorable request"); check(p.request(true), "Request");
            check(!p.acceptOffering(false), "Canceled offering"); check(p.acceptOffering(true), "Accepted offering");
            check(!p.confirm(branch, false, true), "Distant shrine"); check(!p.confirm(branch, true, false), "Unpreviewed branch");
            check(p.confirm(branch, true, true), "Confirmed choice");
            check(!p.confirm(branch.equals("a") ? "b" : "a", true, true), "Permanent branch");
            check(!p.advance(1, path.target(), false), "Canceled deed");
            p.trialStarted("old"); p.trialOutcome("old", true);
            check(p.stage() == NarrativeProgress.Stage.DEED, "Old trial cannot satisfy finale");
            check(p.advance(path.target(), path.target(), true), "Deed completion");
            p.trialStarted("failed"); check(p.trialOutcome("failed", false), "Failure recorded");
            check(!p.trialOutcome("failed", false), "Duplicate failure");
            check(p.stage() == NarrativeProgress.Stage.TRIAL, "Failure preserves deed");
            p.trialStarted("fresh"); check(p.trialOutcome("fresh", true), "Fresh victory");
            check(!p.finish(god.id() + "_" + branch, false), "Remote turn-in");
            check(p.finish(god.id() + "_" + branch, true), "Ending");
            check(!p.finish(god.id() + "_" + branch, true), "Exactly-once reward");
            NarrativeProgress restored = new NarrativeProgress();
            restored.restore(p.stage(), p.branch(), p.progress(), p.specialization(), p.expectedTrial(), p.outcomes());
            check(restored.specialization().equals(god.id() + "_" + branch), "Reward persistence");
            check(!restored.confirm("a", true, true), "Restore retains permanent lock"); paths++;
            NarrativeProgress otherPlayer = new NarrativeProgress();
            check(otherPlayer.stage() == NarrativeProgress.Stage.DORMANT && otherPlayer.specialization().isEmpty(), "Independent player");
            if (path.type().equals("donation")) {
                int[] available = {path.target() / 2, path.target(), 3};
                int[] plan = DonationPlan.plan(available, path.target());
                check(plan != null && java.util.Arrays.stream(plan).sum() == path.target(), "Exact donation");
                check(java.util.Arrays.stream(available).sum() > path.target(), "Planning does not consume inventory");
                check(DonationPlan.plan(new int[]{path.target() - 1}, path.target()) == null, "Insufficient donation atomic");
            }
        }
        NarrativeProgress attempt = new NarrativeProgress(); attempt.request(true); attempt.acceptOffering(true); attempt.confirm("a", true, true);
        attempt.advance(12, 20, true); attempt.resetAttempt(); check(attempt.progress() == 0, "Interrupted attempt");
        attempt.advance(20, 20, true); attempt.resetAttempt(); check(attempt.stage() == NarrativeProgress.Stage.TRIAL, "Completed deed retained");
        check(DonationPlan.plan(new int[]{Integer.MAX_VALUE, Integer.MAX_VALUE}, 16)[0] == 16, "Donation overflow bounded");
        for (var god : BuiltinNarrativeCatalog.entries()) for (String branch : new String[]{"a", "b"}) {
            var original = branch.equals("a") ? god.a() : god.b();
            NarrativeProgress story = new NarrativeProgress();
            check(story.request(true, true), "Expanded request");
            check(story.acceptOffering(true) && story.stage() == NarrativeProgress.Stage.WITNESS, "Offering opens witness");
            check(!story.confirm(branch, true, true), "Witness cannot be skipped");
            check(story.advance(5, 10, true), "Witness counter"); story.resetAttempt();
            check(story.progress() == 0 && story.stage() == NarrativeProgress.Stage.WITNESS, "Witness interruption resets only attempt");
            check(story.advance(10, 10, true) && story.stage() == NarrativeProgress.Stage.CHOICE, "Witness opens testimony");
            check(story.confirm(branch, true, true), "Testimony locks branch");
            check(story.advance(original.target(), original.target(), true) && story.stage() == NarrativeProgress.Stage.RECKONING, "Original deed opens reckoning");
            story.trialStarted("premature"); story.trialOutcome("premature", true);
            check(story.stage() == NarrativeProgress.Stage.RECKONING, "Premature trial cannot skip reckoning");
            check(story.advance(4, 4, true) && story.stage() == NarrativeProgress.Stage.TRIAL, "Reckoning opens trial");
            story.trialStarted("retry"); story.trialOutcome("retry", false);
            check(story.stage() == NarrativeProgress.Stage.TRIAL, "Retry retains prior chapters");
            story.trialStarted("victory"); story.trialOutcome("victory", true);
            check(story.stage() == NarrativeProgress.Stage.RETURN, "Fresh trial opens return");
            check(story.finish(god.id() + "_" + branch, true), "Expanded ending");
            check(!story.finish(god.id() + "_" + branch, true), "Expanded reward exactly once");
            check(!story.advance(1, 1, true), "Completed chapter cannot reopen");
        }
        System.out.println("Narrative checks passed: " + checks + " assertions, " + paths + " story paths");
    }
}
