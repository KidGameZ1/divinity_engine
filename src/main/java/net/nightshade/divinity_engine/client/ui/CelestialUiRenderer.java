package net.nightshade.divinity_engine.client.ui;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.nightshade.divinity_engine.registry.gui.DivinityUiTypes;
import net.nightshade.renderlib.client.RenderLibRenderTypes;
import net.nightshade.renderlib.client.ui.CustomUiNodeRenderContext;
import net.nightshade.renderlib.client.ui.CustomUiNodeRendererRegistry;
import net.nightshade.renderlib.ui.UiValue;
import org.joml.Matrix4f;

/** Bounded, screen-local engraving meshes using Core's render types, also without shaders. */
public final class CelestialUiRenderer {
    private static final ResourceLocation MATERIAL = new ResourceLocation("nightshade_core", "translucent");
    private static final int BRASS = GalaxyUi.GOLD;
    private static final int FAINT = 0x605E665D;

    private CelestialUiRenderer() {}

    public static void register() {
        ShrineUi.register();
        CustomUiNodeRendererRegistry.register(DivinityUiTypes.CELESTIAL_CONTROL, CelestialUiRenderer::control);
        CustomUiNodeRendererRegistry.register(DivinityUiTypes.CELESTIAL_FRAME, CelestialUiRenderer::frame);
        CustomUiNodeRendererRegistry.register(DivinityUiTypes.DIVINE_SEAL, CelestialUiRenderer::seal);
        CustomUiNodeRendererRegistry.register(DivinityUiTypes.CELESTIAL_INVENTORY, CelestialUiRenderer::inventory);
        CustomUiNodeRendererRegistry.register(DivinityUiTypes.CELESTIAL_VAULT, CelestialUiRenderer::vault);
    }

    private static boolean flag(CustomUiNodeRenderContext c, String name) {
        return c.node().properties().getOrDefault(name, UiValue.bool(false)).asBoolean().orElse(false);
    }

    private static int number(CustomUiNodeRenderContext c, String name) {
        return c.node().properties().getOrDefault(name, UiValue.integer(0)).asInteger().orElse(0);
    }

    private static String string(CustomUiNodeRenderContext c, String name) {
        return c.node().properties().getOrDefault(name, UiValue.text("")).value();
    }

    private static void control(CustomUiNodeRenderContext c) {
        var r = c.bounds();
        float x = r.x(), y = r.y(), w = r.width(), h = r.height(), cy = y + h / 2;
        String role = string(c, "role");
        boolean selected = flag(c, "selected"), hovered = flag(c, "highlighted"), disabled = flag(c, "disabled");
        int ink = disabled ? 0xFF778597 : selected || hovered ? 0xFFF1F7FF : GalaxyUi.TEXT;
        int line = disabled ? 0xFF263243 : selected || hovered ? BRASS : 0xFF3B5067;
        String label = c.node().text();
        float textX = x + 4;
        int textWidth = r.width() - 8;
        boolean centered = false, alignRight = false;
        try (Mesh mesh = new Mesh(c.graphics())) {
            mesh.rectangle(x, y, w, h, disabled ? 0xFF101825 : selected ? 0xFF24394D : hovered ? 0xFF233246 : 0xE8162131);
            mesh.line(x, y + h - 1, x + w - 1, y + h - 1, 1, line);
            if (selected) mesh.rectangle(x, y + 2, 2, h - 4, BRASS);
            if (hovered && !disabled) {
                mesh.line(x, y, x + w - 1, y, 1, line);
                mesh.line(x, y, x, y + h - 1, 1, line);
                mesh.line(x + w - 1, y, x + w - 1, y + h - 1, 1, line);
            }
            if (role.equals("close")) {
                mesh.line(x + w / 2 - 2, cy - 2, x + w / 2 + 2, cy + 2, 1, ink);
                mesh.line(x + w / 2 + 2, cy - 2, x + w / 2 - 2, cy + 2, 1, ink);
                return;
            }
            if (role.equals("god")) {
                textX = x + 8;
                textWidth = r.width() - 14;
            } else if (role.equals("seal")) {
                mesh.rectangle(x + 3, cy - 6, 14, 12, 0xFF0B121E);
                textX = x + 20;
                textWidth = r.width() - 23;
            } else if (role.equals("trial")) {
                centered = true;
            } else {
                boolean left = role.equals("blessing_left"), compact = role.equals("blessing");
                if (!compact) {
                    textX = x + 7;
                    textWidth = r.width() - 14;
                    alignRight = left;
                } else {
                    centered = true;
                }
            }
        }
        if (role.equals("seal") && label.contains(": ")) {
            String key = label.substring(0, label.indexOf(": "));
            c.graphics().drawString(Minecraft.getInstance().font, key,
                    (int) x + 10 - Minecraft.getInstance().font.width(key) / 2, (int) cy - 4, ink, false);
            label = label.substring(label.indexOf(": ") + 2);
        }
        String displayed = fit(label, textWidth);
        int size = Minecraft.getInstance().font.width(displayed);
        if (centered) textX = x + (w - size) / 2;
        else if (alignRight) textX += textWidth - size;
        c.graphics().drawString(Minecraft.getInstance().font, displayed, (int) textX, (int) cy - 4, ink, false);
    }

