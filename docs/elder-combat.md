# Elder Combat Integration

> Archived design record, not current gameplay. Aureth, Malgrath, and Vaelith
> have been retired. Their powers, trials, stories, runtime subscribers, and
> natural shrine generation are removed. Saved statues remain inert relics.
> Narrative source JSON is retained under `docs/archive/elder-narratives/`,
> outside resource-pack discovery. Old Inner World scenery and validation
> claims below are historical only. See `elder-validation.md` for migration.

This combat module targets the repository's Minecraft 1.20.1, Forge 47.4.2, Parchment 2023.09.03, and Java 17 setup. It introduces no dependency, command, entity type, terrain mutation, extra loadout slot, or progression system. All fifteen blessing constructors use legacy `neededFavor = 0`. Deed/rank eligibility belongs to the parent progression integration. Unlocking does not equip a blessing.

## Public Integration API

- `ElderCombat.bootstrap()` is called once from the mod constructor. It registers the Forge event subscribers, common-setup visual definitions, and `ElderCombatConfig.SPEC` as `ModConfig.Type.SERVER`, filename `divinity_engine-elder-combat.toml`. Do not register the config a second time.
- `ElderCombat.beginDamagingBlessing(LivingEntity, BlessingsInstance)` returns `ElderCombat.DamageScope`, an `AutoCloseable`. Wrap synchronous original `onPressed`, `onHeld`, and `onRelease` callbacks in try-with-resources. Do not wrap every passive tick or ordinary attack.
- `DamageScope.power()` lazily consumes eligible Sacrificial Might stacks and returns the cast multiplier. A validated delayed action may capture that number, but must not retain a scope or carry its thread-local state onto another thread. Nested same-owner scopes share one consumption. `ElderCombat.endDamagingBlessing(DamageScope)` is an alternative to `close()`.
- `ElderCombat.isSecondaryDamage()` is true while Elder-generated damage executes or a compatible original active-blessing scope is open. Existing offensive `onDamageEntity` and `onKillEntity` dispatch must return early on this guard. Existing retaliation callbacks must also skip it. The inspected legacy retaliation list is Silver Mirror, Thornskin, Shockshield, Coiled Retribution, and Venomblood. Preserve unrelated defensive state callbacks such as Snowveil and Arcane Recall.
- Original scopes are inert unless Sacrificial Might is actually equipped and eligible. An original callback that produces no damage consumes no stacks. Within an effective scope, valid synchronous outgoing damage is multiplied once per event; its original on-hit/retaliation procs are deliberately suppressed. This does not make arbitrary original asynchronous projectiles, toggled damage, or passive ticks automatically compatible.
- `ElderCombat.onLoadoutChanged(LivingEntity)` clears ineligible effects immediately after a slot mutation. A five-tick reconciliation fallback is always installed. `ElderCombat.clear(LivingEntity)` clears all owned combat state and modifiers. `clearCurse(Entity, ElderCurse.Kind)` clears one curse's transient state without altering relationship/progression data.

Runtime entry points and every delayed action check the current three slots and `BlessingSpecializations.isEligible(...)`, including the expected bound god. The shared eligibility implementation resolves the parent's deed gate. No `ElderProgression.requiredFavor(...)` dependency exists.

## Registrations and Specializations

Registry IDs use namespace `divinity_engine`; public registry fields are uppercase versions of these paths. The intended per-god order is:

| God | Slot 1 | Slot 2 | Slot 3 | Slot 4 | Slot 5 |
| --- | --- | --- | --- | --- | --- |
| Aureth | `royal_decree` | `unbroken_will` | `kingsguard` | `sovereigns_reach` | `throne_of_the_world` |
| Malgrath | `ruinbrand` | `sacrificial_might` | `ruinous_grasp` | `ashen_tribute` | `worldbreaker` |
| Vaelith | `rift_lance` | `impossible_angle` | `boundary_prison` | `unwritten_form` | `unmaking` |

Curse fields/paths are `BROKEN_CROWN` / `broken_crown`, `RUIN_WITHIN` / `ruin_within`, and `FRACTURED_EXISTENCE` / `fractured_existence`.

