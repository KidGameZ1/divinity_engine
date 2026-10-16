# Elemental Blessing Effects

## Scope and Contract

All 30 IDs below belong to `divinity_engine` and have independent particle
authors in
`src/main/java/net/nightshade/divinity_engine/util/divinity/effects/ElementalBlessingDesigns.java`.
Each uses vanilla Minecraft sprites through Nightshade: no combat mesh, custom
sprite, bloom, child emitter, repeated burst, or authoritative particle collision.
Sprite names below mean `minecraft:textures/particle/<name>.png`.

This audit reads the actual six blessing packages, not names or old descriptions.
Only the elemental design, cosmetic hooks in those packages, and this document
are owned here. Shared facade, renderer, tests, persistent portals, other authors,
and all pre-existing dirty-tree work remain parent-owned.

Shared contracts read before editing: `BlessingVisualDesign`, `BlessingVisuals`,
and cached Nightshade Core 1.6.0 `ParticleEmitterSpec`/`RenderParticleEngine`.
The facade owns counts, alpha, size curves, material, admission, cleanup, server
authority, and normalized forward. Genuine links start at their source. Local
directional feedback uses the parent-owned overload
`at(instance, owner, position, cue, radius, direction)`, with no link, so movement
direction does not require a fake source/destination or owner-facing fallback.

The parent-supplied six-argument overload was read and verified after addition:
it passes direction through the existing guarded emit/recipe path with the world
anchor unchanged and link null. The four call sites use this API; no shared code
is edited by this audit.

TTL below is emitter duration; particle lifetime is TTL minus 2. Counts remain
4-16, sizes .055-.12, emitter durations 10-28, and particle lifetimes 8-26 ticks.
Only Windcaller's Reflex changes duration, from 8 to 10, to eliminate its previous
6-tick particles. No gameplay duration, timer, cooldown, chance, damage, health,
status flag, target filter, return value, lightning spawn, or portal is changed.
Additional victim cues are inside existing gameplay iterations; no loops or
per-tick scans are added. Parent admission can coalesce repeated/multi-target cues.

## Voltira

| ID / Hook | Actual mechanic and qualifying hook | Cue, placement, and particle mapping | TTL / count x size | Changed in this audit |
| --- | --- | --- | --- | --- |
| `stormstep` / `Stormstep.java` | Press assigns horizontal dash or crouched vertical leap; vertical branch still returns false. Elytra/slow-falling rules and visual lightning remain. | `TRAIL` signed feet-to-feet-plus-applied-movement link; `spark_1` line drifts opposite travel. | 12 / 10 x .06 | Retained: already follows actual movement rather than full look, including vertical launch. |
| `shockshield` / `Shockshield.java` | Source exists, chance succeeds, and retaliation hurt returns true. Incoming damage is not canceled. | `WARD` owner-center-to-attacker-center link with outward `spark_2`; separate tight `IMPACT` at attacker center. | 12 / 8 x .065 | Retained: retaliation, not an invented damage-blocking shield. |
| `tempest_wrath` / `TempestWrath.java` | Server kill callback damages and lifts other entities in the configured owner-centered AABB; weather changes damage. Valid empty area still succeeds. | `IMPACT` at captured owner feet, configured radius; low `spark_1` cylinder, upward kick, outward repulsion and gravity. | 18 / 14 x .075 | Retained: source is owner, not slain target; sparse discharge is not a replacement hitbox. |
| `charge_relay` / `ChargeRelay.java` | Outgoing hit actually reduces positive cooldown of slotted Stormstep/Tempest Wrath and writes the slot. One request after the existing loop. | `RECALL` from struck target center to owner center; source-side `spark_0` travels along the signed cooldown-return link. | 16 / 6 x .055 | Retained: return direction correctly points toward cooldown recipient, not victim. |
| `lightning_conduit` / `LightningConduit.java` | Sprinting player's 1% roll finds a target; effect follows successful lightning insertion. Creeper visual-bolt/direct-damage exception stays intact. | `IMPACT` at the actual lightning block-origin position; narrow upward `spark_3` cone settles back. | 12 / 8 x .065 | Retained: impact follows actual strike position, not assumed target-center or caster position. |

