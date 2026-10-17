# Divinity Engine

A Forge 1.20.1 mod with 18 gods, blessings, curses, protected trials, connected
stories, permanent specializations, a Divine Codex and personal pocket domains.

- [Player and developer wiki](https://github.com/KidGameZ1/divinity_engine/wiki)
- [Versioned wiki source](docs/wiki/Home.md)
- [JSON definitions + KubeJS programming example](examples/json-kubejs-content)
- [JSON-only built-in actions example](examples/json-content)
- [Alternative KubeJS startup declarations](examples/kubejs-content)
- [Java/story addon examples](examples/connected-pantheon-addon)

## Development

Use Java 17 and the matching Forge 1.20.1 Core/Vanguard projects. Development
resolves `net.nightshade:nightshade_core-forge-1.20.1:1.6.0` and
`net.nightshade:vanguard-forge-1.20.1:2.2.1-1.20.1` from local Maven.
Build and publish those sibling projects locally before compiling Divinity.

```sh
./gradlew compileJava --console=plain
./gradlew jarJar --console=plain
```

The full jar bundles Core, Vanguard and Player Animator. KubeJS is optional and
not bundled. Build output defaults to
`~/.gradle/workspace-builds/divinity-engine/forge-1.20.1`.

Use isolated saves for development. Keep published registry IDs stable, and do
not commit worlds, logs, local IDE state or credentials. See [compatibility and
testing](docs/wiki/Compatibility-and-Testing.md) for focused checks and limitations.
