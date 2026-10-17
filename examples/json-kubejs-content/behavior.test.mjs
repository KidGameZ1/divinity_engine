import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';
import vm from 'node:vm';

const readJson = path => JSON.parse(readFileSync(new URL(path, import.meta.url), 'utf8'));
const definition = readJson('config/divinity_engine/content/example_pantheon/blessings/guiding_breath.json');
const callbacks = [];
vm.runInNewContext(readFileSync(new URL('kubejs/server_scripts/aster.js', import.meta.url), 'utf8'), {
  DivinityEvents: { blessing: (id, callback) => callbacks.push({ id, callback }) }
});

test('JSON declares the same blessing that the server script programs', () => {
  assert.equal(callbacks.length, 1);
  assert.equal(callbacks[0].id, 'example_pantheon:guiding_breath');
  assert.equal(definition.mode, 'active');
  assert.equal(definition.cooldown_seconds, 12);
  assert.deepEqual(definition.hooks, {});
  const god = readJson('config/divinity_engine/content/example_pantheon/gods/aster.json');
  assert.deepEqual(god.blessings, [callbacks[0].id]);
  const trial = readJson('config/divinity_engine/content/example_pantheon/trials/trial_of_shelter.json');
  assert.equal(trial.god, 'example_pantheon:aster');
  assert.equal(readJson('resourcepack/pack.mcmeta').pack.pack_format, 15);
  const names = readJson('resourcepack/assets/example_pantheon/lang/en_us.json');
  assert.match(names['example_pantheon.blessings.guiding_breath.description'], /in water/);
});

function invoke({ health = 10, water = true, hook = 'pressed', canceled = false } = {}) {
  const result = { health, heals: 0, successes: 0 };
  callbacks[0].callback({
    hook,
    owner: {
      isInWater: () => water,
      getHealth: () => result.health,
      getMaxHealth: () => 20,
      heal: amount => {
        result.heals++;
        if (!canceled) result.health = Math.min(20, result.health + amount);
      }
    },
    markSuccessful: () => result.successes++
  });
  return result;
}

test('injured owner in water heals and reports success once', () => {
  assert.deepEqual(invoke(), { health: 12, heals: 1, successes: 1 });
});

test('dry land does not heal or report success', () => {
  assert.deepEqual(invoke({ water: false }), { health: 10, heals: 0, successes: 0 });
});

test('full health does not heal or report success', () => {
  assert.deepEqual(invoke({ health: 20 }), { health: 20, heals: 0, successes: 0 });
});

test('canceled healing does not report success', () => {
  assert.deepEqual(invoke({ canceled: true }), { health: 10, heals: 1, successes: 0 });
});

test('partial healing at the health cap still reports success', () => {
  assert.deepEqual(invoke({ health: 19 }), { health: 20, heals: 1, successes: 1 });
});

test('unrelated hooks do nothing', () => {
  assert.deepEqual(invoke({ hook: 'tick' }), { health: 10, heals: 0, successes: 0 });
});
