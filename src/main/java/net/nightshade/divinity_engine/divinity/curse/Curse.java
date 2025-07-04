package net.nightshade.divinity_engine.divinity.curse;

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
import net.nightshade.divinity_engine.network.events.divinity.curse.CursePlayerEvent;
import net.nightshade.divinity_engine.network.events.divinity.curse.UncursePlayerEvent;
import net.nightshade.divinity_engine.registry.divinity.blessing.BlessingsRegistry;
import net.nightshade.nightshade_core.util.MiscHelper;

import javax.annotation.Nullable;
import java.awt.*;

/**
 * Represents a blessing that can be granted to entities in the game.
 * Handles various event callbacks and blessing state management.
 */
public class Curse {


    public Curse() {
    }

    public void onTick(CurseInstance instance, LivingEntity living) {
        
    }

    public void onCurse(CurseInstance instance, CursePlayerEvent event) {

    }

    public void onUncurse(CurseInstance instance, UncursePlayerEvent event) {

    }

    public void onRightClickBlock(CurseInstance instance, PlayerInteractEvent.RightClickBlock event) {
        
    }

    public void onRightClickEntity(CurseInstance instance, PlayerInteractEvent.EntityInteract event) {
        
    }

    public void onRightClickItem(CurseInstance instance, PlayerInteractEvent.RightClickItem event) {
        
    }

    public void onRightClickEmpty(CurseInstance instance, PlayerInteractEvent.RightClickEmpty event) {
        
    }

    public void onBeingTargeted(CurseInstance instance, LivingEntity target, LivingChangeTargetEvent event) {
        
    }

    public void onBeingDamaged(CurseInstance instance, LivingAttackEvent event) {
        
    }

    public void onCriticalHit(CurseInstance instance, CriticalHitEvent event) {
        
    }

    public void onDamageEntity(CurseInstance instance, LivingEntity player, LivingHurtEvent event) {
        
    }

    public void onKillEntity(CurseInstance instance, LivingEntity player, LivingDeathEvent event) {
        
    }

    public void onTakenDamage(CurseInstance instance, LivingDamageEvent event) {
        
    }

    public void onProjectileHit(CurseInstance instance, LivingEntity living, ProjectileImpactEvent event) {
        
    }

    public void onDeath(CurseInstance instance, LivingDeathEvent event) {
        
    }

    public void onRespawn(CurseInstance instance, PlayerEvent.PlayerRespawnEvent event) {
        
    }

    public void onHeal(CurseInstance instance, LivingHealEvent event) {
        
    }

    public void onKnockback(CurseInstance instance, LivingKnockBackEvent event) {
        
    }

    public void onFall(CurseInstance instance, LivingFallEvent event) {
        
    }

    public void onEXPDrop(CurseInstance instance, LivingExperienceDropEvent event) {
        
    }

    public void onEXPPickup(CurseInstance instance, PlayerXpEvent.PickupXp event) {
        
    }

    public void onJump(CurseInstance instance, LivingEvent.LivingJumpEvent event) {
        
    }

    public void onDrowning(CurseInstance instance, LivingDrownEvent event) {
        
    }

    public void onBreathe(CurseInstance instance, LivingBreatheEvent event) {
        
    }

    public void onBlocksWithShield(CurseInstance instance, ShieldBlockEvent event) {
        
    }

    public void onBreakBlock(CurseInstance instance, BlockEvent.BreakEvent event) {
        
    }
    
    public void onPlaceBlock(CurseInstance instance, BlockEvent.EntityPlaceEvent event) {
        
    }

    public void onItemCrafted(CurseInstance instance, PlayerEvent.ItemCraftedEvent event) {
        
    }

    public void onItemPickup(CurseInstance instance, PlayerEvent.ItemPickupEvent event) {
        
    }

    public void onItemSmelted(CurseInstance instance, PlayerEvent.ItemSmeltedEvent event) {
        
    }

    public void onSleepInBed(CurseInstance instance, PlayerSleepInBedEvent event) {
        
    }

    public void onWakeUp(CurseInstance instance, PlayerWakeUpEvent event) {
    }


    /**
     * Creates a default instance of this blessing.
     *
     * @param <T> Type of blessing instance
     * @return New blessing instance
     */
    public <T extends CurseInstance> T createDefaultInstance() {
        return (T) new CurseInstance(this);
    }
    @Nullable
    public ResourceLocation getRegistryName() {
        return ((IForgeRegistry) BlessingsRegistry.BLESSINGS_REGISTRY.get()).getKey(this);
    }


    @Nullable
    public MutableComponent getName() {
        ResourceLocation id = this.getRegistryName();
        return id == null ? null : Component.translatable(String.format("%s.curse.%s", id.getNamespace(), id.getPath().replace('/', '.')));
    }

    public String getNameTranslationKey() {
        return ((TranslatableContents)this.getName().getContents()).getKey();
    }

    public String getDescriptionTranslationKey() {
        ResourceLocation id = this.getRegistryName();
        return id == null ? null : String.format("%s.curse.%s.description", id.getNamespace(), id.getPath().replace('/', '.'));
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
}
