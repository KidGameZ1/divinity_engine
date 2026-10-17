import assert from 'node:assert/strict';
import { existsSync, readFileSync, readdirSync } from 'node:fs';
import test from 'node:test';
import vm from 'node:vm';

const root = new URL('../', import.meta.url);
const readJson = path => JSON.parse(readFileSync(new URL(path, root), 'utf8'));
const content = 'config/divinity_engine/content/example_pantheon/';
const blessingId = 'example_pantheon:guiding_breath';
const counterKey = 'example_pantheon:guiding_breath_successes';
const blessing = readJson(`${content}blessings/guiding_breath.json`);
const god = readJson(`${content}gods/aster.json`);
const trial = readJson(`${content}trials/trial_of_shelter.json`);
const source = readFileSync(new URL('kubejs/server_scripts/aster.js', root), 'utf8');
const recipeSource = readFileSync(new URL('kubejs/server_scripts/recipes.js', root), 'utf8');

function loadCallbacks() {
  const callbacks = [];
  new vm.Script(source, { filename: 'aster.js' }).runInNewContext({
    DivinityEvents: { blessing: (id, callback) => callbacks.push({ id, callback }) }
  }, { timeout: 1000 });
  return callbacks;
}

const callbacks = loadCallbacks();

function loadRecipeCallbacks() {
  const listeners = [];
  new vm.Script(recipeSource, { filename: 'recipes.js' }).runInNewContext({
    ServerEvents: { recipes: callback => listeners.push(callback) }
  }, { timeout: 1000 });
  return listeners;
}

function collectRecipes(callback) {
  const recipes = [];
  callback({
    shaped(output, pattern, keys) {
      const recipe = { output, pattern: Array.from(pattern), keys: { ...keys } };
      recipes.push(recipe);
      return { id(value) { recipe.id = value; return this; } };
    }
  });
  return recipes;
}

// These objects model the methods used by this script, not Minecraft's event bus or save engine.
function instanceDouble(initialCount = 0) {
  const values = new Map([[counterKey, initialCount]]);
  const instance = {
    dirtyCalls: 0,
    tagRequests: 0,
    count: () => values.get(counterKey),
    getOrCreateTag() {
      instance.tagRequests++;
      return {
        getInt: key => values.get(key) ?? 0,
        putInt: (key, value) => {
          assert.ok(Number.isInteger(value), 'NBT counter must stay an integer');
          values.set(key, value);
        }
      };
    },
    markDirty() { instance.dirtyCalls++; }
  };
  return instance;
}

function invoke({ health = 10, water = true, hook = 'pressed', canceled = false,
  alreadySuccessful = false, instance = instanceDouble(), callback = callbacks[0].callback } = {}) {
  const result = { health, healCalls: 0, successCalls: 0, successful: alreadySuccessful, instance };
  const owner = {
    isInWater: () => water,
    getHealth: () => result.health,
    getMaxHealth: () => 20,
    heal: amount => {
      assert.equal(amount, 2);
      result.healCalls++;
      if (!canceled) result.health = Math.min(20, result.health + amount);
    }
  };
  result.returned = callback({
    getHook: () => hook,
    getOwner: () => owner,
    getInstance: () => instance,
    isSuccessful: () => result.successful,
    markSuccessful: () => { result.successCalls++; result.successful = true; }
  });
  return result;
}

function assertNoSuccess(result, expectedHealth, expectedHealCalls = 0) {
  assert.equal(result.health, expectedHealth);
  assert.equal(result.healCalls, expectedHealCalls);
  assert.equal(result.successCalls, 0);
  assert.equal(result.successful, false);
  assert.equal(result.instance.count(), 0);
  assert.equal(result.instance.tagRequests, 0);
  assert.equal(result.instance.dirtyCalls, 0);
}

test('all shipped JSON and resource-pack metadata parse without dependencies', () => {
  let count = 0;
  function visit(directory) {
    for (const entry of readdirSync(directory, { withFileTypes: true })) {
      const path = new URL(entry.name + (entry.isDirectory() ? '/' : ''), directory);
      if (entry.isDirectory()) visit(path);
      else if (entry.name.endsWith('.json') || entry.name === 'pack.mcmeta') {
        JSON.parse(readFileSync(path, 'utf8'));
        count++;
      }
    }
  }
  visit(root);
  assert.equal(count, 5, 'Update this expectation when intentionally adding definitions/translations');
});

test('instance-root layout includes the pack and no duplicate startup declaration', () => {
  assert.equal(readJson('resourcepacks/aster-addon/pack.mcmeta').pack.pack_format, 15);
  assert.equal(existsSync(new URL('kubejs/startup_scripts/', root)), false);
  assert.ok(existsSync(new URL('resourcepacks/aster-addon/assets/example_pantheon/lang/en_us.json', root)));
});

test('JSON identities, trial references, and script subscription agree', () => {
  assert.equal(callbacks.length, 1);
  assert.equal(callbacks[0].id, blessingId);
  assert.deepEqual(god.blessings, [blessingId]);
  assert.equal(trial.god, 'example_pantheon:aster');
  assert.equal(blessing.schema_version, 1);
  assert.equal(god.schema_version, 1);
  assert.equal(trial.schema_version, 1);
});

test('shipped configuration leaves conditional behavior to the script', () => {
  assert.equal(blessing.mode, 'active');
  assert.equal(blessing.favor, 50);
  assert.equal(blessing.cooldown_seconds, 12);
  assert.deepEqual(blessing.hooks, {});
  assert.equal(blessing.particle_texture, 'minecraft:textures/particle/glint.png');
});

