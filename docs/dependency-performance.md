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

Source edits and regression tests are present, but the new dependency builds
have not been verified or integrated into Divinity's bundle. Publication attempts
were stopped while compilation spent minutes reading source files. A captured
Vanguard thread dump showed javac blocked in FileDispatcherImpl.read0 while
loading source; open-file inspection showed it moving slowly between sources.
Nightshade compilation showed the same slow source-read symptom.

Divinity remains on Core 1.6.0 and Vanguard 2.2.1-1.20.1 until both new artifacts
are built and tested. No live improvement should be inferred from these source
changes yet. Resume by building/publishing the dependency patch versions, then
updating Divinity's dependency properties, running the focused integration suite,
and verifying the nested JAR metadata.
