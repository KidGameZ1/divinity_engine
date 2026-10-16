import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { test } from 'node:test';
import { assertRealTree, assertSequential, buildArgs, checkReport, gods, parseArgs, passes } from './run_effects_preview.mjs';

const raw = ['/jdk/bin/java', '-Xmx8G', '-Xms4G', '-XX:MaxHeapSize=8000000000',
  '-Ddivinity_engine.codexSmokeTest=true', '-Ddivinity_engine.communionSmokeTest=true',
  '-Dnightshade_core.clientSmokeTest=true', '-Dnightshade_core.renderPreview=true',
  '-Djava.library.path=/source/natives', '-Djna.tmpdir=/source/natives',
  '-Dorg.lwjgl.system.SharedLibraryExtractPath=/source/natives', '-Dio.netty.native.workdir=/source/natives',
  '-cp', '/source/libraries/forge.jar', 'cpw.mods.bootstraplauncher.BootstrapLauncher', '--launchTarget', 'forgeclient',
  '--gameDir', '/personal/run', '--quickPlaySingleplayer', 'Personal World', '--quickPlayMultiplayer=example.invalid',
  '--server', 'example.invalid', '--port', '25565', '--username', 'Personal', '--accessToken', 'secret',
  '--uuid=personal', '--clientId=personal', '--xuid=personal', '--userType', 'msa', '--width', '320', '--height', '240'];

test('arguments isolate effects, identity, world, native paths, and heap', () => {
  const root = '/tmp/nightshade-production-smoke-effects-unit';
  const args = buildArgs(raw, root);
  assert.deepEqual(args.filter(arg => /^-Xm[sx]/.test(arg)), ['-Xmx2G', '-Xms512M']);
  assert.deepEqual(args.filter(arg => /^-D(?:divinity_engine|nightshade_core)\./.test(arg)), [
    '-Ddivinity_engine.effectsSmokeTest=true', '-Ddivinity_engine.effectsSmokeAutoOpen=true', '-Ddivinity_engine.effectsSmokeAutoExit=true',
  ]);
  for (const flag of ['--quickPlaySingleplayer', '--quickPlayMultiplayer', '--server', '--port'])
    assert.equal(args.some(arg => arg === flag || arg.startsWith(`${flag}=`)), false);
  assert.equal(args.includes('secret'), false);
  assert.equal(args.includes('/personal/run'), false);
  assert.equal(args.some(arg => arg.startsWith('-XX:MaxHeapSize=')), false);
  for (const [flag, value] of [['--gameDir', root], ['--username', 'CoreSmokeQA'], ['--accessToken', '0'], ['--userType', 'legacy'],
    ['--width', '1440'], ['--height', '900']]) {
    assert.equal(args.filter(arg => arg === flag).length, 1);
    assert.equal(args[args.indexOf(flag) + 1], value);
  }
  for (const key of ['java.library.path', 'jna.tmpdir', 'org.lwjgl.system.SharedLibraryExtractPath', 'io.netty.native.workdir'])
    assert.ok(args.includes(`-D${key}=${root}/natives`));
});

test('arguments reject alternate bootstraps, agents, argfiles, and missing values', () => {
  for (const unsafe of ['@extra.args', '-javaagent:/tmp/agent.jar', '-agentpath:/tmp/agent.so'])
    assert.throws(() => buildArgs([raw[0], unsafe, ...raw.slice(1)], '/tmp/test'), /argument file or agent/);
  assert.throws(() => buildArgs([...raw, '--gameDir'], '/tmp/test'), /Missing launch argument/);
  assert.throws(() => buildArgs(raw.map(arg => arg === 'forgeclient' ? 'forgeclientuserdev' : arg), '/tmp/test'), /packaged forgeclient/);
  assert.throws(() => buildArgs(['/jdk/bin/java', '-jar', 'unknown.jar'], '/tmp/test'), /Invalid|packaged Forge/);
});

test('CLI has only effects/no-launch options, no world or other suite selector', () => {
  assert.equal(parseArgs(['--validate-only']).validateOnly, true);
  assert.equal(parseArgs(['--prepare-only']).prepareOnly, true);
  assert.throws(() => parseArgs(['--world', 'Personal']), /Unknown option/);
  assert.throws(() => parseArgs(['--suite', 'all']), /Unknown option/);
  assert.throws(() => parseArgs(['--bundle']), /Missing value/);
  assert.throws(() => parseArgs(['--validate-only', '--prepare-only']), /one no-launch mode/);
});

test('sequential guard refuses Gradle and Minecraft without signaling any process', () => {
  assert.doesNotThrow(() => assertSequential('1 /sbin/launchd\n2 node docs/tools/run_effects_preview.mjs'));
  for (const command of ['org.gradle.launcher.daemon.bootstrap.GradleDaemon', 'org.gradle.wrapper.GradleWrapperMain',
    'org.gradle.process.internal.worker.GradleWorkerMain', 'cpw.mods.bootstraplauncher.BootstrapLauncher', 'net.minecraft.client.main.Main'])
    assert.throws(() => assertSequential(`42 /jdk/bin/java ${command}`), /sequentially/);
});

