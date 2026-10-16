# Earth, Spirit, Hunt, and War Blessing Particles

## Contract and Audit

All 30 definitions in `EarthSpiritBlessingDesigns.java` use one particle-only
Nightshade emitter and vanilla Minecraft sprites. No combat mesh, custom
sprite, bloom, child emitter, or continuous emission is introduced. The shared
facade, renderer, tests, packet codec, budgets, and lifecycle are parent-owned.

This pass reads actual gameplay hooks rather than ability names/tooltips.
Registration is unchanged: durations 8..24 ticks, counts 2..14, particle
lifetime duration minus two, within the shared 8..32 / 1..16 limits. Shared
alpha, size decay, alpha-blended material, renderer, and zero bloom remain
unchanged. Only explicit per-blessing sprite palettes are overridden below.

Ground snapshots use feet plus 0.08 blocks so centered emission volumes do not
start inside the support block. Sprint trails are placed behind actual horizontal
movement, and bracing dust along rejected knockback, using only existing vectors.
This is fixed clearance, not terrain sampling.
Body resistance and healing pulses remain body-local, not moved to the feet.
There are no new entity/block scans, random draws, terrain mutations, gameplay
conditions, effects, damage, cooldowns, scheduling changes, or return changes.

## All 30 Mappings

IDs have namespace `divinity_engine:`. Ticks/count are per request. Shape names
describe particle spawn distributions, never rendered geometry. Gameplay
predicates and shared request throttling remain authoritative.

### Grond

| ID | Ticks/count | Exact cues, actual mechanic, and presentation |
| --- | --- | --- |
| `hammerhand` | 8/9 | `IMPACT`: positive uncanceled TieredItem damage bonus; tight crit box at victim center with a short heavy arc. |
| `stoneform` | 20/7 | `WARD`: low-health resistance branch on damage; neutral stone grit falls through a body box. Torso pulse is intentional. |
| `echoing_strike` | 14/12 | `IMPACT`: accepted shockwave roll and positive uncanceled hit; soil dust hops across a low cylinder around owner feet. The hook does not require a secondary victim. |
| `earthbind_grip` | 18/14 | `CONTROL`: landing with existing nonempty target list; narrow upward soil kicks across the landing area reflect slowness plus vertical launch. |
| `forgeheart` | 16/6 | `AURA`: low-health resistance refresh, native flame embers at torso. `IMPACT`: low-health damage bonus, short tempering sparks at victim center, not generic crit or a burning debuff. |

### Varun

| ID | Ticks/count | Exact cues, actual mechanic, and presentation |
| --- | --- | --- |
| `feral_step` | 12/8 | `TRAIL`: moving sprint boost, small soil-dust cylinder 0.20 blocks behind actual horizontal movement. `WARD`: reduced-fall branch, wider landing dust cylinder. No look-pitch/yaw dependence. |
| `trackers_mark` | 24/4 | Private `MARK`: four nearly stationary native glints in a narrow target-centered box, descending at 0.003 blocks/tick. Existing private targets, cadence, transport, and client glow are untouched. |
| `predators_focus` | 12/2 | `CAST`: completed stationary crouch charge grants strength/night vision; two native glints advance along the eye/look sightline. No unfinished-charge visual or crosshair. |
| `savage_hunger` | 16/5 | `TRAIL`: kill-granted speed/strength, local soil-grit hops at feet, not a claimed moving trail. `HEAL`: specialization actually increased health, rising native hearts at body. |
| `alphas_call` | 16/12 | `CONTROL`: nonempty target list receives slowness and outward/upward push; low soil-dust scatter has a stronger upward hop, not roar rings or a summon. |

### Dravak

