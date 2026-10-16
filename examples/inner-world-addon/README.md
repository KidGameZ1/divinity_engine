# Inner World Example Addon

This standalone development addon demonstrates a **new god**, automatically registered statue, two-branch story, physical trial, independent realm, favor-responsive visuals, interruption dialogue and safe scene edits. Base Divinity Engine does not ship the example god.

From the Divinity Engine root, run `./gradlew innerWorldAddonJar`. This compiles against the same Java 17 / Forge 1.20.1 public API and creates a reobfuscated `communion-example-1.0.0.jar` in the configured build's `libs` directory. Put it alongside the bundled Divinity Engine jar **in an isolated test installation**; its required Core/Vanguard/player-animation dependencies come from that bundle.

For your own Forge addon, use the ordinary Forge 1.20.1 MDK and compile dependency on Divinity Engine. Copy/adapt the source module, not Divinity's own registry or session engine. The automatic statue API supplies default assets; localization is included here. No extra renderer is required for the realm.

Test flow:

1. Obtain `communion_example:vesper_statue` with the normal creative/operator tools and contact Vesper.
2. Leave the statue, select Vesper in the blessings interface and choose Commune. The example has no extra favor requirement for communion.
3. Her story still requires Favorable favor. Make the accepted physical offering of paper or amethyst, then return to communion for branch preview/confirmation. Donations cannot be made remotely.
4. Complete the physical deed, win the sample five-second vigil trial from her statue, and return to that statue for the once-only ending.
5. Have a zombie attack the exposed body while the conversation is open. Vesper's interruption callback responds without delaying the safe return.

The sample story awards a **lore/specialization marker only**, not a new numerical Celestial Ward modifier. Its text says so explicitly. Production blessing upgrades need their own eligibility-aware effect implementation, as documented in the existing narrative API guide. The example deliberately does not invent a new combat power merely to demonstrate realm registration.

Full contract, defaults, server authority, client presentation extension, save compatibility and restrictions: `docs/GOD_INNER_WORLDS.md` in the parent project.
