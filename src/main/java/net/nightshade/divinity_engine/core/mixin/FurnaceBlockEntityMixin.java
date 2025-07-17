package net.nightshade.divinity_engine.core.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractFurnaceBlockEntity.class)
public class FurnaceBlockEntityMixin {

    @Shadow
    @Final
    private ContainerData dataAccess;

    @Inject(method = "serverTick", at = @At("HEAD"))
    private static void onTick(Level pLevel, BlockPos pPos, BlockState pState, AbstractFurnaceBlockEntity pBlockEntity, CallbackInfo ci) {
        if (pLevel.isClientSide) return;

        CompoundTag tag = pBlockEntity.getPersistentData();
        long currentTime = pLevel.getGameTime();

        if (tag.contains("mechanos_boost_timer") && tag.getLong("mechanos_boost_timer") > currentTime) {
            // Accelerate cooking: run extra cook tick logic
            accelerateCooking(pBlockEntity);
        }
    }

    @Unique
    private static void accelerateCooking(AbstractFurnaceBlockEntity furnace) {
        // Double the cooking progress for this tick
        ContainerData data = ((FurnaceBlockEntityAccessor) furnace).getDataAccess();
        int currentProgress = data.get(2); // cookingProgress
        int maxProgress = data.get(3);     // cookingTotalTime

        if (currentProgress < maxProgress) {
            data.set(2, Math.min(currentProgress + 1, maxProgress)); // +1 tick of progress
        }
    }
}
