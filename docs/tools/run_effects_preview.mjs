#!/usr/bin/env node
/** Packaged effects-only QA. Never builds or opens a source/personal save. */
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import crypto from 'node:crypto';
import { execFileSync, spawn } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { buildArgs as isolatedArgs, parseArgs as communionDefaults } from './run_communion_preview.mjs';

const prefix = 'nightshade-production-smoke-effects-';
export const gods = 'solarius nashara grond varun dravak elaris voltira vokar nythea kairoth cryonis nerai zephra terra lumen mechanos ignar aethon'.split(' ');
export const passes = ['gallery', 'shader-fallback', 'reduced-motion'];
const usage = `Packaged Effects Preview (Java 17, at most 2G, no Gradle)

  node docs/tools/run_effects_preview.mjs --validate-only
  node docs/tools/run_effects_preview.mjs --prepare-only
  node docs/tools/run_effects_preview.mjs

  --fixture PATH    Original synthetic packaged QA fixture under
                    ~/.gradle/workspace-runs/nightshade-production-client-qa.
                    Only its saves/Core-Smoke is eligible; never run/saves.
  --bundle PATH     Fresh packaged Divinity Engine jar with embedded dependencies.
  --validate-only   Read-only checks, no copy, Java process, client, or build.
  --prepare-only    Fresh disposable copy and manifest, no client or build.
  --help           Show help without accessing files.

Launch only after the parent packages the edits and explicitly approves launch.
Run sequentially with Gradle, never alongside it. Launch refuses existing Gradle
JVMs (including idle daemons) and Minecraft clients; it does not stop them. It
shares the UI/communion preview lock. No previous preview directory is reused.

Exactly 54 scenes: 18 gods x gallery/shader-fallback/reduced-motion. Each scene
draws five particle-only recipes using Minecraft sprites through Nightshade.
Checks: all 90 recipes, god/native/ash palettes, actual sprite resources and draw
counts, zero combat mesh, private single-channel replacement/removal/expiry,
natural expiry, shader fallback, reduced-motion options, and options restoration.
The persistent pocket portal has a CPU-only diagnostic, not a gameplay preview.
PNG review is still required; draw counts do not measure framebuffer visibility.

Only first-run onboarding and the exact experimental warning from the harness's
own copied Core-Smoke opener may be accepted. No gameplay commands or generic
smoke harness are enabled. Libraries/assets remain read-only fixture dependencies.
Each run gets a fresh OS-temp ${prefix}* directory, retaining:
  effects-preview-manifest.json, effects-preview-startup.json, client.log,
  launcher-result.json, divinity-effects-smoke/run-*/report.json and screenshots.
Deadline: 12 minutes, TERM then KILL after 15 seconds, only to this child.
`;

export function parseArgs(argv) {
  const defaults = communionDefaults([]);
  const config = { fixture: defaults.fixture, bundle: defaults.bundle, validateOnly: false, prepareOnly: false };
  for (let i = 0; i < argv.length; i++) {
    const arg = argv[i];
    if (arg === '--validate-only') config.validateOnly = true;
    else if (arg === '--prepare-only') config.prepareOnly = true;
    else if (['--fixture', '--bundle'].includes(arg)) {
      if (!argv[i + 1] || argv[i + 1].startsWith('--')) throw new Error(`Missing value for ${arg}`);
      config[arg.slice(2)] = argv[++i];
    } else throw new Error(`Unknown option: ${arg}. Use --help.`);
  }
  if (config.validateOnly && config.prepareOnly) throw new Error('Choose one no-launch mode');
  return config;
}

