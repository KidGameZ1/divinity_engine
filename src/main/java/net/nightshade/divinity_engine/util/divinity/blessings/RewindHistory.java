package net.nightshade.divinity_engine.util.divinity.blessings;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals;
import net.nightshade.divinity_engine.util.divinity.narrative.BlessingSpecializations;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/** Short, server-only history; never saved or copied into capability sync packets. */
@Mod.EventBusSubscriber(modid = "divinity_engine")
public final class RewindHistory {
    private static final Map<LivingEntity, Map<Blessings, ArrayDeque<Sample>>> HISTORY = new WeakHashMap<>();
    private record Sample(ServerLevel level, long tick, Vec3 position, float yaw, float pitch) {}

    private RewindHistory() {}

    public static void record(BlessingsInstance instance, LivingEntity living, int delay) {
        if (!(living.level() instanceof ServerLevel level) || !BlessingSpecializations.isEligible(living, instance)) return;
        var samples = HISTORY.computeIfAbsent(living, key -> new HashMap<>())
                .computeIfAbsent(instance.getBlessing(), key -> new ArrayDeque<>());
        long now = level.getGameTime();
        Sample last = samples.peekLast();
        // Unequipping, reconnecting, dimension travel and large teleports invalidate an old trail.
        if (last != null && (last.level != level || now < last.tick || now - last.tick > 4
                || last.position.distanceToSqr(living.position()) > 256)) samples.clear();
        if (!samples.isEmpty() && samples.peekLast().tick == now) return;
        samples.addLast(new Sample(level, now, living.position(), living.getYRot(), living.getXRot()));
        while (samples.size() > delay + 3 || samples.peekFirst().tick < now - delay - 2) samples.removeFirst();
    }

    public static boolean rewind(BlessingsInstance instance, LivingEntity living, int delay) {
        if (!(living instanceof ServerPlayer player) || !player.isAlive() || player.isPassenger()
                || !BlessingSpecializations.isEligible(player, instance)) return false;
        var byBlessing = HISTORY.get(player);
        var samples = byBlessing == null ? null : byBlessing.get(instance.getBlessing());
        long now = player.level().getGameTime();
        if (samples == null || samples.isEmpty() || now - samples.peekLast().tick > 4) return false;
        Sample destination = null;
        for (Sample sample : samples) if (sample.tick <= now - delay) destination = sample;
        if (destination == null || destination.level != player.level() || now - destination.tick > delay + 4) return false;
        Vec3 target = destination.position;
        var box = player.getBoundingBox().move(target.subtract(player.position()));
        if (!player.serverLevel().hasChunksAt(BlockPos.containing(box.minX, box.minY, box.minZ),
                BlockPos.containing(box.maxX, box.maxY, box.maxZ))
                || !player.serverLevel().getWorldBorder().isWithinBounds(box)
                || target.y < player.level().getMinBuildHeight() || box.maxY >= player.level().getMaxBuildHeight()
                || !player.level().noCollision(player, box) || player.level().containsAnyLiquid(box)) return false;
        Vec3 origin = player.position();
        player.teleportTo(player.serverLevel(), target.x, target.y, target.z, destination.yaw, destination.pitch);
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0;
        HISTORY.remove(player);
        if (!player.isInvisible()) BlessingVisuals.link(instance, player, origin, target, BlessingVisuals.Cue.TELEPORT);
        return true;
    }

    @SubscribeEvent public static void travel(PlayerEvent.PlayerChangedDimensionEvent event) { HISTORY.remove(event.getEntity()); }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { HISTORY.remove(event.getEntity()); }
    @SubscribeEvent public static void clone(PlayerEvent.Clone event) { HISTORY.remove(event.getOriginal()); }
    @SubscribeEvent public static void stop(ServerStoppedEvent event) { HISTORY.clear(); }
}
