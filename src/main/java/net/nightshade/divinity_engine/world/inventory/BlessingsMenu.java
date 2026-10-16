package net.nightshade.divinity_engine.world.inventory;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.nightshade.divinity_engine.block.StatueBlock;
import net.nightshade.divinity_engine.divinity.gods.BaseGod;
import net.nightshade.divinity_engine.registry.gui.MenusRegistry;
import net.nightshade.divinity_engine.registry.divinity.gods.GodsRegistry;
import net.nightshade.divinity_engine.util.DivinityEngineHelper;
import net.nightshade.divinity_engine.util.divinity.gods.GodHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.function.Supplier;

public class BlessingsMenu extends AbstractContainerMenu implements Supplier<Map<Integer, Slot>> {
	public final static HashMap<String, Object> guistate = new HashMap<>();
	public final Level world;
	public final Player entity;
	public int x, y, z;
	private final BlockPos openingPos;
	private ContainerLevelAccess access = ContainerLevelAccess.NULL;
	private IItemHandler internal;
	private final Map<Integer, Slot> customSlots = new HashMap<>();
	private boolean bound = false;
	private Supplier<Boolean> boundItemMatcher = null;
	private Entity boundEntity = null;
	private BlockEntity boundBlockEntity = null;
	private BaseGod god = null;
    private final boolean openedFromStatue;
    private long nextStoryAction;

    public boolean takeStoryAction(long now) {
        if (now < nextStoryAction) return false;
        nextStoryAction = now + 4;
        return true;
    }

	public BlessingsMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
		this(id, inv, readOpening(inv.player.level(), extraData));
	}

    private record Opening(BlockPos pos, boolean statue, BaseGod god) {}

    private static Opening readOpening(Level world, FriendlyByteBuf buffer) {
        if (buffer == null) return new Opening(null, false, null);
        BlockPos pos = buffer.readBlockPos();
        if (buffer.isReadable()) {
            boolean statue = buffer.readBoolean();
            return new Opening(pos, statue, statue ? GodsRegistry.GODS_REGISTRY.get().getValue(buffer.readResourceLocation()) : null);
        }
        // Preserve the original position-only constructor for addon menu providers.
        BaseGod god = world.hasChunkAt(pos) && world.getBlockState(pos).getBlock() instanceof StatueBlock block ? block.getGod() : null;
        return new Opening(pos, god != null, god);
    }

    public static void writeOpening(FriendlyByteBuf buffer, BlockPos pos, BaseGod god) {
        buffer.writeBlockPos(pos);
        buffer.writeBoolean(god != null);
        if (god != null) buffer.writeResourceLocation(god.getRegistryName());
    }

    public static BlessingsMenu fromStatue(int id, Inventory inv, BlockPos pos) {
        BaseGod god = inv.player.level().getBlockState(pos).getBlock() instanceof StatueBlock block ? block.getGod() : null;
        return new BlessingsMenu(id, inv, new Opening(pos.immutable(), true, god));
    }

	private BlessingsMenu(int id, Inventory inv, Opening opening) {
		super(MenusRegistry.BLESSINGS_GUI.get(), id);
		this.entity = inv.player;
		this.world = inv.player.level();
        this.internal = new ItemStackHandler(0);
		BlockPos pos = opening.pos();
		if (pos != null) {
			this.x = pos.getX();
			this.y = pos.getY();
			this.z = pos.getZ();
			access = ContainerLevelAccess.create(world, pos);
		}
        this.openingPos = pos;
        this.god = opening.god();
        this.openedFromStatue = opening.statue();
	}

	public BaseGod getGod() {
		return god;
	}

    public boolean isOpenedFromStatue() {
        return openedFromStatue;
    }

    public BlockPos getOpeningPos() {
        return openingPos;
    }

    public boolean allowsGod(ResourceLocation godId) {
        return godId != null && (!openedFromStatue || god != null && god.getRegistryName().equals(godId));
    }

    public List<BaseGod> getVisibleGods() {
        if (openedFromStatue) return god == null ? List.of() : List.of(god);
        return GodHelper.getAllContactedGods(entity).stream().map(instance -> instance.getBaseGod()).distinct().toList();
    }

	@Override
	public boolean stillValid(Player player) {
        if (player != entity || !player.isAlive() || player.level() != world) return false;
        if (openedFromStatue) {
            return god != null && openingPos != null && world.hasChunkAt(openingPos)
                    && world.getBlockState(openingPos).getBlock() instanceof StatueBlock statue
                    && statue.getGod() == god
                    && player.distanceToSqr(openingPos.getX() + .5, openingPos.getY() + .5, openingPos.getZ() + .5) <= 64;
        }
		if (this.bound) {
			if (this.boundItemMatcher != null)
				return this.boundItemMatcher.get();
			else if (this.boundBlockEntity != null)
				return AbstractContainerMenu.stillValid(this.access, player, this.boundBlockEntity.getBlockState().getBlock());
			else if (this.boundEntity != null)
				return this.boundEntity.isAlive();
		}
		return true;
	}

	@Override
	public void removed(Player pPlayer) {
		DivinityEngineHelper.setBlessingsPageNum(pPlayer, 0);
		super.removed(pPlayer);
	}

	@Override
	public ItemStack quickMoveStack(Player playerIn, int index) {
		return ItemStack.EMPTY;
	}

	public Map<Integer, Slot> get() {
		return customSlots;
	}
}
