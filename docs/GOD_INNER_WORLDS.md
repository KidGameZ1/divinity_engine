# God Inner Worlds

Target: Minecraft **1.20.1**, Forge **47.4.2**, Parchment **2023.09.03**, Java **17**.
Uses the existing bundled Nightshade Core, Vanguard and player-animation dependencies. No new runtime dependency, ascension system, or replacement quest engine.

## Player Guide

1. Make first contact with a god at that god's real statue.
2. Away from a statue, open the ordinary blessings interface, select that god, and choose **Commune**. A statue-opened interface remains restricted to its own god.
3. Read the warning, then choose **Begin Communion**. Stand still on safe solid ground while the room is prepared. Preparation normally takes at least three seconds and remains cancellable.
4. Your real player enters the god's Inner World. A meditating, vulnerable body remains at your original location. Nearby hostile mobs can still attack it while you read.
5. Read the conversation, request a compatible story, or preview/confirm a branch when the existing story reaches its choice chapter. Right-click the manifestation to reopen the conversation.
6. Choose **Return to Body** in the conversation or pause menu.

The selected god's **Commune** button opens the warning/eligibility screen. Entry and return have no player commands. Contact, windup and server checks still apply. No Champion requirement was added. A realm may have an additional addon-defined requirement.

First contact, prayer, all offerings, donations, physical rituals, trial starts and final story turn-ins still require the existing physical shrine interactions. Communion alone gives no favor, items or story progress. It cannot be used to store items or harvest the scenery. Active blessing inputs and passive/toggle ticks are suppressed during a visit; equipped blessings and cooldowns are not replaced or reset. Harmful effects, curses and real-player damage are not rolled back.

## Architecture and Ownership

| Area | Source |
| --- | --- |
| Common definition and deferred API | `api/innerworld/InnerWorldDefinition`, `InnerWorldsDeferredRegister` |
| Built-ins and lookup | `util/divinity/innerworld/BuiltinInnerWorlds`, `InnerWorldsRegistry` |
| Sessions, preparation, return, cleanup | `CommunionManager` |
| Save/recovery and source tickets | `CommunionLedger`, `CommunionTickets` |
| Damage deduplication | `CommunionDamage`, four `Communion*` mixins |
| Entities | `entity/TranceBodyEntity`, `GodManifestationEntity` |
| Packets, commands, events, configuration | `CommunionNetwork`, `CommunionCommands`, `CommunionEvents`, `CommunionConfig` |
| Nightshade conversation and local presentation | `client/innerworld/` |
| Automated checks | `gametest/CommunionGameTests`, opt-in `CommunionSmokeTest` |

There is one shared `divinity_engine:inner_world` dimension. Trials and pocket rooms are untouched. Each visit owns a persisted 128-block-spaced slot with finite bounds. Built-ins now occupy 13 x 8 x 13 blocks: an empty black void with the god's statue, an invisible support floor and invisible edge barriers. Slots cannot be reused until their recorded bounds have been cleared. Cleanup never deletes a dimension folder, shared template, trial arena or pocket room. Recovery of older visits retains their recorded 25 x 16 x 25 bounds; it does not shrink cleanup to the new definition.

The real player remains the only health/inventory/progression authority. The proxy has only six **visual equipment copies**, no inventory capability, loot or independent blessing execution. Skin lookup uses already-tracked client player information and a UUID fallback, not synchronous profile requests. Both player model types and displayed armor are supported. The manifestation is a separate, non-worship entity rendered with the god's existing statue model.

Server lifecycle: `PREPARING -> ENTERING -> ACTIVE -> RETURNING -> CLEANUP`; a fully cleaned and returned record is removed (the reserved `CLOSED` enum is not kept as a growing history). The Overworld's game time is the single timer source. Session IDs and interaction revisions invalidate delayed choices. Server-owned story memory remains the checkpoint; no inventory, potion, health or story snapshot is restored on return.

## Damage Contract

