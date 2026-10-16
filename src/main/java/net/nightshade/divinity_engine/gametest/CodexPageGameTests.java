package net.nightshade.divinity_engine.gametest;

import net.minecraft.ChatFormatting;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.nightshade.divinity_engine.api.divine_codex_pages.DivineCodexPage;
import net.nightshade.divinity_engine.api.divine_codex_pages.DivineCodexPageDeferredRegister;
import net.nightshade.divinity_engine.client.codex.CodexJournal;
import net.nightshade.divinity_engine.registry.divinity.DivineCodexPagesRegistry;
import net.nightshade.divinity_engine.util.divinity.DivineCodexPages;

import java.util.Arrays;
import java.util.Set;

/** Registration/projection checks only; never opens a client or alters player progress. */
@Mod.EventBusSubscriber(modid = "divinity_engine", bus = Mod.EventBusSubscriber.Bus.MOD)
@PrefixGameTestTemplate(false)
public final class CodexPageGameTests {
    private static final String BATCH = "divinity_codex";

    @SubscribeEvent
    @SuppressWarnings("deprecation")
    public static void registerTests(RegisterGameTestsEvent event) {
        if (Arrays.stream(System.getProperty("forge.enabledGameTestNamespaces", "").split(","))
                .map(String::trim).noneMatch(BATCH::equals)) return;
        // Forge filters template namespaces; explicitly opt in this suite without unrelated tests.
        for (var method : CodexPageGameTests.class.getDeclaredMethods()) {
            if (method.isAnnotationPresent(GameTest.class)) {
                GameTestRegistry.register(method, Set.of("divinity_engine"));
            }
        }
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", batch = BATCH, timeoutTicks = 40)
    public static void longGuideRemainsOneCompleteEntry(GameTestHelper h) {
        var pages = DivineCodexPagesRegistry.getPages();
        int start = pages.sizeContents();
        int titles = pages.sizeTitles();
        try {
            Component body = Component.literal("A full guide, not another contents entry. ".repeat(150))
                    .withStyle(ChatFormatting.GOLD).append(Component.literal("THE END").withStyle(ChatFormatting.BOLD));
            pages.add(Component.literal("Long guide"), body);
            h.assertTrue(pages.sizeTitles() == titles + 1 && pages.sizeContents() == start + 1,
                    "Registration split a single guide into duplicate contents rows");
            var entry = CodexJournal.registeredPages(pages).get(start);
            h.assertTrue(entry.textComponent().equals(body), "Long guide lost text or formatting");
            h.assertTrue(entry.matches("THE END"), "Search cannot find text at the end of a long guide");
            var snapshot = CodexJournal.read(new CompoundTag(), CodexJournal.registeredPages(pages));
            h.assertTrue(snapshot.entries().get(start).text().equals(body.getString()),
                    "Full guide was lost before personal journal sync");
            h.succeed();
        } finally {
            restore(titles, start);
        }
    }

    @GameTest(templateNamespace = "divinity_engine", template = "pocket_dimension", batch = BATCH, timeoutTicks = 40)
    public static void addonPagesRemainDistinctAndOrdered(GameTestHelper h) {
        var pages = DivineCodexPagesRegistry.getPages();
        int start = pages.sizeContents();
        int titles = pages.sizeTitles();
        try {
            var addon = new DivineCodexPageDeferredRegister("codex_test");
            addon.register("first", () -> new DivineCodexPage(Component.literal("Same title"), Component.literal("First body")));
            addon.register("second", () -> new DivineCodexPage(Component.literal("Same title"), Component.literal("Second body")));
            addon.register("blank", () -> new DivineCodexPage(Component.empty(), Component.literal("Untitled body")));
            addon.registerAll();
            var entries = CodexJournal.addonGuides(pages, start);
            h.assertTrue(entries.size() == 3, "Distinct registrations were merged or duplicated");
            h.assertTrue(entries.stream().map(CodexJournal.Entry::id).distinct().count() == 3,
                    "Same-title pages have colliding navigation IDs");
            h.assertTrue(entries.get(0).text().equals("First body") && entries.get(1).text().equals("Second body"),
                    "Addon registration order changed");
            h.assertTrue(!entries.get(2).title().isBlank() && pages.getMaxPages() == start + 2,
                    "Legacy page access or untitled page navigation broke");
            h.succeed();
        } finally {
            restore(titles, start);
        }
    }

    private static void restore(int titles, int contents) {
        DivineCodexPages.titles.subList(titles, DivineCodexPages.titles.size()).clear();
        DivineCodexPages.contents.subList(contents, DivineCodexPages.contents.size()).clear();
    }
}
