# Connected Pantheon Authoring

This guide describes the checked-in Connected Pantheon API and version-two loader. It supplements, rather than rewrites, `docs/living-pantheon.md` and the existing version-one examples. Read the current API and runtime when updating integrations; definition data, gameplay handlers and client presentation are separate responsibilities.

The original Codex page API is still supported. Pages submitted through `DivineCodexPageDeferredRegister` or added through `DivineCodexPagesRegistry.addPage` appear in the always-available **Codex Pages** section, in registration order, with their component formatting and individual page boundaries preserved. Built-in guides and addon pages remain readable before any god is discovered or narrative sync arrives. The five personal-journal sections supplement these registered pages; they do not replace or filter them. Use the narrative API for discovery-gated story material, not the public page registry.

## Complete Examples And Prerequisites

`examples/connected-pantheon-addon/ConnectedPantheonAddon.java` contains two complete builder stories for the same namespaced god, `example_pantheon:vesper`. `StarChartEvents.java` supplies and registers the first story's custom server event handler. The sibling `examples/connected-pantheon-pack` contains equivalent full version-two stories and their optional relationship, under `data/example_pantheon/divine_narratives/`.

The addon example is a narrative module, not an entire runnable mod. Its host must register Vesper, a matching statue, accepted offerings and a working Vesper trial through the existing god/trial systems. It must also declare its normal Divinity Engine dependency. No custom item, recipe, generation, NPC, or structure is needed for these stories. The compass is the vanilla `minecraft:compass`, metaphorically called a star chart. Suggested paper/amethyst opening offerings must actually be accepted by the host god; listing them in a chronicle does not implement acceptance.

Call `ConnectedPantheonAddon.register()` from the addon's constructor, after attaching its normal registries and **before common setup**. Class initialization registers `example_pantheon:star_chart` before story suppliers run or JSON is decoded. `DEFINITIONS.submit()` queues definitions for the engine's setup; it is not a datapack reload action. The method also explicitly registers `StarChartEvents.class` on `MinecraftForge.EVENT_BUS` once. Do not add a second automatic event subscriber. The base mod owns `NarrativeBootstrap.register(modBus)`; addons must not bootstrap the engine again.

Definitions store full god IDs, not translated names or live god objects. The god need not be resolved while constructing a story. This does not make an absent god playable: contact, favor, matching statue, accepted offerings and trial lookup require a valid registered bound god at runtime. JSON can define a story, not a new god implementation.

## Identities And Builders

| Identity | Example | Contract |
| --- | --- | --- |
| Bound god | `example_pantheon:vesper` | Shared contact, favor, shrine and trial identity. |
| Story | `example_pantheon:first_star`, `example_pantheon:last_lantern` | Independent persistent choices and progress for one god. |
| Objective type | `example_pantheon:star_chart` | Register before common setup; requires a server handler. |
| Condition | `craft_compass` | Exact handler-owned token, not translated prose. |
| Branch | `a`, `b` | Permanent local save keys; never swap their meanings. |
| Specialization | `example_pantheon:first_star_shelter` | Stable earned reward marker; distinct from a blessing ID. |
| Blessing | `divinity_engine:solar_ward`, `divinity_engine:twilight_veil` | Existing registered abilities, not newly registered by the story. |

Use `DivineStoryDefinition.builder(godId).id(storyId).title(...).request(...).branch("a", ...).branch("b", ...).chronicle(...).build()`. `DivineNarrativeDeferredRegister.story("first_star", supplier)` assigns its registry object's full `example_pantheon:first_star` ID to the supplied story; the registration key is authoritative. The example explicitly supplies the same `.id(...)` for clarity. Omitting `.id(...)` in a direct builder defaults to the god ID, which is insufficient for multiple direct definitions for one god.

Use `StoryChronicle.builder().premise(...).offering(itemId).witness(...).reckoning(...).chapter(...).build()`. `new StoryChapterDefinition(key, title, List.of(paragraphs), dialogue)` supplies long Codex prose and a short personal chat line. `StoryObjective.of(type, condition, target, description)` covers item-free objectives; `new StoryObjective(type, condition, target, itemId, description)` includes an item. For custom types, that item is only data: the handler must enforce its intended meaning. Donation objectives require it. Targets must be between 1 and 1,000,000; values represent ticks, actions, distance or points according to the handler, not according to the prose.

