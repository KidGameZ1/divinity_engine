# Assets and Localization

[Developer Guide](Developer-Guide.md) | [JSON + KubeJS Tutorial](JSON-KubeJS-Tutorial.md) | [Java Addon Tutorial](Java-Addon-Tutorial.md) | [Stories and Upgrades](Stories-and-Upgrades.md)

This guide gives Aster's addon names, a statue appearance, and a vanilla-textured
particle cue on Forge 1.20.1. A resource pack changes presentation; it does not
register a god, authorize an ability, or implement a gameplay upgrade.

## 1. Put Each File on the Correct Side

Use the complete [JSON + KubeJS resource-pack template](../../examples/json-kubejs-content/resourcepack/pack.mcmeta)
and [language file](../../examples/json-kubejs-content/resourcepack/assets/example_pantheon/lang/en_us.json)
with the installation instructions in the
[JSON + KubeJS Tutorial](JSON-KubeJS-Tutorial.md). That example uses JSON startup
definitions and a content-blessing server callback. It does not require a
duplicate startup-script declaration, and it adds no custom god/trial lifecycle
callbacks.

| Content | JSON/KubeJS installation | Java-addon project path |
| --- | --- | --- |
| God/blessing/trial definitions | Matching `config/divinity_engine/content/example_pantheon/` files on both sides | Java registration; do not also install conflicting JSON IDs |
| Programmed content blessing | Server `kubejs/server_scripts/aster.js` | Java callback in the addon |
| Language/model/texture resources | Client resource pack `assets/example_pantheon/` | `src/main/resources/assets/example_pantheon/` |
| Story definitions and custom loot | World datapack `data/example_pantheon/` | `src/main/resources/data/example_pantheon/` |
| Pack metadata | `pack.mcmeta` at each pack root | `src/main/resources/pack.mcmeta` |

A dedicated server does not send arbitrary `assets/` files merely because a
datapack contains them. Give clients the resource pack and enable it, arrange
normal server resource-pack delivery, or ship the assets in an addon installed
on clients. Gameplay definitions still need their own correct server setup.
In singleplayer the integrated server shares the instance, but client resource
packs and the world's datapacks remain separate loading systems.

Standalone resource-pack layout:

```text
<instance>/resourcepacks/aster-resources/
  pack.mcmeta
  assets/example_pantheon/lang/en_us.json
  assets/example_pantheon/models/block/aster_statue.json
```

The model is optional: automatic fallback assets already work without it.
When ZIP-packaging, put `pack.mcmeta` and `assets/` at the archive root. For a
datapack, put `pack.mcmeta` and `data/` at its root instead. Minecraft 1.20.1 uses
**pack format 15** for these resource/data packs, as the current examples show:

```json
{
  "pack": {
    "pack_format": 15,
    "description": "Aster resources for Divinity Engine Forge 1.20.1"
  }
}
```

## 2. Distinguish IDs, Paths, and Keys

These strings are not interchangeable:

| String | Meaning | Resolved file/key |
| --- | --- | --- |
| `example_pantheon:aster` | Registered god ID | Startup `gods/aster.json` or Java registration |
| `example_pantheon:aster_statue` | Registered block/item/block-entity ID | Automatic statue identity |
| `example_pantheon:block/aster_statue` | Model resource ID | `assets/example_pantheon/models/block/aster_statue.json` |
| `example_pantheon:block/aster_stone` | Model texture ID | `assets/example_pantheon/textures/block/aster_stone.png` |
| `minecraft:textures/particle/glint.png` | Complete Nightshade texture-file location | Vanilla `assets/minecraft/textures/particle/glint.png` |
| `example_pantheon.gods.aster` | Translation key | Entry inside `assets/example_pantheon/lang/en_us.json` |

Model references omit `models/` and `.json`; model texture references omit
`textures/` and `.png`. Nightshade's `particle_texture` uses the complete texture
path instead. Namespaced resource locations use lowercase resource-safe paths;
a translation key is looked up in a language JSON object, not loaded as a file.

Names are presentation, while registry IDs are save identities. Translate or
rename display text without renaming a released god, blessing, or statue ID.
For a path containing `/`, the god/blessing translation-key methods replace
that slash with `.`; for example, `example_pantheon:travel/guiding_breath` uses
`example_pantheon.blessings.travel.guiding_breath`.

## 3. Add Names and Descriptions

For the combined JSON + KubeJS example, use this complete language file at
`aster-resources/assets/example_pantheon/lang/en_us.json`. A Java addon uses
`src/main/resources/assets/example_pantheon/lang/en_us.json` instead:

```json
{
  "example_pantheon.gods.aster": "Aster",
  "example_pantheon.gods.aster.contact": "Aster: Even a lost traveler deserves a guiding light.",
  "example_pantheon.gods.aster.loss": "Aster: The road remains, should you return.",
  "example_pantheon.blessings.guiding_breath": "Guiding Breath",
  "example_pantheon.blessings.guiding_breath.description": "While in water, restore 2 health points. Does nothing at full health.",
  "example_pantheon.story.road_and_refuge.title": "Road and Refuge"
}
```