| ID | Ticks/count | Exact cues, actual mechanic, and presentation |
| --- | --- | --- |
| `battle_rush` | 16/9 | `TRAIL`: kill-granted speed/strength, local upward soil grit at feet regardless of movement. `WARD`: specialization resistance, neutral stone grit descending around torso. |
| `unyielding` | 16/6 | `AURA`: low-health resistance, compact planted-foot stone grit. `WARD`: canceled knockback, bracing dust 0.12 blocks along normalized negative event ratios, not a claim that half-strength knockback actually moved the owner. |
| `crushing_blow` | 18/14 | `IMPACT`: existing charged-hit branch, broad heavy local crit burst at victim center, with upward hop and settling instead of false owner-look direction. Hit counters/cooldown remain unchanged. |
| `warlords_will` | 20/8 | `CONTROL`: blindness and WarlordConfusion on existing selected enemies, not slowness; compact neutral ash smoke descends across each requested target's eyes, not torso. |
| `fury_unbound` | 20/10 | `CAST`: existing combined strength/resistance branch; body-local rising potion wisps (`spell_0`) instead of a forward damaging crit spray. |

### Vokar

| ID | Ticks/count | Exact cues, actual mechanic, and presentation |
| --- | --- | --- |
| `soul_leech` | 16/8 | Linked `HEAL`: kill actually restores health, native sculk souls travel victim-to-owner. Unlinked `TRAIL`: specialization grants speed, faded native souls rise locally from feet; no invented movement requirement. |
| `wraithwalk` | 8/7 | `WARD`: damage-site invisibility cue, neutral smoke disperses symmetrically from a fixed body snapshot. It never follows or points along the invisible owner's escape. |
| `ghastly_aura` | 16/14 | `AURA`: existing qualifying enemies receive darkness/weakness/slowness; faded native souls drift just above ground throughout the field. |
| `dread_consumption` | 18/9 | Linked `HEAL`: kill of a Weakness victim actually restores health; faded native souls transfer victim-to-owner, with more spread than Soul Leech. |
| `witherbound` | 18/11 | `CONTROL`: successful Wither application; neutral ash grit falls across the debuffed body's box, rather than turquoise debris. |

### Terra

| ID | Ticks/count | Exact cues, actual mechanic, and presentation |
| --- | --- | --- |
| `verdant_touch` | 14/7 | `CAST`: after successful bonemeal callback only; native happy-villager glints rise at the grown block. |
| `thornskin` | 10/8 | Linked `IMPACT`: successful reflected damage; native leaf fragments travel owner-to-attacker. Unlinked preview fallback scatters leaves forward. No projectile or thorn mesh. |
| `stonewardens_embrace` | 20/5 | `AURA`: resistance on configured natural support; nearly still neutral stone dust settles around boots. |
| `rootgrip` | 20/8 | `CONTROL`: existing qualifying slowness hit, tight soil kicks at victim feet; no invented fifth-hit trigger or root geometry. |
| `wildheart_bloom` | 20/12 | `AURA`: native petals over the ground field. `HEAL`: actual callback health increase, native hearts around recipient, distinct from crop growth. Linked `MARK`: native leaves travel owner-to-selected-player after existing registration branch. |

### Mechanos

| ID | Ticks/count | Exact cues, actual mechanic, and presentation |
| --- | --- | --- |
| `tinkers_grace` | 8/4 | `AURA`: existing TieredItem swing placeholder, small sparks at a main-arm-aware hand/tool approximation; no invented durability repair or collision/contact claim. |
| `auto_smelter` | 14/10 | `CAST`: existing accepted roll/item predicate after block handling; native furnace embers rise at block center. Does not promise guaranteed conversion; item predicate unchanged. |
| `clockwork_punch` | 10/7 | `IMPACT`: accepted knockback roll and positive uncanceled hit, compact local spark burst at victim center, replacing look-directed crit fragments. Existing piston sound unchanged. |
| `precision_impact` | 20/6 | `CAST`: accepted furnace tag boost; neutral smoke vents above the furnace roof, not inside the clicked side or as a melee hit. |
| `overdrive_loop` | 12/6 | `TRAIL` while sprinting: soil grit 0.18 blocks behind actual horizontal movement. Otherwise `AURA`: neutral waist-height exhaust wisps. Existing active-window refresh is unchanged, including the menu predicate. |

