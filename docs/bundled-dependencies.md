# Bundled Dependencies

The main `divinity_engine-1.9-1.20.1.jar` is the distributable JarJar artifact.
It embeds the Forge builds of Nightshade Core 1.6.0, Vanguard 2.2.1-1.20.1,
and Player Animator 1.0.2-rc1+1.20. Players do not need separate jars for any
of these three dependencies.

Player Animator is by KosmX. Its [upstream MIT notice](https://github.com/KosmX/minecraftPlayerAnimator/blob/1.20/LICENSE)
is included at `META-INF/licenses/player-animator-MIT.txt` in the release jar.

The `-slim.jar` does not bundle dependencies and is not the all-in-one release.
Do not install both Divinity jars together. An existing separately installed
Core, Vanguard or Player Animator must be compatible with the embedded version requirements.

## Development Build

Use Java 17. `gradle/gradle-daemon-jvm.properties` pins Gradle's build runtime
to an installed Java 17 JDK, independently of the shell's Java version. The
compilation toolchain also targets Java 17. In IntelliJ, use a Java 17 project
SDK and Gradle JVM (locally, `corretto-17`), then reload the Gradle project.
The MixinGradle message about the optional Eclipse APT plugin is not an error.

Publish the dependency projects first:

1. In `NightshadeCore-Forge-1.20.1`, run `./gradlew build publishToMavenLocal`.
2. In `Vanguard/Vanguard-Forge-1.20.1`, run `./gradlew build publishToMavenLocal`.
3. In Divinity Engine, run `./gradlew build --console=plain`.

The versions are set in `gradle.properties`. Dependencies are mapped only on
the development classpath; the separate JarJar configuration embeds unchanged,
reobfuscated Maven artifacts. Forge's JarJar metadata negotiates Core 1.6.x
and Vanguard 2.2.x versions, plus the compatible Player Animator 1.0.x range.
All three remain real mods with their own IDs and APIs;
their classes are not shaded into Divinity's namespace.

`build` runs `verifyBundledMods`, checking all three nested mods, exact included
versions, generated refmaps, Java 17 bytecode compatibility and SHA-256 equality
with the production Maven artifacts. It also checks the 19 Battle Aspect item
models and the complete shared parent/texture chain. Runtime client/server checks are separate.

Build outputs are under `~/.gradle/workspace-builds/divinity-engine/forge-1.20.1/libs`.

See [integration validation](integration-validation.md) for the successful
bundle-only client/server runs, GameTest results and remaining playtest limits.

## Battle Aspect Assets

Vanguard supplies the seven base aspect models in its nested jar. Divinity
supplies twelve addon aspect models in the outer jar. All use Vanguard's
existing Battle Aspect texture. The item IDs remain in `vanguards_kit` for save
compatibility; datagen uses the full registered item ID rather than assuming
the generating mod owns the namespace.

Both projects regenerate these models with `./gradlew runData`. For an isolated
comparison, use `-PdataOutputDir=/absolute/path/to/temporary-output`; this leaves
the checked-in generated resources untouched. Divinity's data run includes
Vanguard as an existing resource pack so its shared parent can be validated.
