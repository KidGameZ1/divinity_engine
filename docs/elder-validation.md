# Deed Relationships and Elder Validation

## Elder Retirement

The sections below this retirement record describe the previous implementation,
not verification of the current removal.

- The active built-in roster is the original 18 gods, 90 blessings, 18 curses,
  and their original trials and stories. Addon registrations remain supported.
- All three `*_statue` block, item, and block-entity IDs remain registered.
  Facing/waterlogging, models, item NBT, and opaque block-entity payloads survive.
  These relics have no god, worship/menu behavior, or repeating statue ticks,
  and are excluded from the creative shrine list. Their self-drops remain.
- Elder god and curse capability records are retained unchanged in their saved
  lists, but never decoded into active instances. Equipped Elder payloads move
  into `legacyElderSlots`; usable slots become empty. A clean incoming snapshot
  clears a previous connection's archive. Existing story choices and rewards
  remain in narrative memory but are not discoverable or playable.
- Missing mappings are ignored only for the explicitly retired custom god,
  blessing, and curse IDs. Blocks, items, block entities, addon namespaces, and
  unrelated missing mappings are never ignored or redirected to another god.
- Existing structure IDs, pools, and NBT templates remain for chunk references.
  Placement sets are removed and biome tags are empty, preventing new natural
  starts. Already saved/pending structure pieces may still finish when loaded.
  Administrator placement commands can still place an inert legacy template.
- The six Elder narrative JSON files are archived outside runtime resources.
  Reload filtering also prevents old datapacks from reviving these stories or
  presences. Original pantheon stories are unchanged.
- No personal world, config, or save file was opened or edited. Normal stale
  trial-session recovery remains responsible for returning a player who logged
  out in a trial; this path does not need the removed trial definition.

The opt-in namespace is `divinity_elder_retirement`. Its eight tests exercise
registry/command absence, original18 bindings, statue and item NBT round-trips,
chunk/update packets, inert interaction/creative hiding, inactive capability
archives, story-memory retention, no natural placement, and narrowly scoped
missing mappings. They are added but not claimed as passing here; the owner
runs compilation and Forge tests in the isolated Gradle directory.

Downgrading is not an automatic gameplay restore: archived equipped slots are
not automatically re-equipped by older versions. Keep a backup before any
version change. No attempt is made to guess ownership of preexisting vanilla
potion/absorption state or remove unrelated effects.

## Scope and Status

The relationship update replaces positive favor thresholds with five persistent
story proofs. Existing numeric favor remains relevant to old-save migration and
negative-standing compatibility; it is not a second positive unlock meter.

Integration was checked on 2026-09-26 using Java 17. All **49 focused Forge
GameTests passed**, including relationship migration, all 42 finale claims,
the original 36 specialization paths, menu permissions, eight Elder combat checks,
all four phases of each Elder trial, timeout cleanup, and rejected Elder prayers.
These include fixture-driven checks, not 42 complete manual story playthroughs.

Compilation, addon-example compilation, reobfuscated packaging and bundled
dependency verification passed. The independent narrative checks also passed
1,330 assertions covering the original 36 paths. Personal saves were not opened.
See the execution record for packaged UI/server results and remaining coverage.

Focused progression test sources:

- `src/main/java/net/nightshade/divinity_engine/gametest/DivineRelationshipGameTests.java`
- `src/main/java/net/nightshade/divinity_engine/gametest/QuestFavorGameTests.java`
- `src/main/java/net/nightshade/divinity_engine/gametest/MenuActionGameTests.java`

## Running Safely

Compile on Java 17, then launch the isolated Forge GameTest server with the JVM property
`forge.enabledGameTestNamespaces=divinity_relationship`. The class registers no
tests unless that exact comma-delimited namespace is selected. It deliberately
does not use `@GameTestHolder`, which would register it with ordinary suites.
The template namespace remains `divinity_engine`, using the existing
`pocket_dimension` test template; the batch name is `divinity_relationship`.