Use the same keys in another locale file, such as
`assets/example_pantheon/lang/fr_fr.json`, with translated values. Do not change
IDs or filenames in the startup definitions to switch language. If you choose
the JSON-only ability instead, describe its actual unconditional heal and speed
effect rather than the combined template's water-only behavior.

**Automatic statue naming is composed.** `StatueBlock.getName()` formats
`block.divinity_engine.named_statue` with the god's translated name, and
`StatueBlockItem.getName()` delegates to it. The English shared format is
`%s Statue`, so translating the god produces "Aster Statue". The example packs
also contain `block.example_pantheon.aster_statue`, but that key alone does not
override this block/item's current name implementation. Overriding the shared
format changes all automatic statues; it is not a per-god naming API.

Domain labels and startup trial message fields are ordinary strings. The strict
content schema does not accept a narrative translation object in those fields.
Do not assume all strings in all formats share the same localization adapter.

## 4. Translate Story Text

In the full narrative JSON from [Stories and Upgrades](Stories-and-Upgrades.md),
replace the `title` value with this object:

```json
{"translate": "example_pantheon.story.road_and_refuge.title"}
```

This is a **field value**, not a standalone narrative file. The narrative text
adapter also supports translation objects for request/branch text, reward
descriptions, chronicle prose, dialogue, and chapter titles. Each translated
paragraph can be an object inside its passage array. Presence dialogue has its
own string-array parser; do not assume it accepts the same object form.

Java narrative builders accept strings; use
`NarrativeText.translatable("example_pantheon.story.road_and_refuge.title")`
from `net.nightshade.divinity_engine.api.narrative`, not a `Component` where a
builder requires a string. The marker survives accepted-story snapshots and
network synchronization; clients resolve it using their language resources.
Changing language text does not change story IDs or earned rewards. Existing
literal snapshot prose is not rewritten into a translation marker by reload.

The [Connected Pantheon translations](../../examples/connected-pantheon-pack/assets/example_pantheon/lang/en_us.json)
and [complete first-star story](../../examples/connected-pantheon-pack/data/example_pantheon/divine_narratives/first_star_story.json)
show this in a full example. That narrative pack still requires its Vesper host
addon and custom objective handler; language assets do not supply either.

## 5. Understand the Automatic Statue Fallback

`GodsDeferredRegister` creates the god's statue block, block item, and block
entity together. Startup JSON uses that same helper. It adds a hidden,
low-priority in-memory client/data pack with these fallback files for Aster:

```text
assets/example_pantheon/blockstates/aster_statue.json
assets/example_pantheon/models/block/aster_statue.json
assets/example_pantheon/models/item/aster_statue.json
data/example_pantheon/loot_tables/blocks/aster_statue.json
```

The block model inherits `divinity_engine:block/statue`, the item inherits
`example_pantheon:block/aster_statue`, and the loot table drops the registered
statue item subject to explosion survival. The blockstate supplies four facing
variants. Nothing writes generated files into your addon or world. No recipe
or world-generation shrine is automatically created.

Normal higher-priority packs can override individual defaults. The fallback
provider also skips a path already shipped by a mod, so an addon jar's resource
is not shadowed. Override only the files you need, preserving registry IDs.
See the [automatic-statue API guide](../automatic-statues.md) for retaining a
previously released custom statue path during Java registration.

## 6. Change the Statue Appearance

### Reuse the Existing Geometry

Create `aster-resources/assets/example_pantheon/models/block/aster_statue.json`:

```json
{
  "parent": "divinity_engine:block/statue",
  "textures": {
    "0": "minecraft:block/amethyst_block",
    "particle": "minecraft:block/amethyst_block"
  }
}
```

The current statue model's face texture variable is `#0`, not `#all`.
This changes its sampled art without requiring a new PNG; the default blockstate
and item model still point to the overridden model. The original UV layout was
authored for the statue texture, so a tiled vanilla block texture will not look
like a purpose-painted skin. The `particle` texture here controls block-model
break/hit fragments, not blessing combat emitters.

For your own painted texture, change those two values to
`example_pantheon:block/aster_stone` and supply your PNG at
`aster-resources/assets/example_pantheon/textures/block/aster_stone.png`.
That custom PNG is author-supplied, not an asset included with this guide.

### Replace the Model

As a minimal complete replacement, use the following JSON at the **same**
`assets/example_pantheon/models/block/aster_statue.json` path:

```json
{
  "parent": "minecraft:block/cube_all",
  "textures": {
    "all": "minecraft:block/amethyst_block"
  }
}
```

This deliberately renders a cube instead of a statue, proving the override
path before introducing complex art. Replace it with your exported block-model
JSON and textures to author your own geometry. This is a block model, not an
arbitrary Java/entity renderer. Model changes do not change the statue's collision
shape, placement, facing/waterlogged properties, container, worship, or trial
behavior. Test appearance against that unchanged block behavior.

