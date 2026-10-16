# Trial recovery and performance

## Runtime behavior

- Arenas prepare before entry. Server-thread jobs perform at most 512 block operations per tick, with a soft 2 ms time budget. Chunk loading is requested without joining its future on the tick thread.
- Completion, failure, logout and arena exits stop the encounter immediately. Online players leave safely before their room is erased. Deletion runs in subsequent slices; the slot cannot be reused until finished.
- `divinity_engine_trial_arenas` SavedData records reservations. On restart, previously placed arenas become cleanup jobs even if their owner never reconnects. Previously managed slots also retain a small tombstone so stale player capabilities cannot erase later terrain.
- An occupied, unowned site is quarantined, not overwritten. No dimension folders or structure template files are deleted. Legacy cleanup is restricted to exact fixed-slot coordinates in the trials dimension.
- Arena protection covers breaking, placement, bucket use, fluid spread, explosions, nearby pistons and mob griefing. Deliberate server-side encounter changes remain possible.
- Trial entities carry a session UUID. Summoned vexes, fangs and owned projectiles inherit it. Entities loaded after their session has ended are discarded; unrelated entities are not swept up.
- Narrative packets use protocol 4. Update clients and servers together. The client retains snapshots received before player creation, rejects deltas missing their baseline, and requests a rate-limited full snapshot. Disconnect clears the replica. Registered Codex pages are unchanged.

## Focused verification

Run with Java 17:

```sh
./gradlew runGameTestServer -PgameTestNamespaces=divinity_trial_recovery narrativeChecks jarJar --offline --console=plain
```

The trial suite uses `~/.gradle/workspace-runs/divinity-trial-recovery-gametest`, not the ordinary run worlds. It checks lifecycle idempotence, phase deadlines, projectile/canceled deaths, independent sessions, room protection, summons, persisted cleanup, stale recovery, journal revisions and incremental counter updates. A separate test exercises cold-chunk preparation over real server ticks.

Final verification passed all 18 focused Forge GameTests and 1,330 narrative assertions across 36 story paths. Java 17 compilation and the reobfuscated bundled JAR build succeeded.

The comparison test deliberately invokes the legacy synchronous deletion API only on an owned disposable fixture. In the September 24 comparison run it took 211.6 ms in one call. The following bounded cleanup took 22.4 ms total across 23 slices, worst slice 4.4 ms. These are local samples, not a guarantee of whole-server TPS. JVM warmup, individual block operations and chunk-ticket processing can exceed the soft time budget.

Slow encounter ticks, arena jobs and full journal builds exceeding 50 ms produce diagnostic log entries. Do not suppress Minecraft's overload warning or change its tick rate to mask stalls.

## Remaining manual coverage

Real connected-client dimension travel, reconnect timing, two-client journal delivery and third-party mod cancellation interactions still need multiplayer playtesting. Serialized restart tests do not simulate power loss between separate chunk and SavedData disk writes. Existing player worlds were not opened by these tests.

## Story capability recovery

A September 25 regression test reproduced the "No story is registered" report
using a real `ServerPlayer` and the production narrative provider. Forge removes
and revives the same player during dimension transfers. Reviving the capability
dispatcher does not revive its already-invalidated `LazyOptional`, so the saved
data remained in the provider but quest/journal code could no longer access it.
Earlier trial fixtures used overridden FakePlayer capabilities and teleports,
which bypassed this lifecycle.

The provider now renews only its access handle when queried after revival, keeping
the same data object. Its invalidation listener targets the provider, not an old
handle, so repeated trips remain safe. Missing player data has a separate error
from genuinely missing story definitions. No story IDs or saved-data formats change.

The focused `divinity_narrative_lifecycle` suite checks repeated invalidation,
death/non-death clone callbacks, actual trial-dimension round trips, failed-trial
retry followed by victory, journal recovery, and the error distinction. Its packet
sink replaces a connected client; teleportation and attached capabilities are real.

Before the fix, all three original lifecycle regressions failed, including loss of
the narrative capability on entry to the trials dimension. After the fix, all four
focused tests passed, together with Java 17 compilation and the bundled JAR build.
