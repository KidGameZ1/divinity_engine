package net.nightshade.divinity_engine.network.messages;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.nightshade.divinity_engine.network.cap.player.gods.MainPlayerCapability;

public class ClientAccess {
    static final Minecraft minecraft = Minecraft.getInstance();

    ClientAccess() {
    }

    public static void updatePlayerCapability(int entityId, CompoundTag tag) {
        if (minecraft.level != null) {
            Entity entity = minecraft.level.getEntity(entityId);
            if (entity instanceof Player) {
                Player player = (Player) entity;
                MainPlayerCapability.PlayerVariables.getFrom(player).ifPresent((data) -> data.deserializeNBT(tag));
                player.refreshDimensions();
            }

        }
    }
}