Branch A calls `BlessingSpecializations.isActive(player, godPath, "a")`: Aureth extends Royal Decree's slow/suppression/marked-target window by 20%; Malgrath raises Ruinbrand's stored-damage cap by 20%; Vaelith extends Rift Lance's range by 15%, never past the hard 32-block cap. It does not extend the Decree stagger or add other damage, armor, or guardian bonuses. Branch B's 10% cooldown reduction is owned by the parent `BlessingSpecializations.cooldownTicks(...)`; all Elder cooldown starts delegate to it.

## Targeting and Damage Rules

- Only the logical server makes combat decisions. Entity queries visit loaded local entity sections, stop after 128 examined living candidates by default, and retain at most 32 valid targets. No chunk loads or world-wide scans are requested. Nearest selection is within this bounded candidate sample, not an unbounded global nearest search.
- Hostile targets are `Enemy` mobs or other mobs currently targeting the caster. Other players qualify only when both vanilla team checks permit damage, server PvP is enabled, `limits.allowPvp` is true, and the victim is neither creative nor spectator. Both directions of alliance checks are honored.
- All owned `OwnableEntity` pets, tamed horses, and Vexes are excluded. Common persistent owner/summoner fields are excluded conservatively. Addons with other ownership models should use entity-type tag `divinity_engine:elder_combat_excluded` or the identical entity command tag. No finite implementation can infer every third-party summon convention.
- Communion participants and entities belonging to a different trial session are excluded. Vanilla invulnerability, armor toughness, resistance, shields, hurt cooldowns, Forge cancellation, and death-prevention processing remain active. No invulnerability timer is reset.
- Every area strike validates visibility from its origin. Targeted casts reject unloaded ray corridors and stop at solid collision geometry. Marks and persistent areas cancel when the owner moves over 64 blocks away. They never follow the owner.
- Eligible direct hits for Ruinbrand storage, Sovereign's Reach, and Impossible Angle are attributed melee or nonexplosive projectiles, not thorns, environmental damage, spells, or generated follow-ups. Ruinbrand storage is based on positive final `LivingDamageEvent` health damage, capped to the target's remaining health.
- Elder active area damage is guarded against all legacy offensive/retaliation procs. Internally it can still trigger an enemy player's bounded defensive windows/curses and grant Ashen Tribute for a genuine kill. Generated Reach/Angle hits, guardian retaliation, and fracture echoes cannot grant Tribute or trigger Elder offensive follow-ups. Elder area damage never feeds Ruinbrand.
- Each Elder-generated individual hit is capped at 300 raw health points, including Sacrificial Might. Armor-bypass here means a temporary fractional reduction to the armor attribute during one normal damage call, not bypassing resistance, invulnerability, toughness, or every protection rule.

## Blessing Defaults

Damage, shields, and health costs use health points: 2 points = 1 heart. Times below are seconds at 20 ticks/second. Passive cooldowns are internal cooldowns, not free active actions. `A` uses the existing R/G/V key for whichever slot contains it; `P` is passive only while equipped and eligible.

