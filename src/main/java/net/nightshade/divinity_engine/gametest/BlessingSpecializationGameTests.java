package net.nightshade.divinity_engine.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.nightshade.divinity_engine.DivinityEngineMod;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.divinity.blessing.lumen.BeaconsGrace;
import net.nightshade.divinity_engine.divinity.gods.BaseGodInstance;
import net.nightshade.divinity_engine.network.cap.player.gods.MainPlayerCapability;
import net.nightshade.divinity_engine.network.cap.player.gods.PlayerGodsCapability;
import net.nightshade.divinity_engine.network.cap.player.narrative.DivineNarrativeCapability;
import net.nightshade.divinity_engine.network.cap.player.narrative.DivineNarrativeData;
import net.nightshade.divinity_engine.network.messages.key.BlessingSlot1Message;
import net.nightshade.divinity_engine.registry.divinity.blessing.BlessingsRegistry;
import net.nightshade.divinity_engine.registry.divinity.gods.GodsRegistry;
import net.nightshade.divinity_engine.util.DivinityEngineHelper;
import net.nightshade.divinity_engine.util.divinity.narrative.BlessingSpecializations;
import net.nightshade.divinity_engine.util.divinity.narrative.NarrativeProgress;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Runs only through Forge's opt-in GameTest discovery, never during ordinary play. */
@GameTestHolder(DivinityEngineMod.MODID)
@PrefixGameTestTemplate(false)
public final class BlessingSpecializationGameTests {
    private static final Map<String, String> TARGETS = Map.ofEntries(
            Map.entry("solarius", "solar_ward"), Map.entry("nashara", "serpents_bite"),
            Map.entry("grond", "stoneform"), Map.entry("varun", "savage_hunger"),
            Map.entry("dravak", "battle_rush"), Map.entry("elaris", "foresight"),
            Map.entry("voltira", "stormstep"), Map.entry("vokar", "soul_leech"),
            Map.entry("nythea", "twilight_veil"), Map.entry("kairoth", "second_chance"),
            Map.entry("cryonis", "icy_pulse"), Map.entry("nerai", "aquatic_grace"),
            Map.entry("zephra", "gale_step"), Map.entry("terra", "thornskin"),
            Map.entry("lumen", "beacons_grace"), Map.entry("mechanos", "clockwork_punch"),
            Map.entry("ignar", "blazing_hands"), Map.entry("aethon", "celestial_ward"));

