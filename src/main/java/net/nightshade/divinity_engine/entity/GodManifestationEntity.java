package net.nightshade.divinity_engine.entity;

import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;
import net.nightshade.divinity_engine.util.divinity.innerworld.CommunionManager;

/** Rendered statue, deliberately NOT a physical StatueBlock or worship target. */
public final class GodManifestationEntity extends TranceBodyEntity {
    public GodManifestationEntity(EntityType<? extends PathfinderMob> type,Level level){super(type,level);}
    @Override public boolean hurt(DamageSource source,float amount){return false;}
    @Override protected InteractionResult mobInteract(Player player,InteractionHand hand){
        if(player instanceof ServerPlayer server)CommunionManager.interact(server,session(),getUUID());
        return InteractionResult.sidedSuccess(level().isClientSide);
    }
}
