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
import net.minecraft.world.level.block.entity.BlockEntityType;
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
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.nightshade.divinity_engine.block.entity.StatueBlockEntity;
import net.nightshade.divinity_engine.divinity.gods.BaseGod;
import net.nightshade.divinity_engine.divinity.gods.BaseGodInstance;
import net.nightshade.divinity_engine.divinity.gods.FavorTiers;
import net.nightshade.divinity_engine.network.events.divinity.gods.worship_events.OfferEvent;
import net.nightshade.divinity_engine.network.events.divinity.gods.worship_events.PrayEvent;
import net.nightshade.divinity_engine.network.events.divinity.gods.worship_events.WhileNearStatueEvent;
import net.nightshade.divinity_engine.util.DivinityEngineHelper;
import net.nightshade.divinity_engine.util.divinity.gods.GodHelper;
import net.nightshade.divinity_engine.world.inventory.BlessingsMenu;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static net.nightshade.divinity_engine.util.divinity.gods.GodHelper.*;

public class StatueBlock extends Block implements SimpleWaterloggedBlock, EntityBlock{
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    private static final double DETECTION_RANGE = 5.0D;
    private static final VoxelShape SHAPE = box(0, 0, 0, 16, 32, 16);
    private final @Nullable RegistryObject<? extends BaseGod> god;
    private final @Nullable Supplier<? extends BlockEntityType<StatueBlockEntity>> blockEntityType;

    /** Compatibility constructor for existing manually registered addon statues. */
    public StatueBlock(@Nullable RegistryObject<? extends BaseGod> godObject) {
        this(godObject, null);
    }

