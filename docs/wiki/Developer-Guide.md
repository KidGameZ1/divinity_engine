# Developer Guide

## Pick The Smallest Tool That Fits

| Goal | Recommended route |
| --- | --- |
| New god, automatic statue, simple abilities, phased arena trial | Startup JSON content |
| Same content plus custom blessing conditions/logic | KubeJS startup + server scripts |
| New stories, chapters, lore or relationships | Existing narrative datapack format |
| Arbitrary gameplay events, custom curses, new objective handlers or renderers | Java addon |

JSON is a bounded behavior format, not a programming language. It does not run
commands, scripts or arbitrary NBT. KubeJS callbacks are trusted server code,
not a sandbox for untrusted pack downloads.

## First Working Addon

1. Install the full Divinity Engine Forge 1.20.1 jar in an isolated test instance.
2. Follow [JSON Content](JSON-Content.md) and install the Aster example plus its resource pack.
3. Contact Aster through the automatically created statue, earn favor, and equip Guiding Breath.
4. Verify its healing, cooldown and trial before adding more content.
5. Add a narrative datapack or a [KubeJS callback](KubeJS.md) only when needed.

The repository includes complete alternatives under
[`examples/json-content`](https://github.com/KidGameZ1/divinity_engine/tree/main/examples/json-content)
and [`examples/kubejs-content`](https://github.com/KidGameZ1/divinity_engine/tree/main/examples/kubejs-content).
Do not install both alternatives with the same IDs: duplicate registrations fail startup.

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