The Gradle run configuration routes the combined namespaces listed in the execution
record to `~/.gradle/workspace-runs/divinity-elder-integration-gametest`, outside
personal saves. The relationship suite creates uncached, independently identified fake players, closes them in
`finally` via try-with-resources, and never starts a real trial, teleports players,
places shrine structures, or modifies existing worlds. It serializes test statue
entities in memory only. Run against the built-in pack without overriding addons
for the exact 21/105 roster assertions.

Record the commit or working-tree snapshot, JVM, selected namespace, isolated save
path, passed/failed count, and log path when executing. Do not record tests as
passing just because they compile. Do not change unrelated old tests merely to
make a broad suite green; identify their obsolete numeric assumptions separately.

## Expected Progression Contract

| Saved story stage | Proof | New blessing access |
| --- | --- | --- |
| DORMANT / OFFERING | 0 | None |
| WITNESS | 1 | First blessing |
| CHOICE / DEED | 2 | First two |
| RECKONING | 3 | First three |
| TRIAL | 4 | First four; trial available |
| RETURN / COMPLETE | 5 | All five |

Both branches have identical proof opportunities. Confirmation alone does not
grant proof three. The fifth blessing requires a fresh matching trial victory;
trial access must not require that fifth blessing. Proofs are monotonic, primary
story only, stored separately per god/player, and gated by active contact.

`Memory.relationship` persists `initialized`, `proof`, `legacy_unlocks` (stable
blessing IDs), and `legacy_trial`. First contact initializes without importing
favor. Explicit legacy migration imports only original-god earned access once;
it does not grant Elder zero-threshold blessings or invent completed chapters.
Original blessing order is previous required favor then registry ID. Elder order
is explicitly authored, not alphabetical or inferred from their zero-favor
constructors. Negative standing temporarily blocks original-god blessings without
deleting proof. Returning to non-negative standing restores eligible access.

The public `proofFor(Stage)` and `isElder(BaseGod)` helpers are checked against the
independent expected stage table and Elder roster, not used to compute their own
expected test results. The compact journal snapshot is checked against server
eligibility and for detached NBT data.

## Automated Relationship Checks Passed

- [x] Exact built-in roster: 21 gods, 105 uniquely bound blessings, 21 curses and 21 distinct god-bound trials; addon namespaces excluded.
- [x] Each Elder has a seven-chapter story, two unique specialization IDs targeting its first blessing, and a registered statue block-entity type that round-trips full metadata.
- [x] New contact never imports favor unlocks, even at 250; story access needs contact rather than positive favor grinding.
- [x] Original Solarius blessing migration checks immediately below and exactly at every prior unlock threshold.
- [x] All original 18 Champion saves retain five blessings and trial access after migration, including after positive favor is removed.
- [x] None of the three Elders inherit free legacy unlocks from zero-favor constructors, even with a forged/high old favor value.
- [x] Repeated migration and NBT reload cannot import later increases in favor.
- [x] Changing positive favor after initialization changes neither rank nor deed eligibility.
- [x] Every saved narrative stage, on both branches, maps to the expected proof without resetting its counter, branch, or specialization.
- [x] Both paths through all three Elder state machines unlock their five blessings in the exact authored order.
- [x] Failed, unrelated, and replayed trial outcomes cannot unlock proof five; a fresh successful retry can.
- [x] Relationship NBT, imported access, branch and unrelated ledger data survive player replacement; older story state cannot reduce proof.
- [x] Losing contact blocks access without removing memories; reconnect restores the same proof and branch.
- [x] Negative standing suspends original blessings and then restores them without deleting completed deeds.
- [x] A completed secondary/orphaned addon story does not advance a god's primary proof.
- [x] Progression is independent across two players and across two gods on the same player.
- [x] Repeated refresh of a completed story cannot regrant rewards, reopen its ending, or mutate persistent story state.
- [x] Compact journal snapshots expose exactly the earned blessings, each deed requirement and trial availability, without sharing mutable NBT with persisted data.
- [x] Rejected Elder prayers cannot record successful prayer memories or progression.

These are contract/state tests. They use trusted server-side stage setup and
synthetic accepted objectives rather than replaying actual combat, donations,
trial rooms or packets. Separate Elder trial tests exercise real arena preparation,
mob-death routing and cleanup. No production registry is changed by the suite.

