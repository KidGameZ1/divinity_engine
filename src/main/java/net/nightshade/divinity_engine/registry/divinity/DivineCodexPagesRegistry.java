package net.nightshade.divinity_engine.registry.divinity;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.nightshade.divinity_engine.DivinityEngineMod;
import net.nightshade.divinity_engine.api.divine_codex_pages.DivineCodexPageDeferredRegister;
import net.nightshade.divinity_engine.util.divinity.DivineCodexPages;
import net.nightshade.divinity_engine.util.divinity.gods.GodHelper;

import java.util.ArrayList;
import java.util.List;

public class DivineCodexPagesRegistry {
    private static boolean initialized = false;
    private static final DivineCodexPages DIVINE_CODEX_PAGES = new DivineCodexPages();

    // List of all deferred registers that need to be initialized
    private static final List<DivineCodexPageDeferredRegister> DEFERRED_REGISTERS = new ArrayList<>();
    private static int addonStartPage;
    public static int getAddonStartPage() { return addonStartPage; }
    public static void addDeferredRegister(DivineCodexPageDeferredRegister register) {
        if (initialized) throw new IllegalStateException("Submit Codex pages before client initialization");
        if (!DEFERRED_REGISTERS.contains(register)) DEFERRED_REGISTERS.add(register);
    }

    public static boolean isRegistered(String domainId) {
        return DIVINE_CODEX_PAGES.contains(domainId.toLowerCase());
    }

    public static DivineCodexPages getPages() {
        return DIVINE_CODEX_PAGES;
    }

    public static void addPage(Component title, Component content) {
        DIVINE_CODEX_PAGES.add(title,  content);
    }

