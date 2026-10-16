# Celestial Blessing Particles

## Scope And Contract

All 30 celestial definitions in `src/main/java/net/nightshade/divinity_engine/util/divinity/effects/CelestialBlessingDesigns.java` remain particle-only: `mesh=false`, `particles=true`, one explicit author per blessing. This pass audited the current Solarius, Nashara, Elaris, Nythea, Kairoth and Aethon hook packages, plus read-only inspection of `RewindHistory`, `PocketDomainManager`, `PocketPortals` and the shared particle API. It does not implement proposed ascension kits.

Every sprite comes from `minecraft:textures/particle/*.png`, emitted through Nightshade rather than Minecraft particle spawning. Boxes, lines, cones and cylinders are emission distributions, not rendered combat surfaces. No custom sprite, mesh, shader glow, bloom, scan, collision callback, subemitter, continuous emission or extra renderer is added. Existing gameplay Glowing effects/tags are unchanged by the no-render-glow requirement.

Per-recipe durations/counts are unchanged: 8..28 ticks and 2..12 particles, within the 8..32 / <=16 contract. The shared facade retains a single burst, maximum-alive cap, duration-minus-two lifetime, visibility, admission throttling, transport, cleanup and seeded cosmetic randomness. Palette exceptions below preserve recognizable vanilla material colors. No recipe changes anchors or adds handles. Shared code, tests, gallery and renderers are outside this pass.

## Current Mechanics To Effects

These are live hook cues, not a claim that every preview cue is a gameplay trigger. Effects remain subject to the shared per-owner/per-blessing budget; an area hit does not guarantee a visible burst on every affected entity. `WHITE` means a white multiplier retaining the sprite's own colors. Other colors inherit the god palette unless explicitly listed below.

