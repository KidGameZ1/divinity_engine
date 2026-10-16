package net.nightshade.divinity_engine.util.divinity.innerworld;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.nightshade.divinity_engine.api.innerworld.InnerWorldDefinition;
import net.nightshade.divinity_engine.util.divinity.narrative.DivineNarrativeManager;

import java.util.List;

/** Empty communion spaces; the session's manifestation renders the god's original statue. */
public final class BuiltinInnerWorlds {
    private record Realm(String god, String name, String sound) {}

    private static final int SIDE = 13;
    private static final List<Realm> REALMS = List.of(
        new Realm("solarius", "The Eternal Court", "block.beacon.ambient"),
        new Realm("nashara", "The Serpent's Garden", "block.azalea_leaves.step"),
        new Realm("grond", "The First Mountain", "block.anvil.land"),
        new Realm("varun", "The Endless Wilds", "entity.wolf.ambient"),
        new Realm("dravak", "The Crimson Battlefield", "block.chain.hit"),
        new Realm("elaris", "The Infinite Archive", "block.enchantment_table.use"),
        new Realm("voltira", "The Eye of the Storm", "weather.rain"),
        new Realm("vokar", "Beyond the Veil", "ambient.soul_sand_valley.mood"),
        new Realm("nythea", "The Endless Dream", "block.amethyst_block.chime"),
        new Realm("kairoth", "The Crossroads of Time", "block.note_block.hat"),
        new Realm("cryonis", "The Silent Expanse", "block.amethyst_block.resonate"),
        new Realm("nerai", "The Endless Sea", "ambient.underwater.loop"),
        new Realm("zephra", "The Boundless Sky", "block.wool.step"),
        new Realm("terra", "The World Tree", "block.grass.step"),
        new Realm("lumen", "The White Sanctuary", "block.beacon.ambient"),
        new Realm("mechanos", "The Divine Machine", "block.piston.contract"),
        new Realm("ignar", "The Heart of Flame", "block.lava.ambient"),
        new Realm("aethon", "The Celestial Expanse", "block.amethyst_block.chime")
    );

    public static void register() {
        for (Realm realm : REALMS) {
            var god = new ResourceLocation("divinity_engine", realm.god);
            var value = DivineNarrativeManager.god(god);
            var visuals = new InnerWorldDefinition.Visuals(value.getColor().getRGB(),
                new ResourceLocation("minecraft", realm.sound),
                new ResourceLocation("minecraft", "end_rod"), "void");
            InnerWorldsRegistry.register(InnerWorldDefinition.builder(
                    new ResourceLocation("divinity_engine", realm.god + "_inner_world"), god)
                .name(realm.name).revision("2-void").bounds(SIDE, 8, SIDE)
                .spawn(new Vec3(6.5, 1, 3.5)).manifestation(new Vec3(6.5, 1, 9.5))
                .scene(BuiltinInnerWorlds::voidSupport).visuals(visuals).story(god).build());
        }
    }

    private static BlockState voidSupport(BlockPos pos) {
        // Invisible support keeps survival visitors safe without adding visible scenery.
        boolean edge = pos.getX() == 0 || pos.getZ() == 0
            || pos.getX() == SIDE - 1 || pos.getZ() == SIDE - 1;
        return (pos.getY() == 0 || edge && pos.getY() < 4
            ? Blocks.BARRIER : Blocks.AIR).defaultBlockState();
    }

    private BuiltinInnerWorlds() {}
}
