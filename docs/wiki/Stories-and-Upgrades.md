# Stories and Upgrades

[Developer Guide](Developer-Guide.md) | [JSON + KubeJS Tutorial](JSON-KubeJS-Tutorial.md) | [Java Addon Tutorial](Java-Addon-Tutorial.md) | [Assets and Localization](Assets-and-Localization.md)

This guide adds a seven-chapter story to an already registered addon god. It
uses the current Forge 1.20.1 narrative API and the same shrine menus used by
the 18 built-in gods. Aster below is example addon content, not another built-in god.

## 1. Install the Host Content First

Start with the template in the [JSON + KubeJS Tutorial](JSON-KubeJS-Tutorial.md)
or its [complete source example](../../examples/json-kubejs-content/README.md).
Its installation must supply all three definitions on both client and server:

```text
config/divinity_engine/content/example_pantheon/gods/aster.json
config/divinity_engine/content/example_pantheon/blessings/guiding_breath.json
config/divinity_engine/content/example_pantheon/trials/trial_of_shelter.json
```

These register `example_pantheon:aster`, its automatic `aster_statue`,
`example_pantheon:guiding_breath`, and Aster's trial. The combined template also
needs `kubejs/server_scripts/aster.js` for its blessing behavior, plus its client
resource pack. The [JSON-only alternative](../../examples/json-content/README.md)
uses fixed actions without KubeJS. Install only one registration alternative:
they intentionally share IDs.

| Input | Installed at | What it creates | Reload boundary |
| --- | --- | --- | --- |
| Startup content JSON | Instance `config/divinity_engine/content/` on both sides | God, statue, blessing, bounded trial | Full restart of both sides |
| Narrative datapack | Server world `datapacks/` | Stories, chapter text, presence, relationships | Server datapack loading/reloading |
| Resource pack | Client `resourcepacks/`, enabled in Options | Language, models, textures | Client resource reload |
| Java addon | Matching mod jar in `mods/` | Registered content and custom runtime handlers | Restart |

A narrative file does **not** register its host god, implement offering rules,
or create a trial. `offering_examples` is guidance, not an acceptance whitelist.
Aster's actual startup god definition accepts bread and amethyst shards, so the
story below lists those items. Custom objective handlers and arbitrary god/trial
lifecycle behavior require Java; the KubeJS custom callback API covers content
blessings, not every narrative, god, or trial event.

## 2. Add a Complete Narrative Datapack

For singleplayer, the full paths are:

```text
<instance>/saves/<world>/datapacks/aster-story/pack.mcmeta
<instance>/saves/<world>/datapacks/aster-story/data/example_pantheon/divine_narratives/road_and_refuge.json
```

On a dedicated server, replace `<instance>/saves/<world>` with the server's
configured world directory. If distributed as a ZIP, `pack.mcmeta` and `data/`
must be directly at its root, not inside an extra `aster-story/` directory.

`aster-story/pack.mcmeta`:

```json
{
  "pack": {
    "pack_format": 15,
    "description": "Aster: Road and Refuge; requires the Aster content template"
  }
}
```

`aster-story/data/example_pantheon/divine_narratives/road_and_refuge.json`:

