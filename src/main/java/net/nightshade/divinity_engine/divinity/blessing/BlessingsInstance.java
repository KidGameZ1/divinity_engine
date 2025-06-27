package net.nightshade.divinity_engine.divinity.blessing;

import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.nightshade.divinity_engine.divinity.gods.BaseGod;
import net.nightshade.divinity_engine.divinity.gods.BaseGodInstance;
import net.nightshade.divinity_engine.registry.divinity.blessing.BlessingsRegistry;
import net.nightshade.divinity_engine.registry.divinity.gods.GodsRegistry;
import net.nightshade.divinity_engine.util.divinity.gods.GodHelper;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nullable;
import java.util.Objects;

public class BlessingsInstance implements Cloneable {
    private int cooldown = 0;
    private BaseGodInstance boundGod = null;

    private boolean dirty = false;

    @Nullable
    private CompoundTag tag;
    private final Holder.Reference<Blessings> blessingsReference;

    public BlessingsInstance(Blessings blessings) {
        this.blessingsReference = BlessingsRegistry.BLESSINGS_REGISTRY.get().getDelegateOrThrow(blessings);
    }

    public Blessings getBlessing() {
        return (Blessings) this.blessingsReference.get();
    }

    public ResourceLocation getBlessingId() {
        return this.blessingsReference.key().location();
    }

    public BlessingsInstance clone() {
        BlessingsInstance clone = new BlessingsInstance(this.getBlessing());
        clone.dirty = this.dirty;
        return clone;
    }

    @ApiStatus.NonExtendable
    public final CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Blessing", this.getBlessingId().toString());
        tag.putInt("Cooldown", this.cooldown);

        if (this.boundGod != null) {
            CompoundTag godTag = new CompoundTag();
            godTag.putString("God Id", this.boundGod.getBaseGodId().toString());
            tag.put("Bound God", godTag);
        }

        if (this.tag != null) {
            tag.put("Tag", this.tag.copy());
        }

