package net.nightshade.divinity_engine.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.nightshade.divinity_engine.client.ui.DivinityUi;
import net.nightshade.divinity_engine.client.ui.GalaxyUi;
import net.nightshade.divinity_engine.client.ui.NightshadeButton;
import net.nightshade.divinity_engine.client.ui.ConstellationButton;
import net.nightshade.divinity_engine.client.ui.NightshadeEditBox;
import net.nightshade.divinity_engine.network.messages.ModMessages;
import net.nightshade.divinity_engine.network.messages.gui.PocketRoomSizeMessage;
import net.nightshade.divinity_engine.network.messages.gui.PocketResidentsMessage;
import net.nightshade.divinity_engine.registry.gui.DivinityUiTypes;
import net.nightshade.divinity_engine.util.divinity.blessings.aethon.PocketDomainData;
import net.nightshade.divinity_engine.world.inventory.PocketDomainMenu;
import net.nightshade.renderlib.client.ui.RenderLibContainerScreen;
import net.nightshade.renderlib.ui.UiNode;
import net.nightshade.renderlib.ui.UiRenderLayer;

/** Room creation and per-room law editing, rendered through the existing Nightshade UI host. */
public final class PocketDomainScreen extends RenderLibContainerScreen<PocketDomainMenu> {
    private static final String[] TABS = {"Chambers", "Build / Size", "Rules", "Residents", "Portals"};
    private static final String[] STYLES = {"Star Sanctum", "Verdant Grove", "Astral Workshop", "Astral Arena"};
    private static final String[] RULES = {"Resident revival", "Emergency soul return", "Resident combat", "Natural spawning", "Owner building"};
    private static final String[] HELP = {
            "Lethal damage returns a bound creature alive to this chamber, without death drops or XP.",
            "An eligible owner can escape lethal damage once every five minutes. Ordinary exit is always available.",
            "Allow residents to target and fight. Off prevents attacks involving a bound resident in this chamber.",
            "Permit natural creature spawning when vanilla conditions allow it. This does not spawn mobs on demand.",
            "Allow the owner to place and break interior blocks. The boundary always remains protected."};
    private int tab, room, style, residentPage;
    private int previousState = Integer.MIN_VALUE;
    private boolean resizing;
    private String draftWidth = "31", draftHeight = "15", draftDepth = "31";
    private NightshadeEditBox widthBox, heightBox, depthBox;

