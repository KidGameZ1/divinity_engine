package net.nightshade.divinity_engine.gametest;

import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;
import net.nightshade.divinity_engine.divinity.gods.BaseGod;
import net.nightshade.divinity_engine.network.cap.player.gods.MainPlayerCapability;
import net.nightshade.divinity_engine.network.cap.player.gods.PlayerGodsCapability;
import net.nightshade.divinity_engine.network.messages.gui.EquipBlessingMessage;
import net.nightshade.divinity_engine.network.messages.gui.PlayerBlessingsButtonMessage;
import net.nightshade.divinity_engine.registry.divinity.gods.GodsRegistry;
import net.nightshade.divinity_engine.util.DivinityEngineHelper;
import net.nightshade.divinity_engine.util.divinity.gods.GodHelper;
import net.nightshade.divinity_engine.util.divinity.narrative.DivineNarrativeManager;
import net.nightshade.divinity_engine.block.StatueBlock;
import net.nightshade.divinity_engine.world.inventory.BlessingsMenu;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = "divinity_engine", bus = Mod.EventBusSubscriber.Bus.MOD)
@PrefixGameTestTemplate(false)
public final class ShrineBlessingsGameTests {
    private static final String SUITE = "divinity_shrine_ui";

    @SubscribeEvent
    @SuppressWarnings("deprecation")
    public static void register(RegisterGameTestsEvent event) {
        if (Arrays.stream(System.getProperty("forge.enabledGameTestNamespaces", "").split(","))
                .map(String::trim).noneMatch(SUITE::equals)) return;
        for (var method : ShrineBlessingsGameTests.class.getDeclaredMethods()) {
            if (method.isAnnotationPresent(GameTest.class)) GameTestRegistry.register(method, Set.of("divinity_engine"));
        }
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", batch = SUITE, timeoutTicks = 40)
    public static void statueRestrictsBothEquipAndUnequip(GameTestHelper h) {
        TestPlayer p = player(h);
        try {
            var menu = shrine(h, p);
            h.assertTrue(menu.getVisibleGods().equals(List.of(GodsRegistry.SOLARIUS.get())), "Another god is visible in a shrine menu");
            var own = GodHelper.getAllBlessingsInstancesOfGod(p, GodsRegistry.SOLARIUS.get()).get(0);
            var foreign = GodHelper.getAllBlessingsInstancesOfGod(p, GodsRegistry.NYTHEA.get()).get(0);
            var ownRequest = new EquipBlessingMessage(17, own.getBoundGod().getRegistryName(), own.getBlessing().getRegistryName(), 0, true);
            h.assertTrue(ownRequest.apply(p), "The shrine rejected its own eligible blessing");
            h.assertFalse(new EquipBlessingMessage(17, foreign.getBoundGod().getRegistryName(), foreign.getBlessing().getRegistryName(), 1, true).apply(p),
                    "A forged request equipped another god's blessing");
            DivinityEngineHelper.setBlessingSlot(p, 1, foreign);
            h.assertFalse(new EquipBlessingMessage(17, foreign.getBoundGod().getRegistryName(), foreign.getBlessing().getRegistryName(), 1, false).apply(p),
                    "A forged request removed another god's blessing");
            h.assertTrue(DivinityEngineHelper.getBlessingInSlot(p, 1) == foreign, "Rejected request changed the loadout");
            PlayerBlessingsButtonMessage.handleButtonAction(p, 1, menu.x, menu.y, menu.z);
            h.assertTrue(p.containerMenu == menu, "Inventory-open packet bypassed the shrine lock");
            h.succeed();
        } finally { p.discard(); }
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", batch = SUITE, timeoutTicks = 40)
    public static void staleStatueMenusCannotChangeLoadout(GameTestHelper h) {
        TestPlayer p = player(h);
        try {
            var menu = shrine(h, p);
            var own = GodHelper.getAllBlessingsInstancesOfGod(p, menu.getGod()).get(0);
            var request = new EquipBlessingMessage(17, menu.getGod().getRegistryName(), own.getBlessing().getRegistryName(), 0, true);
            p.setPos(menu.x + 20, menu.y, menu.z);
            h.assertFalse(menu.stillValid(p) || request.apply(p), "A distant shrine remained usable");
            p.setPos(menu.x + .5, menu.y + 1, menu.z + .5);
            h.getLevel().setBlockAndUpdate(menu.getOpeningPos(), statue(GodsRegistry.NYTHEA.get()).defaultBlockState());
            h.assertFalse(menu.stillValid(p) || request.apply(p), "Replacing the statue changed the menu's identity");
            h.assertTrue(menu.getGod() == GodsRegistry.SOLARIUS.get(), "Bound god changed after block replacement");
            h.succeed();
        } finally { p.discard(); }
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", batch = SUITE, timeoutTicks = 40)
    public static void openingDataPreservesIdentityWithoutClientBlock(GameTestHelper h) {
        TestPlayer p = player(h);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            var source = shrine(h, p);
            BlessingsMenu.writeOpening(buffer, source.getOpeningPos(), source.getGod());
            h.getLevel().setBlockAndUpdate(source.getOpeningPos(), Blocks.AIR.defaultBlockState());
            var decoded = new BlessingsMenu(18, p.getInventory(), buffer);
            h.assertTrue(decoded.isOpenedFromStatue() && decoded.getGod() == source.getGod(), "Menu depends on the client's statue block arriving first");
            h.assertFalse(decoded.allowsGod(GodsRegistry.NYTHEA.getId()), "Missing block fell back to unrestricted browsing");
            h.assertTrue(decoded.getVisibleGods().size() == 1, "Opening sync exposed the rest of the pantheon");
            h.succeed();
        } finally { buffer.release(); p.discard(); }
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", batch = SUITE, timeoutTicks = 40)
    public static void inventoryMenuStillBrowsesAllContactedGods(GameTestHelper h) {
        TestPlayer p = player(h);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            var bound = shrine(h, p);
            BlessingsMenu.writeOpening(buffer, bound.getOpeningPos(), null);
            var menu = new BlessingsMenu(19, p.getInventory(), buffer);
            p.containerMenu = menu;
            h.assertFalse(menu.isOpenedFromStatue(), "Inventory menu was inferred from the block at the player's feet");
            h.assertTrue(menu.getVisibleGods().size() == 2, "Inventory browsing lost contacted gods");
            var foreign = GodHelper.getAllBlessingsInstancesOfGod(p, GodsRegistry.NYTHEA.get()).get(0);
            h.assertTrue(new EquipBlessingMessage(19, foreign.getBoundGod().getRegistryName(), foreign.getBlessing().getRegistryName(), 1, true).apply(p),
                    "Normal inventory loadout editing stopped working");
            h.succeed();
        } finally { buffer.release(); p.discard(); }
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", batch = SUITE, timeoutTicks = 40)
    public static void indexedShrineLookupMatchesTerrainScan(GameTestHelper h) {
        TestPlayer p = player(h);
        try {
            BlockPos center = h.absolutePos(new BlockPos(3, 3, 3));
            p.setPos(center.getX() + .5, center.getY() + .5, center.getZ() + .5);
            List<BlockPos> positions = List.of(center.east(2), center.west(2), center.south(4), center.above(7));
            for (BlockPos pos : positions) h.getLevel().setBlockAndUpdate(pos, statue(GodsRegistry.SOLARIUS.get()).defaultBlockState());
            h.getLevel().setBlockAndUpdate(center.north(), statue(GodsRegistry.NYTHEA.get()).defaultBlockState());
            for (int change = 0; change < 3; change++) {
                for (ResourceLocation god : new ResourceLocation[]{null, GodsRegistry.SOLARIUS.getId(), GodsRegistry.NYTHEA.getId()}) {
                    BlockPos expected = null; double distance = 36.0001;
                    for (BlockPos pos : BlockPos.betweenClosed(p.blockPosition().offset(-6, -6, -6), p.blockPosition().offset(6, 6, 6))) {
                        double d = p.distanceToSqr(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5);
                        if (d > distance || !h.getLevel().hasChunkAt(pos)) continue;
                        if (h.getLevel().getBlockState(pos).getBlock() instanceof StatueBlock block && block.getGod() != null
                                && (god == null || god.equals(block.getGod().getRegistryName()))) {
                            expected = pos.immutable(); distance = d;
                        }
                    }
                    h.assertTrue(java.util.Objects.equals(expected, DivineNarrativeManager.nearestStatue(p, god)),
                            "Indexed lookup changed nearest god, six-block range, tie ordering or removal handling");
                }
                h.getLevel().setBlockAndUpdate(positions.get(change), Blocks.AIR.defaultBlockState());
            }
            p.setPos(center.getX() + 1000000.5, center.getY() + .5, center.getZ() + 1000000.5);
            var chunks = h.getLevel().getChunkSource();
            int chunkX = p.blockPosition().getX() >> 4, chunkZ = p.blockPosition().getZ() >> 4;
            h.assertTrue(chunks.getChunkNow(chunkX, chunkZ) == null, "Expected an unloaded test destination");
            h.assertTrue(DivineNarrativeManager.nearestStatue(p, GodsRegistry.SOLARIUS.getId()) == null,
                    "Moving away reused a stale shrine");
            h.assertTrue(chunks.getChunkNow(chunkX, chunkZ) == null, "Shrine search loaded a chunk");
            h.succeed();
        } finally { p.discard(); }
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", batch = SUITE, timeoutTicks = 40)
    public static void godLookupsUseRegisteredIdentity(GameTestHelper h) {
        TestPlayer p = player(h);
        try {
            var storage = PlayerGodsCapability.load(p);
            for (var god : List.of(GodsRegistry.SOLARIUS.get(), GodsRegistry.NYTHEA.get())) {
                h.assertTrue(storage.getContactedGod(god).orElseThrow().getBaseGod() == god, "Wrong god lookup");
            }
            h.assertTrue(storage.getContactedGod(GodsRegistry.IGNAR.get()).isEmpty(), "Uncontacted god was returned");
            h.assertTrue(storage.getContactedGod(null).isEmpty() && storage.getCurse(null).isEmpty(), "Null lookup must be empty");
            var curse = GodsRegistry.SOLARIUS.get().getCurse();
            h.assertTrue(storage.getCurse(curse).isEmpty(), "Unapplied curse was returned");
            var instance = curse.createDefaultInstance();
            storage.updateCurseInstance(instance, false);
            h.assertTrue(storage.getCurse(curse).orElseThrow() == instance, "Curse lookup did not use its registered ID");
            h.succeed();
        } finally { p.discard(); }
    }

    private static BlessingsMenu shrine(GameTestHelper h, TestPlayer p) {
        BlockPos pos = h.absolutePos(new BlockPos(2, 2, 2));
        h.getLevel().setBlockAndUpdate(pos, statue(GodsRegistry.SOLARIUS.get()).defaultBlockState());
        p.setPos(pos.getX() + .5, pos.getY() + 1, pos.getZ() + .5);
        var menu = BlessingsMenu.fromStatue(17, p.getInventory(), pos);
        p.containerMenu = menu;
        return menu;
    }

    private static net.minecraft.world.level.block.Block statue(BaseGod god) {
        return ForgeRegistries.BLOCKS.getValue(new ResourceLocation(god.getRegistryName().getNamespace(), god.getRegistryName().getPath() + "_statue"));
    }

    private static TestPlayer player(GameTestHelper h) {
        TestPlayer p = new TestPlayer(h.getLevel());
        for (var god : List.of(GodsRegistry.SOLARIUS.get(), GodsRegistry.NYTHEA.get())) {
            var instance = god.createGodDefaultInstance();
            instance.setFavor(250);
            PlayerGodsCapability.load(p).updateGodInstance(instance, false);
        }
        return p;
    }

    private static final class TestPlayer extends FakePlayer {
        private final MainPlayerCapability.PlayerVariables variables = new MainPlayerCapability.PlayerVariables();
        private final LazyOptional<MainPlayerCapability.PlayerVariables> cap = LazyOptional.of(() -> variables);
        private TestPlayer(ServerLevel level) { super(level, new GameProfile(UUID.randomUUID(), "ShrineUiTest")); }
        @Override public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction side) {
            if (capability == MainPlayerCapability.PLAYER_VARIABLES_CAPABILITY && cap != null) return cap.cast();
            return super.getCapability(capability, side);
        }
    }
}
