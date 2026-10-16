# Elder World Content

> Archived design record, not current gameplay. Aureth, Malgrath, and Vaelith
> have been retired. Their powers, trials, stories, runtime subscribers, and
> natural shrine generation are removed. Saved statues remain inert relics.
> Narrative source JSON is retained under `docs/archive/elder-narratives/`,
> outside resource-pack discovery. Old Inner World scenery and validation
> claims below are historical only. See `elder-validation.md` for migration.

## Integration Contract

- Target remains Minecraft 1.20.1, Forge 47.4.2, Parchment 2023.09.03-1.20.1, Java 17.
- No new dependency. No numeric favor unlock or trial gate is introduced here.
- Parent owns deed ranks, global trial permission, story proof persistence, and language entries.
- Combat registrations are supplied by the combat worker.
- `divinity.trial.elder.ElderTrialConfig.SPEC` is registered by the parent as a SERVER config in the mod constructor, filename `divinity-engine-elder-trials.toml`. Profiles `SOVEREIGNTY`, `RUIN`, `UNMAKING` expose `phaseSeconds(int phase)` and `waveCount(int phase)` for phases 1..4.
- Existing `GodlyTrialsRegistry.initializeTrials()` and `InnerWorldsRegistry.initialize()` are the only content bootstraps; do not add a second initialization call.
- God callbacks call normal `GodHelper.increaseFavor`, captured by `BaseGodInstance` through `FavorRewards.interaction`. No direct ledger assignment, ordinary-worship budget bypass, or remote Inner World offering is added.
- Trial completion relies on `TrialSession.recordOutcome` / `DivineNarrativeManager.onTrialOutcome` for proof. The Elder trial callback does not grant repeatable favor or unlock blessings directly.

The mod constructor registers the config with the following existing Forge API:

```java
ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ElderTrialConfig.SPEC,
        "divinity-engine-elder-trials.toml");
```

## Gods and Statues

Every ID below uses the `divinity_engine` namespace.

| God field / ID | Physical worship world | Accepted items | Statue block, item and block entity ID |
| --- | --- | --- | --- |
| `GodsRegistry.AURETH` / `aureth` | Overworld | `minecraft:gold_ingot`, `minecraft:lapis_lazuli`, `minecraft:blue_banner` | `aureth_statue` |
| `GodsRegistry.MALGRATH` / `malgrath` | Nether | `minecraft:ancient_debris`, `minecraft:netherite_scrap`, `minecraft:crying_obsidian` | `malgrath_statue` |
| `GodsRegistry.VAELITH` / `vaelith` | End | `minecraft:popped_chorus_fruit`, `minecraft:shulker_shell`, `minecraft:end_rod` | `vaelith_statue` |

The three registrations use the same `GodsDeferredRegister` instance as the original 18. No manual block, item or block-entity registration is added. Total built-in roster: 21 gods and 21 automatically paired statues. `ElderGod.getWorshipDimension()` and `getAcceptedOfferings()` expose the physical rules. Live-entity offerings are rejected for all three, with no favor-from-pets or summon sacrifice route. Ordinary positive worship remains captured and is interpreted by the parent's relationship implementation; it is not a blessing gate.

Blessings are wired in the agreed order:

- Aureth: `ROYAL_DECREE`, `UNBROKEN_WILL`, `KINGSGUARD`, `SOVEREIGNS_REACH`, `THRONE_OF_THE_WORLD`; curse `BROKEN_CROWN`.
- Malgrath: `RUINBRAND`, `SACRIFICIAL_MIGHT`, `RUINOUS_GRASP`, `ASHEN_TRIBUTE`, `WORLDBREAKER`; curse `RUIN_WITHIN`.
- Vaelith: `RIFT_LANCE`, `IMPOSSIBLE_ANGLE`, `BOUNDARY_PRISON`, `UNWRITTEN_FORM`, `UNMAKING`; curse `FRACTURED_EXISTENCE`.

## Trials

All three use `GodlyTrial`, the existing shared `gods_trial.nbt` arena, bound statue, session owner, HUD, countdown, return handling, and cleanup. Each phase is cleared by the owner's credited kills before its deadline, not by surviving until the timer expires. Config values are snapshotted on session start. `GodTrial` has no per-definition favor field to set; the parent's `TrialSessionManager` proof permission is authoritative.

| Trial ID / Java class | Phase 1 | Phase 2 | Phase 3 | Phase 4 |
| --- | --- | --- | --- | --- |
| `trial_of_sovereignty` / `TrialOfSovereignty` | 6 / 60 s | 8 / 75 s | 10 / 90 s | 12 / 120 s |
| `trial_of_ruin` / `TrialOfRuin` | 6 / 55 s | 8 / 70 s | 10 / 85 s | 12 / 105 s |
| `trial_of_unmaking` / `TrialOfUnmaking` | 6 / 65 s | 8 / 80 s | 10 / 95 s | 12 / 115 s |

