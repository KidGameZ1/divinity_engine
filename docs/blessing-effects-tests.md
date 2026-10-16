# Blessing Effects Verification

## Scope

`BlessingVisualGameTests` contains 23 opt-in effects-only GameTests. Enable
`-PgameTestNamespaces=divinity_effects`, not the story/UI/trial namespaces.
Forge 47 filters templates by namespace, so the subscriber registers only this
class explicitly while reusing `divinity_engine:pocket_dimension`.
A successful Gradle exit or zero discovered tests is not a passing suite.

## Coverage

- Real registry coverage: 18 gods, 90 distinct blessing IDs and correct bindings.
- Idempotent common effect/primitive/material registration without client loading.
- All 900 blessing/cue recipes: identity, palettes, vanilla sprites, bounds,
  selected channels, and Core NBT spec/carrier round trips.
- Ninety separately authored particle-only designs. No built-in combat meshes,
  empty designs, or duplicate registrations.
- All 90 presentations differ structurally even after excluding god colors,
  seeds, counts and identity; a recolored shared recipe cannot pass this check.
- Signed transfer direction for drains, cooldown relay, pulls and reflected
  projectiles; dash wakes trail actual movement and blocked contact recoils.
- Eighteen shrine responses: six-particle vanilla-sprite bursts, no glow, bounded
  lifetime, retained god palette/sprite, and Core codec validation.
- All 1,530 wrong-god combinations plus namespace mismatch rejection.
- Radius/link clamps, signed endpoints, world/block/entity anchors, direction
  normalization, nullable links, and invalid finite/null input rejection.
- Long particle lines must advertise culling bounds enclosing their full path.
- Malformed Core codecs either reject or recover safe optional-field defaults.
- Private packet round trips and malformed wire rejection: counts, IDs,
  bounded payload, TTL, dimension/anchor, carrier consistency, and collision caps.
- Budget intervals, burst allowance, owner independence, global owner windows,
  free rejections, key-table capacity/expiry, and clock rollback.
- Real server handle creation and cleanup, invisibility concealment without
  another request, owner separation, ordinary expiry, and unchanged gameplay data.
- All 90 designs actually spawn exactly their selected channels through the
  server facade, then remove only their own handles.

Recipe assertions follow authored duration/count/size, not god-wide choreography.
Effects last 8-32 ticks, with a single bounded burst and particles expiring two
ticks before their emitter. Alpha fades to zero; glow/bloom are disabled.
Each author may choose different shapes and motion. Links are not automatically
forced into particle lines. Supported behaviors exclude child emitters.
Core registries validate each complete emitter.

Private scouting uses a single owner-only emitter, with strict TTL <=32 and
16KiB/spec. Tracker's Mark and Veilpiercer remain 24-tick designs. Legacy pairs
are still codec-tested for addon compatibility. Synthetic wire tests exercise
other valid recipes; they do not make other blessings into scouting effects.

Lifecycle fixtures create inert pigs and invoke visual hooks only. They never
post gameplay death events or clear the entire effect store. Cleanup targets
only fixture handles. Natural expiry checks use per-ID reads after real ticks,
not synthetic time jumps. Invisibility uses the actual status effect.

## Packaged Gallery

`BlessingEffectsSmokeTest` and `docs/tools/run_effects_preview.mjs` run against a
checksummed reobfuscated JarJar in an isolated copied save. They cover all 90
actual blessing IDs in 54 scenes: 18 gods times normal, shader-fallback, and
reduced-motion passes. Each scene uses representative mechanical cues, and only
the design's populated channels are spawned.

CPU diagnostics exercise 900 ID/cue combinations at three detail levels and
require zero combat geometry. They separately verify the persistent portal and
camera-proximity distance helpers. Every referenced sprite is loaded and checked
for visible texels. Live checks cover shader selection, real Core particle output,
one channel per blessing, screenshot writes, natural expiry, and options restoration.

A private-state preflight invokes the actual client bridge locally and verifies
that a public Core snapshot cannot erase private local handles. It checks
individual removals, expiry, and rejection of an expired replay. This is not
a multiplayer packet-delivery test.

## Limits

These tests do not play through all combat proc conditions. Actual multiplayer
recipient routing, private target rotation, third-party shader packs, and
detached teleport retention across dimension changes need manual coverage.
The level-wide 128-handle and 256-owner saturation limits are not stress-tested.
CPU geometry diagnostics do not establish frame-time performance.
An interrupted gallery is not a passing 54-scene run.

The gallery's tick assertions run at LOWEST priority, after Core's particle
reconciliation, to avoid checking stale pre-cleanup metrics on a time jump.

## Vanilla-Particle Baseline (2026-10-16)

### Ability-Specific Follow-Up

The subsequent ability-by-ability audit passed Java 17 compilation, all 23
effects-only GameTests, and reobfuscated JarJar on October 16, 2026
(`/tmp/divinity-ability-particles-check.log`). The packaged gallery then passed
all 54 scenes for 90 recipes with no cleanup errors
(`/tmp/divinity-ability-particles-gallery.log`). Solarius and Vokar captures were
visually inspected. These are synthetic visual/lifecycle checks, not a playthrough
of every gameplay proc or a multiplayer transport test.

The separate JSON/KubeJS authoring changes are not covered by this particle result.

Before the ability-by-ability follow-up, Java 17 compilation, all 20 then-current
effects GameTests and JarJar passed in `/tmp/divinity-vanilla-particles-final-build.log`.
The packaged gallery passed all 54 scenes using 90 particle-only designs and 35
vanilla sprites; shader fallback, reduced motion, private-handle preservation,
natural expiry and client option restoration passed with no cleanup errors.
The log is `/tmp/divinity-vanilla-particles-gallery-final.log`.
Representative Ignar, Terra, Nerai and Aethon screenshots were visually inspected,
including fallback and reduced-motion captures. An earlier gallery attempt failed
because capture occurred after short effects expired; the corrected capture timing
was rebuilt and tested in a fresh synthetic save. Gameplay durations were unchanged.

These results describe that baseline, not later edits; they do not replace a fresh
build and focused verification of subsequent ability-specific refinements.

## Historical Verification (2026-09-23, Superseded Art)

- Java 17 compilation, all 19 effects-only GameTests, JarJar, and reobfuscation
  passed: `/tmp/divinity-effects-bespoke-verified-build.log`.
- The final packaged gallery passed all 54 scenes and all geometry regressions:
  `/tmp/divinity-effects-bespoke-verified-gallery.log`.
- Detailed report and PNGs are in
  `/Users/kaydenalexis/.gradle/workspace-runs/nightshade-production-client-qa/nightshade-production-smoke-gdb4ln2q/divinity-effects-smoke/run-14832198259696118870/`.
- The report confirms 35 particle-only, eight mesh-only, and 47 combined designs,
  2,700 detail cases, 8,100 expiry checks, and 2,700 motion-zero comparisons.
  Private-state preflight, natural expiry, and options restoration passed;
  cleanup errors and geometry budget drops were zero.
- Representative PNGs were visually reviewed. This remains a synthetic effects
  test, not a playthrough of all 90 blessing activations or multiplayer transport.

The earlier 18-test god-motif pass predates this architecture. An initial bespoke
gallery attempt stopped at a pre-cleanup metric check; the event-order fix and
subsequent complete runs are recorded separately, not counted as a pass retroactively.