    @GameTest(template = "pocket_dimension", timeoutTicks = 100)
    public static void all36PermanentBranches(GameTestHelper helper) {
        long time = helper.getLevel().getDayTime();
        try {
            for (var entry : TARGETS.entrySet()) for (String branch : new String[]{"a", "b"}) {
                TestPlayer player = new TestPlayer(helper.getLevel());
                TestPlayer other = new TestPlayer(helper.getLevel());
                try {
                    String god = entry.getKey();
                    BlessingsInstance instance = equip(player, god);
                    equip(other, god);
                    helper.assertFalse(BlessingSpecializations.isActive(player, god, branch), god + ": unearned reward");
                    reward(player, god, branch);
                    helper.assertTrue(BlessingSpecializations.isActive(player, god, branch), god + ": earned reward");
                    helper.assertFalse(BlessingSpecializations.isActive(player, god, opposite(branch)), god + ": opposite branch");
                    helper.assertFalse(BlessingSpecializations.isActive(other, god, branch), god + ": other player");
                    player.variables.setBlessingSlot1(null);
                    helper.assertFalse(BlessingSpecializations.isActive(player, god, branch), god + ": unequipped");
                    player.variables.setBlessingSlot1(instance);
                    instance.getBoundGodInstance().setFavor(-1);
                    helper.assertFalse(BlessingSpecializations.isActive(player, god, branch), god + ": negative standing must suspend the upgrade");
                    helper.assertTrue(player.narrative.get(id(god)).story.specialization().equals("divinity_engine:" + god + "_" + branch),
                            god + ": suspension must not erase the permanent upgrade");
                    instance.getBoundGodInstance().setFavor(0);
                    helper.assertTrue(BlessingSpecializations.isActive(player, god, branch), god + ": reconciliation must restore earned upgrade without favor grinding");
                    PlayerGodsCapability.load(player).getContactedGods().clear();
                    helper.assertFalse(BlessingSpecializations.isActive(player, god, branch), god + ": lost contact");
                    PlayerGodsCapability.load(player).updateGodInstance(instance.getBoundGodInstance(), false);
                    helper.assertTrue(BlessingSpecializations.isActive(player, god, branch), god + ": recontact");
                    CompoundTag stored = instance.getOrCreateTag().copy();
                    checkBonus(helper, player, instance, god, branch);
                    for (String parameter : new String[]{"horizontal_dash_power", "vertical_dash_power", "fall_immunity_duration",
                            "effect_duration", "radius", "knockback_chance", "knockback_strength"}) {
                        if (stored.contains(parameter)) helper.assertTrue(stored.get(parameter).equals(instance.getOrCreateTag().get(parameter)),
                                god + ": specialization rewrote base parameter " + parameter);
                    }
                    player.narrative.adminReset(id(god));
                    helper.assertFalse(BlessingSpecializations.isActive(player, god, branch), god + ": admin reset");
                } finally {
                    BlessingSpecializations.logout(new PlayerEvent.PlayerLoggedOutEvent(player));
                    BlessingSpecializations.logout(new PlayerEvent.PlayerLoggedOutEvent(other));
                    player.discard(); other.discard();
                }
            }
        } finally {
            helper.getLevel().setDayTime(time);
            helper.getLevel().updateSkyBrightness();
        }
        helper.succeed();
    }

