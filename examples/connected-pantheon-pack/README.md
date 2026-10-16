# Connected Pantheon Version-Two Pack

Requires Minecraft 1.20.1, Connected Pantheon in Divinity Engine, and the hosting `example_pantheon` addon described in `../connected-pantheon-addon/README.md`. This is **not** a standalone vanilla or base-mod-only datapack: Vesper, her statue, offering rules, trial, and the registered `example_pantheon:star_chart` server handler must exist. Missing custom objective types reject the narrative reload; changing that objective to a built-in type would create a different story, not an equivalent pack.

Install this directory as a pack in a disposable world's `datapacks` directory, then use `/reload`. Its two full `version: 2`, `kind: story` files override the same Java story IDs. The optional relationship overrides the Java relationship with the same ID. Java registration plus these JSON overrides is supported; two different visible JSON files declaring the same story ID are not.

The `assets/example_pantheon/lang/en_us.json` file is the client resource-pack counterpart. Install this directory as a resource pack on clients as well, or package the identical language file supplied in the Java example into the addon. A server datapack by itself does not install client language resources. Reload client resources separately after translation edits. English literals remain supported alongside `{"translate":"..."}` fields.

The two existing blessing references are `divinity_engine:solar_ward` and `divinity_engine:twilight_veil`; custom reward IDs record endings, not new executable effects. This pack contains no scripts, new items, new recipes, structures, runtime code, or automatic cross-god rewards.

See `../../docs/connected-pantheon-authoring.md` for save compatibility, chapter gates, server event safety, commands and the manual test matrix. The existing `living-pantheon-pack` version-one example is intentionally unchanged.