### Existing Focused Suite Migration

`MenuActionGameTests` now starts fixtures at zero favor, checks contact rather than
the former Favorable cutoff, and preserves donation/replay/permanent reward
assertions without expecting numeric rewards. `QuestFavorGameTests` verifies all
42 built-in endings, no numeric reward from managed-god worship/deeds/finales,
and once-only proof advancement. The pure allowance tests remain to cover legacy
addon budgets; accepted sanctuary placement is verified through its persisted
foundation marker rather than through a numeric reward. Negative and canceled
paths still assert that the marker is absent.

The borrowed `BlessingSpecializationGameTests.all36PermanentBranches` registration
is retained with its mechanical checks intact. Its obsolete positive-favor
eligibility assertion now checks negative-standing suspension and zero-favor
recovery. That migrated suite passed with the rest of the focused checks.

The Kingsguard capacity fixture supplies enough Vanguard posture to avoid a
posture break while retaining real event-bus dispatch and exact damage assertions.
An initial failure was traced to Vanguard's extra damage on posture break, not a
replenished guardian. The runtime was not weakened to satisfy the test.

## Integration and Manual Checklist

Unchecked items below require broader manual or lifecycle coverage than the focused
fixtures and UI checks provide; they do not indicate that the implementation is absent.

### Progression and Save Compatibility

- [x] Run the new suite in its isolated save and retain results.
- [ ] On a fresh player, contact each Elder through its actual statue at zero favor; start its story through the menu, not a player command.
- [ ] Complete both reference routes with real accepted offering, witness, confirmation, deed, reckoning, trial and return events; no manual stage assignment.
- [ ] Reject canceled offerings, kills, shield events and donations; allies, pets, owned summons and trial mobs cannot farm unrelated deeds.
- [ ] Verify exact explicit donation consumption, stale/replayed menu rejection, and permanent branch confirmation. Neither valid branch strands progression.
- [ ] Import disposable copies of representative legacy saves at each story stage and old favor threshold, including uncontacted/discovered gods. Keep source saves untouched.
- [ ] Test actual logout/login, death clone, dimension change and server restart. The in-memory replacement test is not a substitute for capability lifecycle testing.
- [ ] Verify a missing addon primary story falls back to its legacy rules without rewriting saved requirements; restore the addon and check progress resumes.
- [ ] Verify a registered secondary addon story cannot unlock the primary relationship; preserve addon objectives, stable IDs and saved snapshots on reload.
- [ ] Admin reset removes only the explicitly selected relationship/story. Ordinary failure or abandonment must never perform this reset.
- [ ] Confirm finale upgrades apply only to the original eligible equipped blessing; unrelated players and unequipped blessings receive no upgrade.

### Elder Content and Runtime Safety

- [ ] Verify 12 Overworld / 5 Nether / 4 End shrine assignments, with original placements unchanged and each Elder generated only in its assigned dimension.
- [ ] Preview all new shrine NBTs in disposable worlds, including reload and chunk packets. The in-memory statue check does not validate structure generation or rendering.
- [ ] Verify Inner World entry, exit and cleanup for all three Elders through the existing system, including abandonment and disconnect.
- [ ] Verify all 15 active/passive kits through R/G/V with only three equipped slots and mixed-god builds. No ascension or additional key slot.
- [ ] Verify cooldown, range, target count, line-of-sight, loaded-chunk and effect lifetime bounds under configured limits.
- [ ] Verify secondary damage cannot recursively trigger detonations, healing loops, kill credit or new charges. Friendly targets and owned creatures are excluded.
- [ ] Verify six Elder specialization hooks independently: Aureth duration/cooldown; Malgrath storage cap/cooldown; Vaelith validated range/cooldown. Test actual behavior, not only IDs.
- [ ] Verify all three four-phase trials: 6/8/10/12 wave sizes; matching configured timings; all kills required; final kill at deadline handled correctly.
- [ ] Test melee/projectile credit, canceled or revived deaths, summons, death/logout/bounds exit, duplicate outcomes and fresh retries.
- [ ] Confirm arena cleanup is bounded, scoped to recorded bounds, and finishes before slot reuse; no pocket rooms, player builds or structure templates are deleted.

