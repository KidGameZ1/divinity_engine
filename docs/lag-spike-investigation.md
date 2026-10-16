# Intermittent lag investigation

## Evidence

The September 24 live development run produced overload warnings after login,
including 2.169 seconds behind at 23:35:14. This was not limited to hotbar changes.

A 40-second Java Flight Recorder sample captured another warning. Server samples
were spread across chunk ticking, movement, pathfinding and mod callbacks, rather
than one long-running trial cleanup. No Java garbage-collection event occurred in
that recording. Reported rolling average tick times rose above 200 ms during the
sample; this is not an isolated before/after benchmark.

The 8 GiB Mac was actively swapping memory: two one-second system samples showed
6,431 and 7,600 swap-in pages, with 16 KiB pages (about 100 and 119 MiB). Swap usage
was about 11.4 GiB. These are system-wide figures, not Minecraft-only memory usage.
Profiling and development tools also add overhead; the data does not prove that
memory pressure is the sole cause of every stall.

## Narrow Code Changes

- Nearby shrine lookup uses existing loaded-chunk block-entity indexes instead of
  inspecting a 13-by-13-by-13 cube for each shrine callback. It does not request
  chunk loads or create block entities. Six-block range, god filtering and the
  previous tie-breaking order are preserved. No long-lived cache can go stale.
- Contacted-god and curse lookup use their existing registry-ID maps rather than
  parallel searches on the shared worker pool. Curse lookup uses the curse registry.
- Focused shrine tests compare the indexed search to the previous terrain scan,
  including removals, ties, god filtering and movement to unloaded terrain. They
  also check registered god/curse identity and existing shrine menu restrictions.

Java 17 compilation and all six focused `divinity_shrine_ui` Forge GameTests
passed on September 25. Tests used the isolated narrative GameTest directory,
not an existing player world.

## Retesting

Restart the development client after rebuilding. Keep unused applications closed
when comparing runs so system swapping does not overwhelm the comparison. Do not
increase Java's heap indiscriminately on this machine. Graphics settings, world
files, favor, story progress and Minecraft's 20-TPS target are unchanged.

These changes remove observed avoidable work; they are not proof that all random
lag is fixed. A new recording during ordinary play is needed if spikes persist.

## September 26 Follow-up

The latest client log contains stalls of 20-39 seconds, along with arena jobs
taking hundreds of milliseconds even for zero or one block operation. Minecraft
was already closed when inspected, so no live game recording could be captured.
System swap usage was about 9.5 GiB on the 8 GiB Mac. Two observations during the
investigation showed both swap-in and swap-out counters continuing to increase.
These are system-wide observations, not attribution of individual game stalls.

Two avoidable synchronization costs were found and corrected:

- The changes-only packet reset a god's dirty flag, then re-added its already
  stored blessings. Re-adding marked that god dirty again, causing unchanged gods
  to be serialized in subsequent packets. Serialization now resets flags without
  changing the stored collection.
- Each equipped slot independently sent an update every two ticks. Routine slot
  updates now share one packet per two-tick pass. Ability execution and cooldown
  cadence are unchanged; direct interaction updates still send immediately.
- Dirty-god, curse, and blessing filtering no longer dispatches tiny searches to
  the common worker pool from the game thread.

Focused regression suite: `divinity_god_sync`. It checks packet contents,
dirty-flag clearing, subsequent changes, unchanged serialized save data, and one
packet for three slots with the same cooldown decrement. This is not a measured
before/after tick-time benchmark. Existing worlds and runtime settings are not
modified.

## September 26 Pocket Travel Trace

A 90-second, read-only Flight Recorder capture from the running development
client recorded a portal-travel warning of 16,641 ms / 332 ticks behind at
19:20:36. Earlier warnings included a 15,130 ms room-build stall and 10,519 ms on
exit. No gameplay data was edited for the recording. Local artifacts:
`/tmp/divinity-lag-20260926.jfr` and `/tmp/divinity-lag-samples.json`.

Of 558 sampled server-thread execution events, prominent stacks involved chunk
palette serialization, heightmap reconstruction, chunk generation scheduling and
chunk save/unload work. The recording contained 49 GC pauses totaling about
3.003 seconds, with the largest about 278 ms; this is not evidence of one ten-second
GC pause. System swapping continued, but system-wide counters do not identify
the process responsible. Sampling does not prove a single cause for every stall.

### Targeted Corrections

- Minecraft 1.20.1's `ServerChunkCache.getChunkFuture` managed-blocks when invoked
  on the server thread. Both room construction and arrival preparation were
  calling it as if it were nonblocking. Pocket work now adds bounded FULL-only
  tickets and polls `getChunkNow` on later ticks instead. Collision/portal safety
  queries skip unloaded chunks rather than loading them during the scan.
- Temporary build/arrival tickets load only the necessary chunks, expire, and
  release by their own type and owner. They do not remove chunk-loader or admin
  tickets. No player or loader means normal saving/unloading, not room deletion.
- Enclosed pocket terrain uses a capped four-chunk view and three-chunk simulation
  distance, respecting lower server settings. Other dimensions are unchanged.
- Gateways reuse tagged, indefinite Core render handles while visible. The client
  composer now exempts these anchors from the short-spell lifetime cap. Ordinary
  spell effects still expire. Closing, visibility cleanup, and restart recovery
  remove/reconcile only our tagged effects, without erasing portal anchors.
- Shared entity gateway queries are local to already loaded portals, round-robin
  bounded to sixteen gates and four transfer attempts per tick. Cold crossings
  wait without a blocking chunk call. Resident maintenance checks loaded entities
  once per second, not the unlimited saved resident population. Menus send twelve
  resident records per page, with stable UUID release actions.

This is not a controlled before/after benchmark. The changes remove specific
blocking calls and unnecessary loading, but cannot promise the absence of spikes
from world saves, chunk loaders, high resident populations, other mods or memory
pressure. No tick-rate change, warning suppression, heap increase, world erasure
or relocation is part of this change. Core/Vanguard dependency versions remain
unchanged; the fixes here are in their Divinity integration, not new library jars.
