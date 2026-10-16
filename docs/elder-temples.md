# Detailed Elder Temples

> Archived design record, not current gameplay. Aureth, Malgrath, and Vaelith
> have been retired. Their powers, trials, stories, runtime subscribers, and
> natural shrine generation are removed. Saved statues remain inert relics.
> Narrative source JSON is retained under `docs/archive/elder-narratives/`,
> outside resource-pack discovery. Old Inner World scenery and validation
> claims below are historical only. See `elder-validation.md` for migration.

Only the three Elder shrine templates are replaced. These are fixed, authored
builds rather than random room assemblies or recolored copies. Existing generated
shrines are not rewritten. The original 18 shrines, Inner Worlds and trials retain
their existing resources and behavior.

## Buildings

Dimensions are width x depth x height; NBT stores x, y, z instead.

| Shrine | Build | Dimensions | Architecture |
| --- | --- | --- | --- |
| Aureth | Hall of the First Oath | 31 x 31 x 21 | Courtyard, pitched vaulted hall, raised sanctuary, smaller archive and open side cloister. Sandstone trim, restrained gold, blue banners, seating and repaired masonry. |
| Malgrath | The Broken Crucible | 33 x 29 x 19 | Fortified forge sanctuary with an offset entrance, buttresses, workshop and damaged gallery. A deliberate fracture uses enclosed shroomlight and orange glass instead of lava. |
| Vaelith | Observatory of the Unwritten | 29 x 33 x 23 | End-stone terraces, offset thresholds, purpur ribs and interrupted rings. An incomplete canopy frames the statue without requiring dangerous jumps. |

Every temple retains the author's original statue geometry and texture, with one
correctly bound statue, a clear approach and a 5 x 5 worship space. No additional
entities, loot, spawners, functional machinery, fluids or runtime effects are added.
Outside terrain is omitted from the NBT rather than replaced with an air cuboid.
Interior passages and intentional open spaces contain explicit air.

## Authoring

`docs/tools/elder_temples/` contains one layout per god and basic construction
helpers. `SPEC` defines each layout's size, statue position, entry landing and
worship rectangle. `build(builder)` explicitly places its architecture. Helpers
place blocks, stairs, slabs, columns, lanterns and patterned banners; they do not
choose a room plan or sprinkle random damage.

Run `python3 docs/tools/generate_elder_shrines.py --write` to regenerate all three
templates, or add `--god aureth` (also `malgrath` or `vaelith`) for one. The tool
validates every requested layout before writing any template. Running without
`--write` checks deterministic reproduction without changing anything.
`--models-only` checks the original statue model inheritance only.

The generated NBT's `divinity_temple` compound records the name and local test
positions. Vanilla structure loading ignores this extra root metadata. The
isolated preview and placement tests consume it, avoiding duplicated coordinates
in Java. It is not player save data and does not create a new gameplay API.

Structure IDs, statue IDs, biome tags, spacing, salts, dimension assignments and
Malgrath's absolute Y=40 placement rule remain unchanged. No migration, retroactive
chunk update or gameplay command is needed.

## Validation Record

Static checks pass for reproducible NBT, valid bounds and resources, sparse
exterior coverage, one statue, safe worship clearance and a connected walking
route. Current templates contain 9,242 / 9,257 / 10,179 block entries (including
intentional air) and 37 / 42 / 27 palette states for Aureth / Malgrath / Vaelith.

All 12 `divinity_elder_temples` Forge GameTests pass: each temple in all four
rotations. These use actual registered block states and collision shapes, verify
statue binding and block-entity save roundtrips, walk the entrance-to-court route,
and check first contact, statue-menu eligibility and accepted/rejected offering
callbacks. They also preserve markers outside the authored footprint. They do
not simulate a dropped offering's complete timed collection lifecycle.

Java 17 compilation and the reobfuscated `jarJar` build pass. The 12 rotation
tests also passed after closing Aureth's eaves and changing its benches to stone.

No personal save is used. A flat preview demonstrates the authored architecture,
not a guarantee of natural terrain integration. Natural placement must be reported
separately, especially for Malgrath's preserved fixed-height Nether placement.

The screenshot fixture is launched with `node docs/tools/run_elder_temple_preview.mjs`
after packaging. It copies only the synthetic `Core-Smoke` QA save to a fresh
temporary folder, then captures exterior, interior and statue views in their
respective dimensions. It cannot open normal `run/saves` worlds. Its report's
capture count is not a visual-quality assertion: review the images themselves.

`node docs/tools/run_elder_temple_natural.mjs` uses a separate, new, normal-terrain
dedicated-server save with seed 424242. It searches at most 16 fixed placement
cells per god, selects an actual registered structure start, requests only its
footprint and a four-block margin, and reports foundation, entrance and worship
conditions. It does not move, repair or clear the generated structure. A terrain
finding is distinct from an infrastructure failure. Both launchers retain their
reports and disposable saves for inspection and shut down their own test process.

### Natural Terrain Sample

The packaged normal-server probe completed for all three dimensions with seed
424242. No generation settings were overridden. All three actual structure
starts had the expected template, statue and block entity, clear entry landings,
25 safe worship squares and no fluids in their raw structure bounds. The four
sampled exterior approach blocks were not obstructed at both sampled heights.

| Temple | Actual origin / rotation | Foundation observation |
| --- | --- | --- |
| Aureth | (4416, 132, -848), 180 degrees | 9 of 665 base columns had air immediately below; solid support was two or three blocks down. |
| Malgrath | (3312, 39, 2336), 180 degrees | 16 of 646 base columns had gaps; 8 had no solid support within four blocks. The entrance landing and worship floor were still supported. |
| Vaelith | (-1264, 56, 2096), 90 degrees clockwise | All 651 sampled base columns had immediate support. |

These are real limitations of retaining the current static placement rules, not
a claim of perfect terrain fitting. In particular, fixed-height Nether placement
does not guarantee a distant cavern connection or prevent other seeds from
burying part of a temple. No automatic downward foundation filler or terrain
excavator was added. One sample per god does not cover every eligible biome/seed,
and FULL-chunk inspection does not prove settled fluid simulation.

The probe issued 44 direct FULL requests total (16 / 12 / 16) and released all its
tickets. Vanilla generation dependencies and spawn preparation also generated
protochunks; these counts are not a total-generation or performance benchmark.
Detailed evidence: `/tmp/elder-temple-natural-l6nEsf/elder-temple-natural-report.json`.

### Visual Review And Packaging

The final packaged-client gallery captured and reviewed all nine required views:
exterior, main interior and statue sanctuary for each god. The first pass led to
Aureth's closed sandstone eaves and stone seating. Final views show the original
statues, blue/gold banners, forge fracture, open observatory canopy and usable
sanctuaries without missing-texture blocks. Malgrath's close exterior view trims
part of the foreground test pad/stair edge; it is not a complete elevation survey.

Gallery report and PNGs are retained at
`/private/var/folders/w8/7__x388x10b0tz0467wnp1pc0000gn/T/elder-temple-preview-wWYR6O/elder-temple-preview/`.
These are actual Minecraft renders, not mockups. Only the staged Nether exterior
uses longer fog distances so its masonry can be inspected; the template, interior
lighting and natural-placement test are not modified by that visibility override.

The packaged jar contains byte-identical copies of the three final NBT resources.
The unrelated 109-file preservation inventory (original shrines, worldgen, trial
and Inner World resources/code) and original statue model/texture hashes match
their pre-task values. No personal worlds were opened. Manual prayer/story
playthrough and the complete timed dropped-offering lifecycle were not exercised;
the focused tests cover their statue access points, not those entire systems.
