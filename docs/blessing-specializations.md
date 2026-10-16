# Permanent Blessing Specializations

Specializations are earned by the narrative finale, not by previewing or confirming a branch. `BlessingSpecializations.isActive(entity, godPath, branch)` asks the server narrative manager whether the ending was earned, then checks the matching original blessing, equipped slot, current contact, and that blessing's original favor requirement. Losing eligibility does not erase the permanent narrative reward.

The two branch keys are `a` and `b`. The helper deliberately maps built-in gods to their original blessing IDs; other blessings belonging to the same god do not inherit these upgrades. Addon blessings can query their own registered narrative specializations and keep their own eligibility rules.

## Implemented Values

| God / Original blessing | A | B |
| --- | --- | --- |
| Solarius / Solar Ward | Ward effects 15 seconds | Health threshold 65% |
| Nashara / Serpent's Bite | Poison 7 seconds | Successful poison also Weakness I, 2 seconds |
| Grond / Stoneform | Resistance 12 seconds | Start cooldown at 20 seconds |
| Varun / Savage Hunger | Heal 1 HP on activation | Both kill buffs 7 seconds |
| Dravak / Battle Rush | Strength 7 seconds; speed unchanged | Resistance I, 2 seconds |
| Elaris / Foresight | Four dodges per cycle | Start cooldown at 16 seconds |
| Voltira / Stormstep | Horizontal power +0.5 (default 3.0) | Vertical Slow Falling +3 seconds (default 8) |
| Vokar / Soul Leech | Heal 3 HP | Speed I, 3 seconds on kills |
| Nythea / Twilight Veil | Night Vision at night | Nighttime speed modifier 15% |
| Kairoth / Second Chance | Revive at 60% maximum health | Absorption I, 5 seconds |
| Cryonis / Icy Pulse | Radius +1 (default 4 blocks) | Slowness +2 seconds (default 5) |
| Nerai / Aquatic Grace | Heal 1 HP per 200 submerged ticks | Additional 20% swim-speed modifier in water |
| Zephra / Gale Step | Additional 5% movement-speed modifier | Fall-damage multiplier x0.75 |
| Terra / Thornskin | Successful retaliation adds Slowness I, 2 seconds | Retaliation +1 HP |
| Lumen / Beacon's Grace | Block-light threshold 9 in both callbacks | Resistance I while the original light condition holds |
| Mechanos / Clockwork Punch | Chance +10 percentage points (default 40%) | Horizontal knockback x1.25 |
| Ignar / Blazing Hands | Ignition chance 25% | Ignition 5 seconds |
| Aethon / Celestial Ward | Successful-block cooldown 24 seconds | Successful block heals 1 HP |

Existing durations, proc conditions, and favor requirements are retained without an earned specialization. Existing configurable blessing NBT values are read at use time; specialization bonuses do not rewrite them. Kairoth's original long cooldown is unchanged.

## Lifecycle Guarantees

- Press, hold, and release invoke only their own callbacks. Successful presses/holds now start cooldowns immediately; releasing a press does not activate it again. Client-supplied held durations are ignored in favor of server ticks, repeated held packets within one tick are ignored, and unsolicited releases cannot activate anything.
- Release clears its input latch even if the press started a cooldown. Key packets validate server player, loaded chunk, life/spectator state, slot, contact, and favor. `ModMessages` remains the sole registration point for the three packets.
- Reduced cooldown values are resolved only by `startCooldown`. Neither NBT loading nor tick decrements scales an existing cooldown.
- Celestial Ward has no mutable singleton timer or wall-clock dependency. Its successful-block deadline is saved on the player and blessing instance using the server overworld's game clock. A failed, owned, or already-canceled projectile impact does not spend cooldown or heal.
- Thornskin ignores thorns damage and uses a guarded retaliation scope with `finally` cleanup to prevent recursive damage chains.
- Passive attribute bonuses use stable owned UUIDs and transient modifiers. A server tick cleanup runs even after a blessing leaves its slot. Potion leases stop/remove only the effect instance and duration owned by this system; stronger, longer, or pre-existing foreign effects are preserved. Twilight invisibility cleanup does not remove potion invisibility.

## Verification

`BlessingSpecializationGameTests` supplies five Forge GameTests, using the existing `pocket_dimension` structure fixture:

1. `all36PermanentBranches`: all 36 runtime branches, opposite-branch rejection, player isolation, equipment/contact/favor gates, recontact, admin reset, actual callback values, and unchanged configurable NBT parameters. Chance upgrades use deterministic random seeds.
2. `releaseDoesNotReplayPress`: vertical release replay, stale repeated release, immediate horizontal cooldown, and latch release during cooldown.
3. `cooldownPersistenceAndProjectileIsolation`: independent Celestial Ward players, exact save/load and decrement behavior, and unsuccessful same-tick replay.
4. `retaliationAndOwnedEffectSafety`: thorns recursion rejection, foreign potion preservation, owned cleanup, and foreign attribute preservation.
5. `playerTickCooldownsStayIndependent`: START-phase rejection and one independent decrement per player END tick.

Run these in the project's Forge GameTest server after the new Core integration passes its build gate. They compile as part of `compileJava`, require no JUnit dependency, and Forge only discovers them in an explicitly enabled development GameTest environment. They do not grant rewards to ordinary fake players: the test subclass supplies its own isolated narrative/main capability fixtures.

Integrated validation on 2026-09-22 passed against the actual Core 1.6.0,
Vanguard 2.2.1-1.20.1 and Player Animator dependencies: Java 17 build, all five
specialization GameTests, all eleven narrative GameTests and all eight trial
GameTests (24 required tests total). The pure story-state checks also passed
790 assertions over 36 paths. No compile-only stand-in is used or packaged.
These automated checks do not replace connected multiplayer playtesting.

### Integration Follow-Up

Integration replaced the old five-entry queue with server-player END ticks. A single player is no longer processed repeatedly within one batch, and respawned players do not disappear from processing. The existing two-tick blessing and twenty-tick god intervals remain unchanged. Per-cooldown console spam was also removed.
