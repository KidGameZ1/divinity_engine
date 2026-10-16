package net.nightshade.divinity_engine.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestRegistry;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.divinity.gods.BaseGod;
import net.nightshade.divinity_engine.network.cap.player.gods.PlayerGodsCapability;
import net.nightshade.divinity_engine.network.events.divinity.blessing.BlessingsEvents;
import net.nightshade.divinity_engine.registry.divinity.blessing.BlessingsRegistry;
import net.nightshade.divinity_engine.registry.divinity.gods.GodsRegistry;
import net.nightshade.divinity_engine.util.DivinityEngineHelper;
import net.nightshade.divinity_engine.util.divinity.blessings.RewindHistory;
import net.nightshade.divinity_engine.util.divinity.blessings.WarlordConfusion;

import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = "divinity_engine", bus = Mod.EventBusSubscriber.Bus.MOD)
@PrefixGameTestTemplate(false)
public final class BlessingRepairGameTests {
    private static final String SUITE = "divinity_blessing_repairs";

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        if (!SUITE.equals(System.getProperty("forge.enabledGameTestNamespaces", ""))) return;
        for (var method : BlessingRepairGameTests.class.getDeclaredMethods())
            if (method.isAnnotationPresent(GameTest.class)) GameTestRegistry.register(method, Set.of("divinity_engine"));
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", timeoutTicks = 100)
    public static void echoRewindsZeroCoordinateAndSendsPosition(GameTestHelper h) {
        var p = player(h);
        var b = equip(p, GodsRegistry.KAIROTH.get(), BlessingsRegistry.ECHO_STEP.get());
        // Zero is a valid coordinate, not an absent checkpoint sentinel.
        Vec3 start = new Vec3(0, 250, h.absolutePos(BlockPos.ZERO).getZ() + 2.5);
        h.getLevel().getChunk(BlockPos.containing(start));
        h.getLevel().getChunk(BlockPos.containing(start.add(-1, 0, 0)));
        p.setPos(start);
        b.onTick(p);
        history(h, p, b, start.add(2, 0, 0), 60);
        h.runAtTickTime(61, () -> {
            try {
                b.onPressed(p);
                h.assertTrue(p.position().distanceToSqr(start) < .001, "Echo Step did not rewind three seconds");
                h.assertTrue(p.positionPackets > 0, "Rewind was not sent to the client");
                h.assertTrue(b.getCooldown() == 600, "Echo Step did not start its normal cooldown");
                b.onRelease(p, 1);
                h.assertTrue(p.positionPackets == 1, "Key release teleported again");
                h.succeed();
            } finally { p.discard(); }
        });
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", timeoutTicks = 100)
    public static void echoRejectsObstructionsAndStaleHistory(GameTestHelper h) {
        var p = player(h);
        var b = equip(p, GodsRegistry.KAIROTH.get(), BlessingsRegistry.ECHO_STEP.get());
        Vec3 start = p.position();
        b.onTick(p);
        history(h, p, b, start.add(2, 0, 0), 60);
        h.runAtTickTime(61, () -> {
            try {
                var obstruction = BlockPos.containing(start);
                h.getLevel().setBlockAndUpdate(obstruction, Blocks.STONE.defaultBlockState());
                h.assertFalse(b.getBlessing().onPressed(b, p), "Rewind entered a solid block");
                h.assertTrue(b.getCooldown() == 0 && p.positionPackets == 0, "Failed rewind consumed a cooldown");
                h.getLevel().setBlockAndUpdate(obstruction, Blocks.AIR.defaultBlockState());
                RewindHistory.logout(new PlayerEvent.PlayerLoggedOutEvent(p));
                h.assertFalse(b.getBlessing().onPressed(b, p), "Logout retained an old checkpoint");
                h.succeed();
            } finally { p.discard(); }
        });
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", timeoutTicks = 100)
    public static void arcaneRecallUsesDamageHookAndTwentySecondCooldown(GameTestHelper h) {
        var p = player(h);
        var b = equip(p, GodsRegistry.ELARIS.get(), BlessingsRegistry.ARCANE_RECALL.get());
        h.assertTrue(b.getBlessing().isPassive() && !b.getBlessing().isActive(), "Recall is not registered as automatic");
        Vec3 start = p.position();
        b.onTick(p);
        history(h, p, b, start.add(2, 0, 0), 40);
        h.runAtTickTime(41, () -> {
            try {
                var canceled = new LivingDamageEvent(p, p.damageSources().generic(), 2);
                canceled.setCanceled(true);
                b.onTakenDamage(canceled);
                h.assertTrue(p.positionPackets == 0, "Canceled damage caused a rewind");
                BlessingsEvents.onBlessingTakenDamage(new LivingDamageEvent(p, p.damageSources().generic(), 2));
                h.assertTrue(p.position().distanceToSqr(start) < .001 && p.positionPackets == 1, "Damage did not trigger recall");
                h.assertTrue(b.getCooldown() == 400, "Recall did not start a twenty-second cooldown");
                BlessingsEvents.onBlessingTakenDamage(new LivingDamageEvent(p, p.damageSources().generic(), 2));
                h.assertTrue(p.positionPackets == 1, "Recall ignored cooldown");
                h.succeed();
            } finally { p.discard(); }
        });
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", timeoutTicks = 100)
    public static void snowveilCloaksWithoutSneakingAndBreaksOnDamage(GameTestHelper h) {
        var p = player(h);
        try {
            var b = equip(p, GodsRegistry.CRYONIS.get(), BlessingsRegistry.SNOWVEIL.get());
            h.getLevel().setBlockAndUpdate(p.blockPosition().below(), Blocks.SNOW_BLOCK.defaultBlockState());
            b.onTick(p);
            h.assertTrue(p.hasEffect(MobEffects.INVISIBILITY), "Snowveil did not apply durable invisibility");
            b.onTakenDamage(new LivingDamageEvent(p, p.damageSources().generic(), 2));
            h.assertFalse(p.hasEffect(MobEffects.INVISIBILITY), "Taking damage did not break Snowveil");
            b.onTick(p);
            h.assertFalse(p.hasEffect(MobEffects.INVISIBILITY), "Snowveil immediately recloaked after combat");
            p.tickCount += 40;
            b.onTick(p);
            h.assertTrue(p.hasEffect(MobEffects.INVISIBILITY), "Snowveil never recovered after combat");
            DivinityEngineHelper.setBlessingSlot(p, 0, null);
            for (int i = 0; i < 7; i++) p.tickOnlyEffects();
            h.assertFalse(p.hasEffect(MobEffects.INVISIBILITY), "Unequipping left permanent invisibility");
            h.succeed();
        } finally { p.discard(); }
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", timeoutTicks = 100)
    public static void snowveilPreservesOtherInvisibility(GameTestHelper h) {
        var p = player(h);
        try {
            var b = equip(p, GodsRegistry.CRYONIS.get(), BlessingsRegistry.SNOWVEIL.get());
            h.getLevel().setBlockAndUpdate(p.blockPosition().below(), Blocks.SNOW_BLOCK.defaultBlockState());
            b.onTick(p);
            p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 600, 0));
            b.onTakenDamage(new LivingDamageEvent(p, p.damageSources().generic(), 2));
            h.assertTrue(p.getEffect(MobEffects.INVISIBILITY) != null
                    && p.getEffect(MobEffects.INVISIBILITY).getDuration() == 600, "Snowveil removed a potion's invisibility");
            h.succeed();
        } finally { p.discard(); }
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", timeoutTicks = 100)
    public static void warlordsWillHandlesOneEnemyAndExpires(GameTestHelper h) {
        var p = player(h);
        var b = equip(p, GodsRegistry.DRAVAK.get(), BlessingsRegistry.WARLORDS_WILL.get());
        Zombie mob = zombie(h, p.position().add(2, 0, 0));
        h.assertTrue(b.getBlessing().onPressed(b, p), "Single-target cast failed");
        h.assertTrue(mob.hasEffect(MobEffects.BLINDNESS), "Single enemy was not blinded");
        mob.setTarget(p);
        h.assertTrue(mob.getTarget() == null, "AI immediately canceled fear");
        // Shorten the test's control window, retaining the production restoration path.
        WarlordConfusion.apply(mob, null, 4);
        h.runAtTickTime(6, () -> {
            try {
                mob.setTarget(p);
                h.assertTrue(mob.getTarget() == p, "Fear never expired");
                h.succeed();
            } finally { mob.discard(); p.discard(); }
        });
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", timeoutTicks = 100)
    public static void warlordsWillRedirectsEnemiesWithoutFreezingThem(GameTestHelper h) {
        var p = player(h);
        Zombie first = zombie(h, p.position().add(2, 0, 0));
        Zombie second = zombie(h, p.position().add(4, 0, 0));
        try {
            var b = equip(p, GodsRegistry.DRAVAK.get(), BlessingsRegistry.WARLORDS_WILL.get());
            b.onPressed(p);
            h.assertTrue(first.getTarget() == second && second.getTarget() == first, "Confusion did not target other enemies");
            first.setTarget(p);
            h.assertTrue(first.getTarget() == second, "Normal AI replaced the confused target");
            h.assertFalse(first.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "Enemies cannot move to attack each other");
            h.succeed();
        } finally { first.discard(); second.discard(); p.discard(); }
    }

    private static void history(GameTestHelper h, TestPlayer p, BlessingsInstance b, Vec3 destination, int ticks) {
        for (int tick = 2; tick <= ticks; tick += 2)
            h.runAtTickTime(tick, () -> { p.setPos(destination); b.onTick(p); });
    }

    private static BlessingsInstance equip(TestPlayer p, BaseGod god, Blessings blessing) {
        var bound = god.createGodDefaultInstance();
        bound.setFavor(250);
        PlayerGodsCapability.load(p).updateGodInstance(bound, false);
        BlessingsInstance instance = blessing.createDefaultInstance();
        instance.setBoundGod(bound);
        DivinityEngineHelper.setBlessingSlot(p, 0, instance);
        return instance;
    }

    private static TestPlayer player(GameTestHelper h) {
        var p = new TestPlayer(h.getLevel());
        var pos = h.absolutePos(new BlockPos(3, 12, 3));
        p.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
        return p;
    }

    private static Zombie zombie(GameTestHelper h, Vec3 pos) {
        var mob = new Zombie(EntityType.ZOMBIE, h.getLevel());
        mob.setPos(pos); mob.setNoAi(true);
        h.getLevel().addFreshEntity(mob);
        return mob;
    }

    private static final class TestPlayer extends ServerPlayer {
        int positionPackets;
        TestPlayer(ServerLevel level) {
            super(level.getServer(), level, new GameProfile(UUID.randomUUID(), "BlessingRepair"));
            connection = new ServerGamePacketListenerImpl(server, new Connection(PacketFlow.SERVERBOUND), this) {
                @Override public void send(Packet<?> packet) {
                    if (packet instanceof ClientboundPlayerPositionPacket) positionPackets++;
                }
            };
        }
        void tickOnlyEffects() { super.tickEffects(); }
    }
}
