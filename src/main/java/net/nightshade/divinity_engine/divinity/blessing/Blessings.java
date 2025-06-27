package net.nightshade.divinity_engine.divinity.blessing;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.registries.IForgeRegistry;
import net.nightshade.divinity_engine.divinity.gods.BaseGod;
import net.nightshade.divinity_engine.divinity.gods.BaseGodInstance;
import net.nightshade.divinity_engine.registry.divinity.blessing.BlessingsRegistry;
import net.nightshade.divinity_engine.registry.divinity.gods.GodsRegistry;
import net.nightshade.nightshade_core.util.MiscHelper;

import javax.annotation.Nullable;
import java.awt.*;

public class Blessings {
    private final int neededFavor;
    private final int cooldown;
    private final boolean hasTickCooldown;

    public Blessings(int neededFavor, int cooldown, boolean hasTickCooldown) {
        this.neededFavor = neededFavor;
        this.cooldown = MiscHelper.secondsToTick(cooldown);
        this.hasTickCooldown = hasTickCooldown;
    }

    public int getNeededFavor() {
        return neededFavor;
    }

    public int getCooldown() {
        return cooldown;
    }

    public boolean hasTickCooldown() {
        return hasTickCooldown;
    }

    public boolean onToggleOn(BlessingsInstance instance, LivingEntity entity) {
        return false;
    }

    public boolean onToggleOff(BlessingsInstance instance, LivingEntity entity) {
        return false;
    }

    public boolean onTick(BlessingsInstance instance, LivingEntity living) {
        return false;
    }

    public boolean onPressed(BlessingsInstance instance, LivingEntity entity) {
        return false;
    }

    public boolean onHeld(BlessingsInstance instance, LivingEntity living, int heldTicks) {
        return false;
    }

    public boolean onRelease(BlessingsInstance instance, LivingEntity entity, int heldTicks) {
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

    public boolean onDamageEntity(BlessingsInstance instance, LivingEntity player, LivingHurtEvent event) {
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

    public Color getColor() {
        return Color.WHITE;
    }

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
}
