import assert from 'node:assert/strict';
import { readFileSync, readdirSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, resolve } from 'node:path';

const here = dirname(fileURLToPath(import.meta.url));
const root = resolve(here, '../..');
const pack = resolve(here, '../connected-pantheon-pack');
const source = readFileSync(resolve(here, 'ConnectedPantheonAddon.java'), 'utf8');
const handler = readFileSync(resolve(here, 'StarChartEvents.java'), 'utf8');
const readJson = path => JSON.parse(readFileSync(path, 'utf8'));
const lang = readJson(resolve(pack, 'assets/example_pantheon/lang/en_us.json'));
assert.deepEqual(lang, readJson(resolve(here, 'src/main/resources/assets/example_pantheon/lang/en_us.json')));
assert.equal(readJson(resolve(pack, 'pack.mcmeta')).pack.pack_format, 15);
const resourceId = /^[a-z0-9_.-]+:[a-z0-9/._-]+$/;
const chapters = {
  call: '1. The Call', witness: '2. A Witness', testimony: '3. Divided Testimony',
  deed_a: '4. The Chosen Deed', deed_b: '4. The Chosen Deed',
  reckoning_a: '5. The Reckoning', reckoning_b: '5. The Reckoning',
  trial: '6. The Trial', ending_a: '7. The Return', ending_b: '7. The Return',
};
const keys = (value, expected) => assert.deepEqual(Object.keys(value).sort(), [...expected].sort());
function text(value) {
  if (typeof value === 'string') {
    assert.ok(value.length > 0 && value.length <= 4096);
    return value;
  }
  keys(value, ['translate']);
  assert.equal(typeof value.translate, 'string');
  assert.ok(Object.hasOwn(lang, value.translate), `Missing translation: ${value.translate}`);
  return `{{lang:${value.translate}}}`;
}
function objective(value) {
  keys(value, ['objective', 'condition', 'target', 'deed', ...(Object.hasOwn(value, 'item') ? ['item'] : [])]);
  assert.match(value.objective, resourceId);
  assert.equal(typeof value.condition, 'string');
  assert.ok(Number.isInteger(value.target) && value.target >= 1 && value.target <= 1_000_000);
  text(value.deed);
  if (value.item) assert.match(value.item, resourceId);
  if (value.objective === 'divinity_engine:donation') assert.ok(value.item);
  if (value.objective === 'divinity_engine:observe') {
    assert.ok(['day_sky', 'night_sky', 'forest', 'rain', 'thunder', 'underground', 'high_sky', 'water', 'snow', 'firelight'].includes(value.condition));
  }
}

// This fixture-specific reader checks literal/translation arguments, not Java semantics.
const quoted = '"(?:[^"\\\\]|\\\\.)*"';
const javaTexts = value => [...value.matchAll(new RegExp(`NarrativeText\\.translatable\\((${quoted})\\)|(${quoted})`, 'g'))]
  .map(match => match[1] ? { translate: JSON.parse(match[1]) } : JSON.parse(match[2]));
const rewards = new Set();
const blessings = new Set();
const byId = new Map();
const directory = resolve(pack, 'data/example_pantheon/divine_narratives');
const definitions = readdirSync(directory).filter(file => file.endsWith('.json')).map(file => readJson(resolve(directory, file)));
assert.equal(definitions.length, 3);
for (const definition of definitions) {
  assert.equal(definition.version, 2);
  assert.match(definition.id, resourceId);
  assert.ok(!byId.has(definition.id), `Duplicate definition ID: ${definition.id}`);
  byId.set(definition.id, definition);
}

