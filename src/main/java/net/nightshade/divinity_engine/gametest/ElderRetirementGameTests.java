package net.nightshade.divinity_engine.gametest;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.MissingMappingsEvent;
import net.nightshade.divinity_engine.block.LegacyElderStatueBlock;
import net.nightshade.divinity_engine.block.StatueBlock;
import net.nightshade.divinity_engine.block.entity.LegacyElderStatueBlockEntity;
import net.nightshade.divinity_engine.commands.arg.GodsArgument;
import net.nightshade.divinity_engine.network.cap.player.gods.MainPlayerCapability;
import net.nightshade.divinity_engine.network.cap.player.gods.PlayerGodsCapabilityStorage;
import net.nightshade.divinity_engine.network.cap.player.narrative.DivineNarrativeData;
import net.nightshade.divinity_engine.registry.compat.LegacyElderContent;
import net.nightshade.divinity_engine.registry.divinity.DivineCodexPagesRegistry;
import net.nightshade.divinity_engine.registry.divinity.blessing.BlessingsRegistry;
import net.nightshade.divinity_engine.registry.divinity.curse.CurseRegistry;
import net.nightshade.divinity_engine.registry.divinity.gods.GodsRegistry;
import net.nightshade.divinity_engine.registry.divinity.trial.GodlyTrialsRegistry;
import net.nightshade.divinity_engine.registry.item.ItemsRegistry;
import net.nightshade.divinity_engine.util.divinity.narrative.DivineNarrativeRegistry;
import net.nightshade.divinity_engine.util.divinity.narrative.NarrativeProgress;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Opt-in fixtures only; no personal worlds, registry entries, or natural-generation probes. */
@Mod.EventBusSubscriber(modid = "divinity_engine", bus = Mod.EventBusSubscriber.Bus.MOD)
@PrefixGameTestTemplate(false)
public final class ElderRetirementGameTests {
    private static final String SUITE = "divinity_elder_retirement";
    private static final Set<String> ORIGINAL_GODS = Set.of("solarius", "nashara", "grond", "varun", "dravak", "elaris",
            "voltira", "vokar", "nythea", "kairoth", "cryonis", "nerai", "zephra", "terra", "lumen", "mechanos", "ignar", "aethon");

