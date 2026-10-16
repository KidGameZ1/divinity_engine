package net.nightshade.divinity_engine.util.divinity.effects;

import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Register addon themes during enqueued common setup. Keys always include the namespace. */
public final class BlessingVisualProfiles {
    public record Theme(String motif, int primary, int accent, int highlight, ResourceLocation texture) {
        public Theme {
            Objects.requireNonNull(motif);
            Objects.requireNonNull(texture);
        }
    }

    public record Style(ResourceLocation god, int variant) {
        public Style {
            Objects.requireNonNull(god);
            if (variant < 0 || variant > 4) throw new IllegalArgumentException("Blessing visual variant must be 0..4");
        }
    }

    private static final Map<ResourceLocation, Theme> THEMES = new LinkedHashMap<>();
    private static final Map<ResourceLocation, Style> STYLES = new LinkedHashMap<>();
    private static final Theme FALLBACK = new Theme("unbound", 0xB0DCE8EB, 0x90A9B9C3, 0xC0FFFFFF,
            new ResourceLocation("minecraft", "textures/particle/spark_0.png"));

    static {
        builtin("solarius", 0xD0FFD16A, 0xA0FF8D37, 0xE0FFF4CE, "spark_0",
                "radiant_strike sunskin sunburst solar_ward corona_ascendant");
        builtin("nashara", 0xC077CF4F, 0xA0327053, 0xD0D2FF83, "spell_0",
                "toxic_skin serpents_bite shadowstep venomblood coiled_retribution");
        builtin("grond", 0xC0BDA47A, 0xB0667076, 0xD0F0D4A0, "critical_hit",
                "hammerhand stoneform echoing_strike earthbind_grip forgeheart");
        builtin("varun", 0xC0C1D0A6, 0xA0518064, 0xE0FFDC8B, "critical_hit",
                "feral_step trackers_mark predators_focus savage_hunger alphas_call");
        builtin("dravak", 0xC0E95340, 0xB082292D, 0xE0FFB76B, "critical_hit",
                "battle_rush unyielding crushing_blow warlords_will fury_unbound");
        builtin("elaris", 0xC082B8FF, 0xA04B649C, 0xE0FBE7A9, "enchanted_hit",
                "insightful_kill foresight scholars_edge mind_mirror arcane_recall");
        builtin("voltira", 0xD080D9FF, 0xB03686DA, 0xF0FFF6AC, "spark_0",
                "stormstep shockshield tempest_wrath charge_relay lightning_conduit");
        builtin("vokar", 0xB060D8BF, 0x903B687B, 0xD0D0FFE4, "soul_0",
                "soul_leech wraithwalk ghastly_aura dread_consumption witherbound");
        builtin("nythea", 0xB0CBC8FF, 0xA063719E, 0xE0EEF5FF, "spell_0",
                "twilight_veil lull_of_the_lost phase_blink silver_mirror moondrinker");
        builtin("kairoth", 0xC0ECD497, 0xA04DB4AE, 0xD0FFF2C6, "critical_hit",
                "second_chance echo_step predicted_path threadsever fateful_draw");
        builtin("cryonis", 0xC098E4FF, 0xA04A9ED6, 0xE0F1FDFF, "spark_0",
                "frozen_heart icy_pulse snowveil frostbite_mantle permafrost_surge");
        builtin("nerai", 0xB055CFCB, 0xA03475AE, 0xD0D6FFEF, "bubble",
                "aquatic_grace rains_favor wavebound undercurrent_grip tidecallers_wrath");
        builtin("zephra", 0xA0C4F2E1, 0x907CAEBF, 0xE0F3FFFF, "generic_0",
                "gale_step windcallers_reflex cyclone_leap shearwind_dash tempest_spiral");
        builtin("terra", 0xB08BCC62, 0xA06E8B46, 0xD0E5DA90, "glint",
                "verdant_touch thornskin stonewardens_embrace rootgrip wildheart_bloom");
        builtin("lumen", 0xD0FFF6BF, 0xB07CD4D8, 0xE0FFFFFF, "spark_0",
                "beacons_grace cleansing_flame divine_radiance lightbearers_oath dawnshatter");
        builtin("mechanos", 0xC0D2B181, 0xA074B8C2, 0xE0FFF0CC, "spark_0",
                "tinkers_grace auto_smelter clockwork_punch precision_impact overdrive_loop");
        builtin("ignar", 0xD0FF9D39, 0xB0CC3C26, 0xE0FFE5A2, "flame",
                "blazing_hands fireproof flash_flare kindling_fury inferno_mantle");
        builtin("aethon", 0xC080B6FF, 0xA09E89CB, 0xE0F0F5FF, "spark_0",
                "veilpiercer celestial_ward starlit_mirror gravity_fold pocket_dimension");
    }

    private BlessingVisualProfiles() { }

    private static void builtin(String god, int primary, int accent, int highlight, String texture, String blessings) {
        ResourceLocation id = new ResourceLocation("divinity_engine", god);
        registerTheme(id, new Theme(god, primary, accent, highlight,
                new ResourceLocation("minecraft", "textures/particle/" + texture + ".png")));
        String[] names = blessings.split(" ");
        for (int i = 0; i < names.length; i++) registerStyle(new ResourceLocation("divinity_engine", names[i]), new Style(id, i));
    }

    public static synchronized void registerTheme(ResourceLocation god, Theme theme) {
        if (THEMES.putIfAbsent(Objects.requireNonNull(god), Objects.requireNonNull(theme)) != null)
            throw new IllegalArgumentException("Duplicate blessing visual god: " + god);
    }

    public static synchronized void registerStyle(ResourceLocation blessing, Style style) {
        if (STYLES.putIfAbsent(Objects.requireNonNull(blessing), Objects.requireNonNull(style)) != null)
            throw new IllegalArgumentException("Duplicate blessing visual: " + blessing);
    }

    public static Theme theme(ResourceLocation god) { return THEMES.getOrDefault(god, FALLBACK); }
    public static Style style(ResourceLocation blessing) { return STYLES.get(blessing); }
    public static Map<ResourceLocation, Theme> themes() { return Map.copyOf(THEMES); }
    public static Map<ResourceLocation, Style> styles() { return Map.copyOf(STYLES); }
}
