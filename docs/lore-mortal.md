# Connected Pantheon: Mortal Group

## Scope and Integration Contract

This contribution owns only the six chronicles `terra`, `vokar`, `varun`, `dravak`, `lumen`, and `ignar`; the three relationships `root_farewell`, `pack_valor`, and `light_ember`; and this document. All nine JSON files are under `src/main/resources/data/divinity_engine/divine_narratives/`. No Java, trial, language, loader, runtime, or other group's files are edited. Existing shared-workspace changes are left intact. No Gradle builds are run.

Chronicles are version 2 overlays keyed by the existing story IDs. They intentionally contain no replacement branch definitions, rewards, or titles. `BuiltinNarrativeCatalog.java` remains the source of those contracts. Each playthrough has seven chapters: call and ordinary accepted offering; new witness deed; testimony and permanent branch confirmation; existing branch deed; new reckoning deed; fresh existing god trial; return for the ending. Branch-specific prose does not change the shared witness or reckoning objective.

Each quest is independent. Other gods supply historical context, never contact prerequisites, fetch targets, or mandatory testimony visits. The grove, refuge, evacuation, names, and last light are narrative history, not new structures, entities, collectible quest items, or generation requirements. Donation items are ordinary vanilla items. The existing offering, donation, fresh-trial, completion, and reward systems remain authoritative.

Relationship introductions unlock when both listed gods have been contacted. Epilogues require BOTH stories complete AND an explicit relationship conversation at a shrine. Neither contact nor completion alone should automatically deliver an ending. This is the runtime integration contract, not extra gating encoded in an invented JSON field. Relationship outcomes add no rewards, favor gains or losses, required allegiances, or changes to permanent choices.

## Preserved Catalog Contracts

The following titles, branch names, deeds, and rewards are transcribed from the existing catalog. New prose interprets them; it does not replace them.

| God and story title | Branch | Existing deed | Existing reward |
| --- | --- | --- | --- |
| Terra: Roots Remember | a: Renewal | Plant 12 saplings at distinct positions | Thornskin retaliation also applies Slowness I for two seconds |
| Terra: Roots Remember | b: Guardianship | Harvest 16 mature crops | Thornskin retaliation deals one additional HP of damage |
| Vokar: The Last Farewell | a: Release | Defeat eight undead at night | Soul Leech successful healing restores three HP instead of two |
| Vokar: The Last Farewell | b: Remembrance | Contribute eight soul sand | Soul Leech kills also grant Speed I for three seconds |
| Varun: The Pack's Claim | a: Nurture | Breed four animals | Savage Hunger additionally heals one HP on successful activation |
| Varun: The Pack's Claim | b: Hunt | Defeat eight hostile mobs in forest biomes | Savage Hunger kill buffs last seven seconds instead of five |
| Dravak: The Meaning of Valor | a: Conquest | Defeat eight hostile mobs with melee attacks | Battle Rush Strength lasts seven seconds instead of five |
| Dravak: The Meaning of Valor | b: Resolve | Successfully shield-block eight attacks from hostile mobs | Battle Rush also grants Resistance I for two seconds |
| Lumen: A Light Kept | a: Sanctuary | Contribute eight lanterns | Beacon's Grace qualifying block-light threshold decreases from 12 to nine |
| Lumen: A Light Kept | b: Purification | Defeat four witches | Beacon's Grace grants Resistance I while its existing light condition holds |
| Ignar: What the Fire Leaves | a: Fury | Defeat eight burning hostile mobs | Blazing Hands ignition chance increases from 15% to 25% |
| Ignar: What the Fire Leaves | b: Purpose | Contribute 16 charcoal | Blazing Hands ignition lasts five seconds instead of three |

Offering examples were checked against each god's `onOfferedItems` implementation, not inferred from its branch donation. Terra accepts azalea, moss block, and oak sapling; Vokar accepts soul sand, soul soil, and bone block; Varun accepts beef, leather, and bone; Dravak accepts iron sword, bow, and redstone block; Lumen accepts glowstone dust, blaze powder, and candle; Ignar accepts coal block, blaze rod, and magma block. Lanterns and charcoal are not presented as Lumen's or Ignar's opening offering examples merely because they are branch donations.

## New Objective Rationale