Both stories are independently requestable. Neither requires the other, and choosing `a` in one does not choose `a` in the other. Prefer explicit story IDs in commands. A legacy god-ID lookup can select a primary story (exact god ID first, otherwise sorted story ID); do not rely on sorting as a public identity or introduce new IDs to silently replace an old primary.

## Seven Chapters, Ten Passages

| Chapter | Runtime Gate | Passage Keys |
| --- | --- | --- |
| 1. The Call | At least 50 favor, contact and a loaded matching statue within six blocks; request, then make an ordinary accepted offering. | `call` |
| 2. A Witness | Complete the new witness objective after its chapter starts. | `witness` |
| 3. Divided Testimony | Preview both rewards and permanently confirm one branch at the statue. | `testimony` |
| 4. The Chosen Deed | Complete the chosen existing branch-shaped objective. | `deed_a`, `deed_b` |
| 5. The Reckoning | Complete the new reckoning objective. | `reckoning_a`, `reckoning_b` |
| 6. The Trial | Start and win a fresh registered trial for this god after reckoning. | `trial` |
| 7. The Return | Return to the statue and explicitly finish; only then earn the ending and reward. | `ending_a`, `ending_b` |

All ten passages are required for an expanded chronicle. Supply all ten dialogue keys too; although JSON has a generic fallback, it is not a substitute for authored chapter dialogue. Witness and reckoning each have **one shared objective**, not one per branch. Their alternative prose changes interpretation, not requirements. Preview expiry is 1,200 ticks; confirmation also validates the current choice, shrine, contact and previewed definition.

The server journal reveals only discovered gods, unlocked chapters and the chosen alternative. `RETURN` means the trial is won but the ending is still hidden until `finish`. Unchosen endings and unearned relationship epilogues must not be sent as player journal text. Do not make a client-only UI check the sole spoiler gate. These are progression gates, not encryption: installed datapack files remain inspectable by their owners.

Ordinary offerings use the god's normal offering rules and are distinct from explicit story donations. `/story donate` requires the remaining exact requested count in inventory, verifies the transaction after cancellation callbacks, consumes that count and grants no ordinary offering favor. Donation prose must not imply merely dropping an item completes it. One accepted ordinary offering can advance multiple already-requested stories for the same god. A qualifying gameplay event, or fresh trial started while several stories are ready, may likewise serve several active stories; independent saves do not imply exclusive event consumption.

Trial failures are retryable without repeating completed chapters. Old victories are not banked for future trial chapters. Abandoning a god pauses progression; it does not erase story choices or earned rewards. Recontact resumes the relationship.

## Tick Conditions And Event Semantics

Continuous targets are server ticks, approximately 20 per second at normal server speed. A target of 200 means ten consecutive seconds, not ten client-render frames. Observation does not require staring at a particular screen pixel, standing still or visiting a historical location. The checked-in `observe` handler supports these exact tokens:

| Condition | Server Predicate |
| --- | --- |
| `day_sky`, `night_sky` | Corresponding day/night state plus sky visibility at the player's block. |
| `forest` | Current biome has the forest tag. |
| `rain` | Rain is falling at the player's position. |
| `thunder` | Thundering and rain falling at that position. |
| `underground` | Player Y is below zero. |
| `high_sky` | Player Y is above 100 with sky visibility. |
| `water` | Player is underwater, not merely wading. |
| `snow` | Snow precipitation biome or a nearby supported snow block within two blocks on each axis. |
| `firelight` | Nearby fire-tag block, lit campfire or lava within two blocks on each axis. |

Nearby scans use loaded chunks and a cached candidate set with current-state checks. Finding newly introduced nearby sources can wait for the next scan. Do not ask players to stand in fire or take damage. Ordinary lanterns and torches are not this handler's `firelight` sources. No moon phase is required for `night_sky`. The built-in lore uses short 100-600-tick observations; this is an authoring convention, not a separate constructor range.

`pray` additionally requires active shrine prayer: sneaking with recent prayer contact at a loaded matching shrine. `still` requires no meaningful movement; `night_safe` adds night; `sky_night` adds night, Y above 100 and sky visibility. Continuous prayer, concealment, submersion and observation reset interrupted attempts rather than undoing completed chapters. Logout/dimension changes and other lifecycle interruptions invalidate transient attempt state. Timed kills use the type's 2,400-tick attempt window beginning with the first qualifying kill. Simply setting `continuous=true` on a custom type does not create a tick handler; implement verification, interruption and reset behavior yourself.

