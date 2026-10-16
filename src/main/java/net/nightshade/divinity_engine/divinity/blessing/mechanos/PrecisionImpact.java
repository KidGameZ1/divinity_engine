package net.nightshade.divinity_engine.divinity.blessing.mechanos;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals.Cue;

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

            // Keep the machine sound alongside its blessing visual.
            serverLevel.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1f, 1f);
            if (!event.isCanceled()) {
                BlessingVisuals.at(instance, event.getEntity(), Vec3.atCenterOf(pos).add(0, 0.6, 0), Cue.CAST, 0.65);
            }

            return true;
        }

        return false;
    }

    private boolean isFurnaceBlock(BlockEntity be) {
        return be instanceof AbstractFurnaceBlockEntity;
    }
}