test('fixture traversal rejects linked descendants', t => {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), 'effects-launcher-test-'));
  t.after(() => fs.rmSync(root, { recursive: true, force: true }));
  fs.mkdirSync(path.join(root, 'world'));
  fs.writeFileSync(path.join(root, 'world', 'level.dat'), 'synthetic unit fixture');
  assert.doesNotThrow(() => assertRealTree(root));
  fs.symlinkSync(path.join(root, 'world', 'level.dat'), path.join(root, 'linked.dat'));
  assert.throws(() => assertRealTree(root), /Linked\/special/);
});

function passingReport(root) {
  const images = path.join(root, 'divinity-effects-smoke/run-test/screenshots');
  fs.mkdirSync(images, { recursive: true });
  const png = Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aE1cAAAAASUVORK5CYII=', 'base64');
  return {
    status: 'PASS', schemaVersion: 2, testKind: 'synthetic_local_blessing_effects_only', plannedScenarios: 54, remainingScenarios: 0,
    clientOptionsRestored: true, gameplayMenuUnchanged: true, coreStateEmpty: true, allScenesNaturallyExpired: true,
    serverOrWorldMutationsIssued: false, gameplayCallbacksInvoked: false, cleanupErrors: [],
    diagnostics: { status: 'PASS', particleOnlyRecipes: 90, combatMeshVertices: 0, qualityCases: 2700,
      allGodPalettesVerified: true, minecraftSpritesVerified: true, persistentPortalComposerVerified: true,
      persistentPortalVertices: 100, palettes: gods.map(god => ({ god })), sprites: [{ visibleTexels: 4 }] },
    privateSnapshot: { status: 'PASS', particleOnly: true, handlesAfterPublicReplacement: 1, publicReplacementPreservedEmitter: true,
      individualRemovalVerified: true, naturalExpiryVerified: true, expiredReplayIgnored: true },
    scenarios: passes.flatMap(pass => gods.map(god => {
      const name = `${pass}-${god}`, screenshot = path.join(images, `${name}.png`);
      fs.writeFileSync(screenshot, png);
      return { name, pass, god: `divinity_engine:${god}`, status: 'PASS', localHandles: 5, pixelEmittersVerified: 5,
        combatMeshHandles: 0, particlesAfterExpiry: 0, emittersAfterExpiry: 0, particleBloomPasses: 0,
        renderedParticles: 5, particleVertices: 20, coreStateVerified: true, naturalExpiryVerified: true,
        pixelShaderSelectionVerified: true, minecraftSpritesVisible: true, godPaletteVerified: true, reducedMotionOptionsVerified: true,
        animationStrength: pass === 'reduced-motion' ? 0 : 1, screenEffectScale: pass === 'reduced-motion' ? 0 : 1,
        reducedFlashing: pass === 'reduced-motion', particleShaderAvailable: pass !== 'shader-fallback',
        pixelVanillaFallbackSelected: pass === 'shader-fallback', screenshot,
        recipes: Array.from({ length: 5 }, (_, variant) => ({ id: `divinity_engine:${god}_${variant}`, variant, mesh: false,
          particles: true, godPaletteVerified: true, particleSprite: 'minecraft:textures/particle/spark_0.png' })),
      };
    })),
  };
}

test('report requires all 54 scenes/90 particle recipes, rendering, expiry, and cleanup', t => {
  const root = fs.realpathSync(fs.mkdtempSync(path.join(os.tmpdir(), 'effects-launcher-test-')));
  t.after(() => fs.rmSync(root, { recursive: true, force: true }));
  const report = passingReport(root);
  assert.deepEqual(checkReport(report, root), { errors: [], screenshots: 54, recipes: 90 });
  for (const corrupt of [
    value => { value.scenarios.pop(); },
    value => { value.scenarios[0].recipes[0].mesh = true; },
    value => { value.scenarios[0].renderedParticles = 0; },
    value => { value.scenarios[0].godPaletteVerified = false; },
    value => { value.scenarios[0].particlesAfterExpiry = 1; },
    value => { value.scenarios[18].pixelVanillaFallbackSelected = false; },
    value => { value.scenarios[36].animationStrength = 1; },
    value => { value.privateSnapshot.handlesAfterPublicReplacement = 2; },
    value => { value.diagnostics.persistentPortalComposerVerified = false; },
    value => { value.clientOptionsRestored = false; },
    value => { value.serverOrWorldMutationsIssued = true; },
    value => { value.scenarios[0].screenshot = value.scenarios[1].screenshot; },
    value => { value.scenarios[0].screenshot = '/tmp/not-a-preview.png'; },
  ]) {
    const invalid = structuredClone(report); corrupt(invalid);
    assert.ok(checkReport(invalid, root).errors.length > 0);
  }
  const firstImage = report.scenarios[0].screenshot;
  fs.unlinkSync(firstImage); fs.symlinkSync(report.scenarios[1].screenshot, firstImage);
  assert.ok(checkReport(report, root).errors.some(error => error.includes('screenshot')));
});
