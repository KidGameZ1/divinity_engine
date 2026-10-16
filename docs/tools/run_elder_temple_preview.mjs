#!/usr/bin/env node
/**
 * Isolated packaged-client preview, never a build or a normal run/saves launch.
 * Usage:
 *   node docs/tools/run_elder_temple_preview.mjs --help
 *   node docs/tools/run_elder_temple_preview.mjs --prepare-only
 *   node docs/tools/run_elder_temple_preview.mjs --natural-loaded
 *
 * The first two commands do not start Minecraft. Parent runs one preview client
 * after packaging the mod. Reports and PNGs remain in the disposable directory.
 */
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import crypto from 'node:crypto';
import { spawn } from 'node:child_process';

throw new Error('Archived harness: Elder gameplay was retired. Templates remain only for legacy saves; use divinity_elder_retirement.');

const usage = `Elder Temple Packaged Preview

node docs/tools/run_elder_temple_preview.mjs [options]

  --fixture PATH     Existing isolated packaged QA fixture containing launch-command.json
                     and saves/Core-Smoke (default: known Nightshade packaged QA fixture).
  --bundle PATH      Fresh packaged Divinity Engine jar with embedded dependencies.
  --world NAME       Core-Smoke (default) or Elder-Temple-QA inside that QA fixture.
  --natural-loaded   After each gallery, inspect at most 169 already-loaded chunks for
                     that dimension's Elder structure. Capture one if found; no locate
                     search, discovery chunk generation, or off-thread world reads.
  --prepare-only     Copy the fixture and write the reviewed launch manifest; do not launch.
  --help             Show this help without touching any file.

Required JVM opt-in: -Ddivinity_engine.elderTemplePreview=true
Launcher also sets .autoExit=true and optionally .naturalLoaded=true.
Heap is capped at 2G. Only one instance of this launcher can run at once.

Nine required real-render PNGs: exterior (NW), main interior, and north-facing
statue for Aureth/Overworld, Malgrath/Nether, and Vaelith/End. Template markers
come from root NBT divinity_temple: entrance[3], statue[3], worship[5].
Expected template sizes: 31x21x31, 33x19x29, 29x23x33 respectively.

The launcher copies only an existing isolated QA save, config, options, and natives
into a fresh OS temp elder-temple-preview-* directory. It never opens the source
save, normal run/saves, or any user world. The preview automatically opens its
approved copy via WorldOpenFlows. It places pads and unrotated templates, disables
mob spawning/daylight/weather cycling, sets noon, and moves a spectator camera
ONLY in that disposable copy. It restores client options and exits normally.
First-run accessibility onboarding and the exact experimental-world warning
from this harness's own world-open call are acknowledged in the disposable copy.
No datapack failure, downgrade, customized-world, cache-erasure, or unrelated
confirmation is accepted. Startup screen titles/buttons and diagnostic PNGs
are recorded under openingScreens in report.json; diagnostics do not count
toward the nine gallery captures.

Review elder-temple-preview/report.json and elder-temple-preview/screenshots/*.png
inside the printed run directory. PASS covers the nine gallery captures, not
natural-generation coverage. naturalCoverage and naturalSamples report that
separately; a flat fixture may have no natural starts. Inspect PNGs manually for
occlusion, statue orientation, lighting and composition. No gameplay/admin
commands, fixture progression, production networking, or build tasks are changed.
The 12-minute launcher deadline sends TERM, then KILL after 15 seconds, only to
the child it created. No temp saves or screenshots are deleted afterward.
`;

