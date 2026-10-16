package net.nightshade.divinity_engine.util.divinity.narrative;

import java.util.LinkedHashSet;
import java.util.Set;

/** Pure state machine. Only the server manager may turn validated gameplay into transitions. */
public final class NarrativeProgress {
    public enum Stage { DORMANT, OFFERING, WITNESS, CHOICE, DEED, RECKONING, TRIAL, RETURN, COMPLETE }
    private Stage stage = Stage.DORMANT;
    private String branch = "", specialization = "", expectedTrial = "";
    private int progress;
    private boolean expanded;
    private final Set<String> outcomes = new LinkedHashSet<>();

    public Stage stage() { return stage; }
    public String branch() { return branch; }
    public String specialization() { return specialization; }
    public int progress() { return progress; }
    public String expectedTrial() { return expectedTrial; }
    public Set<String> outcomes() { return Set.copyOf(outcomes); }
    public boolean expanded() { return expanded; }
    public void expanded(boolean value) { expanded = value; }
    public boolean activeObjective() { return stage == Stage.WITNESS || stage == Stage.DEED || stage == Stage.RECKONING; }
    public String chapter() {
        return switch (stage) {
            case DORMANT, OFFERING -> "call";
            case WITNESS -> "witness";
            case CHOICE -> "testimony";
            case DEED -> "deed";
            case RECKONING -> "reckoning";
            case TRIAL -> "trial";
            case RETURN, COMPLETE -> "ending";
        };
    }
    public boolean request(boolean eligible) {
        return request(eligible, false);
    }
    public boolean request(boolean eligible, boolean expanded) {
        if (!eligible || stage != Stage.DORMANT) return false;
        this.expanded = expanded;
        stage = Stage.OFFERING; return true;
    }
    public boolean acceptOffering(boolean accepted) {
        if (!accepted || stage != Stage.OFFERING) return false;
        stage = expanded ? Stage.WITNESS : Stage.CHOICE; progress = 0; return true;
    }
    public boolean confirm(String choice, boolean validShrine, boolean previewed) {
        if (stage != Stage.CHOICE || !branch.isEmpty() || !validShrine || !previewed || !(choice.equals("a") || choice.equals("b"))) return false;
        branch = choice; progress = 0; stage = Stage.DEED; return true;
    }
    public boolean advance(int amount, int target, boolean accepted) {
        if (!accepted || !activeObjective() || amount <= 0 || target <= 0) return false;
        progress = (int) Math.min(target, (long) progress + amount);
        if (progress >= target) {
            stage = stage == Stage.WITNESS ? Stage.CHOICE : stage == Stage.DEED && expanded ? Stage.RECKONING : Stage.TRIAL;
            expectedTrial = "";
            if (stage != Stage.TRIAL) progress = 0;
        }
        return true;
    }
    public void resetAttempt() { if (activeObjective()) progress = 0; }
    public void trialStarted(String session) { if (stage == Stage.TRIAL) expectedTrial = session; }
    public boolean trialOutcome(String session, boolean victory) {
        if (!outcomes.add(session)) return false;
        if (outcomes.size() > 64) outcomes.remove(outcomes.iterator().next());
        if (victory && stage == Stage.TRIAL && session.equals(expectedTrial)) { stage = Stage.RETURN; expectedTrial = ""; }
        return true;
    }
    public boolean finish(String reward, boolean validShrine) {
        if (!validShrine || stage != Stage.RETURN || !specialization.isEmpty()) return false;
        specialization = reward; stage = Stage.COMPLETE; return true;
    }
    public void restore(Stage stage, String branch, int progress, String specialization, String expectedTrial, Set<String> outcomes) {
        this.stage = stage;
        this.branch = branch.equals("a") || branch.equals("b") ? branch : "";
        this.progress = Math.max(0, progress);
        this.specialization = specialization;
        this.expectedTrial = expectedTrial;
        this.outcomes.clear(); outcomes.stream().limit(64).forEach(this.outcomes::add);
        if (!specialization.isEmpty()) this.stage = Stage.COMPLETE;
        else if (this.stage.ordinal() >= Stage.DEED.ordinal() && this.branch.isEmpty()) this.stage = Stage.CHOICE;
    }
}