export function buildArgs(raw, root) {
  // Reuse the reviewed credential/quick-play/JVM/native-path sanitizer, not its harness.
  const args = isolatedArgs(raw, root, '').filter(arg => !arg.startsWith('-Ddivinity_engine.communionSmokeTest')
    && !/^-Dnightshade_core\..*(?:smoke|preview)/i.test(arg));
  const target = args.indexOf('--launchTarget');
  if (target < 0 || args[target + 1] !== 'forgeclient') throw new Error('Expected packaged forgeclient launch target');
  args.unshift('-Ddivinity_engine.effectsSmokeTest=true', '-Ddivinity_engine.effectsSmokeAutoOpen=true',
    '-Ddivinity_engine.effectsSmokeAutoExit=true');
  return args;
}

export function assertSequential(processTable) {
  const busy = processTable.split('\n').filter(line => /(?:org\.gradle\.(?:launcher|wrapper|process)|GradleDaemon|GradleWrapperMain|cpw\.mods\.bootstraplauncher\.BootstrapLauncher|net\.minecraft\.client\.main\.Main)/.test(line));
  if (busy.length) throw new Error('Gradle or Minecraft JVM is present. Wait for the parent to finish/stop it; previews must run sequentially.');
}

export function assertRealTree(source) {
  const stat = fs.lstatSync(source);
  if (stat.isSymbolicLink() || !stat.isFile() && !stat.isDirectory()) throw new Error(`Linked/special fixture path: ${source}`);
  if (stat.isDirectory()) for (const name of fs.readdirSync(source)) assertRealTree(path.join(source, name));
}

function newestMtime(source) {
  const stat = fs.lstatSync(source);
  if (stat.isSymbolicLink()) throw new Error(`Linked source cannot establish bundle freshness: ${source}`);
  return stat.isDirectory() ? Math.max(0, ...fs.readdirSync(source).map(name => newestMtime(path.join(source, name)))) : stat.mtimeMs;
}