    private static void checkBonus(GameTestHelper h, TestPlayer p, BlessingsInstance b, String god, String branch) {
        boolean a = branch.equals("a");
        ServerLevel level = h.getLevel();
        level.setDayTime(god.equals("nythea") ? 18000 : 6000);
        level.updateSkyBrightness();
        p.setPos(h.absoluteVec(new Vec3(2, 2, 2)));
        p.setHealth(10);
        Pig target = new Pig(EntityType.PIG, level);
        target.setPos(p.position().add(1, 0, 0));
        LivingHurtEvent hit = new LivingHurtEvent(target, level.damageSources().playerAttack(p), 1);
        LivingDeathEvent kill = new LivingDeathEvent(target, level.damageSources().playerAttack(p));
        switch (god) {
            case "solarius" -> {
                p.setHealth(a ? 8 : 12);
                b.onPressed(p);
                duration(h, p, MobEffects.ABSORPTION, a ? 300 : 200);
                duration(h, p, MobEffects.GLOWING, a ? 300 : 200);
                h.assertTrue(b.getCooldown() == 600, "Solar Ward press starts its cooldown");
            }
            case "nashara" -> {
                b.onDamageEntity(p, hit);
                duration(h, target, MobEffects.POISON, a ? 140 : 100);
                if (!a) duration(h, target, MobEffects.WEAKNESS, 40);
            }
            case "grond" -> {
                b.onTakenDamage(new LivingDamageEvent(p, level.damageSources().mobAttack(target), 1));
                duration(h, p, MobEffects.DAMAGE_RESISTANCE, a ? 240 : 200);
                h.assertTrue(b.getCooldown() == (a ? 500 : 400), "Stoneform cooldown");
            }
            case "varun" -> {
                b.onKillEntity(p, kill);
                duration(h, p, MobEffects.DAMAGE_BOOST, a ? 100 : 140);
                duration(h, p, MobEffects.MOVEMENT_SPEED, a ? 100 : 140);
                close(h, p.getHealth(), a ? 11 : 10, "Savage Hunger heal");
            }
            case "dravak" -> {
                b.onKillEntity(p, kill);
                duration(h, p, MobEffects.DAMAGE_BOOST, a ? 140 : 100);
                duration(h, p, MobEffects.MOVEMENT_SPEED, 100);
                if (!a) duration(h, p, MobEffects.DAMAGE_RESISTANCE, 40);
            }
            case "elaris" -> {
                for (int i = 0; i < (a ? 4 : 3); i++) {
                    LivingDamageEvent incoming = new LivingDamageEvent(p, level.damageSources().mobAttack(target), 1);
                    b.onTakenDamage(incoming);
                    h.assertTrue(incoming.isCanceled(), "Foresight dodge " + i);
                }
                b.onTakenDamage(new LivingDamageEvent(p, level.damageSources().mobAttack(target), 1));
                h.assertTrue(b.getCooldown() == (a ? 400 : 320), "Foresight cooldown");
            }
            case "voltira" -> {
                p.setXRot(0); p.setYRot(0); p.setShiftKeyDown(!a);
                b.onPressed(p);
                if (a) close(h, p.getDeltaMovement().horizontalDistance(), 3, "Stormstep horizontal power");
                else duration(h, p, MobEffects.SLOW_FALLING, 160);
            }
            case "vokar" -> {
                b.onKillEntity(p, kill);
                close(h, p.getHealth(), a ? 13 : 12, "Soul Leech healing");
                if (!a) duration(h, p, MobEffects.MOVEMENT_SPEED, 60);
            }
            case "nythea" -> {
                b.onTick(p);
                close(h, p.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(BlessingSpecializations.TWILIGHT_SPEED).getAmount(),
                        a ? 0.1 : 0.15, "Twilight Veil speed");
                if (a) h.assertTrue(p.hasEffect(MobEffects.NIGHT_VISION), "Twilight Veil night vision");
            }
            case "kairoth" -> {
                LivingDeathEvent death = new LivingDeathEvent(p, level.damageSources().mobAttack(target));
                b.onDeath(death);
                h.assertTrue(death.isCanceled(), "Second Chance saves player");
                close(h, p.getHealth(), a ? 12 : 10, "Second Chance revival health");
                if (!a) duration(h, p, MobEffects.ABSORPTION, 100);
            }
            case "cryonis" -> {
                target.setPos(p.position().add(a ? 3.85 : 2, 0, 0));
                level.addFreshEntity(target);
                try {
                    b.onDamageEntity(p, hit);
                    duration(h, target, MobEffects.MOVEMENT_SLOWDOWN, a ? 60 : 100);
                } finally { target.discard(); }
            }
            case "nerai" -> {
                p.submerged = true;
                p.getPersistentData().putLong("divinity_engine.aquatic_heal_at", level.getGameTime());
                b.onTick(p);
                if (a) {
                    close(h, p.getHealth(), 11, "Aquatic Grace healing");
                    b.onTick(p);
                    close(h, p.getHealth(), 11, "Aquatic Grace cannot double heal on a duplicate tick");
                } else close(h, p.getAttribute(ForgeMod.SWIM_SPEED.get()).getModifier(BlessingSpecializations.AQUATIC_SPEED).getAmount(), 0.2, "Aquatic Grace swim speed");
            }
            case "zephra" -> {
                if (a) {
                    b.onTick(p);
                    close(h, p.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(BlessingSpecializations.GALE_SPEED).getAmount(), 0.05, "Gale Step speed");
                } else {
                    LivingFallEvent fall = new LivingFallEvent(p, 10, 1);
                    b.onFall(fall);
                    close(h, fall.getDamageMultiplier(), 0.75, "Gale Step fall reduction");
                }
            }
            case "terra" -> {
                float before = target.getHealth();
                b.onTakenDamage(new LivingDamageEvent(p, level.damageSources().mobAttack(target), 1));
                float damage = before - target.getHealth();
                h.assertTrue(damage >= (a ? 1 : 2) && damage < (a ? 2 : 3), "Thornskin retaliation damage");
                if (a) duration(h, target, MobEffects.MOVEMENT_SLOWDOWN, 40);
            }
            case "lumen" -> {
                h.assertTrue(BeaconsGrace.qualifyingLightThreshold(p) == (a ? 9 : 12), "Beacon's Grace block-light threshold");
                // Use a sky-visible position; the existing daylight condition remains valid for either path.
                p.setPos(p.getX(), level.getMaxBuildHeight() - 2, p.getZ());
                b.onTick(p);
                if (!a) h.assertTrue(p.hasEffect(MobEffects.DAMAGE_RESISTANCE), "Beacon's Grace resistance");
            }
            case "mechanos" -> {
                seedRoll(p, a ? 0.31f : 0.01f, a ? 0.39f : 0.1f);
                b.onDamageEntity(p, hit);
                close(h, target.getDeltaMovement().horizontalDistance(), a ? 2 : 2.5, "Clockwork Punch chance/strength");
            }
            case "ignar" -> {
                seedRoll(p, a ? 0.16f : 0.01f, a ? 0.24f : 0.1f);
                b.onDamageEntity(p, hit);
                h.assertTrue(target.getRemainingFireTicks() == (a ? 60 : 100), "Blazing Hands chance/duration");
            }
            case "aethon" -> {
                ProjectileImpactEvent impact = impact(level, p);
                b.onProjectileHit(p, impact);
                h.assertTrue(impact.isCanceled(), "Celestial Ward blocked projectile");
                h.assertTrue(b.getCooldown() == (a ? 480 : 600), "Celestial Ward cooldown");
                close(h, p.getHealth(), a ? 10 : 11, "Celestial Ward healing");
            }
            default -> throw new AssertionError("Missing specialization test: " + god);
        }
    }