Configuration keys are `elderTrials.<sovereignty|ruin|unmaking>.phase<1..4>.seconds` (15..600) and `.enemies` (1..24). Times above correspond to ticks `1200/1500/1800/2400`, `1100/1400/1700/2100`, and `1300/1600/1900/2300`. Total default enemies: 36 per completed trial, never 36 alive simultaneously.

| Trial | Phase 1 composition | Phase 2 composition | Phase 3 composition | Phase 4 composition |
| --- | --- | --- | --- | --- |
| Sovereignty | 4 vindicators, 2 skeletons | 4 vindicators, 2 skeletons, 2 pillagers | 5 vindicators, 3 skeletons, 2 pillagers | 6 vindicators, 3 skeletons, 3 pillagers |
| Ruin | 3 piglin brutes, 2 wither skeletons, 1 skeleton | 4 piglin brutes, 3 wither skeletons, 1 skeleton | 5 piglin brutes, 4 wither skeletons, 1 skeleton | 6 piglin brutes, 4 wither skeletons, 2 skeletons |
| Unmaking | 4 endermites, 1 skeleton, 1 vindicator | 4 endermites, 2 skeletons, 2 vindicators | 6 endermites, 2 skeletons, 2 vindicators | 6 endermites, 3 skeletons, 3 vindicators |

Actual entity IDs are `minecraft:vindicator`, `minecraft:skeleton`, `minecraft:pillager`, `minecraft:piglin_brute`, `minecraft:wither_skeleton`, and `minecraft:endermite`. Unmaking uses cyan-armored Boundary Archers and Unwritten Sentinels as grounded alternatives to teleporting endermen/shulkers. Endermites are persistent Unwritten Fragments. No fliers, splitters, summoners, evokers, explosives, extra bosses, or wave reinforcements are introduced.

All humanoids receive explicit helmets and progressively heavier iron armor; Vaelith's humanoids wear cyan leather chestplates plus bounded armor attributes. Skeletons use bows, pillagers crossbows, vindicators iron axes, brutes golden axes, and wither skeletons stone swords. Common health is `24 + 6 * phase`, melee sentinels `30 + 8 * phase`, brutes `34 + 7 * phase`, and fragments `14 + 5 * phase`. Melee base damage is `4 + phase`; follow range is 32; knockback resistance is `0.05 * phase`. These are encounter statistics, not progression requirements.

Spawn positions are bounded to the existing arena floor, checked for solid support and collision, with a three-block player clearance. Mobs are session-tracked before entering the world. Spawn failure or an unexpectedly removed living wave member fails safely instead of awarding a clear. Brutes are explicitly immune to zombification and get an attack-target brain memory. Spawn finalization is intentionally replaced by authored equipment to avoid incidental riders or patrol setup. Pickup is disabled and equipment drop chances are zero. The existing session death handler cancels tracked deaths, discards owner-defeated mobs, and refuses non-owner credit; session cleanup also discards rather than killing. No new loot/XP or abandoned-wave farming route is added.

## Shrines and Exact Assets

Only the three new resource chains are added. All original 18 shrine structures, biome lists, salts, spacing, and separation remain unchanged. New shrines use independent random-spread sets with spacing 128, separation 48, and salts Aureth `8732001`, Malgrath `8732002`, Vaelith `8732003`. This makes the additions sparse but does not claim a guaranteed exclusion radius around independently placed original shrines.

| Structure ID | Vanilla biome eligibility | Placement | Palette and form |
| --- | --- | --- | --- |
| `aureth_shrine` | plains, sunflower plains, meadow, forest, birch forest | surface heightmap | stone and deepslate sanctuary, aged gold, blue runner and four gold-patterned banners, pillars and open arches |
| `malgrath_shrine` | Nether wastes, soul sand valley, crimson forest, warped forest, basalt deltas | fixed Y=40, underground decoration | covered blackstone fortress, basalt pillars, orange glass and shroomlight fractures, safe solid floor |
| `vaelith_shrine` | End highlands, End midlands | surface heightmap | End stone geometric sanctuary, purpur columns and rings, cyan/violet frames, sea lanterns and end rods |

The templates are detailed, individually authored temples with gzip NBT DataVersion 3465. Aureth measures 31 x 31 x 21, Malgrath 33 x 29 x 19, and Vaelith 29 x 33 x 23 (width x depth x height). Each contains one north-facing, non-waterlogged statue with its correct block-entity ID, empty inventory, clear approach and 5 x 5 worship space. Their local coordinates are recorded in template metadata for focused placement tests. No entity, chest, spawner, jigsaw, command block, fire or lava is placed. Existing generated chunks are not retrofitted. See [Detailed Elder Temples](elder-temples.md) for layouts, authoring and current validation results.