| God / Blessing | Current mechanic and live cue | Duration / Count | Particle result and audit outcome |
| --- | --- | --- | --- |
| Solarius / `radiant_strike` | Daytime hits ignite ordinary targets for 3s; undead receive +10 damage and 10s fire. | 12t / 6 | **Changed:** ordinary ignition is now `AURA`, a rising WHITE `flame` cylinder. Undead bonus damage stays `IMPACT`, a diagonal `critical_hit` cut. AURA is an event burst, not a new tick effect. |
| Solarius / `sunskin` | Daytime heal of 0.02 health per tick below maximum; `HEAL` only on an actual increase, every 40 ticks while visible. | 10t / 2 | **Changed:** tiny WHITE `heart` sprites rise from the healed body instead of generic glitter. |
| Solarius / `sunburst` | Entities in the owner's bounding box inflated by 5 receive blindness, fire and outward/upward push. | 16t / 12 | **Changed:** `CAST` is a shallow area box of WHITE `flame`; `IMPACT` starts at the struck body and travels with actual push. Successful blindness separately gets stationary eye-level `glint` on `CONTROL`. |
| Solarius / `solar_ward` | Daytime low-health activation grants absorption and existing Glowing; specializations adjust threshold/duration. `WARD` requires accepted absorption and visibility. | 18t / 5 | **Changed:** `enchanted_hit` rises through a body-covering cylinder, not a facing-only shield. It represents absorption, not projectile cancellation. |
| Solarius / `corona_ascendant` | Toggled in daytime, newly ignited nearby entities get 2s fire (`AURA`); successful on-hit blindness gets `IMPACT`. | 10t / 5 | **Changed color only:** low rising WHITE `flame` for ignition; retained brief eye-level lateral `glint` for blindness. No continuously renewed owner halo. |
| Nashara / `toxic_skin` | Removes poison when present; visible owner receives `WARD` only after successful removal. | 12t / 5 | **Retained:** body-sized `drip_fall` cylinder sheds poison downward with gravity, rather than advertising permanent immunity. |
| Nashara / `serpents_bite` | Applies Poison II only to an unpoisoned hit target; A extends duration, B also attempts Weakness. | 12t / 4 | **Retained:** `IMPACT` is a narrow lateral `drip_hang` puncture distribution falling almost straight down after accepted poison. The cue does not claim optional Weakness succeeded. |
| Nashara / `shadowstep` | Damage triggers random 5-block displacement, then 60t invisibility; a living attacker may receive Poison III. | 10t / 5 | **Refined:** unlinked `TELEPORT` smoke (`generic_2`) at old feet drifts only upward, without a look/path hint. Separate unlinked `IMPACT` venom (`drip_land`) remains at the poisoned attacker. Never an arrival or owner-to-attacker link. |
| Nashara / `venomblood` | Only a direct living attacker already on fire is poisoned on damage. | 12t / 7 | **Retained:** `IMPACT` is a rising `drip_fall` cone at that attacker, turning downward under stronger gravity. The burning condition belongs to the attacker, not the blessed owner. |
| Nashara / `coiled_retribution` | At <=50% health, incoming damage from a living source is multiplied by 0.75 and poison is attempted on that source. | 14t / 5 | **Refined:** defender `WARD` retains a shallow settling `critical_hit` band. Successful attacker poison is `IMPACT` with falling `drip_fall`; removed unrelated owner-look velocity. |
| Elaris / `insightful_kill` | A player kill spawns an additional orb worth the victim's experience reward +3; `MARK` only after the orb is added. | 16t / 3 | **Retained:** `sga_e` glyphs rise from the victim/orb position. This public reward cue is not a private scouting mark or an invented XP collection path. |
| Elaris / `foresight` | Existing dodge counter cancels eligible damage or projectile contact; canceled projectiles have velocity zeroed. | 10t / 4 | **Changed:** projectile `CONTROL` is a stationary `enchanted_hit` plane perpendicular to incoming velocity at contact; damage `WARD` is a stationary lateral line at the visible defender. No fake dodge displacement or reflection. |
| Elaris / `scholars_edge` | Hits against a full-health target receive +15% damage. | 8t / 3 | **Retained:** precise short diagonal `enchanted_hit` cut on `IMPACT`, without spread. |
| Elaris / `mind_mirror` | An existing 45% roll can remove a harmful effect and attempt to apply it to the struck entity. | 18t / 5 | **Retained:** linked `CONTROL` carries `spell_4` positively from owner to recipient only when both operations succeed and owner is visible. Application-only `IMPACT` sinks at the recipient; removal-only `WARD` rises at the visible owner. Invisible owners never gain a public transfer link. |
| Elaris / `arcane_recall` | Eligible positive uncanceled damage with zero cooldown requests a safe same-dimension position rewind of 40 ticks. It does not restore health. | 20t / 6 | **Changed:** `TELEPORT` starts `generic_0` dust in a short source segment and moves it positively along the actual rewind displacement. Not a historical walking route or heal. Shared history hooks are untouched. |
| Nythea / `twilight_veil` | Eligible nighttime speed increase, optional night vision, and crouching invisibility. | 8t / 2 | **Changed direction only:** visible, non-crouching movement gets dim `generic_3` heel dust on `TRAIL`, opposite actual horizontal movement rather than look. Existing 40t gate remains. No invisibility aura or new transition cue. |
| Nythea / `lull_of_the_lost` | At night, the existing chance/cooldown clears a targeting event, then the existing delayed thread attempts restoration. It does not apply sleep. | 18t / 3 | **Retained:** `CONTROL` smoke (`generic_5`) expands gently above the affected mob's eyes. No sleeping icon, owner link or extra scan. |
| Nythea / `phase_blink` | Four checked night-only zigzag teleports with existing invulnerability and sleeps. | 10t / 8 | **Retained:** each `TELEPORT` uses that dash's actual signed segment, `generic_0` on a full line with negative-dot-link wake velocity. Invisible owners emit no path. |
| Nythea / `silver_mirror` | Within its existing time/count window, direct living attacks are reduced to 75%; the attacker is hurt for 25% of that reduced amount. | 12t / 5 | **Changed:** `CAST` raises an oriented `glint` plane; mitigation `WARD` settles a body cylinder; successful returned damage `IMPACT` scatters falling glints locally at the attacker. No invented projectile reflection or owner-bearing link. |
| Nythea / `moondrinker` | Nighttime sky-visible heal of 0.05 health per tick; `HEAL` only on an actual increase, every 40 ticks while visible. | 16t / 3 | **Changed:** small WHITE `heart` sprites descend from above the eyes instead of water-like drops, distinguishing healing from poison or rain. |
| Kairoth / `second_chance` | Cancels death without an offhand totem and restores 50% max health (60% with A); B attempts 100t absorption. | 28t / 10 | **Extended:** retained rising totem-family `glitter_5` on `HEAL`. Accepted B absorption separately emits `WARD`, a tighter `enchanted_hit` body cylinder. Both retain visibility gates. |
| Kairoth / `echo_step` | Requests a safe same-dimension position rewind of 60 ticks; failed history gives the existing message. | 20t / 6 | **Retained:** stationary shrinking `generic_1` dust spans the actual source-to-destination `TELEPORT` segment. Not a replay of every historical footstep. |
| Kairoth / `predicted_path` | Existing nearby-danger scan grants Speed II for 4s. | 8t / 3 | **Changed:** `TRAIL` heel dust (`generic_0`) lies and drifts behind actual horizontal movement, never toward a detected entity. Existing visible/non-crouching/moving/40t conditions remain. No new scan. |
| Kairoth / `threadsever` | A critical hit attempts Weakness II for 100t. | 12t / 6 | **Changed:** `IMPACT` is a downward-sloping line of sinking `spell_4`, colored with vanilla Weakness, rather than a generic critical-damage explosion. Only accepted Weakness emits. |
| Kairoth / `fateful_draw` | Selects one missing Strength/Speed/Resistance effect with existing RNG and applies level II for 100t. | 20t / 5 | **Refined:** actual Strength `IMPACT` stays a compact rising `critical_hit` burst; Speed `TRAIL` starts at feet and drifts backward as `generic_0`; Resistance `WARD` stays a settling `enchanted_hit` cylinder. No reroll. |
| Aethon / `veilpiercer` | Crouching queries existing nearby targets, filters to 16 blocks and sends bounded owner-only marks every 20 ticks. | 24t / 3 | **Retained exactly:** static `glitter_0` target-center `MARK` motes, alpha 0.65, through `privateMarks`. No public mark, owner cue, target change or new scan. |
| Aethon / `celestial_ward` | An eligible incoming projectile is stopped and its impact canceled; B attempts a 1-health heal. | 14t / 5 | **Changed anchor/shape:** `WARD` is an incoming-normal `enchanted_hit` contact plane whose fragments recoil against incoming motion; the projectile is not reflected. Actual visible healing gets separate WHITE `heart` on `HEAL`. |
| Aethon / `starlit_mirror` | Within existing time/count rules, redirects projectile velocity toward its living shooter, marks it glowing and cancels that impact. | 14t / 5 | **Retained:** unlinked `CAST` settles a narrow `glint` plane; linked `WARD` starts at contact and travels positively along newly assigned projectile velocity, not owner look. |
| Aethon / `gravity_fold` | Existing airborne Space-key path temporarily sets no-gravity using its current counter. | 8t / 6 | **Changed:** `TRAIL` `generic_2` motes are suspended, with zero velocity/spread/gravity and drag 1, in a shallow cylinder just below feet. Existing 12t/visibility gating remains; no falling debris or platform mesh. |
| Aethon / `pocket_dimension` | Opens storage/control menus or requests exit; the domain manager emits short cues at actual transfer endpoints. | 24t / 6 | **Corrected:** live `TELEPORT` is upward `generic_0` dust on a 1.4-block vertical line starting at each endpoint's feet. No live menu `CAST`, cross-dimension link or owner aura. Persistent MARK gateways are a separate, unchanged path. |

