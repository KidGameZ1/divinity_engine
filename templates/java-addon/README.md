# Haven: Java Addon Starter

A standalone **Forge 1.20.1 / Forge 47.4.2 / JDK 17** project. You do not need
to clone Divinity Engine or obtain access to its libraries' private repositories.
You do need the **current full Divinity Engine 1.12-1.20.1 dependency bundle** that
contains `GodsDeferredRegister`. A version number alone does not guarantee that
an older download has the current API.

This starter adds Haven, an automatically bound statue, Guiding Breath (heal two
health points while injured in water; 50 favor; 12-second cooldown), a five-second
vigil trial, translations, and a survival statue recipe. It does not include a
story or a permanent blessing upgrade. Add those deliberately using the guides.

## 1. Prepare Java And Dependencies

Extract this project into its own directory, not into `mods/` or an existing
Divinity checkout. Install a JDK 17 and select it for both `JAVA_HOME` and your
IDE's Gradle JVM. Check `java -version` before continuing. No global Gradle install
is needed; the wrapper is included.

macOS/Linux, from this directory:

```sh
chmod +x gradlew
./gradlew -b setup.gradle importDivinity "-PdivinityJar=/absolute/path/to/divinity_engine-1.12-1.20.1.jar" --console=plain
./gradlew build --console=plain
```

Windows PowerShell:

```powershell
.\gradlew.bat -b setup.gradle importDivinity "-PdivinityJar=C:\Downloads\divinity_engine-1.12-1.20.1.jar" --console=plain
.\gradlew.bat build --console=plain
```

Use the full jar, **not `-slim.jar`**. `setup.gradle` reads its embedded dependency
metadata, deduplicates dependencies, and creates local development-only jars in
`libs/managed/`. It removes nested JarJar entries from these copies so ForgeGradle
can remap each library once. It does not modify the supplied jar or a Minecraft
instance. Its manifest records source hashes and exact imported versions.
Development dependency coordinates include the bundle hash, avoiding stale remaps
when two source builds share a version number.

These generated copies are ignored by Git and must not be shipped with your
addon. No game or library jars are included in this starter archive. Obtain
Divinity through its authorized distribution or build the current source; do
not use jars from an unrelated Minecraft/NeoForge version.

## 2. Open And Run

After dependency import, open this directory in IntelliJ as a Gradle project.
Set Gradle JVM to JDK 17, reload Gradle, and run:

```sh
./gradlew genIntellijRuns --console=plain
./gradlew runClient --console=plain
```

The client uses this project's new `run/` directory. Create a disposable test
world. Do not point the run directory at personal saves. Do not put an additional
Divinity/Core/Vanguard/Animator jar in `run/mods`: Gradle already supplies them.
KubeJS is **not required** for this Java starter.

For a dedicated-server check use `./gradlew runServer`. It uses the separate
`run-server/` folder. Read Minecraft's EULA and accept it yourself only if you
agree; this project does not accept it automatically. Connect using a client
with the same addon and Divinity build.

## 3. Check The Sample In Game

1. Find Haven's statue in the Divinity statue creative tab, or craft it with one
   amethyst shard above two rows of three stone blocks.
2. Place it with clear space above/front and make contact through its menu.
3. Offer bread or amethyst and pray by sneaking near the statue. Shared
   worship budgets still apply; do not expect one large stack to bypass them.
4. At 50 favor, equip Guiding Breath in a blessing slot. Use that slot's configured
   key while injured in water. It heals up to two HP and starts a 12-second cooldown.
5. Repeat on dry land, at full health, and during cooldown. No extra heal should occur.
6. Reopen Haven's physical statue menu and start Shelter Vigil. Keep watch for
   five seconds. The session engine owns return/cleanup; completion grants five favor.
7. Try leaving another vigil early. It should fail, clean up, and allow a later retry.

The sample god uses the addon favor-based compatibility rules, not the built-in
gods' story proof progression. Contact and nonnegative favor are needed for its
trial. This sample intentionally has no enemies; expand it only after this
lifecycle test works.

## 4. Understand And Edit The Project

