# Content Authoring Validation

Verified on October 16, 2026 with Java 17, Minecraft 1.20.1 and Forge 47.4.2.
All game checks used isolated development saves, not personal worlds.

## Results

- `compileJava`: passed after the toggle-key eligibility correction.
- `divinity_content`: all 16 required GameTests passed with KubeJS absent.
  Coverage includes schema bounds, all 21 mode/hook combinations, invalid
  references, fingerprint ordering, automatic statue registration and NBT,
  behavior success/cooldowns, canceled events and trial phase bookkeeping.
- `divinity_content_kubejs`: all four required GameTests passed with KubeJS
  2001.6.5-build.26, Rhino 2001.2.2-build.17 and Architectury 9.1.12.
  Startup and server fixture scripts loaded with zero errors/warnings. Tests
  cover script-only god/statue/blessing/trial declarations, JSON-before-script
  ordering, exactly-once callbacks, ID isolation, cooldown gating and pure-script
  success without a JSON action.
- `jarJar`, `reobfJarJar`, `verifyBundledMods`: passed. Verified embedded Core
  1.6.0, Vanguard 2.2.1-1.20.1 and Player Animator 1.0.2-rc1+1.20; KubeJS stays
  optional and external.
- The particle pass separately passed 23 GameTests and a 54-scene packaged
  gallery covering all 90 authored recipes. See [effects checks](blessing-effects-tests.md).

## Reproduce

```sh
./gradlew runGameTestServer -PgameTestNamespaces=divinity_content --console=plain
./gradlew jarJar verifyBundledMods --console=plain
```

For the optional integration suite, install only the fixture scripts described
in [the KubeJS test example](../examples/kubejs-tests/README.md), then run its
documented command. The four tests deliberately have no Java declaration fallback.

## Remaining Manual Coverage

Automated callbacks use isolated living-entity fixtures rather than a full
two-player UI playthrough. Matching/mismatched client-server handshakes, actual
key input on JSON toggle blessings, server-script reload, resource-pack display,
and reconnect/save migration with third-party content still need multiplayer
manual coverage. The fingerprint test verifies canonical definitions, not a
complete network login. Documentation examples were source-checked; not every
Core/Vanguard documentation snippet was compiled as a standalone addon.

The build emits existing deprecation/unchecked warnings. These checks do not
claim a warning-free build or retest unrelated gameplay systems.
