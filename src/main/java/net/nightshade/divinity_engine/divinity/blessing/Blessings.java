package net.nightshade.divinity_engine.divinity.blessing;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.registries.IForgeRegistry;
import net.nightshade.divinity_engine.divinity.gods.BaseGod;
import net.nightshade.divinity_engine.registry.divinity.blessing.BlessingsRegistry;
import net.nightshade.nightshade_core.util.MiscHelper;

import javax.annotation.Nullable;
import java.awt.*;

/**
 * Represents a blessing that can be granted to entities in the game.
 * Handles various event callbacks and blessing state management.
 */
public class Blessings {
    /**
     * Amount of favor required to activate this blessing
     */
    private final int neededFavor;
    /**
     * Cooldown period in ticks between blessing activations
     */
    private final int cooldown;
    /**
     * Whether this blessing is currently active
     */
    private final boolean isPassive;
    /**
     * Whether this blessing can be toggled on/off
     */
    private final boolean canToggle;
    /**
     * Color used for blessing text display
     */
    private final Color textColor;

    /**
     * Creates a new blessing with the specified parameters.
     *
     * @param neededFavor Amount of favor required to activate
     * @param cooldown    Cooldown period in seconds
     * @param isPassive    Initial active state
     * @param canToggle   Whether blessing can be toggled
     * @param textColor   Color for blessing text
     */
    public Blessings(int neededFavor, int cooldown, boolean isPassive, boolean canToggle, Color textColor) {
        this.neededFavor = neededFavor;
        this.cooldown = MiscHelper.secondsToTick(cooldown);
        this.isPassive = isPassive;
        this.canToggle = canToggle;
        this.textColor = textColor;
    }

    /**
     * Gets the amount of favor needed to activate this blessing.
     *
     * @return Required favor amount
     */
    public int getNeededFavor() {
        return neededFavor;
    }

    /**
     * Gets the cooldown period in ticks.
     *
     * @return Cooldown period
     */
    public int getCooldown() {
        return cooldown;
    }

    /**
     * Called when this blessing is toggled on.
     *
     * @param instance The blessing instance
     * @param entity   The affected entity
     */
    public void onToggleOn(BlessingsInstance instance, LivingEntity entity) {
    }

    /**
     * Called when this blessing is toggled off.
     *
     * @param instance The blessing instance
     * @param entity   The affected entity
     * @return True if toggle was handled
     */
    public boolean onToggleOff(BlessingsInstance instance, LivingEntity entity) {
        return false;
    }

    public boolean onTick(BlessingsInstance instance, LivingEntity living) {
        return false;
    }

    public boolean onPressed(BlessingsInstance instance, LivingEntity living) {
        return false;
    }

    public boolean onHeld(BlessingsInstance instance, LivingEntity living, int heldTicks) {
        return false;
    }

    public boolean onRelease(BlessingsInstance instance, LivingEntity living, int heldTicks) {
        return false;
    }

    public boolean onRightClickBlock(BlessingsInstance instance, PlayerInteractEvent.RightClickBlock event) {
        return false;
    }

    public boolean onRightClickEntity(BlessingsInstance instance, PlayerInteractEvent.EntityInteract event) {
        return false;
    }

    public boolean onRightClickItem(BlessingsInstance instance, PlayerInteractEvent.RightClickItem event) {
        return false;
    }

    public boolean onRightClickEmpty(BlessingsInstance instance, PlayerInteractEvent.RightClickEmpty event) {
        return false;
    }

    public boolean onBeingTargeted(BlessingsInstance instance, LivingEntity target, LivingChangeTargetEvent event) {
        return false;
    }

    public boolean onBeingDamaged(BlessingsInstance instance, LivingAttackEvent event) {
        return false;
    }

    public boolean onCriticalHit(BlessingsInstance instance, CriticalHitEvent event) {
        return false;
    }

    public boolean onDamageEntity(BlessingsInstance instance, LivingEntity player, LivingHurtEvent event) {
        return false;
    }