    public StatueBlock(@Nullable RegistryObject<? extends BaseGod> godObject,
                       @Nullable Supplier<? extends BlockEntityType<StatueBlockEntity>> blockEntityType) {
        super(BlockBehaviour.Properties.of().sound(SoundType.STONE).strength(1f, 10f).randomTicks().lightLevel(s -> 5).noOcclusion().isRedstoneConductor((bs, br, bp) -> false));
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(WATERLOGGED, false));
        this.god = godObject;
        this.blockEntityType = blockEntityType;
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
        return SHAPE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPE;
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
        boolean destroyed = super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
        if (destroyed && !level.isClientSide && GodHelper.hasContactedGod(player, getGod()))
            GodHelper.decreaseFavor(getGod(), player, 10);
        return destroyed;
    }
    @Override
    public void attack(BlockState blockstate, Level world, BlockPos pos, Player player) {
        super.attack(blockstate, world, pos, player);
        // Accidental clicks no longer punish faith; actual destruction carries the penalty.
    }

    @Override
    public void setPlacedBy(Level pLevel, BlockPos pPos, BlockState pState, @Nullable LivingEntity pPlacer, ItemStack pStack) {
        // Favor is awarded by the validated EntityPlaceEvent after cancellation/rollback checks.
        super.setPlacedBy(pLevel, pPos, pState, pPlacer, pStack);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        level.scheduleTick(pos, this, 10);
        BaseGod baseGod = getGod();
        if (baseGod == null) return;
        List<ServerPlayer> players = level.getEntitiesOfClass(ServerPlayer.class, new AABB(pos).inflate(DETECTION_RANGE),
                player -> player.isAlive() && !player.isSpectator());
        if (players.isEmpty()) return;

        // All entity checks, event callbacks and mutations stay on this server tick.
        for (ServerPlayer player : players) {
            if (!GodHelper.hasContactedGod(player, baseGod)) continue;
            BaseGodInstance instance = GodHelper.getGodOrNull(player, baseGod);
            MinecraftForge.EVENT_BUS.post(new WhileNearStatueEvent(instance, player));
            if (!validInteraction(level, pos, player, instance)) continue;
            net.nightshade.divinity_engine.util.divinity.narrative.DivineNarrativeManager.nearShrine(player, baseGod, pos);
            if (!player.isShiftKeyDown() || !pos.equals(net.nightshade.divinity_engine.util.divinity.narrative.DivineNarrativeManager.nearestStatue(player, null))) continue;
            var data = player.getPersistentData();
            String identity = level.dimension().location() + ":" + pos.asLong();
            boolean continued = identity.equals(data.getString("divinity_prayer_shrine"))
                    && level.getGameTime() - data.getLong("divinity_prayer_seen") == 10;
            int duration = continued ? data.getInt("statue_prayer_tick") + 10 : 10;
            data.putString("divinity_prayer_shrine", identity);
            data.putLong("divinity_prayer_seen", level.getGameTime());
            data.putInt("statue_prayer_tick", duration);
            tryPray(level, pos, player, instance, duration);
        }

        ServerPlayer closest = players.stream().filter(player -> GodHelper.hasContactedGod(player, baseGod))
                .min(java.util.Comparator.comparingDouble(player -> player.distanceToSqr(Vec3.atCenterOf(pos)))).orElse(null);
        if (closest == null) return;
        AABB frontBox = new AABB(pos.relative(state.getValue(FACING))).inflate(1, 0.5, 1);
        BaseGodInstance instance = GodHelper.getGodOrNull(closest, baseGod);
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, frontBox)) {
            if (!item.isAlive() || item.getItem().isEmpty()) continue;
            OfferEvent.OfferItemEvent offer = new OfferEvent.OfferItemEvent(instance, closest, item.getItem(), item);
            if (!MinecraftForge.EVENT_BUS.post(offer) && item.isAlive() && !item.getItem().isEmpty()
                    && frontBox.intersects(item.getBoundingBox()) && closest.isAlive()
                    && GodHelper.hasContactedGod(closest, baseGod) && instance.onOfferedItems(offer)) {
                item.discard();
                net.nightshade.divinity_engine.util.divinity.narrative.DivineNarrativeManager.onAcceptedOffering(closest, baseGod);
            }
        }
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, frontBox)) {
            if (entity instanceof Player || !entity.isAlive() || entity.getHealth() > entity.getMaxHealth() * 0.3F) continue;
            OfferEvent.OfferEntityEvent offer = new OfferEvent.OfferEntityEvent(instance, closest, entity);
            if (!MinecraftForge.EVENT_BUS.post(offer) && entity.isAlive() && frontBox.intersects(entity.getBoundingBox())
                    && entity.getHealth() <= entity.getMaxHealth() * 0.3F && closest.isAlive()
                    && GodHelper.hasContactedGod(closest, baseGod) && instance.onOfferedEntity(offer)) {
                entity.kill();
                if (!entity.isAlive()) net.nightshade.divinity_engine.util.divinity.narrative.DivineNarrativeManager.onAcceptedOffering(closest, baseGod);
            }
        }
    }

    /** Validate again after arbitrary prayer listeners, before invoking a god's favor-mutating callback. */
    public boolean tryPray(ServerLevel level, BlockPos pos, ServerPlayer player, BaseGodInstance instance, int duration) {
        if (!validInteraction(level, pos, player, instance) || !player.isShiftKeyDown()) return false;
        PrayEvent prayer = new PrayEvent(instance, player, duration);
        if (!MinecraftForge.EVENT_BUS.post(prayer) && validInteraction(level, pos, player, instance) && player.isShiftKeyDown()) {
            instance.onPrayedTo(prayer);
            if (!prayer.isCanceled() && validInteraction(level, pos, player, instance) && player.isShiftKeyDown()) {
                net.nightshade.divinity_engine.util.divinity.narrative.DivineNarrativeManager.onPrayer(player, instance.getBaseGod(), pos, duration);
                return true;
            }
        }
        net.nightshade.divinity_engine.util.divinity.narrative.DivineNarrativeManager.interruptPrayer(player, instance.getBaseGod());
        return false;
    }

    private boolean validInteraction(ServerLevel level, BlockPos pos, ServerPlayer player, BaseGodInstance instance) {
        return instance != null && player.isAlive() && !player.isRemoved() && player.serverLevel() == level
            && level.hasChunkAt(pos) && level.getBlockState(pos).getBlock() == this
            && getGod() == instance.getBaseGod() && GodHelper.getGodOrNull(player, getGod()) == instance
            && player.distanceToSqr(Vec3.atCenterOf(pos)) <= 36;
    }


    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        if (pLevel.isClientSide()) return InteractionResult.SUCCESS;
        if (getGod() == null || pHand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (hasContactedGod(pPlayer, god.get())){
            if (GodHelper.getAllBlessingsInstances(pPlayer) != null){
                if (!pLevel.isClientSide()) {
                    BlockEntity entity = pLevel.getBlockEntity(pPos);
                    if(entity instanceof StatueBlockEntity) {
                        NetworkHooks.openScreen(((ServerPlayer)pPlayer), (StatueBlockEntity)entity,
                                buffer -> BlessingsMenu.writeOpening(buffer, pPos, getGod()));
                        net.nightshade.divinity_engine.util.divinity.narrative.DivineNarrativeManager.onStatueOpened((ServerPlayer) pPlayer, getGod());
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
        BaseGod value = getGod();
        if (value != null) {
            return Component.translatable("block.divinity_engine.named_statue", Component.translatable(value.getNameTranslationKey()));
        }
        return Component.translatable("block.divinity_engine.statue");
    }

    public BaseGod getGod(){
        return this.god == null || !this.god.isPresent() ? null : this.god.get();
    }

    @Override
    public void appendHoverText(ItemStack pStack, @Nullable BlockGetter pLevel, List<Component> pTooltip, TooltipFlag pFlag) {
        BaseGod value = getGod();
        if (value != null){
            if (Screen.hasShiftDown()){
                pTooltip.add(Component.translatable(value.getNameTranslationKey()).withStyle(Style.EMPTY.withColor(value.getColor().getRGB())));
                pTooltip.add(Component.literal(" -Godly Domains: "+GodHelper.getFormattedDomains(value)).withStyle(Style.EMPTY.withColor(value.getColor().getRGB())));
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
        return getStatueBlockEntityType().create(pos, state);
    }

    public BlockEntityType<?> getStatueBlockEntityType() {
        var id = ForgeRegistries.BLOCKS.getKey(this);
        BlockEntityType<?> type = blockEntityType == null ? null : blockEntityType.get();
        // Only legacy manual registrations need the name/explicit-god fallback.
        if (type == null && id != null) type = ForgeRegistries.BLOCK_ENTITY_TYPES.getValue(id);
        if (type == null && getGod() != null) type = getGod().getStatueBlockEntity();
        if (type == null || ForgeRegistries.BLOCK_ENTITY_TYPES.getKey(type) == null || !type.isValid(defaultBlockState()))
            throw new IllegalStateException("No registered block entity type bound to statue " + id);
        return type;
    }

    @Override
    public boolean triggerEvent(BlockState state, Level world, BlockPos pos, int eventID, int eventParam) {
        super.triggerEvent(state, world, pos, eventID, eventParam);
        BlockEntity blockEntity = world.getBlockEntity(pos);
        return blockEntity == null ? false : blockEntity.triggerEvent(eventID, eventParam);
    }
}
