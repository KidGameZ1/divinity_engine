# Elder Stories

> Archived design record, not current gameplay. Aureth, Malgrath, and Vaelith
> have been retired. Their powers, trials, stories, runtime subscribers, and
> natural shrine generation are removed. Saved statues remain inert relics.
> Narrative source JSON is retained under `docs/archive/elder-narratives/`,
> outside resource-pack discovery. Old Inner World scenery and validation
> claims below are historical only. See `elder-validation.md` for migration.

## Scope And Compatibility

Three complete primary stories use the existing seven-chapter, two-branch narrative framework. Each actual path contains more than 1,100 words across the Call, Witness, Testimony, Chosen Deed, Reckoning, Trial, and Return. Each god has three greeting, prayer, and offering variants, plus contact, relationship, return, recognition, trial, and curse feedback.

The inspected target is Minecraft 1.20.1, Forge 47.4.2, Parchment 2023.09.03-1.20.1, Java 17, package `net.nightshade.divinity_engine`. No dependency, target, objective type, player command, or chat-command link is added. The existing Inner World system is present, but realm definitions and communion behavior are outside this content slice. Story artifacts below are narrative objects, not unimplemented inventory items, structures, or entities.

This content follows the selected deed-based progression direction. Contact makes a primary story eligible to begin. There are no favor-point requirements, numerical relationship thresholds, milestone currency rewards, or point-rescaling rules in these definitions. The parent owns all-god ranks, migration, story-start eligibility, proof gates, claims, and UI.

## Registration And Resources

`DivineNarrativeRegistry.initialize()` calls `ElderNarrativeDefinitions.registerBuiltins()` after the original catalog and before relationship/deferred-addon registration. No additional bootstrap call is needed. Initialization remains idempotent through the existing registry guard. `BuiltinPresenceDefinitions` and the original catalog are unchanged.

The helper reads the same version-2 JSON schema as datapacks, validates primary story/god/presence binding and a complete chronicle, then uses the existing public `registerStory` and `registerPresence` APIs. Missing or malformed bundled Elder content fails with the resource path instead of silently registering a skeleton.

All resource paths below are relative to `src/main/resources/data/divinity_engine/divine_narratives/`:

| Resource | Stable ID | Content |
| --- | --- | --- |
| `aureth.json` | `divinity_engine:aureth` | The Weight of a Promise; complete embedded chronicle |
| `malgrath.json` | `divinity_engine:malgrath` | The Price of Ruin; complete embedded chronicle |
| `vaelith.json` | `divinity_engine:vaelith` | A Thing That Should Not Be; complete embedded chronicle |
| `aureth_presence.json` | `divinity_engine:aureth` | Aureth, the First Sovereign |
| `malgrath_presence.json` | `divinity_engine:malgrath` | Malgrath, the Worldbreaker |
| `vaelith_presence.json` | `divinity_engine:vaelith` | Vaelith, the Unwritten |

Keep the story IDs and persisted branch keys `a` and `b` stable. Packs can replace the same resource paths through normal resource priority. A separate version-2 chronicle override can target the stable story ID. Existing immutable accepted-story snapshots and reload validation remain authoritative; new definitions do not rewrite a player's accepted choice. Removing an override restores the bundled definition for future acceptance. Do not add a second file defining the same story or presence in one effective resource set.

## Proof Contract

The content describes these parent-owned gates identically on both branches. Requesting, previewing, or merely confirming a branch is not itself a blessing proof.

| Proof | Existing transition | Aureth | Malgrath | Vaelith |
| --- | --- | --- | --- | --- |
| Accepted offering | `OFFERING -> WITNESS` | Royal Decree | Ruinbrand | Rift Lance |
| Completed witness | `WITNESS -> CHOICE` | Unbroken Will | Sacrificial Might | Impossible Angle |
| Completed chosen deed | `DEED -> RECKONING` | Kingsguard | Ruinous Grasp | Boundary Prison |
| Completed reckoning | `RECKONING -> TRIAL` | Sovereign's Reach | Ashen Tribute | Unwritten Form |
| Fresh trial victory | `TRIAL -> RETURN` | Throne of the World | Worldbreaker | Unmaking |

Physical return and the existing ending claim produce the permanent specialization. Trial entry cannot depend on the fifth blessing it proves. Neither branch may lose a proof, require the opposite branch, or depend on a numerical relationship threshold. Declining or postponing optional deeds is not a curse trigger. No story adds a time limit to combat deeds, a healing ban, an oath broken by travel, or ongoing artifact upkeep.