    public boolean onKillEntity(BlessingsInstance instance, LivingEntity player, LivingDeathEvent event) {
        return false;
    }

    public boolean onTakenDamage(BlessingsInstance instance, LivingDamageEvent event) {
        return false;
    }

    public boolean onProjectileHit(BlessingsInstance instance, LivingEntity living, ProjectileImpactEvent event) {
        return false;
    }

    public boolean onDeath(BlessingsInstance instance, LivingDeathEvent event) {
        return false;
    }

    public boolean onRespawn(BlessingsInstance instance, PlayerEvent.PlayerRespawnEvent event) {
        return false;
    }

    public boolean onHeal(BlessingsInstance instance, LivingHealEvent event) {
        return false;
    }

    public boolean onKnockback(BlessingsInstance instance, LivingKnockBackEvent event) {
        return false;
    }

    public boolean onFall(BlessingsInstance instance, LivingFallEvent event) {
        return false;
    }

    public boolean onEXPDrop(BlessingsInstance instance, LivingExperienceDropEvent event) {
        return false;
    }

    public boolean onEXPPickup(BlessingsInstance instance, PlayerXpEvent.PickupXp event) {
        return false;
    }

    public boolean onJump(BlessingsInstance instance, LivingEvent.LivingJumpEvent event) {
        return false;
    }

    public boolean onDrowning(BlessingsInstance instance, LivingDrownEvent event) {
        return false;
    }

    public boolean onBreathe(BlessingsInstance instance, LivingBreatheEvent event) {
        return false;
    }

    public boolean onBlocksWithShield(BlessingsInstance instance, ShieldBlockEvent event) {
        return false;
    }

    public boolean onBreakBlock(BlessingsInstance instance, BlockEvent.BreakEvent event) {
        return false;
    }
    
    public boolean onPlaceBlock(BlessingsInstance instance, BlockEvent.EntityPlaceEvent event) {
        return false;
    }

    public boolean onItemCrafted(BlessingsInstance instance, PlayerEvent.ItemCraftedEvent event) {
        return false;
    }

    public boolean onItemPickup(BlessingsInstance instance, PlayerEvent.ItemPickupEvent event) {
        return false;
    }

    public boolean onItemSmelted(BlessingsInstance instance, PlayerEvent.ItemSmeltedEvent event) {
        return false;
    }

    public boolean onSleepInBed(BlessingsInstance instance, PlayerSleepInBedEvent event) {
        return false;
    }

    public boolean onWakeUp(BlessingsInstance instance, PlayerWakeUpEvent event) {
        return false;
    }

    public Color getColor() {
        return textColor;
    }

    public boolean isPassive() {
        return isPassive;
    }

    /**
     * Creates a default instance of this blessing.
     *
     * @param <T> Type of blessing instance
     * @return New blessing instance
     */
    public <T extends BlessingsInstance> T createDefaultInstance() {
        return (T) new BlessingsInstance(this);
    }
    @Nullable
    public ResourceLocation getRegistryName() {
        return ((IForgeRegistry) BlessingsRegistry.BLESSINGS_REGISTRY.get()).getKey(this);
    }


    @Nullable
    public MutableComponent getName() {
        ResourceLocation id = this.getRegistryName();
        return id == null ? null : Component.translatable(String.format("%s.blessings.%s", id.getNamespace(), id.getPath().replace('/', '.')));
    }

    public String getNameTranslationKey() {
        return ((TranslatableContents)this.getName().getContents()).getKey();
    }

    public String getDescriptionTranslationKey() {
        ResourceLocation id = this.getRegistryName();
        return id == null ? null : String.format("%s.blessings.%s.description", id.getNamespace(), id.getPath().replace('/', '.'));
    }


    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass() == o.getClass()) {
            BaseGod gods = (BaseGod) o;
            return this.getRegistryName().equals(gods.getRegistryName());
        } else {
            return false;
        }
    }

    public boolean canToggle() {
        return canToggle;
    }
}