## Changes This Pass

- Replaced misleading crit branches with mechanic-specific glints, tool/tempering
  sparks, potion-buff wisps, stone fragments, or ground grit. Existing true
  heavy-hit crit bursts remain for Hammerhand and Crushing Blow.
- Distinguished Wildheart actual healing (hearts) from Verdant crop growth
  (glints), while keeping both actual-health-increase guards unchanged.
- Removed fake directional movement from stationary kill-granted speed cues.
  Soul Leech, Savage Hunger, and Battle Rush now lift locally at the boots.
- Removed owner-look bias from Hammerhand, Forgeheart, Crushing Blow, and
  Clockwork Punch impact bursts. Feral/Overdrive trail anchors use actual
  movement; Unyielding's WARD anchor uses rejected knockback. No new directed-at
  API dependency or fake link is needed. The local bursts deliberately do not
  claim a precise hit-normal/impulse trajectory unavailable through `at`.
- Warlord's Will moved from torso to eyes and now documents actual
  blindness/confusion, not nonexistent slowness.
- Wraithwalk smoke has zero horizontal mean velocity at its fixed damage site;
  no attached or ongoing invisibility trail was introduced.
- Ground regions get 0.08-block clearance. Precision Impact moves from the
  clicked face to block center + (0, 0.6, 0), i.e. 0.10 above the roof.
  Tinker's Grace uses height * 0.6, look * 0.35, and signed main-arm side * 0.22.
- Corrected earlier documentation claiming Echoing Strike requires secondary
  victims and Unyielding merely reduces knockback. Their gameplay is untouched.

## Explicit Palette Exceptions

The visual/metadata palette is still the god palette. Only particle palettes
change, with cycleTicks = 40. Shader/material, alpha curve, bloom-off, and
shared size decay are not overridden.

| Particle palette | Explicit blessing/cue cases |
| --- | --- |
| Native white `[0xFFFFFFFF]` | Forgeheart `AURA`; Tracker's Mark `MARK`; Predator's Focus `CAST`; Savage Hunger `HEAL`; Soul Leech `HEAL/TRAIL`; Ghastly Aura `AURA`; Dread Consumption `HEAL`; Verdant Touch `CAST`; Thornskin `IMPACT`; Wildheart Bloom `AURA/HEAL/MARK`; Auto Smelter `CAST`. |
| Soil `[0xFF806247, 0xFFB99A70]` | Echoing Strike `IMPACT`; Earthbind Grip `CONTROL`; Feral Step `TRAIL/WARD`; Savage Hunger `TRAIL`; Alpha's Call `CONTROL`; Battle Rush `TRAIL`; Rootgrip `CONTROL`; Overdrive Loop `TRAIL`. |
| Stone `[0xFF888780, 0xFFB2ACA0]` | Stoneform `WARD`; Battle Rush `WARD`; Unyielding `AURA/WARD`; Stonewarden's Embrace `AURA`. |
| Ash `[0xFF77736E, 0xFFAAA49A]` | Warlord's Will `CONTROL`; Wraithwalk `WARD`; Witherbound `CONTROL`; Precision Impact `CAST`; Overdrive Loop `AURA`. |
| God palette retained | Hammerhand `IMPACT`; Forgeheart `IMPACT`; Crushing Blow `IMPACT`; Fury Unbound `CAST`; Tinker's Grace `AURA`; Clockwork Punch `IMPACT`. |

These are per-author/branch choices, not a generic texture-based override.
Non-gameplay preview cues follow the same branch fallback as their author.
Native white prevents heart red, leaf/petal pink, growth green, flame orange,
and soul cyan from being multiplied into unrelated god colors.

## Semantic Test Expectations