    @GameTest(template = "pocket_dimension")
    public static void releaseDoesNotReplayPress(GameTestHelper h) {
        TestPlayer p = new TestPlayer(h.getLevel());
        p.setPos(h.absoluteVec(new Vec3(2, 2, 2)));
        BlessingsInstance b = equip(p, "voltira");
        p.setShiftKeyDown(true);
        BlessingSlot1Message.pressAction(p, 0, 0);
        close(h, p.getDeltaMovement().y, 1.5, "Initial vertical dash");
        p.setDeltaMovement(Vec3.ZERO);
        BlessingSlot1Message.pressAction(p, 1, Integer.MAX_VALUE);
        close(h, p.getDeltaMovement().length(), 0, "Release must not dash again");
        BlessingSlot1Message.pressAction(p, 1, Integer.MAX_VALUE);
        close(h, p.getDeltaMovement().length(), 0, "Repeated release ignored");
        p.setShiftKeyDown(false);
        BlessingSlot1Message.pressAction(p, 0, 0);
        h.assertTrue(b.getCooldown() == 300, "Horizontal press starts cooldown immediately");
        BlessingSlot1Message.pressAction(p, 1, 0);
        b.setCooldown(0);
        p.setDeltaMovement(Vec3.ZERO);
        BlessingSlot1Message.pressAction(p, 0, 0);
        h.assertTrue(p.getDeltaMovement().length() > 0, "Release during cooldown clears input latch");
        h.succeed();
    }

    @GameTest(template = "pocket_dimension")
    public static void cooldownPersistenceAndProjectileIsolation(GameTestHelper h) {
        TestPlayer first = new TestPlayer(h.getLevel()), second = new TestPlayer(h.getLevel());
        BlessingsInstance a = equip(first, "aethon"), b = equip(second, "aethon");
        reward(first, "aethon", "a");
        a.onProjectileHit(first, impact(h.getLevel(), first));
        h.assertTrue(a.getCooldown() == 480 && b.getCooldown() == 0, "Cooldown belongs to first player only");
        ProjectileImpactEvent secondImpact = impact(h.getLevel(), second);
        b.onProjectileHit(second, secondImpact);
        h.assertTrue(secondImpact.isCanceled() && b.getCooldown() == 600, "Second player's ward is independent");
        BlessingsInstance restored = BlessingsInstance.fromNBT(a.toNBT());
        h.assertTrue(restored.getCooldown() == 480, "Deserialization must not apply the reduction again");
        restored.decreaseCooldown(2);
        h.assertTrue(restored.getCooldown() == 478, "Tick decrement must not apply a specialization reduction");
        restored.setCooldown(0);
        ProjectileImpactEvent repeated = impact(h.getLevel(), first);
        restored.onProjectileHit(first, repeated);
        h.assertFalse(repeated.isCanceled(), "Same-tick replay rejected by per-player deadline");
        h.assertTrue(restored.getCooldown() == 0, "An unsuccessful block must not start cooldown");
        h.succeed();
    }