```text
build.gradle                   Forge runs, dependency remapping, reobfuscated jar
setup.gradle                   One-time import from the Divinity bundle
gradle.properties              Addon ID, version and Maven group
gradlew / gradlew.bat           Gradle 8.8 wrapper entry points
src/main/java/com/example/haven/
  HavenAddon.java               Registers the blessing, god/statue and trial
  HavenGod.java                 Prayer and accepted offering behavior
  GuidingBreath.java            Server-side ability and per-instance counter
  ShelterVigil.java             Session-owned trial encounter
src/main/resources/
  META-INF/mods.toml            Forge identity and required versions
  pack.mcmeta                  Resource-pack metadata
  assets/haven_addon/lang/en_us.json
  data/haven_addon/recipes/haven_statue.json
```

Registry suppliers are lazy. Do not resolve `HAVEN.get()` during static
initialization, and do not register a second statue yourself. Definitions are
shared; put player state in `BlessingsInstance.getOrCreateTag()` and mark it dirty.
`onPressed` returns true only after an actual heal; the engine starts cooldown.
The cooldown constructor argument is seconds, while stored cooldown values and
trial durations are ticks. Do not implement the same action again on key release.

To make this your addon **before releasing saves**, replace `haven_addon` in
`gradle.properties`, `HavenAddon.ID`, `mods.toml` (including dependency table
names), resource directory names, recipe references, and translation keys. Rename
the Java package and `mod_group` to your own package. Then change god/blessing/trial
paths consistently. `mod_id` alone does not rewrite source/resources. Once IDs
are released, keep them stable and change display names through translations.

## 5. Build And Distribute

```sh
./gradlew build --console=plain
```

Ship **`build/libs/haven_addon-1.0.0.jar`**, not this source ZIP. Install it in
`mods/` on both client and server alongside the unmodified full Divinity bundle.
The build reobfuscates your addon but does not bundle Divinity or its libraries.
Test that packaged pair in a separate instance, not only an IDE run. Never ship
`libs/managed`, `run`, worlds, logs, Gradle caches, or somebody else's credentials.

When updating the Divinity dependency, stop all dev runs, repeat the import with
the matching full bundle, refresh Gradle dependencies, then rebuild/retest. This
starter intentionally targets the current 1.9 API; inspect API changes before
editing the version checks for a newer version.

## Troubleshooting

| Symptom | Check |
| --- | --- |
| First-run missing dependency manifest | Run the `-b setup.gradle importDivinity` command before IDE import/build |
| Java version error | Both terminal and IDE Gradle JVM must be JDK 17 |
| Missing `GodsDeferredRegister` | The bundle predates this API; use the current source update |
| Wrong/missing library or duplicate mod | Reimport the full bundle; remove manually added duplicates from this project's `run/mods` |
| Statue exists but names are raw keys | Check the `assets/haven_addon/lang/en_us.json` namespace and spelling |
| Blessing is visible but cannot fire | Contact, 50 favor, equipped slot, cooldown, water and missing health all matter |
| Trial button unavailable | Open through Haven's physical statue, not a remote menu; confirm contact and nonnegative favor |
| Works in IDE but not normal Minecraft | Distribute the jar after `reobfJar`, install it on both sides, and compare Divinity versions |
| No quest for Haven | Expected: this starter has no story; follow the story guide rather than assuming a god generates one |

## Continue Learning

- [Full Java tutorial](https://github.com/KidGameZ1/divinity_engine/wiki/Java-Addon-Tutorial)
- [Stories and functional upgrades](https://github.com/KidGameZ1/divinity_engine/wiki/Stories-and-Upgrades)
- [Assets and localization](https://github.com/KidGameZ1/divinity_engine/wiki/Assets-and-Localization)
- [Effects and UI](https://github.com/KidGameZ1/divinity_engine/wiki/Effects-and-UI)

The sample source is MIT-licensed so you can adapt it into your addon; retain
the included notice when reusing it. The Gradle wrapper is third-party software
with notices in `licenses/`. This does not relicense Divinity, Minecraft or their
dependencies. Review your own licensing before publishing.
