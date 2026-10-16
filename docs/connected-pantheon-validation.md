# Connected Pantheon Validation

The subsequent [Codex Page Restoration](codex-pages-validation.md) keeps the
new UI while restoring all registered pages. It records the newer jar and its
48 client checks; the artifact and server results below are the story baseline.

Validated on 2026-09-23 for Minecraft 1.20.1, Forge 47.4.2 and Java 17.
This record supersedes the earlier story-count and artifact results in
`integration-validation.md`, without replacing that historical evidence.

## Delivered Content

- All 18 original stories now have seven chapters, two permanent paths and their
  original branch deeds, reward IDs, balance and once-only 25-favor finale.
- The content checks count 180 passages, 180 short dialogue lines, 36 new
  witness/reckoning objectives, nine optional relationships and 36 paired
  epilogues covering every branch combination.
- Chronicles contain 30,961 words, with 1,660-1,771 words per god. Paired
  epilogues add 7,106 words. `pantheon-history.md` distinguishes shared facts
  from individual beliefs, concealments and incomplete memories.
- The per-player Codex has five searchable sections, chapter/entry navigation,
  pagination, back navigation, start instructions and spoiler-gated discoveries.
- Java builders, version-one compatibility, version-two packs, custom objective
  registration, multiple stories per god and optional crossovers share the
  existing deferred registry and validated model. See
  `connected-pantheon-authoring.md` and `../examples/connected-pantheon-addon/`.

## Artifact

Bundled production jar:

`~/.gradle/workspace-builds/divinity-engine/forge-1.20.1/libs/divinity_engine-1.9-1.20.1.jar`

SHA-256: `2f70d103d7dd29aeb18e2e10e65666b46de75abd215b2f905551317f67b5ac77`

The outer jar includes Nightshade Core 1.6.0, Vanguard 2.2.1-1.20.1,
Player Animator 1.0.2-rc1+1.20 and MixinExtras. Packaged smoke runs install only
the outer Divinity jar, not separate copies of those dependencies.

## Build And Server Checks

`./gradlew build runGameTestServer --offline --console=plain` passed.
Evidence: `/tmp/divinity-connected-migration2.log`.

- Reobfuscated build and bundle verification passed, including embedded mod
  versions, Java 17 bytecode, refmaps, artifact checksums and 19 aspect models.
- Narrative state checks passed 1,330 assertions over 36 paths.
- Connected-content checks validated all 18 arcs, all 36 ending contracts,
  all nine relationships, all four combinations per relationship and the
  two version-two example stories with their optional relationship.
- The original and connected Java addon examples compile against the real API.
  `node examples/connected-pantheon-addon/validate-examples.mjs` also passed
  Java/JSON fixture parity, reward-marker, translation and crossover checks.
- All 37 required Forge GameTests passed; the explicit completion summary was
  checked, not only Gradle's exit code. Coverage includes chapter transitions,
  one-time rewards, all legacy stages, completed-save migration, frozen accepted
  definitions, missing custom handlers, multiple stories, fake-player isolation,
  canceled/duplicate/late events, continuous challenges, discovery/spoiler gates,
  crossovers, specialization behavior and existing trial cleanup/retry paths.
- Malformed-resource tests deliberately log rejection errors. They verify the
  entire last valid catalog survives bad syntax, duplicate fields, invalid roots
  and invalid model data. Those expected test errors are not startup failures.
- The packaged dedicated server reached `Done`, accepted `reload`, loaded its
  resources without narrative rejection, then saved every dimension and exited
  with code zero on `stop`. No client-class loading failure occurred. Evidence:
  `/tmp/divinity-connected-server.log`. Only the isolated
  `~/.gradle/workspace-runs/nightshade-core-packaged-server/` world was used.

## Packaged Rendering Integration

The final jar passed the isolated production Core client smoke run with all
seven render surfaces and all eight shaders active. Forced vanilla fallback,
resource reload, restored shader operation and cleanup passed; zero particles
and emitters remained afterward. Shader and fallback PNGs were inspected.
The client exited with code zero.

Evidence: `/tmp/divinity-connected-client-final.log`; report and PNGs:
`~/.gradle/workspace-runs/nightshade-production-client-qa/nightshade-production-smoke-cq0c6i7l/nightshade-client-smoke/run-8d708141-365b-4413-b400-325686ca5216/`.
Forge logged a slow GLFW initialization warning at ERROR level, but startup,
rendering and the explicit QA completion all succeeded.

## Codex Rendering Checks

The opt-in `CodexSmokeTest` and `tools/run-codex-smoke.mjs` passed 44 scenarios
in an isolated production client using synthetic client journal data. No
server story progress, favor, shrine action or reward was fabricated by this
UI test. It restored the client journal, options and gameplay menu, then the
client exited with code zero.

- Actual GUI scales 2, 3 and 4 were tested at logical window sizes 1280x741,
  854x494 and 640x371. A separate logical 320x240 resize exercised compact mode;
  this was not a physical-window resize.
- Waiting for sync, contents, global search, reading, long headings, chapter
  navigation, page navigation, back history and objective progress were checked.
- A real resource reload passed. Live content changes and same-revision counter
  updates refreshed the reader. Screenshots of contents, objective progress,
  long prose and the minimum logical layout were visually inspected for wrapping
  and overlap.
- Final-artifact evidence: `/tmp/divinity-codex-ui-final.log`; report and PNGs:
  `~/.gradle/workspace-runs/nightshade-production-client-qa/nightshade-production-smoke-cq0c6i7l/divinity-codex-smoke/run-4748261450437633773/`.
  Its installed jar's SHA-256 matches the release artifact above. All 44
  scenarios passed, with zero remaining and all restoration flags true.
- An earlier identical-screen run also passed; contents, objective progress,
  long prose and compact screenshots from that run were inspected. Evidence:
  `/tmp/divinity-codex-ui2.log`, under QA directory `nightshade-production-smoke-2m939q4l`.
  The final-artifact long-reader screenshot was additionally inspected.

## How To Start

Contact a god through its statue and reach 50 favor (Favorable). Reopen that
statue's blessings screen and select **Quests > Begin quest** to hear
the request. **Speak about quest** opens previews and permanent confirmation.
Make an accepted offering directly in front of the statue. The
Codex reveals that god after first contact and explains accepted examples,
current objectives, explicit donations, prayer and statue-only trial access.
It cannot remotely confirm a branch or turn in a shrine objective.

## Remaining Manual Coverage

- Full human playthroughs of Solarius, Nythea and their paired epilogue have not
  been performed. Automated progression and UI fixtures are not substitutes.
- Connected remote multiplayer, authenticated joining and real reconnect
  packet delivery need playtesting; fake-player isolation and capability
  lifecycle checks passed in the local server tests.
- Actual language switching, alternate font/resource packs and physical window
  resizing still need visual QA. Localization serialization and resource reload
  are tested, but the content currently ships in English.
- The addon examples compile and their pack data validate, but a separately
  packaged host addon with its own god, statue and trial was not played end to
  end. Arbitrary third-party modpacks are not certified.
- Deprecation warnings remain in the Forge build. Local startup/tick lag and a
  LAN advertisement warning do not establish production performance or remote
  connectivity. Existing personal worlds were not used for packaged checks.