For each exact prefix `aureth`, `malgrath`, and `vaelith`, the shipped resource paths under `src/main/resources` are:

```text
assets/divinity_engine/blockstates/<prefix>_statue.json
assets/divinity_engine/models/block/<prefix>_statue.json
assets/divinity_engine/models/item/<prefix>_statue.json
data/divinity_engine/loot_tables/blocks/<prefix>_statue.json
data/divinity_engine/tags/worldgen/biome/has_structure/<prefix>_shrine.json
data/divinity_engine/worldgen/structure/<prefix>_shrine.json
data/divinity_engine/worldgen/structure_set/<prefix>_shrine.json
data/divinity_engine/worldgen/template_pool/<prefix>_shrine.json
data/divinity_engine/structures/<prefix>_shrine.nbt
```

All three Elder statues inherit the author's original `assets/divinity_engine/models/block/statue.json`, including its geometry, UVs, item transforms and `textures/block/gods_statue.png`. They do not recolor or replace that model. `models/block/elder_statue.json` remains as a compatibility alias to the same original model rather than a separate mesh.

The per-god block and item model paths, facing rotations, block IDs, block-entity IDs, loot and shrine NBT remain unchanged. Existing statues therefore pick up the visual correction without a world migration. `ModBlockStateProvider`, `ModBlockModelProvider`, and `ModItemModelProvider` continue to leave these shipped Elder assets alone during data generation. No duplicate runtime statue registers are introduced.

Run `python3 docs/tools/generate_elder_shrines.py --models-only` for a read-only check of model inheritance, shared texture references, item parents and all four facing rotations. This mode does not read or regenerate shrine structures or saves.

## Inner Worlds

The existing `InnerWorldsRegistry.initialize()` discovers the new `BuiltinInnerWorlds` entries, with default 25 x 16 x 25 bounded scenes and the original safe arrival/manifestation corridor. No separate dimension or new communion system is added.

| Realm ID | Name | Story ID | Visuals |
| --- | --- | --- | --- |
| `aureth_inner_world` | The First Throne | `aureth` | royal pillars, banners, blue-and-gold throne backdrop; beacon ambience, end-rod particles |
| `malgrath_inner_world` | The Collapsing Crucible | `malgrath` | fractured basalt/blackstone galleries over a safe floor; basalt-delta ambience, ash |
| `vaelith_inner_world` | The Unwritten Expanse | `vaelith` | nested cyan/violet frames, purpur columns and suspended light; amethyst resonance, reverse-portal particles |

Lookup remains `InnerWorldsRegistry.get(godId)`. The `.story(godId)` association matches the story worker's IDs. Existing communion supplies manifestation, dialogue, return, and cleanup. The realm scenes contain no functional statue. Trusted presentation for **all 21** scenes now checks `DivineRelationship.rank(player, god)` for `Favorable`, `Devoted`, or `Champion`, never numeric favor.

## Ownership and Verification

Owned Java additions: `divinity/gods/elder/{ElderGod,Aureth,Malgrath,Vaelith}.java` and `divinity/trial/elder/{ElderTrialConfig,ElderWaveTrial,TrialOfSovereignty,TrialOfRuin,TrialOfUnmaking}.java`. Existing Java edits: `registry/divinity/gods/GodsRegistry.java`, `registry/divinity/trial/GodlyTrialsRegistry.java`, `util/divinity/innerworld/BuiltinInnerWorlds.java`, and the three model/state data providers above. `InnerWorldsRegistry` and the legacy `GodlyTrials` list need no changes because their existing registration paths already accept the new definitions.

The standard-library-only `docs/tools/generate_elder_shrines.py` regenerates only these NBT files with `--write`; without that flag it is read-only. It independently decodes the generated NBT and validates statue binding, palette indices, connected access space, per-temple dimensions, complete structure links and block/texture resources. The detailed layouts live in separate modules under `docs/tools/elder_temples/`. See the temple validation record for current block counts and isolated runtime coverage; the older compact prototype counts no longer apply.

Final cross-reference checks pass for all 21 god fields, 21 trial registrations, 21 realm scenes, 21 shrine resource chains, all 25 new JSON files, all 15 Elder blessing fields, three curse fields, and the three matching story IDs. Kill routing was rechecked against `TrialSessionManager.handleDeathEvent`: the session removes its tracking before invoking `onKillEntity`, so the encounter correctly uses its own wave UUID set and owner UUID instead of asking the already-cleared session tracking map. Duplicate and unrelated kills cannot advance the wave.

Parent integration follow-up: ensure `StatueBlock.tryPray` checks `!prayer.isCanceled()` after `instance.onPrayedTo(prayer)` before recording narrative prayer progress. The god callbacks reject wrong-dimension rites, but the earlier caller ignored cancellation when recording story activity. Config registration is confirmed present. Language, global proof/rank policy, story content, combat, commands, and full integration testing remain owned by their assigned workers.