The exact Forge 47.4.2 patched source was inspected. `Player.hurt` calls the cancelable `ForgeHooks.onPlayerAttack` **before** its mitigation path. The return injection observes the final event result, including LOWEST-priority cancellation. The flow is:

1. Body `LivingAttackEvent` and owner/PvP eligibility checks run.
2. Raw damage enters the real player's `hurt`, preserving the original damage source.
3. If the owner's Forge attack event remains authorized, choices close and the player returns before armor, absorption, totem or health processing.
4. Normal real-player damage finishes. The body and session retire once.

Canceled attacks do not interrupt. Armor/absorption preventing health loss do not by themselves prevent interruption. Scope guards cover vanilla sweeps/explosions; projectile identity and same-tick source identity cover repeated callbacks. Real armor handles mitigation and durability. Visual proxy armor is omitted from server enchantment iteration; supported melee/projectile retaliation uses real-player equipment instead. Flaming-arrow/fire-aspect follow-up is forwarded to the returned owner.

If an exceptional fallback destination is not yet available, the session invalidates immediately and queues up to 16 raw hits while return chunks prepare. Those hits re-enter Forge cancellation/mitigation after return; this rare path can produce a second *attack attempt event*, but not an earlier health loss in the realm. It does not promise to persist in-flight damage sources across a JVM crash.

Support loss and suffocation interrupt and return rather than leave a floating body. Fire, lava and explosions use environmental damage. Nearby `Monster` targeting is refreshed once per second, within the configured radius, limited to 24 candidates, with line-of-sight, follow-range and alliance checks. Neutral endermen and zombified piglins are not made universally hostile. This is **not a replacement for vanilla natural spawning**: a proxy is not a real player for spawning-distance rules.

No universal third-party combat/protection compatibility is claimed. Both cancelable body and player attack events are respected. A protection mod that specifically queries the real player's dimension during the initial owner attack event sees the projection's dimension; integrations with location-specific rules must also consider the body event. Independently written AoE implementations without a shared attack scope may need explicit integration.

## Persistence and Recovery

`divinity_inner_worlds` is Overworld `SavedData`; `DivinityCommunionRecovery` is redundant minimum player recovery metadata in Forge persistent player data. Neither contains inventory or favor copies. Death clones do not inherit an active projection. Non-death clones retain pending recovery, and cooldown is deliberately copied.

Logout, timeout, missing body, unsafe support, resource failure, external teleport and administrative return use the same terminal path. A pending return survives in the ledger/player record and is retried at login. Startup abandons active sessions and resumes bounded deletion, not live conversation. Unmatched bodies discard themselves when they tick. A player-only old recovery record is **never** reinserted as deletion bounds because its slot might already have been reused.

Return prefers the saved anchor and checks nearby collision space within three blocks. Enemy presence is not a reason to relocate. If every nearby space is blocked, the anchor remains the fallback and ordinary danger continues. If the source dimension no longer exists, the fallback is the Overworld spawn. Normal admin travel should first use `/divinity_engine commune end <player>`; a forced external teleport that bypasses normal travel hooks is accepted as the new location, not fought with a teleport loop.

Source area: nine owned ticking chunks (3 x 3). Their session UUIDs use a feature-specific prefix; the Forge validation callback removes only stale communion tickets. Other feature/admin tickets are not unforced. Temporary preparation/return tickets expire after 60 ticks; explicit source and scene ticket removal happens on retirement/cleanup. Chunks are preflighted asynchronously before Forge's synchronous force-ticket call.

## Configuration

Server file: world `serverconfig/divinity-communion-server.toml` (use `defaultconfigs` for new-world defaults).

| Option | Default | Meaning |
| --- | --- | --- |
| `windupTicks` | 60 | Minimum meditation; room preparation can take longer |
| `cooldownTicks` | 200 | Ten seconds after return/recovery |
| `maximumSessionTicks` | 6000 | Five minutes active |
| `concurrentSessions` | 8 | Checked before allocation/body spawn |
| `blockOperationsPerTick` | 256 | Global preparation/edit/cleanup budget; maximum 512 |
| `maximumInstanceSide` | 48 | Reject oversized addon definitions |
| `sourceChunkLimitPerSession` | 9 | Values below nine disable entry rather than weaken body vulnerability |
| `hostileTargetRadius` | 16 | Bounded source-area threat search |
| `diagnostics` | false | Log preparation duration |

