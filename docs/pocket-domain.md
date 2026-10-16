# Aethon's Pocket Domain

Pocket Dimension now opens a Nightshade-rendered domain manager with the pocket
inventory's animated galaxy, celestial frame, and constellation controls. It is
not the stone shrine interface. The original 27-slot storage is still available.

## Using the domain

1. Equip Pocket Dimension and meet its existing 250-favor requirement with Aethon.
2. Press the equipped blessing key to open the manager. Domain abilities are unavailable during trials.
3. Choose **Build / Size > New chamber**, select a blueprint, enter the usable
   width/depth (7-61) and height (5-21), and press **Weave chamber**.
   Construction runs over server ticks. Watch its progress in **Chambers**.
4. Select **Arrival** beside a completed chamber. Outside the realm, aim at the
   top of a solid floor within six blocks, open the manager, and choose
   **Portals > Place entrance**. Close the menu and walk through the gateway.
   Arrival also chooses the destination for future captures. It does not teleport you.
5. Open **Rules**, choose a chamber, and set its laws. Each chamber stores
   its own rules; changes are checked and synchronized by the server.
6. **Storage** opens the original inventory. Sneak-use outside also opens it.
   The storage screen's **Domain** button returns to the manager.
7. Walk through the gold-marked exit at your room's far wall, or use **Leave realm**,
   sneak-use inside, or **Leave Pocket Realm** in the pause menu. These return
   you to a safe position near your entrance, or saved entry point if closed. The pause-menu exit does not
   require the blessing to remain equipped or eligible and is available to visitors too.

Four blueprints default to a 31x31x15 interior: Star Sanctum (quartz/end stone), Verdant
Grove (moss), Astral Workshop (andesite), and Astral Arena (deepslate/stone).
An arena is a private room, not a godly trial. Furnish interiors with your own
blocks. Domains support eight total chambers, including an imported old pocket,
and one construction/resize job at a time.

## Resizing and portals

- Use **Chambers > Resize**, or **Build / Size > Resize selected**, to change the
  selected chamber using the same three number boxes. Values describe usable
  interior space, excluding walls, floor and ceiling.
- Leave that chamber before resizing. Move creatures and loose items out too.
  Builds and containers within the retained interior stay intact. A shrink that
  would cut through a custom block is rejected before any edits. Close/move an
  exit that would fall outside the new bounds. Imported original sanctuary
  structures are not resized, so their unknown geometry remains untouched.
- New chambers stack vertically in fresh private columns. Existing rooms never
  move. Every completed room automatically has its own exit, plus separate
  front-left/down and front-right/up gateways to completed adjacent floors.
  A middle floor therefore has three portals. The lowest has exit/up; the highest
  has exit/down; a single-room domain has an exit. Legacy rooms link in the same
  saved room order without being physically relocated.
- The exit sits at the far wall with a gold doorway/arrow sigil. Floor gateways
  retain blue/cyan up/down arrows. All room exits return outside the anchored
  world entrance, or use the saved return location if the entrance was closed.
- The **Portals** tab has a room selector and displays its down/up destinations.
  **Close room exit** closes only the selected room's exit, not other exits or
  floor links. To move it, stand in that room, aim at a clear floor within six
  blocks, then choose **Place room exit**. It cannot overlap a floor gateway.
- Manually placed exits and the world entrance retain exact saved positions until
  their owner closes them. Automatic room exits follow the room bounds on resize;
  custom exits never move automatically. A closed exit stays closed after reload.
  The previous domain-wide exit is preserved in its original room, not relocated
  or duplicated there. Other existing rooms gain their own automatic exits.
- Gateways use Nightshade-rendered astral doorways, not replacement terrain. They
  do not remove blocks, create drops, or force-load idle rooms. Gateways are shared:
  players enter as visitors, mobs become residents after a successful transfer,
  and items/projectiles pass through without becoming residents. Only owners can
  manage their domain or move/close its portals. If blocked later, gateways wait
  for clearance. Travel cooldowns and a leave-before-reentry latch prevent bounce.
- Cold destinations load over subsequent ticks. Players should stay in the gateway;
  non-player crossings wait briefly at the source while it wakes. Closing the
  gateway cancels pending travel without deleting the source entity. Transfers
  preserve entity identity and item stacks; projectiles retain their motion.
  Mounted/passenger entities must dismount first, and entities that forbid
  dimension changes or belong to a trial cannot cross.
- Portal visuals persist until explicitly removed or out of view, instead of
  repeatedly expiring between server updates. Saved anchors never depend on the
  presence of a visual effect.
