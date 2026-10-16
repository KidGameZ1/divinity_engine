# Connected Pantheon: Elements

## Scope And Runtime Contract

This group owns only six version-2 chronicle resources, three version-2 relationship resources, and this document. Resources live in `src/main/resources/data/divinity_engine/divine_narratives/`. `BuiltinNarrativeCatalog.java` remains authoritative for story titles, stable branch ordering, original branch objectives, blessing specializations, and reward values. It was read, not edited. No Java, generated assets, other gods' files, or build configuration are part of this work.

Each chronicle follows seven chapters: call with an ordinary accepted offering; witness with a new objective; testimony with a permanent branch choice; the existing branch deed; reckoning with a new objective; a fresh successful trial; the branch ending. The ten passage and dialogue keys represent those seven chapters with alternatives for deed, reckoning, and ending. Both branches use exactly the same witness and reckoning objective definitions. Branch-specific reckoning prose changes the interpretation, never the requirement.

Every quest is independently completable. References to another god are testimony, not prerequisites. Historical sanctuaries, workshops, districts, signals, and unnamed mortals are narrated recollections, not required generated locations, NPCs, entities, items, or construction puzzles. The ordinary accepted opening offering is separate from an explicit branch donation. Previous trial victories do not satisfy the fresh-trial chapter. No chronicle adds another reward, favor deduction, or cross-god obligation.

Relationship introductions unlock when both listed gods have been contacted. Their `summary` fields introduce an unresolved relationship and do not deliver its ending. Epilogues require BOTH stories completed AND an explicit shrine conversation; contact alone, the second story's completion event alone, and passive proximity must not emit an epilogue. Runtime gating, persistence, and delivery are the main agent's responsibility, not additional JSON fields. No epilogue starts another quest, changes the chosen branch, awards a benefit, or requires the gods' physical presence together. The conversation brings their accounts together without spawning a meeting.

## Preserved Catalog

The following titles, branch order, deeds, and rewards remain the existing contract. The original catalog ending sentences also appear in their respective ending passages.

| God / Story | Branch A: Existing Deed And Reward | Branch B: Existing Deed And Reward |
| --- | --- | --- |
| Nerai / The Water's Promise | Harmony: Remain submerged for 120 continuous seconds without drowning damage. Aquatic Grace heals one HP every ten seconds while submerged. | Protection: Defeat six drowned. Aquatic Grace grants an additional 20% swim speed in water. |
| Cryonis / Winter's Testament | Preservation: Contribute 32 snowballs. Icy Pulse radius increases from three to four blocks. | Severity: Defeat six strays. Icy Pulse slowing lasts five seconds instead of three. |
| Voltira / The Storm's Will | Pursuit: Defeat eight hostile mobs with projectiles. Stormstep horizontal dash power increases from 2.5 to 3.0. | Conduit: Contribute two lightning rods. Stormstep vertical-dash Slow Falling lasts eight seconds instead of five. |
| Zephra / An Unbound Horizon | Journey: Sprint 1,000 blocks. Gale Step grants an additional 5% movement speed. | Heights: Reach Y=160 and return to this shrine. Gale Step reduces fall damage by 25%. |
| Grond / What Endures | Craft: Collect 16 iron ingots you have smelted from a furnace. Stoneform resistance lasts 12 seconds instead of 10. | Endurance: Mine 32 stone blocks below Y=0. Stoneform begins a 20-second cooldown instead of 25 seconds. |
| Mechanos / The Worth of a Machine | Precision: Contribute four pistons. Clockwork Punch knockback chance increases from 30% to 40%. | Force: Contribute 16 redstone dust. Clockwork Punch knockback strength increases by 25%. |

Opening offering examples were checked against each god's `onOfferedItems` implementation, not inferred from their thematic domain:

| God | Namespaced Vanilla Examples |
| --- | --- |
| Nerai | `minecraft:kelp`, `minecraft:prismarine`, `minecraft:nautilus_shell` |
| Cryonis | `minecraft:snow_block`, `minecraft:packed_ice`, `minecraft:blue_ice` |
| Voltira | `minecraft:lightning_rod`, `minecraft:redstone_block`, `minecraft:trident` |
| Zephra | `minecraft:feather`, `minecraft:scaffolding`, `minecraft:phantom_membrane` |
| Grond | `minecraft:iron_ingot`, `minecraft:copper_ingot`, `minecraft:raw_iron_block` |
| Mechanos | `minecraft:comparator`, `minecraft:observer`, `minecraft:iron_block` |

In particular, Cryonis's opening examples use accepted snow blocks or ice, not the snowballs requested by Preservation. Mechanos's initial component offerings are not interchangeable with the later piston or redstone donations.