The block-job loop also yields after approximately 2 ms. That is a cooperative scheduling budget, **not a guarantee that chunk loading, one addon callback, teleportation or the whole server tick takes less than 2 ms**. Scenes use flag-limited block changes and inert scenery; no giant synchronous structure placement is used.

Client file: `config/divinity-communion-client.toml`. `reducedParticles=false`, `reducedFlashing=true`, `screenTransitions=false`, `motionEffects=false`, `ambientVolume=0.3`. Skies, fog, ambience and particles are local to the owning client's session; dimension time/weather are not changed per player. Built-in voids suppress sky marks, ambient particles and screen tint regardless of these settings. Occasional ambient sound and the conversation/return controls remain. Non-void addon profiles retain configurable visual effects.

Permission-level-2 commands:

- `/divinity_engine commune status`: active/recorded sessions, cleanup backlog, source-ticket upper bound, last block count and maximum observed job-loop time.
- `/divinity_engine commune end <player>`: safe termination. Does not erase story choices.

## Built-in Visual Scope

The original 18 gods have statue-only voids. There are no visible platforms, buildings, vegetation, sky shapes, decorative particles or floating statue nameplates. The manifestation uses the god's original statue model and texture; right-clicking it still opens the existing dialogue. Realm names and stories remain unchanged. Aureth, Malgrath and Vaelith have no Inner World registrations.

An invisible floor and edge barriers allow normal survival movement without granting flight or risking a fall into the void. Each new scene has 1,352 bounded preparation positions instead of 10,000. The existing per-tick job budget, body vulnerability, interruption and safe-return rules are unchanged. Old session recovery removes only its recorded temporary scene before freeing its slot; no personal world structures are rewritten. Addon authors can still register custom scenery through the same API.

## Developer Registration

See the complete standalone source under `examples/inner-world-addon/`. It declares its own god and automatic statue, a two-branch story, a small physical trial and a realm using only public APIs. It is not bundled into the base mod.

```java
private static final InnerWorldsDeferredRegister WORLDS =
    new InnerWorldsDeferredRegister("my_addon");

// Declare in the addon constructor, after declaring the god.
WORLDS.register("quiet_garden", () ->
    InnerWorldDefinition.builder(id("quiet_garden"), MY_GOD.getId())
        .name("The Quiet Garden")
        .revision("1")
        .story(id("a_promise"))
        .scene(local -> local.getY() == 0
            ? Blocks.SMOOTH_STONE.defaultBlockState()
            : Blocks.AIR.defaultBlockState())
        .build());
WORLDS.register();
```

Submission is deferred until common setup. Definitions are immutable snapshots. One definition per god; namespaced definition and god IDs must be unique/registered. Older gods with no definition remain usable and display an unavailable explanation. Defaults provide a safe floor with local spawn `(12.5,1,5.5)` and manifestation `(12.5,1,19.5)`. `bounds(x,y,z)`, `spawn(Vec3)`, `manifestation(Vec3)` use **local coordinates only**. Never calculate or clear shared-dimension world coordinates yourself.

Use `.template(id)` for an existing vanilla structure resource. Missing templates and out-of-bounds positions reject entry with a resource-specific log and no body. Only inert block palettes are accepted: no fluids, block entities, containers or template entities. Templates override `.scene`. Existing active sessions retain their resolved scene/variant; registration is Java/common-setup based, not a new script or datapack quest language. After a resource reload, subsequent visits resolve the current template; saved recovery uses recorded bounds, never a newly changed template's bounds.

`.requires(explanation, predicate)` adds server checks beyond contact. `.variant(new Variant(key, predicate, visuals))` chooses the first matching profile. Predicates can consult existing `GodHelper`, `DivineNarrativeCapability` story/branch records and trial data. Keep predicates cheap and read-only. Use a new `.revision` value when changing an agreement/scene's design; stable god, story and specialization IDs preserve saves.

