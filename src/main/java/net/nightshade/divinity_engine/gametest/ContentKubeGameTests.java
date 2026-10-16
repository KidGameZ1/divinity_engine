package net.nightshade.divinity_engine.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;
import net.nightshade.divinity_engine.block.StatueBlock;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.divinity.gods.BaseGod;
import net.nightshade.divinity_engine.divinity.gods.BaseGodInstance;
import net.nightshade.divinity_engine.registry.divinity.blessing.BlessingsRegistry;
import net.nightshade.divinity_engine.registry.divinity.gods.GodsRegistry;
import net.nightshade.divinity_engine.registry.divinity.trial.GodlyTrialsRegistry;
import net.nightshade.divinity_engine.util.content.ContentBlessing;
import net.nightshade.divinity_engine.util.content.ContentGod;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/** Requires only the isolated examples/kubejs-tests fixture scripts; Java registers no fallback content. */
@Mod.EventBusSubscriber(modid = "divinity_engine", bus = Mod.EventBusSubscriber.Bus.MOD)
@PrefixGameTestTemplate(false)
public final class ContentKubeGameTests {
    private static final String SUITE = "divinity_content_kubejs";
    private static final ResourceLocation GOD = id("script_god");
    private static final ResourceLocation TRIAL = id("script_trial");
    private static final List<String> BLESSINGS = List.of("json_script", "pure_script", "isolation", "unhandled");

    private ContentKubeGameTests() {}