    @SubscribeEvent
    @SuppressWarnings("deprecation")
    public static void register(RegisterGameTestsEvent event) {
        if (Arrays.stream(System.getProperty("forge.enabledGameTestNamespaces", "").split(","))
                .map(String::trim).noneMatch(SUITE::equals)) return;
        for (var method : ElderRetirementGameTests.class.getDeclaredMethods())
            if (method.isAnnotationPresent(GameTest.class)) GameTestRegistry.register(method, Set.of("divinity_engine"));
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", batch = SUITE)
    public static void onlyOriginalPantheonIsActive(GameTestHelper h) throws Exception {
        var names = GodsRegistry.GODS_REGISTRY.get().getKeys().stream()
                .filter(key -> key.getNamespace().equals("divinity_engine")).map(ResourceLocation::getPath)
                .collect(java.util.stream.Collectors.toSet());
        h.assertTrue(names.equals(ORIGINAL_GODS), "Original pantheon was changed: " + names);
        h.assertTrue(builtinCount(BlessingsRegistry.BLESSINGS_REGISTRY.get()) == 90, "Expected 90 original blessings");
        h.assertTrue(builtinCount(CurseRegistry.CURSES_REGISTRY.get()) == 18, "Expected 18 original curses");
        for (String god : LegacyElderContent.GODS) {
            h.assertTrue(GodsRegistry.GODS_REGISTRY.get().getValue(id(god)) == null, "Retired god registered: " + god);
            h.assertTrue(DivineNarrativeRegistry.story(id(god)) == null && DivineNarrativeRegistry.presence(id(god)) == null,
                    "Retired narrative is discoverable: " + god);
            boolean rejected = false;
            try { new GodsArgument().parse(new StringReader(id(god).toString())); }
            catch (CommandSyntaxException expected) { rejected = true; }
            h.assertTrue(rejected, "Command accepted a retired god");
        }
        for (String blessing : LegacyElderContent.BLESSINGS)
            h.assertTrue(BlessingsRegistry.BLESSINGS_REGISTRY.get().getValue(id(blessing)) == null, "Retired blessing registered");
        for (String curse : LegacyElderContent.CURSES)
            h.assertTrue(CurseRegistry.CURSES_REGISTRY.get().getValue(id(curse)) == null, "Retired curse registered");
        for (String trial : LegacyElderContent.TRIALS)
            h.assertTrue(GodlyTrialsRegistry.get(id(trial)) == null, "Retired trial registered");
        for (String name : ORIGINAL_GODS) {
            var god = GodsRegistry.GODS_REGISTRY.get().getValue(id(name));
            h.assertTrue(god.getBlessings().size() == 5 && GodlyTrialsRegistry.get(god) != null
                    && DivineNarrativeRegistry.story(id(name)) != null, "Original kit/story/trial changed: " + name);
        }
        h.assertTrue(new GodsArgument().parse(new StringReader("divinity_engine:solarius")) == GodsRegistry.SOLARIUS.get(),
                "Original god command stopped working");
        h.succeed();
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", batch = SUITE)
    public static void legacyStatueStatesAndOpaqueDataRoundTrip(GameTestHelper h) {
        BlockPos pos = h.absolutePos(new BlockPos(2, 2, 2));
        for (String god : LegacyElderContent.GODS) for (Direction facing : Direction.Plane.HORIZONTAL) for (boolean wet : List.of(false, true)) {
            ResourceLocation id = id(god + "_statue");
            var block = ForgeRegistries.BLOCKS.getValue(id);
            h.assertTrue(block instanceof LegacyElderStatueBlock, "Saved block ID was removed: " + id);
            var statue = (LegacyElderStatueBlock) block;
            var type = ForgeRegistries.BLOCK_ENTITY_TYPES.getValue(id);
            var state = statue.defaultBlockState().setValue(StatueBlock.FACING, facing).setValue(StatueBlock.WATERLOGGED, wet);
            h.assertTrue(type != null && type.isValid(state) && statue.getGod() == null, "Invalid relic binding");
            var saved = statue.newBlockEntity(pos, state).saveWithFullMetadata();
            saved.putString("CustomName", "{\"text\":\"An old statue\"}");
            CompoundTag payload = new CompoundTag(); payload.putString("unrecognized", "keep this data");
            saved.put("LegacyPayload", payload);
            CompoundTag caps = new CompoundTag(); caps.put("unavailable_addon:memory", payload.copy()); saved.put("ForgeCaps", caps);
            saved.put("ForgeData", payload.copy());
            ListTag items = new ListTag(); items.add(new ItemStack(Items.DIAMOND, 7).save(new CompoundTag())); saved.put("Items", items);
            var restored = BlockEntity.loadStatic(pos, state, saved);
            h.assertTrue(restored instanceof LegacyElderStatueBlockEntity && restored.getType() == type, "Saved entity ID was removed");
            h.assertTrue(restored.saveWithFullMetadata().equals(saved), "Legacy payload or metadata was lost");
            restored.getPersistentData().putBoolean("updated_after_loading", true);
            saved.getCompound("ForgeData").putBoolean("updated_after_loading", true);
            h.assertTrue(restored.saveWithFullMetadata().equals(saved), "Current persistent data did not survive alongside legacy data");
            h.assertTrue(type.create(pos, state).getType() == type, "Registry factory lost its type");
            var packet = new FriendlyByteBuf(Unpooled.buffer());
            try {
                restored.getUpdatePacket().write(packet);
                var decoded = new ClientboundBlockEntityDataPacket(packet);
                h.assertTrue(decoded.getType() == type && saved.equals(decoded.getTag()), "Client update lost legacy data");
            } finally { packet.release(); }
            var stack = new ItemStack(ForgeRegistries.ITEMS.getValue(id), 3);
            stack.getOrCreateTag().put("BlockEntityTag", saved.copy());
            var reloaded = ItemStack.of(stack.save(new CompoundTag()));
            h.assertTrue(reloaded.getItem() == statue.asItem() && reloaded.getCount() == 3
                    && saved.equals(reloaded.getTag().getCompound("BlockEntityTag")), "Legacy item stack lost its identity/data");
        }
        h.succeed();
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", batch = SUITE)
    public static void relicsAreInertHiddenAndChunkPacketSafe(GameTestHelper h) {
        var level = h.getLevel(); BlockPos pos = h.absolutePos(new BlockPos(2, 2, 2));
        var previous = level.getBlockState(pos);
        var player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "retired-statue-qa"));
        Set<Item> creative = new HashSet<>(); ItemsRegistry.forEachShrineBlockItem(creative::add);
        try {
            for (String god : LegacyElderContent.GODS) {
                var statue = (LegacyElderStatueBlock) ForgeRegistries.BLOCKS.getValue(id(god + "_statue"));
                var state = statue.defaultBlockState(); level.setBlockAndUpdate(pos, state);
                var menu = player.containerMenu;
                h.assertTrue(statue.use(state, level, pos, player, InteractionHand.MAIN_HAND,
                        new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false)) == InteractionResult.PASS,
                        "Legacy statue handled worship");
                h.assertTrue(player.containerMenu == menu && statue.getMenuProvider(state, level, pos) == null,
                        "Legacy statue opened a divine menu");
                statue.tick(state, level, pos, level.random);
                h.assertFalse(level.getBlockTicks().hasScheduledTick(pos, statue), "Legacy statue rescheduled active ticks");
                h.assertFalse(statue.tryPray(level, pos, player, null, 200), "Legacy statue accepted prayer");
                h.assertFalse(creative.contains(statue.asItem()), "Legacy statue leaked into creative tab");
                h.assertTrue(ItemsRegistry.getAShrineBlockItem().getItem() != statue.asItem(), "Random shrine selected a relic");
                var chunk = level.getChunkAt(pos); var buffer = new FriendlyByteBuf(Unpooled.buffer());
                try {
                    new ClientboundLevelChunkPacketData(chunk).write(buffer);
                    var decoded = new ClientboundLevelChunkPacketData(buffer, chunk.getPos().x, chunk.getPos().z);
                    boolean[] found = {false};
                    decoded.getBlockEntitiesTagsConsumer(chunk.getPos().x, chunk.getPos().z).accept((at, type, tag) -> {
                        if (at.equals(pos)) found[0] = type == statue.getStatueBlockEntityType();
                    });
                    h.assertTrue(found[0], "Chunk packet lost the relic entity type");
                } finally { buffer.release(); }
            }
        } finally { level.setBlockAndUpdate(pos, previous); player.discard(); }
        h.succeed();
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", batch = SUITE)
    public static void retiredGodAndCurseRecordsRemainOpaque(GameTestHelper h) {
        CompoundTag input = new CompoundTag(); ListTag gods = new ListTag(), curses = new ListTag();
        for (String god : LegacyElderContent.GODS) {
            CompoundTag record = new CompoundTag(); record.putString("God", id(god).toString()); record.putInt("Favor", 237);
            record.putString("future_field", "preserve"); ListTag kit = new ListTag();
            CompoundTag blessing = new CompoundTag(); blessing.putString("Type", "divinity_engine:royal_decree"); blessing.putInt("Cooldown", 79);
            kit.add(blessing); record.put("Blessings", kit); gods.add(record);
        }
        for (String curse : LegacyElderContent.CURSES) {
            CompoundTag record = new CompoundTag(); record.putString("Blessing", id(curse).toString()); record.putLong("future_field", 42);
            curses.add(record);
        }
        var sun = GodsRegistry.SOLARIUS.get().createGodDefaultInstance(); sun.setFavor(127); gods.add(sun.toNBT());
        input.put("contacted_gods", gods); input.put("curses", curses);
        CompoundTag untouched = input.copy(); var storage = new PlayerGodsCapabilityStorage();
        storage.deserializeNBT(input); storage.deserializeNBT(input);
        h.assertTrue(storage.getContactedGods().size() == 1 && storage.getCurses().isEmpty(), "Retired instances became active or duplicated");
        h.assertTrue(storage.getContactedGod(GodsRegistry.SOLARIUS.get()).orElseThrow().getFavor() == 127, "Original favor was changed");
        var saved = storage.serializeNBT();
        for (Tag record : gods) if (LegacyElderContent.isGod(ResourceLocation.tryParse(((CompoundTag) record).getString("God"))))
            h.assertTrue(saved.getList("contacted_gods", 10).contains(record), "Retired god payload was discarded");
        for (Tag record : curses) h.assertTrue(saved.getList("curses", 10).contains(record), "Retired curse payload was discarded");
        var clone = new PlayerGodsCapabilityStorage(); clone.deserializeNBT(saved);
        h.assertTrue(clone.serializeNBT().equals(saved) && input.equals(untouched), "Reload/clone changed archived data or input");
        h.succeed();
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", batch = SUITE)
    public static void oldEquippedBlessingsAreArchivedNotActivated(GameTestHelper h) {
        for (String blessing : LegacyElderContent.BLESSINGS) {
            CompoundTag input = new CompoundTag(), slot = new CompoundTag();
            slot.putString("blessing", id(blessing).toString()); slot.putString("future_field", "exact payload");
            for (int i = 1; i <= 3; i++) input.put("blessingSlot" + i, slot.copy());
            var cap = new MainPlayerCapability.PlayerVariables();
            cap.setBlessingSlot1(BlessingsRegistry.RADIANT_STRIKE.get().createDefaultInstance());
            cap.deserializeNBT(input);
            h.assertTrue(cap.getBlessingSlot1() == null && cap.getBlessingSlot2() == null && cap.getBlessingSlot3() == null,
                    "Retired loadout retained a usable/stale slot");
            var saved = cap.serializeNBT();
            for (int i = 1; i <= 3; i++) h.assertTrue(saved.getCompound("legacyElderSlots").getCompound("blessingSlot" + i).equals(slot),
                    "Retired equipped payload was lost");
            var clone = new MainPlayerCapability.PlayerVariables(); clone.deserializeNBT(saved);
            h.assertTrue(clone.serializeNBT().equals(saved), "Equipped archive did not survive reconnect/clone");
            cap.setBlessingSlot1(BlessingsRegistry.RADIANT_STRIKE.get().createDefaultInstance());
            clone.deserializeNBT(cap.serializeNBT());
            h.assertTrue(clone.getBlessingSlot1().getBlessing() == BlessingsRegistry.RADIANT_STRIKE.get(), "Replacement original blessing was cleared");
            clone.deserializeNBT(new MainPlayerCapability.PlayerVariables().serializeNBT());
            h.assertFalse(clone.serializeNBT().contains("legacyElderSlots"), "Clean connection inherited another player's archive");
            h.assertTrue(input.getCompound("blessingSlot1").equals(slot), "Migration mutated source NBT");
        }
        h.succeed();
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", batch = SUITE)
    public static void archivedStoryMemoriesSurviveWithoutDiscovery(GameTestHelper h) {
        var data = new DivineNarrativeData();
        for (String god : LegacyElderContent.GODS) {
            var memory = data.get(id(god)); memory.storyId = id(god).toString(); memory.godId = id(god).toString();
            memory.contacts = 1; memory.contacted = true; memory.relationship.putInt("proof", 5);
            memory.story.restore(NarrativeProgress.Stage.COMPLETE, "b", 0, id(god + "_b").toString(), "", Set.of());
        }
        CompoundTag saved = data.serializeNBT(); var restored = new DivineNarrativeData(); restored.deserializeNBT(saved);
        h.assertTrue(restored.serializeNBT().equals(saved), "Retired choices/rewards/proofs were lost");
        h.assertTrue(DivineNarrativeRegistry.stories().stream().noneMatch(story -> LegacyElderContent.isGod(story.godId())),
                "Retired story remained discoverable");
        DivineCodexPagesRegistry.initializePages(); var pages = DivineCodexPagesRegistry.getPages();
        for (int i = 0; i < pages.sizeContents(); i++) {
            String text = (pages.getTitle(i).getString() + pages.getContent(i).getString()).toLowerCase(java.util.Locale.ROOT);
            h.assertFalse(text.contains("elder gods") || LegacyElderContent.GODS.stream().anyMatch(text::contains), "Codex advertises retired gameplay");
        }
        h.succeed();
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", batch = SUITE)
    public static void oldStructuresRemainLoadableButCannotGenerate(GameTestHelper h) {
        var access = h.getLevel().registryAccess(); var structures = access.registryOrThrow(Registries.STRUCTURE);
        var sets = access.registryOrThrow(Registries.STRUCTURE_SET);
        var resources = h.getLevel().getServer().getResourceManager();
        for (String god : LegacyElderContent.GODS) {
            var key = id(god + "_shrine"); var structure = structures.get(key);
            h.assertTrue(structure != null && structure.biomes().size() == 0, "Legacy structure missing or has eligible biomes");
            h.assertTrue(sets.get(key) == null && sets.stream().noneMatch(set -> set.structures().stream()
                    .anyMatch(entry -> entry.structure().value() == structure)), "Retired structure has a natural placement set");
            h.assertTrue(access.registryOrThrow(Registries.TEMPLATE_POOL).containsKey(key), "Legacy pool removed");
            h.assertTrue(h.getLevel().getStructureManager().get(key).isPresent(), "Legacy structure template removed");
            h.assertTrue(resources.getResource(id("divine_narratives/" + god + ".json")).isEmpty()
                    && resources.getResource(id("divine_narratives/" + god + "_presence.json")).isEmpty(), "Retired narrative still in datapack discovery");
        }
        h.succeed();
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", batch = SUITE)
    public static void missingMappingsIgnoreOnlyRetiredCustomIds(GameTestHelper h) {
        checkMappings(h, GodsRegistry.GODS_REGISTRY.get(), LegacyElderContent.GODS);
        checkMappings(h, BlessingsRegistry.BLESSINGS_REGISTRY.get(), LegacyElderContent.BLESSINGS);
        checkMappings(h, CurseRegistry.CURSES_REGISTRY.get(), LegacyElderContent.CURSES);
        var mapping = new ObservedMapping<>(ForgeRegistries.BLOCKS, id("aureth_statue"));
        LegacyElderContent.missingMappings(new MissingMappingsEvent(Registries.BLOCK, ForgeRegistries.BLOCKS, List.of(mapping)));
        h.assertFalse(mapping.ignored, "Block mappings must never be ignored"); h.succeed();
    }

    private static <T> void checkMappings(GameTestHelper h, IForgeRegistry<T> registry, Set<String> retired) {
        for (String path : retired) {
            var removed = new ObservedMapping<>(registry, id(path));
            var addon = new ObservedMapping<>(registry, new ResourceLocation("another_mod", path));
            var unknown = new ObservedMapping<>(registry, id("unrelated_missing_entry"));
            LegacyElderContent.missingMappings(new MissingMappingsEvent(ResourceKey.createRegistryKey(registry.getRegistryName()),
                    registry, List.of(removed, addon, unknown)));
            h.assertTrue(removed.ignored && !addon.ignored && !unknown.ignored, "Missing mapping handling exceeded retired IDs");
        }
    }

    private static final class ObservedMapping<T> extends MissingMappingsEvent.Mapping<T> {
        private boolean ignored;
        private ObservedMapping(IForgeRegistry<T> registry, ResourceLocation id) { super(registry, registry, id, 10000); }
        @Override public void ignore() { ignored = true; super.ignore(); }
    }

    private static long builtinCount(IForgeRegistry<?> registry) {
        return registry.getKeys().stream().filter(key -> key.getNamespace().equals("divinity_engine")).count();
    }
    private static ResourceLocation id(String path) { return new ResourceLocation("divinity_engine", path); }
}