```json
{
  "version": 2,
  "kind": "story",
  "id": "example_pantheon:road_and_refuge",
  "god": "example_pantheon:aster",
  "title": "Road and Refuge",
  "request": "Make an accepted offering, then decide how to welcome a traveler.",
  "branches": {
    "a": {
      "name": "Shelter",
      "objective": "divinity_engine:donation",
      "condition": "",
      "target": 2,
      "item": "minecraft:bread",
      "deed": "Contribute two bread at Aster's statue",
      "reward": {
        "id": "example_pantheon:road_shelter",
        "blessing": "example_pantheon:guiding_breath",
        "description": "Records Shelter for Guiding Breath; this pack installs no gameplay modifier"
      },
      "ending": "You made a place where a traveler could rest."
    },
    "b": {
      "name": "Wayfinding",
      "objective": "divinity_engine:donation",
      "condition": "",
      "target": 1,
      "item": "minecraft:amethyst_shard",
      "deed": "Contribute one amethyst shard at Aster's statue",
      "reward": {
        "id": "example_pantheon:road_wayfinding",
        "blessing": "example_pantheon:guiding_breath",
        "description": "Records Wayfinding for Guiding Breath; this pack installs no gameplay modifier"
      },
      "ending": "You marked a road without choosing another traveler's destination."
    }
  },
  "chronicle": {
    "premise": "A traveler needs both a place to stop and the freedom to continue.",
    "offering_examples": ["minecraft:bread", "minecraft:amethyst_shard"],
    "witness": {
      "objective": "divinity_engine:observe",
      "condition": "day_sky",
      "target": 100,
      "deed": "Observe open daytime sky for 100 consecutive server ticks"
    },
    "reckoning": {
      "objective": "divinity_engine:observe",
      "condition": "day_sky",
      "target": 100,
      "deed": "After your donation, observe open daytime sky for another 100 consecutive ticks"
    },
    "passages": {
      "call": ["Begin here, then drop an offering Aster accepts in front of the statue."],
      "witness": ["Stand beneath open daytime sky. Watch for one hundred consecutive ticks."],
      "testimony": ["Shelter asks for two bread. Wayfinding asks for one amethyst shard. Both endings record a permanent marker, not an automatic numeric bonus."],
      "deed_a": ["Use Contribute requested items at Aster's statue to donate two bread."],
      "deed_b": ["Use Contribute requested items at Aster's statue to donate one amethyst shard."],
      "reckoning_a": ["Observe the open daytime sky again. Shelter should not become a cage."],
      "reckoning_b": ["Observe the open daytime sky again. Guidance should not become a command."],
      "trial": ["Enter and win a new Aster trial after this reckoning. An older victory does not count."],
      "ending_a": ["Return to the statue and claim Shelter. A place to rest is now part of your story."],
      "ending_b": ["Return to the statue and claim Wayfinding. A road remains open beyond your story."]
    },
    "dialogue": {
      "call": "First, show that you have come to listen.",
      "witness": "Look beyond the roof you know.",
      "testimony": "Will you prepare a refuge or mark a road?",
      "deed_a": "Bring enough to share.",
      "deed_b": "Leave a sign, not an order.",
      "reckoning_a": "A refuge must have an open door.",
      "reckoning_b": "A path must allow another choice.",
      "trial": "Now meet the trial with what you have learned.",
      "ending_a": "Your shelter is remembered.",
      "ending_b": "Your guidance is remembered."
    }
  }
}
```

This complete example uses only registered built-in objective types. The two
donations are separate from the opening ordinary offering. `100` is five seconds
at 20 server ticks per second; interruption resets that observation attempt.
The second observation starts only when Reckoning becomes active. Neither
daylight task requires a special location or staring at a particular screen pixel.

Narratives use `version: 2`; startup definitions use `schema_version: 1`.
Do not move this story into `config/divinity_engine/content`, or put god startup
JSON under `data/.../divine_narratives`. The explicit story `id` is its save
identity; the filename is not a substitute for that field. Keep branch keys
`a`/`b`, god ID, story ID, and reward IDs stable after release.

## 3. Play Through the Menus

Use a disposable world with the host content and datapack installed. No player
commands are needed for this progression:

1. Place Aster's statue from the statue creative tab for testing, then interact to contact Aster. Reopen that same statue's blessings menu.
2. Select **Quests**, choose **Road and Refuge** if multiple stories are listed, and press **Begin quest**.
3. Drop an accepted opening offering in front of the statue after beginning. Complete the daytime Witness objective.
4. Return within six blocks of the same loaded statue. Use **Speak about quest** to open **Shrine Conversation**, preview both paths, and confirm one permanent choice.
5. Hold the required items in inventory and use **Contribute requested items**. Dropping items as ordinary offerings does not fulfill this explicit donation action.
6. Complete Reckoning, then use **Enter Trial** from the matching statue-opened menu and win a fresh trial. Failure permits retry without undoing completed chapters.
7. Return and press **Claim ending and upgrade**. Read the earned chapter and marker in the Codex; revisit **Powers** and **Bond** for eligibility and progress.

