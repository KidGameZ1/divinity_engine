package net.nightshade.divinity_engine.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.locale.Language;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.nightshade.divinity_engine.client.codex.CodexButton;
import net.nightshade.divinity_engine.client.codex.CodexJournal;
import net.nightshade.divinity_engine.client.codex.CodexJournal.Entry;
import net.nightshade.divinity_engine.client.codex.CodexJournal.Section;
import net.nightshade.divinity_engine.client.codex.CodexPagination;
import net.nightshade.divinity_engine.client.codex.CodexResources;
import net.nightshade.divinity_engine.client.ui.DivinityUi;
import net.nightshade.divinity_engine.client.ui.NightshadeEditBox;
import net.nightshade.renderlib.client.ui.RenderLibContainerScreen;
import net.nightshade.renderlib.ui.UiRenderLayer;
import net.nightshade.divinity_engine.registry.divinity.DivineCodexPagesRegistry;
import net.nightshade.divinity_engine.world.inventory.DivineCodexMenu;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.Arrays;
import java.util.stream.Collectors;

public class DivineCodexScreen extends RenderLibContainerScreen<DivineCodexMenu> {
    private static final ResourceLocation BACKGROUND = texture("divine_codex.png");
    private static final int INK = 0xFF293D49;
    private static final int MUTED = 0xFF62635D;
    private static final int ROW_HEIGHT = 21;

    // Store only navigation, scoped to the connection and player, never journal content.
    private static final Map<ClientPacketListener, Map<UUID, Bookmark>> BOOKMARKS = new WeakHashMap<>();

    private record View(Section section, String selectedId, int page, int indexPage,
                        boolean reading, String query) {}
    private record Bookmark(View view, List<View> history) {}

    private final Deque<View> history = new ArrayDeque<>();
    private final List<CodexButton> entryButtons = new ArrayList<>();
    private Section section = Section.REGISTERED_PAGES;
    private String selectedId;
    private String query = "";
    private int page;
    private int indexPage;
    private boolean reading;
    private boolean restored;
    private ClientPacketListener bookmarkConnection;
    private UUID bookmarkPlayer;

    private CompoundTag cachedData;
    private CodexJournal.Snapshot snapshot = new CodexJournal.Snapshot(0, false, List.of());
    private List<Entry> registeredPages = List.of();
    private int registeredTitleCount = -1;
    private int registeredContentCount = -1;
    private List<Entry> filtered = List.of();
    private Entry selectedEntry;
    private CodexPagination pagination;
    private List<FormattedCharSequence> titleLines = List.of();
    private String readingTitle = "";
    private int titleHeight;
    private int bodyY;
    private long resourceGeneration = -1;
    private Language language;
    private Font measuredFont;

    private boolean compact;
    private int innerX;
    private int innerWidth;
    private int contentTop;
    private int footerY;
    private int indexWidth;
    private int readerX;
    private int readerWidth;
    private int rowTop;
    private int visibleRows;

    private EditBox search;
    private CodexButton backButton;
    private CodexButton contentsButton;
    private CodexButton previousEntry;
    private CodexButton nextEntry;
    private CodexButton previousChapter;
    private CodexButton nextChapter;
    private CodexButton previousIndex;
    private CodexButton nextIndex;
    private CodexButton previousPage;
    private CodexButton nextPage;