## Mismatches Fixed Now

- Radiant Strike no longer conflates ordinary ignition with undead bonus damage. Sunburst distinguishes cast, knockback and accepted blindness; knockback follows actual push, not owner look.
- Sunskin/Moondrinker/Celestial Ward healing now uses hearts instead of glitter/water. Flame colors remain native. Threadsever presents its actual Weakness status, with the vanilla status color.
- Solar Ward now covers the body for absorption. Second Chance distinguishes accepted optional absorption from restored health. Silver Mirror distinguishes activation, mitigation and successful returned damage.
- Foresight no longer implies a displacement or reflection that never occurred. Celestial Ward starts recoil fragments at contact instead of upstream. Starlit Mirror retains actual outgoing velocity.
- Gravity Fold now suspends dust instead of dropping it during no-gravity. Twilight Veil and Predicted Path follow actual movement; Fateful Draw speed moved from chest to feet.
- Arcane Recall now travels to the rewind endpoint, unlike static Echo Step dust. Shadowstep departure smoke and Coiled Retribution venom no longer use unrelated owner-look motion.
- Removed the stale Pocket Dimension menu-CAST claim and changed its distribution to rise from, not straddle, the foot anchor. Persistent gateways remain untouched.

## Exact Semantic Test Fixtures

`L` is a nonzero link passed to `recipe`, `D = normalize(L)`, and `H` is normalized horizontal direction with +Z fallback for a vertical/zero projection. Shapes use namespace `nightshade_core`. Values describe authored initial motion, before Nightshade applies per-tick gravity/drag. All sprite names use the vanilla prefix/suffix above.