| Blessing | Mode | Cooldown | Duration | Exact default behavior |
| --- | --- | --- | --- | --- |
| Royal Decree | A | 24 s | 6 s | 10-block, 120-degree forward cone; at most 8 visible hostiles. Movement speed -40%, damage to this caster -25%, this caster's direct damage to the marked target +30%. Horizontal motion is damped for 15 ticks, or 5 ticks against players. Recognized bosses have half the main duration and no stagger; no AI is disabled. Recast needs at least one valid target. |
| Unbroken Will | P | 12 s | 3 s | +0.60 additive knockback resistance while equipped. At most every 12 seconds remove one eligible vanilla Slowness effect, then reject eligible Slowness reapplications for 3 seconds. No cleanse or impairment immunity inside an active trial. Infinite, ambient, long, explicitly protected, and non-Slowness effects are not removed. |
| Kingsguard | A | 45 s | 12 s | Three orbiting spectral guardians, 8 points of damage budget each. Incoming hostile damage spends their budgets in sequence after other Elder mitigation, before armor. A broken guardian retaliates once for 10 damage against a visible valid attacker within 12 blocks, unless broken by guarded secondary damage. Damage beyond remaining budgets continues normally. An existing formation cannot be refilled or recast. |
| Sovereign's Reach | P | 5 s | Instant | A successful direct melee health-damaging hit extends 6 blocks beyond its victim along a line with 0.4-block hitbox inflation. Up to 4 additional visible hostiles take 50% of the triggering damage, capped at 16 each. The primary target is excluded. No copied original on-hit effects. |
| Throne of the World | A | 120 s | 16 s | Fixed 8-block-radius dominion, with a visible particle throne. While inside, direct attacks gain 40% damage and incoming hostile damage is reduced by 30%. Every 30 ticks while the caster is inside, spectral weapons strike up to 6 visible enemies for 10 damage each. Leaving stops personal bonuses and weapon strikes; returning resumes them. One instance per owner. |
| Ruinbrand | A | 24 s | 6 s | Aim at one visible enemy within 24 blocks. Store 35% of subsequent eligible direct health damage, up to 36. On expiry, deal 8 + stored damage to the marked target; up to 7 other visible hostiles within 5 blocks take 65% of that amount. Detach mark before detonating. Target death, unload, invalidation, unequip, or distance over 64 blocks cancels without detonating. |
| Sacrificial Might | P | 1 s gain interval | 20 s stack expiry | Positive hostile health damage grants one Ruin stack, maximum 5; another eligible hit refreshes expiry. The next compatible damaging blessing consumes all stacks for +12% power per stack (default maximum +60%). No stacks from self/environmental/friendly damage, generated follow-ups, fracture echoes, or Worldbreaker cost. All Elder damaging active areas capture the multiplier once at a valid cast; Kingsguard, Decree, and Grasp do not consume it. Hard multiplier ceiling is 3x even with custom stack settings. |
| Ruinous Grasp | A | 28 s | 8 s | Aim within 18 blocks. Temporarily reduce armor contribution by 35% and remove up to 8 absorption. A real successful change grants up to 8 absorption to the caster for 8 seconds. A target with an active Grasp, or neither armor nor absorption, cannot grant another shield. Equipment is never altered. |
| Ashen Tribute | P | 1 s reward interval | 10 s | A qualifying direct or Elder active-blessing kill grants 2 absorption; armored foes with at least 10 armor grant 4; recognized bosses grant 6. Cap 12. Pets, allies, summons, invalid targets, generated follow-up/retaliation/echo kills, and environmental kills give nothing. |
| Worldbreaker | A | 120 s | 1 s expansion, 8 s defense penalty | Requires at least 6 current health. Deduct 25% of current health directly, never leaving less than 1; cost is not a damage event. A fixed rupture expands to 12 blocks over 20 ticks, hits at most 16 distinct visible enemies once each, and deals 40 damage at its origin falling linearly to 14 at the edge. Survivors hit successfully lose 40% armor contribution for 8 seconds. No terrain destruction. |
| Rift Lance | A | 18 s | Instant | Validated 24-block ray, 0.3-block hitbox inflation, up to 6 visible enemies; 18 damage each with 35% of armor contribution bypassed. The beam ends at the first solid obstruction. A valid cast can miss and still consume its cooldown/stacks. |
| Impossible Angle | P | 6 s | 12-tick delay | A successful direct melee/projectile health-damaging hit within 24 blocks schedules one 40%-strength follow-up, capped at 16. Revalidate target, range, and both sightlines at execution. The damage source is positioned 1.5 blocks on the far side of the target, not merely a different visual. No copied procs or forced hurt-cooldown reset. |
| Boundary Prison | A | 36 s | 8 s | Aim at a point within 24 blocks; fixed radius 4. Enroll at most 6 hostiles for the entire cast. Near the edge, apply 5 rift damage and an inward impulse no more than once per 20 ticks per enrolled target. Normal mob impulse 0.45, boss impulse 0.08, player impulse 0.15. Player restrictions end 20 ticks after enrollment. No teleport-back, time freeze, or block placement; jumps/fast movement and leaving the edge band can escape. |
| Unwritten Form | P | 20 s | 3 s opportunity window | A hostile final health-damage hit of at least 6 opens a window without undoing that hit. The next incoming hostile hit during it is reduced by 75%, consumes the window, and starts cooldown. An unused window also starts cooldown when it expires. The mitigation cannot immediately reopen itself. |
| Unmaking | A | 120 s | 2 s warning | Aim at a point within 24 blocks; fixed radius 6. Display a raised cross/sigil during the warning, then deal 48 damage to at most 12 visible enemies. On a successful hit against a surviving nonboss, remove at most one eligible temporary vanilla benefit, in order: Resistance, Absorption, Regeneration, Strength, Speed. Infinite/ambient/protected/long effects and all recognized-boss effects remain. No entity deletion, inventory removal, or terrain changes. |

