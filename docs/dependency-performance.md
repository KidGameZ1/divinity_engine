# Dependency performance patch

## Vanguard 2.2.2-1.20.1

- Snapshot the queue length before processing a five-player batch. Requeuing no
  longer processes the same player up to five times in a single server tick.
- Replace queued player references after respawn; clear the queue on server stop.
- Skip posture recovery callbacks when posture is already full. Keep existing
  recovery amounts, cancellation, and client-side posture prediction.
- Remove per-hit and per-entity posture console output. Damage processing is
  server-side only.

## Nightshade Core 1.6.1

- Reuse the decoded particle spec for an existing emitter revision. Decode new
  revisions once, rather than decoding every emitter on every tick and then
  decoding changed emitters a second time.
- Index loaded entity UUIDs lazily once per particle simulation tick, instead of
  scanning all loaded entities for each attached emitter. Clear the index every
  tick and when the particle engine resets.
- Keep budgets, simulation timing, shaders, periodic recovery snapshots and
  persistent effects unchanged.

## Checks

The Divinity suite `divinity_dependency_performance` exercises the bundled
Vanguard queue and posture implementation. Core's focused emitter codec and
particle isolation tests cover serialization plus structural checks of decode
and lookup reuse. A live client frame-time benchmark is still required; these
checks do not establish that all gameplay stalls are gone.

No world files, structures, saved abilities, graphics settings or save IDs are
changed by this patch.

## Current delivery status

The source-read blocker was resolved on 2026-10-16 by downloading the missing
iCloud project files without moving the projects or changing any worlds.
Fresh Java 17 checks passed for the patch versions:

- Core 1.6.1: `test testCommonPort jarJar reobfJarJar`; 43 unit tests and the
  separate 25-test common-side suite passed with no failures, errors or skips.
  The common suite overlaps the unit suite; these are not 68 unique tests.
- Vanguard 2.2.2-1.20.1: `compileJava test jarJar reobfJarJar` passed. The test
  task reported `NO-SOURCE`, so this is compile/package evidence, not a unit-test
  or gameplay pass.

Neither patch version has been integrated into Divinity's bundle. Divinity
remains on Core 1.6.0 and Vanguard 2.2.1-1.20.1. These builds do not establish
live performance improvements or replace client/server integration testing.
Before changing the bundle, publish the new dependency artifacts to the chosen
Maven repository, update Divinity's dependency properties, run the focused
integration suite and verify nested JAR metadata. A source push to GitHub is
not a Maven publication or a downloadable mod release.
