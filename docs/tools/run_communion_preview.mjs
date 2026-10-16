#!/usr/bin/env node
/** Disposable packaged-client void QA. Never builds, opens a source save, or uses run/saves. */
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import crypto from 'node:crypto';
import { spawn } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const property = 'divinity_engine.communionSmokeTest';
const prefix = 'nightshade-production-smoke-communion-';
const usage = `Communion Void Preview (packaged client, no Gradle)

  node docs/tools/run_communion_preview.mjs --validate-only --god aethon
  node docs/tools/run_communion_preview.mjs --prepare-only --god aethon
  node docs/tools/run_communion_preview.mjs --god aethon

  --fixture PATH    Synthetic nightshade-production-smoke-* fixture under
                    ~/.gradle/workspace-runs/nightshade-production-client-qa.
                    Must contain launch-command.json and saves/Core-Smoke.
  --bundle PATH     Fresh packaged Divinity Engine jar, with embedded dependencies.
  --god ID          Optional built-in god filter (aethon or divinity_engine:aethon).
                    Omit to visit all registered built-in voids.
  --validate-only   Read-only fixture/argument checks; no copy, Java process or build.
  --prepare-only    Create disposable copy and manifest, but never start Minecraft.
  --help           Show help without accessing the fixture.

Wait for the owner to build before launching. Launch mode refuses a jar older than
the Java sources/resources. Each invocation creates a fresh OS-temp ${prefix}*
directory. It never reuses a previous run or opens the source save. Only Core-Smoke
is allowed; there is no arbitrary world or multiplayer option. Symlinks in copied
trees are rejected. Libraries/assets remain read-only fixture dependencies.

The manifest-guarded harness auto-opens its own Core-Smoke copy, seeds contact,
sets survival/peaceful, and builds a small source pad only there. It uses real
communion entry/return and leaves production sky/fog/particle behavior unchanged.
Only first-run accessibility onboarding and the exact experimental-world warning
created synchronously by its own WorldOpenFlows call may be accepted. Datapack,
downgrade, customized-world, upgrade/cache and unrelated prompts are never accepted.

Expected per god: offer, dialogue and HUD-hidden realm PNGs, with real render/write
completion, Air/Barrier scene, manifestation identity/position/name and return checks.
Review communion-smoke/report.json and communion-smoke/screenshots/*.png manually.
Aethon checks the shared void path, not every statue model or combat/multiplayer.
Name assertion failures retain captures, return normally, then report FAIL.

Heap cap: 2G. One communion launcher at a time. The 12-minute deadline sends TERM,
then KILL after 15 seconds, only to the child it created. Artifacts are retained.
`;

export function parseArgs(argv) {
  const config = {
    fixture: path.join(os.homedir(), '.gradle/workspace-runs/nightshade-production-client-qa/nightshade-production-smoke-bjyz4al9'),
    bundle: path.join(os.homedir(), '.gradle/workspace-builds/divinity-engine/forge-1.20.1/libs/divinity_engine-1.9-1.20.1.jar'),
    god: '', prepareOnly: false, validateOnly: false,
  };
  for (let i = 0; i < argv.length; i++) {
    const flag = argv[i];
    if (flag === '--prepare-only') config.prepareOnly = true;
    else if (flag === '--validate-only') config.validateOnly = true;
    else if (['--fixture', '--bundle', '--god'].includes(flag)) {
      if (!argv[i + 1] || argv[i + 1].startsWith('--')) throw new Error(`Missing value for ${flag}`);
      config[flag.slice(2)] = argv[++i];
    } else throw new Error(`Unknown option: ${flag}. Use --help.`);
  }
  if (config.god) {
    if (!config.god.includes(':')) config.god = `divinity_engine:${config.god}`;
    if (!/^divinity_engine:[a-z0-9_]+$/.test(config.god)) throw new Error('Invalid built-in god ID');
    if (['aureth', 'malgrath', 'vaelith'].some(god => config.god === `divinity_engine:${god}`)) throw new Error('Removed Elder gods are not eligible');
  }
  if (config.prepareOnly && config.validateOnly) throw new Error('Choose one no-launch mode');
  return config;
}

