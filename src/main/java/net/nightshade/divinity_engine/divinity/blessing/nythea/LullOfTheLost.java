package net.nightshade.divinity_engine.divinity.blessing.nythea;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;

import java.awt.*;
import java.util.Random;
import java.util.UUID;

public class LullOfTheLost extends Blessings {
    private final Random random = new Random();

    public LullOfTheLost(int neededFavor, int cooldown, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isActive, canToggle, textColor);
    }

    @Override
    public <T extends BlessingsInstance> T createDefaultInstance() {
        T instance = super.createDefaultInstance();
        if (!instance.getOrCreateTag().contains("entityCooldowns"))
            instance.getOrCreateTag().put("entityCooldowns", new ListTag());
        return instance;
    }

    @Override
    public boolean onBeingTargeted(BlessingsInstance instance, LivingEntity target, LivingChangeTargetEvent event) {
        UUID entityId = target.getUUID();
        long currentTime = System.currentTimeMillis();
        Level level = target.level();
        boolean isNight = !level.isDay();
        ListTag cooldowns = instance.getOrCreateTag().getList("entityCooldowns", 10);

        if (isNight) {
            boolean hasCooldown = false;
            for (int i = 0; i < cooldowns.size(); i++) {
                if (cooldowns.getCompound(i).getUUID("id").equals(entityId)) {
                    if (currentTime - cooldowns.getCompound(i).getLong("time") < 20000) {
                        hasCooldown = true;
                    }
                    break;
                }
            }

            if (!hasCooldown && random.nextFloat() <= 0.35f) {
                event.setNewTarget(null);
                CompoundTag tag = new CompoundTag();
                tag.putUUID("id", entityId);
                tag.putLong("time", currentTime);
                cooldowns.add(tag);

                int duration = 1500 + random.nextInt(500);
                new Thread(() -> {
                    try {
                        Thread.sleep(duration);
                        event.setNewTarget(event.getOriginalTarget());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();

                return true;
            }
        }

        return super.onBeingTargeted(instance, target, event);
    }
}