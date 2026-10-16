package net.nightshade.divinity_engine.core.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.ForgeHooks;
import net.nightshade.divinity_engine.util.divinity.innerworld.CommunionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** RETURN observes the final cancellation result, including LOWEST-priority protection listeners. */
@Mixin(value=ForgeHooks.class,remap=false)
public abstract class CommunionAttackHook {
    @Inject(method="onPlayerAttack",at=@At("RETURN"),cancellable=true)
    private static void communion$authorized(LivingEntity entity,DamageSource source,float amount,CallbackInfoReturnable<Boolean> cir){
        if(cir.getReturnValueZ()&&entity instanceof ServerPlayer player&&!CommunionManager.authorizedPlayerAttack(player,source,amount))cir.setReturnValue(false);
    }
}