## Cryonis

| ID / Hook | Actual mechanic and qualifying hook | Cue, placement, and particle mapping | TTL / count x size | Changed in this audit |
| --- | --- | --- | --- | --- |
| `frozen_heart` / `FrozenHeart.java` | Frozen ticks were positive before unconditional zeroing. | Owner-body `WARD`; `generic_0` frost grains fall away from thawed body. | 12 / 8 x .065 | Retained: thaw feedback only, not a freezing aura. |
| `icy_pulse` / `IcyPulse.java` | Direct-owner outgoing hit; existing nearby non-owner target lacks recorded cooldown and accepts slowness. Cooldown writes/removal order is untouched. | `CONTROL` owner feet + .05 to target feet + .05; `generic_2` low frost stitch follows the accepted slow link and settles. | 18 / 10 x .065 | Retained: shows accepted slows, not damage or a freeze the hook never applies. |
| `snowveil` / `Snowveil.java` | Server snow/cold-enough-to-snow condition, non-spectator, specialization eligibility, and reveal window allow entry. Swinging/damage reveal; owned six-tick invisibility refresh respects other sources. | One world-anchored `CAST` at feet + 45% body height, radius .65; white `generic_4` entry puff. No attached aura or movement trail. | 10 / 8 x .085 | Documentation corrected: current hook requires neither crouching nor rain. Code and reveal behavior retained. |
| `frostbite_mantle` / `FrostbiteMantle.java` | Living attacker receives original 200 frozen ticks and slowness after incoming attack. | `CONTROL` at attacker feet + .08; compact `generic_0` ankle frost draws inward and falls. | 20 / 10 x .065 | Retained: retaliatory frost is on attacker, not owner. |
| `permafrost_surge` / `PermafrostSurge.java` | Server press freezes other living entities in existing 6-block AABB for 600 ticks and attempts slowness. Superclass return unchanged. | Existing `CONTROL` at owner feet + .02 spreads low `generic_2` outward; new `IMPACT` at each affected target's feet + .18 contracts ankle frost. | 28 / 16 x .08 | Added victim-local freeze branch in existing loop so elevated/distant victims are not represented only by ground dust around caster. Original nonempty-area gate retained. |

## Nerai

| ID / Hook | Actual mechanic and qualifying hook | Cue, placement, and particle mapping | TTL / count x size | Changed in this audit |
| --- | --- | --- | --- | --- |
| `aquatic_grace` / `AquaticGrace.java` | Water/bubble branch grants dolphin grace/water breathing; specialization A's submerged-eyes timer heals only when due; specialization B swim modifier unchanged. | `AURA` at eyes - .12 raises `bubble`; owner-body `HEAL` uses compact rising `bubble_pop_0` only when health actually increased. | 16 / 6 x .075 | Retained: breathing and actual restoration have different particle patterns; no false heal at full health. |
| `rains_favor` / `RainsFavor.java` | Water/rain/bubble condition; missing regeneration is successfully added, or regeneration remains present while wet. | Owner-body `HEAL` uses hopping `splash_0` for regeneration grant, not direct health gain; `AURA` above head + .1 uses descending `drip_fall`. | 20 / 6 x .065 | Retained: wet passive is not limited to rain and does not claim direct healing each tick. |
| `wavebound` / `Wavebound.java` | In-water press assigns full-look dash velocity. | `TRAIL` center-to-center-plus-applied-velocity link; `splash_1` wake peels backward and falls. | 16 / 12 x .08 | Retained: full signed movement, including downward/upward swimming. |
| `undercurrent_grip` / `UndercurrentGrip.java` | Server water/bubble cast pulls eligible living noncreative targets in full 3D and attempts slowness. Empty cast succeeds. | `CONTROL` from target feet + .25 to owner feet + .25 sends source-side `bubble` inward; owner `CAST` is a contracting `bubble_pop_1` cluster. | 20 / 12 x .075 | Retained: unlike Tempest Spiral, this actual pull includes signed vertical displacement, so its true source/recipient link remains correct. |
| `tidecallers_wrath` / `TidecallersWrath.java` | Server water/bubble cast sets eligible targets' outward X/Z knockback with fixed +.6 Y, including targets below owner. Empty cast succeeds. | Directed `IMPACT` at target feet + .5; local `splash_2` box moves along actual knockback. Existing owner `CONTROL` remains broad outward/upward splash. | 20 / 16 x .09 | Replaced caster-to-target link, which could point downward, with victim-local directed motion. No link remains on impact; no extra lift is added to its authored velocity. |

