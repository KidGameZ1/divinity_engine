# KubeJS

Optional integration targets **KubeJS 2001.6.5-build.26**, Rhino
`2001.2.2-build.17` and Architectury Forge `9.1.12` on Minecraft 1.20.1.
Install these separately when using scripts. Divinity works without KubeJS;
no KubeJS classes are loaded by the normal mod entry point.

## Declare Content In Startup Scripts

Create `kubejs/startup_scripts/my_pantheon.js`:

```js
DivinityEvents.register(event => {
  event.blessing('my_pack:steady_breath', {
    schema_version: 1,
    favor: 50,
    cooldown_seconds: 10,
    mode: 'active',
    hooks: { pressed: [{ type: 'heal', target: 'self', amount: 2 }] }
  })
  event.god('my_pack:haven', {
    domains: ['Refuge'],
    blessings: ['my_pack:steady_breath'],
    offerings: { 'minecraft:bread': 1 }
  })
})
```

`event.god`, `event.blessing` and `event.trial` take a namespaced ID and exactly
the [JSON Content](JSON-Content.md) spec. The event fires once after startup scripts
load, before Forge registers blocks. Do not retain the event, schedule declarations
later, or put declarations in `server_scripts`. Registry changes require restart
on both sides. Script and JSON declarations share one registry: duplicate IDs fail.

## Add Custom Behavior On The Server

Create `kubejs/server_scripts/my_pantheon.js`:

```js
DivinityEvents.blessing('my_pack:steady_breath', event => {
  if (event.hook !== 'pressed') return
  if (event.owner.isInWater()) {
    event.owner.heal(1)
    event.markSuccessful()
  }
})
```

The callback runs after JSON actions on the **server thread**, only through the
configured content blessing's eligible hook/cooldown path. This does not intercept
every built-in blessing. Keep an empty `hooks` object if a content blessing is
entirely scripted. Use an appropriate `mode` and hook.

| Property/method | Meaning |
| --- | --- |
| `id` | Blessing's `ResourceLocation` |
| `hook` | `pressed`, `tick`, `damage_entity`, `taken_damage`, `kill_entity`, `toggle_on`, `toggle_off` |
| `owner` | Acting living entity, not necessarily a player |
| `target` | Optional event target; null for untargeted hooks |
| `instance` | Per-owner blessing state, including custom NBT |
| `level` | Server level |
| `nativeEvent` | Optional underlying Forge event; null for press/tick/toggle |
| `isSuccessful()` | Whether JSON or an earlier callback already succeeded |
| `markSuccessful()` | Tell Divinity to start the normal cooldown |

Returning `true` or canceling the KubeJS wrapper is **not** a success signal.
Use `markSuccessful()` synchronously when custom gameplay really occurs. Existing
JSON success is preserved. Do not manipulate cooldown twice or retain this event
for asynchronous mutations. The guard prevents recursive content damage callbacks,
not arbitrary unsafe Java access from trusted scripts.

Normal KubeJS server-script reload replaces server listeners. It does not re-run
startup registrations or change the handshake fingerprint. Ship matching startup
definitions on both sides; server logic belongs on the server. Use `instance`
data rather than a global variable for per-player counters.

The full [KubeJS example](https://github.com/KidGameZ1/divinity_engine/tree/main/examples/kubejs-content)
includes all three declaration types. Its language/resource pack is shared with
the JSON example. Do not install both registration alternatives together.

For development only, `./gradlew runGameTestServer -PwithKubeJS=true ...` adds the
optional libraries to a development run. They are compile-only by default and
never added to the distributed JarJar.
