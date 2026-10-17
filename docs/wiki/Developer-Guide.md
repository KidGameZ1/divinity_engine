# Developer Guide

## Start With A Complete Project

The API reference is not the first step. Download a runnable starter, follow its
tutorial, then use the reference pages while expanding it.

1. [Download an addon template](Addon-Templates.md): JSON + KubeJS or standalone Java, including build/install instructions.
2. [Build a JSON + KubeJS addon](JSON-KubeJS-Tutorial.md): exact folders, a god, blessing, trial, custom logic, testing and distribution.
3. [Build a Java addon](Java-Addon-Tutorial.md): import dependencies, learn registration, implement behavior, run Forge and build a jar.
4. [Add assets and translations](Assets-and-Localization.md): statue fallback/overrides, resource paths, names and packaging.
5. [Add stories and real blessing upgrades](Stories-and-Upgrades.md): playable chapters, permanent choices, reward eligibility and implementation.

The starter projects are intentionally small enough to understand completely.
They do not silently generate quests or custom worldgen. Add each system only
after you can run and test the previous one.

## Pick The Smallest Tool That Fits

| Goal | Recommended route |
| --- | --- |
| New god, automatic statue, simple abilities, phased arena trial | Startup JSON content |
| JSON-defined content with programmed blessing behavior | JSON definitions + KubeJS server scripts |
| Prefer declaring that content in JavaScript instead of JSON | KubeJS startup declarations + server scripts |
| New stories, chapters, lore or relationships | Existing narrative datapack format |
| Arbitrary gameplay events, custom curses, new objective handlers or renderers | Java addon |

**For a programmable JSON addon, use JSON for definitions and KubeJS for custom
blessing logic.** JSON is a bounded behavior format, not a programming language.
Its built-in actions work without KubeJS, but conditions and custom behavior use
KubeJS server callbacks (or a Java addon). JSON does not run commands, scripts or
arbitrary NBT. KubeJS callbacks are trusted server code, not a sandbox for
untrusted pack downloads.

## First Working Addon

1. Install Divinity Engine and the compatible [KubeJS dependencies](KubeJS.md) in an isolated Forge 1.20.1 test instance.
2. Install the [combined JSON + KubeJS example](https://github.com/KidGameZ1/divinity_engine/tree/main/examples/json-kubejs-content): JSON definitions on both sides, the callback on the server, and the resource pack on the client. Fully restart both sides.
3. Contact Aster through the automatically created statue, earn favor, and equip Guiding Breath.
4. While injured and standing in water, activate Guiding Breath. The script heals 2 health points; Divinity applies the configured cooldown and visual after success. On dry land or at full health it does nothing.
5. Edit the server callback to program your behavior, leaving registry identity and metadata in JSON. Add stories through a narrative datapack when needed.

For fixed built-in actions, KubeJS is unnecessary: use
[`examples/json-content`](https://github.com/KidGameZ1/divinity_engine/tree/main/examples/json-content)
instead. [`examples/kubejs-content`](https://github.com/KidGameZ1/divinity_engine/tree/main/examples/kubejs-content)
shows the alternative of declaring content in startup JavaScript.
Choose one example: all three use the same Aster IDs. The combined example needs
no startup script; re-registering its JSON IDs in JavaScript fails startup.

The current custom callback API covers content blessings. JSON gods and trials
use the built-in systems; arbitrary god/trial callbacks, curses and new objective
handlers still require Java. KubeJS support is not a general scripting hook for
every mod system.

## The Model To Learn

Definitions are shared, immutable descriptions. `BaseGodInstance` and
`BlessingsInstance` hold an individual player's state. Trial sessions own active
arenas. Narrative capability data owns journal progress and permanent choices.

The server validates and changes gameplay; the client displays synchronized
state. A visual particle is never authoritative evidence of a hit. A definition
must not store one player's health, cooldown or objective counter in a shared field.

Namespaced IDs are save identities. Change translations freely, but do not rename
released god, statue, blessing, trial or story IDs in an existing world without a
migration. Make world backups before changing a content pack.

## Build From Source

Use Java 17. The current development dependencies are Core `1.6.0`, Vanguard
`2.2.1-1.20.1`, and Player Animator `1.0.2-rc1+1.20`. Core and Vanguard resolve
from local Maven in this source setup; build/publish their matching Forge projects
locally first. Do not assume these coordinates are available from Maven Central.

```sh
./gradlew compileJava --console=plain
./gradlew jarJar --console=plain
```

The default output directory is
`~/.gradle/workspace-builds/divinity-engine/forge-1.20.1`.
Override it with `-PdivinityBuildDir=...` when appropriate. The unclassified jar
is the dependency bundle; `-slim.jar` requires separately installed dependencies.
