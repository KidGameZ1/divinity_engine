# Addon Starter Projects

Download complete project ZIPs from the
[wiki's Addon Templates page](https://github.com/KidGameZ1/divinity_engine/wiki/Addon-Templates).

- `json-kubejs-addon/`: editable instance overlay with startup JSON definitions,
  KubeJS server logic, client resources and focused tests. No Java build required.
- `java-addon/`: standalone Forge 1.20.1 / JDK 17 Gradle addon. Imports local
  development dependencies from the full Divinity bundle, then builds its own jar.

Each has a complete README. Neither includes Minecraft/library jars or worlds.
These templates target the current source API, which can be newer than a hosted
release carrying the same Divinity version number.

Maintainers regenerate downloads with `python3 tools/package_addon_templates.py`
and verify them with the same command plus `--check`. Generated development
folders and imported dependencies are excluded. Do not manually ZIP a working
development folder that contains saves or logs.
