package net.nightshade.divinity_engine.gametest;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestRegistry;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;
import net.nightshade.divinity_engine.api.content.DivinityContent;
import net.nightshade.divinity_engine.block.StatueBlock;
import net.nightshade.divinity_engine.block.entity.StatueBlockEntity;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.divinity.gods.BaseGod;
import net.nightshade.divinity_engine.network.cap.player.gods.PlayerGodsCapability;
import net.nightshade.divinity_engine.registry.divinity.blessing.BlessingsRegistry;
import net.nightshade.divinity_engine.registry.divinity.gods.GodsRegistry;
import net.nightshade.divinity_engine.registry.divinity.trial.GodlyTrialsRegistry;
import net.nightshade.divinity_engine.util.DivinityEngineHelper;
import net.nightshade.divinity_engine.util.content.ContentBlessing;
import net.nightshade.divinity_engine.util.content.ContentGod;
import net.nightshade.divinity_engine.util.content.ContentSchema;
import net.nightshade.divinity_engine.util.content.ContentTrial;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Disposable server-world tests; no KubeJS, client, script dispatcher, or live trial session required. */
@Mod.EventBusSubscriber(modid = "divinity_engine", bus = Mod.EventBusSubscriber.Bus.MOD)
@PrefixGameTestTemplate(false)
public final class ContentDefinitionGameTests {
    private static final String SUITE = "divinity_content";
    private static final ResourceLocation GOD = id("fixture_god");
    private static final ResourceLocation TRIAL = id("fixture_trial");
    private static final List<String> BLESSINGS = List.of("heal", "effect", "damage", "dash", "area", "toggle", "missing_target");
    private static boolean fixturesRegistered;
    private static boolean duplicateRejected;

    private ContentDefinitionGameTests() {}

    /** Call once after DivinityContent.bootstrap(bus), before Forge's first RegisterEvent. */
    public static void registerFixtures(IEventBus bus) {
        if (!enabled() || fixturesRegistered) return;
        Objects.requireNonNull(bus);
        JsonObject heal = blessingSpec("active", "{\"pressed\":[{\"type\":\"heal\",\"amount\":2}]}");
        DivinityContent.registerBlessing(id("heal"), heal);
        duplicateRejected = rejects(IllegalArgumentException.class, () -> DivinityContent.registerBlessing(id("heal"), heal));
        // Mutating the caller's JSON after registration must not change the live definition.
        heal.getAsJsonObject("hooks").getAsJsonArray("pressed").get(0).getAsJsonObject().addProperty("amount", 40);
        DivinityContent.registerBlessing(id("effect"), blessingSpec("active", """
                {"pressed":[{"type":"effect","effect":"minecraft:speed","ticks":120,"amplifier":1}]}
                """));
        DivinityContent.registerBlessing(id("damage"), blessingSpec("passive", """
                {"damage_entity":[{"type":"damage","target":"target","amount":3}]}
                """));
        DivinityContent.registerBlessing(id("dash"), blessingSpec("active", "{\"pressed\":[{\"type\":\"dash\",\"amount\":3}]}"));
        DivinityContent.registerBlessing(id("area"), blessingSpec("active", """
                {"pressed":[{"type":"effect","target":"nearby_hostiles","radius":3,
                "effect":"minecraft:slowness","ticks":60,"amplifier":0}]}
                """));
        DivinityContent.registerBlessing(id("toggle"), blessingSpec("toggle", """
                {"toggle_on":[{"type":"heal","amount":1}],"tick":[{"type":"heal","amount":1}],
                "toggle_off":[{"type":"heal","amount":1}]}
                """));
        DivinityContent.registerBlessing(id("missing_target"), blessingSpec("active", """
                {"pressed":[{"type":"heal","target":"target","amount":2}]}
                """));
        DivinityContent.registerGod(GOD, godSpec());
        DivinityContent.registerTrial(TRIAL, trialSpec());
        fixturesRegistered = true;
    }

    private static boolean enabled() {
        return Arrays.stream(System.getProperty("forge.enabledGameTestNamespaces", "").split(","))
                .map(String::trim).anyMatch(SUITE::equals);
    }

