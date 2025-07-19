package net.nightshade.divinity_engine.api.divine_codex_pages;

import net.minecraft.network.chat.Component;

public interface IDivineCodexPageRegistrar {
    void addPage(Component title, Component content);
}
