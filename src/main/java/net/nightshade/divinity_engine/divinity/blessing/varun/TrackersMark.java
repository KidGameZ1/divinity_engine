package net.nightshade.divinity_engine.divinity.blessing.varun;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.nightshade_core.util.MiscHelper;

import java.awt.*;
import java.util.List;

public class TrackersMark extends Blessings {

    public TrackersMark(int neededFavor, int cooldown, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isActive, canToggle, textColor);
    }

    @Override
    public <T extends BlessingsInstance> T createDefaultInstance() {
        T instance = super.createDefaultInstance();
        if (!instance.getOrCreateTag().contains("reveal_range"))
            instance.getOrCreateTag().putDouble("reveal_range", 16.0d);
        if (!instance.getOrCreateTag().contains("player_reveal_range"))
            instance.getOrCreateTag().putDouble("player_reveal_range", 8.0d);
        return instance;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean onTick(BlessingsInstance instance, LivingEntity living) {
        MiscHelper.executeClientOnlyCode(()->{
            AABB playerSearchArea = living.getBoundingBox().inflate(getPlayerRevealRange(instance));
            AABB mobSearchArea = living.getBoundingBox().inflate(getRevealRange(instance));

            List<Player> nearbyPlayers = living.level().getEntitiesOfClass(Player.class, playerSearchArea, entity -> entity != living);
            List<LivingEntity> nearbyMobs = living.level().getEntitiesOfClass(LivingEntity.class, mobSearchArea,
                    entity -> entity != living && !(entity instanceof Player));

            for (Player player : nearbyPlayers) {
                player.setGlowingTag(true);
            }

            for (LivingEntity entity : nearbyMobs) {
                entity.setGlowingTag(true);
            }
        });
        return super.onTick(instance, living);
    }

    public static double getRevealRange(BlessingsInstance instance) {
        return instance.getOrCreateTag().getDouble("reveal_range");
    }
    public static double getPlayerRevealRange(BlessingsInstance instance) {
        return instance.getOrCreateTag().getDouble("player_reveal_range");
    }
}
