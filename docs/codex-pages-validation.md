# Codex Page Restoration

Validated on 2026-09-23 with Minecraft 1.20.1, Forge 47.4.2 and Java 17.
The newer Codex UI is retained. Its always-available **Codex Pages** section
reads the entire original `DivineCodexPagesRegistry`, rather than only the
pages after the addon offset. Story sections remain separate and spoiler-gated.

Original and addon pages retain their registration order, page boundaries,
component formatting and next/previous navigation. Repeated and blank titles
do not cause pages to be merged or discarded. Existing page registration APIs
are unchanged. Late appended pages refresh the open screen, and resource or
language changes rebuild the formatted projections.

## Checks

- `./gradlew build --offline --console=plain` passed, including 1,330 narrative
  assertions, connected-content checks, addon compilation and bundle checks.
  Evidence: `/tmp/divinity-codex-pages-final-build.log`.
- The isolated packaged client passed all 48 Codex smoke scenarios and exited
  with code zero. Its temporary journal, options and menu were restored.
- The added checks cover built-in pages before journal sync, original page
  arrows, Known Pantheon search, and a temporary deferred-register addon with
  repeated titles, an untitled page, formatting and overflow content. All 36
  pages in the test registry (25 built-in plus 11 fixture fragments) survived
  projection in order; the fixture pages were removed afterward.
- Original-reader and Known Pantheon screenshots were inspected. Existing
  scale, compact layout, story navigation and resource-reload scenarios passed.
  Evidence: `/tmp/divinity-codex-pages-ui.log`; report and PNGs:
  `~/.gradle/workspace-runs/nightshade-production-client-qa/nightshade-production-smoke-v1_5to3u/divinity-codex-smoke/run-584308588544966540/`.

Bundled jar:
`~/.gradle/workspace-builds/divinity-engine/forge-1.20.1/libs/divinity_engine-1.9-1.20.1.jar`

SHA-256: `545cab40d11224b16e6f97bfe2fa8ad15b53f417f7900ca36842f1082110a83f`

This is client/page presentation validation, not an additional story playthrough.
The prior dedicated-server and 37 GameTest results remain recorded against
their earlier artifact in `connected-pantheon-validation.md`; those server
tests were not rerun for this client-focused follow-up.
