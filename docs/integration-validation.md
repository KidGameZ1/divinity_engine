# Bundled Forge Integration Validation

For the current Connected Pantheon artifact and expanded story/Codex checks,
see [Connected Pantheon Validation](connected-pantheon-validation.md).
The hashes below describe the previous integration baseline.

Validated on 2026-09-22 with Minecraft 1.20.1, Forge 47.4.2 and Java 17.

## Release Artifact

`~/.gradle/workspace-builds/divinity-engine/forge-1.20.1/libs/divinity_engine-1.9-1.20.1.jar`

SHA-256: `8972447b9560865df315b9de5b335f4baecc2c7916c6e3e75035cd8da2826ed9`

The main jar embeds Core 1.6.0, Vanguard 2.2.1-1.20.1, Player Animator
1.0.2-rc1+1.20 and MixinExtras. The packaged checks install only the outer
Divinity jar in their isolated `mods` folders.

## Battle Aspect Asset Follow-Up

- Vanguard `runData build publishToMavenLocal`: passed with seven generated
  base aspect models. Divinity `runData build`: passed with twelve addon aspect
  models. Temporary `dataOutputDir` values kept unrelated generated files intact.
- The generated model JSON matches the checked-in resources. All 19 models
  reference the existing Vanguard Battle Aspect texture; item IDs are unchanged.
- The build passed 790 assertions over 36 story paths and verifies the nested
  production jars plus all 19 model parents and the shared PNG texture.
- Fixed the empty-registry item generator and stopped generating a loot table
  for the pocket-dimension block, which explicitly has no loot table.
- Packaged client: passed all seven render surfaces, eight shaders, vanilla
  fallback, resource reload and effect cleanup; exited with code zero. No
  missing-model or missing-texture warnings appeared during loading or reload.
- The first client attempt stalled before entering its world and was stopped.
  The successful retry used a fresh copy of the previous successful QA save;
  no user worlds were used or modified.

Build evidence: `/tmp/vanguard-aspect-models-build.log` and
`/tmp/divinity-aspect-models-build2.log`. Client evidence:
`/tmp/divinity-aspect-models-client2.log`, with report and screenshots under
`~/.gradle/workspace-runs/nightshade-production-client-qa/nightshade-production-smoke-iosx4044/nightshade-client-smoke/run-99b18128-103d-4e4f-a49d-7e2979d6bd8d/`.

## Previous Full Baseline

The checks below passed before the asset follow-up, on artifact SHA-256
`be846f07d36e536220f77036fd64f150da4131ce45c5413589d54ba0e7075406`.
The dedicated-server and 24 GameTest checks have not been rerun for the
asset-only follow-up; they are recorded here as baseline evidence.

- `./gradlew build runGameTestServer --offline --console=plain`: successful
  build, 790 story-state assertions over 36 paths, addon example compilation,
  and all 24 required Forge GameTests. The explicit test completion summary
  was checked; an exit code alone is insufficient for Forge startup failures.
- `verifyBundledMods`: all three embedded mod IDs, versions, refmaps, Java 17
  bytecode and SHA-256 equality to their raw production Maven artifacts.
- Production client: entered a world, joined its integrated server, exercised
  all seven Core rendering surfaces and eight shaders, forced vanilla fallback,
  reloaded resources, and verified zero remaining particles/emitters on cleanup.
  Shader and fallback screenshots were inspected. Client exited successfully.
- Production dedicated server: reached `Done`, reloaded data/resources and
  saved all dimensions on a clean `stop`. No client-class loading failure.
- Bundle launcher: all 17 Python unit tests passed.

Local evidence: `/tmp/divinity-bundled-build5.log`,
`/tmp/divinity-bundled-client1.log`, `/tmp/divinity-bundled-server1.log`.
Client report and screenshots are under
`~/.gradle/workspace-runs/nightshade-production-client-qa/nightshade-production-smoke-d9mozv92/nightshade-client-smoke/run-4a2caef8-8136-4299-80d7-e97e8ed0c5d2/`.

## Remaining Limits

The dedicated server logged a Yggdrasil public-key request timeout. Startup,
reload and shutdown passed, but authenticated remote joining was not tested.
There were startup/tick lag warnings on this local machine; this is not a
performance benchmark. Connected multiplayer, full manual story/trial
playthroughs and arbitrary third-party modpacks remain separate playtests.

Integration fixes include side-isolated Vanguard packet application, one-time
packet registration, server-owned back-slot toggling, client-only Codex
pagination and removal of duplicate pre-commit worship callback subscribers.
