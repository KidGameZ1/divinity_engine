# Build A Java Addon From Start To Finish

This tutorial builds **Haven**, a god with a craftable automatic statue, a
water-healing blessing and a short trial. You finish with an independent Forge
addon jar. It uses the same public APIs as other addons, not modifications to
Divinity's source. KubeJS is not required.

## 1. Download And Prepare

Download the [Java starter](https://github.com/KidGameZ1/divinity_engine/raw/refs/heads/main/downloads/divinity-java-starter.zip)
and extract it into its own directory. Its full source is also in
[`templates/java-addon`](https://github.com/KidGameZ1/divinity_engine/tree/main/templates/java-addon).
Do not put the source ZIP in Minecraft's `mods` folder.

Install **JDK 17**, then set your terminal's `JAVA_HOME` and your IDE's **Gradle JVM**
to that JDK. `java -version` should report 17. The project includes Gradle 8.8's
wrapper; do not install an unrelated system Gradle. Forge's official
[development setup guide](https://docs.minecraftforge.net/en/1.20.1/gettingstarted/)
explains the underlying IDE and JDK setup.

Obtain the current full **Divinity Engine 1.12-1.20.1 bundle**, not the slim jar.
This guide documents the current source update, which can be newer than a public
mod-hosting release bearing the same version number. The importer checks for the
automatic god/statue API. It cannot prove every behavior in an older build is
identical; record the exact bundle/hash you test.

## 2. Import Dependencies Once

Run these terminal commands from the extracted project root:

```sh
chmod +x gradlew
./gradlew -b setup.gradle importDivinity "-PdivinityJar=/absolute/path/to/divinity_engine-1.12-1.20.1.jar" --console=plain
./gradlew build --console=plain
```

On Windows PowerShell, use the included batch wrapper:

```powershell
.\gradlew.bat -b setup.gradle importDivinity "-PdivinityJar=C:\Downloads\divinity_engine-1.12-1.20.1.jar" --console=plain
.\gradlew.bat build --console=plain
```

`setup.gradle` is a separate setup build so missing mod dependencies do not prevent
the importer from running. It reads the bundle's JarJar metadata and writes
development-only copies into `libs/managed/`, including required Core, Vanguard,
Player Animator and MixinExtras libraries. Nested copies are removed from those
local jars, then ForgeGradle remaps each dependency independently. The original
bundle is untouched; the importer does not alter your Minecraft installation.

`libs/managed/dependencies.json` records versions and hashes. You do not need a
Maven Central release or private Core/Vanguard repository access for this route.
Do not copy these derived jars into a normal game or distribute them: players
install the original Divinity bundle and your finished addon.

Expected checkpoint: `build` succeeds and `build/libs/haven_addon-1.0.0.jar`
exists. A successful compile is not a substitute for the gameplay checks below.

## 3. Read The Project In This Order

| File | Responsibility |
| --- | --- |
| `gradle.properties` | Output name/version, namespace setting and Java package group |
| `src/main/resources/META-INF/mods.toml` | Forge mod identity and required Divinity/Minecraft/Forge versions |
| `HavenAddon.java` | Connect the registers to the mod event bus and submit the trial definition |
| `HavenGod.java` | Define domains, blessings, accepted offerings and prayer response |
| `GuidingBreath.java` | Implement server-side blessing behavior |
| `ShelterVigil.java` | Implement the encounter without duplicating session ownership |
| `assets/haven_addon/lang/en_us.json` | Names, blessing description and messages |
| `data/haven_addon/recipes/haven_statue.json` | A survival crafting recipe for the automatically registered statue |

The four Java classes are under `src/main/java/com/example/haven/`. Client assets
and server data are ordinary resources included in the addon jar. The generated
statue fallback means you can test gameplay before making a custom model.

## 4. Understand Registration

`@Mod(HavenAddon.ID)` makes Forge construct the addon once. The constructor
attaches the blessing register and `GodsDeferredRegister` to the **mod event bus**.
It does not attach them to the general Forge gameplay event bus.

```java
private static final DeferredRegister<Blessings> BLESSINGS = DeferredRegister.create(
        ResourceLocation.fromNamespaceAndPath("divinity_engine", "blessings"), ID);
public static final RegistryObject<Blessings> GUIDING_BREATH = BLESSINGS.register(
        "guiding_breath", GuidingBreath::new);
private static final GodsDeferredRegister GODS = GodsDeferredRegister.create(ID);
public static final RegistryObject<BaseGod> HAVEN = GODS.register("haven",
        () -> new HavenGod(Set.of(GUIDING_BREATH)));
```

These are **suppliers**, not immediate instances. Pass registry handles into the
god; do not call `GUIDING_BREATH.get()` while static fields are initializing.
Do not create a new shared blessing registry with `makeRegistry`: Divinity already
owns it. Your entries belong to `haven_addon`, not `divinity_engine`.

`GODS.register(bus)` owns the god's statue block, block item and block-entity type
as well. Haven's statue ID is `haven_addon:haven_statue`. Creating another block
with that ID is a duplicate-registration error, not a way to customize its model.
Use [resource overrides](Assets-and-Localization.md) instead.

Trials are custom deferred definitions, not Forge registry objects. The starter
declares a lazy `GodTrial(HAVEN.get(), ShelterVigil::new)` factory, then submits the
trial register in enqueued common setup. At that point the god is available;
the registry accepts submissions whether its built-ins initialized just before
or after the addon. The factory creates a new encounter per player session.

## 5. Implement A God That Can Actually Earn Favor

Extending `BaseGod` with a name is not enough to define custom worship rewards.
`HavenGod` supplies the blessing handles, accepts amethyst shards and bread,
and calls `GodHelper.increaseFavor` from valid server-side prayer/offering events.
It rejects canceled events, empty stacks and client-side calls.

The offering handler returns `true` only for accepted offerings. The shared
shrine processor consumes them and handles worship budgets/story attribution.
Do not shrink the stack yourself or run the same favor mutation on the client.
The prayer callback is invoked by the shared prayer system; do not add a second
player tick loop just to award prayer favor.

This addon uses favor-based compatibility rules. Its blessing requires 50 favor.
Do not assume the built-in pantheon's proof-based progression automatically
becomes the progression rule for a newly registered god.

## 6. Program A Blessing Safely

`GuidingBreath` calls:

```java
super(50, 12, false, true, false, HavenAddon.COLOR);
```

The arguments mean required favor, cooldown **seconds**, passive, active, toggle,
and display color. Its implementation:

```java
@Override
public boolean onPressed(BlessingsInstance instance, LivingEntity owner) {
    if (owner.level().isClientSide || !owner.isInWater()) return false;
    float before = owner.getHealth();
    if (before >= owner.getMaxHealth()) return false;
    owner.heal(2.0F);
    if (owner.getHealth() <= before) return false;
    var data = instance.getOrCreateTag();
    data.putInt("haven_successful_heals", Math.min(1_000_000, data.getInt("haven_successful_heals") + 1));
    instance.markDirty();
    return true;
}
```

The full starter also plays a quiet confirmation sound. Two HP is one heart.
Healing can be capped or canceled by another system, so test the actual health
change before returning true. The shared dispatcher validates the equipped slot
and starts the cooldown after success. Do not start another cooldown or replay
the same action in `onRelease`.

The registry definition is shared by every player. An ordinary `int` field on
`GuidingBreath` would be shared too. The example's counter lives in the individual
`BlessingsInstance` tag, and `markDirty()` lets the owning data synchronize.
Do not put entities, worlds or other runtime references in saved data. Do not
assume every `LivingEntity` is a player; check its type before player-only APIs.

For passive/toggle handlers, implement only the callbacks your mode supports,
bound any area search, and clean up your own temporary effects. Return success
only when an effect happened. See [Java API](Java-API.md) and
[Effects and UI](Effects-and-UI.md) before adding custom effects.

## 7. Add An Encounter, Not A Second Trial Engine

`ShelterVigil` is intentionally a five-second noncombat encounter. `runPhase`
sets a 200-tick safety deadline and a 100-tick watch objective. `tick` reports
progress once per second and calls `completeTrial()` when the watch finishes.
`onComplete` awards five favor and sends its message.

The shared session manager handles preparation, transport, room protection,
terminal outcome, return and batched cleanup. Do not teleport or remove rooms
inside `onComplete`, and do not maintain a global list of active players. For
combat encounters, track each spawned entity with `trackEntity`, update targets
at spawn rather than every tick, and use the session's routed death callbacks.
Do not count an unrelated entity's death just because it happened nearby.

Trial registration does not automatically create a quest. To add one, use
[Stories and Upgrades](Stories-and-Upgrades.md); a story can require this trial's
fresh victory and award a separate once-only specialization.

## 8. Launch And Verify

Open the extracted folder as an IntelliJ Gradle project after importing dependencies.
With Gradle JVM set to JDK 17, run:

```sh
./gradlew genIntellijRuns --console=plain
./gradlew runClient --console=plain
```

The test game uses the starter's `run/`, not your normal instance. Create a new
disposable world. No admin commands are needed for these checks:

1. Find Haven's statue in the statue creative tab or craft it with the supplied recipe.
2. Make contact, pray and offer bread/amethyst. Reach 50 favor through normal worship.
3. Equip Guiding Breath. Check injured/in-water success, dry-land refusal, full-health refusal and cooldown.
4. Open the physical statue's blessings menu and start Shelter Vigil. Check success, return and room cleanup.
5. Leave another trial early, then retry. Check failure does not leave a running encounter.
6. Save/rejoin and confirm the statue and blessing remain available.

Also test a dedicated server with `./gradlew runServer`; its separate directory
is `run-server/`. Read and accept Minecraft's EULA yourself if you agree. Test
with a second player: one player's ability counter/cooldown must not affect
the other. Never import personal worlds for development checks.

## 9. Rename Before Releasing

Choose a lowercase namespace and replace `haven_addon` in **all** of these places:
`gradle.properties`, `HavenAddon.ID`, `mods.toml` mod/dependency tables,
resource directory names, recipe IDs and translation keys. Refactor the Java
package and `mod_group` separately. Changing `mod_id` alone does not rewrite Java.

God, blessing, trial, story and specialization IDs are save identities. Once a
release has reached players, keep those IDs stable. Change user-facing names via
translations instead. Removing or renaming released registry IDs requires an
explicit migration and backups.

## 10. Package Your Addon

Run `./gradlew build --console=plain`. Distribute
`build/libs/haven_addon-1.0.0.jar`, after the build's `reobfJar` step, on both client
and server with the original Divinity bundle. The addon jar includes only your
classes/resources; it does not repackage Divinity or its dependencies.

Test the finished jar in a separate normal Forge instance. Publish installation
steps, exact tested versions, a license for your own work and an upgrade policy.
Exclude development dependency copies, logs, caches, worlds and credentials.
Use [Addon Templates](Addon-Templates.md) for download inventories and the starter
README for common setup failures.