    public PocketDomainScreen(PocketDomainMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        tab = Math.max(0, Math.min(4, menu.snapshot.getInt("Tab")));
        room = menu.snapshot.getInt("Room");
    }
    @Override protected void init() {
        imageWidth = Math.min(400, width - 12);
        imageHeight = Math.min(276, height - 12);
        super.init();
        room = Math.max(0, Math.min(room, menu.rooms().size() - 1));
        rebuild();
    }
    private void send(int action) { if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, action); }
    private NightshadeButton button(int x, int y, int w, String text, Runnable action) {
        NightshadeButton button = new ConstellationButton(leftPos + x, topPos + y, w, 18, Component.literal(text),
                "blessing", ignored -> action.run());
        button.setTooltip(Tooltip.create(Component.literal(text)));
        addRenderableWidget(button); return button;
    }
    private String roomName(int i) {
        if (i < 0 || i >= menu.rooms().size()) return "No chamber";
        var saved = menu.rooms().getCompound(i);
        if (saved.getBoolean("Legacy")) return "Original sanctuary";
        try { return STYLES[PocketDomainData.Style.valueOf(saved.getString("Style")).ordinal()] + " " + (i + 1); }
        catch (IllegalArgumentException ex) { return "Chamber " + (i + 1); }
    }
    private void label(UiNode.Builder root, String key, String text, int x, int y, int color) {
        label(root, key, text, x, y, imageWidth - x - 12, color);
    }
    private void label(UiNode.Builder root, String key, String text, int x, int y, int width, int color) {
        root.child(DivinityUi.text(key, font.plainSubstrByWidth(text, width), leftPos + x, topPos + y, width, 10, color));
    }
    private void paragraph(UiNode.Builder root, String text, int y, int lines) {
        var split = font.split(Component.literal(text), imageWidth - 28);
        for (int i = 0; i < Math.min(lines, split.size()); i++) {
            StringBuilder line = new StringBuilder();
            split.get(i).accept((index, format, codePoint) -> { line.appendCodePoint(codePoint); return true; });
            label(root, "paragraph_" + y + "_" + i, line.toString(), 14, y + i * 11, GalaxyUi.MUTED);
        }
    }
    private void rebuild() {
        clearWidgets();
        widthBox = heightBox = depthBox = null;
        int bottom = imageHeight - 26;
        var root = DivinityUi.canvas("pocket_domain")
                .child(GalaxyUi.background(leftPos, topPos, imageWidth, imageHeight, 0x94BCE5))
                .child(GalaxyUi.ornament("domain_frame", DivinityUiTypes.CELESTIAL_FRAME,
                        leftPos, topPos, imageWidth, imageHeight, "aethon", 0, 0))
                .child(DivinityUi.panel("content_card", leftPos + 10, topPos + 50, imageWidth - 20, bottom - 58, 0x7008101D))
                .child(DivinityUi.panel("footer_rule", leftPos + 12, topPos + bottom - 5, imageWidth - 24, 1,
                        (GalaxyUi.GOLD & 0x00FFFFFF) | 0x50000000));
        label(root, "title", "AETHON / YOUR SMALL WORLD", 12, 10, GalaxyUi.GOLD);
        int labelWidth = java.util.Arrays.stream(TABS).mapToInt(text -> font.width(text) + 10).sum();
        int availableWidth = imageWidth - 24 - (TABS.length - 1) * 3;
        int measuredWidth = 0, previousEdge = 0;
        for (int i = 0; i < TABS.length; i++) {
            int target = i;
            measuredWidth += font.width(TABS[i]) + 10;
            int nextEdge = availableWidth * measuredWidth / labelWidth;
            // Only the rendered label is fitted; tooltips, narration and automation retain the full name.
            button(12 + previousEdge + i * 3, 28, nextEdge - previousEdge, TABS[i],
                    () -> { tab = target; rebuild(); }).setSelected(tab == i);
            previousEdge = nextEdge;
        }
        button(12, bottom, 70, "Storage", () -> send(0));
        button(86, bottom, 70, "Refresh", () -> send(3));
        button(imageWidth - 88, bottom, 76, "Leave realm", () -> send(2));
        if (tab == 0) {
            label(root, "summary", menu.rooms().size() + "/8 chambers  |  " + residentCount() + " residents", 14, 54, GalaxyUi.TEXT);
            int rowHeight = Math.min(22, (bottom - 114) / 4);
            int first = room / 4 * 4;
            for (int i = first; i < Math.min(first + 4, menu.rooms().size()); i++) {
                int index = i, y = 68 + (i - first) * rowHeight;
                String progress = menu.progress(i) < 0 ? "Unavailable" : menu.progress(i) == 100 ? "Ready" : menu.progress(i) + "% forming";
                button(14, y, imageWidth - 156, roomName(i), () -> { room = index; rebuild(); }).setSelected(room == i);
                button(imageWidth - 138, y, 60, "Arrival", () -> send(16 + index)).active = menu.progress(i) == 100;
                button(imageWidth - 74, y, 60, "Resize", () -> { room = index; resizing = true; loadSize(); tab = 1; rebuild(); })
                        .active = menu.progress(i) == 100 && !menu.rooms().getCompound(i).getBoolean("Legacy");
                if (i == room) label(root, "status", progress + " | Arrival: " + roomName(menu.selected()), 14, bottom - 20, GalaxyUi.MUTED);
            }
            if (menu.rooms().isEmpty()) paragraph(root, "Create your first chamber. An existing pocket is imported without changing its blocks or stored items.", 78, 4);
            if (menu.rooms().size() > 4) button(14, bottom - 44, 110, first == 0 ? "Next chambers" : "First chambers", () -> { room = first == 0 ? 4 : 0; rebuild(); });
        } else if (tab == 1) {
            int column = columnWidth();
            button(14, 54, column, "New chamber", () -> { resizing = false; draftWidth = "31"; draftHeight = "15"; draftDepth = "31"; rebuild(); }).setSelected(!resizing);
            var resize = button(18 + column, 54, column, "Resize selected", () -> { resizing = true; loadSize(); rebuild(); });
            resize.setSelected(resizing);
            resize.active = !menu.rooms().isEmpty() && !menu.rooms().getCompound(room).getBoolean("Legacy");
            if (!resizing) {
                for (int i = 0; i < STYLES.length; i++) {
                    int selected = i;
                    button(14 + (i % 2) * (column + 4), 78 + (i / 2) * 22, column, STYLES[i], () -> { style = selected; rebuild(); }).setSelected(style == i);
                }
            } else {
                label(root, "resize_room", roomName(room), 14, 82, GalaxyUi.GOLD);
                paragraph(root, "Leave this chamber first. Keep builds inside the new walls; creatures and loose items must be moved out.", 98, 2);
            }
            int field = (imageWidth - 36) / 3;
            label(root, "width", "Width 7-61", 14, 125, field, GalaxyUi.MUTED);
            label(root, "depth", "Depth 7-61", 18 + field, 125, field, GalaxyUi.MUTED);
            label(root, "height", "Height 5-21", 22 + field * 2, 125, field, GalaxyUi.MUTED);
            widthBox = sizeBox(14, field, "Interior width", draftWidth, value -> draftWidth = value);
            depthBox = sizeBox(18 + field, field, "Interior depth", draftDepth, value -> draftDepth = value);
            heightBox = sizeBox(22 + field * 2, field, "Interior height", draftHeight, value -> draftHeight = value);
            button(14, 161, imageWidth - 28, resizing ? "Apply safe resize" : "Weave chamber", this::submitSize).active =
                    (resizing ? !menu.rooms().isEmpty() && !menu.rooms().getCompound(room).getBoolean("Legacy") : menu.rooms().size() < 8)
                    && java.util.stream.IntStream.range(0, menu.rooms().size()).allMatch(i -> menu.progress(i) == 100);
            paragraph(root, "Interior sizes. Blocked resizes are refused.", 183, Math.max(1, (bottom - 186) / 11));
        } else if (tab == 2) {
            roomPicker(root);
            for (int i = 0; i < RULES.length; i++) {
                int rule = i;
                boolean on = (menu.rules(room) & 1 << i) != 0;
                NightshadeButton toggle = button(14, 81 + i * 20, imageWidth - 28, RULES[i] + ": " + (on ? "ON" : "OFF"),
                        () -> send(40 + room * 10 + rule * 2 + (on ? 0 : 1)));
                toggle.setSelected(on);
                toggle.setTooltip(Tooltip.create(Component.literal(HELP[i])));
                toggle.active = !menu.rooms().isEmpty() && menu.progress(room) == 100;
            }
            if (imageHeight > 240) paragraph(root, "Rules affect only this chamber. Soul return: "
                    + (menu.recallSeconds() > 0 ? menu.recallSeconds() + "s remaining." : "ready when eligible.")
                    + " Ownership and manual exit cannot be disabled.", 187, 3);
        } else if (tab == 3) {
            label(root, "residents", "BOUND RESIDENTS / " + residentCount(), 14, 54, GalaxyUi.GOLD);
            button(14, 70, imageWidth - 28, "Capture creature I am looking at", () -> send(1))
                    .setTooltip(Tooltip.create(Component.literal("Outside your realm: aim within six blocks. Weaken hostiles to 30% health. No players, bosses, pets or traders.")));
            int rows = Math.max(2, (bottom - 122) / 20), first = residentPage * rows;
            if (first >= menu.residents().size()) { residentPage = 0; first = 0; }
            for (int i = first; i < Math.min(first + rows, menu.residents().size()); i++) {
                int y = 96 + (i - first) * 20;
                var resident = menu.residents().getCompound(i);
                root.child(DivinityUi.panel("resident_row_" + i, leftPos + 12, topPos + y, imageWidth - 24, 18, 0x60122030));
                label(root, "resident_" + i, resident.getString("Name"), 14, y + 4, imageWidth - 109, GalaxyUi.TEXT);
                button(imageWidth - 87, y, 73, "Release", () -> ModMessages.sendToServer(new PocketResidentsMessage(
                        menu.containerId, menu.snapshot.getInt("ResidentPage"), resident.getUUID("Id"))))
                        .setTooltip(Tooltip.create(Component.literal("Returns this loaded creature to its original world. Revivals: " + resident.getInt("Revivals"))));
            }
            if (menu.residents().size() > rows || menu.snapshot.getInt("ResidentPages") > 1)
                button(14, bottom - 24, 100, "Next residents", () -> {
                    if ((residentPage + 1) * rows < menu.residents().size()) { residentPage++; rebuild(); }
                    else if (menu.snapshot.getInt("ResidentPages") > 1) ModMessages.sendToServer(new PocketResidentsMessage(menu.containerId,
                            (menu.snapshot.getInt("ResidentPage") + 1) % menu.snapshot.getInt("ResidentPages"), null));
                    else { residentPage = 0; rebuild(); }
                });
        } else {
            roomPicker(root);
            boolean entrance = menu.snapshot.contains("Entrance");
            boolean ready = !menu.rooms().isEmpty() && menu.progress(room) == 100;
            var saved = menu.rooms().getCompound(room);
            boolean exit = ready && !saved.getBoolean("ExitClosed");
            boolean inside = minecraft != null && minecraft.player != null
                    && net.nightshade.divinity_engine.util.divinity.blessings.aethon.PocketDomainManager.inPocket(minecraft.player);
            button(14, 80, columnWidth(), entrance ? "Entrance placed" : "Place entrance", () -> send(4)).active = !entrance && !inside;
            button(18 + columnWidth(), 80, columnWidth(), "Close entrance", () -> send(5)).active = entrance;
            button(14, 103, columnWidth(), exit ? "Room exit open" : "Place room exit", () -> send(6)).active = ready && !exit && inside
                    && menu.snapshot.contains("CurrentRoom") && menu.snapshot.getInt("CurrentRoom") == room;
            button(18 + columnWidth(), 103, columnWidth(), "Close room exit", () -> send(120 + room)).active = exit;
            String down = ready && room > 0 && menu.progress(room - 1) == 100 ? "floor " + room : "none";
            String up = ready && room + 1 < menu.rooms().size() && menu.progress(room + 1) == 100 ? "floor " + (room + 2) : "none";
            label(root, "floor_links", "Down: " + down + " | Up: " + up, 14, 129, GalaxyUi.GOLD);
            paragraph(root, "Each room has its own exit at the far wall. Front-left goes down; front-right goes up. To move an exit, close it, stand in that room, then aim at a floor within six blocks and place it.",
                    146, Math.max(1, (bottom - 150) / 11));
        }
        setRenderlibRoot(UiRenderLayer.BACKGROUND, root.build());
    }
    private int columnWidth() { return (imageWidth - 32) / 2; }
    private int residentCount() { return menu.snapshot.contains("ResidentCount") ? menu.snapshot.getInt("ResidentCount") : menu.residents().size(); }
    private void loadSize() {
        var size = menu.size(room);
        draftWidth = Integer.toString(size.width() - 2); draftDepth = Integer.toString(size.depth() - 2); draftHeight = Integer.toString(size.height() - 3);
    }
    private NightshadeEditBox sizeBox(int x, int width, String name, String value, java.util.function.Consumer<String> responder) {
        var box = new NightshadeEditBox(font, leftPos + x, topPos + 137, width, 20, Component.literal(name));
        box.setThemeResolver(GalaxyUi::search);
        box.setMaxLength(2); box.setFilter(text -> text.matches("[0-9]{0,2}")); box.setValue(value); box.setResponder(responder);
        addRenderableWidget(box); return box;
    }
    private void submitSize() {
        try {
            int w = Integer.parseInt(draftWidth), h = Integer.parseInt(draftHeight), d = Integer.parseInt(draftDepth);
            var size = new PocketDomainData.Dimensions(w + 2, h + 3, d + 2);
            if (!size.valid()) throw new NumberFormatException();
            ModMessages.sendToServer(new PocketRoomSizeMessage(menu.containerId, resizing ? room : -1, style, size.width(), size.height(), size.depth()));
        } catch (NumberFormatException invalid) {
            if (minecraft != null && minecraft.player != null) minecraft.player.displayClientMessage(
                    Component.literal("Width/depth must be 7-61 and height 5-21 interior blocks."), true);
        }
    }
    private void roomPicker(UiNode.Builder root) {
        button(14, 54, 26, "<", () -> { room = Math.max(0, room - 1); rebuild(); }).active = room > 0;
        label(root, "room", roomName(room), 48, 59, imageWidth - 96, GalaxyUi.GOLD);
        button(imageWidth - 40, 54, 26, ">", () -> { room = Math.min(menu.rooms().size() - 1, room + 1); rebuild(); }).active = room + 1 < menu.rooms().size();
    }
    @Override public void containerTick() {
        super.containerTick();
        int hash = 31 * menu.selected() + menu.recallSeconds();
        for (int i = 0; i < 8; i++) hash = 31 * hash + menu.progress(i) + menu.rules(i) * 101 + menu.size(i).hashCode();
        boolean editing = widthBox != null && (widthBox.isFocused() || heightBox.isFocused() || depthBox.isFocused());
        if (hash != previousState && !editing) { previousState = hash; rebuild(); }
    }
    @Override public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (getFocused() instanceof NightshadeEditBox box && key != org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE
                && (box.keyPressed(key, scanCode, modifiers) || box.canConsumeInput())) return true;
        return super.keyPressed(key, scanCode, modifiers);
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics); super.render(graphics, mouseX, mouseY, partialTick); renderTooltip(graphics, mouseX, mouseY);
    }
    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {}
    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {}
}
