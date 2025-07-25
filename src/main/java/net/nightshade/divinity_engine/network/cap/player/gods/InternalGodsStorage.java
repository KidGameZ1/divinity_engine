package net.nightshade.divinity_engine.network.cap.player.gods;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.PacketDistributor;

import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.divinity.curse.Curse;
import net.nightshade.divinity_engine.divinity.curse.CurseInstance;
import net.nightshade.divinity_engine.divinity.gods.BaseGod;
import net.nightshade.divinity_engine.divinity.gods.BaseGodInstance;
import net.nightshade.divinity_engine.network.messages.ModMessages;
import net.nightshade.divinity_engine.network.messages.cap.SyncGodsPacket;
import net.nightshade.divinity_engine.registry.divinity.curse.CurseRegistry;
import net.nightshade.divinity_engine.registry.divinity.gods.GodsRegistry;
import org.jetbrains.annotations.ApiStatus.Internal;

import java.util.List;

import static net.nightshade.divinity_engine.network.messages.cap.SyncGodsPacket.*;


@Internal
public interface InternalGodsStorage extends GodsStorage {
    default List<BaseGodInstance> getDirtyContactedGods() {
        return this.getContactedGods().parallelStream().filter(BaseGodInstance::isDirty).toList();
    }

    default List<CurseInstance> getDirtyCurses() {
        return this.getCurses().parallelStream().filter(CurseInstance::isDirty).toList();
    }

    void setOwner(Entity var1);

    Entity getOwner();

    default void sync(SyncType syncType) {
        if (this.getOwner() != null) {
            if (!this.getOwner().level().isClientSide()) {
                if (this.getOwner() instanceof ServerPlayer) {
                    ModMessages.INSTANCE.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(this::getOwner), new SyncGodsPacket(this.getOwner(), this, syncType));
                } else {
                    ModMessages.INSTANCE.send(PacketDistributor.TRACKING_ENTITY.with(this::getOwner), new SyncGodsPacket(this.getOwner(), this, syncType));
                }

            }
        }
    }

    default void syncAll() {
        this.sync(SyncType.FULL);
    }

    default void syncChanges() {
        this.sync(SyncType.CHANGES_ONLY);
    }

    default void syncPlayer(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            ModMessages.INSTANCE.send(PacketDistributor.PLAYER.with(() -> serverPlayer), new SyncGodsPacket(this.getOwner(), this, SyncType.FULL));
        }

    }

    void updateGodInstance(BaseGodInstance var1, boolean var2);

    default void updateGodInstance(BaseGodInstance updatedInstance) {
        this.updateGodInstance(updatedInstance, true);
    }

    void updateBlessingInstance(BaseGodInstance instance, BlessingsInstance updateInstance, boolean sync);

    default void updateGodInstances(List<BaseGodInstance> updatedInstances) {
        updatedInstances.forEach((magicInstance) -> this.updateGodInstance(magicInstance, false));
        this.syncChanges();
    }

    default void updateGodInstances(BaseGodInstance... updatedInstances) {
        for(BaseGodInstance magic : updatedInstances) {
            this.updateGodInstance(magic, false);
        }

        this.syncChanges();
    }

    void updateCurseInstance(CurseInstance var1, boolean var2);

    default void updateCurseInstance(CurseInstance updatedInstance) {
        this.updateCurseInstance(updatedInstance, true);
    }

    default void updateCurseInstances(List<CurseInstance> updatedInstances) {
        updatedInstances.forEach((magicInstance) -> this.updateCurseInstance(magicInstance, false));
        this.syncChanges();
    }

    default void updateCurseInstances(CurseInstance... updatedInstances) {
        for(CurseInstance magic : updatedInstances) {
            this.updateCurseInstance(magic, false);
        }

        this.syncChanges();
    }

    default boolean contactGod(BaseGod god) {
        return this.contactGod(god.createGodDefaultInstance());
    }

    default void loseContactedGod(BaseGod god) {
        this.getContactedGods().removeIf((instance) -> instance.getBaseGodId().equals(GodsRegistry.GODS_REGISTRY.get().getKey(god)));
    }

    default void uncurse(BaseGodInstance magic) {
        this.loseContactedGod(magic.getBaseGod());
    }

    default boolean curse(Curse curse, BaseGod god) {
        return this.curse(curse.createDefaultInstance(), god);
    }

    void loseContactAllGods();

    default void uncurse(Curse god) {
        this.getContactedGods().removeIf((instance) -> instance.getBaseGodId().equals(CurseRegistry.CURSES_REGISTRY.get().getKey(god)));
    }

    default void uncurse(CurseInstance magic) {
        this.uncurse(magic.getCurse());
    }
}
