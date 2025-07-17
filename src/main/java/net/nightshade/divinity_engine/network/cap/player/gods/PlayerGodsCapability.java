package net.nightshade.divinity_engine.network.cap.player.gods;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.nightshade.divinity_engine.DivinityEngineMod;
import net.nightshade.divinity_engine.network.cap.GenericCapabilityProvider;
import org.jetbrains.annotations.ApiStatus;


@ApiStatus.Internal
public class PlayerGodsCapability {
    static final Capability<InternalGodsStorage> CAP = CapabilityManager.get(new CapabilityToken<InternalGodsStorage>() {
    });
    private static final ResourceLocation ID = new ResourceLocation(DivinityEngineMod.MODID, "contacted_gods_storage");

    private static void register(RegisterCapabilitiesEvent e) {
        e.register(InternalGodsStorage.class);
    }

    private static void attach(AttachCapabilitiesEvent<Player> e) {
        e.addCapability(ID, new GenericCapabilityProvider(CAP, PlayerGodsCapabilityStorage::new));
    }

    private static void login(PlayerEvent.PlayerLoggedInEvent e) {
        Player var2 = e.getEntity();
        if (var2 instanceof ServerPlayer player) {
            System.out.println("Player logged in: " + player.getName().getString());
            load(player).syncAll();
        }

    }

    private static void clonePlayer(PlayerEvent.Clone e) {
        Player var2 = e.getEntity();
        if (var2 instanceof ServerPlayer player) {
            e.getOriginal().reviveCaps();
            InternalGodsStorage magicStorage = load(e.getOriginal());
            InternalGodsStorage newStorage = load(player);
            newStorage.deserializeNBT((CompoundTag)magicStorage.serializeNBT());
            e.getOriginal().invalidateCaps();
            newStorage.syncAll();
        }
    }


    private static void respawn(PlayerEvent.PlayerRespawnEvent e) {
        Player var2 = e.getEntity();
        if (var2 instanceof ServerPlayer player) {
            load(player).syncAll();
        }

    }

    private static void changeDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        Player var2 = e.getEntity();
        if (var2 instanceof ServerPlayer player) {
            load(player).syncAll();
        }

    }

    private static void startTracking(PlayerEvent.StartTracking e) {
        load(e.getTarget()).syncPlayer(e.getEntity());
    }

    private static void playerSave(PlayerEvent.SaveToFile event) {
        Player player = event.getEntity();
        if (player instanceof ServerPlayer) {
            player.getCapability(CAP).ifPresent(storage -> {
                CompoundTag tag = storage.serializeNBT();
                System.out.println("Saving player " + player.getName().getString() + " gods data: " + tag);
            });
        }
    }

    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(PlayerGodsCapability::register);
        IEventBus forgeEventBus = MinecraftForge.EVENT_BUS;
        forgeEventBus.addGenericListener(Entity.class, PlayerGodsCapability::attach);
        forgeEventBus.addListener(PlayerGodsCapability::login);
        forgeEventBus.addListener(PlayerGodsCapability::clonePlayer);
        forgeEventBus.addListener(PlayerGodsCapability::respawn);
        forgeEventBus.addListener(PlayerGodsCapability::changeDimension);
        forgeEventBus.addListener(PlayerGodsCapability::startTracking);
        forgeEventBus.addListener(PlayerGodsCapability::playerSave);

    }

    public static InternalGodsStorage load(Entity entity) {
        InternalGodsStorage cap = (InternalGodsStorage)entity.getCapability(CAP).orElseGet(PlayerGodsCapabilityStorage::new);
        cap.setOwner(entity);
        return cap;
    }
}