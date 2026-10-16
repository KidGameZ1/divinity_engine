package net.nightshade.divinity_engine.client.ui;

import net.minecraft.resources.ResourceLocation;
import net.nightshade.renderlib.client.ui.UiDefinitionRepository;
import net.nightshade.renderlib.ui.UiNode;
import net.nightshade.renderlib.ui.UiSkin;
import net.nightshade.renderlib.ui.UiStyle;
import net.nightshade.renderlib.ui.UiTheme;
import net.nightshade.renderlib.ui.UiValue;

import java.util.Map;
import java.util.Optional;

/** Shared Nightshade presentation. Minecraft still owns container slots and text editing. */
public final class DivinityUi {
    public static final int INK = 0xFF293D49;
    public static final int MUTED = 0xFF62635D;
    public static final ResourceLocation BOOK = new ResourceLocation("divinity_engine", "textures/screens/divine_codex.png");

    private DivinityUi() {}

    public static UiNode.Builder canvas(String key) {
        return UiNode.builder(key, "canvas");
    }

    public static UiNode.Builder at(String key, String type, int x, int y, int width, int height) {
        return UiNode.builder(key, type).style(UiStyle.builder().size(width, height).build())
                .property("x", UiValue.integer(x)).property("y", UiValue.integer(y));
    }

    public static UiNode panel(String key, int x, int y, int width, int height, int color) {
        return at(key, "panel", x, y, width, height)
                .style(UiStyle.builder().size(width, height).background(color).build()).build();
    }

    public static UiNode text(String key, String text, int x, int y, int width, int height, int color) {
        return at(key, "text", x, y, width, height).text(text)
                .style(UiStyle.builder().size(width, height).foreground(color).build()).build();
    }

    public static UiNode book(int x, int y, int width, int height) {
        return at("book", "nine_slice", x, y, width, height)
                .property("texture", UiValue.id(BOOK)).property("border", UiValue.integer(12)).build();
    }

    public static UiTheme controlTheme(boolean selected, boolean focused, boolean active) {
        UiSkin skin = new UiSkin(Optional.empty(),
                Optional.of(selected ? 0xFFDCE4E8 : focused ? 0xFFE9E7DF : 0x28D6CFBC),
                Optional.of(active ? INK : 0xFF817D74),
                Optional.of(!active ? 0x40B4AA97 : focused ? 0xFF516D86 : selected ? 0xFF6D879C : 0x60B4AA97),
                Optional.of(0xFF497F93), Optional.of(1), Optional.empty());
        return UiDefinitionRepository.INSTANCE.theme(new ResourceLocation("divinity_engine", "controls"))
                .orElse(new UiTheme(skin, Map.of(), Map.of()));
    }

    public static UiTheme searchTheme(boolean focused) {
        UiSkin skin = new UiSkin(Optional.empty(), Optional.of(0xFF293D49), Optional.empty(),
                Optional.of(focused ? 0xFF79AFBF : 0xFF849797), Optional.empty(), Optional.of(1), Optional.empty());
        return UiDefinitionRepository.INSTANCE.theme(new ResourceLocation("divinity_engine", "search"))
                .orElse(new UiTheme(skin, Map.of(), Map.of()));
    }
}