        return tag;
    }


    public CompoundTag serialize(CompoundTag tag) {
        tag.putInt("Cooldown", this.cooldown);
        if (this.boundGod != null) {
            // Store only the necessary information to identify the bound god
            CompoundTag godTag = new CompoundTag();
            godTag.putString("God Id", this.boundGod.getBaseGodId().toString());
            tag.put("Bound God", godTag);
        }


        if (this.tag != null) {
            tag.put("tag", this.tag.copy());
        }

        return tag;
    }

    public void deserialize(CompoundTag tag) {
        this.cooldown = tag.getInt("Cooldown");
        // Handle bound god deserialization
        if (tag.contains("Bound God", 10)) { // 10 is NBT compound tag type
            CompoundTag godTag = tag.getCompound("Bound God");
            if (godTag.contains("God Id")) {
                try {
                    ResourceLocation godId = new ResourceLocation(godTag.getString("God Id"));
                    BaseGod god = GodsRegistry.GODS_REGISTRY.get().getValue(godId);
                    if (god != null) {
                        this.boundGod = god.createGodDefaultInstance();
                    }
                } catch (Exception e) {
                    System.err.println("Failed to load bound god: " + e.getMessage());
                }
            }
        }

        if (tag.contains("tag", 10)) {
            this.tag = tag.getCompound("tag");
        }


    }

    @ApiStatus.NonExtendable
    public static BlessingsInstance fromNBT(CompoundTag tag) {
        ResourceLocation blessingId = ResourceLocation.tryParse(tag.getString("Blessing"));
        if (blessingId == null) {
            throw new IllegalArgumentException("Invalid blessing ID in NBT");
        }

        Blessings blessing = BlessingsRegistry.BLESSINGS_REGISTRY.get().getValue(blessingId);
        if (blessing == null) {
            throw new IllegalArgumentException("Unknown blessing: " + blessingId);
        }

        // Create the instance
        BlessingsInstance instance = blessing.createDefaultInstance();
        instance.cooldown = tag.getInt("Cooldown");

        // Handle bound god
        if (tag.contains("Bound God", 10)) {
            CompoundTag godTag = tag.getCompound("Bound God");
            if (godTag.contains("God Id")) {
                try {
                    ResourceLocation godId = new ResourceLocation(godTag.getString("God Id"));
                    BaseGod god = GodsRegistry.GODS_REGISTRY.get().getValue(godId);
                    if (god != null) {
                        instance.boundGod = god.createGodDefaultInstance();
                    }
                } catch (Exception e) {
                    System.err.println("Failed to load bound god: " + e.getMessage());
                }
            }
        }

        if (tag.contains("Tag", 10)) {
            instance.tag = tag.getCompound("Tag").copy();
        }

        return instance;
    }




    public void markDirty() {
        this.dirty = true;
    }

    public boolean isDirty() {
        return this.dirty;
    }

    @ApiStatus.Internal
    public void resetDirty() {
        this.dirty = false;
    }

    public int getNeededFavor(){
        return getBlessing().getNeededFavor();
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass() == o.getClass()) {
            BlessingsInstance instance = (BlessingsInstance)o;
            return this.getBlessingId().equals(instance.getBlessingId()) && this.blessingsReference.key().registry().equals(instance.blessingsReference.key().registry());
        } else {
            return false;
        }
    }

    public int hashCode() {
        return Objects.hash(new Object[]{this.blessingsReference});
    }

    public void setBoundGod(BaseGodInstance boundGod) {
        this.boundGod = boundGod;
        this.markDirty();
    }

    public int getCooldown() {
        return this.cooldown;
    }

    public void setCooldown(int cooldown) {
        this.cooldown = cooldown;
        this.markDirty();
    }

    public void decreaseCooldown(int cooldown) {
        this.cooldown -= cooldown;
        this.markDirty();
    }
    public void increaseCooldown(int favor) {
        this.cooldown += favor;
        this.markDirty();
    }

    public BaseGodInstance getBoundGodInstance() {
        return boundGod;
    }
    public BaseGod getBoundGod() {
        return boundGod.getBaseGod();
    }

    @Nullable
    public CompoundTag getTag() {
        return this.tag;
    }

    public CompoundTag getOrCreateTag() {
        if (this.tag == null) {
            this.setTag(new CompoundTag());
        }

        return this.tag;
    }

    public void setTag(@Nullable CompoundTag tag) {
        this.tag = tag;
        this.markDirty();
    }

    public void onToggleOn(LivingEntity entity) {
        this.getBlessing().onToggleOn(this, entity);
    }

    public void onToggleOff(LivingEntity entity) {
        if (this.getBlessing().onToggleOff(this, entity)){
            setCooldown(getBlessing().getCooldown());
        }
    }

    public void onTick(LivingEntity entity) {
        if (this.getBlessing().onTick(this, entity)){
            if(getBlessing().hasTickCooldown()){
                setCooldown(getBlessing().getCooldown());
            }
        }

    }

    public void onPressed(LivingEntity entity) {
        this.getBlessing().onPressed(this, entity);
    }

    public boolean onHeld(LivingEntity entity, int heldTicks) {
        return this.getBlessing().onHeld(this, entity, heldTicks);
    }

    public void onRelease(LivingEntity entity, int heldTicks) {
        if (this.getBlessing().onRelease(this, entity, heldTicks))
            setCooldown(getBlessing().getCooldown());
    }

    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (this.getBlessing().onRightClickBlock(this, event))
            setCooldown(getBlessing().getCooldown());
    }

    public void onRightClickEntity(PlayerInteractEvent.EntityInteract event) {
        if (this.getBlessing().onRightClickEntity(this, event))
            setCooldown(getBlessing().getCooldown());
    }

    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if(this.getBlessing().onRightClickItem(this, event))
            setCooldown(getBlessing().getCooldown());
    }

    public void onRightClickEmpty(PlayerInteractEvent.RightClickEmpty event) {
        if(this.getBlessing().onRightClickEmpty(this, event))
            setCooldown(getBlessing().getCooldown());
    }

    public void onBeingTargeted(LivingEntity target, LivingChangeTargetEvent event) {
        if(this.getBlessing().onBeingTargeted(this, target, event))
            setCooldown(getBlessing().getCooldown());
    }

    public void onBeingDamaged(LivingAttackEvent event) {
        if(this.getBlessing().onBeingDamaged(this, event))
            setCooldown(getBlessing().getCooldown());
    }

    public void onDamageEntity(LivingEntity player, LivingHurtEvent event) {
        if(this.getBlessing().onDamageEntity(this, player,event))
            setCooldown(getBlessing().getCooldown());
    }

    public void onTakenDamage(LivingDamageEvent event) {
        if (this.getBlessing().onTakenDamage(this,event))
            setCooldown(getBlessing().getCooldown());
    }

    public void onProjectileHit(LivingEntity entity, ProjectileImpactEvent event) {
        if (this.getBlessing().onProjectileHit(this, entity, event))
            setCooldown(getBlessing().getCooldown());
    }

    public void onDeath(LivingDeathEvent event) {
        if(this.getBlessing().onDeath(this, event))
            setCooldown(getBlessing().getCooldown());
    }

    public void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if(this.getBlessing().onRespawn(this, event))
            setCooldown(getBlessing().getCooldown());
    }

    public void onHeal(LivingHealEvent event) {
        if(this.getBlessing().onHeal(this, event))
            setCooldown(getBlessing().getCooldown());
    }

    public void onKnockback(LivingKnockBackEvent event) {
        if(this.getBlessing().onKnockback(this, event))
            setCooldown(getBlessing().getCooldown());
    }

    public void onFall(LivingFallEvent event) {
        if(this.getBlessing().onFall(this, event))
            setCooldown(getBlessing().getCooldown());
    }

    public void onEXPDrop(LivingExperienceDropEvent event) {
        if(this.getBlessing().onEXPDrop(this, event))
            setCooldown(getBlessing().getCooldown());
    }

    public void onEXPPickup(PlayerXpEvent.PickupXp event) {
        if(this.getBlessing().onEXPPickup(this, event))
            setCooldown(getBlessing().getCooldown());
    }

    public void onJump(LivingEvent.LivingJumpEvent event) {
        if (this.getBlessing().onJump(this, event))
            setCooldown(getBlessing().getCooldown());
           
    }

    public void onDrowning(LivingDrownEvent event) {
        if(this.getBlessing().onDrowning(this, event))
            setCooldown(getBlessing().getCooldown());
    }

    public void onBreathe(LivingBreatheEvent event) {
        if(this.getBlessing().onBreathe(this, event))
            setCooldown(getBlessing().getCooldown());
    }

    public void onBlocksWithShield(ShieldBlockEvent event) {
        if(this.getBlessing().onBlocksWithShield(this, event))
            setCooldown(getBlessing().getCooldown());
    }

    public void onBreakBlock(BlockEvent.BreakEvent event) {
        if(this.getBlessing().onBreakBlock(this, event))
            setCooldown(getBlessing().getCooldown());
    }

    public void onPlaceBlock(BlockEvent.EntityPlaceEvent event){
        if(this.getBlessing().onPlaceBlock(this, event))
            setCooldown(getBlessing().getCooldown());
    }

    public void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if(this.getBlessing().onItemCrafted(this, event))
            setCooldown(getBlessing().getCooldown());
    }

    public void onItemPickup(PlayerEvent.ItemPickupEvent event) {
        if(this.getBlessing().onItemPickup(this, event))
            setCooldown(getBlessing().getCooldown());
    }

    public void onItemSmelted(PlayerEvent.ItemSmeltedEvent event){
        if (this.getBlessing().onItemSmelted(this, event))
            setCooldown(getBlessing().getCooldown());
    }
}