    private static String fit(String text, int width) {
        var font = Minecraft.getInstance().font;
        if (font.width(text) <= width) return text;
        return font.plainSubstrByWidth(text, Math.max(0, width - font.width("..."))) + "...";
    }

    private static void frame(CustomUiNodeRenderContext c) {
        var r = c.bounds();
        float x = r.x(), y = r.y(), w = r.width(), h = r.height();
        float rail = x + number(c, "rail");
        try (Mesh mesh = new Mesh(c.graphics())) {
            mesh.line(x + 2, y, x + w - 3, y, 1, 0xFF61788D);
            mesh.line(x + 2, y + h - 1, x + w - 3, y + h - 1, 1, 0xFF28394D);
            mesh.line(x, y + 2, x, y + h - 3, 1, 0xFF61788D);
            mesh.line(x + w - 1, y + 2, x + w - 1, y + h - 3, 1, 0xFF28394D);
            mesh.line(x + 1, y + 1, x + w - 2, y + 1, 1, 0xFF1C2A3B);
            mesh.line(x + 10, y + 27, x + w - 11, y + 27, 1, 0xFF26384B);
            if (number(c, "rail") > 0) {
                mesh.line(rail, y + 34, rail, y + h - 12, 1, 0xFF26384B);
            }
        }
    }

    private static void vault(CustomUiNodeRenderContext c) {
        var r = c.bounds();
        float x = r.x(), y = r.y(), w = r.width(), h = r.height();
        try (Mesh mesh = new Mesh(c.graphics())) {
            // One small constellation in the empty margin, never underneath item slots.
            mesh.line(x + w - 5, y + 49, x + w - 3, y + 64, 1, 0xFF2B4056);
            mesh.line(x + w - 3, y + 64, x + w - 5, y + 82, 1, 0xFF2B4056);
            mesh.star(x + w - 5, y + 49, 1, 0xFF6889A5);
            mesh.rectangle(x + w - 3, y + 64, 1, 1, BRASS);
            mesh.star(x + w - 5, y + 82, 1, 0xFF6889A5);
        }
    }

    private static void inventory(CustomUiNodeRenderContext c) {
        var r = c.bounds();
        int count = Math.max(0, Math.min(256, number(c, "slot_count")));
        // The screen supplies actual menu coordinates, not a second copy of its slot layout.
        try (Mesh mesh = new Mesh(c.graphics())) {
            for (int i = 0; i < count; i++) {
                float x = r.x() + number(c, "slot_x_" + i) - 1;
                float y = r.y() + number(c, "slot_y_" + i) - 1;
                mesh.rectangle(x, y, 18, 18, 0xFF0A101B);
                mesh.rectangle(x + 1, y + 1, 16, 16, 0xFF182536);
                mesh.line(x, y + 17, x + 17, y + 17, 1, 0xFF4B6076);
                mesh.line(x + 17, y, x + 17, y + 17, 1, 0xFF4B6076);
            }
            float divider = r.y() + number(c, "divider_y");
            mesh.line(r.x() + 7, divider, r.x() + r.width() - 7, divider, 1, 0xFF26384B);
        }
    }

    private static void seal(CustomUiNodeRenderContext c) {
        var r = c.bounds();
        float cx = r.x() + r.width() / 2F, cy = r.y() + r.height() / 2F;
        try (Mesh mesh = new Mesh(c.graphics())) {
            glyph(mesh, string(c, "god"), cx, cy, Math.max(3, Math.min(10, Math.min(r.width(), r.height()) * .3F)));
        }
    }