Boss resistance classification is bounded: Ender Dragon, Wither, Warden, or an entity that cannot change dimensions. Attribute debuffs use stable transient UUIDs and multiply remaining armor contribution; Grasp and Worldbreaker can coexist but duplicate same-kind effects do not stack. Another owner's active same-kind debuff is not overwritten.

Temporary absorption tracks only amounts this module actually granted. It reconciles observed absorption decreases before further grants, expiry, or cleanup; decreases consume tracked Elder points first conservatively. Removing an Elder shield subtracts only its tracked remainder, not unrelated absorption. Grant caps also limit the recipient's total absorption at grant time, so two Elder shield sources cannot produce unlimited absorption.

## Curse Defaults and Lifecycle

Curses are active only while present in the existing curse runtime list; this module does not infer a new negative-rank gate or modify legacy negative tiers. All triggers require positive hostile health damage. Repeated hits refresh finite state rather than creating stacks.

| Curse | Default behavior |
| --- | --- |
| Broken Crown | Hostile damage applies -20% total attack damage and -0.30 additive knockback resistance for 100 ticks. Stable transient modifiers replace/refresh, never stack. |
| Ruin Within | Hostile damage applies 35% incoming healing reduction for 120 ticks. During that window, remove at most 2 existing absorption per second, never below zero. Maximum health is untouched. |
| Fractured Existence | At most once every 160 ticks, record 25% of a hostile health-damage hit, capped at 6. After 30 ticks, apply one normal attributed delayed echo if the original attacker is still a valid loaded hostile. No recursive echo; armor and other normal protections still apply. |

`ElderCurse.onCurse` and `onUncurse` dispatch the narrative presence kinds `curse` and `curse_cleared` using the bound god registry ID and a server player. A single small persisted lifecycle phase on the curse instance prevents duplicate dispatch during the same lifecycle. Existing generic curse messages remain owned by the existing curse storage.

## Config Reference

Every `blessings.<registry_path>` section contains `cooldownTicks`, `durationTicks`, `strength`, `range`, and `targetCap`. Defaults derive directly from the table above. Duration/range/target fields irrelevant to a particular passive are reserved, not extra mechanics: e.g. Reach and Angle's `durationTicks` do not alter their one-hit behavior; Unwritten Form's `range` and `targetCap` do nothing. Ruinbrand's `range` is its blast radius; the area cast range is in `details`. Worldbreaker's `durationTicks` is its defense penalty, not its expansion time. Unmaking's `durationTicks` is the warning delay. Kingsguard's strength is each guardian's budget; its target cap can reduce the default formation of three but never exceed three.

Validated common ranges: cooldown 20..72000 ticks; duration 5..1200 ticks; range 0..32 blocks; targets 1..32 (Kingsguard 1..3); fractional strength 0..0.90; other strength 0..100. Hard bounds remain even when config values are raised. A zero strength disables that strength contribution, not every associated mechanic.