## Objective Contracts

IDs in this table use the `divinity_engine` namespace. Continuous observation targets are ticks at the existing 20-tick-per-second rate; all other targets count verified events or donated items.

| Story | Witness | Branch A | Branch B | Shared reckoning |
| --- | --- | --- | --- | --- |
| Aureth | `observe`, `day_sky`, 300 | Kept Word: `shield`, empty condition, 12 | Answerable Crown: `kill`, `melee`, 10 | `kill`, `projectile`, 6 |
| Malgrath | `observe`, `firelight`, 300 | Willing Tribute: `donation`, empty condition, 1 `minecraft:netherite_ingot` | Broken Claim: `kill`, `melee`, 12 | `shield`, empty condition, 10 |
| Vaelith | `observe`, `underground`, 300 | Held Boundary: `shield`, empty condition, 12 | Clean Severance: `kill`, `projectile`, 10 | `kill`, `melee`, 6 |

Observation restarts when its condition fails, not the whole story. Day-sky means daytime with sky visibility, not a special shrine. Firelight uses the existing two-block-per-axis neighborhood for fire, a lit campfire, or lava; players can stand safely beside it. Underground means below Y=0. Vaelith explicitly recommends a safe Overworld shelter: the End shrine assignment does not imply an End-only objective or a void challenge.

Kills use the existing personal melee/projectile conditions, not an unrestricted quota of any death. Shield events must actually block hostile damage. No unique-enemy-per-block condition is claimed. The existing event layer excludes trial/communion farming and owned entities. Parent integration must preserve valid hostile targeting, player/alliance/pet/summon exclusion, canceled-event checks, and save/reconnect behavior; prose is not executable enforcement of these safeguards.

Willing Tribute consumes exactly one netherite ingot through the existing explicit story donation at a matching physical statue. Previewing/confirming the permanent branch consumes nothing. An ordinary offering is not the donation. Netherite weapons and armor are different items and do not qualify. Neither branch costs health, deletes unrelated inventory, destroys terrain, or requires a physical Heart item. Once the donation is recorded, a failed trial cannot charge it again.

The offering examples match the current god registrations, rather than the earlier proposed ingredient lists:

| God and physical worship world | Accepted examples |
| --- | --- |
| Aureth, Overworld | Gold ingot, lapis lazuli, blue banner |
| Malgrath, Nether | Ancient debris, netherite scrap, crying obsidian |
| Vaelith, End | Popped chorus fruit, shulker shell, end rod |

## Permanent Reward Contract

Each reward is a real `BlessingSpecialization` identity bound to a new blessing. These modifiers are the agreed parent-owned runtime contracts, not a new configurable stat system. Each requires the blessing to be equipped and used normally. No reward adds a slot, automatically activates a blessing, or modifies the shared blessing singleton. Values are initial bounded tuning, not claims of playtested equivalence.

| Specialization ID | Bound blessing ID | Required effect |
| --- | --- | --- |
| `divinity_engine:aureth_a` | `divinity_engine:royal_decree` | Slow and outgoing-damage suppression duration x1.20 only; no increase to stagger, damage bonus, range, or targets |
| `divinity_engine:aureth_b` | `divinity_engine:royal_decree` | Cooldown x0.90; all other parameters unchanged |
| `divinity_engine:malgrath_a` | `divinity_engine:ruinbrand` | Stored eligible direct-damage cap x1.20 only; storage fraction, duration, radius, target cap, and no-chain protections unchanged |
| `divinity_engine:malgrath_b` | `divinity_engine:ruinbrand` | Cooldown x0.90; all other parameters and no-chain protections unchanged |
| `divinity_engine:vaelith_a` | `divinity_engine:rift_lance` | Validated range x1.15 only; preserve line of sight, loaded-area validation, damage, armor bypass, and target cap |
| `divinity_engine:vaelith_b` | `divinity_engine:rift_lance` | Cooldown x0.90; all other parameters unchanged |

Keep normal duration/range/storage hard caps and a positive cooldown floor. Parent hooks in `BlessingSpecializations` and the Elder ability runtime are responsible for actual mechanical effects. No duplicate reward handling, migration, or hooks were implemented in this content slice.

## Presence And Lore

