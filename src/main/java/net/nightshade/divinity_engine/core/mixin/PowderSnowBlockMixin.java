package net.nightshade.divinity_engine.core.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.PowderSnowBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.nightshade.divinity_engine.registry.divinity.blessing.BlessingsRegistry;
import net.nightshade.divinity_engine.util.MainPlayerCapabilityHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PowderSnowBlock.class)
public class PowderSnowBlockMixin {

    @Inject(method = "entityInside", at =@At("TAIL"),cancellable = true)
    public void entityInside(BlockState pState, Level pLevel, BlockPos pPos, Entity pEntity, CallbackInfo ci){
        if (MainPlayerCapabilityHelper.hasBlessingInSlot(BlessingsRegistry.SNOWVEIL.get(), pEntity)) {
            if (pEntity instanceof LivingEntity _livEntity){
//                _livEntity.setInvisible(true);
                _livEntity.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 2, false, false));
            }
        }

    }
    @Inject(method = "canEntityWalkOnPowderSnow", at =@At("HEAD"),cancellable = true)
    private static void canEntityWalkOnPowderSnow(Entity pEntity, CallbackInfoReturnable<Boolean> cir) {
        if (MainPlayerCapabilityHelper.hasBlessingInSlot(BlessingsRegistry.FROZEN_HEART.get(), pEntity)) {
                cir.setReturnValue(true);
        }
    }


}