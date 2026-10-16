package net.nightshade.divinity_engine.core.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.nightshade.divinity_engine.network.events.divinity.trials.TrialArenaProtection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FlowingFluid.class)
public abstract class TrialFluidMixin {
    @Inject(method = "canSpreadTo", at = @At("HEAD"), cancellable = true)
    private void preventSpread(net.minecraft.world.level.BlockGetter getter, BlockPos from, BlockState fromState,
                               Direction direction, BlockPos to, BlockState toState, FluidState fluidState,
                               net.minecraft.world.level.material.Fluid fluid, CallbackInfoReturnable<Boolean> cir) {
        if (getter instanceof LevelAccessor level && (TrialArenaProtection.protectedAt(level, from)
                || TrialArenaProtection.protectedAt(level, to))) cir.setReturnValue(false);
    }
    @Inject(method = "spreadTo", at = @At("HEAD"), cancellable = true)
    private void protectArena(LevelAccessor level, BlockPos pos, BlockState block, Direction direction, FluidState fluid, CallbackInfo ci) {
        if (TrialArenaProtection.protectedAt(level, pos)
                || TrialArenaProtection.protectedAt(level, pos.relative(direction.getOpposite()))) ci.cancel();
    }
}