- Use the **Portals** tab to close the world entrance or selected room exit.
  The pause-menu exit remains available even if that room's exit or the world entrance was closed.

## Room laws

| Rule | Default | Behavior |
|---|---|---|
| Resident revival | On | Prevents a bound resident's lethal death in its assigned room, restores health, and returns it to a safe arrival position. No death drops or XP are generated. Off means death is permanent. |
| Emergency soul return | On | An owner with the eligible blessing escapes lethal damage at half health. Five-minute cooldown shared across their domain. Does not override administrative `/kill`. |
| Resident combat | Off | Blocks entity attacks involving bound residents and clears hostile targeting. Environmental hazards are separate. |
| Natural spawning | Off | Allows vanilla natural spawn checks when enabled; it does not add biome spawns or create creatures on demand. |
| Owner building | On | Permits the owner to modify/interact with the interior. The boundary remains protected. Other players cannot modify the room. |

Rules cannot disable normal exit or transfer ownership. Revivals require a safe
landing, not clearing blocks. Leave the arrival area unobstructed. Explosions and
mob griefing cannot destroy domain terrain. This is not a security sandbox against
administrator commands or arbitrary world-modifying code from other mods.

## Creatures

There is no resident count cap. The menu requests at most twelve records per
server page; **Next residents** reaches the rest without sending the entire
population to the client. The total displayed is the full saved count, not a limit.

Walking through a shared entrance automatically registers a mob in the selected
chamber. Room gateways update its assigned chamber; an exit releases its binding.
This does not require the owner to be online. Players remain visitors, not captured
creatures, and keep a saved way home. Visitors cannot build in or manage the host's
domain. **Leave Pocket Realm** in the pause menu remains available without a blessing.

Outside the domain, aim at a creature within six blocks and choose **Residents >
Capture creature I am looking at**.
Select a ready arrival chamber first. For this remote capture action (not voluntary
gateway travel), hostile creatures must be weakened to 30% health or less. Players, bosses, traders,
owned pets, leashed/mounted creatures, oversized creatures, and tracked trial mobs
are excluded. The actual entity moves, preserving UUID, equipment and saved data.

Visit a resident's chamber to load it before using **Release**. Release transfers
it back near its original position, only if safe; otherwise it remains in the
domain. If the original location is sleeping, wait briefly and try again. Unloaded
creatures are not duplicated or respawned from snapshots.
Residents remain normal Minecraft mobs, with their normal environmental needs.
Conversions or self-discarding behaviors (such as a creeper detonating) are not
lethal-hit revival; those remove the old resident's binding instead of cloning it.
Revival is prevention inside the realm, not resurrection of creatures already
dead elsewhere. No players are captured or revived as residents.

## Sleeping rooms and chunk loaders

- Unoccupied rooms save and unload through Minecraft's normal chunk system.
  Blocks, containers, resident entities, equipment and scheduled world data remain
  in the world save. Rooms are never erased or copied into blessing NBT to sleep.
- Divinity's construction and arrival tickets are temporary and do not force
  empty rooms to keep ticking. They release only their own tickets. Active chunk
  loaders from other mods and administrator-forced chunks are left alone.
  Merely placing an inactive loader block does not make a room stay awake.
- Players, active chunk loaders and other legitimate Minecraft tickets keep the
  relevant chunks loaded. A portal anchor, saved resident record or sleeping mob
  alone does not keep a room awake. A new crossing requests its destination again.
- Minecraft loads vertical chunk columns, not individual floors: stacked rooms
  in the same columns load together. A loader may cover only part of a large room.
