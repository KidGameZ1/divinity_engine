package net.nightshade.divinity_engine.util.divinity.trials;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.nightshade.divinity_engine.DivinityEngineMod;

import java.util.ArrayList;
import java.util.List;

public class GodsTrialStructureSpawner {

    public static final ResourceLocation STRUCTURE_ID = new ResourceLocation(DivinityEngineMod.MODID, "gods_trial");
    public static final int STRUCTURE_WIDTH = 26;
    public static final int STRUCTURE_HEIGHT = 13;
    public static final int STRUCTURE_LENGTH = 25;
    public static final int FIXED_Y = 80;
    private static final int ARENA_SPACING = 48;
    private static final int GRID_COLUMNS = 8;
    private static final int GRID_ROWS = 8;

    public static int getArenaSlotCount() {
        return GRID_COLUMNS * GRID_ROWS;
    }

    public static BlockPos getArenaOrigin(int slotIndex) {
        int row = slotIndex / GRID_COLUMNS;
        int column = slotIndex % GRID_COLUMNS;
        int startX = -(GRID_COLUMNS / 2) * ARENA_SPACING;
        int startZ = -(GRID_ROWS / 2) * ARENA_SPACING;
        return new BlockPos(startX + (column * ARENA_SPACING), FIXED_Y, startZ + (row * ARENA_SPACING));
    }

    public static BlockPos getArenaCenter(BlockPos origin) {
        return origin.offset(13, 2, 12);
    }

    /** Place the structure in the world */
    public static void placeStructure(ServerLevel level, BlockPos pos) {
        StructureTemplate template = level.getStructureManager().getOrCreate(STRUCTURE_ID);
        if (template == null || template.getSize().equals(BlockPos.ZERO)) {
            DivinityEngineMod.LOGGER.warn("Failed to load gods trial structure {}", STRUCTURE_ID);
            return;
        }

        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setIgnoreEntities(true)
                .setMirror(net.minecraft.world.level.block.Mirror.NONE)
                .setRotation(net.minecraft.world.level.block.Rotation.NONE)
                .addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK);

        template.placeInWorld(level, pos, pos, settings, level.getRandom(), 3);
    }

    /** Replace the stacked barrier marker inside one arena with the god statue */
    public static void replaceBarrierWithStatue(ServerLevel level, BlockPos origin, BlockState statueBlock) {
        for (int dx = 0; dx < STRUCTURE_WIDTH; dx++) {
            for (int dy = 0; dy < STRUCTURE_HEIGHT - 1; dy++) {
                for (int dz = 0; dz < STRUCTURE_LENGTH; dz++) {
                    BlockPos pos = origin.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(pos);
                    BlockState above = level.getBlockState(pos.above());

                    if (state.is(Blocks.BARRIER) && above.is(Blocks.BARRIER)) {
                        level.setBlock(pos, statueBlock, 3);
                        level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 3);
                        return;
                    }
                }
            }
        }
    }

    /** Destroy the structure completely */
    public static void destroyStructure(ServerLevel level, BlockPos origin) {
        for (int dx = 0; dx < STRUCTURE_WIDTH; dx++) {
            for (int dy = 0; dy < STRUCTURE_HEIGHT; dy++) {
                for (int dz = 0; dz < STRUCTURE_LENGTH; dz++) {
                    BlockPos pos = origin.offset(dx, dy, dz);
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
    }
}
