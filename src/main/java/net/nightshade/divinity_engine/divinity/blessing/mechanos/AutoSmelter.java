package net.nightshade.divinity_engine.divinity.blessing.mechanos;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.level.BlockEvent;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;

import java.util.Random;

import java.awt.*;
import java.util.List;
import java.util.Optional;

public class AutoSmelter extends Blessings {
    public AutoSmelter(int neededFavor, int cooldown, boolean isPassive, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isPassive, isActive, canToggle, textColor);
    }

    @Override
    public boolean onBreakBlock(BlessingsInstance instance, BlockEvent.BreakEvent event) {
        if (instance.isToggled()) {
            Player player = event.getPlayer();
            ItemStack tool = player.getMainHandItem();

            // Skip if silk touch is applied
            if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, tool) > 0) {
                return super.onBreakBlock(instance, event);
            }

            BlockState state = event.getState();
            BlockPos pos = event.getPos();
            Level world = event.getPlayer().level();

            // 30% chance to trigger smelting
            if (new Random().nextFloat() < 0.3f) {
                ItemStack smelted = getSmeltedDrop(world, state);

                // Only proceed if there's a valid smelting result
                if (!smelted.isEmpty() && !smelted.equals(new ItemStack(state.getBlock()))) {
                    event.setCanceled(true);
                    state.getBlock().destroy(world, pos, state);

                    // Spawn items
                    Block.popResource(world, pos, smelted);

                    // Add particles
                    if (world instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.CRIT,
                                pos.getX() + 0.5,
                                pos.getY() + 0.5,
                                pos.getZ() + 0.5,
                                10, 0.2, 0.2, 0.2, 0.1);
                    }
                    instance.setToggled(false);
                    return true;
                }
            }
        }
        return super.onBreakBlock(instance, event);
    }

    private ItemStack getSmeltedDrop(Level world, BlockState state) {
        ItemStack originalDrop = new ItemStack(state.getBlock().asItem());

        // Find the smelting recipe for this block
        Optional<SmeltingRecipe> recipe = world.getRecipeManager()
                .getRecipesFor(RecipeType.SMELTING, new SimpleContainer(originalDrop), world)
                .stream()
                .findFirst();

        // If a smelting recipe exists, return the correct smelted output with the right quantity
        return recipe.map(r -> {
            ItemStack result = r.getResultItem(world.registryAccess()).copy();
            result.setCount(r.getResultItem(world.registryAccess()).getCount()); // Ensure correct output amount
            return result;
        }).orElse(originalDrop);
    }
    public static List<ItemStack> getDefaultBlockDrops(ServerLevel world, BlockPos pos, BlockState state, Player player) {
        Block block = state.getBlock();

        // Simulate block breaking to get the default drops
        LootParams.Builder lootBuilder = new LootParams.Builder(world)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                .withParameter(LootContextParams.BLOCK_STATE, state)
                .withParameter(LootContextParams.TOOL, player.getMainHandItem());

        return state.getDrops(lootBuilder);
    }

}
