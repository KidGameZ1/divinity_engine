package net.nightshade.divinity_engine.gametest;

import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.nightshade.divinity_engine.common.inventory.PocketInventory;
import net.nightshade.divinity_engine.network.cap.player.gods.MainPlayerCapability;
import net.nightshade.divinity_engine.network.cap.player.gods.PlayerGodsCapability;
import net.nightshade.divinity_engine.registry.dimensions.DimensionRegistry;
import net.nightshade.divinity_engine.registry.divinity.blessing.BlessingsRegistry;
import net.nightshade.divinity_engine.registry.divinity.gods.GodsRegistry;
import net.nightshade.divinity_engine.util.divinity.blessings.aethon.PocketDomainData;
import net.nightshade.divinity_engine.util.divinity.blessings.aethon.PocketDomainEvents;
import net.nightshade.divinity_engine.util.divinity.blessings.aethon.PocketDomainManager;
import net.nightshade.divinity_engine.world.inventory.PocketDimensionInvMenu;
import net.nightshade.divinity_engine.world.inventory.PocketDomainMenu;

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

import static net.nightshade.divinity_engine.util.divinity.blessings.aethon.PocketDomainData.*;

/** Opt-in isolated domain checks; no fixtures run in ordinary player worlds. */
@Mod.EventBusSubscriber(modid = "divinity_engine", bus = Mod.EventBusSubscriber.Bus.MOD)
@PrefixGameTestTemplate(false)
public final class PocketDomainGameTests {
    private static final String SUITE = "divinity_pocket_domain";
    @SubscribeEvent public static void register(RegisterGameTestsEvent event) {
        if (Arrays.stream(System.getProperty("forge.enabledGameTestNamespaces", "").split(",")).noneMatch(SUITE::equals)) return;
        for (var method : PocketDomainGameTests.class.getDeclaredMethods())
            if (method.isAnnotationPresent(GameTest.class)) GameTestRegistry.register(method, Set.of("divinity_engine"));
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", batch = SUITE)
    public static void legacyMigrationIsOnceOnlyAndKeepsInventory(GameTestHelper h) {
        var data = new PocketDomainData();
        UUID owner = UUID.randomUUID();
        var domain = data.getOrCreate(owner);
        var inventory = new PocketInventory();
        var sword = new ItemStack(Items.DIAMOND_SWORD); sword.setDamageValue(42);
        inventory.setItem(26, sword);
        CompoundTag old = new CompoundTag();
        old.putInt("pocket_x", -126); old.putInt("pocket_y", 80); old.putInt("pocket_z", 279);
        old.put("inventory", inventory.saveToTag());
        old.putInt("return_x", 20); old.putInt("return_y", 65); old.putInt("return_z", -40);
        old.putString("return_dimension", "minecraft:overworld");
        CompoundTag unchanged = old.copy();
        h.assertTrue(data.importLegacy(domain, old), "Legacy data not imported");
        h.assertTrue(old.equals(unchanged), "Migration modified the source blessing");
        h.assertTrue(domain.rooms.get(0).legacy && domain.rooms.get(0).origin.equals(new BlockPos(-126, 80, 279)), "Legacy coordinates changed");
        domain.inventory.removeItem(26, 1);
        h.assertFalse(data.importLegacy(domain, old), "Repeated migration duplicated items");
        var reloaded = PocketDomainData.load(data.save(new CompoundTag())).find(owner);
        h.assertTrue(reloaded.inventory.isEmpty() && reloaded.imported && reloaded.rooms.size() == 1, "Reload lost migration state");
        h.assertTrue(reloaded.returnPoint.position().equals(new Vec3(20.5, 65, -39.5)), "Return position changed");
        inventory.loadFromTag(new CompoundTag());
        h.assertTrue(inventory.isEmpty() && inventory.getContainerSize() == 27, "Empty inventory load retained ghost stacks");
        h.succeed();
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", batch = SUITE)
    public static void slotsAndRulesPersistIndependently(GameTestHelper h) {
        var data = new PocketDomainData(); var a = data.getOrCreate(UUID.randomUUID()); var b = data.getOrCreate(UUID.randomUUID());
        Room first = data.planRoom(a, Style.GROVE), second = data.planRoom(b, Style.WORKSHOP);
        h.assertTrue(first.origin.distSqr(second.origin) >= 128 * 128, "Owners share a site");
        h.assertTrue(data.planRoom(a, Style.SANCTUM) == null, "Duplicate concurrent construction allowed");
        first.stage = 1; first.cursor = 1234; first.setRule(Rule.RESIDENT_REVIVAL, false);
        var reload = PocketDomainData.load(data.save(new CompoundTag()));
        Room saved = reload.find(a.owner).rooms.get(0);
        h.assertTrue(saved.cursor == 1234 && saved.stage == 1 && !saved.allows(Rule.RESIDENT_REVIVAL), "Construction/rules lost on restart");
        h.assertTrue(reload.find(b.owner).rooms.get(0).allows(Rule.RESIDENT_REVIVAL), "Rules leaked to another owner");
        h.succeed();
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", batch = SUITE, timeoutTicks = 1200)
    public static void constructionNeverOverwritesOccupiedSites(GameTestHelper h) {
        var data = PocketDomainData.get(h.getLevel().getServer()); var domain = data.getOrCreate(UUID.randomUUID());
        ServerLevel pocket = pocket(h);
        Room occupied = data.planRoom(domain, Style.SANCTUM);
        pocket.setBlockAndUpdate(occupied.origin, Blocks.DIAMOND_BLOCK.defaultBlockState());
        PocketDomainManager.buildTick(h.getLevel().getServer());
        h.assertTrue(pocket.getBlockState(occupied.origin).is(Blocks.DIAMOND_BLOCK), "Existing block overwritten");
        Room[] next = new Room[1];
        h.startSequence().thenWaitUntil(() -> h.assertTrue(domain.rooms.isEmpty(), "Occupied site check pending"))
                .thenExecute(() -> {
                    next[0] = data.planRoom(domain, Style.GROVE);
                    h.assertFalse(next[0].origin.equals(occupied.origin), "Occupied slot reused");
                    PocketDomainManager.buildTick(h.getLevel().getServer());
                    h.assertFalse(next[0].ready(), "Whole construction ran synchronously");
                }).thenWaitUntil(() -> h.assertTrue(next[0].ready(), "Queued room did not finish"))
                .thenExecute(() -> {
                    h.assertTrue(pocket.getBlockState(next[0].origin.offset(16, 1, 15)).is(Blocks.MOSS_BLOCK), "Grove floor missing");
                    h.assertTrue(pocket.getBlockState(occupied.origin).is(Blocks.DIAMOND_BLOCK), "Old site changed later");
                }).thenSucceed();
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", batch = SUITE)
    public static void menuRejectsForeignOwnersAndStaleActions(GameTestHelper h) {
        TestPlayer owner = new TestPlayer(pocket(h)), stranger = new TestPlayer(pocket(h));
        Room room = readyRoom(owner);
        owner.setPos(room.arrival()); stranger.setPos(room.arrival());
        var domain = PocketDomainManager.domain(owner);
        var menu = new PocketDomainMenu(7, owner.getInventory(), PocketDomainManager.snapshot(domain));
        owner.containerMenu = menu;
        h.assertFalse(menu.clickMenuButton(stranger, 40), "Foreign player changed a rule");
        h.assertTrue(menu.clickMenuButton(owner, 40), "Owner could not disable revival");
        h.assertFalse(room.allows(Rule.RESIDENT_REVIVAL), "Rule not changed");
        domain.nextAction = 0; menu.clickMenuButton(owner, 40);
        h.assertFalse(room.allows(Rule.RESIDENT_REVIVAL), "Duplicate request toggled instead of setting");
        h.assertFalse(PocketDomainEvents.mayBuild(stranger, room.origin.offset(10, 3, 10)), "Foreign building allowed");
        h.assertTrue(PocketDomainEvents.mayBuild(owner, room.origin.offset(10, 3, 10)), "Owner cannot furnish interior");
        domain.nextAction = 0;
        domain.rooms.set(0, new Room(room.origin.offset(128, 0, 0), Style.ARENA, false));
        h.assertFalse(menu.clickMenuButton(owner, 41), "Stale room identity accepted");
        for (int action = 0; action < 120; action++) {
            var buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                new ServerboundContainerButtonClickPacket(7, action).write(buffer);
                h.assertTrue(new ServerboundContainerButtonClickPacket(buffer).getButtonId() == action, "Action truncated on wire: " + action);
            } finally { buffer.release(); }
        }
        owner.discard(); stranger.discard(); h.succeed();
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", batch = SUITE)
    public static void storageAndEscapeSurviveUnequipping(GameTestHelper h) {
        TestPlayer player = new TestPlayer(pocket(h)); Room room = readyRoom(player); player.setPos(room.arrival());
        var domain = PocketDomainManager.domain(player);
        domain.inventory.setItem(0, new ItemStack(Items.DIAMOND, 7));
        var menu = new PocketDimensionInvMenu(8, player.getInventory(), null);
        h.assertTrue(menu.slots.size() == 63 && menu.getSlot(0).getItem().getCount() == 7, "Legacy inventory geometry/content changed");
        player.variables.setBlessingSlot1(null);
        h.assertTrue(menu.stillValid(player), "Storage locked when blessing lost inside own room");
        menu.getSlot(0).remove(2);
        h.assertTrue(domain.inventory.getItem(0).getCount() == 5, "Inventory uses a detached copy");
        BlockPos back = h.absolutePos(new BlockPos(14, 1, 15));
        h.getLevel().setBlockAndUpdate(back.below(), Blocks.STONE.defaultBlockState());
        domain.returnPoint = new ReturnPoint(h.getLevel().dimension().location().toString(), Vec3.atBottomCenterOf(back), 0, 0);
        h.assertTrue(PocketDomainManager.exit(player), "Unequipped player trapped in domain");
        h.assertTrue(player.level() == h.getLevel(), "Wrong return dimension");
        player.discard(); h.succeed();
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", batch = SUITE, timeoutTicks = 200)
    public static void residentRevivalCancelsDeathWithoutCloning(GameTestHelper h) {
        TestPlayer owner = new TestPlayer(pocket(h)); Room room = readyRoom(owner);
        var domain = PocketDomainManager.domain(owner);
        Zombie zombie = EntityType.ZOMBIE.create(pocket(h));
        zombie.setPos(room.arrival().add(3, 0, 0));
        zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        pocket(h).addFreshEntity(zombie);
        UUID id = zombie.getUUID();
        zombie.getPersistentData().putUUID(PocketDomainManager.RESIDENT_OWNER, owner.getUUID());
        domain.residents.put(id, new Resident(id, "Test resident", 0, PocketDomainManager.point(owner), 0));
        var chunk = new net.minecraft.world.level.ChunkPos(zombie.blockPosition());
        pocket(h).setChunkForced(chunk.x, chunk.z, true);
        h.startSequence().thenWaitUntil(() -> h.assertTrue(pocket(h).getEntity(id) == zombie, "Waiting for resident chunk visibility"))
                .thenExecute(() -> {
        try {
        h.assertTrue(zombie.hurt(pocket(h).damageSources().generic(), 1000), "Damage did not reach resident");
        h.assertTrue(zombie.isAlive() && zombie.getHealth() == zombie.getMaxHealth(), "Resident was not revived");
        h.assertTrue(pocket(h).getEntity(id) == zombie && domain.residents.get(id).revivals() == 1, "Revival cloned/replaced resident");
        h.assertTrue(zombie.getMainHandItem().is(Items.IRON_SWORD), "Revival lost equipment");
        h.assertTrue(pocket(h).getEntitiesOfClass(ItemEntity.class, zombie.getBoundingBox().inflate(4)).isEmpty(), "Revival produced death drops");
        var attack = new LivingAttackEvent(zombie, pocket(h).damageSources().playerAttack(owner), 1);
        PocketDomainEvents.attack(attack); h.assertTrue(attack.isCanceled(), "Peaceful room allowed combat");
        room.setRule(Rule.RESIDENT_REVIVAL, false);
        var death = new LivingDeathEvent(zombie, pocket(h).damageSources().generic());
        PocketDomainEvents.death(death); h.assertFalse(death.isCanceled(), "Disabled revival still active");
        } finally { zombie.discard(); owner.discard(); pocket(h).setChunkForced(chunk.x, chunk.z, false); }
                }).thenSucceed();
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", batch = SUITE)
    public static void lethalOwnerRecallUsesOneCooldown(GameTestHelper h) {
        TestPlayer owner = new TestPlayer(pocket(h)); Room room = readyRoom(owner); owner.setPos(room.arrival());
        var domain = PocketDomainManager.domain(owner);
        BlockPos back = h.absolutePos(new BlockPos(14, 1, 15));
        h.getLevel().setBlockAndUpdate(back.below(), Blocks.STONE.defaultBlockState());
        domain.returnPoint = new ReturnPoint(h.getLevel().dimension().location().toString(), Vec3.atBottomCenterOf(back), 0, 0);
        owner.setHealth(0);
        var death = new LivingDeathEvent(owner, owner.damageSources().generic());
        PocketDomainEvents.death(death);
        h.assertTrue(death.isCanceled() && owner.getHealth() > 0 && owner.level() == h.getLevel(), "Lethal owner recall failed");
        owner.teleportTo(pocket(h), room.arrival().x, room.arrival().y, room.arrival().z, 0, 0);
        owner.setHealth(0);
        var repeated = new LivingDeathEvent(owner, owner.damageSources().generic());
        PocketDomainEvents.death(repeated);
        h.assertFalse(repeated.isCanceled(), "Recall ignored cooldown");
        owner.discard(); h.succeed();
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", batch = SUITE, timeoutTicks = 150)
    public static void captureAndReleaseTransferTheSameCreature(GameTestHelper h) {
        TestPlayer owner = new TestPlayer(h.getLevel()); Room room = readyRoom(owner);
        var chunk = new net.minecraft.world.level.ChunkPos(BlockPos.containing(room.arrival().add(3, 0, 0)));
        pocket(h).setChunkForced(chunk.x, chunk.z, true);
        Vec3 start = Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(14, 2, 10)));
        owner.setPos(start); owner.setYRot(0); owner.setXRot(0);
        Zombie zombie = EntityType.ZOMBIE.create(h.getLevel());
        zombie.setPos(start.add(0, 0, 3)); zombie.setHealth(5);
        zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        h.getLevel().setBlockAndUpdate(zombie.blockPosition().below(), Blocks.STONE.defaultBlockState());
        h.getLevel().addFreshEntity(zombie);
        UUID id = zombie.getUUID();
        h.assertTrue(PocketDomainManager.eligible(owner), "Capture fixture is ineligible");
        h.assertTrue(PocketDomainManager.capturable(zombie), "Capture fixture creature is ineligible");
        h.assertTrue(PocketDomainManager.aimedCreature(owner) == zombie,
                "Aim failed: eye=" + owner.getEyePosition() + " look=" + owner.getViewVector(1) + " mob=" + zombie.position()
                        + " los=" + owner.hasLineOfSight(zombie));
        h.assertTrue(PocketDomainManager.capture(owner), "Valid aimed creature was not captured: " + owner.lastMessage);
        h.assertTrue(zombie.isRemoved(), "Source creature was duplicated");
        h.startSequence().thenWaitUntil(() -> h.assertTrue(pocket(h).getEntity(id) instanceof Zombie, "Waiting for captured resident chunk visibility"))
                .thenExecute(() -> {
            try {
            var captured = pocket(h).getEntity(id);
            h.assertTrue(captured instanceof Zombie && ((Zombie) captured).getMainHandItem().is(Items.IRON_SWORD),
                    "Captured identity/equipment lost: " + captured + "; records=" + PocketDomainManager.domain(owner).residents);
            h.assertTrue(PocketDomainManager.release(owner, id), "Release failed");
            var released = h.getLevel().getEntity(id);
            h.assertTrue(released instanceof Zombie && !released.getPersistentData().hasUUID(PocketDomainManager.RESIDENT_OWNER), "Released creature kept ownership or disappeared");
            h.assertTrue(PocketDomainManager.domain(owner).residents.isEmpty(), "Released resident record survived");
            released.discard();
            } finally { owner.discard(); pocket(h).setChunkForced(chunk.x, chunk.z, false); }
        }).thenSucceed();
    }

    private static ServerLevel pocket(GameTestHelper h) { return h.getLevel().getServer().getLevel(DimensionRegistry.POCKET_DIMENSION); }
    private static Room readyRoom(TestPlayer owner) {
        var data = PocketDomainData.get(owner.server); var domain = PocketDomainManager.domain(owner);
        Room room = data.planRoom(domain, Style.SANCTUM);
        room.stage = 2;
        ServerLevel level = owner.server.getLevel(DimensionRegistry.POCKET_DIMENSION);
        for (int x = 10; x <= 24; x++) for (int z = 10; z <= 20; z++)
            level.setBlock(room.origin.offset(x, 1, z), Blocks.STONE.defaultBlockState(), 2);
        return room;
    }
    private static final class TestPlayer extends FakePlayer {
        String lastMessage = "";
        final MainPlayerCapability.PlayerVariables variables = new MainPlayerCapability.PlayerVariables();
        private final LazyOptional<MainPlayerCapability.PlayerVariables> cap = LazyOptional.of(() -> variables);
        TestPlayer(ServerLevel level) {
            super(level, new GameProfile(UUID.randomUUID(), "domain-test"));
            var god = GodsRegistry.AETHON.get().createGodDefaultInstance(); god.setFavor(250);
            PlayerGodsCapability.load(this).updateGodInstance(god, false);
            var blessing = BlessingsRegistry.POCKET_DIMENSION.get().createDefaultInstance(); blessing.setBoundGod(god);
            variables.setBlessingSlot1(blessing);
        }
        @Override public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction side) {
            if (capability == MainPlayerCapability.PLAYER_VARIABLES_CAPABILITY && cap != null) return cap.cast();
            return super.getCapability(capability, side);
        }
        @Override public void teleportTo(ServerLevel level, double x, double y, double z, float yaw, float pitch) {
            setServerLevel(level); setPos(x, y, z); setYRot(yaw); setXRot(pitch);
        }
        @Override public void displayClientMessage(net.minecraft.network.chat.Component message, boolean overlay) { lastMessage = message.getString(); }
    }
}