for (const [method, id] of [['firstStar', 'first_star'], ['lastLantern', 'last_lantern']]) {
  const definition = byId.get(`example_pantheon:${id}`);
  keys(definition, ['version', 'kind', 'id', 'god', 'title', 'request', 'branches', 'chronicle']);
  assert.equal(definition.kind, 'story');
  assert.equal(definition.god, 'example_pantheon:vesper');
  const body = source.split(`public static DivineStoryDefinition ${method}() {`)[1].split('\n    public static ')[0];
  assert.equal(text(definition.title), text(javaTexts(body.match(/\.title\(([^\n]+)/)[1])[0]));
  assert.equal(text(definition.request), text(javaTexts(body.match(/\.request\(([^\n]+)/)[1])[0]));
  const chronicle = definition.chronicle;
  keys(chronicle, ['premise', 'offering_examples', 'witness', 'reckoning', 'passages', 'dialogue', ...(chronicle.chapter_titles ? ['chapter_titles'] : [])]);
  for (const key of Object.keys(chronicle.chapter_titles ?? {})) assert.ok(Object.hasOwn(chapters, key));
  assert.equal(text(chronicle.premise), text(javaTexts(body.match(/\.premise\(([^\n]+)/)[1])[0]));
  assert.deepEqual(chronicle.offering_examples, [...body.matchAll(/\.offering\(vanilla\("([^"]+)"\)\)/g)].map(match => `minecraft:${match[1]}`));
  objective(chronicle.witness);
  objective(chronicle.reckoning);
  keys(chronicle.passages, Object.keys(chapters));
  keys(chronicle.dialogue, Object.keys(chapters));
  let chapterCount = 0;
  for (const match of body.matchAll(/\.chapter\(chapter\(([\s\S]*?)\)\)(?=\s*\.(?:chapter|build))/g)) {
    const [key, title, line, ...paragraphs] = javaTexts(match[1]);
    assert.equal(text(title), text(chronicle.chapter_titles?.[key] ?? chapters[key]));
    assert.equal(text(chronicle.dialogue[key]), text(line));
    assert.ok(chronicle.passages[key].length > 0 && chronicle.passages[key].length <= 32);
    assert.deepEqual(chronicle.passages[key].map(text), paragraphs.map(text));
    chapterCount++;
  }
  assert.equal(chapterCount, 10);
  keys(definition.branches, ['a', 'b']);
  let branchCount = 0;
  for (const match of body.matchAll(/\.branch\("([ab])", donation\(([\s\S]*?)\)\)/g)) {
    const branch = definition.branches[match[1]];
    const [name, item, deed, reward, description, ending] = javaTexts(match[2]);
    assert.deepEqual(branch, {
      name, objective: 'divinity_engine:donation', condition: '',
      target: Number(match[2].match(/,\s*(\d+),/)[1]), item: `minecraft:${item}`, deed,
      reward: { id: `example_pantheon:${reward}`, blessing: `divinity_engine:${match[2].includes('SOLAR_WARD') ? 'solar_ward' : 'twilight_veil'}`, description }, ending,
    });
    assert.ok(!rewards.has(branch.reward.id));
    rewards.add(branch.reward.id);
    blessings.add(branch.reward.blessing);
    branchCount++;
  }
  assert.equal(branchCount, 2);
  const witnessCondition = id === 'first_star' ? 'night_sky' : 'day_sky';
  const witnessTarget = id === 'first_star' ? 200 : 100;
  assert.equal(chronicle.witness.condition, witnessCondition);
  assert.equal(chronicle.witness.target, witnessTarget);
  assert.ok(body.includes(`StoryObjectiveType.OBSERVE, "${witnessCondition}", ${witnessTarget}`));
  if (id === 'first_star') {
    assert.deepEqual(chronicle.reckoning, {
      objective: 'example_pantheon:star_chart', condition: 'craft_compass', target: 1,
      deed: 'Craft a vanilla compass once as a star chart after this chapter begins', item: 'minecraft:compass',
    });
    assert.ok(body.includes('new StoryObjective(STAR_CHART, CHART_CONDITION, 1, vanilla("compass")'));
  } else {
    assert.equal(chronicle.reckoning.objective, 'divinity_engine:observe');
    assert.equal(chronicle.reckoning.condition, 'night_sky');
    assert.equal(chronicle.reckoning.target, 100);
    assert.ok(body.includes('StoryObjectiveType.OBSERVE, "night_sky", 100'));
  }
  assert.ok(body.includes(`"${chronicle.witness.deed}"`));
  assert.ok(body.includes(`"${chronicle.reckoning.deed}"`));
}
assert.deepEqual([...blessings].sort(), ['divinity_engine:solar_ward', 'divinity_engine:twilight_veil']);
const blessingRegistry = readFileSync(resolve(root, 'src/main/java/net/nightshade/divinity_engine/registry/divinity/blessing/BlessingsRegistry.java'), 'utf8');
for (const blessing of blessings) assert.ok(blessingRegistry.includes(`registry.register("${blessing.split(':')[1]}"`));
const relation = byId.get('example_pantheon:dawn_and_evening');
keys(relation, ['version', 'kind', 'id', 'gods', 'stories', 'title', 'relation', 'summary', 'epilogues']);
assert.equal(relation.kind, 'relationship');
assert.deepEqual(relation.gods, ['divinity_engine:solarius', 'example_pantheon:vesper']);
assert.deepEqual(relation.stories, ['divinity_engine:solarius', 'example_pantheon:first_star']);
keys(relation.epilogues, ['aa', 'ab', 'ba', 'bb']);
const relationSource = source.split('public static GodRelationshipDefinition dawnAndEvening() {')[1].split('\n    private static')[0];
for (const value of [relation.title, relation.relation, relation.summary]) assert.ok(relationSource.includes(JSON.stringify(value)));
let combinations = 0;
for (const match of relationSource.matchAll(/"([ab]{2})", List\.of\(([^\n]+?)\)/g)) {
  assert.deepEqual(relation.epilogues[match[1]].map(text), javaTexts(match[2]).map(text));
  combinations++;
}
assert.equal(combinations, 4);
assert.ok(source.includes('StoryObjectiveType.register('));
assert.ok(source.includes('new StoryObjectiveType(id("star_chart"), false, 0)'));
assert.ok(source.includes('CHART_CONDITION = "craft_compass"'));
assert.ok(source.includes('StarChartEvents.register();'));
assert.ok(handler.includes('MinecraftForge.EVENT_BUS.register(StarChartEvents.class)'));
assert.ok(handler.includes('Items.COMPASS'));
assert.ok(handler.includes('!SEEN.add(event)'));
const manager = readFileSync(resolve(root, 'src/main/java/net/nightshade/divinity_engine/util/divinity/narrative/DivineNarrativeManager.java'), 'utf8');
assert.ok(manager.includes('void recordObjective(ServerPlayer player, StoryObjectiveType type, String condition, int amount)'));
assert.ok(handler.includes('DivineNarrativeManager.recordObjective('));
console.log('Static checks passed: 2 story envelopes, 20 passages/dialogue pairs, 4 unique reward markers, 2 registered blessing references, 4 relationship combinations and 4 translated strings, including a custom chapter title.');
console.log('Java-to-JSON text/argument parity checked by fixture-specific source inspection; no compilation, engine JSON execution, Forge event dispatch or gameplay tests performed.');