## Zephra

| ID / Hook | Actual mechanic and qualifying hook | Cue, placement, and particle mapping | TTL / count x size | Changed in this audit |
| --- | --- | --- | --- | --- |
| `gale_step` / `GaleStep.java` | Server speed buff; existing horizontal-motion-squared > .001 gate. Specialization B fall multiplier remains .75, with its existing qualifying fall cue. | Directed `TRAIL` at feet + .04 uses `generic_2` dust opposite horizontal movement; `WARD` at landing feet kicks dust upward. | 12 / 6 x .06 | Trail uses actual horizontal velocity, not gaze. Backpedaling/strafe work; looking up/down cannot inject vertical trail direction. Landing branch unchanged. |
| `windcallers_reflex` / `WindcallersReflex.java` | Projectile impact while original cooldown guard is clear grants speed and cancels impact. | `WARD` on incoming .4-block segment ending at ray-trace contact; four `sweep_2` puffs separate with strong drag. | 10 / 4 x .12 | Cosmetic duration 8 -> 10, giving particle lifetime 8 instead of 6. Incoming path/cancellation retained; no reflected projectile implied. |
| `cyclone_leap` / `CycloneLeap.java` | Press sets Y to .7, preserves X/Z velocity, marks hurt, and grants slow falling. | `TRAIL` signed launch link from feet; `generic_5` dust lifts along actual resultant movement and settles. | 24 / 10 x .075 | Retained: lateral launch motion is preserved; no invented horizontal dash. |
| `shearwind_dash` / `ShearwindDash.java` | Server airborne press changes X/Z but preserves Y; unchanged nearby AABB, not a ray, damages/lifts eligible victims in any direction. | Existing `TRAIL` link uses actual dash velocity and `generic_1`; directed victim-center `IMPACT` uses local `sweep_1` flecks along assigned knockback. | 14 / 8 x .085 | Impact direction now radial outward/+Y even for victims beside/behind caster, not owner's facing. Trail, targets, damage, and hurt flags retained. |
| `tempest_spiral` / `TempestSpiral.java` | Server cast assigns inward X/Z with fixed +.1 Y and deals magic damage in existing AABB. Empty cast succeeds. | Directed victim feet + .15 `IMPACT` uses local `generic_3` dust along actual movement; owner `CONTROL` dust contracts and rotates. | 22 / 14 x .075 | Replaced full target-to-owner link: elevated victims now show upward lift rather than a false downward pull. Victim branch has no orbit; only origin dust rotates. |

## Lumen