All IDs below use the `divinity_engine` namespace. Continuous observations use 200 or 240 ticks, within the required 100-600 range. Targets count gameplay actions unless explicitly stated as ticks. No witness or reckoning adds a kill quota. Only one adds a small donation.

| God | Witness | Reckoning | Narrative purpose |
| --- | --- | --- | --- |
| Terra | `observe`, `forest`, 240 ticks | `breed`, empty condition, 2 | A forest reveals life beneath a reassuring canopy; tending mobile new life challenges possession disguised as care. Renewal permits unfamiliar futures; Guardianship accepts inconvenient needs. |
| Vokar | `observe`, `underground`, 200 ticks | `pray`, `still`, 200 ticks | Being below sight is distinguished from erasure; stillness resists forcing either grief or relief into an approved timetable. Release permits an unfinished feeling; Remembrance leaves room beyond the keeper's record. |
| Varun | `sprint`, empty condition, 240 blocks | `observe`, `water`, 200 ticks | Travel exposes the leader's assumption that everyone shares his pace; water interrupts the certainty of the confident traveler or hunter. Nurture attends to unequal conditions; Hunt attends to incomplete evidence. |
| Dravak | `observe`, `day_sky`, 200 ticks | `smelt`, empty condition, 4 iron ingots | A clear view is not informed consent; useful material and maintenance follow both advance and defense. Conquest must leave work beyond victory; Resolve must include repair rather than glorifying damage. |
| Lumen | `observe`, `night_sky`, 240 ticks | `donation`, empty condition, 3 `minecraft:paper` | Darkness is not guilt; modest provision for testimony accompanies welcome or the use of force. Sanctuary hears its occupants; Purification records its limits. The sheets need no writing and hold no souls. |
| Ignar | `observe`, `firelight`, 200 ticks | `harvest`, empty condition, 4 mature crops | Useful flame is observed without a spectacle; ripeness cannot be hurried by destructive heat. Fury must leave nourishment afterward; Purpose cannot charge forgiveness for useful work. |

Runtime-aligned player directions specify a forest biome, underground below Y=0, open day/night sky, and fully underwater observation with an easy return to air. Varun's water deed is not wading. Ignar names standing safely beside a lit campfire within two blocks, not standing on fire or spreading it. Vokar's motionless prayer occurs at his shrine. Smelting means collecting player-smelted iron ingots from a furnace; neither those ingots nor the harvest is donated. Objectives do not ask for a historical site, custom item, special entity, or another god's blessing.

## Canon and Perspective

Shared canonical facts are not presented as an optional conspiracy theory:

- The First Concord protected mortal life from oblivion. It was not a covert scheme to exploit worshippers or a promise to abolish natural endings.
- Its Sundering forced concealed sacrifices and conflicting rescue duties into the gods' history. Emergency explains pressure without making every concealment innocent or inevitable.
- Terra and Vokar are old friends within the life/death cycle. Terra withheld a dying grove from Vokar; he preserved names instead of punishing her.
- Terra sheltered Varun's pack. Varun evacuated while his sworn brother Dravak held the line under Nashara's false order.
- Nashara forged the order to save refugees. Elaris concealed proof. Nythea hid survivors. These actions have different purposes and costs; they are not flattened into one secret villain's master plan.
- Ignar's fire destroyed a refuge. Ignar also secretly helped Lumen preserve its last light. The help does not cancel the destruction.
- Lumen holds names entrusted by Vokar. Names are people remembered, not a reward currency or collectible objects.

The grove and burned refuge are not asserted to be the same location. These files do not add a required itinerary or force every god's history into a single simultaneous event. In particular, Vokar's entrusted names are not all declared victims of Ignar's fire. Care with those distinctions leaves the other groups' accounts room to develop without accidental contradictions.