    static void drawGodGlyph(GuiGraphics graphics, String god, float x, float y, float size) {
        try (Mesh mesh = new Mesh(graphics)) {
            glyph(mesh, god, x, y, size);
        }
    }

    private static void glyph(Mesh m, String god, float x, float y, float s) {
        switch (god) {
            case "solarius" -> {
                m.arc(x, y, s * .5F, s * .5F, 0, 360, BRASS);
                for (int i = 0; i < 8; i++) {
                    float dx = (float) Math.cos(i * Math.PI / 4), dy = (float) Math.sin(i * Math.PI / 4);
                    m.line(x + dx * s * .7F, y + dy * s * .7F, x + dx * s, y + dy * s, 1, BRASS);
                }
            }
            case "nythea" -> {
                m.arc(x, y, s, s, 55, 305, BRASS);
                m.arc(x + s * .45F, y, s * .75F, s * .85F, 90, 270, BRASS);
                m.star(x + s * .6F, y - s * .4F, 2.5F, GalaxyUi.TEXT);
            }
            case "kairoth" -> m.path(x, y, s, -1,-1, 1,-1, -1,1, 1,1, -1,-1);
            case "voltira" -> m.path(x, y, s, .4F,-1, -.5F,0, .4F,0, -.4F,1);
            case "grond" -> m.path(x, y, s, -1,.7F, -.3F,-.8F, 0,-.1F, .5F,-.6F, 1,.7F, -1,.7F);
            case "varun" -> {
                for (int i = -1; i <= 1; i++) m.path(x + i * s * .6F, y, s, .2F,-1, -.2F,.7F, 0,1);
            }
            case "dravak" -> {
                m.path(x, y, s, -1,-1, .7F,.7F, 1,1);
                m.path(x, y, s, 1,-1, -.7F,.7F, -1,1);
                m.line(x - s, y + s * .5F, x - s * .5F, y + s, 1, BRASS);
                m.line(x + s, y + s * .5F, x + s * .5F, y + s, 1, BRASS);
            }
            case "elaris" -> {
                m.path(x, y, s, -1,0, 0,-.7F, 1,0, 0,.7F, -1,0);
                m.arc(x, y, s * .3F, s * .3F, 0, 360, BRASS);
            }
            case "nashara" -> {
                m.arc(x, y - s * .4F, s * .65F, s * .55F, 155, 365, BRASS);
                m.arc(x, y + s * .4F, s * .65F, s * .55F, -25, 185, BRASS);
                m.diamond(x - s * .6F, y - s * .2F, 2, GalaxyUi.TEXT);
            }
            case "vokar" -> {
                m.arc(x, y, s * .85F, s, 180, 360, BRASS);
                m.path(x, y, s, -.85F,0, -.85F,.5F, -.35F,1, .35F,1, .85F,.5F, .85F,0);
                m.line(x, y - s * .2F, x, y + s, .8F, BRASS);
            }
            case "cryonis" -> {
                for (int i = 0; i < 3; i++) {
                    float dx = (float) Math.cos(i * Math.PI / 3) * s, dy = (float) Math.sin(i * Math.PI / 3) * s;
                    m.line(x - dx, y - dy, x + dx, y + dy, 1, BRASS);
                }
                m.diamond(x, y, s * .45F, GalaxyUi.TEXT);
            }
            case "nerai" -> {
                for (int i = -1; i <= 1; i++) m.path(x, y + i * s * .6F, s, -1,.15F, -.5F,-.15F, .5F,.15F, 1,-.15F);
            }
            case "zephra" -> {
                m.arc(x, y, s, s * .6F, 130, 360, BRASS);
                m.arc(x, y, s * .55F, s * .3F, -40, 180, BRASS);
                m.path(x, y, s, -1,.8F, .4F,.8F, .8F,.4F);
            }
            case "terra" -> {
                m.path(x, y, s, 0,1, 0,-1, -.65F,-.35F, 0,0, .65F,-.6F);
                m.path(x, y, s, -.8F,.4F, 0,.7F, .8F,.1F);
            }
            case "lumen" -> { m.star(x, y, s, BRASS); m.diamond(x, y, s * .45F, GalaxyUi.TEXT); }
            case "mechanos" -> {
                m.arc(x, y, s * .65F, s * .65F, 0, 360, BRASS);
                for (int i = 0; i < 8; i++) {
                    float dx = (float) Math.cos(i * Math.PI / 4), dy = (float) Math.sin(i * Math.PI / 4);
                    m.line(x + dx * s * .65F, y + dy * s * .65F, x + dx * s, y + dy * s, 2, BRASS);
                }
                m.diamond(x, y, s * .25F, GalaxyUi.TEXT);
            }
            case "ignar" -> m.path(x, y, s, 0,-1, -.85F,.4F, -.5F,1, .5F,1, .85F,.3F, .4F,-.2F, .15F,.4F, 0,-1);
            case "aethon" -> { m.arc(x, y, s, s * .4F, 0, 360, BRASS); m.star(x, y, s, GalaxyUi.TEXT); }
            default -> { m.diamond(x, y, s, BRASS); m.star(x, y, s * .6F, GalaxyUi.TEXT); }
        }
    }

