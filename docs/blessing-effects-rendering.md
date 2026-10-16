# Bespoke Blessing Effect Renderer

## Current Built-In Use

Built-in blessing cues are now particle-only. Normal client setup registers
only the persistent Pocket Dimension portal composer, gated to `MARK` with
`Long.MAX_VALUE` duration. Short-lived portal activation cues also use particles.
The old combat composer classes remain source-compatible but are not registered
or emitted by the built-ins. The geometry API below is retained for addons;
it is not the current built-in combat art pipeline.

## Scope And Registration

`BlessingEffectRenderer` is now only a client-side dispatcher, bounded geometry
writer, and low-level helper library. It contains no god effect catalog.
The legacy celestial, elemental, and earth/spirit combat composer classes are
not called by setup. Only `CelestialBlessingEffects.registerPortal()` is used.

Normal client setup calls `BlessingEffectRenderer.register()`. It installs
`divinity_engine:blessing_manifestation` in Core's
`CustomWorldRendererRegistry` once, then registers the persistent portal once. An
addon registers a mesh author using:

```java
BlessingEffectRenderer.register(blessingId, (mesh, frame) -> {
    // Author this particular blessing's geometry, not a god-wide emblem.
});
```

The public functional interface is
`EffectComposer { void compose(Mesh mesh, Frame frame); }`.
Keys are full `ResourceLocation` blessing IDs; duplicate registrations fail fast.
Groups should be idempotent if they may also be called independently.
`registerBuiltinComposers()` is available for CPU tooling without installing a
Core renderer, accessing Minecraft, or issuing GL commands.
`registeredBlessings()` returns a snapshot of currently registered mesh IDs; it
does not initialize groups by itself.

The common `BlessingVisualDesign`/`BlessingVisualDesigns` declarations
can choose mesh, particles, or both for addons. Built-in cues choose particles;
the pocket ID additionally owns its persistent portal, not a combat composer.
The renderer does not author or emit particles and does not consult
god themes to choose geometry. Unknown, missing, malformed, and unregistered IDs
produce **zero mesh vertices**, even when a cue or link is supplied. There is no
neutral fallback emblem.

The old 18 god-motif functions, god dispatch, variant geometry, universal cue
decorations, and automatically generated link ribbons have been removed. This
is a replacement pipeline, not an extra layer over the previous effects. The
90 actual blessing compositions are explicitly authored, not inferred
from names, slot numbers, palettes, or a god/cue cross product.

## Input Contract

| Input | Meaning |
| --- | --- |
| `BlessingId` | Required full namespaced ID; the sole mesh-composer dispatch key. |
| `God`, `GodId`, `Variant` | Ignored by production geometry dispatch. God palette selection remains parent-owned. |
| `Cue` | Event metadata, exposed as public `BlessingEffectRenderer.Cue`. It does not add geometry or transform the body. |
| `ShapeRadius` | Normalized local geometry scale and body-fit radius, clamped to 0.15..12 blocks. Nonfinite input suppresses rendering. Missing values fall back to Core's context radius for legacy callers. |
| `Radius` | Core's conservative culling extent; not a reason to enlarge the motif. Parent profiles use `max(ShapeRadius * 1.8, linkLength / 2 + 1)`. |
| `context.color()`, `Accent`, `Highlight` | Supplied ARGB inks. Missing secondary inks inherit primary. No god lookup changes the geometry. |
| `DirectionX/Y/Z` | World-space facing, normalized safely. Missing/invalid/zero input falls back to +Z. |
| `LinkX/Y/Z` | Optional full world-space segment, about its midpoint anchor. All three components must be numeric. Length is bounded to 48 blocks. A link does not itself cause any geometry to be drawn. |

Core resolves entity/world anchors and supplies an already camera-translated
pose. The renderer translates by `context.center()` exactly once and draws
anchor-relative vertices. It never subtracts the camera again. Parent identity
effect transforms are recommended when raw geometry must retain world direction.

