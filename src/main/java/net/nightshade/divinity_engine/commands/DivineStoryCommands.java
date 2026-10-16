package net.nightshade.divinity_engine.commands;

import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.nightshade.divinity_engine.network.cap.player.narrative.DivineNarrativeCapability;
import net.nightshade.divinity_engine.util.divinity.narrative.DivineNarrativeManager;

public final class DivineStoryCommands {
    public static void register(RegisterCommandsEvent event) {
        // Gameplay uses validated menu packets. Only destructive administrator recovery stays a command.
        var story = Commands.literal("story").requires(source -> source.hasPermission(2));
        story.then(Commands.literal("reset").requires(source -> source.hasPermission(2))
            .then(Commands.argument("player", EntityArgument.player()).then(Commands.argument("god", ResourceLocationArgument.id()).executes(ctx -> {
                var player = EntityArgument.getPlayer(ctx, "player"); var data = DivineNarrativeCapability.get(player);
                if (data == null) return 0;
                data.adminReset(ResourceLocationArgument.getId(ctx, "god")); DivineNarrativeManager.sync(player);
                ctx.getSource().sendSuccess(() -> Component.literal("Narrative memory, ending and specialization reset for this player and god."), true); return 1;
            }))));
        event.getDispatcher().register(Commands.literal("divinity_engine").then(story));
    }
}
