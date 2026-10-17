# JSON Content

This is **startup content**, not a reloadable datapack registry. Forge must know
about gods and their statue blocks before opening a world. Install identical
definition files on the client and server, then fully restart both. A content
fingerprint participates in the network handshake and rejects mismatched definitions.

## Definitions And Programming

Use JSON to define IDs, god membership, favor requirements, cooldowns, visuals
and supported trial phases. Use **KubeJS server scripts** to program custom
blessing conditions and behavior. A JSON-defined blessing can receive a
`DivinityEvents.blessing` callback directly by its ID; it does not need a second
registration in a startup script.

For script-only blessing behavior, use `"hooks": {}` in the JSON and put the logic
in `kubejs/server_scripts`. JSON actions run before the callback, so leaving a heal
action in JSON would heal before your script checks a custom condition.
See the [combined example](https://github.com/KidGameZ1/divinity_engine/tree/main/examples/json-kubejs-content)
and [KubeJS guide](KubeJS.md) for the complete setup and success/cooldown handling.

JSON's fixed actions below still work without KubeJS. JSON alone is not a language
for arbitrary programming. The current KubeJS callback API covers content
blessings, not arbitrary god or trial lifecycle logic.

## File Layout

```text
config/divinity_engine/content/
  example_pantheon/
    gods/aster.json
    blessings/guiding_breath.json
    trials/trial_of_shelter.json
```

The namespace and relative filename determine the ID. `gods/aster.json` above
declares `example_pantheon:aster`. Subdirectories are supported. Use your own
namespace, not `minecraft`, `forge` or `divinity_engine`.

## God

```json
{
  "schema_version": 1,
  "domains": ["Shelter", "Wayfinding"],
  "blessings": ["example_pantheon:guiding_breath"],
  "offerings": {"minecraft:amethyst_shard": 2, "minecraft:bread": 1},
  "prayer_favor": 1,
  "color": "#B5D5EF"
}
```

This declares the god and `example_pantheon:aster_statue` block, item and block
entity type using the existing automatic-statue system. It uses the original
statue model by default; resource packs can override the model/texture normally.
It does not automatically generate a world shrine or recipe.

Offering values are per item before existing worship budgets and caps. The shrine
processor consumes accepted offerings and records stories; your JSON must not
try to consume them a second time. `prayer_favor` is 0..2 and uses the shared
prayer timing/budgets, not an independent per-tick reward.

## Blessing

```json
{
  "schema_version": 1,
  "favor": 50,
  "cooldown_seconds": 12,
  "mode": "active",
  "color": "#B5D5EF",
  "particle_texture": "minecraft:textures/particle/glint.png",
  "hooks": {
    "pressed": [
      {"type": "heal", "target": "self", "amount": 2},
      {"type": "effect", "target": "self", "effect": "minecraft:speed", "ticks": 80, "amplifier": 0}
    ]
  }
}
```

`mode` is `active`, `passive` or `toggle`. Passive/toggled `tick` hooks run at
`tick_interval` (default 20; range 5..1200 ticks), never continuously on the
render thread. Active press hooks use the normal blessing slots, not new keys.

Active mode accepts only `pressed`. Passive mode accepts `tick`, `damage_entity`,
`taken_damage`, and `kill_entity`. Toggle mode accepts the passive hooks plus
`toggle_on` and `toggle_off`; its ongoing hooks run only while toggled on.
Unsupported mode/hook combinations fail validation. All hooks
respect the existing cooldown and server authority. A successful action starts
the normal cooldown; a healing attempt at full health does not count by itself.

| Action | Fields | Bounds / behavior |
| --- | --- | --- |
| `heal` | `amount` | .01..40 health points, respects healing events |
| `damage` | `amount` | .01..40 damage, cannot damage its owner/allies |
| `effect` | `effect`, `ticks`, `amplifier` | Registered effect ID; 1..12000 ticks; amplifier 0..4 |
| `ignite` | `ticks` | 1..12000, rounded up to whole seconds; respects fire immunity |
| `dash` | `amount` | .01..3 horizontal velocity, self only; normal collision remains |

All actions accept `target`: `self`, `target`, or `nearby_hostiles`. Target means
the living entity supplied by the current event; a plain key press has no target.
Area targeting accepts `radius` (.5..12, default 4), checks line of sight,
excludes players/allies/tamed creatures and affects at most 16 nearest living
hostiles. Recursive JSON damage callbacks are suppressed.

Optional `particle_texture` is a complete texture path, not a particle-type ID.
It emits a small Nightshade-managed burst after successful behavior. Omit it for
no automatic visual, or use Java's visual-authoring API for a custom trajectory.

## Trial

```json
{
  "schema_version": 1,
  "god": "example_pantheon:aster",
  "reward_favor": 25,
  "start_message": "Aster: Keep the road open.",
  "complete_message": "Aster: You are a refuge now.",
  "phases": [
    {"name": "Keep the Watch", "objective": "Defeat the sentries",
      "duration_ticks": 600, "goal": "defeat_all",
      "spawns": [{"entity": "minecraft:zombie", "count": 3}]}
  ]
}
```

There are 1..8 phases, each lasting 20..12000 ticks. `defeat_all` requires killing
all configured mobs before timeout; `survive` advances at the duration boundary.
Spawn groups have 1..16 mobs each, at most 32 per phase. Only summonable non-boss
monster types are accepted; arbitrary NBT and boss spawns are not supported.

The shared trial engine retains arena preparation, protection, ownership, death
attribution, logout failure, return teleport and batched cleanup. Starting a trial
still requires the correct statue-opened blessings screen. A JSON trial cannot
replace an existing god's trial. Final favor is bounded to 0..50 and uses normal caps.

## Names, Stories And Validation

The example includes a resource pack with language keys such as
`example_pantheon.gods.aster`, `example_pantheon.blessings.guiding_breath`,
`example_pantheon.blessings.guiding_breath.description`, and
`block.example_pantheon.aster_statue`. Put additional languages in the normal
`assets/<namespace>/lang` layout. These are presentation resources, not saved identities.

Stories/presence/relationships use the separate existing
[`divine_narratives` datapack API](https://github.com/KidGameZ1/divinity_engine/blob/main/docs/connected-pantheon-authoring.md).
It can reference the god created here. Curses, custom objective handlers and
arbitrary encounter logic remain Java APIs in this first content-schema version.

Unknown fields, malformed IDs, unsupported actions, oversized files, duplicates
and unresolved references fail startup with a content/file context. Limits are
512 definitions, 256 KiB per file, 64 blessings per god and 16 actions per hook.
Keep definitions installed while opening saves that use them. `/reload` cannot
add/remove these registry entries.
