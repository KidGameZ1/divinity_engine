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

    public void onToggleOn(BlessingsInstance instance, LivingEntity entity) {
    }

    public void onToggleOff(BlessingsInstance instance, LivingEntity entity) {
    }

    public void onTick(BlessingsInstance instance, LivingEntity living) {
    }

    public void onPressed(BlessingsInstance instance, LivingEntity entity) {
    }

    public boolean onHeld(BlessingsInstance instance, LivingEntity living, int heldTicks) {
        return false;
    }

    public void onRelease(BlessingsInstance instance, LivingEntity entity, int heldTicks) {
    }

    public void onRightClickBlock(BlessingsInstance instance, PlayerInteractEvent.RightClickBlock event) {
    }

    public void onRightClickEntity(BlessingsInstance instance, PlayerInteractEvent.EntityInteract event) {
    }

    public void onRightClickItem(BlessingsInstance instance, PlayerInteractEvent.RightClickItem event) {
    }

    public void onRightClickEmpty(BlessingsInstance instance, PlayerInteractEvent.RightClickEmpty event) {
    }

    public void onBeingTargeted(BlessingsInstance instance, LivingEntity target, LivingChangeTargetEvent event) {
    }

    public void onBeingDamaged(BlessingsInstance instance, LivingAttackEvent event) {
    }

    public void onDamageEntity(BlessingsInstance instance, LivingEntity entity, LivingHurtEvent event) {
    }

    public void onTakenDamage(BlessingsInstance instance, LivingDamageEvent event) {
    }

    public void onProjectileHit(BlessingsInstance instance, LivingEntity living, ProjectileImpactEvent event) {
    }

    public void onDeath(BlessingsInstance instance, LivingDeathEvent event) {
    }

    public void onRespawn(BlessingsInstance instance, PlayerEvent.PlayerRespawnEvent event) {
    }

    public void onHeal(BlessingsInstance instance, LivingHealEvent event) {
    }

    public void onKnockback(BlessingsInstance instance, LivingKnockBackEvent event) {
    }

    public void onFall(BlessingsInstance instance, LivingFallEvent event) {
    }

    public void onEXPDrop(BlessingsInstance instance, LivingExperienceDropEvent event) {
    }

    public void onEXPPickup(BlessingsInstance instance, PlayerXpEvent.PickupXp event) {
    }

    public void onJump(BlessingsInstance instance, LivingEvent.LivingJumpEvent event) {
    }

    public void onDrowning(BlessingsInstance instance, LivingDrownEvent event) {
    }

    public void onBreathe(BlessingsInstance instance, LivingBreatheEvent event) {
    }

    public void onBlocksWithShield(BlessingsInstance instance, ShieldBlockEvent event) {
    }

    public void onBreakBlock(BlessingsInstance instance, BlockEvent.BreakEvent event) {
    }
    public void onPlaceBlock(BlessingsInstance instance, BlockEvent.EntityPlaceEvent event) {
    }

    public void onItemCrafted(BlessingsInstance instance, PlayerEvent.ItemCraftedEvent event) {
    }

    public void onItemPickup(BlessingsInstance instance, PlayerEvent.ItemPickupEvent event) {
    }

    public void onItemSmelted(BlessingsInstance instance, PlayerEvent.ItemSmeltedEvent event) {
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
