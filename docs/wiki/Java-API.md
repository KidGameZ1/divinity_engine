# Java API

New to addon development? Start with the [complete Java tutorial](Java-Addon-Tutorial.md)
and its [downloadable project](Addon-Templates.md). This page is a compact API reference,
not a replacement for project setup.

Use Java when the bounded JSON behaviors are insufficient. All gameplay callbacks
still need server-thread checks and per-player state. Never register world content
from a tick or a datapack reload.

## Gods And Automatic Statues

```java
public final class ExampleGods {
    public static final GodsDeferredRegister GODS = GodsDeferredRegister.create("example_pantheon");
    public static final RegistryObject<BaseGod> HAVEN = GODS.register("haven",
            () -> new BaseGod(Set.of("Refuge"), Set.of(), new Color(0xB5D5EF)));

    public static void register(IEventBus modBus) {
        GODS.register(modBus);
    }
}
```

Import `GodsDeferredRegister` from `net.nightshade.divinity_engine.api.gods`,
`BaseGod` from `divinity.gods`, and normal Forge/Java types. Call `register` in
your addon constructor. Declare blessings before the god references them.

The helper owns the god, statue block, block item and block-entity registration.
Do not separately create a second statue type. Use `register(godPath, statuePath,
factory)` to retain a previously released statue ID when migrating an addon.
See [automatic-statues.md](https://github.com/KidGameZ1/divinity_engine/blob/main/docs/automatic-statues.md)
for assets, old-save concerns and a complete example.

## Blessings And Curses

Register `Blessings` through a Forge `DeferredRegister` targeting
`divinity_engine:blessings` in your namespace. Subclass `Blessings`, then override
the required callbacks such as `onPressed(BlessingsInstance, LivingEntity)`.
Constructor cooldowns are **seconds**; `BlessingsInstance` cooldown values are
**ticks**. Returning true from supported behavior callbacks starts the ordinary
cooldown. Do not repeat a press action on key release.

Registered blessings are singleton definitions. Put counters in
`instance.getOrCreateTag()`, not mutable fields on the definition. Check the
bound god and eligibility through existing helpers; do not grant a blessing
because a client packet says it is equipped.

Curses remain the Java `Curse` / `CurseInstance` API with the
`divinity_engine:curses` registry. This update does not introduce a JSON curse
interpreter or silently replace an addon's curse implementation.

## Trials

```java
GodTrialsDeferredRegister trials = new GodTrialsDeferredRegister("example_pantheon");
trials.register("trial_of_refuge", () -> new GodTrial(ExampleGods.HAVEN.get(), RefugeTrial::new));
GodlyTrialsRegistry.addDeferredRegister(trials);
```

Declare this during enqueued common setup after the god registry is populated.
`GodTrial` and `GodTrialsDeferredRegister` are in `api.trials`;
`GodlyTrialsRegistry` is in `registry.divinity.trial`.

An encounter subclass of `GodlyTrial` supplies `onStart`, `runPhase`,
`onComplete`, `getMaxPhases`, and only necessary tick logic. Use `trackEntity`
for spawned mobs and `setPhaseInfo`/`setObjectiveProgress` for authoritative UI.
Use `nextPhase`, `completeTrial` and `failTrial`; do not delete arenas, teleport
players or maintain a separate active-session list yourself.

Public runtime entry points are `TrialSessionManager.startTrial(player, god)`,
`getSession(player)` and `failSession(player, reason)`. Cleanup is idempotent and
batched. Leaving a trial fails it; there is no suspended encounter to resume.

## Stories And Codex

The [connected-pantheon authoring guide](https://github.com/KidGameZ1/divinity_engine/blob/main/docs/connected-pantheon-authoring.md)
documents version-2 story JSON, Java builders, custom server objective handlers,
stable chapter IDs, migration, optional crossovers and specialization integration.
Examples include a Java addon and equivalent datapack.

`DivineNarrativeDeferredRegister` extends the existing registration style.
Stories can have independent IDs even when bound to the same god. God lookup is
the compatibility primary-story helper; new integrations should use story IDs.

The original `DivineCodexPage`, `DivineCodexPageDeferredRegister` and
`DivineCodexPagesRegistry` remain available. Register public pages once; build
player-specific discoveries in journal snapshots instead of putting them in a
shared static page list. See
[Codex validation](https://github.com/KidGameZ1/divinity_engine/blob/main/docs/codex-pages-validation.md).

Java code may also call `DivinityContent.registerGod/registerBlessing/registerTrial`
with `(ResourceLocation, JsonObject)` during construction. It uses the same strict
startup model as JSON and KubeJS. Do not call it after Forge registration begins.