Both `StoryObjective` and `DivineStoryDefinition.Branch` constructors validate the known `observe`, `kill`/`timed_kill` and `pray` conditions through `StoryObjectiveType.validateCondition`. Invalid built-in tokens fail Java construction or JSON decoding rather than creating a silently impossible objective. Kill tokens are `undead_day`, `undead_night`, `poisoned`, `forest`, `melee`, `projectile`, `phantom`, `stray`, `drowned`, `witch`, `burning` and `any`. Prayer accepts the empty string, `still`, `night_safe` and `sky_night`. Other/custom types still need their handler's own condition contract.

Read `DivineNarrativeEvents.java` for the exact contracts of other built-ins. Examples: smelting counts collected iron ingots, XP counts newly gained points rather than levels, planting deduplicates positions, harvesting requires mature crops, sprint counts qualified movement distance, and height requires reaching Y=160 and returning to the shrine. An arbitrary custom condition string is not an executable predicate; the addon must implement and validate it.

## Custom Server Handler

`StarChartEvents.crafted` accepts only a live, connected survival/adventure `ServerPlayer` on the server thread, outside the trial dimension and any active trial session. It checks `Items.COMPASS` in `PlayerEvent.ItemCraftedEvent`, a non-cancelable successful crafting notification in Forge 1.20.1. It then calls its local `reportObjective` adapter, which uses the current public `DivineNarrativeManager.recordObjective(player, type, condition, amount)` hook and synchronizes the journal. `reportObjective` is the example adapter name, not a claimed additional engine method.

The manager matches active contacted chapters by objective type and exact condition. It cannot verify that your claimed event happened. Never expose this reporting hook in a packet accepting client-supplied amounts, conditions or item IDs. Never call it for recipe previews, inventory presence, tooltip updates or client animations. Keep Java and JSON conditions identical.

This objective counts **one qualifying notification**, deliberately not the number of items in `getCrafting()`. Result-stack counts are not a universal batch delta, especially with shift-click and third-party menus. The target is one and progress is chapter-clamped. A weak event-identity set prevents processing the same live Forge event object twice without retaining players indefinitely. The registration guard prevents duplicate listeners; server-stop cleanup releases the cache. No per-player-per-tick deduplication is used, since that would discard legitimate distinct crafts in one tick.

This is not a universal transaction-ID system: separately constructed duplicate events from another mod cannot be recognized as the same transaction. A total-items objective needs a verified producer-specific count and stable transaction identity, not multiplication by an output stack's apparent size. Test normal pickup, shift-click, repeated crafts and custom menus before promising exact batch accounting. Crafting before reckoning is not saved for later credit.

For a **cancelable** event, reporting at `LOWEST` priority is not enough: another listener may still cancel or change the result. Capture the active story memory, stage, objective, generation, player and dimension at event time; queue bounded work for **server tick END**, verify final non-cancellation and the actual world/inventory result, then revalidate that the captured chapter is still the same and contacted before awarding once. Drop work on logout, reset, dimension change or chapter transition. `MinecraftServer.execute` can run inline on the server thread and is not proof that event dispatch has finished. Do not defer a bare call that searches for whatever chapter happens to be active later. The engine's internal `capture`/`validTarget`/`award` pattern in `DivineNarrativeEvents` is a reference, not a public addon API.

## Version-Two JSON

Use `data/<namespace>/divine_narratives/<name>.json`. A full story contains `version: 2`, `kind: "story"`, `id`, `god`, `title`, `request`, `branches` and `chronicle`. Branches keep the version-one shape: `name`, `objective`, `condition`, `target`, optional `item`, `deed`, `reward` (`id`, `blessing`, `description`) and `ending`. Both `a` and `b` are mandatory.

The embedded `chronicle` contains `premise`, `offering_examples`, `witness`, `reckoning`, `passages` and `dialogue`, with optional `chapter_titles`. Each objective uses `objective`, `condition`, `target`, `deed` and optional `item`. JSON `deed` maps to Java `StoryObjective.description`; it is not a nested branch or a second objective. `passages` maps each of the ten keys to a paragraph array (a single text value is also accepted). `dialogue` maps the same keys to short text values. `chapter_titles` maps passage keys to literal strings or translation objects; omitted titles use `NarrativeDefinitionJson.chapterTitle`. The example overrides only `call`, using a translated title. The encoder includes Java chapter titles in accepted snapshots, so custom titles survive saves instead of reverting to defaults.