### UI, Packaging and Performance

- [ ] Blessings menus, story panels and Codex display deed requirements and earned status rather than misleading positive favor thresholds.
- [ ] Menus opened from a statue remain locked to that god; normal menus retain all valid contacted gods; registered Codex pages remain accessible.
- [ ] Locked fifth blessing explains the trial victory requirement while the fourth-proof player can start the trial.
- [ ] Journal handles opening before initial sync, reconnect, missing deltas and resource/language reload; no full story-text rebuild every tick.
- [ ] Start packaged client and dedicated server with bundled Nightshade, Vanguard and Player Animator; no client-class loading server-side.
- [ ] Compare idle, combat and trial-cleanup tick timings in disposable test worlds; report measured bottlenecks and untested visuals honestly.

## Execution Record

| Check | Status | Evidence |
| --- | --- | --- |
| Java 17 syntax parsing | Passed | JDK compiler parser read all three changed Java files; zero syntax diagnostics. No type analysis or compilation performed. |
| Changed-file whitespace checks | Passed | No whitespace diagnostics for the assigned files. |
| Java 17 compilation | Passed | `/tmp/divinity-elder-final-validation.log` |
| Focused Forge GameTests | Passed, 49/49 | Namespaces `divinity_relationship,divinity_quest_favor,divinity_menu_actions`; same log |
| Original narrative state-machine checks | Passed, 1,330 assertions | `/tmp/divinity-elder-narrative-checks.log`; 36 original paths |
| Narrative Java addon examples | Passed | `compileNarrativeAddonExample` in final validation log |
| Reobfuscated bundle and embedded mods | Passed | Nightshade 1.6.0, Vanguard 2.2.1-1.20.1, Player Animator 1.0.2-rc1+1.20; final validation log |
| Packaged client UI | Passed, 62/62 | `/tmp/divinity-elder-ui-smoke.log`; includes final tooltip refinement and compact tooltip height assertion |
| Final UI rebuild and bundle verification | Passed | `/tmp/divinity-elder-tooltip-build.log` |
| Packaged dedicated server | Passed | `/tmp/divinity-elder-server-smoke.log`; startup, resource reload and clean shutdown, exit 0 |
| Full playthroughs, real multiplayer and performance profiling | Not run | Do not infer balance or load-test results from fixtures |

GameTests used only
`~/.gradle/workspace-runs/divinity-elder-integration-gametest`.
The arena checks run in that disposable save, inspect their recorded bounds, keep
an unrelated entity and an outside marker intact, and confirm slot release after
batched deletion. They advance session-local time; they are not AI or tick-latency
benchmarks. Shrine NBT/resources received static checks and statue serialization
tests, not a complete natural-generation survey of the three vanilla dimensions.

The dedicated-server smoke run used `/tmp/divinity-elder-packaged-server-S1PXo9`,
only the bundled mod jar, and a newly generated flat save bound to localhost.
Its `report.json` records `ready: true`, `reloaded: true`, and `status: PASS`.
It used the previously authorized test-server EULA and did not access personal saves.

The final UI report is
`/private/tmp/nightshade-production-smoke-blessings-header-jGas2t/divinity-codex-smoke/run-12362283682058184252/report.json`.
It records 62 passing scenarios, no unrun scenarios, no cleanup errors, and restored
client options/journal state. The fixture supplies client-visible journal data; it
does not prove real network synchronization or quest rewards. The new long-proof
tooltip height check supplements widget geometry assertions. Blessing hover text
now contains the name, availability and full requirement without duplicating the
entire description, which remains available in the scrollable detail panel.

Visual review covered the galaxy Bond view, compact Elder statue view, compact
branch confirmation and final long-requirement tooltip. The tooltip fits after the
refinement. Other screenshots remain available beside the report; this was not a
complete manual review of every screen or combat effect. The dedicated-server
smoke preceded the last client-only tooltip/test change; server code did not change.
