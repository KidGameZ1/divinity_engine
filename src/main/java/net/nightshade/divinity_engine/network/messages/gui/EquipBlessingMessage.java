package net.nightshade.divinity_engine.network.messages.gui;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.network.cap.player.gods.MainPlayerCapability;
import net.nightshade.divinity_engine.util.DivinityEngineHelper;
import net.nightshade.divinity_engine.util.divinity.gods.GodHelper;
import net.nightshade.divinity_engine.world.inventory.BlessingsMenu;

import java.util.function.Supplier;

/** Stable IDs and an explicit desired state make delayed/repeated tab clicks harmless. */
public record EquipBlessingMessage(int containerId, ResourceLocation godId, ResourceLocation blessingId,
                                   int slot, boolean equip) {
    public EquipBlessingMessage(FriendlyByteBuf buffer) {
        this(buffer.readVarInt(), buffer.readResourceLocation(), buffer.readResourceLocation(),
                buffer.readVarInt(), buffer.readBoolean());
    }

    public static void encode(EquipBlessingMessage message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.containerId);
        buffer.writeResourceLocation(message.godId);
        buffer.writeResourceLocation(message.blessingId);
        buffer.writeVarInt(message.slot);
        buffer.writeBoolean(message.equip);
    }

    public static void handle(EquipBlessingMessage message, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> {
            if (message.apply(context.getSender())) MainPlayerCapability.PlayerVariables.sync(context.getSender());
        });
        context.setPacketHandled(true);
    }

    public boolean apply(ServerPlayer player) {
        if (player == null || !player.isAlive() || slot < 0 || slot > 2
                || !(player.containerMenu instanceof BlessingsMenu menu) || menu.containerId != containerId
                || !menu.stillValid(player) || !menu.allowsGod(godId)) return false;
        BlessingsInstance current = DivinityEngineHelper.getBlessingInSlot(player, slot);
        if (!equip) {
            if (!matches(current)) return false;
            DivinityEngineHelper.setBlessingSlot(player, slot, null);
            return true;
        }
        var selected = GodHelper.getAllBlessingsInstances(player).stream().filter(this::matches).findFirst().orElse(null);
        if (selected == null || matches(current)) return false;
        var god = GodHelper.getGodOrNull(player, selected.getBoundGod());
        if (god == null || !net.nightshade.divinity_engine.util.divinity.gods.DivineRelationship.canUse(player, selected)) return false;
        for (int i = 0; i < 3; i++) {
            var equipped = DivinityEngineHelper.getBlessingInSlot(player, i);
            if (equipped != null && equipped.getBlessing().getRegistryName().equals(blessingId)) return false;
        }
        DivinityEngineHelper.setBlessingSlot(player, slot, selected);
        return true;
    }

    private boolean matches(BlessingsInstance blessing) {
        return blessing != null && blessing.getBoundGod().getRegistryName().equals(godId)
                && blessing.getBlessing().getRegistryName().equals(blessingId);
    }
}
