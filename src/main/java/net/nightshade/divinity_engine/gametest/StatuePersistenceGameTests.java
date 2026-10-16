package net.nightshade.divinity_engine.gametest;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;
import net.nightshade.divinity_engine.block.StatueBlock;
import net.nightshade.divinity_engine.block.entity.StatueBlockEntity;
import net.nightshade.divinity_engine.api.gods.GodsDeferredRegister;
import net.nightshade.divinity_engine.api.gods.StatueResources;
import net.nightshade.divinity_engine.divinity.gods.BaseGod;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Mod.EventBusSubscriber(modid = "divinity_engine", bus = Mod.EventBusSubscriber.Bus.MOD)
@PrefixGameTestTemplate(false)
public final class StatuePersistenceGameTests {
    private static final String SUITE = "divinity_statue_persistence";
    private static GodsDeferredRegister addonA;
    private static GodsDeferredRegister addonB;
    private static boolean duplicateGodRejected;
    private static boolean duplicateStatueRejected;

    /** No test-only registry IDs are added in normal games or existing player worlds. */
    public static void registerFixtures(IEventBus bus) {
        if (!enabled()) return;
        addonA = GodsDeferredRegister.create("statue_test_a");
        addonB = GodsDeferredRegister.create("statue_test_b");
        addonA.register("solarius", StatuePersistenceGameTests::addonGod);
        addonB.register("solarius", StatuePersistenceGameTests::addonGod);
        addonA.register("legacy_god", "old_shrine", StatuePersistenceGameTests::addonGod);
        duplicateGodRejected = rejected(() -> addonA.register("solarius", StatuePersistenceGameTests::addonGod));
        duplicateStatueRejected = rejected(() -> addonA.register("other", "old_shrine", StatuePersistenceGameTests::addonGod));
        addonA.register(bus);
        addonA.register(bus); // Repeated attachment must not duplicate Forge event subscribers.
        addonB.register(bus);
        // Other addon constructors may contribute gods after bus attachment, before registry events.
        addonB.register("late_declaration", StatuePersistenceGameTests::addonGod);
    }

    private static BaseGod addonGod() {
        return new BaseGod(Set.of(), Set.of(), java.awt.Color.WHITE);
    }

    private static boolean rejected(Runnable action) {
        try { action.run(); return false; }
        catch (IllegalArgumentException | IllegalStateException expected) { return true; }
    }

    private static boolean enabled() {
        return Arrays.stream(System.getProperty("forge.enabledGameTestNamespaces", "").split(","))
                .map(String::trim).anyMatch(SUITE::equals);
    }

