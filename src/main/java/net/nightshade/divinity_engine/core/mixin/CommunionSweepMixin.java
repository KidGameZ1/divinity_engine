package net.nightshade.divinity_engine.core.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.nightshade.divinity_engine.util.divinity.innerworld.CommunionDamage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class CommunionSweepMixin {
    @Inject(method="attack",at=@At("HEAD")) private void communion$begin(Entity target,CallbackInfo ci){CommunionDamage.push();}
    @Inject(method="attack",at=@At("RETURN")) private void communion$end(Entity target,CallbackInfo ci){CommunionDamage.pop();}
}
