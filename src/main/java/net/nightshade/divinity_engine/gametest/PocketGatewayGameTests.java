package net.nightshade.divinity_engine.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.nightshade.divinity_engine.network.messages.gui.PocketRoomSizeMessage;
import net.nightshade.divinity_engine.network.messages.gui.PocketResidentsMessage;
import net.nightshade.renderlib.server.RenderEffectSavedData;
import net.nightshade.divinity_engine.registry.blocks.BlocksRegistry;
import net.nightshade.divinity_engine.registry.dimensions.DimensionRegistry;
import net.nightshade.divinity_engine.registry.divinity.blessing.BlessingsRegistry;
import net.nightshade.divinity_engine.registry.divinity.gods.GodsRegistry;
import net.nightshade.divinity_engine.network.cap.player.gods.PlayerGodsCapability;
import net.nightshade.divinity_engine.util.DivinityEngineHelper;
import net.nightshade.divinity_engine.util.divinity.blessings.aethon.*;
import net.nightshade.divinity_engine.world.inventory.PocketDomainMenu;

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

import static net.nightshade.divinity_engine.util.divinity.blessings.aethon.PocketDomainData.*;

@Mod.EventBusSubscriber(modid = "divinity_engine", bus = Mod.EventBusSubscriber.Bus.MOD)
@PrefixGameTestTemplate(false)
public final class PocketGatewayGameTests {
    private static final String SUITE = "divinity_pocket_domain";
    @SubscribeEvent public static void register(RegisterGameTestsEvent event) {
        if (Arrays.stream(System.getProperty("forge.enabledGameTestNamespaces", "").split(",")).noneMatch(SUITE::equals)) return;
        for (var method : PocketGatewayGameTests.class.getDeclaredMethods())
            if (method.isAnnotationPresent(GameTest.class)) GameTestRegistry.register(method, Set.of("divinity_engine"));
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", batch = SUITE)
    public static void roomSizesTowerAndGatewaysSurviveReload(GameTestHelper h) {
        var data = new PocketDomainData(); var domain = data.getOrCreate(UUID.randomUUID());
        var size = new Dimensions(19, 12, 23);
        Room first = data.planRoom(domain, Style.WORKSHOP, size); first.stage = 2;
        Room second = data.planRoom(domain, Style.GROVE, size);
        h.assertTrue(second.origin.equals(first.origin.above(32)), "New chambers are not stacked");
        second.stage = 2;
        h.assertTrue(data.planResize(domain, first, new Dimensions(25, 16, 27)), "Resize not queued");
        first.cursor = 81;
        domain.entrance = new ReturnPoint("minecraft:the_nether", new Vec3(25.5, 61, -2.5), 90, 0);
        domain.exit = new ReturnPoint(DimensionRegistry.POCKET_DIMENSION.location().toString(), second.arrival(), 0, 0);
        domain.inventory.setItem(0, new ItemStack(Items.DIAMOND, 7));
        var saved = PocketDomainData.load(data.save(new CompoundTag())).find(domain.owner);
        h.assertTrue(saved.entrance.equals(domain.entrance) && saved.exit.equals(domain.exit), "Portal anchors moved on reload");
        h.assertTrue(saved.rooms.get(0).size.equals(size) && saved.rooms.get(0).pendingSize.width() == 25
                && saved.rooms.get(0).cursor == 81 && saved.rooms.get(0).stage == 3, "In-progress resize not restored");
        h.assertTrue(saved.inventory.getItem(0).getCount() == 7, "Storage changed");
        var legacy = data.save(new CompoundTag());
        var oldRoom = legacy.getList("Domains", 10).getCompound(0).getList("Rooms", 10).getCompound(1);
        oldRoom.remove("Size");
        h.assertTrue(PocketDomainData.load(legacy).find(domain.owner).rooms.get(1).size.equals(new Dimensions(SIZE, HEIGHT, SIZE)), "V1 room defaults changed");
        h.assertTrue(data.planRoom(domain, Style.ARENA, new Dimensions(Integer.MAX_VALUE, 24, 33)) == null, "Unbounded size accepted");
        h.succeed();
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", batch = SUITE)
    public static void resizeOnlyRemovesOwnedShellAndFloor(GameTestHelper h) {
        Room room = new Room(BlockPos.ZERO, Style.SANCTUM, false);
        room.size = new Dimensions(15, 10, 15); room.pendingSize = new Dimensions(11, 8, 11);
        var boundary = BlocksRegistry.POCKET_DIMENSION.get().defaultBlockState();
        h.assertTrue(PocketRoomBuilder.replacement(room, new BlockPos(14, 2, 5), boundary, false).isAir(), "Old shell not cleared");
        h.assertTrue(PocketRoomBuilder.replacement(room, new BlockPos(5, 2, 5), Blocks.CHEST.defaultBlockState(), false).is(Blocks.CHEST), "Interior container changed");
        h.assertTrue(PocketRoomBuilder.replacement(room, new BlockPos(10, 2, 5), Blocks.CHEST.defaultBlockState(), false) == null, "New wall would erase a chest");
        h.assertTrue(PocketRoomBuilder.replacement(room, new BlockPos(13, 2, 5), Blocks.DIAMOND_BLOCK.defaultBlockState(), false) == null, "Shrink would erase a build");
        h.assertTrue(PocketRoomBuilder.replacement(room, new BlockPos(5, 9, 5), boundary, false).isAir(), "Old ceiling not cleared");
        h.succeed();
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", batch = SUITE, timeoutTicks = 1600)
    public static void batchedResizePreservesChestAndRejectsObstructions(GameTestHelper h) {
        TestPlayer owner = new TestPlayer(h.getLevel());
        var data = PocketDomainData.get(owner.server); var domain = PocketDomainManager.domain(owner);
        var size = new Dimensions(15, 10, 15); var smaller = new Dimensions(11, 8, 11);
        Room room = data.planRoom(domain, Style.SANCTUM, size);
        ServerLevel level = pocket(h);
        BlockPos chestPos = room.origin.offset(5, 2, 5), obstruction = room.origin.offset(13, 2, 5), outside = room.origin.offset(30, 2, 5);
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(room.ready(), "Initial build pending"))
                .thenExecute(() -> {
                    level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 2);
                    ((ChestBlockEntity) level.getBlockEntity(chestPos)).setItem(0, new ItemStack(Items.DIAMOND, 7));
                    level.setBlock(obstruction, Blocks.DIAMOND_BLOCK.defaultBlockState(), 2);
                    level.setBlock(outside, Blocks.GOLD_BLOCK.defaultBlockState(), 2);
                    h.assertTrue(PocketDomainManager.resizeRoom(owner, room, smaller), "Resize rejected before scan");
                })
                .thenWaitUntil(() -> h.assertTrue(room.ready(), "Obstruction check pending"))
                .thenExecute(() -> {
                    h.assertTrue(room.size.equals(size) && level.getBlockState(obstruction).is(Blocks.DIAMOND_BLOCK), "Unsafe shrink ran");
                    level.setBlock(obstruction, Blocks.AIR.defaultBlockState(), 2);
                    h.assertTrue(PocketDomainManager.resizeRoom(owner, room, smaller), "Safe shrink not queued");
                })
                .thenWaitUntil(() -> h.assertTrue(room.ready(), "Shrink pending"))
                .thenExecute(() -> {
                    h.assertTrue(room.size.equals(smaller), "Shrink dimensions not applied");
                    h.assertTrue(level.getBlockState(room.origin.offset(14, 2, 5)).isAir(), "Old shell remains");
                    h.assertTrue(level.getBlockState(room.origin.offset(10, 2, 5)).is(BlocksRegistry.POCKET_DIMENSION.get()), "New shell missing");
                    h.assertTrue(PocketDomainManager.resizeRoom(owner, room, new Dimensions(19, 12, 17)), "Expansion not queued");
                })
                .thenWaitUntil(() -> h.assertTrue(room.ready(), "Expansion pending"))
                .thenExecute(() -> {
                    h.assertTrue(room.size.width() == 19 && room.size.height() == 12, "Expansion dimensions wrong");
                    h.assertTrue(((ChestBlockEntity) level.getBlockEntity(chestPos)).getItem(0).getCount() == 7, "Container contents lost");
                    h.assertTrue(level.getBlockState(outside).is(Blocks.GOLD_BLOCK), "Unrelated blocks changed");
                    owner.dispose();
                }).thenSucceed();
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", batch = SUITE)
    public static void sizePacketsRejectForeignMenusAndInvalidInputs(GameTestHelper h) {
        TestPlayer owner = new TestPlayer(h.getLevel()), stranger = new TestPlayer(h.getLevel());
        Room room = readyRoom(owner); var domain = PocketDomainManager.domain(owner);
        var menu = new PocketDomainMenu(7, owner.getInventory(), PocketDomainManager.snapshot(domain));
        owner.containerMenu = menu; stranger.containerMenu = menu;
        h.assertFalse(new PocketRoomSizeMessage(7, 0, 0, 13, 10, 13).apply(stranger), "Foreign menu changed owner room");
        h.assertFalse(new PocketRoomSizeMessage(8, 0, 0, 13, 10, 13).apply(owner), "Stale menu accepted");
        h.assertFalse(new PocketRoomSizeMessage(7, 0, 0, Integer.MAX_VALUE, 10, 13).apply(owner), "Oversize accepted");
        h.assertFalse(new PocketRoomSizeMessage(7, 0, 0, 13, -1, 13).apply(owner), "Invalid height accepted");
        domain.rooms.set(0, new Room(room.origin.above(32), Style.SANCTUM, false));
        h.assertFalse(new PocketRoomSizeMessage(7, 0, 0, 13, 10, 13).apply(owner), "Stale room identity accepted");
        owner.dispose(); stranger.dispose(); h.succeed();
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", batch = SUITE)
    public static void sizePacketQueuesOnlyOneOwnedResize(GameTestHelper h) {
        TestPlayer owner = new TestPlayer(h.getLevel()); Room room = readyRoom(owner);
        var domain = PocketDomainManager.domain(owner);
        owner.containerMenu = new PocketDomainMenu(7, owner.getInventory(), PocketDomainManager.snapshot(domain));
        var request = new PocketRoomSizeMessage(7, 0, 0, 19, 12, 17);
        h.assertTrue(request.apply(owner), "Valid dimension inputs were not applied");
        h.assertTrue(room.stage == 3 && room.pendingSize.equals(new Dimensions(19, 12, 17)), "Wrong owned resize queued");
        h.assertFalse(request.apply(owner), "Duplicate/stale resize was accepted");
        h.assertTrue(room.size.equals(new Dimensions(15, 10, 15)), "Dimensions changed before safety scan");
        owner.dispose(); h.succeed();
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", batch = SUITE)
    public static void placedEntranceCannotMoveUntilOwnerClosesIt(GameTestHelper h) {
        TestPlayer owner = new TestPlayer(h.getLevel()), stranger = new TestPlayer(h.getLevel()); readyRoom(owner);
        Vec3 pad = pad(h); owner.setPos(pad); owner.setYRot(0); owner.setXRot(65);
        h.assertTrue(PocketPortals.place(owner, true), "Aimed portal placement failed: " + owner.lastMessage);
        var domain = PocketDomainManager.domain(owner); var anchor = domain.entrance;
        owner.setPos(pad.add(1, 0, 0));
        h.assertFalse(PocketPortals.place(owner, true), "Open portal silently moved");
        PocketPortals.close(stranger, true);
        h.assertTrue(domain.entrance.equals(anchor), "Another owner closed the portal");
        h.assertTrue(h.getLevel().getBlockState(BlockPos.containing(anchor.position()).below()).is(Blocks.STONE), "Portal changed the floor");
        h.assertTrue(PocketPortals.close(owner, true) && domain.entrance == null, "Owner cannot close own portal");
        owner.dispose(); stranger.dispose(); h.succeed();
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", batch = SUITE, timeoutTicks = 400)
    public static void entranceAndExitTravelWithoutBounceAndPreserveAnchors(GameTestHelper h) {
        TestPlayer owner = new TestPlayer(h.getLevel()); Room room = readyRoom(owner);
        Vec3 pad = pad(h); var domain = PocketDomainManager.domain(owner);
        domain.entrance = new ReturnPoint(h.getLevel().dimension().location().toString(), pad, 0, 0);
        domain.exit = new ReturnPoint(DimensionRegistry.POCKET_DIMENSION.location().toString(), room.arrival(), 0, 0);
        var entrance = domain.entrance;
        owner.setPos(pad.add(3, 0, 0));
        h.assertFalse(PocketDomainManager.enter(owner, 0), "Owner entered remotely without a portal");
        owner.setPos(pad);
        h.startSequence().thenWaitUntil(() -> {
            PocketPortals.tick(owner);
            h.assertTrue(owner.serverLevel() == pocket(h), "Waiting for prepared entrance: " + owner.lastMessage);
        }).thenExecute(() -> {
            PocketPortals.tick(owner);
            h.assertTrue(owner.serverLevel() == pocket(h), "Arrival immediately bounced out");
            owner.setPos(room.arrival().add(0, 0, 2)); PocketPortals.tick(owner);
            owner.setPos(room.arrival()); domain.nextPortal = 0;
            owner.getPersistentData().putLong("PocketPortalCooldown", 0);
            DivinityEngineHelper.setBlessingSlot(owner, 0, null);
        }).thenWaitUntil(() -> {
            PocketPortals.tick(owner);
            h.assertTrue(owner.serverLevel() == h.getLevel(), "Waiting for prepared exit: " + owner.lastMessage);
        }).thenExecute(() -> {
            h.assertTrue(domain.entrance.equals(entrance), "Travel moved the entrance");
            h.assertTrue(owner.position().distanceToSqr(pad) < 25, "Exit ignored the entrance location");
            owner.dispose();
        }).thenSucceed();
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", batch = SUITE)
    public static void roomGatewaysLinkBothDirectionsForVisitorsWithoutManagementAccess(GameTestHelper h) {
        TestPlayer owner = new TestPlayer(pocket(h)), stranger = new TestPlayer(pocket(h));
        Room lower = readyRoom(owner), upper = readyRoom(owner);
        var domain = PocketDomainManager.domain(owner);
        DivinityEngineHelper.setBlessingSlot(stranger, 0, null);
        stranger.setPos(PocketPortals.roomGate(lower, true)); PocketPortals.tick(stranger);
        h.assertTrue(upper.contains(stranger.blockPosition()), "Visitor could not use a room gateway");
        h.assertFalse(PocketDomainManager.canManage(stranger), "Visitor received domain management access");
        owner.setPos(PocketPortals.roomGate(lower, true)); PocketPortals.tick(owner);
        h.assertTrue(upper.contains(owner.blockPosition()) && domain.selected == 1, "Up gateway did not reach next room");
        PocketPortals.tick(owner); domain.nextPortal = 0;
        owner.getPersistentData().putLong("PocketPortalCooldown", 0);
        owner.setPos(PocketPortals.roomGate(upper, false)); PocketPortals.tick(owner);
        h.assertTrue(lower.contains(owner.blockPosition()) && domain.selected == 0, "Down gateway did not reach previous room");
        owner.dispose(); stranger.dispose(); h.succeed();
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", batch = SUITE)
    public static void visitorsKeepTheirReturnPointAcrossReloadAndCannotManageHost(GameTestHelper h) {
        TestPlayer host = new TestPlayer(h.getLevel()), visitor = new TestPlayer(h.getLevel());
        Room room = readyRoom(host); var domain = PocketDomainManager.domain(host); Vec3 pad = pad(h);
        domain.entrance = new ReturnPoint(h.getLevel().dimension().location().toString(), pad, 0, 0);
        DivinityEngineHelper.setBlessingSlot(visitor, 0, null); visitor.setPos(pad); PocketPortals.tick(visitor);
        h.assertTrue(room.contains(visitor.blockPosition()) && visitor.serverLevel() == pocket(h), "Unequipped visitor did not enter");
        var data = PocketDomainData.get(host.server);
        var saved = PocketDomainData.load(data.save(new CompoundTag()));
        h.assertTrue(saved.visit(visitor.getUUID()).host().equals(host.getUUID()), "Visitor host not saved");
        h.assertFalse(domain.residents.containsKey(visitor.getUUID()), "Player treated as a reviving resident");
        h.assertFalse(PocketDomainManager.canManage(visitor), "Visitor can command host domain");
        domain.entrance = null;
        h.assertTrue(PocketDomainManager.exit(visitor), "Closing entrance trapped visitor");
        h.assertTrue(visitor.position().distanceToSqr(pad) < 25 && data.visit(visitor.getUUID()) == null, "Visitor return was lost");
        host.dispose(); visitor.dispose(); h.succeed();
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", batch = SUITE, timeoutTicks = 300)
    public static void sharedGatewaysAdmitMoreThanTwelveMobsButNotItemsAsResidents(GameTestHelper h) {
        TestPlayer owner = new TestPlayer(h.getLevel()); Room room = readyRoom(owner);
        var domain = PocketDomainManager.domain(owner); Vec3 pad = pad(h);
        domain.entrance = new ReturnPoint(h.getLevel().dimension().location().toString(), pad, 0, 0);
        java.util.List<Entity> moved = new java.util.ArrayList<>();
        for (int i = 0; i < 15; i++) {
            Zombie zombie = EntityType.ZOMBIE.create(h.getLevel()); zombie.setPos(pad); zombie.setNoAi(true);
            zombie.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
            h.getLevel().addFreshEntity(zombie);
            Entity resident = PocketDomainManager.crossGateway(zombie, domain, 0);
            h.assertTrue(resident instanceof Zombie && resident.getUUID().equals(zombie.getUUID()), "Mob transfer failed at " + i);
            moved.add(resident);
        }
        h.assertTrue(domain.residents.size() == 15, "Old twelve-resident cap remains");
        ItemEntity item = new ItemEntity(h.getLevel(), pad.x, pad.y, pad.z, new ItemStack(Items.DIAMOND, 7));
        h.getLevel().addFreshEntity(item); Entity itemMoved = PocketDomainManager.crossGateway(item, domain, 0);
        h.assertTrue(itemMoved instanceof ItemEntity && ((ItemEntity) itemMoved).getItem().getCount() == 7, "Dropped stack lost in transit");
        Arrow arrow = new Arrow(h.getLevel(), pad.x, pad.y, pad.z); arrow.setDeltaMovement(0, 0, 1);
        h.getLevel().addFreshEntity(arrow); Entity arrowMoved = PocketDomainManager.crossGateway(arrow, domain, 0);
        h.assertTrue(arrowMoved instanceof Arrow && arrowMoved.getDeltaMovement().z == 1, "Projectile lost in transit");
        h.assertTrue(domain.residents.size() == 15, "Items/projectiles became residents");
        Entity released = PocketDomainManager.crossGateway(moved.get(0), domain, -1);
        h.assertTrue(released != null && domain.residents.size() == 14 && !released.getPersistentData().hasUUID(PocketDomainManager.RESIDENT_OWNER), "Portal exit did not release resident");
        released.discard(); itemMoved.discard(); arrowMoved.discard(); moved.forEach(Entity::discard);
        owner.dispose(); h.succeed();
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", batch = SUITE)
    public static void residentMenuPagesStayBoundedWithoutAResidentCap(GameTestHelper h) {
        TestPlayer owner = new TestPlayer(h.getLevel()), stranger = new TestPlayer(h.getLevel());
        var domain = PocketDomainManager.domain(owner);
        for (int i = 0; i < 1000; i++) {
            UUID id = UUID.randomUUID(); domain.residents.put(id, new Resident(id, "Resident " + i, 0, PocketDomainManager.point(owner), 0));
        }
        Set<UUID> seen = new java.util.HashSet<>();
        var first = PocketDomainManager.snapshot(domain);
        h.assertTrue(first.getInt("ResidentCount") == 1000, "Resident total was truncated");
        for (int page = 0; page < first.getInt("ResidentPages"); page++) {
            var entries = PocketDomainManager.snapshot(domain, page).getList("Residents", 10);
            h.assertTrue(entries.size() <= RESIDENT_PAGE_SIZE, "Unbounded menu snapshot");
            for (int i = 0; i < entries.size(); i++) seen.add(entries.getCompound(i).getUUID("Id"));
        }
        h.assertTrue(seen.size() == 1000, "Some resident pages are unreachable");
        var menu = new PocketDomainMenu(7, owner.getInventory(), first); stranger.containerMenu = menu;
        h.assertFalse(new PocketResidentsMessage(7, 0, seen.iterator().next()).apply(stranger), "Foreign resident action accepted");
        var saved = PocketDomainData.load(PocketDomainData.get(owner.server).save(new CompoundTag()));
        h.assertTrue(saved.find(owner.getUUID()).residents.size() == 1000, "Save capped residents");
        owner.dispose(); stranger.dispose(); h.succeed();
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", batch = SUITE)
    public static void persistentPortalVisualsDoNotExpireOrRespawnEverySecond(GameTestHelper h) {
        TestPlayer owner = new TestPlayer(h.getLevel()); readyRoom(owner); var domain = PocketDomainManager.domain(owner);
        Vec3 pad = pad(h); owner.setPos(pad); h.getLevel().addNewPlayer(owner);
        domain.entrance = new ReturnPoint(h.getLevel().dimension().location().toString(), pad, 0, 0);
        PocketPortals.visuals(owner.server);
        var effects = RenderEffectSavedData.get(h.getLevel());
        var matches = effects.snapshot(h.getLevel().getGameTime()).stream().filter(effect -> effect.spec().parameters().hasUUID("PocketPortalOwner")
                && effect.spec().parameters().getUUID("PocketPortalOwner").equals(owner.getUUID())).toList();
        h.assertTrue(matches.size() == 1, "Portal effect missing or duplicated");
        var effect = matches.get(0);
        h.assertFalse(effect.expired(h.getLevel().getGameTime() + 100_000), "Portal expires during a server stall");
        PocketPortals.visuals(owner.server);
        h.assertTrue(effects.get(effect.id()).isPresent(), "Stable portal removed/recreated on refresh");
        PocketPortals.stopped(); PocketPortals.visuals(owner.server);
        h.assertTrue(effects.get(effect.id()).isPresent(), "Saved portal handle not adopted after runtime reset");
        PocketPortals.close(owner, true);
        h.assertTrue(effects.get(effect.id()).isEmpty(), "Closed portal retained indefinite visual");
        owner.dispose(); h.succeed();
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", batch = SUITE, timeoutTicks = 1600)
    public static void emptyRoomUnloadsButExplicitChunkLoaderPreservesIt(GameTestHelper h) {
        TestPlayer owner = new TestPlayer(h.getLevel()); Room room = readyRoom(owner); ServerLevel level = pocket(h);
        var chunk = new net.minecraft.world.level.ChunkPos(room.origin);
        level.setChunkForced(chunk.x, chunk.z, true);
        BlockPos chest = room.origin.offset(5, 2, 5);
        level.setBlock(chest, Blocks.CHEST.defaultBlockState(), 2);
        ((ChestBlockEntity) level.getBlockEntity(chest)).setItem(0, new ItemStack(Items.DIAMOND, 7));
        Zombie mob = EntityType.ZOMBIE.create(level); mob.setPos(room.arrival()); mob.setNoAi(true); mob.setPersistenceRequired();
        mob.getPersistentData().putUUID(PocketDomainManager.RESIDENT_OWNER, owner.getUUID());
        level.addFreshEntity(mob); UUID id = mob.getUUID();
        var domain = PocketDomainManager.domain(owner);
        domain.residents.put(id, new Resident(id, "Sleeping resident", 0, PocketDomainManager.point(owner), 0));
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(level.getEntity(id) == mob, "Waiting for entity chunk"))
                .thenExecute(() -> {
                    PocketChunkLoading.prepare(owner, level, room.arrival()); PocketChunkLoading.release(owner.getUUID());
                    h.assertTrue(level.getForcedChunks().contains(chunk.toLong()), "Travel release removed a chunk loader");
                    level.setChunkForced(chunk.x, chunk.z, false);
                })
                .thenWaitUntil(() -> h.assertTrue(level.getChunkSource().getChunkNow(chunk.x, chunk.z) == null, "Empty room is still kept loaded"))
                .thenExecute(() -> {
                    h.assertTrue(domain.residents.containsKey(id), "Unloading removed resident record");
                    h.assertFalse(PocketChunkLoading.prepare(owner, level, room.arrival()), "Cold arrival blocked instead of returning pending");
                    h.assertTrue(level.getChunkSource().getChunkNow(chunk.x, chunk.z) == null, "Prepare synchronously loaded a chunk");
                    PocketChunkLoading.release(owner.getUUID());
                    level.setChunkForced(chunk.x, chunk.z, true);
                })
                .thenWaitUntil(() -> h.assertTrue(level.getChunkSource().getChunkNow(chunk.x, chunk.z) != null && level.getEntity(id) instanceof Zombie, "Waiting for saved room to reload"))
                .thenExecute(() -> {
                    h.assertTrue(((ChestBlockEntity) level.getBlockEntity(chest)).getItem(0).getCount() == 7, "Sleeping room lost chest contents");
                    h.assertTrue(domain.residents.containsKey(id) && level.getEntity(id).getPersistentData().getUUID(PocketDomainManager.RESIDENT_OWNER).equals(owner.getUUID()), "Resident lost identity after reload");
                    level.getEntity(id).discard(); level.setChunkForced(chunk.x, chunk.z, false); owner.dispose();
                }).thenSucceed();
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", batch = SUITE, timeoutTicks = 1600)
    public static void sharedPortalWakesSleepingRoomWithoutLosingItemsOrProjectiles(GameTestHelper h) {
        TestPlayer owner = new TestPlayer(h.getLevel()); Room room = readyRoom(owner); ServerLevel level = pocket(h);
        var domain = PocketDomainManager.domain(owner); Vec3 pad = pad(h);
        var chunk = new net.minecraft.world.level.ChunkPos(room.origin);
        domain.entrance = new ReturnPoint(h.getLevel().dimension().location().toString(), pad, 0, 0);
        var item = new ItemEntity(h.getLevel(), pad.x, pad.y + 0.5, pad.z, new ItemStack(Items.DIAMOND, 7));
        item.setNoGravity(true); item.setDeltaMovement(Vec3.ZERO); item.setPickUpDelay(200);
        var arrow = new Arrow(h.getLevel(), pad.x, pad.y + 1, pad.z);
        arrow.setNoGravity(true); arrow.setDeltaMovement(0, 0, 1);
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(level.getChunkSource().getChunkNow(chunk.x, chunk.z) == null, "Waiting for sleeping room"))
                .thenExecute(() -> {
                    h.getLevel().addFreshEntity(item); h.getLevel().addFreshEntity(arrow);
                    for (int i = 0; i < 64 && (!PocketChunkLoading.pending(item.getUUID()) || !PocketChunkLoading.pending(arrow.getUUID())); i++)
                        PocketPortals.tickEntities(owner.server);
                    h.assertTrue(PocketChunkLoading.pending(item.getUUID()) && PocketChunkLoading.pending(arrow.getUUID()), "Cold crossings not queued");
                    h.assertFalse(item.isRemoved() || arrow.isRemoved(), "Entity removed before destination loaded");
                    h.assertTrue(level.getChunkSource().getChunkNow(chunk.x, chunk.z) == null, "Gateway blocked on cold chunk load");
                })
                .thenWaitUntil(() -> h.assertTrue(item.isRemoved() && arrow.isRemoved(), "Waiting for queued transfers"))
                .thenExecute(() -> level.setChunkForced(chunk.x, chunk.z, true))
                .thenWaitUntil(() -> h.assertTrue(level.getEntity(item.getUUID()) instanceof ItemEntity && level.getEntity(arrow.getUUID()) instanceof Arrow, "Waiting for destination entity visibility"))
                .thenExecute(() -> {
                    h.assertTrue(((ItemEntity) level.getEntity(item.getUUID())).getItem().getCount() == 7, "Cold crossing lost item stack");
                    h.assertTrue(level.getEntity(arrow.getUUID()).getDeltaMovement().lengthSqr() > 0, "Cold crossing lost projectile motion");
                    h.assertTrue(domain.residents.isEmpty(), "Items or projectiles registered as creatures");
                    h.assertFalse(PocketChunkLoading.pending(item.getUUID()) || PocketChunkLoading.pending(arrow.getUUID()), "Completed crossings retained arrival tickets");
                    level.getEntity(item.getUUID()).discard(); level.getEntity(arrow.getUUID()).discard();
                    level.setChunkForced(chunk.x, chunk.z, false); domain.entrance = null; owner.dispose();
                }).thenSucceed();
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", batch = SUITE, timeoutTicks = 1600)
    public static void closingPortalCancelsColdCrossingWithoutRemovingEntity(GameTestHelper h) {
        TestPlayer owner = new TestPlayer(h.getLevel()); Room room = readyRoom(owner); ServerLevel level = pocket(h);
        var domain = PocketDomainManager.domain(owner); Vec3 pad = pad(h);
        var chunk = new net.minecraft.world.level.ChunkPos(room.origin);
        domain.entrance = new ReturnPoint(h.getLevel().dimension().location().toString(), pad, 0, 0);
        var item = new ItemEntity(h.getLevel(), pad.x, pad.y + 0.5, pad.z, new ItemStack(Items.DIAMOND, 7));
        Vec3 velocity = new Vec3(0.1, 0, 0); item.setNoGravity(true); item.setDeltaMovement(velocity);
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(level.getChunkSource().getChunkNow(chunk.x, chunk.z) == null, "Waiting for sleeping room"))
                .thenExecute(() -> {
                    h.getLevel().addFreshEntity(item);
                    for (int i = 0; i < 64 && !PocketChunkLoading.pending(item.getUUID()); i++) PocketPortals.tickEntities(owner.server);
                    h.assertTrue(PocketChunkLoading.pending(item.getUUID()), "Cold crossing not queued");
                    PocketPortals.close(owner, true); PocketPortals.tickEntities(owner.server);
                    h.assertTrue(item.isAlive() && item.level() == h.getLevel() && item.getItem().getCount() == 7, "Closing gateway lost source entity");
                    h.assertTrue(item.getDeltaMovement().equals(velocity), "Cancelled crossing did not restore motion");
                    h.assertFalse(PocketChunkLoading.pending(item.getUUID()), "Cancelled crossing retained arrival ticket");
                    item.discard(); owner.dispose();
                }).thenSucceed();
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", batch = SUITE, timeoutTicks = 400)
    public static void everyFloorHasAnExitAndSeparateAdjacentFloorGateways(GameTestHelper h) {
        TestPlayer owner = new TestPlayer(h.getLevel());
        Room lower = readyRoom(owner), middle = readyRoom(owner), upper = readyRoom(owner);
        var domain = PocketDomainManager.domain(owner); Vec3 pad = pad(h);
        domain.entrance = new ReturnPoint(h.getLevel().dimension().location().toString(), pad, 0, 0);
        h.startSequence().thenWaitUntil(() -> h.assertTrue(PocketChunkLoading.prepare(owner, pocket(h), middle.arrival()), "Waiting for tower"))
                .thenExecute(() -> {
                    owner.teleportTo(pocket(h), middle.arrival().x, middle.arrival().y, middle.arrival().z, 0, 0);
                    PocketPortals.visuals(owner.server);
                    var effects = RenderEffectSavedData.get(pocket(h)).snapshot(pocket(h).getGameTime()).stream()
                            .filter(effect -> effect.spec().parameters().hasUUID("PocketPortalOwner")
                                    && effect.spec().parameters().getUUID("PocketPortalOwner").equals(owner.getUUID())).toList();
                    h.assertTrue(effects.size() == 7, "Three-floor tower should show three exits and four direction gateways, got " + effects.size());
                    h.assertTrue(effects.stream().filter(effect -> effect.spec().parameters().getDouble("LinkX") > 0).count() == 3, "Exit sigils missing");
                    for (int i = 0; i < domain.rooms.size(); i++) {
                        Room room = domain.rooms.get(i);
                        Vec3 exit = PocketPortals.roomExit(domain, room).position();
                        h.assertTrue(exit.distanceToSqr(PocketPortals.roomGate(room, false)) > 4
                                && exit.distanceToSqr(PocketPortals.roomGate(room, true)) > 4, "Exit overlaps a floor gateway");
                        for (boolean above : new boolean[]{false, true}) {
                            int target = i + (above ? 1 : -1);
                            Vec3 gate = PocketPortals.roomGate(room, above);
                            owner.teleportTo(pocket(h), gate.x, gate.y, gate.z, 0, 0);
                            owner.getPersistentData().remove("PocketPortalLatched");
                            owner.getPersistentData().putLong("PocketPortalCooldown", 0);
                            PocketPortals.tick(owner);
                            Room expected = target >= 0 && target < domain.rooms.size() ? domain.rooms.get(target) : room;
                            h.assertTrue(expected.contains(owner.blockPosition()), "Wrong floor from " + i + " above=" + above);
                        }
                        owner.teleportTo(pocket(h), exit.x, exit.y, exit.z, 0, 0);
                        owner.getPersistentData().remove("PocketPortalLatched");
                        owner.getPersistentData().putLong("PocketPortalCooldown", 0);
                        PocketPortals.tick(owner);
                        h.assertTrue(owner.serverLevel() == h.getLevel() && owner.position().distanceToSqr(pad) < 25, "Floor " + i + " has no working exit");
                    }
                    PocketChunkLoading.release(owner.getUUID()); owner.dispose();
                }).thenSucceed();
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", batch = SUITE, timeoutTicks = 400)
    public static void perRoomExitsPreserveLegacyAnchorsAndCloseIndependently(GameTestHelper h) {
        TestPlayer owner = new TestPlayer(pocket(h)), stranger = new TestPlayer(pocket(h));
        Room lower = readyRoom(owner), upper = readyRoom(owner), top = readyRoom(owner);
        var domain = PocketDomainManager.domain(owner);
        var oldExit = new ReturnPoint(DimensionRegistry.POCKET_DIMENSION.location().toString(), lower.arrival(), 90, 0);
        domain.exit = oldExit;
        h.startSequence().thenWaitUntil(() -> h.assertTrue(PocketChunkLoading.prepare(owner, pocket(h), upper.arrival()), "Waiting for tower"))
                .thenExecute(() -> {
                    h.assertTrue(PocketPortals.roomExit(domain, lower).equals(oldExit), "Legacy exit relocated");
                    h.assertTrue(PocketPortals.closeRoom(owner, 1), "Cannot close upper exit");
                    h.assertTrue(PocketPortals.roomExit(domain, upper) == null && PocketPortals.roomExit(domain, lower).equals(oldExit)
                            && PocketPortals.roomExit(domain, top) != null, "Closing upper affected other exits");
                    owner.setPos(upper.arrival()); owner.setYRot(0); owner.setXRot(65);
                    h.assertTrue(PocketPortals.place(owner, false), "Per-room exit placement failed: " + owner.lastMessage);
                    var placed = upper.exit;
                    h.assertTrue(placed != null && domain.exit.equals(oldExit), "Upper placement overwrote lower legacy exit");
                    owner.setPos(upper.arrival().add(1, 0, 0));
                    h.assertFalse(PocketPortals.place(owner, false), "Open room exit moved without being closed");
                    h.assertFalse(PocketPortals.closeRoom(stranger, 0), "Foreign player closed host exit");
                    var menu = new PocketDomainMenu(7, owner.getInventory(), PocketDomainManager.snapshot(domain)); owner.containerMenu = menu;
                    domain.nextAction = 0;
                    h.assertTrue(menu.clickMenuButton(owner, 120), "Selected-room close action failed");
                    h.assertTrue(PocketPortals.roomExit(domain, lower) == null && upper.exit.equals(placed)
                            && PocketPortals.roomExit(domain, top) != null, "Closing lower affected upper/top");
                    var restored = PocketDomainData.load(PocketDomainData.get(owner.server).save(new CompoundTag())).find(owner.getUUID());
                    h.assertTrue(restored.rooms.get(0).exitClosed && PocketPortals.roomExit(restored, restored.rooms.get(0)) == null, "Closed exit reopened on reload");
                    h.assertTrue(restored.rooms.get(1).exit.equals(placed) && PocketPortals.roomExit(restored, restored.rooms.get(2)) != null, "Per-room anchors not preserved");
                    h.assertFalse(PocketDomainManager.resizeRoom(owner, upper, new Dimensions(9, 8, 9)), "Resize would strand a custom exit");
                    PocketChunkLoading.release(owner.getUUID()); owner.dispose(); stranger.dispose();
                }).thenSucceed();
    }

    private static Vec3 pad(GameTestHelper h) {
        BlockPos floor = h.absolutePos(new BlockPos(14, 1, 14));
        for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) {
            h.getLevel().setBlock(floor.offset(x, 0, z), Blocks.STONE.defaultBlockState(), 2);
            for (int y = 1; y <= 4; y++) h.getLevel().setBlock(floor.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
        }
        return Vec3.atBottomCenterOf(floor.above());
    }
    private static ServerLevel pocket(GameTestHelper h) { return h.getLevel().getServer().getLevel(DimensionRegistry.POCKET_DIMENSION); }
    private static Room readyRoom(TestPlayer owner) {
        var data = PocketDomainData.get(owner.server); var domain = PocketDomainManager.domain(owner);
        Room room = data.planRoom(domain, Style.SANCTUM, new Dimensions(15, 10, 15)); room.stage = 2;
        ServerLevel level = owner.server.getLevel(DimensionRegistry.POCKET_DIMENSION);
        for (int x = 1; x < 14; x++) for (int z = 1; z < 14; z++) level.setBlock(room.origin.offset(x, 1, z), Blocks.STONE.defaultBlockState(), 2);
        return room;
    }
    private static final class TestPlayer extends ServerPlayer {
        String lastMessage = "";
        TestPlayer(ServerLevel level) {
            super(level.getServer(), level, new GameProfile(UUID.randomUUID(), "gateway-test"));
            connection = new ServerGamePacketListenerImpl(server, new Connection(PacketFlow.SERVERBOUND), this) {
                @Override public void send(Packet<?> packet) {}
            };
            var god = GodsRegistry.AETHON.get().createGodDefaultInstance(); god.setFavor(250);
            PlayerGodsCapability.load(this).updateGodInstance(god, false);
            var blessing = BlessingsRegistry.POCKET_DIMENSION.get().createDefaultInstance(); blessing.setBoundGod(god);
            DivinityEngineHelper.setBlessingSlot(this, 0, blessing);
        }
        @Override public void displayClientMessage(Component message, boolean overlay) { lastMessage = message.getString(); }
        void dispose() { serverLevel().removePlayerImmediately(this, Entity.RemovalReason.DISCARDED); }
    }
}