    @SubscribeEvent
    @SuppressWarnings("deprecation")
    public static void register(RegisterGameTestsEvent event) {
        if (!enabled()) return;
        for (var method : StatuePersistenceGameTests.class.getDeclaredMethods())
            if (method.isAnnotationPresent(GameTest.class)) GameTestRegistry.register(method, Set.of("divinity_engine"));
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", batch = SUITE, timeoutTicks = 100)
    public static void allStatueFactoriesKeepTheirRegisteredTypeAfterReload(GameTestHelper h) {
        var statues = statues();
        h.assertTrue(statues.size() == 18, "Expected all 18 built-in statues");
        BlockPos pos = h.absolutePos(new BlockPos(2, 2, 2));
        for (StatueBlock statue : statues) {
            var id = ForgeRegistries.BLOCKS.getKey(statue);
            var type = ForgeRegistries.BLOCK_ENTITY_TYPES.getValue(id);
            var state = statue.defaultBlockState();
            h.assertTrue(type != null && type.isValid(state), "Missing type for " + id);
            for (BlockEntity created : List.of(statue.newBlockEntity(pos, state), type.create(pos, state))) {
                h.assertTrue(created instanceof StatueBlockEntity && created.getType() == type, "Wrong factory type for " + id);
                ((StatueBlockEntity) created).setCustomName(Component.literal("Saved " + id));
                var saved = created.saveWithFullMetadata();
                h.assertTrue(saved.getString("id").equals(id.toString()), "Changed existing save ID for " + id);
                // loadStatic calls the registered factory, not StatueBlock.newBlockEntity.
                BlockEntity restored = BlockEntity.loadStatic(pos, state, saved);
                h.assertTrue(restored instanceof StatueBlockEntity && restored.getType() == type, "Reload lost type for " + id);
                h.assertTrue(restored.saveWithFullMetadata().equals(saved), "Reload changed saved statue data for " + id);
                var buffer = new FriendlyByteBuf(Unpooled.buffer());
                try {
                    restored.getUpdatePacket().write(buffer);
                    var decoded = new ClientboundBlockEntityDataPacket(buffer);
                    h.assertTrue(decoded.getType() == type && decoded.getPos().equals(pos), "Update packet lost identity for " + id);
                    h.assertTrue(saved.equals(decoded.getTag()), "Update packet lost statue data for " + id);
                } finally { buffer.release(); }
            }
        }
        h.succeed();
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", batch = SUITE, timeoutTicks = 100)
    public static void reloadedStatuesCanBeSentInChunkPackets(GameTestHelper h) {
        BlockPos pos = h.absolutePos(new BlockPos(2, 2, 2));
        var level = h.getLevel();
        for (StatueBlock statue : statues()) {
            var id = ForgeRegistries.BLOCKS.getKey(statue);
            var type = ForgeRegistries.BLOCK_ENTITY_TYPES.getValue(id);
            var state = statue.defaultBlockState();
            level.setBlockAndUpdate(pos, state);
            var saved = level.getBlockEntity(pos).saveWithFullMetadata();
            var restored = BlockEntity.loadStatic(pos, state, saved);
            h.assertTrue(restored != null, "Statue could not load: " + id);
            level.setBlockEntity(restored);
            var chunk = level.getChunkAt(pos);
            var buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                // Reproduce the chunk-join serialization path from the reported crash.
                new ClientboundLevelChunkPacketData(chunk).write(buffer);
                var decoded = new ClientboundLevelChunkPacketData(buffer, chunk.getPos().x, chunk.getPos().z);
                boolean[] found = {false};
                decoded.getBlockEntitiesTagsConsumer(chunk.getPos().x, chunk.getPos().z).accept((location, entityType, tag) -> {
                    if (!location.equals(pos)) return;
                    found[0] = true;
                    h.assertTrue(entityType == type && saved.equals(tag), "Chunk packet changed statue identity/data for " + id);
                });
                h.assertTrue(found[0], "Chunk packet omitted statue " + id);
            } finally { buffer.release(); }
        }
        h.succeed();
    }

    private static List<StatueBlock> statues() {
        return ForgeRegistries.BLOCKS.getValues().stream()
                .filter(block -> block instanceof StatueBlock statue && statue.getGod() != null
                        && "divinity_engine".equals(ForgeRegistries.BLOCKS.getKey(block).getNamespace()))
                .map(block -> (StatueBlock) block).toList();
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", batch = SUITE, timeoutTicks = 100)
    public static void addonGodsAutomaticallyReceiveIsolatedStatues(GameTestHelper h) {
        h.assertTrue(duplicateGodRejected && duplicateStatueRejected, "Duplicate declarations must fail before partial registration");
        h.assertTrue(rejected(() -> addonA.register("too_late", StatuePersistenceGameTests::addonGod)), "Late registration must fail");
        for (var register : List.of(addonA, addonB)) {
            for (var entry : register.statues()) {
                entry.validate();
                var god = entry.god().get();
                var block = entry.block().get();
                h.assertTrue(god.getStatueBlock() == block && god.getStatueBlockEntity() == entry.blockEntity().get(), "God lost lazy statue binding");
                h.assertTrue(block.getGod() == god && block.asItem() == entry.item().get(), "Wrong god/item for addon statue");
                var entity = block.newBlockEntity(BlockPos.ZERO, block.defaultBlockState());
                var saved = entity.saveWithFullMetadata();
                h.assertTrue(entry.id().toString().equals(saved.getString("id")), "Addon save ID lost namespace");
                var restored = BlockEntity.loadStatic(BlockPos.ZERO, block.defaultBlockState(), saved);
                h.assertTrue(restored != null && restored.getType() == entry.blockEntity().get(), "Addon statue failed NBT reload");
                var drops = Block.getDrops(block.defaultBlockState(), h.getLevel(), h.absolutePos(new BlockPos(2, 2, 2)), entity);
                h.assertTrue(drops.size() == 1 && drops.get(0).is(entry.item().get()), "Generated loot must drop this statue, not another god's");
            }
        }
        var a = ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath("statue_test_a", "solarius_statue"));
        var b = ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath("statue_test_b", "solarius_statue"));
        var base = ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath("divinity_engine", "solarius_statue"));
        h.assertTrue(a != b && a != base && b != base, "Same god paths in different namespaces collided");
        h.succeed();
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", batch = SUITE, timeoutTicks = 100)
    public static void structuresKeepStatueStatesAndNames(GameTestHelper h) {
        var level = h.getLevel();
        BlockPos source = h.absolutePos(new BlockPos(2, 2, 2));
        BlockPos target = h.absolutePos(new BlockPos(5, 2, 2));
        var all = new java.util.ArrayList<>(statues());
        addonA.statues().forEach(entry -> all.add(entry.block().get()));
        addonB.statues().forEach(entry -> all.add(entry.block().get()));
        for (var statue : all) {
            for (var facing : Direction.Plane.HORIZONTAL) {
                for (boolean waterlogged : new boolean[]{false, true}) {
                    var state = statue.defaultBlockState().setValue(StatueBlock.FACING, facing).setValue(StatueBlock.WATERLOGGED, waterlogged);
                    level.setBlockAndUpdate(source, state);
                    ((StatueBlockEntity) level.getBlockEntity(source)).setCustomName(Component.literal("Existing shrine"));
                    StructureTemplate original = new StructureTemplate();
                    original.fillFromWorld(level, source, new Vec3i(1, 1, 1), false, null);
                    CompoundTag serialized = original.save(new CompoundTag());
                    StructureTemplate restored = new StructureTemplate();
                    restored.load(level.registryAccess().lookupOrThrow(Registries.BLOCK), serialized);
                    h.assertTrue(restored.placeInWorld(level, target, target,
                            new StructurePlaceSettings().setRotation(Rotation.CLOCKWISE_90).setKeepLiquids(false), level.random, 3), "Structure placement failed");
                    h.assertTrue(level.getBlockState(target).equals(state.rotate(Rotation.CLOCKWISE_90)), "Facing/waterlogged state changed in structure");
                    var entity = (StatueBlockEntity) level.getBlockEntity(target);
                    h.assertTrue(entity != null && entity.getType() == statue.getStatueBlockEntityType(), "Structure lost statue type");
                    h.assertTrue(entity.getCustomName() != null && "Existing shrine".equals(entity.getCustomName().getString()), "Structure lost custom name");
                }
            }
        }
        h.succeed();
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", batch = SUITE, timeoutTicks = 100)
    public static void generatedResourcesCoverAddonStatues(GameTestHelper h) throws Exception {
        try (var assets = new StatueResources("test", PackType.CLIENT_RESOURCES, addonA.statues())) {
            for (var entry : addonA.statues()) {
                var id = entry.id();
                for (String folder : List.of("blockstates/", "models/block/", "models/item/")) {
                    var resource = assets.getResource(PackType.CLIENT_RESOURCES,
                            ResourceLocation.fromNamespaceAndPath(id.getNamespace(), folder + id.getPath() + ".json"));
                    h.assertTrue(resource != null, "Missing automatic resource: " + folder + id);
                    try (var stream = resource.get()) {
                        h.assertTrue(com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8)).isJsonObject(), "Invalid automatic resource");
                    }
                }
            }
            h.assertTrue(assets.getNamespaces(PackType.SERVER_DATA).isEmpty(), "Client assets leaked into server data");
        }
        var existingLoot = h.getLevel().getServer().getResourceManager().getResource(
                ResourceLocation.fromNamespaceAndPath("divinity_engine", "loot_tables/blocks/solarius_statue.json")).orElseThrow();
        h.assertTrue(!existingLoot.sourcePackId().startsWith("divinity_statues/"), "Generated defaults overrode shipped statue resources");
        h.succeed();
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", batch = SUITE, timeoutTicks = 100)
    public static void existingShrineTemplatesStillResolveStatues(GameTestHelper h) throws Exception {
        var gods = List.of("solarius", "nashara", "grond", "varun", "dravak", "elaris", "voltira", "vokar",
                "nythea", "kairoth", "cryonis", "nerai", "zephra", "terra", "lumen", "mechanos", "ignar", "aethon");
        int statueReferences = 0;
        var lookup = h.getLevel().registryAccess().lookupOrThrow(Registries.BLOCK);
        for (String god : gods) {
            var blockId = ResourceLocation.fromNamespaceAndPath("divinity_engine", god + "_statue");
            h.assertTrue(ForgeRegistries.BLOCKS.containsKey(blockId) && ForgeRegistries.ITEMS.containsKey(blockId)
                    && ForgeRegistries.BLOCK_ENTITY_TYPES.containsKey(blockId), "Changed a released registry ID: " + blockId);
            var resourceId = ResourceLocation.fromNamespaceAndPath("divinity_engine", "structures/" + god + "_shrine.nbt");
            // Read packaged templates only. Never open, edit or resave a player's structures.
            var resource = h.getLevel().getServer().getResourceManager().getResource(resourceId).orElseThrow();
            CompoundTag data;
            try (var stream = resource.open()) { data = net.minecraft.nbt.NbtIo.readCompressed(stream); }
            var palette = data.getList("palette", net.minecraft.nbt.Tag.TAG_COMPOUND);
            for (int i = 0; i < palette.size(); i++) {
                var tag = palette.getCompound(i);
                if (!tag.getString("Name").endsWith("_statue")) continue;
                var state = net.minecraft.nbt.NbtUtils.readBlockState(lookup, tag);
                h.assertTrue(state.getBlock() instanceof StatueBlock, "Unresolved statue in " + resourceId + ": " + tag);
                var entity = ((StatueBlock) state.getBlock()).newBlockEntity(BlockPos.ZERO, state);
                h.assertTrue(entity.saveWithFullMetadata().getString("id").equals(tag.getString("Name")), "Wrong entity type for old palette entry");
                statueReferences++;
            }
            StructureTemplate loaded = new StructureTemplate();
            loaded.load(lookup, data);
            h.assertTrue(loaded.getSize().getX() > 0, "Empty/unreadable packaged shrine: " + resourceId);
        }
        h.assertTrue(statueReferences > 0, "No statue palette references were exercised");
        h.succeed();
    }
}
