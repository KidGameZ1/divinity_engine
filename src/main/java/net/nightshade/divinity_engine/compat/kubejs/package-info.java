/**
 * Optional Forge 1.20.1 / KubeJS 2001 integration, discovered through
 * {@code kubejs.plugins.txt}. Core Divinity classes must not reference this package.
 * No mod entry point, event-bus subscriber, reflection bridge, or client class is needed.
 *
 * <h2>Startup declarations</h2>
 * In {@code kubejs/startup_scripts}, use {@code DivinityEvents.register(event => { ... })}
 * with {@code event.god(id, spec)}, {@code event.blessing(id, spec)}, or
 * {@code event.trial(id, spec)}. IDs must include a namespace; specs are objects in the
 * Divinity JSON content schema, not JSON strings. Specs are copied before registration.
 *
 * <p>KubeJS 2001 calls plugin {@code init}, {@code registerEvents}, startup-script
 * loading, then {@code initStartup} inside its Forge constructor. This plugin posts
 * the declaration event in {@code initStartup}, before Forge's first RegisterEvent.
 * The parent API declares deferred entries as soon as its mod bus is available,
 * retaining earlier declarations until Divinity attaches that bus. The first
 * RegisterEvent closes further declarations;
 * common setup validates references and installs trials. Declaration changes require
 * a full restart on both sides: script reloads do not re-post this event, and retained event objects
 * cannot register additional content after it returns.
 *
 * <h2>Server behavior</h2>
 * In {@code kubejs/server_scripts}, use
 * {@code DivinityEvents.blessing('pack:my_blessing', event => { ... })}.
 * Inspect {@code event.getHook()} to distinguish {@code pressed}, {@code tick},
 * {@code damage_entity}, {@code taken_damage}, {@code kill_entity},
 * {@code toggle_on}, and {@code toggle_off}. The wrapper exposes the parent context's
 * owner, optional target/instance/native event, ID, server level, and success flag.
 * Call {@code event.markSuccessful()} synchronously when an action succeeds; callback
 * returns and KubeJS {@code success()}/{@code cancel()} do not set that flag or cancel
 * the native Forge event. These are non-cancellable KubeJS events.
 *
 * <p>The parent dispatches after JSON actions, even for an empty action list, and
 * checks eligibility/cooldown before dispatch. Existing success is preserved. Only
 * server scripts may listen, and dispatch is restricted to the server thread. KubeJS
 * owns listener storage and replacement on server-script reload; no JS functions or
 * per-world callbacks are cached here. Scripts remain ordinary trusted KubeJS scripts,
 * not a sandbox limited to the JSON action vocabulary.
 *
 * <h2>Build contract</h2>
 * Compile-only ForgeGradle-deobfuscated dependencies: KubeJS
 * {@code dev.latvian.mods:kubejs-forge:2001.6.5-build.26}, Rhino
 * {@code dev.latvian.mods:rhino-forge:2001.2.2-build.17}, and Architectury
 * {@code dev.architectury:architectury-forge:9.1.12}. Declare all three explicitly.
 * Repositories: {@code https://maven.latvian.dev/releases} and
 * {@code https://maven.architectury.dev}. Do not bundle these optional mods.
 *
 * @see <a href="https://github.com/KubeJS-Mods/KubeJS/blob/2001/common/src/main/java/dev/latvian/mods/kubejs/KubeJS.java">KubeJS 2001 lifecycle</a>
 * @see <a href="https://github.com/KubeJS-Mods/KubeJS/blob/2001/common/src/main/java/dev/latvian/mods/kubejs/util/KubeJSPlugins.java">KubeJS plugin discovery</a>
 * @see <a href="https://github.com/MinecraftForge/MinecraftForge/blob/1.20.1/src/main/java/net/minecraftforge/common/ForgeStatesProvider.java">Forge 1.20.1 loading order</a>
 */
package net.nightshade.divinity_engine.compat.kubejs;