| ID / Hook | Actual mechanic and qualifying hook | Cue, placement, and particle mapping | TTL / count x size | Changed in this audit |
| --- | --- | --- | --- | --- |
| `beacons_grace` / `BeaconsGrace.java` | Brightness predicate grants speed; specialization B resistance and original targeting-cancellation predicates unchanged. | Feet + .04 `AURA`: low `generic_0` buff motes; protected owner-center `WARD`: rising `enchanted_hit` body cylinder, once even if both predicates match. | 18 / 6 x .06 | Aura has neutral upward drift instead of look-dependent forward motion; standing/looking down does not produce a directional attack. |
| `cleansing_flame` / `CleansingFlame.java` | Outgoing hit removes at least one of the eight original negative effects; all eight calls still execute. No healing or fire assignment. | Owner-body `HEAL`: body-sized box of rising `generic_5` cleansing motes in Lumen palette. | 18 / 8 x .085 | Removed native orange flame/white override because this proc does not ignite. Restored inherited god palette. Cue still describes cleansing, not health gain. |
| `divine_radiance` / `DivineRadiance.java` | Press applies owner resistance and attempts blindness on original nearby alive non-owner targets. Sound/server branch unchanged. | Owner-center `WARD`: compact rising `enchanted_hit` cylinder; new `CONTROL`: small `enchanted_hit` cloud at eyes of each target that accepted blindness. | 24 / 12 x .09 | Replaced wide generic repelled pulse with separately placed resistance/blindness cues. Original addEffect call executes once per target, with return used only for cosmetic gating. |
| `lightbearers_oath` / `LightbearersOath.java` | Undead or original EnderMan/Endermite/EnderDragon/Shulker class matches receive 1.2x outgoing damage. Not restricted to melee. | `IMPACT` at struck center; tight `enchanted_hit` contact box with short neutral vertical drift. | 10 / 5 x .10 | Removed owner-look diagonal slash: damage multiplier does not create a new sword cut or knockback and may trigger on ranged damage. |
| `dawnshatter` / `Dawnshatter.java` | Server cast checks feet-to-feet dot product against 60-degree cone inside 6-block-inflated AABB; only original undead/monster and creative/spectator filters receive damage/ignition. | Feet-rooted facing-aligned `CAST` cone uses `enchanted_hit`; actual target-center `IMPACT` uses rising native `flame`. | 20 / 14 x .08 | Cast origin moved down 1.5 blocks to actual angular-test origin. Cone basis, half-angle, target gates, ignition, and native impact material retained. |

## Ignar

| ID / Hook | Actual mechanic and qualifying hook | Cue, placement, and particle mapping | TTL / count x size | Changed in this audit |
| --- | --- | --- | --- | --- |
| `blazing_hands` / `BlazingHands.java` | Specialization-aware ignition roll succeeds, then original target fire ticks are assigned. | Target-center `IMPACT`: tight rising native `flame` cone. | 18 / 8 x .085 | Retained: real initial ignition, not caster fire or an area explosion. |
| `fireproof` / `Fireproof.java` | Original IN_FIRE/ON_FIRE/HOT_FLOOR/LAVA source cancels damage; no extinguish is invented. | `WARD` at feet + .16 for hot floor/lava, owner-body otherwise; neutral-gray `generic_0` ash sheds and falls. | 14 / 8 x .065 | Retained: contact placement and prior ash palette remain; no flame attack or new immunity branch. |
| `flash_flare` / `FlashFlare.java` | Press damages, ignites, and attempts blindness on original non-owner/non-dying AABB targets. Empty cast succeeds. | Existing feet `CAST` emits broad native `flame`; new target-eye `CONTROL` emits short `generic_5` flecks in Ignar palette only on accepted blindness. | 22 / 16 x .11 | Added distinct eye-local blind feedback in existing loop instead of representing both ignition and blindness solely with a ground flame burst. No bloom/full-screen flash. |
| `kindling_fury` / `KindlingFury.java` | Outgoing hit on already-burning target multiplies damage by 1.5; no extra knockback. | Target-center `IMPACT`: native `lava` embers kick upward from existing fire and fall. | 10 / 8 x .065 | Removed owner-look forward bias; actual mechanic amplifies a burning hit, not a directional shove. |
| `inferno_mantle` / `InfernoMantle.java` | Existing toggled server scan finds other living entities in 3-block AABB; only not-burning branch sets fire. | Owner-feet `AURA` while target set nonempty: low native `flame` bed. Newly ignited target-center `IMPACT`: taller/faster flame cone. | 20 / 10 x .085 | Retained: separate nearby heat/actual ignition and no fake repeated ignition on already-burning victims. |

