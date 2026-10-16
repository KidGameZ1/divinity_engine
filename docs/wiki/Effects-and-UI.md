# Effects And UI

## Minecraft-Like Particles Through Nightshade

All 90 built-in blessing designs now use small, individually authored particle
bursts with vanilla Minecraft textures. They use **Nightshade's independent
particle engine**, not vanilla's particle-type physics. A flame texture looks
like Minecraft flame but its trajectory/lifetime is authored explicitly.

God palettes are used where appropriate. Native hearts, flame and soul artwork
can keep their original colors; stone, soil and ash use material colors. Reflection
effects start at contact and follow the reflected projectile. Drains travel toward
the recipient. Movement wakes use actual motion, not merely the owner's gaze.

`BlessingVisuals` is a server-side facade. It handles sync, ownership, budgets,
expiry and invisibility. Use:

```java
BlessingVisuals.pulse(instance, owner, Cue.CAST);
BlessingVisuals.at(instance, owner, target.position(), Cue.IMPACT, 1.0);
BlessingVisuals.link(instance, owner, hitPosition, recipient.position(), Cue.HEAL);
```

For contact motion without a connecting line, the `at` overload accepts a final
`Vec3 direction`. Do not encode a fake far-away endpoint just to rotate an effect.

Register an addon's `BlessingVisualDesign` by blessing ID and, where needed, a
`BlessingVisualProfiles.Theme` by god ID during enqueued common setup. An author
configures `ParticleEmitterSpec.Builder` from the cue, radius, direction and link.
Keep each ability's presentation appropriate to what it actually does.

See [the current visual guide](https://github.com/KidGameZ1/divinity_engine/blob/main/docs/nightshade-blessing-effects.md)
and the three per-blessing audit tables in `docs/blessing-effects-*.md`.
The retained persistent pocket portal is a separate world effect, not a combat
particle burst. Never replace its lifecycle with repeatedly spawning temporary rings.

## Budgets And Cleanup

Built-in bursts use 1..16 particles, last 8..32 ticks and fade out. Cosmetic failure
must not prevent gameplay. Core enforces particle budgets; Divinity additionally
limits owner/cue admissions and active handles. No particle renderer scans mobs
or applies damage.

Private scouting effects are sent only to the owner. World effects and private
effects have separate client ownership, so a public sync cannot erase private
handles. Always remove handles you own, not every effect in a dimension.

## UI

Menus use Nightshade UI integration with synchronized state. Keep inventory slot
semantics, keyboard focus, tooltips and narration. Use small responsive layouts
instead of painting text over controls. A statue-opened blessings menu remains
bound to that god; a UI redesign must not bypass its server validation.

Client rendering classes belong behind client-only setup. Shared content
definitions, capabilities and packets must load on a dedicated server without
`Minecraft` or renderer class initialization. Register renderers once, not per frame.

Use the **Forge 1.20.1** Core documentation for `RenderParticles`, `RenderEffects`,
`RenderRuleRegistry` and UI sessions. Newer NeoForge renderer signatures are not
interchangeable. Shader fallback and reduced-motion settings are supported paths,
not permission to leave a menu blank.
