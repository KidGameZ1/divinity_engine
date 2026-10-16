# Nightshade UI integration

## Screens and controls

The four container screens use Nightshade Core's `RenderLibContainerScreen`:

- Blessings: searchable, directly selectable god tabs, a compact two-column blessing list, readable descriptions,
  and three loadout controls. Scroll the god list or use Page Up/Page Down outside the search
  field; Ctrl+F focuses search. Large blessing kits have a visible range/total and previous/
  next page controls. Scroll the blessing grid, or focus a blessing/page control and use
  Page Up/Page Down; Home/End jump to its first/last row. Scroll the description for longer
  text. Press 1/2/3 outside search to use a loadout control.
- Divine Codex: original book artwork rendered with Nightshade nine-slicing, Nightshade
  controls, and the same registered-page and player-specific journal model. Formatted
  Minecraft components retain the existing measured pagination, styles and narration.
- Pocket inventory: the same restrained celestial palette and small Aethon seal as Blessings,
  with dark inset slot wells. Vanilla still owns all 63 slots, carried
  stacks, item tooltips, quick transfer and server container traffic.
- Pocket domain: compact tabs for room creation, rules, residents and portals, with the
  existing server-validated actions and room-size inputs.

Shrine conversations and communion dialogs keep vanilla `Screen` lifecycle/input and use
the shared Nightshade-rendered stone panels and controls. Their reserved text/action space
adapts to the visible choices. Statue-opened Blessings remains locked to its opening god:
no other gods or god-search controls are shown in that context.

The existing compact trial HUD uses a Nightshade node tree. No additional HUD or ascension
system is introduced. The two inventory-injected blessings icons also use Nightshade and
are registered once per screen initialization, not appended on every tick.

`client/ui/DivinityUi` supplies shared layout helpers. `NightshadeButton` and
`NightshadeIconButton` retain native button focus, click, tooltip and narration semantics
while delegating rendering to Core. `NightshadeEditBox` uses a Core field skin around
Minecraft's native clipboard/selection/caret editor. Keeping these native input adapters
is intentional; the UI library does not replace server menus or inventory behavior.

Resource packs may override `divinity_engine:controls` using Core's theme format at
`assets/divinity_engine/renderlib/themes/controls.json`. Without an override, controls
use the built-in parchment palette with selected, focused and disabled styling. Core's
resource-reload repository is consulted at render time. The existing book/icon texture
paths remain unchanged and are still replaceable by resource packs. The old pocket
inventory texture is retained as an asset but is no longer used by the celestial screen.
The search field has a separate `divinity_engine:search` theme, defaulting to an ink-colored
inset so Minecraft's native shadowed editor text stays legible.

## Blessings galaxy

The blessings screen is a compact celestial panel, capped at 480x300 logical pixels.
Flat rectangular controls, small pixel sigils and a two-column blessing list replace the
large central astrolabe and repeated diamond ornaments. Each of the 18 gods retains its
distinct small motif; addons use a neutral seal. The god list, three loadout controls,
quest/favor views, communion and trial actions retain their existing behavior.

`ConstellationButton` preserves native clicks, keyboard focus, tooltips and narration.
Its custom Nightshade nodes, the engraving frame and god seals use bounded position/color
meshes through Core's render types, including with shaders disabled. Cool white lettering,
muted blue highlights, selection stripes and keyboard-focus outlines distinguish state
without adding decoration to every control.

`GalaxyUi` adds a screen-local `divinity_engine:blessings_galaxy` node. Its renderer is
registered through Nightshade's custom UI registry and draws one quad using a Nightshade
material and shader. A static, pixel-quantized nebula stays near the right margin, with
sparse pixel stars and subtle stepped twinkling; the selected god gently tints it. No entities, world
particles, world effects or persistent HUD are created.

Shader sources are in `assets/divinity_engine/shaders/core/blessings_galaxy.*` and reload
with resource packs. If Nightshade shaders are disabled or fail to load, a bounded
position/color mesh renders a bounded grid of faint nebula tiles and pixel stars instead. Setting Minecraft's
Screen Effects to zero freezes animation in both paths. Both paths remain clipped to the
panel and use Core-managed render states.

Resource packs can theme its search field through
`assets/divinity_engine/renderlib/themes/blessings_search.json`. Custom control/seal/frame
node IDs are declared in `DivinityUiTypes` and rendered in `CelestialUiRenderer`, separate
from ordinary Core widgets. The Codex theme, textures and registered pages are unchanged.

Large kits reuse at most six blessing controls (four on compact layouts), rather than
creating a widget for every addon blessing. IDs preserve selection through live list
changes; resize/reordering reveals that selection, and removals clamp stale offsets.
Paging does not change the equipped loadout. Paging controls disappear for small/empty
kits, and an empty god's kit has an explicit empty state.

## Pocket inventory

The pocket screen reuses `GalaxyUi` and the same celestial frame renderer without the god
navigation rail. A small Aethon sigil identifies the blessing's patron. A separate
`divinity_engine:celestial_vault` mesh adds one small constellation in the empty right margin.
The footer shows the vault's storage capacity. Detail stays outside the item cells. One
`divinity_engine:celestial_inventory` node draws all inset slot wells in
a shared Nightshade mesh batch. Coordinates come directly from the live menu, including
its original two-pixel offsets; no slot IDs, backing inventories or transfer rules change.

The 176x154 slot canvas is centered within a 192x206 frame. The extra header
and margin are explicitly inside the menu's click boundary, so clicking the decoration
does not become an outside-click item drop. Native Escape/inventory keys, drag handling,
hover highlights, stack counts, durability bars and tooltips are retained. Shader fallback
and Screen Effects reduced-motion behavior are shared with Blessings.

