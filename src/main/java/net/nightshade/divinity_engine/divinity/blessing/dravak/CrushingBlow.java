package net.nightshade.divinity_engine.divinity.blessing.dravak;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A blessing that increases damage after consecutive hits on the same target.
 * After 6 hits, deals 2.5x damage and knocks back the target.
 */
public class CrushingBlow extends Blessings {

    /**
     * Creates a new Crushing Blow blessing
     *
     * @param neededFavor Amount of favor required to activate
     * @param cooldown Cooldown period in seconds
     * @param isPassive Initial active state
     * @param canToggle Whether blessing can be toggled
     * @param textColor Color for blessing text
     */
    public CrushingBlow(int neededFavor, int cooldown, boolean isPassive, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isPassive, isActive, canToggle, textColor);
    }

    /**
     * Creates a default instance of this blessing with an initialized hit counter
     *
     * @param <T> Type of blessing instance
     * @return New blessing instance with hit counter
     */
    @Override
    public <T extends BlessingsInstance> T createDefaultInstance() {
        T instance = super.createDefaultInstance();
        if (!instance.getOrCreateTag().contains("hitCounter"))
            instance.getOrCreateTag().put("hitCounter", new CompoundTag());
        return instance;
    }

    /**
     * Handles damage events, tracking consecutive hits on targets
     * After 6 hits on the same target, increases damage and applies knockback
     *
     * @param instance The blessing instance
     * @param player The player dealing damage
     * @param event The damage event
     * @return True if the special effect was applied
     */
    @Override
    public boolean onDamageEntity(BlessingsInstance instance, LivingEntity player, LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        String targetId = target.getUUID().toString();
        CompoundTag hitCounter = instance.getOrCreateTag().getCompound("hitCounter");

        if (instance.isOnCooldown()) {
            incrementHits(hitCounter, targetId);
            return false;
        }

        int hits = incrementHits(hitCounter, targetId);

        if (hits >= 6) {
            event.setAmount(event.getAmount() * 2.5f);
            target.knockback(2.0D, player.getX() - target.getX(), player.getZ() - target.getZ());
            instance.setCooldown(600);
            hitCounter.putInt(targetId, 0);
            return true;
        }

        return false;
    }

    /**
     * Increments and returns the hit counter for a specific target
     *
     * @param hitCounter The NBT tag storing hit counts
     * @param targetId The UUID of the target entity
     * @return The new hit count for the target
     */
    private int incrementHits(CompoundTag hitCounter, String targetId) {
        int hits = hitCounter.getInt(targetId) + 1;
        hitCounter.putInt(targetId, hits);
        return hits;
    }
}