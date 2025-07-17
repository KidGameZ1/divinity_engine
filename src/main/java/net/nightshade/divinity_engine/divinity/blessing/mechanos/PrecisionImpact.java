package net.nightshade.divinity_engine.divinity.blessing.mechanos;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;

import java.awt.*;

public class PrecisionImpact extends Blessings {
    /**
     * Creates a new blessing with the specified parameters.
     *
     * @param neededFavor Amount of favor required to activate
     * @param cooldown    Cooldown period in seconds
     * @param isPassive   Is a passive blessing or not
     * @param canToggle   Whether blessing can be toggled
     * @param textColor   Color for blessing text
     */
    public PrecisionImpact(int neededFavor, int cooldown, boolean isPassive, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isPassive, isActive, canToggle, textColor);
    }

    @Override
    public boolean onRightClickBlock(BlessingsInstance instance, PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity().level() instanceof ServerLevel serverLevel)) return false;

        BlockPos pos = event.getHitVec().getBlockPos();

        // Check if it’s a “machine” (Furnace, Blast Furnace, etc.)
        BlockEntity be = serverLevel.getBlockEntity(pos);
        if (be != null && isFurnaceBlock(be)) {
            // Simulate speeding it up: apply a temporary effect or tag
            be.getPersistentData().putLong("mechanos_boost_timer", serverLevel.getGameTime() + 200); // lasts 10 sec

            // Optional: Play gear sound & particles
            serverLevel.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1f, 1f);
            ((ServerLevel) event.getEntity().level()).sendParticles(ParticleTypes.CRIT, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 10, 0.2, 0.2, 0.2, 0.01);

            return true;
        }

        return false;
    }

    private boolean isFurnaceBlock(BlockEntity be) {
        return be instanceof AbstractFurnaceBlockEntity;
    }
}