| Blessing / exact cue | Expected shape, sprite and motion |
| --- | --- |
| `radiant_strike/AURA`, no link | `cylinder(radius=.18,height=.25)`, `flame`, WHITE; velocity `(0,.045,0)`. |
| `radiant_strike/IMPACT`, no link | `line(length=.42)`, `critical_hit`, inherited palette; forward hit motion, not flame-only. |
| `sunburst/CAST`, no link | `box(x=.72*radius,y=.045,z=.72*radius)`, `flame`, WHITE; velocity `(0,.055,0)`. |
| `sunburst/IMPACT`, direction link | `line(length=.18,direction=D)`, `flame`, WHITE; velocity `.08*D`, spread `(.012,.006,.012)`, gravity `.003`, drag `.9`. Hook starts at victim center, link `.4` along normalized `(difference.x*.5,.4,difference.z*.5)`. |
| `sunburst/CONTROL`, no link | `line(length=.28)`, `glint`; zero velocity/spread/gravity, drag 1. Successful blindness at victim eyes. |
| `sunskin/HEAL` | `box(.16,.18,.12)`, `heart`, WHITE; velocity `(0,.015,0)`, alpha `.55 -> 0`. |
| `solar_ward/WARD` | `cylinder(.32,.6)`, `enchanted_hit`; velocity `(0,.012,0)`, not a facing plane. |
| `foresight/CONTROL`, incoming link | Oriented `box(.18,.18,.015)`, forward basis `D`, `enchanted_hit`; zero velocity/spread/gravity, drag 1. Source is contact; incoming is captured before projectile velocity is stopped, link length `.3`. |
| `foresight/WARD`, no link | `line(length=.38)` along horizontal right; `enchanted_hit`; zero velocity/spread/gravity, drag 1. Visible defender damage cancellation only. |
| `mind_mirror/CONTROL`, transfer link | `line(length=min(.12,length(L)),direction=L)`, `spell_4`; velocity `L/16`, zero spread/gravity, drag 1. Positive dot-link. |
| `mind_mirror/WARD` versus `/IMPACT`, no link | Oriented `box(.14,.20,.045)`, `spell_4`; vertical velocity `+.025` for removal WARD, `-.025` for application IMPACT. |
| `arcane_recall/TELEPORT`, rewind link | `line(length=min(.12,length(L)),direction=D)` or point when coincident; `generic_0`; velocity `L/18`, zero spread/gravity, drag 1. Positive dot-link for nonzero rewind. |
| `shadowstep/TELEPORT`, **no link** | `box(.22,.025,.22)`, `generic_2`, size `.09 -> .14`, alpha `.32 -> 0`; velocity `(0,.018,0)`. Old foot position only. |
| `shadowstep/IMPACT`, **no link** | `box(.13,.10,.13)`, `drip_land`; velocity `(0,.028,0)`, gravity `.006`. Poisoned attacker only. |
| `twilight_veil/TRAIL`, movement link | `box(.13,.012,.09)`, `generic_3`; velocity `-.008*H`. Hook starts at feet +.06Y, link `.25` along horizontal movement. |
| `phase_blink/TELEPORT`, actual dash link | Full `line(length=length(L),direction=D)` or point if coincident, `generic_0`; velocity `-.025*D`, negative dot-link. |
| `silver_mirror/CAST`, no link | Oriented `box(.23,.3,.035)`, `glint`; velocity `(0,.012,0)`. |
| `silver_mirror/WARD`, no link | `cylinder(.23,.38)`, `glint`; velocity `(0,-.018,0)`. |
| `silver_mirror/IMPACT`, no link | `box(.14,.19,.14)`, `glint`; zero base velocity, spread `(.025,.0125,.025)`, gravity `.004`. Local returned-damage fragments; no link. |
| `moondrinker/HEAL` | `box(.09,.06,.09)`, `heart`, WHITE; velocity `(0,-.022,0)`, alpha `.55 -> 0`. |
| `second_chance/HEAL` versus `/WARD` | HEAL: `cylinder(.28,.9)`, `glitter_5`, velocity `(0,.025,0)`. WARD: `cylinder(.32,.6)`, `enchanted_hit`, velocity `(0,.012,0)`. WARD requires accepted B absorption. |
| `echo_step/TELEPORT`, rewind link | Full `line(length=length(L),direction=D)` or point if coincident; `generic_1`; zero velocity/spread/gravity, drag 1. |
| `predicted_path/TRAIL`, movement link | `line(length=.4,direction=-H)`, `generic_0`; velocity `-.015*H+(0,.002,0)`. Hook starts at feet +.06Y, link `.35` along horizontal movement. |
| `threadsever/IMPACT` | Down-sloped `line(length=.5,direction=right+(0,-.4,0))`, `spell_4`; velocity `(0,-.035,0)`, vanilla Weakness palette. |
| `fateful_draw/TRAIL` versus `/WARD` versus `/IMPACT` | TRAIL: backward `line(.34)`, `generic_0`, velocity `-.03*H`, foot anchor. WARD: `cylinder(.23,.38)`, `enchanted_hit`, velocity `(0,-.012,0)`. IMPACT: `box(.12,.08,.12)`, `critical_hit`, velocity `(0,.028,0)`. |
| `veilpiercer/MARK`, private | Unchanged `box(.4*radius,.3*radius,.025)`, `glitter_0`, alpha `.65 -> 0`, stationary; exactly 24t, 3 particles. Never replace private delivery with public `at`. |
| `celestial_ward/WARD`, incoming link | Oriented `box(.22,.22,.015)` with forward basis `D`, `enchanted_hit`; velocity `-.035*D`, negative dot incoming; spread `(.008,.004,.008)`, gravity `.001`, drag `.9`. Source is **contact**, second endpoint contact + normalized incoming * `.6`. |
| `celestial_ward/HEAL`, no link | `box(.14,.18,.08)`, `heart`, WHITE; velocity `(0,.012,0)`, alpha `.55 -> 0`. |
| `starlit_mirror/WARD`, reflected link | `line(length=min(.16,length(L)),direction=D)`, `glint`; velocity `.055*D`, positive dot-link, drag `.95`. Contact -> contact + normalized newly assigned projectile velocity. CAST remains unlinked. |
| `gravity_fold/TRAIL`, no link | `cylinder(.25,.012)`, `generic_2`; zero velocity/spread/acceleration/gravity, drag 1. Anchor feet -.06Y. |
| `pocket_dimension/TELEPORT`, **no link** | `line(length=1.4,direction=UP)`, `generic_0`; velocity `(0,.012,0)`. Separate world-anchored endpoints, never a segment across dimensions. |