export function buildArgs(raw, root, god) {
  if (!Array.isArray(raw) || raw.length < 4 || raw.some(value => typeof value !== 'string')) throw new Error('Invalid launch-command.json');
  if (raw.filter(arg => arg === 'cpw.mods.bootstraplauncher.BootstrapLauncher').length !== 1) throw new Error('Expected packaged Forge bootstrap');
  const paired = new Set(['--gameDir', '--width', '--height', '--quickPlayPath', '--quickPlaySingleplayer', '--quickPlayMultiplayer', '--quickPlayRealms',
    '--server', '--port', '--username', '--uuid', '--accessToken', '--clientId', '--xuid', '--userType']);
  const args = [];
  for (let i = 1; i < raw.length; i++) {
    const arg = raw[i];
    if (paired.has(arg)) {
      if (!raw[++i] || raw[i].startsWith('--')) throw new Error(`Missing launch argument value for ${arg}`);
      continue;
    }
    if ([...paired].some(flag => arg.startsWith(`${flag}=`))) continue;
    if (/^-D(?:divinity_engine\.|nightshade_core\.clientSmoke)/.test(arg)
        || /^-Xm[sx]/.test(arg) || /^-XX:(?:MaxHeapSize|InitialHeapSize|MaxRAM|MaxRAMPercentage|InitialRAMPercentage)=/.test(arg)) continue;
    if (arg.startsWith('@') || /^-(?:javaagent|agentlib|agentpath):/.test(arg)) throw new Error('Unexpected argument file or agent in fixture launcher');
    const nativeKey = ['java.library.path', 'jna.tmpdir', 'org.lwjgl.system.SharedLibraryExtractPath', 'io.netty.native.workdir']
      .find(key => arg.startsWith(`-D${key}=`));
    args.push(nativeKey ? `-D${nativeKey}=${path.join(root, 'natives')}` : arg);
  }
  args.unshift('-Xmx2G', '-Xms512M', `-D${property}=true`, `-D${property}.autoOpen=true`, `-D${property}.god=${god}`);
  args.push('--gameDir', root, '--width', '1440', '--height', '900', '--username', 'CoreSmokeQA',
    '--uuid', 'd082db50f54c37ca8955f0c11d697ca0', '--accessToken', '0', '--clientId', '0', '--xuid', '0', '--userType', 'legacy');
  return args;
}

function assertRealTree(source) {
  const stat = fs.lstatSync(source);
  if (stat.isSymbolicLink()) throw new Error(`Linked fixture path is not eligible: ${source}`);
  if (!stat.isDirectory() && !stat.isFile()) throw new Error(`Special fixture file is not eligible: ${source}`);
  if (stat.isDirectory()) for (const entry of fs.readdirSync(source)) assertRealTree(path.join(source, entry));
}

function newestMtime(source) {
  const stat = fs.lstatSync(source);
  if (stat.isSymbolicLink()) throw new Error(`Linked source cannot establish bundle freshness: ${source}`);
  return stat.isDirectory() ? Math.max(0, ...fs.readdirSync(source).map(name => newestMtime(path.join(source, name)))) : stat.mtimeMs;
}

