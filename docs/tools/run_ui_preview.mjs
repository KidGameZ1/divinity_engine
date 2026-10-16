#!/usr/bin/env node
/** Packaged UI QA only. No builds, gameplay commands, personal saves, or source-world opening. */
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import crypto from 'node:crypto';
import { spawn } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { buildArgs as isolatedArgs, parseArgs as communionDefaults } from './run_communion_preview.mjs';

const prefix = 'nightshade-production-smoke-ui-';
const usage = `Packaged UI Preview

  node docs/tools/run_ui_preview.mjs --validate-only
  node docs/tools/run_ui_preview.mjs --prepare-only
  node docs/tools/run_ui_preview.mjs

  --fixture PATH    Original synthetic packaged QA fixture (default: existing
                    nightshade-production-smoke-bjyz4al9). Only its Core-Smoke save
                    under ~/.gradle/workspace-runs/nightshade-production-client-qa.
  --bundle PATH     Fresh packaged jar with embedded dependencies.
  --suite NAME      all (default), full, or header. all runs two clients SEQUENTIALLY,
                    each in its own fresh OS-temp copy, never the source world.
  --validate-only   Read-only safety and argument checks. No copies, Java, or build.
  --prepare-only    Copy fixtures and write manifests without launching clients.
  --help           Show help without accessing files.

Launch only after the owner has packaged the UI edits and approved launch.
This script never runs Gradle and refuses a jar older than current sources/resources.

full: existing Codex navigation/search/pagination/reload, blessings/overflow,
statue, pocket inventory/domain/rules/residents checks, plus two detached communion
fixtures (offer and branch actions), each at desktop and compact sizes.
header: existing relationship/proof gating, long text, all-god reachability,
normal/statue views, and shrine conversation fixtures, including action paging.
No functionality checks are skipped to accommodate compact layouts. Decorative
engraving presence is not a functionality requirement. PNGs need visual review.

Only synthetic client tags/detached menus are used; no story action, equip, trial,
room construction, resident release or communion entry is submitted. Existing
local pagination controls are exercised. The real-entry communion smoke remains
separate. First-run onboarding and the exact experimental warning belonging to
the harness's own copied-world opener are the only automated startup prompts.

Each child has a 2G heap and 12-minute deadline (TERM, then KILL after 15 seconds,
only to the child created here). All files and screenshots are retained.
Reports: printed batch report.json, per-copy ui-preview-startup.json, client.log,
and divinity-codex-smoke/run-*/report.json plus screenshots/*.png.
`;

export function parseArgs(argv) {
  const defaults = communionDefaults([]);
  const config = { fixture: defaults.fixture, bundle: defaults.bundle, suite: 'all', prepareOnly: false, validateOnly: false };
  for (let i = 0; i < argv.length; i++) {
    const arg = argv[i];
    if (arg === '--prepare-only') config.prepareOnly = true;
    else if (arg === '--validate-only') config.validateOnly = true;
    else if (['--fixture', '--bundle', '--suite'].includes(arg)) {
      if (!argv[i + 1] || argv[i + 1].startsWith('--')) throw new Error(`Missing value for ${arg}`);
      config[arg.slice(2)] = argv[++i];
    } else throw new Error(`Unknown option: ${arg}. Use --help.`);
  }
  if (!['all', 'full', 'header'].includes(config.suite)) throw new Error('Suite must be all, full, or header');
  if (config.prepareOnly && config.validateOnly) throw new Error('Choose one no-launch mode');
  return config;
}

export function buildArgs(raw, root, suite) {
  if (!['full', 'header'].includes(suite)) throw new Error('One concrete suite is required per client');
  // Reuse the reviewed credential, quick-play, native-path and JVM sanitization.
  const args = isolatedArgs(raw, root, '').filter(arg => !arg.startsWith('-Ddivinity_engine.communionSmokeTest'));
  args.unshift('-Ddivinity_engine.codexSmokeTest=true', '-Ddivinity_engine.codexSmokeAutoOpen=true',
    '-Ddivinity_engine.codexSmokeAutoExit=true', `-Ddivinity_engine.codexSmokeBlessingsHeaderOnly=${suite === 'header'}`);
  return args;
}

function realTree(source) {
  const stat = fs.lstatSync(source);
  if (stat.isSymbolicLink() || !stat.isFile() && !stat.isDirectory()) throw new Error(`Linked/special fixture path is not eligible: ${source}`);
  if (stat.isDirectory()) for (const name of fs.readdirSync(source)) realTree(path.join(source, name));
}

function newestMtime(source) {
  const stat = fs.lstatSync(source);
  if (stat.isSymbolicLink()) throw new Error(`Linked source cannot establish bundle freshness: ${source}`);
  return stat.isDirectory() ? Math.max(0, ...fs.readdirSync(source).map(name => newestMtime(path.join(source, name)))) : stat.mtimeMs;
}

