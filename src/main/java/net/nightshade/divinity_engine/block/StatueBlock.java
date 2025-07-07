package net.nightshade.divinity_engine.block;

import io.netty.buffer.Unpooled;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.RegistryObject;
import net.nightshade.divinity_engine.block.entity.StatueBlockEntity;
import net.nightshade.divinity_engine.divinity.gods.BaseGod;
import net.nightshade.divinity_engine.divinity.gods.BaseGodInstance;
import net.nightshade.divinity_engine.divinity.gods.FavorTiers;
import net.nightshade.divinity_engine.network.events.divinity.gods.worship_events.OfferEvent;
import net.nightshade.divinity_engine.network.events.divinity.gods.worship_events.PrayEvent;
import net.nightshade.divinity_engine.network.events.divinity.gods.worship_events.WhileNearStatueEvent;
import net.nightshade.divinity_engine.util.divinity.gods.GodHelper;
import net.nightshade.divinity_engine.world.inventory.BlessingsMenu;
import net.nightshade.nightshade_core.util.MiscHelper;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static net.nightshade.divinity_engine.util.divinity.gods.GodHelper.*;

public class StatueBlock extends Block implements SimpleWaterloggedBlock, EntityBlock{
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    private static final double DETECTION_RANGE = 5.0D;
    private final @Nullable RegistryObject<BaseGod> god;
    public StatueBlock(@Nullable RegistryObject<BaseGod> godObject) {
        super(BlockBehaviour.Properties.of().sound(SoundType.STONE).strength(1f, 10f).randomTicks().lightLevel(s -> 5).noOcclusion().isRedstoneConductor((bs, br, bp) -> false));
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(WATERLOGGED, false));
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
        return switch (state.getValue(FACING)) {
            default -> box(0, 0, 0, 16, 32, 16);
            case NORTH -> box(0, 0, 0, 16, 32, 16);
            case EAST -> box(0, 0, 0, 16, 32, 16);
            case WEST -> box(0, 0, 0, 16, 32, 16);
        };
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
        builder.add(FACING, WATERLOGGED);
    }

    @Override
    public void onPlace(BlockState blockstate, Level world, BlockPos pos, BlockState oldState, boolean moving) {
        super.onPlace(blockstate, world, pos, oldState, moving);
        world.scheduleTick(pos, this, 10);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        boolean flag = context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER;
        return super.getStateForPlacement(context).setValue(FACING, context.getHorizontalDirection().getOpposite()).setValue(WATERLOGGED, flag);
    }


    public BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    public BlockState mirror(BlockState state, Mirror mirrorIn) {
        return state.rotate(mirrorIn.getRotation(state.getValue(FACING)));
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor world, BlockPos currentPos, BlockPos facingPos) {
        if (state.getValue(WATERLOGGED)) {
            world.scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
        }
        return super.updateShape(state, facing, facingState, world, currentPos, facingPos);
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {
        if (GodHelper.hasContactedGod(player, this.god.get())){
            if (FavorTiers.getByFavor(GodHelper.getGodOrNull(player, this.god.get()).getFavor()) != FavorTiers.Champion || FavorTiers.getByFavor(GodHelper.getGodOrNull(player, this.god.get()).getFavor()) != FavorTiers.Devoted){
                GodHelper.decreaseFavor(this.god.get(), player, 20);
            } else if (FavorTiers.getByFavor(GodHelper.getGodOrNull(player, this.god.get()).getFavor()) == FavorTiers.Displeased || FavorTiers.getByFavor(GodHelper.getGodOrNull(player, this.god.get()).getFavor()) == FavorTiers.Enemy) {
                GodHelper.decreaseFavor(this.god.get(), player, 30);
            }
        }


        return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
    }
    @Override
    public void attack(BlockState blockstate, Level world, BlockPos pos, Player player) {
        super.attack(blockstate, world, pos, player);
        if (GodHelper.hasContactedGod(player, this.god.get())){
            if (FavorTiers.getByFavor(GodHelper.getGodOrNull(player, this.god.get()).getFavor()) != FavorTiers.Champion || FavorTiers.getByFavor(GodHelper.getGodOrNull(player, this.god.get()).getFavor()) != FavorTiers.Devoted){
                GodHelper.decreaseFavor(this.god.get(), player, 5);
            } else if (FavorTiers.getByFavor(GodHelper.getGodOrNull(player, this.god.get()).getFavor()) == FavorTiers.Displeased || FavorTiers.getByFavor(GodHelper.getGodOrNull(player, this.god.get()).getFavor()) == FavorTiers.Enemy) {
                GodHelper.decreaseFavor(this.god.get(), player, 8);
            }
        }
    }

    @Override
    public void tick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
        super.tick(pState, pLevel, pPos, pRandom);
        int x = pPos.getX();
        int y = pPos.getY();
        int z = pPos.getZ();
        if (this.god.get() == null) {
            return;
        }

        pLevel.scheduleTick(pPos, this, 10);
        BlockPos frontPos = pPos.relative(pState.getValue(FACING));
        List<ItemEntity> itemEntities = pLevel.getEntitiesOfClass(ItemEntity.class,
                new AABB(frontPos).inflate(1.0D, 0.5D, 1.0D));

        List<ItemStack> items = itemEntities.stream()
                .map(ItemEntity::getItem)
                .toList();

        List<Player> nearbyPlayers = pLevel.getEntitiesOfClass(Player.class,
                new AABB(pPos).inflate(5.0D));

        List<LivingEntity> nearbyEntities = pLevel.getEntitiesOfClass(LivingEntity.class,
                new AABB(frontPos).inflate(1.0D, 0.5D, 1.0D));

        if ((!items.isEmpty() || !nearbyEntities.isEmpty()) && !nearbyPlayers.isEmpty()) {
            Player closestPlayer = nearbyPlayers.get(0);
            double closestDistance = closestPlayer.distanceToSqr(pPos.getX() + 0.5D, pPos.getY() + 0.5D, pPos.getZ() + 0.5D);
            int tick = 0;

            for (Player player : nearbyPlayers) {
                double distance = player.distanceToSqr(pPos.getX() + 0.5D, pPos.getY() + 0.5D, pPos.getZ() + 0.5D);
                if (distance < closestDistance) {
                    closestPlayer = player;
                    closestDistance = distance;
                }
                if (GodHelper.hasContactedGod(player, this.god.get())) {
                    BaseGodInstance baseGodInstance = GodHelper.getGodOrNull(player, this.god.get());
                    MinecraftForge.EVENT_BUS.post(new WhileNearStatueEvent(baseGodInstance, player));
                }

            }

            if (closestPlayer.isShiftKeyDown()) {
                if (GodHelper.hasContactedGod(closestPlayer, this.god.get())) {
                    closestPlayer.getPersistentData().putInt("statue_prayer_tick", closestPlayer.getPersistentData().getInt("statue_prayer_tick") + 15);
                    tick = closestPlayer.getPersistentData().getInt("statue_prayer_tick");
                    BaseGodInstance baseGodInstance = GodHelper.getGodOrNull(closestPlayer, this.god.get());
                    MinecraftForge.EVENT_BUS.post(new PrayEvent(baseGodInstance, closestPlayer, tick));
                    System.out.println(closestPlayer.getDisplayName().getString()+" is praying to "+Component.translatable(this.god.get().getNameTranslationKey()).getString()+" for "+ MiscHelper.tickToSeconds(tick) +"s");
                }
            }else {
                closestPlayer.getPersistentData().remove("statue_prayer_tick");
            }

            // Items found in front of statue
            for (ItemEntity itemEntity : itemEntities) {
                ItemStack item = itemEntity.getItem();
                if (GodHelper.hasContactedGod(closestPlayer, this.god.get())) {
                    BaseGodInstance baseGodInstance = GodHelper.getGodOrNull(closestPlayer, this.god.get());
                    if (!MinecraftForge.EVENT_BUS.post(new OfferEvent.OfferItemEvent(baseGodInstance, closestPlayer, item, itemEntity))) {
                        System.out.println(item.getHoverName().getString() + " was offered to " + Component.translatable(this.god.get().getNameTranslationKey()).getString()+" by "+closestPlayer.getName().getString());
                    }
                }
            }

            // Entities found in front of statue
            for (LivingEntity entity : nearbyEntities) {
                if (!(entity instanceof Player)) {
                    if (GodHelper.hasContactedGod(closestPlayer, this.god.get())) {
                        BaseGodInstance baseGodInstance = GodHelper.getGodOrNull(closestPlayer, this.god.get());
                        if (!MinecraftForge.EVENT_BUS.post(new OfferEvent.OfferEntityEvent(baseGodInstance, closestPlayer, entity))) {
                            System.out.println(entity.getName().getString() + " was offered to " + Component.translatable(this.god.get().getNameTranslationKey()).getString()+" by "+closestPlayer.getName().getString());
                        }
                    }
                }
            }
        }
    }

    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {


        if (hasContactedGod(pPlayer, god.get())){
            if (GodHelper.getAllBlessingsInstances(pPlayer) != null){
                if (!pLevel.isClientSide()) {
                    BlockEntity entity = pLevel.getBlockEntity(pPos);
                    if(entity instanceof StatueBlockEntity) {
                        NetworkHooks.openScreen(((ServerPlayer)pPlayer), (StatueBlockEntity)entity, pPos);
                    } else {
                        throw new IllegalStateException("Our Container provider is missing!");
                    }
                }
            }
        }else {
            if (god.get() != null){
                if (!hasContactedGod(pPlayer, god.get())) contactGod(god.get(), pPlayer);
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public MutableComponent getName() {
        if (god.get() != null && god != null){
            return Component.literal(Component.translatable(god.get().getNameTranslationKey()).getString() + " " + Component.translatable("block.divinity_engine.statue").getString());
        }
        return Component.translatable("block.divinity_engine.statue");
    }

    @Override
    public String getDescriptionId() {
        if (god.get() != null && god != null){
            return Component.literal(Component.translatable(god.get().getNameTranslationKey()).getString()+ " " + Component.translatable("block.divinity_engine.statue").getString()).getString();
        }
        return Component.translatable("block.divinity_engine.statue").getString();
    }

    public BaseGod getGod(){
        return this.god.get();
    }

    @Override
    public void appendHoverText(ItemStack pStack, @Nullable BlockGetter pLevel, List<Component> pTooltip, TooltipFlag pFlag) {
        if (god.get() != null){
            if (Screen.hasShiftDown()){
                pTooltip.add(Component.translatable(god.get().getNameTranslationKey()).withStyle(Style.EMPTY.withColor(god.get().getColor().getRGB())));
                pTooltip.add(Component.literal(" -Godly Domains: "+GodHelper.getFormattedDomains(god.get())).withStyle(Style.EMPTY.withColor(god.get().getColor().getRGB())));
            }else {
                pTooltip.add(Component.translatable("divinity_engine.tooltip.shift").withStyle(ChatFormatting.DARK_PURPLE));
            }
        }

        super.appendHoverText(pStack, pLevel, pTooltip, pFlag);
    }

    @Override
    public MenuProvider getMenuProvider(BlockState state, Level worldIn, BlockPos pos) {
        BlockEntity tileEntity = worldIn.getBlockEntity(pos);
        return tileEntity instanceof MenuProvider menuProvider ? menuProvider : null;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StatueBlockEntity(pos, state, this.god.get().getStatueBlockEntity());
    }

    @Override
    public boolean triggerEvent(BlockState state, Level world, BlockPos pos, int eventID, int eventParam) {
        super.triggerEvent(state, world, pos, eventID, eventParam);
        BlockEntity blockEntity = world.getBlockEntity(pos);
        return blockEntity == null ? false : blockEntity.triggerEvent(eventID, eventParam);
    }
}