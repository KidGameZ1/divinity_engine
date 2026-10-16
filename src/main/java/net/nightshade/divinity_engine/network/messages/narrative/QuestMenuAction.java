package net.nightshade.divinity_engine.network.messages.narrative;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.nightshade.divinity_engine.util.divinity.narrative.DivineNarrativeManager;
import net.nightshade.divinity_engine.util.divinity.narrative.DivineNarrativeRegistry;
import net.nightshade.divinity_engine.world.inventory.BlessingsMenu;
import java.util.Set;
import java.util.function.Supplier;

/** Requests an existing server action, never objective progress or a client-chosen reward. */
public record QuestMenuAction(int containerId, ResourceLocation story, String action, String branch, String token) {
    public QuestMenuAction(int containerId, ResourceLocation story, String action) {
        this(containerId, story, action, "", "");
    }
    public static void encode(QuestMenuAction message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.containerId); buffer.writeResourceLocation(message.story); buffer.writeUtf(message.action, 16);
        buffer.writeUtf(message.branch, 256); buffer.writeUtf(message.token, 36);
    }
    public static QuestMenuAction decode(FriendlyByteBuf buffer) {
        return new QuestMenuAction(buffer.readVarInt(), buffer.readResourceLocation(), buffer.readUtf(16), buffer.readUtf(256), buffer.readUtf(36));
    }
    public static void handle(QuestMenuAction message, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get(); context.enqueueWork(() -> message.apply(context.getSender())); context.setPacketHandled(true);
    }
    public boolean apply(ServerPlayer player) {
        if (player == null || !(player.containerMenu instanceof BlessingsMenu menu) || menu.containerId != containerId
                || !menu.stillValid(player) || !menu.isOpenedFromStatue()
                || !Set.of("request", "status", "donate", "finish", "preview", "confirm", "crossover").contains(action)) return false;
        var definition = DivineNarrativeRegistry.byId(story);
        if (definition == null || !menu.allowsGod(definition.godId())
                || !DivineNarrativeManager.contacted(player, definition.godId())
                || DivineNarrativeManager.nearestStatue(player, definition.godId()) == null) return false;
        ResourceLocation target = story;
        if (action.equals("crossover")) {
            target = ResourceLocation.tryParse(branch);
            var relation = target == null ? null : DivineNarrativeRegistry.relationship(target);
            if (relation == null || !relation.gods().contains(definition.godId())) return false;
        } else if (action.equals("preview") || action.equals("confirm")) {
            if (!Set.of("a", "b").contains(branch)) return false;
        } else if (!branch.isEmpty() || !token.isEmpty()) return false;
        if (!menu.takeStoryAction(player.server.overworld().getGameTime())) return false;
        boolean accepted = action.equals("status") || DivineNarrativeManager.action(player, target, action, branch, token) != 0;
        NarrativeNetwork.dialogue(player, containerId,
                net.nightshade.divinity_engine.util.divinity.narrative.ShrineStoryDialogue.build(player, story));
        return accepted;
    }
}