const argv = process.argv.slice(2);
if (argv.includes('--help')) { console.log(usage); process.exit(0); }
const home = os.homedir();
const config = {
  fixture: path.join(home, '.gradle/workspace-runs/nightshade-production-client-qa/nightshade-production-smoke-bjyz4al9'),
  bundle: path.join(home, '.gradle/workspace-builds/divinity-engine/forge-1.20.1/libs/divinity_engine-1.9-1.20.1.jar'),
  world: 'Core-Smoke', natural: false, prepareOnly: false,
};
for (let i = 0; i < argv.length; i++) {
  const flag = argv[i];
  if (flag === '--natural-loaded') config.natural = true;
  else if (flag === '--prepare-only') config.prepareOnly = true;
  else if (['--fixture', '--bundle', '--world'].includes(flag)) {
    if (!argv[i + 1] || argv[i + 1].startsWith('--')) throw new Error(`Missing value for ${flag}`);
    config[flag.slice(2)] = argv[++i];
  } else throw new Error(`Unknown option: ${flag}. Use --help.`);
}
if (!['Core-Smoke', 'Elder-Temple-QA'].includes(config.world)) throw new Error('Unapproved fixture world name');
const fixture = fs.realpathSync(config.fixture);
if (!path.basename(fixture).startsWith('nightshade-production-smoke-') && !path.basename(fixture).startsWith('elder-temple-preview-'))
  throw new Error('Source must be an isolated QA fixture, never a regular Minecraft directory');
const sourceWorld = fs.realpathSync(path.join(fixture, 'saves', config.world));
if (sourceWorld !== path.join(fixture, 'saves', config.world)) throw new Error('Source world resolves through a symlink');
if (!fs.statSync(path.join(sourceWorld, 'level.dat')).isFile()) throw new Error('Missing fixture level.dat');
const bundle = fs.realpathSync(config.bundle);
if (!bundle.endsWith('.jar') || !fs.statSync(bundle).isFile()) throw new Error('Bundle must be a packaged jar');
const raw = JSON.parse(fs.readFileSync(path.join(fixture, 'launch-command.json'), 'utf8'));
if (!Array.isArray(raw) || raw.some(value => typeof value !== 'string') || raw.length < 4) throw new Error('Invalid launch-command.json');
const java = raw[0];
if (!path.isAbsolute(java) || !fs.statSync(java).isFile()) throw new Error('Fixture must specify its absolute Java 17 executable');

