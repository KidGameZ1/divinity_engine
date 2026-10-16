# Automatic God Statues

Register each god once. `GodsDeferredRegister` declares the god, its statue block,
its block item, and its block entity type together. Built-in gods use the same
implementation as addons. Registry objects are resolved lazily; no constructor
or setup pass guesses a god's statue by name.

## Addon Example

```java
import java.awt.Color;
import java.util.Set;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegistryObject;
import net.nightshade.divinity_engine.api.gods.GodsDeferredRegister;
import net.nightshade.divinity_engine.divinity.gods.BaseGod;

public final class ExampleGods {
    public static final GodsDeferredRegister GODS =
            GodsDeferredRegister.create("example_pantheon");

    public static final RegistryObject<BaseGod> SELENE = GODS.register("selene",
            () -> new BaseGod(Set.of("moon"), Set.of(), Color.WHITE));

    public static void register(IEventBus modBus) {
        GODS.register(modBus);
    }
}
```

Call `ExampleGods.register(modBus)` from the addon mod constructor. Depend on
Divinity Engine in `mods.toml`. Do not create the shared god registry again.
Do not register a second block, item, or block entity for this god.

This generates:

- God: `example_pantheon:selene`
- Block: `example_pantheon:selene_statue`
- Item: `example_pantheon:selene_statue`
- Block entity type: `example_pantheon:selene_statue`

Access handles with `GODS.statue(SELENE).block()`, `.item()`, and `.blockEntity()`.
At runtime `SELENE.get().getStatueBlock()` and `.getStatueBlockEntity()` also work.
Use your own `BaseGod` subclass for custom worship and blessing behavior.

Add `"example_pantheon.gods.selene": "Selene"` to the addon language file.
The translated statue/item name then becomes "Selene Statue". All automatic
statues appear in Divinity Engine's existing statue creative tab.

## Assets And Drops

A hidden, low-priority in-memory resource/data pack supplies a stone statue
model, facing variants, an item model, and a self-drop loot table. The existing
Divinity statue texture/model is reused. No generated files are written to a
world, structure directory, or addon source tree.

Override individual defaults with ordinary addon/resource/datapack files:

```text
assets/example_pantheon/blockstates/selene_statue.json
assets/example_pantheon/models/block/selene_statue.json
assets/example_pantheon/models/item/selene_statue.json
data/example_pantheon/loot_tables/blocks/selene_statue.json
```

## Lifecycle And Compatibility

Forge registers blocks before god instances exist. Automatic content must be
declared before registration events, not discovered by scanning gods during
common setup. Addons using a raw Forge `DeferredRegister<BaseGod>` should switch
to `GodsDeferredRegister` to receive automatic statues. Raw/manual registrations
remain supported, but are not intercepted or mutated after registries freeze.

The old `GodsRegistry.register(name, factory)` method still automatically creates
built-in-namespace statues. Addons should use their own `GodsDeferredRegister`
instead, to avoid ID collisions. Existing manually registered `StatueBlock`
constructors and `BaseGod` statue setters remain available.

For a previously released addon whose statue has a different path, preserve it:

```java
GODS.register("selene", "ancient_moon_shrine", Selene::new);
```

This assumes the existing block, item, and block entity share that ID. If an old
addon uses different IDs for those three objects, retain its manual registration
until it has an explicit compatibility migration. Never silently rename IDs.

`GodsDeferredRegister.using(...)` allows a mod to supply its existing content
registers. All supplied registers must use the same namespace. The god helper
attaches them to the bus; do not also attach them separately. Calling the helper's
`register(bus)` twice on the same bus is harmless. Duplicate god/statue paths and
declarations made after registration starts are rejected.

## Save Safety And Checks

All 18 built-in `divinity_engine:<god>_statue` block, item, and block entity IDs are
unchanged. `facing`, `waterlogged`, block geometry, existing container/custom-name
NBT, and structure palettes are retained. No migration rewrites chunks or NBT.

Run only the statue regression suite:

```sh
./gradlew runGameTestServer -PgameTestNamespaces=divinity_statue_persistence --offline --console=plain
```

The suite runs under `~/.gradle/workspace-runs/divinity-statue-gametest`, not `run`
or any existing user world. Test-only gods are declared only when this opt-in
suite is enabled. It checks all built-in factories/save IDs, chunk packet reloads,
addon namespaces, duplicate registration, generated resources/drops, and rotated
structure round-trips including custom names and all facing/waterlogged states.

Verified on 2026-09-24 with Java 17 / Forge 47.4.2: compilation and all six
statue GameTests passed. The suite also read all 18 packaged shrine templates
and confirmed that generated defaults do not override shipped loot resources.
No player saves or saved structures were opened or rewritten. Client appearance
and a separately packaged third-party addon have not been manually play-tested;
the automated addon coverage uses two isolated test namespaces and this API.
