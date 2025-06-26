package net.nightshade.divinity_engine.network.cap;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.ChunkWatchEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.nightshade.divinity_engine.DivinityEngineMod;
import net.nightshade.divinity_engine.network.cap.player.gods.IMainPlayerCapability;
import net.nightshade.divinity_engine.network.cap.player.gods.MainPlayerCapability;
import org.jetbrains.annotations.Nullable;

import static net.nightshade.divinity_engine.network.cap.player.gods.MainPlayerCapability.*;


@Mod.EventBusSubscriber(
    modid = DivinityEngineMod.MODID,
    bus = Mod.EventBusSubscriber.Bus.FORGE
)
public class CapabilityHandler {
    public CapabilityHandler() {
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent e) {
        e.register(IMainPlayerCapability.class);
    }

    @SubscribeEvent
    static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent e) {
        PlayerVariables.sync(e.getEntity());
//        TechniqueLoadoutGUIVariables.checkForFirstLogin(e.getEntity());
    }

    @SubscribeEvent
    static void onPlayerTrack(PlayerEvent.StartTracking e) {
        Entity var2 = e.getTarget();
        if (var2 instanceof Player player) {
            PlayerVariables.sync(player);
            PlayerVariables.sync(e.getEntity());
        }

    }

    @SubscribeEvent
    static void onPlayerClone(PlayerEvent.Clone e) {
        e.getOriginal().reviveCaps();
        PlayerVariables.getFrom(e.getOriginal()).ifPresent((oldData) -> PlayerVariables.getFrom(e.getEntity()).ifPresent((data) -> {
                data.deserializeNBT((CompoundTag)oldData.serializeNBT());
            }));
        if (!e.isWasDeath()) {

            for(MobEffectInstance instance : e.getOriginal().getActiveEffects()) {
                e.getEntity().addEffect(new MobEffectInstance(instance));
            }

            e.getEntity().refreshDimensions();
        }

        e.getOriginal().invalidateCaps();
    }

    @SubscribeEvent
    static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent e) {
//        TechniqueLoadoutGUIVariables.checkForFirstLogin(e.getEntity());
        PlayerVariables.sync(e.getEntity());
        if (!e.isEndConquered()) {
        }
    }

    @SubscribeEvent
    static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        PlayerVariables.sync(e.getEntity());
    }

    @Nullable
    public static <T> T getCapability(Entity entity, Capability<T> capability) {
        return (T)(entity.getCapability(capability).isPresent() ? entity.getCapability(capability).orElseThrow(() -> new IllegalArgumentException("Lazy optional must not be empty")) : null);
    }

    @SubscribeEvent
    static void onWatchChunk(ChunkWatchEvent.Watch e) {
    }
}