// Parent-run only, after rebuilding the packaged JarJar artifact. Never builds or reuses a world.
// Usage: node docs/tools/run_elder_temple_natural.mjs [--jar /absolute/rebuilt.jar]
// Exit: 0 clean observations, 2 terrain/content findings, 1 infrastructure/probe failure.
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import crypto from 'node:crypto';
import {fileURLToPath} from 'node:url';
import {spawn, execFileSync} from 'node:child_process';

throw new Error('Archived harness: Elder generation was retired. Use the divinity_elder_retirement GameTests instead.');

const repo = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const fixture = path.join(os.homedir(), '.gradle/workspace-runs/nightshade-core-packaged-server');
const java = '/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home/bin/java';
const args = process.argv.slice(2);
if (args.length && !(args.length === 2 && args[0] === '--jar' && path.isAbsolute(args[1]))) {
  throw new Error('Usage: node docs/tools/run_elder_temple_natural.mjs [--jar /absolute/rebuilt.jar]');
}
const bundle = fs.realpathSync(args[1] ?? path.join(os.homedir(),
  '.gradle/workspace-builds/divinity-engine/forge-1.20.1/libs/divinity_engine-1.9-1.20.1.jar'));
const forgeArgs = 'libraries/net/minecraftforge/forge/1.20.1-47.4.2/unix_args.txt';
const levelName = 'Elder-Temple-Natural-QA';
const seed = 424242;
const reportName = 'elder-temple-natural-report.json';

function newestRuntimeInput(directory) {
  let newest = {mtime: 0, file: ''};
  for (const entry of fs.readdirSync(directory, {withFileTypes: true})) {
    if (entry.name.startsWith('.')) continue;
    const file = path.join(directory, entry.name);
    if (entry.isSymbolicLink()) throw new Error(`Refusing symlinked runtime input: ${file}`);
    if (entry.isDirectory()) {
      const child = newestRuntimeInput(file);
      if (child.mtime > newest.mtime) newest = child;
    } else if (entry.isFile() && entry.name !== '.DS_Store') {
      const mtime = fs.statSync(file).mtimeMs;
      if (mtime > newest.mtime) newest = {mtime, file};
    }
  }
  return newest;
}

if (!fs.statSync(bundle).isFile() || !bundle.endsWith('.jar')) throw new Error('Expected a rebuilt packaged jar');
const newest = ['src/main/java', 'src/main/resources', 'src/generated/resources']
  .map(relative => path.join(repo, relative)).filter(fs.existsSync).map(newestRuntimeInput)
  .sort((a, b) => b.mtime - a.mtime)[0];
if (fs.statSync(bundle).mtimeMs < newest.mtime) {
  throw new Error(`Packaged jar is stale relative to ${newest.file}. Parent must rebuild before this probe.`);
}
const entries = execFileSync('/usr/bin/unzip', ['-Z1', bundle], {encoding: 'utf8', maxBuffer: 16 * 1024 * 1024}).split('\n');
for (const required of ['net/nightshade/divinity_engine/gametest/ElderTempleNaturalProbe.class',
  'META-INF/jarjar/metadata.json', ...['aureth', 'malgrath', 'vaelith'].map(god => `data/divinity_engine/structures/${god}_shrine.nbt`)]) {
  if (!entries.includes(required)) throw new Error(`Rebuilt packaged jar is missing ${required}`);
}
const eula = fs.readFileSync(path.join(fixture, 'eula.txt'), 'utf8');
if (!/^\s*eula\s*=\s*true\s*$/m.test(eula)) throw new Error('Cached fixture does not contain an approved test EULA');
fs.accessSync(java, fs.constants.X_OK);
fs.accessSync(path.join(fixture, forgeArgs), fs.constants.R_OK);
const root = fs.mkdtempSync('/tmp/elder-temple-natural-');
const jarName = path.basename(bundle);
fs.mkdirSync(path.join(root, 'mods'));
fs.symlinkSync(path.join(fixture, 'libraries'), path.join(root, 'libraries'));
fs.writeFileSync(path.join(root, 'eula.txt'), eula, {flag: 'wx'});
fs.copyFileSync(bundle, path.join(root, 'mods', jarName), fs.constants.COPYFILE_EXCL);
const launch = {
  freshSave: true, levelName, seed, root, createdAt: new Date().toISOString(), bundle,
  bundleSha256: crypto.createHash('sha256').update(fs.readFileSync(path.join(root, 'mods', jarName))).digest('hex'),
  forge: '1.20.1-47.4.2', java, requiredJavaVersion: 17, timeoutSeconds: 600,
};
fs.writeFileSync(path.join(root, 'elder-temple-natural-launch.json'), JSON.stringify(launch, null, 2), {flag: 'wx'});
fs.writeFileSync(path.join(root, 'server.properties'), [
  'server-ip=127.0.0.1', 'server-port=0', `level-name=${levelName}`, 'level-type=minecraft:normal',
  `level-seed=${seed}`, 'generate-structures=true', 'view-distance=2', 'simulation-distance=2',
  'online-mode=true', 'white-list=true', 'enforce-whitelist=true', 'max-players=1',
  'spawn-monsters=false', 'spawn-animals=false', 'spawn-npcs=false', 'enable-rcon=false',
  'enable-query=false', 'max-tick-time=60000', 'motd=Disposable Elder temple natural-generation probe', '',
].join('\n'), {flag: 'wx'});
if (fs.existsSync(path.join(root, levelName))) throw new Error('Refusing an existing level');

