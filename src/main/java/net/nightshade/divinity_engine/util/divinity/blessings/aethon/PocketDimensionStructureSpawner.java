package net.nightshade.divinity_engine.util.divinity.blessings.aethon;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.nightshade.divinity_engine.DivinityEngineMod;

public class PocketDimensionStructureSpawner {

    private static final ResourceLocation STRUCTURE_ID = new ResourceLocation(DivinityEngineMod.MODID, "pocket_dimension");
    private static final int STRUCTURE_WIDTH = 29;
    private static final int STRUCTURE_HEIGHT = 17;
    private static final int STRUCTURE_LENGTH = 31;
    private static final int MIN_DISTANCE_BETWEEN_STRUCTURES = 64;
    private static final int FIXED_Y = 80; // height to build structures

    public static void trySpawnStructure(ServerLevel level) {
        if (!level.dimension().location().getPath().equals("pocket_dimension")) return;

        RandomSource random = level.getRandom();

        for (int attempts = 0; attempts < 10; attempts++) {
            int x = random.nextIntBetweenInclusive(-512, 512);
            int z = random.nextIntBetweenInclusive(-512, 512);
            BlockPos pos = new BlockPos(x, FIXED_Y, z);

            if (!isAreaOccupied(level, pos)) {
                placeStructure(level, pos);
                return;
            }
        }
    }

    public static boolean isAreaOccupied(ServerLevel level, BlockPos origin) {
        BlockPos.MutableBlockPos checkPos = new BlockPos.MutableBlockPos();

        for (int dx = -MIN_DISTANCE_BETWEEN_STRUCTURES / 2; dx <= STRUCTURE_WIDTH + MIN_DISTANCE_BETWEEN_STRUCTURES / 2; dx++) {
            for (int dz = -MIN_DISTANCE_BETWEEN_STRUCTURES / 2; dz <= STRUCTURE_LENGTH + MIN_DISTANCE_BETWEEN_STRUCTURES / 2; dz++) {
                for (int dy = 0; dy < STRUCTURE_HEIGHT; dy++) {
                    checkPos.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    if (!level.isEmptyBlock(checkPos)) {
                        return true; // space is occupied
                    }
                }
            }
        }

        return false; // area is free
    }

    public static void placeStructure(ServerLevel level, BlockPos pos) {
        StructureTemplate template = level.getStructureManager().getOrCreate(STRUCTURE_ID);
        if (template == null || template.getSize().equals(BlockPos.ZERO)) {
            System.out.println("Failed to load structure or it has no size: " + STRUCTURE_ID);
            return;
        }

        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setIgnoreEntities(true)
                .setMirror(Mirror.NONE)
                .setRotation(Rotation.NONE)
                .addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK);

        template.placeInWorld(level, pos, pos, settings, level.getRandom(), 3);
        System.out.println("Placed Pocket Structure at " + pos);
//        x=198, y=64, z=-358
    }
}
