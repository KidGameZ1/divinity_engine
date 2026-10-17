# JSON Content Example

This example adds Aster, Guiding Breath, an automatic statue, and a two-phase trial.
Copy this directory's `config/divinity_engine/content` into the same config path
on both client and server. Install `resourcepack` as a client resource pack for
names and descriptions. Fully restart both sides; `/reload` cannot add Forge
registry entries.

This variant uses fixed JSON actions and does not require KubeJS. For programmed
conditions and behavior, use [JSON + KubeJS](../json-kubejs-content/README.md).
Do not install multiple Aster examples at the same time: they use the same IDs.
Keep definitions installed when reopening saves that use them.
Test in a new disposable world first.

See [the schema](../../docs/wiki/JSON-Content.md) for supported behaviors and bounds.