export async function main(argv) {
  if (argv.includes('--help')) { console.log(usage); return; }
  const config = parseArgs(argv);
  const fixture = fs.realpathSync(config.fixture);
  const qaRoot = fs.realpathSync(path.join(os.homedir(), '.gradle/workspace-runs/nightshade-production-client-qa'));
  if (path.dirname(fixture) !== qaRoot || !path.basename(fixture).startsWith('nightshade-production-smoke-'))
    throw new Error('Source must be a synthetic packaged QA fixture, never a regular Minecraft directory');
  const sourceWorld = path.join(fixture, 'saves/Core-Smoke');
  if (fs.realpathSync(sourceWorld) !== sourceWorld || fs.lstatSync(path.join(fixture, 'saves')).isSymbolicLink()) throw new Error('Linked source world');
  const copies = ['saves/Core-Smoke', 'config', 'defaultconfigs', 'natives', 'options.txt'].filter(name => fs.existsSync(path.join(fixture, name)));
  for (const name of copies) assertRealTree(path.join(fixture, name));
  if (!fs.statSync(path.join(sourceWorld, 'level.dat')).isFile()) throw new Error('Missing fixture level.dat');
  const bundle = fs.realpathSync(config.bundle);
  if (!bundle.endsWith('.jar') || !fs.statSync(bundle).isFile()) throw new Error('Bundle must be a packaged jar');
  const raw = JSON.parse(fs.readFileSync(path.join(fixture, 'launch-command.json'), 'utf8'));
  buildArgs(raw, path.join(os.tmpdir(), `${prefix}validation`), config.god);
  const java = raw[0];
  if (!path.isAbsolute(java) || !fs.statSync(java).isFile() || path.basename(java) !== 'java') throw new Error('Expected absolute fixture Java executable');
  if (raw[raw.indexOf('--username') + 1] !== 'CoreSmokeQA') throw new Error('Source does not identify the synthetic QA player');
  if (config.validateOnly) {
    console.log(JSON.stringify({ status: 'VALIDATED_NO_LAUNCH', fixture, world: sourceWorld, bundle, godFilter: config.god || 'all',
      note: 'No build or client run. Bundle freshness is checked only at launch.' }, null, 2));
    return;
  }
  if (!config.prepareOnly) {
    const project = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
    const newest = Math.max(newestMtime(path.join(project, 'src/main/java')), newestMtime(path.join(project, 'src/main/resources')));
    if (fs.statSync(bundle).mtimeMs < newest) throw new Error('Packaged jar predates current sources/resources. Wait for the owner to build; this script never builds.');
  }

  const temp = fs.realpathSync(os.tmpdir()), lock = path.join(temp, 'communion-preview.launcher.lock');
  let lockFd, logFd, deadline, hardDeadline, child;
  const terminate = () => {
    if (!child || child.exitCode !== null || child.signalCode !== null) return;
    child.kill('SIGTERM');
    hardDeadline ??= setTimeout(() => child.kill('SIGKILL'), 15_000);
  };
  try {
    if (!config.prepareOnly) {
      lockFd = fs.openSync(lock, 'wx', 0o600);
      fs.writeFileSync(lockFd, JSON.stringify({ launcherPid: process.pid, startedAt: new Date().toISOString() }));
    }
    const root = fs.realpathSync(fs.mkdtempSync(path.join(temp, prefix)));
    console.log(`Disposable preview: ${root}`);
    fs.mkdirSync(path.join(root, 'mods')); fs.mkdirSync(path.join(root, 'natives'));
    for (const name of copies) fs.cpSync(path.join(fixture, name), path.join(root, name), {
      recursive: true, filter: source => {
        if (fs.lstatSync(source).isSymbolicLink()) throw new Error(`Linked source file: ${source}`);
        return path.basename(source) !== 'session.lock';
      },
    });
    const copiedBundle = path.join(root, 'mods', path.basename(bundle));
    fs.copyFileSync(bundle, copiedBundle);
    const args = buildArgs(raw, root, config.god);
    const manifest = { kind: 'communion-preview-v1', runDirectory: root, worldName: 'Core-Smoke', worldSource: sourceWorld,
      fixture, bundle, bundleSha256: crypto.createHash('sha256').update(fs.readFileSync(copiedBundle)).digest('hex'),
      godFilter: config.god, maxHeap: '2G', preparedAt: new Date().toISOString() };
    fs.writeFileSync(path.join(root, 'communion-preview-manifest.json'), JSON.stringify(manifest, null, 2));
    fs.writeFileSync(path.join(root, 'launch-command.json'), JSON.stringify([java, ...args], null, 2));
    if (config.prepareOnly) {
      console.log('Prepared only; no client started. After building, re-run without --prepare-only for a fresh copy.'); return;
    }
    const env = { ...process.env };
    for (const key of ['MOD_CLASSES', 'CLASSPATH', 'JAVA_TOOL_OPTIONS', '_JAVA_OPTIONS', 'JDK_JAVA_OPTIONS']) delete env[key];
    const log = path.join(root, 'client.log'); logFd = fs.openSync(log, 'w');
    console.log(`Client log: ${log}`);
    child = spawn(java, args, { cwd: root, env, stdio: ['ignore', logFd, logFd] });
    process.on('SIGINT', terminate); process.on('SIGTERM', terminate);
    let timedOut = false;
    deadline = setTimeout(() => { timedOut = true; terminate(); }, 12 * 60_000);
    const result = await new Promise((resolve, reject) => {
      child.once('exit', (code, signal) => resolve({ code, signal })); child.once('error', reject);
    });
    clearTimeout(deadline); clearTimeout(hardDeadline);
    const reportPath = path.join(root, 'communion-smoke/report.json');
    const report = fs.existsSync(reportPath) ? JSON.parse(fs.readFileSync(reportPath, 'utf8')) : { status: 'MISSING_REPORT' };
    fs.writeFileSync(path.join(root, 'launcher-result.json'), JSON.stringify({ ...result, timedOut, status: report.status, report: reportPath }, null, 2));
    console.log(`Report: ${reportPath}`);
    console.log(JSON.stringify({ status: report.status, realmsVisited: report.realmsVisited, captures: report.captures?.length, failure: report.failure }, null, 2));
    if (timedOut || result.code !== 0 || report.status !== 'PASS') throw new Error(`Preview failed; inspect ${log} and ${reportPath}`);
  } finally {
    process.off('SIGINT', terminate); process.off('SIGTERM', terminate);
    clearTimeout(deadline); clearTimeout(hardDeadline);
    if (logFd !== undefined) fs.closeSync(logFd);
    if (lockFd !== undefined) { fs.closeSync(lockFd); fs.unlinkSync(lock); }
  }
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  main(process.argv.slice(2)).catch(error => { console.error(error.message); process.exitCode = 1; });
}
