# Deed-Based Relationships

## Playing the Story

Contact a god by right-clicking their statue. Reopen the statue, choose **Quests**, and select **Begin quest**. Built-in stories are available after contact, without a numeric favor threshold. Remain within six blocks of the matching statue for story actions.

The seven-chapter route is:

1. **The Call:** accept the request, then drop an accepted offering directly in front of the statue.
2. **A Witness:** complete the chapter's exact objective.
3. **Divided Testimony:** open **Speak about quest**, preview both upgrade rewards and permanently confirm one branch.
4. **The Chosen Deed:** fulfill that branch's deed. Either branch can unlock the complete blessing kit.
5. **The Reckoning:** complete the next objective to earn trial access.
6. **The Trial:** start and win a fresh trial from the matching statue's Powers tab. A victory before this chapter does not replace it.
7. **The Return:** select **Claim ending and upgrade** at the statue. This is where the permanent blessing specialization is awarded, not at branch confirmation or immediately after the trial.

Each built-in god's five blessing proofs are the accepted offering, completed Witness, completed branch deed, completed Reckoning and fresh trial victory, in that order. Check each blessing's displayed requirement rather than relying on its name or whether it is active or passive.

Sneak near a statue to pray. Repeating prayer or offerings does not bypass a required deed. For story donations, use **Contribute requested items** with the requested items in inventory; this is not the same as dropping an ordinary offering. Contributions are explicit, and the requested quantity is shown by the quest.

Trials may be retried. An interrupted continuous objective restarts its current attempt, not completed chapters. Abandonment pauses progression; reconnect through the statue to resume. Confirmed branches, earned upgrades and positive blessing unlocks imported from older saves remain saved. An offended god can require reconciliation without erasing completed proofs.

## Reading the Menus

- **Powers** shows the kit in progression order. Its details and hover text explain the required proof. Unlock permission comes from the server journal, not a local favor comparison.
- **Quests** shows the current chapter, exact task, objective counter, next action and specialization previews. Objective counts are retained; only numeric relationship requirements have been removed.
- **Bond** replaces Favor. It shows the relationship rank, next deed, individual blessing availability, trial access and remembered interactions. There is no worship-budget display or favor progress bar.
- **Commune** retains its separate action in the navigation rail. Communion leaves the player's body vulnerable and is not a trial.

A statue-opened menu only exposes its bound god. The inventory-opened menu keeps its searchable god navigation. The same three blessing slots and rebindable R/G/V inputs remain; powers from different gods can be mixed without adding a new loadout or ascension system.

Missing relationship sync shows a waiting message and disables new equip/trial actions instead of guessing an unlock. An already equipped blessing can still be removed. Long proof text is included in the scrollable description and full tooltip so small layouts do not conceal the requirement.

## Retired Elder Content

Aureth, Malgrath, and Vaelith are no longer active gods. Existing statues are
inert relics, not worship or quest entry points. Their saved progress is retained
without granting powers or exposing stories. See `elder-validation.md` for the
save-compatibility contract and `divinity_elder_retirement` tests.

The Known Pantheon guide calculates totals from the registry: the original
18 built-in gods and 90 blessings, plus any addons. Basic names and domains are
public reference material; personal chapters and chosen endings remain per-player.

## Journal Integration

The existing per-god journal entry with `section: "favor"` and ID `<god-id>/favor` is retained for compatibility. Its `relationship` compound contains the server's `DivineRelationship.snapshot(player, god)` result:

- `rank`: relationship label.
- `trial_unlocked`: authoritative trial eligibility.
- `unlocked_blessings`: list of namespaced blessing IDs currently available.
- `requirements`: compound mapping blessing IDs to readable proof descriptions.
- `next_action`: the player's next relationship action.
- `memories`: readable remembered interactions.

`NarrativeJournal.build` attaches this compound. The existing equality check and delta pipeline still determine whether anything is sent. `syncCounters` compares the compact relationship data along with existing counters without rebuilding chapter prose. Quest entries carry `can_start`, supplied by the same server relationship API.

The client consumes the snapshot; it never calls server-only `canUse` to guess permissions. The public `order(BlessingsInstance)` helper only controls display ordering. Equip and trial requests still require server-side revalidation outside the UI.

Older addons without stories may retain the legacy favor API. The UI displays their server-provided requirement and eligibility; it does not independently impose a deed or point threshold. No registry, packet format or player-specific static Codex page list is introduced here. Existing registered pages, deferred addon pages, pagination and personal journal entries remain supported.

## Addon Proof Mapping

Use `DivineRelationship.registerBlessingProof(ResourceLocation blessing, DivineRelationship.Proof proof)` during common setup to choose a named proof for an addon blessing. This overrides automatic ordering without changing a numeric favor threshold. The supported proofs are `INTRODUCTION`, `WITNESS`, `CHOSEN_DEED`, `RECKONING`, and `TRIAL_VICTORY`.

```java
// Inside your common setup work, after your blessing registration is available:
DivineRelationship.registerBlessingProof(
        new ResourceLocation("example_pantheon", "oathkeeper"),
        DivineRelationship.Proof.CHOSEN_DEED);
```

Relationship proofs come from the god's **primary registered story**. Optional secondary stories cannot bypass that authored progression. Both primary branches count toward the chosen-deed proof. Register stories through the existing narrative API; the Codex and menu use the resulting server journal without addon-specific screen code. Addons without a story keep the documented legacy compatibility fallback.

## Focused Verification

The current roster is 18 built-in gods and 90 blessings. The retirement suite
checks absence of Elder gameplay and retention of inert saved content. Historical
pre-retirement execution results in `elder-validation.md` are not results for
this change.

This UI/content change can be checked without opening a personal save:

- Validate English JSON and ensure every registered god, statue, domain, blessing and curse has a translation.
- Open the menu before the journal arrives: waiting messages, no newly enabled equip/trial buttons, no exception.
- Advance each proof and receive a journal update: the next blessing unlocks and Bond updates without reopening the screen.
- Confirm either branch, win the fresh trial and claim the ending: blessing proof and permanent specialization remain separate steps.
- Compare a new character with a migrated character: retained unlocks follow the server snapshot, not displayed favor.
- Test a displeased relationship: show reconciliation guidance rather than instructing the player to repeat earned deeds.
- Open from a statue and from inventory at small and large GUI scales. Check long requirement wrapping, search, god/kit scrolling, quest navigation and the Commune button.
- Open the Codex: registered addon pages remain present, the live pantheon includes all registered gods/blessings, unearned chapters and the unchosen ending remain hidden.
- Check reconnect, missing snapshots and legacy addons through the integrated parent test run.

The UI fixtures cover missing initial sync, every proof, 18-god navigation,
absence of retired gods in search, shrine isolation, long requirement text and
compact conversations. Compilation and runtime verification of this retirement
are left to the owner; full gameplay playthroughs and multiplayer remain unverified.