    public static void initializePages() {
        if (initialized) return;
        initialized = true;

        DivineCodexPagesRegistry.addPage(
                Component.literal("Welcome, Seeker of Power"),
                Component.literal("The Divine Codex is your guide to communing with the divine. Seek a god's statue and right-click it to make contact. Reopen it, choose Quests, and begin their story. Your deeds and choices build a relationship; there is no favor-point grind for built-in gods.\n\nThe Bond tab explains what you have proven, what blessings you can use, and what to do next. This book preserves registered guides alongside your personal journal, discovered chapters and earned endings.")
        );

        DivineCodexPagesRegistry.addPage(
                Component.literal("Earning Divine Trust"),
                Component.literal("A divine relationship is built from proven deeds, not accumulated points. Each built-in god's five blessings follow these milestones:\n\n"
                        + "Accepted offering after beginning The Call.\nCompleted A Witness.\nCompleted The Chosen Deed on either branch.\nCompleted The Reckoning.\nA fresh victory after reaching The Trial.\n\n"
                        + "Your Bond records the relationship's rank, memories, next action and exact blessing requirements. Prayer and accepted offerings can fulfill a chapter or invite a response, but cannot buy your way past a required deed.\n\n"
                        + "Both story paths can unlock the whole kit. Abandonment pauses progress; reconnect at the statue to resume. Positive blessing unlocks earned in older saves and permanent story upgrades are preserved. Legacy addons without stories may keep their existing favor API for compatibility.")
        );

        DivineCodexPagesRegistry.addPage(
                Component.literal("Blessings of the Divine"),
                Component.literal("As you prove yourself through a god's story, their blessings become available. These may enhance your body, alter the world around you, or grant miraculous abilities.\n" +
                        "\n" +
                        "You may equip up to three blessings at once, including blessings from different gods. Some provide passive effects, while others require activation. Use a shrine or your inventory's blessings button to manage these slots. A shrine menu shows only that statue's god.\n" +
                        "\n" +
                        "Curses:  \n" +
                        "Gods do not only grant power. A damaged relationship can bring a curse, unique to the offended god. Read its effects and the Bond record rather than assuming every god judges alike.\n" +
                        "\n" +
                        "Your choices have consequences. Do not take the divine lightly.\n")
        );

        DivineCodexPagesRegistry.addPage(
                Component.literal("Shrines and Contact"),
                Component.literal("Shrines are sacred structures where the presence of a god can be felt. They are hidden or scattered throughout the world and its realms.\n" +
                        "\n" +
                        "To begin, right-click a god's statue. Contact reveals their story guide; no favor threshold is needed to ask a built-in god for their quest. Stay within six blocks of the matching statue for story actions.\n" +
                        "\n" +
                        "You can maintain contact with multiple gods. Their story progress is independent. Abandonment pauses that relationship but does not erase its permanent story choice or earned specialization.\n" +
                        "\n" +
                        "After starting the story, drop an accepted offering in front of the statue. Sneak nearby to pray. A requested story donation is different: use Quests > Contribute requested items; it consumes only the requested items. Return to the shrine for path confirmation, trial entry and the ending.")
        );

        var gods = GodHelper.getGods();
        long blessingCount = gods.stream().flatMap(god -> god.getBlessings().stream())
                .map(blessing -> blessing.getId()).distinct().count();
        MutableComponent godsComponent = Component.literal("The registered pantheon: " + gods.size()
                + " gods and " + blessingCount + " blessings. Addon gods are included automatically.\n\n");

        for (var god : gods.stream().sorted(java.util.Comparator.comparing(value -> value.getRegistryName().toString())).toList()) {
            Component godName = Component.translatable(god.getNameTranslationKey());
            String domains = GodHelper.getFormattedDomains(god);

            godsComponent.append(Component.literal("-")
                    .append(godName)
                    .append(Component.literal(" - "))
                    .append(Component.translatableWithFallback(god.getNameTranslationKey() + ".domains", domains))
                    .append("\n"));
            for (var blessing : god.getBlessings().stream().sorted(java.util.Comparator.comparing(value -> value.getId().toString())).toList())
                godsComponent.append("  ").append(Component.translatable(blessing.get().getNameTranslationKey())).append("\n");
            godsComponent.append("\n");
        }

        DivineCodexPagesRegistry.addPage(
                Component.literal("The Known Pantheon"),
                godsComponent
        );

        DivineCodexPagesRegistry.addPage(
                Component.literal("Using Your Powers"),
                Component.literal("Divine power requires both faith and control. Passive blessings require no effort, but active blessings must be triggered.\n" +
                        "\n" +
                        "Use the R, G, V key (default) to activate your selected blessing. Some blessings have cooldowns or energy costs—time your use wisely.\n" +
                        "\n" +
                        "To manage your three equipped blessings, open the Blessing Menu by right-clicking a contacted god's statue or using the blessings button in your inventory. Select an unlocked blessing, then choose a slot. Open Bond to see the deed needed for a locked blessing.\n" +
                        "\n" +
                        "Learn your blessings well. Mastery of their timing and synergy is the mark of a true divine champion.")
        );

        // The reader paginates these full guides after the client font and layout are available.
        DivineCodexPagesRegistry.addPage(Component.literal("The Living Pantheon"), Component.literal(
                "After contact, reopen a god's statue and choose Quests > Begin quest to hear their request. " +
                "Make an accepted offering, complete A Witness, then use Speak about quest to preview both rewards and confirm one permanent path in the conversation screen. " +
                "Finish The Chosen Deed and The Reckoning, win a NEW trial for that god, and return to the statue for an ending, shrine recognition and a permanent blessing specialization. Every completed proof unlocks the next blessing; the specialization is claimed separately at The Return.\n\n" +
                "Specializations work only with the original blessing equipped and eligible. Leaving a god pauses the story but never erases your choice. " +
                "Continuous challenges restart if interrupted. Trial failures allow retries.\n\n" +
                "Use the Quests tab for guidance and the Codex for discovered lore. Story contributions use Contribute requested items at the statue, not dropped offerings. " +
                "Only an administrator can reset a confirmed branch. No ascension keys or permanent HUD are added."));

        // Register all slots from other mods
        addonStartPage = DIVINE_CODEX_PAGES.sizeContents();
        for (DivineCodexPageDeferredRegister register : DEFERRED_REGISTERS) {
            register.registerAll();
        }

        DivinityEngineMod.LOGGER.info("Divine Codex Pages Registered");
    }
}