    @SubscribeEvent
    @SuppressWarnings("deprecation")
    public static void register(RegisterGameTestsEvent event) {
        if (Arrays.stream(System.getProperty("forge.enabledGameTestNamespaces", "").split(","))
                .map(String::trim).noneMatch(SUITE::equals)) return;
        for (var method : ContentKubeGameTests.class.getDeclaredMethods()) {
            if (method.isAnnotationPresent(GameTest.class)) GameTestRegistry.register(method, Set.of("divinity_engine"));
        }
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", batch = SUITE)
    public static void startupScriptsRegisterGodStatueBlessingsAndTrial(GameTestHelper h) {
        BaseGod god = god(h);
        h.assertTrue(god instanceof ContentGod && god.getDomains().equals(Set.of("KubeJS fixture")), "Script god definition was not loaded");
        var bound = god.createGodDefaultInstance();
        h.assertTrue(god.getBlessings().size() == BLESSINGS.size(), "Script god lost its blessing list");
        for (String name : BLESSINGS) {
            var blessing = BlessingsRegistry.BLESSINGS_REGISTRY.get().getValue(id(name));
            h.assertTrue(blessing instanceof ContentBlessing, "Missing script-only blessing: " + name);
            var instance = bound.getBlessing(blessing);
            h.assertTrue(instance != null && instance.getBoundGod() == god, "Script blessing is not bound to its god: " + name);
        }
        ResourceLocation statueId = id("script_god_statue");
        var block = ForgeRegistries.BLOCKS.getValue(statueId);
        var type = ForgeRegistries.BLOCK_ENTITY_TYPES.getValue(statueId);
        h.assertTrue(block instanceof StatueBlock && ((StatueBlock) block).getGod() == god, "Startup script did not generate the correct statue");
        h.assertTrue(god.getStatueBlock() == block && god.getStatueBlockEntity() == type && type != null,
                "Script god lost automatic statue handles");
        h.assertTrue(ForgeRegistries.ITEMS.getValue(statueId) == block.asItem(), "Script statue item is missing");
        var state = block.defaultBlockState();
        var entity = ((StatueBlock) block).newBlockEntity(BlockPos.ZERO, state);
        h.assertTrue(entity != null && entity.getType() == type, "Script statue factory returned the wrong type");
        var saved = entity.saveWithFullMetadata();
        h.assertTrue(statueId.toString().equals(saved.getString("id")), "Script statue metadata lost its namespaced ID");
        var restored = BlockEntity.loadStatic(BlockPos.ZERO, state, saved);
        h.assertTrue(restored != null && restored.getType() == type, "Script statue failed registered NBT reconstruction");
        var trial = GodlyTrialsRegistry.get(TRIAL);
        h.assertTrue(trial != null && trial.getGod() == god && GodlyTrialsRegistry.get(god) == trial
                && TRIAL.equals(GodlyTrialsRegistry.getId(god)), "Script-only trial did not bind to the script god");
        h.succeed();
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", batch = SUITE)
    public static void eachPressedHookRunsJsonAndScriptExactlyOnce(GameTestHelper h) {
        try (Fixture f = new Fixture(h)) {
            var blessing = f.blessing("json_script");
            f.owner.setHealth(2);
            h.assertTrue(press(blessing, f.owner), "JSON plus script press failed");
            health(h, f.owner, 5, "Expected JSON heal two followed by script heal one");
            var tag = blessing.getOrCreateTag();
            h.assertTrue(tag.getInt("json_calls") == 1 && tag.getBoolean("json_success_before_script"),
                    "Script did not run exactly once after successful JSON actions");
            h.assertTrue(tag.getBoolean("success_after_script") && tag.getBoolean("pressed_nullables")
                    && tag.getString("seen_hook").equals("pressed") && tag.getString("seen_id").equals(id("json_script").toString()),
                    "Script received wrong context or could not mark success");
            h.assertTrue(tag.getInt("pure_calls") == 0 && tag.getInt("isolation_calls") == 0, "A foreign-ID listener received the press");

            // These are raw blessing hooks on pigs; the ordinary caller owns starting the cooldown.
            blessing.startCooldown(f.owner);
            h.assertTrue(blessing.getCooldown() == 40 && !press(blessing, f.owner), "Cooldown failed to gate the hook");
            health(h, f.owner, 5, "Cooldown still ran JSON or script healing");
            h.assertTrue(tag.getInt("json_calls") == 1, "Cooldown still invoked the script");
            blessing.setCooldown(0);
            h.assertTrue(press(blessing, f.owner), "Second ready press failed");
            health(h, f.owner, 8, "Second press did not execute one JSON/script pair");
            h.assertTrue(tag.getInt("json_calls") == 2, "Callbacks accumulated or were lost between presses");
            h.succeed();
        }
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", batch = SUITE)
    public static void wrongIdListenersCannotObserveAnotherBlessing(GameTestHelper h) {
        try (Fixture f = new Fixture(h)) {
            f.owner.setHealth(3);
            var unhandled = f.blessing("unhandled");
            h.assertFalse(press(unhandled, f.owner), "Unsubscribed empty blessing acquired another ID's success");
            health(h, f.owner, 3, "Unsubscribed ID received another listener's healing");
            h.assertTrue(unhandled.getTag() == null || unhandled.getTag().isEmpty(), "Foreign callback wrote to an unsubscribed instance");

            var isolated = f.blessing("isolation");
            h.assertTrue(press(isolated, f.owner), "Matching isolated listener did not run");
            health(h, f.owner, 4, "Isolation listener did not apply exactly its own heal");
            var tag = isolated.getOrCreateTag();
            h.assertTrue(tag.getInt("isolation_calls") == 1 && tag.getInt("json_calls") == 0 && tag.getInt("pure_calls") == 0,
                    "ID-filtered callback crossed into another blessing's instance");
            h.succeed();
        }
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", batch = SUITE)
    public static void pureScriptEmptyHooksCanSucceedWithoutInventingNoopSuccess(GameTestHelper h) {
        try (Fixture f = new Fixture(h)) {
            var blessing = f.blessing("pure_script");
            f.owner.setHealth(3);
            h.assertTrue(press(blessing, f.owner), "Empty JSON hooks prevented pure-script success");
            health(h, f.owner, 5, "Pure-script healing did not execute");
            var tag = blessing.getOrCreateTag();
            h.assertTrue(tag.getInt("pure_calls") == 1 && !tag.getBoolean("pure_success_before_script")
                    && tag.getBoolean("pure_success_after_script"), "Script did not exclusively determine success");
            h.assertTrue(tag.getInt("json_calls") == 0 && tag.getInt("isolation_calls") == 0, "Pure-script press reached foreign callbacks");
            f.owner.setHealth(f.owner.getMaxHealth());
            h.assertFalse(press(blessing, f.owner), "Full-health callback without markSuccessful invented success");
            h.assertTrue(tag.getInt("pure_calls") == 2 && !tag.getBoolean("pure_success_before_script")
                    && !tag.getBoolean("pure_success_after_script"), "Success leaked between independent script contexts");
            health(h, f.owner, f.owner.getMaxHealth(), "No-op script changed full health");
            h.succeed();
        }
    }

    private static ResourceLocation id(String path) { return new ResourceLocation(SUITE, path); }

    private static BaseGod god(GameTestHelper h) {
        BaseGod god = GodsRegistry.GODS_REGISTRY.get().getValue(GOD);
        h.assertTrue(god != null, "Missing startup fixtures: install examples/kubejs-tests/kubejs in the isolated KubeJS-enabled test directory");
        return god;
    }

    private static boolean press(BlessingsInstance instance, Pig owner) {
        return ((ContentBlessing) instance.getBlessing()).onPressed(instance, owner);
    }

    private static void health(GameTestHelper h, Pig owner, float expected, String message) {
        h.assertTrue(Math.abs(owner.getHealth() - expected) < .00001, message + ": " + owner.getHealth() + " != " + expected);
    }

    private static final class Fixture implements AutoCloseable {
        private final GameTestHelper helper;
        private final BaseGodInstance bound;
        private final Pig owner;

        Fixture(GameTestHelper helper) {
            this.helper = helper;
            bound = god(helper).createGodDefaultInstance();
            bound.setFavor(250);
            owner = new Pig(EntityType.PIG, helper.getLevel());
            owner.setPos(Vec3.atCenterOf(helper.absolutePos(new BlockPos(4, 30, 4))));
            owner.setNoAi(true);
            owner.setNoGravity(true);
        }

        BlessingsInstance blessing(String name) {
            var value = BlessingsRegistry.BLESSINGS_REGISTRY.get().getValue(id(name));
            helper.assertTrue(value instanceof ContentBlessing, "Missing script-only blessing " + name);
            var instance = bound.getBlessing(value);
            helper.assertTrue(instance != null && instance.getBoundGodInstance() == bound, "Script instance lost its bound god");
            return instance;
        }

        @Override public void close() { owner.discard(); }
    }
}