test('shipped trial has bounded, intentional two-phase data', () => {
  assert.equal(trial.reward_favor, 25);
  assert.equal(trial.phases.length, 2);
  assert.equal(trial.phases[0].goal, 'defeat_all');
  assert.equal(trial.phases[0].duration_ticks, 600);
  assert.deepEqual(trial.phases[0].spawns, [{ entity: 'minecraft:zombie', count: 3 }]);
  assert.equal(trial.phases[1].goal, 'survive');
  assert.equal(trial.phases[1].duration_ticks, 400);
  assert.deepEqual(trial.phases[1].spawns, [{ entity: 'minecraft:skeleton', count: 2 }]);
});

test('client text describes the shipped water-only heal and stable names', () => {
  const names = readJson('resourcepacks/aster-addon/assets/example_pantheon/lang/en_us.json');
  assert.equal(names['example_pantheon.gods.aster'], 'Aster');
  assert.equal(names['example_pantheon.gods.aster.contact'], 'Aster: Even a lost traveler deserves a guiding light.');
  assert.equal(names['example_pantheon.gods.aster.loss'], 'Aster: The road remains, should you return.');
  assert.equal(names['example_pantheon.blessings.guiding_breath'], 'Guiding Breath');
  assert.equal(names['block.example_pantheon.aster_statue'], 'Statue of Aster');
  assert.match(names['example_pantheon.blessings.guiding_breath.description'], /in water.*2 health points.*full health/);
});

test('injured owner in water heals once, marks success once, and dirties its own counter', () => {
  const result = invoke();
  assert.equal(result.health, 12);
  assert.equal(result.healCalls, 1);
  assert.equal(result.successCalls, 1);
  assert.equal(result.successful, true);
  assert.equal(result.returned, undefined, 'Returning a boolean is not the success mechanism');
  assert.equal(result.instance.count(), 1);
  assert.equal(result.instance.dirtyCalls, 1);
});

test('dry land does not heal, succeed, or allocate custom state', () => {
  assertNoSuccess(invoke({ water: false }), 10);
});

test('full health does not heal, succeed, or allocate custom state', () => {
  assertNoSuccess(invoke({ health: 20 }), 20);
});

test('canceled healing does not claim success or increment the counter', () => {
  assertNoSuccess(invoke({ canceled: true }), 10, 1);
});

test('partial healing near the cap still counts as one successful use', () => {
  const result = invoke({ health: 19 });
  assert.equal(result.health, 20);
  assert.equal(result.successCalls, 1);
  assert.equal(result.instance.count(), 1);
});

test('non-pressed hooks cannot activate the ability', () => {
  for (const hook of ['tick', 'damage_entity', 'taken_damage', 'kill_entity', 'toggle_on', 'toggle_off']) {
    assertNoSuccess(invoke({ hook }), 10);
  }
});

test('separate blessing instances do not share progress', () => {
  const first = instanceDouble();
  const second = instanceDouble();
  invoke({ instance: first });
  invoke({ instance: first });
  invoke({ instance: second });
  assert.equal(first.count(), 2);
  assert.equal(first.dirtyCalls, 2);
  assert.equal(second.count(), 1);
  assert.equal(second.dirtyCalls, 1);
});

test('existing per-instance count is incremented rather than reset', () => {
  const instance = instanceDouble(41);
  invoke({ instance });
  assert.equal(instance.count(), 42);
});

test('counter saturates instead of overflowing and repairs negative stored values', () => {
  const capped = instanceDouble(1000000000);
  const negative = instanceDouble(-4);
  invoke({ instance: capped });
  invoke({ instance: negative });
  assert.equal(capped.count(), 1000000000);
  assert.equal(negative.count(), 1);
});

test('optional missing instance does not invalidate genuine healing', () => {
  const result = invoke({ instance: null });
  assert.equal(result.health, 12);
  assert.equal(result.successCalls, 1);
});

test('early return does not clear success already supplied by earlier actions', () => {
  const result = invoke({ water: false, alreadySuccessful: true });
  assert.equal(result.health, 10);
  assert.equal(result.successCalls, 0);
  assert.equal(result.successful, true);
  assert.equal(result.instance.count(), 0);
});

test('fresh script evaluation registers one listener without resetting instance-owned state', () => {
  const instance = instanceDouble(7);
  const fresh = loadCallbacks();
  assert.equal(fresh.length, 1);
  invoke({ instance, callback: fresh[0].callback });
  assert.equal(instance.count(), 8);
});

test('recipe script subscribes once without requiring Divinity registration APIs', () => {
  const listeners = loadRecipeCallbacks();
  assert.equal(listeners.length, 1);
  assert.equal(typeof listeners[0], 'function');
});

test('survival statue recipe uses one shard, four stone, and a stable explicit recipe ID', () => {
  const recipes = collectRecipes(loadRecipeCallbacks()[0]);
  assert.deepEqual(recipes, [{
    output: 'example_pantheon:aster_statue',
    pattern: [' A ', ' S ', 'SSS'],
    keys: { A: 'minecraft:amethyst_shard', S: 'minecraft:stone' },
    id: 'example_pantheon:crafting/aster_statue'
  }]);
  const slots = recipes[0].pattern.join('');
  assert.equal([...slots].filter(slot => slot === 'A').length, 1);
  assert.equal([...slots].filter(slot => slot === 'S').length, 4);
});

test('fresh recipe evaluations produce the same single recipe without shared accumulation', () => {
  const first = collectRecipes(loadRecipeCallbacks()[0]);
  const second = collectRecipes(loadRecipeCallbacks()[0]);
  assert.equal(first.length, 1);
  assert.equal(second.length, 1);
  assert.deepEqual(second, first);
});
