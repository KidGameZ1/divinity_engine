package net.nightshade.divinity_engine.block;

import io.netty.buffer.Unpooled;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.RegistryObject;
import net.nightshade.divinity_engine.divinity.gods.BaseGod;
import net.nightshade.divinity_engine.util.divinity.gods.GodHelper;
import net.nightshade.divinity_engine.world.inventory.BlessingsMenu;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

import static net.nightshade.divinity_engine.util.divinity.gods.GodHelper.*;

public class ShrineBlock extends Block{
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    private final RegistryObject<BaseGod> god;
    public ShrineBlock(RegistryObject<BaseGod> godObject) {
        super(BlockBehaviour.Properties.of().sound(SoundType.STONE).strength(1f, 10f).lightLevel(s -> 5).requiresCorrectToolForDrops().noOcclusion().isRedstoneConductor((bs, br, bp) -> false));
        this.god = godObject;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos) {
        return true;
    }

    @Override
    public int getLightBlock(BlockState state, BlockGetter worldIn, BlockPos pos) {
        return 0;
    }

    @Override
    public VoxelShape getVisualShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            default -> box(0, 0, 0, 16, 32, 16);
            case NORTH -> box(0, 0, 0, 16, 32, 16);
            case EAST -> box(0, 0, 0, 16, 32, 16);
            case WEST -> box(0, 0, 0, 16, 32, 16);
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return super.getStateForPlacement(context).setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    public BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    public BlockState mirror(BlockState state, Mirror mirrorIn) {
        return state.rotate(mirrorIn.getRotation(state.getValue(FACING)));
    }


    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {


        if (hasContactedGod(pPlayer, god.get())){
            if (GodHelper.getAllBlessingsInstances(pPlayer) != null){
                if (pPlayer instanceof ServerPlayer _ent) {
                    BlockPos _bpos = BlockPos.containing(pPlayer.getX(), pPlayer.getY(), pPlayer.getZ());
                    NetworkHooks.openScreen((ServerPlayer) _ent, new MenuProvider() {
                        @Override
                        public Component getDisplayName() {
                            return Component.literal("Blessings");
                        }

                        @Override
                        public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
                            return new BlessingsMenu(id, inventory, new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(_bpos));
                        }
                    }, _bpos);
                }
                GodHelper.getAllBlessingsInstances(pPlayer).forEach(blessing -> System.out.println(blessing.getBlessingId()));
            }
        }else {
            contactGod(god.get(), pPlayer);
        }




        return super.use(pState, pLevel, pPos, pPlayer, pHand, pHit);
    }

    @Override
    public MutableComponent getName() {
        return Component.translatable("block.divinity_engine.shrine");
    }

    @Override
    public String getDescriptionId() {
        return Component.translatable("block.divinity_engine.shrine").getString();
    }

    @Override
    public void appendHoverText(ItemStack pStack, @Nullable BlockGetter pLevel, List<Component> pTooltip, TooltipFlag pFlag) {
        if (god.get() != null){
            if (Screen.hasShiftDown()){
                pTooltip.add(Component.translatable(god.get().getNameTranslationKey()).withStyle(Style.EMPTY.withColor(god.get().getColor().getRGB())));
            }else {
                pTooltip.add(Component.translatable("divinity_engine.tooltip.shift").withStyle(ChatFormatting.DARK_PURPLE));
            }
        }

        super.appendHoverText(pStack, pLevel, pTooltip, pFlag);
    }
}