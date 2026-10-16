package net.nightshade.divinity_engine.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.network.NetworkHooks;
import net.nightshade.divinity_engine.util.divinity.innerworld.CommunionManager;
import java.util.*;

/** A damage/appearance proxy, never a player, inventory, loot container or blessing owner. */
public class TranceBodyEntity extends PathfinderMob {
    private static final EntityDataAccessor<Optional<UUID>> OWNER=SynchedEntityData.defineId(TranceBodyEntity.class,EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<String> GOD=SynchedEntityData.defineId(TranceBodyEntity.class,EntityDataSerializers.STRING);
    private UUID session;
    private Vec3 anchor=Vec3.ZERO;
    private ServerPlayer hitOwner;
    public TranceBodyEntity(EntityType<? extends PathfinderMob> type,Level level){super(type,level);setNoAi(true);setNoGravity(true);setPersistenceRequired();}
    public static AttributeSupplier.Builder attributes(){return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,20).add(Attributes.MOVEMENT_SPEED,0).add(Attributes.KNOCKBACK_RESISTANCE,1);}
    @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(OWNER,Optional.empty());entityData.define(GOD,"");}
    public UUID owner(){return entityData.get(OWNER).orElse(null);}
    public UUID session(){return session;}
    public String god(){return entityData.get(GOD);}
    public void bind(ServerPlayer player,UUID id,String god){
        entityData.set(OWNER,Optional.of(player.getUUID()));entityData.set(GOD,god);session=id;anchor=position();
        setCustomName(Component.literal(player.getGameProfile().getName()+" - Communing with "+god));
        for(EquipmentSlot slot:EquipmentSlot.values()){setItemSlot(slot,player.getItemBySlot(slot).copy());setDropChance(slot,0);}
    }
    @Override public void tick(){
        super.tick();
        // The anchor is server-only recovery data. Clients interpolate tracked entity positions.
        if(!level().isClientSide){setDeltaMovement(Vec3.ZERO);setPos(anchor.x,anchor.y,anchor.z);CommunionManager.bodyTick(this);}
    }
    @Override public boolean hurt(DamageSource source,float amount){
        if(level().isClientSide||amount<=0||!Float.isFinite(amount)||!ForgeHooks.onLivingAttack(this,source,amount))return false;
        return CommunionManager.bodyHit(this,source,amount);
    }
    /** Carries fire-aspect/flaming-arrow follow-up through the same immediately interrupted attack. */
    public void acceptedHit(ServerPlayer player){hitOwner=player;if(isOnFire())player.setSecondsOnFire(Math.max(1,(getRemainingFireTicks()+19)/20));}
    @Override public void setSecondsOnFire(int seconds){if(hitOwner!=null&&!level().isClientSide)hitOwner.setSecondsOnFire(seconds);else super.setSecondsOnFire(seconds);}
    @Override public boolean isPushable(){return false;}
    @Override public void push(Entity entity){}
    @Override public void push(double x,double y,double z){}
    @Override public boolean canBeLeashed(Player player){return false;}
    @Override protected boolean canAddPassenger(Entity entity){return false;}
    @Override public boolean canChangeDimensions(){return false;}
    @Override public boolean doHurtTarget(Entity entity){return false;}
    @Override public Iterable<net.minecraft.world.item.ItemStack> getArmorSlots(){return level().isClientSide?super.getArmorSlots():List.of();}
    @Override public Iterable<net.minecraft.world.item.ItemStack> getHandSlots(){return level().isClientSide?super.getHandSlots():List.of();}
    @Override protected net.minecraft.world.InteractionResult mobInteract(Player player,net.minecraft.world.InteractionHand hand){return net.minecraft.world.InteractionResult.FAIL;}
    @Override protected void dropAllDeathLoot(DamageSource source){}
    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap,net.minecraft.core.Direction side){return cap==ForgeCapabilities.ITEM_HANDLER?LazyOptional.empty():super.getCapability(cap,side);}
    @Override public void addAdditionalSaveData(CompoundTag t){super.addAdditionalSaveData(t);if(owner()!=null)t.putUUID("Owner",owner());if(session!=null)t.putUUID("Communion",session);t.putString("God",god());t.putDouble("AnchorX",anchor.x);t.putDouble("AnchorY",anchor.y);t.putDouble("AnchorZ",anchor.z);}
    @Override public void readAdditionalSaveData(CompoundTag t){super.readAdditionalSaveData(t);entityData.set(OWNER,t.hasUUID("Owner")?Optional.of(t.getUUID("Owner")):Optional.empty());session=t.hasUUID("Communion")?t.getUUID("Communion"):null;entityData.set(GOD,t.getString("God"));anchor=new Vec3(t.getDouble("AnchorX"),t.getDouble("AnchorY"),t.getDouble("AnchorZ"));}
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket(){return NetworkHooks.getEntitySpawningPacket(this);}
}
