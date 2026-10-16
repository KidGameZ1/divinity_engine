# Compatibility And Testing

## Versions And Installation

- Minecraft 1.20.1, Forge 47.4.2 and Java 17 are the development target.
- The full Divinity jar embeds Forge Core 1.6.0, Vanguard 2.2.1-1.20.1 and Player Animator 1.0.2-rc1+1.20.
- KubeJS, Rhino and Architectury are optional external dependencies, not embedded.
- Do not mix the newer NeoForge Core/Vanguard artifacts into this Forge instance.

New JSON/KubeJS definitions must exist on both client and server before startup.
The fingerprint covers sorted definition content, including behavior fields.
It is not a content download service or a hash of server-script source. A server
can retain server-only callback logic; startup registry definitions must agree.

## Save Safety

Use stable IDs. Removing a god or its statue from a content pack can leave missing
registry entries in existing saves. Back up worlds before changing installed content.
Testing must use disposable saves, not a player's survival world.

Trials delete only their owned arena bounds through their session engine. Pocket
rooms are saved/unloaded normally; they are not trial cleanup targets. Cosmetic
particles do not mutate terrain. Keep runtime logs, worlds, caches and local IDE
files out of commits.

## Focused Checks

```sh
./gradlew compileJava --console=plain
./gradlew runGameTestServer -PgameTestNamespaces=divinity_effects --console=plain
./gradlew runGameTestServer -PgameTestNamespaces=divinity_content --console=plain
./gradlew jarJar verifyBundledMods --console=plain
```

Run one Minecraft/Gradle process at a time on memory-constrained machines.
Optional KubeJS development runs add `-PwithKubeJS=true`. A successful build with
zero discovered tests is not a passing suite. Inspect the test count and failures.

Effects have 23 focused GameTests plus a 54-scene packaged gallery. The gallery
is synthetic: it does not prove every combat proc, network recipient or third-party
shader works. See [effects verification](https://github.com/KidGameZ1/divinity_engine/blob/main/docs/blessing-effects-tests.md)
and the [content validation record](../content-authoring-validation.md) for results and remaining coverage.

For an addon, check first contact, offerings, eligibility, cooldown, save/reload,
death, two independent players, trial exit/cleanup, resource-pack overrides,
malformed definitions, and matching versus mismatched client/server content.
Test KubeJS absent, installed, and after server-script reload.