// Reject symlinks recursively rather than copying links to any source-world region files.
const safeFilter = source => {
  if (fs.lstatSync(source).isSymbolicLink()) throw new Error(`Linked source file is not eligible: ${source}`);
  return path.basename(source) !== 'session.lock';
};
const copy = (source, target) => fs.cpSync(source, target, { recursive: true, filter: safeFilter });
const temp = fs.realpathSync(os.tmpdir());
const lock = path.join(temp, 'elder-temple-preview.launcher.lock');
let lockFd, child, root, logFd, deadline, hardDeadline;
try {
  if (!config.prepareOnly) {
    lockFd = fs.openSync(lock, 'wx', 0o600);
    fs.writeFileSync(lockFd, JSON.stringify({ launcherPid: process.pid, startedAt: new Date().toISOString() }));
  }
  root = fs.realpathSync(fs.mkdtempSync(path.join(temp, 'elder-temple-preview-')));
  console.log(`Disposable preview: ${root}`);
  fs.mkdirSync(path.join(root, 'mods')); fs.mkdirSync(path.join(root, 'natives'));
  copy(sourceWorld, path.join(root, 'saves', config.world));
  for (const folder of ['config', 'natives']) if (fs.existsSync(path.join(fixture, folder))) copy(path.join(fixture, folder), path.join(root, folder));
  if (fs.existsSync(path.join(fixture, 'options.txt'))) copy(path.join(fixture, 'options.txt'), path.join(root, 'options.txt'));
  fs.copyFileSync(bundle, path.join(root, 'mods', path.basename(bundle)));

  const args = [];
  const paired = new Set(['--gameDir', '--width', '--height', '--quickPlayPath', '--quickPlaySingleplayer', '--quickPlayMultiplayer', '--quickPlayRealms']);
  for (let i = 1; i < raw.length; i++) {
    const arg = raw[i];
    if (paired.has(arg)) { if (++i >= raw.length) throw new Error(`Missing launch argument value for ${arg}`); continue; }
    if (/^-D(?:divinity_engine\.|nightshade_core\.clientSmoke)/.test(arg)
        || /^-Xm[sx]/.test(arg) || /^-XX:(?:MaxHeapSize|InitialHeapSize|MaxRAM|MaxRAMPercentage|InitialRAMPercentage)=/.test(arg)) continue;
    const nativeKey = ['java.library.path', 'jna.tmpdir', 'org.lwjgl.system.SharedLibraryExtractPath', 'io.netty.native.workdir'].find(key => arg.startsWith(`-D${key}=`));
    args.push(nativeKey ? `-D${nativeKey}=${path.join(root, 'natives')}` : arg);
  }
  args.unshift('-Xmx2G', '-Xms512M', '-Ddivinity_engine.elderTemplePreview=true',
    '-Ddivinity_engine.elderTemplePreview.autoExit=true', `-Ddivinity_engine.elderTemplePreview.naturalLoaded=${config.natural}`);
  args.push('--gameDir', root, '--width', '1440', '--height', '900');
  const manifest = {
    kind: 'elder-temple-preview-v1', runDirectory: root, worldName: config.world,
    worldSource: sourceWorld, fixture, bundle, bundleSha256: crypto.createHash('sha256').update(fs.readFileSync(bundle)).digest('hex'),
    naturalLoadedOnly: config.natural, maxHeap: '2G', preparedAt: new Date().toISOString(),
  };
  fs.writeFileSync(path.join(root, 'elder-temple-preview-manifest.json'), JSON.stringify(manifest, null, 2));
  fs.writeFileSync(path.join(root, 'launch-command.json'), JSON.stringify([java, ...args], null, 2));
  if (config.prepareOnly) {
    console.log('Prepared only; no client started. Re-run without --prepare-only to create and launch a fresh disposable copy.');
  } else {
    const env = { ...process.env };
    for (const key of ['MOD_CLASSES', 'CLASSPATH', 'JAVA_TOOL_OPTIONS', '_JAVA_OPTIONS', 'JDK_JAVA_OPTIONS']) delete env[key];
    const log = path.join(root, 'client.log'); logFd = fs.openSync(log, 'w');
    console.log(`Client log: ${log}`);
    child = spawn(java, args, { cwd: root, env, stdio: ['ignore', logFd, logFd] });
    let timedOut = false;
    deadline = setTimeout(() => {
      timedOut = true; child.kill('SIGTERM');
      hardDeadline = setTimeout(() => child.kill('SIGKILL'), 15_000);
    }, 12 * 60_000);
    const code = await new Promise((resolve, reject) => { child.once('exit', resolve); child.once('error', reject); });
    clearTimeout(deadline); clearTimeout(hardDeadline);
    const reportPath = path.join(root, 'elder-temple-preview/report.json');
    const report = fs.existsSync(reportPath) ? JSON.parse(fs.readFileSync(reportPath, 'utf8')) : { status: 'MISSING_REPORT' };
    fs.writeFileSync(path.join(root, 'launcher-result.json'), JSON.stringify({ exitCode: code, timedOut, report: reportPath, status: report.status }, null, 2));
    console.log(`Report: ${reportPath}`);
    console.log(JSON.stringify({ status: report.status, galleryScreenshots: report.galleryScreenshots, naturalCoverage: report.naturalCoverage, naturalSamples: report.naturalSamples }, null, 2));
    if (timedOut || code !== 0 || report.status !== 'PASS') throw new Error(`Preview failed; inspect ${log} and ${reportPath}`);
  }
} finally {
  clearTimeout(deadline); clearTimeout(hardDeadline);
  if (logFd !== undefined) fs.closeSync(logFd);
  if (lockFd !== undefined) { fs.closeSync(lockFd); fs.unlinkSync(lock); }
}