- Pocket-only view/simulation distances are capped at four/three chunks (or the
  server's lower setting). Overworld and other dimension distances are unchanged.
- Uncapped residents still cost CPU when loaded. Chunk-loading a huge mob farm can
  still overload the server; this change does not disable normal mob behavior.

## Saves and developer structure

- The existing `divinity_engine:pocket_dimension` dimension is retained. Owner
  UUIDs reserve separate coordinates; no runtime dimensions or registry IDs are
  created. Existing structure assets are unchanged.
- `PocketDomainData` stores room dimensions, resize/build cursors, tower allocation,
  rules, residents, visitor return records, persistent entrance/exit anchors, return point,
  and inventory in overworld SavedData (`divinity_engine_pocket_domains.dat`).
  Data survives death, disconnect, and unequipping.
- Migration imports legacy blessing coordinates and inventory once, preserving
  the source NBT and room blocks. Later opens cannot reimport withdrawn items.
- New sites start far from the old random allocation area. A read-only scan
  rejects any occupied site before building. At most 512 scan/build operations,
  with a soft two-millisecond work budget, run per server tick across all players.
  `PocketChunkLoading` requests FULL-only chunk tickets and polls `getChunkNow`
  on later ticks. It never calls the server-thread-blocking `getChunkFuture`.
  A resize may replace only
  recorded generated shell/floor blocks, never arbitrary non-air blocks.
- Version-1 rooms default to their original 33x33x18 outer bounds. Version-2 saves
  persist pending dimensions separately until a resize finishes. Version 3 adds
  per-room `Exit` and `ExitClosed`; the older domain-level `Exit` remains a
  compatibility fallback until that specific room's exit is explicitly changed.
  Old room origins, inventories, return points, and structure templates are preserved.
- `PocketDomainManager` validates actions and owns transfers; `PocketRoomBuilder`
  handles scan/build jobs and `PocketPortals` resolves shared crossings.
  `PocketDomainEvents` enforces rules only within the pocket dimension.
- `PocketDomainMenu` sends a bounded room/resident snapshot on opening, and
  integer progress/rule updates through vanilla container data slots. Button
  requests are owner-, eligibility-, room-identity-, and rate-checked. IDs stay
  below 128 because vanilla's container-button packet uses a signed byte.
- Numeric sizes use a server-only `PocketRoomSizeMessage`, validated against the
  current menu owner, room identity, eligibility and dimension limits. No client
  packet is allowed to pick arbitrary world coordinates or another owner's realm.
- Resident pagination/release uses `PocketResidentsMessage`, checked against the
  current menu, owner, eligibility, rate limit and visible UUIDs. Resident list
  size is independent of vanilla's byte-sized container button IDs.
- `PocketDomainScreen` and the inventory screen use `GalaxyUi`,
  `ConstellationButton`, and Nightshade `RenderLibContainerScreen`. No permanent
  HUD, additional keybind, or client-owned world mutation is introduced.

The design takes inspiration from the personal-realm, chamber-control and resident
preservation concepts in [Small World](https://tensura.fandom.com/wiki/Small_World),
with explicit Minecraft limits instead of unlimited space or unrestricted capture.

## Focused validation

Run with Java 17:

```sh
./gradlew runGameTestServer -PgameTestNamespaces=divinity_pocket_domain --offline --console=plain
./gradlew jarJar --offline --console=plain
```

For client checks, launch the packaged bundle in an isolated QA directory with
the `Core-Smoke` test save and these JVM properties (not your normal game save):

```text
-Ddivinity_engine.codexSmokeTest=true
-Ddivinity_engine.codexSmokeDomainOnly=true
-Ddivinity_engine.codexSmokeAutoExit=true
```

The server suite uses `~/.gradle/workspace-runs/divinity-pocket-domain-gametest`,
not the user's saves. The UI suite covers only domain controls and the changed
pocket inventory, using detached menus without sending gameplay actions. These
checks do not replace a two-player playthrough or a crash-recovery exercise.

### Verification record: 2026-09-24

- Java 17 compilation, all eight isolated domain GameTests, JarJar packaging,
  and reobfuscation passed. Log: `/tmp/divinity-pocket-final.log`.
- The production bundle passed sixteen client-only checks: seven existing
  pocket-inventory checks and nine domain checks covering creation, all eight
  room entries, rules, residents, logical 320x240 layout, and shader fallback.
  Screenshots of creation, compact rules, inventory, and fallback were reviewed.
- The initial capture fixture tried to inspect an unloaded destination entity.
  The passing fixture temporarily loads that test chamber, verifies UUID and
  equipment across both transfers, and releases its test chunk ticket afterward.
  Production rooms are not permanently force-loaded.
- The UI runner restored options/client state and did not send room, rule,
  storage-transfer, or capture requests. Real server-side actions were covered
  separately by the GameTests, not by the synthetic UI fixtures.
- Manual two-player playthrough, arbitrary addon entity compatibility, canceled
  destination-spawn callbacks from other mods, and abrupt crash recovery remain
  unverified. No existing user world was opened or regenerated for these checks.

UI report:
`~/.gradle/workspace-runs/nightshade-production-client-qa/nightshade-production-smoke-bjyz4al9/divinity-codex-smoke/run-15453060925403393949/report.json`

### Verification record: 2026-09-26

- Java 17 compilation, all sixteen focused domain GameTests, JarJar packaging,
  and reobfuscation passed. Server log: `/tmp/divinity-pocket-portals-final.log`.
  Core and Vanguard were also restored to the development runtime classpath;
  dependency versions and bundled artifacts were not upgraded in this change.
- New server coverage exercises numeric size validation, version-1 room defaults,
  saved anchors and room ordering, bounded resizing, chest-content preservation,
  obstruction rejection, stale/foreign requests, close-before-move placement,
  real player entrance/exit and up/down travel, and cross-owner isolation.
- Nineteen packaged client checks passed, covering the existing storage menu,
  size fields, resizing, portal controls, all room entries, logical 320x240 layouts,
  reduced motion, and shader fallback. Compact creation, portals, and fallback
  screenshots were reviewed. These are detached client fixtures, not a gameplay
  playthrough; the server GameTests exercise real transfers separately.
- No existing user world was opened, relocated, or regenerated. The production
  UI runner restored its isolated test bundle and client settings afterward.
- A simultaneous multiplayer playthrough, visual inspection of gateways in a
  live world, and abrupt process termination during resizing remain unverified.
  Persistence was tested through SavedData serialization/reload, not by crashing
  Minecraft. Imported sanctuary geometry remains intentionally non-resizable.

UI report:
`~/.gradle/workspace-runs/nightshade-production-client-qa/nightshade-production-smoke-bjyz4al9/divinity-codex-smoke/run-10752676876472159454/report.json`

### Verification record: 2026-09-26 Shared Travel and Sleeping Rooms

- Java 17 compilation, all 23 focused domain GameTests, JarJar packaging and
  reobfuscation passed. Log: `/tmp/divinity-pocket-shared-final.log`.
- Coverage includes fifteen actual mob transfers beyond the old twelve-resident
  limit and pagination/save round-trips of 1,000 resident records. The latter is
  not a performance test of 1,000 simultaneously loaded mobs.
- Sleeping-room tests unload/reload actual chunks and verify chest contents and
  resident UUIDs. A forced-chunk ticket stands in for an active chunk loader and
  survives the release of Divinity's arrival ticket. No arbitrary addon loader
  block has been integration-tested.
- Cold item/projectile crossings wait for loading without blocking or cloning;
  portal closure cancels a pending crossing and restores source motion. Shared
  visitor return points survive serialization and do not grant owner controls.
  Gateway tests wait for arrival readiness rather than assuming same-tick travel.
- Persistent render handles survive refresh and runtime-cache reset, and closing
  removes them. Client geometry probes verify an anchored portal remains visible
  at 12,000 ticks while a one-shot teleport animation still expires.
- All 21 packaged, domain-focused client checks passed, including the uncapped
  resident total, next-page controls, compact layout and shader fallback.
  Normal/compact resident and portal screenshots were reviewed. These detached
  UI fixtures do not send real management requests or replace multiplayer play.
- The old client QA folder refused jar replacement. The successful run used a
  fresh temporary game directory and a copy of the isolated `Core-Smoke` fixture;
  no existing user world was opened, modified, erased or relocated.
- Remaining manual coverage: simultaneous real-client visitors, addon entities
  and chunk-loader implementations, abrupt shutdown during an in-flight crossing,
  sustained gateway visuals in live play, and a controlled tick-time comparison.

UI report:
`/private/tmp/nightshade-production-smoke-pocket-xAwxUu/divinity-codex-smoke/run-13839368569376548214/report.json`

### Verification record: 2026-09-26 Per-Room Exits

- Java 17 compilation, all 25 focused domain GameTests, JarJar packaging and
  reobfuscation passed. Log: `/tmp/divinity-room-portals-final.log`.
- A three-floor tower has three independent exits and four floor-change gates.
  Server tests exercise player travel through every exit and adjacent-floor link,
  independent closure, custom placement, legacy anchor preservation, save/load,
  foreign-owner rejection and unsafe resizing around a manually placed exit.
- All 24 packaged domain client checks passed. Normal and compact portal-menu
  screenshots were reviewed. Checks cover room selection, first/middle/top-floor
  destinations, closed exits, distinct exit/up/down geometry and shader fallback.
- The client run used a fresh isolated QA copy, restored client state and options,
  and sent no gameplay menu actions. No existing user world was opened or changed.
- Live-world visual inspection and a simultaneous manual multiplayer playthrough
  remain unverified; client fixtures and server GameTests cover them separately,
  not as an end-to-end human playthrough.

UI report:
`/private/tmp/nightshade-production-smoke-pocket-Kv50Kb/divinity-codex-smoke/run-10505718047316847399/report.json`