Oriented contact boxes now use an orthonormal 3D basis from the supplied direction, including vertical incoming projectiles. Horizontal movement remains flattened. Short direction-only links are not paths: authors emit at the source and do not draw the full link. The shared facade owns source reanchoring, including nonnull short links. This implementation requires no new directed-at API.

### Palette Exceptions

- `radiant_strike/AURA`: WHITE for native `flame`.
- `sunburst/CAST` and `/IMPACT`: WHITE for native `flame`; `/CONTROL` retains Solarius colors.
- `corona_ascendant/AURA`: WHITE for native `flame`; `/IMPACT` retains Solarius colors.
- `sunskin/HEAL`, `moondrinker/HEAL`, `celestial_ward/HEAL`: WHITE for native `heart`.
- `threadsever/IMPACT`: one-color palette `0xFF000000 | MobEffects.WEAKNESS.getColor()`, cycle 40. This is the vanilla effect color, not a thematic palette.

Arbitrary-cue codec tests also invoke fallback branches: Sunskin/Moondrinker always select hearts, Threadsever always selects Weakness color, Corona selects flame for non-IMPACT, and Sunburst selects flame for non-CONTROL. Validate exceptions against the resulting sprite and blessing, rather than assuming only live cues exist. All other celestial recipes retain inherited palettes.