    public DivineCodexScreen(DivineCodexMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    private static ResourceLocation texture(String path) {
        return new ResourceLocation("divinity_engine", "textures/screens/" + path);
    }

    @Override
    protected void init() {
        imageWidth = Math.min(600, width - 16);
        imageHeight = Math.min(360, height - 16);
        super.init();
        setRenderlibRoot(UiRenderLayer.BACKGROUND, DivinityUi.canvas("codex")
                .child(DivinityUi.book(leftPos, topPos, imageWidth, imageHeight))
                .child(DivinityUi.panel("binding", leftPos + 5, topPos + 12, 3, imageHeight - 26, 0xFF354C65))
                .child(DivinityUi.panel("header_rule", leftPos + 14, topPos + 24, imageWidth - 28, 1, 0x60909CA4)).build());
        restoreBookmark();
        innerX = leftPos + 14;
        innerWidth = imageWidth - 28;
        compact = imageWidth < 548;
        contentTop = topPos + 54;
        footerY = topPos + imageHeight - 32;
        indexWidth = compact ? innerWidth : Math.min(210, innerWidth * 2 / 5);
        readerX = compact ? innerX : innerX + indexWidth + 18;
        readerWidth = compact ? innerWidth : innerWidth - indexWidth - 18;
        rowTop = contentTop + 66;
        visibleRows = Math.max(1, (footerY - 6 - rowTop) / ROW_HEIGHT);

        // Register full guides once; pagination below follows the current font and layout.
        DivineCodexPagesRegistry.initializePages();
        refreshData(true);
        createControls();
        updateControls();
        setInitialFocus(reading ? contentsButton : search);
    }

    private void restoreBookmark() {
        if (restored || minecraft.player == null || minecraft.getConnection() == null) return;
        restored = true;
        bookmarkConnection = minecraft.getConnection();
        bookmarkPlayer = minecraft.player.getUUID();
        Bookmark bookmark = BOOKMARKS.getOrDefault(bookmarkConnection, Map.of()).get(bookmarkPlayer);
        if (bookmark != null) {
            restore(bookmark.view());
            history.addAll(bookmark.history());
        }
    }

    private void createControls() {
        entryButtons.clear();
        int toolbarY = topPos + 28;
        backButton = button(innerX, toolbarY, 48, 18, "Back", "Back through journal history (Alt+Left)", b -> goBack());
        contentsButton = button(innerX + 52, toolbarY, 68, 18, "Contents", "Browse chapters and search (Ctrl+F)", b -> showContents());
        previousEntry = button(innerX + innerWidth - 112, toolbarY, 54, 18,
                "Entry <", "Previous entry in these contents", b -> moveEntry(-1));
        nextEntry = button(innerX + innerWidth - 54, toolbarY, 54, 18,
                "Entry >", "Next entry in these contents", b -> moveEntry(1));
        button(innerX + innerWidth - 18, topPos + 9, 18, 16, "X", "Close the Divine Codex (Esc)", b -> onClose());

        String chapters = Arrays.stream(Section.values()).map(value -> value.title).collect(Collectors.joining(" / "));
        previousChapter = button(innerX, contentTop, 20, 24, "<", "Previous chapter\n" + chapters, b -> changeChapter(-1));
        nextChapter = button(innerX + indexWidth - 20, contentTop, 20, 24, ">", "Next chapter\n" + chapters, b -> changeChapter(1));
        search = new NightshadeEditBox(font, innerX, contentTop + 30, indexWidth, 18, Component.literal("Search unlocked journal entries"));
        search.setMaxLength(128);
        search.setHint(Component.literal("Search all chapters..."));
        search.setTooltip(Tooltip.create(Component.literal("Search titles, text and status in all unlocked chapters. Enter opens the first result.")));
        search.setValue(query);
        search.setResponder(value -> {
            query = value;
            indexPage = 0;
            filterEntries();
            updateControls();
        });
        addRenderableWidget(search);
        for (int i = 0; i < visibleRows; i++) {
            final int row = i;
            CodexButton entryButton = button(innerX, rowTop + i * ROW_HEIGHT, indexWidth, ROW_HEIGHT - 3,
                    "", "Read entry", b -> {
                        int index = indexPage * visibleRows + row;
                        if (index < filtered.size()) openEntry(filtered.get(index));
                    });
            entryButton.setTextAlignment(net.nightshade.renderlib.ui.UiStyle.Alignment.START);
            entryButtons.add(entryButton);
        }
        previousIndex = button(innerX, footerY, 24, 18, "<", "Previous contents page", b -> turnIndex(-1));
        nextIndex = button(innerX + indexWidth - 24, footerY, 24, 18, ">", "Next contents page", b -> turnIndex(1));
        previousPage = pageButton(readerX, false);
        nextPage = pageButton(readerX + readerWidth - 24, true);
    }

    private CodexButton button(int x, int y, int width, int height, String label, String tooltip, Button.OnPress action) {
        return addRenderableWidget(new CodexButton(x, y, width, height, label, tooltip, action));
    }

    private CodexButton pageButton(int x, boolean forward) {
        CodexButton button = button(x, footerY + 1, 24, 15, forward ? "Next text page" : "Previous text page",
                forward ? "Next page (Right / Page Down)" : "Previous page (Left / Page Up)",
                b -> turnPage(forward ? 1 : -1));
        button.setVisualLabel(forward ? ">" : "<");
        return button;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        search.tick();
        if (refreshData(false)) updateControls();
    }

    private boolean refreshData(boolean force) {
        font = minecraft.font;
        boolean resourcesChanged = resourceGeneration != CodexResources.generation()
                || language != Language.getInstance() || measuredFont != font;
        CompoundTag live = minecraft.player == null ? new CompoundTag()
                : minecraft.player.getPersistentData().getCompound(CodexJournal.DATA_KEY);
        var pages = DivineCodexPagesRegistry.getPages();
        boolean pagesChanged = registeredTitleCount != pages.sizeTitles() || registeredContentCount != pages.sizeContents();
        // Compare values too: objective overlays may change independently of the revision counter.
        if (!force && !resourcesChanged && !pagesChanged && live.equals(cachedData)) return false;
        if (force || resourcesChanged || pagesChanged) {
            registeredPages = CodexJournal.registeredPages(pages);
            registeredTitleCount = pages.sizeTitles();
            registeredContentCount = pages.sizeContents();
            resourceGeneration = CodexResources.generation();
            language = Language.getInstance();
            measuredFont = font;
        }
        cachedData = live.copy();
        snapshot = CodexJournal.read(cachedData, registeredPages);
        filterEntries();
        reflow();
        return true;
    }

    private void filterEntries() {
        filtered = snapshot.entries().stream()
                .filter(entry -> (query.isBlank() ? entry.section() == section : entry.matches(query)))
                .toList();
        indexPage = Math.max(0, Math.min(indexPage, indexPageCount() - 1));
    }

    private void reflow() {
        selectedEntry = snapshot.entries().stream().filter(entry -> entry.id().equals(selectedId)).findFirst().orElse(null);
        if (selectedEntry == null && snapshot.synced() && reading) {
            reading = false;
            selectedId = null;
            page = 0;
        }
        Entry entry = reading ? selectedEntry : null;
        readingTitle = entry != null ? entry.title() : "Your Divine Codex";
        titleLines = font.split(entry != null ? entry.titleComponent() : Component.literal(readingTitle), readerWidth);
        titleHeight = Math.min(3, titleLines.size()) * (font.lineHeight + 2);
        bodyY = contentTop + titleHeight + 5;
        if (entry != null && !entry.status().isBlank()) bodyY += font.lineHeight + 3;
        if (entry != null && entry.target() > 0) bodyY += font.lineHeight + 10;
        Component body = entry != null ? entry.textComponent() : Component.literal(
                "Codex Pages contains every registered page, including the original guides and pages from addons. These are always available.\n\n"
                + (snapshot.synced() ? "The other sections hold your story guides and personal discoveries. Visit a shrine to continue your story."
                : "Your personal story journal is still syncing. You can read the registered pages now; discoveries will appear when the server sends them."));
        pagination = CodexPagination.wrap(font, body, readerWidth, footerY - 6 - bodyY);
        // Keep a pending bookmark intact while waiting for its first snapshot.
        if (entry != null) page = pagination.clamp(page);
    }

    private int indexPageCount() {
        return Math.max(1, (filtered.size() + visibleRows - 1) / visibleRows);
    }

    private int selectedIndex() {
        for (int i = 0; i < filtered.size(); i++) {
            if (filtered.get(i).id().equals(selectedId)) return i;
        }
        return -1;
    }

    private void updateControls() {
        if (search == null) return;
        boolean indexVisible = !compact || !reading;
        boolean readerVisible = !compact || reading;
        backButton.active = !history.isEmpty();
        contentsButton.setSelected(!reading);
        int selected = selectedIndex();
        previousEntry.active = reading && selected > 0;
        nextEntry.active = reading && selected >= 0 && selected + 1 < filtered.size();
        previousChapter.visible = nextChapter.visible = indexVisible;
        search.setVisible(indexVisible);
        previousIndex.visible = nextIndex.visible = indexVisible;
        previousIndex.active = indexPage > 0;
        nextIndex.active = indexPage + 1 < indexPageCount();
        previousPage.visible = nextPage.visible = readerVisible;
        int displayedPage = pagination.clamp(page);
        boolean registered = selectedEntry != null && selectedEntry.section() == Section.REGISTERED_PAGES;
        previousPage.active = reading && selectedEntry != null && (displayedPage > 0 || registered && selected > 0);
        nextPage.active = reading && selectedEntry != null && (displayedPage + 1 < pagination.pageCount()
                || registered && selected >= 0 && selected + 1 < filtered.size());
        for (int i = 0; i < entryButtons.size(); i++) {
            CodexButton button = entryButtons.get(i);
            int index = indexPage * visibleRows + i;
            button.visible = indexVisible && index < filtered.size();
            if (index < filtered.size()) {
                Entry entry = filtered.get(index);
                button.setMessage(Component.literal(entry.title()));
                button.setSelected(reading && entry.id().equals(selectedId));
                button.setTooltip(Tooltip.create(Component.literal(entry.section().title + "\n" + entry.title()
                        + (entry.status().isBlank() ? "" : "\n" + entry.statusLabel())
                        + (entry.target() > 0 ? "\n" + entry.progressLabel() : "") + "\nRead entry")));
            }
        }
    }

    private View view() {
        return new View(section, selectedId, page, indexPage, reading, query);
    }

    private void restore(View view) {
        section = view.section();
        selectedId = view.selectedId();
        page = view.page();
        indexPage = view.indexPage();
        reading = view.reading();
        query = view.query();
    }

    private void remember() {
        if (history.isEmpty() || !history.peekLast().equals(view())) history.addLast(view());
        while (history.size() > 64) history.removeFirst();
    }

    private void navigationChanged() {
        filterEntries();
        reflow();
        updateControls();
        setFocused(contentsButton);
        triggerImmediateNarration(false);
    }

    private void openEntry(Entry entry) {
        if (!reading || !entry.id().equals(selectedId)) {
            remember();
        }
        if (!entry.id().equals(selectedId)) page = 0;
        selectedId = entry.id();
        section = entry.section();
        reading = true;
        navigationChanged();
    }

    private void moveEntry(int direction) {
        int index = selectedIndex();
        int next = index + direction;
        if (index >= 0 && next >= 0 && next < filtered.size()) {
            openEntry(filtered.get(next));
            indexPage = next / visibleRows;
            updateControls();
        }
    }

    private void changeChapter(int direction) {
        remember();
        section = section.next(direction);
        selectedId = null;
        reading = false;
        page = indexPage = 0;
        query = "";
        search.setValue(query);
        navigationChanged();
        setFocused(direction < 0 ? previousChapter : nextChapter);
    }

    private void showContents() {
        if (reading) remember();
        reading = false;
        reflow();
        updateControls();
        setFocused(search);
    }

    private void goBack() {
        if (history.isEmpty()) return;
        View previous = history.removeLast();
        // setValue invokes the search responder, so restore page state afterwards.
        search.setValue(previous.query());
        restore(previous);
        navigationChanged();
    }

    private void turnPage(int direction) {
        if (!reading || selectedEntry == null) return;
        int next = pagination.clamp(page + direction);
        if (next == page) {
            if (selectedEntry.section() == Section.REGISTERED_PAGES) {
                int target = selectedIndex() + direction;
                if (selectedIndex() >= 0 && target >= 0 && target < filtered.size()) {
                    moveEntry(direction);
                    if (direction < 0) page = pagination.pageCount() - 1;
                    updateControls();
                    triggerImmediateNarration(false);
                }
            }
            return;
        }
        page = next;
        updateControls();
        triggerImmediateNarration(false);
    }

    private void turnIndex(int direction) {
        indexPage = Math.max(0, Math.min(indexPage + direction, indexPageCount() - 1));
        updateControls();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        if ((!compact || reading) && mouseX >= readerX && mouseX < readerX + readerWidth
                && mouseY >= contentTop && mouseY < bodyY - 2) {
            Entry entry = reading ? selectedEntry : null;
            String tooltip = readingTitle + (entry == null || entry.status().isBlank() ? "" : "\n" + entry.statusLabel())
                    + (entry == null || entry.target() <= 0 ? "" : "\n" + entry.progressLabel());
            graphics.renderTooltip(font, font.split(Component.literal(tooltip), Math.min(280, width - 32)), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.drawString(font, "Divine Codex", innerX, topPos + 13, INK, false);
        String note = snapshot.synced() ? "Guides & journal" : "Journal syncing...";
        int noteWidth = Math.max(0, innerWidth - font.width("Divine Codex") - 36);
        note = CodexButton.fit(font, note, noteWidth);
        graphics.drawString(font, note, innerX + innerWidth - 26 - font.width(note), topPos + 13, MUTED, false);
        if (!compact) graphics.vLine(readerX - 10, contentTop, footerY + 17, 0x55898076);
        if (!compact || !reading) renderIndex(graphics);
        if (!compact || reading) renderReading(graphics);
    }

    private void renderIndex(GuiGraphics graphics) {
        List<FormattedCharSequence> heading = font.split(Component.literal(section.title), indexWidth - 48);
        int y = contentTop + (24 - Math.min(2, heading.size()) * font.lineHeight) / 2;
        for (int i = 0; i < Math.min(2, heading.size()); i++) {
            FormattedCharSequence line = heading.get(i);
            graphics.drawString(font, line, innerX + (indexWidth - font.width(line)) / 2, y, INK, false);
            y += font.lineHeight;
        }
        String description = query.isBlank() ? "Section " + (section.ordinal() + 1) + " / " + Section.values().length : "All sections";
        graphics.drawString(font, description, innerX, contentTop + 53, MUTED, false);
        String count = filtered.size() + (filtered.size() == 1 ? " entry" : " entries");
        graphics.drawString(font, count, innerX + indexWidth - font.width(count), contentTop + 53, MUTED, false);
        if (filtered.isEmpty()) {
            String empty = !query.isBlank() ? "No matching unlocked entries. Try another search."
                    : !snapshot.synced() ? "Waiting for your journal to sync. Entries will appear here automatically."
                    : "No unlocked entries in this chapter yet.";
            drawLines(graphics, font.split(Component.literal(empty), indexWidth), innerX, rowTop,
                    font.lineHeight + 2, footerY - 6, MUTED);
        }
        drawCounter(graphics, innerX, indexWidth, (indexPage + 1) + " / " + indexPageCount());
    }

    private void renderReading(GuiGraphics graphics) {
        int y = contentTop;
        for (int i = 0; i < Math.min(3, titleLines.size()); i++) {
            if (i == 2 && titleLines.size() > 3) {
                String last = plainText(titleLines.get(i));
                graphics.drawString(font, CodexButton.fit(font, last + "...", readerWidth), readerX, y, INK, false);
            } else {
                graphics.drawString(font, titleLines.get(i), readerX, y, INK, false);
            }
            y += font.lineHeight + 2;
        }
        y = contentTop + titleHeight + 3;
        Entry entry = reading ? selectedEntry : null;
        if (entry != null && !entry.status().isBlank()) {
            graphics.drawString(font, CodexButton.fit(font, entry.statusLabel(), readerWidth), readerX, y, MUTED, false);
            y += font.lineHeight + 3;
        }
        if (entry != null && entry.target() > 0) {
            graphics.drawString(font, CodexButton.fit(font, entry.progressLabel(), readerWidth), readerX, y, INK, false);
            int barY = y + font.lineHeight + 2;
            graphics.fill(readerX, barY, readerX + readerWidth, barY + 3, 0xFFCDC8B8);
            int fill = (int) (readerWidth * Math.min(1.0, (double) entry.progress() / entry.target()));
            graphics.fill(readerX, barY, readerX + fill, barY + 3, 0xFF497F93);
        }
        drawLines(graphics, pagination.page(page), readerX, bodyY, pagination.lineHeight(), footerY - 6, INK);
        drawCounter(graphics, readerX, readerWidth, (pagination.clamp(page) + 1) + " / " + pagination.pageCount());
    }

    private void drawCounter(GuiGraphics graphics, int x, int width, String text) {
        graphics.drawString(font, text, x + (width - font.width(text)) / 2, footerY + 4, MUTED, false);
    }

    private void drawLines(GuiGraphics graphics, List<FormattedCharSequence> lines, int x, int y,
                           int lineHeight, int bottom, int color) {
        for (FormattedCharSequence line : lines) {
            if (y + font.lineHeight > bottom) break;
            graphics.drawString(font, line, x, y, color, false);
            y += lineHeight;
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // No inventory labels: all journal text is laid out in absolute coordinates above.
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (hasControlDown() && key == GLFW.GLFW_KEY_F) {
            showContents();
            return true;
        }
        if (hasAltDown() && key == GLFW.GLFW_KEY_LEFT) {
            goBack();
            return true;
        }
        if (search.isVisible() && search.isFocused()) {
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                onClose();
                return true;
            }
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                if (!filtered.isEmpty()) openEntry(filtered.get(0));
                return true;
            }
            if (key != GLFW.GLFW_KEY_TAB) {
                // Do not let inventory hotkeys close the book while typing a query.
                search.keyPressed(key, scanCode, modifiers);
                return true;
            }
        }
        if (key == GLFW.GLFW_KEY_BACKSPACE) {
            goBack();
            return true;
        }
        if (key == GLFW.GLFW_KEY_PAGE_UP || key == GLFW.GLFW_KEY_PAGE_DOWN
                || key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_RIGHT) {
            int direction = key == GLFW.GLFW_KEY_PAGE_UP || key == GLFW.GLFW_KEY_LEFT ? -1 : 1;
            if (reading) turnPage(direction); else turnIndex(direction);
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        // A contents button can hide itself during its click, before vanilla assigns focus.
        if (getFocused() instanceof AbstractWidget widget && !widget.visible) {
            setFocused(reading ? contentsButton : search);
        }
        return handled;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta == 0 || mouseY < contentTop || mouseY > footerY + 18) return false;
        int direction = delta > 0 ? -1 : 1;
        if ((!compact || !reading) && mouseX >= innerX && mouseX < innerX + indexWidth) {
            turnIndex(direction);
            return true;
        }
        if ((!compact || reading) && mouseX >= readerX && mouseX < readerX + readerWidth) {
            turnPage(direction);
            return true;
        }
        return false;
    }

    @Override
    public Component getNarrationMessage() {
        StringBuilder message = new StringBuilder("Divine Codex. ").append(section.title).append(". ");
        if (pagination != null && reading) {
            message.append(readingTitle).append(". ");
            if (selectedEntry != null) {
                message.append(selectedEntry.statusLabel()).append(". ");
                if (selectedEntry.target() > 0) message.append(selectedEntry.progressLabel()).append(". ");
            }
            message.append("Page ").append(pagination.clamp(page) + 1).append(" of ").append(pagination.pageCount()).append(". ");
            pagination.page(page).forEach(line -> message.append(plainText(line)).append(' '));
        } else {
            message.append(snapshot.synced() ? "Contents. " : "Waiting for journal sync. ");
            message.append(filtered.size()).append(" entries. Search spans all unlocked chapters.");
        }
        return Component.literal(message.toString());
    }

    private static String plainText(FormattedCharSequence line) {
        StringBuilder text = new StringBuilder();
        line.accept((index, style, codePoint) -> {
            text.appendCodePoint(codePoint);
            return true;
        });
        return text.toString();
    }

    @Override
    public boolean handleComponentClicked(Style style) {
        return false;
    }

    @Override
    protected void slotClicked(Slot slot, int slotId, int mouseButton, ClickType clickType) {
        // This is a read-only menu, including clicks outside the book and inventory hotkeys.
    }

    @Override
    protected boolean checkHotbarKeyPressed(int key, int scanCode) {
        return false;
    }

    @Override
    public void removed() {
        if (bookmarkConnection != null && bookmarkPlayer != null) {
            BOOKMARKS.computeIfAbsent(bookmarkConnection, ignored -> new HashMap<>())
                    .put(bookmarkPlayer, new Bookmark(view(), List.copyOf(history)));
        }
        // No client inventory to finalize. The legacy menu.removed mutates capability page state.
        // Normal onClose still closes the menu on the server through AbstractContainerScreen.
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
