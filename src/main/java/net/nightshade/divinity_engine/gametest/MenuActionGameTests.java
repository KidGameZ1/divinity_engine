package net.nightshade.divinity_engine.gametest;

import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;
import net.nightshade.divinity_engine.network.cap.player.gods.PlayerGodsCapability;
import net.nightshade.divinity_engine.network.cap.player.narrative.DivineNarrativeCapability;
import net.nightshade.divinity_engine.network.cap.player.narrative.DivineNarrativeData;
import net.nightshade.divinity_engine.network.messages.gui.PocketExitMessage;
import net.nightshade.divinity_engine.network.messages.narrative.QuestMenuAction;
import net.nightshade.divinity_engine.util.divinity.gods.DivineRelationship;
import net.nightshade.divinity_engine.util.divinity.narrative.*;
import net.nightshade.divinity_engine.world.inventory.BlessingsMenu;
import java.util.*;

@Mod.EventBusSubscriber(modid = "divinity_engine", bus = Mod.EventBusSubscriber.Bus.MOD)
@PrefixGameTestTemplate(false)
public final class MenuActionGameTests {
    private static final ResourceLocation SOLARIUS = DivineNarrativeRegistry.id("solarius");
    @SubscribeEvent public static void register(RegisterGameTestsEvent event) {
        if (Arrays.asList(System.getProperty("forge.enabledGameTestNamespaces", "").split(",")).contains("divinity_menu_actions"))
            for (var method : MenuActionGameTests.class.getDeclaredMethods())
                if (method.isAnnotationPresent(GameTest.class)) GameTestRegistry.register(method, Set.of("divinity_engine"));
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension")
    public static void onlyAdministrativeCommandsRemain(GameTestHelper h) {
        var root = h.getLevel().getServer().getCommands().getDispatcher().getRoot().getChild("divinity_engine");
        h.assertTrue(root != null && root.getChild("pocket") == null, "Player pocket command still registered");
        var source = h.getLevel().getServer().createCommandSourceStack().withPermission(0);
        var story = root.getChild("story"); var commune = root.getChild("commune");
        h.assertTrue(!story.canUse(source) && !commune.canUse(source), "Admin tools visible to regular players");
        h.assertTrue(story.getChildren().size() == 1 && story.getChild("reset") != null, "Player story actions remain");
        h.assertTrue(commune.getChildren().size() == 2 && commune.getChild("status") != null && commune.getChild("end") != null,
                "Player communion actions remain");
        h.assertTrue(root.getChild("favor") != null && root.getChild("contact") != null, "Original admin tools removed");
        h.succeed();
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension")
    public static void menuStartRequiresContactNotFavorPoints(GameTestHelper h) {
        var player = player(h);
        try {
            var god = DivineNarrativeManager.god(SOLARIUS);
            var instance = PlayerGodsCapability.load(player).getContactedGod(god).orElseThrow();
            var def = DivineNarrativeRegistry.story(SOLARIUS);
            h.assertTrue(instance.getFavor() == 0, "New-player fixture unexpectedly has numeric favor");
            PlayerGodsCapability.load(player).getContactedGods().remove(instance);
            h.assertFalse(new QuestMenuAction(1, def.id(), "request").apply(player), "Uncontacted player can request a story");
            PlayerGodsCapability.load(player).updateGodInstance(instance, false);
            DivineRelationship.onContact(player, god);
            h.assertTrue(new QuestMenuAction(1, def.id(), "request").apply(player), "Contacted zero-favor player cannot start story");
            h.assertTrue(player.narrative.story(def).story.stage() == NarrativeProgress.Stage.OFFERING,
                    "Story request must begin at accepted-offering chapter");
            h.assertTrue(player.narrative.get(SOLARIUS).relationship.getInt("proof") == 0, "Request alone granted an introductory blessing");
        } finally { player.discard(); }
        h.succeed();
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension")
    public static void packetsRoundTripWithoutCommands(GameTestHelper h) {
        var message = new QuestMenuAction(7, SOLARIUS, "confirm", "b", UUID.randomUUID().toString());
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try { QuestMenuAction.encode(message, buffer); h.assertTrue(message.equals(QuestMenuAction.decode(buffer)), "Menu request changed on wire"); }
        finally { buffer.release(); }
        h.succeed();
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension")
    public static void menuRejectsClosedForeignRemoteAndAdminActions(GameTestHelper h) {
        var player = player(h);
        try {
            var menu = player.containerMenu;
            var story = DivineNarrativeRegistry.story(SOLARIUS).id();
            h.assertFalse(new QuestMenuAction(2, story, "request").apply(player), "Wrong container accepted");
            h.assertFalse(new QuestMenuAction(1, DivineNarrativeRegistry.story(DivineNarrativeRegistry.id("nythea")).id(), "request").apply(player), "Foreign god accepted");
            h.assertFalse(new QuestMenuAction(1, story, "reset").apply(player), "Admin action accepted from packet");
            player.containerMenu = player.inventoryMenu;
            h.assertFalse(new QuestMenuAction(1, story, "request").apply(player), "Closed menu accepted");
            player.containerMenu = new BlessingsMenu(1, player.getInventory(), null);
            h.assertFalse(new QuestMenuAction(1, story, "request").apply(player), "Inventory menu performs shrine actions");
            player.containerMenu = menu;
            var origin = player.position(); player.setPos(origin.add(12, 0, 0));
            h.assertFalse(new QuestMenuAction(1, story, "request").apply(player), "Remote shrine accepted");
            player.setPos(origin);
            h.assertTrue(new QuestMenuAction(1, story, "request").apply(player), "Valid menu request refused");
            h.assertTrue(player.containerMenu == menu, "Quest action closed the shrine menu");
            h.assertFalse(new QuestMenuAction(1, story, "status").apply(player), "Rapid request bypassed limiter");
            h.assertFalse(PocketExitMessage.apply(player), "Pocket exit works outside pocket dimension");
        } finally { player.discard(); }
        h.succeed();
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", timeoutTicks = 80)
    public static void bothPathPreviewsRequireFreshPermanentConfirmation(GameTestHelper h) {
        var player = player(h); var def = DivineNarrativeRegistry.story(SOLARIUS);
        var memory = player.narrative.story(def); memory.snapshot(def);
        memory.story.restore(NarrativeProgress.Stage.CHOICE, "", 0, "", "", Set.of());
        String[] token = {""};
        h.startSequence().thenExecute(() -> {
            h.assertFalse(new QuestMenuAction(1, def.id(), "confirm", "a", "untrusted").apply(player), "Unpreviewed choice accepted");
            var view = ShrineStoryDialogue.build(player, def.id());
            for (String branch : List.of("a", "b")) h.assertTrue(view.getString("text").contains(def.branches().get(branch).reward().description()), "Reward missing from preview");
        }).thenIdle(5).thenExecute(() -> {
            h.assertTrue(new QuestMenuAction(1, def.id(), "preview", "b", "").apply(player), "Preview refused");
            var actions = ShrineStoryDialogue.build(player, def.id()).getList("actions", 10);
            h.assertTrue(actions.size() == 3 && actions.getCompound(2).getString("action").equals("confirm"), "Explicit confirmation missing");
            token[0] = actions.getCompound(2).getString("token");
            h.assertTrue(memory.story.branch().isEmpty(), "Preview silently chose a path");
        }).thenIdle(5).thenExecute(() -> {
            h.assertFalse(new QuestMenuAction(1, def.id(), "confirm", "a", token[0]).apply(player), "Token used for wrong path");
        }).thenIdle(5).thenExecute(() -> {
            h.assertTrue(new QuestMenuAction(1, def.id(), "confirm", "b", token[0]).apply(player), "Valid confirmation refused");
            h.assertTrue(memory.story.branch().equals("b") && memory.story.stage() == NarrativeProgress.Stage.DEED, "Branch did not lock");
        }).thenIdle(5).thenExecute(() -> {
            h.assertFalse(new QuestMenuAction(1, def.id(), "confirm", "b", token[0]).apply(player), "Confirmation replay accepted");
            player.discard();
        }).thenSucceed();
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension")
    public static void expiredConfirmationAndRemovedShrineCannotChangeChoice(GameTestHelper h) {
        var player = player(h);
        try {
            var def = DivineNarrativeRegistry.story(SOLARIUS); var memory = player.narrative.story(def); memory.snapshot(def);
            memory.story.restore(NarrativeProgress.Stage.CHOICE, "", 0, "", "", Set.of());
            memory.preview = UUID.randomUUID() + ":a"; memory.previewExpires = DivineNarrativeManager.now(player) - 1;
            memory.previewDefinition = def.toString();
            h.assertTrue(ShrineStoryDialogue.build(player, def.id()).getList("actions", 10).size() == 2, "Expired confirm button shown");
            h.assertFalse(new QuestMenuAction(1, def.id(), "confirm", "a", memory.preview.substring(0, 36)).apply(player), "Expired token accepted");
            h.getLevel().removeBlock(((BlessingsMenu) player.containerMenu).getOpeningPos(), false);
            h.assertFalse(new QuestMenuAction(1, def.id(), "preview", "b", "").apply(player), "Removed shrine accepted");
            h.assertTrue(memory.story.branch().isEmpty(), "Rejected action changed branch");
        } finally { player.discard(); }
        h.succeed();
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", timeoutTicks = 80)
    public static void menuDonationAndEndingRemainExactlyOnce(GameTestHelper h) {
        var player = player(h); var def = DivineNarrativeRegistry.story(SOLARIUS);
        var memory = player.narrative.story(def); memory.snapshot(def);
        memory.story.restore(NarrativeProgress.Stage.DEED, "b", 0, "", "", Set.of());
        memory.branchSnapshot = def.branches().get("b");
        player.getInventory().setItem(0, new ItemStack(Items.BREAD, 20));
        h.startSequence().thenExecute(() -> {
            h.assertTrue(new QuestMenuAction(1, def.id(), "donate").apply(player), "Donation refused");
            h.assertTrue(player.getInventory().getItem(0).getCount() == 4, "Donation consumed wrong quantity");
        }).thenIdle(5).thenExecute(() -> {
            h.assertFalse(new QuestMenuAction(1, def.id(), "donate").apply(player), "Duplicate donation accepted");
            h.assertTrue(player.getInventory().getItem(0).getCount() == 4, "Duplicate consumed items");
            memory.story.restore(NarrativeProgress.Stage.RETURN, "b", 0, "", "", Set.of());
        }).thenIdle(5).thenExecute(() -> {
            h.assertTrue(new QuestMenuAction(1, def.id(), "finish").apply(player), "Ending refused");
            h.assertTrue(DivineNarrativeManager.hasSpecialization(player, def.branches().get("b").reward().id()), "Upgrade not earned");
            h.assertTrue(memory.story.stage() == NarrativeProgress.Stage.COMPLETE
                    && memory.story.specialization().equals(def.branches().get("b").reward().id().toString()), "Permanent chosen upgrade was not recorded");
            h.assertTrue(player.narrative.get(SOLARIUS).relationship.getInt("proof") == 5, "Finale did not preserve final trial proof");
            h.assertTrue(PlayerGodsCapability.load(player).getContactedGod(DivineNarrativeManager.god(SOLARIUS)).orElseThrow().getFavor() == 0,
                    "Deed and finale must not add numeric favor");
        }).thenIdle(5).thenExecute(() -> {
            h.assertFalse(new QuestMenuAction(1, def.id(), "finish").apply(player), "Duplicate finale accepted");
            h.assertTrue(PlayerGodsCapability.load(player).getContactedGod(DivineNarrativeManager.god(SOLARIUS)).orElseThrow().getFavor() == 0, "Duplicate favor granted");
            h.assertTrue(memory.story.specialization().equals(def.branches().get("b").reward().id().toString()), "Duplicate finale changed earned reward");
            player.discard();
        }).thenSucceed();
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", timeoutTicks = 80)
    public static void epilogueIsAvailableFromEitherRelatedStatueOnly(GameTestHelper h) {
        var player = player(h);
        var relation = DivineNarrativeRegistry.relationships().stream().filter(r -> r.gods().contains(SOLARIUS)).findFirst().orElseThrow();
        for (var id : relation.stories()) {
            var def = DivineNarrativeRegistry.byId(id); var memory = player.narrative.story(def); memory.snapshot(def);
            memory.story.restore(NarrativeProgress.Stage.COMPLETE, "a", 0, def.branches().get("a").reward().id().toString(), "", Set.of());
        }
        var story = DivineNarrativeRegistry.story(SOLARIUS).id();
        h.startSequence().thenExecute(() -> {
            var actions = ShrineStoryDialogue.build(player, story).getList("actions", 10);
            h.assertTrue(actions.size() == 1 && actions.getCompound(0).getString("action").equals("crossover"), "Epilogue button missing");
            var foreign = DivineNarrativeRegistry.relationships().stream().filter(r -> !r.gods().contains(SOLARIUS)).findFirst().orElseThrow();
            h.assertFalse(new QuestMenuAction(1, story, "crossover", foreign.id().toString(), "").apply(player), "Foreign epilogue accepted");
            h.assertTrue(new QuestMenuAction(1, story, "crossover", relation.id().toString(), "").apply(player), "Matching epilogue refused");
        }).thenIdle(5).thenExecute(() -> {
            h.assertFalse(new QuestMenuAction(1, story, "crossover", relation.id().toString(), "").apply(player), "Repeated epilogue accepted");
            h.assertTrue(player.narrative.epilogues.size() == 1, "Epilogue recorded more than once");
            h.assertTrue(ShrineStoryDialogue.build(player, story).getList("actions", 10).isEmpty(), "Earned epilogue still actionable");
            player.discard();
        }).thenSucceed();
    }

    private static TestPlayer player(GameTestHelper h) {
        DivineNarrativeRegistry.initialize();
        TestPlayer player = new TestPlayer(h.getLevel());
        var god = DivineNarrativeManager.god(SOLARIUS); var instance = god.createGodDefaultInstance(); instance.setFavor(0);
        PlayerGodsCapability.load(player).updateGodInstance(instance, false);
        DivineRelationship.onContact(player, god);
        var pos = h.absolutePos(new BlockPos(2, 2, 2));
        h.getLevel().setBlock(pos, ForgeRegistries.BLOCKS.getValue(DivineNarrativeRegistry.id("solarius_statue")).defaultBlockState(), 3);
        player.setPos(pos.getX() + 1.5, pos.getY(), pos.getZ() + .5);
        player.containerMenu = BlessingsMenu.fromStatue(1, player.getInventory(), pos);
        return player;
    }

    private static final class TestPlayer extends FakePlayer {
        final DivineNarrativeData narrative = new DivineNarrativeData();
        private final LazyOptional<DivineNarrativeData> cap = LazyOptional.of(() -> narrative);
        TestPlayer(ServerLevel level) { super(level, new GameProfile(UUID.randomUUID(), "menu-test")); }
        @Override public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction side) {
            return capability == DivineNarrativeCapability.CAP && cap != null ? cap.cast() : super.getCapability(capability, side);
        }
        @Override public void sendSystemMessage(Component message) {}
    }
}