The server checks the actual open shrine menu, bound god, contact, proximity,
and current story state. A remotely opened menu or client-side button state
cannot authorize a quest or trial. Conversation preview expires after 1,200
server ticks; a stale confirmation must be previewed again.

Current built-in gods and addon gods with a registered story use relationship
proofs. Do not copy the old blanket requirement of 50 favor before starting a
story. Addons without a story can retain compatibility favor requirements.
Adding a story therefore changes how the host god is managed; test its blessing
unlock sequence as well as the story. For multiple stories, the compatibility
primary-story lookup prefers a story whose ID equals the god ID, then sorts
matching story IDs. Do not change that effective primary accidentally in a save.

## 4. Make a Reward Functional

`BlessingSpecialization` stores a reward ID, blessing ID, and description. The
JSON `reward` object saves that identity at completion and supplies UI text.
It does not grant/equip the blessing, execute an action, alter cooldowns, or
interpret words such as "heal +1" as code. The built-in 36 branch upgrades work
because their blessing callbacks explicitly consult the specialization system.

The Aster story above deliberately promises only markers. To implement a real
Shelter bonus, add code to the actual healing mechanic and then update the
reward description to match it. Choose either your Java blessing implementation
or the existing content-blessing server callback; do not register a second
blessing with the same ID or apply the base heal twice. The JSON-only action
format has no specialization-conditional action.

Here is a complete Java eligibility helper, at
`src/main/java/example/pantheon/upgrade/AsterUpgradeGate.java` in a Java addon:

```java
package example.pantheon.upgrade;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.nightshade.divinity_engine.util.divinity.narrative.BlessingSpecializations;
import net.nightshade.divinity_engine.util.divinity.narrative.DivineNarrativeManager;

public final class AsterUpgradeGate {
    private static final ResourceLocation GOD =
            new ResourceLocation("example_pantheon", "aster");
    private static final ResourceLocation BLESSING =
            new ResourceLocation("example_pantheon", "guiding_breath");
    private static final ResourceLocation REWARD =
            new ResourceLocation("example_pantheon", "road_shelter");

    private AsterUpgradeGate() {}

    public static boolean shelterActive(ServerPlayer player) {
        return player != null
                && player.serverLevel().getServer().isSameThread()
                && player.isAlive() && !player.isRemoved()
                && !player.hasDisconnected() && !player.isSpectator()
                && DivineNarrativeManager.hasSpecialization(player, REWARD)
                && BlessingSpecializations.hasEligibleBlessing(player, GOD, BLESSING);
    }
}
```

Call this from your server-side healing callback. For example, choosing
`AsterUpgradeGate.shelterActive(player) ? 3.0F : 2.0F` as that callback's heal
amount implements a one-health-point Shelter bonus **only when the callback
actually uses the value**. This helper alone installs no effect or listener.
Preserve the ability's water check, health-change/success check, eligibility,
and ordinary cooldown dispatch described in the
[JSON + KubeJS Tutorial](JSON-KubeJS-Tutorial.md) and
[Java Addon Tutorial](Java-Addon-Tutorial.md).

### Program The Same Upgrade In KubeJS

For the JSON + KubeJS starter, no extra Java addon is necessary for this
particular upgrade. Replace its existing `aster.js` listener with the following
server script. Do not keep both listeners: both would receive the event.
`Java.loadClass` accesses the installed public Java API from trusted KubeJS code;
it does not register another god or change the JSON declaration.