| Section/key | Default | Valid range |
| --- | --- | --- |
| `limits.maxTrackedPlayers` | 1024 | 16..4096 |
| `limits.maxDebuffedEntities` | 1024 | 32..4096 |
| `limits.maxCandidatesPerQuery` | 128 | 32..256 |
| `limits.outlineParticlesPerPlayerPerSecond` | 160 | 32..512 |
| `limits.outlineParticlesPerLevelPerTick` | 512 | 32..2048 |
| `limits.allowPvp` | true | boolean, still requires server/team permission |
| `details.decreeSlowFraction` | 0.40 | 0..0.8 |
| `details.decreeStaggerTicks` | 15 | 1..40 |
| `details.decreeDamageReduction` | 0.25 | 0..0.8 |
| `details.guardianRetaliationDamage` | 10 | 0..100 |
| `details.throneAttackBonus` | 0.40 | 0..1 |
| `details.throneDamageReduction` | 0.30 | 0..0.8 |
| `details.throneStrikeIntervalTicks` | 30 | 10..200 |
| `details.ruinbrandStoredDamageCap` | 36 | 1..200 |
| `details.ruinbrandBaseDamage` | 8 | 0..100 |
| `details.targetedAreaCastRange` | 24 | 1..32 |
| `details.graspAbsorptionDrain` | 8 | 0..40 |
| `details.graspShieldCap` | 8 | 0..40 |
| `details.tributeAbsorptionCap` | 12 | 0..40 |
| `details.worldbreakerCurrentHealthCost` | 0.25 | 0.05..0.75 |
| `details.worldbreakerMinimumHealth` | 6 | 2..100 |
| `details.worldbreakerArmorReduction` | 0.40 | 0..0.75 |
| `details.worldbreakerExpansionTicks` | 20 | 5..100 |
| `details.riftLanceArmorBypassFraction` | 0.35 | 0..0.75 |
| `details.passiveSecondaryDamageCap` | 16 | 1..100 |
| `details.prisonContactCooldownTicks` | 20 | 10..100 |
| `details.prisonPvpConfinementTicks` | 20 | 5..40 |
| `details.unwrittenFormTriggerDamage` | 6 | 1..40 |
| `details.removableEffectMaximumTicks` | 600 | 20..1200 |
| `curses.brokenCrownDurationTicks` | 100 | 5..600 |
| `curses.brokenCrownAttackPenalty` | 0.20 | 0..0.6 |
| `curses.brokenCrownKnockbackPenalty` | 0.30 | 0..1 |
| `curses.ruinWithinDurationTicks` | 120 | 5..600 |
| `curses.ruinWithinHealingReduction` | 0.35 | 0..0.8 |
| `curses.ruinWithinAbsorptionLostPerSecond` | 2 | 0..20 |
| `curses.fractureCooldownTicks` | 160 | 20..1200 |
| `curses.fractureDelayTicks` | 30 | 5..100 |
| `curses.fractureDamageFraction` | 0.25 | 0..0.75 |
| `curses.fractureDamageCap` | 6 | 0..40 |

## Cleanup, Effects, and Performance

Death, logout, login, clone/respawn, dimension transition, player removal, level unload, and server shutdown clear owned transient state. Entity removal clears its target modifiers. Unequipping or losing eligibility invalidates state before effects execute; slot-change integration additionally makes modifier/shield cleanup immediate, with a five-tick fallback. Passive cooldown deadlines can remain as bounded inert tombstones until expiry so rapid re-equipping does not refill an active effect. Idle state without an eligible Elder loadout is then evicted.

Each player has only three guardian budgets, one brand, one of each fixed area, one delayed Angle, one fracture echo, at most two shield accounts, and at most fifteen cooldown deadlines. A Worldbreaker cast holds at most 32 victim UUIDs; a Prison cast holds at most 32 enrollment records. Debuff state has a global cap and three keys per affected entity. Ordinary attacks without the relevant eligible Elder passives/mark do not allocate state. Active player state updates every five ticks; Will checks every ten ticks through the existing passive hook. Debuffs are maintained once per level cadence, not rescanned globally for each tracked player every tick.

The Nightshade `BlessingVisuals` integration registers all fifteen individual particle recipes and the three god palettes in common setup. Additional lightweight, bounded dust outlines make the actual fixed centers/areas readable without client-only classes or decorative entities. Decree uses a forward fan; Will a low ring; Kingsguard three orbiting upright shards; Reach a line; Throne a seat/back plus fixed dominion ring and falling weapons; Brand a rising point-mark and detonation ring; Might a torso halo; Grasp a diagonal break; Tribute rising ash; Worldbreaker expanding ground rings; Lance a narrow beam; Angle a reverse-side slash; Prison a raised boundary; Form a vertical split; Unmaking a raised cross/sigil and descending tears. Small already-emitted cosmetic particles finish their short lifetime after cleanup; no gameplay state remains attached to them.