The parent owns integration/semantic tests. These are expectations, not a
claim that GameTests or Minecraft have been run in this pass.
`F` is normalized supplied forward, and `L` is the full signed link.
Box dimensions are half-extents. Cylinder height
is total height. Texture names below expand to
`minecraft:textures/particle/<name>.png`.

| Blessing / exact cue | Shape / sprite | Mean velocity; gravity; drag |
| --- | --- | --- |
| Hammerhand `IMPACT` | box `(.09,.12,.09)` / `critical_hit` | `(0,.055,0)`; `.022`; `.90` |
| Forgeheart `IMPACT` | point / `spark_0` | `(0,.035,0)`; `.016`; `.92` |
| Feral Step `TRAIL / WARD` | cylinder `(.16/.24,.02)` / `generic_2` | `(0,.045/.07,0)`; `.014`; `.90` |
| Tracker `MARK` | box `(.50r,.75r,.035)` / `glint` | `(0,-.003,0)`; `0`; `1` |
| Predator `CAST` | line `.18` along `F` / `glint` | `.026F`; `0`; `.93` |
| Savage Hunger `TRAIL` | box `(.24,.025,.24)` / `generic_0` | `(0,.10,0)`; `.015`; `.94` |
| Savage Hunger `HEAL` | box `(.22,.24,.18)` / `heart` | `(0,.034,0)`; `0`; `.95` |
| Alpha's Call `CONTROL` | cylinder `(.65r,.04)` / `generic_2` | `(0,.11,0)`; `.014`; `.88` |
| Battle Rush `TRAIL / WARD` | box `(.30,.025/.36,.18)` / `generic_0` | `(0,.065/-.025,0)`; `.014/.008`; `.91` |
| Unyielding `WARD` | box `(.28,.015,.12)` / `generic_0` | `(0,.055,0)`; `.012`; `.86` |
| Crushing Blow `IMPACT` | point / `critical_hit` | `(0,.07,0)`; `.025`; `.91` |
| Warlord's Will `CONTROL` | box `(.24,.12,.24)` / `generic_5`, at eyes | `(0,-.012,0)`; `0`; `.95` |
| Fury Unbound `CAST` | box `(.40,.26,.18)` / `spell_0` | `(0,.025,0)`; `0`; `.94` |
| Soul Leech `HEAL` | line `min(.12,\|L\|)` along `L` / `sculk_soul_3` | `L/14`; `0`; `1`; spread `(.006,.006,.006)` |
| Soul Leech `TRAIL` | box `(.20,.045,.20)` / `sculk_soul_8` | `(0,.018,0)`; `0`; `.96` |
| Wraithwalk `WARD` | box `(.24,.48,.22)` / `generic_5` | `(0,.035,0)`; `0`; `.86` |
| Dread Consumption `HEAL` | line `min(.12,\|L\|)` along `L` / `sculk_soul_8` | `L/16`; `0`; `1`; spread `(.012,.012,.012)` |
| Wildheart Bloom `HEAL` | cylinder `(.26,.35)` / `heart` | `(0,.026,0)`; `.001`; `.96` |
| Tinker's Grace `AURA` | point / `spark_0` | `.05F + (0,.025,0)`; `.018`; `.88`; now block-STOP |
| Clockwork Punch `IMPACT` | box `(.08,.08,.08)` / `spark_0` | `(0,.085,0)`; `.012`; `.91`; spread `(.04,.025,.04)` |
| Precision Impact `CAST` | box `(.16,.06,.16)` / `generic_5`, above roof | `(0,.045,0)`; `0`; `.97` |
| Overdrive Loop `TRAIL / AURA` | box `(.26,.025/.12,.10)` / `generic_0 / generic_5` | `(0,.04/.022,0)`; `.010/0`; `.92` |