`kind: "chronicle"`, `version: 2`, `story: "namespace:story_id"` is a separate overlay format used by the built-in lore. Its premise/objectives/passages/dialogue are at the top level; it is not the full-story envelope. It preserves the catalog's branches and rewards. Do not mix the two shapes. `kind: "presence"` remains separate from a story's chapter dialogue and controls visual/audio presence and ambient lines. It does not satisfy a quest requirement.

The equivalent Vesper pack retains `example_pantheon:star_chart`, so it **requires the addon installed on the server**, including registration and handler. It is not a standard-handler-only drop-in pack. Replacing that reckoning with `divinity_engine:observe` would be a deliberate gameplay variant; use a new story ID if old saves should not share its contract. The second story uses only standard objectives but still requires the Vesper god/statue/trial implementation.

## Optional Relationships

`GodRelationshipDefinition` currently uses its public constructor, not an assumed builder. Supply a stable relationship ID, two distinct god IDs, two story prerequisite IDs in corresponding order, a spoiler-safe entry title, relation label, detailed summary and all four nonempty paragraph lists: `aa`, `ab`, `ba`, `bb`. JSON uses the same fields plus `version: 2` and `kind: "relationship"`. Always supply `stories` explicitly for addon stories; its fallback to the god IDs is only convenient for built-ins whose IDs coincide. The entry title must be safe at discovery; relation/summary may discuss facts revealed in the completed chapter arcs but must not reveal an unclaimed epilogue.

The example follows the same two-god/four-combination format as the nine built-in relationships. Its order is `[divinity_engine:solarius, example_pantheon:vesper]` and its story order is `[divinity_engine:solarius, example_pantheon:first_star]`. Therefore `ba` means Solarius's Sanctuary plus Vesper's Shelter, never the reverse. `example_pantheon:last_lantern` is not a hidden prerequisite.

Once both gods are discovered through contact, the journal shows only the relationship entry title and a generic god-pair teaser. It withholds the detailed `relation` and `summary` until both prerequisite stories are complete, or the player already has that relationship's earned epilogue. This is a server journal gate, not just hidden client text: summaries may contain secrets revealed later in the individual chapter arcs.

Completion of both prerequisite stories makes the explicit crossover action available but does not claim or reveal the epilogue. The player must request the conversation within six blocks of either related loaded statue while contacted with that god. The selected epilogue is persisted only once. Neither merely discovering both gods nor completing the second story automatically reveals it. Relationships grant no additional favor, blessing, item, allegiance lock or new compulsory quest.

## Snapshots, Stable Saves And Reloads

Requesting a current story captures its full accepted definition, including objective and chronicle text/translation markers. Confirming a branch also preserves its immutable deed/reward snapshot. A reload changes defaults for new acceptances, not an accepted bargain. Do not tell an existing player that editing a JSON target or reward description will rewrite their saved objective. Literal text in an accepted snapshot remains literal; a saved translation marker resolves through the current language resources.

Version-one saves without an `expanded` field migrate to the corresponding chapter when their registered story now has a chronicle. Existing stage, counters, confirmed branch, trial identity and earned reward remain intact; preceding chapters are considered discovered rather than new prerequisite work. A saved DEED continues at The Chosen Deed, then reaches The Reckoning; a saved TRIAL does not have to repeat a new reckoning first. Completed saves reveal their chosen chronicle without re-awarding favor. The original chosen branch snapshot still owns its deed and reward. The current chronicle is pinned at migration. An explicit `expanded: false` record or a version-one addon without a chronicle retains its shorter flow. Preserve god, story, reward and branch IDs and meanings; changed display text is not an identity migration. Adding another story for an existing god is not permission to reassign its old primary memory.

A reload rejects changing an existing story ID's bound `god` and retains the previous registry snapshot. For example, `example_pantheon:first_star` cannot be rebound from Vesper to Solarius by a pack override. Use a new stable story ID for a genuinely different binding; this safeguard protects persisted identity and is not a save migration. Relationship prerequisite stories must also correspond to their listed gods in the same order.

If a saved custom objective cannot be decoded because its addon is absent, raw saved data is retained rather than replaced with a different objective or reward. Such stories pause or are unavailable until their definitions/types return. Restoring the addon should use the same identifiers and semantics. A deliberate migration needs its own reviewed save-conversion logic; admin reset is destructive test tooling, not migration. Removing the example pack does not remove Java defaults, and removing definitions does not authorize deleting saved player data.

