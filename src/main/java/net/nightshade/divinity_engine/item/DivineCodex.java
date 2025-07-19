package net.nightshade.divinity_engine.item;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;
import net.nightshade.divinity_engine.world.inventory.DivineCodexMenu;


public class DivineCodex extends Item {
    public DivineCodex() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pUsedHand) {
        if (pPlayer instanceof ServerPlayer _ent) {
            BlockPos _bpos = pPlayer.blockPosition();
            NetworkHooks.openScreen((ServerPlayer) _ent, new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return Component.literal("DivineCodex");
                }

                @Override
                public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
                    return new DivineCodexMenu(id, inventory, new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(_bpos));
                }
            }, _bpos);
        }
        return InteractionResultHolder.success(pPlayer.getItemInHand(pUsedHand));
    }
}
