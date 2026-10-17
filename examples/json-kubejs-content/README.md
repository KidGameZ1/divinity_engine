# JSON Definitions + KubeJS Programming

This complete example uses JSON for Aster, an automatic statue, Guiding Breath
and a two-phase trial. KubeJS programs Guiding Breath to heal 2 health points
only while its injured owner is in water. It does not grant the JSON-only
example's speed effect.

## Install

1. Use an isolated Forge 1.20.1 instance with Divinity Engine, KubeJS
   2001.6.5-build.26, Rhino 2001.2.2-build.17 and Architectury Forge 9.1.12.
2. Install this example's `config/divinity_engine/content` on both client and server.
3. Install `kubejs/server_scripts/aster.js` on the server. In singleplayer, use
   that same game instance's `kubejs/server_scripts` folder.
4. Install and enable `resourcepack` on the client for names and descriptions.
5. Fully restart both sides. Contact Aster through its statue, earn favor and
   equip Guiding Breath through the blessings menu.

There is no startup JavaScript registration. JSON has already registered these
IDs. Choose this example OR `json-content` OR `kubejs-content`, not more than one.
They intentionally share stable Aster IDs. Keep definitions installed when
reopening saves that use them, and back up saves before changing content packs.

## How It Works

- JSON owns identity, eligibility, cooldown, visual texture and trial phases.
- Empty blessing `hooks` leave gameplay to the KubeJS callback, which attaches
  to the same namespaced blessing ID.
- The callback checks the hook, water condition and current health on the server.
- After successful healing, `markSuccessful()` lets Divinity start its normal
  cooldown and emit the configured Nightshade visual. Canceled healing, full
  health or dry land do not count as success.

Keep JSON hooks empty when the script must decide whether the entire ability
works. JSON actions run first; a script cannot undo an unconditional heal already
performed by JSON. Do not start the cooldown again in the script.

KubeJS is required for this example's custom behavior, not for Divinity generally.
Without the callback, this empty-hook blessing does nothing. Custom god/trial
lifecycle logic, curses and new objective handlers still use Java APIs.

See [JSON Content](../../docs/wiki/JSON-Content.md) and
[KubeJS](../../docs/wiki/KubeJS.md). Runtime checks should cover dry land, injured
in water, full health, healing canceled by another mod, and repeated presses
during cooldown. The small `behavior.test.mjs` checks example logic using test
doubles; it is not a Forge/Rhino integration or multiplayer test.