    private static final class Mesh implements AutoCloseable {
        private final GuiGraphics graphics;
        private final VertexConsumer vertices;
        private final Matrix4f pose;

        private Mesh(GuiGraphics graphics) {
            this.graphics = graphics;
            graphics.flush();
            vertices = graphics.bufferSource().getBuffer(RenderLibRenderTypes.quads(MATERIAL, true));
            pose = graphics.pose().last().pose();
        }

        private void line(float x1, float y1, float x2, float y2, float width, int color) {
            int ax = Math.round(x1), ay = Math.round(y1), bx = Math.round(x2), by = Math.round(y2);
            if (ay == by) { rectangle(Math.min(ax, bx), ay, Math.abs(bx - ax) + 1, 1, color); return; }
            if (ax == bx) { rectangle(ax, Math.min(ay, by), 1, Math.abs(by - ay) + 1, color); return; }
            // Bresenham edges stay on the same pixel grid as Minecraft's font and items.
            int dx = Math.abs(bx - ax), dy = -Math.abs(by - ay), sx = ax < bx ? 1 : -1, sy = ay < by ? 1 : -1;
            int error = dx + dy;
            for (int steps = 0; steps < 2048; steps++) {
                rectangle(ax, ay, 1, 1, color);
                if (ax == bx && ay == by) break;
                int twice = error * 2;
                if (twice >= dy) { error += dy; ax += sx; }
                if (twice <= dx) { error += dx; ay += sy; }
            }
        }

        private void rectangle(float x, float y, float width, float height, int color) {
            vertex(x, y, color); vertex(x, y + height, color);
            vertex(x + width, y + height, color); vertex(x + width, y, color);
        }

        private void arc(float x, float y, float rx, float ry, float start, float end, int color) {
            int steps = Math.max(4, (int) ((end - start) / 8));
            for (int i = 0; i < steps; i++) {
                double a = Math.toRadians(start + (end - start) * i / steps);
                double b = Math.toRadians(start + (end - start) * (i + 1) / steps);
                line(x + (float) Math.cos(a) * rx, y + (float) Math.sin(a) * ry,
                        x + (float) Math.cos(b) * rx, y + (float) Math.sin(b) * ry, .7F, color);
            }
        }

        private void diamond(float x, float y, float radius, int color) {
            line(x, y - radius, x + radius, y, .8F, color);
            line(x + radius, y, x, y + radius, .8F, color);
            line(x, y + radius, x - radius, y, .8F, color);
            line(x - radius, y, x, y - radius, .8F, color);
        }

        private void star(float x, float y, float radius, int color) {
            line(x - radius, y, x + radius, y, .8F, color);
            line(x, y - radius, x, y + radius, .8F, color);
            diamond(x, y, radius * .4F, color);
        }

        private void path(float x, float y, float scale, float... points) {
            for (int i = 2; i < points.length; i += 2)
                line(x + points[i - 2] * scale, y + points[i - 1] * scale,
                        x + points[i] * scale, y + points[i + 1] * scale, 1, BRASS);
        }

        private void vertex(float x, float y, int color) {
            vertices.vertex(pose, x, y, 0).color((color >> 16) & 255, (color >> 8) & 255, color & 255, color >>> 24).endVertex();
        }

        @Override public void close() { graphics.flush(); }
    }
}