The public `Frame` exposes immutable fields:

```text
ResourceLocation blessing
Cue cue
int detail, primary, accent, highlight
double radius, phase, progress, fade, seedPhase, motion, growth
Vec3 forward, right, up, link
boolean visible, persistentGateway
```

There are no `god`, `variant`, or cue-dependent `scale` fields.
`count(low, high)` interpolates by detail 0/1/2 and clamps requested counts to
0..128. It does not secretly change counts because a link exists.

Age is level time plus partial tick minus effect start time. Duration is read
from the spec and defensively bounded to 1..60 ticks. Future and expired effects
draw nothing. `phase` and `progress` already honor reduced motion. At motion zero,
phase remains at its seed offset and progress is fixed at 0.45; fade-in/out still
operate. Use these fields for spatial animation rather than independent clocks.

## Local And Raw Geometry

`Frame.point(local)` computes exactly:

```text
(right * local.x + up * local.y + forward * local.z) * radius
```

The basis preserves full facing, including vertical aim, for every cue. There
is no automatic healing lift, mark offset, impact expansion, ward surround,
ground flattening, teleport placement, or trail direction. Composers explicitly
author those semantic decisions. For a world-horizontal ground effect, construct
world offsets and use raw methods rather than relying on cue-dependent transforms.

`Mesh` and `Frame` are public static nested classes with public constructors.
Mesh entrypoints are:

```java
quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color)
triangle(Vec3 a, Vec3 b, Vec3 c, int color)
ribbon(Vec3 a, Vec3 b, double width, int color)
line(Vec3 a, Vec3 b, int color)
rawQuad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color)
rawLine(Vec3 a, Vec3 b, int color)
boolean room(int vertices)
Diagnostics diagnostics()
```

The first four methods take normalized local coordinates and mark every emitted
vertex as local. After composition, only local vertices participate in the
uniform body fit to `ShapeRadius`. The entire local silhouette scales together,
rather than clipping individual vertices to a sphere.

Raw methods take world-oriented offsets in blocks from the anchor. They bypass
`Frame.point` and are **never body-fitted**, even if raw and local calls are
interleaved or a raw quad is emitted after the body. Provenance is recorded per
vertex, not inferred from list position. Thus a drain composer can use:

```java
Vec3 source = frame.link.scale(-0.5);
Vec3 destination = frame.link.scale(0.5);
mesh.rawLine(source, destination, frame.highlight);
```

The composer must first check `frame.link != null`. A real drain can replace that
line with authored moving fragments/ribbons; the example specifies coordinates,
not a shared final effect. Endpoints remain at `center +/- Link/2`, unaffected by
a large local body, radius changes, or facing. Raw geometry must stay inside the
parent's declared culling extent. The shared writer additionally rejects any
nonfinite coordinate or offset component outside +/-64 blocks.

## Reusable Helpers

The following helpers and constants are public static for composers and addons.
They author local geometry unless a composer explicitly uses raw Mesh methods.

```java
block(Mesh m, Vec3 p, double x, double y, double z, int front, int side, int top)
tile(Mesh m, Vec3 center, double halfWidth, double halfHeight, int color)
crystal(Mesh m, Vec3 center, double width, double height, int sides, int surface, int edge)
star(Mesh m, Vec3 center, double radius, int points, double rotation, int color)
leaf(Mesh m, Vec3 p, double angle, double size, int color)
rune(Mesh m, Vec3 p, double size, int glyph, int color)
curve(Mesh m, Path path, int segments, double width, int color)
arc(Mesh m, Vec3 center, Vec3 u, Vec3 v, double radius,
    double start, double sweep, int segments, double width, int color)
Vec3 xy(double angle, double radius)
Vec3 xz(double angle, double radius)
Vec3 square(double angle, double radius)
Vec3 unit(Vec3 value, Vec3 fallback)
double clamp(double value, double low, double high)
double smooth(double value)
```

