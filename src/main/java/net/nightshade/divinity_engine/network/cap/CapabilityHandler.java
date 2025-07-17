package net.nightshade.divinity_engine.network.cap;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.ChunkWatchEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.nightshade.divinity_engine.DivinityEngineMod;
import net.nightshade.divinity_engine.network.cap.player.gods.IMainPlayerCapability;
import net.nightshade.divinity_engine.network.cap.player.gods.MainPlayerCapability;
import net.nightshade.divinity_engine.util.DivinityEngineHelper;
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

    private static void playerSave(PlayerEvent.SaveToFile event) {
        Player player = event.getEntity();
        if (player instanceof ServerPlayer) {
            if (DivinityEngineHelper.getBlessingSlot1(player) != null) {
                System.out.println("Saving player " + player.getName().getString() + " blessing slot 1 data: " + DivinityEngineHelper.getBlessingSlot1(player).toNBT());
            }
            if (DivinityEngineHelper.getBlessingSlot2(player) != null) {
                System.out.println("Saving player " + player.getName().getString() + " blessing slot 2 data: " + DivinityEngineHelper.getBlessingSlot2(player).toNBT());
            }
            if (DivinityEngineHelper.getBlessingSlot3(player) != null) {
                System.out.println("Saving player " + player.getName().getString() + " blessing slot 3 data: " + DivinityEngineHelper.getBlessingSlot3(player).toNBT());
            }
            MainPlayerCapability.PlayerVariables.getFrom(player).ifPresent((data) -> {
                data.setBlessingsPageNum(0);
            });
        }
    }

    public static void init(IEventBus modEventBus) {
        IEventBus forgeEventBus = MinecraftForge.EVENT_BUS;
        forgeEventBus.addListener(CapabilityHandler::playerSave);

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