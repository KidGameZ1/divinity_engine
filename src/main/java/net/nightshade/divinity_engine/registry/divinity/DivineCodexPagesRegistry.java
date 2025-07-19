package net.nightshade.divinity_engine.registry.divinity;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.nightshade.divinity_engine.api.divine_codex_pages.IDivineCodexAPI;
import net.nightshade.divinity_engine.util.divinity.DivineCodexPages;
import net.nightshade.divinity_engine.util.divinity.gods.GodHelper;

import java.util.ServiceLoader;

public class DivineCodexPagesRegistry {
    private static boolean initialized = false;
    private static final DivineCodexPages DIVINE_CODEX_PAGES = new DivineCodexPages();

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
                Component.literal("The Divine Codex is your guide to communing with the divine. Within these pages lie the secrets of blessings, curses, and the Favor that binds mortals to gods. To begin your journey, you must seek out a god’s shrine—an ancient monument through which their will echoes. From there, you will perform acts in their name to gain Favor. Walk the path with intention, for your deeds will shape how the divine sees you.")
        );

        DivineCodexPagesRegistry.addPage(
                Component.literal("Earning Favor"),
                Component.literal("Favor is the measure of your standing with a god. Every action taken in the presence of a shrine may increase or decrease your favor depending on how the god views it.\n" +
                        "\n" +
                        "Favor Tiers:\n" +
                        "\n" +
                        "•Champion (176 to 250): A chosen mortal, entrusted with divine power."+
                        "\n"+
                        "\n"+
                        "•Devoted (101 to 175): A faithful servant worthy of unique blessings."+
                        "\n"+
                        "\n"+
                        "•Favorable (1 to 100): A welcomed follower in the god’s eyes."+
                        "\n"+
                        "\n"+
                        "•Neutral (0): Unknown or untested."+
                        "\n"+
                        "\n"+
                        "•Displeased (-100 to -1): The god is wary of you."+
                        "\n"+
                        "\n"+
                        "•Enemy (-250 to -101): You are seen as a threat."+
                        "\n"+
                        "Beware.\n" +
                        "\n"+
                        "Only actions witnessed near a shrine affect Favor.")
        );

        DivineCodexPagesRegistry.addPage(
                Component.literal("Blessings of the Divine"),
                Component.literal("As your favor grows, gods will bestow upon you their blessings. These may enhance your body, alter the world around you, or grant miraculous abilities.\n" +
                        "\n" +
                        "You may equip up to five blessings at once. Some provide passive effects, while others require activation. Visit a shrine to manage your Divine Loadout.\n" +
                        "\n" +
                        "Curses:  \n" +
                        "Gods do not only grant power—they can also take it away. If your favor falls low enough, a curse may be inflicted. These curses are unique to each god and may activate at any time, regardless of distance.\n" +
                        "\n" +
                        "Your choices have consequences. Do not take the divine lightly.\n")
        );

        DivineCodexPagesRegistry.addPage(
                Component.literal("Shrines and Contact"),
                Component.literal("Shrines are sacred structures where the presence of a god can be felt. They are hidden or scattered throughout the world and its realms.\n" +
                        "\n" +
                        "To begin worship, interact with a shrine using the Shrine Block. Once contact is made, you are bound to that god until you willingly change allegiance.\n" +
                        "\n" +
                        "Only one god may be actively worshiped at a time. Switching your devotion resets your favor with the previous god—choose wisely.\n" +
                        "\n" +
                        "The shrine is also where blessings are granted, curses observed, and rituals performed. Return often to show your faith.")
        );

        // Fix here
        MutableComponent godsComponent = Component.literal("Here are the gods currently known to mortals:\n\n");

        for (int i = 0; i < GodHelper.getGods().size(); i++) {
            Component godName = Component.translatable(GodHelper.getGods().get(i).getNameTranslationKey());
            String domains = GodHelper.getFormattedDomains(GodHelper.getGods().get(i));

            godsComponent.append(Component.literal("-")
                    .append(godName)
                    .append(Component.literal(" — Godly Domains: " + domains + "\n\n")));

            System.out.println("Registered " + godName.getString() + " added to the Divine Codex Book");
        }

        DivineCodexPagesRegistry.addPage(
                Component.literal("The Known Pantheon"),
                godsComponent
        );

        DivineCodexPagesRegistry.addPage(
                Component.literal("Using Your Powers"),
                Component.literal("Divine power requires both faith and control. Passive blessings require no effort, but active blessings must be triggered.\n" +
                        "\n" +
                        "Use the R key (default) to activate your selected blessing. Some blessings have cooldowns or energy costs—time your use wisely.\n" +
                        "\n" +
                        "To manage your equipped blessings, open the Blessing Menu. Here, you may equip, unequip, or switch between saved blessing loadouts. The menu can be opened by right cilcking a Shrine Block or using a dedicated button in the inventory.\n" +
                        "\n" +
                        "Learn your blessings well. Mastery of their timing and synergy is the mark of a true divine champion.")
        );

        // Discover addons using the service loader
        ServiceLoader.load(IDivineCodexAPI.class).forEach(addon -> {
            addon.registerPages(DivineCodexPagesRegistry::addPage);
        });
    }
}