You usually do not need to override the generated state/item files. If your
export uses a different model path, supply explicit files at these paths:

`aster-resources/assets/example_pantheon/blockstates/aster_statue.json`:

```json
{
  "variants": {
    "facing=north": {"model": "example_pantheon:block/aster_statue"},
    "facing=east": {"model": "example_pantheon:block/aster_statue", "y": 90},
    "facing=south": {"model": "example_pantheon:block/aster_statue", "y": 180},
    "facing=west": {"model": "example_pantheon:block/aster_statue", "y": 270}
  }
}
```

`aster-resources/assets/example_pantheon/models/item/aster_statue.json`:

```json
{"parent": "example_pantheon:block/aster_statue"}
```

These reproduce the fallback references; replace the referenced model ID only
when your actual model file has that new path. A client resource pack cannot
change loot. Any deliberate drop override belongs in a server datapack at
`data/example_pantheon/loot_tables/blocks/aster_statue.json`.

## 7. Use a Vanilla Blessing Texture

The complete [Guiding Breath startup definition](../../examples/json-kubejs-content/config/divinity_engine/content/example_pantheon/blessings/guiding_breath.json)
sets `particle_texture` to `minecraft:textures/particle/glint.png`.
Keep it in the startup file
`config/divinity_engine/content/example_pantheon/blessings/guiding_breath.json`
on **both** client and server. The content fingerprint includes behavior and
visual definition fields, so this is not merely a client-only pack edit.

The schema requires a resource path beginning `textures/particle/` and ending
`.png`. It validates the path shape, not that every client actually has the PNG.
The referenced vanilla glint file exists in Minecraft 1.20.1; do not copy it
into your resource pack. Do not use `minecraft:glint`, `minecraft:bubble`, or a
translation key as the value.

After successful behavior, Divinity emits a small burst through Nightshade's
independent particle engine. A vanilla sprite does not instantiate a native
`ParticleTypes` particle or inherit its movement/lifetime. This single JSON
field does not configure a custom trajectory, bloom, hit detection, or damage.
For authored motion and safe lifecycle ownership, see
[Effects and UI](Effects-and-UI.md). Omitting `particle_texture` disables the
content system's automatic burst; a custom script might still emit its own effect.

## 8. Check Packaging Before Distribution

- Open the ZIP and confirm `pack.mcmeta` plus `assets/` or `data/` are at its root, with lowercase paths and `.json`/`.png` extensions in the correct places.
- Check that clients have translations/textures, the server has narrative data, and both sides have identical startup definitions. Restart after definition changes; resource reload alone cannot register content.
- Inspect all four statue facings, the inventory item, waterlogging, drops, and resource-pack priority. A missing model/texture is a resource issue, not evidence the god failed to register.
- Test another client language, long translated labels, and a reconnect. Literal startup trial text and already-saved literal narrative text remain literal.
- Trigger an actual successful blessing action. Full-health or canceled healing should not be used to diagnose a missing success-only particle cue.

This guide statically checks paths and APIs against current source. The vanilla
glint/amethyst textures and cube model were verified in the cached Minecraft
1.20.1 client jar; the Divinity parent model and its `#0` texture exist locally.
No custom PNG was generated, and no rendered-client, pack-loading, build, or
multiplayer validation was performed here.

## Source References

- [StatueResources](../../src/main/java/net/nightshade/divinity_engine/api/gods/StatueResources.java): exact generated files, rotations, pack priority, and mod-asset fallback rules.
- [Statue block](../../src/main/java/net/nightshade/divinity_engine/block/StatueBlock.java) and [statue item](../../src/main/java/net/nightshade/divinity_engine/item/StatueBlockItem.java): composed translated names.
- [God translation keys](../../src/main/java/net/nightshade/divinity_engine/divinity/gods/BaseGod.java) and [blessing translation keys](../../src/main/java/net/nightshade/divinity_engine/divinity/blessing/Blessings.java): namespace/path mapping.
- [Content schema](../../src/main/java/net/nightshade/divinity_engine/util/content/ContentSchema.java), [content registration](../../src/main/java/net/nightshade/divinity_engine/api/content/DivinityContent.java), and [content blessing](../../src/main/java/net/nightshade/divinity_engine/util/content/ContentBlessing.java): texture validation, fingerprint, and success-only visuals.
- [Narrative text](../../src/main/java/net/nightshade/divinity_engine/api/narrative/NarrativeText.java) and [narrative JSON](../../src/main/java/net/nightshade/divinity_engine/util/divinity/narrative/NarrativeDefinitionJson.java): translation markers versus plain strings.
- [Statue model](../../src/main/resources/assets/divinity_engine/models/block/statue.json) and [original texture](../../src/main/resources/assets/divinity_engine/textures/block/gods_statue.png): the inherited geometry and texture variable.