## Authority and compatibility

Browsing tabs is client-local and never changes favor, loadout, the opening statue, or
the old capability page index. An empty or not-yet-synced pantheon stays open with contact
instructions instead of closing the screen.

`EquipBlessingMessage` names the menu, god, blessing and slot, plus the requested equipped
state. The server checks the live menu, contact, favor, ownership and duplicate slots.
Repeated equip/unequip requests are idempotent. An old unequip cannot clear a different
blessing that replaced it. The client waits for the existing authoritative capability sync.

Tabs never grant trial permission. Only the god belonging to the statue that opened the
menu can expose its trial action; inventory-opened menus cannot start trials. No trial
encounters, story rewards, save IDs, Codex registries or inventory slot layouts are changed.

## Automated checks

For the current whole-UI revision, use `node docs/tools/run_ui_preview.mjs --suite all`
after `./gradlew jarJar verifyBundledMods --console=plain`. It creates fresh copies of the
synthetic `Core-Smoke` fixture, runs the full and header suites sequentially, and records
reports plus actual client screenshots. `--suite full` or `--suite header` narrows scope.
The launcher rejects personal saves and stale packages. Client-only fixtures do not equip
blessings, submit story choices, transfer inventory or change server progression.

The full suite includes Codex pagination/reload, seven pocket-inventory scenarios, seven
large-blessing-kit scenarios, statue locking, domain controls and communion dialogs.
The large-kit fixture uses 37 then 47 client-only blessings, final odd rows, insertion,
removal and empty states. Journal fixtures are borrowed only during synchronous UI
callbacks; live client data and options are restored. These are presentation checks,
not multiplayer playthroughs, inventory-transfer tests or proof that gameplay actions work.

`BlessingsUiGameTests` separately covers stable IDs, invalid menu/slot/god/favor,
duplicate requests, stale removals and player isolation. Run only the relevant focused
server namespace when changing that behavior rather than the entire mod's test suite.

## Compact Celestial Verification (2026-10-16)

- Java 17 compilation, reobfuscated `jarJar` packaging and `verifyBundledMods` passed.
- The full packaged-client suite passed all 112 scenarios and wrote 85 screenshots.
  This includes the fixed crash when opening Blessings without any contacted gods.
- The header/shrine-dialog suite passed all 62 scenarios and wrote 50 screenshots
  before the final null-selection guard; no visual code changed after that run.
- Reviewed actual screenshots of Blessings, statue-locked menus, the original Codex,
  long/small Codex pages, pocket inventory and shader fallback, domain room controls,
  and compact shrine/communion choices. Simulated 320x240 layouts use a smaller logical
  screen inside the larger capture, not a physically resized game window.
- Both runs verified fixture cleanup and unchanged real gameplay menus. No personal
  save was opened. Live multiplayer equip/sync and inventory transfers remain manual coverage.

Full report:
`/private/var/folders/w8/7__x388x10b0tz0467wnp1pc0000gn/T/nightshade-production-smoke-ui-3cKBAh/divinity-codex-smoke/run-12242083269673042931/report.json`.

Header report:
`/private/var/folders/w8/7__x388x10b0tz0467wnp1pc0000gn/T/nightshade-production-smoke-ui-fnw7Jc/divinity-codex-smoke/run-2599606120864715037/report.json`.

Final build log: `/tmp/divinity-celestial-empty-fix.log`.
Final jar SHA-256: `4794091c0183b57077c08fb212223d6b0ae3f0526d6ae2fc2f9c3b19543ca9bf`.

## Verification record (2026-09-23)

- Java 17 compilation, JarJar packaging and reobfuscation passed using the `jarJar` task.
- Seven pocket-only client scenarios passed: all 63 original slot coordinates, native
  hover/tooltip handling, decorative click bounds, scales 2/3, logical 320x240, shader
  fallback and reduced motion. Reviewed screenshots of the detailed frame and slot art.
- Seven blessings-overflow client scenarios passed with 37 then 47 synthetic blessings:
  page navigation, final odd row, logical 320x240 resize, live insertions/removals, keyboard
  navigation, and empty state. Reviewed normal and compact range/detail layouts.
- The initial overflow fixture required a correction for Forge's mandatory registry
  holder. The passing fixture borrows a real holder without modifying the frozen registry;
  its display identities are client-only and cannot exercise addon registration or equips.
- Client data/options were restored and real gameplay menus remained unchanged.
- Per the requested scope, only changed UI behavior was tested in these final runs.
  Narrative, trial, broad Codex, addon and server GameTest suites were not rerun for these
  visual/pagination revisions. Earlier baseline results do not substitute for those tests.

Build logs: `/tmp/divinity-pocket-detail-build.log`, `/tmp/divinity-blessings-overflow-build.log`.

Pocket report (before the subsequent Blessings-only changes):
`/Users/kaydenalexis/.gradle/workspace-runs/nightshade-production-client-qa/nightshade-production-smoke-e5_omx41/divinity-codex-smoke/run-749519660552952035/report.json`.

Blessings report:
`/Users/kaydenalexis/.gradle/workspace-runs/nightshade-production-client-qa/nightshade-production-smoke-08kvgill/divinity-codex-smoke/run-10239552853466690547/report.json`.

Artifact:
`/Users/kaydenalexis/.gradle/workspace-builds/divinity-engine/forge-1.20.1/libs/divinity_engine-1.9-1.20.1.jar`.

SHA-256: `80f50e3467d8dd8e3ef8e035005afb84416583feca42577db9ec4a33c1c88af6`.

Remaining manual coverage: live multiplayer equip/sync, real statue trial launches, pocket
item transfers, and interacting with the survival/creative inventory icons. The smoke
runner deliberately does not send gameplay actions or change server progression.