## Semantic Regression Expectations

Use `BlessingVisuals.recipe` with actual cues rather than codec roundtrips alone.
All shape IDs below have `nightshade_core` namespace. Box extents are half-extents;
box radius metadata is their Euclidean norm. Unspecified motion acceleration,
inherited-anchor velocity, and angular velocity are zero. `f` below is normalized
`Context.forward()`. Verify directed-at hook arguments separately from recipes.
Tempest Spiral's synthetic/legacy linked `IMPACT` uses normalized link instead of
`f`, retaining positive motion dot link; its real hook uses unlinked directed-at.

| Blessing / exact cue | Exact shape and sprite | Initial velocity; spread; gravity; drag | Behavior / anchor expectation |
| --- | --- | --- | --- |
| `permafrost_surge / IMPACT` | cylinder radius min(r,.4), height .3; `generic_2` | (0,-.012,0); (.008,.004,.008); .002; .96 | attractor strength +.003; target feet + .18 |
| `permafrost_surge / CONTROL` | cylinder radius min(r*.4,2.4), height .045; `generic_2` | (0,.055,0); (.055,.015,.055); .003; .97 | unchanged attractor -.006; owner feet + .02 |
| `tidecallers_wrath / IMPACT` | box (.2,.12,.2); `splash_2` | f*.14; (.015,.012,.015); .003; .96 | no behavior/link; target feet + .5; f from applied (push.x*1.3,.6,push.z*1.3) |
| `tidecallers_wrath / CONTROL` | cylinder radius min(r*.4,2.4), height .08; `splash_2` | (0,.09,0); (.08,.02,.08); .009; .96 | unchanged attractor -.01 at owner + .5 |
| `gale_step / TRAIL` | box (.2,.025,.16); `generic_2` | normalize(f.x,0,f.z)*-.05 + (0,.02,0); (.025,.004,.025); .003; .9 | no link; feet + .04; f from horizontal movement, not look |
| `gale_step / WARD` | same box/`generic_2` | (0,.065,0); (.025,.004,.025); .008; .9 | unchanged landing response; independent of f |
| `windcallers_reflex / WARD` | actual incoming link line; `sweep_2` | normalize(link)*.02; (.055,.035,.055); 0; .78 | duration 10, lifetime 8, count 4; link still ends at impact contact |
| `shearwind_dash / IMPACT` | box (.06,.28,.08); `sweep_1` | f*.07; (.025,.015,.025); .002; .88 | no link; target center; f from assigned (knock.x,.3,knock.z), not look |
| `tempest_spiral / IMPACT` | box (.18,.08,.18); `generic_3` | f*.065; (.009,.004,.009); 0; .96 | no behavior/link; target feet + .15; f from assigned (pull.x,.1,pull.z) |
| `tempest_spiral / CONTROL` | cylinder radius min(r*.35,1.3), height .35; `generic_3` | (0,.018,0); (.012,.008,.012); 0; .94 | unchanged orbit .11 and attractor +.004, owner feet only |
| `beacons_grace / AURA` | box (.18,.035,.14); `generic_0` | (0,.012,0); (.006,.004,.006); .002; .95 | no behavior, independent of look |
| `cleansing_flame / HEAL` | box (.22,.4,.18); `generic_5` | (0,.04,0); (.016,.008,.016); -.0005; .96 | inherited Lumen palette, not WHITE; owner-body pulse |
| `divine_radiance / WARD` | cylinder radius .33, height 1.2; `enchanted_hit` | (0,.018,0); (.008,.006,.008); 0; .95 | no behavior; owner center, hook radius .85 |
| `divine_radiance / CONTROL` | box (.18,.08,.18); `enchanted_hit` | (0,.006,0); (.008,.004,.008); 0; .9 | no behavior; accepted-blindness target eyes |
| `lightbearers_oath / IMPACT` | box (.12,.2,.12); `enchanted_hit` | (0,.018,0); (.025,.02,.025); .002; .9 | no slash/line, independent of look; target center |
| `dawnshatter / CAST` | oriented cone radius min(r,6), spread tan(pi/6); `enchanted_hit` | f*.12; (.02,.02,.02); 0; .96 | anchor exactly owner feet; basis forward=f, orthonormal right/up including vertical look |
| `dawnshatter / IMPACT` | box (.12,.2,.1); native `flame` | (0,.045,0); (.015,.018,.015); -.0003; .95 | unchanged WHITE palette; actual ignited target center |
| `flash_flare / CONTROL` | box (.16,.07,.16); `generic_5` | (0,.008,0); (.012,.006,.012); 0; .88 | inherited Ignar palette, no behavior; accepted-blindness target eyes |
| `flash_flare / CAST` | cylinder radius min(r*.4,2), height .14; native `flame` | (0,.12,0); (.18,.035,.18); .012; .94 | unchanged WHITE palette and attractor -.012 at owner feet |
| `kindling_fury / IMPACT` | box (.08,.12,.08); native `lava` | (0,.055,0); (.055,.035,.055); .009; .9 | unchanged WHITE palette; target center, independent of look |