## Objective Rationale

| God | Witness | Reckoning | Purpose |
| --- | --- | --- | --- |
| Nerai | `observe`, `water`, 240 ticks / 12 consecutive seconds | `harvest`, empty condition, 6 mature crops | Water first becomes evidence rather than a divine alibi. The harvest makes both listening and defense answer to ordinary nourishment. No harvest donation is implied. |
| Cryonis | `observe`, `snow`, 200 ticks / 10 consecutive seconds | `observe`, `firelight`, 240 ticks / 12 consecutive seconds | Snow raises the difference between silence and absence; warmth challenges both possessive preservation and rules indifferent to need. No freezing damage, burning, or item destruction is requested. |
| Voltira | `observe`, `rain`, 200 ticks / 10 consecutive seconds | `pray`, `still`, 200 ticks / 10 consecutive seconds at her shrine | Rain shows distributed consequences without requiring rare thunder. Brief voluntary stillness contrasts with the ancient binding and makes room to hear an answer after acting. |
| Zephra | `observe`, `day_sky`, 240 ticks / 12 consecutive seconds | `breed`, empty condition, 2 qualifying breeding events | Sky can be observed from safe ground; the Heights deed is not imposed on Journey. Husbandry challenges contempt for dependence or a chosen settled life. No animal donation or travel escort is required. |
| Grond | `observe`, `underground`, 220 ticks / 11 consecutive seconds | `plant`, empty condition, 3 distinct sapling positions | Underground attention examines hidden support; living growth challenges both perfect permanence and the glorification of strain. Neither tree maturity nor repeated placement at one position is required or sufficient respectively. |
| Mechanos | `smelt`, empty condition, 4 iron ingots collected from a furnace | `xp`, empty condition, 40 newly gained experience points | A small observable process values operators' evidence. Learning after commitment challenges false certainty and concealed worker risk. XP is points, not levels or a payment; combat is not prescribed. |

All observation targets lie inside the supported 100-600-tick range and require consecutive ticks. The group uses environmental attention, prayer, harvesting, husbandry, planting, smelting, and learning instead of adding more large kill or donation quotas. Existing branch quotas are untouched. Objectives do not require new generation or assets. Vanilla stone below Y=0 remains Grond's exact existing deed; the prose does not silently broaden it to deepslate. Safe breathing preparation is explicitly part of Nerai's submerged deed, not an additional tracked objective.

## Canon And Perspective

Shared canonical anchors are not reversed by any branch: the First Concord genuinely protected mortal life from oblivion; its Sundering forced choices with concealed sacrifices; no single secret villain explains the failure. Machines saved lives as well as becoming sites of fallible decisions. Branches change present disciplines and the honesty of the account, not historical outcomes.

- Nerai and Cryonis are sisters. Cryonis froze a drowned sanctuary to preserve its final memories. Nerai fears imprisonment. The group adds Nerai's diversion of an evacuation current as a contributing cause of its flooding, not the sole cause of the Sundering. Neither sister has a complete casualty count or establishes whether anyone was still alive when the ice closed. Preserved memories are not asserted to be imprisoned conscious souls.
- Voltira and Zephra are siblings. Voltira was bound to the Concord; Zephra broke restraints to evacuate mortals, transferring strain to the remaining defense. Each believes the other abandoned them. The supporting account adds an unsent recall signal and a return gust whose reception is unknown. Those details explain uncertainty, not a hidden conspiracy or proof that either sibling wanted the other harmed.
- Grond and Mechanos are former mentor and apprentice. Grond made foundations; Mechanos trusted calculation over the maker's warning. Grond and Ignar are old forge companions. The local failure account adds withheld settling records, changed support conditions, and a passage sealed to hold a district. These are a local catastrophe within the Sundering, not a new global origin for it. The workers' complete fate remains unknown. The mentor's omissions and apprentice's decisions are distinct responsibilities, not mathematical cancellation of blame.

Individual perspective boundaries:

| God | Belief, Lie, Or Omission | What The Chronicle Establishes Instead |
| --- | --- | --- |
| Nerai | Calls the ice a prison and avoids the arrivals missing from her rescue account. Her fear becomes an accusation beyond her evidence. | She sent a contributing current, does not know all arrivals, and cannot prove Cryonis intended captivity. Harmony learns consent and present need; Protection learns limits and the ordinary work after danger. |
| Cryonis | Says nothing was lost beneath winter, allowing preserved memories to be mistaken for saved lives. Edits inconvenient disagreement from her account. | Preservation succeeded at keeping memories, not restoring people. Both branches require admitting loss; Preservation makes custody answerable, while Severity applies exactness to the keeper's omissions too. |
| Voltira | Treats silence as proof of desertion; overstated capacity, concealed losses, and suppressed a maintenance warning. | Her binding sheltered real people, yet other people bore hidden costs. She cannot infer Zephra's intent from the rupture alone. Pursuit limits accusation; Conduit legitimizes reporting an unbearable load. |
| Zephra | Treats a distant storm flare as a choice against evacuees and celebrates escape without its transferred burden. Sometimes mistakes settled care for diminished freedom. | Breaking restraints saved mortals and increased strain elsewhere. Journey includes slower travelers and chosen responsibilities; Heights includes descent, return, and practical accessibility. |
| Grond | Presents a warning as sufficient by virtue of his authority; hides records, worker fatigue, and uncertainty below the district. | He had knowledge he should have shared, and sealing the passage was his own decision. Craft accepts inspection and revision; Endurance permits limits, relief, and rest. Neither becomes an anti-machine verdict. |
| Mechanos | Treats the old load limit as applicable despite changed conditions. Removes unresolved lives from a success rate or describes uninformed workers as volunteers. | The model omitted relevant damage and distribution, without making calculation inherently useless. Precision exposes assumptions and missing outcomes; Force names the people bearing risk and refuses equipment-first morality. |

Historical workers, the sanctuary's seed keeper and cook, the harbor clerk and captain, the cartwright and lookout, and the furnace attendant are supporting recollections. They give mortal needs specificity without becoming new named quest actors. Their reported episodes are not instructions to spawn entities or find memorial objects. No ending heals every dispute, resurrects victims, thaws the sanctuary, frees the ancient binding, rebuilds the district, or proves a perfect alternative would have saved everyone.

## Relationship Branch Matrix

The first letter always refers to the first god in `gods`, the second to the second god. These resources contain no reward or favor fields.

| File | Listed Order | `aa` | `ab` | `ba` | `bb` |
| --- | --- | --- | --- | --- | --- |
| `tide_winter_relationship.json` | Nerai, Cryonis | Harmony / Preservation: conditional keeping without forced closure | Harmony / Severity: patient attention with exact self-accounting | Protection / Preservation: guardians must explain the limits of custody | Protection / Severity: retire accusations unsupported by evidence |
| `storm_horizon_relationship.json` | Voltira, Zephra | Pursuit / Journey: follow evidence beyond the abandoned-person story | Pursuit / Heights: distinguish a true observation from an unproven inference | Conduit / Journey: refuges need exits, routes need supported pauses | Conduit / Heights: returning is a fresh choice, not automatic renewed captivity |
| `stone_machine_relationship.json` | Grond, Mechanos | Craft / Precision: make warnings and calculations inspectable | Craft / Force: durable tools serve lives, not the reverse | Endurance / Precision: workers' limits are evidence | Endurance / Force: survival is not a complete account of its costs |

## Validation

Validation uses read-only Node.js file reads and assertions, plus a SHA-256 check of the catalog. No Gradle command or build was run. Static checks cover exact schema keys, namespaced identities, objective conditions and targets, passage/dialogue coverage, accepted offering examples against the god sources, exact catalog deed/reward/ending text, branch ordering, valid JSON without duplicate keys, and prose lengths. This is content validation, not an in-game playthrough or verification of the separately owned loader and runtime.

Word counts include only `premise` plus all `passages` strings for a god; dialogue, objective labels, IDs, offerings, keys, and this document are excluded. Both alternative branches count toward that god's authored corpus. Counts use word tokens with internal apostrophes and hyphens retained. Relationship counts include only the selected epilogue's paragraph strings.

| Chronicle | Words |
| --- | ---: |
| `nerai_chronicle.json` | 1,612 |
| `cryonis_chronicle.json` | 1,635 |
| `voltira_chronicle.json` | 1,612 |
| `zephra_chronicle.json` | 1,622 |
| `grond_chronicle.json` | 1,625 |
| `mechanos_chronicle.json` | 1,631 |

| Relationship | `aa` | `ab` | `ba` | `bb` |
| --- | ---: | ---: | ---: | ---: |
| `tide_winter_relationship.json` | 199 | 204 | 205 | 199 |
| `storm_horizon_relationship.json` | 208 | 201 | 195 | 199 |
| `stone_machine_relationship.json` | 194 | 207 | 203 | 206 |

All six chronicles meet 1,200-1,800 words; all twelve epilogues meet 150-250 words. Exact legacy ending and reward language is retained for compatibility, while the surrounding scenes, testimony, dilemmas, and relationship outcomes are newly authored for this group.