Server `/reload` reads `data/` definitions. Reload precedence is: Java defaults; full story/presence/relationship overrides; then chronicle overlays applied to the resulting story. A chronicle must reference an existing resulting story. For identical resource file paths, Minecraft's normal pack priority selects the visible file; two different visible files declaring the same story or relationship ID are an error, not a reliable priority mechanism. Do not rely on resource iteration order. In particular, a built-in chronicle overlay can still attach to a full-story override of the same built-in ID.

The listener validates a candidate definition set before publishing it. Its custom prepare stage marks the entire candidate invalid on any narrative JSON parse/read failure, including syntax errors and a non-object root. Apply then leaves the **entire last valid narrative registry snapshot** untouched. Schema/semantic failures during apply likewise retain that whole snapshot. Both paths log `Narrative reload rejected`; check server logs rather than assuming `/reload` means your data was accepted. Test syntax and semantic failures separately and verify that unrelated valid files in the rejected candidate did not partially update. Revalidate previews after definition changes; reconnect/logout invalidates transient confirmations. Java objective registration is not refreshed by resource reload.

Client resource reload changes textures and language resources under `assets/`, not story requirements. A server datapack does not deliver a client resource pack. Restart/rebuild the host addon when changing Java type registrations or handlers; do not attempt to register them again on each `/reload`.

## Localization Without Gameplay Changes

All author-facing prose accepts literal text. It can also use `NarrativeText.translatable("example_pantheon.story.first_star.title")`, which produces the stable string marker `{{lang:example_pantheon.story.first_star.title}}`. Java definitions and saved snapshots keep that marker; chat uses translatable Components and the client presentation resolves the player's language. Do not translate to an English string on the server before storing or syncing it.

JSON text fields accept either a literal string or `{"translate":"example_pantheon.story.first_star.title"}`. Passage paragraph arrays may mix literal strings and translation objects. The example localizes the first story title, its call chapter title through `chapter_titles.call`, its call dialogue and its first call paragraph; everything else intentionally demonstrates literal text. `examples/connected-pantheon-addon/src/main/resources/assets/example_pantheon/lang/en_us.json` and the identical `examples/connected-pantheon-pack/assets/example_pantheon/lang/en_us.json` supply these four keys.

Add other language files to the same client asset namespace. Ship language assets in the addon or install the example directory as a client resource pack as well as a server datapack. Changing the value behind an existing key can update accepted stories' displayed wording after resource reload without rewriting their objective snapshots. Keep old keys available for old saves. A missing key may be displayed as the key; the marker carries no embedded fallback prose.

Localize titles, requests, names, deed descriptions, reward descriptions, premise, paragraphs, dialogue and relationship prose. **Never translate structural IDs, branch/passage keys, objective types, condition tokens, item IDs, blessing/reward IDs, targets or relationship ordering.** Translating "Craft a compass" does not change `craft_compass`, `minecraft:compass` or target `1`. Language resources describe the accepted quest; they cannot change its requirements or grant a reward.

## Rewards And Side-Only Effects

An ending records its specialization ID and grants the ordinary 25 favor once, subject to the existing favor cap. `BlessingSpecialization.builder(rewardId).blessing(existingBlessingId)` declares identity and display metadata, not executable tuning or a grant/equip operation. The example uses the real `divinity_engine:solar_ward` and `divinity_engine:twilight_veil` abilities but deliberately describes its custom markers as having no numeric effect. Reusing a built-in reward ID is not an acceptable shortcut to implementing an addon reward.

For an actual addon effect, check the saved reward by `DivineNarrativeManager.hasSpecialization(serverPlayer, rewardId)`, then the original blessing's equipped slot, contacted bound god, favor/eligibility and normal trigger/cooldown. `BlessingSpecializations.hasEligibleBlessing(player, boundGodId, blessingId)` is the current eligibility helper; Solar Ward is ordinarily bound to Solarius, not to Vesper merely because this story names it. The god/branch overload of `hasSpecialization` addresses the legacy primary story; prefer the reward-ID overload for multiple stories. A confirmed choice alone is not an earned reward.

Apply gameplay effects on the logical server only. Do not let a Codex render, client timer, chat click or translation grant attributes, favor or progress. Do not rewrite saved base blessing parameters to implement permanent tuning. If an addon adds transient modifiers or potion effects, use stable ownership IDs, remove only its own effects on ineligibility/logout and preserve unrelated potion sources. This example intentionally installs no such effects.