Compare opposite owner looks with the same actual motion: Gale Step trail should
not rotate, and Shearwind/Tidecaller/Spiral victim effects should remain aligned
with the assigned velocity. Test targets above/below and behind caster: Tidecaller
and Spiral initial Y must stay positive, while Undercurrent's genuine 3D pull may
have either sign. Check genuine Stormstep/Wavebound/Cyclone links, Charge Relay's
recipient direction, and Windcaller's incoming contact segment remain unchanged.

For new target cues, reject false success: Divine Radiance/Flash Flare do not emit
`CONTROL` when addEffect returns false; Permafrost still emits frost after its
unconditional frozen-tick assignment regardless of slowness acceptance. Empty
casts and superclass return values retain their previous behavior. Do not assert
that every multi-target request is delivered: shared cue budgets are unchanged.

## Bounds, Materials, and Validation

- All 30 remain particle-only, one author each, one burst per emitter, maximum alive equal to count, glow zero and bloom OFF. They retain the parent's alpha/material/size curves and vanilla billboard renderer.
- God palettes remain inherited except existing real native fire/lava, white Snowveil snow, and neutral-gray Fireproof ash. Cleansing Flame now inherits Lumen because its source proves no ignition; new Flash Flare blind motes inherit Ignar while its real ignition stays native.
- Cylinders/boxes/cones/lines distribute sprites, not rendered combat geometry. Local area clouds are sparse approximations, never claimed to equal AABB hitboxes, cone range, or collision-resolved destinations.
- Only origin Tempest Spiral dust orbits. Its rotation is cosmetic dust circulation, not invented rotational victim movement. No linked recipe gains orbit or child emission.
- Snowveil remains a detached entry puff. Existing facade invisibility suppression, budgets, cleanup, owner attribution, and server-only transport stay authoritative. No private scouting is introduced.
- No vanilla particle-engine loops, per-tick searches, gameplay projectiles, damage, block mutations, or physics callbacks are added. Original burning/freezing visuals, sounds, lightning behavior, status particle/icon flags, and portals are preserved.
- Read-only source checks verify 30 unique definitions/authors, counts 4-16, particle lifetime 8-26, documented ID parity, and sprite existence in cached Minecraft 1.20.1 client.jar. Baseline comparisons distinguish this pass from pre-existing dirty-tree edits.
- No Gradle, compiler, Minecraft, world, renderer, or integration test is run here. Parent owns semantic test implementation and integration. Client readability, terrain intersection, multiplayer delivery, accessibility settings, shader fallback, and cue-budget behavior need parent verification.
