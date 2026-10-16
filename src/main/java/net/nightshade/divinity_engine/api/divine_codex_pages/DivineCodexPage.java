package net.nightshade.divinity_engine.api.divine_codex_pages;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.NonExtendable
public class DivineCodexPage {
    private final Component title;
    private final Component content;
    public DivineCodexPage(Component title, Component content) {
        this.title = title;
        this.content = content;
    }

    public Component getTitle() {
        return title;
    }

    public Component getContent() {
        return content;
    }
}