Changing look yaw/pitch must not change the ground/impact motion above.
For Feral/Overdrive, use movement orthogonal to look and verify the world anchor
is behind movement, not behind look. Zero horizontal movement gives zero anchor
offset (Overdrive's existing sprint predicate is not changed). For Unyielding
WARD, ratios `(1, 0)` place dust 0.12 blocks toward negative X independent of look;
zero ratios leave it centered. These are visual-only positions, not new queries.

All genuine linked transfers keep their existing source/destination meaning:
Soul/Dread victim-to-owner, Thornskin owner-to-attacker, Wildheart MARK
owner-to-selected-player. Nonzero Soul/Dread links must have positive
`motion.velocity().dot(L)`, including signed downward and tiny links.
No fabricated links are used for directional impacts or trails. Transfer
spread is symmetric, and no gravity, drag loss, orbit, or block collision bends
their mean trajectory. Short-link source anchoring is parent-owned.

Solid debris uses existing non-authoritative block STOP, entity IGNORE, no
collision callbacks. Gases, marks, buff wisps, hearts, and transfers do not
collide. Effects remain snapshot bursts, not persistent physical objects.

## Privacy and Preserved Behavior

Tracker's hook and private transport are unchanged byte-for-byte in this pass:
existing modulo-20 queries, target ranges, batching, recipient-only packets,
glow handling, and cleanup remain intact. No public `at` replacement is used.

Wraithwalk keeps its fixed world snapshot. AURA/TRAIL invisibility concealment,
expiry, death/logout/world cleanup, rate/capacity limits, and shader/reduced
motion handling remain the facade's responsibility. Existing multi-target
requests can be coalesced by the budget; this pass does not promise every
affected enemy a separate emitter.

Existing oddities in Rootgrip counters, Wildheart ally registration/list
handling and delayed callbacks, Auto Smelter's result predicate, Overdrive's
menu activity condition, and all other gameplay remain out of scope.

## Static Verification and Resources

Static checks compare all 30 hook sources with the pre-edit working-tree
snapshot after removing cosmetic statements/imports, verify the 30 unique
registrations/authors, unchanged registration/count/duration/size values,
numeric bounds, and vanilla texture entries. Java compiler parse-only checks
use Java 17 source syntax for the recipe class and all 30 hooks; this is not a
classpath compilation or gameplay execution. No Gradle task, Minecraft launch,
world operation, test edit, renderer edit, or facade edit is performed here.
Parent semantic, all-90 structural-distinctness, transport, and visual checks
remain required.

Sprites verified against the local Minecraft 1.20.1 client archive:

```text
generic_0.png generic_2.png generic_5.png critical_hit.png spell_0.png
flame.png sculk_soul_3.png sculk_soul_8.png
cherry_1.png cherry_6.png glint.png heart.png spark_0.png
```

No assets were copied or repainted. The cached Nightshade Core 1.6.0 source
archive was read for builder, palette, motion, collision, and centered
box/cylinder semantics; the shared API was read before edits.

## Files Changed This Pass

- `src/main/java/net/nightshade/divinity_engine/util/divinity/effects/EarthSpiritBlessingDesigns.java`
- `docs/blessing-effects-earth-spirit.md`
- Under `src/main/java/net/nightshade/divinity_engine/divinity/blessing/grond/`: `EarthbindGrip.java`, `EchoingStrike.java`.
- Under `src/main/java/net/nightshade/divinity_engine/divinity/blessing/varun/`: `AlphasCall.java`, `FeralStep.java`, `SavageHunger.java`.
- Under `src/main/java/net/nightshade/divinity_engine/divinity/blessing/dravak/`: `BattleRush.java`, `Unyielding.java`, `WarlordsWill.java`.
- Under `src/main/java/net/nightshade/divinity_engine/divinity/blessing/vokar/`: `GhastlyAura.java`, `SoulLeech.java`.
- Under `src/main/java/net/nightshade/divinity_engine/divinity/blessing/terra/`: `Rootgrip.java`, `StonewardensEmbrace.java`, `WildheartBloom.java`.
- Under `src/main/java/net/nightshade/divinity_engine/divinity/blessing/mechanos/`: `OverdriveLoop.java`, `PrecisionImpact.java`, `TinkersGrace.java`.