    @GameTest(template = "pocket_dimension")
    public static void retaliationAndOwnedEffectSafety(GameTestHelper h) {
        TestPlayer p = new TestPlayer(h.getLevel());
        BlessingsInstance thornskin = equip(p, "terra");
        Pig attacker = new Pig(EntityType.PIG, h.getLevel());
        float health = attacker.getHealth();
        thornskin.onTakenDamage(new LivingDamageEvent(p, h.getLevel().damageSources().thorns(attacker), 1));
        close(h, attacker.getHealth(), health, "Thornskin must not recurse on thorns damage");
        h.assertTrue(thornskin.getCooldown() == 0, "Ignored retaliation does not consume cooldown");
        BlessingSpecializations.effect(p, MobEffects.NIGHT_VISION, true);
        BlessingSpecializations.effect(p, MobEffects.NIGHT_VISION, false);
        h.assertFalse(p.hasEffect(MobEffects.NIGHT_VISION), "Owned night vision removed");
        p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 1200));
        BlessingSpecializations.effect(p, MobEffects.NIGHT_VISION, true);
        BlessingSpecializations.effect(p, MobEffects.NIGHT_VISION, false);
        duration(h, p, MobEffects.NIGHT_VISION, 1200);
        BlessingSpecializations.effect(p, MobEffects.DAMAGE_RESISTANCE, true);
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 1200, 1));
        BlessingSpecializations.effect(p, MobEffects.DAMAGE_RESISTANCE, false);
        duration(h, p, MobEffects.DAMAGE_RESISTANCE, 1200);
        UUID foreignId = UUID.randomUUID();
        p.getAttribute(Attributes.MOVEMENT_SPEED).addTransientModifier(new AttributeModifier(foreignId, "foreign modifier", 0.1, AttributeModifier.Operation.ADDITION));
        BlessingSpecializations.modifier(p, Attributes.MOVEMENT_SPEED, BlessingSpecializations.GALE_SPEED, "test", 0.05);
        BlessingSpecializations.cleanupIneligible(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, p));
        h.assertTrue(p.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(BlessingSpecializations.GALE_SPEED) == null, "Unequipped specialization modifier removed");
        h.assertTrue(p.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(foreignId) != null, "Foreign modifier preserved");
        h.succeed();
    }

    private static BlessingsInstance equip(TestPlayer player, String path) {
        var god = GodsRegistry.GODS_REGISTRY.get().getValue(id(path));
        var blessing = BlessingsRegistry.BLESSINGS_REGISTRY.get().getValue(id(TARGETS.get(path)));
        if (god == null || blessing == null) throw new AssertionError("Missing built-in: " + path);
        BaseGodInstance bound = god.createGodDefaultInstance();
        bound.setFavor(250);
        PlayerGodsCapability.load(player).updateGodInstance(bound, false);
        BlessingsInstance instance = blessing.createDefaultInstance();
        instance.setBoundGod(bound);
        player.variables.setBlessingSlot1(instance);
        return instance;
    }

    @GameTest(template = "pocket_dimension")
    public static void playerTickCooldownsStayIndependent(GameTestHelper h) {
        TestPlayer first = new TestPlayer(h.getLevel()), second = new TestPlayer(h.getLevel());
        BlessingsInstance a = equip(first, "voltira"), b = equip(second, "voltira");
        a.setCooldown(20); b.setCooldown(20);
        first.tickCount = 2; second.tickCount = 2;
        net.nightshade.divinity_engine.network.events.divinity.DivinityTickHandler.onPlayerTick(
                new TickEvent.PlayerTickEvent(TickEvent.Phase.START, first));
        h.assertTrue(a.getCooldown() == 20, "START phase must not decrement cooldowns");
        net.nightshade.divinity_engine.network.events.divinity.DivinityTickHandler.onPlayerTick(
                new TickEvent.PlayerTickEvent(TickEvent.Phase.END, first));
        h.assertTrue(a.getCooldown() == 18 && b.getCooldown() == 20, "One player's tick must run once and remain isolated");
        net.nightshade.divinity_engine.network.events.divinity.DivinityTickHandler.onPlayerTick(
                new TickEvent.PlayerTickEvent(TickEvent.Phase.END, second));
        h.assertTrue(b.getCooldown() == 18, "Second player has an independent tick");
        first.discard(); second.discard(); h.succeed();
    }

    private static void reward(TestPlayer player, String god, String branch) {
        player.narrative.get(id(god)).story.restore(NarrativeProgress.Stage.COMPLETE, branch, 0,
                "divinity_engine:" + god + "_" + branch, "", Set.of());
    }

    private static ResourceLocation id(String path) { return new ResourceLocation(DivinityEngineMod.MODID, path); }
    private static String opposite(String branch) { return branch.equals("a") ? "b" : "a"; }
    private static void close(GameTestHelper h, double value, double expected, String message) {
        h.assertTrue(Math.abs(value - expected) < 0.0001, message + ": expected " + expected + ", got " + value);
    }
    private static void duration(GameTestHelper h, net.minecraft.world.entity.LivingEntity entity, MobEffect effect, int ticks) {
        h.assertTrue(entity.getEffect(effect) != null, "Missing effect " + effect.getDescriptionId());
        h.assertTrue(entity.getEffect(effect).getDuration() == ticks, effect.getDescriptionId() + ": duration " + ticks);
    }
    private static ProjectileImpactEvent impact(ServerLevel level, TestPlayer player) {
        return new ProjectileImpactEvent(new Arrow(EntityType.ARROW, level), new EntityHitResult(player));
    }
    private static void seedRoll(TestPlayer p, float min, float max) {
        for (long seed = 0; seed < 100000; seed++) {
            RandomSource random = RandomSource.create(seed);
            float roll = random.nextFloat();
            if (roll > min && roll < max) { p.getRandom().setSeed(seed); return; }
        }
        throw new AssertionError("Could not find deterministic random seed");
    }

    /** Fake players normally do not receive narrative/main capabilities. Tests explicitly provide isolated copies. */
    private static final class TestPlayer extends FakePlayer {
        final DivineNarrativeData narrative = new DivineNarrativeData();
        final MainPlayerCapability.PlayerVariables variables = new MainPlayerCapability.PlayerVariables();
        private final LazyOptional<DivineNarrativeData> narrativeCap = LazyOptional.of(() -> narrative);
        private final LazyOptional<MainPlayerCapability.PlayerVariables> variablesCap = LazyOptional.of(() -> variables);
        boolean submerged;
        TestPlayer(ServerLevel level) { super(level, new GameProfile(UUID.randomUUID(), "specialization-test")); }
        @Override public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
            if (cap == DivineNarrativeCapability.CAP && narrativeCap != null) return narrativeCap.cast();
            if (cap == MainPlayerCapability.PLAYER_VARIABLES_CAPABILITY && variablesCap != null) return variablesCap.cast();
            return super.getCapability(cap, side);
        }
        @Override public boolean isInWaterOrBubble() { return submerged || super.isInWaterOrBubble(); }
        @Override public boolean isEyeInFluid(TagKey<Fluid> fluid) { return submerged && fluid == FluidTags.WATER || super.isEyeInFluid(fluid); }
    }
}