export function checkReport(report, root) {
  const errors = [], screenshots = new Set(), ids = new Set();
  const expected = new Set(passes.flatMap(pass => gods.map(god => `${pass}-${god}`)));
  if (report.status !== 'PASS') errors.push(report.detail || 'Harness did not report PASS');
  if (report.schemaVersion !== 2 || report.testKind !== 'synthetic_local_blessing_effects_only'
      || report.plannedScenarios !== 54 || report.remainingScenarios !== 0) errors.push('Wrong/incomplete effects suite');
  for (const flag of ['clientOptionsRestored', 'gameplayMenuUnchanged', 'coreStateEmpty', 'allScenesNaturallyExpired'])
    if (report[flag] !== true) errors.push(`Cleanup not verified: ${flag}`);
  if (report.serverOrWorldMutationsIssued !== false || report.gameplayCallbacksInvoked !== false
      || !Array.isArray(report.cleanupErrors) || report.cleanupErrors.length) errors.push('Gameplay/cleanup guard failed');
  const diagnostic = report.diagnostics || {};
  if (diagnostic.status !== 'PASS' || diagnostic.particleOnlyRecipes !== 90 || diagnostic.combatMeshVertices !== 0
      || diagnostic.qualityCases !== 2700 || diagnostic.allGodPalettesVerified !== true
      || diagnostic.minecraftSpritesVerified !== true || diagnostic.persistentPortalComposerVerified !== true
      || !(diagnostic.persistentPortalVertices > 0) || diagnostic.palettes?.length !== 18
      || !diagnostic.sprites?.length || diagnostic.sprites.some(row => !(row.visibleTexels > 0))) errors.push('Particle/portal diagnostics incomplete');
  const privateCheck = report.privateSnapshot || {};
  if (privateCheck.status !== 'PASS' || privateCheck.particleOnly !== true || privateCheck.handlesAfterPublicReplacement !== 1
      || ['publicReplacementPreservedEmitter', 'individualRemovalVerified', 'naturalExpiryVerified', 'expiredReplayIgnored']
        .some(flag => privateCheck[flag] !== true)) errors.push('Private single-channel preflight incomplete');
  if (!Array.isArray(report.scenarios) || report.scenarios.length !== 54) errors.push('Expected exactly 54 scenes');
  for (const row of report.scenarios || []) {
    if (!expected.delete(row.name) || row.status !== 'PASS' || row.name !== `${row.pass}-${row.god?.replace('divinity_engine:', '')}`
        || row.localHandles !== 5 || row.pixelEmittersVerified !== 5 || row.combatMeshHandles !== 0
        || row.particlesAfterExpiry !== 0 || row.emittersAfterExpiry !== 0 || row.particleBloomPasses !== 0
        || !(row.renderedParticles >= 5) || row.particleVertices !== row.renderedParticles * 4
        || ['coreStateVerified', 'naturalExpiryVerified', 'pixelShaderSelectionVerified', 'minecraftSpritesVisible',
          'godPaletteVerified', 'reducedMotionOptionsVerified'].some(flag => row[flag] !== true)) errors.push(`Incomplete scene: ${row.name}`);
    const reduced = row.pass === 'reduced-motion', fallback = row.pass === 'shader-fallback';
    if (row.animationStrength !== (reduced ? 0 : 1) || row.screenEffectScale !== (reduced ? 0 : 1)
        || row.reducedFlashing !== reduced || row.particleShaderAvailable !== !fallback
        || row.pixelVanillaFallbackSelected !== fallback) errors.push(`Wrong accessibility/shader route: ${row.name}`);
    if (!Array.isArray(row.recipes) || row.recipes.length !== 5 || new Set(row.recipes.map(recipe => recipe.id)).size !== 5
        || new Set(row.recipes.map(recipe => recipe.variant)).size !== 5) errors.push(`Missing recipes: ${row.name}`);
    for (const recipe of row.recipes || []) {
      if (recipe.mesh !== false || recipe.particles !== true || recipe.godPaletteVerified !== true
          || !/^minecraft:textures\/particle\/.+\.png$/.test(recipe.particleSprite || '')
          || !/^divinity_engine:[a-z0-9_]+$/.test(recipe.id || '')) errors.push(`Not a verified particle recipe: ${recipe.id}`);
      ids.add(recipe.id);
    }
    const file = typeof row.screenshot === 'string' ? path.resolve(row.screenshot) : '';
    const inside = path.relative(path.join(root, 'divinity-effects-smoke'), file);
    if (!file || inside.startsWith('..') || path.isAbsolute(inside) || !file.endsWith('.png')
        || !fs.existsSync(file) || fs.realpathSync(file) !== file || screenshots.has(file)) {
      errors.push(`Unsafe/missing/duplicate screenshot: ${row.name}`); continue;
    }
    const bytes = fs.readFileSync(file);
    if (bytes.length <= 24 || !bytes.subarray(0, 8).equals(Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]))
        || bytes.readUInt32BE(16) === 0 || bytes.readUInt32BE(20) === 0) errors.push(`Invalid PNG: ${row.name}`);
    else screenshots.add(file);
  }
  if (expected.size || ids.size !== 90) errors.push('Missing god/pass or 90-recipe coverage');
  return { errors, screenshots: screenshots.size, recipes: ids.size };
}