```js
const AsterServerPlayer = Java.loadClass('net.minecraft.server.level.ServerPlayer')
const AsterId = Java.loadClass('net.minecraft.resources.ResourceLocation')
const AsterNarrative = Java.loadClass('net.nightshade.divinity_engine.util.divinity.narrative.DivineNarrativeManager')
const AsterEligibility = Java.loadClass('net.nightshade.divinity_engine.util.divinity.narrative.BlessingSpecializations')
const AsterEffects = Java.loadClass('net.minecraft.world.effect.MobEffects')
const AsterEffectInstance = Java.loadClass('net.minecraft.world.effect.MobEffectInstance')

const asterGodId = AsterId.fromNamespaceAndPath('example_pantheon', 'aster')
const asterBlessingId = AsterId.fromNamespaceAndPath('example_pantheon', 'guiding_breath')
const asterShelterId = AsterId.fromNamespaceAndPath('example_pantheon', 'road_shelter')
const asterWayfindingId = AsterId.fromNamespaceAndPath('example_pantheon', 'road_wayfinding')

DivinityEvents.blessing('example_pantheon:guiding_breath', event => {
  if (event.hook !== 'pressed') return
  const player = event.owner
  if (!(player instanceof AsterServerPlayer) || !player.isInWater()) return
  const before = player.getHealth()
  if (before >= player.getMaxHealth()) return
  const eligible = AsterEligibility.hasEligibleBlessing(player, asterGodId, asterBlessingId)
  const shelter = eligible && AsterNarrative.hasSpecialization(player, asterShelterId)
  const wayfinding = eligible && AsterNarrative.hasSpecialization(player, asterWayfindingId)
  player.heal(shelter ? 3 : 2)
  if (player.getHealth() <= before) return
  if (wayfinding) player.addEffect(new AsterEffectInstance(AsterEffects.MOVEMENT_SPEED, 60, 0))
  event.markSuccessful()
})
```

Shelter now heals **three HP instead of two**; Wayfinding keeps the two-HP heal
and grants **Speed I for 60 ticks** after a successful heal. Both preserve the
water condition and normal cooldown. Change the two story reward descriptions
to these actual effects **before anyone accepts the story**, and update the
blessing description. The complete story above remains an honest marker-only
example until this optional script replaces its behavior. Test both paths with
fresh players because accepted story requirements and descriptions are saved.
This code is source-checked guidance, not a claim of a completed Rhino gameplay test.

The two public queries serve different purposes: `hasSpecialization(player,
rewardId)` checks an earned completed reward; `hasEligibleBlessing(player,
godId, blessingId)` checks equipped slots, correct bound god, contact, and current
relationship eligibility. A saved reward alone is not permission to apply a
passive bonus while unequipped or abandoned. Do not use the built-in-only
`isActive(player, godPath, branch)` lookup for arbitrary addon reward IDs.

Use per-player/instance state, never mutable counters on singleton definitions.
Own and remove any transient attributes or potion leases your upgrade adds.
Reconnecting must not duplicate modifiers; reading a saved cooldown must not
apply a reduction again. Keep cleanup independent of whether the blessing is
still in a slot.

## 5. Extend the Java Narrative API

The checked-in [Connected Pantheon Java module](../../examples/connected-pantheon-addon/README.md)
contains full builder stories and a real custom objective handler:

- [ConnectedPantheonAddon.java](../../examples/connected-pantheon-addon/ConnectedPantheonAddon.java) belongs at `src/main/java/example/pantheon/connected/ConnectedPantheonAddon.java` in the host addon.
- [StarChartEvents.java](../../examples/connected-pantheon-addon/StarChartEvents.java) belongs at `src/main/java/example/pantheon/connected/StarChartEvents.java`.
- [Its translations](../../examples/connected-pantheon-addon/src/main/resources/assets/example_pantheon/lang/en_us.json) belong at `src/main/resources/assets/example_pantheon/lang/en_us.json`.

These are narrative modules, **not standalone runnable mods**. Their host must
register `example_pantheon:vesper`, a matching statue, accepted paper/amethyst
offerings, and a Vesper trial. The
[equivalent full datapack](../../examples/connected-pantheon-pack/README.md)
also needs that host. Its `first_star` story additionally needs the registered
`example_pantheon:star_chart` objective and Java handler; its `last_lantern`
story uses standard objectives but still needs Vesper and her trial. None of
these example reward markers automatically modifies Solar Ward or Twilight Veil.