const required = {
  full: ['contents-scale-2', 'nightshade-blessings-all-gods', 'shrine-solarius', 'nightshade-pocket-slots',
    'domain-eight-chambers', 'domain-rules', 'domain-residents', 'communion-offer-desktop', 'communion-choices-desktop',
    'communion-offer-compact', 'communion-choices-compact'],
  header: ['blessings-header-setup', 'blessings-header-galaxy-2', 'blessings-header-statue-2',
    'shrine-conversation-choices-desktop', 'shrine-conversation-confirmation-compact', 'shrine-conversation-epilogues-compact'],
};

export function checkReport(report, root, suite) {
  const errors = [];
  if (report.status !== 'PASS') errors.push(report.detail || 'Harness did not report PASS');
  if (!Array.isArray(report.scenarios) || report.scenarios.length !== report.plannedScenarios
      || report.scenarios.some(row => row.status !== 'PASS') || report.remainingScenarios !== 0) errors.push('Incomplete scenario coverage');
  for (const flag of ['clientJournalRestored', 'clientOptionsRestored', 'gameplayMenuUnchanged'])
    if (report[flag] !== true) errors.push(`Cleanup not verified: ${flag}`);
  const captured = new Set();
  for (const row of report.scenarios || []) if (row.screenshot) {
    const file = path.resolve(row.screenshot);
    const inside = path.relative(path.join(root, 'divinity-codex-smoke'), file);
    if (inside.startsWith('..') || path.isAbsolute(inside) || !file.endsWith('.png')
        || !fs.existsSync(file) || fs.realpathSync(file) !== file) { errors.push(`Unsafe/missing screenshot: ${row.name}`); continue; }
    const bytes = fs.readFileSync(file);
    if (bytes.length <= 24 || !bytes.subarray(0, 8).equals(Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]))
        || bytes.readUInt32BE(16) === 0 || bytes.readUInt32BE(20) === 0) errors.push(`Invalid PNG: ${row.name}`);
    else captured.add(row.name);
  }
  for (const name of required[suite]) if (!captured.has(name)) errors.push(`Required screenshot not captured: ${name}`);
  return { errors, screenshots: captured.size };
}

