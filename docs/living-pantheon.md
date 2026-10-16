# Living Pantheon

## Player Guide

At Favorable tier or higher (50 or more favor), open the god's statue, select **Quests**, and choose **Begin quest**. **Speak about quest** opens both path previews, permanent confirmation and any available epilogues in a conversation screen. The menu only shows quests belonging to its selected god, including multiple addon stories. Inventory-opened menus show the same progress, but quest actions require the matching statue within six blocks.

The seven chapters are **The Call** (accepted offering), **A Witness** (a thematic objective), **Divided Testimony** (preview and permanently confirm a path), **The Chosen Deed**, **The Reckoning**, **The Trial** (a fresh victory), and **The Return**. Read full discovered prose in the Codex. A failed trial permits retries without repeating completed chapters.

Preview a branch, then click the second confirmation. This choice is permanent for that player's story. A confirmation expires after one minute and is rejected if the player leaves the loaded matching statue's six-block radius, loses contact, or the definition changes before confirmation.

Complete the deed and reckoning, start and win a fresh trial for that god, and return to **Quests > Claim ending and upgrade**. The ending awards 25 favor, capped at the ordinary maximum, a lore ending, recognition at that god's shrines, and the selected blessing specialization exactly once. The quest tab distinguishes previewed rewards from earned rewards; the Powers tab prepends the earned upgrade to its original blessing's description. For example, Solarius's Judgment upgrades Solar Ward from ten to fifteen seconds; Sanctuary instead raises its activation health threshold from 50% to 65%.

## Favor And Divine Bonds

Existing saved favor and blessing thresholds are unchanged. The **Favor** tab shows progress toward the next tier, highest remembered favor, trial victories, offerings and remaining worship budgets. Tier thresholds remain 50 Favorable, 101 Devoted and 176 Champion, with a 250 maximum.

Each player's god has a persisted, independent 24,000-server-tick worship window (20 minutes at 20 TPS). Sleeping, logout, death, dimension changes and abandoning/recontacting a god do not reset a budget. Windows advance only with server game time, not wall time or the sun.

- Prayer: up to 2 favor per ten seconds, maximum 12 per window. Existing god-specific conditions still affect the requested reward.
- Accepted offerings: up to 12 per interaction, maximum 30 per window. Once 15 favor has already been earned, subsequent offering rewards are halved. Large stacks cannot bypass these limits; ordinary accepted offerings still consume the offered stack and can advance a story even after its favor budget is exhausted.
- Preferred shrine conditions: up to 1 favor per minute, maximum 3 per window. This removes unlimited AFK favor.
- Successful objective-chapter transitions: +5 once per completed objective. Migration and interrupted attempts grant nothing. Explicit story donations get this chapter reward, not ordinary offering favor.
- Trial victories: +50, credited to the correct god, including Nythea. Story finales: +25 and the permanent upgrade. These rewards bypass worship budgets.
- First placed shrine after contact: +5 once per god, remembered across reconnects. Breaking a shrine costs 10; accidental attack clicks no longer penalize favor. Repeated placement is not a favor farm.

No new permanent HUD, ascension, favor decay, forced single-god allegiance, or automatic loadout replacement is added.

Specializations retain the original blessing's key and eligibility rules. They do nothing when that blessing is unequipped or ineligible. Abandoning a god pauses the relationship but never removes its permanent choice, memories, or earned reward. Recontact resumes it.

There are no player story commands or command-backed chat actions. The Quests tab handles interactions, and the Codex records status and lore. The administrative recovery command `/divinity_engine story reset player namespace:god` requires permission level 2.

Donations use the explicit story action, consume only the exact requested item count, and do not award ordinary offering favor. Dropped shrine offerings are a different interaction. Continuous prayer, concealment and submersion attempts restart on interruption or logout; completed chapters remain complete. Kairoth's timed kill attempt begins with the first qualifying kill and may be retried without penalty.

## Addon API

DivineNarrativeDeferredRegister, DivineNarrativeRegistryObject, DivineStoryDefinition, GodPresenceDefinition, StoryObjectiveType, and BlessingSpecialization are public APIs. Submit registers during addon construction, before common setup. Built-in stories use the same builders.

The example at examples/living-pantheon-addon/ExamplePantheonAddon.java demonstrates definition registration for an addon's already-registered god. A definition binds by full namespaced god ID, never by translated name. The story registry and presence registry are independent of active player progress.