Call `ConnectedPantheonAddon.register()` once in the addon constructor before
common setup. It submits `DivineNarrativeDeferredRegister` and registers the
Forge listener. Do not call the engine's `NarrativeBootstrap`, or add a second
automatic subscriber for `StarChartEvents`.

For your own definitions, use `new DivineNarrativeDeferredRegister(namespace)`,
declare `story(path, supplier)`, then `submit()` before setup. Build stories with
`DivineStoryDefinition.builder(godId).id(storyId)` and both branches. Expanded
`StoryChronicle` requires Witness, Reckoning, and the ten passage keys shown
above. The deferred registration key is authoritative over the builder ID.

Register custom `StoryObjectiveType` values before story construction/JSON
decoding. Registration supplies metadata, not an event listener. The example
handler verifies a successful server compass-crafting notification, then calls
`DivineNarrativeManager.recordObjective(player, type, condition, amount)` and
`sync(player)`. That public reporting hook trusts the caller's evidence; it
does not prove that crafting or another claimed action happened. Never accept
arbitrary progress amounts from client packets. For cancelable events, wait for
the final outcome and revalidate the original chapter before awarding credit.
The [detailed authoring guide](../connected-pantheon-authoring.md) explains these
event and snapshot contracts; use this page's current menu/proof flow for play.

## 6. Reload, Saves, and Validation

Datapack definitions can override Java defaults by stable story ID. Two distinct
visible JSON files declaring the same story ID are rejected. Malformed narrative
reloads retain the last valid definitions; inspect the error rather than assuming
the new file loaded. Accepted stories pin their definitions, and confirmed paths
pin their reward/deed. Editing a JSON target is not a migration of a player's
accepted objective. Use a new disposable player/world to test revised defaults.

Translations stored as markers can resolve through updated client resources;
saved literal prose does not become translated automatically. See
[Assets and Localization](Assets-and-Localization.md). Keep released IDs and
branch meanings stable, including the effective primary story for a god.

Before distributing, verify both branches with independent players, interrupted
observations, insufficient donation items, a failed/retried trial, save/reload,
and repeated ending clicks. For functional bonuses also test unequip,
abandon/recontact, death, cooldown, and canceled healing. Metadata-only markers
must not be advertised as installed gameplay upgrades.

This page's JSON and helper signatures are checked against local source; no
Forge build, runtime loader test, multiplayer session, or in-game playthrough
was performed while authoring it.

## Source References

- [Narrative decoder](../../src/main/java/net/nightshade/divinity_engine/util/divinity/narrative/NarrativeDefinitionJson.java) and [reload listener](../../src/main/java/net/nightshade/divinity_engine/util/divinity/narrative/NarrativeReloadListener.java): exact JSON envelopes and reload behavior.
- [StoryChronicle](../../src/main/java/net/nightshade/divinity_engine/api/narrative/StoryChronicle.java), [StoryObjectiveType](../../src/main/java/net/nightshade/divinity_engine/api/narrative/StoryObjectiveType.java), and [deferred registration](../../src/main/java/net/nightshade/divinity_engine/api/narrative/DivineNarrativeDeferredRegister.java): chapter, condition, and Java registration contracts.
- [Narrative manager](../../src/main/java/net/nightshade/divinity_engine/util/divinity/narrative/DivineNarrativeManager.java), [specialization eligibility](../../src/main/java/net/nightshade/divinity_engine/util/divinity/narrative/BlessingSpecializations.java), and [relationship proofs](../../src/main/java/net/nightshade/divinity_engine/util/divinity/gods/DivineRelationship.java): earned rewards versus active abilities.
- [Blessings screen](../../src/main/java/net/nightshade/divinity_engine/client/screen/BlessingsScreen.java) and [conversation screen](../../src/main/java/net/nightshade/divinity_engine/client/screen/ShrineStoryScreen.java): current player menu path.
