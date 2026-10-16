package net.nightshade.divinity_engine.registry.blocks;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.nightshade.divinity_engine.DivinityEngineMod;

public class BlocksRegistry {
	public static final DeferredRegister<Block> REGISTRY = DeferredRegister.create(ForgeRegistries.BLOCKS, DivinityEngineMod.MODID);
	// Start of user code block custom blocks
    public static final RegistryObject<Block> POCKET_DIMENSION = REGISTRY.register("pocket_dimension", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).strength(-1.0F, 3600000.0F).noLootTable().lightLevel(s -> 200).noOcclusion().isRedstoneConductor((bs, br, bp) -> false)));
    public static final RegistryObject<Block> GODS_TRIAL_BLOCK = REGISTRY.register("gods_trial_block", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).strength(-1.0F, 3600000.0F).noLootTable().lightLevel(s -> 200).noOcclusion().isRedstoneConductor((bs, br, bp) -> false)));

	// End of user code block custom blocks
	public static void init(IEventBus bus){
		REGISTRY.register(bus);
	}
}