package net.nightshade.divinity_engine.network.messages.cap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

import java.util.function.Consumer;

final class ClientLevelAccessor {
    static void execute(Consumer<ClientLevel> action) {
        action.accept(Minecraft.getInstance().level);
    }
}