    @SubscribeEvent
    @SuppressWarnings("deprecation")
    public static void register(RegisterGameTestsEvent event) {
        if (!enabled()) return;
        for (var method : ContentDefinitionGameTests.class.getDeclaredMethods()) {
            if (method.isAnnotationPresent(GameTest.class)) GameTestRegistry.register(method, Set.of("divinity_engine"));
        }
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", batch = SUITE)
    public static void registeredContentBindsGodBlessingsAndStatue(GameTestHelper h) {
        h.assertTrue(fixturesRegistered && duplicateRejected, "Fixtures missing or duplicate startup ID was accepted");
        BaseGod god = god();
        h.assertTrue(god instanceof ContentGod && GOD.equals(god.getRegistryName()), "Content god did not enter the real registry");
        h.assertTrue(god.getDomains().equals(Set.of("content_test")), "God domains changed");
        h.assertTrue(god.getBlessings().size() == BLESSINGS.size(), "God lost declared blessings");
        var bound = god.createGodDefaultInstance();
        for (String name : BLESSINGS) {
            var blessing = instance(name);
            h.assertTrue(blessing.getBlessing() instanceof ContentBlessing, "Wrong registry factory for " + name);
            var member = bound.getBlessing(blessing.getBlessing());
            h.assertTrue(member != null && member.getBoundGod() == god, "God kit binding missing for " + name);
        }
        ResourceLocation statueId = id("fixture_god_statue");
        var block = ForgeRegistries.BLOCKS.getValue(statueId);
        var type = ForgeRegistries.BLOCK_ENTITY_TYPES.getValue(statueId);
        h.assertTrue(block instanceof StatueBlock && ((StatueBlock) block).getGod() == god, "Statue has wrong god");
        h.assertTrue(god.getStatueBlock() == block && god.getStatueBlockEntity() == type, "God lost automatic statue handles");
        h.assertTrue(ForgeRegistries.ITEMS.getValue(statueId) == block.asItem(), "Statue item is not registered with its block");
        var state = block.defaultBlockState();
        var entity = ((StatueBlock) block).newBlockEntity(BlockPos.ZERO, state);
        h.assertTrue(entity instanceof StatueBlockEntity && entity.getType() == type, "Statue factory returned wrong type");
        var saved = entity.saveWithFullMetadata();
        h.assertTrue(statueId.toString().equals(saved.getString("id")), "Statue NBT lost its namespaced ID");
        var restored = BlockEntity.loadStatic(BlockPos.ZERO, state, saved);
        h.assertTrue(restored != null && restored.getType() == type && saved.equals(restored.saveWithFullMetadata()),
                "Content statue did not survive registered NBT reconstruction");
        h.succeed();
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", batch = SUITE)
    public static void registeredTrialCreatesRealConfiguredPhases(GameTestHelper h) {
        try (Fixture f = new Fixture(h)) {
            var definition = GodlyTrialsRegistry.get(TRIAL);
            h.assertTrue(definition != null && definition.getGod() == god() && GodlyTrialsRegistry.get(god()) == definition,
                    "Trial did not bind to the registered content god");
            var player = f.player();
            Vec3 origin = player.position();
            var trial = GodlyTrialsRegistry.createTrial(TRIAL, player);
            h.assertTrue(trial instanceof ContentTrial && trial.getPlayer() == player && trial.getMaxPhaseCount() == 2,
                    "Registry did not create the real configured ContentTrial");
            // Empty survival phases exercise real phase setup without spawning mobs or opening an arena session.
            trial.beginPhase(1);
            h.assertTrue(trial.getPhaseName().equals("First watch") && trial.getObjectiveText().equals("Remain here")
                    && trial.getPhaseDuration() == 20 && trial.getObjectiveTarget() == 20 && trial.getObjectiveCurrent() == 0,
                    "First survival phase ignored JSON metadata");
            trial.tick();
            trial.beginPhase(2);
            h.assertTrue(trial.getPhaseName().equals("Second watch") && trial.getPhaseDuration() == 40
                    && trial.getObjectiveTarget() == 40 && trial.getObjectiveCurrent() == 0, "Second phase did not reset its objective");
            h.assertTrue(origin.equals(player.position()), "Constructing/configuring a trial transported the player");
            h.succeed();
        }
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", batch = SUITE)
    public static void schemaRejectsUnknownFieldsAndUnsupportedHooks(GameTestHelper h) {
        JsonObject god = godSpec();
        god.addProperty("unknown", true);
        invalid(h, () -> ContentSchema.god(god), "Unknown god field");
        JsonObject blessing = blessingSpec("active", "{}");
        blessing.addProperty("unknown", true);
        invalid(h, () -> ContentSchema.blessing(blessing), "Unknown blessing field");
        JsonObject trial = trialSpec();
        trial.getAsJsonArray("phases").get(0).getAsJsonObject().addProperty("commands", "say unsafe");
        invalid(h, () -> ContentSchema.trial(trial), "Unknown phase field");
        for (String hooks : List.of("{\"unknown\":[]}", "{\"pressed\":[{\"type\":\"heal\",\"command\":\"op\"}]}",
                "{\"pressed\":[{\"type\":\"teleport\"}]}", "{\"pressed\":[{\"type\":\"dash\",\"target\":\"target\"}]}",
                "{\"toggle_on\":[]}")) {
            invalid(h, () -> ContentSchema.blessing(blessingSpec("active", hooks)), "Unsupported hook/action/field: " + hooks);
        }
        invalid(h, () -> ContentSchema.blessing(blessingSpec("passive", "{\"pressed\":[]}")), "Passive pressed hook");
        for (String mode : List.of("active", "passive", "toggle")) {
            Set<String> reachable = switch (mode) {
                case "active" -> Set.of("pressed");
                case "passive" -> Set.of("tick", "damage_entity", "taken_damage", "kill_entity");
                default -> Set.of("tick", "damage_entity", "taken_damage", "kill_entity", "toggle_on", "toggle_off");
            };
            for (String hook : ContentSchema.HOOKS) {
                for (String actions : List.of("[]", "[{\"type\":\"heal\",\"amount\":1}]")) {
                    var spec = blessingSpec(mode, "{\"" + hook + "\":" + actions + "}");
                    if (reachable.contains(hook)) {
                        h.assertTrue(ContentSchema.blessing(spec).hooks().containsKey(hook), "Reachable " + mode + "/" + hook + " rejected");
                    } else {
                        invalid(h, () -> ContentSchema.blessing(spec), "Unreachable " + mode + "/" + hook + " with " + actions);
                    }
                }
            }
            h.assertTrue(ContentSchema.blessing(blessingSpec(mode, "{}")).hooks().isEmpty(), "Empty hooks must remain valid for scripts");
            var hookless = blessingSpec(mode, "{}");
            hookless.remove("hooks");
            h.assertTrue(ContentSchema.blessing(hookless).hooks().isEmpty(), "Omitted hooks must remain valid for scripts");
        }
        var defaultMode = blessingSpec("active", "{\"tick\":[]}");
        defaultMode.remove("mode");
        invalid(h, () -> ContentSchema.blessing(defaultMode), "Default active mode with tick hook");
        invalid(h, () -> ContentSchema.id("missing_namespace"), "Implicit namespace");
        invalid(h, () -> ContentSchema.id("pack:../escape"), "Traversal ID");
        h.succeed();
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", batch = SUITE)
    public static void schemaEnforcesNumericAndCollectionBounds(GameTestHelper h) {
        for (String action : List.of("{\"type\":\"heal\",\"amount\":40.01}", "{\"type\":\"damage\",\"amount\":0}",
                "{\"type\":\"dash\",\"amount\":3.01}", "{\"type\":\"heal\",\"radius\":12.01}",
                "{\"type\":\"effect\",\"effect\":\"minecraft:speed\",\"ticks\":12001}",
                "{\"type\":\"effect\",\"effect\":\"minecraft:speed\",\"amplifier\":5}",
                "{\"type\":\"ignite\",\"ticks\":1.5}")) {
            invalid(h, () -> ContentSchema.blessing(blessingSpec("active", "{\"pressed\":[" + action + "]}")), "Out-of-range action: " + action);
        }
        for (Number value : List.of(Double.NaN, Double.POSITIVE_INFINITY, -1, 3601, 1.5)) {
            var spec = blessingSpec("active", "{}");
            spec.addProperty("cooldown_seconds", value);
            invalid(h, () -> ContentSchema.blessing(spec), "Invalid numeric cooldown " + value);
        }
        var actions = new JsonArray();
        for (int i = 0; i < 16; i++) actions.add(json("{\"type\":\"heal\",\"amount\":40}"));
        var spec = blessingSpec("active", "{}");
        spec.getAsJsonObject("hooks").add("pressed", actions);
        h.assertTrue(ContentSchema.blessing(spec).hooks().get("pressed").size() == 16, "Maximum legal action list was rejected");
        actions.add(json("{\"type\":\"heal\"}"));
        invalid(h, () -> ContentSchema.blessing(spec), "Seventeenth action");
        var trial = trialSpec();
        var phase = trial.getAsJsonArray("phases").get(0).getAsJsonObject();
        phase.addProperty("goal", "defeat_all");
        phase.add("spawns", JsonParser.parseString("[{\"entity\":\"minecraft:zombie\",\"count\":16},{\"entity\":\"minecraft:zombie\",\"count\":16}]"));
        h.assertTrue(ContentSchema.trial(trial).phases().get(0).spawns().size() == 2, "Maximum legal phase population rejected");
        phase.getAsJsonArray("spawns").add(json("{\"entity\":\"minecraft:zombie\",\"count\":1}"));
        invalid(h, () -> ContentSchema.trial(trial), "Thirty-third phase mob");
        var tooMany = trialSpec();
        while (tooMany.getAsJsonArray("phases").size() < 9) tooMany.getAsJsonArray("phases").add(tooMany.getAsJsonArray("phases").get(0).deepCopy());
        invalid(h, () -> ContentSchema.trial(tooMany), "Ninth trial phase");
        h.succeed();
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", batch = SUITE)
    public static void referenceValidationRejectsUnknownsWithoutRegistering(GameTestHelper h) {
        String before = DivinityContent.fingerprint();
        var unknownBlessing = godSpec();
        unknownBlessing.getAsJsonArray("blessings").add(id("not_registered").toString());
        invalid(h, () -> DivinityContent.validateReferences("gods", id("candidate_god"), unknownBlessing), "Unknown god-kit blessing");
        var unknownOffering = godSpec();
        unknownOffering.getAsJsonObject("offerings").addProperty("divinity_content:not_registered", 1);
        invalid(h, () -> DivinityContent.validateReferences("gods", id("candidate_god"), unknownOffering), "Unknown offering item");
        var unknownEffect = blessingSpec("active", "{\"pressed\":[{\"type\":\"effect\",\"effect\":\"divinity_content:not_registered\"}]}");
        invalid(h, () -> DivinityContent.validateReferences("blessings", id("candidate_blessing"), unknownEffect), "Unknown effect");
        var unknownGod = trialSpec();
        unknownGod.addProperty("god", id("not_registered").toString());
        invalid(h, () -> DivinityContent.validateReferences("trials", id("candidate_trial"), unknownGod), "Unknown trial god");
        invalid(h, () -> DivinityContent.validateReferences("trials", TRIAL, trialSpec()), "Replacing a registered trial");
        DivinityContent.validateReferences("gods", id("candidate_god"), godSpec());
        DivinityContent.validateReferences("blessings", id("candidate_blessing"), blessingSpec("active", "{}"));
        h.assertTrue(before.equals(DivinityContent.fingerprint()) && !GodsRegistry.GODS_REGISTRY.get().containsKey(id("candidate_god"))
                && !BlessingsRegistry.BLESSINGS_REGISTRY.get().containsKey(id("candidate_blessing"))
                && GodlyTrialsRegistry.get(id("candidate_trial")) == null, "Dry-run validation changed registered content");
        h.succeed();
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", batch = SUITE)
    public static void canonicalHashIgnoresObjectKeyOrderButNotBehaviorOrder(GameTestHelper h) {
        var first = json("{\"z\":2,\"a\":{\"y\":1,\"b\":[{\"z\":3,\"a\":4},2]}}");
        var reordered = json("{\"a\":{\"b\":[{\"a\":4,\"z\":3},2],\"y\":1},\"z\":2}");
        String original = first.toString();
        String canonical = DivinityContent.canonical(first).toString();
        h.assertTrue(canonical.equals("{\"a\":{\"b\":[{\"a\":4,\"z\":3},2],\"y\":1},\"z\":2}"), "Canonical serialization did not sort nested object keys");
        h.assertTrue(DivinityContent.canonicalFingerprint(first).equals(DivinityContent.canonicalFingerprint(reordered)),
                "Equivalent key ordering changed canonical SHA-256");
        reordered.getAsJsonObject("a").add("b", JsonParser.parseString("[2,{\"a\":4,\"z\":3}]"));
        h.assertFalse(DivinityContent.canonicalFingerprint(first).equals(DivinityContent.canonicalFingerprint(reordered)),
                "Canonical hashing discarded action/array order");
        reordered = first.deepCopy();
        reordered.addProperty("z", 3);
        h.assertFalse(DivinityContent.canonicalFingerprint(first).equals(DivinityContent.canonicalFingerprint(reordered)),
                "Changed behavior value retained the same canonical hash");
        h.assertTrue(first.toString().equals(original), "Canonicalization mutated caller JSON");
        h.assertTrue(DivinityContent.fingerprint().matches("content-1-[0-9a-f]{64}"), "Startup handshake fingerprint has wrong format");
        h.succeed();
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", batch = SUITE)
    public static void frozenDeclarationsCannotMutateContentOrFingerprint(GameTestHelper h) {
        String before = DivinityContent.fingerprint();
        h.assertTrue(rejects(IllegalStateException.class, () -> DivinityContent.registerGod(id("late_god"), godSpec())), "Late god accepted");
        h.assertTrue(rejects(IllegalStateException.class, () -> DivinityContent.registerBlessing(id("late_blessing"), blessingSpec("active", "{}"))), "Late blessing accepted");
        h.assertTrue(rejects(IllegalStateException.class, () -> DivinityContent.registerTrial(id("late_trial"), trialSpec())), "Late trial accepted");
        h.assertTrue(before.equals(DivinityContent.fingerprint()) && !GodsRegistry.GODS_REGISTRY.get().containsKey(id("late_god"))
                && !BlessingsRegistry.BLESSINGS_REGISTRY.get().containsKey(id("late_blessing"))
                && GodlyTrialsRegistry.get(id("late_trial")) == null, "Rejected declarations mutated frozen content");
        h.succeed();
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", batch = SUITE)
    public static void healUsesCopiedAmountClampsHealthAndReportsNoop(GameTestHelper h) {
        try (Fixture f = new Fixture(h)) {
            var owner = f.pig();
            var blessing = instance("heal");
            owner.setHealth(3);
            h.assertTrue(blessing.getBlessing().onPressed(blessing, owner), "Valid heal was unsuccessful");
            close(h, owner.getHealth(), 5, "Caller mutation changed the registered heal amount");
            owner.setHealth(owner.getMaxHealth() - 1);
            h.assertTrue(blessing.getBlessing().onPressed(blessing, owner), "Partial heal failed");
            close(h, owner.getHealth(), owner.getMaxHealth(), "Heal exceeded maximum health");
            h.assertFalse(blessing.getBlessing().onPressed(blessing, owner), "Full-health no-op reported success");
            var missing = instance("missing_target");
            owner.setHealth(3);
            h.assertFalse(missing.getBlessing().onPressed(missing, owner), "Missing explicit target fell back to owner");
            close(h, owner.getHealth(), 3, "Missing-target action healed self");
            h.succeed();
        }
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", batch = SUITE)
    public static void effectAppliesConfiguredDurationAndAmplifier(GameTestHelper h) {
        try (Fixture f = new Fixture(h)) {
            var owner = f.pig();
            var blessing = instance("effect");
            h.assertTrue(blessing.getBlessing().onPressed(blessing, owner), "Effect action failed");
            var effect = owner.getEffect(MobEffects.MOVEMENT_SPEED);
            h.assertTrue(effect != null && effect.getDuration() == 120 && effect.getAmplifier() == 1, "Effect ignored JSON bounds/values");
            blessing.setCooldown(1);
            owner.removeEffect(MobEffects.MOVEMENT_SPEED);
            h.assertFalse(blessing.getBlessing().onPressed(blessing, owner) || owner.hasEffect(MobEffects.MOVEMENT_SPEED), "Effect bypassed cooldown");
            h.succeed();
        }
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", batch = SUITE)
    public static void dashIsBoundedHorizontalAndPreservesVerticalVelocity(GameTestHelper h) {
        try (Fixture f = new Fixture(h)) {
            var owner = f.pig();
            owner.setYRot(37);
            owner.setXRot(-40);
            owner.setDeltaMovement(20, .375, -20);
            Vec3 look = owner.getLookAngle();
            Vec3 expected = new Vec3(look.x, 0, look.z).normalize().scale(3);
            var blessing = instance("dash");
            h.assertTrue(blessing.getBlessing().onPressed(blessing, owner), "Dash action failed");
            Vec3 movement = owner.getDeltaMovement();
            close(h, movement.x, expected.x, "Dash ignored horizontal look X");
            close(h, movement.z, expected.z, "Dash ignored horizontal look Z");
            close(h, movement.y, .375, "Dash overwrote vertical velocity");
            close(h, Math.sqrt(movement.x * movement.x + movement.z * movement.z), 3, "Dash stacked instead of respecting configured speed");
            h.assertTrue(owner.hurtMarked, "Dash did not request motion synchronization");
            h.succeed();
        }
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", batch = SUITE)
    public static void damageUsesExplicitTargetAndRejectsCanceledZeroAndSelf(GameTestHelper h) {
        try (Fixture f = new Fixture(h)) {
            var owner = f.pig();
            var target = f.pig();
            var bystander = f.pig();
            var blessing = instance("damage");
            var canceled = hit(owner, target, 1);
            canceled.setCanceled(true);
            h.assertFalse(blessing.getBlessing().onDamageEntity(blessing, owner, canceled), "Canceled damage triggered actions");
            h.assertFalse(blessing.getBlessing().onDamageEntity(blessing, owner, hit(owner, target, 0)), "Zero damage triggered actions");
            close(h, target.getHealth(), target.getMaxHealth(), "Rejected hook damaged target");
            h.assertTrue(blessing.getBlessing().onDamageEntity(blessing, owner, hit(owner, target, 1)), "Valid damage action failed");
            close(h, target.getHealth(), target.getMaxHealth() - 3, "Damage amount differed from configured three health");
            close(h, bystander.getHealth(), bystander.getMaxHealth(), "Targeted action damaged bystander");
            h.assertFalse(blessing.getBlessing().onDamageEntity(blessing, owner, hit(owner, owner, 1)), "Damage action attacked its owner");
            close(h, owner.getHealth(), owner.getMaxHealth(), "Damage action changed owner health");
            h.succeed();
        }
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", batch = SUITE)
    public static void nearbyActionsSelectOnlyNearestSixteenHostiles(GameTestHelper h) {
        try (Fixture f = new Fixture(h)) {
            var owner = f.pig();
            var targets = new ArrayList<Zombie>();
            for (int i = 0; i < 17; i++) targets.add(f.zombie(owner.position().add(.3 + i * .1, 0, 0)));
            var outside = f.zombie(owner.position().add(3.5, 0, 0));
            var friendly = f.pig();
            friendly.setPos(owner.position().add(.2, 0, 0));
            h.assertTrue(h.getLevel().addFreshEntity(friendly), "Could not insert neutral fixture");
            var blessing = instance("area");
            h.assertTrue(blessing.getBlessing().onPressed(blessing, owner), "Nearby-hostile action found no fixtures");
            for (int i = 0; i < targets.size(); i++) {
                h.assertTrue(targets.get(i).hasEffect(MobEffects.MOVEMENT_SLOWDOWN) == (i < 16), "Nearest-sixteen target selection failed at " + i);
            }
            h.assertFalse(outside.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) || friendly.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)
                    || owner.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "Area action affected out-of-radius, neutral, or owner entity");
            h.succeed();
        }
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", batch = SUITE)
    public static void successfulEquippedCastStartsCooldownAndFailedCastDoesNot(GameTestHelper h) {
        try (Fixture f = new Fixture(h)) {
            var owner = f.player();
            var blessing = equip(owner, "heal");
            blessing.onPressed(owner);
            h.assertTrue(blessing.getCooldown() == 0, "Full-health no-op consumed cooldown");
            blessing.onRelease(owner, 0);
            owner.setHealth(5);
            blessing.onPressed(owner);
            close(h, owner.getHealth(), 7, "Eligible equipped cast did not heal");
            h.assertTrue(blessing.getCooldown() == 40, "Configured two-second cooldown was not started");
            blessing.onRelease(owner, 0);
            owner.setHealth(5);
            blessing.onPressed(owner);
            close(h, owner.getHealth(), 5, "Repeated press bypassed cooldown");
            blessing.setCooldown(0);
            DivinityEngineHelper.setBlessingSlot(owner, 0, null);
            blessing.onPressed(owner);
            close(h, owner.getHealth(), 5, "Unequipped blessing bypassed eligibility");
            DivinityEngineHelper.setBlessingSlot(owner, 0, blessing);
            blessing.onPressed(owner);
            close(h, owner.getHealth(), 7, "Eligible blessing did not recover after cooldown/reset");
            h.succeed();
        }
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", batch = SUITE)
    public static void toggleHooksRespectStateCooldownAndTickInterval(GameTestHelper h) {
        try (Fixture f = new Fixture(h)) {
            var owner = f.player();
            var blessing = equip(owner, "toggle");
            h.assertTrue(blessing.getBlessing().isActive() && blessing.getBlessing().canToggle()
                    && !blessing.getBlessing().isPassive(), "Toggle mode must pass the active key gate and retain toggle dispatch");
            owner.setHealth(3);
            h.assertFalse(blessing.getBlessing().onTick(blessing, owner), "Untoggled blessing ticked");
            blessing.onToggleOn(owner);
            close(h, owner.getHealth(), 4, "Toggle-on action did not heal");
            h.assertTrue(blessing.isToggled() && blessing.getCooldown() == 40, "Toggle-on did not activate/cool down");
            blessing.onToggleOff(owner);
            h.assertFalse(blessing.isToggled(), "Cooldown prevented turning the toggle off");
            close(h, owner.getHealth(), 4, "Toggle-off action bypassed cooldown");
            blessing.setCooldown(0);
            blessing.setToggled(true);
            blessing.onToggleOff(owner);
            close(h, owner.getHealth(), 5, "Ready toggle-off action failed");
            boolean dueSeen = false;
            boolean skippedSeen = false;
            for (int i = 0; i < 10; i++) {
                var mob = f.pig();
                mob.setHealth(3);
                var tick = instance("toggle");
                tick.setToggled(true);
                boolean due = Math.floorMod(h.getLevel().getGameTime() + mob.getId(), 5) == 0;
                h.assertTrue(tick.getBlessing().onTick(tick, mob) == due, "Tick ignored configured interval/entity staggering");
                close(h, mob.getHealth(), due ? 4 : 3, "Tick applied outside its interval");
                if (due) {
                    dueSeen = true;
                    tick.setCooldown(1);
                    h.assertFalse(tick.getBlessing().onTick(tick, mob), "Due tick bypassed cooldown");
                } else skippedSeen = true;
            }
            h.assertTrue(dueSeen && skippedSeen, "Tick fixture did not exercise both interval branches");
            h.succeed();
        }
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", batch = SUITE)
    public static void nestedDamageCannotReenterAnotherContentBlessing(GameTestHelper h) {
        try (Fixture f = new Fixture(h)) {
            var owner = f.pig();
            owner.setHealth(3);
            var heal = instance("heal");
            var damage = instance("damage");
            var target = f.probe();
            boolean[] nestedSuccess = {true};
            target.duringHurt = () -> nestedSuccess[0] = heal.getBlessing().onPressed(heal, owner);
            h.assertTrue(damage.getBlessing().onDamageEntity(damage, owner, hit(owner, target, 1)), "Outer damage did not execute");
            h.assertTrue(target.hits == 1 && !nestedSuccess[0], "Damage callback reentered a content blessing");
            close(h, owner.getHealth(), 3, "Rejected nested heal still mutated owner");
            close(h, target.getHealth(), target.getMaxHealth() - 3, "Outer damage was lost while rejecting reentry");
            h.assertTrue(heal.getBlessing().onPressed(heal, owner), "Reentry guard leaked beyond the outer callback");
            close(h, owner.getHealth(), 5, "Recovered heal had wrong result");
            h.succeed();
        }
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", batch = SUITE)
    public static void failingActionStillClearsThreadLocalReentryGuard(GameTestHelper h) {
        try (Fixture f = new Fixture(h)) {
            var owner = f.pig();
            owner.setHealth(3);
            var damage = instance("damage");
            var target = f.probe();
            target.duringHurt = () -> { throw new ProbeFailure(); };
            h.assertTrue(rejects(ProbeFailure.class, () -> damage.getBlessing().onDamageEntity(damage, owner, hit(owner, target, 1))),
                    "Fixture never reached the real damage action");
            var heal = instance("heal");
            h.assertTrue(heal.getBlessing().onPressed(heal, owner), "Exceptional action poisoned later content execution");
            close(h, owner.getHealth(), 5, "Post-exception heal failed");
            h.assertTrue(damage.getCooldown() == 0, "Failed direct action invented a cooldown");
            h.succeed();
        }
    }

    private static JsonObject blessingSpec(String mode, String hooks) {
        return json("{\"mode\":\"" + mode + "\",\"cooldown_seconds\":2,\"tick_interval\":5,\"hooks\":" + hooks + "}");
    }

    private static JsonObject godSpec() {
        var god = json("{\"domains\":[\"content_test\"],\"blessings\":[],\"offerings\":{\"minecraft:apple\":1}}");
        BLESSINGS.forEach(name -> god.getAsJsonArray("blessings").add(id(name).toString()));
        return god;
    }

    private static JsonObject trialSpec() {
        return json("""
                {"god":"divinity_content:fixture_god","reward_favor":1,"phases":[
                {"name":"First watch","objective":"Remain here","duration_ticks":20,"goal":"survive"},
                {"name":"Second watch","objective":"Remain longer","duration_ticks":40,"goal":"survive"}]}
                """);
    }

    private static JsonObject json(String source) { return JsonParser.parseString(source).getAsJsonObject(); }
    private static ResourceLocation id(String path) { return new ResourceLocation(SUITE, path); }
    private static BaseGod god() { return Objects.requireNonNull(GodsRegistry.GODS_REGISTRY.get().getValue(GOD), "Content fixtures were not registered"); }
    private static BlessingsInstance instance(String name) {
        return Objects.requireNonNull(BlessingsRegistry.BLESSINGS_REGISTRY.get().getValue(id(name)), "Missing fixture blessing " + name).createDefaultInstance();
    }

    private static BlessingsInstance equip(ServerPlayer owner, String name) {
        var bound = god().createGodDefaultInstance();
        bound.setFavor(250);
        PlayerGodsCapability.load(owner).updateGodInstance(bound, false);
        var blessing = instance(name);
        blessing.setBoundGod(bound);
        DivinityEngineHelper.setBlessingSlot(owner, 0, blessing);
        return blessing;
    }

    private static LivingHurtEvent hit(Pig owner, Pig target, float amount) {
        return new LivingHurtEvent(target, owner.damageSources().mobAttack(owner), amount);
    }

    private static boolean rejects(Class<? extends RuntimeException> type, Runnable action) {
        try { action.run(); return false; }
        catch (RuntimeException error) {
            if (type.isInstance(error)) return true;
            throw error;
        }
    }

    private static void invalid(GameTestHelper h, Runnable action, String label) {
        h.assertTrue(rejects(IllegalArgumentException.class, action), label + " was accepted");
    }

    private static void close(GameTestHelper h, double actual, double expected, String message) {
        h.assertTrue(Math.abs(actual - expected) < .00001, message + ": " + actual + " != " + expected);
    }

    private static final class Fixture implements AutoCloseable {
        private final GameTestHelper helper;
        private final Vec3 origin;
        private final List<Entity> entities = new ArrayList<>();

        Fixture(GameTestHelper helper) {
            this.helper = helper;
            origin = Vec3.atCenterOf(helper.absolutePos(new BlockPos(4, 30, 4)));
        }

        Pig pig() { return inert(new Pig(EntityType.PIG, helper.getLevel())); }
        HurtProbePig probe() { return inert(new HurtProbePig(helper.getLevel())); }

        private <T extends Mob> T inert(T entity) {
            entities.add(entity);
            entity.setPos(origin);
            entity.setNoAi(true);
            entity.setNoGravity(true);
            return entity;
        }

        Zombie zombie(Vec3 pos) {
            var entity = inert(new Zombie(EntityType.ZOMBIE, helper.getLevel()));
            entity.setPos(pos);
            helper.assertTrue(helper.getLevel().addFreshEntity(entity), "Could not insert hostile fixture");
            return entity;
        }

        TestPlayer player() {
            var player = new TestPlayer(helper.getLevel());
            entities.add(player);
            player.setPos(origin);
            player.setNoGravity(true);
            return player;
        }

        @Override public void close() { entities.forEach(Entity::discard); }
    }

    private static final class HurtProbePig extends Pig {
        private Runnable duringHurt = () -> {};
        private int hits;

        HurtProbePig(ServerLevel level) { super(EntityType.PIG, level); }

        @Override public boolean hurt(DamageSource source, float amount) {
            hits++;
            duringHurt.run();
            return super.hurt(source, amount);
        }
    }

    private static final class ProbeFailure extends RuntimeException {}

    private static final class TestPlayer extends ServerPlayer {
        TestPlayer(ServerLevel level) {
            super(level.getServer(), level, new GameProfile(UUID.randomUUID(), "ContentTest"));
            connection = new ServerGamePacketListenerImpl(server, new Connection(PacketFlow.SERVERBOUND), this) {
                @Override public void send(Packet<?> packet) {}
            };
        }
    }
}
