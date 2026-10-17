# Downloadable Addon Projects

These are **complete source starter projects**, not extra mod jars to drop into
`mods/`. Download and unzip one into a working directory, then follow its README.
Both target the **current Divinity Engine source update, Forge 1.20.1 / 47.4.2**.
Older builds with the same Divinity version label can lack these APIs.

| Project | Download | What you get |
| --- | --- | --- |
| JSON + KubeJS | [Download starter ZIP](https://github.com/KidGameZ1/divinity_engine/raw/refs/heads/main/downloads/divinity-json-kubejs-starter.zip) | Aster god/statue, JSON blessing/trial, working server callback, translations, installation layout and focused script tests |
| Java / Forge | [Download starter ZIP](https://github.com/KidGameZ1/divinity_engine/raw/refs/heads/main/downloads/divinity-java-starter.zip) | Standalone Gradle project, wrapper, local dependency importer, Haven god/statue, Java blessing, trial, recipe and translations |

[Checksums and file inventory](https://github.com/KidGameZ1/divinity_engine/blob/main/downloads/addon-templates.json)
identify each archive. Source lives in
[`templates/`](https://github.com/KidGameZ1/divinity_engine/tree/main/templates).
The ZIPs contain no game saves, cached dependencies, Minecraft binaries, or API keys.
The Java archive includes the Gradle wrapper and its third-party license notices.
Starter example code is MIT-licensed for reuse; that permission does not extend
to the separately obtained game or library dependencies.

## Choose A Route

**JSON + KubeJS:** you want an addon/modpack without compiling Java. JSON declares
your IDs, god, blessing metadata and phased trial. KubeJS programs the blessing's
custom server behavior. Install KubeJS, Rhino and Architectury separately; Divinity
does not bundle them. Follow the [complete JSON + KubeJS tutorial](JSON-KubeJS-Tutorial.md).

**Java:** you want arbitrary Forge events, custom god or trial logic, curses,
objective handlers or renderers. The starter imports development dependencies
from a full Divinity jar you already obtained, so private Maven repositories are
not required. JDK 17 is required. Follow the [complete Java tutorial](Java-Addon-Tutorial.md).

The examples use different god namespaces. Still choose one route for your own
content: never register the same god or blessing through both JSON and JavaScript
or Java. Neither starter automatically creates a story, shrine worldgen, or
mechanical specialization merely because a god exists. Those are explicit next
steps in [Stories and Upgrades](Stories-and-Upgrades.md) and
[Assets and Localization](Assets-and-Localization.md).

## What To Send To Players

| Route | Development input | Player/server deliverable |
| --- | --- | --- |
| JSON + KubeJS | The editable starter folder | Documented instance-folder overlay, with matching definitions on both sides, scripts on server and resource pack on clients |
| Java | The Gradle source project | Reobfuscated addon jar from `build/libs/`, installed on both sides with the full Divinity bundle |

Do not redistribute other mods without their licenses permitting it. Tell users
which dependencies to obtain and which exact builds you tested. Keep personal
worlds out of development and distribution folders.

## Maintainer Packaging

From the Divinity source repository, `python3 tools/package_addon_templates.py`
rebuilds the two deterministic ZIPs and inventory. Use `--check` to verify that
committed downloads match the source. The packaging script explicitly excludes
development outputs and imported dependency jars. It does not compile or test
Minecraft; see each tutorial's verification checklist.
