package net.nightshade.divinity_engine.core.mixin;

import net.minecraft.world.level.Explosion;
import net.nightshade.divinity_engine.util.divinity.innerworld.CommunionDamage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Explosion.class)
public abstract class CommunionExplosionMixin {
    @Inject(method="explode",at=@At("HEAD")) private void communion$begin(CallbackInfo ci){CommunionDamage.push();}
    @Inject(method="explode",at=@At("RETURN")) private void communion$end(CallbackInfo ci){CommunionDamage.pop();}
}