`Visuals` accepts a color, registered sound ID, registered **simple particle** ID and motif string. The `void` motif used by all built-ins renders a black background without sky decorations, ambient particles, transitions or a manifestation nameplate. Older motifs such as `sun`, `dream`, `storm`, `aurora` and `stars` remain supported for addons. Unknown motifs safely use the basic color backdrop. Custom namespaced motifs can register bounded `Mark` providers through client-only `InnerWorldPresentationRegistry` from `FMLClientSetupEvent.enqueueWork`; never import that client class into a common definition. Providers receive accessibility flags and must honor them. Invalid providers fall back without trapping the player.

Lifecycle callbacks: `REQUEST`, `ENTER`, `INTERACT`, `STORY`, `INTERRUPT`, `RETURN`, `CLEANUP`. `Context` includes the nullable player, session ID, god, variant and allocated origin. Offline cleanup may have no player. Exceptions are logged; callbacks cannot veto safety exits. **Do not grant rewards from lifecycle hooks**; use the existing story reward records. Hooks may send personal dialogue, request `CommunionManager.sceneBlock(player, session, localPos, inertState)` (bounded queue, no floor/boundary/player obstruction), or call `end(player, Exit.NORMAL)`.

Whitelisted story actions are `request`, `preview`, `confirm`, `status`, `lore`. A registered `.story(id)` grants only those actions for a story bound to the same god. Packets also require current owner/session/revision. Final rewards, offerings and donations are never accepted by the communion route. The existing server story engine still validates stage, favor, permanent choice token and reward idempotence. Existing seven-chapter stories and Codex pages remain authoritative.

## Validation and Manual Coverage

Run only focused checks in isolated saves:

```sh
./gradlew runGameTestServer -PgameTestNamespaces=divinity_inner_world --console=plain
./gradlew compileInnerWorldAddonExample innerWorldAddonJar --console=plain
./gradlew jarJar verifyBundledMods --console=plain
```

The source includes an opt-in packaged-client smoke using `-Ddivinity_engine.communionSmokeTest=true`. It deliberately refuses ordinary worlds: the game directory must be a fresh `nightshade-production-smoke-communion-*` test directory, with the isolated `Core-Smoke` fixture inside it. It visits each registered realm and writes screenshots/report data under `communion-smoke`. Never rename a personal save to run it.

The 2026-10-16 statue-only update passed all 19 `divinity_inner_world` GameTests and all eight `divinity_elder_retirement` compatibility tests on Java 17. These include every built-in scene's Air/Barrier bounds, real entry/return, interruption cleanup, and recovery of an older larger arena whose god is no longer registered. `compileJava`, `jarJar`, and `verifyBundledMods` also passed. The compatibility checks preserve old Elder statue block/entity/item IDs and opaque NBT while removing their gameplay and future shrine generation.

A packaged-client Aethon visit also passed in a fresh copy of the synthetic `Core-Smoke` fixture on 2026-10-16. Screenshots of the offer, live conversation and realm were reviewed: the realm shows the original statue against black, without scenery or a nameplate. The test observed automatic dialogue after the dimension transition, verified 1,039 air and 313 invisible barrier positions, and returned to the original dimension with the temporary body and manifestation removed. This is one real visual visit, not visual certification of every god or multiplayer behavior. Run `node docs/tools/run_communion_preview.mjs --god aethon` to reproduce it with a fresh packaged jar; the launcher refuses personal saves.

Still perform: two human clients in independent rooms, standard/slim skin and armor viewing by a second client, reconnect during preparation/return, process restart and hard-crash fault injection at each write boundary, origin dimension removal, blocked anchors, third-party protection/combat mods, additional language checks, and a full physical story/trial/finale playthrough after interrupted communion. Verify no residual owned tickets, bodies or allocations after repeated sessions. Automated fixtures are not evidence that every personal save or mod combination was tested.

No existing user world is used by the automated feature checks.