| God | Belief, omission, or self-deception | What the prose establishes instead |
| --- | --- | --- |
| Terra | Naming decline would invite death; postponement was necessarily patience; keeping the grove green justified secrecy. Her description of the grove as dormant concealed loss. | She withheld the grove and admits it. Shelter and concealment both happened. Vokar's mercy neither condemns all her care nor makes the concealment honest. |
| Vokar | Exact preservation might complete his duty; a keeper's endurance might suffice; release might supply a single timetable for grief. | He preserved rather than punished, and shared names with Lumen. His interpretations of grief are revisable judgments, not messages objectively dictated by the dead. |
| Varun | His pace was normal; the refugees' gratitude could heal brotherhood; finding a clear enemy would simplify his own account. | The evacuation mattered, but did not purchase Dravak's consent. Survivors are not compelled witnesses, and hunting cannot substitute for a difficult conversation. |
| Dravak | Visible conditions meant he understood the plan; confidence protected the line; being wronged made his whole account unassailable. | The order was forged for rescue, not honestly consented to. His discouraging questions is his own admitted conduct, not a reason to deny the forgery or the rescue. |
| Lumen | A cleaner public account would make sanctuary safer; naming Ignar's help might require universal forgiveness; pure intentions could certify a keeper. | Both the destruction and help are spoken. Lumen's selective telling is admitted without equating it to causing the fire. The harmed retain boundaries and their own judgments. |
| Ignar | Secrecy about his help was only modesty; usefulness could buy silence; a dramatic act could outweigh sustained responsibility. | His help was genuine and insufficient to undo the damage. Friendship is neither a pardon nor a right to victims' trust; repair must remain possible without being a transaction for forgiveness. |

The cup on uneven ground, disputed lamp, leather keepsake, imagined future roots, and specific remembered conversations are original narrative elaborations carried in divine testimony. They provide mortal texture, not new global historical prerequisites or mechanically discoverable evidence. Descriptions of what the gods fear, infer, or would like are their perspectives. Redemption remains prospective work that can be criticized; no ending declares damage unreal or makes another person forgive.

## Relationship Branch Order

Codes refer strictly to the gods in each file's listed order. Each epilogue recognizes both already-completed paths and adds a distinct relational consequence, not an additional deed.

| File | Listed order | aa | ab | ba | bb |
| --- | --- | --- | --- | --- | --- |
| `root_farewell_relationship.json` | Terra, Vokar | Renewal + Release: unfamiliar beginnings after an acknowledged ending | Renewal + Remembrance: mobile memory beside changing growth | Guardianship + Release: care that permits departure without erasing grief | Guardianship + Remembrance: shared custody rather than exhausted ownership |
| `pack_valor_relationship.json` | Varun, Dravak | Nurture + Conquest: informed limits on force used for the vulnerable | Nurture + Resolve: asking rather than presuming another's courage | Hunt + Conquest: refusing a common enemy as a shortcut to trust | Hunt + Resolve: exchanged information and permission to change roles |
| `light_ember_relationship.json` | Lumen, Ignar | Sanctuary + Fury: protection accepts a refuge's right to refuse entry | Sanctuary + Purpose: uncelebrated maintenance without purchased forgiveness | Purification + Fury: no retrospective claim that destruction proved guilt | Purification + Purpose: useful repair remains open to criticism |

## Validation Record

Counts use whitespace-delimited words across `premise` plus every paragraph in `passages`, including both branches. They exclude dialogue, objective descriptions, metadata, offering examples, and this document. Epilogue counts include only that combination's paragraph array. Non-mutating Node checks validate JSON parsing, exact schema keys, IDs, chapter/dialogue coverage, string and target types, objective conditions, approved item examples, branch names and deeds against the catalog, relationship order, and all word limits. The catalog itself is only read.

| Chronicle file | Narrative words |
| --- | ---: |
| `terra_chronicle.json` | 1597 |
| `vokar_chronicle.json` | 1582 |
| `varun_chronicle.json` | 1654 |
| `dravak_chronicle.json` | 1597 |
| `lumen_chronicle.json` | 1609 |
| `ignar_chronicle.json` | 1638 |
| Total | 9677 |

| Relationship file | aa | ab | ba | bb |
| --- | ---: | ---: | ---: | ---: |
| `root_farewell_relationship.json` | 192 | 197 | 197 | 193 |
| `pack_valor_relationship.json` | 193 | 195 | 196 | 202 |
| `light_ember_relationship.json` | 196 | 205 | 193 | 195 |

All chronicles meet 1,200-1,800 narrative words; all twelve epilogues meet 150-250 words. JSON validation is not an in-game playtest. Runtime integration, persistence, explicit-conversation gating, and trial execution remain the main agent's responsibility. No Gradle or game build was invoked for this content work.
