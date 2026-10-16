package net.nightshade.divinity_engine.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.nightshade.divinity_engine.registry.gui.DivinityUiTypes;
import net.nightshade.renderlib.client.ui.CustomUiNodeRenderContext;
import net.nightshade.renderlib.client.ui.CustomUiNodeRendererRegistry;
import net.nightshade.renderlib.client.ui.RetainedUiTree;
import net.nightshade.renderlib.ui.UiNode;
import net.nightshade.renderlib.ui.UiStyle;
import net.nightshade.renderlib.ui.UiValue;

/** Shader-independent carved stone, rendered through Nightshade's retained UI nodes. */
public final class ShrineUi {
    public static final int TEXT = 0xFFE4E8EB;
    public static final int MUTED = 0xFFAAB5BC;
    public static final int GOLD = 0xFFACC8DC;
    private static final ResourceLocation STONE = new ResourceLocation("minecraft", "textures/block/stone.png");

    private ShrineUi() {}

    static void register() {
        CustomUiNodeRendererRegistry.register(DivinityUiTypes.SHRINE_STONE, ShrineUi::stone);
        CustomUiNodeRendererRegistry.register(DivinityUiTypes.SHRINE_SIGIL, ShrineUi::sigil);
        CustomUiNodeRendererRegistry.register(DivinityUiTypes.SHRINE_CONTROL, ShrineUi::control);
    }

    public static UiNode background(int x, int y, int width, int height, int rail, int color) {
        return node("sanctum_stone", DivinityUiTypes.SHRINE_STONE, x, y, width, height)
                .property("rail", UiValue.integer(rail)).property("accent", UiValue.integer(color)).build();
    }

    public static UiNode sigil(int x, int y, int width, String god, int color) {
        return node("sanctum_sigil", DivinityUiTypes.SHRINE_SIGIL, x, y, width, width)
                .property("god", UiValue.text(god)).property("accent", UiValue.integer(color)).build();
    }

    private static UiNode.Builder node(String key, ResourceLocation type, int x, int y, int width, int height) {
        return UiNode.builder(key, type).style(UiStyle.builder().size(width, height).clip(true).build())
                .property("x", UiValue.integer(x)).property("y", UiValue.integer(y));
    }

    private static int number(CustomUiNodeRenderContext c, String name) {
        return c.node().properties().getOrDefault(name, UiValue.integer(0)).asInteger().orElse(0);
    }

    private static boolean flag(CustomUiNodeRenderContext c, String name) {
        return c.node().properties().getOrDefault(name, UiValue.bool(false)).asBoolean().orElse(false);
    }

    private static int accent(int color) {
        // Temper very dark or saturated god colors so inscriptions remain readable on stone.
        int r = ((color >> 16) & 255) / 3 + 150;
        int g = ((color >> 8) & 255) / 3 + 141;
        int b = (color & 255) / 3 + 120;
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static void stone(CustomUiNodeRenderContext c) {
        var r = c.bounds();
        var g = c.graphics();
        int x = r.x(), y = r.y(), w = r.width(), h = r.height();
        // Vanilla stone at doubled pixel size, bounded by the node's clip rectangle.
        for (int py = y; py < y + h; py += 32) {
            for (int px = x; px < x + w; px += 32) {
                g.blit(STONE, px, py, 32, 32, 0, 0, 16, 16, 16, 16);
            }
        }
        g.fill(x, y, x + w, y + h, 0xC016202C);
        border(g, x, y, w, h, 0xFF697D8B, 0xFF111A24, 2);
        border(g, x + 2, y + 2, w - 4, h - 4, 0xFF263440, 0xFF354653, 1);
        int rail = x + number(c, "rail");
        if (rail > x + 24) {
            g.fill(x + 8, y + 28, rail - 4, y + h - 10, 0x60101A24);
            g.fill(rail - 1, y + 30, rail, y + h - 12, 0xFF394A58);
        }
        g.fill(Math.max(x + 8, rail + 3), y + 28, x + w - 8, y + h - 10, 0xA0101A24);
        g.fill(x + 10, y + 25, x + w - 10, y + 26, 0xFF425563);
    }

    private static void sigil(CustomUiNodeRenderContext c) {
        var r = c.bounds();
        var g = c.graphics();
        int x = r.x(), y = r.y(), w = r.width();
        int centerX = x + w / 2, centerY = y + w / 2;
        bevel(g, x + 4, y + 4, w - 8, w - 8, 0xFF202D39, 0xFF415766, 0xFF111C27);
        g.fill(centerX - 8, y + w - 9, centerX + 8, y + w - 8, accent(number(c, "accent")));
        String god = c.node().properties().getOrDefault("god", UiValue.text("")).value();
        CelestialUiRenderer.drawGodGlyph(g, god, centerX, centerY, Math.max(5, Math.min(12, w * .22F)));
    }

    private static void control(CustomUiNodeRenderContext c) {
        var r = c.bounds();
        var g = c.graphics();
        boolean selected = flag(c, "selected"), hovered = flag(c, "highlighted"), disabled = flag(c, "disabled");
        int ink = disabled ? 0xFF788590 : selected || hovered ? 0xFFF1F7FF : TEXT;
        bevel(g, r.x(), r.y(), r.width(), r.height(), disabled ? 0xFF202B35 : selected ? 0xFF354B5E : 0xFF293B49,
                hovered && !disabled ? 0xFFA3BECE : 0xFF576F7E, 0xFF111A25);
        if (selected || hovered && !disabled) {
            g.fill(r.x() + 1, r.y() + 3, r.x() + 2, r.y() + r.height() - 3, ink);
        }
        var font = Minecraft.getInstance().font;
        String text = c.node().text();
        int available = Math.max(0, r.width() - 8);
        if (font.width(text) > available) text = font.plainSubstrByWidth(text, Math.max(0, available - font.width("..."))) + "...";
        if (r.width() < 26) text = c.node().text();
        g.drawString(font, text, r.x() + (r.width() - font.width(text)) / 2, r.y() + (r.height() - 8) / 2, ink, false);
    }

    private static void bevel(GuiGraphics g, int x, int y, int w, int h, int body, int top, int bottom) {
        g.fill(x, y, x + w, y + h, body);
        border(g, x, y, w, h, top, bottom, 1);
    }

    private static void border(GuiGraphics g, int x, int y, int w, int h, int top, int bottom, int thickness) {
        g.fill(x, y, x + w, y + thickness, top);
        g.fill(x, y, x + thickness, y + h, top);
        g.fill(x, y + h - thickness, x + w, y + h, bottom);
        g.fill(x + w - thickness, y, x + w, y + h, bottom);
    }

    public static final class StoneButton extends NightshadeButton {
        private final RetainedUiTree tree = new RetainedUiTree(UiNode.builder("stone_control", "canvas").build());
        private boolean selected;

        public StoneButton(int x, int y, int width, int height, Component label, OnPress action) {
            super(x, y, width, height, label, action);
        }

        @Override public void setSelected(boolean selected) { super.setSelected(selected); this.selected = selected; }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            tree.setRoot(UiNode.builder("stone_control", DivinityUiTypes.SHRINE_CONTROL).text(getMessage().getString())
                    .style(UiStyle.builder().size(width, height).clip(true).build())
                    .property("selected", UiValue.bool(selected)).property("highlighted", UiValue.bool(isHoveredOrFocused()))
                    .property("disabled", UiValue.bool(!active)).build());
            tree.layout(getX(), getY(), width, height);
            tree.render(graphics, mouseX, mouseY, partialTick);
        }
    }
}
