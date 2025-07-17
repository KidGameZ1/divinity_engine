package net.nightshade.divinity_engine.divinity.blessing.cryonis;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A blessing that creates an icy pulse effect around the player when dealing damage.
 * Affects nearby entities by slowing their movement and applying a cooldown period.
 */
public class IcyPulse extends Blessings {
    /**
     * Creates a new IcyPulse blessing.
     *
     * @param neededFavor Amount of favor required to activate this blessing
     * @param cooldown    Cooldown period between activations in seconds
     * @param isPassive    Whether this blessing starts active
     * @param canToggle   Whether this blessing can be toggled on/off
     * @param textColor   Color used for blessing text in UI
     */
    public IcyPulse(int neededFavor, int cooldown, boolean isPassive, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isPassive, isActive, canToggle, textColor);
    }

    /**
     * Creates a default instance of the IcyPulse blessing with initial configuration.
     * Sets up default values for effect duration, cooldown period, and effect radius.
     *
     * @return A new blessing instance with default configuration
     */
    @Override
    public <T extends BlessingsInstance> T createDefaultInstance() {
        T instance = super.createDefaultInstance();
        ListTag tag = new ListTag();
        if (!instance.getOrCreateTag().contains("entityCooldowns"))
            instance.getOrCreateTag().put("entityCooldowns", new ListTag());
        if (!instance.getOrCreateTag().contains("effect_duration"))
            instance.getOrCreateTag().putInt("effect_duration", 60);
        if (!instance.getOrCreateTag().contains("cooldown"))
            instance.getOrCreateTag().putInt("cooldown", 160);
        if (!instance.getOrCreateTag().contains("radius"))
            instance.getOrCreateTag().putDouble("radius", 3.0D);

        instance.getOrCreateTag().put("entityCooldowns", tag);
        return instance;
    }

    /**
     * Triggered when the player damages an entity. Creates an icy pulse effect that slows nearby entities.
     *
     * @param instance The blessing instance
     * @param player   The player using the blessing
     * @param event    The damage event
     * @return True if the event was handled
     */
    @Override
    public boolean onDamageEntity(BlessingsInstance instance, LivingEntity player, LivingHurtEvent event) {
        if (event.getSource().getDirectEntity() == player) {
            long currentTime = player.level().getGameTime();
            CompoundTag tag = instance.getOrCreateTag();
            int effectDuration = tag.getInt("effect_duration");
            int cooldown = tag.getInt("cooldown");
            double radius = tag.getDouble("radius");
            ListTag cooldownsTag = tag.getList("entityCooldowns", 10);

            player.level().getEntitiesOfClass(LivingEntity.class,
                            player.getBoundingBox().inflate(radius),
                            entity -> entity != player && !hasEntityCooldown(cooldownsTag, entity.getUUID()))
                    .forEach(target -> {
                        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, effectDuration, 0));
                        addEntityCooldown(cooldownsTag, target.getUUID(), currentTime + cooldown);

                        if (player.level() instanceof ServerLevel serverLevel) {
                            serverLevel.sendParticles(ParticleTypes.SNOWFLAKE,
                                    target.getX(), target.getY() + 1, target.getZ(),
                                    10, 0.5, 0.5, 0.5, 0.1);
                        }
                    });

            removeExpiredCooldowns(cooldownsTag, currentTime);
            return true;
        }
        return false;
    }

    /**
     * Checks if an entity is currently affected by the cooldown.
     *
     * @param tag      The tag containing cooldown data
     * @param entityId The UUID of the entity to check
     * @return True if the entity has an active cooldown
     */
    private boolean hasEntityCooldown(ListTag tag, UUID entityId) {
        for (int i = 0; i < tag.size(); i++) {
            CompoundTag entry = tag.getCompound(i);
            if (entry.getUUID("id").equals(entityId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Adds a cooldown entry for an entity.
     *
     * @param tag        The tag to store cooldown data
     * @param entityId   The UUID of the affected entity
     * @param expireTime The game time when the cooldown expires
     */
    private void addEntityCooldown(ListTag tag, UUID entityId, long expireTime) {
        CompoundTag entry = new CompoundTag();
        entry.putUUID("id", entityId);
        entry.putLong("expireTime", expireTime);
        tag.add(entry);
    }

    /**
     * Removes expired cooldown entries.
     *
     * @param tag         The tag containing cooldown data
     * @param currentTime The current game time
     */
    private void removeExpiredCooldowns(ListTag tag, long currentTime) {
        tag.removeIf(nbt -> ((CompoundTag) nbt).getLong("expireTime") <= currentTime);
    }
}