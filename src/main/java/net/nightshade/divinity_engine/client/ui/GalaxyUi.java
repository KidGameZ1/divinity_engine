package net.nightshade.divinity_engine.client.ui;

import net.minecraft.resources.ResourceLocation;
import net.nightshade.divinity_engine.registry.gui.DivinityUiTypes;
import net.nightshade.renderlib.client.ui.UiDefinitionRepository;
import net.nightshade.renderlib.ui.UiNode;
import net.nightshade.renderlib.ui.UiSkin;
import net.nightshade.renderlib.ui.UiStyle;
import net.nightshade.renderlib.ui.UiTheme;
import net.nightshade.renderlib.ui.UiValue;

import java.util.Map;
import java.util.Optional;

/** Celestial menu theme; the Codex retains its parchment appearance. */
public final class GalaxyUi {
    public static final int TEXT = 0xFFE4EAF1;
    public static final int MUTED = 0xFF9AAABD;
    public static final int GOLD = 0xFF9CBBD4;
    private static final ResourceLocation CONTROLS = new ResourceLocation("divinity_engine", "blessings_controls");
    private static final ResourceLocation SEARCH = new ResourceLocation("divinity_engine", "blessings_search");
    private static final UiTheme[] THEMES = new UiTheme[8];
    private static final UiTheme[] SEARCH_THEMES = new UiTheme[2];

    static {
        for (int i = 0; i < THEMES.length; i++) {
            boolean selected = (i & 1) != 0, focused = (i & 2) != 0, active = (i & 4) != 0;
            THEMES[i] = theme(!active ? 0xFF101825 : selected ? 0xFF24394D : focused ? 0xFF233246 : 0xFF162131,
                    active ? TEXT : 0xFF7C91A7,
                    !active ? 0xFF263243 : focused ? 0xFFC1D6E8 : selected ? 0xFF8CB8D6 : 0xFF3B5067);
        }
        SEARCH_THEMES[0] = theme(0xFF0B121E, TEXT, 0xFF3B5067);
        SEARCH_THEMES[1] = theme(0xFF0B121E, TEXT, GOLD);
    }

    private GalaxyUi() {}

    public static UiNode background(int x, int y, int width, int height, int godColor) {
        return UiNode.builder("galaxy", DivinityUiTypes.GALAXY)
                .style(UiStyle.builder().size(width, height).clip(true).build())
                .property("x", UiValue.integer(x)).property("y", UiValue.integer(y))
                .property("god_color", UiValue.integer(godColor)).build();
    }

    public static UiTheme controls(boolean selected, boolean focused, boolean active) {
        return UiDefinitionRepository.INSTANCE.theme(CONTROLS)
                .orElse(THEMES[(selected ? 1 : 0) | (focused ? 2 : 0) | (active ? 4 : 0)]);
    }

    public static UiTheme search(boolean focused) {
        return UiDefinitionRepository.INSTANCE.theme(SEARCH).orElse(SEARCH_THEMES[focused ? 1 : 0]);
    }

    public static UiNode ornament(String key, ResourceLocation type, int x, int y, int width, int height,
                                  String god, int rail, int rows) {
        return UiNode.builder(key, type).style(UiStyle.builder().size(width, height).clip(true).build())
                .property("x", UiValue.integer(x)).property("y", UiValue.integer(y))
                .property("god", UiValue.text(god)).property("rail", UiValue.integer(rail))
                .property("rows", UiValue.integer(rows)).build();
    }

    private static UiTheme theme(int background, int foreground, int border) {
        return new UiTheme(new UiSkin(Optional.empty(), Optional.of(background), Optional.of(foreground),
                Optional.of(border), Optional.of(0xFF86C9DF), Optional.of(1), Optional.empty()), Map.of(), Map.of());
    }
}