const env = {...process.env};
for (const key of ['MOD_CLASSES', 'CLASSPATH', 'JAVA_TOOL_OPTIONS', '_JAVA_OPTIONS', 'JDK_JAVA_OPTIONS']) delete env[key];
console.log(`[ElderTempleNaturalProbe] Disposable server: ${root}`);
console.log(`[ElderTempleNaturalProbe] Artifact SHA-256: ${launch.bundleSha256}`);
const log = fs.createWriteStream(path.join(root, 'launch.log'), {flags: 'wx'});
const child = spawn(java, ['-Xmx2G', '-Ddivinity_engine.elderTempleNaturalProbe=true', `@${forgeArgs}`, 'nogui'],
  {cwd: root, env, detached: true, stdio: ['pipe', 'pipe', 'pipe']});
let timedOut = false, interrupted = false, spawnError = null, pending = '';
function signalGroup(signal) {
  if (!child.pid) return;
  try { process.kill(-child.pid, signal); }
  catch (error) { if (error.code !== 'ESRCH') console.error(error.message); }
}
// Reserve ten seconds for graceful termination, then enforce an absolute ten-minute process bound.
const gracefulTimeout = setTimeout(() => { timedOut = true; signalGroup('SIGTERM'); }, 590_000);
const hardTimeout = setTimeout(() => { timedOut = true; signalGroup('SIGKILL'); }, 600_000);
const interruptedHandler = () => { interrupted = true; signalGroup('SIGTERM'); };
process.on('SIGINT', interruptedHandler);
process.on('SIGTERM', interruptedHandler);
child.stderr.on('data', chunk => log.write(chunk));
child.stdout.on('data', chunk => {
  log.write(chunk);
  pending += chunk.toString();
  const lines = pending.split('\n'); pending = lines.pop();
  for (const line of lines) if (line.includes('[ElderTempleNaturalProbe]')) console.log(line);
  if (pending.length > 65536) pending = pending.slice(-65536);
});
const exit = await new Promise(resolve => {
  child.on('error', error => { spawnError = error.message; });
  child.on('close', (code, signal) => resolve({code, signal}));
});
clearTimeout(gracefulTimeout);
clearTimeout(hardTimeout);
process.removeListener('SIGINT', interruptedHandler);
process.removeListener('SIGTERM', interruptedHandler);
await new Promise(resolve => log.end(resolve));

function savedChunks(relative) {
  const directory = path.join(root, levelName, relative, 'region');
  if (!fs.existsSync(directory)) return {regionFiles: 0, allocatedChunkRecords: 0};
  let regionFiles = 0, allocatedChunkRecords = 0;
  for (const name of fs.readdirSync(directory)) {
    if (!/^r\.-?\d+\.-?\d+\.mca$/.test(name)) continue;
    const file = fs.openSync(path.join(directory, name), 'r');
    const header = Buffer.alloc(4096);
    try {
      if (fs.readSync(file, header, 0, 4096, 0) !== 4096) throw new Error(`Incomplete region header: ${name}`);
      for (let index = 0; index < 1024; index++) if (header.readUInt32BE(index * 4) !== 0) allocatedChunkRecords++;
      regionFiles++;
    } finally { fs.closeSync(file); }
  }
  return {regionFiles, allocatedChunkRecords};
}

let probe = null, reportError = null, regions = null;
try { probe = JSON.parse(fs.readFileSync(path.join(root, reportName), 'utf8')); }
catch (error) { reportError = error.message; }
try { regions = {overworld: savedChunks(''), nether: savedChunks('DIM-1'), end: savedChunks('DIM1')}; }
catch (error) { reportError = [reportError, error.message].filter(Boolean).join('; '); }
const complete = !timedOut && !interrupted && !spawnError && !reportError && exit.code === 0
  && probe?.status === 'COMPLETE' && probe?.samples?.length === 3
  && probe.samples.every(sample => sample.status === 'COMPLETE' && sample.actualStart?.pieceCount === 1
    && sample.fullRequestsCompleted === sample.fullChunkPlan?.length && sample.fullRequestsIssued <= 16)
  && probe.ownedTicketsRemaining === 0 && probe.shutdownRequested === true;
const status = !complete ? 'FAIL' : probe.terrainStatus === 'FINDINGS' ? 'FINDINGS' : 'PASS';
const result = {root, ...exit, status, timedOut, interrupted, spawnError, reportError,
  probeReport: path.join(root, reportName), log: path.join(root, 'launch.log'),
  savedRegionRecords: regions, countsIncludeSpawnAndProtochunks: true, bundleSha256: launch.bundleSha256};
fs.writeFileSync(path.join(root, 'launcher-report.json'), JSON.stringify(result, null, 2), {flag: 'wx'});
console.log(JSON.stringify(result, null, 2));
process.exitCode = status === 'PASS' ? 0 : status === 'FINDINGS' ? 2 : 1;