Register custom objective types with StoryObjectiveType.register. From a successfully completed server event, call DivineNarrativeManager.recordObjective(player, type, condition, amount). Do not report progress before a cancelable event has finished; do not trust client-reported kills, inventory, duration, or position. The manager rejects calls off the server thread and ignores players without that active contacted chapter.

Two branch keys, a and b, are stable save identifiers. Built-in specialization IDs use namespace:god_a and namespace:god_b, but addons and packs may choose any stable namespaced reward IDs. A confirmed branch stores its immutable deed/reward snapshot in the independent player capability. Reloading a definition does not alter a deal already confirmed. Never rename an earned reward ID without an explicit save migration. Runtime blessing tuning remains separate from these IDs.

DivineNarrativeManager.hasSpecialization(player, godId, branch) reports an **earned ending**, not merely a chosen path. Eligibility and equipped-blessing checks are the responsibility of the specialization implementation.

DivineNarrativeManager.hasSpecialization(player, specializationId) queries an earned reward directly by its saved ID. Neither overload reconstructs addon reward IDs from the god's name or rewrites existing earned records.

## Pack Definitions

Server datapacks may add or replace definitions under data/namespace/divine_narratives/name.json.

Each file has version 1, a kind of story or presence, and a full god ID. See examples/living-pantheon-pack for complete files. Story definitions contain a request and two branch nodes. The engine enforces the common offering, deed, fresh trial, and ending transitions. Conditions and objective IDs are validated; arbitrary executable scripts are not loaded.

Presence definitions provide an RGB hex color, vanilla or resource-pack texture ID, registered sound ID, and dialogue arrays. Greeting, prayer, and offering arrays need at least three entries. Other supported keys include all favor tier names, return, failure, victory, and recognition. Texture assets are ordinary resource-pack PNGs. Personal text and sounds go only to that player.

Reloads use Forge's server reload apply stage. Definitions are collected and validated before replacing the registry snapshot. A malformed definition retains the last valid snapshot rather than partially applying a pack. Unconfirmed previews are invalidated if their definition changed.

Effects go through the isolated CorePresenceEffects adapter and Core's synchronized independent particle engine. A shrine emits for 80 ticks with particles living at most 20 ticks; no tail can exceed five seconds. Maximum live particles are 24 per shrine, visibility is 24 blocks, same-shrine overlaps are suppressed, and no more than 64 shrine emitters are admitted per level in a five-second window. Core's own budgets remain in force. No chunks are loaded to greet a player or process a story action.

## Integration And Testing

NarrativeBootstrap.register(modBus) is the one-time integration entrypoint. It attaches the independent capability, queues common definition/network setup, registers Forge events and commands, and adds the Codex guide. It is intentionally not an automatic subscriber so integration can remain behind the standalone Core validation gate.

Run ./gradlew narrativeChecks for dependency-free state-machine checks covering all 36 paths, exact donations, cancellation, stale actions, attempts, persistence contracts and player isolation.

DivineNarrativeGameTests adds Forge coverage using the existing divinity_engine:pocket_dimension template: actual NBT round trips, all donation inventories, canceled and reentrant callbacks, loaded-shrine confirmations, successful/canceled shield events, continuous-attempt recovery, Favorable entry boundaries (49 rejected, 50 accepted), and custom addon reward IDs surviving reconnect. These tests passed on 2026-09-22 as part of the integrated 24-test Forge suite, alongside 790 pure state-machine assertions and compilation of the addon example. They do not replace manual multiplayer, rendering, or full trial playthrough checks.

The quest-menu/favor update was checked separately on 2026-09-23 with `runGameTestServer -PgameTestNamespaces=divinity_quest_favor,divinity_shrine_ui` and `jarJar`: all 14 scoped GameTests passed. Coverage includes all 36 finale claims and specialization effects, duplicate/stale/foreign menu requests, exact donations, canceled shrine placements, saved worship budgets and one-time chapter rewards. The packaged client passed all 16 `run-codex-smoke.mjs --shrine` scenarios, including selected-god quests, multiple addon stories, earned upgrades, the Favor page, small layouts and shader fallback. Quest and upgrade screenshots were visually reviewed. Client scenarios use synthetic journal fixtures; this is not a complete story or multiplayer playthrough. Both client and server must update together because the narrative channel is now version 3.