Standard presence keys are `greeting`, `prayer`, `offering`, `return`, `failure`, `victory`, `recognition`, and the existing case-sensitive relationship keys `Neutral`, `Favorable`, `Devoted`, `Champion`, `Displeased`, `Enemy`. These keys are compatibility identifiers, not numerical thresholds. Their dialogue avoids point totals and can be mapped to deed-based ranks by the parent.

Additional authored event keys are `contact`, `trial_start`, `curse`, and `curse_cleared`. They need explicit dispatch from the parent runtime where appropriate; adding resource text alone does not create a callback. Existing first-contact greeting rotation still has three god-specific lines. Chapter dialogue independently explains each next milestone, fresh trial, and physical claim. The curse lines describe bounded mechanics, never punishment for declining a story branch.

Aureth's theme is the exact scope of a freely accepted promise. Kept Word honors a finite defensive obligation; Answerable Crown declines that oath and fulfills an agreed difficult alternative. Neither supplants Solarius's justice, Lumen's care, or Dravak's valor. Aureth's old watch is a local memory, not a claim that he secretly commanded the entire First Concord.

Malgrath's Heart of the Unbroken Gate is a remembered fastening and a claim to endless sacrifice, not an inventory collectible. Tribute surrenders it through a deliberate valuable donation; Broken Claim ends its demand through personal combat. Ignar's craft, Grond's foundations, Mechanos's calculations, Dravak's discipline, and Vokar's understanding of endings retain their own worth. One gate is not a new sole cause of the Sundering.

Vaelith's Unwritten Margin is an unstable story object. Stabilization accepts bounded responsibility; unmaking ends the object while retaining an accountable record. Neither ending proves the other morally inferior. Aethon's stars and Pocket Dimension, Kairoth's chronology, Nythea's survivors, and Elaris's records are not replaced or retroactively rewritten. Both resolutions respect existing lives and refuse ordinary inventory disposal into the void.

The First Concord genuinely defended mortal life from oblivion. These arcs add local perspectives without declaring the original pantheon evil, reversing the Sundering, resurrecting its victims, or inventing a painless solution that the older gods ignored.

## Verification

Completed without Gradle:

- Parsed all six resources; checked IDs, branches, all ten required passages per chronicle, custom titles, chapter dialogue, three interaction variants, exact objectives, unique bound rewards, ASCII text, field-length limits, and absence of numeric favor thresholds or command links.
- Checked every actual seven-chapter branch path exceeds 1,100 words.
- Compiled the narrative API, registry, new bootstrap helper, adapters, original catalog/presences, and state machine with Java 17 against cached Minecraft/Forge dependencies in an isolated temporary output directory.
- Exercised the real registry in an isolated Java process: 21 built-in stories, idempotent initialization, three correct primary bindings, and definition serialization round trips.
- Exercised all six branches through all five state-machine proof transitions, rejected offerings, immutable branch choice, stale trial victories, retry after failure, physical-return authorization, and duplicate reward prevention.
- Exercised story/presence reload overrides and restoration of bundled stories after override removal.

These are definition and pure-state checks, not Forge startup, live objective-event, migration, GUI, combat, multiplayer, or performance tests. Parent gate enforcement and the specialization hooks still require integrated validation. No Gradle tasks or game runs were launched by this content subtask.

Manual integration checklist:

1. Contact each Elder at its assigned-world statue, open its story immediately, and read the full disclosed obligations before accepting a path. Verify no relationship-point requirement blocks it.
2. Make an accepted offering from the table. Confirm the first blessing proof, then complete the exact witness and confirm the second. Interrupt an observation and verify only its partial count resets.
3. On separate player saves, choose each branch and complete its deed. Confirm the third proof on both routes and persist/reconnect without switching paths or losing progress.
4. For Malgrath A, check branch preview/confirmation takes nothing; donation removes one ingot only; repeated donation and later failed trials cannot consume another.
5. Complete each shared reckoning and confirm the fourth proof and trial access without owning the fifth blessing. Verify friendly/owned kills and trial kills do not advance the world deeds.
6. Reject an old trial victory, retry a failed fresh attempt, then win a new one. Confirm the fifth proof at Return and claim the specialization once at the physical statue, not through communion.
7. Equip the bound blessing and verify only the selected modifier applies. Unequip it and ensure no free passive upgrade remains. Confirm the three-slot limit and mixed-god loadouts.
8. Check standard and additional presence events, absence/return dialogue, curse activation/clearance, and original-god regressions. Replace an Elder resource via a pack, reload, and verify active accepted-story snapshots remain intact.