Effect protection extension points: mob-effect tag `divinity_engine:elder_protected_effects` and persistent entity boolean `divinity_engine:elder_protect_effects`. The empty optional tags need no bundled tag files. Unbroken Will also refuses all cleansing in active trials. Vanilla-only benefit removal is intentionally conservative.

## Translation Source Notes

Use `divinity_engine.blessings.<path>` for names and `.description` for the description. Curse equivalents are `divinity_engine.curse.<path>` and `.description`. No shared language file is changed by this module. Suggested short lore lines, followed by a functional description derived from the exact mechanics table:

| Name | Lore line |
| --- | --- |
| Royal Decree | Kneel beneath a word that carries a kingdom. |
| Unbroken Will | The crown bends neither knee nor spirit. |
| Kingsguard | Three vows stand between you and ruin. |
| Sovereign's Reach | A sovereign's blade ends beyond its edge. |
| Throne of the World | Here, the world remembers who must stand. |
| Ruinbrand | Every wound is a promise of collapse. |
| Sacrificial Might | What you endure becomes what you break. |
| Ruinous Grasp | Even the strongest shell has a fault. |
| Ashen Tribute | From a fallen foe, a moment of shelter. |
| Worldbreaker | Pay in blood; answer in ruin. |
| Rift Lance | Draw a line through what should be solid. |
| Impossible Angle | The second edge comes from nowhere expected. |
| Boundary Prison | An edge may be crossed, but never ignored. |
| Unwritten Form | For one heartbeat, certainty loses its hold. |
| Unmaking | Let what is defended learn what is fragile. |
| Broken Crown | Every blow makes the crown heavier. |
| Ruin Within | Wounds linger where ruin has taken root. |
| Fractured Existence | A wound may arrive twice. |

## Verification Checklist and Limits

Implementation-side checks are source inspection, cached mapped-API inspection, registry/ownership assertions, and whitespace checks. No Gradle build, GameTest run, dedicated-server startup, multiplayer test, gameplay balance verification, or performance benchmark was run by the combat implementer. The parent coordinates compilation and integrated tests. These values are initial tuning, not a verified power comparison.

1. Register 15 unique Elder blessings and 3 unique Elder curses; check intended ordering, active/passive flags, all constructor legacy requirements zero, and rank locks enforced by the parent's gate.
2. Equip each ability in each existing slot and activate through R/G/V; reject an unlocked-but-unequipped or ineligible instance. Mix original and Elder blessings without gaining extra slots.
3. Test each mechanic against hostile mobs, bosses, armor, absorption, shields, teams, pets, Vexes, custom tagged summons, players with PvP on/off, and entities from another trial session.
4. Test Royal Decree's cone and resistance; Will's narrow cleanse exclusions; all three guardian budgets breaking; Reach and Angle internal cooldowns and nonrecursion; Throne exit/reentry and its fixed origin.
5. Test Brand direct-only accounting, cap, death/unload cancellation, and no explosion chains; Might hostile-only stack gains and one consumption across a multi-target active; Grasp's already-debuffed/armorless cases; Tribute absorption caps/expiry.
6. Test Worldbreaker's minimum health and nonlethal cost without self-damage callbacks, expanding once-per-target hits, temporary armor cleanup, and unchanged terrain. Confirm no cost for rejected casts.
7. Test Lance against walls and unloaded boundaries; Prison edge contact rate, boss resistance, vertical escape, and one-second PvP limit; Form trigger then exactly one mitigation or expiry cooldown; Unmaking warning and strict one-effect whitelist.
8. Use mixed builds with each legacy reflection/retaliation blessing and verify parent guards prevent recursion while Snowveil/rewind/death-save state still updates. Verify original players without Might do not acquire scope suppression.
9. Remove a blessing or curse while each state is active. Repeat with death, logout/reconnect, clone, dimension transition, target unload, level unload, and shutdown; verify no stale modifiers, budgets, echoes, wards, or extra absorption.
10. Verify both story branches: only the three agreed Branch A modifications and the parent's Branch B cooldown reduction. Confirm curse/clear authored presence messages dispatch once per lifecycle, not every combat hit.
11. Stress capped dense local crowds and simultaneous areas; verify no extra entities, chunk loading, terrain mutation, or uncapped retained target histories. Confirm particle budgets and immediate slot-change cleanup.

