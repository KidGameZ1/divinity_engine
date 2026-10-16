package net.nightshade.divinity_engine.core.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.nightshade.divinity_engine.util.divinity.innerworld.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class CommunionPlayerDamageMixin {
    @Inject(method="hurt",at=@At("HEAD"),cancellable=true)
    private void communion$deduplicate(DamageSource source,float amount,CallbackInfoReturnable<Boolean> cir){if(CommunionDamage.duplicate((ServerPlayer)(Object)this,source))cir.setReturnValue(false);}
    @Inject(method="hurt",at=@At("RETURN"))
    private void communion$finish(DamageSource source,float amount,CallbackInfoReturnable<Boolean> cir){CommunionManager.damageFinished((ServerPlayer)(Object)this);}
}
