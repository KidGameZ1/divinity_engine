package net.nightshade.divinity_engine.client.effects;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.nightshade.divinity_engine.DivinityEngineMod;
import net.nightshade.divinity_engine.network.messages.effects.PrivateBlessingEffectsPacket;
import net.nightshade.renderlib.api.RenderEffectInstance;
import net.nightshade.renderlib.api.RenderEffects;
import net.nightshade.renderlib.api.RenderHandle;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Main-thread private handle ownership; Core still owns all rendering and simulation. */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = DivinityEngineMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class PrivateBlessingEffectsClient {
    private static final int MAX_HANDLES = 64;
    private record LocalEffect(RenderHandle handle, long expires) { }
    private static final Map<UUID, LocalEffect> ACTIVE = new HashMap<>();
    private static ClientLevel currentLevel;

    private PrivateBlessingEffectsClient() { }

    public static void apply(PrivateBlessingEffectsPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.isSameThread()) return;
        synchronizeLevel(minecraft.level);
        if (currentLevel == null || !currentLevel.dimension().location().equals(packet.dimension())) return;
        long now = currentLevel.getGameTime();
        prune(now);
        for (UUID id : packet.removals()) remove(id);
        if (packet.effects().isEmpty() || minecraft.player == null || !minecraft.player.isAlive()) return;
        if (ACTIVE.size() + packet.effects().size() > MAX_HANDLES) return;
        for (RenderEffectInstance effect : packet.effects()) {
            if (ACTIVE.containsKey(effect.id()) || remaining(effect, now) <= 0) return;
        }

        try {
            for (RenderEffectInstance effect : packet.effects()) {
                long lifetime = remaining(effect, now);
                RenderHandle handle = RenderEffects.spawnLocal(effect.spec().toBuilder().duration(lifetime).build());
                ACTIVE.put(effect.id(), new LocalEffect(handle, now + lifetime));
            }
        } catch (RuntimeException unavailable) {
            // Roll back this packet only, without touching unrelated local or synchronized effects.
            for (RenderEffectInstance effect : packet.effects()) remove(effect.id());
            DivinityEngineMod.LOGGER.debug("Skipped private blessing visuals", unavailable);
        }
    }

    private static long remaining(RenderEffectInstance effect, long now) {
        return Math.min(effect.spec().durationTicks(), effect.endGameTime() - now);
    }

    private static void remove(UUID id) {
        LocalEffect effect = ACTIVE.remove(id);
        if (effect != null) RenderEffects.removeLocal(effect.handle());
    }

    private static void prune(long now) {
        ACTIVE.values().removeIf(effect -> {
            if (effect.expires() > now) return false;
            RenderEffects.removeLocal(effect.handle());
            return true;
        });
    }

    private static void synchronizeLevel(ClientLevel level) {
        if (currentLevel != level) {
            clear();
            currentLevel = level;
        }
    }

    private static void clear() {
        ACTIVE.values().forEach(effect -> RenderEffects.removeLocal(effect.handle()));
        ACTIVE.clear();
        currentLevel = null;
    }

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        synchronizeLevel(minecraft.level);
        if (currentLevel == null) return;
        if (minecraft.player == null || !minecraft.player.isAlive()) clear();
        else prune(currentLevel.getGameTime());
    }

    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { clear(); }

    @SubscribeEvent public static void unload(LevelEvent.Unload event) {
        if (event.getLevel() == currentLevel) clear();
    }
}
