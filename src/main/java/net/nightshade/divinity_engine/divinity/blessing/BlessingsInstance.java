package net.nightshade.divinity_engine.divinity.blessing;

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
import net.nightshade.divinity_engine.registry.divinity.blessing.BlessingsRegistry;
import net.nightshade.divinity_engine.registry.divinity.gods.GodsRegistry;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Represents an instance of a blessing that can be applied to entities.
 * Handles blessing state management, cooldowns, and event callbacks.
 * Can be bound to a specific god and serialized/deserialized via NBT.
 */
public class BlessingsInstance implements Cloneable {
    /** Current cooldown ticks remaining before blessing can be used again */
    private int cooldown = 0;
    /** The god instance this blessing is bound to, if any */
    private BaseGodInstance boundGod = null;
    /** Whether this blessing is currently toggled on/off (for toggleable blessings) */
    private boolean toggled = false;

    /**
     * Tracks if blessing state has changed and needs saving
     */
    private boolean dirty = false;

    /**
     * Additional custom NBT data stored with this blessing instance
     */
    @Nullable
    private CompoundTag tag;
    /**
     * Reference to the registry entry for this blessing type
     */
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

    /**
     * Serializes this blessing instance to NBT data.
     * Includes blessing ID, cooldown, toggle state, bound god, and custom tag data.
     *
     * @return NBT compound containing blessing data
     */
    @ApiStatus.NonExtendable
    public final CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Blessing", this.getBlessingId().toString());
        tag.putInt("Cooldown", this.cooldown);
        if (getBlessing().canToggle())
            tag.putBoolean("Toggled", this.toggled);

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
        if (getBlessing().canToggle())
            tag.putBoolean("Toggled", this.toggled);
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
     * Restores this blessing instance's state from NBT data.
     * Updates cooldown, toggle state, bound god, and custom tag data.
     *
     * @param tag NBT compound containing blessing data
     */
    public void deserialize(CompoundTag tag) {
        this.cooldown = tag.getInt("Cooldown");
        if (getBlessing().canToggle())
            this.toggled = tag.getBoolean("Toggled");
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
     * Creates a new blessing instance from serialized NBT data.
     *
     * @param tag NBT compound containing blessing data
     * @return New blessing instance with restored state
     * @throws IllegalArgumentException if blessing ID is invalid or unknown
     */
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
        if (blessing.canToggle())
        instance.toggled = tag.getBoolean("Toggled");

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

    public boolean isOnCooldown() {
        return this.cooldown > 0;
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

    public boolean isToggled() {
        if (getBlessing().canToggle()) {
            return toggled;
        }
        return false;
    }

    public void setToggled(boolean toggled) {
        if (getBlessing().canToggle()) {
            this.toggled = toggled;
        }
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
        this.setToggled(true);
    }

    public void onToggleOff(LivingEntity entity) {
        if (this.getBlessing().onToggleOff(this, entity)){
            setCooldown(getBlessing().getCooldown());
        }
        this.setToggled(false);
    }

    public void onTick(LivingEntity entity) {
        if (this.getBlessing().onTick(this, entity)){
            setCooldown(getBlessing().getCooldown());
        }

    }

    public void onPressed(LivingEntity entity) {
        this.getBlessing().onPressed(this, entity);
    }

    public void onHeld(LivingEntity entity, int heldTicks) {
        this.getBlessing().onHeld(this, entity, heldTicks);
    }

    public void onRelease(LivingEntity entity, int heldTicks) {
        if (this.getBlessing().onRelease(this, entity, heldTicks) || this.getBlessing().onHeld(this, entity, heldTicks) || this.getBlessing().onPressed(this, entity))
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

    public void onCriticalHit(CriticalHitEvent event) {
        if(this.getBlessing().onCriticalHit(this, event))
            setCooldown(getBlessing().getCooldown());
    }

    public void onDamageEntity(LivingEntity player, LivingHurtEvent event) {
        if(this.getBlessing().onDamageEntity(this, player,event))
            setCooldown(getBlessing().getCooldown());
    }

    public void onKillEntity(LivingEntity player, LivingDeathEvent event) {
        if(this.getBlessing().onKillEntity(this, player,event))
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

    public void onSleepInBed(PlayerSleepInBedEvent event) {
        if(this.getBlessing().onSleepInBed(this, event))
            setCooldown(getBlessing().getCooldown());
    }

    public void onWakeUp(PlayerWakeUpEvent event){
        if (this.getBlessing().onWakeUp(this, event))
            setCooldown(getBlessing().getCooldown());
    }
}
