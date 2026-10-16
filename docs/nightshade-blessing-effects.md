# Bespoke Nightshade Blessing Effects

Each of the 90 built-in blessings has its own explicit presentation keyed by
full blessing ID. Gods share palettes, not finished effects. There is no
god/variant mesh generator or automatic ring applied to every activation.
Gameplay values, inputs, favor, cooldowns, summons, and damage are unchanged.
All 90 built-in ability designs use vanilla Minecraft particle sprites through
Nightshade's independent particle engine. No combat mesh is emitted. The usable
Pocket Dimension portal keeps its separate persistent surface; it is not an
activation effect.

## Design

`BlessingVisualDesigns` supplies a separate particle recipe per blessing.
Authors choose emission region, trajectory, vanilla sprite, and physics. An
emission shape positions particles; it does not draw a geometric shape.
The optional mesh API remains available for addons, but built-in combat no
longer uses it.

Compositions follow mechanics: a ward surrounds its owner, ignition sheds
embers at its victim, and a healing drain travels from victim to caster.
Hooks run after actual success checks. Cosmetic effects add no gameplay powers.

The art direction uses small crisp sprites, short bursts, and vanilla textures.
`divinity_engine:particle_alpha` uses alpha
blending, not additive whitewash. The `blessing_pixel` shader samples sprite
texel centers without halo/bloom. Disabling shaders uses vanilla
`position_tex_color`; fallback can be softer but preserves colors.

Exact inventories:

- [Celestial, dream, time, knowledge, and venom](blessing-effects-celestial.md)
- [Elemental, light, and wind](blessing-effects-elements.md)
- [Earth, spirit, hunt, and war](blessing-effects-earth-spirit.md)
- [Optional addon geometry and persistent portal](blessing-effects-rendering.md)

## Lifecycle And Budgets

Server hooks synchronize only selected channels through Nightshade Core.
No client claims activation or creates a second gameplay copy.

- Effects last 8-32 ticks, with one burst of at most 16 particles per emitter.
- Each owner admits at most 12 effects per 20 ticks; each level holds at most
  128 live handles. Each built-in design now costs one handle, not a mesh/emitter pair.
- Optional addon geometry is bounded to 1,920 vertices, with lower distance/quality budgets.
- Core particle settings, reduced motion, reduced flashing, and shader fallback
  remain supported. Saturated cosmetic budgets never interrupt gameplay.
- Death/logout removes owned effects. Dimension travel clears attached effects
  and private marks; detached teleport departures expire naturally.
- Sustained auras/trails are removed when their owner becomes invisible.
- Private scouting uses owner-only packets and Core local handles, not public
  snapshots. Marks clear on expiry, death, logout, and world changes.
- Effects never load chunks, mutate blocks, damage entities, or spawn gameplay
  projectiles. Existing lightning/explosion gameplay is preserved.
- Shrine presence uses six vanilla-sprite particles lasting 30 ticks, with no
  continuous ring or bloom. Existing per-shrine throttling remains in place.

## Addon API

Register common definitions during enqueued common setup. Register mesh
composers only on the client. Built-ins use the same public API.

```java
ResourceLocation god = new ResourceLocation("example", "aurora");
ResourceLocation blessing = new ResourceLocation("example", "polar_guard");
BlessingVisualProfiles.registerTheme(god, new BlessingVisualProfiles.Theme(
    "aurora", 0xC080FFD0, 0xA0388BC0, 0xE0E0FFFF,
    new ResourceLocation("minecraft", "textures/particle/spark_0.png")));
BlessingVisualProfiles.registerStyle(blessing,
    new BlessingVisualProfiles.Style(god, 0));
BlessingVisualDesigns.register(blessing, BlessingVisualDesign.builder()
    .duration(20)
    .particles(8, 0.08, (particles, context) -> particles
        .cylinder(0.45, 0.8, 0, context.lifetime())
        .velocity(new Vec3(0, -0.025, 0), new Vec3(0.01, 0.005, 0.01)))
    .build());
```

This example deliberately uses particles only. Add `.mesh()` only when needed,
then register its bespoke client composer through
`BlessingEffectRenderer.register(blessing, YourEffects::polarGuard)`.
Legacy `motif`/`variant` fields remain compatible metadata; neither selects a
finished mesh. Unregistered designs have no generic visual fallback.

Authors receive cue, bounded radius, normalized forward, optional full signed
world link, and lifetime. Linked particles are anchored at the segment start;
their author chooses the trajectory. The facade reapplies burst and alive caps
after authoring. Do not add child emitters or authoritative collisions.

Call hooks after successful server checks:

```java
BlessingVisuals.pulse(instance, owner, BlessingVisuals.Cue.WARD);
BlessingVisuals.at(instance, owner, victim.getBoundingBox().getCenter(),
    BlessingVisuals.Cue.IMPACT, 1.25);
BlessingVisuals.link(instance, owner, victim.position(), owner.position(),
    BlessingVisuals.Cue.HEAL);
```

Cues: `CAST`, `IMPACT`, `WARD`, `HEAL`, `TRAIL`, `AURA`, `CONTROL`,
`TELEPORT`, `MARK`, `RECALL`. They communicate events, not universal shapes.
An ability can have several presentations: a successful heal, its ongoing buff,
and a hit against an enemy must not all use the same burst. Use the affected
entity/block position for impacts and growth, feet for landing dust, and signed
source-to-recipient links for drains, retaliation, movement and reflection.
Invisibility gets a brief fixed departure effect, not a trail exposing the player.
World positions are snapshots. Links retain endpoints up to 48 blocks; radii
clamp to 0.25-12 blocks. Client-side hooks are ignored.

`privateMarks(instance, owner, targets)` requires a server player and a particle
design. Built-ins send a single owner-only emitter; legacy addon pairs remain
accepted by the packet codec. Query detection at most once per second. Four marks rotate
per second in bounded windows. Never broadcast confidential scouting with
`at(...)`.

`recipe(...)` creates specs without spawning. Preview callers must respect
`renderMesh()` and `emitParticles()`; `handleCount()` gives actual cost.
Wrong-god bindings are rejected.

## Focused Verification

Use Java 17 and only the effects namespace:

```sh
./gradlew runGameTestServer jarJar -PgameTestNamespaces=divinity_effects --offline --console=plain
```

The packaged gallery covers definitions, rendering, shader fallback, reduced
motion, and expiry using synthetic effects. It is not a combat playthrough.
The effects suite compares all 90 presentations with color, seed, count and
identity excluded, so recoloring a shared template cannot satisfy coverage.
Separate direction checks cover healing transfers, cooldown relay, pulls,
reflections and movement wakes, including very short contact segments.
Multiplayer packet delivery, every proc condition, and third-party shader packs
still require manual testing.
