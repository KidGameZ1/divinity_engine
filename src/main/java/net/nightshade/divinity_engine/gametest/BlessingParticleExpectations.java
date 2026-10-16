package net.nightshade.divinity_engine.gametest;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals.Cue;

import java.util.List;

/** Independent material-color expectations shared by effects tests and the packaged gallery. */
public final class BlessingParticleExpectations {
    private static final List<Integer> NATIVE = List.of(0xFFFFFFFF);
    private static final List<Integer> SOIL = List.of(0xFF806247, 0xFFB99A70);
    private static final List<Integer> STONE = List.of(0xFF888780, 0xFFB2ACA0);
    private static final List<Integer> ASH = List.of(0xFF77736E, 0xFFAAA49A);

    private BlessingParticleExpectations() { }

    public static List<Integer> palette(ResourceLocation blessing, Cue cue, List<Integer> godPalette) {
        if (!blessing.getNamespace().equals("divinity_engine")) return godPalette;
        return switch (blessing.getPath()) {
            case "sunskin", "moondrinker" -> NATIVE;
            case "celestial_ward" -> cue == Cue.HEAL ? NATIVE : godPalette;
            case "radiant_strike" -> cue == Cue.AURA ? NATIVE : godPalette;
            case "sunburst" -> cue == Cue.CONTROL ? godPalette : NATIVE;
            case "corona_ascendant" -> cue == Cue.IMPACT ? godPalette : NATIVE;
            case "threadsever" -> List.of(0xFF000000 | MobEffects.WEAKNESS.getColor());
            case "snowveil", "kindling_fury", "blazing_hands", "inferno_mantle",
                    "trackers_mark", "predators_focus", "soul_leech", "ghastly_aura", "dread_consumption",
                    "verdant_touch", "thornskin", "wildheart_bloom", "auto_smelter" -> NATIVE;
            case "dawnshatter" -> cue == Cue.CAST ? godPalette : NATIVE;
            case "flash_flare" -> cue == Cue.CONTROL ? godPalette : NATIVE;
            case "forgeheart" -> cue == Cue.IMPACT ? godPalette : NATIVE;
            case "savage_hunger" -> cue == Cue.HEAL ? NATIVE : SOIL;
            case "echoing_strike", "earthbind_grip", "feral_step", "alphas_call", "rootgrip" -> SOIL;
            case "battle_rush" -> cue == Cue.WARD ? STONE : SOIL;
            case "stoneform", "unyielding", "stonewardens_embrace" -> STONE;
            case "fireproof", "warlords_will", "wraithwalk", "witherbound", "precision_impact" -> ASH;
            case "overdrive_loop" -> cue == Cue.TRAIL ? SOIL : ASH;
            default -> godPalette;
        };
    }
}
