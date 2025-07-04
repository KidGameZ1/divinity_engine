package net.nightshade.divinity_engine.divinity.curse;

import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.event.level.BlockEvent;
import net.nightshade.divinity_engine.divinity.gods.BaseGod;
import net.nightshade.divinity_engine.divinity.gods.BaseGodInstance;
import net.nightshade.divinity_engine.network.events.divinity.curse.CursePlayerEvent;
import net.nightshade.divinity_engine.network.events.divinity.curse.UncursePlayerEvent;
import net.nightshade.divinity_engine.registry.divinity.curse.CurseRegistry;
import net.nightshade.divinity_engine.registry.divinity.gods.GodsRegistry;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Represents an instance of a curse that can be applied to entities.
 * Handles curse state management, cooldowns, and event callbacks.
 * Can be bound to a specific god and serialized/deserialized via NBT.
 */
public class CurseInstance implements Cloneable {
    /** The god instance this curse is bound to, if any */
    private @Nullable BaseGodInstance boundGod = null;

    /**
     * Tracks if curse state has changed and needs saving
     */
    private boolean dirty = false;

    /**
     * Additional custom NBT data stored with this curse instance
     */
    @Nullable
    private CompoundTag tag;
    /**
     * Reference to the registry entry for this curse type
     */
    private final Holder.Reference<Curse> cursesReference;

    public CurseInstance(Curse curse) {
        this.cursesReference = CurseRegistry.CURSES_REGISTRY.get().getDelegateOrThrow(curse);
    }

    public Curse getCurse() {
        return (Curse) this.cursesReference.get();
    }

    public ResourceLocation getCurseID() {
        return this.cursesReference.key().location();
    }

    public CurseInstance clone() {
        CurseInstance clone = new CurseInstance(this.getCurse());
        clone.dirty = this.dirty;
        return clone;
    }

    /**
     * Serializes this curse instance to NBT data.
     * Includes curse ID, cooldown, toggle state, bound god, and custom tag data.
     *
     * @return NBT compound containing curse data
     */
    @ApiStatus.NonExtendable
    public final CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Blessing", this.getCurseID().toString());

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

    /**
     * Restores this curse instance's state from NBT data.
     * Updates cooldown, toggle state, bound god, and custom tag data.
     *
     * @param tag NBT compound containing curse data
     */
    public void deserialize(CompoundTag tag) {
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

    /**
     * Creates a new curse instance from serialized NBT data.
     *
     * @param tag NBT compound containing curse data
     * @return New curse instance with restored state
     * @throws IllegalArgumentException if curse ID is invalid or unknown
     */
    @ApiStatus.NonExtendable
    public static CurseInstance fromNBT(CompoundTag tag) {
        ResourceLocation curseId = ResourceLocation.tryParse(tag.getString("Blessing"));
        if (curseId == null) {
            throw new IllegalArgumentException("Invalid curse ID in NBT");
        }

        Curse curse = CurseRegistry.CURSES_REGISTRY.get().getValue(curseId);
        if (curse == null) {
            throw new IllegalArgumentException("Unknown curse: " + curseId);
        }

        // Create the instance
        CurseInstance instance = curse.createDefaultInstance();

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

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass() == o.getClass()) {
            CurseInstance instance = (CurseInstance)o;
            return this.getCurseID().equals(instance.getCurseID()) && this.cursesReference.key().registry().equals(instance.cursesReference.key().registry());
        } else {
            return false;
        }
    }

    public int hashCode() {
        return Objects.hash(new Object[]{this.cursesReference});
    }

    public void setBoundGod(@Nullable BaseGodInstance boundGod) {
        this.boundGod = boundGod;
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
    

    public void onTick(LivingEntity entity) {this.getCurse().onTick(this, entity);}

    public void onCurse(CursePlayerEvent event) {this.getCurse().onCurse(this, event);}

    public void onUncurse(UncursePlayerEvent event) {this.getCurse().onUncurse(this, event);}

    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {this.getCurse().onRightClickBlock(this, event);}

    public void onRightClickEntity(PlayerInteractEvent.EntityInteract event) {this.getCurse().onRightClickEntity(this, event);}

    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {this.getCurse().onRightClickItem(this, event);
    }

    public void onRightClickEmpty(PlayerInteractEvent.RightClickEmpty event) {this.getCurse().onRightClickEmpty(this, event);}

    public void onBeingTargeted(LivingEntity target, LivingChangeTargetEvent event) {this.getCurse().onBeingTargeted(this, target, event);}

    public void onBeingDamaged(LivingAttackEvent event) {this.getCurse().onBeingDamaged(this, event);}

    public void onCriticalHit(CriticalHitEvent event) {this.getCurse().onCriticalHit(this, event);}

    public void onDamageEntity(LivingEntity player, LivingHurtEvent event) {this.getCurse().onDamageEntity(this, player,event);}

    public void onKillEntity(LivingEntity player, LivingDeathEvent event) {this.getCurse().onKillEntity(this, player,event);}

    public void onTakenDamage(LivingDamageEvent event) {this.getCurse().onTakenDamage(this,event);}

    public void onProjectileHit(LivingEntity entity, ProjectileImpactEvent event) {this.getCurse().onProjectileHit(this, entity, event);}

    public void onDeath(LivingDeathEvent event) {this.getCurse().onDeath(this, event);}

    public void onRespawn(PlayerEvent.PlayerRespawnEvent event) {this.getCurse().onRespawn(this, event);}

    public void onHeal(LivingHealEvent event) {this.getCurse().onHeal(this, event);}

    public void onKnockback(LivingKnockBackEvent event) {this.getCurse().onKnockback(this, event);}

    public void onFall(LivingFallEvent event) {
        this.getCurse().onFall(this, event);
            
    }

    public void onEXPDrop(LivingExperienceDropEvent event) {
        this.getCurse().onEXPDrop(this, event);
            
    }

    public void onEXPPickup(PlayerXpEvent.PickupXp event) {
        this.getCurse().onEXPPickup(this, event);
            
    }

    public void onJump(LivingEvent.LivingJumpEvent event) {this.getCurse().onJump(this, event);}

    public void onDrowning(LivingDrownEvent event) {
        this.getCurse().onDrowning(this, event);
            
    }

    public void onBreathe(LivingBreatheEvent event) {
        this.getCurse().onBreathe(this, event);
            
    }

    public void onBlocksWithShield(ShieldBlockEvent event) {
        this.getCurse().onBlocksWithShield(this, event);
            
    }

    public void onBreakBlock(BlockEvent.BreakEvent event) {
        this.getCurse().onBreakBlock(this, event);
            
    }

    public void onPlaceBlock(BlockEvent.EntityPlaceEvent event){
        this.getCurse().onPlaceBlock(this, event);
            
    }

    public void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        this.getCurse().onItemCrafted(this, event);
            
    }

    public void onItemPickup(PlayerEvent.ItemPickupEvent event) {
        this.getCurse().onItemPickup(this, event);}

    public void onItemSmelted(PlayerEvent.ItemSmeltedEvent event){this.getCurse().onItemSmelted(this, event);}

    public void onSleepInBed(PlayerSleepInBedEvent event) {this.getCurse().onSleepInBed(this, event);}

    public void onWakeUp(PlayerWakeUpEvent event){this.getCurse().onWakeUp(this, event);}
}
