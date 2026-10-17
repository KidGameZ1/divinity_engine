# Build An Addon With JSON And KubeJS

This tutorial builds **Aster**, a small playable Divinity Engine addon for **Minecraft Java Edition 1.20.1 on Forge**. You do not need a Java compiler, a source checkout, Gradle, or npm. Edit ordinary text files, install them into an instance, and restart when changing startup content.

[Download the JSON + KubeJS starter](https://github.com/KidGameZ1/divinity_engine/raw/refs/heads/main/downloads/divinity-json-kubejs-starter.zip). The archive is an instance-folder overlay, not a mod jar or a single resource pack. Its [included README](../../templates/json-kubejs-addon/README.md) also works independently of this wiki.

For the other downloadable option, see [Addon Templates](Addon-Templates.md) and the separate [Java Addon Tutorial](Java-Addon-Tutorial.md). The Java starter uses `haven_addon`; this tutorial uses `example_pantheon`.

## What You Will Make

| Feature | Shipped behavior |
| --- | --- |
| God | Aster, with Shelter and Wayfinding domains |
| Statue | Automatically registered block/item, with a survival crafting recipe |
| Blessing | Guiding Breath, active, requires 50 favor |
| Ability | Heal up to 2 health points, one heart, only while injured and in water |
| Cooldown | 12 seconds after a successful heal; dry/full-health attempts do not succeed |
| State example | A bounded successful-heal counter stored on each blessing instance |
| Trial | Defeat three zombies, then survive two skeletons for twenty seconds |
| Client text | God, blessing, statue, description, contact, and loss translations |

The counter is an authoring example, not a visible meter, bonus heal, or new unlock mechanic. The trial uses the existing trial session system rather than creating a second arena controller.

## Three Layers, Three Jobs

**Startup JSON creates identities and rules.** God, blessing, and trial definitions live below `config/divinity_engine/content/`. They establish registry IDs, favor requirements, cooldowns, offerings, and trial phases. The game reads them during mod construction, before Forge registries freeze.

**Server JavaScript adds behavior.** `aster.js` listens for one existing blessing ID. It checks water and health, heals, then reports actual success. `recipes.js` supplies a normal crafting-table recipe for the already registered statue. Neither script declares a second Aster god.

**A client resource pack supplies presentation.** The pack contains language entries, including Aster's contact/loss messages. Installing it cannot create the underlying god or heal a player.

Keep these jobs separate. In particular, a conditional script cannot reverse an unconditional JSON action that already ran.

## Prepare A Test Instance

Use a disposable world and instance before changing a valued save. Locate the launcher's actual instance folder using its folder-opening action; it is the directory with `mods/` and `config/`, not necessarily the default `.minecraft` folder. A dedicated server's instance root is its launch directory.

Install a Divinity Engine build that includes the JSON content API and KubeJS integration, along with its required dependencies. Use matching Divinity Engine builds on server and clients. The source baseline targets Forge **47.4.2**, Java **17**, and these Forge scripting dependencies:

| Dependency | Integration baseline |
| --- | --- |
| KubeJS | `2001.6.5-build.26` |
| Rhino | `2001.2.2-build.17` |
| Architectury API | `9.1.12` |

Use the actual release's dependency requirements if they differ. Obtain mods through their official distribution channels or your launcher; the starter does not bundle dependency binaries. KubeJS is optional for Divinity Engine generally, but required for this starter's scripted healing and recipe.

**Install only one Aster example.** The combined example, JSON-only example, startup-script example, and this starter reuse Aster's IDs intentionally. They are alternatives, not cumulative layers. Do not copy an Aster `DivinityEvents.register(...)` startup declaration into this starter. If you later prefer script-based registration, follow [KubeJS](KubeJS.md) as a separate setup rather than declaring the same IDs twice.

## Install The Overlay

Extract the archive until the following files are visible. Merge its three top-level game folders into the instance without deleting unrelated files:

```text
INSTANCE/
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
```

The archive also includes `README.md`, `LICENSE`, and `tests/starter.test.mjs`. Those are authoring/distribution files, not files Minecraft executes.

| Destination | JSON definitions | Server scripts | Resource pack |
| --- | --- | --- | --- |
| Singleplayer instance | Install all three | Install both | Install and enable |
| Dedicated server | Install all three | Install both | Not needed for server logic |
| Connecting client | Install matching definitions | Not executed for a remote server; include for local worlds using this addon | Install and enable |

Use the same tested modpack dependencies on both sides. Stop the client/server before first installation. Fully restart both after installing or changing definitions. On each client, enable Aster in Options > Resource Packs.

Do not put the complete starter archive into `mods/` or `resourcepacks/`. Do not place the definitions in world `datapacks/`: these definitions are startup configuration. A final path such as `INSTANCE/json-kubejs-addon/config/...` is also wrong; `config/` must be directly under the instance root.

## Play Without Admin Commands

Craft the statue in a 3-by-3 crafting table using **one amethyst shard and four stone**. Use stone, not cobblestone; smelting cobblestone supplies it. In this diagram, dots are empty slots:

```text
. A .     A = amethyst shard
. S .     S = stone
S S S     Output: one Statue of Aster
```

For a separate Creative test world, find Statue of Aster in Divinity Engine's **Statue's Tab** creative tab. This is an alternative way to inspect it, not a survival requirement.

1. Place the statue and interact to contact Aster. Interact again to open the god menu. Confirm the contact text and display names are readable.
2. Earn favor through normal play. Aster accepts dropped amethyst shards and bread in front of the statue. The normal offering processor owns consumption and favor budgets.
3. Pray by using Minecraft's **Sneak** control, normally Left Shift, while near the contacted statue. It must be the nearest statue. There is no separate prayer key; the server checks the player's crouching state.
4. Once eligible at 50 favor, equip Guiding Breath into a blessing slot. Use that slot's configured activation key, found in Minecraft Controls. Do not confuse blessing activation with sneaking to pray.
5. Compare an injured attempt on dry land with one in water. Only the water attempt should heal, by up to one heart, and start the normal 12-second cooldown.
6. Open Aster's menu to inspect trial access when eligible. Use a disposable world to test both phases and the normal return/cleanup flow.

Prayer and offerings use shared caps and timing rules. Their JSON values are not a promise of unlimited favor per tick or per dropped stack. The starter supplies a statue recipe, not a generated shrine structure.

## IDs: The Foundation

The content loader derives an ID from its path, not from a `name` or `id` property inside the JSON:

```text
config/divinity_engine/content/example_pantheon/gods/aster.json
                               namespace       kind  path

God ID:       example_pantheon:aster
Statue ID:    example_pantheon:aster_statue
Blessing ID:  example_pantheon:guiding_breath
Trial ID:     example_pantheon:trial_of_shelter
Recipe ID:    example_pantheon:crafting/aster_statue
```

Recipe IDs and item IDs belong to different registries. Here the recipe's output is the statue item, but the recipe has its own explicit ID.

Choose a lowercase namespace for your own addon before release, such as `harbor_pack`. Do not declare new content in `minecraft`, `forge`, or `divinity_engine`. References must use explicit `namespace:path` IDs. Nested filenames can create paths such as `harbor_pack:water/guiding_breath`; keep the initial starter flat until you need that organization.

Renaming a JSON file changes identity. Changing a language value changes display text only. For an existing world, prefer a display-name edit and keep saved registry IDs stable. Removing definitions and opening that save is not an uninstall/migration strategy; back up and test any migration separately.

## Define Aster

The complete god file is:

```json
{
  "schema_version": 1,
  "domains": ["Shelter", "Wayfinding"],
  "blessings": ["example_pantheon:guiding_breath"],
  "offerings": {
    "minecraft:amethyst_shard": 2,
    "minecraft:bread": 1
  },
  "prayer_favor": 1,
  "color": "#B5D5EF"
}
```

| Field | Meaning and limits |
| --- | --- |
| `schema_version` | Version 1; omitted defaults to 1 |
| `domains` | Required array; up to 16 domain names, each 1..64 characters |
| `blessings` | Required array; up to 64 distinct, registered blessing IDs |
| `offerings` | Optional item-ID map; up to 64 entries with integer values 1..12 |
| `prayer_favor` | Integer 0..2, default 1; still subject to shared favor rules |
| `color` | Six-digit `#RRGGBB`, default `#C5D9EB` |

God registration supplies the associated statue block, item, and block entity automatically. You do not need a separate block registration script, custom model file, or duplicate statue definition. The starter's additional crafting script makes that registered item obtainable in survival.

Unknown fields are errors, not ignored extension points. Do not add an arbitrary `description`, `onPrayer`, or `statueRecipe` field here. Descriptions belong in translations; the recipe belongs in the recipe script. See [JSON Content](JSON-Content.md) for the API reference.

## Define The Blessing

The complete blessing file is:

```json
{
  "schema_version": 1,
  "favor": 50,
  "cooldown_seconds": 12,
  "mode": "active",
  "color": "#B5D5EF",
  "particle_texture": "minecraft:textures/particle/glint.png",
  "hooks": {}
}
```

| Field | Meaning and limits |
| --- | --- |
| `favor` | Required favor threshold, integer -250..250; default 0 |
| `cooldown_seconds` | Integer 0..3600; default 10 |
| `mode` | `active`, `passive`, or `toggle`; default `active` |
| `tick_interval` | Integer 5..1200 ticks; default 20, relevant to passive/toggled ticking |
| `hooks` | Object mapping reachable hook names to action arrays; default empty |
| `color` | Six-digit `#RRGGBB`; same default as gods |
| `particle_texture` | Optional full `namespace:textures/particle/name.png` resource path |

Empty `hooks` is deliberate and valid. The script callback still runs for an eligible hook even when there are no JSON actions. This lets JavaScript own the entire water condition.

The supplied texture is an existing vanilla particle sprite. The current content visual emits six particles for twenty ticks after success. Omitting `particle_texture` disables that definition's configured success pulse; it does not disable the ability.

### Match Hooks To Mode

| Mode | Reachable hooks |
| --- | --- |
| `active` | `pressed` |
| `passive` | `tick`, `damage_entity`, `taken_damage`, `kill_entity` |
| `toggle` | `toggle_on`, `toggle_off`, `tick`, `damage_entity`, `taken_damage`, `kill_entity` |

An active blessing does not tick. A toggle blessing uses toggle transitions rather than `pressed`, and its ongoing tick/combat effects require the toggled-on state. The schema rejects unreachable JSON hook/mode combinations, including empty arrays under an incompatible hook. Changing the script's hook check alone cannot add a new engine trigger.

Tick interval is a server-tick filter with entity staggering, not a wall-clock timer. The shared tick dispatcher and cooldown also constrain execution; do not promise an exact real-time callback every N milliseconds. Keep periodic behavior bounded and test it under the actual dispatcher.

## Implement The Conditional Heal

This is the starter's actual `kubejs/server_scripts/aster.js`:

```js
DivinityEvents.blessing('example_pantheon:guiding_breath', event => {
  if (event.getHook() !== 'pressed') return
  const owner = event.getOwner()
  if (!owner.isInWater()) return

  const before = owner.getHealth()
  if (before >= owner.getMaxHealth()) return
  owner.heal(2)
  if (owner.getHealth() <= before) return
  event.markSuccessful()

  const instance = event.getInstance()
  if (instance !== null) {
    const tag = instance.getOrCreateTag()
    const key = 'example_pantheon:guiding_breath_successes'
    tag.putInt(key, Math.min(1000000000, Math.max(0, tag.getInt(key)) + 1))
    instance.markDirty()
  }
})
```

The subscription filters by exact blessing ID. The hook check distinguishes the behavior even if you later extend the definition. The owner is a `LivingEntity`, not necessarily a player, so this example avoids player-only inventory or chat calls.

The important boundary is **measured success**. Calling `heal(2)` is only a request: another mod may cancel or reduce it, and the health cap may limit the result. The script compares health after the request, then marks success only if health rose. Healing the last one health point still succeeds once.

Returning `true` does not mark success. `event.markSuccessful()` informs the normal blessing call path, which owns cooldown handling. Do not also call `startCooldown`, force a second activation, or invoke the blessing hook recursively from the script. Normal eligibility, equipped state, and cooldown checks precede dispatch; a reentry guard prevents nested content execution during these callbacks.

This is server behavior. Do not put this listener in `client_scripts`, and do not store the event for asynchronous or scheduled use. Mark success during the current synchronous callback. The bounded JSON schema is not a security sandbox for arbitrary JavaScript; distribute scripts only from trusted authors.

### The Callback Context

| Method | What it supplies |
| --- | --- |
| `getId()` | The current blessing's `ResourceLocation`; use `.toString()` when you need its text |
| `getHook()` | The hook name as a string |
| `getOwner()` | The living entity using the blessing |
| `getTarget()` | A living target or `null`, depending on the hook |
| `getInstance()` | The blessing instance; nullable in the generic context contract |
| `getLevel()` | The authoritative `ServerLevel` |
| `getNativeEvent()` | The underlying combat event when present, otherwise `null` |
| `isSuccessful()` | Whether an earlier action or callback has reported success |
| `markSuccessful()` | Marks this current dispatch successful; not an extra activation |

Press, tick, and toggle callbacks do not provide a selected/aimed target automatically. `damage_entity` supplies the hurt entity; `taken_damage` supplies a living attacker when the damage source has one; environmental damage may have none. A killed entity is generally no longer alive, so do not design a JSON `target` action after a kill assuming a living recipient.

The bridge is not a general callback for every built-in Java blessing or arbitrary god lifecycle event. It dispatches behavior for content-defined blessings. Handle a native event only when you understand its exact Forge event type and cancellation contract; reporting blessing success is not the same as canceling damage.

## Store State On The Instance

The example records `example_pantheon:guiding_breath_successes` only after an actual heal. This is useful as a starting point for per-instance milestones or debugging without adding gameplay effects to the starter.

`getOrCreateTag()` returns the instance's custom NBT. Existing tags are mutated in place, so call `markDirty()` after `putInt`. The normal blessing serialization includes that tag, and the ordinary save/sync path handles dirty state. This is not an immediate forced disk write.

The count is bounded to 1,000,000,000 to avoid Java integer overflow. A negative stored count is treated as zero before incrementing. Failed attempts do not allocate or alter state. If a generic callback lacks an instance, the actual heal can still count as successful without bookkeeping.

Do not use a file-level `let successes = 0` as player progression: it would share a counter across callbacks and reset with script loading. Instance state instead belongs to that specific blessing instance. A system that discards/replaces the instance can also discard its progress; this example does not promise permanent account-wide progression across resets.

If changing the counter key after release, decide how existing values should migrate first. A new key naturally starts a new count. The starter does not expose the count as a HUD or create a migration automatically.

## Combine JSON Actions Deliberately

The execution order is:

```text
Normal eligibility/cooldown checks
  -> actions in this hook's JSON array, in order
  -> matching server-script callbacks
  -> accumulated success result and configured visual
  -> normal caller's cooldown handling
```

Success accumulates; it is not a transaction that rolls back on an early return. If JSON already healed the owner, the water check in JavaScript comes too late to prevent it. Keep Aster's `hooks` empty for the shipped water-only behavior.

For a different design, the following is a valid replacement blessing definition that intentionally heals unconditionally on press and then grants a speed effect. It is **not** the water-gated starter. Disable/change the Aster heal callback if adopting this replacement, or both healing paths can run. Update the description and tests with the new mechanic.

```json
{
  "schema_version": 1,
  "favor": 50,
  "cooldown_seconds": 12,
  "mode": "active",
  "hooks": {
    "pressed": [
      { "type": "heal", "target": "self", "amount": 2 },
      { "type": "effect", "target": "self", "effect": "minecraft:speed", "ticks": 60, "amplifier": 0 }
    ]
  }
}
```

There may be up to 16 actions per hook. Unknown fields are rejected per action type; for example, `amount` is not an effect-duration field.

| Action | Fields and limits | Actual result |
| --- | --- | --- |
| `heal` | `amount` 0.01..40, default 1 | Uses normal healing; success requires increased health |
| `effect` | Required registered `effect` ID; `ticks` 1..12000, default 60; `amplifier` 0..4, default 0 | Uses normal effect application; amplifier zero means level I |
| `ignite` | `ticks` 1..12000, default 60 | Rounds duration up to whole seconds; fire-immune targets do not succeed |
| `damage` | `amount` 0.01..40, default 1 | Normal attributed damage; refuses owner and allied recipients |
| `dash` | `amount` 0.01..3, default 1; only `self` | Replaces horizontal velocity along horizontal look direction, preserves vertical velocity; not teleportation |

All actions accept `target`, defaulting to `self`. `target` means the hook's actual non-null, living, same-level target, not a raycast. `nearby_hostiles` selects at most sixteen nearest visible, living hostile enemies within `radius` (0.5..12, default 4), excluding players, allies, and tame pets. Radius is relevant to that area selector. A dash cannot use an area or other target.

These selectors are deliberately bounded. A press with `target` and no provided target is a no-op; use a suitable self/area design instead of assuming the engine selected whatever the player looked at.

## Add The Survival Recipe

The complete `kubejs/server_scripts/recipes.js` is:

```js
ServerEvents.recipes(event => {
  event.shaped('example_pantheon:aster_statue', [
    ' A ',
    ' S ',
    'SSS'
  ], {
    A: 'minecraft:amethyst_shard',
    S: 'minecraft:stone'
  }).id('example_pantheon:crafting/aster_statue')
})
```

Spaces mean empty slots. Each letter maps to one item per occupied slot, giving one shard and four stone. The output string means one statue. The explicit recipe ID makes later identification/replacement predictable; it does not register the output item.

KubeJS recipes belong in `server_scripts` and are rebuilt through `ServerEvents.recipes`; the official [recipe tutorial](https://kubejs.com/wiki/tutorials/recipes) documents shaped arguments and server reloads. The KubeJS 2001 branch's [RecipeJS implementation](https://github.com/KubeJS-Mods/KubeJS/blob/2001/common/src/main/java/dev/latvian/mods/kubejs/recipe/RecipeJS.java) provides the chained `.id(...)` method.

This script adds no recipe-book unlock advancement. Place the ingredients manually when testing rather than treating absence from the recipe book as proof the recipe is missing. For multiplayer, the authoritative server must load the recipe; installing it only on a connecting client does not add it to that server.

## Define The Trial

`trials/trial_of_shelter.json` contains:

```json
{
  "schema_version": 1,
  "god": "example_pantheon:aster",
  "reward_favor": 25,
  "start_message": "Aster: Make a road where fear has closed the gate.",
  "complete_message": "Aster: You have become a shelter for others.",
  "phases": [
    {
      "name": "Keep the Watch",
      "objective": "Defeat the sentries",
      "duration_ticks": 600,
      "goal": "defeat_all",
      "spawns": [{ "entity": "minecraft:zombie", "count": 3 }]
    },
    {
      "name": "Hold the Refuge",
      "objective": "Survive for twenty seconds",
      "duration_ticks": 400,
      "goal": "survive",
      "spawns": [{ "entity": "minecraft:skeleton", "count": 2 }]
    }
  ]
}
```

At normal server speed, 600 ticks is thirty seconds and 400 ticks is twenty seconds. `defeat_all` requires defeating its spawned enemies before time runs out; `survive` requires lasting the phase duration. Neither goal is a script callback name.

| Field/group | Limits |
| --- | --- |
| `god` | Required registered god ID; cannot replace a trial already assigned to a god |
| `reward_favor` | Integer 0..50, default 25 |
| Messages | Optional literal text, 1..512 characters when supplied |
| `phases` | Required array, 1..8 phases |
| Phase `name`, `objective` | Required literal text, 1..512 characters |
| `duration_ticks` | Integer 20..12000, default 600 |
| `goal` | `defeat_all` (default) or `survive` |
| `spawns` | Up to eight groups; `entity` ID and integer `count` 1..16 per group, default 1 |
| Phase total | At most 32 mobs; `defeat_all` needs at least one, `survive` permits zero |

Spawns must resolve to summonable non-boss monster types. Wither, ender dragon, and warden are excluded. Arbitrary entity NBT is not a supported spawn field. Messages and phase labels here are literal JSON text, unlike the names in the client language file.

Divinity's normal trial lifecycle owns arena placement, player transport, completion, failure, attribution, and cleanup. You are defining phases within it, not implementing a world-arena session in JavaScript. Try success and failure on disposable saves before distributing a harder trial.

## Supply Client Text

The pack uses `pack_format: 15`, the resource-pack format for this Minecraft generation. This is a resource pack, not a data pack. Minecraft's official [1.20 release notes](https://feedback.minecraft.net/hc/en-us/articles/16499677456781-Minecraft-Java-Edition-1-20-Trails-Tales) document the resource-pack version.

```json
{
  "pack": {
    "pack_format": 15,
    "description": "Aster JSON + KubeJS starter names and descriptions"
  }
}
```

The `pack.mcmeta` above must be directly inside `resourcepacks/aster-addon/`. The language file belongs at `assets/example_pantheon/lang/en_us.json` inside that pack:

```json
{
  "example_pantheon.gods.aster": "Aster",
  "example_pantheon.gods.aster.contact": "Aster: Even a lost traveler deserves a guiding light.",
  "example_pantheon.gods.aster.loss": "Aster: The road remains, should you return.",
  "example_pantheon.blessings.guiding_breath": "Guiding Breath",
  "example_pantheon.blessings.guiding_breath.description": "While in water, restore 2 health points. Does nothing at full health.",
  "block.example_pantheon.aster_statue": "Statue of Aster"
}
```

The god/blessing name keys are not prefixed with `god.` or `blessing.`. Automatic statue names currently combine the translated god name with `block.divinity_engine.named_statue`, producing "Aster Statue" in English. The sample's conventional `block.example_pantheon.aster_statue` key alone does not override that composed name; see [Assets and Localization](Assets-and-Localization.md). Keep `.contact` and `.loss` entries so those interactions do not display raw keys. If an ID path contains slashes, the god/blessing translation path uses dots instead.

To rename Aster without changing saved identity, edit the text values on the right. To support another language, add its language file with the same keys. Keep the ability description synchronized with actual health points and conditions when editing the script.

## Reloads, Restarts, And Multiplayer

| Change | Required action |
| --- | --- |
| God/blessing/trial JSON or new registry IDs | Update matching files on clients/server, then fully restart both |
| `aster.js` callback | Restart authoritative server, or use normal KubeJS server-script reload during development and retest |
| `recipes.js` | Server `/reload` with permission, or a restart; retest crafting |
| Language pack | Reload client resources or restart; ensure the pack is enabled |

A full restart is the simplest beginner workflow. Server script reload replaces listeners, but it does not recreate registries. Startup declarations close at the first Forge `RegisterEvent`; common setup validates references and initializes trials. A server-script reload cannot add a god after that boundary.

The content handshake fingerprints startup definitions. Canonicalization sorts object keys recursively while preserving array order. Whitespace/object-key order alone does not change the fingerprint; action order does matter. Do not assume all semantically similar edits hash identically: omitting a default versus explicitly writing it can change the manifest. Distribute identical definition files rather than relying on normalization guesses.

The fingerprint is not a checksum of server scripts, recipes, or resource-pack text. A connection succeeding does not prove those other files match your intended release. Record their versions and test server behavior separately.

Keep backups outside `config/divinity_engine/content/`. A copied file still ending in `.json` inside that tree is another definition, not an ignored backup. The loader limits content to 512 definitions and JSON files to 256 KiB, and rejects unknown fields, duplicate content, invalid limits, and unresolved registry references.

## Customize In Small Steps

For a first edit, change the heal amount in `aster.js`, the description in `en_us.json`, and the expected amount in the test together. Leave the ID stable. Then test both partial healing and the failed dry/full-health cases again; changing the amount should not change success semantics.

Before releasing your own namespace, change all related identities together:

1. Rename the namespace directory under `config/divinity_engine/content/`.
2. Update the god's blessing reference and the trial's god reference.
3. Update `DivinityEvents.blessing(...)` to the new blessing ID.
4. Update the recipe's output statue ID and explicit recipe ID.
5. Rename the resource-pack asset namespace and all matching translation keys, including contact/loss and the statue key.
6. Choose the per-instance counter key, update tests, and test the new identity in a clean world before first release.

To add another blessing, create a new blessing JSON with a distinct filename and add its ID to the god's `blessings` array. Add a callback only if it needs script behavior, plus name/description translations. Use the mode/hook table rather than copying `pressed` into a passive or toggle definition. Client/server definition changes still require a restart.

## Run The Optional Local Checks

Install Node.js 22 or newer only if you want these authoring checks. It is not a Minecraft dependency. In the extracted starter directory, run:

```sh
node --check kubejs/server_scripts/aster.js
node --check kubejs/server_scripts/recipes.js
node --test tests/starter.test.mjs
```

There is no `npm install`. The test uses only Node's standard library and starter-local files. Its paths are based on the test file, so it also works when invoked by absolute path from another directory.

The 21 tests parse all shipped JSON/pack metadata, check ID links and contact/loss text, and execute the actual script sources against small method-level doubles. They cover dry/full/canceled/partial healing, non-pressed hooks, one success per successful callback, independent instance counters, bounds, existing state, fresh evaluation, and the exact single shaped recipe with its stable ID and ingredient counts.

These are not Forge GameTests. They do not run Rhino, prove a real cooldown scheduler, serialize a real world, perform Minecraft crafting, test a network handshake, or prove actual KubeJS reload behavior. Fresh script evaluation is intentionally a narrower test than reloading a running server. Update pinned expectations when intentionally changing the starter.

### In-Game Acceptance Checklist

| Check | Expected result |
| --- | --- |
| Fresh installation | No startup definition or server-script errors |
| Craft exact statue pattern | One named statue; no operator action needed |
| Contact and menu | Aster's name/contact text and Guiding Breath description resolve |
| Injured, dry, ready | No heal and no success cooldown |
| Injured, in water, ready | Up to two health points gained; one normal cooldown starts |
| Press again during cooldown | No second heal |
| Full health, in water, ready | No heal and no success cooldown |
| One health point missing | One point gained; still one successful use |
| Trial | Correct enemies/goals/timers, then normal return and cleanup |
| Server-script reload | One heal per eligible press, not duplicate listeners; recipe remains correct |
| Dedicated client/server | Matching definitions connect and authoritative server runs the callback |

Natural regeneration and other mods can affect observed health, so distinguish them from the exact moment of activation. Inspect the instance tag through your existing developer tooling if validating persistence; the starter intentionally has no counter HUD. Test real save/load and resets separately if your design starts depending on that state.

## Package A Reusable Addon

Build the distribution from a clean starter copy, not from your entire played instance. Put `config/`, `kubejs/`, and `resourcepacks/` at the archive root, alongside the README and license; include tests if you want recipients to verify their edits. Do not add another outer folder that hides the overlay layout.

If shipping the resource pack separately, archive only the contents of `resourcepacks/aster-addon/`. That separate pack archive must have `pack.mcmeta` and `assets/` at its root. Tell users which archive is an extract/merge installer and which is an enable-in-game resource pack.

The included MIT license applies to this starter's example source and documentation. Preserve its notice when distributing substantial portions. It does not relicense Minecraft, Divinity Engine, or separately obtained mods/dependencies; those keep their own licenses. Do not bundle their binaries as though they were part of this starter's license.

List the exact game, loader, Divinity Engine, KubeJS, Rhino, and Architectury versions you actually tested. Include the matching-definition requirement and the one-Aster-example warning. Exclude worlds, logs, caches, unrelated configs, launcher account data, credentials, and dependency jars. Test the final extracted layout in a second clean instance.

## Troubleshooting

| Symptom | First checks |
| --- | --- |
| Statue absent from creative tab | Correct instance root, JSON path, compatible Divinity build, startup logs, and full restart |
| Statue exists but recipe fails | Both server scripts installed; exact pattern uses stone, not cobblestone; inspect recipe errors |
| Duplicate content error | Two Aster examples, a JSON plus startup-script declaration, or backup JSON inside the scanned tree |
| `DivinityEvents` undefined | Compatible integration build, KubeJS/dependencies, and `server_scripts` location |
| No heal | Contact, favor, equipped slot/key, cooldown, injured state, actual water contact, script errors |
| Heal on dry land | Unconditional JSON actions or another callback for the same ID |
| Heal twice | Duplicate listeners/files, or JSON healing plus script healing |
| Raw name/contact key | Client pack enabled, correct asset namespace/key, no extra folder above `pack.mcmeta` |
| Unknown hook or field | Check exact schema spelling and mode/hook compatibility, not a different mod's API |
| Unknown item/effect/entity reference | Confirm explicit registry ID and dependency availability on both sides |
| Definition mismatch on connection | Deploy identical JSON files to both sides and restart; changing translations cannot repair it |

Check `logs/latest.log` and `kubejs/logs/server.log` when available. Fix the first relevant error rather than adding a duplicate registration to make an item appear. For field-level reference details, continue with [JSON Content](JSON-Content.md) and [KubeJS](KubeJS.md).