export async function main(argv) {
  if (argv.includes('--help')) { console.log(usage); return; }
  const config = parseArgs(argv);
  const fixture = fs.realpathSync(config.fixture);
  const qaRoot = fs.realpathSync(path.join(os.homedir(), '.gradle/workspace-runs/nightshade-production-client-qa'));
  if (path.dirname(fixture) !== qaRoot || !path.basename(fixture).startsWith('nightshade-production-smoke-'))
    throw new Error('Only original synthetic packaged QA fixtures are eligible, never a personal Minecraft directory');
  const sourceWorld = path.join(fixture, 'saves/Core-Smoke');
  if (fs.realpathSync(sourceWorld) !== sourceWorld || fs.lstatSync(path.join(fixture, 'saves')).isSymbolicLink()) throw new Error('Linked source world');
  const copies = ['saves/Core-Smoke', 'config', 'defaultconfigs', 'natives', 'options.txt'].filter(name => fs.existsSync(path.join(fixture, name)));
  for (const name of copies) assertRealTree(path.join(fixture, name));
  if (!fs.statSync(path.join(sourceWorld, 'level.dat')).isFile()) throw new Error('Missing synthetic fixture level.dat');
  const bundle = fs.realpathSync(config.bundle);
  if (!bundle.endsWith('.jar') || !fs.statSync(bundle).isFile()) throw new Error('Expected packaged jar');
  const entries = execFileSync('/usr/bin/unzip', ['-Z1', bundle], { encoding: 'utf8', maxBuffer: 8 * 1024 * 1024 }).split('\n');
  if (!entries.includes('META-INF/jarjar/metadata.json') || !entries.includes('net/nightshade/divinity_engine/client/effects/BlessingEffectsSmokeTest.class')
      || !['nightshade_core', 'vanguard'].every(name => entries.some(entry => entry.startsWith('META-INF/jarjar/') && entry.includes(name) && entry.endsWith('.jar'))))
    throw new Error('Expected bundled packaged jar with effects harness, Nightshade Core, and Vanguard');
  const raw = JSON.parse(fs.readFileSync(path.join(fixture, 'launch-command.json'), 'utf8'));
  buildArgs(raw, path.join(os.tmpdir(), `${prefix}validation`));
  const java = raw[0];
  if (!path.isAbsolute(java) || path.basename(java) !== 'java' || !fs.statSync(java).isFile()) throw new Error('Expected absolute Java 17 executable');
  const release = fs.readFileSync(path.resolve(path.dirname(fs.realpathSync(java)), '../release'), 'utf8');
  if (!/^JAVA_VERSION="17(?:[.\-+][^"]*)?"$/m.test(release)) throw new Error('Fixture must use Java 17');
  if (raw[raw.indexOf('--username') + 1] !== 'CoreSmokeQA') throw new Error('Source does not identify the synthetic QA player');
  if (config.validateOnly) {
    console.log(JSON.stringify({ status: 'VALIDATED_NO_LAUNCH', fixture, world: sourceWorld, bundle, java,
      maxHeap: '2G', scenes: 54, recipes: 90, note: 'No copy, Java, client, or build. Freshness and process guards run only at launch.' }, null, 2));
    return;
  }
  const assertLaunchReady = () => {
    const project = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
    if (fs.statSync(bundle).mtimeMs < Math.max(newestMtime(path.join(project, 'src/main/java')), newestMtime(path.join(project, 'src/main/resources'))))
      throw new Error('Packaged jar predates sources/resources. Wait for parent packaging; this launcher never builds.');
    assertSequential(execFileSync('/bin/ps', ['-axo', 'pid=,command='], { encoding: 'utf8', maxBuffer: 8 * 1024 * 1024 }));
  };
  if (!config.prepareOnly) assertLaunchReady();
  const temp = fs.realpathSync(os.tmpdir()), lock = path.join(temp, 'communion-preview.launcher.lock');
  let lockFd, logFd, child, deadline, hardDeadline, interrupted = false;
  const terminate = () => {
    if (child && child.exitCode === null && child.signalCode === null) {
      child.kill('SIGTERM'); hardDeadline ??= setTimeout(() => child.kill('SIGKILL'), 15_000);
    }
  };
  const interrupt = () => { interrupted = true; terminate(); };
  try {
    if (!config.prepareOnly) {
      lockFd = fs.openSync(lock, 'wx', 0o600);
      fs.writeFileSync(lockFd, JSON.stringify({ kind: 'effects-preview', launcherPid: process.pid, startedAt: new Date().toISOString() }));
    }
    const root = fs.realpathSync(fs.mkdtempSync(path.join(temp, prefix)));
    console.log(`Disposable effects preview: ${root}`);
    fs.mkdirSync(path.join(root, 'mods')); fs.mkdirSync(path.join(root, 'natives'));
    for (const name of copies) fs.cpSync(path.join(fixture, name), path.join(root, name), {
      recursive: true, filter: source => {
        const stat = fs.lstatSync(source);
        if (stat.isSymbolicLink() || !stat.isFile() && !stat.isDirectory()) throw new Error(`Linked/special source file: ${source}`);
        return path.basename(source) !== 'session.lock';
      },
    });
    const copiedBundle = path.join(root, 'mods', path.basename(bundle)); fs.copyFileSync(bundle, copiedBundle);
    const args = buildArgs(raw, root);
    const manifest = { kind: 'effects-preview-v1', runDirectory: root, worldName: 'Core-Smoke', worldSource: sourceWorld,
      fixture, bundle, bundleSha256: crypto.createHash('sha256').update(fs.readFileSync(copiedBundle)).digest('hex'),
      plannedScenarios: 54, recipes: 90, java, maxHeap: '2G', preparedAt: new Date().toISOString() };
    fs.writeFileSync(path.join(root, 'effects-preview-manifest.json'), JSON.stringify(manifest, null, 2));
    fs.writeFileSync(path.join(root, 'launch-command.json'), JSON.stringify([java, ...args], null, 2));
    if (config.prepareOnly) { console.log('Prepared only. After packaging/approval, re-run without --prepare-only for a fresh copy.'); return; }
    assertLaunchReady();
    const env = { ...process.env };
    for (const key of ['MOD_CLASSES', 'CLASSPATH', 'JAVA_TOOL_OPTIONS', '_JAVA_OPTIONS', 'JDK_JAVA_OPTIONS']) delete env[key];
    logFd = fs.openSync(path.join(root, 'client.log'), 'w');
    process.on('SIGINT', interrupt); process.on('SIGTERM', interrupt);
    child = spawn(java, args, { cwd: root, env, stdio: ['ignore', logFd, logFd] });
    let timedOut = false;
    deadline = setTimeout(() => { timedOut = true; terminate(); }, 12 * 60_000);
    const result = await new Promise((resolve, reject) => {
      child.once('exit', (exitCode, signal) => resolve({ exitCode, signal })); child.once('error', reject);
    });
    clearTimeout(deadline); clearTimeout(hardDeadline);
    const reports = path.join(root, 'divinity-effects-smoke');
    const runs = fs.existsSync(reports) ? fs.readdirSync(reports).filter(name => name.startsWith('run-')) : [];
    Object.assign(result, { status: 'FAIL', timedOut, interrupted, runDirectory: root });
    if (runs.length === 1 && fs.existsSync(path.join(reports, runs[0], 'report.json'))) {
      result.report = path.join(reports, runs[0], 'report.json');
      Object.assign(result, checkReport(JSON.parse(fs.readFileSync(result.report, 'utf8')), root));
      if (result.exitCode === 0 && !timedOut && !interrupted && result.errors.length === 0) result.status = 'PASS';
    } else result.errors = ['Missing/ambiguous harness report; inspect effects-preview-startup.json and client.log'];
    const resultPath = path.join(root, 'launcher-result.json');
    fs.writeFileSync(resultPath, JSON.stringify(result, null, 2)); console.log(JSON.stringify(result, null, 2));
    console.log(`Launcher result: ${resultPath}`);
    if (result.status !== 'PASS') throw new Error(`Effects preview failed or incomplete; inspect ${resultPath}`);
  } finally {
    process.off('SIGINT', interrupt); process.off('SIGTERM', interrupt);
    clearTimeout(deadline); clearTimeout(hardDeadline);
    if (logFd !== undefined) fs.closeSync(logFd);
    if (lockFd !== undefined) { fs.closeSync(lockFd); fs.unlinkSync(lock); }
  }
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  main(process.argv.slice(2)).catch(error => { console.error(error.message); process.exitCode = 1; });
}