## Preservation And Verification

Changed hook files: `solarius/RadiantStrike.java`, `solarius/Sunburst.java`, `elaris/Foresight.java`, `nythea/TwilightVeil.java`, `kairoth/PredictedPath.java`, `kairoth/FatefulDraw.java`, `kairoth/SecondChance.java`, `aethon/CelestialWard.java`, `aethon/GravityFold.java`. All are under `src/main/java/net/nightshade/divinity_engine/divinity/blessing/`. The only other edited files are this document and `CelestialBlessingDesigns.java`. Nashara hooks, all other hooks, shared facade, registry, tests, gallery, renderer, history and portal implementation were not edited here.

No damage, heal amount, return value, RNG call/order, target selection, gameplay timer, status duration, cooldown or persistent data write changed. Newly captured `addEffect` return values only guard cosmetics. Existing invisibility and movement gates remain; no public private-mark delivery or invisible-owner link was added. Persistent portal geometry/lifecycle is unchanged.

Timing quirks are deliberately preserved: Silver Mirror compares elapsed milliseconds with its existing stored duration; Phase Blink retains its three 500ms sleeps; Lull retains its delayed thread and wall-clock gate; Gravity Fold retains its counter increments/key query. Visuals do not promise repaired timing or additional movement/healing mechanics.

Validation in this pass: Java 17 parse-only check of all 30 hooks plus the design file (31 units, no parse diagnostics); baseline comparison of all 30 hooks after stripping cosmetic-only calls/locals confirms unchanged remaining source. Static checks passed for 30 unique authors, exact six-god profile/document coverage, unchanged durations/counts, bounds, no mesh/shared-policy overrides and Veilpiercer's 24t/3 contract. All 17 used sprites exist in the cached Minecraft 1.20.1 client jar. Signed-motion and basis arithmetic passed six horizontal/oblique/vertical/near-vertical/short-link cases; this checks the formulas, not runtime emitter execution. Scoped whitespace checks passed. No Gradle, Minecraft, world launch, runtime rendering, full type-check or GameTest was run. Parent semantic tests should cover the fixtures above, especially opposite-look incoming/movement directions, vertical/zero links, failed applications, invisible owners, private MARK delivery and unchanged persistent portals.