export async function main(argv) {
  if (argv.includes('--help')) { console.log(usage); return; }
  const config = parseArgs(argv);
  const fixture = fs.realpathSync(config.fixture);
  const qaRoot = fs.realpathSync(path.join(os.homedir(), '.gradle/workspace-runs/nightshade-production-client-qa'));
  if (path.dirname(fixture) !== qaRoot || !path.basename(fixture).startsWith('nightshade-production-smoke-'))
    throw new Error('Only the original synthetic packaged QA fixtures are eligible, never a normal Minecraft directory');
  const sourceWorld = path.join(fixture, 'saves/Core-Smoke');
  if (fs.realpathSync(sourceWorld) !== sourceWorld || fs.lstatSync(path.join(fixture, 'saves')).isSymbolicLink()) throw new Error('Linked source world');
  const copies = ['saves/Core-Smoke', 'config', 'defaultconfigs', 'natives', 'options.txt'].filter(name => fs.existsSync(path.join(fixture, name)));
  for (const name of copies) realTree(path.join(fixture, name));
  if (!fs.statSync(path.join(sourceWorld, 'level.dat')).isFile()) throw new Error('Missing synthetic fixture level.dat');
  const bundle = fs.realpathSync(config.bundle);
  if (!bundle.endsWith('.jar') || !fs.statSync(bundle).isFile()) throw new Error('Bundle must be a packaged jar');
  const raw = JSON.parse(fs.readFileSync(path.join(fixture, 'launch-command.json'), 'utf8'));
  const suites = config.suite === 'all' ? ['full', 'header'] : [config.suite];
  for (const suite of suites) buildArgs(raw, path.join(os.tmpdir(), `${prefix}validation`), suite);
  const java = raw[0];
  if (!path.isAbsolute(java) || !fs.statSync(java).isFile() || path.basename(java) !== 'java') throw new Error('Expected absolute fixture Java executable');
  if (raw[raw.indexOf('--username') + 1] !== 'CoreSmokeQA') throw new Error('Source does not identify the synthetic QA player');
  if (config.validateOnly) {
    console.log(JSON.stringify({ status: 'VALIDATED_NO_LAUNCH', fixture, world: sourceWorld, bundle, suites,
      requiredScreenshots: suites.flatMap(suite => required[suite]), note: 'No client, copy, or build. Bundle freshness checked only at launch.' }, null, 2));
    return;
  }
  if (!config.prepareOnly) {
    const project = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
    if (fs.statSync(bundle).mtimeMs < Math.max(newestMtime(path.join(project, 'src/main/java')), newestMtime(path.join(project, 'src/main/resources'))))
      throw new Error('Packaged jar predates current sources/resources. Wait for owner packaging; this script never builds.');
  }

  const temp = fs.realpathSync(os.tmpdir());
  // Share the communion launch lock: these previews must not compete for a client/heap.
  const lock = path.join(temp, 'communion-preview.launcher.lock');
  let lockFd, child, deadline, hardDeadline, interrupted = false;
  const stopChild = () => {
    if (child && child.exitCode === null && child.signalCode === null) {
      child.kill('SIGTERM'); hardDeadline ??= setTimeout(() => child.kill('SIGKILL'), 15_000);
    }
  };
  const interrupt = () => { interrupted = true; stopChild(); };
  try {
    if (!config.prepareOnly) {
      lockFd = fs.openSync(lock, 'wx', 0o600);
      fs.writeFileSync(lockFd, JSON.stringify({ kind: 'ui-preview', launcherPid: process.pid, startedAt: new Date().toISOString() }));
    }
    const batch = fs.realpathSync(fs.mkdtempSync(path.join(temp, 'nightshade-ui-preview-')));
    const result = { status: config.prepareOnly ? 'PREPARED_NO_LAUNCH' : 'RUNNING', fixture, bundle, suites: [] };
    const batchReport = path.join(batch, 'report.json');
    const write = () => fs.writeFileSync(batchReport, JSON.stringify(result, null, 2));
    console.log(`UI preview batch: ${batchReport}`); write();
    process.on('SIGINT', interrupt); process.on('SIGTERM', interrupt);
    for (const suite of suites) {
      if (interrupted) break;
      const root = fs.realpathSync(fs.mkdtempSync(path.join(temp, prefix)));
      const row = { suite, runDirectory: root, status: 'PREPARED_NO_LAUNCH' }; result.suites.push(row);
      fs.mkdirSync(path.join(root, 'mods')); fs.mkdirSync(path.join(root, 'natives'));
      for (const name of copies) fs.cpSync(path.join(fixture, name), path.join(root, name), {
        recursive: true, filter: source => {
          if (fs.lstatSync(source).isSymbolicLink()) throw new Error(`Linked source file: ${source}`);
          return path.basename(source) !== 'session.lock';
        },
      });
      const copiedBundle = path.join(root, 'mods', path.basename(bundle)); fs.copyFileSync(bundle, copiedBundle);
      const args = buildArgs(raw, root, suite);
      const manifest = { kind: 'ui-preview-v1', runDirectory: root, worldName: 'Core-Smoke', worldSource: sourceWorld,
        fixture, bundle, bundleSha256: crypto.createHash('sha256').update(fs.readFileSync(copiedBundle)).digest('hex'),
        suite, maxHeap: '2G', preparedAt: new Date().toISOString() };
      fs.writeFileSync(path.join(root, 'ui-preview-manifest.json'), JSON.stringify(manifest, null, 2));
      fs.writeFileSync(path.join(root, 'launch-command.json'), JSON.stringify([java, ...args], null, 2));
      console.log(`${suite}: ${root}`); write();
      if (config.prepareOnly) continue;
      const env = { ...process.env };
      for (const key of ['MOD_CLASSES', 'CLASSPATH', 'JAVA_TOOL_OPTIONS', '_JAVA_OPTIONS', 'JDK_JAVA_OPTIONS']) delete env[key];
      const logFd = fs.openSync(path.join(root, 'client.log'), 'w');
      try {
        row.status = 'RUNNING'; write();
        child = spawn(java, args, { cwd: root, env, stdio: ['ignore', logFd, logFd] });
        deadline = setTimeout(() => { row.timedOut = true; stopChild(); }, 12 * 60_000);
        Object.assign(row, await new Promise((resolve, reject) => {
          child.once('exit', (exitCode, signal) => resolve({ exitCode, signal })); child.once('error', reject);
        }));
      } finally {
        clearTimeout(deadline); clearTimeout(hardDeadline); deadline = hardDeadline = undefined;
        fs.closeSync(logFd); child = undefined;
      }
      const reports = path.join(root, 'divinity-codex-smoke');
      const runs = fs.existsSync(reports) ? fs.readdirSync(reports).filter(name => name.startsWith('run-')) : [];
      row.status = 'FAIL';
      if (runs.length === 1 && fs.existsSync(path.join(reports, runs[0], 'report.json'))) {
        row.report = path.join(reports, runs[0], 'report.json');
        const report = JSON.parse(fs.readFileSync(row.report, 'utf8'));
        Object.assign(row, checkReport(report, root, suite));
        if (row.exitCode === 0 && !row.timedOut && !interrupted && row.errors.length === 0) row.status = 'PASS';
      } else row.errors = ['Missing/ambiguous harness report; inspect ui-preview-startup.json and client.log'];
      console.log(JSON.stringify(row, null, 2)); write();
    }
    if (!config.prepareOnly) result.status = !interrupted && result.suites.length === suites.length && result.suites.every(row => row.status === 'PASS') ? 'PASS' : 'FAIL';
    write(); console.log(`Batch report: ${batchReport}`);
    if (config.prepareOnly) console.log('Prepared only. After packaging/approval, re-run without --prepare-only for fresh copies.');
    else if (result.status !== 'PASS') throw new Error(`UI preview failed or incomplete; inspect ${batchReport}`);
  } finally {
    process.off('SIGINT', interrupt); process.off('SIGTERM', interrupt);
    clearTimeout(deadline); clearTimeout(hardDeadline);
    if (lockFd !== undefined) { fs.closeSync(lockFd); fs.unlinkSync(lock); }
  }
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  main(process.argv.slice(2)).catch(error => { console.error(error.message); process.exitCode = 1; });
}
