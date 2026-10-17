# Divinity Engine

Forge **1.20.1**, Java **17**. This wiki describes the current source update,
not necessarily the most recent release on a mod hosting site.

Divinity Engine adds 18 gods, favor, equipped blessings, curses, protected trials,
connected stories, permanent blessing specializations, a Divine Codex, and Aethon's
personal pocket domain.

## Choose Your Starting Point

- [Player Guide](Player-Guide.md): contact, favor, blessing slots, stories and trials.
- [Developer Guide](Developer-Guide.md): choose JSON, KubeJS or a Java addon.
- [JSON Content](JSON-Content.md): create a god, its statue, blessings and a trial.
- [KubeJS](KubeJS.md): startup declarations and server-side custom blessing behavior.
- [Java API](Java-API.md): existing registries, instance state, stories and Codex pages.
- [Effects and UI](Effects-and-UI.md): Nightshade integration and safe visual lifecycles.
- [Compatibility and Testing](Compatibility-and-Testing.md): save IDs, dependencies and focused checks.
- [Pantheon](Pantheon.md): the 18 gods and their connected stories.

The normal gameplay path uses statues, menus and the Codex. Admin commands are
not prerequisites for progression.

## Three Projects, Different Jobs

| Project | Role |
| --- | --- |
| Divinity Engine | God/blessing/trial/story gameplay and its menus |
| Nightshade Core / RenderLib | Shared services, render effects, particles, rendering rules and UI infrastructure |
| Vanguard | Equipment, combat and aspect integration used by Divinity |

Divinity's full JarJar includes the required Forge builds of Core, Vanguard and
Player Animator. KubeJS is optional and is **not bundled**. Developers must use
the Forge 1.20.1 APIs, not similarly named APIs from newer NeoForge branches.

### Library Developer Guides

- [Nightshade Core / RenderLib for Forge 1.20.1](https://github.com/KidGameZ1/NightshadesUtilities/tree/codex/forge-1.20.1/docs/wiki): rendering, particles, UI, networking and lifecycle examples.
- [Vanguard for Forge 1.20.1](https://github.com/KidGameZ1/Vanguard-Forge-1.20.1/tree/main/docs/wiki): combat integration, aspects, equipment and side-safe APIs.

These two repositories are private, so their guides require repository access.
Their Markdown guides live in the repositories because GitHub's wiki feature is
unavailable for them under the current account plan. A newer library source
version is not automatically the version bundled by Divinity; check
[Compatibility and Testing](Compatibility-and-Testing.md) before choosing dependencies.