Known deliberate limits: not every third-party owner/effect convention can be inferred; integration tags are supplied. Original delayed/toggled damaging abilities need explicit bounded multiplier capture to support Might beyond synchronous callbacks. Vanilla hurt invulnerability can reject a generated strike; the module never forces it through. Temporary absorption cannot identify the source of unrelated absorption changes occurring between observations, so bookkeeping consumes Elder-owned points first. Very crowded queries use a bounded sample rather than an unlimited nearest-target scan. Visuals are functioning particle geometry, not new bespoke entity models.

## Changed Paths Owned by Combat

All paths are relative to the repository root:

- `src/main/java/net/nightshade/divinity_engine/divinity/blessing/elder/ElderBlessing.java`
- `src/main/java/net/nightshade/divinity_engine/divinity/blessing/elder/RoyalDecree.java`
- `src/main/java/net/nightshade/divinity_engine/divinity/blessing/elder/UnbrokenWill.java`
- `src/main/java/net/nightshade/divinity_engine/divinity/blessing/elder/Kingsguard.java`
- `src/main/java/net/nightshade/divinity_engine/divinity/blessing/elder/SovereignsReach.java`
- `src/main/java/net/nightshade/divinity_engine/divinity/blessing/elder/ThroneOfTheWorld.java`
- `src/main/java/net/nightshade/divinity_engine/divinity/blessing/elder/Ruinbrand.java`
- `src/main/java/net/nightshade/divinity_engine/divinity/blessing/elder/SacrificialMight.java`
- `src/main/java/net/nightshade/divinity_engine/divinity/blessing/elder/RuinousGrasp.java`
- `src/main/java/net/nightshade/divinity_engine/divinity/blessing/elder/AshenTribute.java`
- `src/main/java/net/nightshade/divinity_engine/divinity/blessing/elder/Worldbreaker.java`
- `src/main/java/net/nightshade/divinity_engine/divinity/blessing/elder/RiftLance.java`
- `src/main/java/net/nightshade/divinity_engine/divinity/blessing/elder/ImpossibleAngle.java`
- `src/main/java/net/nightshade/divinity_engine/divinity/blessing/elder/BoundaryPrison.java`
- `src/main/java/net/nightshade/divinity_engine/divinity/blessing/elder/UnwrittenForm.java`
- `src/main/java/net/nightshade/divinity_engine/divinity/blessing/elder/Unmaking.java`
- `src/main/java/net/nightshade/divinity_engine/divinity/curse/elder/ElderCurse.java`
- `src/main/java/net/nightshade/divinity_engine/divinity/curse/elder/BrokenCrown.java`
- `src/main/java/net/nightshade/divinity_engine/divinity/curse/elder/RuinWithin.java`
- `src/main/java/net/nightshade/divinity_engine/divinity/curse/elder/FracturedExistence.java`
- `src/main/java/net/nightshade/divinity_engine/util/divinity/elder/combat/ElderAbility.java`
- `src/main/java/net/nightshade/divinity_engine/util/divinity/elder/combat/ElderCombat.java`
- `src/main/java/net/nightshade/divinity_engine/util/divinity/elder/combat/ElderCombatConfig.java`
- `src/main/java/net/nightshade/divinity_engine/util/divinity/elder/combat/ElderCombatState.java`
- `src/main/java/net/nightshade/divinity_engine/util/divinity/elder/combat/ElderTargets.java`
- `src/main/java/net/nightshade/divinity_engine/util/divinity/elder/combat/ElderVisuals.java`
- `src/main/java/net/nightshade/divinity_engine/registry/divinity/blessing/BlessingsRegistry.java`
- `src/main/java/net/nightshade/divinity_engine/registry/divinity/curse/CurseRegistry.java`
- `docs/elder-combat.md`

Existing dirty registry work was preserved. Core handlers, progression code, parent-owned tests, language files, and the mod constructor were not edited by the combat implementer.
