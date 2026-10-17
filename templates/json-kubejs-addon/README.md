# Aster: JSON + KubeJS Addon Starter

Create a small Divinity Engine addon without writing or compiling Java. This folder is a complete, editable distribution: it has no dependency on a parent repository, Gradle project, npm package, or build output.

The starter adds Aster, an automatically registered statue with a survival crafting recipe, Guiding Breath, a two-phase trial, and English display text. Guiding Breath restores **2 health points (one heart), only while its injured owner is in water**. A successful cast uses a **12-second cooldown**. The blessing requires **50 favor**. Dry land, full health, and a healing event canceled by another mod do not count as success.

A small per-instance NBT counter records successful uses without changing the healing amount. It is an authoring example, not a new visible meter or unlock system.

## Download And Requirements

[Download the standalone starter ZIP](https://github.com/KidGameZ1/divinity_engine/raw/refs/heads/main/downloads/divinity-json-kubejs-starter.zip)

Use a separate **Minecraft Java Edition 1.20.1 / Forge** instance first. Install a Divinity Engine build that includes the JSON content API and KubeJS bridge. Use the same Divinity Engine build on clients and server. This source project targets Forge **47.4.2**; follow the requirements of the actual Divinity Engine release you install.

Install the Forge builds of these optional scripting dependencies for this starter:

| Mod | Integration baseline |
| --- | --- |
| KubeJS | `2001.6.5-build.26` |
| Rhino | `2001.2.2-build.17` |
| Architectury API | `9.1.12` |

Use the mods' official distribution pages or your launcher to install dependencies, including any requirements of your Divinity Engine release. The starter contains no mod jars. A resource pack is not a replacement for these mods. KubeJS is optional for Divinity Engine generally, but **required for this starter's empty-hook scripted blessing to do anything**.

Java 17 runs this Forge/Minecraft generation. Node.js is optional and only used for the local checks below; it is not required by the game.

**Install only one Aster example.** This starter, the combined example, the JSON-only Aster example, and the startup-script Aster example intentionally reuse `example_pantheon:*` IDs. Do not install them together. In particular, do not add an Aster `DivinityEvents.register(...)` startup script on top of these JSON definitions.

## What To Put Where

The instance root is the directory containing `mods/`, `config/`, and usually `options.txt`. In a launcher, use its "Open Folder" or instance-directory action. It is not necessarily your system-wide `.minecraft` directory. A dedicated server root is the directory from which that server is launched.

This starter's layout is already ready to overlay an instance root:

```text
README.md
config/
  divinity_engine/content/example_pantheon/
    gods/aster.json
    blessings/guiding_breath.json
    trials/trial_of_shelter.json
kubejs/
  server_scripts/
    aster.js
    recipes.js
resourcepacks/
  aster-addon/
    pack.mcmeta
    assets/example_pantheon/lang/en_us.json
tests/
  starter.test.mjs
```

| Files | Singleplayer instance | Multiplayer client | Dedicated server |
| --- | --- | --- | --- |
| The three JSON definitions under `config/` | Required | Required, identical to server | Required, identical to clients |
| Both files in `kubejs/server_scripts/` | Required | Not executed for a remote server; install if also playing this addon locally | Required |
| `resourcepacks/aster-addon/` | Install and enable | Install and enable | Not needed for server logic |
| Compatible mods and scripting dependencies | Required | Use the same tested modpack baseline | Required |
| `README.md` and `tests/` | Optional authoring files | Not needed by Minecraft | Not needed by Minecraft |

The JSON files are **startup configuration**, not world datapacks. Do not put them under `saves/<world>/datapacks`, `world/datapacks`, or inside the resource pack. There is deliberately no `startup_scripts` directory in this starter.

## First Installation

1. Back up any existing instance/world. Create a disposable test instance and install the compatible mods first.
2. Fully close the client. Stop the dedicated server if using one; do not replace startup definitions while it is running.
3. Extract the starter somewhere convenient. Open the extracted folder until you can see `config`, `kubejs`, and `resourcepacks` together. Do not copy an extra outer archive folder into the instance.
4. Merge `config/` into the instance's existing `config/`. Preserve unrelated configuration. The final god path must be `INSTANCE/config/divinity_engine/content/example_pantheon/gods/aster.json`.
5. Merge `kubejs/` into the authoritative server's instance root. For singleplayer, that is the same client instance. The final script paths are `INSTANCE/kubejs/server_scripts/aster.js` and `INSTANCE/kubejs/server_scripts/recipes.js`.
6. Copy `resourcepacks/aster-addon/` into each client's `resourcepacks/`. Open Options > Resource Packs and enable the Aster pack. Its `pack.mcmeta` must be directly inside `aster-addon/`, not nested in another `resourcepack/` folder.
7. For a dedicated server, install the same three JSON files on every connecting client as well. A matching resource pack alone is insufficient.
8. Fully restart both sides after installing the definitions. Resolve startup errors before opening an existing save.

No compiler, npm install, source checkout, or archive mounting is needed. Minecraft reads these folders directly. Do not put the complete starter ZIP into `mods/` or `resourcepacks/`; it is an installer layout, not a mod jar or a single resource pack.

## Try It In A Disposable World

1. Craft Aster's statue in a crafting table using one amethyst shard and four stone, following the pattern below. No cheats or operator permissions are needed. For a separate Creative test world, find Statue of Aster in Divinity Engine's `Statue's Tab` creative tab instead.
2. Place the statue and interact to contact Aster. Interact again to open its blessings menu. This starter supplies a recipe, not a generated shrine structure.
3. Earn the required favor using the normal shrine system. Aster accepts amethyst shards and bread. To pray, stay near the contacted statue and use Minecraft's Sneak control (normally Left Shift); this must be the nearest statue. There is no separate prayer key. Dropped accepted items in front of the statue use the shared offering system and budgets.
4. Equip Guiding Breath in a blessing slot once eligible. Use that slot's configured key; check Minecraft's Controls screen if another mod uses the same key.
5. On dry land while injured, press the key: no healing and no success cooldown should occur. In water while injured, press it: health rises by up to two points and the normal cooldown starts.
6. During the cooldown, repeated presses must not heal again. At full health in water, a ready press must not consume a success cooldown. Partial healing, such as recovering the last one health point, still counts as one successful use.
7. Open Aster's statue menu to inspect trial access. Test the trial in a disposable world: phase one defeats three zombies within 600 ticks, and phase two survives two skeletons for 400 ticks. At normal server speed those durations are 30 and 20 seconds.

The trial uses Divinity's normal session, arena, protection, return, and cleanup logic. The starter does not create its own arena session or client-only combat logic.

### Survival Statue Recipe

Use a 3-by-3 crafting table. `A` is one amethyst shard, `S` is stone (not cobblestone), and `.` is an empty slot:

```text
. A .
. S .
S S S
```

The output is one `example_pantheon:aster_statue`. If necessary, smelt cobblestone into stone first. You can place ingredients manually; this starter does not add a recipe-book unlock advancement. The script `kubejs/server_scripts/recipes.js` adds recipe ID `example_pantheon:crafting/aster_statue` through `ServerEvents.recipes`. The god JSON registers the item earlier at startup; a recipe does not register a new item or god.

For a quick developer-only blessing test, you may change `favor` to `0` on **both** test installations and restart both. Contact and equipping are still required. Restore your intended favor requirement before distributing the addon and update the pinned test expectation if you intentionally keep a different value.

## Understand The Files

### JSON Defines The Content

`gods/aster.json` supplies domains, the blessing list, accepted offerings, prayer favor, and a color. The god creates `example_pantheon:aster_statue` as a block, item, and block entity automatically.

`blessings/guiding_breath.json` defines eligibility, mode, cooldown, and the successful-cast particle texture. `"hooks": {}` is intentional: it leaves the entire custom water condition and healing decision to JavaScript. The particle path is a built-in Minecraft texture; no custom image is needed.

`trials/trial_of_shelter.json` references Aster and defines two supported phases. Trial fields do not create JavaScript god/trial lifecycle callbacks. This starter includes no story datapack.

### The Server Script Performs The Conditional Heal

Read `kubejs/server_scripts/aster.js` in a plain-text editor:

1. Subscribe to the exact blessing ID, and ignore hooks other than `pressed`.
2. Check that the owner is in water and below maximum health. The owner is a living entity; the script does not assume it is always a player.
3. Request healing through `owner.heal(2)`, then compare actual health before and after. Other mods can cancel or modify the heal.
4. Call `event.markSuccessful()` only after health actually increases. Returning `true` is not a substitute. The normal blessing caller starts the configured cooldown, and successful content behavior emits its configured visual.
5. Increment the instance's bounded successful-use counter and call `instance.markDirty()`.

Do not manually call `startCooldown`, `setCooldown`, `onPressed`, or `onTick` from this callback. Do not retain `event` for a scheduled or asynchronous callback. Engine eligibility, cooldown, and reentry guards apply before normal script dispatch.

### Per-Instance State

The NBT key is `example_pantheon:guiding_breath_successes`. It counts successful script heals, not button presses or health points. Failed casts do not create/update the tag. Values saturate at 1,000,000,000 instead of overflowing a Java integer.

`event.getInstance().getOrCreateTag()` accesses that particular blessing instance's custom data. It is included in normal blessing serialization, unlike a file-level `let uses = 0`, which would share state between callers and reset with script loading. `markDirty()` marks an in-place tag change for the normal save/sync path. This is not an immediate forced disk save or a guarantee across resets that replace the instance.

The counter has no visible UI in this starter. It is useful as a basis for your own per-instance milestones or debugging. Keep its key stable if you later rely on existing counts, and use measured gameplay success rather than incrementing before the water/full-health checks.

## Editing Without Surprises

| Change | Edit | Apply |
| --- | --- | --- |
| Healing amount or condition | `kubejs/server_scripts/aster.js` | Restart server; singleplayer restarts its instance. Then test again. |
| Statue recipe ingredients or pattern | `kubejs/server_scripts/recipes.js` | Server `/reload` with permission, or restart; then check the crafting grid. |
| Favor or cooldown | Blessing JSON | Install matching JSON and restart client and server. |
| Accepted offerings or domains | God JSON | Install matching JSON and restart client and server. |
| Trial mobs, timings, reward, messages | Trial JSON | Install matching JSON and restart client and server. |
| Display names/descriptions | Resource-pack `lang/en_us.json` | Reload client resources or restart client. |
| New content IDs or hook/mode changes | JSON and every referring file | Treat as a startup-content change; restart both sides and test on a backup. |

Normal KubeJS server-script reload replaces listeners and may be useful during development. It does **not** re-register content or change JSON definitions. A full restart is the simplest beginner workflow. The content handshake compares startup definitions, not server JavaScript source or resource-pack text, so keep the correct server script version with your distribution yourself.

### JSON Action Order Matters

For a hook, Divinity executes its JSON action list in order, then invokes the KubeJS callback. Success from earlier actions remains successful even if the script immediately returns. A script cannot undo an unconditional JSON heal by checking water afterward.

Keep this starter's `hooks` empty while JavaScript gates the whole ability. If you intentionally want unconditional actions followed by an optional script bonus, add JSON actions knowingly and test the combined outcome. Two heal actions are two healing attempts, not two separate cooldowns.

### Names Are Not IDs

The path `config/divinity_engine/content/example_pantheon/gods/aster.json` defines `example_pantheon:aster`. Changing the namespace folder or file name changes the registered identity. Editing the language value `"Aster"` changes only the display name and is safer for an established world.

Before first release, choose your own namespace. Update the config namespace folder, every ID reference in god/trial JSON, the server subscription, recipe output and recipe ID, the resource-pack asset namespace and translation keys, the counter key if desired, and test expectations together. Use lowercase names such as `harbor_pack`; do not use `minecraft`, `forge`, or `divinity_engine` for your new definitions.

After a world uses the addon, keep god, blessing, trial, and derived statue IDs stable. Do not uninstall definitions and then open that save. Back up saves before upgrades and test migrations separately. Also avoid renaming a `.json` file to make a display name prettier.

## Optional Local Checks

With Node.js 22 or newer, open a terminal in the extracted starter folder:

```sh
node --check kubejs/server_scripts/aster.js
node --check kubejs/server_scripts/recipes.js
node --test tests/starter.test.mjs
```

No `npm install` is needed. The tests use only Node's standard library and files inside this starter. File paths are resolved relative to the test file, so the suite also works when invoked by its absolute path from another directory.

The checks parse all shipped JSON/pack metadata, confirm matching IDs and client text (including contact/loss messages), and execute the actual server scripts against small method-level test doubles. Cases include dry/full/canceled/partial healing, wrong hooks, independent instance counters, count bounds, fresh script evaluation, and the exact single statue recipe with its stable ID and ingredient counts.

These checks are **not** Forge, Rhino, networking, rendering, real save/load, or real KubeJS reload tests. They do not simulate engine cooldown scheduling or prove that a modded healing event behaves like the double. They pin the shipped starter's expected values; update expectations when intentionally changing the addon. Always perform the in-game checks above too.

## Package Your Addon

1. Work from a clean copy of this starter, not your entire game instance.
2. Keep the top-level `config/`, `kubejs/`, and `resourcepacks/` folders. Retain the included `LICENSE`, include this README, and optionally include `tests/` so recipients can understand and check your source. The starter's MIT license covers its example source and documentation; Minecraft, Divinity Engine, and separately obtained dependencies retain their own licenses.
3. If creating an addon ZIP for distribution, put those folders at its root. Do not add an extra `json-kubejs-addon/` level above them. Tell recipients to extract/merge it into their instance, not install the complete archive as a resource pack.
4. To distribute the resource pack separately, archive the **contents** of `resourcepacks/aster-addon/`. In that separate ZIP, `pack.mcmeta` and `assets/` must be at the root. Clients then install that resource-pack ZIP normally.
5. List the game, loader, Divinity Engine, KubeJS, Rhino, and Architectury versions you actually tested. Tell dedicated-server owners that JSON must match clients, while server scripts run on the server.
6. Exclude saves, world files, logs, caches, launcher metadata, unrelated configs, credentials, and dependency jars. The starter needs none of them. Test the packaged layout in a second clean instance before sharing it.

## Troubleshooting

| Symptom | Check |
| --- | --- |
| Unknown `example_pantheon:aster_statue` item | Check JSON path, correct instance folder, full restart, and startup logs. The resource pack alone cannot register content. |
| Statue exists but cannot be crafted | Install `recipes.js` on the authoritative server, inspect script errors, and use stone rather than cobblestone in the exact 3-by-3 pattern. |
| Duplicate content ID at startup | Install only one Aster example. Look for both JSON and startup-script declarations, or renamed backup JSON files still inside the content tree. |
| `DivinityEvents` is undefined or scripts fail to load | Check KubeJS/dependency versions, the compatible Divinity build, script location, and KubeJS server logs. |
| Guiding Breath does nothing | Check contact, favor, equipped slot/key, cooldown, water, health, and script errors. Empty JSON hooks require the callback. |
| Healing occurs on dry land | Check for unconditional JSON heal actions or another callback installed for the same ID. |
| Healing happens twice | Check for duplicate server scripts/listeners and JSON-plus-script action stacking. Keep one starter callback. |
| Raw translation keys appear | Enable the client resource pack and verify `pack.mcmeta` is directly inside its folder. |
| Server rejects connection after an edit | Synchronize all startup definition files on both sides and restart both. Reinstalling translations alone cannot fix a definition mismatch. |
| Node checks fail after intentional customization | Update pinned IDs, values, and expected behavior together; do not assume a passing double replaces game testing. |

Inspect `logs/latest.log` and `kubejs/logs/server.log` when available. Resolve the first relevant error rather than adding a second registration to mask missing content. Keep backups outside the scanned `config/divinity_engine/content/` directory.

For a deeper explanation, read the [JSON + KubeJS tutorial](https://github.com/KidGameZ1/divinity_engine/blob/main/docs/wiki/JSON-KubeJS-Tutorial.md). This README remains sufficient to install, edit, check, and package the standalone files without that repository.

See [Addon Templates](https://github.com/KidGameZ1/divinity_engine/blob/main/docs/wiki/Addon-Templates.md) for download choices, or the separate [Java Addon Tutorial](https://github.com/KidGameZ1/divinity_engine/blob/main/docs/wiki/Java-Addon-Tutorial.md) if you prefer a compiled addon.