`Path` is public and functional: `Vec3 at(double t)`.
`TAU`, `UP`, `RIGHT`, and `FORWARD` are public constants.
Curves are capped at 64 segments, arcs at eight, and star/crystal subdivisions
at eight. `rune`'s glyph argument selects one low-level ornament, not a blessing
slot. Three-face blocks remain open relief, not closed opaque cubes. These are
construction tools, not reusable completed sun/ghost/gear effects.

## Bounded Rendering

- Low/medium/high budgets remain 640/1152/1920 generated vertices for the entire composition, including raw links. A quad costs four; a raw line costs four after vanilla line expansion. Admission is primitive-atomic.
- `room(n)` lets authors budget a complete detail group without overflow. Calls exceeding the budget are dropped; no incomplete quad or line is emitted. Helpers bound their subdivision loops.
- Local `line` is a readable four-vertex ink stroke. Facets and raw quads use at most 24% of supplied alpha, local ink strokes 42%, and raw line cores 62%, all multiplied by the lifetime envelope.
- First-person attenuation remains: nearby surfaces are further reduced, and primitives within 0.35 blocks of the camera fade away, returning by 0.85 blocks. No raw GL state or custom shaders are needed.
- Core owns supplied render types and buffer flushing. Quads use POSITION_COLOR; raw lines use position/color/normal. Core's shader-disabled fallback remains supported.
- Detail uses the minimum of Core quality, particle settings, and distance LOD. Beyond 24 blocks detail is at most medium; beyond 48 it is low. HIGH and ULTRA both remain capped at high.
- Screen-effect scale multiplies Core animation strength. Zero freezes spatial animation through Frame phase/progress; reduced flashing further lowers motion and opacity.
- Direction/link normalization scales before taking length, avoiding overflow for huge finite components. Radius, age, world coordinates, emitted coordinates, and subdivision counts are bounded.

## Diagnostics And Integration

The new full-ID entrypoints are:

```java
diagnose(ResourceLocation blessing, String cue, double radius, double ageTicks,
         long durationTicks, double motion, int detail, Vec3 direction, Vec3 link)
diagnose(String fullBlessingId, String cue, double radius, double ageTicks,
         long durationTicks, double motion, int detail, Vec3 direction, Vec3 link)
```

They initialize the persistent portal without registering Core, then use
the exact production generator with fixed palette and seed. The result record
retains its existing accessors:

```text
generatedVertices, quadVertices, lineEndpoints, rejectedPrimitives,
budgetDrops, maximumAnchorDistance, geometryFingerprint
```

The old `diagnose(String god, String cue, int variant, ...)` overload remains only
as a gallery adapter. It resolves a full blessing ID through
`BlessingVisualProfiles.styles()`, then delegates to full-ID dispatch. Unknown
god/slot combinations return zero, not a fallback mesh. Variant never reaches
Frame or a composer. Particle-only slots legitimately share an empty fingerprint.

Validation should use the common design's mesh flag and registration
presence, not insist that every blessing/cue has vertices. In addition to ID
coverage and distinct mechanic presentations, check:
- Unknown IDs, malformed IDs, and particle-only blessings emit zero even with a link.
- Local body fitting leaves interleaved raw quads AND raw lines unchanged.
- Changing facing/radius affects local geometry but not authored raw endpoints.
- `quadVertices + 2 * lineEndpoints == generatedVertices <= budget`.
- Reduced-motion geometry is stable across interior lifetime samples; alpha is intentionally excluded from the fingerprint.
- Future/expired effects, invalid inputs, long links, and all three LODs remain safe.
- Private targeting effects remain recipient-scoped by the parent; this renderer does not discover targets.

See `blessing-effects-tests.md` for the effects-only verification record and
remaining manual coverage. Historical combat-mesh gallery results do not
validate the current particle-only art.
