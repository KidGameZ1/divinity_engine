# Learning Divinity Engine and Nightshade RenderLib

> **Superseded guide:** This September 2026 walkthrough is retained for historical context. For current APIs and examples, use the [Developer Guide](wiki/Developer-Guide.md).

This is a walkthrough of the current Forge 1.20.1 project, checked against its source and the local Nightshade Core Forge source on September 24, 2026. It explains the systems we added or reworked, why they exist, and how to extend them yourself.

Your original gods, blessings, curses, worship rules, and many encounter mechanics remain the foundation. The recent work mainly adds safer lifecycle management, narrative progression, specialization rewards, presentation, and addon APIs. This is not a claim that every class in the repository was newly written or that every earlier proposal was implemented exactly as originally planned. The ascension prototype was rolled back; it is not part of this guide's current feature set.

The examples below are teaching examples, not newly installed gameplay. Small fragments are labeled with where they belong. They use the local Forge port, not a newer NeoForge API. No builds or gameplay tests were rerun for this documentation update.

## Contents

1. [The mental model](#1-the-mental-model)
2. [What changed](#2-what-changed)
3. [Startup, registration, and saved data](#3-startup-registration-and-saved-data)
4. [Create a god and its automatic statue](#4-create-a-god-and-its-automatic-statue)
5. [Understand and extend blessings](#5-understand-and-extend-blessings)
6. [Trials](#6-trials)
7. [Stories, memories, and relationships](#7-stories-memories-and-relationships)
8. [Permanent blessing upgrades](#8-permanent-blessing-upgrades)
9. [Favor and shrine presence](#9-favor-and-shrine-presence)
10. [Codex pages and personal journals](#10-codex-pages-and-personal-journals)
11. [RenderLib from the beginning](#11-renderlib-from-the-beginning)
12. [Give a blessing its own effect](#12-give-a-blessing-its-own-effect)
13. [Build a Nightshade UI](#13-build-a-nightshade-ui)
14. [Rendering existing models and using shaders](#14-rendering-existing-models-and-using-shaders)
15. [Dependencies, builds, and testing](#15-dependencies-builds-and-testing)
16. [A practical learning order](#16-a-practical-learning-order)

## 1. The Mental Model

Think of the mod as five connected layers:

```text
DEFINITION          PLAYER DATA          SERVER RULES          CLIENT DISPLAY
What exists?        What happened?       Is it allowed?        What can I show?

BaseGod          -> BaseGodInstance   -> GodHelper          -> blessings menu
Blessings        -> BlessingsInstance-> blessing callbacks -> particles/meshes
GodTrial         -> TrialSession     -> session manager    -> trial HUD
Story definition -> narrative data   -> narrative manager  -> Codex/quest tab
                                              |
                                              +-- sync packets --> client cache
```

A **definition** is a shared recipe. A **registry** is the catalog that lets the game find that recipe by an ID such as `divinity_engine:solar_ward`. An **instance** is one player's use of that recipe. A **capability** attaches additional saved data to a player. A **manager** coordinates rules and transitions. A **packet** carries a request or update across the network. A **renderer** draws the result.

The most important rule is: **the server decides; the client displays**. Single-player still has a logical server. A menu button should request an action, not directly grant favor. A particle hitting an enemy is not proof that the enemy should take damage.

Another important rule: **registered objects are shared**. Do not put a player's current cooldown, kill count, or quest choice in a mutable field on a registered `Blessings` or `BaseGod` object. Two players would then share that state. Put it in their instance, capability, or session.

## 2. What Changed

| Area | Current addition or rework | Main place to learn it |
| --- | --- | --- |
| God statues | One declaration creates a god's statue block, item, and block entity type, with fallback assets. Existing built-in save IDs are preserved. | `api/gods` |
| Trials | Definition registry separated from active sessions; shared arena allocation, return handling, entity ownership, recovery data, and HUD synchronization. | `util/divinity/trials` |
| Narrative | Persistent memories, seven-chapter stories, permanent choices, exact donations, retryable trials, and once-only endings. | `api/narrative`, `util/divinity/narrative` |
| Pantheon lore | Connected histories for all 18 gods and nine optional paired epilogues. | `data/divinity_engine/divine_narratives` |
| Blessing upgrades | Two earned specializations per god, checked at use time without rewriting base blessing data. | `BlessingSpecializations` and individual blessings |
| Favor | Saved worship budgets, diminishing offering rewards, objective rewards, and remembered first-shrine placement. | `FavorRewards` |
| Codex | Original registered pages plus a searchable, player-specific story journal and discovery-gated lore. | `client/codex`, `DivineCodexScreen` |
| Blessings menu | Stone/sigil presentation, selected-god quests and favor information; statue-opened menus stay bound to that statue's god. | `BlessingsScreen`, `BlessingsMenu`, `ShrineUi` |
| Pocket inventory | Nightshade galaxy-themed presentation around the existing inventory slots. | `PocketDimensionInvScreen` |
| Blessing visuals | Individual designs for 90 blessings, using particles, custom geometry, or both; god-specific palettes remain consistent. | `util/divinity/effects`, `client/effects` |
| Core integration | Forge-compatible Core/RenderLib, compatibility helpers, render hooks, particle synchronization, UI hosting, and shader fallbacks. | Sibling `NightshadeCore-Forge-1.20.1` project |
| Packaging | Nightshade Core, Vanguard, and Player Animator bundled through Forge JarJar, with Java 17 build configuration and bundle verification. | `build.gradle`, `gradle.properties`, `gradle` |
| Safety checks | Focused tests for stories, specializations, trials, UI, effects, registry/save compatibility, and packaged dependencies. | `gametest`, `src/test`, `tools`, `docs` |

The presence of a feature or test does not mean every multiplayer, shader-mod, and old-world combination has been manually played. The validation documents record that distinction.

## 3. Startup, Registration, and Saved Data

Start with [DivinityEngineMod.java](/Users/kaydenalexis/Documents/DivinityEngine/src/main/java/net/nightshade/divinity_engine/DivinityEngineMod.java). Its constructor attaches registries, capability registration, and narrative bootstrap. Common setup registers shared visual/UI types, trials, and network messages. Client-only setup supplies actual renderers and screens.

The **mod event bus** handles startup work such as registrations and client setup. The **Forge event bus** handles play events such as damage, death, crafting, and ticks. Register a listener once. Do not combine an automatic subscriber with a second manual registration of the same handler.

Deferred registration means declaring a supplier now and allowing Forge to construct the object at the correct registration stage. `RegistryObject<T>` is the handle to that future object. Calling `.get()` before its registry has populated can fail. A `() -> VALUE.get()` supplier postpones resolution; it does not itself create another object.

The existing god/main capabilities hold contact, favor, blessing instances, and equipped slots. The new narrative capability is separate so stories can remember choices after abandonment without resetting ordinary favor or loadouts. The trial capability records recovery information; it is not a saved, resumable combat simulation.

Read [DivineNarrativeCapability.java](/Users/kaydenalexis/Documents/DivinityEngine/src/main/java/net/nightshade/divinity_engine/network/cap/player/narrative/DivineNarrativeCapability.java) for a small provider/serialization example. Read [BlessingsInstance.java](/Users/kaydenalexis/Documents/DivinityEngine/src/main/java/net/nightshade/divinity_engine/divinity/blessing/BlessingsInstance.java) for an instance that saves IDs, cooldown, toggle state, and custom NBT.

Use namespaced IDs as identities, not display names. Changing "Solar Ward" in a language file is presentation. Renaming `divinity_engine:solar_ward` is a save-compatibility change. Likewise, changing a branch key's meaning can corrupt a player's agreement even if its ID still parses.

## 4. Create a God and Its Automatic Statue

The old fragile part was connecting separately created content by guessing names or resolving types at the wrong time. The new helper declares the related objects together and binds them lazily.

This is a complete registry-holder example, not a complete addon mod. Call its `register` method from your addon constructor with the mod event bus:

```java
import java.awt.Color;
import java.util.Set;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegistryObject;
import net.nightshade.divinity_engine.api.gods.GodsDeferredRegister;
import net.nightshade.divinity_engine.divinity.gods.BaseGod;

public final class ExampleGods {
    public static final GodsDeferredRegister GODS =
            GodsDeferredRegister.create("example_pantheon");

    public static final RegistryObject<BaseGod> SELENE = GODS.register(
            "selene", () -> new BaseGod(Set.of("moon"), Set.of(), Color.WHITE));

    public static void register(IEventBus modBus) {
        GODS.register(modBus);
    }
}
```

This declares `example_pantheon:selene` and a block, item, and block entity type all named `example_pantheon:selene_statue` in their respective registries. A hidden low-priority pack supplies missing blockstates, models, and a drop table. You can override them with normal resource/data files.

The empty second set means this example god has no blessings. It does not automatically author offerings, a story, a trial, or custom mechanics. Use a `BaseGod` subclass and attach blessing registry handles for a real god, following a built-in god.

Add `"example_pantheon.gods.selene": "Selene"` to the addon language file. Obtain content handles with `GODS.statue(SELENE).block()`, `.item()`, and `.blockEntity()`.

**Automatic registration requires using this helper.** It does not intercept every raw Forge god registration after registries freeze. An addon using its own manual system can retain it or adopt the helper. Do not declare a second statue under the same ID.

For a previously released statue named differently, use `GODS.register("selene", "ancient_moon_shrine", Selene::new)` only when that correctly preserves its existing block/item/type IDs. The helper does not justify silently renaming saved content.

The block entity must be constructed with the exact registered `BlockEntityType`. That type is how Minecraft writes its save ID; a visually correct block can still fail chunk saving if its type is null or unregistered.

See [Automatic Statues](/Users/kaydenalexis/Documents/DivinityEngine/docs/automatic-statues.md) for resource paths, compatibility rules, and the focused persistence tests. The 18 built-in statue IDs, block states, and container/custom-name data were retained; this work did not rewrite your worlds or structure templates.

## 5. Understand and Extend Blessings

Use [SolarWard.java](/Users/kaydenalexis/Documents/DivinityEngine/src/main/java/net/nightshade/divinity_engine/divinity/blessing/solarius/SolarWard.java) as a small real example. Its gameplay checks daylight and health, applies effects, and emits a visual cue when the ward is applied. Its specialization changes the effective threshold or duration.

The active input path is:

```text
R/G/V input
 -> BlessingSlot1/2/3Message
 -> server checks the player, equipped slot, contact, favor, and cooldown
 -> BlessingsInstance.onPressed / onHeld / onRelease
 -> your Blessings subclass callback
 -> start cooldown when that callback reports success
 -> update player state and emit successful-action visuals
```

[BlessingSlot1Message.java](/Users/kaydenalexis/Documents/DivinityEngine/src/main/java/net/nightshade/divinity_engine/network/messages/key/BlessingSlot1Message.java) contains the shared dispatch path. The client cannot legitimately choose a different unequipped blessing by sending a name. Held timing is calculated on the server. Release clears input state instead of replaying the press.

To create a blessing:

1. Extend `Blessings` and implement the callback appropriate to its behavior.
2. Register it in the blessing registry and include its handle in its god's blessing set.
3. Keep default configuration on the definition and mutable per-player values on `BlessingsInstance`.
4. Check your trigger conditions; return success only when the intended action succeeds.
5. Use the existing instance cooldown path instead of manually decrementing another timer.
6. Emit the visual after success, then provide its separate visual design.
7. Add translations and test two players, unsuccessful activation, cooldown, and unequipping.

For new custom timers, use server ticks rather than `System.currentTimeMillis()`. Normally 20 ticks are one second; under server lag, gameplay ticks and wall time differ. A duration passed to `MobEffectInstance` is in ticks, and amplifier `0` means level I.

Not every legacy blessing has been rewritten into an ideal teaching example. In particular, do not assume an old wall-clock timer or mutable field is a pattern to reproduce merely because it exists elsewhere in the repository.

## 6. Trials

The trial registry answers **what encounter belongs to this god?** The session manager answers **what encounter is this player currently running?** Those are different questions.

`GodTrial` holds a bound god and a factory such as `TrialOfTime::new`. The factory makes a new encounter controller for each player. `GodlyTrial` supplies encounter hooks. `TrialSession` manages one run's phase timing, state, return, and cleanup. `TrialSessionManager` owns active sessions, arena slots, and tracked-entity routing.

Read [GodlyTrial.java](/Users/kaydenalexis/Documents/DivinityEngine/src/main/java/net/nightshade/divinity_engine/divinity/trial/GodlyTrial.java), then [TrialSession.java](/Users/kaydenalexis/Documents/DivinityEngine/src/main/java/net/nightshade/divinity_engine/util/divinity/trials/TrialSession.java). Concrete trials still contain encounter-specific counters and some legacy mob lists; the migration did not turn every fight into a purely data-driven definition.

Registration fragment for an addon that already supplies `VESPER` and a `TrialOfDusk` controller:

```java
GodTrialsDeferredRegister trials = new GodTrialsDeferredRegister("example_pantheon");
trials.register("trial_of_dusk", () -> new GodTrial(VESPER.get(), TrialOfDusk::new));
GodlyTrialsRegistry.addDeferredRegister(trials);
```

Submit during addon construction so definitions are resolved with the normal initialization pass. The god lookup currently selects one trial definition for a god; it is not a multiple-encounter selector.

The controller provides `onStart`, `runPhase`, `onComplete`, and `getMaxPhases`. In a phase, set `phaseDuration` in ticks and call `setPhaseInfo(name, objective, current, target)`. Track every spawned entity through `trackEntity`. Use `setObjectiveProgress`, `nextPhase`, `completeTrial`, and `failTrial` to communicate with the session instead of doing independent teleports or deleting the arena yourself.

Do not copy only a spawn loop and forget ownership. A tracked entity can be associated with its session; an unrelated overworld mob must not affect that trial. The registry should never be searched as though it contains active players.

The shared lifecycle records a return location, claims an arena slot, places the trial structure, runs the encounter, removes tracked mobs and the arena when ending, and returns/releases the player and slot. Recovery data handles an interrupted run rather than resuming it. Exit, logout, and failure handling must be idempotent: two callbacks must not award twice or clean up somebody else's arena.

The client HUD receives a snapshot; it does not run another trial clock that can independently fail the player. Statue-menu validation remains important even though `TrialSessionManager.startTrial` is a public server API: a network request must prove it came from an appropriate current menu.

## 7. Stories, Memories, and Relationships

The narrative system has three distinct types of content:

- `GodPresenceDefinition` supplies personality responses, a shrine color/texture/sound, and dialogue variants.
- `DivineStoryDefinition` supplies one story's identity, objectives, permanent alternatives, and reward metadata.
- `GodRelationshipDefinition` supplies an optional crossover between completed stories.

`DivineNarrativeCapability` stores what happened to this player. `DivineNarrativeManager` is the server's rulekeeper. The client only receives a journal snapshot appropriate to what that player has discovered.

### The Actual Player Journey

Contact a god through its statue, reach at least **50 favor**, reopen the statue, select **Quests**, and choose **Begin quest**. **Speak about quest** opens path previews and permanent confirmation in a shrine conversation. Opening the Codex is not a remote substitute for being at the shrine. Player actions use validated menu packets, not commands or command-backed chat links.

The seven chapters are:

1. **The Call:** accept the request and make an ordinary offering the god actually accepts.
2. **A Witness:** perform the story's first thematic objective.
3. **Divided Testimony:** read both mechanical reward previews and explicitly confirm a permanent branch.
4. **The Chosen Deed:** finish that branch's objective.
5. **The Reckoning:** perform a further thematic objective.
6. **The Trial:** start and win a fresh trial for this god.
7. **The Return:** return to the statue and claim the ending and upgrade.

A failed trial can be retried. An old victory is not banked for a later chapter. Continuous challenges restart when interrupted; completed chapters remain complete. Explicit donations consume the exact requested items and are different from ordinary dropped shrine offerings.

Confirmation is validated on the server against the current stage, contact, definition, expiring preview, and a matching loaded statue within six blocks. A confirmed path remains chosen after death or abandonment. Abandonment pauses the relationship; recontact resumes it.

### The Connected Lore

The shared history centers on the First Concord and its Sundering. Different gods preserve different truths, mistakes, and omissions. The nine paired arcs are Solarius/Nythea, Aethon/Kairoth, Nerai/Cryonis, Voltira/Zephra, Grond/Mechanos, Terra/Vokar, Nashara/Elaris, Varun/Dravak, and Lumen/Ignar.

For example, Solarius and Nythea are estranged siblings: he believed she abandoned defenders, while she was sheltering survivors in dreams. Completing both stories makes an optional conversation available. Its interpretation depends on both branches (`aa`, `ab`, `ba`, or `bb`); it grants lore, not extra combat power or compulsory allegiance.

Read [Pantheon History](/Users/kaydenalexis/Documents/DivinityEngine/docs/pantheon-history.md) before extending the shared history. It distinguishes actual history from the gods' incomplete accounts. A character's accusation is not automatically the world's objective truth.

### Make Your Own Story

Declare `DivineNarrativeDeferredRegister`, add story/presence/relationship suppliers, then call `.submit()` from addon construction before common setup. Do not bootstrap the entire narrative engine again from an addon.

Builder outline, with existing `Branch` and `StoryChronicle` values named `pathA`, `pathB`, and `chronicle`:

```java
DivineStoryDefinition story = DivineStoryDefinition.builder(godId)
        .id(storyId)
        .title("The Unfinished Lantern")
        .request("Bring an accepted offering, then hear what was left unsaid.")
        .branch("a", pathA)
        .branch("b", pathB)
        .chronicle(chronicle)
        .build();
```

Do not mistake this outline for a complete story. A seven-chapter chronicle supplies witness/reckoning objectives and ten passages: `call`, `witness`, `testimony`, `deed_a`, `deed_b`, `reckoning_a`, `reckoning_b`, `trial`, `ending_a`, and `ending_b`. Only the chosen alternatives are shown to the player.

A concrete objective is `StoryObjective.of(StoryObjectiveType.OBSERVE, "night_sky", 200, "Observe the open night sky for ten seconds")`. Here `200` is continuous server ticks, not kills. A donation objective instead needs an item ID and item count. Descriptive text does not implement the predicate.

[ConnectedPantheonAddon.java](/Users/kaydenalexis/Documents/DivinityEngine/examples/connected-pantheon-addon/ConnectedPantheonAddon.java) is the fuller example: two stories for one god, independent branches, a custom objective, and a crossover. It is a narrative module, not a complete runnable god mod. Its host still supplies the god, accepted offerings, statue, and trial.

The equivalent [example datapack](/Users/kaydenalexis/Documents/DivinityEngine/examples/connected-pantheon-pack) uses version-two JSON under `data/<namespace>/divine_narratives/`. Datapacks configure supported objectives; they do not run arbitrary Java or create a god implementation. Version-one definitions remain supported by compatibility handling.

For a genuinely new objective, register a `StoryObjectiveType` in Java, implement its successful server-event handler, and report verified progress through `DivineNarrativeManager.recordObjective`. Cancelable events need final-result verification, not credit before another listener cancels them. The [custom objective example](/Users/kaydenalexis/Documents/DivinityEngine/examples/connected-pantheon-addon/StarChartEvents.java) shows captured chapter context and deferred verification.

Accepted definitions are snapshotted. Editing a datapack does not silently increase an existing player's promised target or change their earned reward. Reload validation publishes a complete valid registry or keeps the previous snapshot. Multiple stories need distinct stable story IDs even when they share a god ID.

For the full schema and event rules, use [Connected Pantheon Authoring](/Users/kaydenalexis/Documents/DivinityEngine/docs/connected-pantheon-authoring.md).

## 8. Permanent Blessing Upgrades

Completing a story awards its selected specialization, lore, shrine recognition, and **25 favor**, once. Choosing a branch is not the same as earning that reward.

A specialization definition is a named reward marker plus metadata. **It does not execute a stat change by itself.** The actual blessing implementation must query that marker and calculate its effective values.

Solar Ward demonstrates this directly:

```java
float threshold = BlessingSpecializations.isActive(living, "solarius", "b")
        ? 0.65f : 0.5f;
int durationTicks = (BlessingSpecializations.isActive(living, "solarius", "a")
        ? 15 : 10) * 20;
```

These are effective values for this use. They do not mutate the shared `SolarWard` object or overwrite everyone's saved base parameters. The helper checks that the reward was earned, the original blessing is equipped, the god is contacted, and the favor requirement is met.

Addon rewards should use their own namespaced specialization ID and `DivineNarrativeManager.hasSpecialization(serverPlayer, rewardId)`, plus the blessing's eligibility checks. The built-in string/branch convenience helper contains a built-in mapping; it is not an automatic interpreter for every addon reward.

Cooldown reductions belong where a cooldown **starts**, not in every tick decrement. Multiplying the timer every tick would produce a completely different and often broken cooldown. Temporary attribute modifiers need stable owned UUIDs, and cleanup must remove only your own modifier, not another mod's speed bonus.

[Permanent Blessing Specializations](/Users/kaydenalexis/Documents/DivinityEngine/docs/blessing-specializations.md) lists all 36 implemented alternatives. That document also explains the related input-release fix, per-player tick timing for Celestial Ward, and recursion prevention for Thornskin retaliation.

## 9. Favor and Shrine Presence

The existing tier boundaries remain: Enemy -250..-101, Displeased -100..-1, Neutral 0..49, Favorable 50..100, Devoted 101..175, Champion 176..250. The rework changes earning and presentation rather than invalidating saved favor or raising blessing thresholds.

[FavorRewards.java](/Users/kaydenalexis/Documents/DivinityEngine/src/main/java/net/nightshade/divinity_engine/util/divinity/gods/FavorRewards.java) is a useful small example of server-owned accounting. Each player/god has a saved 24,000-server-tick worship window:

| Source | Current budget/rule |
| --- | --- |
| Prayer | At most 2 per ten seconds and 12 per window, subject to the god's accepted conditions. |
| Ordinary accepted offerings | At most 12 per interaction and 30 per window; rewards after 15 already earned are halved. |
| Preferred shrine presence | At most 1 per minute and 3 per window. |
| Successful objective chapter | +5 once for that completed objective. |
| Trial victory | +50 to that god. |
| Story finale | +25 and the chosen specialization, once. |
| First placed shrine after contact | +5 once per god; repeated placement is not a farm. |

The window uses server game time, not the sun's time or the system clock. Relogging, changing dimensions, or abandoning/recontacting a god does not reset its saved counters. Ordinary offerings can still be consumed and advance a story after their favor budget is exhausted; don't tell players that capped favor means their items cannot be consumed.

`GodPresenceDefinition` contains three or more routine greeting/prayer/offering variants plus other responses. The server remembers context and limits unsolicited chatter. Explicit quest/status requests have their own response path. Shrine particles go through [CorePresenceEffects.java](/Users/kaydenalexis/Documents/DivinityEngine/src/main/java/net/nightshade/divinity_engine/util/divinity/narrative/CorePresenceEffects.java): short, nearby effects with duplicate suppression and budgets, not unlimited emitters every tick.

## 10. Codex Pages and Personal Journals

There are intentionally **two systems**, not a replacement of the original book:

- Registered pages are shared manuals supplied by the mod or addons. They belong in the always-available Codex Pages section.
- Journal entries are built for one player from discoveries, chapters, choices, earned lore, and relationships. They must not be appended to the shared static page registry.

The UI adds searchable contents, entry/chapter navigation, pagination, and back navigation. A registered entry may take several displayed pages; pagination should not be confused with registering the same entry repeatedly.

Registration fragment, submitted before Codex initialization:

```java
DivineCodexPageDeferredRegister pages =
        new DivineCodexPageDeferredRegister("example_pantheon");
pages.register("moon_guide", () -> new DivineCodexPage(
        Component.literal("Moon Shrines"),
        Component.literal("Contact Selene at her statue to begin.")));
pages.submit();
```

Use translatable components for localized production text. Do not call `registerAll()` every time the book opens. Register once, then let the screen read and paginate the existing data. Multiple legitimate pages may share a title, so blindly deduplicating by translated title would also be wrong.

The personal journal must tolerate opening before synchronization arrives and invalidate wrapping when language/layout/content changes. Spoilers are filtered by server progression, not just hidden by a button on the client.

Read [DivineCodexPagesRegistry.java](/Users/kaydenalexis/Documents/DivinityEngine/src/main/java/net/nightshade/divinity_engine/registry/divinity/DivineCodexPagesRegistry.java) for shared pages and [DivineCodexScreen.java](/Users/kaydenalexis/Documents/DivinityEngine/src/main/java/net/nightshade/divinity_engine/client/screen/DivineCodexScreen.java) for the combined reader.

## 11. RenderLib From the Beginning

### What Nightshade Does

Nightshade Core is the supporting library mod. RenderLib is its rendering subsystem. Divinity's `BlessingVisuals`, `ShrineUi`, and effect composers are consumers of that subsystem, not Minecraft's particle engine itself.

Use the right tool for the job:

| Desired result | Tool |
| --- | --- |
| Sparks, smoke, falling ice dust, little soul wisps | `ParticleEmitterSpec` and `RenderParticles` |
| A coherent mirror, shield outline, root cage, or slash shape | `RenderEffectSpec` plus a custom world renderer |
| Tint or transform an existing entity/item model | A client `RenderRuleRegistry` rule returning a `RenderPlan` |
| Panels, text, themed backgrounds, inventory chrome | `UiNode` trees and a Nightshade screen host |
| A special per-pixel surface such as the animated galaxy | A material shader, with a non-shader fallback |

You do not need to write a shader to use RenderLib. Start with particles. Also, a rendered root cage is not a collision box; immobilizing an enemy remains server gameplay code.

### The Vocabulary

An **anchor** says where the effect belongs. A **palette** says which colors it uses. A **spec** is the description sent/stored by the system. A **handle** identifies the spawned effect so it can be changed or removed. A **primitive** is the shape/renderer category. A **material** controls how surfaces are drawn. A **shader** is the GPU program that processes those surfaces.

`RenderAnchor.world(position)` stays at a world point. `block(pos)` uses the block center. `entity(uuid, offset)` follows an entity; an entity anchor is hidden if that entity is unavailable on the client. A world-position snapshot does not follow the player just because the player spawned it.

Colors are generally **ARGB**, written `0xAARRGGBB`: `0xFFFFA040` is opaque orange, `0x80FFA040` is partly transparent orange. Passing `0xFFA040` as an ARGB color omits the alpha byte and can make it invisible. Presence definitions deliberately use RGB and add alpha in their adapter, so check the API rather than assuming every integer color is identical.

`new RenderVisibility(32, false, false, true)` means maximum distance 32, no through-wall drawing, no shadow flag, and visible in first-person rendering. Visibility is not packet privacy; sensitive tracking marks need an owner-only delivery path.

### A First Particle Burst

This is a complete utility class using the local Core API. Invoke it once from a successful server-side action, on the server thread. It has not been installed into the mod by this guide.

```java
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.nightshade.renderlib.api.RenderAnchor;
import net.nightshade.renderlib.api.RenderHandle;
import net.nightshade.renderlib.api.RenderPalette;
import net.nightshade.renderlib.api.RenderVisibility;
import net.nightshade.renderlib.particle.ParticleEmitterSpec;
import net.nightshade.renderlib.particle.RenderParticles;

public final class LearningEffects {
    private LearningEffects() {}

    public static RenderHandle sparks(ServerLevel level, Vec3 position) {
        ParticleEmitterSpec spec = ParticleEmitterSpec.builder()
                .anchor(RenderAnchor.world(position))
                .palette(new RenderPalette(List.of(0xFFFFC16A, 0xFFFFF1CE), 20))
                .visibility(new RenderVisibility(32, false, false, true))
                .duration(24)
                .sphere(0.3, 0, 20)
                .burst(8, 20)
                .maximumAlive(8)
                .renderer(new ResourceLocation("nightshade_core", "textured_billboard"))
                .texture(new ResourceLocation("minecraft", "textures/particle/spark_0.png"))
                .size(0.12, 0.025)
                .alpha(0.85, 0)
                .velocity(new Vec3(0, 0.025, 0), new Vec3(0.02, 0.01, 0.02))
                .build();
        return RenderParticles.spawn(level, spec);
    }
}
```

Here is what those lines mean:

- `sphere(0.3, 0, 20)` selects a spawn region of radius 0.3, no continuous emission, and a particle lifetime of 20 ticks.
- `burst(8, 20)` emits eight particles together rather than eight per tick.
- `duration(24)` is the emitter/effect duration. It is separate from each particle's lifetime; leave enough duration for a burst to be visible.
- `maximumAlive(8)` bounds the live particle count for this emitter. It does not by itself limit how many separate emitters your gameplay creates.
- `size` and `alpha` interpolate from start to end over particle life.
- `velocity` supplies movement and randomized spread, in the engine's tick-based simulation.
- The texture is an ordinary vanilla PNG path, not a Forge `ParticleType` ID.
- The returned handle refers to this spawned effect, not to its registered definition.

Builder order matters: choosing an emission shape replaces emission settings; `burst` then adds the one-off burst, and `maximumAlive` sets its final cap. In this API `glow(...)` also selects the glow material; if you want Divinity's alpha material, set `.material(BlessingVisuals.PIXEL_MATERIAL)` after that call, with Divinity's common registration already initialized.

For an early cancellation, keep the returned handle and call `RenderParticles.remove(level, handle)` on the server. For a client-only cosmetic preview, use `spawnLocal` and `removeLocal` on the client. Do not spawn both a synchronized server effect and an identical local effect for the same action, or the caster will see duplicates.

For a repeated emitter, `.ring(0.8, 0.25, 20)` means an average rate of 0.25 particles per tick, roughly five per second at normal TPS, around a ring of radius 0.8. It does not mean 0.25 particles per second. Always bound duration and alive count.

### How Synchronization Works

```text
successful server action
 -> construct one immutable effect description
 -> RenderParticles.spawn or RenderEffects.spawn
 -> Core tracks the effect and synchronizes it to relevant clients
 -> each client animates/draws its local representation
 -> lifetime expires, or the owner removes it early
```

A spec is not a renderer class sent over the network. Both sides must already know its registered identifiers. The client receives data and uses its installed implementation. Use common code for descriptions and client-only classes for drawing; a dedicated server must not load `Minecraft`, `GuiGraphics`, or GPU renderer classes.

Core can manage and persist effect lifecycle, but a short gameplay visual is not intended to be immortal. Owner logout, death, lost eligibility, or dimension travel may require removal before natural expiration. Divinity's `BlessingVisuals` adapter supplies additional ownership cleanup and admission budgets.

Verified local API sources: [RenderEffects](/Users/kaydenalexis/Documents/NightshadeCore-Forge-1.20.1/src/renderlib/java/net/nightshade/renderlib/api/RenderEffects.java), [ParticleEmitterSpec](/Users/kaydenalexis/Documents/NightshadeCore-Forge-1.20.1/src/renderlib/java/net/nightshade/renderlib/particle/ParticleEmitterSpec.java), and [RenderParticles](/Users/kaydenalexis/Documents/NightshadeCore-Forge-1.20.1/src/renderlib/java/net/nightshade/renderlib/particle/RenderParticles.java).

## 12. Give a Blessing Its Own Effect

Divinity adds a higher-level layer so you usually do not need to call Core directly in every blessing.

```text
Blessing callback
 -> BlessingVisuals.pulse / at / link
 -> full blessing ID selects its BlessingVisualDesign
 -> bound god selects its palette
 -> Core spawns a particle emitter, mesh effect, or both
 -> client selects that blessing's mesh composer if one is required
```

There are three different responsibilities:

| File/system | Its responsibility |
| --- | --- |
| `BlessingVisualProfiles` | Theme colors/textures and the association between a blessing ID and a god. |
| `BlessingVisualDesigns` and the three design groups | Duration, particle author, particle count/size, and whether the effect needs a mesh. |
| `BlessingEffectRenderer` and the three client composer groups | The actual custom geometry, animation, and detail choices. |

The current catalog has 90 explicit designs: 35 particle-only, eight mesh-only, and 47 combined. The `variant` number still present in style metadata is not a choice between five finished reusable effects. Two blessings can share a god palette or helper without sharing their final composition.

### Add a Particle-Only Design

This common-setup fragment assumes `example_pantheon:selene` and `example_pantheon:moon_ward` are real registered content. Run it once in enqueued common setup, not on every cast or resource reload. The classes are in `net.nightshade.divinity_engine.util.divinity.effects`.

```java
ResourceLocation god = new ResourceLocation("example_pantheon", "selene");
ResourceLocation blessing = new ResourceLocation("example_pantheon", "moon_ward");

BlessingVisualProfiles.registerTheme(god, new BlessingVisualProfiles.Theme(
        "moon", 0xC0BED8F5, 0xA0637C9E, 0xE0F5FAFF,
        new ResourceLocation("minecraft", "textures/particle/spark_0.png")));
BlessingVisualProfiles.registerStyle(blessing, new BlessingVisualProfiles.Style(god, 0));
BlessingVisualDesigns.register(blessing, BlessingVisualDesign.builder()
        .duration(20)
        .particles(8, 0.08, (particles, context) -> particles
                .cylinder(0.45, 0.8, 0, context.lifetime())
                .velocity(new Vec3(0, 0.025, 0), new Vec3(0.01, 0.005, 0.01)))
        .build());
```

Register a god theme only once even if you add several blessings. To add another blessing for a built-in god, use its existing theme rather than registering the god theme again. The design helper currently restricts duration to 8..32 ticks, count to 1..16, and particle size to 0.025..0.3.

Inside that blessing's successful server callback, emit the cue:

```java
BlessingVisuals.pulse(instance, living, BlessingVisuals.Cue.WARD);
```

Other placements are `at(instance, owner, impactPosition, Cue.IMPACT, radius)` and `link(instance, owner, startPosition, endPosition, Cue.HEAL)`. A link supplies endpoints; the chosen design still has to author how it uses them. `Cue.WARD` describes an event, not a promise that a generic sphere appears.

The adapter requires a bound god and a matching registered design. Merely placing a `pulse` call in a class does not author an effect for an unknown blessing. Unregistered designs intentionally do not silently fall back to the same generic ring.

### Add Custom Geometry

If particles do not communicate the mechanic clearly, add `.mesh()` to the common design. Then, in a separate physical-client class during enqueued client setup, register its composer:

```java
BlessingEffectRenderer.register(
        new ResourceLocation("example_pantheon", "moon_ward"),
        (mesh, frame) -> {
            double extent = 0.25 + 0.45 * frame.progress;
            Vec3 bottom = new Vec3(0, -extent, 0);
            Vec3 right = new Vec3(extent, 0, 0);
            Vec3 top = new Vec3(0, extent, 0);
            Vec3 left = new Vec3(-extent, 0, 0);
            mesh.line(bottom, right, frame.highlight);
            mesh.line(right, top, frame.highlight);
            mesh.line(top, left, frame.highlight);
            mesh.line(left, bottom, frame.highlight);
        });
```

This deliberately small learning composition draws an opening diamond. It is not another complete built-in effect. `frame.progress` is the motion-adjusted normalized age. The framework handles the local orientation, scaling, fading, and bounded submission. Use `frame.detail` to reduce optional decoration at low quality.

A mesh is a collection of vertices. A vertex is a position plus data such as color. Two endpoints make a line; three points make a triangle; four corners make a quad. More detail means more geometry and more work, not necessarily a clearer effect.

The local composer coordinates are relative to the effect anchor and oriented through the frame's basis. `frame.forward`, `right`, and `up` establish its directions. **Do not subtract the camera again**: the shared renderer/Core already handles camera-relative world drawing. Link geometry also needs to respect the adapter's midpoint placement.

Common `.mesh()` with no client composer draws no mesh. A client composer without a common mesh-enabled design is never requested by that blessing. Those two halves must agree on the full ID.

Read [Blessing Effects Rendering](/Users/kaydenalexis/Documents/DivinityEngine/docs/blessing-effects-rendering.md) for the mesh helpers. Compare the actual [CelestialBlessingEffects.java](/Users/kaydenalexis/Documents/DivinityEngine/src/main/java/net/nightshade/divinity_engine/client/effects/CelestialBlessingEffects.java) compositions rather than starting with a large shader.

### Detail Without Visual Noise

Choose a silhouette and motion that communicate the blessing: a mirror at a reflection point, grasping roots at a rooted target, a short slash along an attack, wisps moving toward a healed owner. Reuse low-level drawing helpers, not the entire same animation for unrelated mechanics.

Minecraft-like presentation uses crisp textures, readable stepped/blocky silhouettes, restrained transparency, and limited bright areas. Match the god's palette without turning every ability into an opaque colored dome. Invisibility-sensitive auras must not reveal a hidden player; tracker marks use an owner-only path rather than public effects.

The adapter's owner/level admission budgets and the client's bounded mesh/detail settings are deliberate. Do not bypass them to make an effect "stronger." For a persistent passive, refresh at a controlled interval or maintain an owned handle; do not create an unbounded new long-lived emitter on every tick.

Catalog and extension reference: [Nightshade Blessing Effects](/Users/kaydenalexis/Documents/DivinityEngine/docs/nightshade-blessing-effects.md).

## 13. Build a Nightshade UI

A Minecraft **menu** contains server-side inventory/action rules. A **screen** is the client's view of that menu. Replacing the appearance must not replace server slot validation or turn a screen into the authority for quest rewards.

Your screens extend `RenderLibContainerScreen<MenuType>`. Nightshade hosts retained `UiNode` trees while Minecraft still owns normal item slots and existing widgets. "Retained" means describing a tree and keeping it until it needs rebuilding, rather than reconstructing all content every rendered frame.

A node has an ID/key, type, style, properties, and children. A `canvas` contains other nodes. A `panel` draws a background. A `text` node draws text. A custom type such as the shrine stone node delegates its drawing to your registered client renderer.

This fragment goes in `init()` of a `RenderLibContainerScreen` subclass, after `super.init()`. It uses Divinity's convenience helpers, not extra Core methods:

```java
int x = (width - 220) / 2;
int y = (height - 120) / 2;
setRenderlibRoot(UiRenderLayer.BACKGROUND,
        DivinityUi.canvas("learning_panel")
                .child(DivinityUi.panel("stone", x, y, 220, 120, 0xFF252B2D))
                .child(DivinityUi.text("title", "Moon Shrine",
                        x + 12, y + 12, 196, 12, 0xFFE7DAB8))
                .build());
```

The fragment does not register a menu, screen factory, or buttons. It shows the visual tree once the screen host exists. Use real translated and server-synced values instead of the literal title in a finished screen.

[PocketDimensionInvScreen.java](/Users/kaydenalexis/Documents/DivinityEngine/src/main/java/net/nightshade/divinity_engine/client/screen/PocketDimensionInvScreen.java) is the clearest complete project example. It keeps the original slots, centers a larger frame around them, sets its background tree, and lets `super.render` draw the hosted UI and inventory. Its decorative padding is also included in outside-click detection so clicking the frame does not drop a held item.

For custom nodes, there are two registration steps:

1. Declare the type ID in common setup, as in [DivinityUiTypes.java](/Users/kaydenalexis/Documents/DivinityEngine/src/main/java/net/nightshade/divinity_engine/registry/gui/DivinityUiTypes.java).
2. Register its drawing implementation with `CustomUiNodeRendererRegistry` on the physical client, as in [ShrineUi.java](/Users/kaydenalexis/Documents/DivinityEngine/src/main/java/net/nightshade/divinity_engine/client/ui/ShrineUi.java).

Use ordinary widgets/the project's Nightshade button wrappers for input. Their callback sends a validated request. A shrine menu must remain bound to its selected god on the server, not merely hide other god tabs on the client. Quest previews, donations, and reward claims likewise require server checks.

Rebuild when data, layout, selection, or language changes. Recalculate clipping, scrolling, wrapping, and hitboxes when the window/GUI scale changes. A screen that fits your monitor is not necessarily usable on a smaller window. Keep pointer focus and keyboard narration working rather than replacing every control with painted text.

The current blessings shrine presentation is stone and sigils. The pocket inventory retains the galaxy presentation, and the Codex retains a book aesthetic. Older UI documents describe earlier visual iterations; they are not a reason to restore the superseded blessing style.

## 14. Rendering Existing Models and Using Shaders

### Render Rules Are Different From Spawned Effects

A world effect creates an additional visual object. A render rule changes the presentation of an object already being drawn. A rule contains an ID, priority, a cheap matching predicate, and a provider returning a `RenderPlan`.

This client-setup teaching fragment tints all skeletons orange. It is not a feature enabled in the mod:

```java
RenderRuleRegistry.registerEntity(
        new ResourceLocation("example_pantheon", "learning_skeleton_tint"),
        100,
        context -> context.entity().getType() == EntityType.SKELETON,
        context -> new RenderPlan(
                RenderTransform.IDENTITY,
                0xFFFFB060,
                1.0F,
                -1,
                java.util.Optional.empty(),
                java.util.Optional.empty(),
                false,
                java.util.List.of()));
```

`RenderRuleRegistry` is in `net.nightshade.renderlib.client.rule`; `RenderPlan` and `RenderTransform` are in `net.nightshade.renderlib.api`. The plan uses no extra transform, the given tint, normal alpha, no light override (`-1`), no texture/material override, no suppression, and no overlay passes.

For a blessing-owned tint, match a **client-visible synchronized state**, not unsynchronized server-only NBT or scoreboard tags. Registration installs the rule once; the predicate decides when it applies. Do not register a new rule every tick or scan a large area inside its predicate, which runs during rendering.

The corresponding item, block-entity, particle, and screen rule APIs target their own render surfaces. A block-entity rule acts on an existing block-entity render path; it is not a replacement for ordinary block model resources, nor does it create a renderer for a block that has none. Render suppression changes visibility, not server collision or damage immunity.

### Shader, Material, and Texture

A texture is image data. A material selects drawing behavior. A shader processes vertices/pixels on the GPU. These are related but not interchangeable.

The pocket galaxy background is a custom UI node which can draw a quad with the galaxy material/shader. Its client renderer supplies uniforms such as time and color. When the shader is unavailable or disabled, it uses bounded fallback geometry instead of leaving the screen blank.

Read [GalaxyUiRenderer.java](/Users/kaydenalexis/Documents/DivinityEngine/src/main/java/net/nightshade/divinity_engine/client/ui/GalaxyUiRenderer.java) for material registration, availability checks, uniforms, and fallback. The blessing pixel particle shader is registered separately in [BlessingVisualClientSetup.java](/Users/kaydenalexis/Documents/DivinityEngine/src/main/java/net/nightshade/divinity_engine/client/effects/BlessingVisualClientSetup.java).

Shader assets live in `assets/divinity_engine/shaders/core`: the `.json` defines the shader and inputs/uniforms, `.vsh` handles vertices, and `.fsh` handles fragments/pixels. A particle texture normally lives in a textures directory and is sampled by the renderer; it is not a shader file.

Use the Forge port's shader APIs and vertex formats, not newer NeoForge pipeline examples. Resource reload must recreate usable shader state. Respect reduced-motion/effect settings, and test fallback as a real supported rendering path. Start with a particle or mesh before attempting a shader because most blessing silhouettes do not need custom GPU code.

Core's larger [RenderLib Wiki](/Users/kaydenalexis/Documents/NightshadeCore-Forge-1.20.1/docs/renderlib/wiki/Home.md) covers additional surfaces, presets, configuration, and UI sessions. Some inherited examples use newer-version syntax; use the checked local Forge source when a signature differs. The examples in this guide use the project's actual `ResourceLocation` constructor style.

## 15. Dependencies, Builds, and Testing

The current dependency versions are Nightshade Core **1.6.0**, Vanguard **2.2.1-1.20.1**, and Player Animator **1.0.2-rc1+1.20**. Player Animator remains a third-party library; it was bundled, not authored as part of this mod.

**JarJar** embeds dependency jars inside the release jar while keeping them separate mods with their own IDs. It does not paste their source into Divinity or rename all their classes. The slim jar is not the all-in-one release. Do not install both Divinity variants together.

Development uses mapped dependencies so source code has readable Minecraft names. Distribution uses reobfuscated artifacts appropriate to the normal Forge runtime. Updating the sibling Core source alone does not update the dependency already resolved by Divinity: publish the intended Core artifact to local Maven, then rebuild/refresh Divinity as appropriate. Version changes must match the dependency configuration and compatibility requirements.

Use Java 17 for the Gradle runtime and compilation toolchain. The build output is configured outside Documents under `~/.gradle/workspace-builds/divinity-engine/forge-1.20.1`. The project itself remains in your requested Documents location. This separates build outputs from source location; it does not guarantee iCloud availability for every source or world file.

Useful commands, run only when relevant:

```sh
./gradlew compileJava --console=plain
./gradlew narrativeChecks --console=plain
./gradlew jarJar --console=plain
./gradlew runGameTestServer -PgameTestNamespaces=divinity_statue_persistence --console=plain
```

`compileJava` catches source/type errors. Narrative checks exercise story logic. Forge GameTests exercise actual game objects and events. `jarJar` builds the distributable dependency bundle. None of those alone proves a visual looks good in a running client.

For an effect change, focus on the changed blessing's definition/renderer, compilation, and a visual check with normal/minimal particles, first/third person, and shaders disabled. For a statue change, focus on registration, save IDs, NBT/chunk/structure round trips, drops, and addon namespaces. Do not run broad unrelated suites or launch your real survival world as the first experiment.

Never rename released registry IDs or delete old NBT fields without an explicit compatibility design. Keep a backup before manual world-upgrade testing and use disposable worlds for development. An admin story reset removes persistent choices/rewards; it is a test/admin operation, not a migration technique.

Further reading: [Bundled Dependencies](/Users/kaydenalexis/Documents/DivinityEngine/docs/bundled-dependencies.md), [Integration Validation](/Users/kaydenalexis/Documents/DivinityEngine/docs/integration-validation.md), and [Blessing Effects Tests](/Users/kaydenalexis/Documents/DivinityEngine/docs/blessing-effects-tests.md). Their historical results are separate from this documentation-only update.

## 16. A Practical Learning Order

You do not have to understand every renderer, packet, and registry before making something useful. Work through one complete vertical slice, then add complexity.

1. Trace Solar Ward from the input packet to its callback, specialization check, and visual cue. Identify which code is server-only and which is client-only.
2. In a development branch, change one existing effect's count, size, or velocity and observe exactly what changes. Keep damage and cooldown unchanged.
3. Add a small new particle-only design for a test blessing with a stable namespaced ID. Verify one successful trigger produces one burst.
4. Add a four-line mesh composer for that same test effect, then animate one dimension using the frame rather than wall time.
5. Add a test god through `GodsDeferredRegister` and verify its automatic statue, item, name, drops, and save/reload in a disposable world.
6. Adapt one example story using existing objectives. Test start, branch preview, interrupted progress, trial retry, and once-only turn-in.
7. Implement a real specialization check inside your test blessing. Compare two players with different branches and an ineligible player.
8. Build a small Nightshade panel around synced data, then add a validated server action. Only after that experiment with custom materials/shaders.

When something does not work, follow the chain rather than changing random files: **was it registered, did the server accept it, did state change, was that state synced, and did the client select the correct renderer?** That sequence is the foundation for building these systems yourself.
