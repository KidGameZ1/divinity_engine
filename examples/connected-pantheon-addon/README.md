# Connected Pantheon Java Example

This is a source integration example for Minecraft 1.20.1 / Forge, not a standalone mod or a replacement for the version-one example. Put these two Java classes in your addon's `example.pantheon.connected` source package and call `ConnectedPantheonAddon.register()` from its mod constructor, before common setup. Do not register `StarChartEvents` again with an automatic subscriber annotation.

The hosting addon must separately register `example_pantheon:vesper`, a matching usable statue, ordinary accepted offerings and a working trial for that god. Suggested offering rules accept vanilla paper and amethyst shards. `offeringExamples` is guidance, not an offering whitelist. Narrative registration stores namespaced IDs and need not resolve the god during construction; player actions need the real registered god at runtime. The base mod owns `NarrativeBootstrap`; do not call it from this addon.

`ConnectedPantheonAddon.java` supplies two complete seven-chapter stories, `example_pantheon:first_star` and `example_pantheon:last_lantern`, for the same god. Each has its own two permanent paths and reward IDs. The optional `example_pantheon:dawn_and_evening` relationship uses the existing `divinity_engine:solarius` story plus `example_pantheon:first_star`. Remove its registration before release if you do not want that optional conversation.

`StarChartEvents.java` registers an actual Forge-bus handler. The custom `example_pantheon:star_chart` type is initialized before common setup. Crafting a vanilla compass while the first story's reckoning is active reports one `craft_compass` completion on the server. There is no custom item or recipe. Acquiring, renaming or merely holding a compass does not qualify. This is intentionally a notification-count objective, not a total-items-crafted counter; shift-crafting and modded menus require integration testing.

The two existing blessing references are `divinity_engine:solar_ward` and `divinity_engine:twilight_veil`. The four `example_pantheon:*` reward IDs are persistent specialization markers, not automatically implemented numeric modifiers or grants of those blessings. The prose makes that limitation explicit. See the authoring guide for server-side reward eligibility and effect ownership.

The sibling `../connected-pantheon-pack` supplies the same stories and relationship as version-two JSON. It **still requires this addon's god, trial and custom objective handler**. Loading both is intentional: the pack overrides the Java defaults by stable story ID. Never install the old Vesper example alongside this module without reconciling its IDs and assumptions.

Full guide: `../../docs/connected-pantheon-authoring.md`. No Gradle build, Forge launch or gameplay test is claimed by this source example.

## Static Validation

From the repository root, run these read-only checks with Node.js and a JDK 17 or newer:

```sh
node examples/connected-pantheon-addon/validate-examples.mjs
java examples/connected-pantheon-addon/ValidateJavaSyntax.java examples/connected-pantheon-addon/ConnectedPantheonAddon.java examples/connected-pantheon-addon/StarChartEvents.java
```

The Node check validates both JSON envelopes, all twenty passage/dialogue pairs, objective data, four distinct reward markers, the two existing blessing references, four relationship combinations, language resources and fixture-specific Java-to-JSON text/argument parity. The JDK helper parses Java 17 syntax only; it does not compile/type-check against Minecraft or emit class files into the workspace. Neither check executes the engine loader, dispatches a Forge event or proves gameplay behavior. Do not include `ValidateJavaSyntax.java` as an addon runtime class; only the two integration classes belong in the mod sources.

Both checks passed for this example on 2026-09-23, including four translation keys and the optional custom chapter title. Compilation and in-game verification remain the main integration run's responsibility; no Gradle command was used here.