Ambient personal dialogue and notify sounds are player-targeted; shrine particles use the engine's bounded, nearby-visible Core adapter and need not be private to that player. Rendering and textures belong on the client; event registration and objective verification belong on the server. Do not import client-only classes in this common example or load chunks just to produce a greeting. A visual presence effect is never proof that a chapter or reward succeeded.

## Menus And Test Paths

Use a disposable test world with Vesper's host addon installed. Contact Vesper, reach 50 favor, open the statue's **Quests** tab and browse to either registered story. Use **Begin quest**, **Contribute requested items**, and **Claim ending and upgrade** at the appropriate chapters. **Speak about quest** opens the shrine conversation with both reward previews, explicit permanent confirmation, and any available paired epilogue. The Codex provides status and earned lore without performing remote shrine actions. No player commands or command-backed chat links are registered.

`QuestMenuAction` identifies the still-open statue container and namespaced story ID. The server checks shrine distance, god binding, contact and action eligibility. Permanent confirmation still uses an expiring server-generated preview token; do not fabricate one or grant progress from the UI. Missing/closed menus, foreign gods, stale previews and repeated claims are rejected. `NarrativeNetwork` protocol 5 carries these requests and an on-demand dialogue response, with no per-tick story-text resend.

The only story command is permission-level-two `/divinity_engine story reset <player> example_pantheon:vesper`, an administrator recovery tool that clears that god's narrative memories and related epilogues. Treat reset as destructive, including earned rewards; prefer fresh test players/worlds rather than experimenting on production saves.

Manual integration checks to run, **not results claimed here**:

1. Request both Vesper stories independently at favor 50; verify 49 fails. Confirm different branches, compare the two Codex journals and ensure no cross-god lock is added.
2. Complete each of the four branch playthroughs through offering, witness, explicit choice, donation, reckoning, fresh trial and return. Inspect all ten passages per definition, while each player sees only seven. Test stale tokens, distance, unloaded shrine and lost contact.
3. Craft a compass before reckoning, during it and after it; only the active matching chapter should advance. Test normal crafting, shift-click, separate crafts in one tick, duplicate invocation with the same event, inventory transfers, `/give`, renaming, creative/spectator mode, trial dimension/session and two players.
4. Interrupt observations with roof/day-night changes, logout and dimension travel. Completed chapters must remain complete. For any adapted cancelable handler, cancel at a later listener and change/reset the chapter before the END queue drains; no credit should escape.
5. Fail/retry the trial; reject victories from before the trial chapter. Repeat `finish`; no duplicate favor/reward. Try a second Vesper story already at TRIAL to verify documented shared trial-event behavior.
6. Contact only one related god, then both; verify that only the entry title/god-pair teaser is present, with no detailed relation/summary or epilogue in the journal payload. Complete both prerequisites without talking; relation/summary may now appear, but the epilogue must still be absent. Explicitly talk at either valid statue and test all four combinations with fresh profiles; a previously earned epilogue also permits the detailed context.
7. Accept a story, reload changed prose/targets, reconnect and check its accepted snapshot. Test a fresh acceptance, a legacy version-one save, removal/restoration of the addon and a missing custom type. Preserve original IDs and raw unresolved data.
8. Test Java-only defaults, equivalent pack overrides, identical-path pack priority, duplicate declared IDs, missing chronicle target, attempted story-ID/god rebinding and bad JSON separately. For each rejected candidate, verify the whole previous snapshot survives. Read reload logs. Remove the pack and confirm new requests use Java defaults.
9. Reload a client language value without changing server data. Check story title, custom chapter title, call paragraph and chat line in both clients with different languages, including an already accepted story; verify targets, conditions and rewards do not change. Check missing-key behavior and custom Java chapter-title snapshot round trips.
10. If you implement reward effects, unequip the blessing, lose favor/contact, leave its trigger condition and reconnect. Confirm no effect leaks to another player or survives ineligibility and that unrelated potion effects remain intact.

Reference paths for integration owners: `src/test/java/net/nightshade/divinity_engine/narrative/NarrativeStateMachineChecks.java`; `src/main/java/net/nightshade/divinity_engine/gametest/DivineNarrativeGameTests.java`; `TrialNarrativeGameTests.java`; `BlessingSpecializationGameTests.java`; and `docs/integration-validation.md`. Those suites are not evidence that this addon, its custom handler or the newly integrated UI has been played through. This contribution does not run Gradle or claim a Forge/gameplay test pass. Static example checks are recorded separately in the example's validation instructions/results.
