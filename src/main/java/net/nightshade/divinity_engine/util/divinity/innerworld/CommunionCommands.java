package net.nightshade.divinity_engine.util.divinity.innerworld;

import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;

public final class CommunionCommands {
    public static void register(RegisterCommandsEvent e){
        var communion=Commands.literal("commune").requires(s->s.hasPermission(2))
            .then(Commands.literal("status").requires(s->s.hasPermission(2)).executes(c->{c.getSource().sendSuccess(()->Component.literal(CommunionManager.diagnostics(c.getSource().getServer())),false);return 1;}))
            .then(Commands.literal("end").requires(s->s.hasPermission(2)).then(Commands.argument("player",EntityArgument.player()).executes(c->{CommunionManager.end(EntityArgument.getPlayer(c,"player"),CommunionLedger.Exit.ADMIN);return 1;})));
        e.getDispatcher().register(Commands.literal("divinity_engine").then(communion));
    }
    private CommunionCommands(){}
}
